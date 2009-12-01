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
import java.beans.PropertyChangeListener;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.dbcp.ConnectionFactory;
import org.apache.commons.dbcp.PoolableConnectionFactory;
import org.apache.commons.pool.BaseObjectPool;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.commons.pool.impl.GenericObjectPool.Config;
import org.apache.log4j.Logger;

import ca.sqlpower.object.ObjectDependentException;
import ca.sqlpower.object.SPObject;
import ca.sqlpower.sql.JDBCDSConnectionFactory;
import ca.sqlpower.sql.JDBCDataSource;
import ca.sqlpower.sql.SPDataSource;
import ca.sqlpower.sql.jdbcwrapper.DatabaseMetaDataDecorator;

public class SQLDatabase extends SQLObject implements java.io.Serializable, PropertyChangeListener {
	private static Logger logger = Logger.getLogger(SQLDatabase.class);

	/**
	 * This SPDataSource describes how to connect to the 
	 * physical database that backs this SQLDatabase object.
	 */
	private JDBCDataSource dataSource;

	/**
	 * A pool of JDBC connections backed by a Jakarta Commons DBCP pool.
	 * You should access it only via the getConnectionPool() method.
	 */
	private transient BaseObjectPool connectionPool;
	
	/**
	 * Tells this database that it is being used to back the PlayPen.  Also 
	 * stops removal of children and the closure of connection when properties
	 * change.
	 */
	private boolean playPenDatabase = false;
	
	/**
	 * Indicates the maximum number of connections held active ever.
	 */
	private int maxActiveConnections = 0;
	
	/**
	 * The catalog term for the underlying database, according to the JDBC driver's
	 * database meta data at the time this database object was populated. Null means
	 * the database does not have catalogs.
	 */
	private String catalogTerm;
	
    /**
     * The schema term for the underlying database, according to the JDBC driver's
     * database meta data at the time this database object was populated. Null means
     * the database does not have schemas.
     */
	private String schemaTerm;
	
	/**
	 * The {@link SQLCatalog} children of this {@link SQLDatabase}.
	 */
	private final List<SQLCatalog> catalogs = new ArrayList<SQLCatalog>();
	
	private final List<SQLSchema> schemas = new ArrayList<SQLSchema>();
	
	private final List<SQLTable> tables = new ArrayList<SQLTable>();
	
	/**
	 * Constructor for instances that connect to a real database by JDBC.
	 */
	public SQLDatabase(JDBCDataSource dataSource) {
		setDataSource(dataSource);
	}
	
	/**
	 * Constructor for non-JDBC connected instances.
	 */
	public SQLDatabase() {
		populated = true;
	}

	public synchronized boolean isConnected() {
		return connectionPool != null;
	}

	protected synchronized void populateImpl() throws SQLObjectException {
	    logger.debug("SQLDatabase: is populated " + populated); //$NON-NLS-1$
		if (populated) return;
		int oldSize = getChildrenWithoutPopulating().size();
		
		logger.debug("SQLDatabase: populate starting"); //$NON-NLS-1$
		
		Connection con = null;
		ResultSet rs = null;
		try {
			begin("Populating Database " + this);
			con = getConnection();
			DatabaseMetaData dbmd = con.getMetaData();
			
			catalogTerm = dbmd.getCatalogTerm();
			if ("".equals(catalogTerm)) catalogTerm = null; //$NON-NLS-1$
			
			schemaTerm = dbmd.getSchemaTerm();
			if ("".equals(schemaTerm)) schemaTerm = null; //$NON-NLS-1$
			
			List<SQLCatalog> fetchedCatalogs = SQLCatalog.fetchCatalogs(dbmd);
			for (SQLCatalog cat : fetchedCatalogs) {
				addCatalog(cat);
			}

			// If there were no catalogs, we should look for schemas
			// instead (i.e. this database has no catalogs, and schemas
            // may be attached directly to the database)
			if (getChildrenWithoutPopulating().size() == oldSize) {
				List<SQLSchema> fetchedSchemas = SQLSchema.fetchSchemas(dbmd, null);
				for (SQLSchema schema : fetchedSchemas) {
					addSchema(schema);
				}
			}
            
            // Finally, look for tables directly under the database (this
            // could be a platform without catalogs or schemas at all)
            if (getChildrenWithoutPopulating().size() == oldSize) {
                List<SQLTable> fetchedTables = SQLTable.fetchTablesForTableContainer(dbmd, "", ""); //$NON-NLS-1$ //$NON-NLS-2$
                for (SQLTable table : fetchedTables) {
                	addTable(table);
                }
            }
            commit();
            
		} catch (SQLException e) {
			rollback(e.getMessage());
			throw new SQLObjectException(Messages.getString("SQLDatabase.populateFailed"), e); //$NON-NLS-1$
		} finally {
			populated = true;
			try {
				if (rs != null ) rs.close();
			} catch (SQLException e2) {
				throw new SQLObjectException(Messages.getString("SQLDatabase.closeRSFailed"), e2); //$NON-NLS-1$
			}
			try {
				if (con != null ) con.close();
			} catch (SQLException e2) {
				throw new SQLObjectException(Messages.getString("SQLDatabase.closeConFailed"), e2); //$NON-NLS-1$
			}
		}
		
		logger.debug("SQLDatabase: populate finished"); //$NON-NLS-1$
	}
	
	public SQLCatalog getCatalogByName(String catalogName) throws SQLObjectException {
		populate();
		if (getChildrenWithoutPopulating().isEmpty()) {
			return null;
		}
		if (catalogs.isEmpty()) {
			// this database doesn't contain catalogs!
			return null;
		}
		for (SQLCatalog child : catalogs) {
			if (child.getName().equalsIgnoreCase(catalogName)) {
				return child;
			}
		}
		return null;
	}

	/**
	 * Searches for the named schema as a direct child of this
	 * database, or as a child of any catalog of this database.
	 *
	 * <p>Note: there may be more than one schema with the given name,
	 * if your RDBMS supports catalogs.  In that case, use {@link
	 * SQLCatalog#getSchemaByName} or write another version of this
	 * method that return an array of SQLSchema.
	 *
	 * @return the first SQLSchema whose name matches the given schema
	 * name.
	 */
	public SQLSchema getSchemaByName(String schemaName) throws SQLObjectException {
		populate();
		if (getChildrenWithoutPopulating().isEmpty()) {
			return null;
		}
		if (schemas.isEmpty() && catalogs.isEmpty()) {
			// this database doesn't contain schemas or catalogs!
			return null;
		}
		for (SQLObject child : getChildren()) {
			if (child instanceof SQLCatalog) {
				// children are tables or schemas
				SQLSchema schema = ((SQLCatalog) child).getSchemaByName(schemaName);
				if (schema != null) {
					return schema;
				}
			} else if (child instanceof SQLSchema) {
				boolean match = (child.getName() == null ?
								 schemaName == null :
								 child.getName().equalsIgnoreCase(schemaName)); 
				if (match) {
					return (SQLSchema) child;
				}
			} else {
				throw new IllegalStateException("Database contains a mix of schemas or catalogs with other objects"); //$NON-NLS-1$
			}
		}
		return null;
	}

	public SQLTable getTableByName(String tableName) throws SQLObjectException {
		return getTableByName(null, null, tableName);
	}

	/**
	 * Searches this database's list of tables for one with the given
	 * name, ignoring case because SQL isn't (usually) case sensitive.
	 *
	 * @param catalogName The name of the catalog to search, or null
	 * if you want to search all catalogs.
	 * @param schemaName The name of the schema to search (in this
	 * database or in the given catalog) or null to search all
	 * schemas.
	 * @param tableName The name of the table to look for (null is not
	 * allowed).
	 * @return the first SQLTable with the given name, or null if no
	 * such table exists.
	 */
	public SQLTable getTableByName(String catalogName, String schemaName, String tableName)
		throws SQLObjectException {

		this.populate();

		if (tableName == null || tableName.length() == 0) {
			throw new NullPointerException("Table Name must be specified"); //$NON-NLS-1$
		}
		
		// we will recursively search a target (database, catalog, or schema)
		SQLObject target = this;
		
		if (catalogName != null && catalogName.length() > 0 ) {
			target = getCatalogByName(catalogName);
		}

		// no such catalog?
		if (target == null) {
		    if (logger.isDebugEnabled())
		        logger.debug("getTableByName("+catalogName+","+schemaName+","+tableName+"): no such catalog!"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			return null;
		}

		if (schemaName != null && schemaName.length() > 0) {
			if (target instanceof SQLDatabase) {
				target = ((SQLDatabase) target).getSchemaByName(schemaName);
			} else if (target instanceof SQLCatalog) {
				target = ((SQLCatalog) target).getSchemaByName(schemaName);
			} else {
				throw new IllegalStateException("Oops, somebody forgot to update this!"); //$NON-NLS-1$
			}
		}

		// no such schema or catalog.schema?
		if (target == null) {
		    if (logger.isDebugEnabled())
		        logger.debug("getTableByName("+catalogName+","+schemaName+","+tableName+"): no such schema!"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			return null;
		}
		target.populate();		
		for (SQLObject child : target.getChildren()) {
			if (child instanceof SQLTable) {
				SQLTable table = (SQLTable) child;
				if (table.getName().equalsIgnoreCase(tableName)) {
					return table;
				}
			} else if (child instanceof SQLCatalog) {
				SQLTable table = ((SQLCatalog) child).getTableByName(tableName);
				if (table != null) {
					return table;
				}
			} else if (child instanceof SQLSchema) {
				SQLTable table = ((SQLSchema) child).getTableByName(tableName);
				if (table != null) {
					return table;
				}
			}
		}

		if (logger.isDebugEnabled())
	        logger.debug("getTableByName("+catalogName+","+schemaName+","+tableName+"): catalog and schema ok; no such table!"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		return null;
	}

	// ---------------------- SQLObject support ------------------------

	@Override
	public String getName() {
		if (isPlayPenDatabase()) {
			return Messages.getString("SQLDatabase.playPenDB"); //$NON-NLS-1$
		} else if (dataSource != null) {
		    return dataSource.getDisplayName();
		} else {
		    return Messages.getString("SQLDatabase.disconnected"); //$NON-NLS-1$
		}
	}
	/**
	 * Sets the data source name if the data source is not null
	 */
	@Override
	public void setName(String argName)
	{
		if (dataSource != null) {
			dataSource.setName(argName);
		}
		
	}

	public String getShortDisplayName() {
		return getName();
	}
	
	public boolean allowsChildren() {
		return true;
	}

	/**
	 * Determines whether this SQL object is a container for catalog
	 *
	 * @return true (the default) if there are no children; false if
	 * the first child is not of type SQLCatlog.
	 */
	public boolean isCatalogContainer() throws SQLObjectException {
		if (getParent() != null){
			populate();
		}
		
		return (getChildrenWithoutPopulating().isEmpty() || !catalogs.isEmpty());
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
		return (getChildrenWithoutPopulating().isEmpty() || !schemas.isEmpty());
	}

	// ----------------- accessors and mutators -------------------

    /**
     * Recursively searches this database for SQLTable descendants, compiles a
     * list of those that were found, and returns that list.
     * 
     * @return a list of all the tables in this database (which may exist under
     *         catalogs and schemas). You are free to modify the returned list,
     *         but doing so will not affect the contents of this database.
     */
	public List<SQLTable> getTables() throws SQLObjectException {
		return getTableDescendants(this);
	}

    /**
     * This is the recursive subroutine used by {@link #getTables}. It is
     * preferable to use this algorithm to discover all the tables rather than
     * the generic {@link SQLObjectUtils#findDescendentsByClass(SQLObject, Class, List)}
     * because this one does not cause all the tables in the database to populate.
     */
	private static List<SQLTable> getTableDescendants(SQLObject o) throws SQLObjectException {

		// this seemingly redundant short-circuit is required because
		// we don't want o.getChildren() to be null
		if (!o.allowsChildren()) return Collections.emptyList();

		List<SQLTable> tables = new LinkedList<SQLTable>();
		for (SQLObject c : o.getChildren()) {
			if (c instanceof SQLTable) {
				tables.add((SQLTable) c);
			} else {
				tables.addAll(getTableDescendants(c));
			}
		}
		return tables;
	}

	/**
	 * Gets the value of dataSource
	 *
	 * @return the value of dataSource
	 */
	public JDBCDataSource getDataSource()  {
		return this.dataSource;
	}

	/**
	 * Sets the value of dataSource
	 *
	 * @param argDataSource Value to assign to this.dataSource
	 */
	public void setDataSource(JDBCDataSource argDataSource) {
		SPDataSource oldDataSource = this.dataSource;
		begin("Resetting Database");
		if (dataSource != null) {
			dataSource.removePropertyChangeListener(this);
			if (isMagicEnabled()) {
				reset();
			}
		}
		dataSource = argDataSource;
		dataSource.addPropertyChangeListener(this);		
		firePropertyChange("dataSource",oldDataSource,argDataSource); //$NON-NLS-1$
		commit();
	}

	public void setPlayPenDatabase(boolean v) {
		boolean oldValue = playPenDatabase;
		playPenDatabase = v;

		if (oldValue != v) {
			firePropertyChange("playPenDatabase", oldValue, v); //$NON-NLS-1$
		}
	}

	public boolean isPlayPenDatabase() {
		return playPenDatabase;
	}

	/**
	 * Removes all children, closes and discards the JDBC connection.  
	 * Unless {@link #playPenDatabase} is true
	 */
	protected synchronized void reset() {
		
		if (playPenDatabase) {
			// preserve the objects that are in the Target system when
            // the connection spec changes
			logger.debug("Ignoring Reset request for: " + getDataSource()); //$NON-NLS-1$
			populated = true;
		} else {
			// discard everything and reload (this is generally for source systems)
			logger.debug("Resetting: " + getDataSource() ); //$NON-NLS-1$
			// tear down old connection stuff
			try {
				begin("Resetting Database " + this);
				for (int i = getChildrenWithoutPopulating().size()-1; i >= 0; i--) {
					removeChild(getChildrenWithoutPopulating().get(i));
				}
				populated = false;
				commit();
			} catch (IllegalArgumentException e) {
				rollback(e.getMessage());
				throw new RuntimeException(e);
			} catch (ObjectDependentException e) {
				rollback(e.getMessage());
				throw new RuntimeException(e);
			}
		}
		
		// destroy connection pool in either case (it still points to the old data source)
		if (connectionPool != null) {
			try {
				connectionPool.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		connectionPool = null;
	}

	/**
	 * Listens for changes in DBCS properties, and resets this
	 * SQLDatabase if a critical property (url, driver, username)
	 * changes.
	 */
	public void propertyChange(PropertyChangeEvent e) {		
		String pn = e.getPropertyName();
		if ( (e.getOldValue() == null && e.getNewValue() != null)
			 || (e.getOldValue() != null && e.getNewValue() == null)
			 || (e.getOldValue() != null && e.getNewValue() != null 
				 && !e.getOldValue().equals(e.getNewValue())) ) {
			if ("url".equals(pn) || "driverClass".equals(pn) || "user".equals(pn)) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				reset();
			} else if ("name".equals(pn)) {				 //$NON-NLS-1$
				firePropertyChange("shortDisplayName",e.getOldValue(),e.getNewValue()); //$NON-NLS-1$
			}
		}
	}

	/**
	 * Returns a JDBC connection to the backing database, if there
	 * is one.  The connection that you get will be yours and only yours
	 * until you call close() on it.  To maximize efficiency of the pool,
	 * try to call close() as soon as you are done with the connection.
	 *
	 * @return an open connection if this database has a valid
	 * dataSource; null if this is a dummy database (such as the
	 * playpen instance).
	 */
	public Connection getConnection() throws SQLObjectException {
		if (dataSource == null) {
			return null;
		} else {
			try {
			    int newActiveCount = getConnectionPool().getNumActive() + 1;
                maxActiveConnections = Math.max(maxActiveConnections,
			            newActiveCount);
			    if (logger.isDebugEnabled()) {
			        logger.debug("getConnection(): giving out active connection " + newActiveCount); //$NON-NLS-1$
			        for (StackTraceElement ste : Thread.currentThread().getStackTrace()) {
			        	logger.debug(ste.toString());
			        }
			    }
				return (Connection) getConnectionPool().borrowObject();
			} catch (Exception e) {
			    SQLObjectException ex = new SQLObjectException(
			            "Couldn't connect to database: "+e.getMessage(), e); //$NON-NLS-1$
			    setChildrenInaccessibleReason(ex, true);
			    throw new AssertionError("Unreachable code");
			}
		}
	}

	public String toString() {
		return getName();
	}
	
	/**
	 * Closes all connections and other resources that were allocated
	 * by the connect() method.  Logs, but does not propogate, SQL exceptions.
	 */
	public void disconnect() {
		try {
			if (connectionPool != null){
				connectionPool.close();
            }
		} catch (Exception ex) {
			logger.error("Error closing connection pool", ex); //$NON-NLS-1$
		} finally {
			connectionPool = null;
		}
		maxActiveConnections = 0;
	}
	
	synchronized BaseObjectPool getConnectionPool() {
		if (connectionPool == null) {
            Config poolConfig = new GenericObjectPool.Config();
            poolConfig.maxActive = 5;
            poolConfig.whenExhaustedAction = GenericObjectPool.WHEN_EXHAUSTED_FAIL;
			connectionPool = new GenericObjectPool(null, poolConfig);
			ConnectionFactory cf = new JDBCDSConnectionFactory(dataSource);			
			new PoolableConnectionFactory(cf, connectionPool, null,
					null, false, true);
		}
		return connectionPool;
	}

	/**
	 * Returns the maximum number of active connections that
	 * this database has ever opened. 
	 * @return Maximum number of active connections ever opened.
	 */
    public int getMaxActiveConnections() {
        return maxActiveConnections;
    }

    /**
     * Re-reads the definition of all populated objects for this entire
     * database. This involves a lot of recursion and re-populating. When
     * SQLObjects are added, removed, or modified as a result of this process,
     * they will fire the appropriate SQLObject events. The entire operation
     * happens in the context of a single compound event.
     * <p>
     * The refresh will not cause any additional SQLObjects to become populated,
     * although it may end up removing some objects that were already populated
     * (because they were dropped in the physical database).
     */
    public void refresh() throws SQLObjectException {
        if (!populated) {
            logger.info("Not refreshing unpopulated database " + getName()); //$NON-NLS-1$
            return;
        }

        DatabaseMetaDataDecorator.putHint(DatabaseMetaDataDecorator.CACHE_STALE_DATE, new Date());

        // We're going to just leave caching on all the time and see how it pans out
        DatabaseMetaDataDecorator.putHint(
                DatabaseMetaDataDecorator.CACHE_TYPE,
                DatabaseMetaDataDecorator.CacheType.EAGER_CACHE);

        Connection con = null;
        try {
            begin(Messages.getString("SQLDatabase.refreshDatabase", getName())); //$NON-NLS-1$

            con = getConnection();
            DatabaseMetaData dbmd = con.getMetaData();
            
            if (catalogTerm != null) {
                logger.debug("refresh: catalogTerm is '"+catalogTerm+"'. refreshing catalogs!"); //$NON-NLS-1$ //$NON-NLS-2$
                List<SQLCatalog> newCatalogs = SQLCatalog.fetchCatalogs(dbmd);
                SQLObjectUtils.refreshChildren(this, newCatalogs, SQLCatalog.class);
            } else if (schemaTerm != null) {
                logger.debug("refresh: schemaTerm is '"+schemaTerm+"'. refreshing schemas!"); //$NON-NLS-1$ //$NON-NLS-2$
                List<SQLSchema> newSchemas = SQLSchema.fetchSchemas(dbmd, null);
                SQLObjectUtils.refreshChildren(this, newSchemas, SQLSchema.class);
            }
            
            // close connection before invoking the super refresh,
            // which will probably want to open another one
            con.close();
            con = null;
            
            // bootstrap: if this database is a catalog or schema container, 
            super.refresh();

        } catch (SQLException e) {
            throw new SQLObjectException(Messages.getString("SQLDatabase.refreshFailed"), e); //$NON-NLS-1$
        } finally {
            commit();
            
            try {
                if (con != null) con.close();
            } catch (SQLException ex) {
                logger.warn("Failed to close connection. Squishing this exception:", ex); //$NON-NLS-1$
            }
        }
    }

    /**
     * Returns all the relationships under this database. Beware of calling this
     * method if this SQLDatabase instance is lazy-loading from a physical
     * database, because it will cause all objects underneath to fully populate!
     * 
     * @return a collection of all the SQLRelationship objects that exist within
     *         this database.
     * @throws SQLObjectException
     *             if this is a lazy-loading database and populating any of its
     *             objects fails for any reason.
     */
    public Collection<SQLRelationship> getRelationships() throws SQLObjectException {
        List<SQLRelationship> allRelationships =
            SQLObjectUtils.findDescendentsByClass(
                this, SQLRelationship.class, new ArrayList<SQLRelationship>());

        // relationships appear in two places within the SQLObject tree, so
        // we have to uniquify the list before returning it
        Set<SQLRelationship> uniqueRelationships = new HashSet<SQLRelationship>(allRelationships);
        
        return uniqueRelationships;
    }
	
	public List<? extends SQLObject> getChildrenWithoutPopulating() {
		List<SQLObject> children = new ArrayList<SQLObject>();
		children.addAll(catalogs);
		children.addAll(schemas);
		children.addAll(tables);
		return Collections.unmodifiableList(children);
	}

	@Override
	protected boolean removeChildImpl(SPObject child) {
		if (child instanceof SQLCatalog) {
			return removeCatalog((SQLCatalog) child);
		} else if (child instanceof SQLSchema) {
			return removeSchema((SQLSchema) child);
		} else if (child instanceof SQLTable) {
			return removeTable((SQLTable) child);
		} else {
			throw new IllegalArgumentException("Cannot remove children of type " 
					+ child.getClass() + " from " + getName());
		}
	}
	
	public boolean removeCatalog(SQLCatalog child) {
		if (child.getParent() != this) {
			throw new IllegalStateException("Cannot remove child " + child.getName() + 
					" of type " + child.getClass() + " as its parent is not " + getName());
		}
		int index = catalogs.indexOf(child);
		if (index != -1) {
			 catalogs.remove(index);
			 child.setParent(null);
			 fireChildRemoved(SQLCatalog.class, child, index);
			 return true;
		}
		return false;
	}
	
	public boolean removeSchema(SQLSchema child) {
		if (child.getParent() != this) {
			throw new IllegalStateException("Cannot remove child " + child.getName() + 
					" of type " + child.getClass() + " as its parent is not " + getName());
		}
		int index = schemas.indexOf(child);
		if (index != -1) {
			 schemas.remove(index);
			 fireChildRemoved(SQLSchema.class, child, index);
			 child.setParent(null);
			 return true;
		}
		return false;
	}
	
	public boolean removeTable(SQLTable child) {
		if (child.getParent() != this) {
			throw new IllegalStateException("Cannot remove child " + child.getName() + 
					" of type " + child.getClass() + " as its parent is not " + getName());
		}
		int index = tables.indexOf(child);
		if (index != -1) {
			 tables.remove(index);
			 fireChildRemoved(SQLTable.class, child, index);
			 child.setParent(null);
			 return true;
		}
		return false;
	}

	public int childPositionOffset(Class<? extends SPObject> childType) {
		int offset = 0;
		
		if (childType == SQLCatalog.class) return offset;
		offset += catalogs.size();
		
		if (childType == SQLSchema.class) return offset;
		offset += schemas.size();
		
		if (childType == SQLTable.class) return offset;
		
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
		if (child instanceof SQLCatalog) {
			addCatalog((SQLCatalog) child, index);
		} else if (child instanceof SQLSchema) {
			addSchema((SQLSchema) child, index);
		} else if (child instanceof SQLTable) {
			addTable((SQLTable) child, index);
		} else {
			throw new IllegalArgumentException("The child " + child.getName() + 
					" of type " + child.getClass() + " is not a valid child type of " + 
					getClass() + ".");
		}
	}
	
	public void addCatalog(SQLCatalog catalog) {
		addCatalog(catalog, catalogs.size());
	}
	
	public void addCatalog(SQLCatalog catalog, int index) {
		catalogs.add(index, catalog);
		catalog.setParent(this);
		fireChildAdded(SQLCatalog.class, catalog, index);
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

	public List<Class<? extends SPObject>> getAllowedChildTypes() {
		List<Class<? extends SPObject>> types = new ArrayList<Class<? extends SPObject>>();
		types.add(SQLCatalog.class);
		types.add(SQLSchema.class);
		types.add(SQLTable.class);
		return Collections.unmodifiableList(types);
	}
}
