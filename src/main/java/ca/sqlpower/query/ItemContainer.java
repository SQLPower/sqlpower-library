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

import java.awt.Point;
import java.awt.geom.Point2D;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.apache.log4j.Logger;

/**
 * This container is used to hold a generic list of items in
 * the same section.
 */
public class ItemContainer implements Container {
	
	private static final Logger logger = Logger.getLogger(ItemContainer.class);

	/**
	 * The user visible name to this container.
	 */
	private String name;

	/**
	 * This section holds all of the Items containing the strings in this
	 * container.
	 */
	private final List<Item> itemList;
	
	private String alias;
	
	private Point2D position;
	
	private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);
	
	private String uuid;
	
	private final List<ContainerChildListener> childListeners = new ArrayList<ContainerChildListener>();
	
	public ItemContainer(String name) {
		this(name, null);
	}
	
	/**
	 * This constructor allows defining a specific UUID when creating a container.
	 * This should only be used in loading. Giving a null UUID will automatically 
	 * generate a UUID.
	 */
	public ItemContainer(String name, String uuid) {
		if (uuid == null) {
		    this.uuid = "w" + UUID.randomUUID();
		} else {
		    this.uuid = uuid;
		}
		this.name = name;
		itemList = new ArrayList<Item>();
		logger.debug("Container created.");
		position = new Point(0, 0);
	}

	public Object getContainedObject() {
		return Collections.unmodifiableList(itemList);
	}

	public Item getItem(Object item) {
		for (Item i : itemList) {
			if (i.getItem().equals(item)) {
				return i;
			}
		}
		return null;
	}
	
	public void addItem(Item item) {
	    addItem(item, itemList.size());
	}
	
	public void addItem(Item item, int index) {
		itemList.add(index, item);
		item.setParent(this);
		fireChildAdded(item, index);
	}
	
    protected void fireChildAdded(Item item, int index) {
    	synchronized(childListeners) {
    		for (int i = childListeners.size() - 1; i >= 0; i--) {
    			childListeners.get(i).containerChildAdded(new ContainerChildEvent(this, item, index));
    		}
    	}
    }
	
	public void removeItem(Item item) {
		int index = itemList.indexOf(item);
		itemList.remove(item);
		fireChildRemoved(item, index);
	}

    protected void fireChildRemoved(Item item, int index) {
    	synchronized(childListeners) {
    		for (int i = childListeners.size() - 1; i >= 0; i--) {
    			childListeners.get(i).containerChildRemoved(new ContainerChildEvent(this, item, index));
    		}
    	}
    }

	public String getName() {
		return name;
	}

	public List<Item> getItems() {
		return Collections.unmodifiableList(itemList);
	}

	public String getAlias() {
		return alias;
	}

	/**
	 * Sets the alias of the container. Null is not allowed.
	 */
	public void setAlias(String alias) {
		String oldAlias = this.alias;
		this.alias = alias;
		pcs.firePropertyChange(CONTAINTER_ALIAS_CHANGED, oldAlias, alias);
	}

	public Point2D getPosition() {
		return position;
	}

	public void setPosition(Point2D position) {
	    Point2D oldPosition = this.position;
	    this.position = position;
	    pcs.firePropertyChange("position", oldPosition, position);
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ItemContainer && ((ItemContainer) obj).getUUID().equals(getUUID())) {
			return true;
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return 31 * 17 + getUUID().hashCode();
	}

    public void addChildListener(ContainerChildListener l) {
        childListeners.add(l);        
    }

    public void removeChildListener(ContainerChildListener l) {
        childListeners.remove(l);
    }

    public void addPropertyChangeListener(PropertyChangeListener l) {
        pcs.addPropertyChangeListener(l);
    }

    public String getUUID() {
        return uuid;
    }
    
    public void setUUID(String id) {
        String oldUUID = uuid;
        uuid = id;
        pcs.firePropertyChange("uuid", oldUUID, id);
    }

    public void removePropertyChangeListener(PropertyChangeListener l) {
        pcs.removePropertyChangeListener(l);
    }

    public void setName(String name) {
        this.name = name;
    }

    public Container createCopy() {
        ItemContainer copy = new ItemContainer(getName());
        copy.setAlias(getAlias());
        copy.setPosition(new Point2D.Double(getPosition().getX(), getPosition().getY()));
        for (Item item : getItems()) {
            copy.addItem(item.createCopy());
        }
        return copy;
    }

    public void removeItem(int i) {
        Item removed = itemList.remove(i);
        fireChildRemoved(removed, i);
    }

}
