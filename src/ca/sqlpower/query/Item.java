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

import ca.sqlpower.query.QueryImpl.OrderByArgument;


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
	
	/**
	 * Sets the selected position of this item with respect to other items in
	 * the query the item is contained in. If this value is null then the item
	 * is not selected. The value set here cannot be the same as the selected
	 * position of another item in the query.
	 * <p>
	 * The selected order of an item is set here instead of being stored on the
	 * query itself as updating and firing appropriate events for an item list
	 * on the query requires the items as children of the query to be wrapped.
	 * Once the items are wrapped and events are based on the wrappers in the
	 * query difficulties start to arise with adding and removing the wrappers
	 * to and from a query. This becomes increasingly difficult when the item
     * wrappers are wrapped again in places like Wabit.
	 */
	void setSelected(Integer selected);

	/**
	 * Gets the value set by {@link #setSelected(Integer)}.
	 * @see #setSelected(Integer)
	 */
	Integer getSelected();
	
	/**
	 * Returns true if the item is selected.
	 * @see #setSelected(Integer)
	 */
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

    /**
     * Sets this item to have the given ascending or descending ordering. The ordering
     * will affect how the results of a query are sorted based on which selected columns
     * are ordered and the ordering of the order by values.
     */
    public void setOrderBy(OrderByArgument orderBy);

    /**
     * Sets this item to order its values in a query after any numbers with an
     * integer lower than it have been ordered. For example, if this is the
     * fourth item to be ordered the results of a query will be ordered by all
     * of the items with a lower ordering first then the results will be ordered
     * based on this column and finally the columns with ordering will be sorted
     * that have a higher ordering value then this column. The value entered here
     * cannot be the same as the ordering value of another item in the query.
     * <p>
     * The order by order of an item is set here instead of being stored on the
     * query itself as updating and firing appropriate events for an item list
     * on the query requires the items as children of the query to be wrapped.
     * Once the items are wrapped and events are based on the wrappers in the
     * query difficulties start to arise with adding and removing the wrappers
     * to and from a query. This becomes increasingly difficult when the item
     * wrappers are wrapped again in places like Wabit.
     */
    public void setOrderByOrdering(Integer ordering);

    /**
     * Gets the value set by {@link #setOrderBy(OrderByArgument)}.
     * @see #setOrderBy(OrderByArgument)
     */
    public OrderByArgument getOrderBy();
    
    /**
     * Gets the value set by {@link #setOrderByOrdering(Integer)}.
     * @see Item#setOrderByOrdering(Integer)
     */
    public Integer getOrderByOrdering();
    
    String getUUID();

    /**
     * Creates a new copy of the item. The listeners from the current item are
     * not attached to the new item.
     */
    public Item createCopy();
	
}
