/*
 * Copyright (c) 2009, SQL Power Group Inc.
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

package ca.sqlpower.swingui;

import java.util.regex.Pattern;

/**
 * Classes implementing this interface will be able to search their contained
 * objects with a {@link SearchTextField}. The pattern given to doSearch will
 * have a {@link Pattern#LITERAL} flag set if the pattern is not a regular
 * expression.
 */
public interface Search {

	/**
	 * This will search the contained objects for the given pattern and do any
	 * necessary filtering as well.
	 * 
	 * @param matchExactly
	 *            If set to true the search will match the pattern exactly. If
	 *            false the pattern only needs to be contained in the values
	 *            being searched to match.
	 */
	public void doSearch(Pattern p, boolean matchExactly);

}
