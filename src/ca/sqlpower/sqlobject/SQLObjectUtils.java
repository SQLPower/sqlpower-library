/*
 * Copyright (c) 2009, SQL Power Group Inc.
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import ca.sqlpower.sqlobject.undo.CompoundEventListener;

public class SQLObjectUtils {
    
    private final static Logger logger = Logger.getLogger(SQLObjectUtils.class);
    
    private SQLObjectUtils() {}

    /**
     * This will check if the two objects given are contained in the same
     * session by finding their top ancestors and comparing them. o1 and o2
     * cannot be null.
     */
    public static boolean isInSameSession(SQLObject o1, SQLObject o2) {
        SQLObject o1Parent = o1;
        while (o1Parent.getParent() != null) {
            o1Parent = o1Parent.getParent();
        }
        SQLObject o2Parent = o2;
        while (o2Parent.getParent() != null) {
            o2Parent = o2Parent.getParent();
        }
        
        logger.debug("Parent of " + o1 + " is " + o1Parent + ", parent of " + o2 + " is " + o2Parent);
        return o1Parent == o2Parent;
    }

    /**
     * Adds listener to source's listener list and all of source's
     * children's listener lists recursively.
     */
    public static void listenToHierarchy(SQLObjectListener listener, SQLObject source)
    throws SQLObjectException {
    	if (logger.isDebugEnabled()) logger.debug("Listening to new SQL Object "+source);
    	source.addSQLObjectListener(listener);
    	if (source.isPopulated() && source.allowsChildren()) {
    		Iterator it = source.getChildren().iterator();
    		while (it.hasNext()) {
    			listenToHierarchy(listener, (SQLObject) it.next());
    		}
    	}
    
    }

    /**
     * Calls listenToHierarchy on each element in the sources array.
     * Does nothing if sources is null.
     */
    public static void listenToHierarchy(SQLObjectListener listener, SQLObject[] sources)
    throws SQLObjectException {
    	if (sources == null) return;
    	for (int i = 0; i < sources.length; i++) {
    		listenToHierarchy(listener, sources[i]);
    	}
    }

    /**
     * Removes listener from source's listener list and all of source's
     * children's listener lists recursively.
     */
    public static void unlistenToHierarchy(SQLObjectListener listener, SQLObject source)
    throws SQLObjectException {
        if (logger.isDebugEnabled()) logger.debug("Removing "+listener+" from listener list of "+source);
    	source.removeSQLObjectListener(listener);
    	if (source.isPopulated() && source.allowsChildren()) {
            if (logger.isDebugEnabled()) logger.debug("        Now removing for children: "+source.getChildren());
    		Iterator it = source.getChildren().iterator();
    		while (it.hasNext()) {
    			SQLObject ob = (SQLObject) it.next();
    			unlistenToHierarchy(listener, ob);
    		}
    	}
    }

    /**
     * Calls unlistenToHierarchy on each element in the sources array.
     * Does nothing if sources is null.
     */
    public static void unlistenToHierarchy(SQLObjectListener listener, SQLObject[] sources)
    throws SQLObjectException {
    	if (sources == null) return;
    	for (int i = 0; i < sources.length; i++) {
    		unlistenToHierarchy(listener, sources[i]);
    	}
    }

    /**
     * Creates a dot-separated string of the name of the given SQLObject and the
     * names of each of its ancestors, stopping at the first ancestor of the
     * given type. The top-level ancestor's name will be the first name to
     * appear in the string, and the given object's name will be the last.
     * 
     * @param obj
     *            The object whose qualified name you wish to obtain
     * @param stopAt
     *            The class of ancestor to stop at. The name of this ancestor
     *            will not be included in the returned string. If stopAt is
     *            null, or a class which is not an ancestor of the given
     *            SQLObject, the returned string will contain all ancestor
     *            object names up to the root of the SQLObject tree.
     */
    public static String toQualifiedName(SQLObject obj, Class<? extends SQLObject> stopAt) {
        List<SQLObject> ancestors = new ArrayList<SQLObject>();
        while (obj != null && obj.getClass() != stopAt) {
            ancestors.add(obj);
            obj = obj.getParent();
        }
        StringBuilder sb = new StringBuilder();
        for (int i = ancestors.size() - 1; i >= 0; i--) {
            sb.append(ancestors.get(i).getName());
            if (i != 0) {
                sb.append(".");
            }
        }
        return sb.toString();
    }

    /**
     * Creates a dot-separated string of the name of the given SQLObject and the
     * names of each of its ancestors.  The top-level ancestor's name will be the
     * first name to appear in the string, and the given object's name will be
     * the last. This is the equivalent of calling toQualifiedName(obj, null).
     */
    public static String toQualifiedName(SQLObject obj) {
        return toQualifiedName(obj, null);
    }

    /**
     * Adds listener to source's listener list and all of source's
     * children's listener lists recursively.
     */
    public static void addUndoListenerToHierarchy(CompoundEventListener listener, SQLObject source)
    throws SQLObjectException {
        if (logger.isDebugEnabled()) logger.debug("Undo Listening to new SQL Object "+source);
    	source.addUndoEventListener(listener);
    	if (source.isPopulated() && source.allowsChildren()) {
    		Iterator it = source.getChildren().iterator();
    		while (it.hasNext()) {
    			addUndoListenerToHierarchy(listener, (SQLObject) it.next());
    		}
    	}
    
    }

    /**
     * Calls listenToHierarchy on each element in the sources array.
     * Does nothing if sources is null.
     */
    public static void addUndoListenerToHierarchy(CompoundEventListener listener, SQLObject[] sources)
    throws SQLObjectException {
    	if (sources == null) return;
    	for (int i = 0; i < sources.length; i++) {
    		addUndoListenerToHierarchy(listener, sources[i]);
    	}
    }

    /**
     * Removes listener from source's listener list and all of source's
     * children's listener lists recursively.
     */
    public static void undoUnlistenToHierarchy(CompoundEventListener listener, SQLObject source)
    throws SQLObjectException {
        if (logger.isDebugEnabled()) logger.debug("Unlistening to SQL Object "+source);
    	source.removeUndoEventListener(listener);
    	if (source.isPopulated() && source.allowsChildren()) {
    		Iterator it = source.getChildren().iterator();
    		while (it.hasNext()) {
    			SQLObject ob = (SQLObject) it.next();
    			undoUnlistenToHierarchy(listener, ob);
    		}
    	}
    }

    /**
     * Calls unlistenToHierarchy on each element in the sources array.
     * Does nothing if sources is null.
     */
    public static void undoUnlistenToHierarchy(CompoundEventListener listener, SQLObject[] sources)
    throws SQLObjectException {
    	if (sources == null) return;
    	for (int i = 0; i < sources.length; i++) {
    		undoUnlistenToHierarchy(listener, sources[i]);
    	}
    }

}
