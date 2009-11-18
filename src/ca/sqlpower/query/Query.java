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

import ca.sqlpower.query.QueryImpl.OrderByArgument;
import ca.sqlpower.sql.JDBCDataSource;
import ca.sqlpower.sqlobject.SQLDatabase;
import ca.sqlpower.sqlobject.SQLDatabaseMapping;

public interface Query {

    public abstract SQLDatabaseMapping getDBMapping();

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

    /**
     * Returns a list of the {@link Item}s that are selected in the query in
     * the order they are selected. 
     */
    public abstract List<Item> getSelectedColumns();

    /**
     * Returns a list of {@link Item}s that define the order that columns
     * should be sorted in. The order by list is different from the selected
     * items list in the fact that not all selected items are ordered and the
     * items selected can be selected in an order that is different from how the
     * items are ordered. For example, the last item selected in a query could
     * be the first to be sorted by, then the first item in a query could be the
     * second item to be sorted by and the rest of the items in the query may
     * not be used to sort the query at all.
     * 
     * @return The list of {@link QueryItem}s that represents the order the
     *         items are sorted in.
     */
    public abstract List<Item> getOrderByList();

    /**
     * This method will move the given item to a different position in the order
     * by list. The given index cannot be larger than the number of items in the
     * order by list.
     * 
     * @param item
     *            The item to move in the order by list. This item must already
     *            exist in the order by list.
     * @param index
     *            The index to move the item to. This cannot be greater than the
     *            number of items in the order by list.
     */
    public abstract void moveOrderBy(Item item, int index);

    /**
     * This will move a column that is being sorted to the end of the
     * list of columns that are being sorted.
     */
    public abstract void moveOrderByItemToEnd(Item item);

    /**
     * Removes the given container that represents a table from this query if it
     * exists.
     * 
     * @param table
     *            The table container to remove.
     */
    public abstract void removeTable(Container table);

    /**
     * Adds the given container that represents a table to the query. The
     * container will be added to the end of the list of containers in the
     * query. Each container can only be added to this query once and if a
     * container is added to this query when it is already in the query an
     * {@link IllegalArgumentException} will be thrown.
     * 
     * @param container
     *            The container to remove.
     */
    public abstract void addTable(Container container);

    /**
     * Adds the given container that represents a table to the query at the
     * given index in the list of currently existing tables. The index cannot be
     * greater than the number of tables currently in the query. Each container
     * can only be added to this query once and if a container is added to this
     * query when it is already in the query an {@link IllegalArgumentException}
     * will be thrown.
     * 
     * @param container
     *            The container to add to the query.
     * @param index
     *            The index to add the container at.
     */
    public abstract void addTable(Container container, int index);

    /**
     * This setter will fire a property change event.
     */
    public abstract void setGlobalWhereClause(String whereClause);

    /**
     * Removes the given join from this query if it exists.
     * 
     * @param joinLine
     *            The join to remove.
     */
    public abstract void removeJoin(SQLJoin joinLine);

    /**
     * Adds the given join to the query. The join will be added to the end of
     * the list of joins in this query. The two items that this join is
     * connecting must be in the query. If the join being added already exists
     * in the query an {@link IllegalArgumentException} will be thrown.
     * 
     * @param join
     *            The join to add.
     */
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

    /**
     * Moves an {@link Item} currently selected in the query to a new position
     * in the selection list in the query. This allows for easy rearrangement of
     * selected items.
     * 
     * @param movedColumn
     *            The selected item to move in the list of selected items.
     * @param toIndex
     *            The index to move the selected items to.
     */
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
    
    /**
     * Returns a unique identifier for this query.
     */
    public abstract String getUUID();

    public abstract String getGlobalWhereClause();

    public abstract Container getConstantsContainer();

	/**
	 * This sets the query's data source to the given data source. As a side
	 * effect it also resets the query, removing all tables, and resetting the
	 * constants table. The query's streaming flag will be set to the data
	 * source type's streaming flag as well.
	 * 
	 * @return True if the data source was set, false otherwise.
	 */
    public abstract void setDataSource(JDBCDataSource dataSource);

	/**
	 * This sets the data source without resetting the query. This should only
	 * be called from loading code or when no side effects are desired. If the
	 * user is trying to set the data source use
	 * {@link #setDataSource(JDBCDataSource)} so the tables in the query are
	 * removed as well.
	 * 
	 * @return True if the data source was set, false otherwise.
	 */
    public abstract boolean setDataSourceWithoutSideEffects(JDBCDataSource dataSource);
    
    /**
     * Returns the data source that the query is based on.
     */
    public JDBCDataSource getDataSource();

    /**
     * If this is set then only this query string will be returned by the generateQuery method
     * and the query cache will not accurately represent the query. If the query should be generated
     * by the objects in this class this query must be set to null.
     */
    public abstract void setUserModifiedQuery(String query);

    /**
     * Returns the query defined by the user that was typed by hand instead of using tables
     * and columns. This will be null if the user is using the query with tables and
     * columns instead.
     * TODO Make a parser that will convert the query defined by hand to tables and containers
     * to keep the two in sync. This is fairly difficult so be warned.
     */
    public abstract String getUserModifiedQuery();

    /**
     * Returns true if the user manually edited the text of the query. Returns false otherwise.
     */
    public abstract boolean isScriptModified();

    /**
     * Resets the manual modifications the user did to the text of the query so the textual
     * query is the same as the query cache.
     */
    public abstract void removeUserModifications();

    public abstract void setZoomLevel(int zoomLevel);

    public abstract int getZoomLevel();

    public abstract String toString();

    public abstract String getName();

    public abstract void setName(String name);

    public abstract void setRowLimit(int rowLimit);

    public abstract int getRowLimit();

    public abstract void setStreamingRowLimit(int streamingRowLimit);

    public abstract int getStreamingRowLimit();

    /**
     * If set to true the query will act as a streaming query. Streaming queries
     * act differently than a regular query as their results come in over time
     * instead of in one batch.
     */
    public abstract void setStreaming(boolean streaming);

    /**
     * If true the query will act as a streaming query. Streaming queries
     * act differently than a regular query as their results come in over time
     * instead of in one batch.
     */
    public abstract boolean isStreaming();

    public abstract void addQueryChangeListener(QueryChangeListener l);

    public abstract void removeQueryChangeListener(QueryChangeListener l);

    /**
     * Resets the query to be as it was when it is first created.
     */
    public abstract void reset();
    
    /**
     * Returns the index of the given item in the list of selected items.
     * If the item is not in the list of selected items it will return -1.
     * @param item
     * @return
     */
    public abstract int indexOfSelectedItem(Item item);

    /**
     * Removes the given item from being selected. The other selected items in 
     * the query may need to be updated.
     */
    public abstract void unselectItem(Item item);

    /**
     * Sets the given item's selection value to be a valid value with respect to
     * the other items selected in this query. This item will be added to the
     * end of the selection 'list'.
     * <p>
     * If the item already exists in the query this will do nothing.
     */
    public abstract void selectItem(Item item);

    /**
     * Sets the given item's ordering to be the given ordering value. The item
     * will have its order by ordering set to be after the last item previously
     * being ordered if it is being ordered. All of the other items being
     * ordered may be shifted in their ordering if the item is moved.
     */
    public abstract void orderColumn(Item item, OrderByArgument ordering);
    
}