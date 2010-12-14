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

import java.io.InputStream;

import ca.sqlpower.object.SPObject;

/**
 * An interface for objects that persist {@link SPObject}s. The medium into
 * which they are persisted is entirely up to the implementation.
 */
public interface SPPersister {

	/**
	 * Defines each type of object that can be persisted by the SPPersister.
	 * Each object type can be converted to a simple type based on its name.
	 */
	public enum DataType {
		STRING("String", String.class),
		INTEGER("Integer", Integer.class),
		DOUBLE("Double", Double.class),
		BOOLEAN("Boolean", Boolean.class),
		SHORT("Short", Short.class),
		LONG("Long", Long.class),
		FLOAT("Float", Float.class),
		
		/**
		 * This is an {@link SPObject} reference.
		 */
		REFERENCE("Reference", String.class),
		PNG_IMG("PNG_IMG", InputStream.class),
		NULL("Null", null);
		
		private final String name;
		private final Class<?> representation;
		
		private DataType(String name, Class<?> representation){
			this.name = name;
			this.representation = representation;
		}

		public String getTypeName() {
			return name;
		}

		public Class<?> getRepresentation() {
			return representation;
		}
		
		/**
		 * Returns a DataType that represents the given class type. The class given
		 * can be null
		 */
		public static DataType getTypeByClass(Class<?> lookupValue) {
			for (DataType type : values()) {
				if ((type.getRepresentation() == null && lookupValue == null) || 
						(lookupValue != null && type.getRepresentation() != null && 
								type.representation.isAssignableFrom(lookupValue))) {
					return type;
				}
			}
			throw new IllegalArgumentException("The class type " + lookupValue.getName() + 
					" does not exist as a type in this DataType enum");
		}
	}

	/**
	 * An enumeration of possible SPPersister commands. The
	 * {@link MessageSender}s and {@link MessageDecoder}s can use this is a
	 * common reference for the type of persistence methods calls that can be
	 * sent through messages.
	 */
	public enum SPPersistMethod {
		begin,
		commit,
		persistObject,
		persistProperty,
		changeProperty, // Actually refers to persistPropety with old and new values
		removeObject,
		rollback
	}
	
	/**
	 * Modifies the named property of the specified {@link SPObject} in this
	 * persister's workspace. It may throw an {@link Exception} if the actual
	 * previous value in persistent storage does not match the expected previous
	 * value as an indication to the object using this SPPersister that their
	 * cached copy of the {@link SPObject} may be out of sync with the
	 * persistent storage.
	 * 
	 * @param uuid
	 *            The UUID of the {@link SPObject} in which to set the
	 *            property
	 * @param propertyName
	 *            The JavaBeans property name of the property that changed, as
	 *            it would be discovered by the java.beans.Introspector class
	 * @param propertyType
	 *            The type, and Java representation, of this property
	 * @param oldValue
	 *            The expected previous value of the property
	 * @param newValue
	 *            The new value to set for the property
	 * @throws SPPersistenceException
	 *             A general Exception that is thrown if any Exception occurs
	 *             while persisting the property. It can be used to wrap the
	 *             specific cause Exception and provide other details like the
	 *             SPObject UUID. Some potential exceptional situations
	 *             include:
	 *             <ul>
	 *             <li>The SPObject UUID is unknown to this persister</li>
	 *             <li>The given property name does not exist</li>
	 *             <li>The given property is not writable</li>
	 *             <li>The property type of the given old/new values do not
	 *             match each other or the actual property in the object</li>
	 *             <li>The existing persistent value doesn't match expected
	 *             oldValue</li>
	 *             </ul>
	 */
	public void persistProperty(String uuid, String propertyName,
			DataType propertyType, Object oldValue, Object newValue)
			throws SPPersistenceException;

	/**
	 * Modifies the named property of the specified SPObject in this
	 * persister's workspace. This version is an unconditional call, and does
	 * not check the previous state of the property. To ensure the
	 * {@link SPObject}s stay in synch, this method should only be called by
	 * the Persister representing the master copy of the SPObject.
	 * 
	 * @param uuid
	 *            The UUID of the {@link SPObject} in which to set the
	 *            property
	 * @param propertyName
	 *            The JavaBeans property name of the property that changed, as
	 *            it would be discovered by the java.beans.Introspector class
	 * @param propertyType
	 *            The type, and Java representation, of this property
	 * @param newValue
	 *            The new value to set for the property
	 * @throws SPPersistenceException
	 *             A general Exception that is thrown if any Exception occurs
	 *             while persisting the property. It can be used to wrap the
	 *             specific cause Exception and provide other details like the
	 *             SPObject UUID. Some potential exceptional situations
	 *             include:
	 *             <ul>
	 *             <li>The SPObject UUID is unknown to this persister</li>
	 *             <li>The given property name does not exist</li>
	 *             <li>The given property is not writable</li>
	 *             <li>The property type of the given value does not match the
	 *             actual property in the object</li>
	 */
	public void persistProperty(String uuid, String propertyName,
			DataType propertyType, Object newValue)
			throws SPPersistenceException;

	/**
	 * Adds a {@link SPObject} into the persistent storage. If the
	 * SPObject already exists in persistent storage, then it will throw an
	 * exception Note that this will not persist its properties or any child
	 * objects.
	 * 
	 * @param parentUUID
	 *            The UUID of the parent {@link SPObject} of the object to
	 *            persist. If the SPObject has no parent, then it can be set to null.
	 * @param type
	 *            A String of the fully qualified class name of the SPObject to be persisted
	 * @param uuid
	 *            The UUID of the {@link SPObject} to actually persist
	 * @param index
	 *            The index of the {@link SPObject} in its parents' list of children
	 * @throws SPPersistenceException
	 *             A general Exception that is thrown if any Exception occurs
	 *             while persisting the SPObject. It can be used to wrap the
	 *             specific cause Exception and provide other details like the
	 *             SPObject UUID. Some potential exceptional situations
	 *             include:
	 *             <ul>
	 *             <li>An SPObject with the given UUID already exists in the
	 *             persistent storage</li>
	 *             <li>An SPObject with the given parent UUID does not exist</li>
	 *             </ul>
	 */
	public void persistObject(String parentUUID, String type, String uuid, int index) throws SPPersistenceException;

	/**
	 * Removes an SPObject from persistent storage
	 * 
	 * @param parentUUID
	 *            The UUID of the parent {@link SPObject} of the object to
	 *            remove
	 * @param uuid
	 *            The UUID of the {@link SPObject} to remove
	 * @throws SPPersistenceException
	 *             A general Exception that is thrown if any Exception occurs
	 *             while persisting the SPObject. It can be used to wrap the
	 *             specific cause Exception and provide other details like the
	 *             SPObject UUID.
	 */
	public void removeObject(String parentUUID, String uuid)
			throws SPPersistenceException;

	/**
	 * Indicates the start of an atomic transaction of persisting multiple
	 * {@link SPObject}s. To be used with a paired call to {@link #commit()}
	 * 
	 * @throws SPPersistenceException
	 *             A general Exception that is thrown if any Exception occurs
	 *             while persisting the SPObject. It can be used to wrap the
	 *             specific cause Exception and provide other details like the
	 *             SPObject UUID.
	 */
	public void begin() throws SPPersistenceException;

	/**
	 * Causes a current {@link SPObject} persistence transaction to commit
	 * its results.
	 * 
	 * @throws SPPersistenceException
	 *             A general Exception that is thrown if any Exception occurs
	 *             while committing the transaction. This could be caused by one
	 *             being thrown by {@link #persistObject(String, String)},
	 *             {@link #persistProperty(String, String, Object, Object)}, or
	 *             {@link #removeObject(String, String)}. Some other exceptional
	 *             situations include:
	 *             <ul>
	 *             <li>An update conflict with another transaction</li>
	 *             <li>Network issues</li>
	 *             <li>Insufficient permissions in the backing store</li>
	 *             </ul>
	 */
	public void commit() throws SPPersistenceException;

	/**
	 * Restores the persisted {@link SPObject} back to the state it was in before the
	 * transaction began (i.e. when the call to {@link #begin()} was made).
	 * Typically, this would be called if an exception occurs during an atomic
	 * transaction. In the event that a rollback() is called within a nested
	 * transaction (that is, a begin() call after another begin() call before
	 * commit()), then the state of the {@link SPObject}s must be rolled back to the
	 * state they were in before the highest level transaction began.
	 */
	public void rollback();
}
