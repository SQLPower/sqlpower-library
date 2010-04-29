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

import ca.sqlpower.object.ObjectDependentException;
import ca.sqlpower.sqlobject.SQLRelationship.SQLImportedKey;

public class SQLObjectUtils {
    
    private final static Logger logger = Logger.getLogger(SQLObjectUtils.class);
    
    private SQLObjectUtils() {}

    /**
     * This will check if the two objects given are contained in the same
     * root SQLObject by finding their top ancestors and comparing them. o1 and o2
     * cannot be null.
     */
    public static boolean isInSameSession(SQLObject o1, SQLObject o2) {
        SQLObject o1Parent = o1;
        while (o1Parent.getParent() != null && o1Parent.getParent() instanceof SQLObject) {
            o1Parent = (SQLObject) o1Parent.getParent();
        }
        SQLObject o2Parent = o2;
        while (o2Parent.getParent() != null && o2Parent.getParent() instanceof SQLObject) {
            o2Parent = (SQLObject) o2Parent.getParent();
        }
        
        logger.debug("Parent of " + o1 + " is " + o1Parent + ", parent of " + o2 + " is " + o2Parent);
        return o1Parent == o2Parent;
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
            if (!(obj.getParent() instanceof SQLObject)) break;
            obj = (SQLObject) obj.getParent();
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
     * @param childType The type of child that this method should look for to update.
     * @throws SQLObjectException If this exercise causes any SQLObjects to populate, and that
     * populate operation fails.
     */
    public static <T extends SQLObject> void refreshChildren(SQLObject parent, List<T> newChildren, Class<T> childType) throws SQLObjectException {
        Set<String> oldChildNames = parent.getChildNames(childType);
        Set<String> newChildNames = new HashSet<String>(); // will populate in following loop
        for (T newChild : newChildren) {
            newChildNames.add(newChild.getName());
            if (oldChildNames.contains(newChild.getName())) {
                parent.getChildByName(newChild.getName(), childType).updateToMatch(newChild);
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
        	SQLObject removeMe = parent.getChildByName(removedColName, childType);
        	try {
        		parent.removeChild(removeMe);
        	} catch (IllegalArgumentException e) {
        		throw new SQLObjectException(e);
        	} catch (ObjectDependentException e) {
        		throw new SQLObjectException(e);
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
        	if (!schemaContainer.allowsChildType(SQLSchema.class)) {
        		throw new SQLObjectException(
                        "The schema container ("+schemaContainer+
                        ") can't actually contain children of type SQLSchema.");
        	}
            tableContainer = schemaContainer.getChildByName(schema, SQLSchema.class);
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
    		for (SQLObject child : so.getChildren()) {
    			myCount += countTables(child);
    		}
    		return myCount;
    	}
    }

    /**
     * Recursively count tables in the project, including ones that have not
     * been expanded in the DBTree.
     * <p>
     * This has the side effect of populating all of the objects from the given
     * object to the object containing {@link SQLTable}s.
     * 
     * @param source
     *            the source object (usually the database)
     */
    public static int countTablesSnapshot(SQLObject so) throws SQLObjectException {
    	if (so instanceof SQLTable) {
    		return 1;
    	} else {
    		int count = 0;
    		for (SQLObject child : so.getChildren()) {
    			count += countTablesSnapshot(child);
    		}
    	    return count;
    	}
    }

    /**
     * Searches for all SQLObjects under a given starting point which are of the
     * given type. Keep in mind that if you go after anything lower than
     * SQLTable in a lazy-loading database, you will invoke many potentially
     * expensive populate() methods.
     * 
     * @param source
     *            the source object (usually the database)
     * @param clazz
     *            the type of SQLObject to look for.
     * @param addTo
     *            the list to accumulate the results into. This list will likely
     *            be modified.
     * @return the list passed in as the <code>addTo</code> argument.
     */
    public static <T extends SQLObject>
    List<T> findDescendentsByClass(SQLObject so, java.lang.Class<T> clazz, List<T> addTo)
    throws SQLObjectException {
        if (clazz == so.getClass()) {
            addTo.add(clazz.cast(so));
        } else {
            for (SQLObject child : (List<? extends SQLObject>) so.getChildren()) {
                findDescendentsByClass(child, clazz, addTo);
            }
        }
        return addTo;
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
    				if (col.getSourceColumn() != null && source.equals(col.getSourceColumn().getParent().getParentDatabase())) {
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

    /**
     * Returns a list of SQLObjects where the first entity in the list is the
     * highest ancestor and the last object in the list is the given object.
     */
    public static List<SQLObject> ancestorList(SQLObject so) {
        List<SQLObject> ancestors = new LinkedList<SQLObject>();
        while (so != null) {
            ancestors.add(0, so);
            if (!(so.getParent() instanceof SQLObject)) break;
            so = (SQLObject) so.getParent();
        }
        return ancestors;
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
            if (schemaContainer.allowsChildType(SQLSchema.class)){
                tableContainer = schemaContainer.getChildByName(schema, SQLSchema.class);
                if (tableContainer == null) {
                    return true;
                }
            } else {
                return false;
            }
        } else {
            tableContainer = schemaContainer;
        }
    
        if (name != null) {
        	if (tableContainer.allowsChildType(SQLTable.class)) {
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
        if (db.isTableContainer()) {
            if (catName != null || schemaName != null) {
                throw new IllegalArgumentException("Catalog or Schema name was given but neither is necessary.");
            }
            return db;
        } else if (db.isSchemaContainer()) {
           if (catName != null) {
               throw new IllegalArgumentException("Catalog name was given but is not necessary.");
           }
           if (schemaName == null) {
               throw new IllegalArgumentException("Schema name was expected but none was given.");
           }
           
           return (SQLSchema) db.getChildByNameIgnoreCase(schemaName, SQLSchema.class);
        } else if (db.isCatalogContainer()) {
            if (catName == null) {
                throw new IllegalArgumentException("Catalog name was expected but none was given.");
            }
            SQLCatalog tempCat = db.getCatalogByName(catName);
            
            if (tempCat == null) return null;
            
            tempCat.populate();
            
            logger.debug("Found catalog "+catName+". Child Type="+tempCat.getChildrenWithoutPopulating().get(0).getClass());
            if (tempCat.isSchemaContainer()) {
                if (schemaName == null) {
                    throw new IllegalArgumentException("Schema name was expected but none was given.");
                }
                
                return (SQLSchema) tempCat.getChildByNameIgnoreCase(schemaName, SQLSchema.class);
            }
            
            if (schemaName != null) {
                throw new IllegalArgumentException("Schema name was given but is not necessary.");
            }
            
            return tempCat;
        } else if (db.getChildrenWithoutPopulating().isEmpty()) {
            // special case: there are no children of db
            logger.debug("Database "+db+" has no children");
            return null;
        } else {
            throw new IllegalStateException("Unknown database child type: " + db.getChildrenWithoutPopulating().get(0).getClass());
        }
    }

    /**
     * Populates the given object by adding all of the children in the list to
     * the object and setting necessary flags. This method must be called on the
     * foreground and the object cannot be populated already.
     * 
     * @param objectToPopulate
     *            The object to populate with the given child types.
     * @param children
     *            A list of children that will be used to populate the given
     *            object. The object will be defined to be populated for the
     *            child type in the list. The list can contain multiple child
     *            types but must contain all of the children of any one type to
     *            fully populate that type of children for the object.
     */
    public static void populateChildrenWithList(SQLObject objectToPopulate, List<SQLObject> children) {
        if (objectToPopulate instanceof SQLTable) {
            List<SQLColumn> columnChildren = new ArrayList<SQLColumn>();
            List<SQLIndex> indexChildren = new ArrayList<SQLIndex>();
            List<SQLRelationship> relationshipChildren = new ArrayList<SQLRelationship>();
            List<SQLImportedKey> importedKeyChildren = new ArrayList<SQLImportedKey>();
            for (SQLObject o : children) {
                if (o instanceof SQLColumn) {
                    columnChildren.add((SQLColumn) o);
                } else if (o instanceof SQLIndex) {
                    indexChildren.add((SQLIndex) o);
                } else if (o instanceof SQLRelationship) {
                    relationshipChildren.add((SQLRelationship) o);
                } else if (o instanceof SQLImportedKey) {
                    importedKeyChildren.add((SQLImportedKey) o);
                } else {
                    throw new IllegalArgumentException("Unknown child type " + o.getClass() + 
                            " for table " + objectToPopulate + ". Cannot add " + o + " as a child.");
                }
            }

            if (!columnChildren.isEmpty()) {
                SQLTable.populateColumnsWithList((SQLTable) objectToPopulate, columnChildren);
            }
            if (!indexChildren.isEmpty()) {
                SQLTable.populateIndicesWithList((SQLTable) objectToPopulate, indexChildren);
            }

            if (!relationshipChildren.isEmpty()) {
                SQLTable.populateRelationshipsWithList((SQLTable) objectToPopulate, relationshipChildren);
            }

            //Imported keys do not follow the normal pattern as they are not actually
            //populated by calling populate().
            //TODO come up with a better way to satisfy handling imported keys
            //that still doesn't cause a tightly connected graph of tables to be
            //fully populated when populating one table.
            if (!importedKeyChildren.isEmpty()) {
                for (SQLImportedKey child : importedKeyChildren) {
                    ((SQLTable) objectToPopulate).addImportedKey(child);
                }
                ((SQLTable) objectToPopulate).setImportedKeysPopulated(true);
            }

        } else if (objectToPopulate instanceof SQLDatabase) {
            SQLDatabase.populateDatabaseWithList((SQLDatabase) objectToPopulate, children);
        } else if (objectToPopulate instanceof SQLCatalog) {
            SQLCatalog.populateCatalogWithList((SQLCatalog) objectToPopulate, children);
        } else if (objectToPopulate instanceof SQLSchema) {
            List<SQLTable> tables = new ArrayList<SQLTable>();
            for (SQLObject child : children) {
                tables.add((SQLTable) child);
            }
            SQLSchema.populateSchemaWithList((SQLSchema) objectToPopulate, tables);
        } else {
            throw new IllegalArgumentException("Object " + objectToPopulate + " does not " +
            		"allow children and cannot be populated.");
        }
    }

}
