/*
 * Copyright (c) 2008, SQL Power Group Inc.
 *
 * This file is part of Wabit.
 *
 * Wabit is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * Wabit is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>. 
 */

package ca.sqlpower.query;

import ca.sqlpower.sqlobject.SQLObject;

/**
 * This container item wraps a SQLColumn for use in a ContainerPane.
 */
public class SQLObjectItem extends AbstractItem {
	
	private SQLObject sqlObject;
	
	private String alias;
	
	private String where;
	
	public SQLObjectItem(SQLObject object) {
		sqlObject = object;
		this.alias = "";
		this.where = "";
		setSelected(null);
	}
	
	/**
	 * If a null uuid is given to this constructor a new UUID will
	 * be generated.
	 */
	public SQLObjectItem(String name, String uuid) {
		super(uuid);
		sqlObject = null;
		super.setName(name);
		this.alias = "";
		this.where = "";
		setSelected(null);
	}
	
	public String getName() {
		if (sqlObject == null) {
			return super.getName();
		}
		return sqlObject.getName();
	}
	
	public void setName(String name) {
		throw new IllegalStateException("Cannot set the name of a SQL object retrieved from the database through a query.");
	}
	
	public void setItem(SQLObject object) {
		sqlObject = object;
	}
	
	/**
	 * Since the sql objects are loaded lazily this can be a moderately long
	 * operation if the file was loaded but the sql objects themselves have
	 * not yet been loaded.
	 */
	public Object getItem() {
		if (sqlObject == null) {
			((TableContainer) getParent()).loadTableByQualifiedName();
		}
		return sqlObject;
	}
	
	public String getAlias() {
		return alias;
	}
	
	public void setAlias(String alias) {
		String oldAlias = this.alias;
		if(alias.equals(oldAlias)) {
			return;
		}
		this.alias = alias;
		firePropertyChange(ALIAS, oldAlias, alias);
	}
	
	public String getWhere() {
		return where;
	}

	public void setWhere(String where) {
		String oldWhere = this.where;
		if (where.equals(oldWhere)) {
			return;
		}
		this.where = where;
		firePropertyChange(WHERE, oldWhere, where);
	}

	public Container getContainer() {
		return (Container) getParent();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof SQLObjectItem) {
			if (((SQLObjectItem) obj).getUUID().equals(getUUID())) {
				return true;
			}
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		int result = 17;
		result = 31 * result + getUUID().hashCode();
		return result;
	}

    public Item createCopy() {
        SQLObjectItem copy = new SQLObjectItem(sqlObject);
        copy.setAlias(getAlias());
        copy.setColumnWidth(getColumnWidth());
        copy.setGroupBy(getGroupBy());
        copy.setHaving(getHaving());
        copy.setOrderBy(getOrderBy());
        copy.setOrderByOrdering(getOrderByOrdering());
        copy.setSelected(getSelected());
        copy.setWhere(getWhere());
        return copy;
    }
	
}
