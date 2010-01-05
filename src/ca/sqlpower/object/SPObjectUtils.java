/*
 * Copyright (c) 2010, SQL Power Group Inc.
 *
 * This file is part of SQL Power Library.
 *
 * SQL Power Library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * SQL Power Library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>. 
 */

package ca.sqlpower.object;


/**
 * A bunch of utility methods for SPObjects. 
 */
public class SPObjectUtils {

	private SPObjectUtils() {
		//utility class, cannot instantiate
	}

	/**
	 * Returns the first ancestor of <tt>so</tt> which is of the given type, or
	 * <tt>null</tt> if <tt>so</tt> doesn't have an ancestor whose class is
	 * <tt>ancestorType</tt>.
	 * 
	 * @param so
	 *            The object for whose ancestor to look. (Thanks, Winston).
	 * @return The nearest ancestor of type ancestorType, or null if no such
	 *         ancestor exists.
	 */
	public static <T extends SPObject> T getAncestor(SPObject so, Class<T> ancestorType) {
	    while (so != null) {
	        if (so.getClass().equals(ancestorType)) return ancestorType.cast(so);
	        so = so.getParent();
	    }
	    return null;
	}
	
}
