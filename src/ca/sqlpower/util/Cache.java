package ca.sqlpower.util;

import java.util.Map;
import java.util.Date;

/**
 * The Cache interface extends the normal Java Map interface to
 * provide the methods and parameters necessary for generic object
 * caching.
 *
 * @author Jonathan Fuerth
 * @version $Id$
 */
public interface Cache extends Map {

	/**
	 * Sets the maximum member count, which influences the behaviour
	 * of <code>itemsInserted</code>.
	 */
	public void setMaxMembers(int argMaxMembers);

	/**
	 * Gets the maximum member count, which influences the behaviour
	 * of <code>itemsInserted</code>.
	 */
	public int getMaxMembers();

	/**
	 * Gets the last time this cache was fully emptied.  Useful for
	 * deciding if the cache needs to be refreshed.  The cache will
	 * set the flush date when it is first created, and again every
	 * time the flush() method is called.
	 */
	public Date getLastFlushDate();

	/**
	 * Removes all items from the cache, and records the current time
	 * as the last flush date.
	 */
	public void flush();
	
	/**
	 * Returns the instance of CacheStats which contains useful
	 * statistics for tuning this cache.
	 */
	public CacheStats getStats();

}
