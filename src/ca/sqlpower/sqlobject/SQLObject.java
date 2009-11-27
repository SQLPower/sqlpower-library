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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;
import ca.sqlpower.object.AbstractSPObject;
import ca.sqlpower.object.ObjectDependentException;
import ca.sqlpower.object.SPListener;
import ca.sqlpower.object.SPObject;
import ca.sqlpower.sql.jdbcwrapper.DatabaseMetaDataDecorator;

/**
 * SQLObject is the main base class of the Architect API. All objects that can
 * be reverse-engineered from or forward-engineered to an SQL database are
 * represented as SQLObject subclasses. The main features inherited from
 * SQLObject are:
 * 
 * <h2>Tree structure</h2>
 * 
 * SQLObjects are arranged in a tree structure: each object has a parent, which
 * is also a SQLObject, and it has a list of children which point back to it.
 * All children of any given SQLObject must be of the exact same type. This is
 * enforced in several places, so you should find out quickly if you break this
 * rule.
 * 
 * <h2>Transparent lazy reverse engineering</h2>
 * 
 * SQLObjects have two primary states: populated and unpopulated. The state
 * transitions from unpopulated to populated when the child list is filled in by
 * reverse engineering the information from a physical SQL database. The state
 * never transitions from populated to unpopulated.
 * <p>
 * When creating a SQLObject, you can decide whether you want it to start in the
 * populated state or not. When starting in the populated state, the lazy
 * reverse engineering feature will not be active, and the SQLObject can (must)
 * be completely configured via its API.
 * 
 * <h2>Event System</h2>
 * 
 * Most changes to the state of a SQLObject cause an event to be fired. This is
 * useful when building GUI components and undo/redo systems around SQLObjects.
 * See {@link SQLObjectEvent} and {@link SQLObjectListener} for details.
 * 
 * <h2>Client Properties</h2>
 * 
 * Every SQLObject maintains a map of key/value pairs. This map is segregated
 * into namespaces to ensure multiple clients who don't know about each other do
 * not end up suffering naming collisions.
 */
public abstract class SQLObject extends AbstractSPObject implements java.io.Serializable {

	private static Logger logger = Logger.getLogger(SQLObject.class);
	protected boolean populated = false;
	
	/**
	 * The name used for this object in a physical database system. This name may have
	 * to be altered to fit the naming constraints of a particular system, in terms
	 * of length, case, allowable characters, and other requirements.
	 */
	private String physicalName;

	/**
	 * The map that hold the client properties of this object. Don't modify the
	 * contents of this map directly; use the {@link #putClientProperty(Class, String, Object)}
	 * and {@link #getClientProperty(Class, String)} methods which take care of
	 * firing events and other such bookkeeping.
	 */
	private final Map<String, Object> clientProperties = new HashMap<String, Object>();
	
	/**
	 * When this counter is > 0, the fireXXX methods will ignore secondary changes.
	 */
	protected int magicDisableCount = 0;
	
	/**
	 * This is the throwable that tells if the children of this component can be reached
	 * or not. If this is null then the children can be reached. If it is not null
	 * then there was an exception the last time the children were attempted to be accessed.
	 */
	private Throwable childrenInaccessibleReason = null;
	
	public synchronized void setMagicEnabled(boolean enable) {
		if (magicDisableCount < 0) {
			throw new IllegalStateException("magicDisableCount < 0");
		}
		if (enable) {
			if (magicDisableCount == 0) {
				throw new IllegalArgumentException("Sorry, you asked me to enable, but disable count already 0");
				// return;
			}
			--magicDisableCount;
		} else { // disable
			++magicDisableCount;
		}
	}
	
	public boolean isMagicEnabled() {
		if (magicDisableCount > 0) {
			return false;
		}
		if (getParent() != null) {
			return getParent().isMagicEnabled();
		}
		return true;
	}
	
    /**
     * Returns the name used for this object in a physical database system. This
     * name may have to be altered to fit the naming constraints of a particular
     * system, in terms of length, case, allowable characters, duplication of
     * names within the same namespace, and other requirements. Presently, the
     * DDL Generators take it upon themselves to perform this alteration. In the
     * near future, we hope to make the DDL Generators use this name verbatim
     * and simply fail on names that are not permissible. The responsibility of
     * suggesting physical name changes will shift to critics configured for
     * each database platform's particular needs. Those critics would provide
     * "quick fix" suggestions with names that are legal in their own target
     * platform, and then set them.
     * <p>
     * there is no good reason why this method is declared final, but there is
     * no good reason to override it at this time.
     * 
     * @return The physical name to use for forward engineering and database
     *         comparison, or the logical name (see {@link #getName()}) if no
     *         physical name has been set.
     */
	public final String getPhysicalName() {
		if (physicalName != null) {
			return physicalName;
		}
		return getName(); 
	}

    /**
     * Sets the physical identifier name to use when forward-engineering into
     * the target database and comparing with existing databases.
     * 
     * @param argName The new physical name to use.
     */
	public void setPhysicalName(String argName) {
		String oldPhysicalName = this.physicalName;
		this.physicalName = argName;
		firePropertyChange("physicalName",oldPhysicalName,argName);
	}

	/**
	 * Returns the parent of this SQLObject or <code>null</code> if it
	 * is a root object.
	 */
	public SQLObject getParent() {
	   return (SQLObject) super.getParent(); 
	}

	/**
     * Causes this SQLObject to load its children through populateImpl (if any exist).
     * This will do nothing if the object is already populated.
     */
	public final void populate() throws SQLObjectException {
	    if (populated) return;
	    
        // We're going to just leave caching on all the time and see how it pans out
        DatabaseMetaDataDecorator.putHint(
                DatabaseMetaDataDecorator.CACHE_TYPE,
                DatabaseMetaDataDecorator.CacheType.EAGER_CACHE);

	    childrenInaccessibleReason = null;
	    try {
	        populateImpl();
	    } catch (SQLObjectException e) {
	        setChildrenInaccessibleReason(e, true);
	    } catch (RuntimeException e) {
	        setChildrenInaccessibleReason(e, true);
	    }
	}

	/**
	 * Causes this SQLObject to load its children (if any exist).
	 * This method will be called lots of times, so track whether or
	 * not you need to do anything and return right away whenever
	 * possible.
	 */
	protected abstract void populateImpl() throws SQLObjectException;

	/**
	 * Returns a short string that should be displayed to the user for
	 * representing this SQLObject as a label.
	 */
	public abstract String getShortDisplayName();

	/**
	 * Tells if this object has already been filled with children, or
	 * if that operation is still pending.
	 */
	public boolean isPopulated() {
		return populated;
	}
	
	/**
	 * Lets outside users modify the internal flag that says whether
	 * or not the list of child objects has already been loaded from
	 * the source database.  Users of this SQLObject hierarchies should
	 * not normally call this method, but it needs to be public for the
	 * SwingUIProject load implementation.
	 */
	public void setPopulated(boolean v) {
		populated = v;
	}

	/**
	 * Returns true if and only if this object can have child
	 * SQLObjects.  Your implementation of this method <b>must not</b>
	 * cause JDBC activity, or the lazy loading properties of your
	 * SQLObjects will be wasted!  Typically, you will implement this
	 * with a hardcoded "<code>return true</code>" or 
	 * "<code>return false</code>" depending on object type.
	 */
	public abstract boolean allowsChildren();

	@Override
	public boolean removeChild(SPObject child) throws ObjectDependentException,
			IllegalArgumentException {
		if (child instanceof SQLObject) {
			if (!fireDbChildPreRemove(getChildrenWithoutPopulating(child.getClass()).indexOf(child), (SQLObject) child)) {
				return false;
			}
		}
		return super.removeChild(child);
	}
	
	/**
	 * Returns an unmodifiable view of the child list.  All list
	 * members will be SQLObject subclasses (SQLTable,
	 * SQLRelationship, SQLColumn, etc.) which are directly contained
	 * within this SQLObject.
	 */
	public List<? extends SQLObject> getChildren() {
		return getChildren(SQLObject.class);
	}
	
	public <T extends SPObject> List<T> getChildren(Class<T> type) {
		try {
			populate();
			return getChildrenWithoutPopulating(type);
		} catch (SQLObjectException e) {
			throw new RuntimeException("Could not populate " + getName() + 
					": " + e.getMessage());
		}
	}
	
	public abstract List<? extends SQLObject> getChildrenWithoutPopulating();
	
	public <T extends SPObject> List<T> getChildrenWithoutPopulating(Class<T> type) {
		List<T> children = new ArrayList<T>();
		for (SQLObject child : getChildrenWithoutPopulating()) {
			if (type.isAssignableFrom(child.getClass())) {
				children.add(type.cast(child));
			}
		}
		return Collections.unmodifiableList(children);
	}

	public SQLObject getChild(int index) throws SQLObjectException {
		populate();
		return (SQLObject) getChildren().get(index);
	}

	public int getChildCount() throws SQLObjectException {
		populate();
		return getChildren().size();
	}

	/**
	 * Returns the names of all children of this SQLObject. Causes this
	 * SQLObject to become populated.
	 * <p>
	 * Originally created for internal use during refresh. There should be no
	 * harm in making this method public if it's needed externally.
	 * 
	 * @throws SQLObjectException
	 *             if populating this object fails
	 */
    Set<String> getChildNames() throws SQLObjectException {
        return getChildNames(SQLObject.class);
    }

	/**
	 * Returns the names of all children of a certain type of this SQLObject.
	 * Causes this SQLObject to become populated.
	 * <p>
	 * Originally created for internal use during refresh. There should be no
	 * harm in making this method public if it's needed externally.
	 * 
	 * @throws SQLObjectException
	 *             if populating this object fails
	 */
    <T extends SQLObject> Set<String> getChildNames(Class<T> childType) {
        HashSet<String> names = new HashSet<String>();
        for (T child : getChildren(childType)) {
            names.add(child.getName());
        }
        return names;
    }

	/**
	 * Adds the given SQLObject to this SQLObject at index. Causes a
	 * DBChildrenInserted event.  If you want to override the
	 * behaviour of addChild, override this method.
	 * @throws SQLObjectException 
	 */
//	public void addChild(SQLObject newChild, int index) throws SQLObjectException {
//		addChildImpl(newChild, index);
//	}

	/**
	 * Adds the given SQLObject to this SQLObject at the end of the
	 * child list by calling {@link #addChild(SPObject, int)}. Causes
	 * a DBChildrenInserted event.  If you want to override the
	 * behaviour of addChild, do not override this method.
	 * @throws SQLObjectException 
	 */
	public void addChild(SQLObject newChild) throws SQLObjectException {
		addChild(newChild, getChildren(newChild.getClass()).size());
	}
	
	// ------------------- sql object event support -------------------

	/*
	 * @return An immutable copy of the list of SQLObject listeners
	 */
	public List<SPListener> getSPListeners() {
			return listeners;
	}
	
    // ------------------- sql object Pre-event support -------------------
    private final transient List<SQLObjectPreEventListener> sqlObjectPreEventListeners = 
        new ArrayList<SQLObjectPreEventListener>();

    /**
     * @return An immutable copy of the list of SQLObject pre-event listeners
     */
    public List<SQLObjectPreEventListener> getSQLObjectPreEventListeners() {
            return sqlObjectPreEventListeners;
    }
    
    public void addSQLObjectPreEventListener(SQLObjectPreEventListener l) {
        if (l == null) throw new NullPointerException("You can't add a null listener");
        synchronized(sqlObjectPreEventListeners) {
            if (sqlObjectPreEventListeners.contains(l)) {
                if (logger.isDebugEnabled()) {
                    logger.debug("NOT Adding duplicate pre-event listener "+l+" to SQLObject "+this);
                }
                return;
            }       
            sqlObjectPreEventListeners.add(l);
        }
    }

    public void removeSQLObjectPreEventListener(SQLObjectPreEventListener l) {
        synchronized(sqlObjectPreEventListeners) {
            sqlObjectPreEventListeners.remove(l);
        }
    }

    /**
     * Fires a pre-remove event, and returns the status of whether or not the
     * operation should proceed.
     * 
     * @param oldIndices The child indices that might be removed
     * @param oldChildren The children that might be removed
     * @return  True if the operation should proceed; false if it should not. 
     */
    protected boolean fireDbChildrenPreRemove(int[] oldIndices, List oldChildren) {
        if (logger.isDebugEnabled()) {
            logger.debug(getClass().getName()+" "+toString()+": " +
                    "firing dbChildrenPreRemove event");
        }
        SQLObjectPreEvent e = new SQLObjectPreEvent
            (this,
             oldIndices,
             (SQLObject[]) oldChildren.toArray(new SQLObject[oldChildren.size()]));
        int count = 0;
        synchronized (sqlObjectPreEventListeners) {
            SQLObjectPreEventListener[] listeners =
                sqlObjectPreEventListeners.toArray(new SQLObjectPreEventListener[0]);
            for (SQLObjectPreEventListener l : listeners) {
                l.dbChildrenPreRemove(e);
                count++;
            }
        }
        if (logger.isDebugEnabled()) logger.debug("Notified "+count+" listeners. Veto="+e.isVetoed());
        return !e.isVetoed();
    }

    /**
     * Convenience method for {@link #fireDbChildrenPreRemove(int[], List)} when there
     * is only one child being removed.
     * 
     * @param oldIndex The index of the child to be removed
     * @param oldChild The child to be removed
     */
    protected boolean fireDbChildPreRemove(int oldIndex, SQLObject oldChild) {
        int[] oldIndexArray = new int[1];
        oldIndexArray[0] = oldIndex;
        List oldChildList = new ArrayList(1);
        oldChildList.add(oldChild);
        return fireDbChildrenPreRemove(oldIndexArray, oldChildList);
    }

	public abstract Class<? extends SQLObject> getChildType();
	
    /**
     * Returns the first child (in the sequence of the getChildren() list) which has the
     * given name (case sensitive).
     *  
     * @param name The name of the child to look for (case sensitive).
     * @return The first child with the given name, or null if there is no such child.
     * @throws SQLObjectException If the moon is waxing gibbous.
     */
    public SQLObject getChildByName(String name) throws SQLObjectException {
        return getChildByNameImpl(name, false);
    }
    
    public <T extends SQLObject> T getChildByName(String name, Class<T> childType) {
    	return getChildByNameImpl(name, false, childType);
    }
    
    /**
     * Returns the first child (in the sequence of the getChildren() list) which has the
     * given name (case insensitive).
     *  
     * @param name The name of the child to look for (case insensitive).
     * @return The first child with the given name, or null if there is no such child.
     * @throws SQLObjectException If the moon is waxing gibbous.
     */
    public SQLObject getChildByNameIgnoreCase(String name) throws SQLObjectException {
        return getChildByNameImpl(name, true);
    }
    
    public <T extends SQLObject> T getChildByNameIgnoreCase(String name, Class<T> childType) {
    	return getChildByNameImpl(name, true, childType);
    }

	/**
	 * Searches for a child object through the entire list of children based on
	 * its name and case sensitivity.
	 * 
	 * @param name The name of the child
	 * @param ignoreCase Whether the name search should be case sensitive
	 * @return The found child with the given name, or null if it does not exist.
	 */
    private SQLObject getChildByNameImpl(String name, boolean ignoreCase) {
        return getChildByNameImpl(name, ignoreCase, SQLObject.class);
    }

	/**
	 * Searches for a child object based on its class type, name and case
	 * sensitivity.
	 * 
	 * @param <T>
	 *            The child type of SQLObject to look for
	 * @param name
	 *            The name of the child
	 * @param ignoreCase
	 *            Whether the name search should be case sensitive
	 * @param childType
	 *            The child type to look for when searching for the SQLObject's
	 *            name
	 * @return The found child with the given name, or null if it does not exist.
	 */
    private <T extends SQLObject> T getChildByNameImpl(String name, boolean ignoreCase, Class<T> childType) {
        for (T o : getChildren(childType)) {
            if ( (ignoreCase && o.getName().equalsIgnoreCase(name))
                  || ( (!ignoreCase) && o.getName().equals(name)) ) {
                return o;
            }
        }
        return null;
    }
    
    /**
     * Returns the index of the named child, or -1 if there is no child with
     * that name.
     * 
     * @param name The name of the child to look for (case sensitive)
     * @return The index of the named child in the child list, or -1 if there
     * is no such child.
     * @throws SQLObjectException if the child list can't be populated
     */
    public int getIndexOfChildByName(String name) throws SQLObjectException {
        int i = 0;
        for (Object o : getChildren()) {
            SQLObject so = (SQLObject) o;
            if (so.getName().equals(name)) {
                return i;
            }
            i++;
        }
        return -1;
    }
    
    /**
     * Sets the current value of the named client property in the given
     * namespace. If the new value is different from the existing value,
     * a SQLObjectChangedEvent will be fired with the property name
     * of <code>namespace.getName() + "." + propName</code>.
     * 
     * @param namespace
     *            The namespace to look in. This is usually the class of the
     *            code calling in, but there is no restriction from getting and
     *            setting client properties maintained by other classes.
     * @param propName
     *            The name of the property to set.
     */
    public void putClientProperty(Class<?> namespace, String propName, Object property) {
        String key = namespace + "." + propName;
        Object oldValue = clientProperties.get(key);
        clientProperties.put(key, property);
        firePropertyChange("clientProperty." + key, oldValue, property);
    }

    /**
     * Returns the current value of the named client property in the given
     * namespace.
     * 
     * @param namespace
     *            The namespace to look in. This is usually the class of the
     *            code calling in, but there is no restriction from getting and
     *            setting client properties maintained by other classes.
     * @param propName
     *            The name of the property to get.
     * @return The property's current value, or null if the property is not set
     *         on this SQL Object.
     */
    public Object getClientProperty(Class<?> namespace, String propName) {
        return clientProperties.get(namespace + "." + propName);
    }
    
    /**
     * Rerturns the property names of all client properties currently set
     * on this SQLObject.
     */
    public Set<String> getClientPropertyNames() {
        return clientProperties.keySet();
    }
    
    public Throwable getChildrenInaccessibleReason() {
        return childrenInaccessibleReason;
    }

	/**
	 * This setter will take in either a Throwable to set the inaccessible
	 * reason to, for things like copy methods, or a string of the exception
	 * message, for things like loading the exception.
	 * 
	 * @param cause
	 *            The throwable that made the children of this object
	 *            inaccessible
	 * @param rethrow
	 *            Decides if the cause should be rethrown wrapped in a
	 *            SQLObjectException. Set this to true to have the exception be
	 *            rethrown.
	 */
    public void setChildrenInaccessibleReason(Throwable cause, boolean rethrow) throws SQLObjectException {
        Throwable oldVal = this.childrenInaccessibleReason;
        this.childrenInaccessibleReason = cause;
        firePropertyChange("childrenInaccessibleReason", oldVal, childrenInaccessibleReason);
        if (rethrow) {
        	if (cause instanceof SQLObjectException) {
        		throw (SQLObjectException) cause;
        	} else if (cause instanceof RuntimeException) {
        		throw (RuntimeException) cause;
        	} else {
        		throw new SQLObjectException(cause);
        	}
        }
    }

    /**
     * A basic refresh method that simply calls refresh on all existing
     * children. Most classes will have to override this with an implementation
     * that checks for changes in the physical database and adjusts the child
     * list accordingly.
     * <p>
     * Note that this package-private method is not meant to be called directly!
     * All refresh operations have to be initiated by calling the parent database's
     * refresh method, {@link SQLDatabase#refresh()}, which is public.
     */
    void refresh() throws SQLObjectException {
        if (!isPopulated()) {
            logger.debug("Not refreshing unpopulated object " + this);
            return;
        }
        
        if (isTableContainer()) {
            logger.debug("Refreshing table container " + this);
            Connection con = null;
            try {
                SQLDatabase db = SQLObjectUtils.getAncestor(this, SQLDatabase.class);
                SQLCatalog cat = SQLObjectUtils.getAncestor(this, SQLCatalog.class);
                SQLSchema sch = SQLObjectUtils.getAncestor(this, SQLSchema.class);
                
                con = db.getConnection();
                DatabaseMetaData dbmd = con.getMetaData();
                List<SQLTable> newChildren = SQLTable.fetchTablesForTableContainer(
                        dbmd,
                        cat == null ? null : cat.getName(),
                        sch == null ? null : sch.getName());
                SQLObjectUtils.refreshChildren(this, newChildren, SQLTable.class);

                for (SQLTable t : getChildren(SQLTable.class)) {
                    t.refreshColumns();
                }
                for (SQLTable t : getChildren(SQLTable.class)) {
                    t.refreshIndexes();
                }
                for (SQLTable t : getChildren(SQLTable.class)) {
                    t.refreshImportedKeys();
                }
                
                logger.debug("Table container refresh complete for " + this);
            } catch (SQLException e) {
                throw new SQLObjectException("Refresh failed", e);
            } finally {
                try {
                    if (con != null) con.close();
                } catch (SQLException ex) {
                    logger.warn("Failed to close connection! Squishing this exception:", ex);
                }
            }
        } else {
            for (SQLObject o : getChildren()) {
                o.refresh();
            }
        }
    }

    /**
     * Returns true if this SQLObject is definitely a container for SQLTable
     * objects. Depending on the source database topology, instances of
     * SQLDatabase, SQLCatalog, and SQLSchema may return true. Other types of
     * SQLObject will always return false, since there is no topology in which
     * they are table containers. Calling this method will never result in
     * populating an unpopulated SQLObject.
     * <p>
     * If this SQLObject is populated and has at least one child, this method
     * makes the determination cheap and accurate by checking if the children
     * are of type SQLTable. Otherwise, the object has no children (whether or
     * not it is populated), so this method examines the JDBC driver's database
     * metadata to determine the topology based on the reported catalogTerm and
     * schemaTerm. A null value for either term is interpreted to mean the
     * database does not have that level of object containment. The (major)
     * downside of this approach is that it does not work when the database
     * connection is unavailable.
     * 
     * @return
     * @throws SQLObjectException
     *             if the determination requires database metadata access, and
     *             it's not possible to obtain the database connection or the
     *             database metadata.
     */
    public boolean isTableContainer() throws SQLObjectException {
        
        // first, check for existing SQLTable children--this is a dead giveaway for a table container!
        if (getChildren().size() > 0) {
            return (getChildren(SQLTable.class).size() != 0);
        }
        
        // no children. we have to do a bit of structural investigation.
        
        // schemas can only contain tables
        if (getClass() == SQLSchema.class) {
            return true;
        }

        // determination for catalogs and databases requires database metadata
        Connection con = null;
        try {
            
            // catalogs could contain schemas or tables. If schemaTerm is null, it must be tables.
            if (getClass() == SQLCatalog.class) {
                SQLDatabase db = (SQLDatabase) getParent();
                con = db.getConnection();
                if (con == null) {
                    throw new SQLObjectException("Unable to determine table container status without database connection");
                }
                DatabaseMetaData dbmd = con.getMetaData();
                return dbmd.getSchemaTerm() == null;
            }

            // databases could contain catalogs, schemas, or tables
            if (getClass() == SQLDatabase.class) {
                SQLDatabase db = (SQLDatabase) this;
                con = db.getConnection();
                if (con == null) {
                    throw new SQLObjectException("Unable to determine table container status without database connection");
                }
                DatabaseMetaData dbmd = con.getMetaData();
                return (dbmd.getSchemaTerm() == null) && (dbmd.getCatalogTerm() == null);
            }
        } catch (SQLException ex) {
            throw new SQLObjectException("Failed to obtain database metadata", ex);
        } finally {
            try {
                if (con != null) con.close();
            } catch (SQLException ex) {
                logger.warn("Failed to close connection", ex);
            }
        }
        
        // other types of SQLObject are never table containers
        return false;
    }
    
    /**
     * Updates all the properties of this SQLObject to match those of the other
     * SQLObject. Some implementations also update the list of children (see
     * SQLIndex and SQLRelationship for examples of this). If any of the properties
     * of this object change as a result of the update, the corresponding
     * events will be fired.
     * 
     * @param source
     *            The SQLObject to read the new property values from. Must be of
     *            the same type as the receiving object.
     * @throws SQLObjectException if the attempted update causes a populate that fails
     * @throws NotImplementedException
     *             The default implementation from SQLObject just throws this
     *             exception. Subclasses that want to implement this
     *             functionality must override this method.
     * @throws ClassCastException
     *             if the given source object is not the same type as this
     *             SQLObject.
     */
    public void updateToMatch(SQLObject source) throws SQLObjectException {
        throw new NotImplementedException();
    }
}
