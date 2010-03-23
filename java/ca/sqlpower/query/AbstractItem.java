/*
 * Copyright (c) 2009, SQL Power Group Inc.
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
import java.beans.PropertyChangeSupport;
import java.util.Arrays;
import java.util.UUID;

import org.apache.log4j.Logger;

import ca.sqlpower.query.QueryImpl.OrderByArgument;


public abstract class AbstractItem implements Item {
    
    private static final Logger logger = Logger.getLogger(AbstractItem.class);
    
	/**
	 * The width that this item should take up when displayed in a column of a table.
	 */
	private Integer columnWidth;
	
	private String uuid;
	
	private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);
	
	private Container parent;
	
	private String name;
	
	private Integer selected;
	
    /**
     * This aggregate is either the toString of a SQLGroupFunction or null
     * if the item is not being aggregated on.
     */
    private SQLGroupFunction groupBy = SQLGroupFunction.GROUP_BY;
    
    private String having;
    private OrderByArgument orderBy = OrderByArgument.NONE;

    /**
     * This is the order that this item will come in with respect to other 
     * selected items in the query.
     */
    private Integer orderByOrdering;
	
	public AbstractItem() {
	    uuid = "w" + UUID.randomUUID();
	}
	
	public AbstractItem(String uuid) {
	    if (uuid == null) {
	        this.uuid = "w" + UUID.randomUUID();
	    } else {
	        this.uuid = uuid;
	    }
	}
	
	public void setColumnWidth(Integer width) {
		Integer oldWidth = this.columnWidth;
		this.columnWidth = width;
		pcs.firePropertyChange("columnWidth", oldWidth, width);
	}
	
	public Integer getColumnWidth() {
		return columnWidth;
	}
	
    public void addPropertyChangeListener(PropertyChangeListener l) {
        pcs.addPropertyChangeListener(l);
    }
    
    public void removePropertyChangeListener(PropertyChangeListener l) {
        pcs.removePropertyChangeListener(l);
    }
    
    protected void firePropertyChange(String propertyName, boolean oldValue, boolean newValue) {
        pcs.firePropertyChange(propertyName, oldValue, newValue);
    }

    protected void firePropertyChange(String propertyName, int oldValue, int newValue) {
        pcs.firePropertyChange(propertyName, oldValue, newValue);
    }

    protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
        if (logger.isDebugEnabled()) {
            logger.debug("Firing property change \"" + propertyName + "\" to " +
                    pcs.getPropertyChangeListeners().length +
                    " listeners: " + Arrays.toString(pcs.getPropertyChangeListeners()));
        }
        pcs.firePropertyChange(propertyName, oldValue, newValue);
    }
    
    public Container getParent() {
        return parent;
    }
    
    public void setParent(Container parent) {
        this.parent = parent;
    }
    
    /**
     * Returns the short name for this object.
     */
    public String getName() {
        return name;
    }
    
    /**
     * Sets the name for this object 
     */
    public void setName(String name) {
        this.name = name;
    }
    
    public String getUUID() {
        return uuid;
    }
    
    public void setUUID(String uuid){
    	this.uuid = uuid;
    }
    
    public void setGroupBy(SQLGroupFunction groupBy) {
        SQLGroupFunction oldGroupBy = this.groupBy;
        this.groupBy = groupBy;
        firePropertyChange(GROUP_BY, oldGroupBy, groupBy);
    }

    public SQLGroupFunction getGroupBy() {
        return groupBy;
    }

    public void setHaving(String having) {
        String oldHaving = this.having;
        this.having = having;
        firePropertyChange(HAVING, oldHaving, having);
    }

    public String getHaving() {
        return having;
    }

    public void setOrderBy(OrderByArgument orderBy) {
        if (orderBy == null) throw new IllegalArgumentException("The order by value of a column should not be set to null");
        OrderByArgument oldOrder = this.orderBy;
        this.orderBy = orderBy;
        firePropertyChange(ORDER_BY, oldOrder, orderBy);
    }

    public OrderByArgument getOrderBy() {
        return orderBy;
    }
    
	public Integer getSelected() {
		return selected;
	}
	
	public boolean isSelected() {
	    return selected != null;
	}

	public void setSelected(Integer selected) {
		Integer oldSelect = this.selected;
		this.selected = selected;
		firePropertyChange(SELECTED, oldSelect, selected);
	}
	
	public void setOrderByOrdering(Integer ordering) {
	    Integer oldOrdering = orderByOrdering;
	    orderByOrdering = ordering;
	    firePropertyChange("orderByOrdering", oldOrdering, ordering);
	}
	
	public Integer getOrderByOrdering() {
	    return orderByOrdering;
	}

}
