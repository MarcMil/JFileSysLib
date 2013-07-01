package jfilesyslib.filesystems;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jfilesyslib.FileSystem;
import jfilesyslib.FullFileSystem;
import jfilesyslib.data.EntityInfo;
import jfilesyslib.data.ExtendedAttribute;
import jfilesyslib.data.FileHandle;
import jfilesyslib.data.FileInfo;
import jfilesyslib.data.SymbolicLinkInfo;
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
import jfilesyslib.utils.FileSystemUtils;



/**
 * Provides default handlers for symbolic links, unix permissions, windows attributes and extended attributes.
 * It enables file systems to use these advanced features without writing an own implementation.
 * 
 * 
 * @author Marc Miltenberger
 */
public class ExtendedSupportFs extends FullFileSystem {
	class FileLock {
		FileHandle handle;
		long from;
		long to;
		
		FileLock(FileHandle handle, long from, long to)
		{
			this.handle = handle;
			this.from = from;
			this.to = to;
		}
	}
	class RedirectedFileHandle extends FileHandle {

		public RedirectedFileHandle(String filePath, Object objHandle) {
			super(filePath, objHandle);
		}
		
		public FileHandle getRedirectedFileHandle() {
			return (FileHandle)this.getObjHandle();
		}
	}
	
	private Map<String, LinkedList<FileLock>> fileLocks = new ConcurrentHashMap<String, LinkedList<FileLock>>();
	
	private FileSystem innerFs, attributeFs;
	private static final String hiddenPrefix = "EXTENDED_$$";
	private static final String hiddenPermissions = "EXTENDED_$$PERMISSIONS";
	private static final String hiddenAttributes = "EXTENDED_$$ATTRIBUTES";
	static final String hiddenSymLinkMarker = "EXTENDED_$$SYMLINK";
	static final String hiddenHardLinkMarker = "EXTENDED_$$HARDLINK";
	
	private boolean symlinks = true;
	private boolean hardlinks = true;
	private boolean unixPermissions = true;
	private boolean windowsAttributes = true;
	private boolean fileLocking = true;
	private boolean extendedAttributes = true;
	

	/**
	 * Creates a new instance of ExtendedSupportFs
	 * @param innerFs the inner file system
	 */
	public ExtendedSupportFs(FileSystem innerFs)
	{
		this.innerFs = innerFs;
		this.attributeFs = innerFs;
	}
	/**
	 * Creates a new instance of ExtendedSupportFs
	 * @param innerFs the inner file system
	 * @param attributeFs the file system used to save attribute information. May be the same as innerFs
	 */
	public ExtendedSupportFs(FileSystem innerFs, FileSystem attributeFs)
	{
		this.innerFs = innerFs;
		this.attributeFs = attributeFs;
	}
	
	/**
	 * Creates a new instance of ExtendedSupportFs
	 * @param innerFs the inner file system
	 * @param attributeFs the file system used to save attribute information. May be the same as innerFs
	 * @param symolicLinks whether symbolic links should be handled by ExtendedSupportFs
	 * @param unixPermissions whether unix permissions should be handled by ExtendedSupportFs
	 * @param windowsAttributes whether windows attributes should be handled by ExtendedSupportFs
	 * @param extendedAttributes whether extended attributes should be handled by ExtendedSupportFs
	 * @param fileLocking whether file locking should be handled by ExtendedSupportFs
	 * @param hardLinks whether hard links should be handled by ExtendedSupportFs
	 */
	public ExtendedSupportFs(FileSystem innerFs, FileSystem attributeFs, boolean symolicLinks, boolean unixPermissions, boolean windowsAttributes, boolean extendedAttributes, boolean fileLocking, boolean hardLinks)
	{
		this.innerFs = innerFs;
		this.symlinks = symolicLinks;
		this.unixPermissions = unixPermissions;
		this.windowsAttributes = windowsAttributes;
		this.extendedAttributes = extendedAttributes;
		this.fileLocking = fileLocking;
		this.hardlinks = hardLinks;
		this.attributeFs = innerFs;
	}

	static String getSymLinkPath(String path)
	{
		return path + hiddenSymLinkMarker;
	}


	static String getHardLinkPath(String path)
	{
		return path + hiddenHardLinkMarker;
	}

	private static String getPermissionsPath(String path)
	{
		return path + hiddenPermissions;
	}
	
	private static String getAttributePath(String path)
	{
		return path + hiddenAttributes;
	}
	
	private static String getAttributePath(String path, String attributeName)
	{
		return path + hiddenAttributes + "_" + attributeName;
	}
	
	private LinkedList<FileLock> getFileLockList(String path, boolean create)
	{
		synchronized (fileLocks)
		{
			LinkedList<FileLock> locks = fileLocks.get(path);
			if (locks == null && create)
			{
				locks = new LinkedList<FileLock>();
				fileLocks.put(path, locks);
			}
			return locks;
		}
	}
	
	@Override
	public void deleteFile(String file) throws PathNotFoundException, AccessDeniedException
	{
		boolean wasSymlink = symlinks && attributeFs.pathExists(getSymLinkPath(file));
		boolean wasHardlink = hardlinks && attributeFs.pathExists(getHardLinkPath(file));
		if (!wasSymlink && !wasHardlink)
			innerFs.deleteFile(file);
		if (wasHardlink)
		{
			//Updating hard link paths
			String[] hardlinks = getHardLinks(file);
			String[] hardlinksAfter = new String[hardlinks.length - 1];
			int c = 0;
			for (int i = 0; i < hardlinks.length; i++)
			{
				if (!hardlinks[i].equals(file))
					hardlinksAfter[c++] = hardlinks[i];
			}
			//Only one file is left... Deleting hard link path
			if (hardlinksAfter.length == 1)
			{
				attributeFs.delete(getHardLinkPath(hardlinksAfter[0]));
			} else {
				for (String hardlink : hardlinksAfter)
				{
					try {
						FileSystemUtils.writeLines(attributeFs, getHardLinkPath(hardlink), hardlinksAfter);
					} catch (NotAFileException e) {
						e.printStackTrace();
					} catch (DriveFullException e) {
						e.printStackTrace();
					}
				}
			}
			if (file.equals(hardlinks[0]))
			{
				if (hardlinksAfter.length >= 1)
				{
					//oh oh... this is the original
					//lets save it...
						innerFs.delete(hardlinksAfter[0]);
					try
					{
						rename(file, hardlinksAfter[0]);
					} catch (Exception ex)
					{
					}
				} else
					innerFs.deleteFile(file);
			} else
				innerFs.deleteFile(file);
			attributeFs.delete(getHardLinkPath(file));
		}
		
		try
		{
			attributeFs.deleteFile(getPermissionsPath(file));
		} catch (Exception ex)
		{
		}
		try
		{
			attributeFs.deleteFile(getSymLinkPath(file));
		} catch (Exception ex)
		{
		}
		
		

		try
		{
			Iterable<ExtendedAttribute> extendedattributes = listExtendedAttributes(file);
			for (ExtendedAttribute attribute : extendedattributes)
			{
				try
				{
					attributeFs.deleteFile(getAttributePath(file, attribute.getName()));
				} catch (Exception ex)
				{
				}
			}
		} catch (Exception e)
		{
		}
		try
		{
			attributeFs.deleteFile(getAttributePath(file));
		} catch (Exception ex)
		{
		}
	}

	@Override
	public void createSymbolicLink(String source, String destination) throws PathNotFoundException, SourceAlreadyExistsException, AccessDeniedException, UnsupportedFeatureException {
		if (symlinks)
		{
			try {
				if (pathExists(source))
					throw new SourceAlreadyExistsException();
				innerFs.createFile(source);
				FileSystemUtils.writeWholeText(attributeFs, getSymLinkPath(source), destination);
			} catch (NotAFileException e) {
				e.printStackTrace();
			} catch (DriveFullException e) {
				e.printStackTrace();
			} catch (DestinationAlreadyExistsException e) {
				throw new SourceAlreadyExistsException();
			}
		} else
			innerFs.createSymbolicLink(source, destination);
	}
	


	@Override
	public FileHandle openFile(String path, boolean read, boolean write)
			throws PathNotFoundException, AccessDeniedException,
			NotAFileException {
		
		if (path.contains(hiddenPrefix))
			throw new AccessDeniedException();
		
		if (hardlinks)
		{
			//Get hard link paths
			String[] hardlinks = getHardLinks(path);
			if (hardlinks.length > 0)
			{
				FileHandle handle = innerFs.openFile(hardlinks[0], read, write);
				RedirectedFileHandle dummyHandle = new RedirectedFileHandle(path, handle);
				return dummyHandle;
			}
		}
		if (symlinks)
		{
			if (attributeFs.pathExists(getSymLinkPath(path)))
				throw new NotAFileException();
		}

		
		FileHandle handle = innerFs.openFile(path, read, write);
		return handle;
	}


	@Override
	public void createFile(String path) throws PathNotFoundException,
			DestinationAlreadyExistsException, AccessDeniedException {
		if (path.contains(hiddenPrefix))
			throw new AccessDeniedException();
		
		innerFs.createFile(path);
	}


	@Override
	public void createDirectory(String path) throws PathNotFoundException,
			DestinationAlreadyExistsException, AccessDeniedException {
		if (path.contains(hiddenPrefix))
			throw new AccessDeniedException();

		innerFs.createDirectory(path);
		if (attributeFs != innerFs)
			attributeFs.createDirectory(path);
	}


	public void flush(FileHandle handle) throws DriveFullException {
		if (hardlinks)
		{
			if (RedirectedFileHandle.class.isInstance(handle))
			{
				innerFs.flush(((RedirectedFileHandle)handle).getRedirectedFileHandle());
				return;
			}
		}
		innerFs.flush(handle);
	}


	@Override
	public void close(FileHandle handle) throws DriveFullException {
		if (hardlinks)
		{
			if (RedirectedFileHandle.class.isInstance(handle))
			{
				innerFs.close(((RedirectedFileHandle)handle).getRedirectedFileHandle());
				return;
			}
		}
		innerFs.close(handle);
	}


	@Override
	public void deleteDirectoryRecursively(String directory)
			throws PathNotFoundException, AccessDeniedException {
		try
		{
			attributeFs.deleteFile(getPermissionsPath(directory));
		} catch (Exception ex)
		{
		}
		try
		{
			attributeFs.deleteFile(getSymLinkPath(directory));
		} catch (Exception ex)
		{
		}

		try
		{
			Iterable<ExtendedAttribute> extendedattributes = listExtendedAttributes(directory);
			for (ExtendedAttribute attribute : extendedattributes)
			{
				try
				{
					attributeFs.deleteFile(getAttributePath(directory, attribute.getName()));
				} catch (Exception ex)
				{
				}
			}
		} catch (Exception e)
		{
		}
		try
		{
			attributeFs.deleteFile(getAttributePath(directory));
		} catch (Exception ex)
		{
		}
		innerFs.deleteDirectoryRecursively(directory);
	}

	@Override
	public Iterable<EntityInfo> listDirectory(String path)
			throws NotADirectoryException, PathNotFoundException, AccessDeniedException {
		
		return new FilterIterableEntityString(new FilterSymlinks(innerFs.listDirectory(path), innerFs, attributeFs), hiddenPrefix);
	}

	@Override
	public EntityInfo getFileMetaData(String path) throws PathNotFoundException {
		if (path.contains(hiddenPrefix))
			throw new PathNotFoundException(path);

		if (hardlinks)
		{
			//Get hard link paths
			String[] hardlinks = getHardLinks(path);
			if (hardlinks.length > 0)
			{
				EntityInfo info = innerFs.getFileMetaData(hardlinks[0]);
				FileInfo fileInfo = (FileInfo)info;
				FileInfo target = new FileInfo(path, fileInfo.getFileSize());
				target.setCreationTime(fileInfo.getCreationTime());
				target.setLastAccessTime(fileInfo.getLastAccessTime());
				target.setLastModificationTime(fileInfo.getLastModificationTime());
				return target;
			}
		}
		if (symlinks)
		{
			if (attributeFs.pathExists(getSymLinkPath(path)))
			{
				String dest = "";
				try {
					dest = FileSystemUtils.readWholeText(attributeFs, getSymLinkPath(path));
				} catch (PathNotFoundException e) {
					e.printStackTrace();
				} catch (AccessDeniedException e) {
					e.printStackTrace();
				} catch (NotAFileException e) {
					e.printStackTrace();
				}
				return new SymbolicLinkInfo(path, dest);
			}
		}
		return innerFs.getFileMetaData(path);
	}

	@Override
	public void rename(String from, String to) throws PathNotFoundException,
			DestinationAlreadyExistsException, AccessDeniedException {
		innerFs.rename(from, to);
		if (hardlinks && attributeFs.pathExists(getHardLinkPath(from)))
		{
			//Updating hard link paths
			String[] hardlinks = getHardLinks(from);
			for (int i = 0; i < hardlinks.length; i++)
			{
				if (hardlinks[i].equals(from))
					hardlinks[i] = to;
			}
			for (String hardlink : hardlinks)
			{
				try {
					FileSystemUtils.writeLines(attributeFs, getHardLinkPath(hardlink), hardlinks);
				} catch (NotAFileException e) {
					e.printStackTrace();
				} catch (DriveFullException e) {
					e.printStackTrace();
				}
			}
			attributeFs.delete(getHardLinkPath(from));
		}
		try
		{
			attributeFs.rename(getPermissionsPath(from), getPermissionsPath(to));
		} catch (Exception ex)
		{
			
		}
		try
		{
			attributeFs.rename(getSymLinkPath(from), getSymLinkPath(to));
		} catch (Exception ex)
		{
		}
		try
		{
			Iterable<ExtendedAttribute> extendedattributes = listExtendedAttributes(from);
			for (ExtendedAttribute attribute : extendedattributes)
			{
				try
				{
					attributeFs.rename(getAttributePath(from, attribute.getName()), getAttributePath(to, attribute.getName()));
				} catch (Exception ex)
				{
				}
			}
		} catch (Exception e)
		{
		}
		try
		{
			attributeFs.rename(getAttributePath(from), getAttributePath(to));
		} catch (Exception ex)
		{
			
		}
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
		if (hardlinks)
		{
			if (RedirectedFileHandle.class.isInstance(fh))
			{
				innerFs.setLength(((RedirectedFileHandle)fh).getRedirectedFileHandle(), length);
				return;
			}
		}
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
		return innerFs.getBlockSize();
	}

	
	public long getTotalBlockCount() {
		return innerFs.getTotalBlockCount();
	}

	public long getFreeBlockCount() {
		return innerFs.getFreeBlockCount();
	}

	public int getFilesFreeCount() {
		return innerFs.getFilesFreeCount();
	}

	public int getTotalFilesCount() {
		return innerFs.getTotalFilesCount();
	}

	public long getFreeBlockAvailableCount() {
		return innerFs.getFreeBlockAvailableCount();
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
	
	private String[] getHardLinks(String path)
	{
		try {
			return FileSystemUtils.readLines(attributeFs, getHardLinkPath(path));
		} catch (Exception e) {
			return new String[0];
		}
	}


	@Override
	public void createHardLink(String source, String destination) throws PathNotFoundException, SourceAlreadyExistsException, AccessDeniedException, UnsupportedFeatureException {
		if (hardlinks)
		{
			if (innerFs.pathExists(source))
				throw new SourceAlreadyExistsException();
			
			if (!innerFs.pathExists(destination))
				throw new PathNotFoundException(destination);
			
			//create source dummy file
			try {
				innerFs.createFile(source);
			} catch (DestinationAlreadyExistsException e1) {
				throw new SourceAlreadyExistsException();
			}
			
			try {
				String[] hardLinksBef = getHardLinks(destination);
				if (hardLinksBef.length == 0)
					hardLinksBef = new String[] { destination };
				String[] hardLinksAfter = new String[hardLinksBef.length + 1];
				System.arraycopy(hardLinksBef, 0, hardLinksAfter, 0, hardLinksBef.length);
				hardLinksAfter[hardLinksAfter.length - 1] = source;
				
				//write at first our new source file:
				FileSystemUtils.writeLines(attributeFs, getHardLinkPath(source), hardLinksAfter);
				for (String files : hardLinksBef)
					FileSystemUtils.writeLines(attributeFs, getHardLinkPath(files), hardLinksAfter);
				
			} catch (NotAFileException e) {
				e.printStackTrace();
			} catch (DriveFullException e) {
				e.printStackTrace();
			}
		} else
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
		
		if (hardlinks)
		{
			//Get hard link paths
			String[] hardlinks = getHardLinks(path);
			if (hardlinks.length > 0)
				path = hardlinks[0];
		}
		if (unixPermissions)
		{
			String[] lines = FileSystemUtils.readLines(attributeFs, getPermissionsPath(path), new String[4]);
			if (lines[0] == null)
			{
				if (!innerFs.pathExists(path))
					throw new PathNotFoundException(path);
			}
			lines[0] = String.valueOf(perms.getPermissions());
			lines[1] = String.valueOf(perms.getUid());
			lines[2] = String.valueOf(perms.getGid());
			if (lines[3] == null)
				lines[3] = "0";
			try {
				FileSystemUtils.writeLines(attributeFs, getPermissionsPath(path), lines);
			} catch (NotAFileException e) {
				e.printStackTrace();
			} catch (DriveFullException e) {
				e.printStackTrace();
			}
		} else
			innerFs.setUnixPermissions(path, perms);
	}


	@Override
	public UnixPermissions getUnixPermissions(String path)
			throws PathNotFoundException {

		if (hardlinks)
		{
			//Get hard link paths
			String[] hardlinks = getHardLinks(path);
			if (hardlinks.length > 0)
				path = hardlinks[0];
		}
		if (symlinks)
		{
			if (attributeFs.pathExists(getSymLinkPath(path)))
				return UnixPermissions.DefaultDirectoryPermissions;
		}
		if (unixPermissions)
		{
			try {
				String[] lines = FileSystemUtils.readLines(attributeFs, getPermissionsPath(path));
				if (lines.length == 0 || !lines[0].isEmpty())
				{
					int permissions = Integer.valueOf(lines[0]);
					int uid = Integer.valueOf(lines[1]);
					int gid = Integer.valueOf(lines[2]);
					return new UnixPermissions(permissions, uid, gid);
				}
				
			} catch (AccessDeniedException e) {
				e.printStackTrace();
			} catch (NotAFileException e) {
				e.printStackTrace();
			} catch (PathNotFoundException e)
			{
				if (!innerFs.pathExists(path))
					throw e;
			}
		}
		return innerFs.getUnixPermissions(path);
	}


	@Override
	public void setWindowsAttributes(String path,
			WindowsAttributes windowsAttributes) throws PathNotFoundException,
			AccessDeniedException, UnsupportedFeatureException {
		if (hardlinks)
		{
			//Get hard link paths
			String[] hardlinks = getHardLinks(path);
			if (hardlinks.length > 0)
				path = hardlinks[0];
		}

		if (this.windowsAttributes)
		{
			String[] lines = FileSystemUtils.readLines(attributeFs, getPermissionsPath(path), new String[4]);
			if (lines.length < 4)
			{
				List<String> s = new LinkedList<String>();
				for (String str : lines)
					s.add(str);
				while (s.size() < 4)
					s.add("");
				lines = s.toArray(new String[s.size()]);
			}
			if (lines[0] == null)
			{
				if (!innerFs.pathExists(path))
					throw new PathNotFoundException(path);
				
				lines[0] = "";
				lines[1] = "";
				lines[2] = "";
			}
			lines[3] = String.valueOf(windowsAttributes.getAttributes());
			try {
				FileSystemUtils.writeLines(attributeFs, getPermissionsPath(path), lines);
			} catch (NotAFileException e) {
				e.printStackTrace();
			} catch (DriveFullException e) {
				e.printStackTrace();
			}
		} else
			innerFs.setWindowsAttributes(path, windowsAttributes);
	}


	@Override
	public WindowsAttributes getWindowsAttributes(String path)
			throws PathNotFoundException, AccessDeniedException,
			UnsupportedFeatureException {
		if (hardlinks)
		{
			//Get hard link paths
			String[] hardlinks = getHardLinks(path);
			if (hardlinks.length > 0)
				path = hardlinks[0];
		}
		if (windowsAttributes)
		{
			try {
				String[] lines = FileSystemUtils.readLines(attributeFs, getPermissionsPath(path));
				if (lines.length > 3)
				{
					int permissions = Integer.valueOf(lines[3]);
					return new WindowsAttributes(permissions);
				}
				
			} catch (AccessDeniedException e) {
				e.printStackTrace();
			} catch (NotAFileException e) {
				e.printStackTrace();
			} catch (PathNotFoundException e)
			{
				if (!innerFs.pathExists(path))
					throw e;
			}
		}
		return new WindowsAttributes();
	}


	@Override
	public void lockFile(FileHandle handle, long byteOffset, long length)
			throws PathNotFoundException, AccessDeniedException,
			NotAFileException, UnsupportedFeatureException, AlreadyLockedException {

		if (hardlinks)
		{
			if (RedirectedFileHandle.class.isInstance(handle))
			{
				handle = ((RedirectedFileHandle)handle).getRedirectedFileHandle();
			}
		}
		if (fileLocking)
		{
			FileLock lock = getFileLockWithinOffset(handle.getFilePath(), byteOffset, byteOffset + length);
			if (lock != null)
				throw new AlreadyLockedException();
			
			LinkedList<FileLock> list = getFileLockList(handle.getFilePath(), true);
			list.add(new FileLock(handle, byteOffset, byteOffset + length));
		} else
			innerFs.lockFile(handle, byteOffset, length);
	}


	@Override
	public void unlockFile(FileHandle handle, long byteOffset, long length)
			throws PathNotFoundException, AccessDeniedException,
			NotAFileException, UnsupportedFeatureException {
		if (hardlinks)
		{
			if (RedirectedFileHandle.class.isInstance(handle))
			{
				handle = ((RedirectedFileHandle)handle).getRedirectedFileHandle();
			}
		}
		if (fileLocking)
		{
			FileLock lock = getFileLockWithinOffset(handle.getFilePath(), byteOffset, byteOffset + length);
			if (lock == null)
				return;
			
			if (lock.handle != handle)
				throw new AccessDeniedException();
			
			LinkedList<FileLock> locklist = this.getFileLockList(handle.getFilePath(), false);
			locklist.remove(lock);
			
		} else
			innerFs.unlockFile(handle, byteOffset, length);
	}


	@Override
	public Iterable<ExtendedAttribute> listExtendedAttributes(String path)
			throws PathNotFoundException, AccessDeniedException,
			UnsupportedFeatureException {

		if (extendedAttributes)
		{
			try {
				String[] lines = FileSystemUtils.readLines(attributeFs, getAttributePath(path));
				List<ExtendedAttribute> attributes = new LinkedList<ExtendedAttribute>();
				for (String line : lines)
				{
					if (line.isEmpty())
						continue;
					byte[] content;
					try
					{
						content = FileSystemUtils.readWhole(attributeFs, getAttributePath(path, line));
					} catch (Exception e)
					{
						content = new byte[0];
					}
					attributes.add(new ExtendedAttribute(line, content));
				}
				return attributes;
				
			} catch (AccessDeniedException e) {
				e.printStackTrace();
			} catch (NotAFileException e) {
				e.printStackTrace();
			} catch (PathNotFoundException e)
			{
				if (!innerFs.pathExists(path))
					throw e;
			}
			return new LinkedList<ExtendedAttribute>();
		}
		return innerFs.listExtendedAttributes(path);
	}


	@Override
	public void setExtendedAttribute(String path, ExtendedAttribute attribute)
			throws PathNotFoundException, AccessDeniedException,
			UnsupportedFeatureException {

		if (extendedAttributes)
		{
			String[] lines = null;
			try {
				lines = FileSystemUtils.readLines(attributeFs, getAttributePath(path));
				
			} catch (AccessDeniedException e) {
				e.printStackTrace();
			} catch (NotAFileException e) {
				e.printStackTrace();
			} catch (PathNotFoundException e)
			{
				if (!innerFs.pathExists(path))
					throw e;
				lines = new String[0];
			}
			try
			{
				if (!containsString(lines, attribute.getName()))
				{
					String[] newLines = new String[lines.length + 1];
					System.arraycopy(lines, 0, newLines, 0, lines.length);
					newLines[newLines.length - 1] = attribute.getName();
					FileSystemUtils.writeLines(attributeFs, getAttributePath(path), newLines);
				}
				FileSystemUtils.writeWhole(attributeFs, getAttributePath(path, attribute.getName()), attribute.getContent());

			} catch (AccessDeniedException e) {
				e.printStackTrace();
			} catch (NotAFileException e) {
				e.printStackTrace();
			} catch (PathNotFoundException e)
			{
				if (!innerFs.pathExists(path))
					throw e;
			} catch (DriveFullException e) {
				e.printStackTrace();
			}			
		} else
			innerFs.setExtendedAttribute(path, attribute);
	}


	private static boolean containsString(String[] seek, String contains) {
		for (String s : seek)
			if (s.equals(contains))
				return true;
		return false;
	}

	@Override
	public void removeExtendedAttribute(String path, String attributeName)
			throws PathNotFoundException, AccessDeniedException,
			UnsupportedFeatureException, AttributeNotFoundException {

		if (extendedAttributes)
		{
			String[] lines = null;
			try {
				lines = FileSystemUtils.readLines(attributeFs, getAttributePath(path));
				
			} catch (AccessDeniedException e) {
				e.printStackTrace();
				throw e;
			} catch (NotAFileException e) {
				e.printStackTrace();
			} catch (PathNotFoundException e)
			{
				if (!innerFs.pathExists(path))
					throw e;
				lines = new String[0];
			}
			try
			{
				if (containsString(lines, attributeName))
				{
					String[] newLines = new String[lines.length - 1];
					int i = 0;
					for (String s : lines)
					{
						if (!attributeName.equals(s))
							newLines[i++] = s;
					}
					if (attributeFs.pathExists(getAttributePath(path, attributeName)))
						attributeFs.deleteFile(getAttributePath(path, attributeName));
					FileSystemUtils.writeLines(attributeFs, getAttributePath(path), newLines);
				} else
					throw new AttributeNotFoundException();
			} catch (AccessDeniedException e) {
				throw e;
			} catch (NotAFileException e) {
				e.printStackTrace();
			} catch (PathNotFoundException e)
			{
				if (!innerFs.pathExists(path))
					throw e;
			} catch (DriveFullException e) {
				e.printStackTrace();
			}			
		}
	}


	@Override
	public int read(FileHandle handle, ByteBuffer buffer, long offset) {
		if (hardlinks)
		{
			if (RedirectedFileHandle.class.isInstance(handle))
			{
				return innerFs.read(((RedirectedFileHandle)handle).getRedirectedFileHandle(), buffer, offset);
			}
		}
		return innerFs.read(handle, buffer, offset);
	}


	@Override
	public void write(FileHandle handle, ByteBuffer buffer, long offset) throws DriveFullException, PartIsLockedException {
		if (fileLocking)
		{
			FileLock lock = getFileLockWithinOffset(handle.getFilePath(), offset, offset + buffer.limit());
			if (lock != null)
			{
				if (lock.handle != handle)
					throw new PartIsLockedException();
			}
		}
		if (hardlinks)
		{
			if (RedirectedFileHandle.class.isInstance(handle))
			{
				innerFs.write(((RedirectedFileHandle)handle).getRedirectedFileHandle(), buffer, offset);
				return;
			}
		}
		innerFs.write(handle, buffer, offset);
	}

	private FileLock getFileLockWithinOffset(String path, long from, long to) {
		LinkedList<FileLock> list = this.getFileLockList(path, false);
		if (list == null)
			return null;
		for (FileLock lock : list)
		{
			if ((from >= lock.from && from <= lock.to) || 
				(to >= lock.from && to <= lock.to) ||
				(from <= lock.from && to >= lock.to))
				return lock;
		}
		return null;
	}

	@Override
	public void beforeMounting(String mountPath) {
		innerFs.beforeMounting(mountPath);
	}

	@Override
	public void beforeUnmounting() {
		innerFs.beforeUnmounting();
	}

	@Override
	public void afterUnmounting() {
		innerFs.afterUnmounting();
	}
}
