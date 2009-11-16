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

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * This interface represents any kind of object within this library or extending
 * projects that can have a parent and multiple children.
 */
public interface SQLPowerLibraryObject {
	
    /**
     * Adds a listener that will be notified when children are added to or
     * removed from this object, when properties change, and when a transaction
     * starts and ends. The events will always be fired in the foreground.
     * 
     * @param l
     *            The listener to add.
     * @see SQLPowerLibraryListener
     */
    void addSQLPowerLibraryListener(SQLPowerLibraryListener l);

    /**
     * Removes a listener that was previously attached to this {@link SQLPowerLibraryObject}.
     * 
     * @param l
     *            The listener to remove.
     */
    void removeSQLPowerLibraryListener(SQLPowerLibraryListener l);

	/**
	 * Returns the parent of this {@link SQLPowerLibraryObject}. This will be
	 * null when the object is first created until it is added as a child to
	 * another object. If this object is never added as a child to another
	 * object this will remain null and the object may be treated as the root
	 * node of a {@link SQLPowerLibraryObject} tree.
	 */
	SQLPowerLibraryObject getParent();
	
    /**
     * Sets the parent of this object to the given object. This should only be
     * done when this object is being added as a child to another object.
     * 
     * @param parent
     *            The new parent of this object.
     */
	void setParent(SQLPowerLibraryObject parent);

	/**
	 * Returns an unmodifiable list of the children in this
	 * {@link SQLPowerLibraryObject}. If there are no children in this
	 * {@link SQLPowerLibraryObject}, an empty list should be returned.
	 */
    List<? extends SQLPowerLibraryObject> getChildren();

    /**
     * Returns true if this object may contain children. Not all types of
     * {@link SQLPowerLibraryObject}s can be a child to any {@link SQLPowerLibraryObject}.
     */
    boolean allowsChildren();
    
    /**
     * Returns the position in the list that would be returned by getChildren()
     * that the first object of type childClass is, or where it would be if
     * there were any children of that type.
     */
    int childPositionOffset(Class<? extends SQLPowerLibraryObject> childType);

	/**
	 * Removes the given child object from this object.
	 * 
	 * @param child
	 *            The object to remove as a child of this object.
	 * @return True if the child was successfully removed. False if the child
	 *         was not removed from this object.
	 * @throws IllegalArgumentException
	 *             Thrown if the given child is not an actual child of this
	 *             object.
	 * @throws ObjectDependentException
	 *             Thrown if the given child has dependencies and cannot be
	 *             removed from this object.
	 */
    boolean removeChild(SQLPowerLibraryObject child) throws ObjectDependentException, IllegalArgumentException;

	/**
	 * Adds the given child object to this object.
	 * 
	 * @param child
	 *            The object to add as a child of this object.
	 * @param index
	 *            The index to add the child to. This cannot be greater than the
	 *            number of children in the object of the given type. This is
	 *            the position of the child in the list of children of a
	 *            specific type. The position of the child is in respect to
	 *            children of its type.
	 * @throws IllegalArgumentException
	 *             If the given child is not a valid child type of the object.
	 */
    void addChild(SQLPowerLibraryObject child, int index) throws IllegalArgumentException;
    
    /**
     * Returns the short name for this object.
     */
    @Nullable
    String getName();
    
    /**
     * Sets the name for this object 
     */
    void setName(@Nullable String name);
    
    String getUUID();
    
    void setUUID(String uuid);
    
    /**
     * Sets the UUID of this object to a newly generated UUID. This is necessary
     * if the object is being cloned or copied to a new workspace.
     */
    void generateNewUUID();
    
    /**
     * Removes the given object as a dependency of this object. For this object
     * to no longer be dependent on the given dependency all of its children
     * must also not be dependent on the given dependency when this method
     * returns. This may remove this object from its parent if necessary.
     */
    void removeDependency(@Nonnull SQLPowerLibraryObject dependency);

    /**
     * Returns a list of all {@link SQLPowerLibraryObject}s that this SQLPowerLibraryObject is
     * dependent on. Children of a SQLPowerLibraryObject are not dependencies and will not
     * be returned in this list. If there are no objects this SQLPowerLibraryObject object is
     * dependent on an empty list should be returned. These are only the
     * immediate dependencies of this object.
     */
    List<SQLPowerLibraryObject> getDependencies();

    /**
     * Disconnects this object from any other objects it is listening to, closes
     * any open connections, and performs any other necessary operations to
     * ensure that this object can be discarded. Once this object has been
     * cleaned up none of its methods should be called. This method will only
     * cleanup this object and not its descendants. To clean up this object and
     * its descendants see {@link SQLPowerUtils.#cleanupSQLPowerLibraryObject(SQLPowerLibraryObject)}.
     * <p>
     * Calling cleanup does not mean the object must be disconnected from the
     * workspace as all objects will be cleaned up when the session is closing.
     * The object can also still have other objects dependent on it unlike
     * {@link #removeChild(SQLPowerLibraryObject)}.
     * 
     * @return A collection of exceptions and errors that occurred during
     *         cleanup if any occurred.
     */
    CleanupExceptions cleanup();

	/**
	 * Starts a transaction that will pool multiple events into a compound
	 * event.
	 * 
	 * @param message
	 *            Description of the compound event.
	 */
    void begin(String message);
    
    /**
     * Signals the end of a transaction of a compound event.
     */
    void commit();

	/**
	 * Signals the roll back of a transaction. The events of the transaction
	 * should not be acted on and/or should be undone.
	 * 
	 * @param message
	 *            Reason for the roll back.
	 */
    void rollback(String message);

    /**
     * Returns a list of all children of the given type
     */
    public <T extends SQLPowerLibraryObject> List<T> getChildren(Class<T> type);
	
}
