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

import java.beans.PropertyChangeListener;

import ca.sqlpower.query.Query.OrderByArgument;


/**
 * A class implementing this interface wraps an item in a container.
 */
public interface Item {
	
	/**
	 * Defines a property change of an alias on an item.
	 */
	public static final String ALIAS = "alias";
	
	/**
	 * Defines the item contained by this Item was added
	 * or removed from the select list.
	 */
	public static final String SELECTED = "selected";
	
	/**
	 * Defines a change of the where filer for this contained item.
	 */
	public static final String WHERE = "where";
	
	public static final String GROUP_BY = "groupBy";
	
	public static final String HAVING = "having";
	
	public static final String ORDER_BY = "orderBy";

	/**
	 * Defines a change to the item contained by this Item itself.
	 */
	public static final String PROPERTY_ITEM = "ITEM";
	
	/**
	 * Defines a change that the item this ItemPNode contains will be removed
	 * from its container immediately after this event is fired. The item is
	 * removed immediately after this event so the parent container will get the event.
	 */
	public static final String PROPERTY_ITEM_REMOVED = "ITEM_REMOVED";

	Object getItem();
	
	//XXX Redundant to the change to getParent. Remove this once some of the other refactoring is done.
	Container getContainer();
	
	void setAlias(String alias);
	
	String getAlias();
	
	void setWhere(String where);
	
	String getWhere();
	
	void setSelected(boolean selected);
	
	boolean isSelected();
	
	Integer getColumnWidth();
	
	void setColumnWidth(Integer width);
	
    void addPropertyChangeListener(PropertyChangeListener l);
    void removePropertyChangeListener(PropertyChangeListener l);
    Container getParent();
    void setParent(Container parent);
    
    /**
     * Returns the short name for this object.
     */
    String getName();
    
    /**
     * Sets the name for this object 
     */
    void setName(String name);
    
    public void setGroupBy(SQLGroupFunction groupBy);

    public SQLGroupFunction getGroupBy();

    public void setHaving(String having);

    public String getHaving();

    public void setOrderBy(OrderByArgument orderBy);

    public OrderByArgument getOrderBy();
    
    String getUUID();

    /**
     * Creates a new copy of the item. The listeners from the current item are
     * not attached to the new item.
     */
    public Item createCopy();
	
}
