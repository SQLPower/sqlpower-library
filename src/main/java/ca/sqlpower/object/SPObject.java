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

import ca.sqlpower.object.annotation.Accessor;
import ca.sqlpower.object.annotation.Mutator;
import ca.sqlpower.object.annotation.NonBound;
import ca.sqlpower.object.annotation.NonProperty;
import ca.sqlpower.object.annotation.Transient;
import ca.sqlpower.util.RunnableDispatcher;
import ca.sqlpower.util.SessionNotFoundException;
import ca.sqlpower.util.WorkspaceContainer;

/**
 * This interface represents any kind of object within this library or extending
 * projects that can have a parent and multiple children.
 */
public interface SPObject {
	
    /**
     * Adds a listener that will be notified when children are added to or
     * removed from this object, when properties change, and when a transaction
     * starts and ends. The events will always be fired in the foreground.
     * 
     * @param l
     *            The listener to add.
     * @see SPListener
     */
    void addSPListener(SPListener l);

    /**
     * Removes a listener that was previously attached to this {@link SPObject}.
     * 
     * @param l
     *            The listener to remove.
     */
    void removeSPListener(SPListener l);

	/**
	 * Returns the parent of this {@link SPObject}. This will be
	 * null when the object is first created until it is added as a child to
	 * another object. If this object is never added as a child to another
	 * object this will remain null and the object may be treated as the root
	 * node of a {@link SPObject} tree.
	 */
    @Accessor
	SPObject getParent();
	
    /**
     * Sets the parent of this object to the given object. This should only be
     * done when this object is being added as a child to another object.
     * 
     * @param parent
     *            The new parent of this object.
     */
    @Mutator
	void setParent(SPObject parent);

	/**
	 * Returns an unmodifiable list of the children in this
	 * {@link SPObject}. If there are no children in this
	 * {@link SPObject}, an empty list should be returned.
	 */
	@NonProperty
    List<? extends SPObject> getChildren();

    /**
     * Returns true if this object may contain children. Not all types of
     * {@link SPObject}s can be a child to any {@link SPObject}.
     */
    boolean allowsChildren();

	/**
	 * Returns the position in the list that would be returned by getChildren()
	 * that the first object of type childClass is, or where it would be if
	 * there were any children of that type. If this class does not contain
	 * children of type <code>childType</code>, this throws an
	 * IllegalArgumentException
	 */
    int childPositionOffset(Class<? extends SPObject> childType);

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
    boolean removeChild(SPObject child) throws ObjectDependentException, IllegalArgumentException;

	/**
	 * Adds the given child object to this object. Throws an
	 * {@link IllegalArgumentException} if the given object is not a valid child
	 * of this object.
	 * 
	 * @param child
	 *            The object to add as a child of this object.
	 * @param index
	 *            The index to add the child to. This cannot be greater than the
	 *            number of children in the object of the given type. This is
	 *            the position of the child in the list of children of a
	 *            specific type. The position of the child is in respect to
	 *            children of its type.
	 */
    void addChild(SPObject child, int index);
    
    /**
     * Returns the short name for this object.
     */
    @Nullable
    @Accessor
    String getName();
    
    /**
     * Sets the name for this object 
     */
    @Mutator
    void setName(@Nullable String name);
    
    @Accessor
    String getUUID();
    
    @Mutator
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
    void removeDependency(@Nonnull SPObject dependency);

    /**
     * Returns a list of all {@link SPObject}s that this SPObject is
     * dependent on. Children of an SPObject are not dependencies and will not
     * be returned in this list. If there are no objects this SPObject is
     * dependent on an empty list should be returned. These are only the
     * immediate dependencies of this object.
     */
    @NonBound
    List<? extends SPObject> getDependencies();

    /**
     * Disconnects this object from any other objects it is listening to, closes
     * any open connections, and performs any other necessary operations to
     * ensure that this object can be discarded. Once this object has been
     * cleaned up none of its methods should be called. This method will only
     * cleanup this object and not its descendants. To clean up this object and
     * its descendants see {@link SQLPowerUtils.#cleanupSPObject(SPObject)}.
     * <p>
     * Calling cleanup does not mean the object must be disconnected from the
     * workspace as all objects will be cleaned up when the session is closing.
     * The object can also still have other objects dependent on it unlike
     * {@link #removeChild(SPObject)}.
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
     * Signals the end of a transaction of a compound event.
     */
    void commit(String message);

    /**
     * Gets the WorkspaceContainer that contains this SPObject. If the session is null a
     * {@link SessionNotFoundException} will be thrown.
     */
    @Transient @Accessor
    WorkspaceContainer getWorkspaceContainer();
    
    /**
     * Gets the RunnableDispacher that contains this SPObject. If the session is null a
     * {@link SessionNotFoundException} will be thrown.
     */
    @Transient @Accessor
    RunnableDispatcher getRunnableDispatcher();
    
	/**
	 * Signals the roll back of a transaction. The events of the transaction
	 * should not be acted on and should be undone by the SPPersisterListener.
	 * 
	 * @param message
	 *            Reason for the roll back.
	 */
    void rollback(String message);

    /**
     * Returns a list of all children of the given type
     */
    @NonProperty
    <T extends SPObject> List<T> getChildren(Class<T> type);
    
    /**
     * Returns a list of classes that are allowed to be children of this object.
     * If no children are allowed this will return an empty list.
     */
    @Transient @Accessor
    List<Class<? extends SPObject>> getAllowedChildTypes();

	/**
	 * Returns true if this object allows children of the given type. Returns
	 * false otherwise.
	 * 
	 * @param type
	 *            The class of object that is being decided on if it can be
	 *            added to this object as a child.
	 */
    boolean allowsChildType(Class<? extends SPObject> type);

	/**
	 * Enables or disables magic. Magic determines whether certain property
	 * changes trigger secondary side effects. Magic is enabled if and only if
	 * an equal number of <code>setMagicEnabled(true)</code> and
	 * <code>setMagicEnabled(false)</code> calls have been made.
	 * 
	 * @param enable
	 *            True if magic should be enabled, causing the disable magic
	 *            counter to decrement. False if magic should be disabled,
	 *            causing the enable magic counter to increment.
	 */
    @Transient @Mutator
    void setMagicEnabled(boolean enable);

	/**
	 * Returns true if magic is enabled, where some property changes can trigger
	 * secondary magical side effects.
	 */
    @Transient @Accessor
    boolean isMagicEnabled();
    
}
