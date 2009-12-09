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

import ca.sqlpower.object.SPListener;
import ca.sqlpower.object.SPObject;
import ca.sqlpower.object.annotation.Accessor;
import ca.sqlpower.object.annotation.Constructor;
import ca.sqlpower.object.annotation.ConstructorParameter;
import ca.sqlpower.object.annotation.Mutator;

/**
 * This persister helper is used by a session {@link SPPersister} or workspace
 * persister {@link SPListener} to assist in creating new {@link SPObject}s and
 * applying/retrieving persistable properties.
 * 
 * @param <T>
 *            The {@link SPObject} type this persister helper deals with. Any
 *            new object created or persisted will be of this type and any
 *            properties will be from objects of this type.
 */
public interface SPPersisterHelper<T extends SPObject> {

	/**
	 * Creates a new {@link SPObject} of type T given a {@link Collection} of
	 * persisted properties. The properties taken from the {@link Collection} of
	 * properties to pass into the {@link SPObject} constructor must be removed
	 * from this collection.
	 * 
	 * @param persistedProperties
	 *            A mutable {@link Collection} of persisted properties for a
	 *            specific {@link SPObject}, each represented by
	 *            {@link PersistedSPOProperty}. Some entries within this
	 *            {@link Collection} will be removed if the {@link SPObject}
	 *            constructor it calls requires arguments.
	 * @return The created {@link SPObject} with the given required persisted
	 *         properties.
	 */
	T commitObject(Collection<PersistedSPOProperty> persistedProperties);
	
	/**
	 * Applies a property change on the given {@link SPObject} and property
	 * name.
	 * 
	 * @param spo
	 *            The {@link SPObject} to apply the property change on.
	 * @param propertyName
	 *            The JavaBean property name.
	 * @param newValue
	 *            The new property value.
	 * @throws SPPersistenceException
	 *             Thrown if the property is not a persistable property. The
	 *             setter method for this property in the {@link SPObject} class
	 *             must be annotated with {@link Mutator}.
	 */
	void commitProperty(T spo, String propertyName, Object newValue) throws SPPersistenceException;

	/**
	 * Retrieves a property value from the given {@link SPObject} and property
	 * name.
	 * 
	 * @param spo
	 *            The {@link SPObject} to retrieve the property from.
	 * @param propertyName
	 *            The JavaBean property name.
	 * @return The property value of the given property name.
	 * @throws SPPersistenceException
	 *             Thrown if the property is not a persistable property. The
	 *             getter method for this property in the {@link SPObject} class
	 *             must be annotated with {@link Accessor}.
	 */
	Object retrieveProperty(T spo, String propertyName) throws SPPersistenceException;

	/**
	 * Persists an {@link SPObject} and all of its required properties into an
	 * {@link SPPersister}. The persisted properties must either have
	 * getter/setter methods annotated with {@link Accessor} and {@link Mutator},
	 * or be set in a constructor annotated with {@link Constructor} where its
	 * parameters are annotated with {@link ConstructorParameter} with reference
	 * to the property it will set.
	 * 
	 * @param spo
	 *            The {@link SPObject} to persist along with its required
	 *            properties.
	 * @param index
	 *            The index of the {@link SPObject} to persist relative to its
	 *            siblings of the same type.
	 * @param persister
	 *            The {@link SPPersister} to persist the object onto.
	 */
	void persistObject(T spo, int index, SPPersister persister);
	
}
