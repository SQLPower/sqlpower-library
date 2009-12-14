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

import java.util.Collection;

import ca.sqlpower.object.SPObject;

public abstract class AbstractSPPersisterHelper<T extends SPObject> implements SPPersisterHelper<T> {

	/**
	 * Returns a simple string for use in exceptions from
	 * {@link SPPersisterHelper#commitProperty(SPObject, String, Object)} and
	 * {@link SPPersisterHelper#findProperty(SPObject, String)}. This
	 * message describes that a persistable property cannot be found on the
	 * object. This is refactored here as a lot of methods throw an exception
	 * with a message equivalent to this one.
	 * 
	 * @param spo
	 *            The {@link SPObject} that does not contain the given property.
	 * @param propertyName
	 *            The property we want to find on the {@link SPObject} that
	 *            cannot be found.
	 * @return An error message for exceptions that describes the above.
	 */
	protected String createSPPersistenceExceptionMessage(T spo,
			String propertyName) {
		return "Cannot persist property \"" + propertyName + "\" on "
				+ spo.getClass() + " with name \"" + spo.getName()
				+ "\" and UUID \"" + spo.getUUID() + "\"";
	}
	
	/**
	 * Finds and removes a property from a {@link Collection} of persisted
	 * properties.
	 * 
	 * @param persistedProperties
	 *            {@link Collection} of persisted properties to retrieve and
	 *            remove the property from.
	 * @param propertyName
	 *            The JavaBean property name.
	 * @return The value of the property.
	 */
	protected Object findPropertyAndRemove(Collection<PersistedSPOProperty> persistedProperties, String propertyName) {
		for (PersistedSPOProperty property : persistedProperties) {
			if (property.getPropertyName().equals(propertyName)) {
				Object newValue = property.getNewValue();
				persistedProperties.remove(property);
				return newValue;
			}
		}
		// Property might not be persisted because it might be null. 
		// We therefore need to return null.
		return null;
	}
	
}
