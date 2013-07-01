package jfilesyslib.filesystems;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jfilesyslib.FileSystem;
import jfilesyslib.FullFileSystem;
import jfilesyslib.data.EntityInfo;
import jfilesyslib.data.ExtendedAttribute;
import jfilesyslib.data.FileHandle;
import jfilesyslib.data.UnixPermissions;
import jfilesyslib.data.WindowsAttributes;
import jfilesyslib.exceptions.AccessDeniedException;
import jfilesyslib.exceptions.AlreadyLockedException;
import jfilesyslib.exceptions.AttributeNotFoundException;
import jfilesyslib.exceptions.DestinationAlreadyExistsException;
import jfilesyslib.exceptions.DriveFullException;
import jfilesyslib.exceptions.NotADirectoryException;
import jfilesyslib.exceptions.NotAFileException;
import jfilesyslib.exceptions.PartIsLockedException;
import jfilesyslib.exceptions.PathNotFoundException;
import jfilesyslib.exceptions.SourceAlreadyExistsException;
import jfilesyslib.exceptions.UnsupportedFeatureException;



/**
 * Provides a simple caching mechanism.
 * @author Marc Miltenberger
 */
public class CachingFs extends FullFileSystem {
	private class ReadCacheEntry
	{
		public boolean isValid;
		public byte[] cacheContent;
		public int validUntil;
		public long position;
	}

	private class WriteCacheEntry
	{
		private WriteCacheSingleEntry lastEntry = null; 
	}

	private class WriteCacheSingleEntry
	{
		public ByteArrayOutputStream cacheContent = new ByteArrayOutputStream(WriteCacheSize);
		public long position;
		
		public WriteCacheSingleEntry(byte[] cacheContent, long position)
		{
			try {
				this.cacheContent.write(cacheContent);
			} catch (IOException e) {
				e.printStackTrace();
			}
			this.position = position;
		}
	}
	
	private FileSystem innerFs;
	private int CacheSize, WriteCacheSize;
	
	
	private static final boolean PRINTDEBUG = false; 
	private Map<FileHandle, ReadCacheEntry> readcache = new ConcurrentHashMap<FileHandle, ReadCacheEntry>();
	private Map<FileHandle, WriteCacheEntry> writecache = new ConcurrentHashMap<FileHandle, WriteCacheEntry>();
	
	
	private WriteCacheEntry getWriteCache(FileHandle handle)
	{
		WriteCacheEntry entry = writecache.get(handle);
		if (entry == null)
		{
			entry = new WriteCacheEntry();
			writecache.put(handle, entry);
		}
		return entry;
	}
	
	/**
	 * Creates a new instance of the caching file system
	 * @param innerFs the inner file system
	 */
	public CachingFs(FileSystem innerFs)
	{
		this.innerFs = innerFs;
		CacheSize = innerFs.getBlockSize();
		WriteCacheSize = innerFs.getBlockSize();
	}
	

	/**
	 * Creates a new instance of the caching file system
	 * @param innerFs the inner file system
	 * @param cacheSize the cache size per entry in bytes
	 */
	public CachingFs(FileSystem innerFs, int cacheSize)
	{
		this.innerFs = innerFs;
		CacheSize = cacheSize;
		WriteCacheSize = cacheSize;
	}
	
	
	@Override
	public void deleteFile(String file) throws PathNotFoundException, AccessDeniedException
	{
		innerFs.deleteFile(file);
	}

	@Override
	public void createSymbolicLink(String from, String to) throws PathNotFoundException, SourceAlreadyExistsException, AccessDeniedException, UnsupportedFeatureException {
		innerFs.createSymbolicLink(from, to);
	}
	


	@Override
	public FileHandle openFile(String path, boolean read, boolean write)
			throws PathNotFoundException, AccessDeniedException,
			NotAFileException {
		FileHandle handle = innerFs.openFile(path, read, write);
		if (read)
			readcache.put(handle, new ReadCacheEntry());
		return handle;
	}


	@Override
	public void createFile(String path) throws PathNotFoundException,
			DestinationAlreadyExistsException, AccessDeniedException {
		innerFs.createFile(path);
	}


	@Override
	public void createDirectory(String path) throws PathNotFoundException,
			DestinationAlreadyExistsException, AccessDeniedException {

		innerFs.createDirectory(path);
	}


	@Override
	public int read(FileHandle fh, ByteBuffer buffer, long offset) {
		ReadCacheEntry entry = this.readcache.get(fh);
		if (entry.isValid)
		{
			int offsetInCache = (int)(offset - entry.position);
			if (offsetInCache >= 0 && offset + buffer.limit() < entry.position + entry.validUntil)
			{
				if (PRINTDEBUG)
					System.out.println("Cache hit");
				buffer.put(entry.cacheContent, offsetInCache, buffer.limit());
				return buffer.limit();
			}
		}
		if (buffer.limit() < CacheSize)
		{
			if (PRINTDEBUG)
				System.out.println("Cache miss");
			byte[] cache = new byte[CacheSize];
			ByteBuffer cbuffer = ByteBuffer.wrap(cache);
			int read = innerFs.read(fh, cbuffer, offset);
			int lengthRead;
			
			entry.cacheContent = cache;
			entry.position = offset;
			entry.validUntil = read;
			if (read > 0)
				entry.isValid = true;
			if (read > buffer.limit())
				lengthRead = buffer.limit();
			else
				lengthRead = read;
			
			buffer.put(cache, 0, lengthRead);
			return lengthRead;
		}
		else
			return innerFs.read(fh, buffer, offset);
	}


	@Override
	public void write(FileHandle fh, ByteBuffer buffer, long offset) throws DriveFullException, PartIsLockedException {
		ReadCacheEntry entry = this.readcache.get(fh);
		if (entry != null)
		{
			if (offset >= entry.position)
			{
				if (offset < entry.position + entry.validUntil)
				{
					entry.isValid = false;
				}
			}
		}
		
		
		WriteCacheEntry write = getWriteCache(fh);
		byte[] r = new byte[buffer.limit()];
		buffer.get(r);
		synchronized (write)
		{
			if (write.lastEntry == null)
			{
				if (PRINTDEBUG)
					System.out.println("Cache miss");
				write.lastEntry = new WriteCacheSingleEntry(r, offset);
			} else {
				long cachePosition = write.lastEntry.position + write.lastEntry.cacheContent.size();
				//System.out.println("Cache: " + cachePosition + ", offset: " + offset);
				if (cachePosition == offset)
				{
					//System.out.println("Write cache success :)");
					if (write.lastEntry.cacheContent.size() + r.length > WriteCacheSize)
					{
						commitWriteCache(fh, write);
						write.lastEntry = new WriteCacheSingleEntry(r, offset);
					} else
						write.lastEntry.cacheContent.write(r, 0, r.length);
				} else
				{
					if (PRINTDEBUG)
						System.out.println("Cache miss");
					commitWriteCache(fh, write);
					write.lastEntry = new WriteCacheSingleEntry(r, offset);
					//it works without it...
					commitWriteCache(fh, write);
				}
			}
		}
/*		buffer.rewind();
		innerFs.write(fh, buffer, offset);*/
	}


	private void commitWriteCache(FileHandle fh, WriteCacheEntry write) throws DriveFullException, PartIsLockedException {
		synchronized (write)
		{
			innerFs.write(fh, ByteBuffer.wrap(write.lastEntry.cacheContent.toByteArray()), write.lastEntry.position);
			write.lastEntry = null;
		}
	}
	
	private void flushWrite(FileHandle handle) throws DriveFullException, PartIsLockedException
	{
		WriteCacheEntry write = writecache.get(handle);
		if (write != null)
		{
			if (write.lastEntry != null)
			{
				commitWriteCache(handle, write);
			}
		}
	}

	@Override
	public void flush(FileHandle fh) throws DriveFullException {
		try {
			flushWrite(fh);
		} catch (PartIsLockedException e) {
		}
		innerFs.flush(fh);
	}


	@Override
	public void close(FileHandle fh) throws DriveFullException {
		try {
			flushWrite(fh);
		} catch (PartIsLockedException e) {
		}
		innerFs.flush(fh);
		innerFs.close(fh);
		readcache.remove(fh);
	}


	@Override
	public void deleteDirectoryRecursively(String directory)
			throws PathNotFoundException, AccessDeniedException {
		innerFs.deleteDirectoryRecursively(directory);
	}

	@Override
	public Iterable<EntityInfo> listDirectory(String path)
			throws NotADirectoryException, PathNotFoundException, AccessDeniedException {
		return innerFs.listDirectory(path);
	}

	@Override
	public EntityInfo getFileMetaData(String path) throws PathNotFoundException {
		return innerFs.getFileMetaData(path);
	}

	@Override
	public void rename(String from, String to) throws PathNotFoundException,
			DestinationAlreadyExistsException, AccessDeniedException {
		innerFs.rename(from, to);
	}

	@Override
	public String getVolumeName() {
		String volumeName = innerFs.getVolumeName();
		return volumeName;
	}

	@Override
	public String getFileSystemName() {
		String fsName = innerFs.getFileSystemName();
		return fsName;
	}

	@Override
	public void setLength(FileHandle fh, long length) throws DriveFullException {
		ReadCacheEntry entry = this.readcache.get(fh);
		if (entry != null)
			entry.isValid = false;
		innerFs.setLength(fh, length);
	}

	@Override
	public void setLastAccessTime(String path, long atime)
			throws PathNotFoundException, AccessDeniedException {
		innerFs.setLastAccessTime(path, atime);
	}

	@Override
	public void setLastModificationTime(String path, long mtime)
			throws PathNotFoundException, AccessDeniedException {
		innerFs.setLastAccessTime(path, mtime);
	}

	@Override
	public void setCreationTime(String path, long creationTime)
			throws PathNotFoundException, AccessDeniedException {
		innerFs.setLastAccessTime(path, creationTime);
	}

	@Override
	public boolean isCaseSensitive() {
		boolean res = innerFs.isCaseSensitive();
		return res;	
	}

	@Override
	public int getBlockSize() {
		return CacheSize;
	}
	


	public long getTotalBlockCount() {
		long total = innerFs.getTotalBlockCount() * innerFs.getBlockSize();
		total /= CacheSize;
		return total;
	}

	public long getFreeBlockCount() {
		long free = innerFs.getFreeBlockCount() * innerFs.getBlockSize();
		free /= CacheSize;
		return free;
	}

	public int getFilesFreeCount() {
		return innerFs.getFilesFreeCount();
	}

	public int getTotalFilesCount() {
		return innerFs.getTotalFilesCount();
	}

	public long getFreeBlockAvailableCount() {
		long free = innerFs.getFreeBlockAvailableCount() * innerFs.getBlockSize();
		free /= CacheSize;
		return free;
	}

	public boolean supportsUnicodeFilenames() {
		return innerFs.supportsUnicodeFilenames();
	}

	public boolean isCompressed() {
		return innerFs.isCompressed();
	}

	public int getVolumeSerialNumber() {
		return innerFs.getVolumeSerialNumber();
	}


	@Override
	public boolean isReadOnly() {
		return innerFs.isReadOnly();
	}

	@Override
	public void createHardLink(String source, String destination)
			throws PathNotFoundException, SourceAlreadyExistsException,
			AccessDeniedException, UnsupportedFeatureException {
		innerFs.createHardLink(source, destination);
	}

	@Override
	public int getMaxPathLength() {
		return innerFs.getMaxPathLength();
	}

	@Override
	public void setUnixPermissions(String path, UnixPermissions perms)
			throws PathNotFoundException, AccessDeniedException,
			UnsupportedFeatureException {
		innerFs.setUnixPermissions(path, perms);
	}

	@Override
	public UnixPermissions getUnixPermissions(String path)
			throws PathNotFoundException {
		return innerFs.getUnixPermissions(path);
	}

	@Override
	public void setWindowsAttributes(String path,
			WindowsAttributes windowsAttributes) throws PathNotFoundException,
			AccessDeniedException, UnsupportedFeatureException {
		innerFs.setWindowsAttributes(path, windowsAttributes);
	}

	@Override
	public WindowsAttributes getWindowsAttributes(String path)
			throws PathNotFoundException, AccessDeniedException,
			UnsupportedFeatureException {
		return innerFs.getWindowsAttributes(path);
	}

	@Override
	public void lockFile(FileHandle handle, long byteOffset, long length)
			throws PathNotFoundException, AccessDeniedException,
			NotAFileException, UnsupportedFeatureException, AlreadyLockedException {
		innerFs.lockFile(handle, byteOffset, length);
	}

	@Override
	public void unlockFile(FileHandle handle, long byteOffset, long length)
			throws PathNotFoundException, AccessDeniedException,
			NotAFileException, UnsupportedFeatureException {
		innerFs.unlockFile(handle, byteOffset, length);
	}

	@Override
	public Iterable<ExtendedAttribute> listExtendedAttributes(String path)
			throws PathNotFoundException, AccessDeniedException,
			UnsupportedFeatureException {
		return innerFs.listExtendedAttributes(path);
	}

	@Override
	public void setExtendedAttribute(String path, ExtendedAttribute attribute)
			throws PathNotFoundException, AccessDeniedException,
			UnsupportedFeatureException {
		innerFs.setExtendedAttribute(path, attribute);
		
	}

	@Override
	public void removeExtendedAttribute(String path, String attributeName)
			throws PathNotFoundException, AccessDeniedException,
			UnsupportedFeatureException, AttributeNotFoundException {
		innerFs.removeExtendedAttribute(path, attributeName);
		
	}

	@Override
	public void beforeMounting(String mountPath) {
		innerFs.beforeMounting(mountPath);
	}

	@Override
	public void beforeUnmounting() {
		for (FileHandle c : writecache.keySet())
		{
			WriteCacheEntry entry = writecache.remove(c);
			if (entry == null)
				continue;
			try {
				commitWriteCache(c, entry);
				innerFs.close(c);
			} catch (Exception e) {
			}
		}
		innerFs.beforeUnmounting();
	}
	

	@Override
	public void afterUnmounting() {
		innerFs.afterUnmounting();
	}
}
