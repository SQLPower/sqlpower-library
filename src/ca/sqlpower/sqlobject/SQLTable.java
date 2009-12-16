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
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import ca.sqlpower.object.ObjectDependentException;
import ca.sqlpower.object.SPObject;
import ca.sqlpower.sql.CachedRowSet;
import ca.sqlpower.sqlobject.SQLIndex.AscendDescend;
import ca.sqlpower.sqlobject.SQLIndex.Column;

public class SQLTable extends SQLObject {

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
	protected List<SQLRelationship> importedKeys = new ArrayList<SQLRelationship>();

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

	public SQLTable(SQLObject parent, String name, String remarks, String objectType, boolean startPopulated) throws SQLObjectException {
		logger.debug("NEW TABLE "+name+"@"+hashCode());
		initFolders(startPopulated);
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
	public void initFolders(boolean populated) throws SQLObjectException {
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
		populateRelationships();
        populateIndices();
		SQLTable t = new SQLTable(parent, true);
		t.setName(getName());
		t.remarks = remarks;
		t.setChildrenInaccessibleReason(getChildrenInaccessibleReason(), false);

		t.setPhysicalName(getPhysicalName());

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
     * inherit indices from the source table,
     * @param source
     * @param target    
     * @throws SQLObjectException 
     */
    private static void inheritIndices(SQLTable source, SQLTable target) throws SQLObjectException {
        for ( SQLIndex index : source.getIndices()) {
            SQLIndex index2 = SQLIndex.getDerivedInstance(index,target);
            target.addIndex(index2);
        }
    }

    /**
     * Populates the columns of this table from the database.  If successful, then the
     * indices will also be populated.
     * 
     * @throws SQLObjectException
     */
    protected synchronized void populateColumns() throws SQLObjectException {

		if (columnsPopulated) return;
    	if (columns.size() > 0) {
    		throw new IllegalStateException("Can't populate table because it already contains columns");
    	}

		logger.debug("column folder populate starting for table " + getName());

		Connection con = null;
		try {
		    con = getParentDatabase().getConnection();
		    DatabaseMetaData dbmd = con.getMetaData();
			List<SQLColumn> cols = SQLColumn.fetchColumnsForTable(
			        getCatalogName(), getSchemaName(), getName(), dbmd);
			begin("Populating columns for Table " + this);
			for (SQLColumn col : cols) {
			    addColumnWithoutPopulating(col);
			}
			commit();
		} catch (SQLException e) {
			rollback(e.getMessage());
			throw new SQLObjectException("Failed to populate columns of table "+getName(), e);
		} finally {
			columnsPopulated = true;
			if (con != null) {
			    try {
			        con.close();
			    } catch (SQLException ex) {
			        logger.error("Couldn't close connection. Squishing this exception: ", ex);
			    }
			}
		}

		logger.debug("column folder populate finished for table " + getName());

        populateIndices();
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
		} else if (!columnsPopulated) {
			throw new IllegalStateException("Columns must be populated");
		}
        
        // If the SQLTable is a view, simply indicated folder is populated and then leave
        // Since Views don't have indices (and Oracle throws an error)
        if (objectType.equals("VIEW")) { 
            indicesPopulated = true;
            return;
        }
        
        logger.debug("index folder populate starting");

        Connection con = null;
        try {
            con = getParentDatabase().getConnection();
            DatabaseMetaData dbmd = con.getMetaData();
            logger.debug("before addIndicesToTable");
            
            List<SQLIndex> indexes = SQLIndex.fetchIndicesForTable(dbmd, this);
            
            begin("Populating Indices for Table " + this);
            for (SQLIndex i : indexes) {
            	addIndex(i);
            }
            commit();
            logger.debug("found "+indices.size()+" indices.");
          
        } catch (SQLException e) {
        	rollback(e.getMessage());
            throw new SQLObjectException("Failed to populate indices of table "+getName(), e);
        } finally {
            indicesPopulated = true;

            try {
                if (con != null) con.close();
            } catch (SQLException e) {
                logger.error("Closing connection failed. Squishing this exception: ", e);
            }
            
            // FIXME this might change the order of columns without firing an event
            Collections.sort(columns, new SQLColumn.CompareByPKSeq());
            
        }
        normalizePrimaryKey();
        logger.debug("index folder populate finished");
    }
	
	protected synchronized void populateExportedKeys() throws SQLObjectException {
		if (exportedKeysPopulated) return;
		
		CachedRowSet crs = null;
		Connection con = null;
		try {
			con = getParentDatabase().getConnection();
			DatabaseMetaData dbmd = con.getMetaData();
			crs = new CachedRowSet();
			ResultSet exportedKeysRS = dbmd.getExportedKeys(getCatalogName(), getSchemaName(), getName());
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
				String cat = crs.getString(5);
				String sch = crs.getString(6);
				String tab = crs.getString(7);
				SQLTable fkTable = getParentDatabase().getTableByName(cat, sch, tab);
                if (fkTable == null) {
                    throw new IllegalStateException("While populating table " +
                            SQLObjectUtils.toQualifiedName(getParent()) +
                            ", I failed to find child table " +
                            "\""+cat+"\".\""+sch+"\".\""+tab+"\"");
                }
				fkTable.populateColumns();
				fkTable.populateRelationships(this);
			}
			exportedKeysPopulated = true;
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

	/**
	 * Populates all the imported key relationships, where it first ensures that
	 * columns have been populated.
	 * 
	 * @throws SQLObjectException
	 */
	protected synchronized void populateImportedKeys() throws SQLObjectException {
		populateColumns();
		populateRelationships();
	}

	/**
	 * Populates all the imported key relationships.  This has the
	 * side effect of populating the exported key side of the
	 * relationships for the exporting tables.
	 */
    protected synchronized void populateRelationships() throws SQLObjectException {
        populateRelationships(null);
    }

	/**
	 * Populates all the imported key relationships. This has the side effect of
	 * populating the exported key side of the relationships for the exporting
	 * tables.
	 * 
	 * @param pkTable
	 *            if not null only relationships that have this table as the
	 *            parent will be added. If this is null than all relationships
	 *            will be added regardless of the parent.
	 */
    protected synchronized void populateRelationships(SQLTable pkTable) throws SQLObjectException {
    	if (!columnsPopulated) {
    		throw new IllegalStateException("Table must be populated before relationships are added");
    	} else if (importedKeysPopulated) {
    		return;
    	}

		logger.debug("SQLTable: relationship populate starting");

		/* this must come before
		 * SQLRelationship.addImportedRelationshipsToTable because
		 * addImportedRelationshipsToTable causes SQLObjectEvents to be fired,
		 * which in turn could cause infinite recursion when listeners
		 * query the size of the relationships folder.
		 */
		importedKeysPopulated = true;
		boolean allRelationshipsAdded = false;
		try {
			List<SQLRelationship> newKeys = SQLRelationship.fetchImportedKeys(this);
			begin("Populating relationships for Table " + this);
			
			/* now attach the new SQLRelationship objects to their tables,
             * which may already be partially populated (we avoid adding the
			 * same relationship if it's already there). We also filter by
			 * pkTable if that was requested.
			 */
			List<SQLRelationship> addedRels = new ArrayList<SQLRelationship>();
            for (SQLRelationship addMe : newKeys) {
                if (pkTable != null && addMe.pkTable != pkTable) {
                    allRelationshipsAdded = false;
                    continue;
                }
                if (!addMe.pkTable.getExportedKeysWithoutPopulating().contains(addMe)) {
                    addMe.attachRelationship(addMe.pkTable, addMe.fkTable, false, false);
                    addedRels.add(addMe);
                }
            }

			// XXX Review above code, and make sure everything is firing
			// events where it should be. The above block is surrounded
			// within a transaction, so all events should be fired
			// there.
            commit();
            
            // This call attaches listeners to this table. It must be called
			// outside of all transactions, otherwise the listener would start
			// with the wrong transaction count, and throw an exception on
			// commit()
            for (SQLRelationship added : addedRels) {
            	added.attachListeners();
            }
		} catch (SQLObjectException e) {
			rollback(e.getMessage());
			throw e;
		} finally {
			//The imported keys folder will only be guaranteed to be completely populated if
			//any table can be the pkTable not a specific table.
			if (pkTable != null && !allRelationshipsAdded) {
				importedKeysPopulated = false;
			}
		}

		logger.debug("SQLTable: relationship populate finished");

	}

	public void addImportedKey(SQLRelationship r) {
		addImportedKey(r, importedKeys.size());
	}
	
	public void addImportedKey(SQLRelationship r, int index) {
		importedKeys.add(index, r);
		// Does not set parent because this should be done by SQLRelationship.attachRelationship()
		fireChildAdded(SQLRelationship.class, r, index);
	}

	public boolean removeImportedKey(SQLRelationship r) {
		if (r.getFkTable() != this) {
			throw new IllegalStateException("Cannot remove child " + r.getName() + 
					" of type " + r.getClass() + " as its parent is not " + getName());
		}
		int index = importedKeys.indexOf(r);
		if (index != -1) {
			 importedKeys.remove(index);
			 r.setParent(null);
			 fireChildRemoved(SQLRelationship.class, r, index);
			 return true;
		}
		return false;
	}

	public void addExportedKey(SQLRelationship r) {
		addExportedKey(r, exportedKeys.size());
	}
	
	public void addExportedKey(SQLRelationship r, int index) {
		exportedKeys.add(index, r);
		// Does not set parent because this should be done by SQLRelationship.attachRelationship()
		fireChildAdded(SQLRelationship.class, r, index);
	}

	public boolean removeExportedKey(SQLRelationship r) {
		if (r.getParent() != this) {
			throw new IllegalStateException("Cannot remove child " + r.getName() + 
					" of type " + r.getClass() + " as its parent is not " + getName());
		}
		int index = exportedKeys.indexOf(r);
		if (index != -1) {
			 exportedKeys.remove(index);
			 r.setParent(null);
			 fireChildRemoved(SQLRelationship.class, r, index);
			 return true;
		}
		return false;
	}

	/**
	 * Counts the number of columns in the primary key of this table.
	 */
	public int getPkSize() {
		int size = 0;
		for (SQLColumn c : columns) {
			if (c.getPrimaryKeySeq() != null) {
				size++;
			} else {
				break;
			}
		}
		return size;
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
	public void inherit(int pos, SQLTable source, TransferStyles transferStyle, boolean preserveColumnSource) throws SQLObjectException {
		SQLColumn c;
		if (source == this)
		{
			throw new SQLObjectException("Cannot inherit from self");
		}

		boolean addToPK;
		int pkSize = getPkSize();
		int sourceSize = source.getColumns().size();
		int originalSize = getColumns().size();
		if (originalSize == 0 || pos < pkSize) {
			addToPK = true;
			for (int i = pos; i < pkSize; i++) {
				columns.get(i).primaryKeySeq = new Integer(i + sourceSize);
			}
		} else {
			addToPK = false;
		}

		begin("Inherting columns from source table");
		for (SQLColumn child : source.getColumns()) {
			switch (transferStyle) {
			case REVERSE_ENGINEER:
				c = child.createInheritingInstance(this);
				break;
			case COPY:
				c = child.createCopy(this, preserveColumnSource);
				break;
			default:
				throw new IllegalStateException("Unknown transfer type of " + transferStyle);
			}
			if (originalSize > 0) {
				if (addToPK) {
					c.primaryKeySeq = new Integer(pos);
				} else {
					c.primaryKeySeq = null;
				}
			}
			addColumn(c, pos, false);
			pos++;
		}
		commit();
	}

	public void inherit(int pos, SQLColumn sourceCol, boolean addToPK, TransferStyles transferStyle, boolean preserveColumnSource) throws SQLObjectException {
	    if (addToPK && pos > 0 && !getColumn(pos - 1).isPrimaryKey()) {
	        throw new IllegalArgumentException("Can't inherit new PK column below a non-PK column! Insert pos="+pos+"; addToPk="+addToPK);
	    }
		SQLColumn c = sourceCol.createInheritingInstance(this);
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
		if (addToPK) {
			c.primaryKeySeq = new Integer(1);
		} else {
			c.primaryKeySeq = null;
		}
		addChild(c, pos);
	}

	public SQLColumn getColumn(int index) throws SQLObjectException {
		populateColumns();
		return columns.get(index);
	}

	/**
	 * Populates this table then searches for the named column in a case-insensitive
     * manner.
	 */
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
	public SQLColumn getColumnByName(String colName, boolean populate, boolean caseSensitive) 
	    throws SQLObjectException {

		if (populate) populateColumns();
		/* if columnsFolder.children.iterator(); gets changed to getColumns().iterator()
		 * we get infinite recursion between populateColumns, getColumns,
		 * getColumnsByName and addColumnsToTable
		 */
		if (logger.isDebugEnabled()) {
		    logger.debug("Looking for column "+colName+" in "+columns);
		    logger.debug("Table " + getName() + " has " + columns.size() + " columns");
		}
		for (SQLColumn col : columns) {
			logger.debug("Current column name is '" + col.getName() + "'");
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

	public int getColumnIndex(SQLColumn col) throws SQLObjectException {
		logger.debug("Looking for column index of: " + col);

		List<SQLColumn> columns = getColumns();
		int index = columns.indexOf(col);
		
		if (index == -1) {
			logger.debug("NOT FOUND");
		}
		
		return index;
	}

	public void addColumn(SQLColumn col) throws SQLObjectException {
		addColumn(col, columns.size());
	}

	/**
	 * Adds a column to the given position in the table. If magic is enabled the
	 * primary key sequence of the column may be updated depending on the position
	 * of the column and the number of primary keys. 
	 */
	public void addColumn(SQLColumn col, int pos) throws SQLObjectException {
		addColumn(col, pos, true);
	}

	/**
	 * Adds a column to the given position in the table. If magic is enabled and
	 * correctPK is true the primary key sequence of the column may be updated
	 * depending on the position of the column and the number of primary keys.
	 * 
	 * @param col
	 *            The column to add.
	 * @param pos
	 *            The position to add the column to.
	 * @param correctPK
	 *            If true and magic is enabled the column will be placed in the
	 *            primary key if it is above at least one primary key column in
	 *            terms of position in the table. If false the primary key
	 *            sequence of the column will be left as is and normalize will
	 *            need to be called on the table to correct the sequence of
	 *            columns in the primary key.
	 * @throws SQLObjectException
	 */
	public void addColumn(SQLColumn col, int pos, boolean correctPK) throws SQLObjectException {
		if (getColumnIndex(col) != -1) {
			col.addReference();
			return;
		}

		if (isMagicEnabled() && correctPK) {
		    boolean addToPK = false;
		    int pkSize = getPkSize();
		    if (getColumns().size() > 0 && pos < pkSize) {
		        addToPK = true;
		        for (int i = pos; i < pkSize; i++) {
		        	SQLColumn changingPKCol = getColumns().get(i);
		        	try {
		        		changingPKCol.setMagicEnabled(false);
		        		changingPKCol.setPrimaryKeySeq(new Integer(i + 1), false);
		        	} finally {
		        		changingPKCol.setMagicEnabled(true);
		        	}
		        }
		    }

		    col.setParent(null);
		    if (addToPK) {
		        col.nullable = DatabaseMetaData.columnNoNulls;
		        try {
		        	col.setMagicEnabled(false);
		        	col.setPrimaryKeySeq(new Integer(pos), false);
		        } finally {
		        	col.setMagicEnabled(true);
		        }
		    } else {
		    	try {
		    		col.setMagicEnabled(false);
		    		col.setPrimaryKeySeq(null, false);
		    	} finally {
		    		col.setMagicEnabled(true);
		    	}
		    }
		}
		columns.add(pos, col);
		col.setParent(this);
		fireChildAdded(SQLColumn.class, col, pos);
	}
	
	/**
	 * Adds a {@link SQLColumn} to the end of the child list without populating first.
	 */
	public void addColumnWithoutPopulating(SQLColumn col) {
		addColumnWithoutPopulating(col, columns.size());
	}
	
	/**
	 * Adds a {@link SQLColumn} at a given index of the child list without populating first.
	 */
	public void addColumnWithoutPopulating(SQLColumn col, int index) {
		columns.add(index, col);
		col.setParent(this);
		fireChildAdded(SQLColumn.class, col, index);
	}

	/**
     * Adds the given SQLIndex object to this table's index folder.
	 */
    public void addIndex(SQLIndex sqlIndex) {
        if (sqlIndex.isPrimaryKeyIndex()) {
        	addIndex(sqlIndex, 0);
        } else {
        	addIndex(sqlIndex, indices.size());
        }
    }
    
    public void addIndex(SQLIndex sqlIndex, int index) {
    	indices.add(index, sqlIndex);
    	sqlIndex.setParent(this);
    	fireChildAdded(SQLIndex.class, sqlIndex, index);
    }

    /**
     * Simply tries to add the child to this object without doing any side effects like
     * correcting index sequences.
     */
    @Override
    protected void addChildImpl(SPObject child, int index) {
		if (child instanceof SQLColumn) {
			addColumnWithoutPopulating((SQLColumn) child, index);
		} else if (child instanceof SQLRelationship) {
			throw new IllegalArgumentException("Cannot add SQLRelationship " + child.getName() + 
					" to table " + getName() + " as it should be called from addImportedKey" +
							" or addExportedKey instead of addChild.");
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
		if (col.getParent() != this) {
			throw new IllegalStateException("Cannot remove child " + col.getName() + 
					" of type " + col.getClass() + " as its parent is not " + getName());
		}
		int index = columns.indexOf(col);
		if (index != -1) {
			columns.remove(index);
			fireChildRemoved(SQLColumn.class, col, index);
			col.setParent(null);
			return true;
		}
		return false;
	}
	

	/**
	 * Moves the column at index <code>oldIdx</code> to index
	 * <code>newIdx</code>.  This may cause the moved column to become
	 * part of the primary key (or to be removed from the primary
	 * key).
	 *
	 * @param oldIdx the present index of the column.
	 * @param newIdx the index that the column will have when this
	 * method returns.
	 */
	public void changeColumnIndex(int oldIdx, int newIdx, boolean putInPK) throws SQLObjectException {
		// remove and add the column directly, then manually fire the event.
	    // This is necessary because the relationships prevent deletion of locked keys.
        try {
            begin("Changing column index");
            SQLColumn col = columns.get(oldIdx);
            Integer interimPkSeq;
            if (putInPK) {
                interimPkSeq = new Integer(1); // will get sane value when normalized
                col.setNullable(DatabaseMetaData.columnNoNulls);
            } else {
                interimPkSeq = null;
            }
            
            col.setPrimaryKeySeq(interimPkSeq, false, false);

            // If the indices are the same, then there's no point in moving the column
            if (oldIdx != newIdx) {
            	removeColumn(col);
            	addColumn(col, newIdx, false);
            }

            normalizePrimaryKey();
        } finally {
        	commit();
        }
	}

    private boolean normalizing = false;
    private boolean normalizeAgain = false;
	/**
     * Renumbers the PrimaryKeySeq values of all columns in this table, then
     * rebuilds this table's Primary Key Index so it correctly reflects the
     * new PrimaryKeySeq column properties.
     * <p>
     * The process happens in three phases:
     * <ol>
     *  <li>Columns are assigned PrimaryKeySeq values starting with 0 and
     *      increasing by 1 with each successive column. This phase ends when a
     *      column with a null PrimaryKeySeq is encountered (of course, this could be
     *      the very first column).
     *  <li>The remaining columns all have their PrimaryKeySeq set to null.
     *  <li>This table's PrimaryKeyIndex is rebuilt from scratch, created if
     *      it did not previously exist, or deleted if there are no primary
     *      key columns left after phases 1 and 2. The Primary Key index will
     *      agree with the new set of columns that have a non-null PrimaryKeySeq.
     * </ol>
     * <p>
     * Assumptions:
     * <ul>
     *  <li>The correct primary key information is the current column order,
     *      <i>not</i> the contents of this table's PrimaryKey Index object.
     *  <li>All columns that you want to keep in the primary key are already
     *      contiguous starting from column 0, and they all have non-null
     *      PrimaryKeySeq values. The actual numeric PrimaryKeySeq value is not
     *      important.
     *  <li>The first column that should not be in the Primary Key has a null
     *      PrimaryKeySeq value.  For all successive columns, the nullity of
     *      PrimaryKeySeq is not important. 
     * </ul>
     */
	public void normalizePrimaryKey() throws SQLObjectException {
        if (normalizing) {
            logger.debug("Already normalizing! Original normalize should make a second pass", new Exception("Stack trace!"));
//            normalizeAgain = true;
            return;
//            throw new RuntimeException("stack trace!");
        }
		try {
            normalizing = true;
            
            begin("Normalizing Primary Key");

            do {
                normalizeAgain = false;

                // Phase 1 and 2 (see doc comment)
                int i = 0;  //Primary key count

                // iterating over a copy of the column list because new columns can
                // be added or removed from the table as a side effect of the
                // primaryKeySeq change events. The effect of iterating over
                // the copy is that new columns will be ignored in this normalize
                // effort, and removed columns will be treated as if they were
                // still in the table.
                for (SQLColumn col : new ArrayList<SQLColumn>(getColumns())) {
                    logger.debug("*** normalize " + getName() + " phase 1/2: " + col);
                    if (col.getPrimaryKeySeq() != null) {
                        Integer newPkSeq = new Integer(i);
                        col.setPrimaryKeySeq(newPkSeq, false, false);
                        i++;
                    }
                }

                // Phase 3 (see doc comment)

                if (getPrimaryKeyIndex() == null) {
                    SQLIndex pkIndex = new SQLIndex(getPhysicalName()+"_pk", true, null, null ,null);
                    pkIndex.setPrimaryKeyIndex(true);
                    addIndex(pkIndex);
                    logger.debug("new pkIndex.getChildCount()="+pkIndex.getChildCount());
                }

                SQLIndex pkIndex = getPrimaryKeyIndex();
                Map<SQLColumn, Column> oldColumnInstances = new HashMap<SQLColumn, Column>();
                for (int j = pkIndex.getChildCount()-1; j >= 0; j--) {
                    Column child = (Column) pkIndex.getChild(j);
                    pkIndex.removeChild(child);
                    if (child.getColumn() == null) {
                        throw new IllegalStateException(
                                "Found a functional index column in PK." +
                                " PK Name: " + pkIndex.getName() +
                                ", Index Column name: " + child.getName());
                    }
                    oldColumnInstances.put(child.getColumn(),child);
                }

                assert pkIndex.getChildCount() == 0;

                for (SQLColumn col : getColumns()) {
                    if (col.getPrimaryKeySeq() == null) break;
                    if (oldColumnInstances.get(col) != null) {
                        pkIndex.addChild(oldColumnInstances.get(col));
                    } else {
                        pkIndex.addIndexColumn(col,AscendDescend.UNSPECIFIED);
                    }
                }
                if (pkIndex.getChildCount() == 0) {
                	removeIndex(pkIndex);
                }
            } while (normalizeAgain);
            commit();
		} catch (IllegalArgumentException e) {
			rollback("Could not remove child: " + e.getMessage());
			throw new RuntimeException(e);
		} catch (ObjectDependentException e) {
			rollback("Could not remove child: " + e.getMessage());
			throw new RuntimeException(e);
		} finally {
            normalizing = false;
            normalizeAgain = false;
		}
        if (logger.isDebugEnabled()) {
            logger.debug("----Normalize Results for table " + getName() + "----");
            for (SQLColumn col : getColumns()) {
                logger.debug("Column Name " + col.getName() + " Key Sequence " +col.getPrimaryKeySeq() );
            }
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

	//TODO XXX Sql object should be doing this when we add generics
	protected void setParent(SQLObject newParent) {
		logger.debug("Setting " + getName() + "'s parent to "+ newParent);
		if (getParent() == newParent) return;
		SQLObject oldVal = getParent();
		super.setParent(newParent);
		firePropertyChange("parent", oldVal, getParent());
	}



	/**
	 * The table's name.
	 */
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
			begin("Populating Children of Table " + this);
			populateColumns();
			populateRelationships();
			populateIndices();
			commit();
		} catch (SQLObjectException e) {
			rollback(e.getMessage());
			throw e;
		} finally {
			populated = true;
		}
	}

	public boolean isPopulated() {
		if (!populated && isColumnsPopulated() && isImportedKeysPopulated() && isExportedKeysPopulated() && isIndicesPopulated()) {
			populated = true;
		}
		return populated;
	}

	/**
	 * Returns true (tables have several folders as children).
	 */
	public boolean allowsChildren() {
		return true;
	}

	public Class<? extends SQLObject> getChildType() {
		return SQLObject.class;
	}

	/**
	 * The Folder class is a SQLObject that holds a SQLTable's child
	 * folders (columns and relationships).
	 */
//	public static class Folder<T extends SQLObject> extends SQLObject {
//		protected int type;
//		protected String name;
//		protected SQLTable parent;
//
//		public static final int COLUMNS = 1;
//		public static final int IMPORTED_KEYS = 2;
//        public static final int EXPORTED_KEYS = 3;
//        public static final int INDICES = 4;
//
//		public Folder(int type, boolean populated) {
//			this.populated = populated;
//			this.type = type;
//			this.children = new ArrayList<T>();
//			if (type == COLUMNS) {
//				name = "Columns";
//			} else if (type == IMPORTED_KEYS) {
//				name = "Imported Keys";
//            } else if (type == EXPORTED_KEYS) {
//                name = "Exported Keys";
//            } else if (type == INDICES) {
//                name = "Indices";
//			} else {
//				throw new IllegalArgumentException("Unknown folder type: "+type);
//			}
//		}
//
//		public String getName() {
//			return name;
//		}
//
//		public void setName(String n) {
//			String oldName = name;
//			name = n;
//			firePropertyChange("name", oldName, name);
//		}
//
//		public SQLTable getParent() {
//			return parent;
//		}
//
//		/**
//		 * Sets the parent reference in this folder.
//		 *
//		 * @throws ClassCastException if newParent is not an instance of SQLTable.
//		 */
//		protected void setParent(SQLObject newParentTable) {
//			parent = (SQLTable) newParentTable;
//		}
//
//		protected void populateImpl() throws SQLObjectException {
//		    if (logger.isDebugEnabled()) {
//		        List<SQLObject> hierarchy = new ArrayList<SQLObject>();
//		        SQLObject parent = this;
//		        while (parent != null) {
//		            hierarchy.add(0, parent);
//		            parent = parent.getParent();
//		        }
//		        logger.debug("Populating folder on " + getParent().getName() + " for type " + type + " with ancestor path " + hierarchy);
//		    }
//
//		    if (populated) return;
//
//			logger.debug("SQLTable.Folder["+getName()+"]: populate starting");
//
//			
//			try {
//				if (type == COLUMNS) {
//					parent.populateColumns();
//				} else if (type == IMPORTED_KEYS) {
//					parent.populateColumns();
//					parent.populateRelationships(null);
//				} else if (type == EXPORTED_KEYS) {
//					CachedRowSet crs = null;
//					Connection con = null;
//					try {
//						con = parent.getParentDatabase().getConnection();
//						DatabaseMetaData dbmd = con.getMetaData();
//						crs = new CachedRowSet();
//						ResultSet exportedKeysRS = dbmd.getExportedKeys(parent.getCatalogName(), parent.getSchemaName(), parent.getName());
//                        crs.populate(exportedKeysRS);
//                        exportedKeysRS.close();
//					} catch (SQLException ex) {
//                        throw new SQLObjectException("Couldn't locate related tables", ex);
//                    } finally {
//                        // close the connection before it makes the recursive call
//                        // that could lead to opening more connections
//                        try {
//                            if (con != null) con.close();
//                        } catch (SQLException ex) {
//                            logger.warn("Couldn't close connection", ex);
//                        }
//                    }
//                    try {
//						while (crs.next()) {
//							if (crs.getInt(9) != 1) {
//								// just another column mapping in a relationship we've already handled
//								logger.debug("Got exported key with sequence " + crs.getInt(9) + " on " + crs.getString(5) + "." + crs.getString(6) + "." + crs.getString(7) + ", continuing.");
//								continue;
//							}
//							logger.debug("Got exported key with sequence " + crs.getInt(9) + " on " + crs.getString(5) + "." + crs.getString(6) + "." + crs.getString(7) + ", populating.");
//							String cat = crs.getString(5);
//							String sch = crs.getString(6);
//							String tab = crs.getString(7);
//							SQLTable fkTable = parent.getParentDatabase().getTableByName(cat, sch, tab);
//                            if (fkTable == null) {
//                                throw new IllegalStateException("While populating table " +
//                                        SQLObjectUtils.toQualifiedName(getParent()) +
//                                        ", I failed to find child table " +
//                                        "\""+cat+"\".\""+sch+"\".\""+tab+"\"");
//                            }
//							fkTable.populateColumns();
//							fkTable.populateRelationships(parent);
//						}
//					} catch (SQLException ex) {
//						throw new SQLObjectException("Couldn't locate related tables", ex);
//					} finally {
//						try {
//							if (crs != null) crs.close();
//						} catch (SQLException ex) {
//							logger.warn("Couldn't close resultset", ex);
//						}
//					}
//                } else if (type == INDICES) {
//                    parent.populateColumns();
//                    parent.populateIndices();
//				} else {
//					throw new IllegalArgumentException("Unknown folder type: "+type);
//				}
//			} finally {
//				populated = true;
//			}
//
//			logger.debug("SQLTable.Folder["+getName()+"]: populate finished");
//
//		}
//		
//        @Override
//		protected void addChildImpl(int index, SQLObject child) throws SQLObjectException {
//			logger.debug("Adding child "+child.getName()+" to folder "+getName());
//			super.addChildImpl(index, child);
//		}
//
//        /**
//         * Overrides default remove behaviour to normalize the primary key in
//         * the case of a removed SQLColumn and to check for locked (imported)
//         * columns. In both cases, the special checking is not performed if
//         * magic is disabled or this folder has no parent.
//         * 
//         * @throws LockedColumnException
//         *             If this is a folder of columns, and the column you
//         *             attempt to remove is "owned" by a relationship.
//         */
//        @Override
//        protected SQLObject removeImpl(int index) {
//            if (isMagicEnabled() && type == COLUMNS && getParent() != null) {
//                SQLColumn col = (SQLColumn) children.get(index);
//                // a column is only locked if it is an IMPORTed key--not if it is EXPORTed.
//                for (SQLRelationship r : (List<SQLRelationship>) parent.importedKeysFolder.children) {
//                    r.checkColumnLocked(col);
//                }
//            }
//            SQLObject removed = super.removeImpl(index);
//            if (isMagicEnabled() && type == COLUMNS && removed != null && getParent() != null) {
//                try {
//                    getParent().normalizePrimaryKey();
//                } catch (SQLObjectException e) {
//                    throw new SQLObjectRuntimeException(e);
//                }
//            }
//            return removed;
//        }
//        
//		public String getShortDisplayName() {
//			return name;
//		}
//
//		public boolean allowsChildren() {
//			return true;
//		}
//
//		public String toString() {
//			if (parent == null) {
//				return name+" folder (no parent)";
//			} else {
//				return name+" folder of "+parent.getName();
//			}
//		}
//
//		/**
//		 * Returns the type code of this folder.
//		 *
//		 * @return One of COLUMNS, IMPORTED_KEYS, or EXPORTED_KEYS.
//		 */
//		public int getType() {
//			return type;
//		}
//
//		/**
//	     * Returns an unmodifiable view of the child list.  All list
//	     * members will be SQLObject subclasses (SQLTable,
//	     * SQLRelationship, SQLColumn, etc.) which are directly contained
//	     * within this SQLObject.
//	     */
//	    public List getChildren(DatabaseMetaData dbmd) throws SQLObjectException {
//	        if (!allowsChildren()) //never return null;
//	            return children;
//	        populate();
//	        return Collections.unmodifiableList(children);
//	    }
//		
//		@Override
//		public Class<? extends SQLObject> getChildType() {
//			return SQLColumn.class;
//		}
//	}

	// ------------------ Accessors and mutators below this line ------------------------

	/**
	 * Walks up the SQLObject containment hierarchy and returns the
	 * first SQLDatabase object encountered.  If this SQLTable has no
	 * SQLDatabase ancestors, the return value is null.
	 *
	 * @return the value of parentDatabase
	 */
	public SQLDatabase getParentDatabase()  {
		SQLObject o = getParent();
		while (o != null && ! (o instanceof SQLDatabase)) {
			o = o.getParent();
		}
		return (SQLDatabase) o;
	}

	/**
	 * @return An empty string if the catalog for this table is null;
	 * otherwise, getCatalog().getCatalogName().
	 */
	public String getCatalogName() {
		SQLCatalog catalog = getCatalog();
		if (catalog == null) {
			return "";
		} else {
			return catalog.getName();
		}
	}

	public SQLCatalog getCatalog()  {
		SQLObject o = getParent();
		while (o != null && ! (o instanceof SQLCatalog)) {
			o = o.getParent();
		}
		return (SQLCatalog) o;
	}

	/**
	 * @return An empty string if the schema for this table is null;
	 * otherwise, schema.getSchemaName().
	 */
	public String getSchemaName() {
		SQLSchema schema = getSchema();
		if (schema == null) {
			return "";
		} else {
			return schema.getName();
		}
	}

	public SQLSchema getSchema()  {
		SQLObject o = getParent();
		while (o != null && ! (o instanceof SQLSchema)) {
			o = o.getParent();
		}
		return (SQLSchema) o;
	}

	public List<SQLColumn> getColumnsFolder() {
		return columns;
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
	public void setPhysicalName(String argName) {

        logger.debug("About to change table name from \""+getPhysicalName()+"\" to \""+argName+"\"");
        
        // this method can be called very early in a SQLTable's life,
        // before its folders exist.  Therefore, we have to
        // be careful not to look up the primary key before one exists.

        if ( (!isMagicEnabled()) || (indices == null) || (columns == null) ) {
            super.setPhysicalName(argName);
        } else try {
        	logger.info("The physical name of the table is: " + getPhysicalName());
        	String oldName;
        	if (getPhysicalName() != null) {
        		oldName = getPhysicalName();
        	} else {
        		oldName = getName();
        	}
            
            begin("Table Name Change");
            super.setPhysicalName(argName);
            
            if (isIndicesPopulated()) {
                SQLIndex primaryKeyIndex = getPrimaryKeyIndex();
                if (argName != null &&
                        primaryKeyIndex != null && 
                        (primaryKeyIndex.getName() == null
                                || "".equals(primaryKeyIndex.getName().trim())
                                || (oldName + "_pk").equals(primaryKeyIndex.getName())) ) {
                    // if the physical name is still null when forward engineer,
                    // the DDLGenerator will generate the physical name from the 
                    // logic name and this index will be updated.
                    primaryKeyIndex.setName(getPhysicalName()+"_pk");

                }
            }
            
            if (isColumnsPopulated()) {
                for (SQLColumn col : getColumns()) {
                    if (col.isAutoIncrementSequenceNameSet()) {
                        String newName = col.getAutoIncrementSequenceName().replace(oldName, argName);
                        col.setAutoIncrementSequenceName(newName);
                    }
                }
            }
            
        } catch (SQLObjectException e) {
            throw new SQLObjectRuntimeException(e);
        } finally {
            commit();
        }
	}

	/**
	 * Gets the value of remarks
	 *
	 * @return the value of remarks
	 */
	public String getRemarks()  {
		return this.remarks;
	}

	/**
	 * Sets the value of remarks
	 *
	 * @param argRemarks Value to assign to this.remarks
	 */
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
	public synchronized List<SQLColumn> getColumns() throws SQLObjectException {
		populateColumns();
		return getColumnsWithoutPopulating();
	}

	/**
	 * Returns the list of columns without populating first. This will allow
	 * access to the columns at current.
	 */
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
	public List<SQLRelationship> getImportedKeys() throws SQLObjectException {
		populateColumns();
		populateRelationships();
		return getImportedKeysWithoutPopulating();
	}

	/**
	 * Returns the list of imported keys without populating first. This will
	 * allow access to the imported keys at current.
	 */
	public List<SQLRelationship> getImportedKeysWithoutPopulating() {
		return Collections.unmodifiableList(importedKeys);
	}

	/**
	 * Gets the value of exportedKeys after populating. This will allow access
	 * to the most updated list of exported keys.
	 * 
	 * @return the value of exportedKeys
	 * @throws SQLObjectException
	 */
	public List<SQLRelationship> getExportedKeys() throws SQLObjectException {
		populateExportedKeys();
		return getExportedKeysWithoutPopulating();
	}

	/**
	 * Returns the list of exported keys without populating first. This will
	 * allow access to the exported keys at current.
	 */
	public List<SQLRelationship> getExportedKeysWithoutPopulating() {
		return Collections.unmodifiableList(exportedKeys);
	}

    /**
     * Gets the value of exportedKeys by name
     *
     * @return the value of exportedKeys
     */
    public SQLRelationship getExportedKeyByName(String name) throws SQLObjectException {
        return getExportedKeyByName(name,true);
    }

    /**
     * Gets the value of exportedKeys by name
     *
     * @return the value of exportedKeys
     */
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
    public synchronized List<SQLIndex> getUniqueIndices() throws SQLObjectException {
        populateColumns();
        populateIndices();
        List<SQLIndex> list = new ArrayList<SQLIndex>();
        for (SQLIndex index : indices) {
        	if (index.isUnique()) {
        		list.add(index);
        	}
        }
        return list;
    }

    /**
     * Gets the value of index by name
     *
     * @return the value of index
     */
    public SQLIndex getIndexByName(String name) throws SQLObjectException {
        return getIndexByName(name,true);
    }

    /**
     * Gets the value of index by name
     *
     * @return the value of index
     */
    public SQLIndex getIndexByName(String name,
            boolean populate ) throws SQLObjectException {
        if (populate) {
            populateColumns();
            populateIndices();
        }
        logger.debug("Looking for Index ["+name+"] in "+indices);
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
	public boolean isColumnsPopulated()  {
		return columnsPopulated;
	}

	/**
	 * Returns true if this table's imported keys and exported
     * keys both say are populated.
	 */
	public boolean isRelationshipsPopulated()  {
		return importedKeysPopulated && exportedKeysPopulated;
	}
	
	/**
	 * Returns true if this table's imported keys have been populated.
	 */
	public boolean isImportedKeysPopulated() {
		return importedKeysPopulated;
	}
	
	/**
	 * Returns true if this table's exported keys have been populated. 
	 */
	public boolean isExportedKeysPopulated() {
		return exportedKeysPopulated;
	}

    /**
     * Returns true if this table's indices folder says it's populated.
     */
    public boolean isIndicesPopulated()  {
        return indicesPopulated;
    }

    /**
     * Gets the name of this table's Primary Key index if it has one, otherwise
     * returns null.
     * 
     * @throws SQLObjectException
     * @deprecated this method is left over from an incomplete refactoring, and
     *             it will be removed in a future release. Use
     *             {@link #getPrimaryKeyIndex()}.getName() instead.
     */
    @Deprecated
	public String getPrimaryKeyName() throws SQLObjectException  {
        SQLIndex primaryKeyIndex = getPrimaryKeyIndex();
		return primaryKeyIndex == null ? null : primaryKeyIndex.getName();
	}

	/**
	 * Gets the type of table this object represents (TABLE or VIEW).
	 *
	 * @return the value of objectType
	 */
	public String getObjectType()  {
		return this.objectType;
	}

	/**
	 * Sets the type of table this object represents (TABLE or VIEW).
	 *
	 * @param argObjectType Value to assign to this.objectType
	 */
	public void setObjectType(String argObjectType) {
		String oldObjectType = this.objectType;
		this.objectType = argObjectType;
        if (this.objectType == null) throw new NullPointerException();
		firePropertyChange("objectType",oldObjectType, argObjectType);
	}

    /**
     * Returns the primary key for this table, or null if none exists.
     * 
     * @throws SQLObjectException
     */
    public SQLIndex getPrimaryKeyIndex() throws SQLObjectException {
    	for (SQLIndex i : getIndices()) {
    		if (i.isPrimaryKeyIndex()) return i;
    	}
        return null;
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
    public List<SQLIndex> getIndices() throws SQLObjectException {
    	populateColumns();
    	populateIndices();
        return Collections.unmodifiableList(indices);
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
    
    void refreshColumns() throws SQLObjectException {
        if (!isColumnsPopulated()) return;
        Connection con = null;
        try {
            con = getParentDatabase().getConnection();
            DatabaseMetaData dbmd = con.getMetaData();
            List<SQLColumn> newCols = SQLColumn.fetchColumnsForTable(getCatalogName(), getSchemaName(), getName(), dbmd);
            SQLObjectUtils.refreshChildren(this, newCols, SQLColumn.class);
            
            normalizePrimaryKey();
            
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
    
    public boolean removeIndex(SQLIndex sqlIndex) {
    	if (sqlIndex.getParent() != this) {
			throw new IllegalStateException("Cannot remove child " + sqlIndex.getName() + 
					" of type " + sqlIndex.getClass() + " as its parent is not " + getName());
    	}
    	int index = indices.indexOf(sqlIndex);
    	if (index != -1) {
    		indices.remove(index);
    		sqlIndex.setParent(null);
    		fireChildRemoved(SQLIndex.class, sqlIndex, index);
    		return true;
    	}
    	return false;
    }

	public List<? extends SQLObject> getChildrenWithoutPopulating() {
		List<SQLObject> children = new ArrayList<SQLObject>();
		children.addAll(columns);
		children.addAll(importedKeys);
		children.addAll(exportedKeys);
		children.addAll(indices);
		return Collections.unmodifiableList(children);
	}

	@Override
	protected boolean removeChildImpl(SPObject child) {
		if (child instanceof SQLColumn) {
            if (isMagicEnabled()) {
                SQLColumn col = (SQLColumn) child;
                // a column is only locked if it is an IMPORTed key--not if it is EXPORTed.
                for (SQLRelationship r : importedKeys) {
                    r.checkColumnLocked(col);
                }
            }
            
            boolean removed = removeColumn((SQLColumn) child);
            
            if (isMagicEnabled() && removed && getParent() != null) {
                try {
                    normalizePrimaryKey();
                } catch (SQLObjectException e) {
                    throw new SQLObjectRuntimeException(e);
                }
            }
		} else if (child instanceof SQLRelationship) {
			if (importedKeys.contains(child)) {
				removeImportedKey((SQLRelationship) child);
			} else {
				removeExportedKey((SQLRelationship) child);
			}
		} else if (child instanceof SQLIndex) {
			return removeIndex((SQLIndex) child);
		}
		return false;
	}
	
	public int childPositionOffset(Class<? extends SPObject> childType) {
		int offset = 0;
		
		if (childType == SQLColumn.class) return offset;
		offset += columns.size();
		
		if (childType == SQLRelationship.class) return offset;
		offset += importedKeys.size();
		offset += exportedKeys.size();
		
		if (childType == SQLIndex.class) return offset;
		
		throw new IllegalArgumentException("The type " + childType + 
				" is not a valid child type of " + getName());
	}

	public List<? extends SPObject> getDependencies() {
		// TODO Auto-generated method stub
		return null;
	}

	public void removeDependency(SPObject dependency) {
		// TODO Auto-generated method stub
		
	}

	/**
	 * Overridden so that when you set the populated flag, the populated flag
	 * for columns, imported keys, exported keys and indices are updated
	 * accordingly as well.
	 */
	@Override
	public void setPopulated(boolean v) {
		populated = v;
		columnsPopulated = v;
		importedKeysPopulated = v;
		exportedKeysPopulated = v;
		indicesPopulated = v;
	}

    void refreshImportedKeys() throws SQLObjectException {
        if (!importedKeysPopulated) {
            logger.debug("Not refreshing unpopulated imported keys of " + this);
            return;
        }

        List<SQLRelationship> newRels = SQLRelationship.fetchImportedKeys(this);
        if (logger.isDebugEnabled()) {
            logger.debug("New imported keys of " + getName() + ": " + newRels);
        }
        SQLObjectUtils.refreshChildren(this, newRels, SQLRelationship.class);
    }

    void refreshIndexes() throws SQLObjectException {
        if (!isIndicesPopulated()) return;
        Connection con = null;
        try {
            con = getParentDatabase().getConnection();
            DatabaseMetaData dbmd = con.getMetaData();
            
            // indexes (incl. PK)
            List<SQLIndex> newIndexes = SQLIndex.fetchIndicesForTable(dbmd, this);
            SQLObjectUtils.refreshChildren(this, newIndexes, SQLIndex.class);
            
            normalizePrimaryKey();
            
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
		List<Class<? extends SPObject>> types = new ArrayList<Class<? extends SPObject>>();
		types.add(SQLColumn.class);
		types.add(SQLRelationship.class);
		types.add(SQLIndex.class);
		return Collections.unmodifiableList(types);
	}
}