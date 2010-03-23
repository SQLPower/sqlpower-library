/*
 * Copyright (c) 2008, SQL Power Group Inc.
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

package ca.sqlpower.sqlobject;

import java.io.Serializable;

/**
 * Event object associated with the SQLObject pre-event system. Presently,
 * this event class does not support property change type events; this
 * will be added in the future if the need arises.
 */
public class SQLObjectPreEvent implements Serializable {
    
    private final SQLObject source;
    private final int[] changeIndices;
    private final SQLObject[] children;
    private boolean vetoed;
    
    /**
     * Use this constructor for DBChildrenInserted and
     * DBChildrenRemoved type events.  <code>propertyName</code> will be set to the
     * string "children".
     *
     * @param source The SQLObject that may undergo a change.
     * @param changeIndices The indices of the children that might be
     * added or removed.  The indices must be in ascending order.
     * @param children The actual SQLObject instances that might be added
     * or removed to/from the source object.
     */
    public SQLObjectPreEvent(SQLObject source, int[] changeIndices, SQLObject[] children) {
        this.source = source;
        this.changeIndices = changeIndices;
        this.children = children;
    }

    /**
     * Returns the child indices that may be added/removed. Do not modify
     * the returned array.
     */
    public int[] getChangeIndices() {
        return changeIndices;
    }

    /**
     * Returns the child objects that may be added/removed. Do not modify
     * the returned array.
     */
    public SQLObject[] getChildren() {
        return children;
    }

    /**
     * Returns the source object of this event. In the case of an add/remove
     * notification, this is the parent object that is imminently gaining or
     * losing children.
     */
    public SQLObject getSource() {
        return source;
    }

    /**
     * Sets the vetoed state of this event to true. This means that, once all
     * pre-event listeners have been notified of the impending change, the
     * source object will abort the operation it was about to carry out.  If none
     * of the listeners veto this event, it the source object should proceed
     * with the action (which will cause a corresponding SQLObject event to be
     * fired announcing that the change has actually taken place).
     */
    public void veto() {
        vetoed = true;
    }
    
    /**
     * Returns whether or not this event has been vetoed yet. In general, it is
     * only useful for the source SQLObject (the one firing the event) to check
     * this, and only then after all listeners have been notified. The reason
     * is, listeners have no way of knowing how many other listeners there are
     * for this event, or what order those listeners will be notified in. This
     * means that if a listener checks the vetoed state of the event, it is
     * likely to get a false negative response.
     */
    public boolean isVetoed() {
        return vetoed;
    }
}
