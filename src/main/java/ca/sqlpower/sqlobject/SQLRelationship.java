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
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import ca.sqlpower.object.AbstractSPListener;
import ca.sqlpower.object.ObjectDependentException;
import ca.sqlpower.object.SPChildEvent;
import ca.sqlpower.object.SPListener;
import ca.sqlpower.object.SPObject;
import ca.sqlpower.object.annotation.Accessor;
import ca.sqlpower.object.annotation.Constructor;
import ca.sqlpower.object.annotation.ConstructorParameter;
import ca.sqlpower.object.annotation.ConstructorParameter.ParameterType;
import ca.sqlpower.object.annotation.Mutator;
import ca.sqlpower.object.annotation.NonProperty;
import ca.sqlpower.object.annotation.Transient;
import ca.sqlpower.sql.CachedRowSet;
import ca.sqlpower.sqlobject.SQLIndex.Column;
import ca.sqlpower.util.SQLPowerUtils;
import ca.sqlpower.util.SessionNotFoundException;

/**
 * The SQLRelationship class represents a foreign key relationship between
 * two SQLTable objects or two groups of columns within the same table.
 */
public class SQLRelationship extends SQLObject implements java.io.Serializable {
	
	/**
	 * Defines an absolute ordering of the child types of this class.
	 */
	public static final List<Class<? extends SPObject>> allowedChildTypes = 
		Collections.<Class<? extends SPObject>>singletonList(ColumnMapping.class);

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
    
    /**
	 * If true then one side of this relationship is being disconnected from
	 * its parent table and the manager is making a call to remove the
	 * relationship from the other parent. Then this relationship should not
	 * go back to the first table and try to remove the relationship again
	 * or else it will bounce back and forth between the tables like a bad
	 * game of pong.
	 */
	private boolean isDisconnecting = false;
    
    /**
     * This is the imported side of this SQLRelationship. The imported key holds
     * a reference to this relationship. The imported key is added as a child to
     * the child table to let the relationship exist as a child of both the
     * parent and child tables.
     * <p>
     * It is important that the foreign key be final otherwise you can very
     * quickly and easily break the relationship as the two ends float apart or
     * you end up with two ends on one side pointing to the same object on the
     * other side. If you feel like you need to make this value non-final or
     * make a setter for it think hard about your reasoning or if there is a
     * different way.
     */
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

	/**
	 * A convenience method for turning a list of SQLImported keys into a list
	 * of their corresponding SQLRelationships
	 */
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
		setName("New SQL Relationship");
		setPopulated(true);
		foreignKey = new SQLImportedKey(this);
	}

    /**
     * NOTE: Magic MUST be disabled before calling this constructor. If not the
     * relationship will try to be attached between the parent table and a null
     * FK table throwing a NPE.
     * <p>
     * NOTE 2: No foreign key will be created with this constructor. After
     * calling this constructor you MUST set the SQLImportedKey before using it
     * or else it will only be half a relationship.
     */
	@Constructor
    public SQLRelationship(@ConstructorParameter(parameterType=ParameterType.PROPERTY, propertyName="parent") SQLTable pkTable) {
		pkCardinality = ONE;
		fkCardinality = ZERO | ONE | MANY;
		setName("New SQL Relationship");
		setPopulated(true);
        setParent(pkTable);
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
	@Mutator
	public void setName(String name) {
		super.setName(name);
		setPhysicalName(name);
	}
	
	@Override
	public final void updateToMatch(SQLObject source) throws SQLObjectException {
	    updateToMatch(source, false);
	}
	
	public final void updateToMatch(SQLObject source, boolean ignoreColumnMappings) throws SQLObjectException {
	    SQLRelationship relationshipToCopy = (SQLRelationship) source;
	    setName(relationshipToCopy.getName());
	    setIdentifying(relationshipToCopy.determineIdentifyingStatus());
	    setUpdateRule(relationshipToCopy.getUpdateRule());
	    setDeleteRule(relationshipToCopy.getDeleteRule());
	    setDeferrability(relationshipToCopy.getDeferrability());
	    for (Map.Entry<Class<? extends SQLObject>, Throwable> inaccessibleReason : source.getChildrenInaccessibleReasons().entrySet()) {
	        setChildrenInaccessibleReason(inaccessibleReason.getValue(), inaccessibleReason.getKey(), false);
	    }
	    setTextForChildLabel(relationshipToCopy.getTextForChildLabel());
	    setTextForParentLabel(relationshipToCopy.getTextForParentLabel());

	    if (ignoreColumnMappings) return;

	    List<ColumnMapping> columnsToRemove = new ArrayList<ColumnMapping>(getChildrenWithoutPopulating());
	    for (ColumnMapping newColMapping : relationshipToCopy.getChildrenWithoutPopulating()) {
	        boolean foundColumn = false;
	        for (int i = columnsToRemove.size() - 1; i >= 0; i--) {
	            ColumnMapping existingMapping = columnsToRemove.get(i);
	            SQLTable existingFKTable;
	            String existingColName;
	            if (existingMapping.getFkColumn() == null) {
	            	existingFKTable = existingMapping.getFkTable();
	            	existingColName = existingMapping.getFkColName();
	            } else {
	            	existingFKTable = existingMapping.getFkColumn().getParent();
	            	existingColName = existingMapping.getFkColumn().getName();
	            }
	            SQLTable newColFKTable;
	            String newColFKColName;
	            if (newColMapping.getFkColumn() == null) {
	            	newColFKTable = newColMapping.getFkTable();
	            	newColFKColName = newColMapping.getFkColName();
	            } else {
	            	newColFKTable = newColMapping.getFkColumn().getParent();
	            	newColFKColName = newColMapping.getFkColumn().getName();
	            }
	            
	            if (existingMapping.getPkColumn().equals(newColMapping.getPkColumn())
	                    && ((existingMapping.getFkColumn() != null && 
	                    		newColMapping.getFkColumn() != null && 
	                    		existingMapping.getFkColumn().equals(newColMapping.getFkColumn())
	                    	|| (existingFKTable.equals(newColFKTable) && 
	                    			existingColName.equals(newColFKColName))))) {
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

	public void attachListeners() throws SQLObjectException {
		SQLPowerUtils.listenToHierarchy(getParent(), fkColumnUpdater);
	}

	private void detachListeners(){
        if (getParent() != null) {
            SQLPowerUtils.unlistenToHierarchy(getParent(), fkColumnUpdater);
        }
	}

	@Mutator(constructorMutator=true)
	public void setPkTable(SQLTable pkTable) {
		if (getPkTable() != null && !getPkTable().equals(pkTable)) 
			throw new IllegalArgumentException("Cannot set the parent table of relationship " + 
					getName() + " to " + pkTable.getName() + " as it is already attached to " + 
					getParent().getName());
		SQLTable oldTable = getPkTable();
		setParent(pkTable);
		firePropertyChange("pkTable", oldTable, pkTable);
	}
	
	@Transient @Mutator
	public void setFkTable(SQLTable fkTable) {
		if (getFkTable() != null && !getFkTable().equals(fkTable)) 
			throw new IllegalArgumentException("Cannot set the child table of relationship " + 
					getName() + " to " + fkTable.getName() + " as it is already attached to " + 
					getFkTable().getName());
		SQLTable oldTable = getFkTable();
		foreignKey.setParent(fkTable);
		firePropertyChange("fkTable", oldTable, fkTable);
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
     * @throws SQLObjectException
     */
	public void attachRelationship(SQLTable pkTable, SQLTable fkTable, 
	        boolean autoGenerateMapping) throws SQLObjectException {
	    attachRelationship(pkTable, fkTable, autoGenerateMapping, true);
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
     * @param addToParents
     *            If true the relationship and its imported key will be added to
     *            their corresponding tables as children by this method. If
     *            false the relationship will be fully setup but will not be an
     *            actual child of the table and will have to be added after this
     *            method call.
     * @throws SQLObjectException
     */
	public void attachRelationship(SQLTable pkTable, SQLTable fkTable, 
	        boolean autoGenerateMapping, boolean addToParents) throws SQLObjectException {
		setFkTable(fkTable);
		try {
			setMagicEnabled(false);
			setParent(pkTable);
		} finally {
			setMagicEnabled(true);
		}
		attachRelationship(autoGenerateMapping, addToParents);
	}
	
	/**
	 * Associates an {@link SQLRelationship} with the given {@link SQLTable}
	 * objects. Also automatically generates the PK to FK column mapping if
	 * autoGenerateMapping is set to true. The PK and FK tables must be set
	 * already before calling this method.
	 * 
	 * @param autoGenerateMapping
	 *            Automatically generates the PK to FK column mapping if true
	 * @throws SQLObjectException
	 */
	private void attachRelationship(boolean autoGenerateMapping) throws SQLObjectException {
	    attachRelationship(autoGenerateMapping, true);
	}

    /**
     * Associates an {@link SQLRelationship} with the given {@link SQLTable}
     * objects. Also automatically generates the PK to FK column mapping if
     * autoGenerateMapping is set to true. The PK and FK tables must be set
     * already before calling this method.
     * 
     * @param autoGenerateMapping
     *            Automatically generates the PK to FK column mapping if true
     * @param addToParents
     *            If true the relationship and its imported key will be added to
     *            their corresponding tables as children by this method. If
     *            false the relationship will be fully setup but will not be an
     *            actual child of the table and will have to be added after this
     *            method call. You may not want to attach a relationship as a child
     *            of tables to prevent events from being pushed to the object model
     *            while the relationship is being setup.
     * @throws SQLObjectException
     */
	private void attachRelationship(boolean autoGenerateMapping, boolean addToParents) throws SQLObjectException {
		if(getParent() == null) throw new NullPointerException("Null pkTable not allowed");
		SQLTable fkTable = getFkTable();
		if(fkTable == null) throw new NullPointerException("Null fkTable not allowed");

		//Listeners detached so they don't get added twice when adding the 
		//relationship to parent tables?
		detachListeners();

		boolean alreadyExists = false;
		
		for (SQLRelationship r : getParent().getExportedKeysWithoutPopulating()) {
		    if (r.getFkTable().equals(fkTable)) {
		        alreadyExists = true;
		        break;
		    }
		}
	
		if (addToParents) {
		    getParent().addChild(this);
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
					if (!pkCol.isPrimaryKey()) break;

					SQLColumn match = fkTable.getColumnByName(pkCol.getName());
					SQLColumn fkCol = new SQLColumn(pkCol);
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
			if (m.getFkColumn() == null) continue; //fk col has not yet been populated.
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
					if (fkCol.getParent() == null || !fkCol.isPrimaryKey()) {
						logger.debug("realizeMapping: fkCol PK seq is null. Inserting at end of PK.");
						insertIdx = getFkTable().getPkSize();
					} else {
						insertIdx = getFkTable().getColumnIndex(fkCol);
						logger.debug("realizeMapping: using existing fkCol PK seq " + insertIdx);
					}
				} else {
					if (fkCol.getParent() != null && fkCol.isPrimaryKey()) {
						insertIdx = getFkTable().getColumnIndex(fkCol);
					} else {
						insertIdx = getFkTable().getColumns().size();
					}
				}

				fkCol.setAutoIncrement(false);

				// This might bump up the reference count (which would be
				// correct)
				getFkTable().addColumn(fkCol, insertIdx);
				logger.debug("realizeMapping: Added column '" + fkCol.getName() + "' at index " + insertIdx);
				if (fkCol.getReferenceCount() <= 0)
					throw new IllegalStateException("Created a column with 0 references!");

				if (identifying && !fkCol.isPrimaryKey()) {
					getFkTable().addToPK(fkCol);
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
	 * <p>
	 * Also note that the table may not have had its children populated at this
	 * point. If this method is being called in a background thread the columns
	 * and indices will be added to the table in the foreground thread. Any
	 * section that requires the table to have the correct children and indices
	 * needs to be run on the foreground thread to ensure the objects exist.
	 * 
     * @throws SQLObjectException
     *             if a database error occurs or if the given table's parent
     *             database is not marked as populated.
     */
	static List<SQLRelationship> fetchExportedKeys(final SQLTable table, final SQLTable originalFkTable)
	throws SQLObjectException {
		final SQLDatabase db = table.getParentDatabase();
		if (!db.isPopulated()) {
			throw new SQLObjectException("relationship.unpopulatedTargetDatabase");
		}
		CachedRowSet crs = new CachedRowSet();
		ResultSet tempRS = null; // just a temporary place for the live result set. use crs instead.
		Connection con = null;
		try {
		    con = table.getParentDatabase().getConnection();
		    DatabaseMetaData dbmd = con.getMetaData();
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
				final String pkCat = crs.getString(1);
				final String pkSchema = crs.getString(2);
				final String pkTableName = crs.getString(3);
				final String pkColName = crs.getString(4);
				final SQLTable parentTable = db.getTableByName(pkCat, pkSchema, pkTableName);
				final String fkCat = crs.getString(5);
				final String fkSchema = crs.getString(6);
				final String fkTableName = crs.getString(7);
				final String fkColName = crs.getString(8);
				final SQLTable fkTable = db.getTableByName(fkCat,fkSchema,fkTableName);
				//For performance we only care about the relationships that are being searched for 
				//on this fk table.
				if (originalFkTable != null && originalFkTable != fkTable) {
					continue;
				}
				final int updateRule = crs.getInt(10);
				final int deleteRule = crs.getInt(11);
				final String fkName = crs.getString(12);
				final int deferrability = crs.getInt(14);
				
				if (currentKeySeq == 1) {
					r = new SQLRelationship();
					final SQLRelationship finalRelation = r;
					Runnable fkTableRunner = new Runnable() {
						public void run() {
							finalRelation.setFkTable(fkTable);
						}
					};
					try {
						table.getRunnableDispatcher().runInForeground(fkTableRunner);
					} catch (SessionNotFoundException e) {
						fkTableRunner.run();
					}
					
					//must be done on current thread
					newKeys.add(r);
				}
				if (parentTable == null) {
					logger.error("addImportedRelationshipsToTable: Couldn't find exporting table "
							+pkCat+"."+pkSchema+"."+pkTableName
							+" in target database!");
					continue;
				}
				
				final SQLRelationship relToModify = r;
				Runnable runner = new Runnable() {
					public void run() {
						if (!parentTable.isColumnsPopulated()) {
							throw new IllegalStateException("FK table " + parentTable + 
									" is missing columns, cannot populate relationships.");
						}
						try {
							relToModify.setMagicEnabled(false);
							ColumnMapping m = new ColumnMapping();
							relToModify.addMapping(m);
							
							relToModify.setParent(parentTable); 
							
							if (relToModify.getParent() != table) {
								throw new IllegalStateException("fkTable did not match requested table");
							}
							
							logger.debug("Looking for pk column '"+pkColName+"' in table '"+relToModify.getParent()+"'");
							m.pkColumn = relToModify.getParent().getColumnByName(pkColName);
							if (m.pkColumn == null) {
								throw new SQLObjectException("relationship.populate.nullPkColumn");
							}
							
							m.fkColumn = relToModify.getFkTable().getColumnByName(fkColName, false, false);
							if (m.fkColumn == null) {
								m.setFkColName(fkColName);
								m.setFkTable(relToModify.getFkTable());
							}
							// column 9 (currentKeySeq) handled above
							relToModify.updateRule = UpdateDeleteRule.ruleForCode(updateRule);
							relToModify.deleteRule = UpdateDeleteRule.ruleForCode(deleteRule);
							relToModify.setName(fkName);
							try {
								relToModify.deferrability = Deferrability.ruleForCode(deferrability);
							} catch (IllegalArgumentException ex) {
								logger.warn("Invalid code when reverse engineering" +
										" relationship. Defaulting to NOT_DEFERRABLE.", ex);
								relToModify.deferrability = Deferrability.NOT_DEFERRABLE;
							}
						} catch (SQLObjectException e) {
							throw new SQLObjectRuntimeException(e);
						} finally {
							relToModify.setMagicEnabled(true);
						}
					}
				};
				try {
					table.getRunnableDispatcher().runInForeground(runner);
				} catch (SessionNotFoundException e) {
					runner.run();
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
	
	@NonProperty
	public ColumnMapping getMappingByPkCol(SQLColumn pkcol) {
		for (ColumnMapping m : mappings) {
			if (m.pkColumn == pkcol) {
				return m;
			}
		}
		return null;
	}

	public void reassignMappingsByPkCol(SQLColumn pkCol) {
		for (ColumnMapping m : mappings) {
			if (m.pkColumn != null && m.pkColumn != pkCol && m.pkColumn.getUUID().equals(pkCol.getUUID())) {
				m.setFkColumn(pkCol);
			}
		}
	}

	public boolean containsPkColumn(SQLColumn col) {
		return getMappingByPkCol(col) != null;
	}

	@NonProperty
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
	
	public void reassignMappingsByFkCol(SQLColumn fkCol) {
		for (ColumnMapping m : mappings) {
			if (m.fkColumn != null && m.fkColumn != fkCol && m.fkColumn.getUUID().equals(fkCol.getUUID())) {
				m.setFkColumn(fkCol);
			}
		}
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
	 * child to this relationship. This will not increase the reference
	 * count to the columns in the mapping.
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
	 * This listener will update the fk columns of the fk table based on the
	 * mappings in this relationship when there are property changes to the 
	 * columns in the pk table. This listener only needs to be attached to the
	 * pk table.
	 */
	protected SPListener fkColumnUpdater = 
		new ForeignKeyColumnUpdaterPoolingSPListener(this);
	
//	---------------------- Former RelationshipManager ------------------------
		
	/**
	 * When a child is added to the parent table this method must be
	 * called to fix the relationship mappings.
	 * 
	 * @param col
	 *            The column just added to the parent table's relationship.
	 */
	public void fixMappingNewChildInParent(SQLColumn col) {
		if (!getParent().isMagicEnabled()){
			logger.debug("Magic disabled; not fixing mapping for " + col);
			return;
		}

		try {
			if (col.isPrimaryKey()) {
				ensureInMapping(col);
			} else {
				ensureNotInMapping(col);
			}
		} catch (SQLObjectException ex) {
			logger.warn("Couldn't add/remove mapped FK columns", ex);
		}
	}

	/**
	 * Must be called when this relationship or its foreignKey is being
	 * removed from its parent table.
	 * 
	 * @param isRelationship
	 *            If true the relationship is being removed from the pk
	 *            table and the associated {@link SQLImportedKey} must be
	 *            removed from the fk table. If false the
	 *            {@link SQLImportedKey} is being removed from the fk table
	 *            and the containing {@link SQLRelationship} must be removed
	 *            from the pk table.
	 */
	public void disconnectRelationship(boolean isRelationship) {
		if (!getParent().isMagicEnabled()){
			logger.debug("Magic disabled; ignoring relationship remove " + SQLRelationship.this);
			return;
		}

		if (isDisconnecting) return;

		try {
			detachListeners();
			try {
				isDisconnecting = true;
				if (isRelationship) {
					SQLImportedKey fk = foreignKey;
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
				if (cm.getFkColumn() != null) {
					cm.getFkColumn().removeReference();
				}
			}
		} finally {
			isDisconnecting = false;
		}
	}

	/**
	 * Must be called when a column is being removed from its parent table.
	 * While this can be done from both pk and fk table it appears to only
	 * have effect for the pk table.
	 * 
	 * @param col
	 *            The column removed from the table.
	 */
	public void fixMappingChildRemoved(SQLColumn col) {
		if (!col.getParent().isMagicEnabled()){
			logger.debug("Magic disabled; not fixing mapping for " + col);
			return;
		}

		try {
			ensureNotInMapping(col);
		} catch (SQLObjectException ex) {
			logger.warn("Couldn't remove mapped FK columns", ex);
		}
	}

	/**
	 * To be called when the pk or fk table is removed from its parent.
	 */
	public void tableDisconnected() {
		if (!getParent().isMagicEnabled() || !getFkTable().isMagicEnabled()){
			logger.debug("Magic disabled; ignoring table disconnect that would " +
					"clean up relationship " + SQLRelationship.this);
			return;
		}

		getParent().removeExportedKey(SQLRelationship.this);
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
			if (pkcol.getParent().equals(getFkTable())) {
				// self-reference! must create new column!
				fkcol = new SQLColumn(pkcol);
				fkcol.setName(generateUniqueColumnName("Parent_"+pkcol.getName(), pkcol.getParent()));
			} else {
				fkcol = getFkTable().getColumnByName(pkcol.getName());
				if (fkcol == null) fkcol = new SQLColumn(pkcol);
			}

			if (identifying && getParent() != getFkTable()) {
			    //Each inserted column will be placed: 
			    //1: just above the next column in the fk index that we know about and can find.
			    //2: just below the previous column in the fk index that we know about and can find.
			    //3: at the bottom of the fk index.
			    int index = -1;
			    int pkIndex = getPkTable().getColumnIndex(pkcol);
			    for (int i = pkIndex + 1; i < getPkTable().getPkSize(); i++) {
			        for (int j = 0; j < getFkTable().getPkSize(); j++) {
			            if (getFkTable().getColumn(j).getName().equals(getPkTable().getColumn(i).getName())) {
			                index = j;
			                break;
			            }
			        }
			        if (index >= 0) break;
			    }
			    if (index == -1) {
			        for (int i = pkIndex - 1; i >=0; i--) {
			            for (int j = 0; j < getFkTable().getPkSize(); j++) {
			                if (getFkTable().getColumn(j).getName().equals(getPkTable().getColumn(i).getName())) {
			                    index = j + 1;
			                    break;
			                }
			            }
			            if (index >= 0) break;
			        }
			    }
			    if (index == -1) {
			        index = getFkTable().getPkSize();
			    }
			    
				// this either adds the new column or bumps up the refcount on existing col
				getFkTable().addColumn(fkcol, index);
				getFkTable().addToPK(fkcol);
			} else {
				// this either adds the new column or bumps up the refcount on existing col
				getFkTable().addColumn(fkcol);
			}
			logger.debug("ensureInMapping("+getName()+"): added fkcol " + fkcol);
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

	// ---------------------- SQLRelationship SQLObject support ------------------------

	/**
	 * Returns the foreign key name.
	 */
	@Transient @Accessor
	public String getShortDisplayName() {
		return getName();
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
	@Transient @Accessor
	public boolean isPopulated() {
		return true;
	}


	// ----------------- accessors and mutators -------------------

	@Accessor(isInteresting=true)
	public UpdateDeleteRule getUpdateRule()  {
		return this.updateRule;
	}

	@Mutator
	public void setUpdateRule(UpdateDeleteRule rule) {
	    UpdateDeleteRule oldRule = updateRule;
	    updateRule = rule;
		firePropertyChange("updateRule", oldRule, rule);
	}

	@Accessor(isInteresting=true)
	public UpdateDeleteRule getDeleteRule()  {
		return this.deleteRule;
	}

	@Mutator
	public void setDeleteRule(UpdateDeleteRule rule) {
        UpdateDeleteRule oldRule = deleteRule;
        deleteRule = rule;
        firePropertyChange("deleteRule", oldRule, rule);
	}

	@Accessor(isInteresting=true)
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
		int oldFkCardinality = this.fkCardinality;
		this.fkCardinality = argFkCardinality;
		firePropertyChange("fkCardinality",oldFkCardinality,argFkCardinality);
	}

	/**
	 * Gets the value of identifying
	 *
	 * @return the value of identifying
	 */
	@Accessor(isInteresting=true)
	public boolean isIdentifying()  {
		return this.identifying;
	}

	/**
	 * Sets the value of identifying, and moves the FK columns into or
	 * out of the FK Table's primary key as appropriate.
	 * <p>
	 * XXX Does anything in this method actually throw a SQLObjectException?
	 *
	 * @param argIdentifying Value to assign to this.identifying
	 */
	@Mutator
	public void setIdentifying(boolean argIdentifying) throws SQLObjectException {
		try {
			fireTransactionStarted("Setting " + getName() + " to be identifying: " + argIdentifying);
			
			boolean oldIdentifying = this.identifying;
			if (identifying != argIdentifying) {
				identifying = argIdentifying;

				if (identifying) {
					
					firePropertyChange("identifying", oldIdentifying, argIdentifying);
					
					if (isMagicEnabled()) {
					    for (ColumnMapping m : getChildren(ColumnMapping.class)) {
					        if (m.getFkColumn() != null && !m.getFkColumn().isPrimaryKey()) {
					            getFkTable().addToPK(m.getFkColumn());
					        }
					    }	
					}
					
				} else {
				    if (isMagicEnabled()) {
				        for (ColumnMapping m : getChildren(ColumnMapping.class)) {
				            if (m.getFkColumn() != null && m.getFkColumn().isPrimaryKey()) {
				                getFkTable().moveAfterPK(m.getFkColumn());
				            }
				        }
				    }
					
					firePropertyChange("identifying", oldIdentifying, argIdentifying);
				}
			}
			
			fireTransactionEnded();
		} catch (RuntimeException e) {
			fireTransactionRollback(e.getMessage());
			throw e;
		}
	}
	
	@Accessor
	public SQLTable getParent() {
		return (SQLTable) super.getParent();
	}
	
	/**
	 * Because we constrained the return type on getParent there needs to be a
	 * setter that has the same constraint otherwise the reflection in the undo
	 * events will not find a setter to match the getter and won't be able to
	 * undo parent property changes.
	 */
	@Mutator
	public void setParent(SQLTable parent) {
		setParentHelper(parent);
	}
	
	@Accessor
	public SQLTable getPkTable() {
		return getParent();
	}

	@Transient @Accessor
	public SQLImportedKey getForeignKey() {
		return foreignKey;
	}
	
	@Transient @Mutator
	public void setForeignKey(SQLImportedKey newKey) {
		SQLImportedKey oldKey = foreignKey;
		this.foreignKey = newKey;
		firePropertyChange("foreignKey", oldKey, newKey);
	}
	
	@Mutator
	public void setParent(SPObject parent) {
		setParentHelper(parent);
	}
	
	/**
	 * See the comment on {@link #setParent(SQLTable)} for why this method
	 * exists if it seems goofy.
	 */
	private void setParentHelper(SPObject parent) {
		SPObject oldVal = getParent();
		super.setParent(parent);
		if (parent != null) {
			if (isMagicEnabled() && parent != oldVal) {
				try {
					attachRelationship(true);
				} catch (SQLObjectException e) {
					throw new RuntimeException(e);
				}
			} else {
				//Column manager is removed first in case it has already been
				//added before. One case is when the relationship is being re-added 
				//to the same table it was removed from by the undo system. 
				SQLPowerUtils.unlistenToHierarchy(getParent(), fkColumnUpdater);
				
				SQLPowerUtils.listenToHierarchy(getParent(), fkColumnUpdater);
			}
		}
	}
	
	@Transient @Accessor
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
	public static class SQLImportedKey extends SQLObject {

		private final SQLRelationship relationship;

        /**
         * Listens for changes to the properties of the relationship this imported key
         * is attached to. This keeps the properties of the two relationship ends the
         * same where necessary.
         */
		private final SPListener relationshipPropertyListener = new AbstractSPListener() {
		    
		    @Override
		    public void propertyChanged(PropertyChangeEvent evt) {
		        if (evt.getPropertyName().equals("name")) {
		            setName((String) evt.getNewValue());
		        } else if (evt.getPropertyName().equals("populated")) {
		            setPopulated((Boolean) evt.getNewValue());
		        }
		    }
		};

		/**
		 * This listener is for when an fk-column is moved from one index to
		 * another. The reference of the column could change, so this will
		 * reassign all column mappings to the new reference by comparing UUIDs.
		 */
		private final SPListener fkTableListener = new AbstractSPListener() {
		    @Override
			/*
			 * This is a cleaner/better fix than the one from commit 1466, but I
			 * am not sure what the problem was, as the mappings seem to be
			 * reassigned fine without this listener.
			 */
		    public void childAdded(SPChildEvent e) {
		        if (e.getChild() instanceof SQLColumn) {
		        	relationship.reassignMappingsByFkCol((SQLColumn) e.getChild());
		        }
		    }
		};

		@Constructor
		public SQLImportedKey(
				@ConstructorParameter(propertyName="relationship") SQLRelationship relationship) {
			super();
			this.relationship = relationship;
			setName(relationship.getName());
			setPopulated(relationship.isPopulated());
			relationship.addSPListener(relationshipPropertyListener);
			relationship.setForeignKey(this);
		}
		
		@Override
		public SQLTable getParent() {
			return (SQLTable) super.getParent();
		}
		
		/**
		 * Because we constrained the return type on getParent there needs to be a
		 * setter that has the same constraint otherwise the reflection in the undo
		 * events will not find a setter to match the getter and won't be able to
		 * undo parent property changes.
		 */
		@Mutator
		public void setParent(SQLTable parent) {
		    if (getParent() != null) {
		        getParent().removeSPListener(fkTableListener);
		    }
		    super.setParent(parent);
		    if (parent != null) {		            
		        parent.addSPListener(fkTableListener);
		    }		
		}
		
		@Override
		public List<? extends SQLObject> getChildrenWithoutPopulating() {
			return Collections.emptyList();
		}

		@Override
		public String getShortDisplayName() {
			if (relationship != null) {
				return relationship.getShortDisplayName();
			} else {
				return getName();
			}
		}

		@Override
		protected void populateImpl() throws SQLObjectException {
			//no-op
		}

		@Override
		protected boolean removeChildImpl(SPObject child) {
			return false;
		}

		public List<Class<? extends SPObject>> getAllowedChildTypes() {
			return Collections.emptyList();
		}

		public List<? extends SPObject> getDependencies() {
			return Collections.emptyList();
		}

		public void removeDependency(SPObject dependency) {
			throw new UnsupportedOperationException("Need to decide the correct " +
					"dependency of this object.");
		}
		
		@Accessor
		public SQLRelationship getRelationship() {
			return relationship;
		}

		@Override
		public String toString() {
			return getShortDisplayName();
		}
		
		@Override
		public final void updateToMatch(SQLObject source) throws SQLObjectException {
			//Do nothing, this is handled by the SQLRelationship
		}

		/**
		 * The equals method for {@link SQLImportedKey} needs to keep in line
		 * with {@link SQLRelationship}.
		 */
		@Override
		public boolean equals(Object obj) {
			if (obj instanceof SQLImportedKey) {
				SQLImportedKey key = (SQLImportedKey) obj;
				return (((getName() == null && key.getName() == null) || 
							getName().equals(key.getName())) && 
						relationship.equals(key.getRelationship()));
			}
			return false;
		}
		
		@Override
		public int hashCode() {
			int result = 17;
	    	result = 31 * result + (getName() == null? 0 : getName().hashCode());
	    	result = 31 * relationship.hashCode();
			return result;
		}
	}
	
	// -------------------------- COLUMN MAPPING ------------------------

	public static class ColumnMapping extends SQLObject {
		
		/**
		 * Defines an absolute ordering of the child types of this class.
		 */
		public static final List<Class<? extends SPObject>> allowedChildTypes = Collections.emptyList();
		
		protected SQLColumn pkColumn;
		protected SQLColumn fkColumn;

		/**
		 * If the fk table has not been populated the column mapping will point
		 * to the column in the fk table by table reference and column name.
		 * Then when the table is actually populated the fkColumn property will
		 * be set.
		 * <p>
		 * If the fkColumn is already populated it should be used over this value.
		 */
		private SQLTable fkTable;
		
		/**
		 * Holds the name of the fk table column this mapping should connect if
		 * the table does not have columns created yet. 
		 * @see #fkTable
		 */
		private String fkColName;

		/**
		 * A temporary hack to signal that the column mapping is being loaded
		 * and it should add a reference to the fkColumn when it is set on this
		 * object to properly complete the loading. It would be better if we
		 * changed how columns signaled that they had multiple references
		 * rather than properly maintaining the reference count.
		 */
		private boolean loading;
		
		/**
		 * This listener will be attached to the fkTable if the fkTable has not
		 * had its columns populated yet. This will let the column mapping update
		 * correctly when the fkTable gets populated.
		 */
		private final SPListener fkTableListener = new AbstractSPListener() {
			
			public void childAdded(SPChildEvent e) {
				if (e.getChild() instanceof SQLColumn && e.getChild().getName().equals(fkColName)) {
					setFkColumn((SQLColumn) e.getChild());
				}
			};
		};

		public ColumnMapping() {
			setName("Column Mapping");
			setPopulated(true);
		}
		
		@Constructor
		public ColumnMapping(@ConstructorParameter(parameterType=ParameterType.PROPERTY, propertyName="pkColumn") SQLColumn pkColumn) {
			this();
			setPkColumn(pkColumn);
			pkColumn.addReference();
			loading = true;
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
			SQLColumn oldPK = pkColumn;
			this.pkColumn = argPkColumn;
			firePropertyChange("pkColumn", oldPK, argPkColumn);
		}

		/**
		 * Gets the value of fkColumn
		 *
		 * @return the value of fkColumn
		 */
		@Accessor
		public SQLColumn getFkColumn()  {
			if (fkColumn == null && fkColName != null && fkTable != null) {
				try {
					setFkColumn(fkTable.getColumnByName(fkColName));
				} catch (SQLObjectException e) {
					throw new RuntimeException(e);
				}
			}
			return this.fkColumn;
		}

		/**
		 * Sets the value of fkColumn
		 *
		 * @param argFkColumn Value to assign to this.fkColumn
		 */
		@Mutator
		public void setFkColumn(SQLColumn argFkColumn) {
			try {
				begin("Setting column mapping fk column.");
				SQLColumn oldFK = this.fkColumn;
				this.fkColumn = argFkColumn;
				firePropertyChange("fkColumn", oldFK, argFkColumn);
				if (fkColumn != null) {
					setFkTable(null);
					setFkColName(null);
				}
				if (loading) {
					fkColumn.addReference();
					loading = false;
				}
				commit();
			} catch (RuntimeException e) {
				rollback(e.getMessage());
				throw e;
			}
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
			return (SQLRelationship) super.getParent();
		}
		
		/**
		 * Because we constrained the return type on getParent there needs to be a
		 * setter that has the same constraint otherwise the reflection in the undo
		 * events will not find a setter to match the getter and won't be able to
		 * undo parent property changes.
		 */
		@Mutator
		public void setParent(SQLRelationship parent) {
			super.setParent(parent);
		}

		/**
		 * Returns the table and column name of the pkColumn.
		 */
		@Transient @Accessor
		public String getShortDisplayName() {
			if (pkColumn == null || fkColumn == null) return "Incomplete mapping";
			
			String fkTableName = null;
			if (fkColumn == null && fkTable != null) {
				fkTableName = fkTable.getName();
			} else if (fkColumn.getParent() != null) {
				fkTableName = fkColumn.getParent().getName();
			}
			String fkColumnName = null;
			if (fkColumn == null && fkColName != null) {
				fkColumnName = fkColName;
			} else if (fkColumn != null) {
				fkColumnName = fkColumn.getName();
			}
			return pkColumn.getName() + " - " +
				fkTableName + "." + fkColumnName;
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
		@Transient @Accessor
		public boolean isPopulated() {
			return true;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof ColumnMapping) {
				ColumnMapping cmap = (ColumnMapping) obj;
				return fkColumn == cmap.fkColumn &&
						pkColumn == cmap.pkColumn &&
						fkTable == cmap.fkTable &&
						fkColName == cmap.fkColName;
			}
			return false;
		}
		
		@Override
		public int hashCode() {
			int result = 17;
			result = result * 31 + (fkColumn == null? 0 : fkColumn.hashCode());
			result = result * 31 + (pkColumn == null? 0 :pkColumn.hashCode());
			result = result * 31 + (fkTable == null? 0 : fkTable.hashCode());
			result = result * 31 + (fkColName == null? 0 :fkColName.hashCode());
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
			return allowedChildTypes;
		}

		@Mutator
		public void setFkTable(SQLTable fkTable) {
			SQLTable oldTable = this.fkTable;
			this.fkTable = fkTable;
			if (oldTable != null) {
				oldTable.removeSPListener(fkTableListener);
			}
			if (fkTable != null) {
				fkTable.addSPListener(fkTableListener);
			}
			firePropertyChange("fkTable", oldTable, fkTable);
		}

		@Accessor
		public SQLTable getFkTable() {
			return fkTable;
		}

		@Mutator
		public void setFkColName(String fkColName) {
			String oldName = this.fkColName;
			this.fkColName = fkColName;
			firePropertyChange("fkColName", oldName, fkColName);
		}

		@Accessor
		public String getFkColName() {
			return fkColName;
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
	    
	    List<ColumnMapping> columnMappings = getChildren(ColumnMapping.class);
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
        Set<String> rel = new HashSet<String>();
        SPObject tableParent = pkTable.getParent();
        for(SQLTable tbl : tableParent.getChildren(SQLTable.class)) {
        	for(SQLRelationship r : tbl.getChildren(SQLRelationship.class)) {
        		rel.add(r.getPhysicalName());
        	}
        }
        if (rel.contains(sb.toString())) {
        	int i = 1;
        	while (rel.contains(sb.toString() + Integer.toString(i))) {
        		i++;
        	}
        	sb.append(i);
        }
        model.setName(sb.toString());
        model.getForeignKey().setName(sb.toString());
        model.setIdentifying(identifying);
        model.attachRelationship(pkTable,fkTable,true);
        return model;
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
		return Collections.unmodifiableList(new ArrayList<ColumnMapping>(mappings));
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
					" of type " + child.getClass() + " as its parent is not " + getName() + "." +
							" The parent is " + child.getParent());
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

	public List<? extends SPObject> getDependencies() {
		return Collections.singletonList(foreignKey);
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
