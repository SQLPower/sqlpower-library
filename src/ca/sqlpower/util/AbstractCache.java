package ca.sqlpower.util;

import java.util.*;

/**
 * The AbstractCache class is a generic cache that never disposes of
 * the items it contains.  This class is declared abstract because
 * without a disposal policy, there is no difference between a cache
 * and a map.
 * 
 * <p>This class is may not extend HashMap in the future, but it will
 * always implement Cache, which will always implement Map.
 * Therefore, it's not safe to assume an AbstractCache is a HashMap
 * but it is safe to assume an AbstractCache is a Map (or a Collection).
 */
public abstract class AbstractCache extends HashMap implements Cache, java.io.Serializable {
	
	public AbstractCache(int initialMaxMembers) {
		super();
		maxMembers=initialMaxMembers;
		lastFlushDate=new Date();
	}

	/**
	 * This field holds the maximum member count normally allowed in
	 * the cache.  Subclasses may interpret this as a hard or soft
	 * limit, and should document which interpretation they have
	 * chosen.
	 */
	protected int maxMembers;

	/**
	 * This is initialized to the current date when the cache is first
	 * created, and is updated to the current date whenever the {@link
	 * #flush()} method is called.
	 */
	protected Date lastFlushDate;

	/**
	 * Whenever an item is inserted (using <code>put</code> or
	 * <code>putAll</code>), this method will be called.  At that
	 * time, the subclass's implementation should ensure that the
	 * cache's item count does not exceed maxMembers by removing zero
	 * or more items as necessary.  The removal process is permitted
	 * to remove more items than necessary to lower the count, and in
	 * some cases (such as "soft" limit policies) the item count may
	 * remain in excess of maxMembers.  The actual policy used to
	 * decide which item to dispose of is left up to each individual
	 * subclass.
	 */
	protected abstract void itemsInserted(Object[] keys);

	/**
	 * Whenever an item is requested (via <code>get</code>), this
	 * method will be called.  Many cache eviction policies need to
	 * know when items are requsted to keep their stats accurate.
	 *
	 * @param key The key which was used in the request.
	 * @param wasPresent True if the requested key was present in the
	 * cache (a cache hit), false otherwise (a cache miss).
	 */
	protected abstract void itemRequested(Object key, boolean wasPresent);

	/**
	 * Sets the maximum member count, which influences the behaviour
	 * of <code>itemsInserted</code>.
	 *
	 * @see #maxMembers
	 */
	public void setMaxMembers(int argMaxMembers) {
		maxMembers=argMaxMembers;
	}

	/**
	 * Gets the maximum member count, which influences the behaviour
	 * of <code>itemsInserted</code>.
	 *
	 * @see #maxMembers
	 */
	public int getMaxMembers() {
		return maxMembers;
	}

	/**
	 * Works like Put() as specified in the java.util.Map interface,
	 * but also is allowed to delete zero or more items from this
	 * collection after inserting the given item.  The choice of which
	 * items to delete and when is controlled by the various
	 * <code>itemsInserted</code> implementations of concrete
	 * subclasses.
	 */
	public Object put(Object key, Object value) {
		Object retval=super.put(key, value);
		
		Object[] theKey=new Object[1];
		theKey[0]=key;
		itemsInserted(theKey);

		return retval;
	}

	/**
	 * Works like PutAll() as specified in the java.util.Map
	 * interface, but also is allowed to delete zero or more items
	 * from this collection after inserting the given items.  The
	 * choice of which items to delete and when is controlled by the
	 * various <code>itemsInserted</code> implementations of concrete
	 * subclasses.
	 */
	public void putAll(Map t) {
		super.putAll(t);
		itemsInserted(t.keySet().toArray());
	}

	/**
	 * Works like get() as specified in the java.util.Map interface,
	 * but also notifies the implementing subclass of the request via
	 * the itemRequested hook.
	 */
	public Object get(Object key) {
		Object retval=super.get(key);
		itemRequested(key, retval!=null);
		return retval;
	}

	/**
	 * @see #lastFlushDate
	 */
	public Date getLastFlushDate() {
		return lastFlushDate;
	}

	/**
	 * Releases all objects in the cache and updates the last flush
	 * date.
	 */
	public void flush() {
		super.clear();
		lastFlushDate = new Date();
	}
}
