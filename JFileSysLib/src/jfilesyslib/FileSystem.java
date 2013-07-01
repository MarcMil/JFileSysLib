package jfilesyslib;

import java.nio.ByteBuffer;

import jfilesyslib.data.DirectoryInfo;
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
 * Inherit from this class in order to get a basic, minimal file system.
 * There are some non-abstract methods which <i>should</i> be implemented.
 * 
 * @author Marc Miltenberger
 */
public abstract class FileSystem {
	String mountPath = null;
	Thread thrMounted;

	/**
	 * Reads a given directory and returns an iterator.
	 * @param path the path
	 * @return the iterator
	 * @throws NotADirectoryException the path is not a directory
	 * @throws PathNotFoundException the path could not be found
	 * @throws AccessDeniedException the access is denied
	 */
	public abstract Iterable<EntityInfo> listDirectory(String path) throws NotADirectoryException, PathNotFoundException, AccessDeniedException;

	/**
	 * Returns some meta data about the path.
	 * @param path the path
	 * @return the meta data
	 * @throws PathNotFoundException the path could not be found
	 */
	public abstract EntityInfo getFileMetaData(String path) throws PathNotFoundException;

	/**
	 * Renames a file/directory/symbolic link.<br>
	 * It is equivalent to moving a file/directory.
	 * @param source the source path
	 * @param destination the destination path
	 * @throws PathNotFoundException the source path or the parent directory of the destination could not be found
	 * @throws DestinationAlreadyExistsException the destination already exists
	 * @throws AccessDeniedException the access is denied
	 */
	public abstract void rename(String source, String destination) throws PathNotFoundException, DestinationAlreadyExistsException, AccessDeniedException;

	/**
	 * Opens a file.<br>
	 * It is guaranteed that createFile is called first for new files.<br>
	 * You should <b>not</b> overwrite the file if write is true.
	 * @param file the file to open
	 * @param read whether the file should be open to read
	 * @param write whether the file should be open to write
	 * @return the file handle
	 * @throws PathNotFoundException the file was not found
	 * @throws AccessDeniedException the access is denied
	 * @throws NotAFileException the given path is not a file
	 */
	public abstract FileHandle openFile(String file, boolean read, boolean write) throws PathNotFoundException, AccessDeniedException, NotAFileException;	

	/**
	 * Creates a new (blank) file.
	 * @param path the path to the file
	 * @throws PathNotFoundException the parent directory could not be found
	 * @throws DestinationAlreadyExistsException the destination already exists (regardless whether it is a directory or a file)
	 * @throws AccessDeniedException the access is denied
	 */
	public abstract void createFile(String path) throws PathNotFoundException, DestinationAlreadyExistsException, AccessDeniedException;
	
	/**
	 * Creates a new, empty directory.
	 * @param path the path to the new directory
	 * @throws PathNotFoundException the parent directory could not be found
	 * @throws DestinationAlreadyExistsException the destination already exists (regardless whether it is a directory or a file)
	 * @throws AccessDeniedException the access is denied
	 */
	public abstract void createDirectory(String path) throws PathNotFoundException, DestinationAlreadyExistsException, AccessDeniedException;	

	/**
	 * Reads from a file.<br>
	 * Offset specifies the offset within the opened file, not within the buffer.<br>
	 * The handle is guaranteed to be a valid file handle.
	 * @param handle the file handle
	 * @param buffer the buffer to fill
	 * @param offset the offset within the file
	 * @return the number of read bytes
	 */
	public abstract int read(FileHandle handle, ByteBuffer buffer, long offset);
	
	/**
	 * Truncates or lengthens a file.<br>
	 * The handle is guaranteed to be a valid file handle.
	 * @param handle the file handle
	 * @param length the new file size in bytes
	 * @throws DriveFullException there is no more space
	 */
	public abstract void setLength(FileHandle handle, long length) throws DriveFullException;

	/**
	 * (Over-)writes the buffer into the specified file handle.<br>
	 * Offset specifies the offset within the opened file, not within the buffer.<br>
	 * The handle is guaranteed to be a valid file handle.<p>
	 * Be <b>careful</b>: offset may be <i>larger</i> than the file size!<br>
	 * In this case you should fill everything in between with zeros.
	 * @param handle the file handle.
	 * @param buffer the buffer to write
	 * @param offset the offset within the file
	 * @throws DriveFullException there is no more space
	 * @throws PartIsLockedException the specified range has been locked
	 */
	public abstract void write(FileHandle handle, ByteBuffer buffer, long offset) throws DriveFullException, PartIsLockedException;

	/**
	 * Should write all pending data in the cache.<br>
	 * It is not guaranteed that this method is called before close.<br>
	 * The handle is guaranteed to be a valid file handle.
	 * @param handle the file handle
	 * @throws DriveFullException there is no more free space
	 */
	public abstract void flush(FileHandle handle) throws DriveFullException;

	/**
	 * Closes the file handle.<br>
	 * The handle is guaranteed to be a valid file handle and not closed yet.
	 * @param handle the file handle
	 * @throws DriveFullException there is no more space 
	 */
	public abstract void close(FileHandle handle) throws DriveFullException;

	/**
	 * Deletes a file or symbolic link.
	 * @param file the file
	 * @throws PathNotFoundException the file could not be found
	 * @throws AccessDeniedException the access is denied
	 * @throws NotAFileException the specified path is not a file
	 */
	public abstract void deleteFile(String file) throws PathNotFoundException, AccessDeniedException;
	
	/**
	 * Deletes a directory recursively.
	 * @param directory the directory
	 * @throws PathNotFoundException the directory could not be found
	 * @throws AccessDeniedException the access is denied
	 */
	public abstract void deleteDirectoryRecursively(String directory) throws PathNotFoundException, AccessDeniedException;

	/**
	 * Returns the volume name.
	 * @return the volume name
	 */
	public abstract String getVolumeName();
	
	/**
	 * Returns the file system name
	 * @return the file system name
	 */
	public abstract String getFileSystemName();

	/**
	 * Sets the last access time (unix timestamp).
	 * @param path the path
	 * @param atime the last access time (unix timestamp)
	 * @throws PathNotFoundException the path could not be found.
	 * @throws AccessDeniedException the access is denied
	 */
	public abstract void setLastAccessTime(String path, long atime) throws PathNotFoundException, AccessDeniedException;

	/**
	 * Sets the last modification time (unix timestamp).
	 * @param path the path
	 * @param mtime the last modification time (unix timestamp)
	 * @throws PathNotFoundException the path could not be found.
	 * @throws AccessDeniedException the access is denied
	 */
	public abstract void setLastModificationTime(String path, long mtime) throws PathNotFoundException, AccessDeniedException;

	/**
	 * Sets the creation time (unix timestamp).
	 * @param path the path
	 * @param ctime the creation time (unix timestamp)
	 * @throws PathNotFoundException the path could not be found.
	 * @throws AccessDeniedException the access is denied
	 */
	public abstract void setCreationTime(String path, long ctime) throws PathNotFoundException, AccessDeniedException;

	/**
	 * Returns true if the pathes are case sensitive
	 * @return true if the pathes are case sensitive
	 */
	public abstract boolean isCaseSensitive();

	/**
	 * Returns the block size in bytes
	 * @return the block size
	 */
	public abstract int getBlockSize();

	/**
	 * Returns the number of total blocks.
	 * E.g. a virtual 40 mb hard drive with 1 mb block size has 40 total blocks.
	 * @return the number of total blocks
	 */
	public abstract long getTotalBlockCount();
	
	/**
	 * Returns the number of free blocks, which are available to the current user.<br>
	 * In the easiast case:<br> Available Free Block Count = Free Block Count
	 * @return the number of free blocks
	 */
	public abstract long getFreeBlockAvailableCount();

	/**
	 * Returns the number of free blocks.
	 * @return the number of free blocks
	 */
	public abstract long getFreeBlockCount();
	
	@Override
    protected void finalize()  
	{
		if (mountPath != null)
			Mounter.unmount(this);
    }


	/**
	 * Returns the mount path
	 * @return the mount path
	 */
	public String getMountPath() {
		return mountPath;
	}
	
	/**
	 * Creates a symbolic link at <i>source</i> pointing to <i>destination</i>.<br>
	 * The default file system does not support this feature.
	 * @param source the source
	 * @param destination the destination
	 * @throws PathNotFoundException the parent directory (of source) could not be found
	 * @throws SourceAlreadyExistsException the <i>source</i> does already exist
	 * @throws AccessDeniedException the access was denied
	 * @throws UnsupportedFeatureException the feature is unsupported
	 * @see https://en.wikipedia.org/wiki/Symbolic_link
	 */
	public void createSymbolicLink(String source, String destination) throws PathNotFoundException, SourceAlreadyExistsException, AccessDeniedException, UnsupportedFeatureException {
		throw new UnsupportedFeatureException();
	}
	
	/**
	 * Creates a hard link at <i>source</i> pointing to <i>destination</i>.<br>
	 * The default file system does not support this feature.
	 * @param source the source
	 * @param destination the destination
	 * @throws PathNotFoundException the parent directory could not be found
	 * @throws SourceAlreadyExistsException the <i>source</i> does already exist
	 * @throws AccessDeniedException the access was denied
	 * @throws UnsupportedFeatureException the feature is unsupported
	 * @see https://en.wikipedia.org/wiki/Hard_link
	 */
	public void createHardLink(String source, String destination) throws PathNotFoundException, SourceAlreadyExistsException, AccessDeniedException, UnsupportedFeatureException {
		throw new UnsupportedFeatureException();
	}
	
	/**
	 * Checks whether the path already exists.<br>
	 * This is done via calling getFileMetaData and catching the PathNotFoundException.<br>
	 * You should consider to override it with a faster implementation
	 * @param path the path
	 * @return true if path exists
	 */
	public boolean pathExists(String path) {
		try {
			getFileMetaData(path);
			return true;
		} catch (PathNotFoundException e) {
			return false;
		}
	}

	/**
	 * Returns the maximum path length.
	 * The default value is 32768.
	 * @return the maximum path length
	 */
	public int getMaxPathLength() {
		return 32768;
	}

	/**
	 * Returns the number of files which may be created.
	 * @return the number of files which may be created
	 */
	public int getFilesFreeCount() {
		return 256000;
	}

	/**
	 * Returns the total number of files on the volume.<br>
	 * By default it returns 0, but it does not seem to harmful.
	 * @return the total number of files
	 */
	public int getTotalFilesCount() {
		return 0;
	}

	


	/**
	 * Sets the unix permissions for a file/directory.<br>
	 * This feature is not supported by default.
	 * @param path the path
	 * @param perms the permissions
	 * @throws PathNotFoundException the path was not found
	 * @throws AccessDeniedException the access is denied
	 * @throws UnsupportedFeatureException the feature is unsupported
	 */
	public void setUnixPermissions(String path, UnixPermissions perms) throws PathNotFoundException, AccessDeniedException, UnsupportedFeatureException  {
		throw new UnsupportedFeatureException();
	}

	/**
	 * Returns the unix permissions
	 * @param path the path
	 * @return the permissions
	 * @throws PathNotFoundException the path was not found
	 */
	public UnixPermissions getUnixPermissions(String path) throws PathNotFoundException {
		EntityInfo info = getFileMetaData(path);
		if (info == null)
		{
			System.err.println("getFileMetaData(" + path + ") returns null");
			return UnixPermissions.DefaultFilePermissions;
		}
		else
		{
			if (DirectoryInfo.class.isInstance(info))
				return UnixPermissions.DefaultDirectoryPermissions;
			else
				return UnixPermissions.DefaultFilePermissions;
		}
	}


	/**
	 * Sets the windows attributes.<br>
	 * This feature is not supported by default.
	 * @param path the path
	 * @param windowsAttributes the windows attributes 
	 * @throws PathNotFoundException the path was not found
	 * @throws AccessDeniedException the access is denied
	 * @throws UnsupportedFeatureException the feature is not supported
	 */
	public void setWindowsAttributes(String path, WindowsAttributes windowsAttributes) throws PathNotFoundException, AccessDeniedException, UnsupportedFeatureException {
		throw new UnsupportedFeatureException();
	}

	/**
	 * Returns the windows attributes.<br>
	 * The default implementation:<br>
	 * If the file system is read only, it returns the read only attribute.<br>
	 * If not, it does not return any attribute.
	 * @param path the path
	 * @return the windows attributes 
	 * @throws PathNotFoundException the path was not found
	 * @throws AccessDeniedException the access is denied
	 * @throws UnsupportedFeatureException the feature is not supported
	 */
	public WindowsAttributes getWindowsAttributes(String path) throws PathNotFoundException, AccessDeniedException, UnsupportedFeatureException {
		if (isReadOnly())
			return WindowsAttributes.ReadOnlyWindowsAttributes;
		else
			return WindowsAttributes.DefaultWindowsAttributes;
	}

	/**
	 * Locks a file in the specified range for every write access except for the File Handle <i>handle</i>.<br>
	 * This is windows specific.<br>
	 * This feature is not supported by default.
	 * @param handle the file handle
	 * @param byteOffset the byte offset
	 * @param length the length
	 * @throws PathNotFoundException the path was not found
	 * @throws AccessDeniedException the access is denied
	 * @throws NotAFileException the path is not a file
	 * @throws UnsupportedFeatureException the feature is not supported
	 * @throws AlreadyLockedException parts of the specified range are already locked
	 */
	public void lockFile(FileHandle handle, long byteOffset, long length) throws PathNotFoundException, AccessDeniedException, NotAFileException, UnsupportedFeatureException, AlreadyLockedException {
		throw new UnsupportedFeatureException();
	}
	
	/**
	 * Unlocks a file in the specified range.<br>
	 * This is windows specific.<br>
	 * The caller does not perform a check that the file handle is identical to the locker's file handle.<br>
	 * This feature is not supported by default.
	 * @param handle the file handle
	 * @param byteOffset the byte offset
	 * @param length the length
	 * @throws PathNotFoundException the path was not found
	 * @throws AccessDeniedException the access is denied
	 * @throws NotAFileException the path is not a file
	 * @throws UnsupportedFeatureException the feature is not supported
	 */
	public void unlockFile(FileHandle handle, long byteOffset, long length) throws PathNotFoundException, AccessDeniedException, NotAFileException, UnsupportedFeatureException {
		throw new UnsupportedFeatureException();
	}


	/**
	 * Returns the number of files (and direct subdirectories) in a directory
	 * @param info the directory info
	 * @return the number of files
	 */
	public int getNumberOfFilesInDirectory(DirectoryInfo info) {
		Iterable<EntityInfo> iterator = null;
		try {
			iterator = listDirectory(info.getFullPath());
		} catch (NotADirectoryException e) {
			e.printStackTrace();
			return 0;
		} catch (PathNotFoundException e) {
			e.printStackTrace();
			return 0;
		} catch (AccessDeniedException e) {
			e.printStackTrace();
		}
		int count = 0;
		for (@SuppressWarnings("unused")
			EntityInfo it : iterator)
			count++;
		return count;
	}


	/**
	 * Checks whether path is a file or directory and calls the appropriate method
	 * @param path the path
	 * @throws PathNotFoundException the path was not found
	 * @throws AccessDeniedException the access is denied
	 */
	public final void delete(String path) throws PathNotFoundException, AccessDeniedException {
		EntityInfo info = getFileMetaData(path);
		if (info == null)
			System.err.println("getFileMetaData(" + path + ") returns null");
		else
		{
			if (DirectoryInfo.class.isInstance(info))
				deleteDirectoryRecursively(path);
			else
				deleteFile(path);
		}
	}


	/**
	 * Returns true if the file system supports unicode filenames.<br>
	 * Since most file systems use Java Strings to encode file names, the default value is true.
	 * @return true if the file system supports unicode filenames.
	 */
	public boolean supportsUnicodeFilenames() {
		return true;
	}

	/**
	 * Returns true if the file system is compressed
	 * @return true if the file system is compressed
	 */
	public boolean isCompressed() {
		return false;
	}

	/**
	 * Returns a volume serial number
	 * @return a volume serial number
	 */
	public int getVolumeSerialNumber() {
		return 42;
	}


	/**
	 * Returns true if the file system is read only
	 * @return true if the file system is read only
	 */
	public boolean isReadOnly() {
		return false;
	}
	
	/**
	 * Returns one specific extended attribute.<br>
	 * This method does not need to be overriden.
	 * @param path the path of the file
	 * @param name the name of the attribute
	 * @return the attribute
	 * @throws PathNotFoundException the path was not found
	 * @throws AccessDeniedException the access is denied
	 * @throws UnsupportedFeatureException the feature is not supported
	 * @throws AttributeNotFoundException the specified attribute was not found
	 */
	public ExtendedAttribute getExtendedAttribute(String path, String name) throws PathNotFoundException, AccessDeniedException, UnsupportedFeatureException, AttributeNotFoundException
	{
		Iterable<ExtendedAttribute> attrList = listExtendedAttributes(path);
		for (ExtendedAttribute attribute : attrList)
		{
			if (attribute.getName().equals(name))
				return attribute;
		}
		throw new AttributeNotFoundException();
	}

	/**
	 * Lists the extended attributes of a path.<br>
	 * This feature is not supported by default.
	 * @param path the path
	 * @return the extended attributes
	 * @throws PathNotFoundException the path was not found
	 * @throws AccessDeniedException the access is denied
	 * @throws UnsupportedFeatureException the feature is not supported
	 */
	public Iterable<ExtendedAttribute> listExtendedAttributes(String path) throws PathNotFoundException, AccessDeniedException, UnsupportedFeatureException {
		throw new UnsupportedFeatureException();
	}
	
	/**
	 * Sets or adds extended attribute.<br>
	 * This feature is not supported by default.
	 * @param path the path
	 * @param attribute the attribute to add/set
	 * @throws PathNotFoundException the path was not found
	 * @throws AccessDeniedException the access is denied
	 * @throws UnsupportedFeatureException the feature is not supported
	 */
	public void setExtendedAttribute(String path, ExtendedAttribute attribute) throws PathNotFoundException, AccessDeniedException, UnsupportedFeatureException {
		throw new UnsupportedFeatureException();
	}
	
	/**
	 * Sets or adds extended attributes.<br>
	 * This feature is not supported by default.
	 * @param path the path
	 * @param attributeName the attribute name
	 * @throws PathNotFoundException the path was not found
	 * @throws AccessDeniedException the access is denied
	 * @throws UnsupportedFeatureException the feature is not supported
	 * @throws AttributeNotFoundException the specified attribute was not found
	 */
	public void removeExtendedAttribute(String path, String attributeName) throws PathNotFoundException, AccessDeniedException, UnsupportedFeatureException, AttributeNotFoundException {
		throw new UnsupportedFeatureException();
	}

	/**
	 * Is called directly before the file system is mounted
	 * @param mountPath the mount path
	 */
	public void beforeMounting(String mountPath)
	{
	}

	/**
	 * Is called directly before the file system is unmounted
	 */
	public void beforeUnmounting()
	{
	}

	/**
	 * Is called after the file system has been unmounted successfully.
	 */
	public void afterUnmounting() {
	}




}
