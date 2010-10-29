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

package ca.sqlpower.dao;

import java.util.ArrayList;
import java.util.List;

import ca.sqlpower.object.SPObject;

/**
 * This filters persist calls and removes any of them belonging to the filtered classes.
 * This, combined with the filter listener allow us to skip pieces of the SPObject
 * tree and not load them all.
 */
public class AbstractSPFilterPersister implements SPPersister {

	private boolean filter;
	private final List<String> filteredClasses;
	private final SPPersister nextPersister;
	private List<String> UUIDSToIgnore;

	/**
	 * This filters persist calls and removes any of them belonging to the filtered classes.
	 * @param filteredClasses
	 * @param nextPersister
	 */
	public AbstractSPFilterPersister(List<Class<? extends SPObject>> filteredClasses,
			SPPersister nextPersister) {
		this.filter = false;
		this.filteredClasses = new ArrayList<String>();
		for(Class<? extends SPObject> c : filteredClasses) {
			this.filteredClasses.add(c.getName());
		}
		this.nextPersister = nextPersister;
		UUIDSToIgnore = new ArrayList<String>();
	}
	
	@Override
	public void persistProperty(String uuid, String propertyName,
			DataType propertyType, Object oldValue, Object newValue)
			throws SPPersistenceException {
		if(!filter || !UUIDSToIgnore.contains(uuid)) {
			persistProperty(uuid, propertyName, propertyType, oldValue, newValue);
		}

	}

	@Override
	public void persistProperty(String uuid, String propertyName,
			DataType propertyType, Object newValue)
			throws SPPersistenceException {
		if(!filter || !UUIDSToIgnore.contains(uuid)) {
			persistProperty(uuid, propertyName, propertyType, newValue);
		}

	}

	@Override
	public void persistObject(String parentUUID, String type, String uuid,
			int index) throws SPPersistenceException {
		if(filter && filteredClasses.contains(type)) {
			UUIDSToIgnore.add(uuid);
		} else {
			persistObject(parentUUID, type, uuid, index);
		}
	}

	@Override
	public void removeObject(String parentUUID, String uuid)
			throws SPPersistenceException {
		// TODO Auto-generated method stub

	}

	@Override
	public void begin() throws SPPersistenceException {
		UUIDSToIgnore = new ArrayList<String>();
		nextPersister.begin();
	}

	@Override
	public void commit() throws SPPersistenceException {
		nextPersister.commit();
	}

	@Override
	public void rollback() {
		nextPersister.rollback();
	}

}
