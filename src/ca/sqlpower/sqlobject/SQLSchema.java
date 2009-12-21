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
import java.util.List;

import org.apache.log4j.Logger;

import ca.sqlpower.object.SPObject;

/**
 * A SQLSchema is a container for SQLTables.  If it is in the
 * containment hierarchy for a given RDBMS, it will be directly above
 * SQLTables.  Its parent could be either a SQLDatabase or a SQLCatalog.
 */
public class SQLSchema extends SQLObject {
	private static final Logger logger = Logger.getLogger(SQLSchema.class);
	
	private final List<SQLTable> tables = new ArrayList<SQLTable>();

    /**
     * Creates a list of unpopulated Schema objects corresponding to the list of
     * schemas in the given database metadata.
     * 
     * @param dbmd
     *            The database metadata to get the schema names from.
     * @param catalogName
     *            The catalog under which to look for the schemas. If the
     *            underlying database does not support catalogs, or you just
     *            want the schema list for the current catalog, set this
     *            argument to null.
     * @return A list of unpopulated, unparented SQLSchema objects whose names
     *         and order matches that given by the database metadata.
     * @throws SQLObjectException If database access fails.
     */
    public static List<SQLSchema> fetchSchemas(DatabaseMetaData dbmd, String catalogName)
    throws SQLObjectException {
    
        ResultSet rs = null;
        String oldCatalog = null;
        try {
            
            if (catalogName != null) {
                oldCatalog = dbmd.getConnection().getCatalog();
                // This can fail in SS2K because of privilege problems.  There is
                // apparently no way to check if it will fail; you just have to try.
                try {
                    dbmd.getConnection().setCatalog(catalogName);
                } catch (SQLException ex) {
                    // XXX it would be preferable to store this exception in a popuateFailReason on the containing catalog
                    logger.info("populate: Could not setCatalog("+catalogName+"). Assuming it's a permission problem.  Stack trace:", ex);
                    return Collections.emptyList();
                }
            }
            
            List<SQLSchema> schemas = new ArrayList<SQLSchema>();
            rs = dbmd.getSchemas();
            while (rs.next()) {
                String schemaName = rs.getString(1);
                if (schemaName != null) {
                    SQLSchema schema = new SQLSchema(null, schemaName, false);
                    schema.setNativeTerm(dbmd.getSchemaTerm());
                    logger.debug("Set schema term to "+schema.getNativeTerm());
                    schemas.add(schema);
                }
            }
            return schemas;
        } catch (SQLException ex) {
            throw new SQLObjectException("Failed to get schema names from source database", ex);
        } finally {
            try {
                if (rs != null) rs.close();
            } catch (SQLException e) {
                logger.warn("Couldn't close result set. Squishing this exception:", e);
            }
            try {
                if (oldCatalog != null) {
                    dbmd.getConnection().setCatalog(oldCatalog);
                }
            } catch (SQLException e) {
                logger.warn("Couldn't set catalog back to '"+oldCatalog+"'. Squishing this exception:", e);
            }
        }
    }

	protected String nativeTerm;
	
	public SQLSchema(boolean populated) {
		this(null, null, populated);
	}

	public SQLSchema(SQLObject parent, String name, boolean populated) {
		if (parent != null && !(parent instanceof SQLCatalog || parent instanceof SQLDatabase)) {
			throw new IllegalArgumentException("Parent to SQLSchema must be SQLCatalog or SQLDatabase");
		}
		setParent(parent);
		setName(name);
		this.nativeTerm = "schema";
		this.populated = populated;
	}

    @Override
    public void updateToMatch(SQLObject source) throws SQLObjectException {
        SQLSchema s = (SQLSchema) source;
        setName(s.getName());
        setNativeTerm(s.getNativeTerm());
        setPhysicalName(s.getPhysicalName());
    }

	public SQLTable getTableByName(String tableName) throws SQLObjectException {
		populate();
		for (SQLTable child : tables) {
			logger.debug("getTableByName: is child '"+child.getName()+"' equal to '"+tableName+"'?");		
			if (child.getName().equalsIgnoreCase(tableName)) {
				return child;
			}
		}
		return null;
	}

	public String toString() {
		return getShortDisplayName();
	}

	public boolean isParentTypeDatabase() {
		return (getParent() instanceof SQLDatabase);
	}

	// ---------------------- SQLObject support ------------------------
	
	public String getShortDisplayName() {
		return getName();
	}
	
	public boolean allowsChildren() {
		return true;
	}

	/**
	 * Populates this schema from the source database, if there
	 * is one.  Schemas that have no parent should not need to be
	 * autopopulated, because this makes no sense.
	 * 
	 * @throws NullPointerException if this schema has no parent database.
	 */
	protected void populateImpl() throws SQLObjectException {
		if (populated) return;
		
		logger.debug("SQLSchema: populate starting");

		SQLObject tmp = getParent();
		while (tmp != null && (! (tmp instanceof SQLDatabase))) {
			tmp = tmp.getParent();
		}
		if (tmp == null) throw new IllegalStateException("Schema does not have a SQLDatabase ancestor. Can't populate.");
		SQLDatabase parentDatabase = (SQLDatabase) tmp;
		
		Connection con = null;
		ResultSet rs = null;
		try {
			synchronized (parentDatabase) {
				begin("Populating schema");
				con = parentDatabase.getConnection();
				DatabaseMetaData dbmd = con.getMetaData();
				
				if ( getParent() instanceof SQLDatabase ) {
                    List<SQLTable> fetchedTables = SQLTable.fetchTablesForTableContainer(dbmd, null, getName());
                    for (SQLTable table : fetchedTables) {
                        addTable(table);
                    }

				} else if ( getParent() instanceof SQLCatalog ) {
                    List<SQLTable> fetchedTables = SQLTable.fetchTablesForTableContainer(dbmd, getParent().getName(), getName());
                    for (SQLTable table : fetchedTables) {
                    	addTable(table);
                    }
				}
				commit();
			}
		} catch (SQLException e) {
			rollback(e.getMessage());
			throw new SQLObjectException("schema.populate.fail", e);
		} finally {
			populated = true;
			try {
				if ( rs != null ) rs.close();
			} catch (SQLException e2) {
				logger.error("Could not close result set", e2);
			}
			try {
				if ( con != null ) con.close();
			} catch (SQLException e2) {
				logger.error("Could not close connection", e2);
			}
		}
		logger.debug("SQLSchema: populate finished");
	}


	// ----------------- accessors and mutators -------------------
	
	/**
	 * Gets the value of nativeTerm
	 *
	 * @return the value of nativeTerm
	 */
	public String getNativeTerm()  {
		return this.nativeTerm;
	}

	/**
	 * Sets the value of nativeTerm to a lowercase version of argNativeTerm.
	 *
	 * @param argNativeTerm Value to assign to this.nativeTerm
	 */
	public void setNativeTerm(String argNativeTerm) {
	    String oldValue = nativeTerm;
		if (argNativeTerm != null) argNativeTerm = argNativeTerm.toLowerCase();
		this.nativeTerm = argNativeTerm;
		firePropertyChange("nativeTerm", oldValue, argNativeTerm);
	}

	@Override
	public List<SQLTable> getChildrenWithoutPopulating() {
		return Collections.unmodifiableList(tables);
	}

	@Override
	protected boolean removeChildImpl(SPObject child) {
		if (child instanceof SQLTable) {
			return removeTable((SQLTable) child);
		} else {
			throw new IllegalArgumentException("Cannot remove children of type " 
					+ child.getClass() + " from " + getName());
		}
	}
	
	public boolean removeTable(SQLTable table) {
		if (isMagicEnabled() && table.getParent() != this) {
			throw new IllegalStateException("Cannot remove child " + table.getName() + 
					" of type " + table.getClass() + " as its parent is not " + getName());
		}
		int index = tables.indexOf(table);
		if (index != -1) {
			 tables.remove(index);
			 table.setParent(null);
			 fireChildRemoved(SQLTable.class, table, index);
			 return true;
		}
		return false;
	}

	public int childPositionOffset(Class<? extends SPObject> childType) {
		if (childType == SQLTable.class) return 0;
		
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
	
	@Override
	protected void addChildImpl(SPObject child, int index) {
		if (child instanceof SQLTable) {
			addTable((SQLTable) child, index);
		} else {
			throw new IllegalArgumentException("The child " + child.getName() + 
					" of type " + child.getClass() + " is not a valid child type of " + 
					getClass() + ".");
		}
	}
	
	public void addTable(SQLTable table) {
		addTable(table, tables.size());
	}
	
	public void addTable(SQLTable table, int index) {
		tables.add(index, table);
		table.setParent(this);
		fireChildAdded(SQLTable.class, table, index);
	}

	public List<Class<? extends SPObject>> getAllowedChildTypes() {
		List<Class<? extends SPObject>> types = new ArrayList<Class<? extends SPObject>>();
		types.add(SQLTable.class);
		return Collections.unmodifiableList(types);
	}

}
