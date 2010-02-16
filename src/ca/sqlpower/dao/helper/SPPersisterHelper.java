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
import ca.sqlpower.object.SPListener;
import ca.sqlpower.object.SPObject;
import ca.sqlpower.object.annotation.Accessor;
import ca.sqlpower.object.annotation.Constructor;
import ca.sqlpower.object.annotation.ConstructorParameter;
import ca.sqlpower.object.annotation.Mutator;
import ca.sqlpower.object.annotation.SPAnnotationProcessorFactory;

import com.google.common.collect.Multimap;

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
	 * Updates the given {@link SPObject} to match the object that would be
	 * created based on the parameters if
	 * {@link #commitObject(PersistedSPObject, Multimap, List, SessionPersisterSuperConverter)}
	 * was called. The only use for updating an object is to update the root
	 * object to match the object that would be created by persist calls when
	 * the root is being initially created. Because the root object may have
	 * values passed to its constructor that are final other objects that are
	 * children of the root may need to be updated to match as well. There is
	 * no way to reverse an update at current but only the root node and its final
	 * parameters should be updated when the project is first created so the
	 * reverse of this is to remove the project.
	 * <p>
	 * TODO For objects that are final properties that are not {@link SPObject}s
	 * an updater similar to the converter classes will be needed.
	 * 
	 * @param objectToUpdate
	 *            The object that will be updated to match the one that would be
	 *            created by
	 *            {@link #commitObject(PersistedSPObject, Multimap, List, SessionPersisterSuperConverter)}
	 *            .
	 * @param pso
	 *            The persist call that would create the object.
	 * @param persistedProperties
	 *            All of the persist property calls in the current transaction.
	 *            Persist calls may be removed if they are used to update the
	 *            object to prevent updates from happening twice.
	 * @param persistedObjects
	 *            All of the persist object calls in the current transaction.
	 *            Persist calls may be removed if they are used to create final
	 *            children for the object being updated. Removing the persist
	 *            call prevents the child from being created twice.
	 * @param converter
	 *            A converter that can take simple representation of objects in
	 *            a persist call and create an {@link Object} to set properties
	 *            to.
	 * @throws SPPersistenceException
	 */
	void updateObject(SPObject objectToUpdate, PersistedSPObject pso, 
			Multimap<String, PersistedSPOProperty> persistedProperties, 
			List<PersistedSPObject> persistedObjects,
			SessionPersisterSuperConverter converter) throws SPPersistenceException;

	/**
	 * Creates a new {@link SPObject} of type T given a {@link Multimap} of
	 * {@link SPObject} UUIDs to persisted properties. The properties taken from
	 * the {@link Multimap} of the given UUID to pass into the {@link SPObject}
	 * constructor must be removed.
	 * 
	 * @param pso
	 *            The {@link PersistedSPObject} that the {@link SPObject} is
	 *            being created from. The UUID to use for the created
	 *            {@link SPObject} is to be taken from this object and the
	 *            loaded flag should be set the <code>true</code> before
	 *            returning the newly created {@link SPObject}.
	 * @param persistedProperties
	 *            A mutable {@link Multimap} of {@link SPObject} UUIDs to
	 *            persisted properties, each represented by
	 *            {@link PersistedSPOProperty}. Some entries within this
	 *            {@link Multimap} will be removed if the {@link SPObject}
	 *            constructor it calls requires arguments.
	 * @param persistedObjects
	 *            The {@link List} of {@link PersistedSPObject}s that has been
	 *            persisted in an {@link SPPersister}. This is to be used for
	 *            {@link SPObject}s that take children in their constructor,
	 *            where the {@link SPPersisterHelper} factory finds the
	 *            appropriate {@link SPPersisterHelper} for that child type and
	 *            calls commitObject on that as well.
	 * @param converter
	 *            The {@link SPPersisterHelperFactory} to use to convert complex
	 *            properties into simple ones.
	 * @return The created {@link SPObject} with the given required persisted
	 *         properties.
	 */
	T commitObject(PersistedSPObject pso,
			Multimap<String, PersistedSPOProperty> persistedProperties, 
			List<PersistedSPObject> persistedObjects,
			SessionPersisterSuperConverter converter) throws SPPersistenceException;

	/**
	 * Applies a property change on the given {@link SPObject} and property
	 * name.
	 * 
	 * @param spo
	 *            The {@link SPObject} to apply the property change on.
	 * @param propertyName
	 *            The JavaBean property name.
	 * @param newValue
	 *            The new property value. This value must be converted through
	 *            the {@link SessionPersisterSuperConverter} from simple type
	 *            into a complex type before setting the property value.
	 * @param converter
	 *            The {@link SessionPersisterSuperConverter} class to use to
	 *            convert newValue from a simple type to a complex type.
	 * @throws SPPersistenceException
	 *             Thrown if the property is not a persistable property. The
	 *             setter method for this property in the {@link SPObject} class
	 *             must be annotated with {@link Mutator}.
	 */
	void commitProperty(SPObject spo, String propertyName, Object newValue, 
			SessionPersisterSuperConverter converter) throws SPPersistenceException;

	/**
	 * Finds and returns a property value from the given {@link SPObject} based
	 * on the property name and converts it to something that can be passed to
	 * an {@link SPPersister}.
	 * 
	 * @param spo
	 *            The {@link SPObject} to retrieve the property from.
	 * @param propertyName
	 *            The property name that needs to be retrieved and converted.
	 *            This is the name of the property in the class itself based on
	 *            the property fired by the setter for the event which is
	 *            enforced by tests using JavaBeans methods. If changes are made
	 *            to an {@link SPObject} class such that one or more properties
	 *            are changed (i.e. changed property name or property type), the
	 *            {@link SPAnnotationProcessorFactory} should be executed to
	 *            generate an updated {@link SPPersisterHelper} class.
	 * @param converter
	 *            The {@link SessionPersisterSuperConverter} class to use to
	 *            convert the retrieved property value from a complex type to a
	 *            simple type.
	 * @return The value stored in the variable of the object we are given at
	 *         the property name after it has been converted to a type that can
	 *         be stored. The conversion is based on the
	 *         {@link SessionPersisterSuperConverter}.
	 * @throws SPPersistenceException
	 *             Thrown if the property is not a persistable property. The
	 *             getter method for this property in the {@link SPObject} class
	 *             must be annotated with {@link Accessor}.
	 */
	Object findProperty(SPObject spo, String propertyName, 
			SessionPersisterSuperConverter converter) throws SPPersistenceException;

	/**
	 * Persists an {@link SPObject} and all of its required properties into an
	 * {@link SPPersister}. The persisted properties must either have
	 * getter/setter methods annotated with {@link Accessor} and {@link Mutator}
	 * , or be set in a constructor annotated with {@link Constructor} where its
	 * parameters are annotated with {@link ConstructorParameter} with reference
	 * to the property it will set.
	 * 
	 * @param o
	 *            The {@link SPObject} to persist along with its required
	 *            properties.
	 * @param index
	 *            The index of the {@link SPObject} to persist relative to its
	 *            siblings of the same type.
	 * @param persister
	 *            The {@link SPPersister} to persist the object onto.
	 * @param converter
	 *            The {@link SessionPersisterSuperConverter} class to use to
	 *            convert the properties of the {@link SPObject} from complex
	 *            types to simple types which will be recognized by the
	 *            {@link SPPersister}.
	 * @throws SPPersistenceException
	 *             Thrown if the {@link SPPersister} cannot persist the object
	 *             or any one of its properties.
	 */
	void persistObject(SPObject o, int index, SPPersister persister, 
			SessionPersisterSuperConverter converter) throws SPPersistenceException;
	
}
