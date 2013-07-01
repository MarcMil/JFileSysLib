package jfilesyslib;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;

import jfilesyslib.data.DirectoryInfo;
import jfilesyslib.data.EntityInfo;
import jfilesyslib.data.ExtendedAttribute;
import jfilesyslib.data.FileHandle;
import jfilesyslib.data.FileInfo;
import jfilesyslib.data.SymbolicLinkInfo;
import jfilesyslib.data.UnixPermissions;
import jfilesyslib.exceptions.AccessDeniedException;
import jfilesyslib.exceptions.AttributeNotFoundException;
import jfilesyslib.exceptions.DestinationAlreadyExistsException;
import jfilesyslib.exceptions.DriveFullException;
import jfilesyslib.exceptions.NotADirectoryException;
import jfilesyslib.exceptions.NotAFileException;
import jfilesyslib.exceptions.PartIsLockedException;
import jfilesyslib.exceptions.PathNotFoundException;
import jfilesyslib.exceptions.SourceAlreadyExistsException;
import jfilesyslib.exceptions.UnsupportedFeatureException;

import fuse.Errno;
import fuse.Filesystem3;
import fuse.FilesystemConstants;
import fuse.FuseDirFiller;
import fuse.FuseException;
import fuse.FuseFtypeConstants;
import fuse.FuseGetattrSetter;
import fuse.FuseOpenSetter;
import fuse.FuseSizeSetter;
import fuse.FuseStatfsSetter;
import fuse.XattrLister;
import fuse.XattrSupport;

class FuseWrapper implements Filesystem3, XattrSupport {
	private FileSystem fileSystem;

	private boolean binitial = true;

	public static final String INITIALFILENAME = "/.INITIALFILENAME____$"; 
	private MountOptions options;
	
	
	public FuseWrapper(FileSystem fileSystem, MountOptions options) {
		this.fileSystem = fileSystem;
		this.options = options;
	}

	@Override
	public int getattr(String path, FuseGetattrSetter getattrSetter) throws FuseException {

		try {
			if (binitial)
			{
				if (path.equals(INITIALFILENAME))
				{
					getattrSetter.set(5, FuseFtypeConstants.TYPE_FILE, 1, 0, 0, 0, 0, 0,
							0, 0, 0);
					return 0;
				}
			}
			EntityInfo entity = fileSystem.getFileMetaData(path);
			if (entity == null)
				System.err.println("Your file system returned null for getFileMetaData(" + path + ")");
			
			UnixPermissions perms = fileSystem.getUnixPermissions(path);
			if ((path.equals("/") && options.isUnixOwnerImpersonateNewFiles()) || options.isUnixOwnerImpersonateAllFiles())
			{
				perms.setUid(Environment.getUserId());
				perms.setGid(Environment.getGroupId());
			}

			int inode = entity.hashCode();
			int nlink = 1;
			int uid = perms.getUid();
			int gid = perms.getGid();
			int rdev = 0;

			if (FileInfo.class.isInstance(entity))
			{
				FileInfo info = (FileInfo)entity;
				int BLOCK_SIZE = fileSystem.getBlockSize();
				long blocksizeFile = (info.getFileSize() + BLOCK_SIZE - 1) / BLOCK_SIZE;
				getattrSetter.set(inode, FuseFtypeConstants.TYPE_FILE | perms.getPermissions(), nlink, uid, gid, rdev, info.getFileSize(), blocksizeFile,
						(int)info.getLastAccessTime(), (int)info.getLastModificationTime(), (int)info.getCreationTime());
			} else if (DirectoryInfo.class.isInstance(entity))
			{
				DirectoryInfo info = (DirectoryInfo)entity;
				int BLOCK_SIZE = fileSystem.getBlockSize();
				int filesInDir = fileSystem.getNumberOfFilesInDirectory(info);
				long dirsizeBlock = filesInDir * fileSystem.getMaxPathLength();
				getattrSetter.set(inode, FuseFtypeConstants.TYPE_DIR | perms.getPermissions(), nlink, uid, gid, rdev, dirsizeBlock, (dirsizeBlock + BLOCK_SIZE - 1) / BLOCK_SIZE,
						(int)info.getLastAccessTime(), (int)info.getLastModificationTime(), (int)info.getCreationTime());
				
			} else if (SymbolicLinkInfo.class.isInstance(entity))
			{
				SymbolicLinkInfo info = (SymbolicLinkInfo)entity;
				getattrSetter.set(inode, FuseFtypeConstants.TYPE_SYMLINK | perms.getPermissions(), nlink, uid, gid, rdev, 0, 0,
						(int)info.getLastAccessTime(), (int)info.getLastModificationTime(), (int)info.getCreationTime());
				
			}
			return 0;
		} catch (PathNotFoundException e) {
			return Errno.ENOENT;
		}
	}
	

	@Override
	public int readlink(String path, CharBuffer link) throws FuseException {
		EntityInfo info;
		try {
			info = fileSystem.getFileMetaData(path);
		} catch (PathNotFoundException e) {
			return Errno.ENOENT;
		}
		if (SymbolicLinkInfo.class.isInstance(info))
		{
			link.append(((SymbolicLinkInfo)info).destination);
		}
		return 0;
	}

	@Override
	public int getdir(String path, FuseDirFiller dirFiller) throws FuseException {
		Iterable<EntityInfo> iterator;
		try {
			iterator = fileSystem.listDirectory(path);
			if (iterator == null)
				System.err.println("Your file system returned null for readDirectory(" + path + ")");
		} catch (PathNotFoundException e) {
			return Errno.ENOENT;
		} catch (AccessDeniedException e) {
			return Errno.EACCES;
		} catch (NotADirectoryException e) {
			return Errno.ENOTDIR;
		}
        dirFiller.add(".", 0, 0);
        dirFiller.add("..", 0, 0);
		for (EntityInfo info : iterator)
		{
			UnixPermissions perms;
			try {
				perms = fileSystem.getUnixPermissions(info.getFullPath());
			} catch (PathNotFoundException e) {
				System.err.println("readDirectory and getUnixPermissions inconsistent for " + info.getFullPath());
				e.printStackTrace();
				continue;
			}
			int mode = 0;
			
			if (FileInfo.class.isInstance(info))
				mode |= FuseFtypeConstants.TYPE_FILE;
			
			if (DirectoryInfo.class.isInstance(info))
				mode |= FuseFtypeConstants.TYPE_DIR; 
			
			if (SymbolicLinkInfo.class.isInstance(info))
			{
//				SymbolicLinkInfo sym = (SymbolicLinkInfo)info;
				mode |= FuseFtypeConstants.TYPE_SYMLINK;
			}
			
			mode |= perms.getPermissions();
			
            dirFiller.add(info.getFileName(), info.hashCode(), mode);
		}
		return 0;
	}

	@Override
	public int mknod(String path, int mode, int rdev) throws FuseException {
		try {
			fileSystem.createFile(path);
			UnixPermissions perms = new UnixPermissions(mode);
			if (options.isUnixOwnerImpersonateNewFiles() || options.isUnixOwnerImpersonateAllFiles())
			{
				perms.setUid(Environment.getUserId());
				perms.setGid(Environment.getGroupId());
			}
			fileSystem.setUnixPermissions(path, perms);
		} catch (PathNotFoundException e) {
			return Errno.ENOTDIR;
		} catch (AccessDeniedException e) {
			return Errno.EACCES;
		} catch (DestinationAlreadyExistsException e) {
			return Errno.EEXIST;
		} catch (UnsupportedFeatureException e) {
		}
		return 0;
	}

	@Override
	public int mkdir(String path, int mode) throws FuseException {
		try {
			fileSystem.createDirectory(path);
			UnixPermissions perms = new UnixPermissions(mode);
			if (options.isUnixOwnerImpersonateNewFiles())
			{
				perms.setUid(Environment.getUserId());
				perms.setGid(Environment.getGroupId());
			}
			fileSystem.setUnixPermissions(path, perms);
		} catch (AccessDeniedException e) {
			return Errno.EACCES;
		} catch (PathNotFoundException e) {
			return Errno.ENOTDIR;
		} catch (DestinationAlreadyExistsException e) {
			return Errno.EEXIST;
		} catch (UnsupportedFeatureException e) {
		}
		return 0;
	}

	@Override
	public int unlink(String path) throws FuseException {
		if (fileSystem.isReadOnly())
			return Errno.EROFS;
		try {
			if (DirectoryInfo.class.isInstance(fileSystem.getFileMetaData(path)))
				return rmdir(path);
		} catch (PathNotFoundException e) {
			return Errno.ENOENT;
		}
		try {
			fileSystem.deleteFile(path);
		} catch (AccessDeniedException e) {
			return Errno.EACCES;
		} catch (PathNotFoundException e) {
			return Errno.EINVAL;
		}
		return 0;
	}

	@Override
	public int rmdir(String path) throws FuseException {
		if (fileSystem.isReadOnly())
			return Errno.EROFS;
		try {
			fileSystem.deleteDirectoryRecursively(path);
		} catch (AccessDeniedException e) {
			return Errno.EACCES;
		} catch (PathNotFoundException e) {
			return Errno.EINVAL;
		}
		return 0;
	}

	@Override
	public int symlink(String from, String to) throws FuseException {
		try {
			if (fileSystem.isReadOnly())
				return Errno.EROFS;

			/*if (!from.startsWith("/"))
				from = "/" + from;*/
			if (!to.startsWith("/"))
				to = "/" + to;
			fileSystem.createSymbolicLink(to, from);
		} catch (PathNotFoundException e) {
			return Errno.ENOENT;
		} catch (SourceAlreadyExistsException e) {
			return Errno.EEXIST;
		} catch (AccessDeniedException e) {
			return Errno.EACCES;
		} catch (UnsupportedFeatureException e) {
			return Errno.ENOSYS;
		}
		return 0;
	}

	@Override
	public int rename(String from, String to) throws FuseException {
		try {
			if (fileSystem.isReadOnly())
				return Errno.EROFS;
			fileSystem.rename(from, to);
		} catch (PathNotFoundException e) {
			return Errno.ENOENT;
		} catch (DestinationAlreadyExistsException e) {
			return Errno.EEXIST;
		} catch (AccessDeniedException e) {
			return Errno.EACCES;
		}
		return 0;
	}

	@Override
	public int link(String from, String to) throws FuseException {
		try {
			if (fileSystem.isReadOnly())
				return Errno.EROFS;

			if (!from.startsWith("/"))
				from = "/" + from;
			if (!to.startsWith("/"))
				to = "/" + to;
			fileSystem.createHardLink(to, from);
		} catch (PathNotFoundException e) {
			return Errno.ENOENT;
		} catch (SourceAlreadyExistsException e) {
			return Errno.EEXIST;
		} catch (AccessDeniedException e) {
			return Errno.EACCES;
		} catch (UnsupportedFeatureException e) {
			return Errno.ENOSYS;
		}
		return 0;
	}

	@Override
	public int chmod(String path, int mode) throws FuseException {
		
		try {
			UnixPermissions unixPerms = fileSystem.getUnixPermissions(path);
			unixPerms.setPermissions(mode);
			fileSystem.setUnixPermissions(path, unixPerms);

		} catch (PathNotFoundException e) {
			return Errno.ENOENT;
		} catch (AccessDeniedException e) {
			return Errno.EACCES;
		} catch (UnsupportedFeatureException e) {
			return Errno.ENOSYS;
		}
		return 0;
	}

	@Override
	public int chown(String path, int uid, int gid) throws FuseException {
		try {
			UnixPermissions perms = fileSystem.getUnixPermissions(path);
			perms.setUid(uid);
			perms.setGid(gid);
			fileSystem.setUnixPermissions(path, perms);
		} catch (PathNotFoundException e) {
			return Errno.ENOENT;
		} catch (AccessDeniedException e) {
			return Errno.EACCES;
		} catch (UnsupportedFeatureException e) {
			return Errno.ENOSYS;
		}
		return 0;
	}

	@Override
	public int truncate(String path, long size) throws FuseException {
		try {
			if (fileSystem.isReadOnly())
				return Errno.EROFS;
			FileHandle handle = fileSystem.openFile(path, true, true);
			fileSystem.setLength(handle, size);
			fileSystem.close(handle);
		} catch (PathNotFoundException e) {
			return Errno.ENOENT;
		} catch (AccessDeniedException e) {
			return Errno.EACCES;
		} catch (NotAFileException e) {
			return Errno.EISDIR;
		} catch (DriveFullException e) {
			return Errno.ENOSPC;
		}
		return 0;
	}

	@Override
	public int utime(String path, int atime, int mtime) throws FuseException {
		try {
			if (fileSystem.isReadOnly())
				return Errno.EROFS;
			fileSystem.setLastAccessTime(path, atime);
			fileSystem.setLastModificationTime(path, mtime);
		} catch (AccessDeniedException e) {
			return Errno.EACCES;
		} catch (PathNotFoundException e) {
			return Errno.ENOENT;
		}
		return 0;
	}

	@Override
	public int statfs(FuseStatfsSetter statfsSetter) throws FuseException {
        statfsSetter.set(fileSystem.getBlockSize(), (int)fileSystem.getTotalBlockCount(), (int)fileSystem.getFreeBlockCount(), (int)fileSystem.getFreeBlockAvailableCount(), fileSystem.getTotalFilesCount(), fileSystem.getFilesFreeCount(), fileSystem.getMaxPathLength());
        return 0;
	}

	@Override
	public int open(String path, int flags, FuseOpenSetter openSetter)
			throws FuseException {
		try {
			
			EntityInfo entity = fileSystem.getFileMetaData(path);
			if (entity == null)
				System.err.println("Your file system returned null for getFileMetaData(" + path + ")");

			boolean read = false;
			boolean write = false;
			if ((flags & FilesystemConstants.O_RDONLY) == FilesystemConstants.O_RDONLY)
				read = true;
			
			if ((flags & FilesystemConstants.O_WRONLY) == FilesystemConstants.O_WRONLY)
				write = true;
			
			if ((flags & FilesystemConstants.O_RDWR) == FilesystemConstants.O_RDWR)
			{
				read = true;
				write = true;
			}
			if (write && fileSystem.isReadOnly())
				return Errno.EROFS;
			
			FileHandle handle = fileSystem.openFile(path, read, write);
			handle.read = read;
			handle.write = write;
			openSetter.setFh(handle);
			handle.isEmptyFile = (((FileInfo)entity).getFileSize() == 0); 
			//openSetter.setFh(new FileHandle(node));
		} catch (PathNotFoundException e) {
			return Errno.ENOENT;
		} catch (AccessDeniedException e) {
			return Errno.EACCES;
		} catch (NotAFileException e) {
			return Errno.EISDIR;
		}
		return 0;
	}

	@Override
	public int read(String path, Object fh, ByteBuffer buf, long offset) throws FuseException {
		FileHandle handle = (FileHandle)fh;
		//a workaround (for a bug?)
		if (handle == null)
			return Errno.EBADSLT;
		if (handle.hasClosed)
			return Errno.EBADSLT;
		if (handle.isEmptyFile)
			return 0;
		if (!handle.read)
			return Errno.EACCES;
		/*int read = */fileSystem.read(handle, buf, offset);
		return 0;
	}

	@Override
	public int write(String path, Object fh, boolean isWritepage,
			ByteBuffer buf, long offset) throws FuseException {
		FileHandle handle = (FileHandle)fh;
		if (handle == null)
			return Errno.EBADSLT;
		if (handle.hasClosed)
			return Errno.EBADSLT;
		if (!handle.write)
			return Errno.EACCES;
		handle.isEmptyFile = false;
		try {
			fileSystem.write(handle, buf, offset);
		} catch (DriveFullException e) {
			return Errno.ENOSPC;
		} catch (PartIsLockedException e) {
			return Errno.EACCES;
		}
		return 0;
	}

	@Override
	public int flush(String path, Object fh) throws FuseException {
		FileHandle handle = (FileHandle)fh;
		if (handle == null)
			return Errno.EBADSLT;
		if (handle.hasClosed)
			return Errno.EBADSLT;
		
		//well... shouldn't happen, but it does happen...
		if (!handle.write)
			return 0;
			//return Errno.EACCES;
		try {
			fileSystem.flush(handle);
		} catch (DriveFullException e) {
			return Errno.ENOSPC;
		}
		return 0;
	}

	@Override
	public int release(String path, Object fh, int flags) throws FuseException {
		FileHandle handle = (FileHandle)fh;
		if (handle == null)
			return Errno.EBADSLT;
		if (handle.hasClosed)
			return 0;
		handle.hasClosed = true;
		try {
			fileSystem.close(handle);
		} catch (DriveFullException e) {
			return Errno.ENOSPC;
		}
		return 0;
	}

	@Override
	public int fsync(String path, Object fh, boolean isDatasync)
			throws FuseException {
		return 0;
	}


	public void setInitial(boolean binitial) {
		this.binitial = binitial;
	}

	@Override
	public int getxattrsize(String path, String name, FuseSizeSetter sizeSetter)
			throws FuseException {
		try {
			ExtendedAttribute attribute = fileSystem.getExtendedAttribute(path, name);
			sizeSetter.setSize(attribute.getContent().length);
			return 0;
		} catch (PathNotFoundException e) {
			return Errno.ENOENT;
		} catch (AccessDeniedException e) {
			return Errno.EACCES;
		} catch (UnsupportedFeatureException e) {
			//Sadly, we are not supposed to return "Not supported"
			if (Environment.isMac())
				return Errno.ENOTSUPP;
			return 0;
		} catch (AttributeNotFoundException e) {
			if (Environment.isMac() && MacOSXHack.isHackAttributeName(name))
				return 0;
			return Errno.ENOATTR;
		}
	}

	@Override
	public int getxattr(String path, String name, ByteBuffer dst, int position)
			throws FuseException, BufferOverflowException {
		try {
			ExtendedAttribute attribute = fileSystem.getExtendedAttribute(path, name);
			position = 0;
			dst.put(attribute.getContent(), position, attribute.getContent().length - position);
			return 0;
		} catch (PathNotFoundException e) {
			return Errno.ENOENT;
		} catch (AccessDeniedException e) {
			return Errno.EACCES;
		} catch (UnsupportedFeatureException e) {
			//Sadly, we are not supposed to return "Not supported"
			if (Environment.isMac())
				return Errno.ENOTSUPP;
			return 0;
		} catch (AttributeNotFoundException e) {
			if (Environment.isMac() && MacOSXHack.isHackAttributeName(name))
				return 0;
				
			return Errno.ENOATTR;
		}
	}

	@Override
	public int listxattr(String path, XattrLister lister) throws FuseException {
		try {
			Iterable<ExtendedAttribute> attributes = fileSystem.listExtendedAttributes(path);

			if (Environment.isMac())
				attributes = new MacOSXHack(attributes);
			
			for (ExtendedAttribute attribute : attributes)
				lister.add(attribute.getName());
						
		} catch (PathNotFoundException e) {
			return Errno.ENOENT;
		} catch (AccessDeniedException e) {
			return Errno.EACCES;
		} catch (UnsupportedFeatureException e) {
			if (Environment.isMac())
				return Errno.ENOTSUPP;
			//Sadly, we are not supposed to return "Not supported"
			return 0;
		}
		return 0;
	}

	@Override
	public int setxattr(String path, String name, ByteBuffer value, int flags,
			int position) throws FuseException {
		ExtendedAttribute attribute = null;
		if (flags == XATTR_CREATE)
		{
			try {
				fileSystem.getExtendedAttribute(path, name);
				return Errno.EEXIST;
			} catch (PathNotFoundException e) {
			} catch (AccessDeniedException e) {
			} catch (UnsupportedFeatureException e) {
			} catch (AttributeNotFoundException e) {
			}
		}
		if (flags == XATTR_REPLACE)
		{
			try {
				attribute = fileSystem.getExtendedAttribute(path, name);
			} catch (PathNotFoundException e) {
			} catch (AccessDeniedException e) {
			} catch (UnsupportedFeatureException e) {
			} catch (AttributeNotFoundException e) {
				return Errno.ENOATTR;
			}
		}
		if (attribute == null)
		{
			attribute = new ExtendedAttribute();
			attribute.setName(name);
		}
		byte[] r = new byte[value.limit()];
		value.get(r);
		if (position + r.length > attribute.getContent().length)
		{
			byte[] old = attribute.getContent();
			byte[] newContent = new byte[position + r.length];
			System.arraycopy(old, 0, newContent, 0, old.length);
			attribute.setContent(newContent);
		}
		System.arraycopy(r, 0, attribute.getContent(), position, r.length);
		try {
			fileSystem.setExtendedAttribute(path, attribute);
		} catch (PathNotFoundException e) {
			return Errno.ENOENT;
		} catch (AccessDeniedException e) {
			return Errno.EACCES;
		} catch (UnsupportedFeatureException e) {
			if (Environment.isMac())
				return Errno.ENOTSUPP;
			//Sadly, we are not supposed to return "Not supported"
			
			return 0;
		}
		return 0;
	}

	@Override
	public int removexattr(String path, String name) throws FuseException {
		try {
			fileSystem.removeExtendedAttribute(path, name);
		} catch (PathNotFoundException e) {
			return Errno.ENOENT;
		} catch (AccessDeniedException e) {
			return Errno.EACCES;
		} catch (UnsupportedFeatureException e) {
			if (Environment.isMac())
				return Errno.ENOTSUPP;
			//Sadly, we are not supposed to return "Not supported"
			return 0;
		} catch (AttributeNotFoundException e) {
			return Errno.ENOATTR;
		}
		return 0;
	}
}
