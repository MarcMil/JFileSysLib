package jfilesyslib.filesystems;

import java.util.Iterator;

import jfilesyslib.data.EntityInfo;


class CombineIterables implements Iterable<EntityInfo> {
	private Iterable<EntityInfo> first;
	private Iterable<EntityInfo> second;
	
	public CombineIterables(Iterable<EntityInfo> first, Iterable<EntityInfo> second) {
		this.first = first;
		this.second = second;
	}

	@Override
	public Iterator<EntityInfo> iterator() {
		return new Iterator<EntityInfo>() {
			Iterator<EntityInfo> useFirst = first.iterator();
			Iterator<EntityInfo> useSecond = second.iterator();
			boolean isFirst = true;
			
			@Override
			public boolean hasNext() {
				if (isFirst)
				{
					if (!useFirst.hasNext())
					{
						isFirst = false;
						return useSecond.hasNext();
					}
					return useFirst.hasNext();
				} else
					return useSecond.hasNext();
			}

			@Override
			public EntityInfo next() {
				if (isFirst)
					return useFirst.next();
				else
					return useSecond.next();
			}

			@Override
			public void remove() {				
			}
			
		};
	}

}
