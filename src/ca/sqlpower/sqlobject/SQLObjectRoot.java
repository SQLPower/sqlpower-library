/*
 * Copyright (c) 2008, SQL Power Group Inc.
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

package ca.sqlpower.sqlobject;

import java.util.LinkedList;

/**
 * This is normally an invisible root node that contains
 * SQLDatabase objects.
 */
public class SQLObjectRoot extends SQLObject {
	public SQLObjectRoot() {
		children = new LinkedList();
	}

	public SQLObject getParent() {
		return null;
	}

	protected void setParent(SQLObject newParent) {
		// no parent
	}

	public String getName() {
		return getShortDisplayName();
	}

	public String getShortDisplayName() {
		return "Database Connections";
	}
	
	public boolean allowsChildren() {
		return true;
	}
	
	protected void populateImpl() throws SQLObjectException {
		return;
	}
	
	public boolean isPopulated() {
		return true;
	}

	public String toString() {
		return getShortDisplayName();
	}

	@Override
	public Class<? extends SQLObject> getChildType() {
		return SQLDatabase.class;
	}
}