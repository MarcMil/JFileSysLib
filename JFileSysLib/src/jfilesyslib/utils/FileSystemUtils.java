package jfilesyslib.utils;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import jfilesyslib.FileSystem;
import jfilesyslib.data.EntityInfo;
import jfilesyslib.data.FileHandle;
import jfilesyslib.data.FileInfo;
import jfilesyslib.exceptions.AccessDeniedException;
import jfilesyslib.exceptions.DestinationAlreadyExistsException;
import jfilesyslib.exceptions.DriveFullException;
import jfilesyslib.exceptions.NotAFileException;
import jfilesyslib.exceptions.PartIsLockedException;
import jfilesyslib.exceptions.PathNotFoundException;


/**
 * Contains some file system utilities to write/read files directly on the file system
 * @author Marc Miltenberger
 */
public class FileSystemUtils {
	private FileSystemUtils() {
	}
	
	/**
	 * Performs a byte array comparison.
	 * @param byt1 the first byte array
	 * @param byt2 the second byte array
	 * @return true iff the byte arrays are equal
	 */
	public static boolean simpleByteArrayCompare(byte[] byt1, byte[] byt2)
	{
		if (byt1.length != byt2.length)
			return false;
		for (int i = 0; i < byt2.length; i++)
			if (byt1[i] != byt2[i])
				return false;
		return true;
	}
	
	/**
	 * Reads a text file fully into memory and splits the lines.
	 * @param filesystem the file system
	 * @param file the file
	 * @param defaultReturn the default return value
	 * @return the lines or the default return value if an error ocurred
	 */
	public static String[] readLines(FileSystem filesystem, String file, String[] defaultReturn)
	{
		try {
			return readWholeText(filesystem, file).split("\n");
		} catch (Exception e) {
			return defaultReturn;
		}
	}

	/**
	 * Reads a text file fully into memory and splits the lines.
	 * @param filesystem the file system
	 * @param file the file
	 * @return the lines
	 * @throws PathNotFoundException the path was not found
	 * @throws AccessDeniedException the access is denied
	 * @throws NotAFileException the given path is not a file
	 */
	public static String[] readLines(FileSystem filesystem, String file) throws PathNotFoundException, AccessDeniedException, NotAFileException
	{
		return readWholeText(filesystem, file).split("\n");
	}

	/**
	 * Reads a text file fully into memory.
	 * @param filesystem the file system
	 * @param file the file
	 * @return the content
	 * @throws PathNotFoundException the path was not found
	 * @throws AccessDeniedException the access is denied
	 * @throws NotAFileException the given path is not a file
	 */
	public static String readWholeText(FileSystem filesystem, String file) throws PathNotFoundException, AccessDeniedException, NotAFileException
	{
		try {
			return new String(readWhole(filesystem, file), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return "";
		}
	}
	/**
	 * Reads a binary file fully into memory.
	 * @param filesystem the file system
	 * @param file the file
	 * @return the content
	 * @throws PathNotFoundException the path was not found
	 * @throws AccessDeniedException the access is denied
	 * @throws NotAFileException the given path is not a file
	 */
	public static byte[] readWhole(FileSystem filesystem, String file) throws PathNotFoundException, AccessDeniedException, NotAFileException
	{
		EntityInfo info = filesystem.getFileMetaData(file);
		FileHandle handle = filesystem.openFile(file, true, false);

		FileInfo fileInfo = (FileInfo)info;
		byte[] content = new byte[(int)fileInfo.getFileSize()];
		filesystem.read(handle, ByteBuffer.wrap(content), 0);
		try {
			filesystem.close(handle);
		} catch (DriveFullException e) {
			System.err.println("The file " + file + " has been opened read only...");
			e.printStackTrace();
		}
		return content;
	}
	
	/**
	 * Writes the lines into a text file.
	 * @param filesystem the file system
	 * @param file the file
	 * @param content the content (lines)
	 * @throws PathNotFoundException the path was not found
	 * @throws AccessDeniedException the access is denied
	 * @throws NotAFileException the given path is not a file
	 * @throws DriveFullException there is no more space
	 */
	public static void writeLines(FileSystem filesystem, String file, String[] content) throws PathNotFoundException, AccessDeniedException, NotAFileException, DriveFullException
	{
		StringBuilder builder = new StringBuilder();
		for (String line : content)
		{
			builder.append(line);
			builder.append("\n");
		}
		writeWholeText(filesystem, file, builder.toString());
	}
	
	/**
	 * Writes the text into a text file.
	 * @param filesystem the file system
	 * @param file the file
	 * @param content the content
	 * @throws PathNotFoundException the path was not found
	 * @throws AccessDeniedException the access is denied
	 * @throws NotAFileException the given path is not a file
	 * @throws DriveFullException there is no more space
	 */
	public static void writeWholeText(FileSystem filesystem, String file, String content) throws PathNotFoundException, AccessDeniedException, NotAFileException, DriveFullException
	{
		try {
			writeWhole(filesystem, file, content.getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Writes the content into a file.
	 * @param filesystem the file system
	 * @param file the file
	 * @param content the content
	 * @throws PathNotFoundException the path was not found
	 * @throws AccessDeniedException the access is denied
	 * @throws NotAFileException the given path is not a file
	 * @throws DriveFullException there is no more space
	 */
	public static void writeWhole(FileSystem filesystem, String file, byte[] content) throws PathNotFoundException, AccessDeniedException, NotAFileException, DriveFullException
	{
		if (filesystem.pathExists(file))
			filesystem.delete(file);
		
		try {
			filesystem.createFile(file);
		} catch (DestinationAlreadyExistsException e) {
			e.printStackTrace();
		}
		FileHandle handle = filesystem.openFile(file, false, true);
		try {
			filesystem.write(handle, ByteBuffer.wrap(content), 0);
		} catch (PartIsLockedException e) {
			e.printStackTrace();
		}
		filesystem.close(handle);
	}
}
