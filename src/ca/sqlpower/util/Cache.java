package ca.sqlpower.util;

import java.util.Map;

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
	 *
	 * @see #maxMembers
	 */
	public void setMaxMembers(int argMaxMembers);

	/**
	 * Gets the maximum member count, which influences the behaviour
	 * of <code>itemsInserted</code>.
	 *
	 * @see #maxMembers
	 */
	public int getMaxMembers();

}
