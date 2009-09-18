/*
 * Copyright (c) 2008, SQL Power Group Inc.
 *
 * This file is part of Wabit.
 *
 * Wabit is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * Wabit is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>. 
 */

package ca.sqlpower.query;

import java.awt.geom.Point2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.annotation.concurrent.GuardedBy;

import org.apache.log4j.Logger;

import ca.sqlpower.graph.DepthFirstSearch;
import ca.sqlpower.graph.GraphModel;
import ca.sqlpower.sql.JDBCDataSource;
import ca.sqlpower.sqlobject.SQLColumn;
import ca.sqlpower.sqlobject.SQLDatabase;
import ca.sqlpower.sqlobject.SQLDatabaseMapping;
import ca.sqlpower.sqlobject.SQLObjectException;
import ca.sqlpower.sqlobject.SQLTable;

/**
 * This class will cache all of the parts of a select
 * statement and also listen to everything that could
 * change the select statement.
 */
public class QueryImpl implements Query {
	
	private static final Logger logger = Logger.getLogger(QueryImpl.class);
	
    /**
     * If the row limit changes causing the result set cache to become empty
     * a change event will fire with this property.
     */
    protected static final String ROW_LIMIT = "rowLimit";
	
	/**
	 * A property name that is thrown in PropertyChangeListeners when part of
	 * the query has changed. This is a generic default change to a query
	 * rather than a specific query change.
	 */
	public static final String PROPERTY_QUERY = "query";
	
	/**
	 * This is the property name for grouping enabled.
	 */
	public static final String GROUPING_ENABLED = "groupingEnabled";
	
	/**
	 * This is the property name for the global where clause text.
	 */
	private static final String GLOBAL_WHERE_CLAUSE = "globalWhereClause";
	
	/**
	 * The arguments that can be added to a column in the 
	 * order by clause.
	 */
	public enum OrderByArgument {
		ASC,
		DESC,
		NONE
	}
	
	/**
	 * This graph represents the tables in the SQL statement. Each table in
	 * the statement is a vertex in the graph. Each join is an edge in the 
	 * graph coming from the left table and moving towards the right table.
	 * This class is package private for testing.
	 */
	class TableJoinGraph implements GraphModel<Container, SQLJoin> {

		public Collection<Container> getAdjacentNodes(Container node) {
			List<Container> adjacencyNodes = new ArrayList<Container>();
			if (joinMapping.get(node) != null) {
				for (SQLJoin join : joinMapping.get(node)) {
					if (join.getLeftColumn().getContainer() == node) {
						adjacencyNodes.add(join.getRightColumn().getContainer());
					} else {
					    adjacencyNodes.add(join.getLeftColumn().getContainer());
					}
				}
			}
			return adjacencyNodes;
		}

		public Collection<SQLJoin> getEdges() {
			List<SQLJoin> edgesList = new ArrayList<SQLJoin>();
			for (List<SQLJoin> joinList : joinMapping.values()) {
				for (SQLJoin join : joinList) {
					edgesList.add(join);
				}
			}
			return edgesList;
		}

		public Collection<SQLJoin> getInboundEdges(Container node) {
			List<SQLJoin> inboundEdges = new ArrayList<SQLJoin>();
			if (joinMapping.get(node) != null) {
				for (SQLJoin join : joinMapping.get(node)) {
				    inboundEdges.add(join);
				}
			}
			return inboundEdges;
		}

		public Collection<Container> getNodes() {
			return fromTableList;
		}

		public Collection<SQLJoin> getOutboundEdges(Container node) {
			List<SQLJoin> outboundEdges = new ArrayList<SQLJoin>();
			if (joinMapping.get(node) != null) {
				for (SQLJoin join : joinMapping.get(node)) {
				    outboundEdges.add(join);
				}
			}
			return outboundEdges;
		}
		
	}

	/**
	 * Tracks if there are groupings added to this select statement.
	 * This will affect when columns are added to the group by collections.
	 */
	private boolean groupingEnabled = false;

	/**
	 * The columns in the SELECT statement that will be returned.
	 * These columns are stored in the order they will be returned
	 * in.
	 */
	private final List<Item> selectedColumns;
	
	/**
	 * The order by list keeps track of the order that columns were selected in.
	 */
	private final List<Item> orderByList;
	
	/**
	 * The list of tables that we are selecting from.
	 */
	private final List<Container> fromTableList;
	
	/**
	 * This maps each table to a list of SQLJoin objects.
	 * These column pairs defines a join in the select statement.
	 */
	private final Map<Container, List<SQLJoin>> joinMapping;
	
	/**
	 * This is the global where clause that is for all non-column-specific where
	 * entries.
	 */
	private String globalWhereClause;
	
	/**
	 * This is the level of the zoom in the query.
	 */
	private int zoomLevel;
	
	/**
	 * The number times {@link #startCompoundEdit()} has been called without
	 * {@link #endCompoundEdit()} being called. When this level goes from 1 to
	 * 0 an end compound edit event will occur.
	 */
	private int compoundEditLevel = 0;
	
	@GuardedBy("changeListeners")
	private final List<QueryChangeListener> changeListeners = new ArrayList<QueryChangeListener>();

    /**
     * Listens for changes to the item and fires events to its listeners.
     */
	private PropertyChangeListener itemListener = new PropertyChangeListener() {
		public void propertyChange(PropertyChangeEvent e) {
		    Item selectedItem = (Item) e.getSource();
			if (e.getPropertyName().equals(Item.SELECTED)) {
		        if ((Boolean) e.getNewValue()) {
		            selectedColumns.add(selectedItem);
		            if (!selectedItem.getOrderBy().equals(OrderByArgument.NONE)) {
		            	orderByList.add(selectedItem);
		            }
		        } else {
		            selectedColumns.remove(selectedItem);
		            orderByList.remove(e.getSource());
		        }
		    } else if (e.getPropertyName().equals(Item.ORDER_BY)) {
		        orderByList.remove(e.getSource());
		        if (!e.getNewValue().equals(OrderByArgument.NONE)) {
		            orderByList.add(selectedItem);
		        }
		    }
		    
		    fireItemPropertyChangeEvent(e);
		}
	};

    /**
     * This change listener will re-send the change event to listeners on this
     * query. This will also keep the map of {@link Container}s to the joins on
     * them in order.
     */
	private PropertyChangeListener joinChangeListener = new PropertyChangeListener() {
		public void propertyChange(PropertyChangeEvent e) {
		    //XXX This if/else looks like leftovers from correcting listeners in Wabit and probably just needs to be deleted.
			if (e.getPropertyName().equals(SQLJoin.LEFT_JOIN_CHANGED)) {
				logger.debug("Got left join changed.");
				SQLJoin changedJoin = (SQLJoin) e.getSource();
				Container leftJoinContainer = changedJoin.getLeftColumn().getContainer();
				for (SQLJoin join : joinMapping.get(leftJoinContainer)) {
					if (join.getLeftColumn().getContainer() == leftJoinContainer) {
						join.setLeftColumnOuterJoin((Boolean)e.getNewValue());
					} else {
						join.setRightColumnOuterJoin((Boolean)e.getNewValue());
					}
				}
			} else if (e.getPropertyName().equals(SQLJoin.RIGHT_JOIN_CHANGED)) {
				logger.debug("Got right join changed.");
				SQLJoin changedJoin = (SQLJoin) e.getSource();
				Container rightJoinContainer = changedJoin.getRightColumn().getContainer();
				logger.debug("There are " + joinMapping.get(rightJoinContainer) + " joins on the table with the changed join.");
				for (SQLJoin join : joinMapping.get(rightJoinContainer)) {
					if (join.getLeftColumn().getContainer() == rightJoinContainer) {
						logger.debug("Changing left side");
						join.setLeftColumnOuterJoin((Boolean)e.getNewValue());
					} else {
						logger.debug("Changing right side");
						join.setRightColumnOuterJoin((Boolean)e.getNewValue());
					}
				}
			}
			fireJoinPropertyChangeEvent(e);
		}
	};
	
	private final ContainerChildListener tableChildListener = new ContainerChildListener() {
		public void containerChildRemoved(ContainerChildEvent e) {
			removeItem(e.getChild());
		}
		public void containerChildAdded(ContainerChildEvent e) {
			addItem(e.getChild());
		}
	}; 
	
	/**
	 * This container holds the items that are considered constants in the SQL statement.
	 * This could include functions or other elements that don't belong in a table.
	 */
	private Container constantsContainer;
	
	/**
	 * This database instance is obtained from the session when the 
	 * data source is called.
	 */
	private SQLDatabase database;
	
	/**
	 * This is the text of the query if the user edited the text manually. This means
	 * that the parts of the query cache will not represent the new query text the user
	 * created. If this is null then the user did not change the query manually.
	 */
	private String userModifiedQuery = null;
	
	private SQLDatabaseMapping dbMapping;
	
	private String uuid;
	
	private String name;
	
    /**
     * This is the streaming row limit for the query. No more than this many
     * rows will be shown in the streaming result set and if this limit is
     * reached and new rows are added then the oldest rows in the result set
     * will be removed.
     */
    private int streamingRowLimit = 1000;
    
    /**
     * This is the row limit of a standard query. 
     */
    private int rowLimit = 1000;
    
    /**
     * Tracks if the data source should be used as a streaming query or as a regular
     * query. Streaming queries are populated on their own thread.
     */
    private boolean streaming = false;

    /**
     * A property change of this type is fired if the user defined
     * text of the query is modified. Property changes to the objects
     * maintained and monitored by this query will not contain this
     * type.
     */
    public static final String USER_MODIFIED_QUERY = "userModifiedQuery";

    /**
     * A property name that is thrown when the Table is removed.
     */
    public static final String PROPERTY_TABLE_REMOVED = "PROPERTY_TABLE_REMOVED";

	public QueryImpl(SQLDatabaseMapping dbMapping) {
		this((String)null, dbMapping);
	}
	
	/**
	 * The uuid defines the unique id of this query cache. If null
	 * is passed in a new UUID will be generated.
	 */
	public QueryImpl(String uuid, SQLDatabaseMapping dbMapping) {
		if (uuid == null) {
		    generateNewUUID();
		} else {
		    this.uuid = uuid;
		}
        this.dbMapping = dbMapping;
		orderByList = new ArrayList<Item>();
		selectedColumns = new ArrayList<Item>();
		fromTableList = new ArrayList<Container>();
		joinMapping = new HashMap<Container, List<SQLJoin>>();
		
		constantsContainer = new ItemContainer("Constants");
		resetConstantsContainer();
		
	}
	
    /* (non-Javadoc)
     * @see ca.sqlpower.query.Query#generateNewUUID()
     */
	public void generateNewUUID() {
	    uuid = "w" + UUID.randomUUID().toString();
	}

    /**
     * This method will remove all of the current items in the constants
     * container and the add in the default constants.
     */
    private void resetConstantsContainer() {
        constantsContainer.setPosition(new Point2D.Double(0, 0));
        for (int i = constantsContainer.getItems().size() - 1; i >= 0; i--) {
            constantsContainer.removeItem(i);
        }
        
        StringItem currentTime = new StringItem("current_time");
		constantsContainer.addItem(currentTime);
		addItem(currentTime);
		StringItem currentDate = new StringItem("current_date");
		constantsContainer.addItem(currentDate);
		addItem(currentDate);
		StringItem user = new StringItem("user");
		constantsContainer.addItem(user);
		addItem(user);
		StringItem countStar = new StringItem("count(*)");
		constantsContainer.addItem(countStar);
		addItem(countStar);
    }
	
	public QueryImpl(QueryImpl copy, boolean connectListeners) {
		this(copy, connectListeners, copy.getDatabase());
	}
	
	/**
	 * A copy constructor for the query cache. This will not
	 * hook up listeners.
	 */
	public QueryImpl(QueryImpl copy, boolean connectListeners, SQLDatabase database) {
	    generateNewUUID();
		selectedColumns = new ArrayList<Item>();
		fromTableList = new ArrayList<Container>();
		joinMapping = new HashMap<Container, List<SQLJoin>>();
		orderByList = new ArrayList<Item>();
		
		this.dbMapping = copy.dbMapping;
		setName(copy.getName());

		Map<Container, Container> oldToNewContainers = new HashMap<Container, Container>();
		for (Container table : copy.getFromTableList()) {
		    final Container tableCopy = table.createCopy();
		    oldToNewContainers.put(table, tableCopy);
            fromTableList.add(tableCopy);
		}
		
		constantsContainer = copy.getConstantsContainer().createCopy();
		oldToNewContainers.put(copy.getConstantsContainer(), constantsContainer);
		
		for (Item column : copy.getSelectedColumns()) {
		    Container table = oldToNewContainers.get(column.getParent());
		    Item item = table.getItem(column.getItem());
		    selectedColumns.add(item);
		}

		Set<SQLJoin> joinSet = new HashSet<SQLJoin>();
		for (Map.Entry<Container, List<SQLJoin>> entry : copy.getJoinMapping().entrySet()) {
		    joinSet.addAll(entry.getValue());
		}
		
		Set<SQLJoin> newJoinSet = new HashSet<SQLJoin>();
		for (SQLJoin oldJoin : joinSet) {
		    Container newLeftContainer = oldToNewContainers.get(oldJoin.getLeftColumn().getContainer());
		    Item newLeftItem = newLeftContainer.getItem(oldJoin.getLeftColumn().getItem());
		    Container newRightContainer = oldToNewContainers.get(oldJoin.getRightColumn().getContainer());
		    Item newRightItem = newRightContainer.getItem(oldJoin.getRightColumn().getItem());
		    SQLJoin newJoin = oldJoin.createCopy(newLeftItem, newRightItem);
		    newJoinSet.add(newJoin);
		    
		    List<SQLJoin> newJoinList = joinMapping.get(newLeftContainer);
		    if (newJoinList == null) {
		        newJoinList = new ArrayList<SQLJoin>();
		        joinMapping.put(newLeftContainer, newJoinList);
		    }
		    newJoinList.add(newJoin);
		    
		    newJoinList = joinMapping.get(newRightContainer);
            if (newJoinList == null) {
                newJoinList = new ArrayList<SQLJoin>();
                joinMapping.put(newRightContainer, newJoinList);
            }
            newJoinList.add(newJoin);
		}

		for (Item column : copy.getOrderByList()) {
		    Container table = oldToNewContainers.get(column.getParent());
		    Item item = table.getItem(column.getItem());
		    orderByList.add(item);
		}
		globalWhereClause = copy.getGlobalWhereClause();
		groupingEnabled = copy.isGroupingEnabled();
		streaming = copy.isStreaming();

		this.database = database;
		userModifiedQuery = copy.getUserModifiedQuery();
		
		if (connectListeners) {
		    for (Container table : fromTableList) {
		        table.addChildListener(getTableChildListener());
		        for (Item column : table.getItems()) {
		            column.addPropertyChangeListener(itemListener);
		        }
		    }
		    //XXX does the constants container not need a child listener?
		    for (Item column : constantsContainer.getItems()) {
		        column.addPropertyChangeListener(itemListener);
		    }
		    for (SQLJoin join : newJoinSet) {
		        join.addJoinChangeListener(joinChangeListener);
		    }
		}
	}
	
	/* (non-Javadoc)
     * @see ca.sqlpower.query.Query#getDbMapping()
     */
	public SQLDatabaseMapping getDbMapping() {
		return dbMapping;
	}
	
	/* (non-Javadoc)
     * @see ca.sqlpower.query.Query#setDBMapping(ca.sqlpower.sqlobject.SQLDatabaseMapping)
     */
	public void setDBMapping(SQLDatabaseMapping dbMapping) {
	    this.dbMapping = dbMapping;
	}
	
	/* (non-Javadoc)
     * @see ca.sqlpower.query.Query#getDatabase()
     */
	public SQLDatabase getDatabase() {
        return database;
    }
	
	/* (non-Javadoc)
     * @see ca.sqlpower.query.Query#setGroupingEnabled(boolean)
     */
	public void setGroupingEnabled(boolean enabled) {
		logger.debug("Setting grouping enabled to " + enabled);
		if (!groupingEnabled && enabled) {
			startCompoundEdit("Defining the grouping function of string items to be count.");
			for (Item item : getSelectedColumns()) {
				if (item instanceof StringItem) {
					item.setGroupBy(SQLGroupFunction.COUNT);
				}
			}
			endCompoundEdit();
		}
		firePropertyChangeEvent(new PropertyChangeEvent(this, GROUPING_ENABLED, groupingEnabled, enabled));
		groupingEnabled = enabled;
	}
	
	/**
	 * Removes the column from the selected columns list and all other
	 * related lists.
	 */
	private void removeColumnSelection(Item column) {
		selectedColumns.remove(column);
		orderByList.remove(column);
	}
	
	/* (non-Javadoc)
     * @see ca.sqlpower.query.Query#generateQuery()
     */
	public String generateQuery() {
	    JDBCDataSource dataSource = null;
	    if (database != null) {
            dataSource = database.getDataSource();
	    }
        logger.debug("Data source is " + dataSource + " while generating the query.");
		ConstantConverter converter = ConstantConverter.getConverter(dataSource);
		if (userModifiedQuery != null) {
			return userModifiedQuery;
		}
		
		String quoteString = "";
		Connection con = null;
		if (database != null) {
		    try {
		        con = database.getConnection();
		        quoteString = con.getMetaData().getIdentifierQuoteString();
		    } catch (SQLObjectException e) {
		        // Don't throw an exception for cases where we can't connect to the
		        // database but the user still wants to view or save the query. If
		        // this throws an exception going, to the SQL text tab will
		        // cause an exception.
		    } catch (SQLException e) {
		        // Don't throw an exception for cases where we can't connect to the
		        // database but the user still wants to view or save the query. If
		        // this throws an exception going, to the SQL text tab will
		        // cause an exception.
		    } finally {
		        if (con != null) {
		            try {
		                con.close();
		            } catch (SQLException e) {
		                logger.error(e);
		            }
		        }
		    }
		}

		if (selectedColumns.size() ==  0) {
			return "";
		}
		StringBuffer query = new StringBuffer();
		query.append("SELECT");
		boolean isFirstSelect = true;
		for (Item col : selectedColumns) {
			if (isFirstSelect) {
				query.append(" ");
				isFirstSelect = false;
			} else {
				query.append(", ");
			}
			
			if (isColumnGrouped(col)) {
				query.append(col.getGroupBy() + "(");
			}
			String alias = col.getContainer().getAlias();
			if (alias != null && alias.length() > 0) {
				query.append(quoteString + alias + quoteString + ".");
			} else if (fromTableList.contains(col.getContainer())) {
				query.append(quoteString + col.getContainer().getName() + quoteString + ".");
			}
			query.append(getColumnName(col, quoteString, converter));
			if (isColumnGrouped(col)) {
				query.append(")");
			}
			if (col.getAlias() != null && col.getAlias().trim().length() > 0) {
				query.append(" AS " + quoteString + col.getAlias() + quoteString);
			} else if (isGroupingEnabled() && col.getGroupBy() != null && col.getGroupBy() != SQLGroupFunction.GROUP_BY) {
			    query.append(" AS " + quoteString + col.getGroupBy() + "_" + col.getName() + quoteString);
			}
		}
		if (!fromTableList.isEmpty()) {
			query.append(" \nFROM");
		}
		boolean isFirstFrom = true;
		
		DepthFirstSearch<Container, SQLJoin> dfs = new DepthFirstSearch<Container, SQLJoin>();
		dfs.performSearch(new TableJoinGraph());
		Container previousTable = null;
		for (Container table : dfs.getFinishOrder()) {
			String qualifiedName;
			if (table.getContainedObject() instanceof SQLTable) {
				qualifiedName = ((SQLTable)table.getContainedObject()).toQualifiedName(quoteString);
			} else {
				qualifiedName = table.getName();
			}
			String alias = table.getAlias();
			if (alias == null || alias.length() <= 0) {
				alias = table.getName();
			}
			alias = quoteString + alias + quoteString;
			if (isFirstFrom) {
				query.append(" " + qualifiedName + " " + alias);
				isFirstFrom = false;
			} else {
				boolean joinFound = false;
				if (previousTable != null && joinMapping.get(table) != null) {
					for (SQLJoin join : joinMapping.get(table)) {
						if (join.getLeftColumn().getContainer() == previousTable) {
							joinFound = true;
							if (join.isLeftColumnOuterJoin() && join.isRightColumnOuterJoin()) {
								query.append(" \nFULL OUTER JOIN ");
							} else if (join.isLeftColumnOuterJoin() && !join.isRightColumnOuterJoin()) {
								query.append(" \nLEFT OUTER JOIN ");
							} else if (!join.isLeftColumnOuterJoin() && join.isRightColumnOuterJoin()) {
								query.append(" \nRIGHT OUTER JOIN ");
							} else {
								query.append(" \nINNER JOIN ");
							}
							break;
						}
					}
				}
				if (!joinFound) {
					query.append(" \nINNER JOIN ");
				}
				query.append(qualifiedName + " " + alias + " \n  ON ");
				if (joinMapping.get(table) == null || joinMapping.get(table).isEmpty()) {
					query.append("0 = 0");
				} else {
					boolean isFirstJoin = true;
					for (SQLJoin join : joinMapping.get(table)) {
						Item otherColumn;
						if (join.getLeftColumn().getContainer() == table) {
							otherColumn = join.getRightColumn();
						} else {
							otherColumn = join.getLeftColumn();
						}
						for (int i = 0; i < dfs.getFinishOrder().indexOf(table); i++) {
							if (otherColumn.getContainer() == dfs.getFinishOrder().get(i)) {
								if (isFirstJoin) {
									isFirstJoin = false;
								} else {
									query.append(" \n    AND ");
								}
								String leftAlias = join.getLeftColumn().getContainer().getAlias();
								if (leftAlias == null || leftAlias.length() <= 0) {
									leftAlias = join.getLeftColumn().getContainer().getName();
								}
								String rightAlias = join.getRightColumn().getContainer().getAlias();
								if (rightAlias == null || rightAlias.length() <= 0) {
									rightAlias = join.getRightColumn().getContainer().getName();
								}
								query.append(quoteString + leftAlias + quoteString + "." + getColumnName(join.getLeftColumn(), quoteString, converter) + 
										" " + join.getComparator() + " " + 
										quoteString + rightAlias + quoteString + "." + getColumnName(join.getRightColumn(), quoteString, converter));
							}
						}
					}
					if (isFirstJoin) {
						query.append("0 = 0");
					}
				}
			}
			previousTable = table;
		}
		query.append(" ");
		boolean isFirstWhere = true;
		Map<Item, String> whereMapping = new HashMap<Item, String>();
		for (Item item : constantsContainer.getItems()) {
			if (item.getWhere() != null && item.getWhere().trim().length() > 0) {
				whereMapping.put(item, item.getWhere());
			}
		}
		for (Container container : fromTableList) {
			for (Item item : container.getItems()) {
				if (item.getWhere() != null && item.getWhere().trim().length() > 0) {
					whereMapping.put(item, item.getWhere());
				}
			}
		}
		for (Map.Entry<Item, String> entry : whereMapping.entrySet()) {
			if (entry.getValue().length() > 0) {
				if (isFirstWhere) {
					query.append(" \nWHERE ");
					isFirstWhere = false;
				} else {
					query.append(" AND ");
				}
				String alias = entry.getKey().getContainer().getAlias();
				if (alias != null && alias.length() > 0) {
					query.append(quoteString + alias + quoteString + ".");
				} else if (fromTableList.contains(entry.getKey().getContainer())) {
					query.append(quoteString + entry.getKey().getContainer().getName() + quoteString + ".");
				}
				query.append(getColumnName(entry.getKey(), quoteString, converter) + " " + entry.getValue());
			}
		}
		if ((globalWhereClause != null && globalWhereClause.length() > 0)) {
			if (!isFirstWhere) {
				query.append(" AND"); 
			} else {
				query.append(" \nWHERE ");
			}
			query.append(" " + globalWhereClause);
		}
		if (groupingEnabled) {
		    boolean isFirstGroupBy = true;
		    for (Item col : selectedColumns) {
		        if (col.getGroupBy().equals(SQLGroupFunction.GROUP_BY) && !isStringItemAggregated(col)) {
		            if (isFirstGroupBy) {
		                query.append("\nGROUP BY ");
		                isFirstGroupBy = false;
		            } else {
		                query.append(", ");
		            }
		            String alias = col.getContainer().getAlias();
		            if (alias != null && alias.length() > 0) {
		                query.append(quoteString + alias + quoteString + ".");
		            } else if (fromTableList.contains(col.getContainer())) {
		                query.append(quoteString + col.getContainer().getName() + quoteString + ".");
		            }
		            query.append(getColumnName(col, quoteString, converter));
		        }
		    }
		    query.append(" ");
		    boolean isFirstHaving = true;
		    for (Item column : selectedColumns) {
		        if (column.getHaving() != null && column.getHaving().trim().length() > 0) {
		            if (isFirstHaving) {
		                query.append("\nHAVING ");
		                isFirstHaving = false;
		            } else {
		                query.append(" AND ");
		            }
                    if (isColumnGrouped(column)) {
		                query.append(column.getGroupBy() + "(");
		            }
		            String alias = column.getContainer().getAlias();
		            if (alias != null && alias.length() > 0) {
		                query.append(quoteString + alias + quoteString + ".");
		            } else if (fromTableList.contains(column.getContainer())) {
		                query.append(quoteString + column.getContainer().getName() + quoteString + ".");
		            }
		            query.append(getColumnName(column, quoteString, converter));
		            if (isColumnGrouped(column)) {
		                query.append(")");
		            }
		            query.append(" ");
		            query.append(column.getHaving());
		        }
		    }
		    query.append(" ");
		}
		
		if (!orderByList.isEmpty()) {
			boolean isFirstOrder = true;
			for (Item col : orderByList) {
				if (isFirstOrder) {
					query.append("\nORDER BY ");
					isFirstOrder = false;
				} else {
					query.append(", ");
				}
				if (groupingEnabled && !col.getGroupBy().equals(SQLGroupFunction.GROUP_BY)) {
					query.append(col.getGroupBy() + "(");
				}
				String alias = col.getContainer().getAlias();
				if (alias != null && alias.length() > 0) {
					query.append(quoteString + alias + quoteString + ".");
				} else if (fromTableList.contains(col.getContainer())) {
					query.append(quoteString + col.getContainer().getName() + quoteString + ".");
				}
				query.append(getColumnName(col, quoteString, converter));
				if (groupingEnabled && !col.getGroupBy().equals(SQLGroupFunction.GROUP_BY)) {
					query.append(")");
				}
				query.append(" ");
				if (!col.getOrderBy().equals(OrderByArgument.NONE)) {
					query.append(col.getOrderBy().toString() + " ");
				}
			}
		}
		logger.debug(" Query is : " + query.toString());
		return query.toString();
	}

    /**
     * Returns true if the column given is wrapped by a grouping function and we
     * are actually grouping. The allowed grouping functions are stored in
     * {@link SQLGroupFunction}. Returns false otherwise.
     */
    private boolean isColumnGrouped(Item col) {
        return groupingEnabled && !col.getGroupBy().equals(SQLGroupFunction.GROUP_BY) 
        		&& !isStringItemAggregated(col);
    }
	
	/* (non-Javadoc)
     * @see ca.sqlpower.query.Query#containsCrossJoins()
     */
	public boolean containsCrossJoins() {
	    DepthFirstSearch<Container, SQLJoin> dfs = new DepthFirstSearch<Container, SQLJoin>();
	    final TableJoinGraph graph = new TableJoinGraph();
        dfs.performSearch(graph);
        //If each container is connected to at least one of the containers
        //that came before it in the finish order there will be no cross joins
        List<Container> previousContainers = new ArrayList<Container>();
        if (dfs.getFinishOrder().size() == 0) return false;
        previousContainers.add(dfs.getFinishOrder().get(0));
	    for (int i = 1; i < dfs.getFinishOrder().size(); i++) {
	        Container container = dfs.getFinishOrder().get(i);
	        
	        boolean connected = false;
	        List<SQLJoin> list = joinMapping.get(container);
			if (list == null) return true;
	        for (SQLJoin join : list) {
	            Container leftContainer = join.getLeftColumn().getParent();
	            Container rightContainer = join.getRightColumn().getParent();
	            if ((leftContainer == container && previousContainers.contains(rightContainer))
	                || (rightContainer == container && previousContainers.contains(leftContainer))) {
	                connected = true;
	                break;
	            }
	        }
	        if (!connected) {
	            return true;
	        }
	        previousContainers.add(container);
	    }
	    return false;
	}

	/**
	 * If the item passed in is a {@link StringItem} and it starts with an aggregator
	 * (like sum, count, avg) true will be returned. False will be returned otherwise.
	 */
	private boolean isStringItemAggregated(Item col) {
		StringBuffer groupingRegex = new StringBuffer();
		for (SQLGroupFunction function : SQLGroupFunction.values()) {
			if (function != SQLGroupFunction.GROUP_BY) {
				if (groupingRegex.length() == 0) {
					groupingRegex.append("(");
				} else {
					groupingRegex.append("|");
				}
				groupingRegex.append(function.getGroupingName());
			}
		}
		groupingRegex.append(").*");
		boolean isStringItemAndAggregated = col instanceof StringItem 
				&& ((String) col.getItem()).toUpperCase().matches(
						groupingRegex.toString().toUpperCase());
		return isStringItemAndAggregated;
	}

    /**
     * This is a helper method for {@link #generateQuery()} to properly return a
     * column name. For items that represent {@link SQLColumn}s the name of the
     * column needs to be quoted and will use the given quote string.
     * Additionally, some columns that represent constants need to be converted
     * to a different constant based on the database being queried. (For example
     * current_time changes to current_timestamp in SQL Server.)
     * 
     * @param item
     *            The item that represents a column in the query. The name of
     *            the item will be returned and may be possibly modified.
     * @param quote
     *            The quote string to place before and after an item's name if
     *            it needs to be quoted.
     * @param converter
     *            The database specific converter that will change some constant
     *            names to a valid constant in the database.
     */
	private String getColumnName(Item item, String quote, ConstantConverter converter) {
	    if (item instanceof StringItem) return converter.getName(item);
	    if (item instanceof SQLObjectItem) return quote + item.getName() + quote;
	    throw new IllegalArgumentException("Unknown item type " + item.getClass() + " when trying to define a name for the item " + item.getName());
	}

	/* (non-Javadoc)
     * @see ca.sqlpower.query.Query#getSelectedColumns()
     */
	public List<Item> getSelectedColumns() {
		return Collections.unmodifiableList(selectedColumns);
	}

	/* (non-Javadoc)
     * @see ca.sqlpower.query.Query#getOrderByList()
     */
	public List<Item> getOrderByList() {
		return Collections.unmodifiableList(orderByList);
	}
	
	/* (non-Javadoc)
     * @see ca.sqlpower.query.Query#moveSortedItemToEnd(ca.sqlpower.query.Item)
     */
	public void moveSortedItemToEnd(Item item) {
	    orderByList.remove(item);
	    orderByList.add(item);
	}
	
	/* (non-Javadoc)
     * @see ca.sqlpower.query.Query#removeTable(ca.sqlpower.query.Container)
     */
	public void removeTable(Container table) {
	    try {
	        startCompoundEdit("Removing table " + table.getName() + ", its columns and its joins.");
	        boolean removed = fromTableList.remove(table);
	        if (!removed) {
	            return;
	        }
	        table.removeChildListener(getTableChildListener());
	        for (Item col : table.getItems()) {
	            removeItem(col);
	        }
	        for (List<SQLJoin> joins : joinMapping.values()) {
	            for (int i = joins.size() - 1; i >= 0; i--) {
	                SQLJoin join = joins.get(i);
	                if (join.getLeftColumn().getParent().equals(table) ||
	                        join.getRightColumn().getParent().equals(table)) {
	                    removeJoin(join);
	                }
	            }
	        }
	        fireContainerRemoved(table);
	    } finally {
	        endCompoundEdit();
	    }
	}

	/* (non-Javadoc)
     * @see ca.sqlpower.query.Query#addTable(ca.sqlpower.query.Container)
     */
	public void addTable(Container container) {
		fromTableList.add(container);
		container.addChildListener(getTableChildListener());
		for (Item col : container.getItems()) {
			addItem(col);
		}
		fireContainerAdded(container);
	}
	
	/* (non-Javadoc)
     * @see ca.sqlpower.query.Query#setGlobalWhereClause(java.lang.String)
     */
	public void setGlobalWhereClause(String whereClause) {
		String oldWhere = globalWhereClause;
		globalWhereClause = whereClause;
		firePropertyChangeEvent(new PropertyChangeEvent(this, GLOBAL_WHERE_CLAUSE, oldWhere, whereClause));
	}
	
	/* (non-Javadoc)
     * @see ca.sqlpower.query.Query#removeJoin(ca.sqlpower.query.SQLJoin)
     */
	public void removeJoin(SQLJoin joinLine) {
		joinLine.removeJoinChangeListener(joinChangeListener);
		Item leftColumn = joinLine.getLeftColumn();
		Item rightColumn = joinLine.getRightColumn();

		List<SQLJoin> leftJoinList = joinMapping.get(leftColumn.getContainer());
		for (SQLJoin join : leftJoinList) {
			if (leftColumn == join.getLeftColumn() && rightColumn == join.getRightColumn()) {
				leftJoinList.remove(join);
				break;
			}
		}

		List<SQLJoin> rightJoinList = joinMapping.get(rightColumn.getContainer());
		for (SQLJoin join : rightJoinList) {
			if (leftColumn == join.getLeftColumn() && rightColumn == join.getRightColumn()) {
				rightJoinList.remove(join);
				break;
			}
		}
		fireJoinRemoved(joinLine);
	}

	/* (non-Javadoc)
     * @see ca.sqlpower.query.Query#addJoin(ca.sqlpower.query.SQLJoin)
     */
	public void addJoin(SQLJoin join) {
		join.addJoinChangeListener(joinChangeListener);
		Item leftColumn = join.getLeftColumn();
		Item rightColumn = join.getRightColumn();
		Container leftContainer = leftColumn.getContainer();
		Container rightContainer = rightColumn.getContainer();
		if (joinMapping.get(leftContainer) == null) {
			List<SQLJoin> joinList = new ArrayList<SQLJoin>();
			joinList.add(join);
			joinMapping.put(leftContainer, joinList);
		} else {
			if (joinMapping.get(leftContainer).size() > 0) {
				SQLJoin prevJoin = joinMapping.get(leftContainer).get(0);
				if (prevJoin.getLeftColumn().getContainer() == leftContainer) {
					join.setLeftColumnOuterJoin(prevJoin.isLeftColumnOuterJoin());
				} else if (prevJoin.getRightColumn().getContainer() == leftContainer) {
					join.setLeftColumnOuterJoin(prevJoin.isRightColumnOuterJoin());
				}
			}
				
			joinMapping.get(leftContainer).add(join);
		}

		if (joinMapping.get(rightContainer) == null) {
			List<SQLJoin> joinList = new ArrayList<SQLJoin>();
			joinList.add(join);
			joinMapping.put(rightContainer, joinList);
		} else {
			if (joinMapping.get(rightContainer).size() > 0) {
				SQLJoin prevJoin = joinMapping.get(rightContainer).get(0);
				if (prevJoin.getLeftColumn().getContainer() == rightContainer) {
					join.setRightColumnOuterJoin(prevJoin.isLeftColumnOuterJoin());
				} else if (prevJoin.getRightColumn().getContainer() == rightContainer) {
					join.setRightColumnOuterJoin(prevJoin.isRightColumnOuterJoin());
				} else {
					throw new IllegalStateException("A table contains a join that is not connected to any of its columns in the table.");
				}
			}
			joinMapping.get(rightContainer).add(join);
		}
		fireJoinAdded(join);
	}
	
	/* (non-Javadoc)
     * @see ca.sqlpower.query.Query#removeItem(ca.sqlpower.query.Item)
     */
	public void removeItem(Item col) {
		logger.debug("Item name is " + col.getName());
		col.removePropertyChangeListener(itemListener);
		removeColumnSelection(col);
		fireItemRemoved(col);
	}
	
	/* (non-Javadoc)
     * @see ca.sqlpower.query.Query#addItem(ca.sqlpower.query.Item)
     */
	public void addItem(Item col) {
		col.addPropertyChangeListener(itemListener);
		fireItemAdded(col);
		if (col.isSelected()) {
			selectedColumns.add(col);
		}
		if (col.isSelected() && !col.getOrderBy().equals(OrderByArgument.NONE)) {
			orderByList.add(col);
		}
	}
	
	/* (non-Javadoc)
     * @see ca.sqlpower.query.Query#moveItem(ca.sqlpower.query.Item, int)
     */
	public void moveItem(Item movedColumn, int toIndex) {
		selectedColumns.remove(movedColumn);
		selectedColumns.add(toIndex, movedColumn);
		fireItemOrderChanged(movedColumn);
	}

    /* (non-Javadoc)
     * @see ca.sqlpower.query.Query#startCompoundEdit(java.lang.String)
     */
	public void startCompoundEdit(String message) {
	    int currentEditLevel = compoundEditLevel;
	    compoundEditLevel++;
	    if (currentEditLevel == 0) {
	        fireCompoundEditStarted(message);
	    }
	}
	
	/* (non-Javadoc)
     * @see ca.sqlpower.query.Query#endCompoundEdit()
     */
	public void endCompoundEdit() {
	    compoundEditLevel--;
	    if (compoundEditLevel == 0) {
	        fireCompoundEditEnded();
	    }
	}

	/* (non-Javadoc)
     * @see ca.sqlpower.query.Query#isGroupingEnabled()
     */
	public boolean isGroupingEnabled() {
		return groupingEnabled;
	}

	/* (non-Javadoc)
     * @see ca.sqlpower.query.Query#getFromTableList()
     */
	public List<Container> getFromTableList() {
		return Collections.unmodifiableList(fromTableList);
	}

	protected Map<Container, List<SQLJoin>> getJoinMapping() {
		return Collections.unmodifiableMap(joinMapping);
	}
	
	/* (non-Javadoc)
     * @see ca.sqlpower.query.Query#getJoins()
     */
	public Collection<SQLJoin> getJoins() {
		Set<SQLJoin> joinSet = new HashSet<SQLJoin>();
		for (List<SQLJoin> joins : joinMapping.values()) {
			for (SQLJoin join : joins) {
				joinSet.add(join);
			}
		}
		return joinSet;
	}

	/* (non-Javadoc)
     * @see ca.sqlpower.query.Query#getGlobalWhereClause()
     */
	public String getGlobalWhereClause() {
		return globalWhereClause;
	}

	/* (non-Javadoc)
     * @see ca.sqlpower.query.Query#getConstantsContainer()
     */
	public Container getConstantsContainer() {
		return constantsContainer;
	}
	
	/* (non-Javadoc)
     * @see ca.sqlpower.query.Query#setDataSource(ca.sqlpower.sql.JDBCDataSource)
     */
	public void setDataSource(JDBCDataSource dataSource) {
	    final SQLDatabase newDatabase = dbMapping.getDatabase(dataSource);
	    if (database != null && database == newDatabase) return;
	    this.database = newDatabase;
	    reset();
	    if (dataSource != null) {
            setStreaming(dataSource.getParentType().getSupportsStreamQueries());
        }
	}
	
	/* (non-Javadoc)
     * @see ca.sqlpower.query.Query#defineUserModifiedQuery(java.lang.String)
     */
	public void defineUserModifiedQuery(String query) {
		String generatedQuery = generateQuery();
		logger.debug("Generated query is " + generatedQuery + " and given query is " + query);
		if (generatedQuery.equals(query)) {
			return;
		}
		firePropertyChangeEvent(new PropertyChangeEvent(this, USER_MODIFIED_QUERY, userModifiedQuery, query));
		userModifiedQuery = query;
	}
	
	/* (non-Javadoc)
     * @see ca.sqlpower.query.Query#isScriptModified()
     */
	public boolean isScriptModified() {
		return userModifiedQuery != null;
	}
	
	/* (non-Javadoc)
     * @see ca.sqlpower.query.Query#removeUserModifications()
     */
	public void removeUserModifications() {
		logger.debug("Removing user modified query.");
		userModifiedQuery = null;
	}

	/* (non-Javadoc)
     * @see ca.sqlpower.query.Query#newConstantsContainer(java.lang.String)
     */
	public Container newConstantsContainer(String uuid) {
		constantsContainer = new ItemContainer("Constants", uuid);
		return constantsContainer;
	}

	/* (non-Javadoc)
     * @see ca.sqlpower.query.Query#setZoomLevel(int)
     */
	public void setZoomLevel(int zoomLevel) {
		this.zoomLevel = zoomLevel;
	}

	/* (non-Javadoc)
     * @see ca.sqlpower.query.Query#getZoomLevel()
     */
	public int getZoomLevel() {
		return zoomLevel;
	}
	
	/**
	 * Used for constructing copies of the query cache.
	 */
	protected String getUserModifiedQuery() {
		return userModifiedQuery;
	}
	
	/* (non-Javadoc)
     * @see ca.sqlpower.query.Query#toString()
     */
	@Override
	public String toString() {
		return getName();
	}

    /* (non-Javadoc)
     * @see ca.sqlpower.query.Query#getUUID()
     */
    public String getUUID() {
        return uuid;
    }
    
    /* (non-Javadoc)
     * @see ca.sqlpower.query.Query#getName()
     */
    public String getName() {
        return name;
    }
    
    /* (non-Javadoc)
     * @see ca.sqlpower.query.Query#setName(java.lang.String)
     */
    public void setName(String name) {
        this.name = name;
    }
    
    /* (non-Javadoc)
     * @see ca.sqlpower.query.Query#setRowLimit(int)
     */
    public void setRowLimit(int rowLimit) {
        int oldLimit = this.rowLimit;
        this.rowLimit = rowLimit;
        firePropertyChangeEvent(new PropertyChangeEvent(this, ROW_LIMIT, oldLimit, rowLimit));
    }

    /* (non-Javadoc)
     * @see ca.sqlpower.query.Query#getRowLimit()
     */
    public int getRowLimit() {
        return rowLimit;
    }
    
    /* (non-Javadoc)
     * @see ca.sqlpower.query.Query#setStreamingRowLimit(int)
     */
    public void setStreamingRowLimit(int streamingRowLimit) {
        this.streamingRowLimit = streamingRowLimit;
    }

    /* (non-Javadoc)
     * @see ca.sqlpower.query.Query#getStreamingRowLimit()
     */
    public int getStreamingRowLimit() {
        return streamingRowLimit;
    }

    /* (non-Javadoc)
     * @see ca.sqlpower.query.Query#setStreaming(boolean)
     */
    public void setStreaming(boolean streaming) {
        this.streaming = streaming;
    }

    /* (non-Javadoc)
     * @see ca.sqlpower.query.Query#isStreaming()
     */
    public boolean isStreaming() {
        return streaming;
    }

    /* (non-Javadoc)
     * @see ca.sqlpower.query.Query#addQueryChangeListener(ca.sqlpower.query.QueryChangeListener)
     */
    public void addQueryChangeListener(QueryChangeListener l) {
        synchronized(changeListeners) {
            changeListeners.add(l);
        }
    }
    
    /* (non-Javadoc)
     * @see ca.sqlpower.query.Query#removeQueryChangeListener(ca.sqlpower.query.QueryChangeListener)
     */
    public void removeQueryChangeListener(QueryChangeListener l) {
        synchronized(changeListeners) {
            changeListeners.remove(l);
        }
    }

    /**
     * This is package private as it is only used in testing and shouldn't be
     * used anywhere else.
     */
    ContainerChildListener getTableChildListener() {
        return tableChildListener;
    }
    
    /**
     * This is package private as it is only used in testing and shouldn't be
     * used anywhere else.
     */
    PropertyChangeListener getItemListener() {
        return itemListener;
    }
    
    /**
     * This is package private as it is only used in testing and shouldn't be
     * used anywhere else.
     */
    PropertyChangeListener getJoinChangeListener() {
        return joinChangeListener;
    }
    
    /* (non-Javadoc)
     * @see ca.sqlpower.query.Query#reset()
     */
    public void reset() {
        try {
            startCompoundEdit("Resetting query");
            for (int i = getFromTableList().size() - 1; i >= 0; i--) {
                removeTable(getFromTableList().get(i));
            }
            resetConstantsContainer();
            setGlobalWhereClause(null);
            setGroupingEnabled(false);
            defineUserModifiedQuery("");
            setZoomLevel(0);
        } finally {
            endCompoundEdit();
        }
            
    }
    
//---------------------------- Protected methods to fire events -------------------
    
    protected void fireJoinAdded(SQLJoin joinAdded) {
        synchronized(changeListeners) {
            for (int i = changeListeners.size() - 1; i >= 0; i--) {
                changeListeners.get(i).joinAdded(new QueryChangeEvent(this, joinAdded));
            }
        }
    }
    
    protected void fireJoinRemoved(SQLJoin joinRemoved) {
        synchronized(changeListeners) {
            for (int i = changeListeners.size() - 1; i >= 0; i--) {
                changeListeners.get(i).joinRemoved(new QueryChangeEvent(this, joinRemoved));
            }
        }
    }
    
    protected void fireJoinPropertyChangeEvent(PropertyChangeEvent e) {
        synchronized(changeListeners) {
            for (int i = changeListeners.size() - 1; i >= 0; i--) {
                changeListeners.get(i).joinPropertyChangeEvent(e);
            }
        }
    }
    
    protected void fireItemPropertyChangeEvent(PropertyChangeEvent e) {
        synchronized(changeListeners) {
            for (int i = changeListeners.size() - 1; i >= 0; i--) {
                changeListeners.get(i).itemPropertyChangeEvent(e);
            }
        }
    }
    
    protected void fireItemAdded(Item itemAdded) {
        synchronized(changeListeners) {
            for (int i = changeListeners.size() - 1; i >= 0; i--) {
                changeListeners.get(i).itemAdded(new QueryChangeEvent(this, itemAdded));
            }
        }
    }
    
    protected void fireItemRemoved(Item itemRemoved) {
        synchronized(changeListeners) {
            for (int i = changeListeners.size() - 1; i >= 0; i--) {
                changeListeners.get(i).itemRemoved(new QueryChangeEvent(this, itemRemoved));
            }
        }
    }
    
    protected void fireItemOrderChanged(Item movedColumn) {
        synchronized(changeListeners) {
            for (int i = changeListeners.size() - 1; i >= 0; i--) {
                changeListeners.get(i).itemOrderChanged(new QueryChangeEvent(this, movedColumn));
            }
        }
    }
    
    protected void fireContainerAdded(Container containerAdded) {
        synchronized(changeListeners) {
            for (int i = changeListeners.size() - 1; i >= 0; i--) {
                changeListeners.get(i).containerAdded(new QueryChangeEvent(this, containerAdded));
            }
        }
    }
    
    protected void fireContainerRemoved(Container containerRemoved) {
        synchronized(changeListeners) {
            for (int i = changeListeners.size() - 1; i >= 0; i--) {
                changeListeners.get(i).containerRemoved(new QueryChangeEvent(this, containerRemoved));
            }
        }
    }
    
    protected void firePropertyChangeEvent(PropertyChangeEvent e) {
        synchronized(changeListeners) {
            for (int i = changeListeners.size() - 1; i >= 0; i--) {
                changeListeners.get(i).propertyChangeEvent(e);
            }
        }
    }
    
    protected void fireCompoundEditStarted(String message) {
        synchronized(changeListeners) {
            for (int i = changeListeners.size() - 1; i >= 0; i--) {
                changeListeners.get(i).compoundEditStarted(
                        QueryCompoundEditEvent.createStartCompoundEditEvent(this, message));
            }
        }
    }
    
    protected void fireCompoundEditEnded() {
        synchronized(changeListeners) {
            for (int i = changeListeners.size() - 1; i >= 0; i--) {
                changeListeners.get(i).compoundEditEnded(
                        QueryCompoundEditEvent.createEndCompoundEditEvent(this));
            }
        }
    }
    
//---------------------------- End of protected methods to fire events -------------------
}
