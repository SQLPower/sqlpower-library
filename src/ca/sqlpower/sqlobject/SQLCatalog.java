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
 * A SQLCatalog is a container for other SQLObjects.  If it is in the
 * containment hierarchy for a given RDBMS, it will be directly under
 * SQLDatabase.
 */
public class SQLCatalog extends SQLObject {
	private static Logger logger = Logger.getLogger(SQLCatalog.class);
	
	private List<SQLSchema> schemas = new ArrayList<SQLSchema>();
	
	private List<SQLTable> tables = new ArrayList<SQLTable>();

	static List<SQLCatalog> fetchCatalogs(DatabaseMetaData dbmd) throws SQLObjectException {
	    ResultSet rs = null;
	    try {
	        List<SQLCatalog> catalogs = new ArrayList<SQLCatalog>();
	        rs = dbmd.getCatalogs();
            while (rs.next()) {
                String catName = rs.getString(1);
                if (catName != null) {
                    SQLCatalog cat = new SQLCatalog(null, catName);
                    cat.setNativeTerm(dbmd.getCatalogTerm());
                    logger.debug("Set catalog term to "+cat.getNativeTerm());
                    catalogs.add(cat);
                }
            }
            return catalogs;
	    } catch (SQLException ex) {
	        throw new SQLObjectException("Failed to get catalog names from source database", ex);
	    } finally {
            try {
                if (rs != null ) rs.close();
            } catch (SQLException e) {
                throw new SQLObjectException("Couldn't close result set. Squishing this exception:", e);
            }
	    }
	}
	
	/**
	 * The term used for catalogs in the native database system.  In
	 * SQLServer2000, this is "database".
	 */
	protected String nativeTerm;

	public SQLCatalog() {
		this(null, null, false);
	}

	public SQLCatalog(SQLDatabase parent, String name) {
        this(parent, name, false);
	}
    
    public SQLCatalog(SQLDatabase parent, String name, boolean startPopulated) {
        setParent(parent);
        setName(name);
        this.nativeTerm = "catalog";
        this.populated = startPopulated;
    }
    
    @Override
    public void updateToMatch(SQLObject source) throws SQLObjectException {
        SQLCatalog c = (SQLCatalog) source;
        setName(c.getName());
        setNativeTerm(c.getNativeTerm());
        setPhysicalName(c.getPhysicalName());
    }

	protected SQLTable getTableByName(String tableName) throws SQLObjectException {
		populate();
		for (SQLObject child : getChildren()) {
			if (child instanceof SQLTable) {
				SQLTable table = (SQLTable) child;
				if (table.getName().equalsIgnoreCase(tableName)) {
					return table;
				}
			} else if (child instanceof SQLSchema) {
				SQLTable table = ((SQLSchema) child).getTableByName(tableName);
				if (table != null) {
					return table;
				}
			}
		}
		return null;
	}

	/**
	 *
	 * @return The schema in this catalog with the given name, or null
	 * if no such schema exists.
	 */
	public SQLSchema getSchemaByName(String schemaName) throws SQLObjectException {
		populate();
		if (!isSchemaContainer()) {
			return null;
		}
		for (SQLSchema schema : schemas) {
			if (schema.getName().equalsIgnoreCase(schemaName)) {
				return schema;
			}
		}
		return null;
	}

	public String toString() {
		return getShortDisplayName();
	}

	// ---------------------- SQLObject support ------------------------
	
	public String getShortDisplayName() {
		return getName();
	}
	
	public boolean allowsChildren() {
		return true;
	}

	protected void populateImpl() throws SQLObjectException {
		if (populated) return;

		logger.debug("SQLCatalog: populate starting");
	
		int oldSize = getChildrenWithoutPopulating().size();
		synchronized (getParent()) {
			Connection con = null;
			try {
				fireTransactionStarted("Populating Catalog " + this);
				con = ((SQLDatabase) getParent()).getConnection();
				DatabaseMetaData dbmd = con.getMetaData();	
				
				// Try to find schemas in this catalog
				List<SQLSchema> fetchedSchemas = SQLSchema.fetchSchemas(dbmd, getName());
				for (SQLSchema schema : fetchedSchemas) {
				    addSchema(schema);
				}

				// No schemas found--fall through and check for tables
				if (oldSize == getChildren().size()) {
				    List<SQLTable> fetchedTables = SQLTable.fetchTablesForTableContainer(dbmd, getName(), null);
				    for (SQLTable table : fetchedTables) {
				    	addTable(table);
				    }
				}
				
				fireTransactionEnded();
			} catch (SQLException e) {
				fireTransactionRollback(e.getMessage());
				throw new SQLObjectException("catalog.populate.fail", e);
			} finally {
				populated = true;
				try {
					if (con != null) {
                        con.close();
                    }
				} catch (SQLException e) {
					throw new SQLObjectException("Couldn't close connection", e);
				}
			}
		}
		
		logger.debug("SQLCatalog: populate finished");

	}


	// ----------------- accessors and mutators -------------------

	public SQLDatabase getParent() {
		return (SQLDatabase) super.getParent();
	}

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
		if (argNativeTerm != null) argNativeTerm = argNativeTerm.toLowerCase();
		String oldValue = nativeTerm;
		this.nativeTerm = argNativeTerm;
		firePropertyChange("nativeTerm", oldValue, nativeTerm);
	}

	@Override
	public Class<? extends SQLObject> getChildType() {
		if (getChildren().size() == 0){
			return null;
		} else {
			return (schemas.isEmpty()? SQLTable.class : SQLSchema.class);
		}
	}
	
	/**
	 * Determines whether this SQL object is a container for schemas
	 *
	 * @return true (the default) if there are no children; false if
	 * the first child is not of type SQLSchema.
	 */
	public boolean isSchemaContainer() throws SQLObjectException {
		if (getParent() != null){
			populate();
		}
	
		// catalog has been populated
	
		if (getChildren().size() == 0) {
			return true;
		} else {
			return !schemas.isEmpty();
		}
	}
	
	@Override
	protected void addChildImpl(SPObject child, int index) {
		if (child instanceof SQLSchema) {
			addSchema((SQLSchema) child, index);
		} else if (child instanceof SQLTable) {
			addTable((SQLTable) child, index);
		} else {
			throw new IllegalArgumentException("The child " + child.getName() + 
					" of type " + child.getClass() + " is not a valid child type of " + 
					getClass() + ".");
		}
	}
	
	public void addSchema(SQLSchema schema) {
		addSchema(schema, schemas.size());
	}
	
	public void addSchema(SQLSchema schema, int index) {
		schemas.add(index, schema);
		schema.setParent(this);
		fireChildAdded(SQLSchema.class, schema, index);
	}
	
	public void addTable(SQLTable table) {
		addTable(table, tables.size());
	}
	
	public void addTable(SQLTable table, int index) {
		tables.add(index, table);
		table.setParent(this);
		fireChildAdded(SQLTable.class, table, index);
	}

	@Override
	public List<? extends SQLObject> getChildren() {
		try {
			populate();
			return getChildrenWithoutPopulating();
		} catch (SQLObjectException e) {
			throw new SQLObjectRuntimeException(e);
		}
	}
	
	public List<? extends SQLObject> getChildrenWithoutPopulating() {
		List<SQLObject> children = new ArrayList<SQLObject>();
		children.addAll(schemas);
		children.addAll(tables);
		return Collections.unmodifiableList(children);
	}

	@Override
	protected boolean removeChildImpl(SPObject child) {
		if (child instanceof SQLSchema) {
			return removeSchema((SQLSchema) child);
		} else if (child instanceof SQLTable) {
			return removeTable((SQLTable) child);
		}
		return false;
	}
	
	public boolean removeSchema(SQLSchema schema) {
		int index = schemas.indexOf(schema);
		if (index != -1) {
			schemas.remove(index);
			schema.setParent(null);
			fireChildRemoved(SQLSchema.class, schema, index);
			return true;
		}
		return false;
	}
	
	public boolean removeTable(SQLTable table) {
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
		// TODO Auto-generated method stub
		return 0;
	}

	public List<? extends SPObject> getDependencies() {
		return Collections.emptyList();
	}

	public void removeDependency(SPObject dependency) {
		for (SQLObject child : getChildren()) {
			child.removeDependency(dependency);
		}
	}
	
}
