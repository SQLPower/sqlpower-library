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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ca.sqlpower.object.SPObject;

/**
 * This is normally an invisible root node that contains
 * SQLDatabase objects.
 */
public class SQLObjectRoot extends SQLObject {
	private List<SQLDatabase> databases = new ArrayList<SQLDatabase>();
	
	public SQLObjectRoot() {
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
		// no-op
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

	@Override
	public List<SQLDatabase> getChildren() {
		return Collections.unmodifiableList(databases);
	}

	@Override
	protected boolean removeChildImpl(SPObject child) {
		if (child instanceof SQLDatabase) {
			return removeDatabase((SQLDatabase) child);
		} else {
			throw new IllegalArgumentException("Cannot remove children of type " 
					+ child.getClass() + " from " + getName());
		}
	}
	
	public boolean removeDatabase(SQLDatabase child) {
		if (child.getParent() != this) {
			throw new IllegalStateException("Cannot remove child " + child.getName() + 
					" of type " + child.getClass() + " as its parent is not " + getName());
		}
		int index = databases.indexOf(child);
		if (index != -1) {
			databases.remove(index);
			 child.setParent(null);
			 fireChildRemoved(SQLDatabase.class, child, index);
			 return true;
		}
		return false;
	}

	public int childPositionOffset(Class<? extends SPObject> childType) {
		if (childType == SQLDatabase.class) return 0;
		
		throw new IllegalArgumentException("The type " + childType + 
				" is not a valid child type of " + getName());
	}

	public List<? extends SPObject> getDependencies() {
		return Collections.emptyList();
	}

	public void removeDependency(SPObject dependency) {
		for (SQLObject child : getChildren()) {
			child.removeDependency(dependency);
		}
	}
}