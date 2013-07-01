package jfilesyslib.filesystems;

import java.util.Iterator;

import jfilesyslib.FileSystem;
import jfilesyslib.data.EntityInfo;
import jfilesyslib.data.SymbolicLinkInfo;
import jfilesyslib.exceptions.AccessDeniedException;
import jfilesyslib.exceptions.NotAFileException;
import jfilesyslib.exceptions.PathNotFoundException;
import jfilesyslib.utils.FileSystemUtils;


class FilterSymlinks implements Iterable<EntityInfo> {
	private Iterable<EntityInfo> inner;
	private FileSystem fileSystem, attributeFs;
	
	public FilterSymlinks(Iterable<EntityInfo> inner, FileSystem fileSystem, FileSystem attributeFs) {
		this.inner = inner;
		this.fileSystem = fileSystem;
		this.attributeFs = attributeFs;
	}

	@Override
	public Iterator<EntityInfo> iterator() {
		return new Iterator<EntityInfo>() {
			Iterator<EntityInfo> use = inner.iterator();
			
			@Override
			public boolean hasNext() {
				return use.hasNext();
			}

			@Override
			public EntityInfo next() {
				EntityInfo nxt = use.next();
				if (attributeFs.pathExists(nxt.getFullPath() + ExtendedSupportFs.hiddenSymLinkMarker))
				{
					String dest = "";
					try {
						dest = FileSystemUtils.readWholeText(fileSystem, nxt.getFullPath());
					} catch (PathNotFoundException e) {
						e.printStackTrace();
					} catch (AccessDeniedException e) {
						e.printStackTrace();
					} catch (NotAFileException e) {
						e.printStackTrace();
					}
					SymbolicLinkInfo symlink = new SymbolicLinkInfo(nxt.getFullPath(), dest);
					return symlink;
				}
				return nxt;
			}

			@Override
			public void remove() {				
			}
			
		};
	}

}
