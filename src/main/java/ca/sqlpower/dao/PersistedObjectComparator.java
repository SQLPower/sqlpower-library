/*
 * Copyright (c) 2013, SQL Power Group Inc.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ca.sqlpower.object.SPObject;
import ca.sqlpower.util.SQLPowerUtils;

/**
 * This comparator is used mainly by the {@link SPSessionPersister} to provide
 * an ordering to how persist calls for objects should be acted on. The order we
 * want the persists to appear on is dictated by the class's allowedChildTypes
 * static variable. This persister will then go depth first in creating the
 * objects, this way later objects have complete objects created to make
 * reference to. If a different ordering is needed due to other dependencies
 * this comparator can be extended.
 */
public class PersistedObjectComparator implements Comparator<PersistedSPObject> {
	
	/**
	 * The root object of the object tree we are persisting to. This root object
	 * will be used to inspect how the tree currently looks to know the order to
	 * persist in.
	 */
	private final SPObject root;
	
	/**
	 * This maps the objects being created by the id of the object to be
	 * created. We will use this collection of persisted object to understand
	 * how the objects being compared relate to each other.
	 * <p>
	 * We could just include the list of persisted objects but the mapping helps
	 * improve performance.
	 */
	private final Map<String, PersistedSPObject> persistedObjectsMap;
	
	/**
	 * This cache provides a quick way to look up objects by UUID.
	 */
	private final Map<String, SPObject> lookupCache;

	public PersistedObjectComparator(SPObject root, Map<String, PersistedSPObject> persistedObjectsMap) {
		this.root = root;
		this.persistedObjectsMap = persistedObjectsMap;
		lookupCache = new HashMap<String, SPObject>(SQLPowerUtils.buildIdMap(this.root));
	}

	// If the two objects being compared are of the same type and are
	// children of the same parent, the one with the lower index should go
	// first. Otherwise, the one with the smaller ancestor tree should go first
	// (e.g. Report should go before Page).
	@Override
	public int compare(PersistedSPObject o1, PersistedSPObject o2) {

		if (o1.getParentUUID() == null && o2.getParentUUID() == null) {
			return 0;
		} else if (o1.getParentUUID() == null) {
			return -1;
		} else if (o2.getParentUUID() == null) {
			return 1;
		}

		if (o1.getParentUUID().equals(o2.getParentUUID()) && 
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

		//This is the shared parent of ancestor1 and ancestor2
		PersistedSPObject sharedAncestor = null;
		//This is the ancestor to o1 which is the last ancestor different to o1 before we reach a shared parent.
		PersistedSPObject ancestor1 = null;
		//This is the ancestor to o2 which is the last ancestor different to o2 before we reach a shared parent.
		PersistedSPObject ancestor2 = null;
		//If true these ancestor objects should be compared. If false we made it to the end of one of the lists.
		boolean compareWithAncestor = false;

		//Looking at the highest ancestor that is different in the list
		for (int i = 0, j = 0; i < ancestorList1.size() && j < ancestorList2.size(); i++, j++) {
			ancestor1 = ancestorList1.get(i);
			ancestor2 = ancestorList2.get(j);

			if (sharedAncestor != null && !ancestor1.equals(ancestor2)) {
				compareWithAncestor = true;
				break;
			}

			sharedAncestor = ancestor1;
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

		return compareAncestors(o1, o2, ancestorList1, ancestorList2,
				sharedAncestor, ancestor1, ancestor2);
	}

	/**
	 * This method will compare the ancestors of the objects we are comparing
	 * instead of comparing the objects themselves. The ancestors are being
	 * compared instead of the objects because the objects are so distant in the
	 * tree that we can't compare them directly.
	 * 
	 * @param o1
	 *            The first object we are comparing.
	 * @param o2
	 *            The second object we are comparing.
	 * @param ancestorList1
	 *            The list of ancestors for the first object. The root is at the
	 *            start of the list and the parent of the object is at the end.
	 * @param ancestorList2
	 *            The list of ancestors for the second object. The root is at
	 *            the start of the list and the parent of the object is at the
	 *            end.
	 * @param sharedAncestor
	 *            This is the parent of ancestor1 and ancestor2. It is the
	 *            closest ancestor to o1 and o2 that they share. This value can
	 *            be null if o1 and o2 are not in the same tree.
	 * @param ancestor1
	 *            The last ancestor of o1 which is not part of o2's ancestor
	 *            tree.
	 * @param ancestor2
	 *            The last ancestor of o2 which is not part of o1's ancestor
	 *            tree.
	 * @return -1, 0, or 1 if o1 is less than, equal to, or greater than o2
	 *         respectively.
	 */
	protected int compareAncestors(PersistedSPObject o1, PersistedSPObject o2,
			List<PersistedSPObject> ancestorList1,
			List<PersistedSPObject> ancestorList2,
			PersistedSPObject sharedAncestor, PersistedSPObject ancestor1,
			PersistedSPObject ancestor2) {
		int c;

		if (ancestor1.getType().equals(ancestor2.getType())) {
			c = ancestor1.getIndex() - ancestor2.getIndex();
		} else {
			if (sharedAncestor == null) {
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
						sharedAncestor.getType());
				if (ancestorType1Index == -1) {
					throw new IllegalStateException("Allowed child types for " + sharedAncestor + 
							" does not contain " + ancestor1);
				}
				int ancestorType2Index = PersisterUtils.getTypePosition(ancestor2.getType(),
						sharedAncestor.getType());
				if (ancestorType2Index == -1) {
					throw new IllegalStateException("Allowed child types for " + sharedAncestor + 
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

		SPObject spo = findByUuid(uuid, SPObject.class);
		List<PersistedSPObject> existingAncestorList = new ArrayList<PersistedSPObject>();
		if (spo != null) {
			existingAncestorList.add(0, PersisterUtils.createPersistedObjectFromSPObject(spo));
			List<SPObject> ancestorList = SQLPowerUtils.getAncestorList(spo);
			Collections.reverse(ancestorList);

			for (SPObject ancestor : ancestorList) {
				existingAncestorList.add(0, PersisterUtils.createPersistedObjectFromSPObject(ancestor));
			}
		}
		resultList.addAll(0, existingAncestorList);
		
		return resultList;
	}
	
	/**
	 * This section is originally taken from the SPSessionPersister and we may
	 * need to refactor this to prevent code duplication.
	 * <p>
	 * This uuid lookup uses the cache created in the constructor of this class
	 * to find objects rather than iterating through the tree of objects.
	 */
	protected <T extends SPObject> T findByUuid(String uuid, Class<T> expectedType) {
		if (lookupCache.get(uuid) != null) {
			SPObject foundObject = lookupCache.get(uuid);
			if (!expectedType.isAssignableFrom(foundObject.getClass())) {
				throw new IllegalStateException("The object " + foundObject + " is not of type " + 
						expectedType + " from the cache.");
			}
			return expectedType.cast(foundObject);
		}
		return null;
	}
}
