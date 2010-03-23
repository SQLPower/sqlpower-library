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
 * This event will be given on each change event that can occur on
 * a query.
 */
public class QueryChangeEvent {
    
    /**
     * This will be null if a join is not being modified in this change event.
     */
    private final SQLJoin joinChanged;
    
    /**
     * This will be null if an {@link Item} is not being modified in this change event.
     */
    private final Item itemChanged;
    
    /**
     * This will be null if a {@link Container} is not being modified in this event.
     */
    private final Container containerChanged;
    private final Query source;
    
    public QueryChangeEvent(Query source, SQLJoin joinChanged) {
        this.source = source;
        this.joinChanged = joinChanged;
        itemChanged = null;
        containerChanged = null;
    }
    
    public QueryChangeEvent(Query source, Item itemChanged) {
        this.source = source;
        this.itemChanged = itemChanged;
        joinChanged = null;
        containerChanged = null;
    }
    
    public QueryChangeEvent(Query source, Container containerChanged) {
        this.source = source;
        this.containerChanged = containerChanged;
        joinChanged = null;
        itemChanged = null;
    }

    public SQLJoin getJoinChanged() {
        return joinChanged;
    }

    public Query getSource() {
        return source;
    }

    public Item getItemChanged() {
        return itemChanged;
    }

    public Container getContainerChanged() {
        return containerChanged;
    }

}
