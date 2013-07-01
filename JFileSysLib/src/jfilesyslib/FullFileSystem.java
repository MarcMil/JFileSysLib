package jfilesyslib;

import jfilesyslib.data.ExtendedAttribute;
import jfilesyslib.data.FileHandle;
import jfilesyslib.data.UnixPermissions;
import jfilesyslib.data.WindowsAttributes;
import jfilesyslib.exceptions.AccessDeniedException;
import jfilesyslib.exceptions.AlreadyLockedException;
import jfilesyslib.exceptions.AttributeNotFoundException;
import jfilesyslib.exceptions.NotAFileException;
import jfilesyslib.exceptions.PathNotFoundException;
import jfilesyslib.exceptions.SourceAlreadyExistsException;
import jfilesyslib.exceptions.UnsupportedFeatureException;

/**
 * This abstract class lets you implement every file system feature.
 * @author Marc Miltenberger
 */
public abstract class FullFileSystem extends FileSystem {
	@Override
	public abstract void createSymbolicLink(String source, String destination) throws PathNotFoundException, SourceAlreadyExistsException, AccessDeniedException, UnsupportedFeatureException;
	
	@Override
	public abstract void createHardLink(String source, String destination) throws PathNotFoundException, SourceAlreadyExistsException, AccessDeniedException, UnsupportedFeatureException;
	
	@Override
	public abstract int getMaxPathLength();
	
	@Override
	public abstract int getFilesFreeCount();
	
	@Override
	public abstract int getTotalFilesCount();

	

	@Override
	public abstract void setUnixPermissions(String path, UnixPermissions perms) throws PathNotFoundException, AccessDeniedException, UnsupportedFeatureException;

	@Override
	public abstract UnixPermissions getUnixPermissions(String path) throws PathNotFoundException;


	@Override
	public abstract void setWindowsAttributes(String path, WindowsAttributes windowsAttributes) throws PathNotFoundException, AccessDeniedException, UnsupportedFeatureException;
	
	@Override
	public abstract WindowsAttributes getWindowsAttributes(String path) throws PathNotFoundException, AccessDeniedException, UnsupportedFeatureException;
	
	@Override
	public abstract void lockFile(FileHandle handle, long byteOffset, long length) throws PathNotFoundException, AccessDeniedException, NotAFileException, UnsupportedFeatureException, AlreadyLockedException;
	
	@Override
	public abstract void unlockFile(FileHandle handle, long byteOffset, long length) throws PathNotFoundException, AccessDeniedException, NotAFileException, UnsupportedFeatureException;

	@Override
	public abstract boolean supportsUnicodeFilenames();

	@Override
	public abstract boolean isCompressed();

	@Override
	public abstract int getVolumeSerialNumber();


	@Override
	public abstract boolean isReadOnly();
	
	@Override
	public abstract Iterable<ExtendedAttribute> listExtendedAttributes(String path) throws PathNotFoundException, AccessDeniedException, UnsupportedFeatureException;
	
	@Override
	public abstract void setExtendedAttribute(String path, ExtendedAttribute attribute) throws PathNotFoundException, AccessDeniedException, UnsupportedFeatureException;
	
	@Override
	public abstract void removeExtendedAttribute(String path, String attributeName) throws PathNotFoundException, AccessDeniedException, UnsupportedFeatureException, AttributeNotFoundException;


	@Override
	public abstract void beforeMounting(String mountPath);

	@Override
	public abstract void beforeUnmounting();

	@Override
	public abstract void afterUnmounting();

}
