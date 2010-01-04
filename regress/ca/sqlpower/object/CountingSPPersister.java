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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ca.sqlpower.dao.PersistedSPOProperty;
import ca.sqlpower.dao.PersistedSPObject;
import ca.sqlpower.dao.RemovedSPObject;
import ca.sqlpower.dao.SPPersistenceException;
import ca.sqlpower.dao.SPPersister;

/**
 * Counts the number of times each persister method is called.
 * Each call can also be retrieved from respective methods. 
 */
public class CountingSPPersister implements SPPersister {
	
	private int beginCount = 0;
	private int commitCount = 0;
	private int rollbackCount = 0;
	private int persistObjectCount = 0;
	private int persistPropertyCount = 0;
	private int removeObjectCount = 0;
	
	private final List<PersistedSPObject> persistObjectList = 
		new ArrayList<PersistedSPObject>();
	private final List<PersistedSPOProperty> persistPropertyList = 
		new ArrayList<PersistedSPOProperty>();
	private final List<RemovedSPObject> removedObjectList =
		new ArrayList<RemovedSPObject>();

	public void begin() throws SPPersistenceException {
		beginCount++;
	}

	public void commit() throws SPPersistenceException {
		commitCount++;
	}

	public void persistObject(String parentUUID, String type, String uuid,
			int index) throws SPPersistenceException {
		persistObjectCount++;
		persistObjectList.add(new PersistedSPObject(parentUUID, type, uuid, index));
	}

	public void persistProperty(String uuid, String propertyName,
			DataType propertyType, Object oldValue, Object newValue)
			throws SPPersistenceException {
		persistPropertyCount++;
		persistPropertyList.add(new PersistedSPOProperty(uuid, propertyName, 
				propertyType, oldValue, newValue, false));
	}

	public void persistProperty(String uuid, String propertyName,
			DataType propertyType, Object newValue)
			throws SPPersistenceException {
		persistPropertyCount++;
		persistPropertyList.add(new PersistedSPOProperty(uuid, propertyName, 
				propertyType, null, newValue, true));
	}

	public void removeObject(String parentUUID, String uuid)
			throws SPPersistenceException {
		removeObjectCount++;
		removedObjectList.add(new RemovedSPObject(parentUUID, uuid));
	}

	public void rollback() {
		rollbackCount++;
	}

	public int getBeginCount() {
		return beginCount;
	}

	public int getCommitCount() {
		return commitCount;
	}

	public int getRollbackCount() {
		return rollbackCount;
	}

	public int getPersistObjectCount() {
		return persistObjectCount;
	}

	public int getPersistPropertyCount() {
		return persistPropertyCount;
	}

	public int getRemoveObjectCount() {
		return removeObjectCount;
	}

	public List<PersistedSPObject> getPersistObjectList() {
		return Collections.unmodifiableList(persistObjectList);
	}

	public List<PersistedSPOProperty> getPersistPropertyList() {
		return Collections.unmodifiableList(persistPropertyList);
	}

	public List<RemovedSPObject> getRemovedObjectList() {
		return Collections.unmodifiableList(removedObjectList);
	}

	public void clearAllPropertyChanges() {
		persistPropertyList.clear();
	}

	public Object getLastOldValue() {
		return persistPropertyList.get(persistPropertyList.size() - 1).getOldValue();
	}
	
}
