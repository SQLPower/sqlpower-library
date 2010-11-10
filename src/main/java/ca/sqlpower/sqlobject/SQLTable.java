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

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import ca.sqlpower.object.ObjectDependentException;
import ca.sqlpower.object.SPObject;
import ca.sqlpower.object.annotation.Accessor;
import ca.sqlpower.object.annotation.Constructor;
import ca.sqlpower.object.annotation.ConstructorParameter;
import ca.sqlpower.object.annotation.ConstructorParameter.ParameterType;
import ca.sqlpower.object.annotation.Mutator;
import ca.sqlpower.object.annotation.NonBound;
import ca.sqlpower.object.annotation.NonProperty;
import ca.sqlpower.object.annotation.Transient;
import ca.sqlpower.sql.CachedRowSet;
import ca.sqlpower.sqlobject.SQLIndex.Column;
import ca.sqlpower.sqlobject.SQLRelationship.SQLImportedKey;
import ca.sqlpower.util.SQLPowerUtils;
import ca.sqlpower.util.SessionNotFoundException;

import com.google.common.collect.ListMultimap;

public class SQLTable extends SQLObject {
	
	/**
	 * Defines an absolute ordering of the child types of this class.
	 */
	@SuppressWarnings("unchecked")
	public static final List<Class<? extends SPObject>> allowedChildTypes = 
		Collections.unmodifiableList(new ArrayList<Class<? extends SPObject>>(
				Arrays.asList(SQLColumn.class, SQLRelationship.class, 
						SQLImportedKey.class, SQLIndex.class)));

	private static Logger logger = Logger.getLogger(SQLTable.class);

	/**
	 * These are the different ways of transferring an object
	 * to/in the play pen. An object could be copied from one 
	 * place to another or reverse engineered. These two 
	 * operations differ slightly in what they create and
	 * inherit.
	 */
	public enum TransferStyles {
	    REVERSE_ENGINEER,
	    COPY;
	}

	protected String remarks="";
	private String objectType;

	/**
	 * A List of SQLColumn objects which make up all the columns of
	 * this table.
	 */
	protected List<SQLColumn> columns = new ArrayList<SQLColumn>();

	/**
	 * A List of SQLRelationship objects describing keys that this
	 * table exports.  This SQLTable is the "pkTable" in its exported
	 * keys.
	 */
	protected List<SQLRelationship> exportedKeys = new ArrayList<SQLRelationship>();

	/**
	 * A List for SQLRelationship objects describing keys that this
	 * table imports.  This SQLTable is the "fkTable" in its imported
	 * keys.
	 */
	protected List<SQLImportedKey> importedKeys = new ArrayList<SQLImportedKey>();

	/**
	 * This index represents the primary key of this table. This will never be
	 * null or changed to prevent confusion with the primary key becoming null.
	 * If there are no columns in the primary key this index will not have
	 * children representing columns.
	 */
	private final SQLIndex primaryKeyIndex;

    /**
     * A List for SQLIndex objects that describe the various database indices
     * that exist on this table.
     */
    private List<SQLIndex> indices = new ArrayList<SQLIndex>();
    
    /**
     * Determinant of whether this table's {@link SQLColumn}s have been populated.
     */
    private boolean columnsPopulated = false;
    
    /**
     * Determinant of whether this table's {@link SQLIndex}s have been populated.
     */
    private boolean indicesPopulated = false;
    
    /**
     * Determinant of whether this table's exported keys have been populated.
     */
    private boolean exportedKeysPopulated = false;
    
    /**
     * Determinant of whether this table's imported keys have been populated.
     */
    private boolean importedKeysPopulated = false;
    
    public SQLTable(SQLObject parent, String name, String remarks, String objectType, 
    		boolean startPopulated) throws SQLObjectException {
    	this(parent, name, remarks, objectType, startPopulated, new SQLIndex());
    }
    
    public SQLTable(SQLObject parent, String name, String remarks, String objectType, 
            boolean startPopulated, SQLIndex primaryKey) throws SQLObjectException {
        this(parent, name, remarks, objectType, startPopulated, 
                primaryKey, startPopulated, startPopulated, startPopulated, startPopulated);
    }

    @Constructor
	public SQLTable(@ConstructorParameter(propertyName = "parent") SQLObject parent,
			@ConstructorParameter(propertyName = "name") String name,
			@ConstructorParameter(propertyName = "remarks") String remarks,
			@ConstructorParameter(propertyName = "objectType") String objectType,
			@ConstructorParameter(propertyName = "populated") boolean startPopulated,
			@ConstructorParameter(parameterType = ParameterType.CHILD, propertyName = "primaryKeyIndex") SQLIndex primaryKeyIndex,
			@ConstructorParameter(propertyName = "columnsPopulated") boolean columnsPopulated,
			@ConstructorParameter(propertyName = "indicesPopulated") boolean indicesPopulated,
			@ConstructorParameter(propertyName = "exportedKeysPopulated") boolean exportedKeysPopulated,
			@ConstructorParameter(propertyName = "importedKeysPopulated") boolean importedKeysPopulated) 
    		throws SQLObjectException {
        super();
		logger.debug("NEW TABLE "+name+"@"+hashCode());
		for (Column wrapper : primaryKeyIndex.getChildrenWithoutPopulating()) {
			if (wrapper.getColumn() == null) {
				throw new SQLObjectException("The primary key of the table " + name + 
						" is not allowed to have calculated columns");
			} else if (!columns.contains(wrapper.getColumn())) {
				throw new SQLObjectException("The primary key of the table " + name + 
						" contains a column " + wrapper.getColumn() + " that is not " +
								"part of the table.");
			}
		}
		this.primaryKeyIndex = primaryKeyIndex;
		primaryKeyIndex.setParent(this);
		initFolders(startPopulated);
		setColumnsPopulated(columnsPopulated);
		setIndicesPopulated(indicesPopulated);
		setExportedKeysPopulated(exportedKeysPopulated);
		setImportedKeysPopulated(importedKeysPopulated);
		setup(parent, name, remarks, objectType);
	}

    /**
     * Sets up the values for the new Table
     */
    private void setup(SQLObject parent, String name, String remarks, String objectType) {
        super.setParent(parent);
		super.setName(name);  // this.setName will try to be far to fancy at this point, and break stuff
		this.remarks = remarks;
		this.objectType = objectType;
		super.setPhysicalName(name);
		updatePKIndexNameToMatch(null, name);
		if (this.objectType == null) throw new NullPointerException();
    }

	/**
	 * Creates a new SQLTable with parent as its parent and a null
	 * schema and catalog.  The table will contain the four default
	 * folders: "Columns" "Exported Keys" "Imported Keys" and "Indices".
     *
     * @param startPopulated The initial setting of this table's folders' <tt>populated</tt> flags.
     * If this is set to false, the table will attempt to lazy-load the child folders.  Otherwise,
     * this table will not try to load its children from a database connection.
	 */
	public SQLTable(SQLDatabase parent, boolean startPopulated) throws SQLObjectException {
		this(parent, "", "", "TABLE", startPopulated);
	}

	/**
	 * Creates a new SQLTable with parent as its parent and a null schema and
	 * catalog. The table will contain the four default folders: "Columns"
	 * "Exported Keys" "Imported Keys" and "Indices".
	 * 
	 * @param startPopulated
	 *            The initial setting of this table's folders'
	 *            <tt>populated</tt> flags. If this is set to false, the table
	 *            will attempt to lazy-load the child folders. Otherwise, this
	 *            table will not try to load its children from a database
	 *            connection.
	 * @param primaryKeyIndex
	 *            the primary key of the table. This allows for a setup to
	 *            create a primary key specific for this table.
	 * @throws SQLObjectException 
	 */
	public SQLTable(SQLDatabase parent, boolean startPopulated, SQLIndex primaryKeyIndex) throws SQLObjectException {
		this(parent, "", "", "TABLE", startPopulated, primaryKeyIndex);
	}

	/**
	 * Creates a new SQLTable with no children, no parent, and all
	 * properties set to their defaults.   Note this should never
     * Initialize the folders.
	 *
	 * <p>This is mainly for code that needs to reconstruct a SQLTable
	 * from outside configuration info, such as the SwingUIProject.load() method.
	 * If you want to make SQLTable objects from scratch, consider using one
	 * of the other constructors, which initialise the state more thoroughly.
	 *
	 */
	public SQLTable() {
	    super();
		primaryKeyIndex = new SQLIndex();
		primaryKeyIndex.setParent(this);
        setup(null,null,null,"TABLE");
	}



	/**
	 * If you create a table from scratch using the no-args
	 * constructor, you should call this to create the standard set of
	 * Folder objects under this table.  The regular constructor does
	 * it automatically.
	 *
	 * @param populated The initial value to give the folders'
	 * populated status.  When loading from a file, this should be true;
	 * if lazy loading from a database, it should be false.
	 */
	public void initFolders(boolean populated) {
		this.populated = populated;
		columnsPopulated = populated;
		exportedKeysPopulated = populated;
		importedKeysPopulated = populated;
		indicesPopulated = populated;
	}

	/**
	 * Updates all the simple properties of this SQLTable to match those of the given
	 * source table. Does not descend into children (columns, indexes, etc). Does not
	 * update the parent pointer of this SQLObject.
	 */
	@Override
	public void updateToMatch(SQLObject source) throws SQLObjectException {
	    SQLTable sourceTable = (SQLTable) source;
	    setName(sourceTable.getName());
	    setRemarks(sourceTable.getRemarks());
	    setPhysicalName(sourceTable.getPhysicalName());
	    setObjectType(sourceTable.getObjectType());
	}
	
	/**
	 * Creates a new SQLTable under the given parent database.  The new table will have
	 * all the same properties as the given source table.
	 *
	 * @param parent The database to insert the new table into
	 * @return The new table
	 * @throws SQLObjectException if there are populate problems on source or parent
	 * Or if the parent has children of type other than SQLTable.
	 */
	public SQLTable createInheritingInstance(SQLDatabase parent) throws SQLObjectException {
		return createTableFromSource(parent, TransferStyles.REVERSE_ENGINEER, false);
	}
	
	/**
	 * This method creates a new table based on this table. This method
	 * is used for reverse engineering and copying as they are similar.
	 * @throws SQLObjectException if there are populate problems on source or parent
	 * Or if the parent has children of type other than SQLTable.
	 */
	private SQLTable createTableFromSource(SQLDatabase parent, TransferStyles transferStyle, boolean preserveColumnSource)
		throws SQLObjectException {
		populateColumns();
		populateIndices();
		populateRelationships();
		populateImportedKeys();
        SQLIndex newPKIndex = new SQLIndex();
        SQLTable t = new SQLTable(parent, getName(), remarks, "TABLE", true, newPKIndex);
		for (Map.Entry<Class<? extends SQLObject>, Throwable> inaccessibleReason : getChildrenInaccessibleReasons().entrySet()) {
        	t.setChildrenInaccessibleReason(inaccessibleReason.getValue(), inaccessibleReason.getKey(), false);
        }

		t.inherit(this, transferStyle, preserveColumnSource);
        inheritIndices(this, t);
        
		parent.addChild(t);
		return t;
	}
	
	/**
	 * Creates a new SQLTable under the given parent database.  The new table will have
	 * all the same properties as the given source table.
	 *
	 * @param parent The database to insert the new table into
	 * @return The new table
	 * @throws SQLObjectException if there are populate problems on source or parent
	 * Or if the parent has children of type other than SQLTable.
	 */
	public SQLTable createCopy(SQLDatabase parent, boolean preserveColumnSource)
		throws SQLObjectException {
		return createTableFromSource(parent, TransferStyles.COPY, preserveColumnSource);
	}

	/**
	 * inherit indices from the source table. This will update the target's
	 * primary key index to match the source table.
	 * 
	 * @param source
	 * @param target
	 * @throws SQLObjectException
	 */
    private static void inheritIndices(SQLTable source, SQLTable target) throws SQLObjectException {
        for ( SQLIndex index : source.getIndices()) {
        	if (index.isPrimaryKeyIndex()) {
        		target.getPrimaryKeyIndex().updateToMatch(index);
        	} else {
        		SQLIndex index2 = SQLIndex.getDerivedInstance(index,target);
        		target.addIndex(index2);
        	}
        }
    }

	/**
	 * Populates the columns of all tables from the database that are not
	 * already populated. If successful, then the indices for just this table
	 * will also be populated.
	 * 
	 * @throws SQLObjectException
	 */
    protected void populateColumns() throws SQLObjectException {
    	if (columnsPopulated) return;
    	synchronized(getClass()) {
    		synchronized(this) {
    			if (columns.size() > 0) {
    				throw new IllegalStateException("Can't populate table because it already contains columns");
    			}

    			logger.debug("column folder populate starting for table " + getName());

    			populateAllColumns(getCatalogName(), getSchemaName(), getName(), getParentDatabase(), getParent());

    			logger.debug("column folder populate finished for table " + getName());

    			populateIndices();
    		}
    	}
    }

	/**
	 * This method will populate all of the columns in all of the tables with
	 * one database call. This is done for optimization as making one database
	 * call for each table for just the columns in that table can become very
	 * slow with network traffic. At the end of this method all of the tables
	 * passed in will be populated with all of the columns found for the tables
	 * in the database. No additional tables will be created even if columns
	 * exist for missing tables.
	 * <p>
	 * Note that this class will iterate over all columns, obtaining locks on
	 * all them. Any methods calling this must be sure to synchronize on the
	 * class <b>before</b> the table instance, or else risk causing deadlock.
	 * <p>
	 * This is a helper method for {@link #populateColumns()}.
	 * 
	 * @param catalogName
	 *            The catalog name the tables are contained in, may be null.
	 * @param schemaName
	 *            The schema name the tables are contained in, may be null.
	 * @param parentDB
	 *            The parent database object. Will be used to connect to the
	 *            database with.
	 * @param tableContainer
	 *            The SQLObject that contains all of the tables in the system.
	 * @throws SQLObjectException
	 */
    private synchronized static void populateAllColumns(final String catalogName, final String schemaName,
    		final String tableName,
    		final SQLDatabase parentDB, final SQLObject tableContainer) throws SQLObjectException {
    	Connection con = null;
		try {
		    con = parentDB.getConnection();
		    DatabaseMetaData dbmd = con.getMetaData();
		    final ListMultimap<String, SQLColumn> cols = SQLColumn.fetchColumnsForTable(
		    		catalogName, schemaName, tableName, dbmd);
		    Runnable runner = new Runnable() {
				public void run() {
					try {
						parentDB.begin("Populating all columns");
						if (cols.isEmpty()) {
							SQLTable t = parentDB.getTableByName(catalogName, schemaName, tableName);
							if (t != null) {
								t.setColumnsPopulated(true);
							}
						}
						for (String tableName : cols.keySet()) {
						    SQLTable table = tableContainer.getChildByName(tableName, SQLTable.class);
						    //The multimap will contain table names of system tables 
						    //that are not contained in the table container. Skipping 
						    //these tables. If a table is missed due to an error it will
						    //at least not be marked as populated.
						    if (table == null) continue; 
						    if (table.isColumnsPopulated()) continue;
						    populateColumnsWithList(table, cols.get(tableName));
						}
						parentDB.commit();
					} catch (Throwable t) {
						parentDB.rollback(t.getMessage());
						throw new RuntimeException(t);
					}
				}
			};
			try {
				parentDB.getRunnableDispatcher().runInForeground(runner);
			} catch (SessionNotFoundException e) {
				runner.run();
			}
		} catch (SQLException e) {
			throw new SQLObjectException("Failed to populate columns of tables", e);
		} finally {
			if (con != null) {
			    try {
			        con.close();
			    } catch (SQLException ex) {
			        logger.error("Couldn't close connection. Squishing this exception: ", ex);
			    }
			}
		}
    }

    /**
     * Used to populate a table based on a list containing all of the column
     * children of the table. This method must be called on the foreground
     * thread. If the table is not populated when calling this method the table
     * will be considered populated in terms of columns at the end. If the table
     * is populated then this is will add the children to the table as a group.
     * (Happens when loading from a repository with the server.)
     * <p>
     * Package private so they can be called from {@link SQLObjectUtils}.
     * 
     * @param table
     *            The table to populate with the child list.
     * @param allChildren
     *            A list that contains all of the child columns that belong to
     *            the table.
     */
    static void populateColumnsWithList(SQLTable table, List<SQLColumn> allChildren) {
        if (!table.isForegroundThread()) 
            throw new IllegalStateException("This method must be called on the foreground thread.");
        boolean populateStart = table.columnsPopulated;
        try {
            //The children are added without firing events because we need all of the
            //children to be added to the table and have the table defined as populated
            //before outside code is notified.
            int index = table.columns.size();
            for (SQLColumn col : allChildren) {
                table.columns.add(col);
                col.setParent(table);
            }
            table.columnsPopulated = true;
            
            table.begin("Populating all columns");
            for (SQLColumn col : allChildren) {
                table.fireChildAdded(SQLColumn.class, col, index);
                index++;
            }
            table.firePropertyChange("columnsPopulated", populateStart, true);
            table.commit();
        } catch (Throwable t) {
            table.rollback(t.getMessage());
            for (SQLColumn col : allChildren) {
                table.columns.remove(col);
            }
            table.columnsPopulated = populateStart;
            throw new RuntimeException(t);
        }
    }

    /**
     * Retrieves all index information about this table from the source database
     * it is associated with.  If the index folder has already been populated, this
     * method returns immediately with no side effects.
     * 
     * <p>Note: It is essential that the columns folder of this table has been populated before calling
     * this method.
     * 
     * @throws IllegalStateException if the columns folder is not yet populated, or if the
     * index folder is both non-empty and non-populated
     */
	protected synchronized void populateIndices() throws SQLObjectException {
        if (indicesPopulated) return;
		if (indices.size() > 0) {
			throw new IllegalStateException("Can't populate indices because it already contains children!");
		} 
        
        // If the SQLTable is a view, simply indicated folder is populated and then leave
        // Since Views don't have indices (and Oracle throws an error)
        if (objectType.equals("VIEW")) { 
        	runInForeground(new Runnable() {
				public void run() {
					setIndicesPopulated(true);
				}
			});
            return;
        }
        
        logger.debug("index folder populate starting");

        Connection con = null;
        try {
            con = getParentDatabase().getConnection();
            DatabaseMetaData dbmd = con.getMetaData();
            logger.debug("before addIndicesToTable");
            
            final List<SQLIndex> indexes = SQLIndex.fetchIndicesForTableAndUpdatePK(dbmd, this);
            
            runInForeground(new Runnable() {
				public void run() {
					//someone beat us to this already
					if (indicesPopulated) return;
					populateIndicesWithList(SQLTable.this, indexes);
				}
			});
            logger.debug("found "+indices.size()+" indices.");
          
        } catch (SQLException e) {
            throw new SQLObjectException("Failed to populate indices of table "+getName(), e);
        } finally {
            try {
                if (con != null) con.close();
            } catch (SQLException e) {
                logger.error("Closing connection failed. Squishing this exception: ", e);
            }
            
        }
        logger.debug("index folder populate finished");
    }

    /**
     * Used to populate a table based on a list containing all of the index
     * children of the table. This method must be called on the foreground
     * thread. The columns of the table must be populated before the indices for
     * the indices to have proper objects to reference. If the table is not
     * populated when calling this method the table will be considered populated
     * in terms of indices at the end. If the table is populated then this is
     * will add the children to the table as a group. (Happens when loading from
     * a repository with the server.)
     * <p>
     * Package private so they can be called from {@link SQLObjectUtils}.
     * 
     * @param table
     *            The table to populate with the child list.
     * @param indices
     *            A list that contains all of the child indices that belong to
     *            the table.
     */
	static void populateIndicesWithList(SQLTable table, List<SQLIndex> indices) {
	    if (!table.isForegroundThread()) 
            throw new IllegalStateException("This method must be called on the foreground thread.");
        if (!table.columnsPopulated)
            throw new IllegalStateException("Columns must be populated");
        boolean startPopulated = table.indicesPopulated;
        try {
            //Must set the indices and then fire events so the populate flag
            //is set before events fire to keep the state of the system consistent.
            for (SQLIndex i : indices) {
                table.indices.add(i);
                i.setParent(table);
            }
            table.indicesPopulated = true;
            
            table.begin("Populating Indices for Table " + table);
            for (SQLIndex i : indices) {
                table.fireChildAdded(SQLIndex.class, i, table.indices.indexOf(i) + 1);
            }
            table.firePropertyChange("indicesPopulated", startPopulated, true);
            table.commit();
        } catch (Throwable t) {
            table.rollback(t.getMessage());
            for (SQLIndex i : indices) {
                table.indices.remove(i);
            }
            table.indicesPopulated = startPopulated;
            throw new RuntimeException(t);
        }
	}
	
	protected void populateImportedKeys() throws SQLObjectException {
		// Must synchronize on class before instance. See populateAllColumns
		if (importedKeysPopulated) return;
		synchronized(getClass()) {
			synchronized(this) {

				CachedRowSet crs = null;
				Connection con = null;
				try {
					con = getParentDatabase().getConnection();
					DatabaseMetaData dbmd = con.getMetaData();
					crs = new CachedRowSet();
					ResultSet exportedKeysRS = dbmd.getImportedKeys(getCatalogName(), getSchemaName(), getName());
					crs.populate(exportedKeysRS);
					exportedKeysRS.close();
				} catch (SQLException ex) {
					throw new SQLObjectException("Couldn't locate related tables", ex);
				} finally {
					// close the connection before it makes the recursive call
					// that could lead to opening more connections
					try {
						if (con != null) con.close();
					} catch (SQLException ex) {
						logger.warn("Couldn't close connection", ex);
					}
				}
				try {
					while (crs.next()) {
						if (crs.getInt(9) != 1) {
							// just another column mapping in a relationship we've already handled
							logger.debug("Got exported key with sequence " + crs.getInt(9) + " on " + crs.getString(5) + "." + crs.getString(6) + "." + crs.getString(7) + ", continuing.");
							continue;
						}
						logger.debug("Got exported key with sequence " + crs.getInt(9) + " on " + crs.getString(5) + "." + crs.getString(6) + "." + crs.getString(7) + ", populating.");
						String cat = crs.getString(1);
						String sch = crs.getString(2);
						String tab = crs.getString(3);
						SQLTable pkTable = getParentDatabase().getTableByName(cat, sch, tab);
						if (pkTable == null) {
							throw new IllegalStateException("While populating table " +
									SQLObjectUtils.toQualifiedName(getParent()) +
									", I failed to find child table " +
									"\""+cat+"\".\""+sch+"\".\""+tab+"\"");
						}
						pkTable.populateColumns();
						pkTable.populateIndices();
						pkTable.populateRelationships(this);
					}
					setImportedKeysPopulated(true);
				} catch (SQLException ex) {
					throw new SQLObjectException("Couldn't locate related tables", ex);
				} finally {
					try {
						if (crs != null) crs.close();
					} catch (SQLException ex) {
						logger.warn("Couldn't close resultset", ex);
					}
				}
			}
		}
	}
	
	/**
	 * Populates all the imported key relationships, where it first ensures that
	 * columns have been populated.
	 * 
	 * @throws SQLObjectException
	 */
	protected void populateExportedKeys() throws SQLObjectException {
		// Must synchronize on class before instance. See populateAllColumns
		synchronized(getClass()) {
			synchronized(this) {
				populateColumns();
				populateIndices();
				populateRelationships();
			}
		}
	}
	
	/**
	 * Populates all the exported key relationships.  This has the
	 * side effect of populating the imported key side of the
	 * relationships for the exporting tables.
	 * <p>
	 * XXX This is a temporary patch to make relationship populating not cascade.
	 * This can be improved upon.
	 */
    protected synchronized void populateRelationships(SQLTable fkTable) throws SQLObjectException {
    	if (exportedKeysPopulated) {
    		return;
    	}

		logger.debug("SQLTable: relationship populate starting");

		final List<SQLRelationship> newKeys = SQLRelationship.fetchExportedKeys(this, fkTable);
		runInForeground(new Runnable() {
			public void run() {

				//Someone beat us to populating the relationships
				if (exportedKeysPopulated) return;
				populateRelationshipsWithList(SQLTable.this, newKeys);
			}
		});

		logger.debug("SQLTable: relationship populate finished");

	}

	/**
	 * Populates all the exported key relationships.  This has the
	 * side effect of populating the imported key side of the
	 * relationships for the exporting tables.
	 */
    protected synchronized void populateRelationships() throws SQLObjectException {
    	if (exportedKeysPopulated) {
    		return;
    	}

		logger.debug("SQLTable: relationship populate starting");

		final List<SQLRelationship> newKeys = SQLRelationship.fetchExportedKeys(this, null);
		runInForeground(new Runnable() {
			public void run() {

				//Someone beat us to populating the relationships
				if (exportedKeysPopulated) return;
				populateRelationshipsWithList(SQLTable.this, newKeys);
			}
		});

		logger.debug("SQLTable: relationship populate finished");

	}

    /**
     * Used to populate a table based on a list containing all of the exported
     * relationship children of the table. This method must be called on the
     * foreground thread. The columns and indices of the table must be populated
     * before the relationships so the relationships have proper columns and
     * primary key to reference. If the table is not populated when calling this
     * method the table will be considered populated in terms of exported keys
     * at the end. If the table is populated then this is will add the children
     * to the table as a group. (Happens when loading from a repository with the
     * server.)
     * <p>
     * Package private so they can be called from {@link SQLObjectUtils}.
     * 
     * @param table
     *            The table to populate with the child list.
     * @param allChildren
     *            A list that contains all of the child relationships that will
     *            be added to the table. For backwards compatibility if the
     *            relationship exists in the table it won't be added again.
     */
    static void populateRelationshipsWithList(SQLTable table, List<SQLRelationship> allChildren) {
        if (!table.isForegroundThread()) 
            throw new IllegalStateException("This method must be called on the foreground thread.");
        if (!table.columnsPopulated) {
            throw new IllegalStateException("Table must be populated before relationships are added");
        }
        if (!table.indicesPopulated) {
            throw new IllegalStateException("Table indices must be populated before relationships are added");
        }
        boolean startPopulated = table.exportedKeysPopulated;
        List<SQLRelationship> relsAdded = new ArrayList<SQLRelationship>();
        try {

            for (SQLRelationship addMe : allChildren) {
                //This if is for backwards compatibility
            	addMe.getParent().exportedKeys.add(addMe);
            	if (addMe.getPkTable().isMagicEnabled()) {
            		if (!addMe.getFkTable().getImportedKeysWithoutPopulating().contains(addMe.getForeignKey())) {
            			addMe.getFkTable().importedKeys.add(addMe.getForeignKey());
            			relsAdded.add(addMe);
            		}
            	}
            }
            table.exportedKeysPopulated = true;

            table.begin("Populating relationships for Table " + table);
            for (SQLRelationship addMe : relsAdded) {
                SQLTable pkTable = addMe.getParent();
                if (pkTable.isMagicEnabled()) {
                	SQLTable fkTable = addMe.getFkTable();
                	SQLImportedKey foreignKey = addMe.getForeignKey();
                	addMe.attachRelationship(pkTable, fkTable, false, false);
                	fkTable.fireChildAdded(SQLImportedKey.class, foreignKey, fkTable.importedKeys.indexOf(foreignKey));
                }
                pkTable.fireChildAdded(SQLRelationship.class, addMe, pkTable.exportedKeys.indexOf(addMe));
            }
            table.firePropertyChange("exportedKeysPopulated", startPopulated, true);
            table.commit();
        } catch (SQLObjectException e) {
            table.rollback(e.getMessage());
            for (SQLRelationship rel : relsAdded) {
                rel.getParent().exportedKeys.remove(rel);
                if (table.isMagicEnabled()) {
                	rel.getFkTable().importedKeys.remove(rel.getForeignKey());
                }
            }
            table.exportedKeysPopulated = startPopulated;
            throw new SQLObjectRuntimeException(e);
        } catch (Throwable t) {
            table.rollback(t.getMessage());
            for (SQLRelationship rel : relsAdded) {
                rel.getParent().exportedKeys.remove(rel);
                rel.getFkTable().importedKeys.remove(rel.getForeignKey());
            }
            table.exportedKeysPopulated = startPopulated;
            throw new RuntimeException(t);
        }
    }

	public void addImportedKey(SQLImportedKey r) {
		addImportedKey(r, importedKeys.size());
	}
	
	public void addImportedKey(SQLImportedKey k, int index) {
		importedKeys.add(index, k);
		k.setParent(this);
		fireChildAdded(SQLImportedKey.class, k, index);
	}

	public boolean removeImportedKey(SQLImportedKey k) {
		if (isMagicEnabled() && k.getParent() != this) {
			throw new IllegalStateException("Cannot remove child " + k.getName() + 
					" of type " + k.getClass() + " as its parent is not " + getName());
		}
		
		k.getRelationship().disconnectRelationship(false);
		
		int index = importedKeys.indexOf(k);
		if (index != -1) {
			 importedKeys.remove(index);
			 fireChildRemoved(SQLImportedKey.class, k, index);
			 return true;
		}
		return false;
	}

	public void addExportedKey(SQLRelationship r) {
		addExportedKey(r, exportedKeys.size());
	}
	
	public void addExportedKey(SQLRelationship r, int index) {
		exportedKeys.add(index, r);
		r.setParent(this);
		fireChildAdded(SQLRelationship.class, r, index);
	}

	public boolean removeExportedKey(SQLRelationship r) {
		if (isMagicEnabled() && r.getParent() != this) {
			throw new IllegalStateException("Cannot remove child " + r.getName() + 
					" of type " + r.getClass() + " as its parent is not " + getName());
		}
		r.disconnectRelationship(true);
		
		int index = exportedKeys.indexOf(r);
		if (index != -1) {
			 exportedKeys.remove(index);
			 fireChildRemoved(SQLRelationship.class, r, index);
			 return true;
		}
		return false;
	}

	/**
	 * Counts the number of columns in the primary key of this table.
	 * <p>
	 * This does not populate the columns or indicies of the table. Either
	 * the table should be populated already or populate may need to be called.
	 */
	@Transient @Accessor
	public int getPkSize() {
		return primaryKeyIndex.getChildrenWithoutPopulating().size();
	}

	/**
	 * Adds all the columns of the given source table to the end of
	 * this table's column list.
	 */
	public void inherit(SQLTable source, TransferStyles transferStyle, boolean preserveColumnSource) throws SQLObjectException {
		inherit(columns.size(), source, transferStyle, preserveColumnSource);
	}

	/**
	 * Inserts all the columns of the given source table into this
	 * table at position <code>pos</code>.
	 *
	 * <p>If this table currently has no columns, then the source's
	 * primary key will remain intact (and this table will become an
	 * identical copy of source).  If not, and if the insertion
	 * position <= this.pkSize(), then all source columns will be
	 * added to this table's primary key.  Otherwise, no source
	 * columns will be added to this table's primary key.
	 */
	public List<SQLColumn> inherit(int pos, SQLTable source, TransferStyles transferStyle, boolean preserveColumnSource) throws SQLObjectException {
		if (source == this) {
			throw new SQLObjectException("Cannot inherit from self");
		}

		boolean addToPK;
		int pkSize = getPkSize();
		source.populateColumns();
		source.populateIndices();
		if (pos < pkSize) {
			addToPK = true;
		} else {
			addToPK = false;
		}

		List<SQLColumn> addedColumns = new ArrayList<SQLColumn>(); 
		
		begin("Inherting columns from source table");
		for (SQLColumn child : source.getColumns()) {
			addedColumns.add(inherit(pos, child, addToPK, transferStyle, preserveColumnSource));
			pos++;
		}
		commit();
		return addedColumns;
	}

	public SQLColumn inherit(int pos, SQLColumn sourceCol, boolean addToPK, TransferStyles transferStyle, boolean preserveColumnSource) throws SQLObjectException {
	    if (addToPK && pos > 0 && !getColumn(pos - 1).isPrimaryKey()) {
	        throw new IllegalArgumentException("Can't inherit new PK column below a non-PK column! Insert pos="+pos+"; addToPk="+addToPK);
	    }
		SQLColumn c;
		switch (transferStyle) {
		case REVERSE_ENGINEER:
			c = sourceCol.createInheritingInstance(this);
			break;
		case COPY:
			c = sourceCol.createCopy(this, preserveColumnSource);
			break;
		default:
			throw new IllegalStateException("Unknown transfer type of " + transferStyle);
		}
		addColumn(c, addToPK, pos);
		return c;
	}

	@NonProperty
	public SQLColumn getColumn(int index) throws SQLObjectException {
		populateColumns();
		return columns.get(index);
	}

	/**
	 * Populates this table then searches for the named column in a case-insensitive
     * manner.
	 */
	@NonProperty
	public SQLColumn getColumnByName(String colName) throws SQLObjectException {
		return getColumnByName(colName, true, false);
	}
	
	/**
	 * Searches for the named column.
	 *
	 * @param populate If true, this table will retrieve its column
	 * list from the database; otherwise it just searches the current
	 * list.
	 */
	@NonProperty
	public SQLColumn getColumnByName(String colName, boolean populate, boolean caseSensitive) 
	    throws SQLObjectException {

		if (populate) populateColumns();
		/* if columnsFolder.children.iterator(); gets changed to getColumns().iterator()
		 * we get infinite recursion between populateColumns, getColumns,
		 * getColumnsByName and addColumnsToTable
		 */
		if (logger.isDebugEnabled()) {
//		    logger.debug("Looking for column "+colName+" in "+columns);
//		    logger.debug("Table " + getName() + " has " + columns.size() + " columns");
		}
		for (SQLColumn col : columns) {
			//logger.debug("Current column name is '" + col.getName() + "'");
            if (caseSensitive) {
                if (col.getName().equals(colName)) {
                    logger.debug("FOUND");
                    return col;
                }
            } else {
                if (col.getName().equalsIgnoreCase(colName)) {
                    logger.debug("FOUND");
                    return col;
                }
            }
		}
		logger.debug("NOT FOUND");
		return null;
	}

	@NonProperty
	public int getColumnIndex(SQLColumn col) throws SQLObjectException {
//		logger.debug("Looking for column index of: " + col);

		List<SQLColumn> columns = getColumns();
		int index = columns.indexOf(col);
		
		if (index == -1) {
//			logger.debug("NOT FOUND");
		}
		
		return index;
	}

	public void addColumn(SQLColumn col) throws SQLObjectException {
		addColumn(col, columns.size());
	}

	/**
	 * Adds a column to the given position in the table. If magic is enabled the
	 * primary key sequence of the column may be updated depending on the
	 * position of the column and the number of primary keys.If the column is
	 * being added just after the primary key the column will not be added to
	 * the primary key. If this decision has been already decided see
	 * {@link #addColumn(SQLColumn, boolean, int)}.
	 * 
	 * @param col
	 *            The column to add.
	 * @param pos
	 *            The position to add the column to.
	 * @throws SQLObjectException
	 */
	public void addColumn(SQLColumn col, int pos) throws SQLObjectException {
		boolean addToPk = getPkSize() > pos;
		
		addColumnWithoutPopulating(col, addToPk, pos);
	}
	
	/**
	 * XXX The boolean is before the position to prevent it from having the same
	 * signature of a recently removed method. This is mainly for refactoring
	 * aid.
	 * 
	 * @param col
	 *            The column to add to the table.
	 * @param addToPk
	 *            If true and the position is valid to be in the primary key the
	 *            column will be added to the primary key. If false and the
	 *            position is valid the column will not be added to the primary
	 *            key. This is mainly for use in the edge case when adding a
	 *            column just after the last column of the primary key to decide
	 *            if the column should be in the primary key.
	 * @param pos
	 *            The position to add the column to.
	 */
	public void addColumn(SQLColumn col, boolean addToPk, int pos) throws SQLObjectException {
		populateColumns();
		addColumnWithoutPopulating(col, addToPk, pos);		
	}
	
	/**
	 * Adds a {@link SQLColumn} to the end of the child list without populating first.
	 */
	public void addColumnWithoutPopulating(SQLColumn col) {
		addColumnWithoutPopulating(col, columns.size());
	}

	/**
	 * Adds a {@link SQLColumn} at a given index of the child list without
	 * populating first. If the column is being added just after the primary key
	 * the column will not be added to the primary key. If this decision has
	 * been already decided see
	 * {@link #addColumnWithoutPopulating(SQLColumn, boolean, int)}.
	 */
	public void addColumnWithoutPopulating(SQLColumn col, int pos) {
		boolean addToPk = getPkSize() > pos;
		
		addColumnWithoutPopulating(col, addToPk, pos);
	}

	/**
	 * Adds a {@link SQLColumn} at a given index of the child list without
	 * populating first. This will throw an {@link IllegalArgumentException} if
	 * the boolean to add to the pk and the position do not agree.
	 * <p>
	 * XXX The boolean is before the position to prevent it from having the same
	 * signature of a recently removed method. This is mainly for refactoring
	 * aid.
	 * 
	 * @param col
	 *            The column to add to the table.
	 * @param addToPk
	 *            If true and the position is valid to be in the primary key the
	 *            column will be added to the primary key. If false and the
	 *            position is valid the column will not be added to the primary
	 *            key. This is mainly for use in the edge case when adding a
	 *            column just after the last column of the primary key to decide
	 *            if the column should be in the primary key.
	 * @param pos
	 *            The position to add the column to.
	 */
	public void addColumnWithoutPopulating(SQLColumn col, boolean addToPk, int pos) {
		int pkSize = getPkSize();
		if ((pos < pkSize && !addToPk) || (pos > pkSize && addToPk)) {
			throw new IllegalArgumentException("The column " + col + " is being " + (addToPk ? "" : "not") + " added to " +
					"the primary key at position " + pos + " but there are " + pkSize + 
					" pk column(s) so the add position is invalid.");
		}
		
		if (columns.indexOf(col) != -1) {
			col.addReference();
			return;
		}

		columns.add(pos, col);
		col.setParent(this);
		fireChildAdded(SQLColumn.class, col, pos);

		if (isMagicEnabled()) {
			if (addToPk) {
				primaryKeyIndex.addIndexColumn(col);
			}
		}
	}

	/**
     * Adds the given SQLIndex object to this table's index folder.
	 */
    public void addIndex(SQLIndex sqlIndex) {
    	addIndex(sqlIndex, indices.size() + 1);
    }

	/**
	 * Adds the given SQLIndex object to this table's index list.
	 * 
	 * @param sqlIndex
	 *            The index to add to this table
	 * @param index
	 *            The index to place the object at. This includes the primary
	 *            key index.
	 */
    public void addIndex(SQLIndex sqlIndex, int index) {
    	//The primary key index is not added to the index list.
    	if (sqlIndex == primaryKeyIndex) return;
    	
    	indices.add(index - 1, sqlIndex);
    	sqlIndex.setParent(this);
    	fireChildAdded(SQLIndex.class, sqlIndex, index);
    }

    @Override
    protected void addChildImpl(SPObject child, int index) {
		if (child instanceof SQLColumn) {
			addColumnWithoutPopulating((SQLColumn) child, index);
		} else if (child instanceof SQLRelationship) {
			addExportedKey((SQLRelationship) child);
		} else if (child instanceof SQLImportedKey) {
			addImportedKey((SQLImportedKey) child);
		} else if (child instanceof SQLIndex) {
			addIndex((SQLIndex) child, index);
		} else {
			throw new IllegalArgumentException("The child " + child.getName() + 
					" of type " + child.getClass() + " is not a valid child type of " + 
					getClass() + ".");
		}
	}

	/**
     * Calls {@link #removeColumn(SQLColumn)} with the appropriate argument.
     * 
     * @throws LockedColumnException
     *             If the column is "owned" by a relationship, and cannot be
     *             safely removed.
     */
	public boolean removeColumn(int index) throws SQLObjectException {
		try {
			return removeChild(columns.get(index));
		} catch (IllegalArgumentException e) {
			throw new RuntimeException(e);
		} catch (ObjectDependentException e) {
			throw new RuntimeException(e);
		}
	}

    /**
     * Removes the given column if it is in this table. If you want to change a
     * column's, index, use the {@link #changeColumnIndex(int,int)} method
     * because it does not throw LockedColumnException.
     * 
     * <p>
     * FIXME: This should be implemented by decreasing the column's reference
     * count. (addColumn already does increase reference count when appropriate)
     * Then, everything that manipulates reference counts directly can just use
     * regular addColumn and removeColumn and magic will take care of the
     * correct behaviour!
     * 
     * @throws LockedColumnException
     *             If the column is "owned" by a relationship, and cannot be
     *             safely removed.
     */
	public boolean removeColumn(SQLColumn col) {
		if (isMagicEnabled() && col.getParent() != this) {
			throw new IllegalStateException("Cannot remove child " + col.getName() + 
					" of type " + col.getClass() + " as its parent is not " + getName());
		}
		
		if (isMagicEnabled()) {
			primaryKeyIndex.removeColumn(col);
		}
		
		try {
			begin("Removing column " + col.getName());
			int index = columns.indexOf(col);
			if (index != -1) {
				columns.remove(index);
				fireChildRemoved(SQLColumn.class, col, index);
				col.setParent(null);
				return true;
			}
		} finally {
			commit();
		}
		return false;
	}

	/**
	 * Moves the column at index <code>oldIdx</code> to index
	 * <code>newIdx</code>. This may cause the moved column to become part of
	 * the primary key (or to be removed from the primary key).
	 * 
	 * @param oldIdx
	 *            the present index of the column.
	 * @param newIdx
	 *            the index that the column will have when this
	 * @param putInPK
	 *            Used to decide if the column should go into the pk on corner
	 *            cases. If the column is being moved to a position where it is
	 *            above a column in the primary key it will be placed in the
	 *            primary key. If the column is being moved to a position where
	 *            it is below a column not in the primary key it will not be
	 *            placed in the primary key. If the column is placed where it
	 *            could be either in or out of the primary key the boolean will
	 *            decide what to do. method returns.
	 */
	public void changeColumnIndex(int oldIdx, int newIdx, boolean putInPK) throws SQLObjectException {
		SQLColumn col = columns.get(oldIdx);
		int pkSize = getPkSize();
		if ((newIdx < pkSize && !putInPK && !col.isPrimaryKey()) ||
				(newIdx < pkSize - 1 && !putInPK && col.isPrimaryKey())) {
			putInPK = true;
		} else if ((newIdx > pkSize && putInPK && !col.isPrimaryKey()) ||
				(newIdx > pkSize - 1 && putInPK && col.isPrimaryKey())) {
			putInPK = false;
		}
		
        try {
            begin("Changing column index");
            
            // If the indices are the same, then there's no point in moving the column
            if (oldIdx != newIdx) {
            	removeColumn(col);
            	addColumn(col, putInPK, newIdx);
            } else if (putInPK && !col.isPrimaryKey()) {
            	primaryKeyIndex.addIndexColumn(col);
            } else if (!putInPK && col.isPrimaryKey()) {
            	primaryKeyIndex.removeColumn(col);
            }

            commit();
        } catch (SQLObjectException e) {
            rollback(e.getMessage());
            throw e;
        } catch (Exception e) {
            rollback(e.getMessage());
            throw new RuntimeException(e);
        }
	}

	public List<SQLRelationship> keysOfColumn(SQLColumn col) throws SQLObjectException {
		LinkedList<SQLRelationship> keys = new LinkedList<SQLRelationship>();
		for (SQLRelationship r : exportedKeys) {
			if (r.containsPkColumn(col)) {
				keys.add(r);
			}
		}
		for (SQLRelationship r : exportedKeys) {
			if (r.containsFkColumn(col)) {
				keys.add(r);
			}
		}
		return keys;
	}

	public String toString() {
		return getShortDisplayName();
	}

	// ---------------------- SQLObject support ------------------------

	/**
	 * The table's name.
	 */
	@Transient @Accessor
	public String getShortDisplayName() {
		SQLSchema schema = getSchema();
		if (schema != null) {
			return schema.getName()+"."+ getName()+" ("+objectType+")";
		} else {
			if (objectType != null) {
				return  getName()+" ("+objectType+")";
			} else {
				return  getName();
			}
		}
	}
	
	/**
	 * Since SQLTable is just a container for Folders, there is no special populate
     * step.  The various populate operations (columns, keys, indices) are triggered
     * by visiting the individual folders.
	 */
	protected void populateImpl() throws SQLObjectException {
		if (populated) return;
		
		try {
		    runInForeground(new Runnable() {
                public void run() {
                    begin("Populating");
                }
            });
			populateColumns();
			populateIndices();
			populateRelationships();
			if (columnsPopulated && indicesPopulated && exportedKeysPopulated) {
			    populated = true;
			}
			runInForeground(new Runnable() {
                public void run() {
                    commit();
                }
            });
		} catch (final SQLObjectException e) {
		    runInForeground(new Runnable() {
                public void run() {
                    rollback(e.getMessage());
                    logger.error("Sketchy transaction rollback");
                }
            });
			throw e;
		} catch (final RuntimeException e) {
		    runInForeground(new Runnable() {
                public void run() {
                    rollback(e.getMessage());
                }
            });
		    throw e;
		}
	}

	@Accessor
	public boolean isPopulated() {
		if (!populated && isColumnsPopulated() && isImportedKeysPopulated() && isExportedKeysPopulated() && isIndicesPopulated()) {
			populated = true;
		}
		return populated;
	}

	@NonBound
	public Class<? extends SQLObject> getChildType() {
		return SQLObject.class;
	}

	// ------------------ Accessors and mutators below this line ------------------------

	/**
	 * Walks up the SQLObject containment hierarchy and returns the
	 * first SQLDatabase object encountered.  If this SQLTable has no
	 * SQLDatabase ancestors, the return value is null.
	 *
	 * @return the value of parentDatabase
	 */
	@Transient @Accessor
	public SQLDatabase getParentDatabase()  {
		return SQLPowerUtils.getAncestor(this, SQLDatabase.class);
	}
	
	@Override
	@Accessor
	public SQLObject getParent() {
		return (SQLObject) super.getParent();
	}

	/**
	 * Because we constrained the return type on getParent there needs to be a
	 * setter that has the same constraint otherwise the reflection in the undo
	 * events will not find a setter to match the getter and won't be able to
	 * undo parent property changes.
	 */
	@Mutator
	public void setParent(SQLObject parent) {
		super.setParent(parent);
	}

	/**
	 * @return An empty string if the catalog for this table is null;
	 * otherwise, getCatalog().getCatalogName().
	 */
	@Transient @Accessor
	public String getCatalogName() {
		SQLCatalog catalog = getCatalog();
		if (catalog == null) {
			return "";
		} else {
			return catalog.getName();
		}
	}

	@Transient @Accessor
	public SQLCatalog getCatalog()  {
		return SQLPowerUtils.getAncestor(this, SQLCatalog.class);
	}

	/**
	 * @return An empty string if the schema for this table is null;
	 * otherwise, schema.getSchemaName().
	 */
	@Transient @Accessor
	public String getSchemaName() {
		SQLSchema schema = getSchema();
		if (schema == null) {
			return "";
		} else {
			return schema.getName();
		}
	}

	@Transient @Accessor
	public SQLSchema getSchema()  {
		return SQLPowerUtils.getAncestor(this, SQLSchema.class);
	}

	/**
	 * Sets the table name, and also modifies the primary key name if
	 * it was previously null or set to the default of
	 * "oldTableName_pk".  Additionally, if any of this table's columns'
     * sequence names have been explicitly set, the old table name within
     * those sequence names will be replaced by the new table name.
	 *
	 * @param argName The new table name.  NULL is not allowed.
	 */
	@Mutator
	public void setPhysicalName(String argName) {

        logger.debug("About to change table name from \""+getPhysicalName()+"\" to \""+argName+"\"");
        
        // this method can be called very early in a SQLTable's life,
        // before its folders exist.  Therefore, we have to
        // be careful not to look up the primary key before one exists.

        if ( (!isMagicEnabled()) || (indices == null) || (columns == null) ) {
            super.setPhysicalName(argName);
        } else try {
        	String oldName;
        	if (getPhysicalName() != null) {
        		oldName = getPhysicalName();
        	} else {
        		oldName = getName();
        	}
        	
            begin("Table Name Change");
            super.setPhysicalName(argName);
            
            updatePKIndexNameToMatch(oldName, argName);
            
            if (isColumnsPopulated()) {
                for (SQLColumn col : getColumns()) {
                    if (col.isAutoIncrementSequenceNameSet()) {
                    	String testingName = col.discoverSequenceNameFormat(oldName, col.getPhysicalName());

                    	if (testingName.equals(col.getAutoIncrementSequenceName())) {
                    		col.setAutoIncrementSequenceName(
                    				col.makeAutoIncrementSequenceName());
                    	}
                    }
                }
            }
            
        } catch (SQLObjectException e) {
            throw new SQLObjectRuntimeException(e);
        } finally {
            commit();
        }
	}
	
	@Mutator
	@Override
	public void setName(String name) {
	    try {
	        begin("Setting name and possibly physical or primary key name.");
	        if (isMagicEnabled()) {
	            updatePhysicalNameToMatch(getName(), name);
	        }
	        super.setName(name);
	        commit();
	    } catch (Throwable t) {
	        rollback(t.getMessage());
	        throw new RuntimeException(t);
	    }
	}
	
	/**
	 * Simple helper that updates the PK Index name to match this table's name
	 * if the user hasn't changed it.
	 */
	private void updatePKIndexNameToMatch(String oldName, String newName) {
        if (newName != null &&
        		primaryKeyIndex != null && 
        		(primaryKeyIndex.getName() == null
        				|| "".equals(primaryKeyIndex.getName().trim())
        				|| (oldName + "_pk").equals(primaryKeyIndex.getName())) ) {
        	// if the physical name is still null when forward engineer,
        	// the DDLGenerator will generate the physical name from the 
        	// logic name and this index will be updated.
        	primaryKeyIndex.setName(newName + "_pk");

        }
	}
	
	/**
	 * Gets the value of remarks
	 *
	 * @return the value of remarks
	 */
	@Accessor(isInteresting=true)
	public String getRemarks()  {
		return this.remarks;
	}

	/**
	 * Sets the value of remarks
	 *
	 * @param argRemarks Value to assign to this.remarks
	 */
	@Mutator
	public void setRemarks(String argRemarks) {
		String oldRemarks = this.remarks;
		this.remarks = argRemarks;
		firePropertyChange("remarks",oldRemarks,argRemarks);
	}

	/**
	 * Gets the value of columns after populating. This will allow access to the
	 * most up to date list of columns.
	 * 
	 * @return the value of columns
	 */
	@NonProperty
	public List<SQLColumn> getColumns() throws SQLObjectException {
		// Must synchronize on class before instance. See populateAllColumns
		synchronized(getClass()) {
			synchronized(this) {
				populateColumns();
				return getColumnsWithoutPopulating();
			}
		}
	}

	/**
	 * Returns the list of columns without populating first. This will allow
	 * access to the columns at current.
	 */
	@NonProperty
	public synchronized List<SQLColumn> getColumnsWithoutPopulating() {
		return Collections.unmodifiableList(columns);
	}

	/**
	 * Gets the value of importedKeys after populating. This will allow access
	 * to the most up to date list of imported keys.
	 * 
	 * @return the value of importedKeys
	 * @throws SQLObjectException
	 */
	@NonProperty
	public List<SQLImportedKey> getImportedKeys() throws SQLObjectException {
		populateImportedKeys();
		return getImportedKeysWithoutPopulating();
	}

	/**
	 * Returns the list of imported keys without populating first. This will
	 * allow access to the imported keys at current.
	 */
	@NonProperty
	public List<SQLImportedKey> getImportedKeysWithoutPopulating() {
		return Collections.unmodifiableList(importedKeys);
	}
	
	/**
	 * Gets the value of exportedKeys after populating. This will allow access
	 * to the most updated list of exported keys.
	 * 
	 * @return the value of exportedKeys
	 * @throws SQLObjectException
	 */
	@NonProperty
	public List<SQLRelationship> getExportedKeys() throws SQLObjectException {
		populateExportedKeys();
		return getExportedKeysWithoutPopulating();
	}

	/**
	 * Returns the list of exported keys without populating first. This will
	 * allow access to the exported keys at current.
	 */
	@NonProperty
	public List<SQLRelationship> getExportedKeysWithoutPopulating() {
		return Collections.unmodifiableList(exportedKeys);
	}

    /**
     * Gets the value of exportedKeys by name
     *
     * @return the value of exportedKeys
     */
	@NonProperty
    public SQLRelationship getExportedKeyByName(String name) throws SQLObjectException {
        return getExportedKeyByName(name,true);
    }

    /**
     * Gets the value of exportedKeys by name
     *
     * @return the value of exportedKeys
     */
	@NonProperty
    public SQLRelationship getExportedKeyByName(String name,
            boolean populate ) throws SQLObjectException {
        if (populate) populateRelationships();
        logger.debug("Looking for Exported Key ["+name+"] in "+exportedKeys );
        for (SQLRelationship r : exportedKeys) {
        	if (r.getName().equalsIgnoreCase(name)) {
        		logger.debug("FOUND");
        		return r;
        	}
        }
        logger.debug("NOT FOUND");
        return null;
    }

    /**
     * Gets a list of unique indices
     */
	@NonProperty
    public List<SQLIndex> getUniqueIndices() throws SQLObjectException {
		// Must synchronize on class before instance. See populateAllColumns
		synchronized(getClass()) {
			synchronized(this) {
				populateColumns();
				populateIndices();
				List<SQLIndex> list = new ArrayList<SQLIndex>();
				list.add(primaryKeyIndex);
				for (SQLIndex index : indices) {
					if (index.isUnique()) {
						list.add(index);
					}
				}
				return list;
			}
		}
    }

    /**
     * Gets the value of index by name
     *
     * @return the value of index
     */
	@NonProperty
    public SQLIndex getIndexByName(String name) throws SQLObjectException {
        return getIndexByName(name,true);
    }

    /**
     * Gets the value of index by name
     *
     * @return the value of index
     */
	@NonProperty
    public SQLIndex getIndexByName(String name,
            boolean populate ) throws SQLObjectException {
        if (populate) {
            populateColumns();
            populateIndices();
        }
        logger.debug("Looking for Index ["+name+"] in "+indices);
        if (primaryKeyIndex.getName().equalsIgnoreCase(name)) {
        	logger.debug("Found primary key");
        	return primaryKeyIndex;
        }
        for (SQLIndex index : indices) {
        	if (index.getName().equalsIgnoreCase(name)) {
        		logger.debug("FOUND");
        		return index;
        	}
        }
        logger.debug("NOT FOUND");
        return null;
    }

	/**
     * Returns true if this table's columns folder says it's populated.
	 */
    @Accessor
	public boolean isColumnsPopulated()  {
		return columnsPopulated;
	}

	/**
	 * Returns true if this table's imported keys and exported
     * keys both say are populated.
	 */
    @Transient @Accessor
	public boolean isRelationshipsPopulated()  {
		return importedKeysPopulated && exportedKeysPopulated;
	}
	
	/**
	 * Returns true if this table's imported keys have been populated.
	 */
    @Accessor
	public boolean isImportedKeysPopulated() {
		return importedKeysPopulated;
	}
	
	/**
	 * Returns true if this table's exported keys have been populated. 
	 */
    @Accessor
	public boolean isExportedKeysPopulated() {
		return exportedKeysPopulated;
	}

    /**
     * Returns true if this table's indices folder says it's populated.
     */
    @Accessor
    public boolean isIndicesPopulated()  {
        return indicesPopulated;
    }
    
    @Mutator
    public void setColumnsPopulated(boolean columnsPopulated) {
    	boolean oldPop = this.columnsPopulated;
		this.columnsPopulated = columnsPopulated;
		firePropertyChange("columnsPopulated", oldPop, columnsPopulated);
		if (!columnsPopulated) {
		    setUnpopulatedIfPartiallyPopulated();
		}
	}
    
    @Mutator
    public void setImportedKeysPopulated(boolean importedKeysPopulated) {
    	boolean oldPop = this.importedKeysPopulated;
		this.importedKeysPopulated = importedKeysPopulated;
		firePropertyChange("importedKeysPopulated", oldPop, importedKeysPopulated);
		if (!columnsPopulated) {
            setUnpopulatedIfPartiallyPopulated();
        }
	}
    
    @Mutator
    public void setExportedKeysPopulated(boolean exportedKeysPopulated) {
    	boolean oldPop = this.exportedKeysPopulated;
		this.exportedKeysPopulated = exportedKeysPopulated;
		firePropertyChange("exportedKeysPopulated", oldPop, exportedKeysPopulated);
		if (!columnsPopulated) {
            setUnpopulatedIfPartiallyPopulated();
        }
	}
    
    @Mutator
    public void setIndicesPopulated(boolean indicesPopulated) {
    	boolean oldPop = this.indicesPopulated;
		this.indicesPopulated = indicesPopulated;
		firePropertyChange("indicesPopulated", oldPop, indicesPopulated);
		if (!columnsPopulated) {
            setUnpopulatedIfPartiallyPopulated();
        }
	}

    /**
     * Helper method for setting one of the populated child types to be
     * unpopulated. This will set the top level "populated" value to false if it
     * was previously true.
     * <p>
     * XXX This may not be necessary if we can get rid of the top level
     * populated flag and just keep a set of populated flags, one for each child
     * type.
     */
    private void setUnpopulatedIfPartiallyPopulated() {
        if (populated) {
            populated = false;
            firePropertyChange("populated", true, false);
        }
    }
    
	/**
	 * Gets the type of table this object represents (TABLE or VIEW).
	 *
	 * @return the value of objectType
	 */
    @Accessor
	public String getObjectType()  {
		return this.objectType;
	}

	/**
	 * Sets the type of table this object represents (TABLE or VIEW).
	 *
	 * @param argObjectType Value to assign to this.objectType
	 */
    @Mutator
	public void setObjectType(String argObjectType) {
		String oldObjectType = this.objectType;
		this.objectType = argObjectType;
        if (this.objectType == null) throw new NullPointerException();
		firePropertyChange("objectType",oldObjectType, argObjectType);
	}

	/**
	 * Returns the primary key for this table. This will always be the same
	 * index and never be null, though the index could have no columns
	 * associated with it.
	 */
    @Transient @Accessor
    public SQLIndex getPrimaryKeyIndex() {
    	return primaryKeyIndex;
    }

    /**
     * Retrieves all of the table names for the given catalog, schema
     * in the container's database using DatabaseMetaData.  This method
     * is a subroutine of the populate() methods in SQLDatabase, SQLCatalog,
     * and SQLSchema.
     * <p>
     * Important Note: This method adds the tables directly to the parent's
     * children list.  No SQLObjectEvents will be generated.  Calling code
     * has to do this at the appropriate time, when it's safe to do so. 
     * 
     * @param container The container that will be the direct parent of
     * all tables created by this method call.
     * @param dbmd The DatabaseMetaData for the parent database in question.
     * The fact that you have to pass it in is just an optimization: all
     * the places from which this method gets called already have an instance
     * of DatabaseMetaData ready to go.
     */
    static List<SQLTable> fetchTablesForTableContainer(DatabaseMetaData dbmd, String catalogName, String schemaName)
    throws SQLObjectException, SQLException {
        ResultSet rs = null;
        try {
            rs = dbmd.getTables(catalogName,
                    schemaName,
                    "%",
                    new String[] {"TABLE", "VIEW"});

            List<SQLTable> tables = new ArrayList<SQLTable>();
            while (rs.next()) {
                tables.add(new SQLTable(null,
                        rs.getString(3),
                        rs.getString(5),
                        rs.getString(4),
                        false));
            }
            
            return tables;
            
        } finally {
            if (rs != null) rs.close();
        }
    }

    /**
     * Returns an unmodifiable list of all the indices of this table, in the
     * same order they appear in the indices folder. If this table has no indices,
     * the returned list will be empty (never null).
     * 
     * @throws SQLObjectException If there is a problem populating the indices folder
     */
    @NonProperty
    public List<SQLIndex> getIndices() throws SQLObjectException {
    	populateColumns();
    	populateIndices();
    	List<SQLIndex> allIndices = new ArrayList<SQLIndex>();
    	allIndices.add(primaryKeyIndex);
    	allIndices.addAll(indices);
        return Collections.unmodifiableList(allIndices);
    }
    
    public String toQualifiedName() {
        return SQLObjectUtils.toQualifiedName(this, SQLDatabase.class);
    }
    
    public String toQualifiedName(String quote) {
        return SQLObjectUtils.toQualifiedName(this, SQLDatabase.class, quote);
    }

    /**
     * Refreshing tables only works if it is done for the whole
     * "table container" at once, since all the columns have to be refreshed
     * before any of the FK relationships. As such, this method just throws
     * an exception. The generic refresh in SQLObject knows about this requirement,
     * and does the right thing when it encounters a table container.
     */
    @Override
    void refresh() throws SQLObjectException {
        // XXX think about automatically forwarding this request to parent.refresh(),
        // since parent ought to be the table container we're looking for...
        throw new UnsupportedOperationException("Individual tables can't be refreshed.");
    }

    public boolean removeIndex(SQLIndex sqlIndex) {
    	if (isMagicEnabled() && sqlIndex.getParent() != this) {
			throw new IllegalStateException("Cannot remove child " + sqlIndex.getName() + 
					" of type " + sqlIndex.getClass() + " as its parent is not " + getName());
    	}
    	int index = indices.indexOf(sqlIndex);
    	if (index != -1) {
    		try {
    			begin("Removing index " + sqlIndex.getName());
    			indices.remove(index);
    			//Primary key is the first index in the first position.
    			fireChildRemoved(SQLIndex.class, sqlIndex, index + 1);
    			sqlIndex.setParent(null);
    			commit();
    			return true;
    		} catch (Throwable t) {
    			rollback("Failed to remove the index.");
    			throw new RuntimeException(t);
    		}
    	}
    	return false;
    }
    
    @Override
    public <T extends SPObject> List<T> getChildren(Class<T> type) {
    	if (!isMagicEnabled()) {
    		return getChildrenWithoutPopulating(type);
    	}
        try {
        	if (type == SQLImportedKey.class) {
        		//doing nothing because we can now populate imported keys without
        		//needing columns.
        	} else if (type == SQLColumn.class) {
                populateColumns();
            } else if (type == SQLIndex.class) {
                populateColumns();
                populateIndices();
            } else {
                populate();
            }
            return getChildrenWithoutPopulating(type);
        } catch (SQLObjectException e) {
            throw new RuntimeException("Could not populate " + getName(), e);
        }
    }

	public List<? extends SQLObject> getChildrenWithoutPopulating() {
		List<SQLObject> children = new ArrayList<SQLObject>();
		children.addAll(columns);
		children.addAll(exportedKeys);
		children.addAll(importedKeys);
		children.add(primaryKeyIndex);
		children.addAll(indices);
		return Collections.unmodifiableList(children);
	}
	
	@Override
	protected boolean removeChildImpl(SPObject child) {
		if (child instanceof SQLColumn) {
            if (isMagicEnabled()) {
                SQLColumn col = (SQLColumn) child;
                // a column is only locked if it is an IMPORTed key--not if it is EXPORTed.
                for (SQLImportedKey k : importedKeys) {
                    k.getRelationship().checkColumnLocked(col);
                }
            }
            
            return removeColumn((SQLColumn) child);
            
		} else if (child instanceof SQLRelationship) {
			return removeExportedKey((SQLRelationship) child);
		} else if (child instanceof SQLImportedKey) {
			return removeImportedKey((SQLImportedKey) child);
		} else if (child instanceof SQLIndex) {
			return removeIndex((SQLIndex) child);
		}
		return false;
	}
	
	public List<? extends SPObject> getDependencies() {
		return Collections.emptyList();
	}

	public void removeDependency(SPObject dependency) {
		for (SQLObject child : getChildrenWithoutPopulating()) {
			child.removeDependency(dependency);
		}
	}

	/**
	 * Overridden so that when you set the populated flag, the populated flag
	 * for columns, imported keys, exported keys and indices are updated
	 * accordingly as well.
	 */
	@Override
	@Mutator
	public void setPopulated(boolean v) {
		boolean oldPop = populated;
		populated = v;
		columnsPopulated = v;
		importedKeysPopulated = v;
		exportedKeysPopulated = v;
		indicesPopulated = v;
		firePropertyChange("populated", oldPop, v);
	}

    void refreshExportedKeys() throws SQLObjectException {
        if (!exportedKeysPopulated) {
            logger.debug("Not refreshing unpopulated exported keys of " + this);
            return;
        }

        final List<SQLRelationship> newRels = SQLRelationship.fetchExportedKeys(this, null);
        if (logger.isDebugEnabled()) {
            logger.debug("New imported keys of " + getName() + ": " + newRels);
        }
        runInForeground(new Runnable() {
            public void run() {
                try {
                    SQLObjectUtils.refreshChildren(SQLTable.this, newRels, SQLRelationship.class);
                } catch (SQLObjectException e) {
                    throw new SQLObjectRuntimeException(e);
                }
            }
        });
    }

    void refreshIndexes() throws SQLObjectException {
        if (!isIndicesPopulated()) return;
        Connection con = null;
        try {
            con = getParentDatabase().getConnection();
            DatabaseMetaData dbmd = con.getMetaData();
            
            // indexes (incl. PK)
            final List<SQLIndex> newIndexes = SQLIndex.fetchIndicesForTableAndUpdatePK(dbmd, this);
            newIndexes.add(0, getPrimaryKeyIndex());
            runInForeground(new Runnable() {
                public void run() {
                    try {
                        SQLObjectUtils.refreshChildren(SQLTable.this, newIndexes, SQLIndex.class);
                    } catch (SQLObjectException e) {
                        throw new SQLObjectRuntimeException(e);
                    }
                }
            });
            
        } catch (SQLException e) {
            throw new SQLObjectException("Refresh failed", e);
        } finally {
            if (con != null) {
                try {
                    con.close();
                } catch (SQLException e) {
                    logger.error("Failed to close connection. Squishing this exception: ", e);
                }
            }
        }
    }

	public List<Class<? extends SPObject>> getAllowedChildTypes() {
		return allowedChildTypes;
	}

	/**
	 * Called when a table is being removed from its parent. This will be called
	 * before the table is actually removed from its parent. Package private as
	 * classes outside of the SQLObjects do not need to call this method.
	 */
	void removeNotify() {
		for (int i = exportedKeys.size() - 1; i >= 0; i--) {
			exportedKeys.get(i).tableDisconnected();
		}
		for (int i = importedKeys.size() - 1; i >= 0; i--) {
			importedKeys.get(i).getRelationship().tableDisconnected();
		}
	}

	/**
	 * This method will move the given column to the last position just after
	 * the primary key. This means that the column will be just below the
	 * primary key line and not in the primary key.
	 * 
	 * @param col
	 *            The column to move.
	 */
	public void moveAfterPK(SQLColumn col) throws SQLObjectException {
		int targetIndex;
		if (col.isPrimaryKey()) {
			targetIndex = getPkSize() - 1;
		} else {
			targetIndex = getPkSize();
		}
		int currentIndex = columns.indexOf(col);
		changeColumnIndex(currentIndex, targetIndex, false);
	}

	/**
	 * If the given column is not part of the primary key the column will be
	 * moved to the last position in the primary key and added to the primary
	 * key. If the column is already part of the primary key this method will do
	 * nothing.
	 */
	public void addToPK(SQLColumn col) throws SQLObjectException {
		if (!col.isPrimaryKey()) {
			moveAfterPK(col);
			primaryKeyIndex.addIndexColumn(col);
		}
	}

	/**
	 * Updates the column mappings of relationships for changes to the primary
	 * key index. This must be done on each column added to the primary key
	 * index.
	 * <p>
	 * Package private because only the SQLIndex need to be concerned about this.
	 */
	void updateRelationshipsForNewIndexColumn(SQLColumn col) {
		if (isMagicEnabled()) {
			for (SQLRelationship r : exportedKeys) {
				r.fixMappingNewChildInParent(col);
			}
		}
	}

	/**
	 * Updates the column mappings of relationships for each column that gets
	 * removed from the primary key index. Must be called once per column
	 * removal on the primary key index.
	 * <p>
	 * Package private because only the SQLIndex need to be concerned about this.
	 */
	void updateRelationshipsForRemovedIndexColumns(SQLColumn col) {
		if (isMagicEnabled()) {
			for (SQLRelationship r : exportedKeys) {
				r.fixMappingChildRemoved(col);
			}
		}
	}

	/**
	 * Helper method for finding if an index is the primary key without causing
	 * the table to populate.
	 * <p>
	 * Package private for use in SQLIndex. 
	 */
	boolean isPrimaryKey(SQLIndex index) {
		return index == primaryKeyIndex;
	}

	/**
	 * Helper method for finding if a column is in the primary key index without
	 * causing the table to populate.
	 * <p>
	 * Package private for use in SQLColumn.
	 * 
	 * @param col
	 * @return
	 */
	boolean isInPrimaryKey(SQLColumn col) {
		return primaryKeyIndex.containsColumn(col);
	}
	
	SQLIndex getPrimaryKeyIndexWithoutPopulating() {
		return primaryKeyIndex;
	}
}