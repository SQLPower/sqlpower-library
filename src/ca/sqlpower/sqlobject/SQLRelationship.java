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
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import ca.sqlpower.object.ObjectDependentException;
import ca.sqlpower.object.SPChildEvent;
import ca.sqlpower.object.SPListener;
import ca.sqlpower.object.SPObject;
import ca.sqlpower.object.annotation.Accessor;
import ca.sqlpower.object.annotation.Constructor;
import ca.sqlpower.object.annotation.ConstructorParameter;
import ca.sqlpower.object.annotation.Mutator;
import ca.sqlpower.object.annotation.Persistable;
import ca.sqlpower.sql.CachedRowSet;
import ca.sqlpower.sqlobject.SQLIndex.Column;
import ca.sqlpower.util.SQLPowerUtils;
import ca.sqlpower.util.TransactionEvent;

/**
 * The SQLRelationship class represents a foreign key relationship between
 * two SQLTable objects or two groups of columns within the same table.
 */
public class SQLRelationship extends SQLObject implements java.io.Serializable {

    /**
     * Comparator that orders ColumnMapping objects by FK column position.
     */
	public static class ColumnMappingFKColumnOrderComparator implements Comparator<ColumnMapping> {
        public int compare(ColumnMapping o1, ColumnMapping o2) {
            int fkPos1 = o1.getFkColumn().getParent().getChildren().indexOf(o1.getFkColumn());
			int fkPos2 = o2.getFkColumn().getParent().getChildren().indexOf(o2.getFkColumn());
			if (fkPos1 == fkPos2) return 0;
			if (fkPos1 < fkPos2) return -1;
			return 1;
        }
    }

    private static Logger logger = Logger.getLogger(SQLRelationship.class);
    
    private List<ColumnMapping> mappings = new ArrayList<ColumnMapping>();
    
    private SQLImportedKey foreignKey;

    /**
     * The enumeration of all referential integrity constraint checking
     * policies.
     */
    public static enum Deferrability {
        
        /**
         * Indicates the constrain is deferrable, and checking is deferred by
         * default unless the current transaction has been set for immediate
         * constraint checking.
         */
        INITIALLY_DEFERRED(5),
        
        /**
         * Indicates the constrain is deferrable, and checking is performed
         * immediately unless the current transaction has been set for deferred
         * constraint checking.
         */
        INITIALLY_IMMEDIATE(6),
        
        /**
         * Indicates that the checking for this constraint must always be immediate
         * regardless of the current transaction setting.
         */
        NOT_DEFERRABLE(7);
        
        /**
         * The JDBC code number for this deferrability policy.
         */
        private final int code;
        
        private Deferrability(int code) {
            this.code = code;
        }
        
        /**
         * Returns the enumeration value associated with the given code number.
         * The code numbers are defined in the JDBC specification.
         * 
         * @throws IllegalArgumentException if the given code number is not valid.
         */
        public static Deferrability ruleForCode(int code) {
            for (Deferrability d : values()) {
                if (d.code == code) return d;
            }
            throw new IllegalArgumentException("No such deferrability code " + code);
        }
        
        /**
         * Returns the enumeration value associated with the given code number,
         * or the given default value if the given code number is not valid.
         * This method exists mainly for backward compatibility with old projects
         * where all the deferrability rules were defaulted to 0, which is an
         * invalid code.  New code should normally be written to use {@link #ruleForCode(int)},
         * which throws an exception when asked for an invalid code.
         */
        public static Deferrability ruleForCode(int code, Deferrability defaultValue) {
            for (Deferrability d : values()) {
                if (d.code == code) return d;
            }
            return defaultValue;
        }
        
        /**
         * Returns the JDBC code number for this deferrability rule.
         */
        public int getCode() {
            return code;
        }
    }
    
    /**
     * Enumeration of the various rules allowed for (foreign/imported/child)
     * columns when their parent value is updated or deleted.
     */
    public static enum UpdateDeleteRule {
        
        /**
         * When parent value changes, child value should be modified to
         * match new parent value.
         */
        CASCADE(DatabaseMetaData.importedKeyCascade),
        
        /**
         * Modifying or deleting the parent value should fail if
         * there are child records. This is different from {@link #NO_ACTION}
         * in that the constraint check will not be deferrable on some
         * platforms.
         */
        RESTRICT(DatabaseMetaData.importedKeyRestrict),
        
        /**
         * The child value will be set to SQL NULL if the parent value
         * is modified or deleted.
         */
        SET_NULL(DatabaseMetaData.importedKeySetNull),
        
        /**
         * Modifying or deleting the parent value should fail if
         * there are child records. This is different from {@link #RESTRICT}
         * in that the constraint checking will be deferrable on some platforms.
         * This is the default update and delete rule on most database platforms.
         */
        NO_ACTION(DatabaseMetaData.importedKeyNoAction),
        
        /**
         * Modifying or deleting the parent value should cause the child
         * value to be set to its default.
         */
        SET_DEFAULT(DatabaseMetaData.importedKeySetDefault);
        
        /**
         * The JDBC code for this update/delete rule.
         */
        private final int code;
        
        private UpdateDeleteRule(int code) {
            this.code = code;
        }
        
        /**
         * Returns the update/delete rule associated with the given code number.
         * The code numbers are defined in the JDBC specification.
         * 
         * @throws IllegalArgumentException if the given code number is not valid.
         */
        public static UpdateDeleteRule ruleForCode(int code) {
            for (UpdateDeleteRule r : values()) {
                if (r.code == code) return r;
            }
            throw new IllegalArgumentException("No such update/delete rule code " + code);
        }
        
        /**
         * Returns the JDBC code number for this update/delete rule.
         */
        public int getCode() {
            return code;
        }
    }

    public static List<SQLRelationship> getExportedKeys(List<SQLImportedKey> foreignKeys) {
    	List<SQLRelationship> primaryKeys = new ArrayList<SQLRelationship>();
    	for (SQLImportedKey k : foreignKeys) {
    		primaryKeys.add(k.getRelationship());
    	}
    	return primaryKeys;
    }
    
	public static final int ZERO = 1;
	public static final int ONE = 2;
	public static final int MANY = 4;
	public static final int PKCOLUMN = 4;
	public static final int FKCOLUMN = 5;

	/**
	 * The rule for what the DBMS should do to the child (imported) key value when its
	 * parent table (exported) key value changes.
	 */
	protected UpdateDeleteRule updateRule = UpdateDeleteRule.NO_ACTION;
	
    /**
     * The rule for what the DBMS should do to the child (imported) key value when its
     * parent table (exported) row is deleted.
     */
	protected UpdateDeleteRule deleteRule = UpdateDeleteRule.NO_ACTION;
    
    /**
     * The deferrability rule for constraint checking on this relationship.
     * Defaults to NOT_DEFERRABLE.
     */
	protected Deferrability deferrability = Deferrability.NOT_DEFERRABLE;

	protected int pkCardinality;
	protected int fkCardinality;
	
	/**
     * Value should be true if this relationship is identifying, and false if
     * otherwise.
     * <p>
     * Here is our definition of identifying relationships and non-identifying
     * relationships (as discussed in the <a
     * href="http://groups.google.com/group/architect-developers/browse_thread/thread/d70e3e3ee3353f1"/>
     * Architect Developer's mailing list</a>).
     * <p>
     * An 'identifying' relationship is: A foreign key relationship in which the
     * whole primary key of the parent table is entirely contained in the
     * primary key of the child table.
     * <p>
     * A 'non-identifying' relationship is: A foreign key relationship in which
     * the whole primary key of the parent table is NOT entirely contained in
     * the primary key of the child table.
     */
	protected boolean identifying;


	protected String physicalName;

	protected RelationshipManager fkColumnManager;

    /**
     * A counter for {@link #setParent()} to decide when to detach listeners.
     */
    private int parentCount;
    
    /**
     * This is the text for parent label of relationship.
     */
    private String textForParentLabel = "";
    
    /**
     * This is the text for child label of relationship.
     */
    private String textForChildLabel = "";

	public SQLRelationship() {
		pkCardinality = ONE;
		fkCardinality = ZERO | ONE | MANY;
		fkColumnManager = new RelationshipManager();
	}
	
	@Constructor
	public SQLRelationship(@ConstructorParameter(propertyName = "foreignKey") SQLImportedKey foreignKey) {
		this();
		this.foreignKey = foreignKey;
	}
	
	/**
     * A copy constructor that returns a copy of the provided SQLRelationship
     * with the following properties copied: 
     * <li> Name </li>
     * <li> Identifying status </li>
     * <li> Update rule </li>
     * <li> Delete rule </li>
     * <li> Deferrability </li>
     * 
     * @param relationshipToCopy
     *            The SQLRelationship object to copy
     */
	public SQLRelationship(SQLRelationship relationshipToCopy) throws SQLObjectException {
	    this();
	    updateToMatch(relationshipToCopy);
	}
	
	@Override
	public final void updateToMatch(SQLObject source) throws SQLObjectException {
	    SQLRelationship relationshipToCopy = (SQLRelationship) source;
        setName(relationshipToCopy.getName());
        setIdentifying(relationshipToCopy.determineIdentifyingStatus());
        setUpdateRule(relationshipToCopy.getUpdateRule());
        setDeleteRule(relationshipToCopy.getDeleteRule());
        setDeferrability(relationshipToCopy.getDeferrability());
        setChildrenInaccessibleReason(relationshipToCopy.getChildrenInaccessibleReason(), false);
        setTextForChildLabel(relationshipToCopy.getTextForChildLabel());
        setTextForParentLabel(relationshipToCopy.getTextForParentLabel());
        
        List<ColumnMapping> columnsToRemove = new ArrayList<ColumnMapping>(getChildrenWithoutPopulating());
        for (ColumnMapping newColMapping : relationshipToCopy.getChildrenWithoutPopulating()) {
        	boolean foundColumn = false;
        	for (int i = columnsToRemove.size() - 1; i >= 0; i--) {
        		ColumnMapping existingMapping = columnsToRemove.get(i);
        		if (existingMapping.getPkColumn().equals(newColMapping.getPkColumn())
        				&& existingMapping.getFkColumn().equals(newColMapping.getFkColumn())) {
        			columnsToRemove.remove(existingMapping);
        			foundColumn = true;
        			break;
        		}
        	}
        	if (!foundColumn) {
        		addChild(newColMapping);
        	}
        }
        for (ColumnMapping removeMe : columnsToRemove) {
        	try {
				removeChild(removeMe);
			} catch (Exception e) {
				throw new SQLObjectException(e);
			}
        }
	}
	
	/**
	 *  Adds a counter to the end of the default column name until
	 *  it is unique in the given table.
	 */
	private static String generateUniqueColumnName(String colName,
	        SQLTable table) throws SQLObjectException {
	    if (table.getColumnByName(colName) == null) return colName;
	    int count = 1;
	    String uniqueName;
	    do {
	        uniqueName = colName + "_" + count; 
	        count++;
	    } while (table.getColumnByName(uniqueName) != null);
	    return uniqueName;
	}

    /**
     * This method is for the benefit of the unit tests.  It should never
     * be necessary to use it in the real world.
     */
    public RelationshipManager getRelationshipManager() {
        return fkColumnManager;
    }

	public void attachListeners() throws SQLObjectException {
		fkColumnManager.target = getParent();
		SQLPowerUtils.listenToHierarchy(getParent(), fkColumnManager);
		SQLPowerUtils.listenToHierarchy(foreignKey.getParent(), fkColumnManager);
	}

	private void detachListeners(){
        if (fkColumnManager.target != null)
            SQLPowerUtils.unlistenToHierarchy(fkColumnManager.target, fkColumnManager);
        if (foreignKey.getParent() != null)
            SQLPowerUtils.unlistenToHierarchy(foreignKey.getParent(), fkColumnManager);
	}

	public void attachRelationship(SQLTable pkTable, SQLTable fkTable, boolean autoGenerateMapping) throws SQLObjectException {
		foreignKey = new SQLImportedKey(this);
		foreignKey.setParent(fkTable);
		try {
			setMagicEnabled(false);
			setParent(pkTable);
		} finally {
			setMagicEnabled(true);
		}
		attachRelationship(autoGenerateMapping);
	}

	/**
	 * Associates an {@link SQLRelationship} with the given {@link SQLTable}
	 * objects. Also automatically generates the PK to FK column mapping if
	 * autoGenerateMapping is set to true.
	 * 
	 * @param pkTable
	 *            The parent table in this relationship.
	 * @param fkTable
	 *            The child table in this relationship that contains the foreign
	 *            key.
	 * @param autoGenerateMapping
	 *            Automatically generates the PK to FK column mapping if true
	 * @param attachListeners
	 *            Attaches listeners to both parent tables if true. If false,
	 *            attachListeners must be called elsewhere.
	 * @throws SQLObjectException
	 */
	private void attachRelationship(boolean autoGenerateMapping) throws SQLObjectException {
		if(getParent() == null) throw new NullPointerException("Null pkTable not allowed");
		SQLTable fkTable = foreignKey.getParent();
		if(fkTable == null) throw new NullPointerException("Null fkTable not allowed");

		detachListeners();

		boolean alreadyExists = false;
		
		for (SQLRelationship r : getParent().getExportedKeysWithoutPopulating()) {
		    if (r.getFkTable().equals(fkTable)) {
		        alreadyExists = true;
		        break;
		    }
		}
		
		if (!getParent().getChildrenWithoutPopulating(SQLRelationship.class).contains(this)) {
			getParent().addChild(this);
		}
		
		if (!fkTable.getChildrenWithoutPopulating(SQLImportedKey.class).contains(foreignKey)) {
			fkTable.addChild(foreignKey);
		}
		
		try {
			fkTable.setMagicEnabled(false);

			if (autoGenerateMapping) {
				// iterate over a copy of pktable's column list to avoid comodification
				// when creating a self-referencing table
				java.util.List<SQLColumn> pkColListCopy = new ArrayList<SQLColumn>(getParent().getColumns().size());
				pkColListCopy.addAll(getParent().getColumns());

				for (SQLColumn pkCol : pkColListCopy) {
					if (pkCol.getPrimaryKeySeq() == null) break;

					SQLColumn match = fkTable.getColumnByName(pkCol.getName());
					SQLColumn fkCol = new SQLColumn(pkCol);
					fkCol.setPrimaryKeySeq(null);
                    if (getParent() == fkTable) {
                        // self-reference should never hijack the PK!
                        String colName = "Parent_" + fkCol.getName();
                        fkCol.setName(generateUniqueColumnName(colName, fkTable));
                        setIdentifying(false);
                    } else if (match == null) { 
                        // no match, so we need to import this column from PK table
                        fkCol.setName(generateUniqueColumnName(pkCol.getName(),fkTable));
                    } else {
						// does the matching column have a compatible data type?
						if (!alreadyExists && match.getType() == pkCol.getType() &&
								match.getPrecision() == pkCol.getPrecision() &&
								match.getScale() == pkCol.getScale()) {
							// column is an exact match, so we don't have to recreate it
							fkCol = match;
						} else {
						    String colName = pkCol.getParent().getName() + "_" + pkCol.getName();
							fkCol.setName(generateUniqueColumnName(colName,fkTable));
						}
                    }
					this.addMapping(pkCol, fkCol);

				}
			}

			realizeMapping();

            // normally, it wouldn't hurt to normalize anyway, but in order to remain
            // backward compatible with older project files that don't have an indicesFolder
            // for each table, we will only do this when asked to auto-generate mappings.
            // If we ever have time (or more likely, it turns out that there are cases where
            // normalizing the PK is required even when autoGenerateMapping is false), we
            // could modify the SwingUIProject to fix up old tables with no PK folder as soon
            // as they're created, then this normalize could happen unconditionally.
            if (autoGenerateMapping) {
                fkTable.normalizePrimaryKey();
            }
            
            this.attachListeners();
		} finally {
			if ( fkTable != null ) {
				fkTable.setMagicEnabled(true);
			}
		}
	}

	/**
	 * Takes the existing ColumnMapping children of this relationship, and ensures
	 * that the FK Columns exist in the FK Table, and that they are in/out of the FK
	 * table's primary key depending on whether or not this is an identifying relationship.
	 *
	 * @throws SQLObjectException If something goes terribly wrong
	 */
    	private void realizeMapping() throws SQLObjectException {
        for (ColumnMapping m : getChildren(ColumnMapping.class)) {
            if (logger.isDebugEnabled()) {
                logger.debug("realizeMapping: processing " + m);
            }
            SQLColumn fkCol = m.getFkColumn();
            try {
                fkCol.setMagicEnabled(false);
                if (fkCol.getReferenceCount() == 0)
                    fkCol.addReference();

                // since we turned magic off, we have to insert the PK cols in
                // the correct position
                int insertIdx;
                if (identifying) {
                    if (fkCol.getPrimaryKeySeq() == null) {
                        logger.debug("realizeMapping: fkCol PK seq is null. Inserting at end of PK.");
                        insertIdx = foreignKey.getParent().getPkSize();
                    } else {
                        logger.debug("realizeMapping: using existing fkCol PK seq " + fkCol.getPrimaryKeySeq());
                        insertIdx = fkCol.getPrimaryKeySeq();
                    }
                } else {
                    if (fkCol.getPrimaryKeySeq() != null) {
                        insertIdx = fkCol.getPrimaryKeySeq();
                    } else {
                        insertIdx = foreignKey.getParent().getColumns().size();
                    }
                }
                
                fkCol.setAutoIncrement(false);

                // This might bump up the reference count (which would be
                // correct)
                foreignKey.getParent().addColumn(fkCol, insertIdx);
                logger.debug("realizeMapping: Added column '" + fkCol.getName() + "' at index " + insertIdx);
                if (fkCol.getReferenceCount() <= 0)
                    throw new IllegalStateException("Created a column with 0 references!");

                if (identifying && fkCol.getPrimaryKeySeq() == null) {
                    fkCol.setPrimaryKeySeq(new Integer(foreignKey.getParent().getPkSize()));
                }

            } finally {
                fkCol.setMagicEnabled(true);
            }
        }
    }

    /**
     * Fetches imported keys for the given table. (Imported keys are the
     * uniquely-indexed columns of other tables that are referenced by (imported
     * from) the given table). Nothing is added to the given table or any other
     * objects, although the returned SQLRelationship column mapping objects
     * will likely have references to columns in that table and others. It is
     * the caller's option whether or not to attach some, all, or none of the
     * returned SQLRelationship objects to the object model. All returned
     * objects can be safely thrown away simply by ignoring them, or kept by
     * invoking their {@link #attachRelationship(SQLTable, SQLTable, boolean)}
     * method.
     * <p>
     * Note that <code>table</code>'s database must be fully populated up to the
     * table level (the tables themselves can be unpopulated) before you call
     * this method; it requires that all referenced tables are represented by
     * in-memory SQLTable objects. The tables this table imports its keys from
     * will be populated as a side effect of this call.
     * 
     * @throws SQLObjectException
     *             if a database error occurs or if the given table's parent
     *             database is not marked as populated.
     */
	static List<SQLRelationship> fetchExportedKeys(SQLTable table)
	throws SQLObjectException {
		SQLDatabase db = table.getParentDatabase();
		if (!db.isPopulated()) {
			throw new SQLObjectException("relationship.unpopulatedTargetDatabase");
		}
		CachedRowSet crs = null;
		ResultSet tempRS = null; // just a temporary place for the live result set. use crs instead.
		Connection con = null;
		try {
		    con = table.getParentDatabase().getConnection();
		    DatabaseMetaData dbmd = con.getMetaData();
			crs = new CachedRowSet();
			tempRS = dbmd.getExportedKeys(table.getCatalogName(),
                        			      table.getSchemaName(),
                        			      table.getName());
            crs.populate(tempRS);
		} catch (SQLException e) {
		    throw new SQLObjectException("relationship.populate", e);
		} finally {
            try {
                if (tempRS != null) tempRS.close();
            } catch (SQLException e) {
                logger.warn("Couldn't close imported keys result set", e);
            }
            try {
                if (con != null) con.close();
            } catch (SQLException e) {
                logger.warn("Couldn't close connection", e);
            }
		}
		try {
			int currentKeySeq;
			List<SQLRelationship> newKeys = new LinkedList<SQLRelationship>();

			logger.debug("search relationship for table:"+table.getCatalogName()+"."+
					table.getSchemaName()+"."+
					table.getName());

			SQLRelationship r = null;
			while (crs.next()) {
				currentKeySeq = crs.getInt(9);
				if (currentKeySeq == 1) {
					SQLImportedKey foreignKey = new SQLImportedKey(r);
					foreignKey.setParent(db.getTableByName(	crs.getString(5),  // catalog
							crs.getString(6), // schema
							crs.getString(7))); // table
					
					r = new SQLRelationship(foreignKey);
					newKeys.add(r);
				}
				try {
					r.setMagicEnabled(false);
					ColumnMapping m = new ColumnMapping();
					r.addMapping(m);
					String pkCat = crs.getString(1);
					String pkSchema = crs.getString(2);
					String pkTableName = crs.getString(3);

					r.setParent(db.getTableByName(pkCat,  // catalog
							pkSchema,  // schema
							pkTableName)); // table

					if (r.getParent() == null) {
						logger.error("addImportedRelationshipsToTable: Couldn't find exporting table "
								+pkCat+"."+pkSchema+"."+pkTableName
								+" in target database!");
						continue;
					}

					if (r.getParent() != table) {
						throw new IllegalStateException("fkTable did not match requested table");
					}
					
					logger.debug("Looking for pk column '"+crs.getString(4)+"' in table '"+r.getParent()+"'");
					m.pkColumn = r.getParent().getColumnByName(crs.getString(4));
					if (m.pkColumn == null) {
						throw new SQLObjectException("relationship.populate.nullPkColumn");
					}
					
					m.fkColumn = r.getFkTable().getColumnByName(crs.getString(8));
					if (m.fkColumn == null) {
						throw new SQLObjectException("relationship.populate.nullFkColumn");
					}
					// column 9 (currentKeySeq) handled above
					r.updateRule = UpdateDeleteRule.ruleForCode(crs.getInt(10));
					r.deleteRule = UpdateDeleteRule.ruleForCode(crs.getInt(11));
					r.setName(crs.getString(12));
					try {
						r.deferrability = Deferrability.ruleForCode(crs.getInt(14));
					} catch (IllegalArgumentException ex) {
						logger.warn("Invalid code when reverse engineering" +
								" relationship. Defaulting to NOT_DEFERRABLE.", ex);
						r.deferrability = Deferrability.NOT_DEFERRABLE;
					}
				} finally {
					r.setMagicEnabled(true);
				}
			}

			return newKeys;
			
		} catch (SQLException e) {
			throw new SQLObjectException("relationship.populate", e);
		} finally {
			try {
				if (crs != null) crs.close();
			} catch (SQLException e) {
				logger.warn("Couldn't close resultset", e);
			}
		}
	}
	
	public ColumnMapping getMappingByPkCol(SQLColumn pkcol) {
		for (ColumnMapping m : mappings) {
			if (m.pkColumn == pkcol) {
				return m;
			}
		}
		return null;
	}

	public boolean containsPkColumn(SQLColumn col) {
		return getMappingByPkCol(col) != null;
	}

	public ColumnMapping getMappingByFkCol(SQLColumn fkcol) {
		for (ColumnMapping m : mappings) {
			if (m.fkColumn == fkcol) {
				return m;
			}
		}
		return null;
	}

	public boolean containsFkColumn(SQLColumn col) {
		return getMappingByFkCol(col) != null;
	}

	public String printKeyColumns(int keyType) {
		StringBuffer s = new StringBuffer();
		int i = 0;
		for (ColumnMapping cm : mappings) {
			if ( i++ > 0 )
				s.append(",");
			if ( keyType == PKCOLUMN )
				s.append(cm.getPkColumn().getName());
			else
				s.append(cm.getFkColumn().getName());
		}
		return s.toString();
	}

	@Override
	protected void addChildImpl(SPObject child, int index) {
		if (child instanceof ColumnMapping) {
			addMapping((ColumnMapping) child, index);
		} else {
			throw new IllegalArgumentException("The child " + child.getName() + 
					" of type " + child.getClass() + " is not a valid child type of " + 
					getClass() + ".");
		}
	}
	
	/**
	 * Convenience method for adding a SQLRelationship.ColumnMapping
	 * child to this relationship.
	 * @throws SQLObjectException
	 */
	public void addMapping(SQLColumn pkColumn, SQLColumn fkColumn) throws SQLObjectException {
		ColumnMapping cmap = new ColumnMapping();
		cmap.setPkColumn(pkColumn);
		cmap.setFkColumn(fkColumn);

		logger.debug("add column mapping: "+pkColumn.getParent()+"." +
				pkColumn.getName() + " to " +
				fkColumn.getParent()+"."+fkColumn.getName() );

		addMapping(cmap);
	}
	
	public void addMapping(ColumnMapping mapping) {
		addMapping(mapping, mappings.size());
	}
	
	public void addMapping(ColumnMapping mapping, int index) {
		mappings.add(index, mapping);
		mapping.setParent(this);
		fireChildAdded(ColumnMapping.class, mapping, index);
	}

	public String toString() {
		return getShortDisplayName();
	}

	// ------------------ SQLObject Listener ---------------------

	/**
	 * Listens to all activity at and under the pkTable and fkTable.  Updates
	 * and maintains the mapping from pkTable to fkTable, and even removes the
	 * whole relationship when necessary.
	 */
	protected class RelationshipManager implements SPListener {
		
		private SPObject target;
		
		public void childAdded(SPChildEvent e) {
			if (!((SQLObject) e.getSource()).isMagicEnabled()){
				logger.debug("Magic disabled; ignoring children inserted event "+e);
				return;
			}
			if (logger.isDebugEnabled()) {
				logger.debug("childAdded event! parent="+e.getSource()+";" +
						" child="+e.getChild());
			}
			SQLPowerUtils.listenToHierarchy(e.getChild(), this);
			
			// Code from dbChildrenInserted
			if (e.getSource() == getParent() && e.getChildType() == SQLColumn.class) {
				SQLColumn col = (SQLColumn) e.getChild();
				try {
					if (col.getPrimaryKeySeq() != null) {
						ensureInMapping(col);
					} else {
						ensureNotInMapping(col);
					}
				} catch (SQLObjectException ex) {
					logger.warn("Couldn't add/remove mapped FK columns", ex);
				}
			}
		}

		public void childRemoved(SPChildEvent e) {
			if (!((SQLObject) e.getSource()).isMagicEnabled()){
				logger.debug("Magic disabled; ignoring child removed event "+e);
				return;
			}
			if (logger.isDebugEnabled()) {
				logger.debug("dbChildrenRemoved event! parent="+e.getSource()+";" +
						" child="+e.getChild());
			}
			SQLPowerUtils.unlistenToHierarchy(e.getChild(), this);

			// Code from dbChildrenRemoved
			if (e.getChild() == SQLRelationship.this || e.getChild() == foreignKey) {
				detachListeners();
				try {
					if (e.getChild() == SQLRelationship.this) {
						SQLImportedKey fk = foreignKey;
						setForeignKey(null);
						fk.getParent().removeChild(fk);
					} else {
						getParent().removeChild(SQLRelationship.this);
					}
				} catch (ObjectDependentException e1) {
					throw new RuntimeException(e1); // This should not happen
				}

				logger.debug("Removing references for mappings: "+getChildren());

				// references to fk columns are removed in reverse order in case
				// this relationship is reconnected in the future. (if not removed
				// in reverse order, the PK sequence numbers will change as each
				// mapping is removed and the subsequent column indexes shift down)
				List<ColumnMapping> mappings = new ArrayList<ColumnMapping>(getChildren(ColumnMapping.class));
				Collections.sort(mappings, Collections.reverseOrder(new ColumnMappingFKColumnOrderComparator()));
				for (ColumnMapping cm : mappings) {
					logger.debug("Removing reference to fkcol "+ cm.getFkColumn());
					cm.getFkColumn().removeReference();
				}
			} else if (e.getChildType() == SQLColumn.class) {
				SQLColumn col = (SQLColumn) e.getChild();
				try {
					ensureNotInMapping(col);
				} catch (SQLObjectException ex) {
					logger.warn("Couldn't remove mapped FK columns", ex);
				}
			}
		}

		public void propertyChange(PropertyChangeEvent e) {
			if (!((SQLObject) e.getSource()).isMagicEnabled()){
				logger.debug("Magic disabled; ignoring sqlobject changed event "+e);
				return;
			}
			String prop = e.getPropertyName();
			if (logger.isDebugEnabled()) {
				logger.debug("Property changed!" +
						"\n source=" + e.getSource() +
						"\n property=" + prop +
						"\n old=" + e.getOldValue() +
						"\n new=" + e.getNewValue());
			}
			if (e.getSource() instanceof SQLColumn) {
				SQLColumn col = (SQLColumn) e.getSource();

				if (col.getParent() != null && col.getParent().equals(getParent())) {
					if (prop.equals("primaryKeySeq")) {
						try {
							if (col.getPrimaryKeySeq() != null) {
								ensureInMapping(col);
							} else {
								ensureNotInMapping(col);
							}
						} catch (SQLObjectException ae) {
							throw new SQLObjectRuntimeException(ae);
						}
						return;
					}

					ColumnMapping m = getMappingByPkCol(col);
					if (m == null) {
						logger.debug("Ignoring change for column "+col+" parent "+col.getParent());
						return;
					}
					if (m.getPkColumn() == null) throw new NullPointerException("Missing pk column in mapping");
					if (m.getFkColumn() == null) throw new NullPointerException("Missing fk column in mapping");

					if (prop == null
							|| prop.equals("parent")
							|| prop.equals("remarks")
							|| prop.equals("autoIncrement")) {
						// don't care
					} else if (prop.equals("sourceColumn")) {
						m.getFkColumn().setSourceColumn(m.getPkColumn().getSourceColumn());
					} else if (prop.equals("name")) {
						// only update the fkcol name if its name was the same as the old pkcol name
						if (m.getFkColumn().getName().equalsIgnoreCase((String) e.getOldValue())) {
							m.getFkColumn().setName(m.getPkColumn().getName());
						}
					} else if (prop.equals("type")) {
						m.getFkColumn().setType(m.getPkColumn().getType());
					} else if (prop.equals("sourceDataTypeName")) {
						m.getFkColumn().setSourceDataTypeName(m.getPkColumn().getSourceDataTypeName());
					} else if (prop.equals("scale")) {
						m.getFkColumn().setScale(m.getPkColumn().getScale());
					} else if (prop.equals("precision")) {
						m.getFkColumn().setPrecision(m.getPkColumn().getPrecision());
					} else if (prop.equals("nullable")) {
						m.getFkColumn().setNullable(m.getPkColumn().getNullable());
					} else if (prop.equals("defaultValue")) {
						m.getFkColumn().setDefaultValue(m.getPkColumn().getDefaultValue());
					} else {
						logger.warn("Warning: unknown column property "+prop
								+" changed while monitoring pkTable");
					}
				}
			} else if (e.getSource() == foreignKey.getParent() || e.getSource() == getParent()) {
				if (prop.equals("parent") && e.getNewValue() == null) {
					// this will cause a callback to this listener which removes the imported key from fktable
					getParent().removeExportedKey(SQLRelationship.this);
				}
			}
		}
		
		public void transactionStarted(TransactionEvent e) {
			// RelationshipManager does not respect transactions
			
		}
		
		public void transactionEnded(TransactionEvent e) {
			// RelationshipManager does not respect transactions
			
		}
		
		public void transactionRollback(TransactionEvent e) {
			// RelationshipManager does not respect transactions
		}

        // XXX this code serves essentially the same purpose as the loop in realizeMapping().
        //     We should refactor that method to use this one as a subroutine, and at that
        //     time, ensure the special cases in both places are preserved.
        //     (if there is a special case in there that's not here, it's probably a bug)
		protected void ensureInMapping(SQLColumn pkcol) throws SQLObjectException {
		    if (!containsPkColumn(pkcol)) {
		        if (logger.isDebugEnabled()) {
		            logger.debug("ensureInMapping("+getName()+"): Adding "
		                    +pkcol.getParent().getName()+"."+pkcol.getName()
		                    +" to mapping");
		        }
                
                SQLColumn fkcol;
		        if (pkcol.getParent().equals(foreignKey.getParent())) {
                    // self-reference! must create new column!
                    fkcol = new SQLColumn(pkcol);
                    fkcol.setName(generateUniqueColumnName("Parent_"+pkcol.getName(), pkcol.getParent()));
                } else {
                    fkcol = foreignKey.getParent().getColumnByName(pkcol.getName());
                    if (fkcol == null) fkcol = new SQLColumn(pkcol);
                }
                
                // this either adds the new column or bumps up the refcount on existing col
		        foreignKey.getParent().addColumn(fkcol);
                
		        if (identifying && getParent() != foreignKey.getParent()) {
		            fkcol.setPrimaryKeySeq(new Integer(foreignKey.getParent().getPkSize()));
		        } else {
		            // XXX might only want to do this if fkcol was newly created
		            fkcol.setPrimaryKeySeq(null);
		        }
		        logger.debug("ensureInMapping("+getName()+"): added fkcol at pkSeq "+fkcol.getPrimaryKeySeq());
		        fkcol.setAutoIncrement(false);
		        addMapping(pkcol, fkcol);
		    }
		}

		/**
		 * Ensures there is no mapping for pkcol in this relationship.
		 * If there was, it is removed along with the column that may
		 * have been pushed into the relationship's fkTable.
		 */
		protected void ensureNotInMapping(SQLColumn pkcol) throws SQLObjectException {
			logger.debug("Removing "+pkcol.getParent()+"."+pkcol+" from mapping");
			if (containsPkColumn(pkcol)) {
				ColumnMapping m = getMappingByPkCol(pkcol);
				try {
					removeChild(m);
				} catch (IllegalArgumentException e) {
					throw new SQLObjectException(e);
				} catch (ObjectDependentException e) {
					throw new SQLObjectException(e);
				}
				try {
                    // XXX no magic here? this is suspect
					m.getFkColumn().setMagicEnabled(false);
					m.getFkColumn().removeReference();
				} finally {
					m.getFkColumn().setMagicEnabled(true);
				}
			}
		}

		@Override
		public String toString() {
			return "RelManager of "+SQLRelationship.this.toString();
		}
	}

	// ---------------------- SQLRelationship SQLObject support ------------------------

	/**
	 * Returns the foreign key name.
	 */
	@Accessor
	public String getShortDisplayName() {
		return getName();
	}

	/**
	 * Relationships have ColumnMapping children.
	 *
	 * @return true
	 */
	public boolean allowsChildren() {
		return true;
	}

	/**
	 * This class is not a lazy-loading class.  This call does nothing.
	 */
	protected void populateImpl() {
		// nothing to do.
	}

	/**
	 * Returns true.
	 */
	@Accessor
	public boolean isPopulated() {
		return true;
	}


	// ----------------- accessors and mutators -------------------

	@Accessor
	public UpdateDeleteRule getUpdateRule()  {
		return this.updateRule;
	}

	@Mutator
	public void setUpdateRule(UpdateDeleteRule rule) {
	    UpdateDeleteRule oldRule = updateRule;
	    updateRule = rule;
		firePropertyChange("updateRule", oldRule, rule);
	}

	@Accessor
	public UpdateDeleteRule getDeleteRule()  {
		return this.deleteRule;
	}

	@Mutator
	public void setDeleteRule(UpdateDeleteRule rule) {
        UpdateDeleteRule oldRule = deleteRule;
        deleteRule = rule;
        firePropertyChange("deleteRule", oldRule, rule);
	}

	@Accessor
	public Deferrability getDeferrability()  {
		return this.deferrability;
	}

	@Mutator
	public void setDeferrability(Deferrability argDeferrability) {
        if (argDeferrability == null) {
            throw new NullPointerException("Deferrability policy must not be null");
        }
        Deferrability oldDefferability = this.deferrability;
		this.deferrability = argDeferrability;
		firePropertyChange("deferrability",oldDefferability,argDeferrability);
	}


	/**
	 * Gets the value of pkCardinality
	 *
	 * @return the value of pkCardinality
	 */
	@Accessor
	public int getPkCardinality()  {
		return this.pkCardinality;
	}

	/**
	 * Sets the value of pkCardinality
	 *
	 * @param argPkCardinality Value to assign to this.pkCardinality
	 */
	@Mutator
	public void setPkCardinality(int argPkCardinality) {
		int oldPkCardinality = this.pkCardinality;
		this.pkCardinality = argPkCardinality;
		firePropertyChange("pkCardinality",oldPkCardinality,argPkCardinality);
	}

	/**
	 * Gets the value of fkCardinality
	 *
	 * @return the value of fkCardinality
	 */
	@Accessor
	public int getFkCardinality()  {
		return this.fkCardinality;
	}

	/**
	 * Sets the value of fkCardinality
	 *
	 * @param argFkCardinality Value to assign to this.fkCardinality
	 */
	@Mutator
	public void setFkCardinality(int argFkCardinality) {
		begin("Modify the Foreign key cardinality");
		int oldFkCardinality = this.fkCardinality;
		this.fkCardinality = argFkCardinality;
		firePropertyChange("fkCardinality",oldFkCardinality,argFkCardinality);
		commit();
	}

	/**
	 * Gets the value of identifying
	 *
	 * @return the value of identifying
	 */
	@Accessor
	public boolean isIdentifying()  {
		return this.identifying;
	}

	/**
	 * Sets the value of identifying, and moves the FK columns into or
	 * out of the FK Table's primary key as appropriate.
	 *
	 * @param argIdentifying Value to assign to this.identifying
	 */
	@Mutator
	public void setIdentifying(boolean argIdentifying) throws SQLObjectException {
		boolean oldIdentifying = this.identifying;
		if (identifying != argIdentifying) {
			identifying = argIdentifying;
			firePropertyChange("identifying",oldIdentifying,argIdentifying);
			if (identifying) {
				for (ColumnMapping m : getChildren(ColumnMapping.class)) {
					if (m.getFkColumn().getPrimaryKeySeq() == null) {
						m.getFkColumn().setPrimaryKeySeq(new Integer(foreignKey.getParent().getPkSize()));
					}
				}
			} else {
				for (ColumnMapping m : getChildren(ColumnMapping.class)) {
					if (m.getFkColumn().getPrimaryKeySeq() != null) {
						m.getFkColumn().setPrimaryKeySeq(null);
					}
				}
			}
		}
	}
	
	@Accessor
	public SQLTable getParent() {
		return (SQLTable) super.getParent();
	}
	
	public SQLTable getPkTable() {
		return getParent();
	}

	@Accessor
	public SQLImportedKey getForeignKey() {
		return foreignKey;
	}
	
	@Mutator
	public void setForeignKey(SQLImportedKey k) {
		SQLImportedKey oldVal = foreignKey;
		foreignKey = k;
		firePropertyChange("foreignKey", oldVal, k);
	}
	
	@Mutator
	public void setParent(SPObject parent) {
		SPObject oldVal = getParent();
		super.setParent(parent);
		if (isMagicEnabled() && parent != null && parent != oldVal) {
			try {
				attachRelationship(true);
			} catch (SQLObjectException e) {
				throw new RuntimeException(e);
			}
		}
	}
	
	public SQLTable getFkTable() {
		if (foreignKey != null) {
			return foreignKey.getParent();
		} else {
			return null;
		}
	}

	/**
	 * This class acts a wrapper around a SQLRelationship. It should be added to
	 * the foreign key table as a child, and is depended on by the Relationship.
	 */
	@Persistable
	public static class SQLImportedKey extends SQLObject {

		private SQLRelationship relationship;

		@Constructor
		public SQLImportedKey() {
			super();
		}
		
		public SQLImportedKey(SQLRelationship relationship) {
			super();
			this.relationship = relationship;
		}
		
		@Override
		public boolean allowsChildren() {
			return false;
		}
		
		@Override
		public String getName() {
			if (relationship != null) {
				return relationship.getName();
			} else {
				return super.getName();
			}
		}
		
		@Override
		public SQLTable getParent() {
			return (SQLTable) super.getParent();
		}
		
		@Override
		public List<? extends SQLObject> getChildrenWithoutPopulating() {
			return Collections.emptyList();
		}

		@Override
		public String getShortDisplayName() {
			return relationship.getShortDisplayName();
		}

		@Override
		protected void populateImpl() throws SQLObjectException {
			//no-op
		}

		@Override
		protected boolean removeChildImpl(SPObject child) {
			return false;
		}

		public int childPositionOffset(Class<? extends SPObject> childType) {
			throw new IllegalArgumentException("Cannot retrieve the child position offset of " + 
					childType + " but " + getClass() + " does not allow children.");
		}

		public List<Class<? extends SPObject>> getAllowedChildTypes() {
			return Collections.emptyList();
		}

		public List<? extends SPObject> getDependencies() {
			return Collections.emptyList();
		}

		public void removeDependency(SPObject dependency) {
			getParent().removeImportedKey(this);
		}
		
		@Accessor
		public SQLRelationship getRelationship() {
			return relationship;
		}

		/**
		 * Note that this should probably not be called outside of the
		 * persistence framework.
		 */
		@Mutator
		public void setRelationship(SQLRelationship relationship) {
			SQLRelationship oldVal = this.relationship;
			this.relationship = relationship;
			firePropertyChange("relationship", oldVal, relationship);
		}
	}
	
	// -------------------------- COLUMN MAPPING ------------------------

	@Persistable
	public static class ColumnMapping extends SQLObject {
		protected SQLRelationship parent;
		protected SQLColumn pkColumn;
		protected SQLColumn fkColumn;

		@Constructor
		public ColumnMapping() {
		}

		/**
		 * Gets the value of pkColumn
		 *
		 * @return the value of pkColumn
		 */
		@Accessor
		public SQLColumn getPkColumn()  {
			return this.pkColumn;
		}

		/**
		 * Sets the value of pkColumn
		 *
		 * @param argPkColumn Value to assign to this.pkColumn
		 */
		@Mutator
		public void setPkColumn(SQLColumn argPkColumn) {
			this.pkColumn = argPkColumn;
		}

		/**
		 * Gets the value of fkColumn
		 *
		 * @return the value of fkColumn
		 */
		@Accessor
		public SQLColumn getFkColumn()  {
			return this.fkColumn;
		}

		/**
		 * Sets the value of fkColumn
		 *
		 * @param argFkColumn Value to assign to this.fkColumn
		 */
		@Mutator
		public void setFkColumn(SQLColumn argFkColumn) {
			this.fkColumn = argFkColumn;
		}

		public String toString() {
			return getShortDisplayName();
		}

		// ---------------------- ColumnMapping SQLObject support ------------------------

		/**
		 * Returns the table that holds the primary keys (the imported table).
		 */
		@Accessor
		public SQLRelationship getParent() {
			return (SQLRelationship) parent;
		}

		@Mutator
		protected void setParent(SQLObject newParent) {
			parent = (SQLRelationship) newParent;
		}

		public String getName() {
			return "Column Mapping";
		}

		/**
		 * Returns the table and column name of the pkColumn.
		 */
		public String getShortDisplayName() {
			if (pkColumn == null || fkColumn == null) return "Incomplete mapping";
			
			String pkTableName = null;
			if (pkColumn.getParent() != null) {
				pkTableName = pkColumn.getParent().getName();
			}
			return fkColumn.getName()+" - "+
				pkTableName+"."+pkColumn.getName();
		}

		/**
		 * Mappings do not contain other SQLObjects.
		 *
		 * @return false
		 */
		public boolean allowsChildren() {
			return false;
		}

		/**
		 * This class is not a lazy-loading class.  This call does nothing.
		 */
		protected void populateImpl() throws SQLObjectException {
			return;
		}

		/**
		 * Returns true.
		 */
		public boolean isPopulated() {
			return true;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof ColumnMapping) {
				ColumnMapping cmap = (ColumnMapping) obj;
				return fkColumn == cmap.fkColumn &&
						pkColumn == cmap.pkColumn;
			}
			return false;
		}
		
		@Override
		public int hashCode() {
			int result = 17;
			result = result * 31 + (fkColumn == null? 0 : fkColumn.hashCode());
			result = result * 31 + (pkColumn == null? 0 :pkColumn.hashCode());
			return result;
		}

		@Override
		public List<? extends SQLObject> getChildrenWithoutPopulating() {
			return Collections.emptyList();
		}

		@Override
		protected boolean removeChildImpl(SPObject child) {
			return false;
		}

		public int childPositionOffset(Class<? extends SPObject> childType) {
			throw new IllegalArgumentException("Cannot retrieve the child position offset of " + 
					childType + " but " + getClass() + " does not allow children.");
		}

		public List<? extends SPObject> getDependencies() {
			List<SPObject> dependencies = new ArrayList<SPObject>();
			dependencies.add(getFkColumn());
			dependencies.add(getPkColumn());
			return dependencies;
		}

		public void removeDependency(SPObject dependency) {
			if (dependency == getFkColumn() || dependency == getPkColumn()) {
				getParent().removeColumnMapping(this);
			}
		}

		public List<Class<? extends SPObject>> getAllowedChildTypes() {
			return Collections.emptyList();
		}

	}

	/**
	 * Throws a column locked exception if col is in a columnmapping of this relationship
	 *
	 * @param col
	 * @throws LockedColumnException
	 */
	public void checkColumnLocked(SQLColumn col) throws LockedColumnException {
		for (SQLRelationship.ColumnMapping cm : getChildren(ColumnMapping.class)) {
			if (cm.getFkColumn() == col) {
				throw new LockedColumnException(this,col);
			}
		}
	}
	
	/**
     * Some SQLRelationship objects may not have their {@link #identifying}
     * property set properly which is particularly the case then creating
     * SQLRelationships for source database objects and then reverse
     * engineering, so this method will determine for certain if a relationship
     * is identifying or non-identifying. This is currently primarily being used
     * for determining the identifying status of reverse-engineered
     * relationships.
     * 
     * @return True if this SQLRelationship is identifying. False if it is
     *         non-identifying.
     */
	public boolean determineIdentifyingStatus() throws SQLObjectException {
	    
	    if (getPkTable().getPkSize() > getFkTable().getPkSize()) return false;
	    
	    List<ColumnMapping> columnMappings = (List<ColumnMapping>)getChildren();
	    SQLIndex pkTablePKIndex = getPkTable().getPrimaryKeyIndex();
	    if (pkTablePKIndex == null) return false;
	    List<Column> pkColumns = pkTablePKIndex.getChildren(Column.class);
	    
	    for (Column col: pkColumns) {
	        boolean colIsInFKTablePK = false;
	        for (ColumnMapping mapping: columnMappings) {
	            if (mapping.getPkColumn().equals(col.getColumn()) &&
	                    mapping.getFkColumn().isPrimaryKey()) { 
                    colIsInFKTablePK = true;
                    break;
                }
	        }
	        if (colIsInFKTablePK == false) return false;
	    }
	    return true;
	}

    public static SQLRelationship createRelationship(SQLTable pkTable, SQLTable fkTable, boolean identifying)
            throws SQLObjectException {
        SQLRelationship model = new SQLRelationship();
        // XXX: need to ensure uniqueness of setName(), but 
        // to_identifier should take care of this...		
        StringBuilder sb = new StringBuilder();
        if (pkTable.getPhysicalName() == null || pkTable.getPhysicalName().trim().equals("")) {
        	sb.append(pkTable.getName());
        } else {
        	sb.append(pkTable.getPhysicalName());
        }
        sb.append("_");
        if (fkTable.getPhysicalName() == null || fkTable.getPhysicalName().trim().equals("")) {
        	sb.append(fkTable.getName());
        } else {
        	sb.append(fkTable.getPhysicalName());
        }
        sb.append("_fk");
        model.setName(sb.toString());  //$NON-NLS-1$ //$NON-NLS-2$
        model.setIdentifying(identifying);
        model.attachRelationship(pkTable,fkTable,true);
        return model;
    }
    
    @Override
    public boolean equals(Object obj) {
    	if (obj instanceof SQLRelationship) {
    		SQLRelationship rel = (SQLRelationship) obj;
    		if (deferrability == rel.deferrability &&
    				deleteRule == rel.deleteRule &&
    				fkCardinality == rel.fkCardinality &&
    				getFkTable() == rel.getFkTable() &&
    				identifying == rel.identifying &&
    				physicalName == rel.physicalName &&
    				pkCardinality == rel.pkCardinality &&
    				getParent() == rel.getParent() &&
    				updateRule == rel.updateRule) {
    			return getChildren().equals(rel.getChildren());
    		}
    	}
    	return false;
    }
    
    @Override
    public int hashCode() {
    	int result = 17;
    	result = 31 * result + (deferrability == null? 0 : deferrability.hashCode());
    	result = 31 * result + (deleteRule == null? 0 : deleteRule.hashCode());
    	result = 31 * result + fkCardinality;
    	result = 31 * result + ((foreignKey == null || foreignKey.getParent() == null) ? 0 : foreignKey.getParent().hashCode());
    	result = 31 * result + (identifying?1:0);
    	result = 31 * result + (physicalName == null? 0 : physicalName.hashCode());
    	result = 31 * result + pkCardinality;
    	result = 31 * result + (getParent() == null? 0 : getParent().hashCode());
    	result = 31 * result + (updateRule == null? 0 : updateRule.hashCode());
    	result = 31 * result + getChildren().hashCode();
    	return result;
    }

    @Mutator
	public void setTextForParentLabel(String textForParentLabel) {
		String oldVal = this.textForParentLabel;
		this.textForParentLabel = textForParentLabel;
		firePropertyChange("textForParentLabel", oldVal, textForParentLabel);
	}

    @Accessor
	public String getTextForParentLabel() {
		return textForParentLabel;
	}

    @Mutator
	public void setTextForChildLabel(String textForChildLabel) {
		String oldVal = this.textForChildLabel;
		this.textForChildLabel = textForChildLabel;
		firePropertyChange("textForChildLabel", oldVal, textForChildLabel);
	}

    @Accessor
	public String getTextForChildLabel() {
		return textForChildLabel;
	}

	@Override
	public List<ColumnMapping> getChildrenWithoutPopulating() {
		return Collections.unmodifiableList(mappings);
	}
	
	@Override
	protected boolean removeChildImpl(SPObject child) {
		if (child instanceof ColumnMapping) {
			return removeColumnMapping((ColumnMapping) child);
		} else {
			throw new IllegalArgumentException("Cannot remove children of type " 
					+ child.getClass() + " from " + getName());
		}
	}
	
	public boolean removeColumnMapping(ColumnMapping child) {
		if (isMagicEnabled() && child.getParent() != this) {
			throw new IllegalStateException("Cannot remove child " + child.getName() + 
					" of type " + child.getClass() + " as its parent is not " + getName());
		}
		int index = mappings.indexOf(child);
		if (index != -1) {
			mappings.remove(index);
			fireChildRemoved(SQLTable.class, child, index);
			child.setParent(null);
			return true;
		}
		return false;
	}

	public int childPositionOffset(Class<? extends SPObject> childType) {
		if (childType == ColumnMapping.class) {
			return 0;
		}
		
		throw new IllegalArgumentException("The type " + childType + 
				" is not a valid child type of " + getName());
	}

	public List<? extends SPObject> getDependencies() {
		return Collections.singletonList(foreignKey);
	}

	public void removeDependency(SPObject dependency) {
		for (SQLObject child : getChildren()) {
			child.removeDependency(dependency);
		}
	}

	public List<Class<? extends SPObject>> getAllowedChildTypes() {
		List<Class<? extends SPObject>> types = new ArrayList<Class<? extends SPObject>>();
		types.add(ColumnMapping.class);
		return Collections.unmodifiableList(types);
	}
}
