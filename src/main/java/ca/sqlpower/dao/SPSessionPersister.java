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
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import ca.sqlpower.dao.helper.PersisterHelperFinder;
import ca.sqlpower.dao.session.SessionPersisterSuperConverter;
import ca.sqlpower.object.ObjectDependentException;
import ca.sqlpower.object.SPChildEvent;
import ca.sqlpower.object.SPListener;
import ca.sqlpower.object.SPObject;
import ca.sqlpower.sqlobject.SQLCatalog;
import ca.sqlpower.sqlobject.SQLColumn;
import ca.sqlpower.sqlobject.SQLDatabase;
import ca.sqlpower.sqlobject.SQLIndex;
import ca.sqlpower.sqlobject.SQLObject;
import ca.sqlpower.sqlobject.SQLObjectUtils;
import ca.sqlpower.sqlobject.SQLRelationship;
import ca.sqlpower.sqlobject.SQLSchema;
import ca.sqlpower.sqlobject.SQLTable;
import ca.sqlpower.util.SQLPowerUtils;
import ca.sqlpower.util.TransactionEvent;
import ca.sqlpower.util.WorkspaceContainer;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;

public abstract class SPSessionPersister implements SPPersister {
	
	/**
	 * The god mode means that this listener will output
	 * events that are unconditional, always. This makes it the
	 * purveyor of the truth.
	 */
	private boolean godMode = false;
	
	private static final Logger logger = Logger.getLogger(SPSessionPersister.class);
	
	/**
	 * An {@link SPSession} to persist objects and properties onto.
	 */
	private WorkspaceContainer workspaceContainer;
	
	/**
	 * A count of transactions, mainly to keep track of nested transactions.
	 */
	private int transactionCount = 0;
	
	/**
	 * Persisted property buffer, mapping of {@link SPObject} UUIDs to each
	 * individual persisted property
	 */
	protected Multimap<String, PersistedSPOProperty> persistedProperties = 
		LinkedListMultimap.create();

	/**
	 * This will be the list we will use to rollback persisted properties
	 */
	private List<PersistedPropertiesEntry> persistedPropertiesRollbackList = 
		new LinkedList<PersistedPropertiesEntry>();

	/**
	 * Persisted {@link SPObject} buffer, contains all the data that was passed
	 * into the persistedObject call in the order of insertion. Note that this
	 * list must be kept consistent with persistedObjectsMap.
	 */
	protected List<PersistedSPObject> persistedObjects = 
		new LinkedList<PersistedSPObject>();

	/**
	 * This map stores values looked up by findByUUID and allows them to be
	 * looked up faster to improve performance. This cache should be cleared
	 * regularly before and after each transaction so the values in it stay
	 * consistent with what actually exists in the client model.
	 */
	private final Map<String, SPObject> lookupCache = new HashMap<String, SPObject>();
	
	/**
	 * This map allows for fast lookups of persisted objects by their UUID.
	 * <p>
	 * Note that the entries in this map must be kept consistent with those in
	 * persistedObjects.
	 * <p>
	 * Changing persistedObjects to be an ordered map may be more sensible then
	 * using this map in the future but we will see how much this improves
	 * performance.
	 */
	private Map<String, PersistedSPObject> persistedObjectsMap = 
		new HashMap<String, PersistedSPObject>();
	
	/**
	 * This will be the list we use to rollback persisted objects.
	 * It contains UUIDs of objects that were created.
	 */
	private List<PersistedObjectEntry> persistedObjectsRollbackList = 
		new LinkedList<PersistedObjectEntry>();

	/**
	 * {@link Comparator} for comparing {@link PersistedSPObject}s, to be used
	 * before committing persisted objects.
	 */
	protected final Comparator<PersistedSPObject> persistedObjectComparator = 
		new Comparator<PersistedSPObject>() {

		// If the two objects being compared are of the same type and are
		// children of the same parent, the one with the lower index should go
		// first. Otherwise, the one with the smaller ancestor tree should go first
		// (e.g. Report should go before Page).
		public int compare(PersistedSPObject o1, PersistedSPObject o2) {

			if (o1.getParentUUID() == null && o2.getParentUUID() == null) {
				return 0;
			} else if (o1.getParentUUID() == null) {
				return -1;
			} else if (o2.getParentUUID() == null) {
				return 1;
			} else if (o1.getParentUUID().equals(o2.getParentUUID()) && 
					o1.getType().equals(o2.getType())) {
				return Integer.signum(o1.getIndex() - o2.getIndex());
			}
			
			List<PersistedSPObject> ancestorList1 = buildAncestorListFromPersistedObjects(o1);
			List<PersistedSPObject> ancestorList2 = buildAncestorListFromPersistedObjects(o2);
			
			if (ancestorList1.contains(o2)) {
			    return 1;
			} else if (ancestorList2.contains(o1)) {
			    return -1;
			}
			
			PersistedSPObject previousAncestor = null;
			PersistedSPObject ancestor1 = null;
			PersistedSPObject ancestor2 = null;
			boolean compareWithAncestor = false;
			
			//Looking at the highest ancestor that is different in the list
			for (int i = 0, j = 0; i < ancestorList1.size() && j < ancestorList2.size(); i++, j++) {
				ancestor1 = ancestorList1.get(i);
				ancestor2 = ancestorList2.get(j);
				
				if (previousAncestor != null && !ancestor1.equals(ancestor2)) {
					compareWithAncestor = true;
					break;
				}
				
				previousAncestor = ancestor1;
			}
			
			if (!compareWithAncestor) {
				if (ancestorList1.size() < ancestorList2.size()) {
					ancestor1 = o1;
					ancestor2 = ancestorList2.get(ancestorList1.size());
				} else if (ancestorList1.size() > ancestorList2.size()) {
					ancestor1 = ancestorList1.get(ancestorList2.size());
					ancestor2 = o2;
				} else {
					ancestor1 = o1;
					ancestor2 = o2;
				}
			}
			if (ancestor1.equals(ancestor2)) {
			    throw new IllegalStateException("The ancestors of " + o1 + " and " + o2 + 
			            " is the same object but should not be by checks above. The ancestor is " + ancestor1);
			}
			
			int c;
			
			if (ancestor1.getType().equals(ancestor2.getType())) {
				c = ancestor1.getIndex() - ancestor2.getIndex();
			} else {
				if (previousAncestor == null) {
					if (ancestorList1.isEmpty()) throw new IllegalStateException("The object represented by " + o1 + 
							" is not correctly connected to the model");
					if (ancestorList2.isEmpty()) throw new IllegalStateException("The object represented by " + o2 + 
							" is not correctly connected to the model");
					throw new NullPointerException("There was an issue comparing " + o1 + " and " + o2 + " which is " +
							"normally caused by the objects not being in the same tree.");
				}
				//Looking at the highest ancestor that is different in the list and finding the order
				//of these ancestors based on the absolute ordering defined in their shared parent class type.
				try {
					
					int ancestorType1Index = PersisterUtils.getTypePosition(ancestor1.getType(),
					        previousAncestor.getType());
					if (ancestorType1Index == -1) {
					    throw new IllegalStateException("Allowed child types for " + previousAncestor + 
					            " does not contain " + ancestor1);
					}
					int ancestorType2Index = PersisterUtils.getTypePosition(ancestor2.getType(),
					        previousAncestor.getType());
                    if (ancestorType2Index == -1) {
                        throw new IllegalStateException("Allowed child types for " + previousAncestor + 
                                " does not contain " + ancestor2);
                    }
					c = ancestorType1Index - ancestorType2Index;

					if (c == 0 && ancestor1.getParentUUID().equals(ancestor2.getParentUUID())) {
					    //If you reach this point the two objects are subclasses of the same
					    //object type contained in the allowedChildTypes list and their order
					    //is decided by their index locations.
					    c = ancestor1.getIndex() - ancestor2.getIndex();
					    if (c == 0) {
					        throw new IllegalStateException("The objects " + ancestor1 + " and " + 
					                ancestor2 + " are defined as equal but shouldn't be.");
					    }
					} else if (c == 0) {
					    throw new IllegalStateException("Error comparing " + o1 + " to " + o2);
					}
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
			return Integer.signum(c);
		}
	};

	/**
	 * This comparator sorts buffered removeObject calls by each
	 * {@link SPObject} UUID. The UUIDs being compared must matchup with an
	 * existing SPObject in the {@link #root}. If it does not exist, it means
	 * that the SPObject has just been removed and this comparator is
	 * reshuffling the map.
	 * TODO We need a generic way of comparing {@link SPObject}s.
	 */
	protected final Comparator<String> removedObjectComparator = new Comparator<String>() {
		public int compare(String uuid1, String uuid2) {
			SPObject spo1 = findByUuid(root, uuid1, SPObject.class);
			SPObject spo2 = findByUuid(root, uuid2, SPObject.class);
			
			if (uuid1.equals(uuid2)) {
				return 0;
			} else if (spo1 == null && spo2 == null) {
				return uuid2.compareTo(uuid1);
			} else if (spo1 == null) {
				return -1;
			} else if (spo2 == null) {
				return 1;
			} else if (spo1.getParent() == null && spo2.getParent() == null) {
				return 0;
			} else if (spo1.getParent() == null) {
				return 1;
			} else if (spo2.getParent() == null) {
				return -1;
			} else if (spo1.equals(spo2)) {
				return 0;
			} else if (spo1.getParent().equals(spo2.getParent())) {
				List<? extends SPObject> siblings;
				if (spo1.getParent() instanceof SQLObject) {
					siblings = ((SQLObject) spo1.getParent()).getChildrenWithoutPopulating();
				} else {
					siblings = spo1.getParent().getChildren();
				}
				
				return Integer.signum(siblings.indexOf(spo2) - siblings.indexOf(spo1));
			}
				
			List<SPObject> ancestorList1 = SQLPowerUtils.getAncestorList(spo1);
			List<SPObject> ancestorList2 = SQLPowerUtils.getAncestorList(spo2);

			SPObject previousAncestor = null;
			SPObject ancestor1 = spo1;
			SPObject ancestor2 = spo2;
			boolean compareWithAncestor = false;

			for (int i = 0, j = 0; i < ancestorList1.size() && j < ancestorList2.size(); i++, j++) {
				ancestor1 = ancestorList1.get(i);
				ancestor2 = ancestorList2.get(j);

				if (previousAncestor != null && !ancestor1.equals(ancestor2)) {
					compareWithAncestor = true;
					break;
				}

				previousAncestor = ancestor1;

			}

			if (!compareWithAncestor) {
				if (ancestorList1.size() < ancestorList2.size()) {
					ancestor1 = spo1;
					ancestor2 = ancestorList2.get(ancestorList1.size());
				} else if (ancestorList1.size() > ancestorList2.size()) {
					ancestor1 = ancestorList1.get(ancestorList2.size());
					ancestor2 = spo2;
				} else {
					ancestor1 = spo1;
					ancestor2 = spo2;
				}
			}

			int c;

			if (ancestor1.equals(ancestor2)) {
				c = ancestorList2.size() - ancestorList1.size();

			} else if (ancestor1.getClass() == ancestor2.getClass()) {
				List<? extends SPObject> siblings;
				if (previousAncestor instanceof SQLObject) {
					siblings = ((SQLObject) previousAncestor).getChildrenWithoutPopulating();
				} else {
					siblings = previousAncestor.getChildren();
				}
				
				int index1 =  siblings.indexOf(ancestor1);
				int index2 = siblings.indexOf(ancestor2);

				c = index2 - index1;

			} else {
				// XXX The comparator should really never reach
				// this else block. However in the case that it does, compare by UUID.

				c = uuid2.compareTo(uuid1);
			}

			return Integer.signum(c);
		}
	};
	
	/**
	 * {@link SPObject} removal buffer, mapping of {@link SPObject} UUIDs
	 * to their parents
	 */
	protected Map<String, String> objectsToRemove = new HashMap<String, String>();
	
	private void setPersistedProperties(
			Multimap<String, PersistedSPOProperty> persistedProperties) {
		this.persistedProperties = persistedProperties;
	}

	private void setPersistedObjects(List<PersistedSPObject> persistedObjects) {
		this.persistedObjects = persistedObjects;
		persistedObjectsMap.clear();
		for (PersistedSPObject pso : persistedObjects) {
			persistedObjectsMap.put(pso.getUUID(), pso);
		}
	}

	private void setObjectsToRemove(Map<String, String> objectsToRemove) {
		this.objectsToRemove = objectsToRemove;
	}

	/**
	 * This is the map we use to rollback object removal. The key is the UUID of the
	 * object removed for faster lookup if an object was moved instead of being
	 * directly removed. The map is ordered to keep the object order that they were
	 * removed in for rollback.
	 */
	private LinkedHashMap<String, RemovedObjectEntry> objectsToRemoveRollbackList = 
		new LinkedHashMap<String, RemovedObjectEntry>();

	/**
	 * This root object is used to find other objects by UUID by walking the
	 * descendant tree when an object is required.
	 */
	protected final SPObject root;
	
	/**
	 * Name of this persister (for debugging purposes).
	 */
	private final String name;

	private Thread currentThread;

	private boolean headingToWisconsin;

	/**
	 * A class that can convert a complex object into a basic representation
	 * that can be placed in a string and can also convert the string
	 * representation back into the complex object.
	 */
	protected final SessionPersisterSuperConverter converter;

	/**
	 * Creates a session persister that can update an object at or a descendant
	 * of the given root now. If the persist call involves an object that is
	 * outside of the scope of the root node and its descendant tree an
	 * exception will be thrown depending on the method called as the object
	 * will not be found.
	 * 
	 * @param name
	 *            The name of the persister. Helps to tell which persister is
	 *            performing actions.
	 * @param root
	 *            The root of the tree of {@link SPObject}s. The tree rooted at
	 *            this node will have objects added to it and properties changed
	 *            on nodes in this tree.
	 * @param converter
	 *            Used to convert objects given in strings to complex objects to
	 *            update the session with.
	 */
	public SPSessionPersister(String name, SPObject root, SessionPersisterSuperConverter converter) {
		this.name = name;
		this.root = root;
		this.converter = converter;
	}
	
	@Override
	public String toString() {
		return "SPSessionPersister \"" + name + "\"";
	}

	public void begin() throws SPPersistenceException {
		synchronized (getWorkspaceContainer()) {
			enforceThreadSafety();
			if (transactionCount == 0) {
				lookupCache.clear();
				converter.setUUIDCache(lookupCache);
			}
			transactionCount++;
			
			if (logger.isDebugEnabled()) {
				logger.debug("spsp.begin(); - transaction count : " + transactionCount);
			}
		}
	}

	public void commit() throws SPPersistenceException {
		synchronized (getWorkspaceContainer()) {
			enforceThreadSafety();
			
			if (logger.isDebugEnabled()) {
				logger.debug("spsp.commit(); - transaction count : " + transactionCount);
			}
			
			final SPObject workspace = getWorkspaceContainer().getWorkspace();
			synchronized (workspace) {
				try {
					workspace.setMagicEnabled(false);
					if (transactionCount == 0) {
						throw new SPPersistenceException(null,
							"Commit attempted while not in a transaction");
					}
	
					// Make sure the rollback lists are empty.
					objectsToRemoveRollbackList.clear();
					persistedObjectsRollbackList.clear();
					persistedPropertiesRollbackList.clear();
					
					if (transactionCount == 1) {
						if (logger.isDebugEnabled()) {
							logger.debug("Begin of commit phase...");
							logger.debug("Committing " + persistedObjects.size() + 
									" new objects, " + persistedProperties.size() + 
									" changes to different property names, and " + 
									objectsToRemove.size() + " objects are being removed.");
						}
						
						workspace.begin("Begin batch transaction...");
						commitRemovals();
						commitObjects();
						commitProperties();
						workspace.commit();
						
						if (logger.isDebugEnabled()) {
							logger.debug("...commit succeeded.");
						}
					}
						
				} catch (Throwable t) {
					logger.error("SPSessionPersister caught an exception while " +
							"performing a commit operation. Will try to rollback...", t);
					rollback();
					if (t instanceof SPPersistenceException) throw (SPPersistenceException) t;
					if (t instanceof FriendlyRuntimeSPPersistenceException) throw (FriendlyRuntimeSPPersistenceException) t;
					throw new SPPersistenceException(null, t);
				} finally {
					if (transactionCount > 0) {
						transactionCount--;
						if (transactionCount == 0) {
							objectsToRemove.clear();
							objectsToRemoveRollbackList.clear();
							persistedObjects.clear();
							persistedObjectsMap.clear();
							persistedObjectsRollbackList.clear();
							persistedProperties.clear();
							persistedPropertiesRollbackList.clear();
							lookupCache.clear();
							converter.removeUUIDCache();
							currentThread = null;
						}
					}
					
					workspace.setMagicEnabled(true);
				}
			}
		}
	}

	public void persistObject(String parentUUID, String type, String uuid,
			int index) throws SPPersistenceException {
		logger.debug("Persisting object " + uuid + " of type " + type + " as a child to " + parentUUID);
		synchronized (getWorkspaceContainer()) {
			enforceThreadSafety();
			
			if (logger.isDebugEnabled()) {
				logger.debug(String.format(
						"spsp.persistObject(\"%s\", \"%s\", \"%s\", %d);", parentUUID,
						type, uuid, index));
			}
			
			if (transactionCount == 0) {
				rollback();
				throw new SPPersistenceException("Cannot persist objects while outside " +
						"a transaction.");
			}
			SPObject objectToPersist = findByUuid(root, uuid, SPObject.class);
			if (exists(uuid) && objectToPersist.getClass() != root.getClass()) {
				rollback();
				throw new SPPersistenceException(uuid,
						"An SPObject with UUID " + uuid + " and type " + type
						+ " under parent with UUID " + parentUUID
						+ " already exists.\n"
						+ " The object exists in the root already? " + (objectToPersist != null) + "\n"
						+ " The persisted objects map contains keys: " + persistedObjectsMap.keySet() + "\n"
						+ " The persisted properties map contains values for this object? " 
						+ (persistedProperties.get(uuid) != null && !persistedProperties.get(uuid).isEmpty()) + "\n"
						+ " The removed set contains the object? " + (objectsToRemove.containsKey(uuid)));
			}

			PersistedSPObject pso = new PersistedSPObject(parentUUID,
					type, uuid, index);
			persistedObjects.add(pso);
			persistedObjectsMap.put(pso.getUUID(), pso);
		}
	}

	public void persistProperty(String uuid, String propertyName,
			DataType propertyType, Object oldValue, Object newValue)
			throws SPPersistenceException {
		if (transactionCount <= 0) {
			rollback();
			throw new SPPersistenceException(null, "Cannot persist objects while outside " +
					"a transaction.");
		}
		synchronized (getWorkspaceContainer()) {
			enforceThreadSafety();
			
			if (logger.isDebugEnabled()) {
				logger.debug(String.format(
						"spsp.persistProperty(\"%s\", \"%s\", DataType.%s, %s, %s);",
						uuid, propertyName, propertyType.name(), oldValue, newValue));
			}
			
			try {
				persistPropertyHelper(uuid, propertyName, propertyType, oldValue,
						newValue, godMode);
			} catch (SPPersistenceException e) {
				rollback();
				throw e;
			}
		}
	}

	public void persistProperty(String uuid, String propertyName,
			DataType propertyType, Object newValue)
			throws SPPersistenceException {
		logger.debug("Persisting property " + propertyName + ", changing to " + newValue);
		if (transactionCount <= 0) {
			rollback();
			throw new SPPersistenceException("Cannot persist objects while outside " +
					"a transaction.");
		}
		synchronized (getWorkspaceContainer()) {
			enforceThreadSafety();
			
			if (logger.isDebugEnabled()) {
				logger.debug(String.format(
						"spsp.persistProperty(\"%s\", \"%s\", DataType.%s, %s); // unconditional",
						uuid, propertyName, propertyType.name(),
						newValue));
			}
			
			try {
				if (newValue instanceof InputStream && !((InputStream) newValue).markSupported()) {
					newValue = new BufferedInputStream((InputStream) newValue);
				}
				persistPropertyHelper(uuid, propertyName, propertyType, newValue,
					newValue, true);
			} catch (SPPersistenceException e) {
				rollback();
				throw e;
			}
		}
	}
	
	/**
	 * Helper to persist a {@link SPObject} property given by its object
	 * UUID, property name, property type, expected old value, and new value.
	 * This can be done either conditionally or unconditionally based on which
	 * persistProperty method called this one.
	 * 
	 * @param uuid
	 *            The UUID of the {@link SPObject} to persist the property
	 *            upon
	 * @param propertyName
	 *            The property name
	 * @param propertyType
	 *            The property type
	 * @param oldValue
	 *            The expected old property value
	 * @param newValue
	 *            The new property value to persist
	 * @throws SPPersistenceException
	 *             Thrown if the property name is not known in this method.
	 */
	private void persistPropertyHelper(String uuid, String propertyName,
			DataType propertyType, Object oldValue, Object newValue,
			boolean unconditional) throws SPPersistenceException {
		
		if (!exists(uuid)) {
			throw new SPPersistenceException(uuid,
					"SPObject with UUID " + uuid + " could not be found." +
					" Was trying to set its property \"" + propertyName + "\" " +
					"to value \"" + newValue + "\".");
		}
		
		Object lastPropertyValueFound = null;
		
		for (PersistedSPOProperty persistedProperty : persistedProperties.get(uuid)) {
			if (propertyName.equals(persistedProperty.getPropertyName())) {
				lastPropertyValueFound = persistedProperty.getNewValue();
			}
		}
		
		Object propertyValue = null;
		SPObject spo = findByUuid(root, uuid,
				SPObject.class);
		
		if (lastPropertyValueFound != null) {
			if (!unconditional && !lastPropertyValueFound.equals(oldValue)) {
				throw new SPPersistenceException(uuid, "For property \""
						+ propertyName + "\", the expected property value \""
						+ oldValue
						+ "\" does not match with the actual property value \""
						+ lastPropertyValueFound + "\"");
			}
		} else {
			if (!unconditional && spo != null) {
				
				try {
					propertyValue = PersisterHelperFinder.findPersister(spo.getClass()).findProperty(spo, propertyName, converter);
				} catch (Exception e) {
					throw new SPPersistenceException("Could not find the persister helper for " + spo.getClass(), e);
				}

				if (propertyValue != null && oldValue == null) {
					throw new SPPersistenceException(uuid, "For property \""
							+ propertyName + "\" on SPObject of type "
							+ spo.getClass() + " and UUID + " + spo.getUUID()
							+ ", the expected property value \"" + oldValue
							+ "\" does not match with the actual property value \""
							+ propertyValue + "\"");
				}
//			} else if (!unconditional) {
//				throw new SPPersistenceException(uuid, "Could not find the object with id " + 
//						uuid + " to set property " + propertyValue);
			}
		}
		
		if (spo != null) {
			persistedProperties.put(uuid, new PersistedSPOProperty(uuid,
					propertyName, propertyType, propertyValue, newValue, unconditional));
		} else {
			persistedProperties.put(uuid, new PersistedSPOProperty(uuid,
					propertyName, propertyType, oldValue, newValue, unconditional));
		}
	}

	public void removeObject(String parentUUID, String uuid)
			throws SPPersistenceException {
		synchronized (getWorkspaceContainer()) {
			enforceThreadSafety();
			
			if (logger.isDebugEnabled()) {
				logger.debug(String.format("spsp.removeObject(\"%s\", \"%s\");",
						parentUUID, uuid));
			}
			
			if (transactionCount == 0) {
				logger.error("Remove Object attempted while not in a transaction. " +
						"Rollback initiated.");
				rollback();
				throw new SPPersistenceException(uuid,"Remove Object attempted while " +
						"not in a transaction. Rollback initiated.");
			}
			//Now that we allow remove persist calls on objects that are children of
			//objects being removed the persister needs to handle them.
			SPObject spo = findByUuid(root, uuid, SPObject.class);
			if (spo == null) {
			    rollback();
			    throw new SPPersistenceException(uuid,
			            "Cannot remove the SPObject with UUID " + uuid
			            + " from parent UUID " + parentUUID
			            + " as it does not exist.");
			}
			objectsToRemove.put(uuid, parentUUID);
		}
	}

	public void rollback() {
		rollback(false);
	}
	
	public void rollback(boolean force) {
		final SPObject workspace = getWorkspaceContainer().getWorkspace();
		synchronized (workspace) {
			if (headingToWisconsin) {
				return;
			}
			headingToWisconsin = true;
			if (!force) {
				enforceThreadSafety();
			}
			try {
				// We catch ANYTHING that comes out of here and rollback.
				// Some exceptions are Runtimes, so we must catch those too.
				workspace.setMagicEnabled(false);
				workspace.begin("Rolling back changes.");
				rollbackProperties();
				rollbackCreations();
				rollbackRemovals();
				workspace.commit("Done Rolling back");
			} catch (Exception e) {
				// This is a major fuck up. We could not rollback so now we must restore
				// by whatever means
				logger.fatal("First try at restore failed.", e);
				// TODO Monitor this
			} finally {
				workspace.setMagicEnabled(true);
				objectsToRemove.clear();
				objectsToRemoveRollbackList.clear();
				persistedObjects.clear();
				persistedObjectsMap.clear();
				persistedObjectsRollbackList.clear();
				persistedProperties.clear();
				persistedPropertiesRollbackList.clear();
				lookupCache.clear();
				converter.removeUUIDCache();
				transactionCount = 0;
				currentThread = null;
				headingToWisconsin = false;
				
				if (logger.isDebugEnabled()) {
					logger.debug("spsp.rollback(); - Killed all current transactions.");
				}
			}
		}
	}

	/**
	 * Commits the removal of persisted {@link SPObject}s
	 * 
	 * @throws SPPersistenceException
	 *             Thrown if an {@link SPObject} could not be removed from its
	 *             parent.
	 */
	private void commitRemovals() throws SPPersistenceException {
		Map<String, String> sortedObjectsToRemove = 
			new TreeMap<String, String>(removedObjectComparator);
		sortedObjectsToRemove.putAll(objectsToRemove);
		for (Map.Entry<String, String> removeEntry : sortedObjectsToRemove.entrySet()) {
			SPObject spo = findByUuid(root, removeEntry.getKey(),
					SPObject.class);
			
			//The ancestor of this object has been deleted by this transaction
			//already so we don't need to delete the object again.
			if (spo == null) {
			    boolean descendantRemoved = false;
			    for (RemovedObjectEntry removedRollbackEntry : objectsToRemoveRollbackList.values()) {
			        if (findByUuid(removedRollbackEntry.getRemovedChild(), removeEntry.getKey(), SPObject.class) != null) {
			            descendantRemoved = true;
			            break;
			        }
			    }
			    if (descendantRemoved) continue;
			}
			SPObject parent = findByUuid(root, removeEntry.getValue(), 
					SPObject.class);
			try {
				List<? extends SPObject> siblings;
				if (parent instanceof SQLObject) {
					siblings = ((SQLObject) parent).getChildrenWithoutPopulating();
				} else {
					siblings = parent.getChildren();
				}
				
				int index = siblings.indexOf(spo);
				index -= parent.childPositionOffset(spo.getClass());
				parent.removeChild(spo);
				// Add spo and hierarchy of its children to the remove-roll-back-list
				removeRollBackList(spo, parent, index);
				
				Set<String> removedKeys = SQLPowerUtils.buildIdMap(spo).keySet();
				for (String removedKey : removedKeys) {
					lookupCache.remove(removedKey);
				}
			} catch (IllegalArgumentException e) {
				throw new SPPersistenceException(removeEntry.getKey(), e);
			} catch (ObjectDependentException e) {
				throw new SPPersistenceException(removeEntry.getKey(), e);
			}
		}
		if (objectsToRemoveRollbackList.size() != objectsToRemove.size()) {
		    logger.warn("Skipped some objects");
		}
		objectsToRemove.clear();
	}
	
	private void removeRollBackList(SPObject object, SPObject parent, int index) {
		
		if (!object.getChildren().isEmpty()) {
			for (int i = object.getChildren().size()-1 ; i >= 0 ; i--) {
				removeRollBackList(object.getChildren().get(i), object, i);
			}
		}
		objectsToRemoveRollbackList.put(object.getUUID(),
				new RemovedObjectEntry(parent.getUUID(), object, index));
	}
	
	/**
	 * We do this because we override this in MMSessionPersister which needs to do some work after sorting
	 * the list, then calling commitSortObjects
	 * @throws SPPersistenceException
	 */
	protected void commitObjects() throws SPPersistenceException {
		Collections.sort(persistedObjects, persistedObjectComparator);
		commitSortedObjects();
	}
	
	/**
	 * Commits the persisted {@link SPObject}s
	 * 
	 * @throws SPPersistenceException
	 */
	protected void commitSortedObjects() throws SPPersistenceException {
		
		// importedKeys must be persisted after relationships. This is a bit of a ridiculous hack, so
		// we may want to change it!
		List<PersistedSPObject> rImportedKeys = new ArrayList<PersistedSPObject>();
		for (int i = 0; i < persistedObjects.size(); i++) {
			if (persistedObjects.get(i).getType().equals(SQLRelationship.SQLImportedKey.class.getName())) {
				rImportedKeys.add(persistedObjects.get(i));
			}
		}
		persistedObjects.removeAll(rImportedKeys);
		persistedObjects.addAll(rImportedKeys);
		// --------------------------------------------------------------------------------------------
		
		for (PersistedSPObject pso : persistedObjects) {
		    logger.debug("Persisting " + pso);
			if (pso.isLoaded())
				continue;
			SPObject parent = findByUuid(root, pso.getParentUUID(), 
					SPObject.class);
			SPObject spo = null;
			
			if (parent == null && pso.getType().equals(root.getClass().getName())) {
				lookupCache.clear();
                refreshRootNode(pso);
                lookupCache.clear();
                continue;
            } else if (parent == null) {
                throw new IllegalStateException("Missing parent with uuid " + pso.getParentUUID() + 
                        " when trying to load " + pso);
            }
			
	         //XXX This was initially part of the fix for bug 2326 but is a larger problem
            //that is we need to get the children of a SQLObject without populating them
            //for the persister layer but other than this case we should not need to let
            //other places get at the children of SQLObjects in this manner.
            Class<? extends SPObject> parentAllowedChildType;
            try {
                parentAllowedChildType = PersisterUtils.getParentAllowedChildType(pso.getType(), 
                        parent.getClass().getName());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            List<? extends SPObject> siblings;
            if (parent instanceof SQLObject) {
                siblings = ((SQLObject) parent).getChildrenWithoutPopulating(parentAllowedChildType);
            } else {
                siblings = parent.getChildren(parentAllowedChildType);
            }

            if (objectsToRemoveRollbackList.keySet().contains(parent.getUUID())) {
                for (SPObject sibling : siblings) {
                    if (sibling.getUUID().equals(pso.getUUID())) {
                        spo = sibling;
                        break;
                    }
                }
            }
            //Ancestor list does not contain the node passed in.
            if (spo == null) {
                for (SPObject ancestor : SQLPowerUtils.getAncestorList(parent)) {
                    if (objectsToRemoveRollbackList.keySet().contains(ancestor.getUUID())) {
                        for (SPObject sibling : siblings) {
                            if (sibling.getUUID().equals(pso.getUUID())) {
                                spo = sibling;
                                break;
                            }
                        }
                        break;
                    }
                }
            }
			
			if (spo != null) {
			    //Case for the ancestor was moved and on adding the ancestor this object
			    //was essentially re-added as well as it was never technically removed.
			    //This is done above.
			    
			    removeFinalPersistProperties(pso);
			    persistedObjectsRollbackList.add(
			            new PersistedObjectEntry(
			                    parent.getUUID(), 
			                    spo.getUUID()));
			    continue;
			} else if (objectsToRemoveRollbackList.get(pso.getUUID()) != null) {
			    //Object was removed and added back in. This is the current way a 'move' of a 
			    //child object is represented in the system. We may want to revisit this and
			    //look at creating a different command, or special property change of 'move'.
			    spo = objectsToRemoveRollbackList.get(pso.getUUID()).getRemovedChild();

			    removeFinalPersistProperties(pso);
			} else if (parent instanceof SQLObject && populateSQLObject((SQLObject) parent)) {
			    continue;
			} else {
				try {
					spo = PersisterHelperFinder.findPersister(pso.getType()).commitObject(pso, persistedProperties, 
							persistedObjects, converter);
				} catch (Exception ex) {
					throw new SPPersistenceException("Could not find the persister helper for " + pso.getType(), ex);
				}
			}
			if (spo != null) {
				SPListener removeChildOnAddListener = new SPListener() {
					public void propertyChanged(PropertyChangeEvent arg0) {
						//do nothing
					}
					public void childRemoved(SPChildEvent e) {
						objectsToRemoveRollbackList.put(e.getChild().getUUID(), 
								new RemovedObjectEntry(e.getSource().getUUID(), 
										e.getChild(), e.getIndex()));
					}
					public void childAdded(SPChildEvent e) {
						//do nothing
					}
					public void transactionStarted(TransactionEvent e) {
						//do nothing
					}
					public void transactionRollback(TransactionEvent e) {
						//do nothing
					}
					public void transactionEnded(TransactionEvent e) {
						//do nothing
					}
				};
				parent.addSPListener(removeChildOnAddListener);
				
				// FIXME Terrible hack, see bug 2326
				//XXX This appears to shuffle columns up which will cause columns to be rearranged
				parent.addChild(spo, Math.min(pso.getIndex(), siblings.size()));
				
				parent.removeSPListener(removeChildOnAddListener);
				persistedObjectsRollbackList.add(
					new PersistedObjectEntry(
						parent.getUUID(), 
						spo.getUUID()));
				lookupCache.putAll(SQLPowerUtils.buildIdMap(spo));
			}
		}
		persistedObjects.clear();
		persistedObjectsMap.clear();
	}

    /**
     * Helper method for {@link #commitObjects()}. Removes the persist property
     * calls that are final to the given object. For the case where the object
     * was moved and is not actually being recreated but just moved in-tact.
     */
    private void removeFinalPersistProperties(PersistedSPObject pso)
            throws SPPersistenceException {
        List<String> persistedPropNames;
        try {
            persistedPropNames = PersisterHelperFinder.findPersister(pso.getType()).getPersistedProperties();
        } catch (Exception ex) {
            throw new SPPersistenceException("Could not find the persister helper for " + pso.getType(), ex);
        }
        List<PersistedSPOProperty> propertiesToRemove = new ArrayList<PersistedSPOProperty>();
        for (PersistedSPOProperty spoProperty : persistedProperties.get(pso.getUUID())) {
            if (!persistedPropNames.contains(spoProperty.getPropertyName())) {
                propertiesToRemove.add(spoProperty);
            }
        }
        for (PersistedSPOProperty spoProperty : propertiesToRemove) {
            persistedProperties.get(pso.getUUID()).remove(spoProperty);
            //Using the new value as this property is a final field.
            persistedPropertiesRollbackList.add(new PersistedPropertiesEntry(
                    spoProperty.getUUID(), spoProperty.getPropertyName(), 
                    spoProperty.getDataType(), spoProperty.getNewValue()));
        }
    }

    /**
     * This is a special corner case for populating SQLObjects. If an object is
     * populating, the objects must be added in a special way so the populate
     * flags are set when objects are notified that the population has happened.
     * This prevents the problem of the DBTreeModel from calling populate when
     * an object is added and an infinite loop occurring.
     * 
     * @param parent
     *            The parent that is being added to the model. This may or may
     *            not have been populated.
     * @return True if the object was populated and its children do not need to
     *         be added to the object again. False otherwise.
     */
	private boolean populateSQLObject(SQLObject parent) throws SPPersistenceException {
	    //This gross chunk of code essentially simulates, and uses part of, the populate
	    //method of classes that have the populate method defined.
	    
	    if (!(parent instanceof SQLTable || parent instanceof SQLDatabase || 
	            parent instanceof SQLSchema || parent instanceof SQLCatalog)) {
	        return false;
	    }
	    List<PersistedSPObject> childrenForPopulate = new ArrayList<PersistedSPObject>();
	    if (parent instanceof SQLTable) {
	        //For SQLTable
	        Boolean columnsPopulated = PersisterUtils.findPersistedBooleanProperty(persistedProperties, parent.getUUID(), "columnsPopulated");
	        Boolean indicesPopulated = PersisterUtils.findPersistedBooleanProperty(persistedProperties, parent.getUUID(), "indicesPopulated");
	        Boolean exportedKeysPopulated = PersisterUtils.findPersistedBooleanProperty(persistedProperties, parent.getUUID(), "exportedKeysPopulated");
	        Boolean importedKeysPopulated = PersisterUtils.findPersistedBooleanProperty(persistedProperties, parent.getUUID(), "importedKeysPopulated");
	        final SQLTable table = (SQLTable) parent;
            if (!table.isColumnsPopulated() && columnsPopulated != null && columnsPopulated) {
                List<PersistedSPObject> columnsForPopulate = new ArrayList<PersistedSPObject>();
	            for (PersistedSPObject spo : persistedObjects) {
	                if (!spo.isLoaded() && spo.getParentUUID().equals(parent.getUUID())
	                        && spo.getType().equals(SQLColumn.class.getName())) {
	                    columnsForPopulate.add(spo);
	                }
	            }
	            List<SQLObject> children = new ArrayList<SQLObject>();
	            for (PersistedSPObject pso : columnsForPopulate) {
	                try {
	                    SQLObject spo = (SQLObject) PersisterHelperFinder.findPersister(pso.getType()).commitObject(pso, persistedProperties, 
	                            persistedObjects, converter);
	                    children.add(spo);
	                } catch (Exception ex) {
	                    throw new SPPersistenceException("Could not find the persister helper for " + pso.getType(), ex);
	                }
	            }

	            SQLObjectUtils.populateChildrenWithList(parent, children);
	            childrenForPopulate.addAll(columnsForPopulate);
	            if (columnsForPopulate.isEmpty()) {
	                //Need to be set even if no columns exist so the indices and relationships
	                //don't explode
	                table.setColumnsPopulated(columnsPopulated);
	            }
	        }
	        
	        if (!table.isIndicesPopulated() && indicesPopulated != null && indicesPopulated) {
	            List<PersistedSPObject> indicesForPopulate = new ArrayList<PersistedSPObject>();
	            for (PersistedSPObject spo : persistedObjects) {
	                if (!spo.isLoaded() && spo.getParentUUID().equals(parent.getUUID())
	                        && spo.getType().equals(SQLIndex.class.getName())) {
	                    indicesForPopulate.add(spo);
	                }
	            }
	            List<SQLObject> children = new ArrayList<SQLObject>();
	            for (PersistedSPObject pso : indicesForPopulate) {
	                try {
	                    SQLObject spo = (SQLObject) PersisterHelperFinder.findPersister(pso.getType()).commitObject(pso, persistedProperties, 
	                            persistedObjects, converter);
	                    children.add(spo);
	                } catch (Exception ex) {
	                    throw new SPPersistenceException("Could not find the persister helper for " + pso.getType(), ex);
	                }
	            }

	            SQLObjectUtils.populateChildrenWithList(parent, children);
	            childrenForPopulate.addAll(indicesForPopulate);
	            if (indicesForPopulate.isEmpty()) {
	                //Need to be set even if no columns exist so the relationships don't explode
	                table.setIndicesPopulated(indicesPopulated);
	            }
	        } 
	        
	        if (!table.isExportedKeysPopulated() && exportedKeysPopulated != null && exportedKeysPopulated) {
	            List<PersistedSPObject> exportedKeysForPopulate = new ArrayList<PersistedSPObject>();
	            for (PersistedSPObject spo : persistedObjects) {
	                if (!spo.isLoaded() && spo.getParentUUID().equals(parent.getUUID())
	                        && spo.getType().equals(SQLRelationship.class.getName())) {
	                    exportedKeysForPopulate.add(spo);
	                }
	            }
	            List<SQLObject> children = new ArrayList<SQLObject>();
	            for (PersistedSPObject pso : exportedKeysForPopulate) {
	                try {
	                    SQLObject spo = (SQLObject) PersisterHelperFinder.findPersister(pso.getType()).commitObject(pso, persistedProperties, 
	                            persistedObjects, converter);
	                    children.add(spo);
	                } catch (Exception ex) {
	                    throw new SPPersistenceException("Could not find the persister helper for " + pso.getType(), ex);
	                }
	            }

	            SQLObjectUtils.populateChildrenWithList(parent, children);
	            childrenForPopulate.addAll(exportedKeysForPopulate);
	        }

			// SQLImportedKeys do not need to be added to parent in any special
			// way, and in fact must wait until all relationships have been
			// added.

	    } else {
	        //For all SQLObjects that are not SQLTable
	        Boolean populated = PersisterUtils.findPersistedBooleanProperty(persistedProperties, parent.getUUID(), "populated");
	        if (!parent.isPopulated() && populated != null && populated) {
	            for (PersistedSPObject spo : persistedObjects) {
	                if (!spo.isLoaded() && spo.getParentUUID().equals(parent.getUUID())) {
	                    childrenForPopulate.add(spo);
	                }
	            }
	        }
	        List<SQLObject> children = new ArrayList<SQLObject>();
	        for (PersistedSPObject pso : childrenForPopulate) {
	            try {
	                SQLObject spo = (SQLObject) PersisterHelperFinder.findPersister(pso.getType()).commitObject(pso, persistedProperties, 
	                        persistedObjects, converter);
	                children.add(spo);
	            } catch (Exception ex) {
	                throw new SPPersistenceException("Could not find the persister helper for " + pso.getType(), ex);
	            }
	        }

	        SQLObjectUtils.populateChildrenWithList(parent, children);
	    }
	    
	    if (!childrenForPopulate.isEmpty()) {
	        
	        //TODO Assert children are in their correct location.
	        for (PersistedSPObject spo : childrenForPopulate) {
	            persistedObjectsRollbackList.add(
	                    new PersistedObjectEntry(
	                            parent.getUUID(), 
	                            spo.getUUID()));
	        }
	        return true;
	    }
	    return false;
	}

	/**
	 * Called when we get a persist object of the root node. This will reset the
	 * object tree and update the root node and any final children within it to
	 * values in the persisted objects and properties list.
	 * 
	 * @param pso
	 *            The persist object call that would create the root object.
	 */
	protected abstract void refreshRootNode(PersistedSPObject pso);

	/**
	 * Commits the persisted {@link SPObject} property values
	 * 
	 * @throws SPPersistenceException
	 *             Thrown if an invalid SPObject type has been persisted into
	 *             storage. This theoretically should not occur.
	 */
	private void commitProperties() throws SPPersistenceException {
		SPObject spo;
		String propertyName;
		Object newValue;

		for (String uuid : persistedProperties.keySet()) {
			spo = findByUuid(root, uuid, SPObject.class);
			if (spo == null) {
				throw new IllegalStateException("Couldn't locate object "
						+ uuid + " in session");
			}

			for (PersistedSPOProperty persistedProperty : persistedProperties.get(uuid)) {
				
				propertyName = persistedProperty.getPropertyName();
				newValue = persistedProperty.getNewValue();
				
				if (logger.isDebugEnabled()) {
					logger.debug("Applying property " + propertyName + " to " + 
							spo.getClass().getSimpleName() + " at " + spo.getUUID());
				}
				
				try {
					PersisterHelperFinder.findPersister(spo.getClass()).commitProperty(
							spo, propertyName, newValue, persistedProperty.getDataType(), converter);
				} catch (Exception e) {
					throw new SPPersistenceException("Could not find the persister helper for " + spo.getClass(), e);
				}
				
				persistedPropertiesRollbackList.add(
					new PersistedPropertiesEntry(
						spo.getUUID(), //The uuid can be changed so using the currently set one.
						persistedProperty.getPropertyName(), 
						persistedProperty.getDataType(), 
						persistedProperty.getOldValue()));
			}
		}
		persistedProperties.clear();
	}
	
	/**
	 * Rolls back the removal of persisted {@link SPObject}s
	 */
	private void rollbackRemovals() {
		// We must rollback in the inverse order the operations were performed.
	    List<RemovedObjectEntry> removedObjects = 
	        new ArrayList<RemovedObjectEntry>(objectsToRemoveRollbackList.values());
		Collections.reverse(removedObjects);
		for (RemovedObjectEntry entry : removedObjects) {
			final String parentUuid = entry.getParentUUID();
			final SPObject objectToRestore = entry.getRemovedChild();
			final int index = entry.getIndex();
			final SPObject parent = findByUuid(root, parentUuid, SPObject.class);
			try {
				if (!parent.getChildren().contains(objectToRestore)) {
					parent.addChild(objectToRestore, index);
				}
			} catch (Throwable t) {
				// Keep going. We need to rollback as much as we can.
				logger.error("Cannot rollback " + entry.getRemovedChild() + " child removal", t);
			}
		}
	}

	/**
	 * Rolls back the changed properties of persisted {@link SPObject}s.
	 */
	private void rollbackProperties() {
		Collections.reverse(persistedPropertiesRollbackList);
		Set<String> objectCreationRollbackUUIDs = new HashSet<String>();
		for (PersistedObjectEntry entry : persistedObjectsRollbackList) {
			objectCreationRollbackUUIDs.add(entry.getChildId());
		}
		for (PersistedPropertiesEntry entry : persistedPropertiesRollbackList) {
			try {
				final String parentUUID = entry.getUUID();
				//These objects will be removed and we cannot roll back final properties so we
				//will skip them.
				if (objectCreationRollbackUUIDs.contains(parentUUID)) continue;
				final String propertyName = entry.getPropertyName();
				final Object rollbackValue = entry.getRollbackValue();
				final SPObject parent = findByUuid(root, parentUUID, SPObject.class);
				if (parent != null) {
					PersisterHelperFinder.findPersister(parent.getClass()).commitProperty(
							parent, propertyName, rollbackValue, entry.getPropertyType(), converter);
				}
			} catch (Throwable t) {
				// Keep going. We need to rollback as much as we can.
				logger.error("Cannot rollback change to " + entry.getPropertyName() + 
						" to value " + entry.getRollbackValue(), t);
			}
		}
	}

	/**
	 * Rolls back the created persisted {@link SPObject}s by removing them from
	 * their parents.
	 */
	private void rollbackCreations() {
		Collections.reverse(persistedObjectsRollbackList);
		for (PersistedObjectEntry entry : persistedObjectsRollbackList) {
			try {
				// We need to verify if the entry specifies a parent.
				// Root objects don't have parents so we can't remove them really...
				if (entry.getParentId() != null) {
					final SPObject parent = findByUuid(root, 
							entry.getParentId(), SPObject.class);
					final SPObject child = findByUuid(root, 
							entry.getChildId(), SPObject.class);
					parent.removeChild(child);
				}
			} catch (Throwable t) {
				// Keep going. We need to rollback as much as we can.
				logger.error("Cannot rollback " + entry.getChildId() + " child creation", t);
			}
		}
	}
	
	/**
	 * Checks to see if a {@link SPObject} with a certain UUID exists
	 * 
	 * @param uuid
	 *            The UUID to search for
	 * @return Whether or not the {@link SPObject} exists
	 */
	private boolean exists(String uuid) {
		if (persistedObjectsMap.get(uuid) != null) return true;
        SPObject spo = findByUuid(root, uuid, SPObject.class);
        if (spo != null) {
            List<SPObject> ancestors = SQLPowerUtils.getAncestorList(spo);
            ancestors.add(spo);
            for (SPObject ancestor : ancestors) {
                if (objectsToRemove.containsKey(ancestor.getUUID())) {
                    return false;
                }
            }
            return true;
        }
		return false;
	}
	
	public boolean isHeadingToWisconsin() {
		return headingToWisconsin;
	}

	/**
	 * This is part of the 'echo-cancellation' system to notify any
	 * {@link WorkspacePersisterListener} listening to the same session to
	 * ignore modifications to that session.
	 */
	public boolean isUpdatingWorkspace() {
		if (transactionCount > 0) {
			return true;
		} else if (transactionCount == 0) {
			return false;
		} else {
			rollback();
			throw new IllegalStateException("This persister is in an illegal state. " +
					"transactionCount was :" + transactionCount);
		}
	}

	/**
	 * Enforces thread safety.
	 */
	public void enforceThreadSafety() {
		if (currentThread == null) {
			currentThread = Thread.currentThread();
		} else {
			if (currentThread != Thread.currentThread()) {
				rollback(true);
				throw new RuntimeException("A call from two different threads was detected. " +
						"Callers of a sessionPersister should synchronize prior to " +
						"opening transactions. The thread " + Thread.currentThread().getName() + 
						" tried to access the persister while the thread " + currentThread.getName() + 
						" was already using it.");
			}
		}
	}

	/**
	 * Turns this persister as a preacher of the truth and always the truth. All
	 * calls are turned into unconditionals.
	 * 
	 * @param godMode
	 *            True or False
	 */
	public void setGodMode(boolean godMode) {
		this.godMode = godMode;
	}
	
	private void setObjectsToRemoveRollbackList(
			LinkedHashMap<String, RemovedObjectEntry> objectsToRemoveRollbackList) {
		this.objectsToRemoveRollbackList = objectsToRemoveRollbackList;
	}
	
	private void setPersistedObjectsRollbackList(
			List<PersistedObjectEntry> persistedObjectsRollbackList) {
		this.persistedObjectsRollbackList = persistedObjectsRollbackList;
	}
	
	private void setPersistedPropertiesRollbackList(
			List<PersistedPropertiesEntry> persistedPropertiesRollbackList) {
		this.persistedPropertiesRollbackList = persistedPropertiesRollbackList;
	}

	/**
	 * Returns an ancestor list of {@link PersistedSPObject}s from a given child
	 * {@link PersistedSPObject}. The list holds persist objects starting from
	 * the root and going to the parent's persist call. This list does not hold
	 * the persist call for the persist passed in. This list holds persist calls
	 * that are outside of the current transaction of persist calls, it will
	 * create persist calls as necessary.
	 */
	private List<PersistedSPObject> buildAncestorListFromPersistedObjects(PersistedSPObject child) {
		List<PersistedSPObject> resultList = new ArrayList<PersistedSPObject>();

		// Iterate through list of persisted SPObjects to build an ancestor
		// list from objects that do not exist in the root yet.
		String uuid = child.getParentUUID();
		PersistedSPObject pso;
		while ((pso = persistedObjectsMap.get(uuid)) != null) {
			resultList.add(0, pso);
			uuid = pso.getParentUUID();
		}

		SPObject spo = findByUuid(root, uuid, SPObject.class);
		List<PersistedSPObject> existingAncestorList = new ArrayList<PersistedSPObject>();
		if (spo != null) {
			existingAncestorList.add(0, createPersistedObjectFromSPObject(spo));
			List<SPObject> ancestorList = SQLPowerUtils.getAncestorList(spo);
			Collections.reverse(ancestorList);

			for (SPObject ancestor : ancestorList) {
				existingAncestorList.add(0, createPersistedObjectFromSPObject(ancestor));
			}
		}
		resultList.addAll(0, existingAncestorList);
		
		return resultList;
	}
	
	/**
	 * Returns a new {@link PersistedSPObject} based on a given {@link SPObject}.
	 */
	private PersistedSPObject createPersistedObjectFromSPObject(SPObject spo) {
		String parentUUID = null;
		int index = 0;
		
		SPObject parent = spo.getParent();
		if (parent != null) {
			parentUUID = parent.getUUID();
			
			List<? extends SPObject> siblings;
			Class<? extends SPObject> siblingClass;
			try {
				siblingClass = PersisterUtils.getParentAllowedChildType(spo.getClass().getName(), parent.getClass().getName());
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			if (parent instanceof SQLObject) {
				siblings = ((SQLObject) parent).getChildrenWithoutPopulating(siblingClass);
			} else {
				siblings = parent.getChildren(siblingClass);
			}
			index = siblings.indexOf(spo);
		}
		
		return new PersistedSPObject(parentUUID, spo.getClass().getName(), 
				spo.getUUID(), index);
	}

	public void setWorkspaceContainer(WorkspaceContainer workspaceContainer) {
		this.workspaceContainer = workspaceContainer;
	}

	public WorkspaceContainer getWorkspaceContainer() {
		return workspaceContainer;
	}

	/**
	 * Undoes the persist calls on the root object that are passed into this
	 * method. This allows the persister listener to use the session persister's
	 * roll back method. Nothing will be done on the persist of a root node when
	 * rolling back. If we are rolling back a set of persist calls with the root
	 * node then a load or refresh failed and there is no simple way to go back.
	 * 
	 * @param root
	 *            The root of the object tree. The object tree will be searched
	 *            for objects with corresponding UUIDs and will be updated based
	 *            on the persist calls.
	 * @param creations
	 *            A set of persist object calls that created objects that needs
	 *            to be reversed.
	 * @param properties
	 *            A set of persist property calls that updated objects that need
	 *            to be reversed.
	 * @param removals
	 *            An ordered set of remove object calls that need objects to be added
	 *            back in. The key of each entry is the UUID of the object that was removed.
	 * @param converter
	 *            An object converter that can convert the simple property types
	 *            in the persist property calls to full objects to update the
	 *            object tree.
	 * @throws SPPersistenceException
	 */
	public static void undoForSession(
			SPObject root,
			List<PersistedObjectEntry> creations,
			List<PersistedPropertiesEntry> properties,
			LinkedHashMap<String, RemovedObjectEntry> removals,
			SessionPersisterSuperConverter converter) throws SPPersistenceException
	{
		SPSessionPersister persister = new SPSessionPersister("undoer", root, converter) {
			@Override
			protected void refreshRootNode(PersistedSPObject pso) {
				//do nothing for refresh.
			}
		};
		persister.setWorkspaceContainer(root.getWorkspaceContainer());
		persister.setGodMode(true);
		persister.setObjectsToRemoveRollbackList(removals);
		persister.setPersistedObjectsRollbackList(creations);
		persister.setPersistedPropertiesRollbackList(properties);
		persister.rollback(true);
	}
	
	public static void undoForSession(SPObject root,
			List<PersistedSPObject> creations,
			Multimap<String, PersistedSPOProperty> properties,
			List<RemovedObjectEntry> removals,
			SessionPersisterSuperConverter converter) throws SPPersistenceException {
		
		List<PersistedObjectEntry> c = new LinkedList<PersistedObjectEntry>();
		List<PersistedPropertiesEntry> p = new LinkedList<PersistedPropertiesEntry>();
		LinkedHashMap<String, RemovedObjectEntry> r = new LinkedHashMap<String, RemovedObjectEntry>();
		
		for (PersistedSPObject pso : creations) {
			c.add(new PersistedObjectEntry(pso.getParentUUID(), pso.getUUID()));
		}
		for (PersistedSPOProperty property : properties.values()) {
			p.add(new PersistedPropertiesEntry(property.getUUID(), property.getPropertyName(), property.getDataType(), property.getOldValue()));
		}
		for (RemovedObjectEntry removal : removals) {
			r.put(removal.getRemovedChild().getUUID(), removal);
		}
		
		undoForSession(root, c, p, r, converter);
	}

	public static void redoForSession(
			SPObject root,
			List<PersistedSPObject> creations,
			Multimap<String, PersistedSPOProperty> properties,
			List<RemovedObjectEntry> removals,
			SessionPersisterSuperConverter converter) throws SPPersistenceException
	{
		SPSessionPersister persister = new SPSessionPersister("redoer", root, converter) {
			@Override
			protected void refreshRootNode(PersistedSPObject pso) {
				//do nothing for refresh.
			}
		};
		
		Map<String, String> objToRmv = new TreeMap<String, String>(persister.removedObjectComparator);
		for (RemovedObjectEntry roe : removals) {
			objToRmv.put(roe.getRemovedChild().getUUID(), roe.getParentUUID());
		}
		
		persister.setWorkspaceContainer(root.getWorkspaceContainer());
		persister.setGodMode(true);
		persister.setPersistedObjects(creations);
		persister.setPersistedProperties(properties);
		persister.setObjectsToRemove(objToRmv);

		persister.begin();
		persister.commit();
	}
	
	protected void clearUUIDCache() {
		lookupCache.clear();
	}
	
	protected <T extends SPObject> T findByUuid(SPObject root, String uuid, Class<T> expectedType) {
		if (lookupCache.get(uuid) != null) {
			SPObject foundObject = lookupCache.get(uuid);
			if (!expectedType.isAssignableFrom(foundObject.getClass())) {
				throw new IllegalStateException("The object " + foundObject + " is not of type " + 
						expectedType + " from the cache.");
			}
			return expectedType.cast(foundObject);
		}
		if (uuid == null || uuid.trim().isEmpty() || !lookupCache.isEmpty()) return null;
		
		lookupCache.putAll(SQLPowerUtils.buildIdMap(this.root));
		return expectedType.cast(lookupCache.get(uuid));
	}
}
