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

import java.util.EventObject;

/**
 * An event that is passed to listeners when a child is added to or removed from
 * its parent.
 */
public class SPChildEvent extends EventObject {

    /**
     * The type of event that signals if a child is being added or removed.
     */
    public enum EventType {
        /**
         * Defines this event to be adding a child to the parent object.
         */
        ADDED,
        
        /**
         * Defines this event to be removing a child from the parent object.
         */
        REMOVED
    }

	/**
	 * The child type for which the parent gained or lost a child (
	 * SPObjects support multiple child types).
	 */
    private final Class<? extends SPObject> childType;
    
    /**
     * The child that was added or removed.
     */
    private final SPObject child;

    /**
     * XXX Is this correct? 
     * <p>
     * The index of the child that was added or removed. This index is the
     * overall position in the list returned by
     * <code>source.getChildren()</code>, not just the position within the
     * separate list of just these children). For example, if the source is a
     * Schema, and the added child is a Cube called newCube, this is the same as
     * <code>schema.getChildren().indexOf(newCube)</code>, not
     * <code>schema.getCubes().indexOf(newCube)</code>.
     */
    private final int index;

    /**
     * Defines if this event is adding a child to its parent or removing the
     * child from its parent.
     */
    private final EventType type;

	/**
	 * Creates a new event object that describes adding or removing a single
	 * child of the given type to/from a parent.
	 * 
	 * @param source
	 *            The parent that gained or lost a child.
	 * @param childType
	 *            The child type for which the parent gained or lost a child
	 *            (SPObjects support multiple child types).
	 * @param child
	 *            The child that was added or removed.
	 * @param index
	 *            XXX is this correct? The index of the child that was added or
	 *            removed (this is the overall index in the parent, not the
	 *            index within one child type).
	 */
    public SPChildEvent(SPObject source, 
    		Class<? extends SPObject> childType, 
    		SPObject child, int index, EventType type) {
    	super(source);
    	this.source = source;
        this.childType = childType;
        this.child = child;
        this.index = index;
        this.type = type;
    }

    public SPObject getSource() {
        return (SPObject) source;
    }

    public Class<? extends SPObject> getChildType() {
        return childType;
    }

    public SPObject getChild() {
        return child;
    }

    public int getIndex() {
        return index;
    }
    
    public EventType getType() {
        return type;
    }
    
    @Override
    public String toString() {
        return (type == EventType.ADDED ? "Child Added: [" : "Child Removed: [") + "Parent: " + source + "; child: " + child + "; index " + index + "]";
    }

}
