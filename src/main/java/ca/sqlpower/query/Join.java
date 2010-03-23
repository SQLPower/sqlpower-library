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

package ca.sqlpower.query;

import java.beans.PropertyChangeListener;

public interface Join {
	
	public abstract Item getLeftColumn();
	
	public abstract Item getRightColumn();

	public abstract String getComparator();
	
	public abstract void setComparator(String comparator);

	/**
	 * Returns true if this join's left column is an outer join.
	 * Returns false if this join's left column is an inner join.
	 */
	public abstract boolean isLeftColumnOuterJoin();

	public abstract void setLeftColumnOuterJoin(boolean isLeftColumnOuterJoin);
	
	/**
	 * Returns true if this join's right column is an outer join.
	 * Returns false if this join's right column is an inner join.
	 */	
	public abstract boolean isRightColumnOuterJoin();

	public abstract void setRightColumnOuterJoin(boolean isRightColumnOuterJoin);
	
	public abstract void addJoinChangeListener(PropertyChangeListener l);
	
	public abstract void removeJoinChangeListener(PropertyChangeListener l);

	/**
	 * Ensures that all listeners are removed from this join.
	 * XXX This method should be removed as objects adding listeners to a join
	 * should remove the joins appropriately as well.
	 */
	public abstract void removeAllListeners();
	
	public abstract String getName();

	public abstract void setName(String name);

	public abstract String getUUID();
	
	public abstract void setUUID(String uuid);
	
}
