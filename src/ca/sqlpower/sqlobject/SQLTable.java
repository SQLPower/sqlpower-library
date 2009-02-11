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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import ca.sqlpower.sql.CachedRowSet;
import ca.sqlpower.sqlobject.SQLIndex.AscendDescend;
import ca.sqlpower.sqlobject.SQLIndex.Column;

public class SQLTable extends SQLObject {

	private static Logger logger = Logger.getLogger(SQLTable.class);

	protected String remarks="";
	private String objectType;
	protected String physicalPrimaryKeyName;

	/**
	 * A List of SQLColumn objects which make up all the columns of
	 * this table.
	 */
	protected Folder<SQLColumn> columnsFolder;

	/**
	 * A List of SQLRelationship objects describing keys that this
	 * table exports.  This SQLTable is the "pkTable" in its exported
	 * keys.
	 */
	protected Folder<SQLRelationship> exportedKeysFolder;

	/**
	 * A container for SQLRelationship objects describing keys that this
	 * table imports.  This SQLTable is the "fkTable" in its imported
	 * keys.
	 */
	protected Folder<SQLRelationship> importedKeysFolder;

    /**
     * A container for SQLIndex objects that describe the various database indices
     * that exist on this table.
     */
    private Folder<SQLIndex> indicesFolder;

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
        this.children = new ArrayList();  // initFolders normally does this, but this constructor never calls it
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
        this.children = new ArrayList();
		addChild(new Folder(Folder.COLUMNS, populated));
		addChild(new Folder(Folder.EXPORTED_KEYS, populated));
        addChild(new Folder(Folder.IMPORTED_KEYS, populated));
        addChild(new Folder(Folder.INDICES, populated));
	}

	/**
	 * Creates a new SQLTable under the given parent database.  The new table will have
	 * all the same properties as the given source table.
	 *
	 * @param source The table to copy
	 * @param parent The database to insert the new table into
	 * @return The new table
	 * @throws SQLObjectException if there are populate problems on source or parent
	 * Or if the parent has children of type other than SQLTable.
	 */
	public static SQLTable getDerivedInstance(SQLTable source, SQLDatabase parent)
		throws SQLObjectException {
		source.populateColumns();
		source.populateRelationships();
        source.populateIndices();
		SQLTable t = new SQLTable(parent, true);
		t.setName(source.getName());
		t.remarks = source.remarks;
		t.setChildrenInaccessibleReason(source.getChildrenInaccessibleReason());

		t.setPhysicalName(source.getPhysicalName());
		t.physicalPrimaryKeyName = source.getPhysicalPrimaryKeyName();

		t.inherit(source);
        inheritIndices(source,t);
        
		parent.addChild(t);
		return t;
	}
    
    /**
     * inherit indices from the source table,
     * @param source
     * @param target    
     * @throws SQLObjectException 
     */
    private static void inheritIndices(SQLTable source, SQLTable target) throws SQLObjectException {
        for ( SQLIndex index : (List<SQLIndex>)source.getIndicesFolder().getChildren()) {
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
    private synchronized void populateColumns() throws SQLObjectException {

		if (columnsFolder.isPopulated()) return;
		if (columnsFolder.children.size() > 0) throw new IllegalStateException("Can't populate table because it already contains columns");

		logger.debug("column folder populate starting for table " + getName());

		Connection con = null;
		try {
		    con = getParentDatabase().getConnection();
		    DatabaseMetaData dbmd = con.getMetaData();
			List<SQLColumn> cols = SQLColumn.fetchColumnsForTable(
			        getCatalogName(), getSchemaName(), getName(), dbmd);
			for (SQLColumn col : cols) {
			    columnsFolder.children.add(col);
			    col.setParent(columnsFolder);
			}
		} catch (SQLException e) {
			throw new SQLObjectException("Failed to populate columns of table "+getName(), e);
		} finally {
			columnsFolder.populated = true;
			int newSize = columnsFolder.children.size();
			int[] changedIndices = new int[newSize];
			for (int i = 0; i < newSize; i++) {
				changedIndices[i] = i;
			}
			columnsFolder.fireDbChildrenInserted(changedIndices, columnsFolder.children);
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
	private synchronized void populateIndices() throws SQLObjectException {
        if (indicesFolder.isPopulated()) return;
        if (indicesFolder.children.size() > 0) throw new IllegalStateException("Can't populate indices folder because it already contains children!");
        if (!columnsFolder.isPopulated()) throw new IllegalStateException("Columns folder must be populated");
        
        // If the SQLTable is a view, simply indicated folder is populated and then leave
        // Since Views don't have indices (and Oracle throws an error)
        if (objectType.equals("VIEW")) { 
            indicesFolder.populated = true;
            return;
        }
        
        logger.debug("index folder populate starting");

        Connection con = null;
        try {
            con = getParentDatabase().getConnection();
            DatabaseMetaData dbmd = con.getMetaData();
            logger.debug("before addIndicesToTable");
            
            List<SQLIndex> indexes = SQLIndex.fetchIndicesForTable(dbmd, this);
            
            for (SQLIndex i : indexes) {
                indicesFolder.children.add(i);
                i.setParent(indicesFolder);
            }
            logger.debug("found "+indicesFolder.children.size()+" indices.");
          
            indicesFolder.populated = true;
        } catch (SQLException e) {
            throw new SQLObjectException("Failed to populate indices of table "+getName(), e);
        } finally {
            indicesFolder.populated = true;

            try {
                if (con != null) con.close();
            } catch (SQLException e) {
                logger.error("Closing connection failed. Squishing this exception: ", e);
            }
            
            // FIXME this might change the order of columns without firing an event
            Collections.sort(columnsFolder.children, new SQLColumn.CompareByPKSeq());
            
            int newSize = indicesFolder.children.size();
            int[] changedIndices = new int[newSize];
            for (int i = 0; i < newSize; i++) {
                changedIndices[i] = i;
            }
            indicesFolder.fireDbChildrenInserted(changedIndices, indicesFolder.children);
        }
        normalizePrimaryKey();
        logger.debug("index folder populate finished");
    }

	/**
	 * Populates all the imported key relationships.  This has the
	 * side effect of populating the exported key side of the
	 * relationships for the exporting tables.
	 */
    private synchronized void populateRelationships() throws SQLObjectException {
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
    private synchronized void populateRelationships(SQLTable pkTable) throws SQLObjectException {
		if (!columnsFolder.isPopulated()) throw new IllegalStateException("Table must be populated before relationships are added");
		if (importedKeysFolder.isPopulated()) return;

		logger.debug("SQLTable: relationship populate starting");

		int oldSize = importedKeysFolder.children.size();

		/* this must come before
		 * SQLRelationship.addImportedRelationshipsToTable because
		 * addImportedRelationshipsToTable causes SQLObjectEvents to be fired,
		 * which in turn could cause infinite recursion when listeners
		 * query the size of the relationships folder.
		 */
		importedKeysFolder.populated = true;
		boolean allRelationshipsAdded = false;
		try {
			allRelationshipsAdded = SQLRelationship.addImportedRelationshipsToTable(pkTable, this);
		} finally {
			int newSize = importedKeysFolder.children.size();
			if (newSize > oldSize) {
				int[] changedIndices = new int[newSize - oldSize];
				for (int i = 0, n = newSize - oldSize; i < n; i++) {
					changedIndices[i] = oldSize + i;
				}
				try {
					importedKeysFolder.fireDbChildrenInserted
						(changedIndices,
						 importedKeysFolder.children.subList(oldSize, newSize));
				} catch (IndexOutOfBoundsException ex) {
					logger.error("Index out of bounds while adding imported keys to table "
								 +getName()+" where oldSize="+oldSize+"; newSize="+newSize
								 +"; imported keys="+importedKeysFolder.children);
				}
			}
			//The imported keys folder will only be guaranteed to be completely populated if
			//any table can be the pkTable not a specific table.
			if (pkTable != null && !allRelationshipsAdded) {
				importedKeysFolder.populated = false;
			}
		}

		logger.debug("SQLTable: relationship populate finished");

	}

	/**
	 * Convenience method for getImportedKeys.addChild(r).
	 */
	void addImportedKey(SQLRelationship r) throws SQLObjectException {
		importedKeysFolder.addChild(r);
	}

	/**
	 * Convenience method for getImportedKeys.removeChild(r).
	 */
	public void removeImportedKey(SQLRelationship r) {
		importedKeysFolder.removeChild(r);
	}

	/**
	 * Convenience method for getExportedKeys.addChild(r).
	 */
	void addExportedKey(SQLRelationship r) throws SQLObjectException {
		exportedKeysFolder.addChild(r);
	}

	/**
	 * Convenience method for getExportedKeys.removeChild(r).
	 */
	public void removeExportedKey(SQLRelationship r) {
		exportedKeysFolder.removeChild(r);
	}

	/**
	 * Counts the number of columns in the primary key of this table.
	 */
	public int getPkSize() {
		int size = 0;
		Iterator it = columnsFolder.children.iterator();
		while (it.hasNext()) {
			SQLColumn c = (SQLColumn) it.next();
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
	public void inherit(SQLTable source) throws SQLObjectException {
		inherit(columnsFolder.children.size(), source);
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
	public void inherit(int pos, SQLTable source) throws SQLObjectException {
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
				((SQLColumn) columnsFolder.children.get(i)).primaryKeySeq = new Integer(i + sourceSize);
			}
		} else {
			addToPK = false;
		}

		Iterator it = source.getColumns().iterator();
		while (it.hasNext()) {
			SQLColumn child = (SQLColumn) it.next();
			c = SQLColumn.getDerivedInstance(child, this);
			if (originalSize > 0) {
				if (addToPK) {
					c.primaryKeySeq = new Integer(pos);
				} else {
					c.primaryKeySeq = null;
				}
			}
			columnsFolder.addChild(pos, c);
			pos += 1;
		}
	}

	public void inherit(int pos, SQLColumn sourceCol, boolean addToPK) throws SQLObjectException {
	    if (addToPK && pos > 0 && !getColumn(pos - 1).isPrimaryKey()) {
	        throw new IllegalArgumentException("Can't inherit new PK column below a non-PK column! Insert pos="+pos+"; addToPk="+addToPK);
	    }
		SQLColumn c = SQLColumn.getDerivedInstance(sourceCol, this);
		if (addToPK) {
			c.primaryKeySeq = new Integer(1);
		} else {
			c.primaryKeySeq = null;
		}
		columnsFolder.addChild(pos, c);
	}

	public SQLColumn getColumn(int idx) throws SQLObjectException {
		return (SQLColumn) columnsFolder.getChild(idx);
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
		    logger.debug("Looking for column "+colName+" in "+columnsFolder.children);
		}
		Iterator it = columnsFolder.children.iterator();
		logger.debug("Table " + getName() + " has " + columnsFolder.children.size() + " columns");
		while (it.hasNext()) {
			SQLColumn col = (SQLColumn) it.next();
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

		Iterator it = getColumns().iterator();

		int colIdx = 0;
		while (it.hasNext()) {
			if (it.next() == col) {
				return colIdx;
			}
			colIdx++;
		}

		logger.debug("NOT FOUND");
		return -1;
	}

	public void addColumn(SQLColumn col) throws SQLObjectException {
		addColumnImpl(columnsFolder.children.size(), col);
	}

	public void addColumn(int pos, SQLColumn col) throws SQLObjectException {
		addColumnImpl(pos, col);
	}

	private void addColumnImpl(int pos, SQLColumn col) throws SQLObjectException {
		if (getColumnIndex(col) != -1) {
			col.addReference();
			return;
		}

		if (isMagicEnabled()) {
		    boolean addToPK = false;
		    int pkSize = getPkSize();
		    if (getColumns().size() > 0 && pos < pkSize) {
		        addToPK = true;
		        for (int i = pos; i < pkSize; i++) {
		            ((SQLColumn) getColumns().get(i)).primaryKeySeq = new Integer(i + 1);
		        }
		    }

		    col.setParent(null);
		    if (addToPK) {
		        col.nullable = DatabaseMetaData.columnNoNulls;
		        col.primaryKeySeq = new Integer(pos);
		    } else {
		        col.primaryKeySeq = null;
		    }
		}
		columnsFolder.addChild(pos, col);
	}

	/**
     * Adds the given SQLIndex object to this table's index folder.
	 */
    public void addIndex(SQLIndex idx) throws SQLObjectException {
        if (idx.isPrimaryKeyIndex()) {
            getIndicesFolder().addChild(0, idx);
            return;
        }
        getIndicesFolder().addChild(idx);
    }

	/**
	 * Connects up the columnsFolder, exportedKeysFolder,
	 * importedKeysFolder, and indicesFolder pointers to the
     * children at indices 0, 1, 2, and 3 respectively.
	 */
	protected void addChildImpl(int index, SQLObject child) throws SQLObjectException {
		if (child instanceof Folder) {
			if (children.size() == 0) {
				columnsFolder = (Folder) child;
			} else if (children.size() == 1) {
				exportedKeysFolder = (Folder) child;
            } else if (children.size() == 2) {
                importedKeysFolder = (Folder) child;
            } else if (children.size() == 3) {
                indicesFolder = (Folder) child;
			} else {
				throw new UnsupportedOperationException("Can't add a 5th folder to SQLTable");
			}
		} else {
			throw new UnsupportedOperationException("You can only add Folders to SQLTable");
		}
		super.addChildImpl(index, child);
	}

    /**
     * Calls {@link #removeColumn(SQLColumn)} with the appropriate argument.
     * 
     * @throws LockedColumnException
     *             If the column is "owned" by a relationship, and cannot be
     *             safely removed.
     */
	public void removeColumn(int index) throws SQLObjectException {
		removeColumn((SQLColumn) columnsFolder.children.get(index));
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
	public void removeColumn(SQLColumn col) throws SQLObjectException {
	    columnsFolder.removeChild(col);
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
            startCompoundEdit("Changing column index");
            SQLColumn col = (SQLColumn) columnsFolder.children.get(oldIdx);
            Integer oldPkSeq = col.primaryKeySeq;
            Integer interimPkSeq;
            if (putInPK) {
                interimPkSeq = new Integer(1); // will get sane value when normalized
                col.setNullable(DatabaseMetaData.columnNoNulls);
            } else {
                interimPkSeq = null;
            }
            col.primaryKeySeq = interimPkSeq;
            col.fireDbObjectChanged("primaryKeySeq", oldPkSeq, interimPkSeq);

            // If the indices are the same, then there's no point in moving the column
            if (oldIdx != newIdx) {
                columnsFolder.children.remove(oldIdx);
                columnsFolder.fireDbChildRemoved(oldIdx, col);
                columnsFolder.children.add(newIdx, col);
                columnsFolder.fireDbChildInserted(newIdx, col);
            }

            normalizePrimaryKey();
        } finally {
            endCompoundEdit("Changing column index");
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
            
            startCompoundEdit("Normalizing Primary Key");

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
                        Integer oldPkSeq = col.getPrimaryKeySeq();
                        Integer newPkSeq = new Integer(i);
                        col.primaryKeySeq = newPkSeq;
                        col.fireDbObjectChanged("primaryKeySeq", oldPkSeq, newPkSeq);
                        i++;
                    }
                }

                // Phase 3 (see doc comment)

                if (getPrimaryKeyIndex() == null) {
                    SQLIndex pkIndex = new SQLIndex(getName()+"_pk", true, null, null ,null);
                    pkIndex.setPrimaryKeyIndex(true);
                    addIndex(pkIndex);
                    logger.debug("new pkIndex.getChildCount()="+pkIndex.getChildCount());
                }

                SQLIndex pkIndex = getPrimaryKeyIndex();
                Map<SQLColumn, Column> oldColumnInstances = new HashMap<SQLColumn, Column>();
                while (pkIndex.getChildCount() > 0) {
                    Column child = (Column) pkIndex.removeChild(0);
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
                    getIndicesFolder().removeChild(pkIndex);
                }
            } while (normalizeAgain);
		} finally {
		    endCompoundEdit("Normalizing Primary Key");
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
		Iterator it = getExportedKeys().iterator();
		while (it.hasNext()) {
			SQLRelationship r = (SQLRelationship) it.next();
			if (r.containsPkColumn(col)) {
				keys.add(r);
			}
		}
		it = getExportedKeys().iterator();
		while (it.hasNext()) {
			SQLRelationship r = (SQLRelationship) it.next();
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
		fireDbObjectChanged("parent", oldVal, getParent());
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
	public void populateImpl() throws SQLObjectException {
		// SQLTable: populate is a no-op
	}

	public boolean isPopulated() {
		return true;
	}

	/**
	 * Returns true (tables have several folders as children).
	 */
	public boolean allowsChildren() {
		return true;
	}

	public Class<? extends SQLObject> getChildType() {
		return Folder.class;
	}

	/**
	 * The Folder class is a SQLObject that holds a SQLTable's child
	 * folders (columns and relationships).
	 */
	public static class Folder<T extends SQLObject> extends SQLObject {
		protected int type;
		protected String name;
		protected SQLTable parent;

		public static final int COLUMNS = 1;
		public static final int IMPORTED_KEYS = 2;
        public static final int EXPORTED_KEYS = 3;
        public static final int INDICES = 4;

		public Folder(int type, boolean populated) {
			this.populated = populated;
			this.type = type;
			this.children = new ArrayList<T>();
			if (type == COLUMNS) {
				name = "Columns";
			} else if (type == IMPORTED_KEYS) {
				name = "Imported Keys";
            } else if (type == EXPORTED_KEYS) {
                name = "Exported Keys";
            } else if (type == INDICES) {
                name = "Indices";
			} else {
				throw new IllegalArgumentException("Unknown folder type: "+type);
			}
		}

		public String getName() {
			return name;
		}

		public void setName(String n) {
			String oldName = name;
			name = n;
			fireDbObjectChanged("name", oldName, name);
		}

		public SQLTable getParent() {
			return parent;
		}

		/**
		 * Sets the parent reference in this folder.
		 *
		 * @throws ClassCastException if newParent is not an instance of SQLTable.
		 */
		protected void setParent(SQLObject newParentTable) {
			parent = (SQLTable) newParentTable;
		}

		public void populateImpl() throws SQLObjectException {
		    if (logger.isDebugEnabled()) {
		        List<SQLObject> hierarchy = new ArrayList<SQLObject>();
		        SQLObject parent = this;
		        while (parent != null) {
		            hierarchy.add(0, parent);
		            parent = parent.getParent();
		        }
		        logger.debug("Populating folder on " + getParent().getName() + " for type " + type + " with ancestor path " + hierarchy);
		    }

		    if (populated) return;

			logger.debug("SQLTable.Folder["+getName()+"]: populate starting");

			
			try {
				if (type == COLUMNS) {
					parent.populateColumns();
				} else if (type == IMPORTED_KEYS) {
					parent.populateColumns();
					parent.populateRelationships(null);
				} else if (type == EXPORTED_KEYS) {
					CachedRowSet crs = null;
					Connection con = null;
					try {
						con = parent.getParentDatabase().getConnection();
						DatabaseMetaData dbmd = con.getMetaData();
						crs = new CachedRowSet();
						ResultSet exportedKeysRS = dbmd.getExportedKeys(parent.getCatalogName(), parent.getSchemaName(), parent.getName());
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
							SQLTable fkTable = parent.getParentDatabase().getTableByName(cat, sch, tab);
                            if (fkTable == null) {
                                throw new IllegalStateException("While populating table " +
                                        SQLObjectUtils.toQualifiedName(getParent()) +
                                        ", I failed to find child table " +
                                        "\""+cat+"\".\""+sch+"\".\""+tab+"\"");
                            }
							fkTable.populateColumns();
							fkTable.populateRelationships(parent);
						}
					} catch (SQLException ex) {
						throw new SQLObjectException("Couldn't locate related tables", ex);
					} finally {
						try {
							if (crs != null) crs.close();
						} catch (SQLException ex) {
							logger.warn("Couldn't close resultset", ex);
						}
					}
                } else if (type == INDICES) {
                    parent.populateColumns();
                    parent.populateIndices();
				} else {
					throw new IllegalArgumentException("Unknown folder type: "+type);
				}
			} finally {
				populated = true;
			}

			logger.debug("SQLTable.Folder["+getName()+"]: populate finished");

		}
		
        @Override
		protected void addChildImpl(int index, SQLObject child) throws SQLObjectException {
			logger.debug("Adding child "+child.getName()+" to folder "+getName());
			super.addChildImpl(index, child);
		}

        /**
         * Overrides default remove behaviour to normalize the primary key in
         * the case of a removed SQLColumn and to check for locked (imported)
         * columns. In both cases, the special checking is not performed if
         * magic is disabled or this folder has no parent.
         * 
         * @throws LockedColumnException
         *             If this is a folder of columns, and the column you
         *             attempt to remove is "owned" by a relationship.
         */
        @Override
        protected SQLObject removeImpl(int index) {
            if (isMagicEnabled() && type == COLUMNS && getParent() != null) {
                SQLColumn col = (SQLColumn) children.get(index);
                // a column is only locked if it is an IMPORTed key--not if it is EXPORTed.
                for (SQLRelationship r : (List<SQLRelationship>) parent.importedKeysFolder.children) {
                    r.checkColumnLocked(col);
                }
            }
            SQLObject removed = super.removeImpl(index);
            if (isMagicEnabled() && type == COLUMNS && removed != null && getParent() != null) {
                try {
                    getParent().normalizePrimaryKey();
                } catch (SQLObjectException e) {
                    throw new SQLObjectRuntimeException(e);
                }
            }
            return removed;
        }
        
		public String getShortDisplayName() {
			return name;
		}

		public boolean allowsChildren() {
			return true;
		}

		public String toString() {
			if (parent == null) {
				return name+" folder (no parent)";
			} else {
				return name+" folder of "+parent.getName();
			}
		}

		/**
		 * Returns the type code of this folder.
		 *
		 * @return One of COLUMNS, IMPORTED_KEYS, or EXPORTED_KEYS.
		 */
		public int getType() {
			return type;
		}

		/**
	     * Returns an unmodifiable view of the child list.  All list
	     * members will be SQLObject subclasses (SQLTable,
	     * SQLRelationship, SQLColumn, etc.) which are directly contained
	     * within this SQLObject.
	     */
	    public List getChildren(DatabaseMetaData dbmd) throws SQLObjectException {
	        if (!allowsChildren()) //never return null;
	            return children;
	        populate();
	        return Collections.unmodifiableList(children);
	    }
		
		@Override
		public Class<? extends SQLObject> getChildType() {
			return SQLColumn.class;
		}

		/**
		 * This will get the number of children currently populated. Folders
		 * with foreign keys can be partially populated when lazy loading but
		 * calling the SQLObject child access methods will populate all of the
		 * children.
		 * 
		 * <p>This is not the standard way of getting children of a folder.
		 * Use the getChildCount() method to get children
		 * unless you specifically don't want the folder to populate.
		 */
		public int retrieveChildCountNoPopulate() {
			return children.size();
		}

		/**
		 * This will get the children currently populated. Folders with foreign
		 * keys can be partially populated when lazy loading but calling the
		 * SQLObject child access methods will populate all of the children.
		 * 
		 * <p>This is not the standard way of getting children of a folder.
		 * Use the getChildren() method to get children
		 * unless you specifically don't want the folder to populate.
		 */
		public List<T> retrieveChildrenNoPopulate() {
			return children;
		}
	}

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

	public Folder<SQLColumn> getColumnsFolder() {
		return columnsFolder;
	}

	public Folder<SQLRelationship> getImportedKeysFolder() {
		return importedKeysFolder;
	}

	public Folder<SQLRelationship> getExportedKeysFolder() {
		return exportedKeysFolder;
	}

    public Folder<SQLIndex> getIndicesFolder() {
        return indicesFolder;
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
	public void setName(String argName) {

        logger.debug("About to change table name from \""+getName()+"\" to \""+argName+"\"");
        
        // this method can be called very early in a SQLTable's life,
        // before its folders exist.  Therefore, we have to
        // be careful not to look up the primary key before one exists.

        if ( (!isMagicEnabled()) || (indicesFolder == null) || (columnsFolder == null) ) {
            super.setName(argName);
        } else try {
            String oldName = getName();
            
            startCompoundEdit("Table Name Change");
            super.setName(argName);
            SQLIndex primaryKeyIndex = getPrimaryKeyIndex();
            if (argName != null &&
                primaryKeyIndex != null && 
               (getPrimaryKeyName() == null
                    || "".equals(getPrimaryKeyName())
                    || (oldName+"_pk").equals(getPrimaryKeyName())) ) {
                primaryKeyIndex.setName(getName()+"_pk");
            }
            
            for (SQLColumn col : getColumns()) {
                if (col.isAutoIncrementSequenceNameSet()) {
                    String newName = col.getAutoIncrementSequenceName().replace(oldName, argName);
                    col.setAutoIncrementSequenceName(newName);
                }
            }
        } catch (SQLObjectException e) {
            throw new SQLObjectRuntimeException(e);
        } finally {
            endCompoundEdit("Ending table name compound edit");
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
		fireDbObjectChanged("remarks",oldRemarks,argRemarks);
	}

	/**
	 * Gets the value of columns
	 *
	 * @return the value of columns
	 */
	public synchronized List<SQLColumn> getColumns() throws SQLObjectException {
		populateColumns();
		return columnsFolder.getChildren();
	}

	/**
	 * Gets the value of importedKeys
	 *
	 * @return the value of importedKeys
	 */
	public List<SQLRelationship> getImportedKeys() throws SQLObjectException {
		return this.importedKeysFolder.getChildren();
	}

	/**
	 * Gets the value of exportedKeys
	 *
	 * @return the value of exportedKeys
	 */
	public List<SQLRelationship> getExportedKeys() throws SQLObjectException {
		return this.exportedKeysFolder.getChildren();
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
        logger.debug("Looking for Exported Key ["+name+"] in "+exportedKeysFolder.getChildren() );
        Iterator it = exportedKeysFolder.children.iterator();
        while (it.hasNext()) {
            SQLRelationship r = (SQLRelationship) it.next();
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
        Iterator it = getIndicesFolder().children.iterator();
        while (it.hasNext()) {
            SQLIndex idx = (SQLIndex) it.next();
            if (idx.isUnique() ) {
                list.add(idx);
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
        logger.debug("Looking for Index ["+name+"] in "+getIndicesFolder().children);
        Iterator it = getIndicesFolder().children.iterator();
        while (it.hasNext()) {
            SQLIndex idx = (SQLIndex) it.next();
            if (idx.getName().equalsIgnoreCase(name)) {
                logger.debug("FOUND");
                return idx;
            }
        }
        logger.debug("NOT FOUND");
        return null;
    }

	/**
     * Returns true if this table's columns folder says it's populated.
	 */
	public boolean isColumnsPopulated()  {
		return columnsFolder.isPopulated();
	}

	/**
	 * Returns true if this table's imported keys folder and exported
     * keys folders both say they're populated.
	 */
	public boolean isRelationshipsPopulated()  {
		return importedKeysFolder.isPopulated() && exportedKeysFolder.isPopulated();
	}

    /**
     * Returns true if this table's indices folder says it's populated.
     */
    public boolean isIndicesPopulated()  {
        return indicesFolder.isPopulated();
    }

	/**
	 * Gets the name of this table's Primary Key index if it has one, otherwise
     * returns null.
	 * @throws SQLObjectException 
	 */
	public String getPrimaryKeyName() throws SQLObjectException  {
        SQLIndex primaryKeyIndex = getPrimaryKeyIndex();
		return primaryKeyIndex == null ? null : primaryKeyIndex.getName();
	}
  
	/**
	 * Gets the value of physicalPrimaryKeyName
	 *
	 * @return the value of physicalPrimaryKeyName
	 * @throws SQLObjectException 
	 */
	public String getPhysicalPrimaryKeyName() throws SQLObjectException  {
        SQLIndex primaryKeyIndex = getPrimaryKeyIndex();
        return primaryKeyIndex == null ? null : primaryKeyIndex.getPhysicalName();
	}

	/**
	 * Sets the value of physicalPrimaryKeyName
	 *
	 * @param argPhysicalPrimaryKeyName Value to assign to this.physicalPrimaryKeyName
	 */

    public void setPhysicalPrimaryKeyName(String argPhysicalPrimaryKeyName) {
		String oldPhysicalPrimaryKeyName = this.physicalPrimaryKeyName;
		this.physicalPrimaryKeyName = argPhysicalPrimaryKeyName;
		fireDbObjectChanged("physicalPrimaryKeyName",oldPhysicalPrimaryKeyName,argPhysicalPrimaryKeyName);
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
		fireDbObjectChanged("objectType",oldObjectType, argObjectType);
	}

    /**
     * Returns the primary key for this table, or null if none exists.
     * 
     * @throws SQLObjectException
     */
    public SQLIndex getPrimaryKeyIndex() throws SQLObjectException {
        for (SQLIndex i : (List<SQLIndex>)getIndicesFolder().getChildren()){
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
    static void addTablesToTableContainer(
            SQLObject container, DatabaseMetaData dbmd,
            String catalogName, String schemaName)
    throws SQLObjectException, SQLException {
        ResultSet rs = null;
        try {
            rs = dbmd.getTables(catalogName,
                    schemaName,
                    "%",
                    new String[] {"TABLE", "VIEW"});

            while (rs.next()) {
                container.children.add(new SQLTable(container,
                        rs.getString(3),
                        rs.getString(5),
                        rs.getString(4),
                        false));
            }
            rs.close();
            rs = null;
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
        List<SQLIndex> indices = getIndicesFolder().getChildren();
        return Collections.unmodifiableList(indices);
    }
    
    public String toQualifiedName() {
        return SQLObjectUtils.toQualifiedName(this, SQLDatabase.class);
    }
    
    @Override
    void refresh() throws SQLObjectException {
        if (!isColumnsPopulated()) return;
        Connection con = null;
        try {
            con = getParentDatabase().getConnection();
            DatabaseMetaData dbmd = con.getMetaData();
            List<SQLColumn> newCols = SQLColumn.fetchColumnsForTable(getCatalogName(), getSchemaName(), getName(), dbmd);
            SQLObjectUtils.refreshChildren(columnsFolder, newCols);
            
            // TODO relationships
            
            // indexes (incl. PK)
            List<SQLIndex> newIndexes = SQLIndex.fetchIndicesForTable(dbmd, this);
            SQLObjectUtils.refreshChildren(indicesFolder, newIndexes);
            
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
    
}