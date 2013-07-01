package jfilesyslib.filesystems;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import jfilesyslib.FileSystem;
import jfilesyslib.data.DirectoryInfo;
import jfilesyslib.data.EntityInfo;
import jfilesyslib.data.FileHandle;
import jfilesyslib.data.FileInfo;
import jfilesyslib.exceptions.AccessDeniedException;
import jfilesyslib.exceptions.DestinationAlreadyExistsException;
import jfilesyslib.exceptions.NotADirectoryException;
import jfilesyslib.exceptions.NotAFileException;
import jfilesyslib.exceptions.PathNotFoundException;
import jfilesyslib.utils.DateUtils;
import jfilesyslib.utils.MemoryStream;


/**
 * A temporary (virtual) file system.
 * @author Marc Miltenberger
 */
public class MemoryFs extends FileSystem {
	Directory root = new Directory("/");
	
	class InternalFileHandle {
		boolean read;
		boolean write;
		MemoryStream stream;
		
		
		public InternalFileHandle(File file, boolean read, boolean write) {
			this.file = file;
			this.read = read;
			this.write = write;
			file.setLastAccessTime(DateUtils.getNow());
			open();
		}
		
		private void open()
		{
			stream = new MemoryStream(file.content);
			try {
				stream.seek(0);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		
		public void close() {
			if (stream != null)
			{
				try {
					if (write)
					{
						file.setLastModificationTime(DateUtils.getNow());
						int l = stream.getLength();
						file.content = stream.getChunkList();
						file.setFileSize(l);
					}
					stream.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		File file;


		public void setLength(long length) {
			if (stream == null)
				return;
			
			/*close();
			file.setFileSize(length);
			byte[] newFileContent = new byte[(int)length];
			System.arraycopy(file.content, 0, newFileContent, 0, (int)length);
			file.content = newFileContent;
			open();*/
			try {
				stream.setLength(length);
				file.setFileSize(length);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	
	class File extends FileInfo {
		public List<MemoryStream.Chunk> content = new ArrayList<MemoryStream.Chunk>();
		Directory parentDirectory;

		public File(String fullPath, long fileSize) {
			super(fullPath, fileSize);
		}


		public Directory getParentDirectory()
		{
			return parentDirectory;
		}
		
		public void setParentDirectory(Directory dir)
		{
			parentDirectory = dir;
		}
	}
	
	class Directory extends DirectoryInfo {
		Directory parentDirectory;
		List<Directory> subdirs = new LinkedList<Directory>();
		List<File> files = new LinkedList<File>();

		public Directory(String fullPath) {
			super(fullPath);
		}
		
		public Directory getParentDirectory()
		{
			return parentDirectory;
		}

		public void setParentDirectory(Directory dir)
		{
			parentDirectory = dir;
		}
		
		public Directory findSubdir(String subdirName)
		{
			for (Directory dir : subdirs)
			{
				if (dir.getFileName().equals(subdirName))
					return dir;
			}
			return null;
		}
		
		public File findFile(String file)
		{
			for (File seek : files)
			{
				if (seek.getFileName().equals(file))
					return seek;
			}
			return null;
		}
		
		public boolean containsSubItem(String name)
		{
			if (findSubdir(name) != null || findFile(name) != null)
				return true;
			else
				return false;
		}
		
		public List<EntityInfo> combine() {
			List<EntityInfo> result = new LinkedList<EntityInfo>();
			result.addAll(files);
			result.addAll(subdirs);
			return result;
		}

		public void removeSubItem(EntityInfo info) {
			Directory dir = findSubdir(info.getFileName());
			if (dir != null)
				subdirs.remove(dir);
			File file = findFile(info.getFileName());
			if (file != null)
				files.remove(file);
		}

		public void addSubItem(EntityInfo info) {
			if (Directory.class.isInstance(info))
			{
				Directory dir = (Directory)info;
				subdirs.add(dir);
				dir.parentDirectory = this;
			}
			
			if (File.class.isInstance(info))
			{
				File file = (File)info;
				files.add(file);
				file.parentDirectory = this;
			}
			
		}
	}
	
	private Directory findParent(EntityInfo info) {
		if (Directory.class.isInstance(info))
			return ((Directory)info).getParentDirectory();
		if (File.class.isInstance(info))
			return ((File)info).getParentDirectory();
		return null;
	}

	
	private Directory findParent(String path) throws PathNotFoundException {

		if (path.equals("/"))
			return root;
		String[] splitted = path.split("/");
		Directory currentDir = root;
		for (int i = 1; i < splitted.length - 1; i++)
		{
			currentDir = currentDir.findSubdir(splitted[i]);
			if (currentDir == null)
				throw new PathNotFoundException(path);
		}
		return currentDir;
	}
	
	private EntityInfo find(String path) throws PathNotFoundException {

		if (path.equals("/"))
			return root;
		String[] splitted = path.split("/");
		Directory parent = (Directory)findParent(path);
		Directory test = parent.findSubdir(splitted[splitted.length - 1]);
		if (test != null)
			return test;
		return parent.findFile(splitted[splitted.length - 1]);
	}
	
	
	@Override
	public Iterable<EntityInfo> listDirectory(String path)
			throws NotADirectoryException, PathNotFoundException {
		
		EntityInfo info = find(path);
		if (info == null)
			throw new PathNotFoundException(path);
		if (!Directory.class.isInstance(info))
			throw new NotADirectoryException();
		return ((Directory)info).combine();
	}

	@Override
	public EntityInfo getFileMetaData(String path) throws PathNotFoundException {
		EntityInfo info = find(path);
		if (info == null)
			throw new PathNotFoundException(path);
		return info;
	}

	@Override
	public void rename(String from, String to) throws PathNotFoundException,
			DestinationAlreadyExistsException, AccessDeniedException {

		Directory dirParentFrom = findParent(from);
		Directory dirParentTo = findParent(to);
		
		EntityInfo info = find(from);
		
		if (info == null)
			throw new PathNotFoundException(from);
		

		
		if (dirParentTo.containsSubItem(to.substring(to.lastIndexOf('/') + 1)))
			throw new DestinationAlreadyExistsException();
		
		if (dirParentFrom != dirParentTo)
		{
			dirParentFrom.removeSubItem(info);
			dirParentTo.addSubItem(info);
		}
		info.setFullPath(to);
	}

	@Override
	public FileHandle openFile(String path, boolean read, boolean write)
			throws PathNotFoundException, AccessDeniedException,
			NotAFileException {
		
		EntityInfo info = find(path);
		if (info == null)
			throw new PathNotFoundException(path);

		if (!File.class.isInstance(info))
			throw new NotAFileException();
		
		return new FileHandle(path, new InternalFileHandle((File) info, read, write));
	}

	@Override
	public void createFile(String path) throws PathNotFoundException,
			DestinationAlreadyExistsException {

		EntityInfo seek = find(path);
		
		if (seek != null)
			throw new DestinationAlreadyExistsException();

		EntityInfo parent = findParent(path);
		Directory dir = (Directory)parent;
		File newFile = new File(path, 0);
		newFile.setCreationTime(DateUtils.getNow());
		newFile.setLastAccessTime(newFile.getCreationTime());
		newFile.setLastModificationTime(newFile.getCreationTime());
		dir.files.add(newFile);
		newFile.setParentDirectory(dir);
		
	}

	@Override
	public void createDirectory(String path) throws PathNotFoundException,
			DestinationAlreadyExistsException {
		if (path.equals("/"))
			throw new DestinationAlreadyExistsException();
		Directory dir = findParent(path);
		if (dir.containsSubItem(path.substring(path.lastIndexOf('/') + 1)))
			throw new DestinationAlreadyExistsException();
		Directory newDir = new Directory(path);
		newDir.setCreationTime(DateUtils.getNow());
		newDir.setLastAccessTime(newDir.getCreationTime());
		newDir.setLastModificationTime(newDir.getCreationTime());
		dir.subdirs.add(newDir);
		newDir.setParentDirectory(dir);
	}

	@Override
	public int read(FileHandle fh, ByteBuffer buffer, long offset) {

		InternalFileHandle Handle = (InternalFileHandle) fh.getObjHandle();
		if (Handle.stream != null)
		{
			try {
				byte[] bytbuffer = new byte[buffer.limit()];
				Handle.stream.seek(offset);
				int read = Handle.stream.read(bytbuffer);
				buffer.put(bytbuffer, 0, read);
				return read;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return 0;
	}

	@Override
	public void setLength(FileHandle fh, long length) {
		InternalFileHandle Handle = (InternalFileHandle) fh.getObjHandle();
		if (Handle.stream != null)
		{
			Handle.setLength(length);
		} 		
	}

	@Override
	public void write(FileHandle fh, ByteBuffer buffer, long offset) {

		InternalFileHandle Handle = (InternalFileHandle) fh.getObjHandle();
		if (Handle.stream != null)
		{
			try {
				byte[] r = new byte[buffer.limit()];
				buffer.get(r);
				if (offset > Handle.stream.getLength())
					Handle.stream.setLength(offset);
				Handle.stream.seek(offset);
				Handle.stream.write(r);
				Handle.file.setFileSize(Handle.stream.getLength());
			} catch (IOException e) {
				e.printStackTrace();
			}
		} 		
	}

	@Override
	public void flush(FileHandle fh) {
		InternalFileHandle Handle = (InternalFileHandle) fh.getObjHandle();
		if (Handle.stream != null)
		{
			Handle.stream.flush();
		}
	}

	@Override
	public void close(FileHandle fh) {
		InternalFileHandle Handle = (InternalFileHandle) fh.getObjHandle();
		Handle.close();
	}

	@Override
	public void deleteFile(String file) throws PathNotFoundException {
		EntityInfo info = find(file);
		if (info == null)
			throw new PathNotFoundException(file);
		this.findParent(info).files.remove(info);
	}

	@Override
	public void deleteDirectoryRecursively(String directory)
			throws PathNotFoundException {
		EntityInfo info = find(directory);
		if (info == null)
			throw new PathNotFoundException(directory);
		this.findParent(info).subdirs.remove(info);
	}

	@Override
	public String getVolumeName() {
		return "RAM-Disk";
	}

	@Override
	public String getFileSystemName() {
		return "Memory Fs";
	}


	@Override
	public void setLastAccessTime(String path, long atime)
			throws PathNotFoundException {
		EntityInfo info = find(path);
		if (info == null)
			throw new PathNotFoundException(path);
		info.setLastAccessTime(atime);
	}


	@Override
	public void setLastModificationTime(String path, long mtime)
			throws PathNotFoundException {
		EntityInfo info = find(path);
		if (info == null)
			throw new PathNotFoundException(path);
		info.setLastModificationTime(mtime);
	}


	@Override
	public void setCreationTime(String path, long creationTime)
			throws PathNotFoundException {
		EntityInfo info = find(path);
		if (info == null)
			throw new PathNotFoundException(path);
		info.setCreationTime(creationTime);
	}


	@Override
	public boolean isCaseSensitive() {
		return true;
	}


	public int getBlockSize() {
		return MemoryStream.DEFAULTCHUNKSIZE;
	}
	
	@Override
	public long getTotalBlockCount() {
		long l = Runtime.getRuntime().totalMemory();
		if (l == Long.MAX_VALUE)
			l = Runtime.getRuntime().maxMemory();
		return (int) (l / getBlockSize());
	}

	@Override
	public long getFreeBlockCount() {
		long l = (Runtime.getRuntime().freeMemory() / getBlockSize());
		long b = getTotalBlockCount();
		if (l < b)
			return b;
		else
			return l;
	}
	
	@Override
	public long getFreeBlockAvailableCount() {
		return getFreeBlockCount();
	}
}
