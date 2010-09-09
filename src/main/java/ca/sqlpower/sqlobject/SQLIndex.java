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

import java.beans.PropertyChangeEvent;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import ca.sqlpower.object.AbstractSPListener;
import ca.sqlpower.object.ObjectDependentException;
import ca.sqlpower.object.SPChildEvent;
import ca.sqlpower.object.SPListener;
import ca.sqlpower.object.SPObject;
import ca.sqlpower.object.annotation.Accessor;
import ca.sqlpower.object.annotation.Constructor;
import ca.sqlpower.object.annotation.ConstructorParameter;
import ca.sqlpower.object.annotation.Mutator;
import ca.sqlpower.object.annotation.NonProperty;
import ca.sqlpower.object.annotation.Transient;
import ca.sqlpower.sql.SQL;
import ca.sqlpower.util.SessionNotFoundException;
import ca.sqlpower.util.TransactionEvent;

/**
 * The SQLIndex class represents an index on a table in a relational database.
 *
 * @author fuerth
 */
public class SQLIndex extends SQLObject {

    private static final Logger logger = Logger.getLogger(SQLIndex.class);
    
    /**
	 * Defines an absolute ordering of the child types of this class.
	 */
	public static final List<Class<? extends SPObject>> allowedChildTypes = 
		Collections.<Class<? extends SPObject>>singletonList(Column.class);

    /**
     * An enumeration to define if a column in an index should be ordered in ascending
     * order, descending order, or it should be left undefined.
     */
    public static enum AscendDescend {
        ASCENDING, DESCENDING, UNSPECIFIED;
    }

    /**
     * This is the property name in the PL.ini file that will indicate what Index types
     * are supported for any specific database.
     */
    public static final String INDEX_TYPE_DESCRIPTOR = SQLIndex.class.getName() + ".IndexType";

    /**
     * This is the index type. If this is null the default type in the database should be used.
     */
    private String type;

    /**
     * This is the name of the column that will be augmented by the custom 
     * JDBC wrappers to represent index type;
     */
    public static final String RS_INDEX_TYPE_COL = "SPG_INDEX_TYPE";

    /**
     * A simple placeholder for a column.  We're not using real SQLColumn instances here so that the
     * tree of SQLObjects can remain tree-like.  If we put the real SQLColumns in here, the columns
     * would appear in two places in the tree (here and under the table's columns folder)!
     */
    public static class Column extends SQLObject {
    	
    	/**
    	 * Defines an absolute ordering of the child types of this class.
    	 */
    	public static final List<Class<? extends SPObject>> allowedChildTypes = Collections.emptyList();

        /**
         * Small class for reacting to changes in this index columns's
         * target SQLColumn (if it has one at all).
         */
        private class TargetColumnListener implements SPListener {

            /**
             * Updates the index column name to match the new value in this
             * event, if the event is a name change from the target SQLColumn.
             * The process of doing the update will cause the SQLIndex.Column
             * object to fire an event of its own.
             */
            public void propertyChanged(PropertyChangeEvent e) {
                if ("name".equals(e.getPropertyName())) {
                    setName((String) e.getNewValue());
                } else if ("physicalName".equals(e.getPropertyName())) {
                    setPhysicalName((String) e.getNewValue());
                }
            }
            
            public void childAdded(SPChildEvent e) {
            	// no-op
            }
            public void childRemoved(SPChildEvent e) {
            	// no-op
            }
            public void transactionStarted(TransactionEvent e) {
            	// no-op
            }
            public void transactionEnded(TransactionEvent e) {
            	// no-op
            }
            public void transactionRollback(TransactionEvent e) {
            	// no-op
            }

            @Override
            public String toString() {
                StringBuffer buf = new StringBuffer();
                buf.append(getParent().getName());
                buf.append(".");
                buf.append(Column.this.getName());
                buf.append(".");
                buf.append("TargetColumnListener");
                buf.append(" isPrimarykey?");
                buf.append(getParent().isPrimaryKeyIndex());
                return buf.toString();
            }
        }

        /**
         * The column in the table that this index column represents. Might be
         * null if this index column represents an expression rather than a
         * single column value.
         */
        private SQLColumn column;

        /**
         * Specifies if the column is ascending, descending, or undefined.
         */
        private AscendDescend ascendingOrDescending;

        /**
         * A proxy that refires certain events on the target column.
         *
         * <p>It is the job of {@link #setColumn(SQLColumn)} to keep this
         * listener hooked up to the correct SQLColumn object (or completely
         * disconnected in the case that there is no target SQLColumn).
         */
        private final TargetColumnListener targetColumnListener = new TargetColumnListener();

        /**
         * Creates a Column object that corresponds to a particular SQLColumn.
         */
        public Column(SQLColumn col, AscendDescend ad) {
            this(col.getName(), ad);
            setColumn(col);
        }

        /**
         * Creates a Column object that does not correspond to a particular column
         * (such as an expression index).
         */
        public Column(
        		String name, 
        		AscendDescend ad) {
            setName(name);

            setPopulated(true);
            ascendingOrDescending = ad;
        }

		/**
		 * Creates a Column object that may or may not correspond to a column.
		 * 
		 * @param name
		 *            The name of the column being wrapped.
		 * @param col
		 *            If this column is representing a {@link SQLColumn} the
		 *            column must be passed in. If the column is representing an
		 *            expression or a column other than a SQLColumn null must be
		 *            passed in.
		 * @param ad
		 *            Decides if the column should be indexed in ascending or
		 *            descending order.
		 */
        @Constructor
        public Column(@ConstructorParameter(propertyName="name") String name, 
        		@ConstructorParameter(propertyName="column") SQLColumn col, 
        		@ConstructorParameter(propertyName="ascendingOrDescending") AscendDescend ad) {
        	this(name, ad);
        	setColumn(col);
        }

        public Column() {
            this((String) null, AscendDescend.UNSPECIFIED);
        }

        @Override
        @Accessor
        public SQLIndex getParent() {
            return (SQLIndex) super.getParent();
        }
        
    	/**
    	 * Because we constrained the return type on getParent there needs to be a
    	 * setter that has the same constraint otherwise the reflection in the undo
    	 * events will not find a setter to match the getter and won't be able to
    	 * undo parent property changes.
    	 */
        @Mutator
    	public void setParent(SQLIndex parent) {
    		super.setParent(parent);
    	}

        @Override
        @Transient @Accessor
        public String getShortDisplayName() {
            return getName();
        }

        @Override
        protected void populateImpl() throws SQLObjectException {
            // nothing to do
        }

        @Override
        @Transient @Accessor
        public boolean isPopulated() {
            return true;
        }

        /**
         * NOTE: This column can be null if the column it represents is an expression
         * and not a basic column.
         */
        @Accessor
        public SQLColumn getColumn() {
            return column;
        }

        @Mutator
        public void setColumn(SQLColumn column) {
            try {
                begin("Setting SQLIndex column");
                if (this.column != null) {
                    this.column.removeSPListener(targetColumnListener);
                }
                SQLColumn oldValue = this.column;
                this.column = column;
                if (this.column != null) {
                    this.column.addSPListener(targetColumnListener);
                }
                firePropertyChange("column", oldValue, column);
                if (this.column != null) {
                    setPhysicalName(column.getPhysicalName());
                }
                commit();
            } catch (RuntimeException e) {
                rollback(e.getMessage());
                throw e;
            }
        }

        @Accessor
        public AscendDescend getAscendingOrDescending() {
            return ascendingOrDescending;
        }
        
        /**
         * This setter should be passed an enumerated item of type
         * AscendDescend.
         */
        @Mutator
        public void setAscendingOrDescending(AscendDescend ad) {
        	AscendDescend oldValue = ascendingOrDescending;
        	ascendingOrDescending = (AscendDescend) ad;
        	firePropertyChange("ascendingOrDescending", oldValue, ascendingOrDescending);
        }

        @Mutator
        public void setAscending(boolean ascending) {
            AscendDescend oldValue = this.ascendingOrDescending;
            if (ascending) {
                this.ascendingOrDescending = AscendDescend.ASCENDING;
            }
            firePropertyChange("ascending", oldValue, ascendingOrDescending);
        }

        @Mutator
        public void setDescending(boolean descending) {
            AscendDescend oldValue = this.ascendingOrDescending;
            if (descending) {
                this.ascendingOrDescending = AscendDescend.DESCENDING;
            }
            firePropertyChange("descending", oldValue, descending);
        }

        @Override
        public String toString() {
            return getName();
        }

        @Override
        public int hashCode() {
            final int PRIME = 31;
            int result = 1;
            result = PRIME * result + (ascendingOrDescending == AscendDescend.ASCENDING ? 1231 : 1237);
            result = PRIME * result + ((column == null) ? 0 : column.hashCode());
            result = PRIME * result + (ascendingOrDescending == AscendDescend.DESCENDING ? 1231 : 1237);
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            final Column other = (Column) obj;
            if (ascendingOrDescending != other.ascendingOrDescending)
                return false;
            if (column == null) {
                if (other.column != null)
                    return false;
            } else if (!column.equals(other.column))
                return false;
            return true;
        }

		@Override
		public List<? extends SQLObject> getChildrenWithoutPopulating() {
			return Collections.emptyList();
		}

		@Override
		protected boolean removeChildImpl(SPObject child) {
			return false;
		}

		public List<? extends SPObject> getDependencies() {
			return Collections.singletonList(column);
		}

		public void removeDependency(SPObject dependency) {
			if (dependency == column) {
				getParent().removeColumn(this);
			}
		}

		public List<Class<? extends SPObject>> getAllowedChildTypes() {
			return allowedChildTypes;
		}
		
		@Override
		public void updateToMatch(SQLObject source) throws SQLObjectException {
			Column sourceCol = (Column) source;
			setAscendingOrDescending(sourceCol.getAscendingOrDescending());
		}
    }

    /**
     * Flags whether or not this index enforces uniqueness.
     */
    private boolean unique;

    /**
     * The qualifier that must be used for referring to this index in the database.  This is
     * usually the name of the table the index belongs to (in the case of SQL Server), or null
     * (in the case of Oracle).
     */
    private String qualifier;

    /**
     * The filter condition on this index, if any.  According to the ODBC programmer's reference,
     * this is probably a property of the index as a whole (as opposed to the individual index columns),
     * but it doesn't say that explicitly.  According to the JDBC spec, this could be anything at all.
     */
    private String filterCondition;

    /**
     * This indicates if an index is clustered or not.
     */
    private boolean clustered;

    /**
     * This is a listener that will listen for SQLColumns in the SQLTable's column folder
     * and make sure that the SQLIndex will also remove its Column object associated
     * with the SQLColumn removed.
     */
    private SPListener removeColumnListener;
    
    private List<Column> columns = new ArrayList<Column>();

    @Constructor
    public SQLIndex(
    		@ConstructorParameter(propertyName = "name") String name, 
    		@ConstructorParameter(propertyName = "unique") boolean unique, 
    		@ConstructorParameter(propertyName = "qualifier") String qualifier, 
    		@ConstructorParameter(propertyName = "type") String type, 
    		@ConstructorParameter(propertyName = "filterCondition") String filter) {
        this();
        setName(name);
        this.unique = unique;
        this.qualifier = qualifier;
        this.type = type;
        this.filterCondition = filter;
    }

    public SQLIndex() {
        super();
        setPopulated(true);
        removeColumnListener = new AbstractSPListener() {

            public void childRemoved(SPChildEvent e) {
                if (e.getChildType() == SQLColumn.class) {
                    removeColumnFromIndices(e);
                }
            }
        };
    }

    /**
     * Copy constructor for a sql index
     * @param oldIndex
     * @throws SQLObjectException
     */
    public SQLIndex(SQLIndex oldIndex) throws SQLObjectException {
        this();
        setParent(oldIndex.getParent());
        updateToMatch(oldIndex);
    }

    /**
     * Updates all properties and child objects of this index to match the given
     * index, except the parent pointer.
     * 
     * @param source
     *            The index to copy properties and columns from. If it has columns,
     *            they must already belong to the same table as this index does.
     */
    @Override
    public final void updateToMatch(SQLObject source) throws SQLObjectException {
    	updateToMatch(source, true);
    }

	/**
	 * Updates all properties and child objects of this index to match the given
	 * index, except the parent pointer.
	 * 
	 * @param source
	 *            The index to copy properties and columns from. If it has
	 *            columns, they must already belong to the same table as this
	 *            index does.
	 * @param updateChildren
	 *            If true the children of the index will be updated as well. If
	 *            false only this index's parameters will be updated.
	 */
    public final void updateToMatch(SQLObject source, boolean updateChildren) throws SQLObjectException {
        SQLIndex sourceIdx = (SQLIndex) source;
        
        try {
        	begin("Updating SQLIndex to match source object.");

        	setName(sourceIdx.getName());
        	setUnique(sourceIdx.unique);
        	populated = sourceIdx.populated;
        	setType(sourceIdx.type);
        	setFilterCondition(sourceIdx.filterCondition);
        	setQualifier(sourceIdx.qualifier);
        	setClustered(sourceIdx.clustered);
        	setPhysicalName(sourceIdx.getPhysicalName());

        	if (updateChildren) {
        		makeColumnsLike(sourceIdx);
        	}
        	
        	commit();
        } catch (IllegalArgumentException e) {
        	rollback("Could not remove columns from SQLIndex: " + e.getMessage());
        	throw new RuntimeException(e);
        } catch (ObjectDependentException e) {
        	rollback("Could not remove columns from SQLIndex: " + e.getMessage());
        	throw new RuntimeException(e);        	
        }
    }

    /**
     * Indices are associated with one or more table columns.  The children of this index represent those columns,
     * and the order in which the index applies to them.
     */
    @Override
    public boolean allowsChildren() {
        return true;
    }

    /**
     * Overriden to narrow return type.
     */
    @Override
    @NonProperty
    public Column getChild(int index) throws SQLObjectException {
        return (Column) super.getChild(index);
    }

    /**
     * Overriden to narrow return type.
     */
    @Override
    @NonProperty
    public List<Column> getChildrenWithoutPopulating() {
    	return Collections.unmodifiableList(new ArrayList<Column>(columns));
    }
    
    /**
     * Returns the table folder that owns this index.
     */
    @Override
    public SQLTable getParent() {
        return (SQLTable) super.getParent();
    }
    
	/**
	 * Because we constrained the return type on getParent there needs to be a
	 * setter that has the same constraint otherwise the reflection in the undo
	 * events will not find a setter to match the getter and won't be able to
	 * undo parent property changes.
	 */
    @Mutator
	public void setParent(SQLTable parent) {
		setParentHelper(parent);
	}

    @Override
    @Transient @Accessor
    public String getShortDisplayName() {
        return getName();
    }

    /**
     * Indices are populated when first created, so populate is a no-op.
     */
    @Override
    protected void populateImpl() throws SQLObjectException {
        // nothing to do
    }

    @Override
    @Transient @Accessor
    public boolean isPopulated() {
        return true;
    }

    /**
     * Updates this index's parent reference, and attaches a listener to the
     * columns folder of the new parent's parent table.
     * 
     * @param parent
     *            The new parent. Must be null or a SQLTable.Folder instance. If
     *            it's a folder, it must already have a parent table.
     * @throws IllegalStateException
     *             if the given parent is non-null and does not itself have a
     *             parent table.
     */
    @Override
    @Mutator
    public void setParent(SPObject parent) {
    	setParentHelper(parent);
    }
    
    /**
     * See the documentation on {@link #setParent(SQLTable)} for why
     * setting the parent seems kind of goofy.
     */
    private void setParentHelper(SPObject parent) {
    	if (getParent() != null) {
            getParent().removeSPListener(removeColumnListener);
        }
        
        super.setParent(parent);
        
        if (getParent() != null) {
            getParent().addSPListener(removeColumnListener);
        }
    }

    /**
     * This is used by the removeColumn method to make sure that once a column
     * is removed from a table, it is also removed from all the indices of that table.
     */
    private void removeColumnFromIndices(SPChildEvent e) {
        if (getParent() != null && getParent().isMagicEnabled()) {
            try {
            	//begin and commit on the parent table in case the index is removed from the system
            	//as the commit would then not be fired to the undo listeners.
            	SQLTable parentTable = getParent();
            	parentTable.begin("Removing column from indices");
            	
                    for (int j = this.getChildCount() - 1; j >= 0; j--) {
                    	Column col = getChild(j);
                        if (col.getColumn() != null && col.getColumn().equals(e.getChild())) {
                            removeChild(col);
                        }
                    }
                cleanUpIfChildless();
                
                parentTable.commit();
            } catch (SQLObjectException e1) {
            	rollback("Could not remove child: " + e1.getMessage());
                throw new SQLObjectRuntimeException(e1);
            } catch (IllegalArgumentException e1) {
            	rollback("Could not remove child: " + e1.getMessage());
                throw new RuntimeException(e1);            	
			} catch (ObjectDependentException e1) {
            	rollback("Could not remove child: " + e1.getMessage());
                throw new RuntimeException(e1);
			}
        }
    }

    /**
     * This method is used to clean up the index when it no longer has any children.
     */
    public void cleanUpIfChildless() {
    	//This is a final field on the table
    	if (isPrimaryKeyIndex()) return;
    	
        try {
            if (getChildCount() == 0 && getParent() != null) {
                logger.debug("Removing " + getName() + " index from table " + getParent().getName());
                getParent().removeSPListener(removeColumnListener);
                getParent().removeChild(this);
            }
        } catch (SQLObjectException e) {
            throw new SQLObjectRuntimeException(e);
        } catch (IllegalArgumentException e) {
        	throw new RuntimeException(e);
		} catch (ObjectDependentException e) {
			throw new RuntimeException(e);
		}
    }
    
    @Override
    protected void addChildImpl(SPObject child, int index) {
    	if (child instanceof SQLIndex.Column) {
    		Column c = (Column) child;
    		if (isPrimaryKeyIndex() && c.getColumn() == null) {
    			throw new IllegalArgumentException("The primary key index must consist of real columns, not expressions");
    		}
    		addIndexColumn(c, index);
    		if (c.getColumn() != null) {
    			// this will be redundant in some cases, but the addSQLObjectListener method
    			// checks for adding duplicate listeners and does nothing in that case
    			c.getColumn().addSPListener(c.targetColumnListener);
    		}
    	} else {
			throw new IllegalArgumentException("The child " + child.getName() + 
					" of type " + child.getClass() + " is not a valid child type of " + 
					getClass() + ".");
    	}
    }

    @Accessor(isInteresting=true)
    public String getFilterCondition() {
        return filterCondition;
    }

    @Mutator
    public void setFilterCondition(String filterCondition) {
        String oldValue = this.filterCondition;
        this.filterCondition = filterCondition;
        firePropertyChange("filterCondition", oldValue, filterCondition);
    }

    @Accessor(isInteresting=true)
    public String getQualifier() {
        return qualifier;
    }

    @Mutator
    public void setQualifier(String qualifier) {
        String oldValue = this.qualifier;
        this.qualifier = qualifier;
        firePropertyChange("qualifier", oldValue, qualifier);
    }

    @Accessor(isInteresting=true)
    public String getType() {
        return type;
    }

    @Mutator
    public void setType(String type) {
        String oldValue = this.type;
        this.type = type;
        firePropertyChange("type", oldValue, type);
    }

    @Accessor(isInteresting=true)
    public boolean isUnique() {
        return unique;
    }

    @Accessor(isInteresting=true)
    public boolean isClustered() {
        return clustered;
    }

    @Mutator
    public void setUnique(boolean unique) {
        boolean oldValue = this.unique;
        this.unique = unique;
        firePropertyChange("unique", oldValue, unique);
    }

    @Mutator
    public void setClustered(boolean value) {
        boolean oldValue = this.clustered;
        this.clustered = value;
        firePropertyChange("clustered", oldValue, clustered);
    }

	/**
	 * Creates a list of new SQLIndex objects based on the indexes that exist on
	 * the given table in the given database metadata. The index child objects
	 * will have direct references to the columns of the given SQLTable, but
	 * nothing in the table will be modified. You can add the indexes to the
	 * table yourself when the list is returned.
	 * <p>
	 * As a side effect of calling this method the primary key of the table will
	 * be updated to match the primary key of the table in the database.
	 * <p>
	 * Note: Because the columns are added to the table in the foreground thread
	 * and this method can be called by a background thread you cannot rely on
	 * the columns to already exist while running on the background. If you push
	 * {@link Runnable}s to the foreground that need the children the children
	 * will exist by the time the foreground of the index is reached as the EDT
	 * is a queue.
	 * 
	 * @param dbmd
	 *            The metadata to read the index descriptions from
	 * @param targetTable
	 *            The table the indexes are on.
	 */
    static List<SQLIndex> fetchIndicesForTableAndUpdatePK(DatabaseMetaData dbmd, final SQLTable targetTable) throws SQLException,
            SQLObjectException {
        ResultSet rs = null;

        String catalog = targetTable.getCatalogName();
        String schema = targetTable.getSchemaName();
        String tableName = targetTable.getName();
        
        List<SQLIndex> indexes = new ArrayList<SQLIndex>();
        
        try {
            String pkName = null;
            rs = dbmd.getPrimaryKeys(catalog, schema, tableName);
            final SortedMap<Integer, String> pkColPositionToName = new TreeMap<Integer, String>();
            while (rs.next()) {
            	pkColPositionToName.put(rs.getInt(5) - 1, rs.getString(4));
            	String pkNameCheck = rs.getString(6);
            	if (pkName == null) {
            		pkName = pkNameCheck;
            	} else if (!pkName.equals(pkNameCheck)) {
            		throw new IllegalStateException(
            				"The PK name has changed from " + pkName + " to " +
            				pkNameCheck + " while adding indices to table");                    
            	}
            }
            
            Runnable runner = new Runnable() {
				public void run() {
					for (Map.Entry<Integer, String> namedPositions : pkColPositionToName.entrySet()) {
						if (!targetTable.isColumnsPopulated()) {
							throw new IllegalStateException("Table " + targetTable + " is missing columns, cannot populate primary key.");
						}
						try {
							SQLColumn col = targetTable.getColumnByName(namedPositions.getValue(), false, true);
							if (col != null) {
								targetTable.changeColumnIndex(
										targetTable.getColumnsWithoutPopulating().indexOf(col), 
										namedPositions.getKey(), true);
							} else {
								logger.error("Column " + namedPositions.getValue() + " not found in " + targetTable);
								throw new RuntimeException("Column " + col.getName() + " not found in " + targetTable);
							}
						} catch (SQLObjectException e) {
							throw new SQLObjectRuntimeException(e);
						}
					}
				}
			};
			try {
				targetTable.getRunnableDispatcher().runInForeground(runner);
			} catch (SessionNotFoundException e) {
				runner.run();
			}
            rs.close();
            rs = null;

            logger.debug("SQLIndex.addIndicesToTable: catalog=" + catalog + "; schema=" + schema + "; tableName=" +
                    tableName + "; primary key name=" + pkName);
            SQLIndex idx = null;
            rs = dbmd.getIndexInfo(catalog, schema, tableName, false, true);
            while (rs.next()) {
                /*
                 * DatabaseMetadata result set columns:
                 *
                1  TABLE_CAT String => table catalog (may be null)
                2  TABLE_SCHEM String => table schema (may be null)
                3  TABLE_NAME String => table name
                4  NON_UNIQUE boolean => Can index values be non-unique. false when TYPE is tableIndexStatistic
                5  INDEX_QUALIFIER String => index catalog (may be null); null when TYPE is tableIndexStatistic
                6  INDEX_NAME String => index name; null when TYPE is tableIndexStatistic
                7  TYPE short => index type:
                      tableIndexStatistic - this identifies table statistics that are returned in conjuction with a table's index descriptions
                      tableIndexClustered - this is a clustered index
                      tableIndexHashed - this is a hashed index
                      tableIndexOther - this is some other style of index
                8  ORDINAL_POSITION short => column sequence number within index; zero when TYPE is tableIndexStatistic
                9  COLUMN_NAME String => column name; null when TYPE is tableIndexStatistic
                10 ASC_OR_DESC String => column sort sequence, "A" => ascending, "D" => descending, may be null if sort sequence is not supported; null when TYPE is tableIndexStatistic
                11 CARDINALITY int => When TYPE is tableIndexStatistic, then this is the number of rows in the table; otherwise, it is the number of unique values in the index.
                12 PAGES int => When TYPE is tableIndexStatisic then this is the number of pages used for the table, otherwise it is the number of pages used for the current index.
                13 FILTER_CONDITION String => Filter condition, if any. (may be null)
                 */
                boolean nonUnique = rs.getBoolean(4);
                boolean isClustered = rs.getShort(7) == DatabaseMetaData.tableIndexClustered ? true : false;
                String qualifier = rs.getString(5);
                String name = rs.getString(6);
                String type = null;
                if (SQL.findColumnIndex(rs, RS_INDEX_TYPE_COL) > 0) {
                    type = rs.getString(RS_INDEX_TYPE_COL);
                }
                int pos = rs.getInt(8);
                final String colName = rs.getString(9);
                String ascDesc = rs.getString(10);
                final AscendDescend aOrD;
                if (ascDesc != null && ascDesc.equals("A")) {
                    aOrD = AscendDescend.ASCENDING;
                } else if (ascDesc != null && ascDesc.equals("D")) {
                    aOrD = AscendDescend.DESCENDING;
                } else {
                	aOrD = AscendDescend.UNSPECIFIED;
                }
                String filter = rs.getString(13);

                if (pos == 0) {
                    // this is just the table stats, not an index
                    continue;
                } else if (pos == 1) {
                    logger.debug("Found index " + name);
                    idx = new SQLIndex(name, !nonUnique, qualifier, type, filter);
                    idx.setClustered(isClustered);
                    if (name.equals(pkName)) {
                    	final SQLIndex pkIndex = idx;
                    	Runnable pkIndexUpdate = new Runnable() {
							public void run() {
								try {
									targetTable.getPrimaryKeyIndexWithoutPopulating().updateToMatch(pkIndex, false);
								} catch (SQLObjectException e) {
									throw new SQLObjectRuntimeException(e);
								}
							}
						};
						try {
							targetTable.getRunnableDispatcher().runInForeground(pkIndexUpdate);
						} catch (SessionNotFoundException e) {
							pkIndexUpdate.run();
						}
						
                    	idx = targetTable.getPrimaryKeyIndexWithoutPopulating();
                    } else {
                    	indexes.add(idx);
                    }
                }

                //Child columns of pk fixed before this loop
                if (!idx.isPrimaryKeyIndex()) {
                	logger.debug("Adding column " + colName + " to index " + idx.getName());

                	final SQLIndex nonPKIndex = idx;
                	Runnable indexChildRunner = new Runnable() {
					
						public void run() {
							if (!targetTable.isColumnsPopulated()) {
								throw new IllegalStateException("Table " + targetTable + 
										" is missing columns, cannot populate indices.");
							}
							SQLColumn tableCol;
							try {
								tableCol = targetTable.getColumnByName(colName, false, true);
								Column indexCol;
								if (tableCol != null) {
									indexCol = new Column(tableCol, aOrD);
								} else {
									indexCol = new Column(colName, aOrD); // probably an expression like "col1+col2"
								}

								nonPKIndex.addChild(indexCol);
							} catch (SQLObjectException e) {
								throw new SQLObjectRuntimeException(e);
							}
						}
					};
					try {
						targetTable.getRunnableDispatcher().runInForeground(indexChildRunner);
					} catch (SessionNotFoundException e) {
						indexChildRunner.run();
					}
                }
            }
            rs.close();
            rs = null;

            return indexes;
            
        } finally {
            try {
                if (rs != null)
                    rs.close();
            } catch (SQLException ex) {
                logger.error("Couldn't close result set", ex);
            }
        }
    }

    @Transient @Accessor
    public boolean isPrimaryKeyIndex() {
    	if (getParent() == null) return false;
        return getParent().isPrimaryKey(this);
    }

    @Override
    public String toString() {
        return getName();
    }

	/**
	 * Adds a column to the index. If col1 is null a NPE will be thrown. The
	 * given column must also be a child of the parent table. If this index is
	 * the primary key of the table the column will be added to a position in
	 * the index to match its position in the table. Since the position will be
	 * matched by the index the column must be moved to the correct location in
	 * the index before calling this method if it is being done for the primary
	 * key.
	 * <p>
	 * The column in the index will be defined as unspecified for its ascending
	 * or descending term.
	 */
    public void addIndexColumn(SQLColumn col) {
    	addIndexColumn(col, AscendDescend.UNSPECIFIED);
    }
    
    /**
	 * Adds a column to the index. If col1 is null a NPE will be thrown. The
	 * given column must also be a child of the parent table. If this index is
	 * the primary key of the table the column will be added to a position in
	 * the index to match its position in the table. Since the position will be
	 * matched by the index the column must be moved to the correct location in
	 * the index before calling this method if it is being done for the primary
	 * key. 
	 */
    public void addIndexColumn(SQLColumn col1, AscendDescend aOrD) {
    	if (getParent() != null && !getParent().getColumnsWithoutPopulating().contains(col1)) 
    		throw new IllegalArgumentException("Cannot add " + col1 + " to " + this + 
    				" because the column is not part of the table " + getParent());
    	if (indexOf(col1) != -1) 
    		throw new IllegalArgumentException("Column " + col1 + " already exists in this index.");
    	
        Column col = new Column(col1, aOrD);
        addIndexColumn(col);
    }
    
    public void addIndexColumn(String colName, AscendDescend aOrD) throws SQLObjectException {
        Column col = new Column(colName, aOrD);
        addIndexColumn(col);
    }
    
    public void addIndexColumn(SQLIndex.Column col) {
    	addIndexColumn(col, columns.size());
    }
    
    public void addIndexColumn(SQLIndex.Column col, int index) {
    	if (col != null && indexOf(col.getColumn()) != -1) 
    		throw new IllegalArgumentException("Column " + col + " already exists in this index.");
    	
        if (isPrimaryKeyIndex()) {
        	index = getParent().getColumnsWithoutPopulating().indexOf(col.getColumn());
        	col.getColumn().setNullable(DatabaseMetaData.columnNoNulls);
        }
    	
    	columns.add(index, col);
    	col.setParent(this);
    	fireChildAdded(SQLIndex.Column.class, col, index);
    	
    	if (isPrimaryKeyIndex()) {
    		getParent().updateRelationshipsForNewIndexColumn(col.getColumn());
    	}
    }

    /**
     * Returns a copy of a SQLIndex from a given SQLIndex in a parent SQLTable.
     * This appears to be mainly used for creating a SQLIndex for a copied table
     * in the playpen when importing tables from a source database as part of
     * the reverse engineering feature.
     *
     * @param source The source SQLIndex to copy
     * @param parentTable The parent SQLTable of the source SQLIndex
     * @return A copy of the given source SQLIndex.
     * @throws SQLObjectException
     */
    public static SQLIndex getDerivedInstance(SQLIndex source, SQLTable parentTable) throws SQLObjectException {

        SQLIndex index = new SQLIndex();
        index.setName(source.getName());
        index.setUnique(source.isUnique());
        index.setPopulated(source.isPopulated());
        index.setType(source.getType());
        index.setFilterCondition(source.getFilterCondition());
        index.setQualifier(source.getQualifier());
        index.setPhysicalName(source.getPhysicalName());
        index.setClustered(source.isClustered());
        for (Map.Entry<Class<? extends SQLObject>, Throwable> inaccessibleReason : source.getChildrenInaccessibleReasons().entrySet()) {
        	index.setChildrenInaccessibleReason(inaccessibleReason.getValue(), inaccessibleReason.getKey(), false);
        }

        for (Column column : source.getChildren(Column.class)) {
            Column newColumn;

            if (column.getColumn() != null) {
            	SQLColumn sqlColumn = findEquivalentColumnNotIncluded(parentTable, index,
						column.getColumn());
            	
                if (sqlColumn == null) {
                    throw new SQLObjectException("Can not derive instance, because coulmn " +
                            column.getColumn().getName() + "is not found in parent table [" + parentTable.getName() +
                            "]");
                }
                newColumn = new Column(sqlColumn, column.getAscendingOrDescending());
            } else {
                newColumn = new Column(column.getName(), column.getAscendingOrDescending());
            }
            index.addChild(newColumn);
        }
        return index;
    }

	/**
	 * Returns a column that is equivalent to the given column in the parent
	 * table that has not been added to the given index. This is a helper method
	 * for updating the given index to another index based on another index.
	 * 
	 * @param parentTable
	 *            The column returned will be a child of this table.
	 * @param index
	 *            The index that the column cannot be a child of already.
	 * @param column
	 *            The column we are trying to find an equivalent version of. If
	 *            the column is in the parent table then the same column will be
	 *            returned. If the column is not in the parent table a different
	 *            column will be returned that closely matches this column.
	 */
	private static SQLColumn findEquivalentColumnNotIncluded(SQLTable parentTable,
			SQLIndex index, SQLColumn column) {
		if (column.getParent().equals(parentTable)) return column;
		
		for (SQLColumn existingCol : parentTable.getColumnsWithoutPopulating()) {
			if (existingCol.getName().equals(column.getName()) &&
					index.indexOf(existingCol) == -1) {
				return existingCol;
			}
		}
		return null;
	}

	/**
	 * Make this index's columns look like the columns in index. If this is the
	 * primary key index columns may be moved in the parent table.
	 * 
	 * @param index
	 *            The index who's columns are what we want in this index
	 * @throws SQLObjectException
	 * @throws ObjectDependentException
	 * @throws IllegalArgumentException
	 */
    public void makeColumnsLike(SQLIndex index) throws SQLObjectException, IllegalArgumentException, ObjectDependentException {
    	makeColumnsLike(index.getChildrenWithoutPopulating(Column.class));
    }
    
    public void makeColumnsLike(List<Column> sourceCols) throws SQLObjectException {
    	List<Column> originalCols = new ArrayList<Column>(columns);
    	for (Column c : originalCols) {
    		boolean remove = true;
    		for (Column sourceCol : sourceCols) {
    			if ((sourceCol.getColumn() == null && sourceCol.getName().equals(c.getName())) 
    					|| (sourceCol.getColumn() != null && sourceCol.getColumn().equals(c.getColumn()))) {
    				remove = false;
    				c.updateToMatch(sourceCol);
    				break;
    			}
    		}
    		if (remove) { 
    			if (isPrimaryKeyIndex()) {
    				getParent().moveAfterPK(c.getColumn());
    			} else {
    				removeColumn(c);
    			}
    		}
    	}
    	
    	int insertIndex = 0;
    	for (Column c : sourceCols) {
    		int currentIndex;
    		if (c.getColumn() == null) {
    			currentIndex = -1;
    			for (int i = 0; i < originalCols.size(); i++) {
    				Column t = originalCols.get(i);
    				if (t.getName().equals(c.getName())) {
    					currentIndex = i;
    					break;
    				}
    			}
    		} else {
    			currentIndex = indexOf(c.getColumn());
    		}
    		if (isPrimaryKeyIndex()) {
    			if (currentIndex != -1 && currentIndex != insertIndex) {
    				getParent().changeColumnIndex(currentIndex, insertIndex, true);
    			} else {
    				SQLColumn equivalentCol = findEquivalentColumnNotIncluded(getParent(), this, c.getColumn());
    				getParent().changeColumnIndex(
    						getParent().getColumnsWithoutPopulating().indexOf(equivalentCol), 
    						insertIndex, true);
    			}
    			getChild(insertIndex).setAscendingOrDescending(c.getAscendingOrDescending());
    		} else {
    			if (currentIndex != -1 && currentIndex != insertIndex) {
    				Column child = getChild(currentIndex);
    				removeColumn(child);
    				child.setAscendingOrDescending(c.getAscendingOrDescending());
    				addIndexColumn(child, insertIndex);
    			} else if (currentIndex == -1) {
    				Column newCol = new Column(c.getName(), c.getAscendingOrDescending());
    	            newCol.setColumn(c.getColumn());
    	            addChild(newCol);
    			}
    		}
    			
    		insertIndex++;
    	}
    }

	@Override
	protected boolean removeChildImpl(SPObject child) {
		if (child instanceof SQLIndex.Column) {
			return removeColumn((SQLIndex.Column) child);
		} else {
			throw new IllegalArgumentException("Cannot remove children of type " 
					+ child.getClass() + " from " + getName());
		}
	}
	
	public boolean removeColumn(SQLIndex.Column col) {
		int index = columns.indexOf(col);
		if (index != -1) {
			columns.remove(index);
			fireChildRemoved(SQLIndex.Column.class, col, index);
			
	        if (col.getColumn() != null) {
	            col.getColumn().removeSPListener(col.targetColumnListener);
	        }
	        
	        if (isPrimaryKeyIndex()) {
	        	getParent().updateRelationshipsForRemovedIndexColumns(col.getColumn());
	        }
	        
			col.setParent(null);
			return true;
		}
		return false;
	}
	
	/**
	 * Removes the given column from this index. This does not change the ordering
	 * of the columns in the table.
	 */
	public boolean removeColumn(SQLColumn col) {
		for (Column colWrapper : columns) {
			if (colWrapper.getColumn().equals(col)) {
				return removeColumn(colWrapper);
			}
		}
		return false;
	}

	public int childPositionOffset(Class<? extends SPObject> childType) {
		if (childType == SQLIndex.Column.class) return 0;
		
		throw new IllegalArgumentException("The type " + childType + 
				" is not a valid child type of " + getName());
	}

	public List<? extends SPObject> getDependencies() {
		return Collections.emptyList();
	}

	public void removeDependency(SPObject dependency) {
        for (SQLObject child : getChildren()) {
            child.removeDependency(dependency);
        }
	}

	@NonProperty
	public List<Class<? extends SPObject>> getAllowedChildTypes() {
		return allowedChildTypes;
	}

	/**
	 * Returns true if there is a {@link Column} wrapper that points to the
	 * given column.
	 */
	public boolean containsColumn(SQLColumn column) {
		for (Column colWrapper : columns) {
			if (colWrapper.getColumn().equals(column)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Helper method for finding the index of a {@link SQLColumn} inside this
	 * index. Returns the index in this column or -1 if the column does not
	 * exist as a wrapped child of this index.
	 */
	public int indexOf(SQLColumn col) {
		for (int i = 0; i < columns.size(); i++) {
			if (columns.get(i).getColumn() != null && 
					columns.get(i).getColumn().equals(col)) {
				return i;
			}
		}
		return -1;
	}
	
	/**
	 * Returns true if there are columns in this index.
	 * @return
	 */
	@Transient @Accessor
	public boolean isEmpty() {
		return columns.isEmpty();
	}

}