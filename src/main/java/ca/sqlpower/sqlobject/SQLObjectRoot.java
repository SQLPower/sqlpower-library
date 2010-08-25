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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import ca.sqlpower.object.SPObject;
import ca.sqlpower.object.annotation.Accessor;
import ca.sqlpower.object.annotation.Constructor;
import ca.sqlpower.object.annotation.NonProperty;
import ca.sqlpower.object.annotation.Transient;

/**
 * This is normally an invisible root node that contains
 * SQLDatabase objects.
 */
public class SQLObjectRoot extends SQLObject {
	
	/**
	 * Defines an absolute ordering of the child types of this class.
	 */
	@SuppressWarnings("unchecked")
	public static final List<Class<? extends SPObject>> allowedChildTypes = 
		Collections.unmodifiableList(new ArrayList<Class<? extends SPObject>>(
				Arrays.asList(SQLDatabase.class, SQLTable.class)));
	
	private List<SQLDatabase> databases = new ArrayList<SQLDatabase>();
	private List<SQLTable> tables = new ArrayList<SQLTable>();
	
	@Constructor
	public SQLObjectRoot() {
		setName("Database Connections");
	}

	@Transient @Accessor
	public String getShortDisplayName() {
		return getName();
	}
	
	protected void populateImpl() throws SQLObjectException {
		// no-op
	}
	
	@Transient @Accessor
	public boolean isPopulated() {
		return true;
	}

	public String toString() {
		return getShortDisplayName();
	}

	@Override
	public List<? extends SQLObject> getChildrenWithoutPopulating() {
		List<SQLObject> children = new ArrayList<SQLObject>();
		children.addAll(databases);
		children.addAll(tables);
		return Collections.unmodifiableList(children);
	}
	
	@Override
	protected boolean removeChildImpl(SPObject child) {
		if (child instanceof SQLDatabase) {
			return removeDatabase((SQLDatabase) child);
		} else if (child instanceof SQLTable) {
			return removeTable((SQLTable) child);
		} else {
			throw new IllegalArgumentException("Cannot remove children of type " 
					+ child.getClass() + " from " + getName());
		}
	}
	
	public boolean removeTable(SQLTable child) {
		if (child.getParent() != this) {
			throw new IllegalStateException("Cannot remove child " + child.getName() + 
					" of type " + child.getClass() + " as its parent is not " + getName());
		}
		int index = tables.indexOf(child);
		if (index != -1) {
			tables.remove(index);
			child.setParent(null);
			fireChildRemoved(SQLDatabase.class, child, index);
			return true;
		}
		return false;
	}

	public boolean removeDatabase(SQLDatabase child) {
		if (child.getParent() != this) {
			throw new IllegalStateException("Cannot remove child " + child.getName() + 
					" of type " + child.getClass() + " as its parent is not " + getName());
		}
		int index = databases.indexOf(child);
		if (index != -1) {
			databases.remove(index);			
			fireChildRemoved(SQLDatabase.class, child, index);
			child.setParent(null);
			child.disconnect();
			return true;
		}
		return false;
	}

	@NonProperty
	public List<? extends SPObject> getDependencies() {
		return Collections.emptyList();
	}

	public void removeDependency(SPObject dependency) {
		for (SQLObject child : getChildren()) {
			child.removeDependency(dependency);
		}
	}
	
	@Override
	protected void addChildImpl(SPObject child, int index) {
		if (child instanceof SQLDatabase) {
			addDatabase((SQLDatabase) child, index);
		} else if (child instanceof SQLTable) {
			addTable((SQLTable) child, index);
		} else {
			throw new IllegalArgumentException("The child " + child.getName() + 
					" of type " + child.getClass() + " is not a valid child type of " + 
					getClass() + ".");
		}
	}
	
	public void addDatabase(SQLDatabase child, int index) {
		databases.add(index, child);
		child.setParent(this);
		fireChildAdded(SQLDatabase.class, child, index);
	}

	public void addTable(SQLTable child, int index) {
		tables.add(index, child);
		child.setParent(this);
		fireChildAdded(SQLTable.class, child, index);
	}

	public List<Class<? extends SPObject>> getAllowedChildTypes() {
		return allowedChildTypes;
	}
}