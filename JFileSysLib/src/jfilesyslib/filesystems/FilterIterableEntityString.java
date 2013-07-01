package jfilesyslib.filesystems;

import java.util.Iterator;

import jfilesyslib.data.EntityInfo;


class FilterIterableEntityString implements Iterable<EntityInfo> {
	private String exclude;
	private Iterable<EntityInfo> inner;
	
	public FilterIterableEntityString(Iterable<EntityInfo> inner, String exclude) {
		this.inner = inner;
		this.exclude = exclude;
	}

	@Override
	public Iterator<EntityInfo> iterator() {
		return new Iterator<EntityInfo>() {
			Iterator<EntityInfo> use = inner.iterator();
			private EntityInfo nextElement;
			
			@Override
			public boolean hasNext() {
				EntityInfo info;
				while (use.hasNext())
				{
					info = use.next();
					if (!info.getFullPath().contains(exclude))
					{
						nextElement = info;
						return true;
					}
				}
				return false;
			}

			@Override
			public EntityInfo next() {
				if (nextElement == null)
				{
					if (!hasNext())
						throw new RuntimeException("Iterator used wrong");
				}
				EntityInfo nxt = nextElement;
				nextElement = null;
				return nxt;
			}

			@Override
			public void remove() {				
			}
			
		};
	}

}
