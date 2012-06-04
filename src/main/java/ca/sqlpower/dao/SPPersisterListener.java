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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;

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
import ca.sqlpower.util.HashTreeSetMultimap;
import ca.sqlpower.util.SQLPowerUtils;
import ca.sqlpower.util.TransactionEvent;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.SortedSetMultimap;

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
	private SPSessionPersister eventSource;
	
	/**
	 * Persisted property buffer, mapping a {@link SPObject} uuid 
	 * to a list of persisted properties for that uuid.
	 */
	private Multimap<String, PersistedSPOProperty> persistedProperties = LinkedListMultimap.create();
	
	/**
	 * Persisted {@link WabitObject} buffer, contains all the data that was
	 * passed into the persistedObject call in the order of insertion
	 */
	private LinkedHashMap<String, PersistedSPObject> persistedObjects = new LinkedHashMap<String, PersistedSPObject>();
	
	/**
	 * Keep track of the SPObjects that exist under the same parent. 
	 * Key - the UUID of the parent.
	 * Value - the PersistedSPObject that is a child of parent with (parentUUID = key)
	 */
	private SortedSetMultimap<String, PersistedSPObject> parentPeristedObjects = new HashTreeSetMultimap<String, PersistedSPObject>(new Comparator<PersistedSPObject>() {

		@Override
		public int compare(PersistedSPObject o1, PersistedSPObject o2) {
			if (o1 == null && o2 == null) return 0;
			if (o1 == null) return -1;
			if (o2 == null) return 1;
			return o1.getIndex() - o2.getIndex();
		}
		
	});
	
	/**
	 * {@link WabitObject} removal buffer, mapping of {@link WabitObject} UUIDs
	 * to their parents
	 */
	private LinkedHashMap<String, RemovedObjectEntry> objectsToRemove = new LinkedHashMap<String, RemovedObjectEntry>();
	
	/**
     * Keeps track of the UUIDs of all removed objects and all their descendants
     * so that when properties are being persisted, this can be used to check
     * whether the object the property belongs to has been removed or not.
     */
	private Set<String> removedObjectsUUIDs = new HashSet<String>();

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
	 * If defined this listener will not roll back changes to the workspace it
	 * is listening to. Instead it will still reset itself but it will rely on
	 * this persister to perform the rollback. This listener will also not
	 * forward changes while this persister is rolling back.
	 * <p>
	 * If this value is null a rollback on this listener will undo the changes
	 * it received from the model.
	 */
	private final SPSessionPersister rollbackPersister;
	
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

	/**
	 * For the rest of the parameters see
	 * {@link #SPPersisterListener(SPPersister, SessionPersisterSuperConverter)}
	 * .
	 * 
	 * @param dontEcho
	 *            If this persister is specified the listener will not collect
	 *            events when the persister is updating the model.
	 */
	public SPPersisterListener(SPPersister target, SPSessionPersister dontEcho, SessionPersisterSuperConverter converter) {
		this(target, dontEcho, converter, null);
	}

	/**
	 * For the rest of the parameters see
	 * {@link #SPPersisterListener(SPPersister, SPSessionPersister, SessionPersisterSuperConverter)}
	 * 
	 * @param rollbackUpdatesWorkspace
	 *            If true this listener will roll back changes it heard when
	 *            rollback is performed. Rollback is normally performed when
	 *            either explicitly called or when an exception occurs in a
	 *            later persister. If false the listener will reset but it will
	 *            not update the workspace it is listening to.
	 */
	public SPPersisterListener(SPPersister target, SPSessionPersister dontEcho, 
			SessionPersisterSuperConverter converter, SPSessionPersister rollbackPersister) {
		this.target = target;
		this.converter = converter;
		this.eventSource = dontEcho;
		this.rollbackPersister = rollbackPersister;
	}
	
	private String getParentPersistedObjectsId(PersistedSPObject pso) {
		return getParentPersistedObjectsId(pso.getParentUUID(), pso.getType());
	}
	
	private String getParentPersistedObjectsId(String parentUUID, String type) {
		return parentUUID + "." + type; 
	}
	
	public void childAdded(SPChildEvent e) {
			    if (!e.getSource().getRunnableDispatcher().isForegroundThread()) {
            throw new RuntimeException("New child event " + e + " not fired on the foreground.");
        }
		int index = e.getIndex();
		// Get all the sibling under the same parent
		PersistedSPObject minValue = new PersistedSPObject(
				e.getSource().getUUID(), e.getChildType().getName(), e.getChild().getUUID(), index);
		Set<PersistedSPObject> toBeUpdated = new HashSet<PersistedSPObject>(parentPeristedObjects.get(getParentPersistedObjectsId(minValue)).tailSet(minValue));
		
		for (PersistedSPObject psp : toBeUpdated) {
			// Update the sibling's index
			PersistedSPObject newIndexedSibling = new PersistedSPObject(psp.getParentUUID(), psp.getType(), psp.getUUID(), psp.getIndex()+1);
			parentPeristedObjects.remove(getParentPersistedObjectsId(psp), psp);
			parentPeristedObjects.put(getParentPersistedObjectsId(newIndexedSibling), newIndexedSibling);
			psp.setIndex(psp.getIndex() + 1);
		} 
			    
		SQLPowerUtils.listenToHierarchy(e.getChild(), this);
		if (wouldEcho()) return;
		logger.debug("Child added: " + e);
		persistObject(e.getChild(), index);
		removedObjectsUUIDs.removeAll(getDescendantUUIDs(e.getChild()));
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
	    persistObject(o, index, true);
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
     * @param includeRootPersistObject
     *            If set to false the persist object for 'o' that is passed in
     *            will not be sent. The persist properties of final values will
     *            also not be sent. This is useful if we want to use the
     *            persister listener to update an object tree but the root
     *            already exists and we cannot or do not want to recreate it.
     */
	public void persistObject(final SPObject o, int index, final boolean includeRootPersistObject) {
		if (wouldEcho()) return;
		
		this.transactionStarted(TransactionEvent.createStartTransactionEvent(this, 
			"Persisting " + o.getName() + " and its descendants."));
		
		
		
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
				persistedProperties.put(uuid, new PersistedSPOProperty(uuid, propertyName, 
						propertyType, newValue, newValue, true));
			}
		
			public void persistProperty(String uuid, String propertyName,
					DataType propertyType, Object oldValue, Object newValue)
					throws SPPersistenceException {
			    persistedProperties.put(uuid, new PersistedSPOProperty(
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
				PersistedSPObject pspo = new PersistedSPObject(parentUUID, type, uuid, index);
				persistedObjects.put(uuid, pspo);
				parentPeristedObjects.put(getParentPersistedObjectsId(pspo), pspo);
			}
		
			public void commit() throws SPPersistenceException {
				//do nothing, just looking for persist calls.
			}
		
			public void begin() throws SPPersistenceException {
				//do nothing, just looking for persist calls.		
			}
		};
		
		try {
			persistObjectInterleaveProperties(o, index, includeRootPersistObject, poolingPersister);
		} catch (SPPersistenceException e) {
			throw new RuntimeException(e);
		}
			
		this.transactionEnded(TransactionEvent.createEndTransactionEvent(this));
	}

	/**
	 * The object and all of its descendants will be persisted to the given
	 * persister. The difference from this method and
	 * {@link #persistObject(SPObject, int, boolean)} is this method will
	 * persist the objects and properties inter-leaved where each set of
	 * properties will come after its persist object call.
	 * 
	 * @param o
	 *            The object to persist and all of its descendants will be
	 *            persisted as well.
	 * @param index
	 *            The index of the object in its parent list.
	 * @param includeRootPersistObject
	 *            If false the persist call for persisting object passed in as o
	 *            will be skipped.
	 * @param localTarget
	 *            The persister to make all of the persist object calls to.
	 * @throws SPPersistenceException
	 */
	public void persistObjectInterleaveProperties(SPObject o, int index, boolean includeRootPersistObject, SPPersister localTarget) throws SPPersistenceException {
		final SPPersisterHelper<? extends SPObject> persisterHelper;
		try {
			persisterHelper = PersisterHelperFinder.findPersister(o.getClass());
		} catch (Exception e) {
			throw new SPPersistenceException(o.getUUID(), e);
		}
		
		localTarget.begin();
		if (includeRootPersistObject) {
			persisterHelper.persistObject(o, index, localTarget, converter);
		} else {
			List<String> emptyList = new ArrayList<String>();
			persisterHelper.persistObjectProperties(o, localTarget, converter, emptyList);
		}

		for (Class<? extends SPObject> childType : o.getAllowedChildTypes()) {
			List<? extends SPObject> children;
			if (o instanceof SQLObject) {
				children = ((SQLObject) o).getChildrenWithoutPopulating(childType);
			} else {
				children = o.getChildren(childType);
			}
			logger.debug("Persisting children " + children + " of " + o);
			for (int i = 0; i < children.size(); i++) {
				persistObjectInterleaveProperties(children.get(i), i, true, localTarget);
			}
		}
		
		localTarget.commit();
	}

	public void childRemoved(SPChildEvent e) {
       if (!e.getSource().getRunnableDispatcher().isForegroundThread()) {
            throw new RuntimeException("Removed child event " + e + " not fired on the foreground.");
        }
       
       String parentUUID = e.getSource().getUUID();
		int index = e.getIndex();
		List<PersistedSPObject> toBeUpdated = new ArrayList<PersistedSPObject>();
		// Get all the sibling under the same parent
		//TODO make this work like childAdded
		for (PersistedSPObject persisterSibling : parentPeristedObjects.get(getParentPersistedObjectsId(parentUUID, e.getChildType().getName()))) {
			
			if (persisterSibling.getIndex() > index && persisterSibling.getType().equals(e.getChildType().getName())) {
				toBeUpdated.add(persisterSibling);
			}
		}
		
		for (PersistedSPObject psp : toBeUpdated) {
			// Update the sibling's index
			PersistedSPObject newIndexedSibling = new PersistedSPObject(psp.getParentUUID(), psp.getType(), psp.getUUID(), psp.getIndex()-1);
			persistedObjects.remove(psp.getUUID());
			persistedObjects.put(psp.getUUID(), newIndexedSibling);
			parentPeristedObjects.remove(getParentPersistedObjectsId(psp), psp);
			parentPeristedObjects.put(getParentPersistedObjectsId(newIndexedSibling), newIndexedSibling);
		} 
       
       
       
		SQLPowerUtils.unlistenToHierarchy(e.getChild(), this);
		if (wouldEcho()) return;
		String uuid = e.getChild().getUUID();
		if (getRemovedObject(uuid) != null && getPersistedObject(uuid) == null) {
		    throw new IllegalStateException("Cannot add object of type " 
                    + e.getChildType() + " with UUID " + uuid + " because an object with "
                    + " the same UUID has already been removed");     
		}
		PersistedSPObject pso = getPersistedObject(uuid);
		if (pso == null) {
		    transactionStarted(TransactionEvent.createStartTransactionEvent(this, 
		    "Start of transaction triggered by childRemoved event"));
		    objectsToRemove.put(e.getChild().getUUID(),
		            new RemovedObjectEntry(
		                    e.getSource().getUUID(),
		                    e.getChild(),
		                    e.getIndex()));
		    removedObjectsUUIDs.addAll(getDescendantUUIDs(e.getChild()));
		    transactionEnded(TransactionEvent.createEndTransactionEvent(this));
		}
		//When a remove comes in we need to remove all of the persist calls for the
		//object being removed and its descendants regardless if a remove event is included.
	    persistedProperties.removeAll(uuid);
	    if (pso != null) {
	    	persistedObjects.remove(pso.getUUID());
	    	parentPeristedObjects.remove(getParentPersistedObjectsId(pso), pso);
	    }
	    List<String> descendantUUIDs = new ArrayList<String>(getDescendantUUIDs(e.getChild()));
	    descendantUUIDs.remove(uuid);
	    for (String uuidToRemove : descendantUUIDs) {
	        persistedProperties.removeAll(uuidToRemove);
	        PersistedSPObject childPSO = persistedObjects.get(uuidToRemove);
	        persistedObjects.remove(uuidToRemove);
	        if (childPSO != null) {
	        	parentPeristedObjects.remove(getParentPersistedObjectsId(childPSO), childPSO);
	        }
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
	    
		if (!((SPObject) evt.getSource()).getRunnableDispatcher().isForegroundThread()) {
	        throw new RuntimeException("Property change " + evt + " not fired on the foreground.");
	    }

        Object oldBasicType = converter.convertToBasicType(oldValue);
        Object newBasicType = converter.convertToBasicType(newValue);
		
		PersistedSPOProperty property = null;
        for (PersistedSPOProperty p : persistedProperties.get(uuid))  {
            if (p.getPropertyName().equals(propertyName)) {
                property = p;
                break;
            }
        }
        if (property != null) {
            boolean valuesMatch;
            if (property.getNewValue() == null) {
                valuesMatch = oldBasicType == null;
            } else {
                // Check that the old property's new value is equal to the new change's old value.
                // Also, accept the change if it is the same as the last one.
                valuesMatch = property.getNewValue().equals(oldBasicType) || 
                (property.getOldValue().equals(oldBasicType) && property.getNewValue().equals(newBasicType));
            }
            if (!valuesMatch) {
                try {
                    throw new RuntimeException("Multiple property changes do not follow after each other properly. " +
                            "Property " + property.getPropertyName() + ", on object " + source + " of type " + 
                            source.getClass() + ", Old " + oldBasicType + ", new " + property.getNewValue());
                } finally {
                    this.rollback();
                }
            }
        }
		
        if (wouldEcho()) {
            //The persisted property was changed by a persist call received from the server.
            //The property is removed from the persist calls as it now matchs what is
            //in the server.
            persistedProperties.remove(uuid, property);
            return;
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
		
        boolean unconditional = false;
        if (property != null) {
            // Hang on to the old value
            oldBasicType = property.getOldValue();
            // If an object was created, and unconditional properties are being sent,
            // this will maintain that flag in the event of additional property changes
            // to the same property in the same transaction.
            unconditional = property.isUnconditional();
            persistedProperties.remove(uuid, property);
        }
        logger.debug("persistProperty(" + uuid + ", " + propertyName + ", " + 
                typeForClass.name() + ", " + oldValue + ", " + newValue + ")");
        persistedProperties.put(uuid, new PersistedSPOProperty(
                uuid, propertyName, typeForClass, oldBasicType, newBasicType, unconditional));
		
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
		if (rollbackPersister != null && rollbackPersister.isHeadingToWisconsin()) return true;
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
				target.begin();
				commitRemovals();
				commitObjects();
				commitProperties();
				target.commit();
				logger.debug("...commit completed.");
				if (logger.isDebugEnabled()) {
			        try {
			            final Clip clip = AudioSystem.getClip();
			            clip.open(AudioSystem.getAudioInputStream(
			                    getClass().getResource("/sounds/transaction_complete.wav")));
			            clip.addLineListener(new LineListener() {
                            public void update(LineEvent event) {
                                if (event.getType().equals(LineEvent.Type.STOP)) {
                                    logger.debug("Stopping sound");
                                    clip.close();
                                }
                            }
                        });
			            clip.start();
			        } catch (Exception ex) {
			            logger.debug("A transaction committed but we cannot play the commit sound.", ex);
			        }
				}
			} catch (Throwable t) {
			    logger.warn("Rolling back due to " + t, t);
				this.rollback();
				if (t instanceof SPPersistenceException) throw (SPPersistenceException) t;
				if (t instanceof FriendlyRuntimeSPPersistenceException) throw (FriendlyRuntimeSPPersistenceException) t;
				throw new SPPersistenceException(null,t);
			} finally {
				clear();
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
			this.objectsToRemove.clear();
			this.removedObjectsUUIDs.clear();
			this.persistedObjects.clear();
			this.parentPeristedObjects.clear();
			this.persistedProperties.clear();
			this.transactionCount = 0;
			target.rollback();
			return;
		}
		rollingBack = true;
		SPObject workspace = eventSource.getWorkspaceContainer().getWorkspace(); 
		boolean initialMagic = workspace.isMagicEnabled();
		if (initialMagic) {
		    workspace.setMagicEnabled(false);
		}
		try {
			if (rollbackPersister == null) {
				List<PersistedObjectEntry> rollbackObjects = new LinkedList<PersistedObjectEntry>();
				List<PersistedPropertiesEntry> rollbackProperties = new LinkedList<PersistedPropertiesEntry>();

				for (PersistedSPObject o : persistedObjects.values()) {
					rollbackObjects.add(new PersistedObjectEntry(o.getParentUUID(), o.getUUID()));
				}

				for (PersistedSPOProperty p : persistedProperties.values()) {
					rollbackProperties.add(new PersistedPropertiesEntry(
							p.getUUID(), p.getPropertyName(), p.getDataType(), p.getOldValue()));
				}

				SPSessionPersister.undoForSession(
						eventSource.getWorkspaceContainer().getWorkspace(), 
						rollbackObjects,
						rollbackProperties, 
						objectsToRemove,
						converter);
			}
		} catch (SPPersistenceException e) {
			logger.error(e);
		} finally {
			this.objectsToRemove.clear();
			this.removedObjectsUUIDs.clear();
			this.persistedObjects.clear();
			this.parentPeristedObjects.clear();
			this.persistedProperties.clear();
			this.transactionCount = 0;
			rollingBack = false;
			target.rollback();
			if (initialMagic) {
			    eventSource.getWorkspaceContainer().getWorkspace().setMagicEnabled(true);
			}
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
		for (PersistedSPObject pwo : persistedObjects.values()) {
			logger.debug("Commiting persist call: " + pwo);
			target.persistObject(
				pwo.getParentUUID(), 
				pwo.getType(),
				pwo.getUUID(),
				pwo.getIndex());
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
            if (removedObjectsUUIDs.contains(property.getUUID())) continue;
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
        }
	}

	/**
	 * Commits the removals that were pooled during the transaction. Also
	 * updates the roll back list.
	 */
	private void commitRemovals() throws SPPersistenceException {
		logger.debug("commitRemovals()");
		for (RemovedObjectEntry entry: this.objectsToRemove.values()) {
		    // Don't make removal persist calls for children of
		    // objects that are also being removed, since the JCR handles that.
		    if (removedObjectsUUIDs.contains(entry.getParentUUID())) continue;
			logger.debug("target.removeObject(" + entry.getParentUUID() + ", " + 
					entry.getRemovedChild().getUUID() + ")");
			target.removeObject(
				entry.getParentUUID(), 
				entry.getRemovedChild().getUUID());
		}
	}
	
	public List<PersistedSPOProperty> getPersistedProperties() {
	    return new LinkedList<PersistedSPOProperty>(persistedProperties.values());
	}

	public List<PersistedSPObject> getPersistedObjects() {
		return new ArrayList<PersistedSPObject>(persistedObjects.values());
	}

	public LinkedHashMap<String, RemovedObjectEntry> getObjectsToRemove() {
		return objectsToRemove;
	}
	
	/**
	 * This method cycles through the given list of pooled objects that
	 * are being persisted to see if it contains the object with the given uuid
	 * @param uuid
	 * @return The object with the given uuid, or null if it cannot be found
	 */
	public PersistedSPObject getPersistedObject(String uuid) {
        return persistedObjects.get(uuid);
	}
	
	/**
     * This method cycles through the given list of pooled objects that
     * are being removed to see if it contains the object with the given uuid
     * @param uuid
     * @return The object with the given uuid, or null if it cannot be found
     */
	public RemovedObjectEntry getRemovedObject(String uuid) {
	    return objectsToRemove.get(uuid);
	}
	
    /**
     * Returns a list of UUIDs that contains this object's UUID and all of the
     * children's uuid.
     */
    public Set<String> getDescendantUUIDs(SPObject parent) {
        Set<String> uuids = new HashSet<String>();
        uuids.add(parent.getUUID());
        List<? extends SPObject> children;
        //XXX We need a way to get the children of SQLObjects for persistence
        //without causing them to populate.
        if (parent instanceof SQLObject) {
            children = ((SQLObject) parent).getChildrenWithoutPopulating();
        } else {
            children = parent.getChildren();
        }
        for (SPObject child : children) {
            uuids.addAll(getDescendantUUIDs(child));
        }
        return uuids;
    }
    
    /**
     * This method can be used to reset the listener back to an initial state.
     * The ability to reset the listener is useful if we are trying to re-apply
     * changes such as in the network conflict resolver but the listener is already
     * in a transaction.
     */
    public void clear() {
		this.objectsToRemove.clear();
		this.removedObjectsUUIDs.clear();
		this.persistedObjects.clear();
		this.parentPeristedObjects.clear();
		this.persistedProperties.clear();
    }

	/**
	 * Returns true if the listener is in a transaction. False if the listener
	 * is not in a transaction.
	 */
    public boolean isInTransaction() {
    	return transactionCount > 0;
    }

	/**
	 * The event source of a listener can be changed so the listener does not
	 * echo events. This can only be done when the listener is not in a
	 * transaction.
	 */
    public void setEventSource(SPSessionPersister eventSource) {
    	if (isInTransaction()) throw new IllegalStateException("Cannot set the event source when in a transaction!");
		this.eventSource = eventSource;
	}
}
