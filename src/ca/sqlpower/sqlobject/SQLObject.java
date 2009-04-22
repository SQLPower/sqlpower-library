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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;
import ca.sqlpower.sqlobject.SQLRelationship.RelationshipManager;
import ca.sqlpower.sqlobject.undo.CompoundEvent;
import ca.sqlpower.sqlobject.undo.CompoundEventListener;
import ca.sqlpower.sqlobject.undo.CompoundEvent.EventTypes;

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
public abstract class SQLObject implements java.io.Serializable {

	private static Logger logger = Logger.getLogger(SQLObject.class);
	protected boolean populated = false;
	
	/**
	 * This is the actual table name which satisfies the specific 
	 * database requirements.
	 */
	private String physicalName;
	
	/**
	 * This is the alternate name for this column for
	 * easier understanding.
	 */
	private String name;
	
	/**
	 * The map that hold the client properties of this object. Don't modify the
	 * contents of this map directly; use the {@link #putClientProperty(Class, String, Object)}
	 * and {@link #getClientProperty(Class, String)} methods which take care of
	 * firing events and other such bookkeeping.
	 */
	private final Map<String, Object> clientProperties = new HashMap<String, Object>();
	
	/**
	 * The children of this SQLObject (if not applicable, set to
	 * Collections.EMPTY_LIST in your constructor).
	 */
	protected List children;

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
	
	/**
	 * The parent of this SQLObject. This may be null if it is
	 * a root object.
	 */
	private SQLObject parent;
	 
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
	 * This is the alternate name of the object for easier understanding. 
	 * For tables, it returns the table name; for catalogs, 
	 * the catalog name, and so on.
	 */
	public String getName()
	{
		return name;
	}
	
	
	/**
	 * Sets the value of sql object name
	 *
	 * @param argName Value to assign to this.name
	 */
	public void setName(String argName) {
		String oldValue = name;
		this.name = argName;
		fireDbObjectChanged("name", oldValue, name);
	}
	

	/**
	 * when the logical name is an illegal identifier in the target
     * database, generate a legal name store it here.  Some 
     * SQLObject classes do not need to implement this, so the method
     * is declared concrete, and passes through to getName() by
     * default.  SQLObject subclasses that use this idea should
     * override this class to return the physicalName and then 
     * pass through to the getName method if one is not found.
     * 
     * <p>there is no good reason why this is final, but there is no good
     * reason to override it at this time. </p>
	 */
	public final String getPhysicalName() {
		if (physicalName != null) {
			return physicalName;
		}
		return getName(); 
	}
	
	public void setPhysicalName(String argName) {
		String oldPhysicalName = this.physicalName;
		this.physicalName = argName;
		fireDbObjectChanged("physicalName",oldPhysicalName,argName);
	}

	/**
	 * Returns the parent of this SQLObject or <code>null</code> if it
	 * is a root object.
	 */
	public SQLObject getParent() {
	   return parent; 
	}

	/**
	 * Parents call this on their children to update parent pointers
	 * during addChild and removeChild requests.
	 */
	protected void setParent(SQLObject parent) {
	    this.parent = parent;
	}
	
	/**
     * Causes this SQLObject to load its children through populateImpl (if any exist).
     * This will do nothing if the object is already populated.
     */
	public void populate() throws SQLObjectException {
	    if (populated) return;
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

	/**
	 * Returns an unmodifiable view of the child list.  All list
	 * members will be SQLObject subclasses (SQLTable,
	 * SQLRelationship, SQLColumn, etc.) which are directly contained
	 * within this SQLObject.
	 */
	public List getChildren() throws SQLObjectException {
		if (!allowsChildren()) //never return null;
			return children;
		populate();
		return Collections.unmodifiableList(children);
	}

	public SQLObject getChild(int index) throws SQLObjectException {
		populate();
		return (SQLObject) children.get(index);
	}

	public int getChildCount() throws SQLObjectException {
		populate();
		return children.size();
	}

    /**
     * Returns the names of all children of this SQLObject. Causes this
     * SQLObject to become populated.
     * <p>
     * Originally created for internal use during refresh. There should be no
     * harm in making this method public if it's needed externally.
     * 
     * @throws SQLObjectException if populating this object fails
     */
    Set<String> getChildNames() throws SQLObjectException {
        HashSet<String> names = new HashSet<String>();
        for (SQLObject child : (List<SQLObject>) getChildren()) {
            names.add(child.getName());
        }
        return names;
    }

	/**
	 * All other addChild() methods call this one.  If you want to override the addChild behaviour,
	 * override this method only.
	 * 
	 * @param index The index that the new child will have
	 * @param newChild The new child to add (must be same type as all other children)
	 * @throws SQLObjectException  If you try to add a child of a different type than the existing children.
	 */
	protected void addChildImpl(int index, SQLObject newChild) throws SQLObjectException {
		if ( children.size() > 0 && 
				! (children.get(0).getClass().isAssignableFrom(newChild.getClass())
					|| newChild.getClass().isAssignableFrom(children.get(0).getClass()))) {
            
            throw new SQLObjectException(
                    "You Can't mix SQL Object Types! You gave: " +
                    newChild.getClass().getName() +
                    "; I need " + children.get(0).getClass());
		}
		children.add(index, newChild);
		newChild.setParent(this);
		fireDbChildInserted(index, newChild);
	}
	
	/**
	 * Adds the given SQLObject to this SQLObject at index. Causes a
	 * DBChildrenInserted event.  If you want to override the
	 * behaviour of addChild, override this method.
	 * @throws SQLObjectException 
	 * @throws SQLObjectException 
	 */
	public void addChild(int index, SQLObject newChild) throws SQLObjectException {
		addChildImpl(index, newChild);
	}

	/**
	 * Adds the given SQLObject to this SQLObject at the end of the
	 * child list by calling {@link #addChild(int,SQLObject)}. Causes
	 * a DBChildrenInserted event.  If you want to override the
	 * behaviour of addChild, do not override this method.
	 * @throws SQLObjectException 
	 * @throws SQLObjectException 
	 * @throws Exception 
	 */
	public void addChild(SQLObject newChild) throws SQLObjectException {
		addChildImpl(children.size(), newChild);
	}
	
    /**
     * This implementation calls {@link#removeImpl(int)}.
     */
	public SQLObject removeChild(int index) {
	    return removeImpl(index);
	}

	/**
	 * This method is implemented in terms of {@link #removeImpl(int)}.
	 */
	public boolean removeChild(SQLObject child) {
		int childIdx = children.indexOf(child);
		if (childIdx >= 0) {
			removeChild(childIdx);
		}
		return childIdx >= 0;
	}

    /**
     * The implementation that all remove methods delegate to.  If you want
     * to override the behaviour of removeChild, override this method.
     */
    protected SQLObject removeImpl(int index) {
        boolean shouldProceed = fireDbChildPreRemove(index, (SQLObject) children.get(index));

        if (shouldProceed) {
            try {
                startCompoundEdit("Remove child of " + getName());
                SQLObject removedChild = (SQLObject) children.remove(index);
                if (removedChild != null) {
                    removedChild.setParent(null);
                    fireDbChildRemoved(index, removedChild);
                }
                return removedChild;
            } finally {
                endCompoundEdit("Remove child of " + getName());
            }
        } else {
            return null;
        }
    }
	
	// ------------------- sql object event support -------------------
	private final transient List<SQLObjectListener> sqlObjectListeners = 
		new LinkedList<SQLObjectListener>();

	/*
	 * @return An immutable copy of the list of SQLObject listeners
	 */
	public List<SQLObjectListener> getSQLObjectListeners() {
			return sqlObjectListeners;
	}
	
	public void addSQLObjectListener(SQLObjectListener l) {
		if (l == null) throw new NullPointerException("You can't add a null listener");
		synchronized(sqlObjectListeners) {
			if (sqlObjectListeners.contains(l)) {
				if (logger.isDebugEnabled()) {
					logger.debug("NOT Adding duplicate listener "+l+" to SQLObject "+this);
				}
				return;
			}		
			sqlObjectListeners.add(l);
		}
	}

	public void removeSQLObjectListener(SQLObjectListener l) {
		synchronized(sqlObjectListeners) {
			sqlObjectListeners.remove(l);
		}
	}

	protected void fireDbChildrenInserted(int[] newIndices, List newChildren) {
		if (logger.isDebugEnabled()) {
			logger.debug(getClass().getName()+" "+toString()+": " +
					"firing dbChildrenInserted event");
		}
		SQLObjectEvent e = new SQLObjectEvent
			(this,
			 newIndices,
			 (SQLObject[]) newChildren.toArray(new SQLObject[newChildren.size()]));
		synchronized(sqlObjectListeners) {
		    int count = 0;
		    
		    // XXX Notifying the RelationshipManager last is a workaround for an elusive bug
		    //     we are still trying to nail down. This is not intended to be used long term,
		    //     and is definitely not an example of good practice! See bug 1640 for details.
		    
            for (SQLObjectListener l : new ArrayList<SQLObjectListener>(sqlObjectListeners)) {
                if (!(l instanceof RelationshipManager)) {
                    count++;
                    l.dbChildrenInserted(e);
                }
            }
            for (SQLObjectListener l : new ArrayList<SQLObjectListener>(sqlObjectListeners)) {
                if (l instanceof RelationshipManager) {
                    count++;
                    l.dbChildrenInserted(e);
                }
            }
			logger.debug(getClass().getName()+": notified "+count+" listeners");
		}
	}

	protected void fireDbChildInserted(int newIndex, SQLObject newChild) {
		int[] newIndexArray = new int[1];
		newIndexArray[0] = newIndex;
		List newChildList = new ArrayList(1);
		newChildList.add(newChild);
		fireDbChildrenInserted(newIndexArray, newChildList);
	}

	protected void fireDbChildrenRemoved(int[] oldIndices, List oldChildren) {
		if (logger.isDebugEnabled()) {
			logger.debug(getClass().getName()+" "+toString()+": " +
					"firing dbChildrenRemoved event");
			logger.debug("Removing children " + oldChildren + " from " + this);
		}
		SQLObjectEvent e = new SQLObjectEvent
			(this,
			 oldIndices,
			 (SQLObject[]) oldChildren.toArray(new SQLObject[oldChildren.size()]));
		int count =0;
		synchronized(sqlObjectListeners) {
			SQLObjectListener[] listeners = sqlObjectListeners.toArray(new SQLObjectListener[0]);
			for(int i = listeners.length-1;i>=0;i--) {
				listeners[i].dbChildrenRemoved(e);
				count++;
			}
		}
		if (logger.isDebugEnabled()) logger.debug("Notified "+count+" listeners.");
	}

	protected void fireDbChildRemoved(int oldIndex, SQLObject oldChild) {
		int[] oldIndexArray = new int[1];
		oldIndexArray[0] = oldIndex;
		List oldChildList = new ArrayList(1);
		oldChildList.add(oldChild);
		fireDbChildrenRemoved(oldIndexArray, oldChildList);
		
	}

	protected void fireDbObjectChanged(String propertyName, Object oldValue, Object newValue) {
		boolean same = (oldValue == null ? oldValue == newValue : oldValue.equals(newValue));
		if (same) {
            if (logger.isDebugEnabled()) {
                logger.debug("Not firing property change: "+getClass().getName()+"."+propertyName+
                        " '"+oldValue+"' == '"+newValue+"'");
            }
			return;
		}
		if (logger.isDebugEnabled()) {
			logger.debug("Firing Property Change: "+getClass().getName()+"."+propertyName+
                    " '"+oldValue+"' -> '"+newValue+"'");
		}

        SQLObjectEvent e = new SQLObjectEvent(
                this,
                propertyName,
                oldValue,
                newValue);

		int count = 0;
		synchronized(sqlObjectListeners) {
			SQLObjectListener[] listeners = sqlObjectListeners.toArray(new SQLObjectListener[0]);
//            for(int i = listeners.length-1;i>=0;i--) {
			for (int i = 0; i < listeners.length; i++) {
			    listeners[i].dbObjectChanged(e);
			    count++;
            }
		}
		if (logger.isDebugEnabled()) logger.debug("Notified "+count+" listeners.");
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
	 * The list of SQLObject property change event listeners
	 * used for undo
	 */
	protected LinkedList<CompoundEventListener> undoEventListeners = new LinkedList<CompoundEventListener>();

	
	public void addUndoEventListener(CompoundEventListener l) {
		if (undoEventListeners.contains(l)) {
			if (logger.isDebugEnabled()) {
				logger.debug("NOT Adding duplicate Undo listener "+l+" to SQLObject "+this);
			}
			return;
		}
		undoEventListeners.add(l);
	}

	public void removeUndoEventListener(CompoundEventListener l) {
		undoEventListeners.remove(l);
	}
	
	private void fireUndoCompoundEvent(CompoundEvent e) {
		CompoundEventListener[] listeners = undoEventListeners.toArray(new CompoundEventListener[0]);
		if (e.getType().isStartEvent()) {
			for(int i = listeners.length-1;i>=0;i--) {
				listeners[i].compoundEditStart(e);
			}
		} else {
			for(int i = listeners.length-1;i>=0;i--) {
				listeners[i].compoundEditEnd(e);
			}
		} 
		
	}
	
	public void startCompoundEdit(String message){
		fireUndoCompoundEvent(new CompoundEvent(EventTypes.COMPOUND_EDIT_START,message));
	}
	
	public void endCompoundEdit(String message){
		fireUndoCompoundEvent(new CompoundEvent(EventTypes.COMPOUND_EDIT_END,message));
	}

	public LinkedList<CompoundEventListener> getUndoEventListeners() {
		return undoEventListeners;
	}

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
    
    /**
     * Common implementation for the two getChildByName methods.
     */
    private SQLObject getChildByNameImpl(String name, boolean ignoreCase) throws SQLObjectException {
        for (SQLObject o : (List<SQLObject>) getChildren()) {
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
        fireDbObjectChanged("clientProperty." + key, oldValue, property);
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
        fireDbObjectChanged("childrenInaccessibleReason", oldVal, childrenInaccessibleReason);
        if (rethrow) {
        	throw new SQLObjectException(cause);
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
                SQLObjectUtils.refreshChildren(this, newChildren);

                for (SQLTable t : (List<SQLTable>) children) {
                    t.refreshColumns();
                }
                for (SQLTable t : (List<SQLTable>) children) {
                    t.refreshIndexes();
                }
                for (SQLTable t : (List<SQLTable>) children) {
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
            for (SQLObject o : (List<SQLObject>) children) {
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
        if (children.size() > 0) {
            if (children.get(0).getClass() == SQLTable.class) {
                return true;
            } else {
                return false;
            }
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
