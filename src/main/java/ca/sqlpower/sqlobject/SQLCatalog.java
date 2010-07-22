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
import java.util.List;

import org.apache.log4j.Logger;

import ca.sqlpower.object.SPObject;
import ca.sqlpower.object.annotation.Accessor;
import ca.sqlpower.object.annotation.Constructor;
import ca.sqlpower.object.annotation.ConstructorParameter;
import ca.sqlpower.object.annotation.Mutator;
import ca.sqlpower.object.annotation.NonBound;
import ca.sqlpower.object.annotation.Transient;

/**
 * A SQLCatalog is a container for other SQLObjects.  If it is in the
 * containment hierarchy for a given RDBMS, it will be directly under
 * SQLDatabase.
 */
public class SQLCatalog extends SQLObject {
	private static Logger logger = Logger.getLogger(SQLCatalog.class);
	
	/**
	 * Defines an absolute ordering of the child types of this class.
	 */
	@SuppressWarnings("unchecked")
	public static final List<Class<? extends SPObject>> allowedChildTypes = 
		Collections.unmodifiableList(new ArrayList<Class<? extends SPObject>>(
				Arrays.asList(SQLSchema.class, SQLTable.class)));
	
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

	@Constructor
	public SQLCatalog(@ConstructorParameter(propertyName = "parent") SQLDatabase parent,
			@ConstructorParameter(propertyName = "name") String name) {
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
				SQLTable table = ((SQLSchema) child).findTableByName(tableName);
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
	public SQLSchema findSchemaByName(String schemaName) throws SQLObjectException {
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
	
	@Transient @Accessor
	public String getShortDisplayName() {
		return getName();
	}
	
	protected void populateImpl() throws SQLObjectException {
		if (populated) return;

		logger.debug("SQLCatalog: populate starting");
	
		final List<SQLSchema> fetchedSchemas;
        final List<SQLTable> fetchedTables;
		synchronized (getParent()) {
			Connection con = null;
			try {
				con = ((SQLDatabase) getParent()).getConnection();
				DatabaseMetaData dbmd = con.getMetaData();	
				
				// Try to find schemas in this catalog
				fetchedSchemas = SQLSchema.fetchSchemas(dbmd, getName());

				// No schemas found--fall through and check for tables
				if (fetchedSchemas.isEmpty()) {
					fetchedTables = SQLTable.fetchTablesForTableContainer(dbmd, getName(), null);
				} else {
					fetchedTables = null;
				}
				
			} catch (SQLException e) {
				throw new SQLObjectException("catalog.populate.fail", e);
			} finally {
				try {
					if (con != null) {
                        con.close();
                    }
				} catch (SQLException e) {
					throw new SQLObjectException("Couldn't close connection", e);
				}
			}
			
			runInForeground(new Runnable() {
				public void run() {
					synchronized(SQLCatalog.this) {
						if (populated) return;
						if (!fetchedSchemas.isEmpty()) {
						    populateCatalogWithList(SQLCatalog.this, fetchedSchemas);
						} else if (!fetchedTables.isEmpty()) {
                            populateCatalogWithList(SQLCatalog.this, fetchedTables);
						}
					}
				}
			});
		}
		
		logger.debug("SQLCatalog: populate finished");

	}

    /**
     * Populates the SQLCatalog with a given list of children. This must be done
     * on the foreground thread. The list of children must be of one type only
     * and of type {@link SQLSchema} or {@link SQLTable}.
     * <p>
     * Package private for use in the {@link SQLObjectUtils}.
     * 
     * @param catalog
     *            The catalog to populate
     * @param children
     *            The list of children to add as children. All objects in this
     *            list must be of the same type.
     */
    static void populateCatalogWithList(SQLCatalog catalog, List<? extends SQLObject> children) {
        try {
            
            if (children.isEmpty()) return;
            
            Class<? extends SPObject> childType;
            int index;
            if (children.get(0) instanceof SQLSchema) {
                index = catalog.schemas.size();
                childType = SQLSchema.class;
                for (SQLObject schema : children) {
                    catalog.schemas.add((SQLSchema) schema);
                    schema.setParent(catalog);
                }
            } else if (children.get(0) instanceof SQLTable) {
                index = catalog.tables.size();
                childType = SQLTable.class;
                for (SQLObject table : children) {
                    catalog.tables.add((SQLTable) table);
                    table.setParent(catalog);
                }
            } else {
                throw new IllegalArgumentException("Catalog " + catalog + " cannot have children " +
                        "of type " + children.get(0).getClass());
            }

            catalog.populated = true;
            catalog.begin("Populating Catalog " + catalog);
            for (SQLObject o : children) {
                catalog.fireChildAdded(childType, o, index);
                index++;
            }
            catalog.firePropertyChange("populated", false, true);
            catalog.commit();
        } catch (Exception e) {
            catalog.rollback(e.getMessage());
            if (children.get(0) instanceof SQLSchema) {
                for (SQLObject schema : children) {
                    catalog.schemas.remove((SQLSchema) schema);
                }
            } else if (children.get(0) instanceof SQLTable) {
                for (SQLObject table : children) {
                    catalog.tables.remove((SQLTable) table);
                }
            }
            catalog.populated = false;
            throw new RuntimeException(e);
        }
    }


	// ----------------- accessors and mutators -------------------

	@Accessor
	public SQLDatabase getParent() {
		return (SQLDatabase) super.getParent();
	}
	
	/**
	 * Because we constrained the return type on getParent there needs to be a
	 * setter that has the same constraint otherwise the reflection in the undo
	 * events will not find a setter to match the getter and won't be able to
	 * undo parent property changes.
	 */
	@Mutator
	public void setParent(SQLDatabase parent) {
		super.setParent(parent);
	}
	
	/**
	 * Gets the value of nativeTerm
	 *
	 * @return the value of nativeTerm
	 */
	@Accessor
	public String getNativeTerm()  {
		return this.nativeTerm;
	}

	/**
	 * Sets the value of nativeTerm to a lowercase version of argNativeTerm.
	 *
	 * @param argNativeTerm Value to assign to this.nativeTerm
	 */
	@Mutator
	public void setNativeTerm(String argNativeTerm) {
		if (argNativeTerm != null) argNativeTerm = argNativeTerm.toLowerCase();
		String oldValue = nativeTerm;
		this.nativeTerm = argNativeTerm;
		firePropertyChange("nativeTerm", oldValue, nativeTerm);
	}

	/**
	 * Determines whether this SQL object is a container for schemas
	 *
	 * @return true (the default) if there are no children; false if
	 * the first child is not of type SQLSchema.
	 */
	@NonBound
	public boolean isSchemaContainer() throws SQLObjectException {
		if (getParent() != null){
			populate();
		}
	
		// catalog has been populated
	
		if (getChildrenWithoutPopulating().isEmpty()) {
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
			fireChildRemoved(SQLSchema.class, schema, index);
			schema.setParent(null);
			return true;
		}
		return false;
	}
	
	public boolean removeTable(SQLTable table) {
		table.removeNotify();
		int index = tables.indexOf(table);
		if (index != -1) {
			tables.remove(index);
			fireChildRemoved(SQLTable.class, table, index);
			table.setParent(null);
			return true;
		}
		return false;
	}

	public List<? extends SPObject> getDependencies() {
		return Collections.emptyList();
	}

	public void removeDependency(SPObject dependency) {
		for (SQLObject child : getChildren()) {
			child.removeDependency(dependency);
		}
	}

	public List<Class<? extends SPObject>> getAllowedChildTypes() {
		return allowedChildTypes;
	}
	
}
