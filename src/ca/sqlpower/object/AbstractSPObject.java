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

package ca.sqlpower.object;

import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.apache.log4j.Logger;

import ca.sqlpower.object.SPChildEvent.EventType;
import ca.sqlpower.util.TransactionEvent;

public abstract class AbstractSPObject implements SPObject {
	
    private static final Logger logger = Logger.getLogger(SPObject.class);
	
    private final List<SPListener> listeners = 
        Collections.synchronizedList(new ArrayList<SPListener>());
    
	private SPObject parent;
	private String name;
	
	public AbstractSPObject() {
		this(null);
	}
	
	/**
	 * The uuid string passed in must be the toString representation of the UUID
	 * for this object. If the uuid string given is null then a new UUID will be
	 * automatically generated.
	 */
    public AbstractSPObject(String uuid) {
    	if (uuid == null) {
    	    generateNewUUID();
    	} else {
    		this.uuid = uuid;
    	}
    }
	
    /**
     * This UUID is for saving and loading to allow saved files to be diff friendly.
     */
    private String uuid;

	public void addChild(SPObject child, int index)
			throws IllegalArgumentException {
		if (!allowedChildTypes().contains(child.getClass())) {
			throw new IllegalArgumentException("Cannot add child of type " + child.getClass() + 
					" because it is not a valid child class type.");
		}
		
		child.setParent(this);
		addChildImpl(child, index);
	}
	
	protected void addChildImpl(SPObject child, int index) {
		throw new UnsupportedOperationException("This SPObject item cannot have children. " +
				"This class is " + getClass() + " and trying to add " + child.getName() + 
				" of type " + child.getClass());
	}

	public void addSPListener(SPListener l) {
    	if (l == null) {
    		throw new NullPointerException("Cannot add child listeners that are null.");
    	}
    	synchronized (listeners) {
    	    listeners.add(l);
    	}
	}

	public void begin(String message) {
		fireTransactionStarted(message);
	}

	/**
	 * Default cleanup method that does nothing. Override and implement this
	 * method if cleanup is necessary.
	 */
	public CleanupExceptions cleanup() {
	    return new CleanupExceptions();
	}

	public void commit() {
		fireTransactionEnded();
	}

	public void generateNewUUID() {
		uuid = UUID.randomUUID().toString();
	}

	public <T extends SPObject> List<T> getChildren(Class<T> type) {
		List<T> children = new ArrayList<T>();
		for (SPObject child : getChildren()) {
			if (type.isAssignableFrom(child.getClass())) {
				children.add(type.cast(child));
			}
		}
		return children;
	}

	public String getName() {
		return name;
	}

	public SPObject getParent() {
		return parent;
	}

	public String getUUID() {
		return uuid;
	}

	public boolean removeChild(SPObject child)
			throws ObjectDependentException, IllegalArgumentException {
	    if (!getChildren().contains(child)) {
	        throw new IllegalArgumentException("Child object " + child.getName() + " of type " + child.getClass()
	                + " is not a child of " + getName() + " of type " + getClass());
	    }
	    
	    return false;
	}

	public void removeSPListener(SPListener l) {
        synchronized (listeners) {
            listeners.remove(l);
        }
	}

	public void rollback(String message) {
		fireTransactionRollback(message);
	}

	public void setName(String name) {
		String oldName = this.name;
		this.name = name;
		firePropertyChange("name", oldName, name);
	}

	public void setParent(SPObject parent) {
		SPObject oldParent = this.parent;
		this.parent = parent;
		if (parent != null) {
			firePropertyChange("parent", oldParent, parent);
		}
	}

	public void setUUID(String uuid) {
		String oldUUID = this.uuid;
		
		if (uuid == null) {
			generateNewUUID();
		} else {
			this.uuid = uuid;
		}
		
		firePropertyChange("uuid", oldUUID, this.uuid);
	}
	
    /**
     * Fires a child added event to all child listeners. The child should have
     * been added by the calling code already.
     * 
     * @param type
     *            The canonical type of the child being added
     * @param child
     *            The child object that was added
     * @param index
     *            The index of the added child within its own child list (this
     *            will be converted to the overall child position before the
     *            event object is constructed).
     * @return The child event that was fired or null if no event was fired, for
     *         testing purposes.
     */
    protected SPChildEvent fireChildAdded(Class<? extends SPObject> type, SPObject child, int index) {
    	logger.debug("Child Added: " + type + " notifying " + listeners.size() + " listeners");
    	
    	// XXX This was taken from the AbstractWabitObject implementation of fireChildAdded and should be reworked into the library if necessary.
//    	if (!isForegroundThread()) {
//    		throw new IllegalStateException("Event for adding the child " + child.getName() + 
//    				" must fired on the foreground thread.");
//    	}
    	
        synchronized(listeners) {
            if (listeners.isEmpty()) return null;
        }
        final SPChildEvent e = new SPChildEvent(this, type, child, index, EventType.ADDED);
        synchronized(listeners) {
        	for (int i = listeners.size() - 1; i >= 0; i--) {
        		final SPListener listener = listeners.get(i);
        		listener.sqlPowerLibraryChildAdded(e);
        	}
        }
        return e;
    }
    
    /**
     * Fires a child removed event to all child listeners. The child should have
     * been removed by the calling code.
     * 
     * @param type
     *            The canonical type of the child being removed
     * @param child
     *            The child object that was removed
     * @param index
     *            The index that the removed child was at within its own child
     *            list (this will be converted to the overall child position
     *            before the event object is constructed).
     * @return The child event that was fired or null if no event was fired, for
     *         testing purposes.
     */
    protected SPChildEvent fireChildRemoved(Class<? extends SPObject> type, SPObject child, int index) {
    	logger.debug("Child Removed: " + type + " notifying " + listeners.size() + " listeners: " + listeners);
    	
    	// XXX This was taken from the AbstractWabitObject implementation of fireChildRemoved and should be reworked into the library if necessary.
//    	if (!isForegroundThread()) {
//    		throw new IllegalStateException("Event for removing the child " + child.getName() + 
//    				" must fired on the foreground thread.");
//    	}
    	
        synchronized(listeners) {
            if (listeners.isEmpty()) return null;
        }
        final SPChildEvent e = new SPChildEvent(this, type, child, index, EventType.REMOVED);
        synchronized(listeners) {
        	for (int i = listeners.size() - 1; i >= 0; i--) {
        		final SPListener listener = listeners.get(i);
        		listener.sqlPowerLibraryChildRemoved(e);
        	}
        }
        return e;
    }
    
    /**
     * Fires a property change on the foreground thread as defined by the
     * current session being used.
     * 
     * @return The property change event that was fired or null if no event was
     *         fired, for testing purposes.
     */
    protected PropertyChangeEvent firePropertyChange(final String propertyName, final boolean oldValue, 
            final boolean newValue) {
    	if (oldValue == newValue) return null;
    	
    	// XXX This was taken from the AbstractWabitObject implementation of firePropertyChange and should be reworked into the library if necessary.
//    	if (!isForegroundThread()) {
//    		throw new IllegalStateException("Event for property change " + propertyName + 
//    				" must fired on the foreground thread.");
//    	}
        synchronized(listeners) {
            if (listeners.size() == 0) return null;
        }
        final PropertyChangeEvent evt = new PropertyChangeEvent(this, propertyName, oldValue, newValue);
        synchronized(listeners) {
        	for (int i = listeners.size() - 1; i >= 0; i--) {
        		listeners.get(i).propertyChange(evt);
        	}
        }
        return evt;
    }

    /**
     * Fires a property change on the foreground thread as defined by the
     * current session being used.
     * 
     * @return The property change event that was fired or null if no event was
     *         fired, for testing purposes.
     */
    protected PropertyChangeEvent firePropertyChange(final String propertyName, final int oldValue, 
            final int newValue) {
    	if (oldValue == newValue) return null;
    	
    	// XXX This was taken from the AbstractWabitObject implementation of firePropertyChange and should be reworked into the library if necessary.
//    	if (!isForegroundThread()) {
//    		throw new IllegalStateException("Event for property change " + propertyName + 
//    				" must fired on the foreground thread.");
//    	}
    	
        synchronized(listeners) {
            if (listeners.size() == 0) return null;
        }
        final PropertyChangeEvent evt = new PropertyChangeEvent(this, propertyName, oldValue, newValue);
        synchronized(listeners) {
        	for (int i = listeners.size() - 1; i >= 0; i--) {
        		listeners.get(i).propertyChange(evt);
        	}
        }
        return evt;
    }

    /**
     * Fires a property change on the foreground thread as defined by the
     * current session being used.
     * 
     * @return The property change event that was fired or null if no event was
     *         fired, for testing purposes.
     */
    protected PropertyChangeEvent firePropertyChange(final String propertyName, final Object oldValue, 
            final Object newValue) {
    	if ((oldValue == null && newValue == null)
    			|| (oldValue != null && oldValue.equals(newValue))) return null; 
    	
    	// XXX This was taken from the AbstractWabitObject implementation of firePropertyChange and should be reworked into the library if necessary.
//    	if (!isForegroundThread()) {
//    		throw new IllegalStateException("Event for property change " + propertyName + 
//    				" must fired on the foreground thread.");
//    	}
    	
        synchronized(listeners) {
            if (listeners.size() == 0) return null;
            if (logger.isDebugEnabled()) {
                logger.debug("Firing property change \"" + propertyName
                        + "\" to " + listeners.size() + " listeners: "
                        + listeners);
            }
        }
        final PropertyChangeEvent evt = new PropertyChangeEvent(this, propertyName, oldValue, newValue);
        synchronized(listeners) {
        	for (int i = listeners.size() - 1; i >= 0; i--) {
        		listeners.get(i).propertyChange(evt);
        	}
        }
        return evt;
    }
    
    /**
     * Fires a transaction started event with a message indicating the
     * reason/type of the transaction.
     * 
     * @return The event that was fired or null if no event was fired, for
     *         testing purposes.
     */
    protected TransactionEvent fireTransactionStarted(final String message) {
    	// XXX This was taken from the AbstractWabitObject implementation of fireTransactionStarted and should be reworked into the library if necessary.
//    	if (!isForegroundThread()) {
//    		throw new IllegalStateException("Event for a transaction start" + 
//    				" must fired on the foreground thread.");
//    	}
        synchronized (listeners) {
            if (listeners.size() == 0) return null;            
        }
        final TransactionEvent evt = TransactionEvent.createStartTransactionEvent(this, message);
        synchronized (listeners) {
        	for (int i = listeners.size() - 1; i >= 0; i--) {
        		listeners.get(i).transactionStarted(evt);
        	}
        }
        return evt;
    }

    /**
     * Fires a transaction ended event.
     * 
     * @return The event that was fired or null if no event was fired, for
     *         testing purposes.
     */
    protected TransactionEvent fireTransactionEnded() {
    	// XXX This was taken from the AbstractWabitObject implementation of fireTransactionEnded and should be reworked into the library if necessary.
//    	if (!isForegroundThread()) {
//    		throw new IllegalStateException("Event for a transaction end" + 
//    				" must fired on the foreground thread.");
//    	}
        synchronized (listeners) {
            if (listeners.size() == 0) return null;            
        }
        final TransactionEvent evt = TransactionEvent.createEndTransactionEvent(this);
        synchronized (listeners) {
        	for (int i = listeners.size() - 1; i >= 0; i--) {
        		listeners.get(i).transactionEnded(evt);
        	}
        }
        return evt;
    }

    /**
     * Fires a transaction rollback event with a message indicating the
     * reason/type of the rollback.
     * 
     * @return The event that was fired or null if no event was fired, for
     *         testing purposes.
     */
    protected TransactionEvent fireTransactionRollback(final String message) {
    	// XXX This was taken from the AbstractWabitObject implementation of fireTransactionRollback and should be reworked into the library if necessary.
//    	if (!isForegroundThread()) {
//    		throw new IllegalStateException("Event for a transaction rollback" + 
//    				" must fired on the foreground thread.");
//    	}
        synchronized (listeners) {
            if (listeners.size() == 0) return null;            
        }
        final TransactionEvent evt = TransactionEvent.createRollbackTransactionEvent(this, message);
        synchronized (listeners) {
        	for (int i = listeners.size() - 1; i >= 0; i--) {
        		listeners.get(i).transactionRollback(evt);
        	}
        }
        return evt;
    }
    
    @Override
    public boolean equals(Object obj) {
    	return (obj instanceof SPObject && 
    			getUUID().equals(((SPObject) obj).getUUID()));
    }
    
    @Override
    public String toString() {
    	return super.toString() + ", " + getName() + ":" + getUUID();
    }

}
