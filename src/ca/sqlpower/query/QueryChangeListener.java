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

package ca.sqlpower.query;

import java.beans.PropertyChangeEvent;

/**
 * Classes can implement this interface to have a way to listen for changes
 * to a Query object.
 */
public interface QueryChangeListener {
    
    void joinAdded(QueryChangeEvent evt);
    
    void joinRemoved(QueryChangeEvent evt);

    /**
     * The {@link Query} will re-send property change events on individual
     * {@link SQLJoin}s that change that are in the query. This simplifies
     * classes that want to listen to all joins in the query so they only have
     * to listen to the query and not all of the items themselves.
     */
    void joinPropertyChangeEvent(PropertyChangeEvent evt);
    
    /**
     * The {@link Query} will re-send property change events on individual
     * items that change that are in the query. This simplifies classes that
     * want to listen to all items in the query so they only have to listen
     * to the query and not all of the items themselves.
     */
    void itemPropertyChangeEvent(PropertyChangeEvent evt);

    void itemAdded(QueryChangeEvent evt);
    
    void itemRemoved(QueryChangeEvent evt);
    
    /**
     * This will be fired if the position of a column was changed.
     */
    void itemOrderChanged(QueryChangeEvent evt);
    
    void containerAdded(QueryChangeEvent evt);
    
    void containerRemoved(QueryChangeEvent evt);

    /**
     * If the query is in a state that the query cannot be executed due to
     * a compound edit or other reasons this event will be fired when it
     * is valid to execute the query again. This way if a listener gets an
     * event that the query changed but cannot execute the query this method
     * will be called once the query is valid again.
     */
    void canExecuteQuery();
    
    /**
     * This is the property change events of the {@link Query} itself.
     */
    void propertyChangeEvent(PropertyChangeEvent evt);
}
