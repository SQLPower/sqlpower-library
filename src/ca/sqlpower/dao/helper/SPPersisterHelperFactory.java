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

package ca.sqlpower.dao.helper;

import java.util.List;

import ca.sqlpower.dao.PersistedSPOProperty;
import ca.sqlpower.dao.PersistedSPObject;
import ca.sqlpower.dao.SPPersistenceException;
import ca.sqlpower.dao.SPPersister;
import ca.sqlpower.dao.session.SessionPersisterSuperConverter;
import ca.sqlpower.object.SPObject;

import com.google.common.collect.Multimap;

public abstract class SPPersisterHelperFactory {
	
	private final SPPersister persister;
	private final SessionPersisterSuperConverter converter;

	public SPPersisterHelperFactory(
			SPPersister persister, 
			SessionPersisterSuperConverter converter) {
		this.persister = persister;
		this.converter = converter;
	}

	public abstract <T extends SPObject> SPPersisterHelper<T> getSPPersisterHelper(
			Class<T> clazz);

	public <T extends SPObject> T commitObject(
			Class<T> clazz, 
			Multimap<String, 
			PersistedSPOProperty> persistedProperties, 
			PersistedSPObject pso, 
			List<PersistedSPObject> persistedObjects) {
		SPPersisterHelper<T> helper = getSPPersisterHelper(clazz);
		return helper.commitObject(persistedProperties, pso, persistedObjects, this);
	}

	public <T extends SPObject> void commitProperty(
			T spo, 
			String propertyName, 
			Object newValue, 
			SessionPersisterSuperConverter converter) throws SPPersistenceException {
		SPPersisterHelper<T> helper = getSPPersisterHelper((Class<T>) spo.getClass());
		helper.commitProperty(spo, propertyName, newValue, converter);
	}

	public <T extends SPObject> void findProperty(
			T spo, 
			String propertyName, 
			SessionPersisterSuperConverter converter) throws SPPersistenceException {
		SPPersisterHelper<T> helper = getSPPersisterHelper((Class<T>) spo.getClass());
		helper.findProperty(spo, propertyName, converter);
	}

	public <T extends SPObject> void persistObject(T spo, int index) throws SPPersistenceException {
		SPPersisterHelper<T> helper = getSPPersisterHelper((Class<T>) spo.getClass());
		helper.persistObject(spo, index, persister, converter);
	}
}
