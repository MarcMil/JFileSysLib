package jfilesyslib;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import jfilesyslib.data.ExtendedAttribute;



/**
 * In order to work successfully with Finder, we need to pretend that there are some extended attributes available and existing by default...
 * @author Marc Miltenberger
 */
class MacOSXHack implements Iterable<ExtendedAttribute> {
	private Iterable<ExtendedAttribute> inner;


	private static List<String> appleExtendedAttributesHack = Arrays.asList("com.apple.ResourceFork", "com.apple.quarantine");
	
	public MacOSXHack(Iterable<ExtendedAttribute> inner) {
		this.inner = inner;
	}

	@Override
	public Iterator<ExtendedAttribute> iterator() {
		return new Iterator<ExtendedAttribute>() {
			private List<String> hasNotSeen = new LinkedList<String>(appleExtendedAttributesHack);
			Iterator<ExtendedAttribute> use = inner.iterator();
			private boolean useNext;
			
			@Override
			public boolean hasNext() {
				useNext = use.hasNext();
				if (useNext)
					return true;
				if (hasNotSeen.size() == 0)
					return false;
				return true;
			}

			@Override
			public ExtendedAttribute next() {
				if (useNext)
				{
					ExtendedAttribute attr = use.next();
					hasNotSeen.remove(attr.getName());
					return attr;
				}
				ExtendedAttribute attr = new ExtendedAttribute(hasNotSeen.get(0), new byte[0]);
				hasNotSeen.remove(0);
				return attr;
			}

			@Override
			public void remove() {				
			}
			
		};
	}

	public static boolean isHackAttributeName(String name) {
		return appleExtendedAttributesHack.contains(name);
	}

}
