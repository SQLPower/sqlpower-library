/*
 * Copyright (c) 2008, SQL Power Group Inc.
 *
 * This file is part of Power*Architect.
 *
 * Power*Architect is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * Power*Architect is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>. 
 */
package ca.sqlpower.diff;

import ca.sqlpower.sqlobject.SQLColumn;
import java.util.ArrayList;
import java.util.List;


public class DiffChunk<T> {
	private DiffType type;
	private T data;
	private T originalData;
    /**
     * A list of property changes this object had.
     */
    final List<PropertyChange> changes;
	
	/**
	 * @param data
	 * @param type
	 */
	public DiffChunk(T data, DiffType type) {
		this.data = data;
		this.type = type;
		changes = new ArrayList<PropertyChange>();
	}

    public void setOriginalData(T old) {
        originalData = old;
    }

    public T getOriginalData() {
        return originalData;
    }
    
	public T getData() {
		return data;
	}

	public DiffType getType() {
		return type;

	}
	@Override
	public String toString() {
		if (data instanceof SQLColumn) {
			SQLColumn c = (SQLColumn)data;
			String colname = c.getParent().getName() + "." + c.toString();
			return super.toString() + "(" +type+")["+colname+"]";
		}
		return super.toString() + "(" +type+")["+data+"]";
	}
	
	public List<PropertyChange> getPropertyChanges() {
	    return changes;
	}
	
	public void addPropertyChange(PropertyChange change) {
	    changes.add(change);
	}
	
}
