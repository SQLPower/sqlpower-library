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

import ca.sqlpower.dao.PersisterUtils;
import ca.sqlpower.dao.SPObjectVetoException;
import ca.sqlpower.dao.VetoableSPListener;
import ca.sqlpower.object.SPChildEvent.EventType;
import ca.sqlpower.object.annotation.Accessor;
import ca.sqlpower.object.annotation.Constructor;
import ca.sqlpower.object.annotation.Mutator;
import ca.sqlpower.object.annotation.NonBound;
import ca.sqlpower.object.annotation.NonProperty;
import ca.sqlpower.object.annotation.Persistable;
import ca.sqlpower.object.annotation.Transient;
import ca.sqlpower.util.RunnableDispatcher;
import ca.sqlpower.util.SessionNotFoundException;
import ca.sqlpower.util.TransactionEvent;
import ca.sqlpower.util.WorkspaceContainer;

@Persistable
public abstract class AbstractSPObject implements SPObject {
	
    private static final Logger logger = Logger.getLogger(AbstractSPObject.class);
	
    protected final List<SPListener> listeners = 
        Collections.synchronizedList(new ArrayList<SPListener>());
    
	private SPObject parent;
	private String name;

	/**
	 * If magic is disabled, secondary side effects of methods or events should
	 * not be performed. Magic is disabled if the disabled value is greater than
	 * 0. For example in Wabit, if a guide is moved in a report the content
	 * boxes attached to it will normally be moved with it. If magic is disabled
	 * the content boxes attached to a guide should not be moved as the guide
	 * moves. The guide that is moved while magic is disabled should still fire
	 * an event that it was moved but the secondary event of moving the content
	 * boxes and any other side effects should not take place.
	 */
	private int magicDisableCount = 0;
	
	@Constructor
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
    protected String uuid;

    public boolean allowsChildType(Class<? extends SPObject> type) {
    	for (Class<? extends SPObject> child : getAllowedChildTypes()) {
    		if (child.isAssignableFrom(type)) {
    			return true;
    		}
    	}
    	return false;
    }
    
	public final void addChild(SPObject child, int index)
			throws IllegalArgumentException {
		if (!allowsChildType(child.getClass())) {
			throw new IllegalArgumentException(child.getClass() + " is not a valid child type of " + this.getClass());
		}
		
		child.setParent(this);
		addChildImpl(child, index);
	}
	
	public void addChild(SPObject child) throws IllegalArgumentException {
		if (!allowsChildType(child.getClass())) {
			throw new IllegalArgumentException(child.getClass() + " is not a valid child type of " + this.getClass());
		}
		
		child.setParent(this);
		Class<? extends SPObject> childClass;
		try {
			childClass = PersisterUtils.getParentAllowedChildType(child.getClass(), this.getClass());
		} catch (IllegalAccessException e) {
			throw new RuntimeException("The allowedChildTypes field must be accessible", e);
		} catch (NoSuchFieldException e) {
			throw new RuntimeException("The allowedChildTypes field must exist", e);
		}
		addChild(child,getChildren(childClass).size());
	}
	
    /**
     * This is the object specific implementation of
     * {@link #addChild(SPObject, int)}. There are checks in the
     * {@link #addChild(SPObject, int))} method to ensure that the object given
     * here is a valid child type of this object.
     * <p>
     * This method should be overwritten if children are allowed.
     * 
     * @param child
     *            The child to add to this object.
     * @param index
     *            The index to add the child at.
     */
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
    		if (listeners.contains(l)) {
    			logger.debug("Listener " + l + " was added twice! Ignoring second add");
    		} else {
    			listeners.add(l);
    		}
    	}
	}

	/**
	 * Default cleanup method that does nothing. Override and implement this
	 * method if cleanup is necessary.
	 */
	public CleanupExceptions cleanup() {
	    return new CleanupExceptions();
	}

	public void generateNewUUID() {
		uuid = UUID.randomUUID().toString();
	}

	@NonProperty
	public <T extends SPObject> List<T> getChildren(Class<T> type) {
		List<T> children = new ArrayList<T>();
		for (SPObject child : getChildren()) {
			if (type.isAssignableFrom(child.getClass())) {
				children.add(type.cast(child));
			}
		}
		return Collections.unmodifiableList(children);
	}
	
    public boolean allowsChildren() {
        return !getAllowedChildTypes().isEmpty();
    }
	
    public int childPositionOffset(Class<? extends SPObject> childType) {  
        int offset = 0;
        for (Class<? extends SPObject> type : getAllowedChildTypes()) {
            if (type.isAssignableFrom(childType)) {
                return offset;
            } else {
                offset += getChildren(type).size();
            }
        }
        throw new IllegalArgumentException(childType.getName() + 
                " is not a valid child type of " + getClass().getName());
    }
    
	@Accessor(isInteresting=true)
	public String getName() {
		return name;
	}

	@Accessor
	public SPObject getParent() {
		return parent;
	}

	@Accessor
	public String getUUID() {
		return uuid;
	}


	public boolean removeChild(SPObject child)
			throws ObjectDependentException, IllegalArgumentException {
	    if (!getChildren().contains(child)) {
	        throw new IllegalArgumentException("Child object " + child.getName() + " of type " + child.getClass()
	                + " is not a child of " + getName() + " of type " + getClass());
	    }
	    
	    if (removeChildImpl(child)) {
	    	child.setParent(null);
	    	return true;
	    }
	    return false;
	}
	
    /**
     * This is the object specific implementation of removeChild. There are
     * checks in the removeChild method to ensure the child being removed has no
     * dependencies and is a child of this object.
     * 
     * @see #removeChild(SPObject)
     */
	protected abstract boolean removeChildImpl(SPObject child);

	public void removeSPListener(SPListener l) {
        synchronized (listeners) {
            listeners.remove(l);
        }
	}

	public void rollback(String message) {
		fireTransactionRollback(message);
	}

	@Mutator
	public void setName(String name) {
		String oldName = this.name;
		this.name = name;
		firePropertyChange("name", oldName, name);
	}

	@Mutator
	public void setParent(SPObject parent) {
		SPObject oldParent = this.parent;
		this.parent = parent;
		firePropertyChange("parent", oldParent, parent);
	}

	@Mutator
	public void setUUID(String uuid) {
		String oldUUID = this.uuid;
		
		if (uuid == null) {
			generateNewUUID();
		} else {
			this.uuid = uuid;
		}
		
		firePropertyChange("UUID", oldUUID, this.uuid);
	}

	/**
	 * Gets the current workspace container by passing the request up the tree.
	 */
	@Transient @Accessor
	public WorkspaceContainer getWorkspaceContainer() throws SessionNotFoundException {
		// The root object of the tree model should have a reference back to the
		// session (like WabitWorkspace), and should therefore override this
		// method. If it does not, a SessionNotFoundException will be thrown.
		if (getParent() != null) {
			return getParent().getWorkspaceContainer();
		} else {
			throw new SessionNotFoundException("Root object does not have a workspace container reference");
		}
	}
	
	/**
	 * Gets the current runnable dispatcher by passing the request up the tree.
	 */
	@Transient @Accessor
	public RunnableDispatcher getRunnableDispatcher() throws SessionNotFoundException {
		// The root object of the tree model should have a reference back to the
		// session (like WabitWorkspace), and should therefore override this
		// method. If it does not, a SessionNotFoundException will be thrown.
		if (getParent() != null) {
			return getParent().getRunnableDispatcher();
		} else {
			throw new SessionNotFoundException("Root object does not have a runnable dispatcher reference");
		}
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
    	
    	synchronized(listeners) {
    		if (listeners.isEmpty()) return null;
    	}
    	
    	if (!isForegroundThread()) {
    		throw new IllegalStateException("Event for adding the child " + child.getName() + 
    				" must fired on the foreground thread.");
    	}
    	
        final SPChildEvent e = new SPChildEvent(this, type, child, index, EventType.ADDED);
        synchronized(listeners) {
        	List<SPListener> staticListeners = new ArrayList<SPListener>(listeners);
        	for (int i = staticListeners.size() - 1; i >= 0; i--) {
        		final SPListener listener = staticListeners.get(i);
        		listener.childAdded(e);
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
    	
    	synchronized(listeners) {
    		if (listeners.isEmpty()) return null;
    	}
    	
    	if (!isForegroundThread()) {
    		throw new IllegalStateException("Event for removing the child " + child.getName() + 
    				" must fired on the foreground thread.");
    	}
    	
        final SPChildEvent e = new SPChildEvent(this, type, child, index, EventType.REMOVED);
        synchronized(listeners) {
        	List<SPListener> staticListeners = new ArrayList<SPListener>(listeners);
        	for (int i = staticListeners.size() - 1; i >= 0; i--) {
        		final SPListener listener = staticListeners.get(i);
        		listener.childRemoved(e);
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
    	synchronized(listeners) {
    		if (listeners.size() == 0) return null;
    	}
    	
    	if (!isForegroundThread()) {
    		throw new IllegalStateException("Event for property change " + propertyName + 
    				" must fired on the foreground thread.");
    	}
    	
        final PropertyChangeEvent evt = new PropertyChangeEvent(this, propertyName, oldValue, newValue);
        synchronized(listeners) {
        	List<SPListener> staticListeners = new ArrayList<SPListener>(listeners);
        	for (int i = staticListeners.size() - 1; i >= 0; i--) {
        		SPListener listener = staticListeners.get(i);
        		listener.propertyChanged(evt);
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
    	
    	synchronized(listeners) {
    		if (listeners.size() == 0) return null;
    	}
    	
    	if (!isForegroundThread()) {
    		throw new IllegalStateException("Event for property change " + propertyName + 
    				" must fired on the foreground thread.");
    	}
    	
        final PropertyChangeEvent evt = new PropertyChangeEvent(this, propertyName, oldValue, newValue);
        synchronized(listeners) {
        	List<SPListener> staticListeners = new ArrayList<SPListener>(listeners);
        	for (int i = staticListeners.size() - 1; i >= 0; i--) {
        		SPListener listener = staticListeners.get(i);
        		listener.propertyChanged(evt);
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
    protected PropertyChangeEvent firePropertyChange(final String propertyName, final char oldValue, 
            final char newValue) {
    	if (oldValue == newValue) return null;
    	
    	synchronized(listeners) {
    		if (listeners.size() == 0) return null;
    	}
    	
    	if (!isForegroundThread()) {
    		throw new IllegalStateException("Event for property change " + propertyName + 
    				" must fired on the foreground thread.");
    	}
    	
        final PropertyChangeEvent evt = new PropertyChangeEvent(this, propertyName, oldValue, newValue);
        synchronized(listeners) {
        	List<SPListener> staticListeners = new ArrayList<SPListener>(listeners);
        	for (int i = staticListeners.size() - 1; i >= 0; i--) {
        		SPListener listener = staticListeners.get(i);
        		listener.propertyChanged(evt);
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
    	
    	synchronized(listeners) {
    		if (listeners.size() == 0) return null;
    		if (logger.isDebugEnabled()) {
    			logger.debug("Firing property change \"" + propertyName
    					+ "\" to " + listeners.size() + " listeners: "
    					+ listeners);
    		}
    	}
    	
    	if (!isForegroundThread()) {
    		throw new IllegalStateException("Event for property change " + propertyName + 
    				" must fired on the foreground thread.");
    	}
    	
        final PropertyChangeEvent evt = new PropertyChangeEvent(this, propertyName, oldValue, newValue);
        synchronized(listeners) {
        	List<SPListener> staticListeners = new ArrayList<SPListener>(listeners);
        	for (int i = staticListeners.size() - 1; i >= 0; i--) {
        		SPListener listener = staticListeners.get(i);
        		listener.propertyChanged(evt);
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
    	synchronized (listeners) {
    		if (listeners.size() == 0) return null;            
    	}
    	if (!isForegroundThread()) {
    		throw new IllegalStateException("Event for a transaction start" + 
    				" must fired on the foreground thread.");
    	}
    	logger.debug(getName() + "[" + getUUID() + "]: Firing transaction started to " + listeners.size() + " listeners");
        final TransactionEvent evt = TransactionEvent.createStartTransactionEvent(this, message);
        synchronized (listeners) {
        	List<SPListener> staticListeners = new ArrayList<SPListener>(listeners);
        	for (int i = staticListeners.size() - 1; i >= 0; i--) {
        		final SPListener listener = staticListeners.get(i);
        		listener.transactionStarted(evt);
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
    	return fireTransactionEnded("Transaction Ended; Source: " + this);
    }

    /**
     * Fires a transaction ended event.
     * 
     * @return The event that was fired or null if no event was fired, for
     *         testing purposes.
     */
    protected TransactionEvent fireTransactionEnded(final String message) {
    	synchronized (listeners) {
    		if (listeners.size() == 0) return null;            
    	}
    	if (!isForegroundThread()) {
    		throw new IllegalStateException("Event for a transaction end" + 
    				" must fired on the foreground thread.");
    	}
    	logger.debug(getName() + "[" + getUUID() + "]: Firing transaction ended to " + listeners.size() + " listeners");
        final TransactionEvent evt = TransactionEvent.createEndTransactionEvent(this, message);
        
        synchronized (listeners) {
        	List<SPListener> staticListeners = new ArrayList<SPListener>(listeners);
        	for (int i = staticListeners.size() - 1; i >= 0; i--) {
        		if (staticListeners.get(i) instanceof VetoableSPListener) {
        			final VetoableSPListener vetoableListener = (VetoableSPListener) staticListeners.get(i);
        			try {
						vetoableListener.vetoableChange();
					} catch (SPObjectVetoException e) {
						rollback(e.getMessage());
						throw new RuntimeException(e);
					}
        		}
        	}
        }
        
        synchronized (listeners) {
        	List<SPListener> staticListeners = new ArrayList<SPListener>(listeners);
        	for (int i = staticListeners.size() - 1; i >= 0; i--) {
        		final SPListener listener = staticListeners.get(i);
        		listener.transactionEnded(evt);
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
    	synchronized (listeners) {
    		if (listeners.size() == 0) return null;            
    	}
    	if (!isForegroundThread()) {
    		throw new IllegalStateException("Event for a transaction rollback" + 
    				" must fired on the foreground thread.");
    	}
    	logger.debug(getName() + "[" + getUUID() + "]: Firing transaction rollback to " + listeners.size() + " listeners");
        final TransactionEvent evt = TransactionEvent.createRollbackTransactionEvent(this, message);
        synchronized (listeners) {
        	List<SPListener> staticListeners = new ArrayList<SPListener>(listeners);
        	for (int i = staticListeners.size() - 1; i >= 0; i--) {
        		final SPListener listener = staticListeners.get(i);
        		listener.transactionRollback(evt);
        	}
        }
        return evt;
    }
    
    public void begin(String message) {
    	fireTransactionStarted(message);
    }
    
    public void commit() {
    	fireTransactionEnded();
    }
    
    public void commit(String message) {
    	fireTransactionEnded(message);
    }
    
    protected boolean isForegroundThread() {
		try {
			return getRunnableDispatcher().isForegroundThread();
		} catch (SessionNotFoundException e) {
			return true;
		}
	}
    
    /**
     * Calls the runInBackground method on the session this object is attached
     * to if it exists. If this object is not attached to a session, which can
     * occur when loading, copying, or creating a new object, the runner will be
     * run on the current thread due to not being able to run elsewhere. Any
     * SPObject that wants to run a runnable in the background should call to
     * this method instead of to the session.
     * 
     * @see WabitSession#runInBackground(Runnable)
     */
	protected void runInBackground(Runnable runner) {
	    try {
	        getRunnableDispatcher().runInBackground(runner);
	    } catch (SessionNotFoundException e) {
	        runner.run();
	    }
	}
	
	 /**
     * Calls the runInForeground method on the session this object is attached
     * to if it exists. If this object is not attached to a session, which can
     * occur when loading, copying, or creating a new object, the runner will be
     * run on the current thread due to not being able to run elsewhere. Any
     * SPObject that wants to run a runnable in the foreground should call to
     * this method instead of to the session.
     * 
     * @see WabitSession#runInBackground(Runnable)
     */
	protected void runInForeground(Runnable runner) {
	    try {
	        getRunnableDispatcher().runInForeground(runner);
	    } catch (SessionNotFoundException e) {
	        runner.run();
	    }
	}
	
	@NonBound
	public boolean isMagicEnabled() {
		return (magicDisableCount == 0 && 
				(getParent() == null || getParent().isMagicEnabled()));
	}
	
	@NonBound
	public synchronized void setMagicEnabled(boolean enable) {
		if (enable) {
			if (magicDisableCount == 0) {
				throw new IllegalArgumentException("Cannot enable magic because it is already enabled.");
			}
			magicDisableCount--;
		} else {
			magicDisableCount++;
		}
	}
    
    @Override
    public boolean equals(Object obj) {
    	return (obj instanceof SPObject && 
    			getUUID().equals(((SPObject) obj).getUUID()));
    }
    
    @Override
    public int hashCode() {
    	final int prime = 31;
    	int result = 17;
    	
    	result = prime * result + uuid.hashCode();
    	
    	return result;
    }
    
    @Override
    public String toString() {
    	return super.toString() + ", " + getName() + ":" + getUUID();
    }

}
