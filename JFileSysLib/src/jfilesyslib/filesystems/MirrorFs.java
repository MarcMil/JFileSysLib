package jfilesyslib.filesystems;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import jfilesyslib.Environment;
import jfilesyslib.FileSystem;
import jfilesyslib.data.DirectoryInfo;
import jfilesyslib.data.EntityInfo;
import jfilesyslib.data.FileHandle;
import jfilesyslib.data.FileInfo;
import jfilesyslib.data.UnixPermissions;
import jfilesyslib.exceptions.AccessDeniedException;
import jfilesyslib.exceptions.DestinationAlreadyExistsException;
import jfilesyslib.exceptions.NotADirectoryException;
import jfilesyslib.exceptions.NotAFileException;
import jfilesyslib.exceptions.PathNotFoundException;


/**
 * Mirrors a directory.
 * @author Marc Miltenberger
 */
public class MirrorFs extends FileSystem {
	private File baseRoot;
	private boolean readOnly = false;
	
	/**
	 * Creates a new instance of the mirror file system
	 * @param baseRoot the mirrored root directory
	 */
	public MirrorFs(File baseRoot)
	{
		if (!baseRoot.isDirectory())
			throw new IllegalArgumentException("baseRoot " + baseRoot + " should be a directory");
		this.baseRoot = baseRoot;
	}
	
	/**
	 * Creates a new instance of the mirror file system
	 * @param baseRoot the mirrored root directory
	 * @param readOnly whether the mirrored file system should be read only
	 */
	public MirrorFs(File baseRoot, boolean readOnly)
	{
		if (!baseRoot.isDirectory())
			throw new IllegalArgumentException("baseRoot " + baseRoot + " should be a directory");
		this.baseRoot = baseRoot;
		this.readOnly = readOnly;
	}
	
	private File getFile(String path)
	{
		return new File(baseRoot, path.replace('\\', '/'));
	}
	
	@Override
	public Iterable<EntityInfo> listDirectory(String path)
			throws NotADirectoryException, PathNotFoundException {
		File dir = getFile(path);
		if (!dir.exists())
			throw new PathNotFoundException(path);
		if (!dir.isDirectory())
			throw new NotADirectoryException();
		
		File[] files = dir.listFiles();
		List<EntityInfo> infos;
		if (files == null)
		{
			return new ArrayList<EntityInfo>();
		}
		else
			infos = new ArrayList<EntityInfo>(files.length);
		for (File file : files)
		{
			if (file.isDirectory())
				infos.add(new DirectoryInfo(translateFilePath(file)));

			if (file.isFile())
				infos.add(new FileInfo(translateFilePath(file), file.length()));
		}
		return infos;
	}

	private String translateFilePath(File file) {
		String path = file.getAbsolutePath().replace(baseRoot.getAbsolutePath(), "");
		if (!path.startsWith("/"))
			path = "/" + path;
		
		return path.replace('\\', '/');
	}

	@Override
	public EntityInfo getFileMetaData(String path) throws PathNotFoundException {
		File file = getFile(path);
		if (!file.exists())
			throw new PathNotFoundException(path);
		
		EntityInfo info = null;
		if (file.isDirectory())
			info = new DirectoryInfo(translateFilePath(file));

		if (file.isFile())
			info = new FileInfo(translateFilePath(file), file.length());
		
		if (info != null)
		{
			info.setLastModificationTime(file.lastModified());
		}
		return info;
	}

	@Override
	public void rename(String from, String to) throws PathNotFoundException, AccessDeniedException {
		File src = new File(baseRoot, from);
		File dest = new File(baseRoot, to);
		
		if (!src.exists())
			throw new PathNotFoundException(from);
		
		if (readOnly)
			throw new AccessDeniedException();
		src.renameTo(dest);
	}

	@Override
	public FileHandle openFile(String path, boolean read, boolean write)
			throws PathNotFoundException, AccessDeniedException, NotAFileException {
		File open = getFile(path);
		if (!open.exists())
			throw new PathNotFoundException(path);
		if (!open.isFile())
			throw new NotAFileException();
		
		if (write && readOnly)
			throw new AccessDeniedException();
		
		FileHandle handle = new FileHandle(path);
		String mode = "r";
		/*if (read)
			mode = "r";*/
		if (write)
			mode += "w";
		try {
			handle.setObjHandle(new RandomAccessFile(open, mode));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return handle;
	}

	@Override
	public int read(FileHandle fh, ByteBuffer buffer, long offset) {
		RandomAccessFile stream = (RandomAccessFile) fh.getObjHandle();
		try {
			stream.seek(offset);
		} catch (IOException e) {
			e.printStackTrace();
			return 0;
		}
		try {
			byte[] r = new byte[buffer.limit()];
			int read = stream.read(r);
			buffer.put(r, 0, read);
			return read;
		} catch (IOException e) {
			e.printStackTrace();
			return 0;
		}
	}

	@Override
	public void flush(FileHandle fh) {
	}

	@Override
	public void close(FileHandle fh) {
		RandomAccessFile stream = (RandomAccessFile) fh.getObjHandle();
		try {
			stream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void write(FileHandle fh, ByteBuffer buffer, long offset) {
		RandomAccessFile stream = (RandomAccessFile) fh.getObjHandle();
		try {
			stream.seek(offset);
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			byte[] b = new byte[buffer.limit()];
			buffer.get(b);
			stream.write(b);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void createFile(String path) {
		try {
			getFile(path).createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void createDirectory(String path) throws DestinationAlreadyExistsException {
		if (getFile(path).exists())
			throw new DestinationAlreadyExistsException();
		getFile(path).mkdir();
	}

	@Override
	public void deleteFile(String file) throws PathNotFoundException, AccessDeniedException {
		File del = new File(baseRoot, file);
		if (!del.exists())
			throw new PathNotFoundException(file);
		del.delete();
	}

	@Override
	public void deleteDirectoryRecursively(String directory) {
		File del = new File(baseRoot, directory);
		try {
			delete(del);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	void delete(File f) throws IOException {
		if (f.isDirectory()) {
			for (File c : f.listFiles())
				delete(c);
		}
		if (!f.delete())
			throw new FileNotFoundException("Failed to delete file: " + f);
	}

	@Override
	public String getVolumeName() {
		return "Mirror - " + baseRoot.getAbsolutePath();
	}

	@Override
	public String getFileSystemName() {
		return "MirrorFs";
	}

	@Override
	public void setLength(FileHandle fh, long length) {
		RandomAccessFile stream = (RandomAccessFile) fh.getObjHandle();
		try {
			stream.setLength(length);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void setLastAccessTime(String path, long atime)
			throws PathNotFoundException {
		File open = getFile(path);
		if (!open.exists())
			throw new PathNotFoundException(path);
		//FileTime fileTime = FileTime.fromMillis(atime * 1000);
		//Files.setAttribute(path, "lastAccessTime", fileTime);
	}

	@Override
	public void setLastModificationTime(String path, long mtime)
			throws PathNotFoundException {
		File open = getFile(path);
		if (!open.exists())
			throw new PathNotFoundException(path);
	}

	@Override
	public void setCreationTime(String path, long creationTime)
			throws PathNotFoundException {
		File open = getFile(path);
		if (!open.exists())
			throw new PathNotFoundException(path);
	}

	@Override
	public boolean isCaseSensitive() {
		// We don't know...
		return true;
	}

	@Override
	public int getBlockSize() {
		return 1024 * 64;
	}

	@Override
	public long getTotalBlockCount() {
		return  (baseRoot.getTotalSpace() / getBlockSize());
	}

	@Override
	public long getFreeBlockAvailableCount() {
		return (baseRoot.getFreeSpace() / getBlockSize());
	}

	@Override
	public long getFreeBlockCount() {
		return getFreeBlockAvailableCount();
	}

	@Override
	public UnixPermissions getUnixPermissions(String path) throws PathNotFoundException {
		File f = getFile(path);
		return new UnixPermissions(f.canRead(), f.canWrite(), f.canExecute(), f.canRead(), f.canWrite(), f.canExecute(), f.canRead(), f.canWrite(), f.canExecute(), false, false, false, Environment.getUserId(), Environment.getGroupId());
	}
}
