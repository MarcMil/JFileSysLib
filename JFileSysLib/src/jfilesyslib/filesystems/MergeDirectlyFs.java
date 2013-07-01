package jfilesyslib.filesystems;

import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

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
 * Merges two file systems directly
 * @author Marc Miltenberger
 */
public class MergeDirectlyFs extends FullFileSystem {
	private FileSystem master;
	private FileSystem slave;
	private Set<FileHandle> slaveFileHandles = new HashSet<FileHandle>();
	
	/**
	 * Creates a new instance of MergeDirectlyFs.<br>
	 * The master filesystem is being checked first and used if a file or directory is about to be created.
	 * @param master the master file system
	 * @param slave the slave file system
	 */
	public MergeDirectlyFs(FileSystem master, FileSystem slave)
	{
		this.master = master;
		this.slave = slave;
	}

	@Override
	public void createSymbolicLink(String source, String destination)
			throws PathNotFoundException, SourceAlreadyExistsException,
			AccessDeniedException, UnsupportedFeatureException {
		master.createSymbolicLink(source, destination);
	}

	@Override
	public void createHardLink(String source, String destination)
			throws PathNotFoundException, SourceAlreadyExistsException,
			AccessDeniedException, UnsupportedFeatureException {
		throw new UnsupportedFeatureException();
	}

	@Override
	public int getMaxPathLength() {
		return Math.min(master.getMaxPathLength(), slave.getMaxPathLength());
	}

	@Override
	public int getFilesFreeCount() {
		return Math.min(master.getMaxPathLength(), slave.getMaxPathLength());
	}

	@Override
	public int getTotalFilesCount() {
		return slave.getTotalFilesCount() + master.getTotalFilesCount();
	}

	@Override
	public void setUnixPermissions(String path, UnixPermissions perms)
			throws PathNotFoundException, AccessDeniedException,
			UnsupportedFeatureException {
		try
		{
			master.setUnixPermissions(path, perms);
		} catch (PathNotFoundException ex)
		{
			slave.setUnixPermissions(path, perms);
		} catch (UnsupportedFeatureException ex) {
			slave.setUnixPermissions(path, perms);
		}
	}

	@Override
	public UnixPermissions getUnixPermissions(String path)
			throws PathNotFoundException {
		try
		{
			return master.getUnixPermissions(path);
		} catch (PathNotFoundException ex)
		{
			return slave.getUnixPermissions(path);
		}
	}

	@Override
	public void setWindowsAttributes(String path,
			WindowsAttributes windowsAttributes) throws PathNotFoundException,
			AccessDeniedException, UnsupportedFeatureException {
		try
		{
			master.setWindowsAttributes(path, windowsAttributes);
		} catch (PathNotFoundException ex)
		{
			slave.setWindowsAttributes(path, windowsAttributes);
		} catch (UnsupportedFeatureException ex) {
			slave.setWindowsAttributes(path, windowsAttributes);
		}
		
	}

	@Override
	public WindowsAttributes getWindowsAttributes(String path)
			throws PathNotFoundException, AccessDeniedException,
			UnsupportedFeatureException {
		try
		{
			return master.getWindowsAttributes(path);
		} catch (PathNotFoundException ex)
		{
			return slave.getWindowsAttributes(path);
		} catch (UnsupportedFeatureException ex) {
			return slave.getWindowsAttributes(path);
		}
	}

	@Override
	public void lockFile(FileHandle handle, long byteOffset, long length)
			throws PathNotFoundException, AccessDeniedException,
			NotAFileException, UnsupportedFeatureException,
			AlreadyLockedException {

		try
		{
			master.lockFile(handle, byteOffset, length);
		} catch (PathNotFoundException ex)
		{
			slave.lockFile(handle, byteOffset, length);
		}
	}

	@Override
	public void unlockFile(FileHandle handle, long byteOffset, long length)
			throws PathNotFoundException, AccessDeniedException,
			NotAFileException, UnsupportedFeatureException {
		try
		{
			master.unlockFile(handle, byteOffset, length);
		} catch (PathNotFoundException ex)
		{
			slave.unlockFile(handle, byteOffset, length);
		}
	}

	@Override
	public boolean supportsUnicodeFilenames() {
		return master.supportsUnicodeFilenames() && slave.supportsUnicodeFilenames();
	}

	@Override
	public boolean isCompressed() {
		return master.isCompressed() || slave.isCompressed();
	}

	@Override
	public int getVolumeSerialNumber() {
		return master.getVolumeSerialNumber();
	}

	@Override
	public boolean isReadOnly() {
		return master.isReadOnly();
	}

	@Override
	public Iterable<ExtendedAttribute> listExtendedAttributes(String path)
			throws PathNotFoundException, AccessDeniedException,
			UnsupportedFeatureException {
		try
		{
			return master.listExtendedAttributes(path);
		} catch (PathNotFoundException ex)
		{
			return slave.listExtendedAttributes(path);
		} catch (UnsupportedFeatureException ex) {
			return slave.listExtendedAttributes(path);
		}
	}

	@Override
	public void setExtendedAttribute(String path, ExtendedAttribute attribute)
			throws PathNotFoundException, AccessDeniedException,
			UnsupportedFeatureException {
		try
		{
			master.setExtendedAttribute(path, attribute);
		} catch (PathNotFoundException ex)
		{
			slave.setExtendedAttribute(path, attribute);
		} catch (UnsupportedFeatureException ex) {
			slave.setExtendedAttribute(path, attribute);
		}
	}

	@Override
	public void removeExtendedAttribute(String path, String attributeName)
			throws PathNotFoundException, AccessDeniedException,
			UnsupportedFeatureException, AttributeNotFoundException {

		try
		{
			master.removeExtendedAttribute(path, attributeName);
		} catch (PathNotFoundException ex)
		{
			slave.removeExtendedAttribute(path, attributeName);
		} catch (UnsupportedFeatureException ex) {
			slave.removeExtendedAttribute(path, attributeName);
		}
	}

	@Override
	public Iterable<EntityInfo> listDirectory(String path)
			throws NotADirectoryException, PathNotFoundException,
			AccessDeniedException {
		Iterable<EntityInfo> info1 = null, info2 = null;
		try
		{
			info1 = master.listDirectory(path);
		} catch (Exception ex)
		{
		}
		try
		{
			info2 = slave.listDirectory(path);
		} catch (Exception ex)
		{
		}
		if (info1 == null && info2 == null)
			return master.listDirectory(path);
		
		final Iterable<EntityInfo> cinfo1 = info1, cinfo2 = info2;

		final Iterator<EntityInfo> iter1 = (cinfo1 != null ? cinfo1.iterator() : null); 
		final Iterator<EntityInfo> iter2 = (cinfo2 != null ? cinfo2.iterator() : null); 
		
		return new Iterable<EntityInfo>() {

			@Override
			public Iterator<EntityInfo> iterator() {
				return new Iterator<EntityInfo>() {

					@Override
					public boolean hasNext() {
						if (iter1 != null && iter1.hasNext())
							return iter1.hasNext();
						if (iter2 != null && iter2.hasNext())
							return iter2.hasNext();
						return false;
					}

					@Override
					public EntityInfo next() {
						if (iter1 != null && iter1.hasNext())
							return iter1.next();
						return iter2.next();
					}

					@Override
					public void remove() {
					}
					
				};
			}
			
		};
	}

	@Override
	public EntityInfo getFileMetaData(String path) throws PathNotFoundException {
		try
		{
			return master.getFileMetaData(path);
		} catch (PathNotFoundException ex)
		{
			return slave.getFileMetaData(path);
		}
	}

	@Override
	public void rename(String source, String destination)
			throws PathNotFoundException, DestinationAlreadyExistsException,
			AccessDeniedException {
		try
		{
			master.rename(source, destination);
		} catch (Exception ex)
		{
			slave.rename(source, destination);
		}
		
	}

	@Override
	public FileHandle openFile(String file, boolean read, boolean write)
			throws PathNotFoundException, AccessDeniedException,
			NotAFileException {

		try
		{
			return master.openFile(file, read, write);
		} catch (PathNotFoundException ex)
		{
			
			FileHandle handle = slave.openFile(file, read, write);
			slaveFileHandles.add(handle);
			return handle;
		}
	}

	@Override
	public void createFile(String path) throws PathNotFoundException,
			DestinationAlreadyExistsException, AccessDeniedException {
		master.createFile(path);
	}

	@Override
	public void createDirectory(String path) throws PathNotFoundException,
			DestinationAlreadyExistsException, AccessDeniedException {
		master.createDirectory(path);
	}

	@Override
	public int read(FileHandle handle, ByteBuffer buffer, long offset) {
		if (slaveFileHandles.contains(handle))
			return slave.read(handle, buffer, offset);
		else
			return master.read(handle, buffer, offset);
	}

	@Override
	public void setLength(FileHandle handle, long length)
			throws DriveFullException {
		if (slaveFileHandles.contains(handle))
			slave.setLength(handle, length);
		else
			master.setLength(handle, length);
	}

	@Override
	public void write(FileHandle handle, ByteBuffer buffer, long offset)
			throws DriveFullException, PartIsLockedException {
		if (slaveFileHandles.contains(handle))
			slave.write(handle, buffer, offset);
		else
			master.write(handle, buffer, offset);
		
	}

	@Override
	public void flush(FileHandle handle) throws DriveFullException {
		if (slaveFileHandles.contains(handle))
			slave.flush(handle);
		else
			master.flush(handle);
	}

	@Override
	public void close(FileHandle handle) throws DriveFullException {
		if (slaveFileHandles.remove(handle))
			slave.close(handle);
		else
			master.close(handle);
		
	}

	@Override
	public void deleteFile(String file) throws PathNotFoundException,
			AccessDeniedException {

		try
		{
			master.deleteFile(file);
		} catch (Exception ex)
		{
			slave.deleteFile(file);
		}		
	}

	@Override
	public void deleteDirectoryRecursively(String directory)
			throws PathNotFoundException, AccessDeniedException {

		try
		{
			master.deleteDirectoryRecursively(directory);
		} catch (Exception ex)
		{
			slave.deleteDirectoryRecursively(directory);
		}
	}

	@Override
	public String getVolumeName() {
		return master.getVolumeName();
	}

	@Override
	public String getFileSystemName() {
		return master.getFileSystemName();
	}

	@Override
	public void setLastAccessTime(String path, long atime)
			throws PathNotFoundException, AccessDeniedException {
		try
		{
			master.setLastAccessTime(path, atime);
		} catch (PathNotFoundException ex)
		{
			slave.setLastAccessTime(path, atime);
		}
	}

	@Override
	public void setLastModificationTime(String path, long mtime)
			throws PathNotFoundException, AccessDeniedException {
		try
		{
			master.setLastModificationTime(path, mtime);
		} catch (PathNotFoundException ex)
		{
			slave.setLastModificationTime(path, mtime);
		}
	}

	@Override
	public void setCreationTime(String path, long ctime)
			throws PathNotFoundException, AccessDeniedException {
		try
		{
			master.setCreationTime(path, ctime);
		} catch (PathNotFoundException ex)
		{
			slave.setCreationTime(path, ctime);
		}
	}

	@Override
	public boolean isCaseSensitive() {
		return master.isCaseSensitive() || slave.isCaseSensitive();
	}

	@Override
	public int getBlockSize() {
		return master.getBlockSize();
	}

	@Override
	public long getTotalBlockCount() {
		return master.getTotalBlockCount();
	}

	@Override
	public long getFreeBlockAvailableCount() {
		return master.getFreeBlockAvailableCount();
	}

	@Override
	public long getFreeBlockCount() {
		return master.getFreeBlockCount();
	}

	@Override
	public void beforeMounting(String mountPath) {
		master.beforeMounting(mountPath);
		slave.beforeMounting(mountPath);
	}

	@Override
	public void beforeUnmounting() {
		master.beforeUnmounting();
		slave.beforeUnmounting();
	}

	@Override
	public void afterUnmounting() {
		master.afterUnmounting();
		slave.afterUnmounting();
	}
}
