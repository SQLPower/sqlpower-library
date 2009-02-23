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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

/**
 * A SQLCatalog is a container for other SQLObjects.  If it is in the
 * containment hierarchy for a given RDBMS, it will be directly under
 * SQLDatabase.
 */
public class SQLCatalog extends SQLObject {
	private static Logger logger = Logger.getLogger(SQLCatalog.class);

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
        this.children = new LinkedList();
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
		Iterator childit = children.iterator();
		while (childit.hasNext()) {
			SQLObject child = (SQLObject) childit.next();
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
		Iterator childit = children.iterator();
		while (childit.hasNext()) {
			SQLSchema schema = (SQLSchema) childit.next();
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

	public void populateImpl() throws SQLObjectException {
		if (populated) return;

		logger.debug("SQLCatalog: populate starting");
	
		int oldSize = children.size();
		synchronized (getParent()) {
			String oldCatalog = null;
			Connection con = null;
			ResultSet rs = null;
			try {
			
				con = ((SQLDatabase) getParent()).getConnection();
				DatabaseMetaData dbmd = con.getMetaData();	
				oldCatalog = con.getCatalog();
                
                // This can fail in SS2K because of privelege problems.  There is
                // apparently no way to check if it will fail; you just have to try.
                try {
                    con.setCatalog(getName());
                } catch (SQLException ex) {
                    logger.info("populate: Could not setCatalog("+getName()+"). Assuming it's a permission problem.  Stack trace:", ex);
                    return;
                }
				
				rs = dbmd.getSchemas();
				while (rs.next()) {
					String schName = rs.getString(1);
					SQLSchema schema = null;

					if (schName != null) {
						schema = new SQLSchema(this, schName, false);
						children.add(schema);
						schema.setNativeTerm(dbmd.getSchemaTerm());
						logger.debug("Set schema term to "+schema.getNativeTerm());
					}
				}
				rs.close();
				rs = null;
				
				if (oldSize == children.size()) {
				    List<SQLTable> tables = SQLTable.fetchTablesForTableContainer(dbmd, getName(), null);
				    for (SQLTable table : tables) {
				        table.setParent(this);
				        children.add(table);
				    }
				}
				
			} catch (SQLException e) {
				throw new SQLObjectException("catalog.populate.fail", e);
			} finally {
				populated = true;
				int newSize = children.size();
				if (newSize > oldSize) {
					int[] changedIndices = new int[newSize - oldSize];
					for (int i = 0, n = newSize - oldSize; i < n; i++) {
						changedIndices[i] = oldSize + i;
					}
					fireDbChildrenInserted(changedIndices, children.subList(oldSize, newSize));
				}
				try {
					if (rs != null) rs.close();
				} catch (SQLException e2) {
					throw new SQLObjectException("catalog.rs.close.fail", e2);
				}
				try {
					if (con != null) {
                        con.setCatalog(oldCatalog);
                        con.close();
                    }
				} catch (SQLException e2) {
					throw new SQLObjectException("Couldn't close connection", e2);
				}
			}
		}
		
		logger.debug("SQLCatalog: populate finished");

	}


	// ----------------- accessors and mutators -------------------

	public SQLDatabase getParentDatabase() {
		return (SQLDatabase) getParent();
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
		fireDbObjectChanged("nativeTerm", oldValue, nativeTerm);
	}

	@Override
	public Class<? extends SQLObject> getChildType() {
		if (children.size() == 0){
			return null;
		}
		else{
			return ((SQLObject)children.get(0)).getClass();
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
	
		if (children.size() == 0) {
			return true;
		} else {
			return (children.get(0) instanceof SQLSchema);
		}
	}
}
