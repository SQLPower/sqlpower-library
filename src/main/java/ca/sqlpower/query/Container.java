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

import java.awt.geom.Point2D;
import java.beans.PropertyChangeListener;
import java.util.List;


/**
 * A container model stores an object that has multiple items 
 * stored in different sections.
 */
public interface Container {
	
	/**
	 * Defines the property change to be a name change on the container.
	 */
	public static final String CONTAINTER_ALIAS_CHANGED = "alias";
	public static final String CONTAINTER_ITEM_ADDED = "itemAdded";
	public static final String CONTAINER_ITEM_REMOVED = "itemRemoved";
	public static final String PROPERTY_WHERE_MODIFIED = "WHERE_MODIFIED";

    /**
     * Returns a copy of this container. The items in the container will be
     * copies as well. Listeners will not be added from the current container to
     * the new container.
     */
	Container createCopy();
	
	/**
	 * Gets all of the sections of the contained object.
	 */
	List<Item> getItems();
	
	/**
	 * Gets the Item wrapper that contains the given item. Returns null if the
	 * object is not contained in this container.
	 */
	Item getItem(Object item);
	
	/**
	 * Gets the object this container is modeling. This object will be the object
	 * that contains the children wrapped by the items and possibly contains more.
	 */
	Object getContainedObject();

	void removeItem(Item item);

	/**
	 * Adds an item as a child of this container to the end of the list of items.
	 */
	void addItem(Item item);
	
	/**
	 * Adds an item as a child of this container at the specified index.
	 */
	void addItem(Item item, int index);
	
	void setAlias(String alias);
	
	String getAlias();
	
	/**
	 * Sets the position of the container. This will allow any view to understand
	 * how the containers are laid out in relation to each other.
	 */
	void setPosition(Point2D p);
	
	/**
	 * Gets the position of the container. This will allow any view to understand
	 * how the containers are laid out in relation to each other.
	 */
	Point2D getPosition();
	
	void addChildListener(ContainerChildListener l);
    void removeChildListener(ContainerChildListener l);
    void addPropertyChangeListener(PropertyChangeListener l);
    void removePropertyChangeListener(PropertyChangeListener l);
    
    /**
     * Returns the short name for this object.
     */
    String getName();
    
    /**
     * Sets the name for this object 
     */
    void setName(String name);
    
    String getUUID();
    
    void setUUID(String id);

    void removeItem(int i);

}