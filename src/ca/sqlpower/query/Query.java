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

import java.util.Collection;
import java.util.List;

import ca.sqlpower.sql.JDBCDataSource;
import ca.sqlpower.sqlobject.SQLDatabase;
import ca.sqlpower.sqlobject.SQLDatabaseMapping;

public interface Query {

    /**
     * Sets the UUID of this object to a newly generated UUID. This is necessary
     * if the object is being cloned or copied.
     */
    public abstract void generateNewUUID();

    public abstract SQLDatabaseMapping getDbMapping();

    public abstract void setDBMapping(SQLDatabaseMapping dbMapping);

    public abstract SQLDatabase getDatabase();

    public abstract void setGroupingEnabled(boolean enabled);

    /**
     * Generates the query based on the cache.
     */
    public abstract String generateQuery();

    /**
     * Returns true if the query this object represents contains a cross join between
     * two tables. Returns false otherwise.
     */
    public abstract boolean containsCrossJoins();

    public abstract List<Item> getSelectedColumns();

    public abstract List<Item> getOrderByList();

    /**
     * This will move a column that is being sorted to the end of the
     * list of columns that are being sorted.
     */
    public abstract void moveSortedItemToEnd(Item item);

    public abstract void removeTable(Container table);

    public abstract void addTable(Container container);

    /**
     * This setter will fire a property change event.
     */
    public abstract void setGlobalWhereClause(String whereClause);

    public abstract void removeJoin(SQLJoin joinLine);

    public abstract void addJoin(SQLJoin join);

    /**
     * This removes the item from all lists it could be
     * contained in as well as disconnect its listeners.
     */
    public abstract void removeItem(Item col);

    /**
     * This adds the appropriate listeners to the new Item.
     * <p>
     * XXX This method should not be public. Items should be
     * added to a container in the query or their container
     * should be added to a query. They should not be added
     * directly.
     */
    public abstract void addItem(Item col);

    public abstract void moveItem(Item movedColumn, int toIndex);

    /**
     * This notes that a compound edit has started. A message can be given to
     * describe what this compound edit is doing.
     */
    public abstract void startCompoundEdit(String message);

    public abstract void endCompoundEdit();

    public abstract boolean isGroupingEnabled();

    public abstract List<Container> getFromTableList();

    /**
     * This returns the joins between tables. Each join will be
     * contained only once.
     */
    public abstract Collection<SQLJoin> getJoins();

    public abstract String getGlobalWhereClause();

    public abstract Container getConstantsContainer();

    public abstract void setDataSource(JDBCDataSource dataSource);

    /**
     * If this is set then only this query string will be returned by the generateQuery method
     * and the query cache will not accurately represent the query.
     */
    public abstract void defineUserModifiedQuery(String query);

    /**
     * Returns true if the user manually edited the text of the query. Returns false otherwise.
     */
    public abstract boolean isScriptModified();

    /**
     * Resets the manual modifications the user did to the text of the query so the textual
     * query is the same as the query cache.
     */
    public abstract void removeUserModifications();

    /**
     * Creates a new constants container for this QueryCache. This should
     * only be used in loading.
     */
    public abstract Container newConstantsContainer(String uuid);

    public abstract void setZoomLevel(int zoomLevel);

    public abstract int getZoomLevel();

    public abstract String toString();

    public abstract String getUUID();

    public abstract String getName();

    public abstract void setName(String name);

    public abstract void setRowLimit(int rowLimit);

    public abstract int getRowLimit();

    public abstract void setStreamingRowLimit(int streamingRowLimit);

    public abstract int getStreamingRowLimit();

    public abstract void setStreaming(boolean streaming);

    public abstract boolean isStreaming();

    public abstract void addQueryChangeListener(QueryChangeListener l);

    public abstract void removeQueryChangeListener(QueryChangeListener l);

    /**
     * Resets the query to be as it was when it is first created.
     */
    public abstract void reset();

}