package jfilesyslib.filesystems;

import java.nio.ByteBuffer;

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
import jfilesyslib.utils.DateUtils;



/**
 * The logging file system.
 * Logs relevant actions.
 * 
 * @author Marc Miltenberger
 */
public class LoggingFs extends FullFileSystem {
	private FileSystem innerFs;
	private boolean pathNotFoundVerbose = false;
	
	/**
	 * Creates a new instance of the logging file system
	 * @param innerFs the inner file system
	 */
	public LoggingFs(FileSystem innerFs)
	{
		this.innerFs = innerFs;
	}
	
	/**
	 * Creates a new instance of the logging file system
	 * @param innerFs the inner file system
	 * @param pathNotFoundVerbose whether PathNotFoundExceptions should be logged verbose
	 */
	public LoggingFs(FileSystem innerFs, boolean pathNotFoundVerbose)
	{
		this.innerFs = innerFs;
		this.pathNotFoundVerbose = pathNotFoundVerbose;
	}
	
	protected void log(String text) {
		System.out.println("LoggingFs: " + text);
	}

	protected void log(Exception ex) {
		if (pathNotFoundVerbose)
			ex.printStackTrace(System.out);
		else
		{
			if (PathNotFoundException.class.isInstance(ex))
				System.out.println("Path not found: " + ((PathNotFoundException)ex).getPath());
			else
				ex.printStackTrace(System.out);
		}
	}
	
	@Override
	public void deleteFile(String file) throws PathNotFoundException, AccessDeniedException
	{
		log("deleteFile(\"" + file + "\")");
		try
		{
			innerFs.deleteFile(file);
		} catch (PathNotFoundException ex)
		{
			log(ex);
			throw ex;
		} catch (AccessDeniedException ex) {
			log(ex);
			throw ex;
		}
	}

	@Override
	public void createSymbolicLink(String from, String to) throws PathNotFoundException, SourceAlreadyExistsException, AccessDeniedException, UnsupportedFeatureException {
		log("createSymbolicLink(\"" + from + "\", \"" + to + "\")");
		try
		{
			innerFs.createSymbolicLink(from, to);
		} catch (PathNotFoundException ex)
		{
			log(ex);
			throw ex;
		} catch (SourceAlreadyExistsException ex)
		{
			log(ex);
			throw ex;
		} catch (UnsupportedFeatureException ex)
		{
			log(ex);
			throw ex;
		}
	}
	

	@Override
	public FileHandle openFile(String path, boolean read, boolean write)
			throws PathNotFoundException, AccessDeniedException,
			NotAFileException {
		log("openFile(\"" + path + "\", read = " + read + ", write = " + write + ")");
		try
		{
			return innerFs.openFile(path, read, write);
		} catch (PathNotFoundException ex)
		{
			log(ex);
			throw ex;
		} catch (AccessDeniedException ex)
		{
			log(ex);
			throw ex;
		} catch (NotAFileException ex)
		{
			log(ex);
			throw ex;
		} 
	}


	@Override
	public void createFile(String path) throws PathNotFoundException,
			DestinationAlreadyExistsException, AccessDeniedException {
		log("createFile(\"" + path + "\")");
		try
		{
			innerFs.createFile(path);
		} catch (PathNotFoundException ex)
		{
			log(ex);
			throw ex;
		} catch (DestinationAlreadyExistsException ex)
		{
			log(ex);
			throw ex;
		} catch (AccessDeniedException ex) {
			log(ex);
			throw ex;
		}
	}


	@Override
	public void createDirectory(String path) throws PathNotFoundException,
			DestinationAlreadyExistsException, AccessDeniedException {

		log("createDirectory(\"" + path + "\")");
		try
		{
			innerFs.createDirectory(path);
		} catch (PathNotFoundException ex)
		{
			log(ex);
			throw ex;
		} catch (DestinationAlreadyExistsException ex)
		{
			log(ex);
			throw ex;
		} catch (AccessDeniedException ex) {
			log(ex);
			throw ex;
		}
	}


	@Override
	public int read(FileHandle fh, ByteBuffer buffer, long offset) {

		log("read(\"" + fh.getFilePath() + "\", length = " + buffer.limit() + ", offset = " + offset + ")");
		int read = innerFs.read(fh, buffer, offset);
		log("Read " + read + " bytes");
		return read;
	}


	@Override
	public void write(FileHandle fh, ByteBuffer buffer, long offset) throws DriveFullException, PartIsLockedException {
		log("write(\"" + fh.getFilePath() + "\", length = " + buffer.limit() + ", offset = " + offset + ")");
		innerFs.write(fh, buffer, offset);
	}


	@Override
	public void flush(FileHandle fh) throws DriveFullException {
		log("flush(\"" + fh.getFilePath() + "\")");
		innerFs.flush(fh);
	}


	@Override
	public void close(FileHandle fh) throws DriveFullException {
		log("close(\"" + fh.getFilePath() + "\")");
		innerFs.close(fh);
	}


	@Override
	public void deleteDirectoryRecursively(String directory)
			throws PathNotFoundException, AccessDeniedException {
		log("deleteDirectoryRecursively(\"" + directory + "\")");
		try
		{
			innerFs.deleteDirectoryRecursively(directory);
		} catch (PathNotFoundException ex)
		{
			log(ex);
			throw ex;
		} catch (AccessDeniedException ex) {
			log(ex);
			throw ex;
		}
	}

	@Override
	public Iterable<EntityInfo> listDirectory(String path)
			throws NotADirectoryException, PathNotFoundException, AccessDeniedException {
		log("readDirectory(\"" + path + "\")");
		try
		{
			return innerFs.listDirectory(path);
		} catch (NotADirectoryException ex)
		{
			log(ex);
			throw ex;
		} catch (PathNotFoundException ex)
		{
			log(ex);
			throw ex;
		} catch (AccessDeniedException ex) {
			log(ex);
			throw ex;
		}
	}

	@Override
	public EntityInfo getFileMetaData(String path) throws PathNotFoundException {
		log("getFileMetaData(\"" + path + "\")");
		try
		{
			return innerFs.getFileMetaData(path);
		} catch (PathNotFoundException ex)
		{
			log(ex);
			throw ex;
		}
	}

	@Override
	public void rename(String from, String to) throws PathNotFoundException,
			DestinationAlreadyExistsException, AccessDeniedException {
		log("rename(from = \"" + from + "\", to = \"" + to + "\")");
		try
		{
			innerFs.rename(from, to);
		} catch (PathNotFoundException ex)
		{
			log(ex);
			throw ex;
		} catch (DestinationAlreadyExistsException ex)
		{
			log(ex);
			throw ex;
		} catch (AccessDeniedException ex)
		{
			log(ex);
			throw ex;
		}
	}

	@Override
	public String getVolumeName() {
		String volumeName = innerFs.getVolumeName();
		log("getVolumeName() returns " + volumeName);
		return volumeName;
	}

	@Override
	public String getFileSystemName() {
		String fsName = innerFs.getFileSystemName();
		log("getFileSystemName() returns " + fsName);
		return fsName;
	}

	@Override
	public void setLength(FileHandle fh, long length) throws DriveFullException {
		log("setLength(\"" + fh.getFilePath() + "\", " + length + ")");
		innerFs.setLength(fh, length);
	}

	@Override
	public void setLastAccessTime(String path, long atime)
			throws PathNotFoundException, AccessDeniedException {
		log("setLastAccessTime(\"" + path + "\", " + DateUtils.getDate(atime) + ")");
		try
		{
			innerFs.setLastAccessTime(path, atime);
		} catch (PathNotFoundException ex)
		{
			log(ex);
			throw ex;
		} catch (AccessDeniedException ex) {
			log(ex);
			throw ex;
		}
	}

	@Override
	public void setLastModificationTime(String path, long mtime)
			throws PathNotFoundException, AccessDeniedException {
		log("setLastModificationTime(\"" + path + "\", " + DateUtils.getDate(mtime) + ")");
		try
		{
			innerFs.setLastModificationTime(path, mtime);
		} catch (PathNotFoundException ex)
		{
			log(ex);
			throw ex;
		} catch (AccessDeniedException ex) {
			log(ex);
			throw ex;
		}
	}

	@Override
	public void setCreationTime(String path, long creationTime)
			throws PathNotFoundException, AccessDeniedException {
		log("setCreationTime(\"" + path + "\", " + DateUtils.getDate(creationTime) + ")");
		try
		{
			innerFs.setCreationTime(path, creationTime);
		} catch (PathNotFoundException ex)
		{
			log(ex);
			throw ex;
		} catch (AccessDeniedException ex) {
			log(ex);
			throw ex;
		}
	}

	@Override
	public boolean isCaseSensitive() {
		boolean res = innerFs.isCaseSensitive();
		log("isCaseSensitive() returns " + res);
		return res;	
	}

	@Override
	public int getBlockSize() {
		System.out.println("Block size: " + innerFs.getBlockSize() + ", total blocks: " + innerFs.getTotalBlockCount() + ", free blocks available: " + innerFs.getFreeBlockAvailableCount() + ", free blocks: " + innerFs.getFreeBlockCount());
		return innerFs.getBlockSize();
	}

	@Override
	public long getTotalBlockCount() {
		return innerFs.getTotalBlockCount();
	}

	@Override
	public long getFreeBlockAvailableCount() {
		return innerFs.getFreeBlockCount();
	}

	@Override
	public long getFreeBlockCount() {
		return innerFs.getFreeBlockCount();
	}
	
	@Override
	public void setUnixPermissions(String path, UnixPermissions perm) throws PathNotFoundException, AccessDeniedException, UnsupportedFeatureException  {
		log("setUnixPermissions: " + path + ", " + perm.toString());
		try {
			innerFs.setUnixPermissions(path, perm);
		}
		catch (PathNotFoundException ex)
		{
			log(ex);
			throw ex;
		} catch (UnsupportedFeatureException ex)
		{
			log(ex);
			throw ex;
		} catch (AccessDeniedException ex)
		{
			log(ex);
			throw ex;
		}
	}

	@Override
	public void createHardLink(String source, String destination)
			throws PathNotFoundException, SourceAlreadyExistsException,
			AccessDeniedException, UnsupportedFeatureException {
		log("createHardLink(" + source + ", " + destination + ")52655");
		try
		{
			innerFs.createHardLink(source, destination);
		}
		catch (PathNotFoundException ex)
		{
			log(ex);
			throw ex;
		} catch (UnsupportedFeatureException ex)
		{
			log(ex);
			throw ex;
		} catch (AccessDeniedException ex)
		{
			log(ex);
			throw ex;
		} catch (SourceAlreadyExistsException ex)
		{
			log(ex);
			throw ex;
		}			
	}

	@Override
	public int getMaxPathLength() {
		return innerFs.getMaxPathLength();
	}

	@Override
	public int getFilesFreeCount() {
		return innerFs.getFilesFreeCount();
	}

	@Override
	public int getTotalFilesCount() {
		return innerFs.getTotalFilesCount();
	}

	@Override
	public UnixPermissions getUnixPermissions(String path)
			throws PathNotFoundException {
		UnixPermissions perms;
		try {
			perms = innerFs.getUnixPermissions(path);
		}
		catch (PathNotFoundException ex)
		{
			log(ex);
			throw ex;
		}
		log("getUnixPermissions(\"" + path + "\"): " + perms);
		return perms;
	}

	@Override
	public void setWindowsAttributes(String path,
			WindowsAttributes windowsAttributes) throws PathNotFoundException,
			AccessDeniedException, UnsupportedFeatureException {
		log("SetWindowsAttributes(\"" + path + "\"): " + windowsAttributes);
		try {
			innerFs.setWindowsAttributes(path, windowsAttributes);
		}
		catch (PathNotFoundException ex)
		{
			log(ex);
			throw ex;
		} catch (UnsupportedFeatureException ex)
		{
			log(ex);
			throw ex;
		} catch (AccessDeniedException ex)
		{
			log(ex);
			throw ex;
		}
	}

	@Override
	public WindowsAttributes getWindowsAttributes(String path)
			throws PathNotFoundException, AccessDeniedException,
			UnsupportedFeatureException {
		WindowsAttributes perms;
		try
		{
			perms = innerFs.getWindowsAttributes(path);
		}
		catch (PathNotFoundException ex)
		{
			log(ex);
			throw ex;
		} catch (UnsupportedFeatureException ex)
		{
			log(ex);
			throw ex;
		} catch (AccessDeniedException ex)
		{
			log(ex);
			throw ex;
		}
		log("getWindowsAttributes(\"" + path + "\"): " + perms);
		return perms;
	}

	@Override
	public void lockFile(FileHandle handle, long byteOffset, long length)
			throws PathNotFoundException, AccessDeniedException,
			NotAFileException, UnsupportedFeatureException, AlreadyLockedException {
		log("lock(\"" + handle.getFilePath() + "\", " + byteOffset + ", " + length + ")");
		try {
			innerFs.lockFile(handle, byteOffset, length);
		}
		catch (PathNotFoundException ex)
		{
			log(ex);
			throw ex;
		} catch (UnsupportedFeatureException ex)
		{
			log(ex);
			throw ex;
		} catch (AccessDeniedException ex)
		{
			log(ex);
			throw ex;
		} catch (NotAFileException ex)
		{
			log(ex);
			throw ex;
		}
	}

	@Override
	public void unlockFile(FileHandle handle, long byteOffset, long length)
			throws PathNotFoundException, AccessDeniedException,
			NotAFileException, UnsupportedFeatureException {
		log("unlock(\"" + handle.getFilePath() + "\", " + byteOffset + ", " + length + ")");
		try {
			innerFs.unlockFile(handle, byteOffset, length);
		}
		catch (PathNotFoundException ex)
		{
			log(ex);
			throw ex;
		} catch (UnsupportedFeatureException ex)
		{
			log(ex);
			throw ex;
		} catch (AccessDeniedException ex)
		{
			log(ex);
			throw ex;
		} catch (NotAFileException ex)
		{
			log(ex);
			throw ex;
		}
	}

	@Override
	public boolean supportsUnicodeFilenames() {
		return innerFs.supportsUnicodeFilenames();
	}

	@Override
	public boolean isCompressed() {
		return innerFs.isCompressed();
	}

	@Override
	public int getVolumeSerialNumber() {
		return innerFs.getVolumeSerialNumber();
	}

	@Override
	public boolean isReadOnly() {
		return innerFs.isReadOnly();
	}

	@Override
	public Iterable<ExtendedAttribute> listExtendedAttributes(String path)
			throws PathNotFoundException, AccessDeniedException,
			UnsupportedFeatureException {

		log("List extended attributes of " + path);
		try {
			return innerFs.listExtendedAttributes(path);
		} catch (PathNotFoundException ex)
		{
			log(ex);
			throw ex;
		} catch (UnsupportedFeatureException ex)
		{
			log(ex);
			throw ex;
		} catch (AccessDeniedException ex)
		{
			log(ex);
			throw ex;
		}
	}

	@Override
	public void setExtendedAttribute(String path, ExtendedAttribute attribute)
			throws PathNotFoundException, AccessDeniedException,
			UnsupportedFeatureException {
		log("Set extended attribute " + attribute.getName() + " of " + path + " to " + attribute.getContent().length + " bytes");
		try	{
			innerFs.setExtendedAttribute(path, attribute);
		}
		catch (PathNotFoundException ex)
		{
			log(ex);
			throw ex;
		} catch (UnsupportedFeatureException ex)
		{
			log(ex);
			throw ex;
		} catch (AccessDeniedException ex)
		{
			log(ex);
			throw ex;
		}
	}

	@Override
	public void removeExtendedAttribute(String path, String attributeName)
			throws PathNotFoundException, AccessDeniedException,
			UnsupportedFeatureException, AttributeNotFoundException {
		log("Remove extended attribute " + attributeName + " from " + path);
		try {
			innerFs.removeExtendedAttribute(path, attributeName);
		} catch (PathNotFoundException ex)
		{
			log(ex);
			throw ex;
		} catch (AttributeNotFoundException ex)
		{
			log(ex);
			throw ex;
		} catch (UnsupportedFeatureException ex)
		{
			log(ex);
			throw ex;
		} catch (AccessDeniedException ex)
		{
			log(ex);
			throw ex;
		}
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
