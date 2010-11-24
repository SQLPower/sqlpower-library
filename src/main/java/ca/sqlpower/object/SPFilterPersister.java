/*
 * Copyright (c) 2010, SQL Power Group Inc.
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

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;

import ca.sqlpower.dao.SPPersistenceException;
import ca.sqlpower.dao.SPPersister;

/**
 * This persister is made so that you can filter out persists that you don't want.
 */
public class SPFilterPersister implements SPPersister {

	final private SPPersister targetPersister;
	final private Callable<Boolean> filterOn;
	private final Set<String> filterObjects;
	private final Set<String> seenUUIDS;
	
	/**
	 * Constructs a default filter persister.
	 * @param targetPersister The filter persister will persist to this filter persister.
	 * @param filterOn This is a callable that will tell us whether or not to filter.
	 * @param filterObjects For every object in this list, if you are currenty filtering,
	 * the persister will remove any persist calls relating to that object type or its children
	 * types.
	 */
	public SPFilterPersister(SPPersister targetPersister, Callable<Boolean> filterOn, Set<String> filterObjects) {
		this.filterObjects = filterObjects;
		this.filterOn = filterOn;
		this.targetPersister = targetPersister;
		seenUUIDS = new HashSet<String>();
	}


	@Override
	public void removeObject(String parentUUID, String uuid)
			throws SPPersistenceException {
		targetPersister.removeObject(parentUUID, uuid);
	}
	
	@Override
	public void persistObject(String parentUUID, String type, String uuid,
			int index) throws SPPersistenceException {
		try {
			if (filterOn.call() == Boolean.TRUE) {
				if(filterObjects.contains(type) || seenUUIDS.contains(parentUUID)) {
					seenUUIDS.add(uuid);
				} else {
					targetPersister.persistObject(parentUUID, type, uuid, index);
				}
			} else {
				targetPersister.persistObject(parentUUID, type, uuid, index);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public void persistProperty(String uuid, String propertyName,
			DataType propertyType, Object oldValue, Object newValue)
			throws SPPersistenceException {
		try {
			if (filterOn.call() == Boolean.TRUE) {
				if(!seenUUIDS.contains(uuid)) {
					targetPersister.persistProperty(uuid, propertyName, propertyType, oldValue, newValue);
				}
			} else {
				targetPersister.persistProperty(uuid, propertyName, propertyType, oldValue, newValue);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void persistProperty(String uuid, String propertyName,
			DataType propertyType, Object newValue)
			throws SPPersistenceException {
		try {
			if (filterOn.call() == Boolean.TRUE) {
				if(!seenUUIDS.contains(uuid)) {
					targetPersister.persistProperty(uuid, propertyName, propertyType, newValue);
				}
			} else {
				targetPersister.persistProperty(uuid, propertyName, propertyType, newValue);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void begin() throws SPPersistenceException {
		targetPersister.begin();
	}

	@Override
	public void commit() throws SPPersistenceException {
		targetPersister.commit();
	}

	@Override
	public void rollback() {
		targetPersister.rollback();
	}

}
