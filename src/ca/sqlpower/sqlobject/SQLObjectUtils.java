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
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import ca.sqlpower.sql.SQL;
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
        return toQualifiedName(obj, stopAt, "");
    }

    /**
     * Creates a dot-separated quoted string of the name of the given SQLObject
     * and the names of each of its ancestors, stopping at the first ancestor of
     * the given type. The top-level ancestor's name will be the first name to
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
     * @param quote
     *            The string to quote each SQLObject name with. If no quoting is
     *            desired set this to the empty string.
     */
    public static String toQualifiedName(SQLObject obj, Class<? extends SQLObject> stopAt, String quote) {
        List<SQLObject> ancestors = new ArrayList<SQLObject>();
        while (obj != null && obj.getClass() != stopAt) {
            ancestors.add(obj);
            obj = obj.getParent();
        }
        StringBuilder sb = new StringBuilder();
        for (int i = ancestors.size() - 1; i >= 0; i--) {
            String ancestorName;
                ancestorName = quote + ancestors.get(i).getName() + quote;
            sb.append(ancestorName);
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

    /**
     * Updates the child list of the given parent object with the new children in the
     * given list, in the following way:
     * 
     * <ol>
     *  <li>For children in both lists, the parent's existing child object is retained,
     *      but its updateToMatch() method is called to update its properties
     *  <li>For children in the newChildren list but not in the parent's current
     *      list of children, the newChildren object is added as a child
     *  <li>Children of the parent that are not in the newChildren list are removed from
     *      the parent.
     * </ol>
     * 
     * All comparisons are done by child name, and are case sensitive. The caveat is,
     * this refresh process will not work properly if the parent contains multiple children
     * with exactly the same name.
     * <p>
     * There is a special case to make this method work with SQLRelationship: if the objects
     * in newChildren are SQLRelationships, those objects will be attached using
     * {@link SQLRelationship#attachRelationship(SQLTable, SQLTable, boolean)} rather than
     * {@link SQLObject#addChild(SQLObject)}. 
     * 
     * @param parent The object whose children list to update
     * @param newChildren The list of children to update from. All objects in this list
     * are expected to be of the correct child type for parent. Also, they must not be attached
     * to any parent objects to start with (this method may connect some or all of them).
     * @throws SQLObjectException If this exercise causes any SQLObjects to populate, and that
     * populate operation fails.
     */
    public static void refreshChildren(SQLObject parent, List<? extends SQLObject> newChildren) throws SQLObjectException {
        Set<String> oldChildNames = parent.getChildNames();
        Set<String> newChildNames = new HashSet<String>(); // will populate in following loop
        for (SQLObject newChild : newChildren) {
            newChildNames.add(newChild.getName());
            if (oldChildNames.contains(newChild.getName())) {
                parent.getChildByName(newChild.getName()).updateToMatch(newChild);
            } else {
                if (newChild instanceof SQLRelationship) {
                    SQLRelationship r = (SQLRelationship) newChild;
                    r.attachRelationship(r.getPkTable(), r.getFkTable(), false);
                } else {
                    parent.addChild(newChild);
                }
            }
        }
        
        // get rid of removed children
        oldChildNames.removeAll(newChildNames);
        for (String removedColName : oldChildNames) {
            SQLObject removeMe = parent.getChildByName(removedColName);
            if (removeMe instanceof SQLRelationship) {
                SQLRelationship r = (SQLRelationship) removeMe;
                r.getPkTable().getExportedKeysFolder().removeChild(r);
                r.getFkTable().getImportedKeysFolder().removeChild(r);
            } else {
                parent.removeChild(removeMe);
            }
        }
    
    }

    /**
     * Creates a SQLTable in the given database, optionally under a catalog and/or schema.
     * 
     * @param db The database to create the table in.
     * @param catalog The catalog that the table (or the table's schema) should be in.
     * If null, it is assumed the given database doesn't have catalogs.
     * @param schema The schema that the table should be in.  If null, it is assumed the
     * given database doesn't have schemas.
     * @param name The name of the table to create.
     * @return The table that was created
     * @throws SQLObjectException If you specify catalog or schema for a database that doesn't
     * support catalogs or schemas; also if the database uses catalogs and schemas but you
     * fail to provide them.
     */
    public static SQLTable addSimulatedTable(SQLDatabase db, String catalog, String schema, String name) throws SQLObjectException {
        if (db.getTableByName(catalog, schema, name) != null) {
            throw new SQLObjectException("The table "+catalog+"."+schema+"."+name+" already exists");
        }
        SQLObject schemaContainer;
        if (catalog != null) {
            if (!db.isCatalogContainer()) {
                throw new SQLObjectException("You tried to add a table with a catalog ancestor to a database that doesn't support catalogs.");
            }
            schemaContainer = db.getCatalogByName(catalog);
            if (schemaContainer == null) {
                schemaContainer = new SQLCatalog(db, catalog, true);
                db.addChild(schemaContainer);
            }
        } else {
            schemaContainer = db;
        }
    
        SQLObject tableContainer;
        if (schema != null) {
            Class<? extends SQLObject> childType = schemaContainer.getChildType();
            if ( !(childType == null || childType == SQLSchema.class) ) {
                throw new SQLObjectException(
                        "The schema container ("+schemaContainer+
                        ") can't actually contain children of type SQLSchema.");
            }
            tableContainer = schemaContainer.getChildByName(schema);
            if (tableContainer == null) {
                tableContainer = new SQLSchema(schemaContainer, schema, true);
                schemaContainer.addChild(tableContainer);
            }
        } else {
            tableContainer = schemaContainer;
        }
    
        SQLTable newTable = new SQLTable(tableContainer, name, null, "TABLE", true);
        tableContainer.addChild(newTable);
    
        return newTable;
    }

    /**
     * Recursively count tables in the project, but only consider tables that
     * have been expanded.
     *
     * This might be undercounting a little bit because I think this suppresses
     * the Target Database (playpen) entries.
     *
     * @param source the source object (usually the database)
     */
    public static int countTables(SQLObject so) throws SQLObjectException {
    	if (so instanceof SQLTable) {
    		return 1;
    	} else if ( (!so.allowsChildren()) || !(so.isPopulated()) || so.getChildren() == null) {
    	    return 0;
    	} else {
    		int myCount = 0;
    		Iterator it = so.getChildren().iterator();
    		while (it.hasNext()) {
    			myCount += countTables((SQLObject) it.next());
    		}
    		return myCount;
    	}
    }

    /**
     * Recursively count tables in the project, including ones that have not been
     * expanded in the DBTree.
     *
     * @param source the source object (usually the database)
     */
    public static int countTablesSnapshot(SQLObject so) throws SQLObjectException {
    	if (so instanceof SQLTable) {
    		return 1;
    	} else {
    		int count = 0;
    		Iterator it = so.getChildren().iterator();
    		while (it.hasNext()) {
    			count += countTablesSnapshot((SQLObject) it.next());
    		}
    	    return count;
    	}
    }

    /**
     * Searches for all columns in the target database which are marked as having
     * source columns in the given source database.
     *
     * @param target The database to search.  All columns of all tables in this database are searched.
     * @param source The database to look for in the target database's columns.
     * @return A list of all columns in the target database whose source database is the same
     * as the given source object. Every item in the list will be of type SQLColumn.
     */
    public static List<SQLColumn> findColumnsSourcedFromDatabase(SQLDatabase target, SQLDatabase source) throws SQLObjectException {
    	if (logger.isDebugEnabled()) logger.debug("Searching for dependencies on "+source+" in "+target);
    	List<SQLColumn> matches = new ArrayList<SQLColumn>();
    	Iterator<?> it = target.getChildren().iterator();
    	while (it.hasNext()) {
    		SQLObject so = (SQLObject) it.next();
    		if (logger.isDebugEnabled()) logger.debug("-->Next target item is "+so.getClass().getName()+": "+so+" ("+so.getChildCount()+" children)");
    		if (so instanceof SQLTable) {
    			SQLTable t = (SQLTable) so;
    			for (SQLColumn col : t.getColumns()) {
    				if (col.getSourceColumn() != null && source.equals(col.getSourceColumn().getParentTable().getParentDatabase())) {
    					matches.add(col);
    				}
    			}
    		}
    	}
    	return matches;
    }

    /**
     * Finds the nearest common ancestor of all SQLObjects passed in. For
     * example, if a bunch of columns from the same table are passed in, this
     * method will return that table's columns folder. If a bunch of columns
     * from different tables in the same schema are passed in, this method
     * returns the database, catalog, or schema the tables belong to.
     * 
     * @param items The items to find the common ancestor of
     * @return
     */
    public static SQLObject findCommonAncestor(Collection<? extends SQLObject> items) {
        
        // first build up the full ancestory of one randomly chosen item
        List<SQLObject> commonAncestors = ancestorList(items.iterator().next());
        logger.debug("Initial ancestor list: " + commonAncestors);
        
        // now prune the ancestor list to the largest common prefix with each item
        for (SQLObject item : items) {
            List<SQLObject> itemAncestors = ancestorList(item);
            logger.debug("       Comparing with: " + itemAncestors);
            
            Iterator<SQLObject> cit = commonAncestors.iterator();
            Iterator<SQLObject> iit = itemAncestors.iterator();
            while (cit.hasNext() && iit.hasNext()) {
                if (cit.next() != iit.next()) {
                    cit.remove();
                    break;
                }
            }
            
            // remove all remaining items in the common list because they're not in common with this item
            while (cit.hasNext()) {
                cit.next();
                cit.remove();
            }
            logger.debug("     After this prune: " + commonAncestors);
        }
        
        SQLObject commonAncestor = commonAncestors.get(commonAncestors.size() - 1);
        logger.debug("Returning: " + commonAncestor);
        return commonAncestor;
    }

    public static List<SQLObject> ancestorList(SQLObject so) {
        List<SQLObject> ancestors = new LinkedList<SQLObject>();
        while (so != null) {
            ancestors.add(0, so);
            so = so.getParent();
        }
        return ancestors;
    }

    /**
     * Returns the first ancestor of <tt>so</tt> which is of the given type, or
     * <tt>null</tt> if <tt>so</tt> doesn't have an ancestor whose class is
     * <tt>ancestorType</tt>.
     * 
     * @param so
     *            The object for whose ancestor to look. (Thanks, Winston).
     * @return The nearest ancestor of type ancestorType, or null if no such
     *         ancestor exists.
     */
    public static <T extends SQLObject> T getAncestor(SQLObject so, Class<T> ancestorType) {
        while (so != null) {
            if (so.getClass().equals(ancestorType)) return (T) so;
            so = so.getParent();
        }
        return null;
    }

    /**
     * Returns true if and only if the given set of arguments would result in a
     * successful call to {@link addSimulatedTable}.
     * See that method's documentation for the meaning of the arguments.
     * 
     * @throws SQLObjectException if populating any of the relevant SQLObjects fails.
     */
    public static boolean isCompatibleWithHierarchy(SQLDatabase db, String catalog, String schema, String name) throws SQLObjectException {
        SQLObject schemaContainer;
        if ( catalog != null){
            if (db.isCatalogContainer()){
                schemaContainer = db.getCatalogByName(catalog);
                if (schemaContainer == null) {
                    return true;
                }
            } else {
                return false;
            }
        } else {
            schemaContainer = db;
        }
    
        SQLObject tableContainer;
        if (schema != null){
            if (schemaContainer.getChildType() == SQLSchema.class){
                tableContainer = schemaContainer.getChildByName(schema);
                if (tableContainer == null) {
                    return true;
                }
            } else if (schemaContainer.getChildType() == null) {
                return true;
            } else {
                return false;
            }
        } else {
            tableContainer = schemaContainer;
        }
    
        if (name != null) {
            if (tableContainer.getChildType() == null || tableContainer.getChildType() == SQLTable.class){
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    /**
     * Returns the object that contains tables in the given database.
     * Depending on platform, this could be a SQLDatabase, a SQLCatalog,
     * or a SQLSchema.  A null catName or schemName argument means that
     * catalogs or schemas are not present in the given database.
     * <p>
     * Note, all comparisons are done case-insensitively.
     * 
     * @param db The database to retrieve the table container from.
     * @param catName The name of the catalog to retrieve.  Must be null iff the
     * database does not have catalogs.
     * @param schemaName The name of the schema to retrieve.  Must be null iff the
     * database does not have schemas.
     * @return The appropriate SQLObject under db that is a parent of SQLTable objects,
     * given the catalog and schema name arguments.
     * @throws SQLObjectException 
     */
    public static SQLObject getTableContainer(SQLDatabase db, String catName, String schemaName) throws SQLObjectException {
        db.populate();
        logger.debug("Looking for catalog="+catName+", schema="+schemaName+" in db "+db);
        if (db.getChildType() == SQLTable.class) {
            if (catName != null || schemaName != null) {
                throw new IllegalArgumentException("Catalog or Schema name was given but neither is necessary.");
            }
            return db;
        } else if (db.getChildType() == SQLSchema.class) {
           if (catName != null) {
               throw new IllegalArgumentException("Catalog name was given but is not necessary.");
           }
           if (schemaName == null) {
               throw new IllegalArgumentException("Schema name was expected but none was given.");
           }
           
           return (SQLSchema) db.getChildByNameIgnoreCase(schemaName);
        } else if (db.getChildType() == SQLCatalog.class) {
            if (catName == null) {
                throw new IllegalArgumentException("Catalog name was expected but none was given.");
            }
            SQLCatalog tempCat = db.getCatalogByName(catName);
            
            if (tempCat == null) return null;
            
            tempCat.populate();
            
            logger.debug("Found catalog "+catName+". Child Type="+tempCat.getChildType());
            if (tempCat.getChildType() == SQLSchema.class) {
                if (schemaName == null) {
                    throw new IllegalArgumentException("Schema name was expected but none was given.");
                }
                
                return (SQLSchema) tempCat.getChildByNameIgnoreCase(schemaName);
            }
            
            if (schemaName != null) {
                throw new IllegalArgumentException("Schema name was given but is not necessary.");
            }
            
            return tempCat;
        } else if (db.getChildType() == null) {
            // special case: there are no children of db
            logger.debug("Database "+db+" has no children");
            return null;
        } else {
            throw new IllegalStateException("Unknown database child type: " + db.getChildType());
        }
    }

}
