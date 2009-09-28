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

/**
 * An event signalling that a column was added to or removed from the list of columns
 * being ordered in the query. This list of ordered columns is different from the list
 * of columns selected as they can be in a different ordering.
 */
public class OrderByItemEvent {

    /**
     * The source of the event.
     */
    private final Query source;
    
    /**
     * The item being added to or removed from the order by list.
     */
    private final QueryItem item;
    
    /**
     * The index the item is be added to or removed from the query.
     */
    private final int index;

    /**
     * True if the item is being added to the order by list, false if it is
     * being removed.
     */
    private final boolean added;

    public Query getSource() {
        return source;
    }

    public QueryItem getItem() {
        return item;
    }

    public int getIndex() {
        return index;
    }

    public boolean isAdded() {
        return added;
    }

    public OrderByItemEvent(Query queryImpl, QueryItem item, int index,
            boolean added) {
                this.source = queryImpl;
                this.item = item;
                this.index = index;
                this.added = added;
    }

}
