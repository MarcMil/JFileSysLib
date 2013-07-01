package jfilesyslib;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import jfilesyslib.data.DirectoryInfo;
import jfilesyslib.data.EntityInfo;
import jfilesyslib.data.FileHandle;
import jfilesyslib.data.FileInfo;
import jfilesyslib.data.WindowsAttributes;
import jfilesyslib.exceptions.AccessDeniedException;
import jfilesyslib.exceptions.AlreadyLockedException;
import jfilesyslib.exceptions.DestinationAlreadyExistsException;
import jfilesyslib.exceptions.DriveFullException;
import jfilesyslib.exceptions.NotADirectoryException;
import jfilesyslib.exceptions.NotAFileException;
import jfilesyslib.exceptions.PartIsLockedException;
import jfilesyslib.exceptions.PathNotFoundException;
import jfilesyslib.exceptions.UnsupportedFeatureException;


import net.decasdev.dokan.ByHandleFileInformation;
import net.decasdev.dokan.DokanDiskFreeSpace;
import net.decasdev.dokan.DokanFileInfo;
import net.decasdev.dokan.DokanOperationException;
import net.decasdev.dokan.DokanOperations;
import net.decasdev.dokan.DokanVolumeInformation;
import net.decasdev.dokan.Win32FindData;


class DokanWrapper implements DokanOperations {
	private FileSystem fileSystem;
	
	private Map<Long, FileHandle> handles = new HashMap<Long, FileHandle>();
	private Random rand = new Random();
	
	private final static int CREATE_ALWAYS = 2;
	private final static int CREATE_NEW = 1;
	private final static int OPEN_ALWAYS = 4;
	private final static int OPEN_EXISTING = 3;
	private final static int TRUNCATE_EXISTING = 5;

	private static final int FILE_CASE_SENSITIVE_SEARCH = 1;
	private static final int FILE_UNICODE_ON_DISK = 4;
	private static final int FILE_VOLUME_IS_COMPRESSED = 32768;
	private static final int FILE_READ_ONLY_VOLUME = 524288;
	
	private boolean binitial = true;

	private static boolean DEBUG = false;
	
	public static final String INITIALFILENAME = "/.INITIALFILENAME____$"; 
	
	public DokanWrapper(FileSystem fileSystem) {
		this.fileSystem = fileSystem;
	}


	@Override
	public void onCleanup(String arg0, DokanFileInfo arg1)
			throws DokanOperationException {
		if (DEBUG)
			System.out.println("CleanUp: " + arg0);
		//this.onCloseFile(arg0, arg1);
	}

	@Override
	public void onCloseFile(String arg0, DokanFileInfo arg1)
			throws DokanOperationException {

		synchronized (handles)
		{
			if (DEBUG)
				System.out.println("CloseFile: " + arg0);
			if (arg1.handle == 0)
				return;
			FileHandle handle = handles.remove(arg1.handle);
			if (handle != null)
			{
				try {
					fileSystem.close(handle);
				} catch (DriveFullException e) {
					throw new DokanOperationException(net.decasdev.dokan.WinError.ERROR_DISK_FULL);
				} catch (Exception e)
				{
					e.printStackTrace();
				}
			}
			else
				throw new DokanOperationException(net.decasdev.dokan.WinError.ERROR_INVALID_HANDLE);
				
		}		
	}

	@Override
	public void onCreateDirectory(String arg0, DokanFileInfo arg1)
			throws DokanOperationException {
		try {
			if (DEBUG)
				System.out.println("CreateDirectory: " + arg0);
			fileSystem.createDirectory(this.mapWinToUnixPath(arg0));
		} catch (PathNotFoundException e) {
			throw new DokanOperationException(net.decasdev.dokan.WinError.ERROR_PATH_NOT_FOUND);
		} catch (AccessDeniedException e) {
			throw new DokanOperationException(net.decasdev.dokan.WinError.ERROR_ACCESS_DENIED);
		} catch (DestinationAlreadyExistsException e) {
			throw new DokanOperationException(net.decasdev.dokan.WinError.ERROR_ALREADY_EXISTS);
		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	@Override
	public long onCreateFile(String fileName, int desiredAccess, int shareMode, int creationDisposition,
			int flagsAndAttributes, DokanFileInfo dokanInfo) throws DokanOperationException {
		
		if (DEBUG)
			System.out.println("CreateFile: " + fileName + ", " + desiredAccess + ", " + shareMode + "," + creationDisposition + ", " + flagsAndAttributes);

		if (binitial)
		{
			if (mapWinToUnixPath(fileName).equals(INITIALFILENAME))
			{
				dokanInfo.handle = 5000;
				return 0;
			}
		}
		
		
		boolean read = (desiredAccess & net.decasdev.dokan.FileAccess.GENERIC_READ) == net.decasdev.dokan.FileAccess.GENERIC_READ;
		boolean write = (desiredAccess & net.decasdev.dokan.FileAccess.GENERIC_WRITE) == net.decasdev.dokan.FileAccess.GENERIC_WRITE;
		boolean append = (desiredAccess & 4) == 4;
		if (append)
			write = true;
		if (DEBUG)
			System.out.println("Read: "+ read + " write: " + write + " append: " + append);

		boolean setLengthZero = false;
		if (fileName.endsWith("*"))
			fileName = fileName.substring(0, fileName.length() - 1);
		boolean existed = false;
		boolean returnZero = false;
		switch (creationDisposition)
		{
			case TRUNCATE_EXISTING:
				try {
					EntityInfo info = fileSystem.getFileMetaData(mapWinToUnixPath(fileName));
					if (info == null)
						System.err.println("getFileMetaData(" + mapWinToUnixPath(fileName) + ") returns null");
					
					if (DirectoryInfo.class.isInstance(info))
						throw new DokanOperationException(net.decasdev.dokan.WinError.ERROR_DIRECTORY);
					
					write = true;
					setLengthZero = true;
				} catch (PathNotFoundException e) {
					throw new DokanOperationException(net.decasdev.dokan.WinError.ERROR_FILE_NOT_FOUND);
				} catch (Exception e)
				{
					e.printStackTrace();
				}
				break;
			case OPEN_EXISTING:
				try {
					EntityInfo info = fileSystem.getFileMetaData(mapWinToUnixPath(fileName));
					if (info == null)
						System.err.println("getFileMetaData(" + mapWinToUnixPath(fileName) + ") returns null");
					
					/*if (DirectoryInfo.class.isInstance(info))
						throw new DokanOperationException(net.decasdev.dokan.WinError.ERROR_DIRECTORY);*/
					
	
					existed = true;
				} catch (PathNotFoundException e) {
					throw new DokanOperationException(net.decasdev.dokan.WinError.ERROR_FILE_NOT_FOUND);
				} catch (Exception e)
				{
					e.printStackTrace();
				}
				break;
			case OPEN_ALWAYS:
				try {
					EntityInfo info = fileSystem.getFileMetaData(mapWinToUnixPath(fileName));
					if (info == null)
						System.err.println("getFileMetaData(" + mapWinToUnixPath(fileName) + ") returns null");
					
					if (DirectoryInfo.class.isInstance(info))
						throw new DokanOperationException(net.decasdev.dokan.WinError.ERROR_DIRECTORY);
					
	
					//existed = true;
				} catch (PathNotFoundException e) {
					try {
						fileSystem.createFile(mapWinToUnixPath(fileName));
						//returnZero = true;
					} catch (AccessDeniedException e1) {
						e1.printStackTrace();
					} catch (PathNotFoundException e1) {
						e1.printStackTrace();
					} catch (DestinationAlreadyExistsException e1) {
						e1.printStackTrace();
					}
				} catch (Exception e)
				{
					e.printStackTrace();
				}
				break;
			case CREATE_NEW:
				EntityInfo info = null;
				try {
					write = true;
					info = fileSystem.getFileMetaData(mapWinToUnixPath(fileName));
					if (info == null)
						System.err.println("getFileMetaData(" + mapWinToUnixPath(fileName) + ") returns null");
					
				} catch (PathNotFoundException e) {
					try {
						fileSystem.createFile(mapWinToUnixPath(fileName));
					} catch (AccessDeniedException e1) {
						e1.printStackTrace();
					} catch (PathNotFoundException e1) {
						e1.printStackTrace();
					} catch (DestinationAlreadyExistsException e1) {
						e1.printStackTrace();
					}
				} catch (Exception e)
				{
					e.printStackTrace();
				}
				
				if (info != null)
				{
					if (DirectoryInfo.class.isInstance(info))
						throw new DokanOperationException(net.decasdev.dokan.WinError.ERROR_DIRECTORY);
					
	
					throw new DokanOperationException(net.decasdev.dokan.WinError.ERROR_ALREADY_EXISTS);
				}
			case CREATE_ALWAYS:
				setLengthZero = true;
				write = true;
				info = null;
				try {
					info = fileSystem.getFileMetaData(mapWinToUnixPath(fileName));
					if (info == null)
						System.err.println("getFileMetaData(" + mapWinToUnixPath(fileName) + ") returns null");
					//should be...
					//existed = true;
				} catch (PathNotFoundException e) {
					try {
						fileSystem.createFile(mapWinToUnixPath(fileName));
					} catch (AccessDeniedException e1) {
						e1.printStackTrace();
					} catch (PathNotFoundException e1) {
						e1.printStackTrace();
					} catch (DestinationAlreadyExistsException e1) {
						e1.printStackTrace();
					}
				} catch (Exception e)
				{
					e.printStackTrace();
				}
				
				if (DirectoryInfo.class.isInstance(info))
					throw new DokanOperationException(net.decasdev.dokan.WinError.ERROR_DIRECTORY);
				
			
		}
		FileHandle handle;
		try {
			handle = fileSystem.openFile(this.mapWinToUnixPath(fileName), read, write);
			if (handle != null)
			{
				handle.read = read;
				handle.write = write;
			}
			if (setLengthZero)
				fileSystem.setLength(handle, 0);
		} catch (PathNotFoundException e) {
			throw new DokanOperationException(net.decasdev.dokan.WinError.ERROR_PATH_NOT_FOUND);
		} catch (AccessDeniedException e) {
			throw new DokanOperationException(net.decasdev.dokan.WinError.ERROR_ACCESS_DENIED);
		} catch (NotAFileException e) {
			dokanInfo.isDirectory = true;
			return 0;
			//throw new DokanOperationException(net.decasdev.dokan.WinError.ERROR_DIRECTORY);
		} catch (DriveFullException e) {
			throw new DokanOperationException(net.decasdev.dokan.WinError.ERROR_DISK_FULL);
		} catch (Exception e)
		{
			e.printStackTrace();
			return 0;
		}
		if (handle == null)
			System.err.println("openFile(" + mapWinToUnixPath(fileName) + ") returns null");
		long lhandle = this.getNextHandle(handle);
		
		dokanInfo.handle = lhandle;
		if (DEBUG)
			System.out.println("Handle: " + lhandle);

		if (existed && ((creationDisposition == CREATE_ALWAYS)
					|| (creationDisposition == OPEN_ALWAYS)))
			throw new DokanOperationException(net.decasdev.dokan.WinError.ERROR_ALREADY_EXISTS);
		if (returnZero)
			throw new DokanOperationException(0);
		return lhandle;
	}

	@Override
	public int onReadFile(String fileName, ByteBuffer buffer, long offset, DokanFileInfo arg3) throws DokanOperationException {
		FileHandle handle;
		synchronized (handles)
		{
			handle = handles.get(arg3.handle);
		}
		if (handle == null)
		{
			if (DEBUG)
				System.out.println("Invalid handle");
			
			throw new DokanOperationException(net.decasdev.dokan.WinError.ERROR_INVALID_HANDLE);
		}
		
		if (!handle.read)
		{
			if (DEBUG)
				System.out.println("No read access allowed");
			throw new DokanOperationException(net.decasdev.dokan.WinError.ERROR_ACCESS_DENIED);
		}
		
		try 
		{
			int read = fileSystem.read(handle, buffer, offset);
			if (DEBUG)
				System.out.println("ReadFile: " + fileName + ", offset: " + offset + ", read: " + read);
				
			return read;
		} catch (Exception e)
		{
			e.printStackTrace();
		}
		return 0;
	}
	
	@Override
	public void onDeleteDirectory(String arg0, DokanFileInfo arg1)
			throws DokanOperationException {
		try {
			if (DEBUG)
				System.out.println("SeleteDir: " + arg0);
			fileSystem.deleteDirectoryRecursively(mapWinToUnixPath(arg0));
		} catch (AccessDeniedException e) {
			throw new DokanOperationException(net.decasdev.dokan.WinError.ERROR_ACCESS_DENIED);
		} catch (PathNotFoundException e) {
			throw new DokanOperationException(net.decasdev.dokan.WinError.ERROR_PATH_NOT_FOUND);
		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	@Override
	public void onDeleteFile(String arg0, DokanFileInfo arg1)
			throws DokanOperationException {
		try {
			if (DEBUG)
				System.out.println("DeleteFile: " + arg0);
			fileSystem.deleteFile(mapWinToUnixPath(arg0));
		} catch (AccessDeniedException e) {
			throw new DokanOperationException(net.decasdev.dokan.WinError.ERROR_ACCESS_DENIED);
		} catch (PathNotFoundException e) {
			throw new DokanOperationException(net.decasdev.dokan.WinError.ERROR_PATH_NOT_FOUND);
		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public String mapWinToUnixPath(String winPath)
	{
		winPath = winPath.replace("\\*", "");
		return winPath.replace('\\', '/');
	}
	
	public String mapUnixToWinPath(String winPath)
	{
		return winPath.replace('/', '\\');
	}

	@Override
	public Win32FindData[] onFindFiles(String pathName, final DokanFileInfo fileInfo) throws DokanOperationException {
		return onFindFilesWithPattern(pathName, "*", fileInfo);
	}

	@Override
	public Win32FindData[] onFindFilesWithPattern(final String pathName, String wildcard, DokanFileInfo fileInfo) throws DokanOperationException {

		try {
			if (DEBUG)
				System.out.println("FindFiles: " + pathName);
			Iterable<EntityInfo> info = fileSystem.listDirectory(mapWinToUnixPath(pathName));
			if (info == null)
			{
				System.err.println("getFileSystemName(\"$PATH$\") returns null".replace("$PATH$", mapWinToUnixPath(pathName)));
				return new Win32FindData[0];
			}
			List<Win32FindData> resultList = new LinkedList<Win32FindData>();
			for (EntityInfo inf : info)
			{
				Win32FindData data = new Win32FindData();
				data.fileName = mapUnixToWinPath(inf.getFileName());
				data.creationTime = inf.getCreationTime();
				data.lastAccessTime = inf.getLastAccessTime();
				data.lastWriteTime = inf.getLastModificationTime();
				WindowsAttributes attr = null;
				try {
					attr = fileSystem.getWindowsAttributes(inf.getFullPath());
				} catch (UnsupportedFeatureException e) {
				}

				if (DEBUG)
					System.out.println("FindFiles: Found " + data.fileName);
				if (FileInfo.class.isInstance(inf)) {
					FileInfo cinfo = (FileInfo)inf;
					data.fileSize = cinfo.getFileSize();
					data.fileAttributes = net.decasdev.dokan.FileAttribute.FILE_ATTRIBUTE_NORMAL;
					resultList.add(data);
				}
				if (DirectoryInfo.class.isInstance(inf)) {
					data.fileAttributes = net.decasdev.dokan.FileAttribute.FILE_ATTRIBUTE_DIRECTORY;
					resultList.add(data);
				}
				if (attr != null)
					data.fileAttributes |= attr.getAttributes();
			}
			return resultList.toArray(new Win32FindData[resultList.size()]);
		} catch (AccessDeniedException e) {
			throw new DokanOperationException(net.decasdev.dokan.WinError.ERROR_ACCESS_DENIED);
		} catch (NotADirectoryException e) {
			throw new DokanOperationException(net.decasdev.dokan.WinError.ERROR_BAD_PATHNAME);
		} catch (PathNotFoundException e) {
			throw new DokanOperationException(net.decasdev.dokan.WinError.ERROR_PATH_NOT_FOUND);
		} catch (Exception e)
		{
			e.printStackTrace();
			return new Win32FindData[0];
		}
	}

	@Override
	public void onFlushFileBuffers(String arg0, DokanFileInfo arg1)
			throws DokanOperationException {
		if (DEBUG)
			System.out.println("FlushFileBuffers: " + arg0);
		FileHandle handle;
		synchronized (handles)
		{
			handle = handles.get(arg1.handle);
		}
		if (handle == null)
			throw new DokanOperationException(net.decasdev.dokan.WinError.ERROR_INVALID_HANDLE);
		
		if (!handle.write)
			return;
			//throw new DokanOperationException(net.decasdev.dokan.WinError.ERROR_ACCESS_DENIED);
		
		try {
			fileSystem.flush(handle);
		} catch (DriveFullException e) {
			throw new DokanOperationException(net.decasdev.dokan.WinError.ERROR_DISK_FULL);
		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	@Override
	public DokanDiskFreeSpace onGetDiskFreeSpace(DokanFileInfo arg0)
			throws DokanOperationException {
		DokanDiskFreeSpace space = new DokanDiskFreeSpace();

		try	{
			space.totalNumberOfBytes = fileSystem.getTotalBlockCount() * fileSystem.getBlockSize();
			space.freeBytesAvailable = fileSystem.getFreeBlockAvailableCount() * fileSystem.getBlockSize();
			space.totalNumberOfFreeBytes = fileSystem.getFreeBlockCount() * fileSystem.getBlockSize();
		} catch (Exception e)
		{
			e.printStackTrace();
		}
		return space;
	}

	@Override
	public ByHandleFileInformation onGetFileInformation(String arg0,
			DokanFileInfo arg1) throws DokanOperationException {
		EntityInfo inf;
		if (mapWinToUnixPath(arg0).equals(INITIALFILENAME))
		{
			return new ByHandleFileInformation(net.decasdev.dokan.FileAttribute.FILE_ATTRIBUTE_NORMAL, 0, 0, 0, 0, 0, 1, 0);
		}
		try {
			if (arg0.endsWith("*"))
				arg0 = arg0.substring(0, arg0.length() - 1);
			inf = fileSystem.getFileMetaData(mapWinToUnixPath(arg0));
		} catch (PathNotFoundException e) {
			throw new DokanOperationException(net.decasdev.dokan.WinError.ERROR_PATH_NOT_FOUND);
		} catch (Exception e)
		{
			e.printStackTrace();
			return null;
		}
		
		
		if (FileInfo.class.isInstance(inf)) {
			FileInfo cinfo = (FileInfo)inf;
			return new ByHandleFileInformation(net.decasdev.dokan.FileAttribute.FILE_ATTRIBUTE_NORMAL, inf.getCreationTime(), inf.getLastAccessTime(), inf.getLastModificationTime(), 0, cinfo.getFileSize(), 1, 0);
		}
		if (DirectoryInfo.class.isInstance(inf)) {
			return new ByHandleFileInformation(net.decasdev.dokan.FileAttribute.FILE_ATTRIBUTE_DIRECTORY, inf.getCreationTime(), inf.getLastAccessTime(), inf.getLastModificationTime(), 0, 0, 1, 0);
		}
		throw new DokanOperationException(net.decasdev.dokan.WinError.ERROR_BAD_PATHNAME); 
	}

	@Override
	public DokanVolumeInformation onGetVolumeInformation(String arg0,
			DokanFileInfo arg1) throws DokanOperationException {
		DokanVolumeInformation info = new DokanVolumeInformation();
		info.maximumComponentLength = fileSystem.getMaxPathLength();
		if (fileSystem.supportsUnicodeFilenames())
			info.fileSystemFlags |= FILE_UNICODE_ON_DISK; 
			
		if (fileSystem.isCaseSensitive())
			info.fileSystemFlags |= FILE_CASE_SENSITIVE_SEARCH; 
		
		if (fileSystem.isCompressed())
			info.fileSystemFlags |= FILE_VOLUME_IS_COMPRESSED;
		
		if (fileSystem.isReadOnly())
			info.fileSystemFlags |= FILE_READ_ONLY_VOLUME;
	
		
		try
		{
			info.volumeSerialNumber = fileSystem.getVolumeSerialNumber();
	
			info.fileSystemName = fileSystem.getFileSystemName();
			if (info.fileSystemName == null)
				System.err.println("getFileSystemName() returns null");
			info.volumeName = fileSystem.getVolumeName();
			if (info.volumeName == null)
				System.err.println("getVolumeName() returns null");
		} catch (Exception e)
		{
			e.printStackTrace();
		}
		
		return info;
	}
	


	long getNextHandle(FileHandle handle) {
		while (true)
		{
			long handleTest = rand.nextLong();
			if (handleTest < 0)
				handleTest *= -1;
			if (handleTest == 0)
				continue;
			
			synchronized (handles)
			{
				if (!handles.containsKey(handleTest))
					handles.put(handleTest, handle);
			}
			return handleTest;
		}
	}

	@Override
	public void onMoveFile(String arg0, String arg1, boolean replace,
			DokanFileInfo arg3) throws DokanOperationException {
		try {
			if (replace)
			{
				try {
					EntityInfo inf = fileSystem.getFileMetaData(mapWinToUnixPath(arg1));
					if (DirectoryInfo.class.isInstance(inf))
						fileSystem.deleteDirectoryRecursively(arg1);
					else
						fileSystem.deleteFile(arg1);
				} catch (PathNotFoundException e) {
				}
			}
			fileSystem.rename(mapWinToUnixPath(arg0), mapWinToUnixPath(arg1));
		} catch (PathNotFoundException e) {
			throw new DokanOperationException(net.decasdev.dokan.WinError.ERROR_PATH_NOT_FOUND);
		} catch (DestinationAlreadyExistsException e) {
			throw new DokanOperationException(net.decasdev.dokan.WinError.ERROR_ALREADY_EXISTS);
		} catch (AccessDeniedException e) {
			throw new DokanOperationException(net.decasdev.dokan.WinError.ERROR_ACCESS_DENIED);
		} catch (Exception e)
		{
			e.printStackTrace();
		}
		
	}

	@Override
	public long onOpenDirectory(String arg0, DokanFileInfo arg1)
			throws DokanOperationException {
		if (DEBUG)
			System.out.println("OpenDirectory: " + arg0);
		return 0;
	}


	@Override
	public void onSetEndOfFile(String arg0, long arg1, DokanFileInfo arg2)
			throws DokanOperationException {
		FileHandle handle;
		synchronized (handles)
		{
			handle = handles.get(arg2.handle);
		}
		if (handle == null)
			throw new DokanOperationException(net.decasdev.dokan.WinError.ERROR_INVALID_HANDLE);
		try {
			fileSystem.setLength(handle, arg1);
		} catch (DriveFullException e) {
			throw new DokanOperationException(net.decasdev.dokan.WinError.ERROR_DISK_FULL);
		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	@Override
	public void onSetFileAttributes(String path, int fileAttributes, DokanFileInfo fileInfo)
			throws DokanOperationException {
		try {
			path = mapWinToUnixPath(path);
			fileSystem.setWindowsAttributes(path, new WindowsAttributes(fileAttributes));
		} catch (PathNotFoundException e) {
			throw new DokanOperationException(net.decasdev.dokan.WinError.ERROR_PATH_NOT_FOUND);
		} catch (AccessDeniedException e) {
			throw new DokanOperationException(net.decasdev.dokan.WinError.ERROR_ACCESS_DENIED);
		} catch (UnsupportedFeatureException e) {
			throw new DokanOperationException(net.decasdev.dokan.WinError.ERROR_NOT_SUPPORTED);
		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	@Override
	public void onSetFileTime(String fileName, final long creationTime, final long lastAccessTime,
			final long lastWriteTime, final DokanFileInfo fileInfo) throws DokanOperationException {
		try {
			fileName = mapWinToUnixPath(fileName);
			fileSystem.setLastAccessTime(fileName, lastAccessTime);
			fileSystem.setLastModificationTime(fileName, lastWriteTime);
			fileSystem.setCreationTime(fileName, creationTime);
		} catch (AccessDeniedException e) {
			throw new DokanOperationException(net.decasdev.dokan.WinError.ERROR_ACCESS_DENIED);
		} catch (PathNotFoundException e) {
			throw new DokanOperationException(net.decasdev.dokan.WinError.ERROR_PATH_NOT_FOUND);
		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	@Override
	public void onLockFile(String path, long byteOffset, long length, DokanFileInfo arg3)
			throws DokanOperationException {
		try {
			FileHandle handle;
			synchronized (handles)
			{
				handle = handles.get(arg3.handle);
			}
			if (handle == null)
				throw new DokanOperationException(net.decasdev.dokan.WinError.ERROR_INVALID_HANDLE);
			path = mapWinToUnixPath(path);
			if (!handle.getFilePath().equals(path))
				throw new DokanOperationException(net.decasdev.dokan.WinError.ERROR_ACCESS_DENIED);
			fileSystem.lockFile(handle, byteOffset, length);

		} catch (AccessDeniedException e) {
			throw new DokanOperationException(net.decasdev.dokan.WinError.ERROR_ACCESS_DENIED);
		} catch (PathNotFoundException e) {
			throw new DokanOperationException(net.decasdev.dokan.WinError.ERROR_PATH_NOT_FOUND);
		} catch (NotAFileException e) {
			throw new DokanOperationException(net.decasdev.dokan.WinError.ERROR_DIRECTORY);
		} catch (UnsupportedFeatureException e) {
			throw new DokanOperationException(net.decasdev.dokan.WinError.ERROR_NOT_SUPPORTED);
		} catch (AlreadyLockedException e){
			throw new DokanOperationException(net.decasdev.dokan.WinError.ERROR_LOCK_VIOLATION);
		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	@Override
	public void onUnlockFile(String path, long byteOffset, long length,
			DokanFileInfo arg3) throws DokanOperationException {
		try {
			FileHandle handle;
			synchronized (handles)
			{
				handle = handles.get(arg3.handle);
			}
			if (handle == null)
				throw new DokanOperationException(net.decasdev.dokan.WinError.ERROR_INVALID_HANDLE);
			path = mapWinToUnixPath(path);
			if (!handle.getFilePath().equals(path))
				throw new DokanOperationException(net.decasdev.dokan.WinError.ERROR_ACCESS_DENIED);
			fileSystem.unlockFile(handle, byteOffset, length);
		} catch (AccessDeniedException e) {
			throw new DokanOperationException(net.decasdev.dokan.WinError.ERROR_ACCESS_DENIED);
		} catch (PathNotFoundException e) {
			throw new DokanOperationException(net.decasdev.dokan.WinError.ERROR_PATH_NOT_FOUND);
		} catch (NotAFileException e) {
			throw new DokanOperationException(net.decasdev.dokan.WinError.ERROR_DIRECTORY);
		} catch (UnsupportedFeatureException e) {
			throw new DokanOperationException(net.decasdev.dokan.WinError.ERROR_NOT_SUPPORTED);
		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	@Override
	public void onUnmount(DokanFileInfo arg0) throws DokanOperationException {
	}

	@Override
	public int onWriteFile(String fileName, ByteBuffer buffer, long offset, DokanFileInfo arg3) throws DokanOperationException {
		FileHandle handle;
		synchronized (handles)
		{
			handle = handles.get(arg3.handle);
		}
		if (handle == null)
			throw new DokanOperationException(net.decasdev.dokan.WinError.ERROR_INVALID_HANDLE);
		
		if (!handle.write)
			throw new DokanOperationException(net.decasdev.dokan.WinError.ERROR_ACCESS_DENIED);
		
		try {
			fileSystem.write(handle, buffer, offset);
		} catch (DriveFullException e) {
			throw new DokanOperationException(net.decasdev.dokan.WinError.ERROR_DISK_FULL);
		} catch (PartIsLockedException e) {
			throw new DokanOperationException(net.decasdev.dokan.WinError.ERROR_INVALID_ACCESS);
		} catch (Exception e)
		{
			e.printStackTrace();
		}
		
		return buffer.limit();
	}


	public void setInitial(boolean binitial) {
		this.binitial = binitial;
	}

}
