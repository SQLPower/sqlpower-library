package ca.sqlpower.util;

import java.util.*;

/**
 * An object cache that employs the Least Recently Used disposal
 * policy.  The maxMembers tuning parameter is treated as a hard
 * limit.
 *
 * @author Jonathan Fuerth
 * @version $Id$
 */
public class LeastRecentlyUsedCache extends AbstractCache implements java.io.Serializable {

	public LeastRecentlyUsedCache(int initialMaxMembers) {
		super(initialMaxMembers);
	}
	
	/**
	 * Tracks the cache contents in order of use, most recent first.
	 * For example, if item <code>A</code> has been used since item
	 * <code>B</code>, the index of <code>A</code> in
	 * <code>useOrder</code> will be less than the index of item
	 * <code>B</code> in <code>useOrder</code>.
	 */
	private LinkedList useOrder=new LinkedList();
	
	/**
	 * If maxItems is exceeded by n, n &gt; 0, the n least-recently
	 * used items in the cache will be evicted.
	 */
	public void itemsInserted(Object[] keys) {
		useOrder.addAll(0, Arrays.asList(keys));
		while (size() > maxMembers) {
			Object evictKey = useOrder.getLast();
			useOrder.removeLast();
			remove(evictKey);
		}
	}

	/**
	 * Moves the requested item's key to the front of the
	 * <code>useOrder</code> list.
	 */
	public void itemRequested(Object key, boolean wasPresent) {
		if(wasPresent) {
			int itemIndex=useOrder.indexOf(key);
			if (itemIndex == -1) {
				throw new IllegalStateException("LRUCache.itemRequested: key '"
												+key+"' was not found in useList");
			}
			useOrder.remove(itemIndex);
			useOrder.addFirst(key);
		}
	}
}
