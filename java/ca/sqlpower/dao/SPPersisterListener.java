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

package ca.sqlpower.dao;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyDescriptor;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.log4j.Logger;

import ca.sqlpower.dao.SPPersister.DataType;
import ca.sqlpower.dao.helper.PersisterHelperFinder;
import ca.sqlpower.dao.helper.SPPersisterHelper;
import ca.sqlpower.dao.session.SessionPersisterSuperConverter;
import ca.sqlpower.object.SPChildEvent;
import ca.sqlpower.object.SPListener;
import ca.sqlpower.object.SPObject;
import ca.sqlpower.sqlobject.SQLObject;
import ca.sqlpower.util.SQLPowerUtils;
import ca.sqlpower.util.TransactionEvent;

/**
 * This generic listener will use the persister helper factory given to it to
 * make persist calls when events are fired on the object being listened to.
 */
public class SPPersisterListener implements SPListener {
	
	private static final Logger logger = Logger.getLogger(SPPersisterListener.class);

	/**
	 * This persister will have persist calls made on it when the object(s) this
	 * listener is attached to fires events.
	 */
	private final SPPersister target;

	/**
	 * A class that can convert a complex object into a basic representation
	 * that can be placed in a string and can also convert the string
	 * representation back into the complex object.
	 */
	private final SessionPersisterSuperConverter converter;

	/**
	 * This is the persister that is tied to the listener for echo cancellation.
	 * If this persister is currently changing the object model we should not
	 * send new persist calls.
	 */
	private final SPSessionPersister eventSource;
	
	/**
	 * This will be the list we will use to rollback persisted properties
	 */
	private List<PersistedPropertiesEntry> persistedPropertiesRollbackList = new LinkedList<PersistedPropertiesEntry>();
	
	/**
	 * This will be the list we use to rollback persisted objects.
	 * It contains UUIDs of objects that were created.
	 */
	private List<PersistedObjectEntry> persistedObjectsRollbackList = new LinkedList<PersistedObjectEntry>();
	
	/**
	 * This is the list we use to rollback object removal
	 */
	private List<RemovedObjectEntry> objectsToRemoveRollbackList = new LinkedList<RemovedObjectEntry>();
	
	/**
	 * Persisted property buffer, mapping of {@link SPObject} UUIDs 
	 * concatenated with a property name to the given property.
	 */
	private HashMap<String, PersistedSPOProperty> persistedProperties = new HashMap<String, PersistedSPOProperty>();
	
	/**
	 * A map keeping track of the most recent old value of property changes,
	 * mapped by the concatenation of their uuid and property name.
	 * Used for verifying property changes when condensing them.
	 */
	private HashMap<String, Object> lastOldValue = new HashMap<String, Object>();
	
	/**
	 * Persisted {@link WabitObject} buffer, contains all the data that was
	 * passed into the persistedObject call in the order of insertion
	 */
	private List<PersistedSPObject> persistedObjects = new LinkedList<PersistedSPObject>();
	
	/**
	 * {@link WabitObject} removal buffer, mapping of {@link WabitObject} UUIDs
	 * to their parents
	 */
	private List<RemovedObjectEntry> objectsToRemove = new LinkedList<RemovedObjectEntry>();

	/**
	 * The depth of the current transaction. When this goes from 1 to 0 we know we have exited the outer-most
	 * transaction and need to send the pooled persist calls.
	 */
	private int transactionCount = 0;
	
	/**
	 * If true the current persist calls in the roll back lists are being undone.
	 */
	private boolean rollingBack;
	
	/**
	 * This listener can be attached to a hierarchy of objects to persist events
	 * to the target persister contained in the given persister helper factory.
	 * 
	 * @param target
	 *            The target persister that will be sent persist calls.
	 * @param converter
	 *            A converter to convert complex types of objects into simple
	 *            objects so the object can be passed or persisted.
	 */
	public SPPersisterListener(SPPersister target, SessionPersisterSuperConverter converter) {
		this(target, null, converter);
	}

	public SPPersisterListener(SPPersister target, SPSessionPersister dontEcho, SessionPersisterSuperConverter converter) {
		this.target = target;
		this.converter = converter;
		this.eventSource = dontEcho;
	}
	
	
	public void childAdded(SPChildEvent e) {
		SQLPowerUtils.listenToHierarchy(e.getChild(), this);
		if (wouldEcho()) return;
		logger.debug("Child added: " + e);
		persistObject(e.getChild(), e.getIndex());
	}

	/**
	 * This persists a given object and all of its descendants to the target
	 * persister in this listener. Each object in the descendant tree will have
	 * one persist object call made and any number of additional persist
	 * property calls as needed. This can be useful for persisting an entire
	 * tree of objects to the JCR as an initial commit.
	 * 
	 * @param o
	 *            The object to be persisted.
	 * @param index
	 *            the index the object is located in its parent's list of
	 *            children of the same object type.
	 */
	public void persistObject(final SPObject o, int index) {
		if (wouldEcho()) return;
		
		this.transactionStarted(TransactionEvent.createStartTransactionEvent(this, 
			"Persisting " + o.getName() + " and its descendants."));
		
		final SPPersisterHelper<? extends SPObject> persisterHelper;
		try {
			persisterHelper = PersisterHelperFinder.findPersister(o.getClass());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		
		//This persister is used to put persist calls to the pooled lists in this listener.
		SPPersister poolingPersister = new SPPersister() {
		
			public void rollback() {
				//do nothing, just looking for persist calls.
			}
		
			public void removeObject(String parentUUID, String uuid)
					throws SPPersistenceException {
				throw new IllegalStateException("There was a remove object call when " +
						"trying to persist an object. This should not happen.");
			}
		
			public void persistProperty(String uuid, String propertyName,
					DataType propertyType, Object newValue)
					throws SPPersistenceException {
				//unconditional properties in this case are only on new objects
				//so undoing these properties will only come just before the object is
				//removed and doesn't matter.			    
				persistedProperties.put(uuid + propertyName, new PersistedSPOProperty(uuid, propertyName, 
						propertyType, newValue, newValue, true));
			}
		
			public void persistProperty(String uuid, String propertyName,
					DataType propertyType, Object oldValue, Object newValue)
					throws SPPersistenceException {
			    persistedProperties.put(uuid + propertyName, new PersistedSPOProperty(
                        uuid, propertyName, propertyType, oldValue, newValue, false));			
			}

		
			public void persistObject(String parentUUID, String type, String uuid,
					int index) throws SPPersistenceException {
				logger.debug("Adding a " + type + " with UUID: " + uuid + " to persistedObjects");
				// Check to see this object has not already been added.
				if (getPersistedObject(uuid) != null) {
                    throw new SPPersistenceException(uuid, "Cannot add object of type " 
                                + type + " with UUID " + uuid + " because an object with "
                                + " the same UUID has already been added");                    
                }
				persistedObjects.add(new PersistedSPObject(parentUUID, type, uuid, index));
			}
		
			public void commit() throws SPPersistenceException {
				//do nothing, just looking for persist calls.
			}
		
			public void begin() throws SPPersistenceException {
				//do nothing, just looking for persist calls.		
			}
		};

		try {
			persisterHelper.persistObject(o, index, poolingPersister, converter);
		} catch (SPPersistenceException e) {
			throw new RuntimeException(e);
		}

		List<? extends SPObject> children;
		if (o instanceof SQLObject) {
			children = ((SQLObject) o).getChildrenWithoutPopulating();
		} else {
			children = o.getChildren();
		}
		logger.debug("Persisting children " + children + " of " + o);
		for (SPObject child : children) {
			int childIndex = 0;
			childIndex = getIndexWithinSiblings(o, child);
			persistObject(child, childIndex);
		}

		this.transactionEnded(TransactionEvent.createEndTransactionEvent(this));
	}

	/**
	 * Find the index of the child {@link SPObject} within the list of children
	 * of the same type contained in the parent object.
	 */
	private int getIndexWithinSiblings(SPObject parent, SPObject child) {
	    Class<? extends SPObject> parentAllowedChildType;
        try {
            parentAllowedChildType = PersisterUtils.getParentAllowedChildType(child.getClass().getName(), 
                    parent.getClass().getName());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
		int childIndex;
		if (parent instanceof SQLObject) {
			childIndex = ((SQLObject) parent).getChildrenWithoutPopulating(parentAllowedChildType).indexOf(child);
		} else {
			childIndex = parent.getChildren(parentAllowedChildType).indexOf(child);
		}
		return childIndex;
	}

	public void childRemoved(SPChildEvent e) {
		SQLPowerUtils.unlistenToHierarchy(e.getChild(), this);
		if (wouldEcho()) return;
		String uuid = e.getChild().getUUID();
		if (getRemovedObject(uuid) != null) {
		    throw new IllegalStateException("Cannot add object of type " 
                    + e.getChildType() + " with UUID " + uuid + " because an object with "
                    + " the same UUID has already been added");     
		}
		PersistedSPObject pso = getPersistedObject(uuid);
		if (pso != null) {
		    // Remove the object from the list of objects to be persisted
		    // This way, addition-removal sequences are cancelled out.
		    if (!persistedObjects.remove(pso)) {
		        throw new RuntimeException("This shouldn't have happened");
		    }
		} else {
		    transactionStarted(TransactionEvent.createStartTransactionEvent(this, 
		    "Start of transaction triggered by childRemoved event"));
		    objectsToRemove.add(
		            new RemovedObjectEntry(
		                    e.getSource().getUUID(),
		                    e.getChild(),
		                    e.getIndex()));
		    transactionEnded(TransactionEvent.createEndTransactionEvent(this));
		}
	}

	public void transactionEnded(TransactionEvent e) {
		if (wouldEcho()) return;
		try {
			logger.debug("transactionEnded " + ((e == null) ? null : e.getMessage()));
			commit();
		} catch (SPPersistenceException e1) {
			throw new RuntimeException(e1);
		}
	}

	public void transactionRollback(TransactionEvent e) {
		if (wouldEcho()) return;
		logger.debug("transactionRollback " + ((e == null) ? null : e.getMessage()));
		rollback();
	}

	public void transactionStarted(TransactionEvent e) {
		if (wouldEcho()) return;
		logger.debug("transactionStarted " + ((e == null) ? null : e.getMessage()));
		transactionCount++;
	}

	public void propertyChanged(PropertyChangeEvent evt) {
		if (wouldEcho()) return;
		
		SPObject source = (SPObject) evt.getSource();
		String uuid = source.getUUID();
		String propertyName = evt.getPropertyName();
		Object oldValue = evt.getOldValue();
		Object newValue = evt.getNewValue();
		
		try {
			if (!PersisterHelperFinder.findPersister(source.getClass())
					.getPersistedProperties()
					.contains(propertyName)) {
				logger.debug("Tried to persist a property that shouldn't be. Ignoring the property: " + propertyName);
				return;
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		
		transactionStarted(TransactionEvent.createStartTransactionEvent(this, 
				"Creating start transaction event from propertyChange on object " + 
				evt.getSource().getClass().getSimpleName() + " and property name " + evt.getPropertyName()));
		
		//Not persisting non-settable properties.
		//TODO A method in the persister helpers would make more sense than
		//using reflection here.
		PropertyDescriptor propertyDescriptor;
		try {
			propertyDescriptor= PropertyUtils.getPropertyDescriptor(source, propertyName);
		} catch (Exception ex) {
			this.rollback();
			throw new RuntimeException(ex);
		}
		
		if (propertyDescriptor == null 
				|| propertyDescriptor.getWriteMethod() == null) {
			transactionEnded(TransactionEvent.createEndTransactionEvent(this));
			return;
		}
		
		DataType typeForClass = PersisterUtils.
				getDataType(newValue == null ? Void.class : newValue.getClass());	
		
        String key = uuid + propertyName;
        PersistedSPOProperty property = persistedProperties.get(key);
        if (property != null) {
            // Hang on to the oldest value
            
            if (property.getNewValue().equals(lastOldValue.get(key))) {        
                try {
                    throw new RuntimeException("Multiple property changes do not follow after each other properly");
                } finally {
                    this.rollback();
                }
            }
        }
        
        Object oldBasicType = converter.convertToBasicType(oldValue);
        Object newBasicType = converter.convertToBasicType(newValue);
        
        lastOldValue.put(key, oldBasicType);        
        if (property != null) {
            oldBasicType = property.getOldValue();
        }    
        logger.debug("persistProperty(" + uuid + ", " + propertyName + ", " + 
                typeForClass.name() + ", " + oldValue + ", " + newValue + ")");
        persistedProperties.put(key, new PersistedSPOProperty(
                uuid, propertyName, typeForClass, oldBasicType, newBasicType, false));  
		
		this.transactionEnded(TransactionEvent.createEndTransactionEvent(this));
	}
	
	/**
	 * Returns true if the WabitSessionPersister that this listener complements
	 * is currently in the middle of an update. In that case, none of the
	 * WabitListener methods should make calls into the target persister.
	 * 
	 * @return True if forwarding an event to the target persister would
	 *         constitute an echo.
	 */
	private boolean wouldEcho() {
		if (rollingBack) return true;
		return eventSource != null && eventSource.isUpdatingWorkspace();
	}

	/**
	 * Does the actual commit when the transaction count reaches 0. This ensures
	 * the persist calls are contained in a transaction if a lone change comes
	 * through. This also allows us to roll back changes if an exception comes
	 * from the server.
	 * 
	 * @throws SPPersistenceException
	 */
	private void commit() throws SPPersistenceException {
		logger.debug("commit(): transactionCount = " + transactionCount);
		if (transactionCount==1) {
			try {
				logger.debug("Calling commit...");
				//If nothing actually changed in the transaction do not send
				//the begin and commit to reduce server traffic.
				if (objectsToRemove.isEmpty() && persistedObjects.isEmpty() && 
						persistedProperties.isEmpty()) return;
				
				this.objectsToRemoveRollbackList.clear();
				this.persistedObjectsRollbackList.clear();
				this.persistedPropertiesRollbackList.clear();
				target.begin();
				commitRemovals();
				commitObjects();
				commitProperties();
				target.commit();
				logger.debug("...commit completed.");
			} catch (Throwable t) {
				this.rollback();
				throw new SPPersistenceException(null,t);
			} finally {
				this.objectsToRemove.clear();
				this.objectsToRemoveRollbackList.clear();
				this.persistedObjects.clear();
				this.persistedObjectsRollbackList.clear();
				this.persistedProperties.clear();
				this.persistedPropertiesRollbackList.clear();
				this.transactionCount = 0;
			}
		} else {
			transactionCount--;
		}
	}

	/**
	 * Performs the rollback if a problem occurred during the persist call.
	 */
	public void rollback() {
		if (rollingBack) {
			// This happens when we pick up our own events.
			return;
		}
		if (eventSource == null ||
				eventSource.isHeadingToWisconsin()) {
			// This means that the SessionPersister is cleaning his stuff and
			// we need to do the same. Close all current transactions... bla bla bla.
			this.objectsToRemoveRollbackList.clear();
			this.persistedObjectsRollbackList.clear();
			this.persistedPropertiesRollbackList.clear();
			this.objectsToRemove.clear();
			this.persistedObjects.clear();
			this.persistedProperties.clear();
			this.transactionCount = 0;
			target.rollback();
			return;
		}
		rollingBack = true;
		try {
			SPSessionPersister.undoForSession(
				eventSource.getSession().getWorkspace(), 
				this.persistedObjectsRollbackList, 
				this.persistedPropertiesRollbackList, 
				this.objectsToRemoveRollbackList,
				converter);
		} catch (SPPersistenceException e) {
			logger.error(e);
		} finally {
			this.objectsToRemoveRollbackList.clear();
			this.persistedObjectsRollbackList.clear();
			this.persistedPropertiesRollbackList.clear();
			this.objectsToRemove.clear();
			this.persistedObjects.clear();
			this.persistedProperties.clear();
			this.transactionCount = 0;
			rollingBack = false;
			target.rollback();
		}
	}
	
	/**
	 * Commits the persisted {@link SPObject}s that we pooled during
	 * the transaction. Also updates the roll back list as we go in case
	 * it is needed.
	 * 
	 * @throws SPPersistenceException
	 */
	private void commitObjects() throws SPPersistenceException {
		logger.debug("Committing objects");
		for (PersistedSPObject pwo : persistedObjects) {
			logger.debug("Commiting persist call: " + pwo);
			target.persistObject(
				pwo.getParentUUID(), 
				pwo.getType(),
				pwo.getUUID(),
				pwo.getIndex());
			this.persistedObjectsRollbackList.add(
				new PersistedObjectEntry(
					pwo.getParentUUID(),
					pwo.getUUID()));
		}
	}

	/**
	 * Commits the persisted properties that were pooled during the transaction.
	 * Updates the roll back list as we go.
	 * 
	 * @throws SPPersistenceException
	 */
	private void commitProperties() throws SPPersistenceException {
		logger.debug("commitProperties()");
		for (PersistedSPOProperty property : persistedProperties.values()) {
			if (property.isUnconditional()) {
				target.persistProperty(
					property.getUUID(),
					property.getPropertyName(),
					property.getDataType(),
					property.getNewValue());
			} else {
				target.persistProperty(
				    property.getUUID(), 
					property.getPropertyName(), 
					property.getDataType(), 
					property.getOldValue(),
					property.getNewValue());
			}
			this.persistedPropertiesRollbackList.add(
				new PersistedPropertiesEntry(
				    property.getUUID(), 
					property.getPropertyName(),
					property.getDataType(), 
					property.getOldValue()));
		}
	}

	/**
	 * Commits the removals that were pooled during the transaction. Also
	 * updates the roll back list.
	 */
	private void commitRemovals() throws SPPersistenceException {
		logger.debug("commitRemovals()");
		for (RemovedObjectEntry entry: this.objectsToRemove) {
			logger.debug("target.removeObject(" + entry.getParentUUID() + ", " + 
					entry.getRemovedChild().getUUID() + ")");
			target.removeObject(
				entry.getParentUUID(), 
				entry.getRemovedChild().getUUID());
			this.objectsToRemoveRollbackList.add(entry);
		}
	}
	
	public HashMap<String, PersistedSPOProperty> getPersistedProperties() {
		return persistedProperties;
	}

	public List<PersistedSPObject> getPersistedObjects() {
		return persistedObjects;
	}

	public List<RemovedObjectEntry> getObjectsToRemove() {
		return objectsToRemove;
	}
	
	public List<PersistedObjectEntry> getObjectsRollbackList() {
		return persistedObjectsRollbackList;
	}
	
	public List<PersistedPropertiesEntry> getPropertiesRollbackList() {
		return persistedPropertiesRollbackList;
	}
	
	public List<RemovedObjectEntry> getRemovedRollbackList() {
		return objectsToRemoveRollbackList;
	}
	
	/**
	 * This method cycles through the given list of pooled objects that
	 * are being persisted to see if it contains the object with the given uuid
	 * @param uuid
	 * @return The object with the given uuid, or null if it cannot be found
	 */
	public PersistedSPObject getPersistedObject(String uuid) {
        for (PersistedSPObject o : persistedObjects) {
            if (o.getUUID().equals(uuid)) {
                return o;
            }
        }
        return null;
	}
	
	/**
     * This method cycles through the given list of pooled objects that
     * are being removed to see if it contains the object with the given uuid
     * @param uuid
     * @return The object with the given uuid, or null if it cannot be found
     */
	public RemovedObjectEntry getRemovedObject(String uuid) {
	    for (RemovedObjectEntry o : objectsToRemove) {
	        if (o.getRemovedChild().getUUID().equals(uuid)) {
	            return o;
	        }
	    }
	    return null;
	}
}
