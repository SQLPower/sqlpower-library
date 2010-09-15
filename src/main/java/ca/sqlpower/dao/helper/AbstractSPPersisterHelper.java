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
import ca.sqlpower.object.SPObject;

import com.google.common.collect.Multimap;

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
	public static String createSPPersistenceExceptionMessage(SPObject spo,
			String propertyName) {
		return "Cannot persist property \"" + propertyName + "\" on "
				+ spo.getClass() + " with name \"" + spo.getName()
				+ "\" and UUID \"" + spo.getUUID() + "\"";
	}

	/**
	 * Finds and removes a property from a {@link Multimap} of persisted
	 * properties of a given {@link SPObject}.
	 * 
	 * @param uuid
	 *            The UUID of the {@link SPObject} to find and remove the
	 *            property from.
	 * @param propertyName
	 *            The JavaBean property name.
	 * @param persistedProperties
	 *            {@link Multimap} of persisted properties to retrieve and
	 *            remove the property from.
	 * @return The value of the property.
	 */
	public static Object findPropertyAndRemove(
			String uuid, 
			String propertyName, 
			Multimap<String, PersistedSPOProperty> persistedProperties) {
		for (PersistedSPOProperty property : persistedProperties.get(uuid)) {
			if (property.getPropertyName().equals(propertyName)) {
				Object newValue = property.getNewValue();
				persistedProperties.remove(uuid, property);
				return newValue;
			}
		}
		// Property might not be persisted because it might be null. 
		// We therefore need to return null.
		return null;
	}
	
	/**
	 * Finds a property from a {@link Multimap} of persisted
	 * properties of a given {@link SPObject} without removing the property.
	 * 
	 * @param uuid
	 *            The UUID of the {@link SPObject} to find and remove the
	 *            property from.
	 * @param propertyName
	 *            The JavaBean property name.
	 * @param persistedProperties
	 *            {@link Multimap} of persisted properties to retrieve and
	 *            remove the property from.
	 * @return The value of the property.
	 */
	public static PersistedSPOProperty findProperty(
			String uuid, 
			String propertyName, 
			Multimap<String, PersistedSPOProperty> persistedProperties) {
		for (PersistedSPOProperty property : persistedProperties.get(uuid)) {
			if (property.getPropertyName().equals(propertyName)) {
				return property;
			}
		}
		// Property might not be persisted because it might be null. 
		// We therefore need to return null.
		return null;
	}
	
	/**
	 * Finds the {@link PersistedSPObject} in a {@link List} that matches the
	 * given UUIDs
	 * 
	 * @param parentUUID
	 *            The {@link SPObject}'s parent UUID.
	 * @param persistedObjects
	 *            The {@link List} of {@link PersistedSPObject}s to search
	 *            through.
	 * @return The matching {@link PersistedSPObject}. If it cannot be found,
	 *         null is returned.
	 */
	public static PersistedSPObject findPersistedSPObject(String parentUUID,
			String childUUID,
			List<PersistedSPObject> persistedObjects) {
		for (PersistedSPObject pwo : persistedObjects) {
			if (pwo.isLoaded())
				continue;
			if (pwo.getParentUUID().equals(parentUUID)) {
				if (pwo.getUUID().equals(childUUID)) {
					return pwo;
				}
			}
		}
		return null;
	}
	
}
