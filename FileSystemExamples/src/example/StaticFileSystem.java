package example;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

import jfilesyslib.FileSystem;
import jfilesyslib.data.DirectoryInfo;
import jfilesyslib.data.EntityInfo;
import jfilesyslib.data.FileHandle;
import jfilesyslib.data.FileInfo;
import jfilesyslib.exceptions.AccessDeniedException;
import jfilesyslib.exceptions.DestinationAlreadyExistsException;
import jfilesyslib.exceptions.DriveFullException;
import jfilesyslib.exceptions.NotADirectoryException;
import jfilesyslib.exceptions.NotAFileException;
import jfilesyslib.exceptions.PartIsLockedException;
import jfilesyslib.exceptions.PathNotFoundException;

/**
 * A static (read only) demo file system.
 * @author Marc Miltenberger
 */
public class StaticFileSystem extends FileSystem {
	private static File[] staticFiles = new File[] {
		new File("/Hello World.txt", "This is just a simple test file system.".getBytes()),
		
		//The sound file:
//		http://www.freesound.org/people/th_sounds/sounds/105237/
		new File("/Music Box.ogg", readByteArray(StaticFileSystem.class.getResourceAsStream("MusicBox.ogg")))
		

	};
	
	
	static class File {
		String name;
		byte[] content;
		
		public File(String name, byte[] content)
		{
			this.name = name;
			this.content = content;
		}
		
		public FileInfo generateFileInfo() 
		{
			return new FileInfo(name, content.length);
		}
	}
	
	private static byte[] readByteArray(InputStream inputstream)
	{
		try
		{
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			byte[] buffer = new byte[1024];
			while (true)
			{
				int read = inputstream.read(buffer);
				if (read <= 0)
					break;
				output.write(buffer, 0, read);
			}
			output.close();
			return output.toByteArray();
		} catch (IOException ex) {
			ex.printStackTrace();
			return new byte[0];
		}
	}
	
	private static File getFile(String path) throws PathNotFoundException
	{
		for (File file : staticFiles)
			if (file.name.equals(path))
				return file;
		throw new PathNotFoundException(path);
	}

	@Override
	public void close(FileHandle handle) throws DriveFullException {
	}

	@Override
	public void createDirectory(String path) throws PathNotFoundException,
			DestinationAlreadyExistsException, AccessDeniedException {
		if (path.equals("/"))
			throw new DestinationAlreadyExistsException();
		
		throw new AccessDeniedException();
	}

	@Override
	public void createFile(String path) throws PathNotFoundException,
			DestinationAlreadyExistsException, AccessDeniedException {
		throw new AccessDeniedException();
	}

	@Override
	public void deleteDirectoryRecursively(String directory)
			throws PathNotFoundException, AccessDeniedException {
		throw new AccessDeniedException();
	}

	@Override
	public void deleteFile(String file) throws PathNotFoundException,
			AccessDeniedException {
		throw new AccessDeniedException();
	}

	@Override
	public void flush(FileHandle handle) throws DriveFullException {
	}

	@Override
	public int getBlockSize() {
		return 1024 * 4;
	}

	@Override
	public EntityInfo getFileMetaData(String path) throws PathNotFoundException {
		if (path.equals("/"))
			return new DirectoryInfo("/");
		File f = getFile(path);
		return f.generateFileInfo();
	}

	@Override
	public String getFileSystemName() {
		return "StaticFs";
	}

	@Override
	public long getFreeBlockAvailableCount() {
		return 0;
	}

	@Override
	public long getFreeBlockCount() {
		return 0;
	}

	@Override
	public long getTotalBlockCount() {
		return 0;
	}

	@Override
	public String getVolumeName() {
		return "Static file system";
	}

	@Override
	public boolean isCaseSensitive() {
		return true;
	}

	@Override
	public Iterable<EntityInfo> listDirectory(String path) throws NotADirectoryException,
			PathNotFoundException, AccessDeniedException {
		List<EntityInfo> files = new LinkedList<EntityInfo>();
		
		if (!path.equals("/"))
			throw new PathNotFoundException(path);

		for (File file : staticFiles)
			files.add(file.generateFileInfo());
		
		return files;
	}

	@Override
	public FileHandle openFile(String file, boolean read, boolean write)
			throws PathNotFoundException, AccessDeniedException,
			NotAFileException {
		
		if (file.equals("/"))
			throw new NotAFileException();
		
		File f = getFile(file);
		
		if (write)
			throw new AccessDeniedException();

		FileHandle handle = new FileHandle(file);
		handle.setObjHandle(f);
		return handle;
	}

	@Override
	public int read(FileHandle handle, ByteBuffer buffer, long offset) {
		File file = (File) handle.getObjHandle();
		
		int limit = buffer.limit();
		if (file.content.length - offset < limit)
			limit = file.content.length - (int)offset;
		
		buffer.put(file.content, (int)offset, limit);
		return limit;
	}

	@Override
	public void rename(String source, String destination)
			throws PathNotFoundException, DestinationAlreadyExistsException,
			AccessDeniedException {
		throw new AccessDeniedException();
	}

	@Override
	public void setCreationTime(String path, long ctime)
			throws PathNotFoundException, AccessDeniedException {
		throw new AccessDeniedException();
	}

	@Override
	public void setLastAccessTime(String path, long atime)
			throws PathNotFoundException, AccessDeniedException {
		throw new AccessDeniedException();
	}

	@Override
	public void setLastModificationTime(String path, long mtime)
			throws PathNotFoundException, AccessDeniedException {
		throw new AccessDeniedException();
	}

	@Override
	public void setLength(FileHandle handle, long length)
			throws DriveFullException {
	}

	@Override
	public void write(FileHandle handle, ByteBuffer buffer, long offset)
			throws DriveFullException, PartIsLockedException {
	}


}
