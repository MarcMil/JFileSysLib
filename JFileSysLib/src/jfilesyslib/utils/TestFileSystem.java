package jfilesyslib.utils;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.Random;

import jfilesyslib.FileSystem;
import jfilesyslib.Mounter;
import jfilesyslib.data.DirectoryInfo;
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
import jfilesyslib.exceptions.NoDriveLetterLeftException;
import jfilesyslib.exceptions.NotADirectoryException;
import jfilesyslib.exceptions.NotAFileException;
import jfilesyslib.exceptions.PartIsLockedException;
import jfilesyslib.exceptions.PathNotFoundException;
import jfilesyslib.exceptions.SourceAlreadyExistsException;
import jfilesyslib.exceptions.TestFailedException;
import jfilesyslib.exceptions.UnsupportedFeatureException;


/**
 * Performs some basic tests on the file system.
 * It may be used to find some problems in your file system.
 *  
 * @author Marc Miltenberger
 */
public  class TestFileSystem {

	static void byteArrayCompare(byte[] expected, byte[] actual) throws IOException
	{
		if (expected.length != actual.length)
			throw new IOException("Expected length: " + expected.length + ", actual length: " + actual.length);
		for (int i = 0; i < actual.length; i++)
			if (expected[i] != actual[i])
				throw new IOException("Expected at index " + i + ": " + expected[i] + ", actual: " + actual[i]);
	}
	
	/**
	 * Performs some tests when the file system is mounted
	 * @param mountedPath the path
	 * @throws TestFailedException 
	 */
	public static void testFileSystemMounted(String mountedPath) throws TestFailedException
	{
		try
		{
			File dir = new File(mountedPath, "Test directory");
			if ((!dir.exists() && !dir.isDirectory()) && !dir.mkdir())
				throw new TestFailedException("Creating directory " + dir.getAbsolutePath() + " failed");
			
			File testWrite = new File(dir, "TestFile");
			File testExp = File.createTempFile("Test", "Test");
			
			
			byte[] resExpected = performTest(testExp);
			byte[] resActual = performTest(testWrite);
			byteArrayCompare(resExpected, resActual);
			testExp.delete();
			testWrite.delete();
			dir.delete();
		} catch (IOException ex)
		{
			throw new TestFailedException(ex);
		}
	}
	
	private static byte[] performTest(File write) throws IOException
	{
		Random r = new Random(33);
		byte[] byt = new byte[1024 * 1024 * 4];
		r.nextBytes(byt);
		RandomAccessFile file = new RandomAccessFile(write, "rw");
		for (int i = 0; i < 10000; i++)
		{
			byte[] cwrite = new byte[r.nextInt(200)];
			r.nextBytes(cwrite);
			file.seek(r.nextInt(byt.length));
			file.write(cwrite);
		}
		file.seek(0);
		file.close();
		file = new RandomAccessFile(write, "r");
		byte[] b = new byte[(int) file.length()];
		file.read(b);
		file.close();
		return b;
		
	}
	
	/**
	 * Tests the file system.<br>
	 * It does not have to be mounted.<br>
	 * If isReadOnly is true, some tests may not be performed.
	 * 
	 * @param fileSystem the file system to test
	 * @param isReadOnly whether the file system is read only
	 */
	public static void testFileSystemUnmounted(FileSystem fileSystem, boolean isReadOnly) throws TestFailedException
	{
		byte[] write;
		try {
			write = "I am testing your file system. :)".getBytes("UTF-8");
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
			return;
		}
		try {
			fileSystem.listDirectory("/");
			EntityInfo info = fileSystem.getFileMetaData("/");
			if (info == null)
				throw new TestFailedException("getFileMetaData(\"/\") returns null");
			if (!DirectoryInfo.class.isInstance(info))
				throw new TestFailedException("/ should be an directory, not " + info.getClass().toString());
			DirectoryInfo dirinfo = (DirectoryInfo)info;
			if (!dirinfo.getFullPath().equals("/"))
				throw new TestFailedException("/ should have the full path \"/\", not " + dirinfo.getFullPath());
			Iterable<EntityInfo> cinfo = fileSystem.listDirectory("/");
			if (cinfo == null)
				throw new TestFailedException("readDirectory(\"/\") returns null");
			
			try {
				fileSystem.openFile("/", true, true);
				throw new TestFailedException("The file system should throw a NotAFileException on openFile(\"/\")");
			} catch (NotAFileException e) {
			}
		} catch (NotADirectoryException e) {
			throw new TestFailedException("The file system should not throw this exception on /", e);
		} catch (PathNotFoundException e) {
			throw new TestFailedException("The file system should not throw this exception on /", e);
		} catch (AccessDeniedException e) {
			throw new TestFailedException("The file system should not throw this exception on /", e);
		}
		
		try {
			fileSystem.createDirectory("/");
			throw new TestFailedException("createDirectory(\"/\") - the file system should throw a DestinationAlreadyExistsException");
			
		} catch (DestinationAlreadyExistsException e) {
		} catch (PathNotFoundException e) {
			throw new TestFailedException("createDirectory(\"/\") - the file system should throw a DestinationAlreadyExistsException", e);
		} catch (AccessDeniedException e) {
			throw new TestFailedException("createDirectory(\"/\") - the file system should throw a DestinationAlreadyExistsException", e);
		}
		
		if (!isReadOnly)
		{
			String TESTFILENAME = "/AFKpfkofkoFKOaortfeujiwqroujt";
			String TESTFILENAME_RENAMED = "/FDAKFDA/SUBDIR/subdirRenamed";
			String TESTFILENAME_RENAMED2 = "/DDSAJIDSAIDD/SUBDIR/subdirRenamed";
			String TESTDIR = "/DDSAJIDSAIDD";
			String TESTDIR_RENAMED = "/FDAKFDA";
			
			String TESTDIR_SUBDIR = "/DDSAJIDSAIDD/SUBDIR";
			
			
			//cleaning up...
			if (fileSystem.pathExists(TESTFILENAME))
			{
				try {
					fileSystem.delete(TESTFILENAME);
				} catch (PathNotFoundException e) {
					throw new TestFailedException("Well... pathExists(" + TESTFILENAME + ") returns true, but delete throws the following exception: ", e);
				} catch (AccessDeniedException e) {
					throw new TestFailedException(e);
				}
			}
			
			checkAndDeleteFile(fileSystem, TESTFILENAME_RENAMED);
			checkAndDeleteFile(fileSystem, TESTFILENAME_RENAMED2);

			checkAndDeleteDir(fileSystem, TESTDIR);
			checkAndDeleteDir(fileSystem, TESTDIR_RENAMED);
			
			
			//Create file
			
			try {
				fileSystem.createFile(TESTFILENAME);
				if (!fileSystem.pathExists(TESTFILENAME))
					throw new TestFailedException("I created " + TESTFILENAME + ", but pathExists says it does not exist");
			} catch (PathNotFoundException e) {
				throw new TestFailedException("Root should exist :)", e);
			} catch (DestinationAlreadyExistsException e) {
				throw new TestFailedException("I have checked before. The file " + TESTFILENAME + " should NOT exist", e);
			} catch (AccessDeniedException e) {
				throw new TestFailedException(e);
			}
			FileHandle handle;
			try {
				handle = fileSystem.openFile(TESTFILENAME, false, true);
			} catch (PathNotFoundException e) {
				throw new TestFailedException(e);
			} catch (AccessDeniedException e) {
				throw new TestFailedException("You have claimed that the file system is not read only", e);
			} catch (NotAFileException e) {
				throw new TestFailedException(TESTFILENAME + " should be a file", e);
			}
			try {
				fileSystem.write(handle, ByteBuffer.wrap(write), 0);
				fileSystem.flush(handle);
			} catch (Exception e) {
				throw new TestFailedException(e);
			}
			try {
				EntityInfo info = fileSystem.getFileMetaData(TESTFILENAME);
				if (info == null)
					throw new TestFailedException(TESTFILENAME + " should exist, but getFileMetaData returns null");
				if (!FileInfo.class.isInstance(info))
					throw new TestFailedException(TESTFILENAME + " should be a file");
				FileInfo finfo = (FileInfo)info;
				if (finfo.getFileSize() != write.length)
					throw new TestFailedException("I have written " + write.length + " bytes in " + TESTFILENAME + ", but according to the metadata the file length is " + finfo.getFileSize());
			} catch (PathNotFoundException e) {
				throw new TestFailedException(TESTFILENAME + " should exist.", e);
			}
			try {
				fileSystem.close(handle);
			} catch (DriveFullException e) {
				throw new TestFailedException(e);
			}
			
			readFilename(fileSystem, TESTFILENAME, write);

			createDirectory(fileSystem, TESTDIR);
			
			try {
				fileSystem.createDirectory(TESTDIR);
				throw new TestFailedException("createDirectory(\"" + TESTDIR + "\") - the file system should throw a DestinationAlreadyExistsException");
				
			} catch (DestinationAlreadyExistsException e) {
			} catch (PathNotFoundException e) {
				throw new TestFailedException("createDirectory(\"" + TESTDIR + "\") - the file system should throw a DestinationAlreadyExistsException", e);
			} catch (AccessDeniedException e) {
			throw new TestFailedException(e);
			}
			createDirectory(fileSystem, TESTDIR_SUBDIR);
			
			//currently exists:
			//TESTDIR
			//TESTDIR_SUBDIR

			testRename(fileSystem, TESTDIR, TESTDIR_RENAMED);
			testRename(fileSystem, TESTFILENAME, TESTFILENAME_RENAMED);

			readFilename(fileSystem, TESTFILENAME_RENAMED, write);
			
			testRename(fileSystem, TESTDIR_RENAMED, TESTDIR);
			testRename(fileSystem, TESTFILENAME_RENAMED2, TESTFILENAME);

			
			checkAndDeleteDir(fileSystem, TESTDIR);
			
			readFilename(fileSystem, TESTFILENAME, write);
			
			byte[] partly = new byte[6];
			System.arraycopy(write, 0, partly, 0, partly.length);
			setLength(fileSystem, TESTFILENAME, partly.length);
			try
			{
				readFilename(fileSystem, TESTFILENAME, partly);
			} catch (Exception e) {
				throw new TestFailedException("SetLength(\"" + TESTFILENAME + "\", 4) does not seem to work correctly", e);
			}
			
			
			//Test offset (over)writing
			try {
				handle = fileSystem.openFile(TESTFILENAME, false, true);
			} catch (PathNotFoundException e) {
				throw new TestFailedException(e);
			} catch (AccessDeniedException e) {
				throw new TestFailedException("You have claimed that the file system is not read only", e);
			} catch (NotAFileException e) {
				throw new TestFailedException(TESTFILENAME + " should be a file", e);
			}
			try {
				fileSystem.write(handle, ByteBuffer.wrap(write), 4);
				fileSystem.close(handle);
			} catch (Exception e) {
				throw new TestFailedException(e);
			}
			
			byte[] newWritten = new byte[write.length + 4];
			System.arraycopy(write, 0, newWritten, 0, write.length);
			System.arraycopy(write, 0, newWritten, 4, write.length);

			readFilename(fileSystem, TESTFILENAME, newWritten);
			
			
			
			//Test setLength

			try {
				handle = fileSystem.openFile(TESTFILENAME, false, true);
			} catch (PathNotFoundException e) {
				throw new TestFailedException(e);
			} catch (AccessDeniedException e) {
				throw new TestFailedException("You have claimed that the file system is not read only", e);
			} catch (NotAFileException e) {
				throw new TestFailedException(TESTFILENAME + " should be a file", e);
			}
			try {
				byte[] bytWriteOffset = new byte[] { 5, 6, 7, 8, 9 };
				byte[] bytWriteOffsetResult = new byte[] { 0, 0, 5, 6, 7, 8, 9 };
				fileSystem.setLength(handle, 0);
				try {
					EntityInfo info = fileSystem.getFileMetaData(TESTFILENAME);
					if (info == null)
						throw new TestFailedException(TESTFILENAME + " should exist, but getFileMetaData returns null");
					if (!FileInfo.class.isInstance(info))
						throw new TestFailedException(TESTFILENAME + " should be a file");
					FileInfo finfo = (FileInfo)info;
					if (finfo.getFileSize() != 0)
						throw new TestFailedException("I truncated " + TESTFILENAME + " to length 0, but according to the metadata the file length is " + finfo.getFileSize());
				} catch (PathNotFoundException e) {
					throw new TestFailedException(TESTFILENAME + " should exist.", e);
				}
				try
				{
					fileSystem.write(handle, ByteBuffer.wrap(bytWriteOffset), 2);
					fileSystem.close(handle);
				} catch (Exception e)
				{
					throw new TestFailedException("It seems your file system does not support write requests where the offset is larger than the file size.", e);
				}
				try {
					EntityInfo info = fileSystem.getFileMetaData(TESTFILENAME);
					if (info == null)
						throw new TestFailedException(TESTFILENAME + " should exist, but getFileMetaData returns null");
					if (!FileInfo.class.isInstance(info))
						throw new TestFailedException(TESTFILENAME + " should be a file");
					FileInfo finfo = (FileInfo)info;
					if (finfo.getFileSize() != bytWriteOffsetResult.length)
						throw new TestFailedException("I have written " + bytWriteOffsetResult.length + " bytes in " + TESTFILENAME + ", but according to the metadata the file length is " + finfo.getFileSize());
				} catch (PathNotFoundException e) {
					throw new TestFailedException(TESTFILENAME + " should exist.", e);
				}
				readFilename(fileSystem, TESTFILENAME, bytWriteOffsetResult);
			} catch (DriveFullException e) {
				throw new TestFailedException(e);
			}
			
			
			long blockCount = fileSystem.getTotalBlockCount();
			long freeBlocksAvailCount = fileSystem.getFreeBlockAvailableCount();
			long freeBlocksCount = fileSystem.getFreeBlockCount();
			
			if (blockCount <= 0)
				throw new TestFailedException("The file system returns a total block count of " + blockCount);

			if (freeBlocksAvailCount <= 0)
				throw new TestFailedException("The file system returns an available free block count of " + freeBlocksAvailCount);
			
			if (freeBlocksCount <= 0)
				throw new TestFailedException("The file system returns a free block count of " + freeBlocksCount);
			
			if (freeBlocksAvailCount > freeBlocksCount)
				throw new TestFailedException("The available free block count must not be larger than the free block count");
			
			
			checkAndDeleteFile(fileSystem, TESTFILENAME);

			
			testLargeFile(fileSystem);
		}
	}
	
	private static void testLargeFile(FileSystem fileSystem) throws TestFailedException {
		String TESTFILENAME = "/AFKpfkofkoFKOaortfeujiwqroujt";
		int FILESIZE = 1024 * 1024 * 10;
		MemoryStream ms = new MemoryStream();
		try {
			
			ms.setLength(FILESIZE);
			fileSystem.createFile(TESTFILENAME);
			FileHandle handle = fileSystem.openFile(TESTFILENAME, true, true);
			fileSystem.setLength(handle, FILESIZE);
			byte[] buffer = new byte[1024 * 512];
			Random rand = new Random(55);
			for (int i = 0; i < 2; i++)
			{
				rand.nextBytes(buffer);
				int pos = rand.nextInt(FILESIZE - buffer.length);
				fileSystem.write(handle, ByteBuffer.wrap(buffer), pos);
				ms.seek(pos);
				ms.write(buffer);
			}
			ms.close();
			byte[] res = new byte[ms.getLength()];
			fileSystem.read(handle, ByteBuffer.wrap(res), 0);
			fileSystem.close(handle);
			byteArrayCompare(ms.toArray(), res);
			fileSystem.deleteFile(TESTFILENAME);
		} catch (Exception e) {
			throw new TestFailedException(e);
		}
	}

	private static void createDirectory(FileSystem fileSystem, String path) throws TestFailedException {

		try {
			fileSystem.createDirectory(path);
			if (!fileSystem.pathExists(path))
				throw new TestFailedException("I created the directory " + path + ", but pathExists says it does not exist");
		} catch (PathNotFoundException e) {
			throw new TestFailedException("createDirectory(" + path + ")", e);
		} catch (DestinationAlreadyExistsException e) {
			throw new TestFailedException("Should not occur", e);
		} catch (AccessDeniedException e) {
			throw new TestFailedException(e);
		}
	}
	
	private static void readFilename(FileSystem fileSystem, String file, byte[] cmp) throws TestFailedException {
		FileHandle handle;
		try {
			handle = fileSystem.openFile(file, true, true);
		} catch (PathNotFoundException e) {
			throw new TestFailedException(e);
		} catch (AccessDeniedException e) {
			throw new TestFailedException("You have claimed that the file system is not read only", e);
		} catch (NotAFileException e) {
			throw new TestFailedException(file + " should be a file", e);
		}
		
		byte[] testBuffer = new byte[cmp.length];
		ByteBuffer buf = ByteBuffer.wrap(testBuffer);
		int read = fileSystem.read(handle, buf, 0);
		if (read != cmp.length)
			throw new TestFailedException("Reading of " + file + " at position 0 failed. Should read " + cmp.length + " bytes, actual: " + read);
		
		try {
			byteArrayCompare(cmp, testBuffer);
		} catch (IOException e) {
			throw new TestFailedException("Reading of " + file + " at position 0 failed", e);
		}
		byte[] testBuffer2 = new byte[cmp.length - 4];
		ByteBuffer buffer2 = ByteBuffer.wrap(testBuffer2);
		read = fileSystem.read(handle, buffer2, 4);
		if (read != cmp.length - 4)
			throw new TestFailedException("Reading of " + file + " at position 0 failed. Should read " + (cmp.length - 4) + " bytes, actual: " + read);
		byte[] cmp2 = new byte[cmp.length - 4];
		System.arraycopy(cmp, 4, cmp2, 0, cmp2.length);
		try {
			byteArrayCompare(cmp2, testBuffer2);
		} catch (IOException e) {
			throw new TestFailedException("Reading of " + file + " at position 4 failed", e);
		}
		try
		{
			fileSystem.close(handle);
		} catch (DriveFullException e) {
			throw new TestFailedException(e);
		}
	}
	

	private static void setLength(FileSystem fileSystem, String file, long length) throws TestFailedException {
		FileHandle handle;
		try {
			handle = fileSystem.openFile(file, true, true);
		} catch (PathNotFoundException e) {
			throw new TestFailedException(e);
		} catch (AccessDeniedException e) {
			throw new TestFailedException("You have claimed that the file system is not read only", e);
		} catch (NotAFileException e) {
			throw new TestFailedException(file + " should be a file", e);
		}
		
		try {
			fileSystem.setLength(handle, length);
		} catch (DriveFullException e) {
			throw new TestFailedException(e);
		}
		try
		{
			fileSystem.close(handle);
		} catch (DriveFullException e) {
			throw new TestFailedException(e);
		}
	}
	
	private static void testRename(FileSystem fileSystem, String src, String dest) throws TestFailedException {

		try {
			fileSystem.rename(src, dest);
			if (!fileSystem.pathExists(dest))
				throw new TestFailedException("Seems that renaming from " + src + " to " + dest + " did not work");
		} catch (PathNotFoundException e) {
			throw new TestFailedException("The path " + src + " should exist...", e);
		} catch (DestinationAlreadyExistsException e) {
			throw new TestFailedException("The destination should not exist", e);
		} catch (AccessDeniedException e) {
			throw new TestFailedException("You have claimed that the file system is not read only", e);
		}
	}

	private static void checkAndDeleteFile(FileSystem fileSystem, String file) throws TestFailedException {
		try {
			if (fileSystem.pathExists(file))
				fileSystem.deleteFile(file);
			
			if (fileSystem.pathExists(file))
				throw new TestFailedException("I have closed all handles, therefore I should be able to delete " + file);
		} catch (PathNotFoundException e) {
			throw new TestFailedException("I have closed all handles, therefore I should be able to delete " + file, e);
		} catch (AccessDeniedException e) {
			throw new TestFailedException(e);
		}
	}
	private static void checkAndDeleteDir(FileSystem fileSystem, String directory) throws TestFailedException {
		try {
			if (fileSystem.pathExists(directory))
				fileSystem.deleteDirectoryRecursively(directory);
			
			if (fileSystem.pathExists(directory))
				throw new TestFailedException("I deleted the directory " + directory + " recursively, but pathExists says it does exist");
		} catch (PathNotFoundException e) {
			throw new TestFailedException("deleteDirectoryRecursively(" + directory + ")", e);
		} catch (AccessDeniedException e) {
			throw new TestFailedException(e);
		}
	}
	
	/**
	 * Performs some tests on unix permissions
	 * @param fileSystem the file system
	 * @throws TestFailedException 
	 */
	public static void performUnixPermissionsTest(FileSystem fileSystem) throws TestFailedException
	{
		String TESTFILENAME = "/AFKpfkofkoFKOaortfeujiwqroujt";
		
		
		//cleaning up...
		if (fileSystem.pathExists(TESTFILENAME))
		{
			try {
				fileSystem.delete(TESTFILENAME);
			} catch (PathNotFoundException e) {
				throw new TestFailedException("Well... pathExists(" + TESTFILENAME + ") returns true, but delete throws the following exception: ", e);
			} catch (AccessDeniedException e) {
				throw new TestFailedException(e);
			}
		}
		
		
		
		//Create file
		try {
			fileSystem.createFile(TESTFILENAME);
			if (!fileSystem.pathExists(TESTFILENAME))
				throw new TestFailedException("I created " + TESTFILENAME + ", but pathExists says it does not exist");
		} catch (PathNotFoundException e) {
			throw new TestFailedException("Root should exist :)", e);
		} catch (DestinationAlreadyExistsException e) {
			throw new TestFailedException("I have checked before. The file " + TESTFILENAME + " should NOT exist", e);
		} catch (AccessDeniedException e) {
			throw new TestFailedException(e);
		}
		try
		{
			for (int b1 = 0; b1 <= 1; b1++)
			{
				for (int b2 = 0; b2 <= 1; b2++)
				{
					for (int b3 = 0; b3 <= 1; b3++)
					{
						for (int b4 = 0; b4 <= 1; b4++)
						{
							for (int b5 = 0; b5 <= 1; b5++)
							{
								for (int b6 = 0; b5 <= 1; b5++)
								{
									for (int b7 = 0; b5 <= 1; b5++)
									{
										for (int b8 = 0; b5 <= 1; b5++)
										{
											for (int b9 = 0; b5 <= 1; b5++)
											{
												for (int b10 = 0; b5 <= 1; b5++)
												{
													for (int b11 = 0; b5 <= 1; b5++)
													{
														for (int b12 = 0; b5 <= 1; b5++)
														{
															UnixPermissions perms = new UnixPermissions(getBoolean(b1), getBoolean(b2), getBoolean(b3), getBoolean(b4), getBoolean(b5), getBoolean(b6), getBoolean(b7), getBoolean(b8), getBoolean(b9), getBoolean(b10), getBoolean(b11), getBoolean(b12), b1, b2);
															fileSystem.setUnixPermissions(TESTFILENAME, perms);
															UnixPermissions gperms = fileSystem.getUnixPermissions(TESTFILENAME);
															if (!perms.equals(gperms))
															{
																throw new TestFailedException("I set the unix permissions of " + TESTFILENAME + " to " + perms + ", but it returns " + gperms);
															}
														}
							
													}
												}
											}
										}
									}
								}
							}
						}
						
					}
					
				}
				
			}
		} catch (UnsupportedFeatureException ex) {
			System.err.println("setUnixPermissions or getUnixPermissions are not supported");
		} catch (PathNotFoundException e) {
			throw new TestFailedException(e);
		} catch (AccessDeniedException e) {
			throw new TestFailedException(e);
		}
		
		try {
			fileSystem.delete(TESTFILENAME);
		} catch (PathNotFoundException e) {
			throw new TestFailedException(e);
		} catch (AccessDeniedException e) {
			throw new TestFailedException(e);
		}
	}

	/**
	 * Performs some tests on windows attributes
	 * @param fileSystem the file system
	 * @throws TestFailedException 
	 */
	public static void performWindowsAttributesTest(FileSystem fileSystem) throws TestFailedException
	{
		String TESTFILENAME = "/AFKpfkofkoFKOaortfeujiwqroujt";
		
		
		//cleaning up...
		if (fileSystem.pathExists(TESTFILENAME))
		{
			try {
				fileSystem.delete(TESTFILENAME);
			} catch (PathNotFoundException e) {
				throw new TestFailedException("Well... pathExists(" + TESTFILENAME + ") returns true, but delete throws the following exception: ", e);
			} catch (AccessDeniedException e) {
				throw new TestFailedException(e);
			}
		}
		
		
		
		//Create file
		try {
			fileSystem.createFile(TESTFILENAME);
			if (!fileSystem.pathExists(TESTFILENAME))
				throw new TestFailedException("I created " + TESTFILENAME + ", but pathExists says it does not exist");
		} catch (PathNotFoundException e) {
			throw new TestFailedException("Root should exist :)", e);
		} catch (DestinationAlreadyExistsException e) {
			throw new TestFailedException("I have checked before. The file " + TESTFILENAME + " should NOT exist", e);
		} catch (AccessDeniedException e) {
			throw new TestFailedException(e);
		}
		try
		{
			for (int b1 = 0; b1 <= 1; b1++)
			{
				for (int b2 = 0; b2 <= 1; b2++)
				{
					for (int b3 = 0; b3 <= 1; b3++)
					{
						for (int b4 = 0; b4 <= 1; b4++)
						{
							for (int b5 = 0; b5 <= 1; b5++)
							{
								for (int b6 = 0; b5 <= 1; b5++)
								{
									for (int b7 = 0; b5 <= 1; b5++)
									{
										for (int b8 = 0; b5 <= 1; b5++)
										{
											WindowsAttributes perms = new WindowsAttributes(getBoolean(b1), getBoolean(b2), getBoolean(b3), getBoolean(b4), getBoolean(b5), getBoolean(b6), getBoolean(b7), getBoolean(b8));
											fileSystem.setWindowsAttributes(TESTFILENAME, perms);
											WindowsAttributes gperms = fileSystem.getWindowsAttributes(TESTFILENAME);
											if (!perms.equals(gperms))
											{
												throw new TestFailedException("I set the windows attributes of " + TESTFILENAME + " to " + perms + ", but it returns " + gperms);
											}
										}
									}
								}
							}
						}
						
					}
					
				}
				
			}
		} catch (UnsupportedFeatureException ex) {
			System.err.println("setWindowsAttributes or getWindowsAttributes are not supported");
		} catch (PathNotFoundException e) {
			throw new TestFailedException(e);
		} catch (AccessDeniedException e) {
			throw new TestFailedException(e);
		}
		
		try {
			fileSystem.delete(TESTFILENAME);
		} catch (PathNotFoundException e) {
			throw new TestFailedException(e);
		} catch (AccessDeniedException e) {
			throw new TestFailedException(e);
		}
	}


	/**
	 * Performs some tests on extended attributes
	 * @param fileSystem the file system
	 * @throws TestFailedException 
	 */
	public static void performExtendedAttributesTest(FileSystem fileSystem) throws TestFailedException
	{
		String TESTFILENAME = "/AFKpfkofkoFKOaortfeujiwqroujt";
		
		
		//cleaning up...
		if (fileSystem.pathExists(TESTFILENAME))
		{
			try {
				fileSystem.delete(TESTFILENAME);
			} catch (PathNotFoundException e) {
				throw new TestFailedException("Well... pathExists(" + TESTFILENAME + ") returns true, but delete throws the following exception: ", e);
			} catch (AccessDeniedException e) {
				throw new TestFailedException(e);
			}
		}
		
		
		
		//Create file
		try {
			fileSystem.createFile(TESTFILENAME);
			if (!fileSystem.pathExists(TESTFILENAME))
				throw new TestFailedException("I created " + TESTFILENAME + ", but pathExists says it does not exist");
		} catch (PathNotFoundException e) {
			throw new TestFailedException("Root should exist :)", e);
		} catch (DestinationAlreadyExistsException e) {
			throw new TestFailedException("I have checked before. The file " + TESTFILENAME + " should NOT exist", e);
		} catch (AccessDeniedException e) {
			throw new TestFailedException(e);
		}
		try
		{
			byte[] test = new byte[] { 2, 5, 7, 9 };
			if (fileSystem.listExtendedAttributes(TESTFILENAME).iterator().hasNext())
				throw new TestFailedException("listExtendedAttributes(" + TESTFILENAME + ").iterator() has a next element, but it shouldn't");
			
			try {
				fileSystem.getExtendedAttribute(TESTFILENAME, "TEST");
				throw new TestFailedException(TESTFILENAME + " should not have the extended attribute TEST, but getExtendedAttribute does not throw AttributeNotFoundException.");
			} catch (AttributeNotFoundException e) {
			}
			ExtendedAttribute extSet = new ExtendedAttribute("TEST", test);
			fileSystem.setExtendedAttribute(TESTFILENAME, extSet);
			ExtendedAttribute ext = fileSystem.getExtendedAttribute(TESTFILENAME, "TEST");
			if (!ext.equals(extSet))
				throw new TestFailedException("Set extended attribute " + extSet + " of " + TESTFILENAME + ", but getExtendedAttribute returns " + ext);
			
			Iterator<ExtendedAttribute> iter = fileSystem.listExtendedAttributes(TESTFILENAME).iterator();
			if (!iter.hasNext())
				throw new TestFailedException("listExtendedAttributes(" + TESTFILENAME + ").iterator() hasn't a next element, but it should");
			
			ext = iter.next();
			
			if (iter.hasNext())
				throw new TestFailedException("listExtendedAttributes(" + TESTFILENAME + ").iterator() has a next element, but it has a second element, which it shouldn't");
			
			if (!ext.equals(extSet))
				throw new TestFailedException("Set extended attribute " + extSet + " of " + TESTFILENAME + ", but listExtendedAttribute returns " + ext);
			
			
			test = new byte[] { 6, 7, 2, 4 };
			extSet = new ExtendedAttribute("TEST", test);
			fileSystem.setExtendedAttribute(TESTFILENAME, extSet);
			ext = fileSystem.getExtendedAttribute(TESTFILENAME, "TEST");
			if (!ext.equals(extSet))
				throw new TestFailedException("Set extended attribute " + extSet + " of " + TESTFILENAME + ", but getExtendedAttribute returns " + ext);
			
			fileSystem.removeExtendedAttribute(TESTFILENAME, "TEST");
			if (fileSystem.listExtendedAttributes(TESTFILENAME).iterator().hasNext())
				throw new TestFailedException("listExtendedAttributes(" + TESTFILENAME + ").iterator() has a next element, but it shouldn't, since I deleted the attribute TEST");
			
			try
			{
				fileSystem.removeExtendedAttribute(TESTFILENAME, "TEST2");
				
				throw new TestFailedException(TESTFILENAME + " should not have the extended attribute TEST2, but removeExtendedAttribute does not throw AttributeNotFoundException.");
			} catch (AttributeNotFoundException e) {
			}
			
		} catch (UnsupportedFeatureException ex) {
			System.err.println("setExtendedAttribute, listExtendedAttribute or removeExtendedAttribute are not supported");
		} catch (PathNotFoundException e) {
			throw new TestFailedException(e);
		} catch (AccessDeniedException e) {
			throw new TestFailedException(e);
		} catch (AttributeNotFoundException e) {
			throw new TestFailedException(e);
		}
		
		try {
			fileSystem.delete(TESTFILENAME);
		} catch (PathNotFoundException e) {
			throw new TestFailedException(e);
		} catch (AccessDeniedException e) {
			throw new TestFailedException(e);
		}
	}

	/**
	 * Performs some tests on symbolic links
	 * @param fileSystem the file system
	 * @throws TestFailedException 
	 */
	public static void performSymbolicLinksTest(FileSystem fileSystem) throws TestFailedException
	{
		String TESTFILENAME = "/AFKpfkofkoFKOaortfeujiwqroujt_src";
		String TESTFILENAME2 = "/AFKpfkofkoFKOaortfeujiwqroujt_dest";
		
		
		//cleaning up...
		if (fileSystem.pathExists(TESTFILENAME))
		{
			try {
				fileSystem.delete(TESTFILENAME);
			} catch (PathNotFoundException e) {
				throw new TestFailedException("Well... pathExists(" + TESTFILENAME + ") returns true, but delete throws the following exception: ", e);
			} catch (AccessDeniedException e) {
				throw new TestFailedException(e);
			}
		}
		
		
		
		//Create file
		try {
			fileSystem.createFile(TESTFILENAME);
			if (!fileSystem.pathExists(TESTFILENAME))
				throw new TestFailedException("I created " + TESTFILENAME + ", but pathExists says it does not exist");
		} catch (PathNotFoundException e) {
			throw new TestFailedException("Root should exist :)", e);
		} catch (DestinationAlreadyExistsException e) {
			throw new TestFailedException("I have checked before. The file " + TESTFILENAME + " should NOT exist", e);
		} catch (AccessDeniedException e) {
			throw new TestFailedException(e);
		}
		try
		{
			try {
				fileSystem.createSymbolicLink(TESTFILENAME, TESTFILENAME2);
				throw new TestFailedException("Are you confusing dest and src?" + "\ncreateSymbolicLink(" + TESTFILENAME + ", " + TESTFILENAME2 + ") should throw an DestinationAlreadyExistsException, because " + TESTFILENAME + " already exists");
			} catch (SourceAlreadyExistsException e) {
			}
			fileSystem.createSymbolicLink(TESTFILENAME2, TESTFILENAME);
			EntityInfo info = fileSystem.getFileMetaData(TESTFILENAME2);
			//TO DO: test symlink
			if (!SymbolicLinkInfo.class.isInstance(info))
				throw new TestFailedException("getMetaData(" + TESTFILENAME2 + ") should return a SymbolicLinkInfo...");
			SymbolicLinkInfo symlinkInfo = (SymbolicLinkInfo)info;
			if (!symlinkInfo.destination.equals(TESTFILENAME))
				throw new TestFailedException("getMetaData(" + TESTFILENAME2 + ") returns a SymbolicLinkInfo object, which has the destination " + symlinkInfo.destination  + ". Expected: " + TESTFILENAME);
		} catch (UnsupportedFeatureException ex) {
			System.err.println("createSymbolicLink is not supported");
		} catch (PathNotFoundException e) {
			throw new TestFailedException(e);
		} catch (AccessDeniedException e) {
			throw new TestFailedException(e);
		} catch (SourceAlreadyExistsException e) {
			throw new TestFailedException(e);
		}
		try {
			if (fileSystem.pathExists(TESTFILENAME))
				fileSystem.delete(TESTFILENAME);
			if (fileSystem.pathExists(TESTFILENAME2))
				fileSystem.delete(TESTFILENAME2);
		} catch (PathNotFoundException e) {
			throw new TestFailedException(e);
		} catch (AccessDeniedException e) {
			throw new TestFailedException(e);
		}
	}


	/**
	 * Performs some tests on hard links
	 * @param fileSystem the file system
	 * @throws TestFailedException 
	 */
	public static void performHardLinksTest(FileSystem fileSystem) throws TestFailedException
	{
		String TESTFILENAME = "/AFKpfkofkoFKOaortfeujiwqroujt_src";
		String TESTFILENAMELINK = "/AFKpfkofkoFKOaortfeujiwqroujt_src2";
		String TESTFILENAME2 = "/AFKpfkofkoFKOaortfeujiwqroujt_dest";
		byte[] write = new byte[] { 5, -22, 5, 7, 9 };
		//cleaning up...
		if (fileSystem.pathExists(TESTFILENAME))
		{
			try {
				fileSystem.delete(TESTFILENAME);
			} catch (PathNotFoundException e) {
				throw new TestFailedException("Well... pathExists(" + TESTFILENAME + ") returns true, but delete throws the following exception: ", e);
			} catch (AccessDeniedException e) {
				throw new TestFailedException(e);
			}
		}
		if (fileSystem.pathExists(TESTFILENAME2))
		{
			try {
				fileSystem.delete(TESTFILENAME2);
			} catch (PathNotFoundException e) {
				throw new TestFailedException("Well... pathExists(" + TESTFILENAME + ") returns true, but delete throws the following exception: ", e);
			} catch (AccessDeniedException e) {
				throw new TestFailedException(e);
			}
		}
		if (fileSystem.pathExists(TESTFILENAMELINK))
		{
			try {
				fileSystem.delete(TESTFILENAMELINK);
			} catch (PathNotFoundException e) {
				throw new TestFailedException("Well... pathExists(" + TESTFILENAME + ") returns true, but delete throws the following exception: ", e);
			} catch (AccessDeniedException e) {
				throw new TestFailedException(e);
			}
		}
		
		
		
		//Create file
		try {
			fileSystem.createFile(TESTFILENAME);
			if (!fileSystem.pathExists(TESTFILENAME))
				throw new TestFailedException("I created " + TESTFILENAME + ", but pathExists says it does not exist");
		} catch (PathNotFoundException e) {
			throw new TestFailedException("Root should exist :)", e);
		} catch (DestinationAlreadyExistsException e) {
			throw new TestFailedException("I have checked before. The file " + TESTFILENAME + " should NOT exist", e);
		} catch (AccessDeniedException e) {
			throw new TestFailedException(e);
		}
		try
		{
			try {
				fileSystem.createHardLink(TESTFILENAME, TESTFILENAME2);
				throw new TestFailedException("Are you confusing dest and src?" + "\ncreateSymbolicLink(" + TESTFILENAME + ", " + TESTFILENAME2 + ") should throw an DestinationAlreadyExistsException, because " + TESTFILENAME + " already exists");
			} catch (SourceAlreadyExistsException e) {
			}
			fileSystem.createHardLink(TESTFILENAME2, TESTFILENAME);
			fileSystem.rename(TESTFILENAME2, TESTFILENAMELINK);
			FileHandle handle;
			try {
				handle = fileSystem.openFile(TESTFILENAMELINK, false, true);
			} catch (PathNotFoundException e) {
				throw new TestFailedException(e);
			} catch (AccessDeniedException e) {
				throw new TestFailedException("You have claimed that the file system is not read only", e);
			} catch (NotAFileException e) {
				throw new TestFailedException(TESTFILENAME + " should be a file", e);
			}
			try {
				fileSystem.write(handle, ByteBuffer.wrap(write), 0);
				fileSystem.close(handle);
			} catch (Exception e) {
				throw new TestFailedException(e);
			}

			readFilename(fileSystem, TESTFILENAMELINK, write);
			try
			{
				readFilename(fileSystem, TESTFILENAME, write);
			} catch (TestFailedException e)
			{
				throw new TestFailedException("Seems that hard linked files are not really hard linked. Content changes should be visible at once in other hard linked files.", e);
			}
			
			fileSystem.deleteFile(TESTFILENAMELINK);
			if (!fileSystem.pathExists(TESTFILENAME))
				throw new TestFailedException("Deleting of a hard linked file should not result in a delete process of the other hard linked file.");
			readFilename(fileSystem, TESTFILENAME, write);
			fileSystem.deleteFile(TESTFILENAME);
				

		} catch (UnsupportedFeatureException ex) {
			System.err.println("createHardLink is not supported");
		} catch (PathNotFoundException e) {
			throw new TestFailedException(e);
		} catch (AccessDeniedException e) {
			throw new TestFailedException(e);
		} catch (SourceAlreadyExistsException e) {
			throw new TestFailedException(e);
		} catch (DestinationAlreadyExistsException e) {
			throw new TestFailedException(e);
		}
		try {
			if (fileSystem.pathExists(TESTFILENAME))
				fileSystem.delete(TESTFILENAME);
			if (fileSystem.pathExists(TESTFILENAME2))
				fileSystem.delete(TESTFILENAME2);
		} catch (PathNotFoundException e) {
			throw new TestFailedException(e);
		} catch (AccessDeniedException e) {
			throw new TestFailedException(e);
		}
	}
	
	/**
	 * Performs some tests on windows file locking
	 * @param fileSystem the file system
	 * @throws TestFailedException 
	 */
	public static void performLockTest(FileSystem fileSystem) throws TestFailedException
	{

		String TESTFILENAME = "/AFKpfkofkoFKOaortfeujiwqroujt";
		
		
		//cleaning up...
		if (fileSystem.pathExists(TESTFILENAME))
		{
			try {
				fileSystem.delete(TESTFILENAME);
			} catch (PathNotFoundException e) {
				throw new TestFailedException("Well... pathExists(" + TESTFILENAME + ") returns true, but delete throws the following exception: ", e);
			} catch (AccessDeniedException e) {
				throw new TestFailedException(e);
			}
		}
		
		
		
		//Create file
		try {
			fileSystem.createFile(TESTFILENAME);
			if (!fileSystem.pathExists(TESTFILENAME))
				throw new TestFailedException("I created " + TESTFILENAME + ", but pathExists says it does not exist");
		} catch (PathNotFoundException e) {
			throw new TestFailedException("Root should exist :)", e);
		} catch (DestinationAlreadyExistsException e) {
			throw new TestFailedException("I have checked before. The file " + TESTFILENAME + " should NOT exist", e);
		} catch (AccessDeniedException e) {
			throw new TestFailedException(e);
		}
		FileHandle handleLock = null, handleOther = null;
		try
		{
			byte[] test = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 };
			handleLock = fileSystem.openFile(TESTFILENAME, true, true);
			fileSystem.write(handleLock, ByteBuffer.wrap(test), 0);
			handleOther = fileSystem.openFile(TESTFILENAME, true, true);
			fileSystem.lockFile(handleLock, 1, 5);
			try
			{
				fileSystem.lockFile(handleOther, 2, 8);
				throw new TestFailedException("The file system should throw an AlreadyLockedException if the file is already locked at the same part");
			} catch (AlreadyLockedException ex)
			{
			}
			try {
				fileSystem.write(handleLock, ByteBuffer.wrap(test), 0);
			} catch (PartIsLockedException e) {
				throw new TestFailedException("An PartIsLockedException should not be thrown if the handle, which locked the file, tries to access the part it locked.", e);
			}
			try
			{
				fileSystem.write(handleOther, ByteBuffer.wrap(test), 0);
				fileSystem.write(handleOther, ByteBuffer.wrap(test), 1);
				fileSystem.write(handleOther, ByteBuffer.wrap(new byte[1]), 1);
				throw new TestFailedException("An PartIsLockedException should be thrown if another handle tries to write at a locked part.");
			} catch (PartIsLockedException ex)
			{ 
			}
			fileSystem.unlockFile(handleLock, 1, 5);
		} catch (UnsupportedFeatureException ex) {
			System.err.println("lockFile or unlockFile are not supported");
		} catch (PathNotFoundException e) {
			throw new TestFailedException(e);
		} catch (AccessDeniedException e) {
			throw new TestFailedException(e);
		} catch (NotAFileException e) {
			throw new TestFailedException(e);
		} catch (DriveFullException e) {
			throw new TestFailedException(e);
		} catch (AlreadyLockedException e) {
			throw new TestFailedException(e);
		} catch (PartIsLockedException e) {
			throw new TestFailedException(e);
		}
		try {
			fileSystem.close(handleLock);
			fileSystem.close(handleOther);
		} catch (DriveFullException e1) {
			throw new TestFailedException(e1);
		}
		
		try {
			fileSystem.delete(TESTFILENAME);
		} catch (PathNotFoundException e) {
			throw new TestFailedException(e);
		} catch (AccessDeniedException e) {
			throw new TestFailedException(e);
		}
	}
	
	private static boolean getBoolean(int i)
	{
		return i == 1;
	}

	/**
	 * This is a small utillity function, which calls testFileSystemUnmounted, mounts the file system, performs the mounted file system tests and unmounts it.<br>
	 * The file system must <b>not</b> be read only!<br>
	 * @param fs the file system
	 * @throws TestFailedException 
	 */
	public static void performFullTests(FileSystem fs) throws TestFailedException {
		TestFileSystem.testFileSystemUnmounted(fs, false);
		TestFileSystem.performUnixPermissionsTest(fs);
		TestFileSystem.performWindowsAttributesTest(fs);
		TestFileSystem.performExtendedAttributesTest(fs);
		TestFileSystem.performSymbolicLinksTest(fs);
		TestFileSystem.performHardLinksTest(fs);
		TestFileSystem.performLockTest(fs);
		File mountPath;
		try {
			mountPath = Mounter.chooseMountPath(fs);
		} catch (NoDriveLetterLeftException e) {
			throw new TestFailedException("There is no drive letter left...");
		}
		Mounter.mount(fs, mountPath);
		if (!Mounter.unmount(mountPath))
			throw new TestFailedException("The file system (mounted on " + mountPath + ") could not be unmounted");
		
	}

}
