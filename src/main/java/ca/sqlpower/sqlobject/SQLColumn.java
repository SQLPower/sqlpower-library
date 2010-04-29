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

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.log4j.Logger;

import ca.sqlpower.object.ObjectDependentException;
import ca.sqlpower.object.SPObject;
import ca.sqlpower.object.annotation.Accessor;
import ca.sqlpower.object.annotation.Constructor;
import ca.sqlpower.object.annotation.ConstructorParameter;
import ca.sqlpower.object.annotation.Mutator;
import ca.sqlpower.object.annotation.NonBound;
import ca.sqlpower.object.annotation.NonProperty;
import ca.sqlpower.object.annotation.Transient;
import ca.sqlpower.object.annotation.ConstructorParameter.ParameterType;
import ca.sqlpower.sql.SQL;
import ca.sqlpower.sqlobject.SQLRelationship.SQLImportedKey;
import ca.sqlpower.sqlobject.SQLTypePhysicalProperties.SQLTypeConstraint;
import ca.sqlpower.sqlobject.SQLTypePhysicalPropertiesProvider.PropertyType;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

public class SQLColumn extends SQLObject implements java.io.Serializable {

	private static Logger logger = Logger.getLogger(SQLColumn.class);
	
    public static final List<Class<? extends SPObject>> allowedChildTypes = 
        Collections.<Class<? extends SPObject>>singletonList(SQLTypePhysicalPropertiesProvider.class);

	protected final UserDefinedSQLType userDefinedSQLType;
	protected String platform;
	
	// *** REMEMBER *** update the copyProperties method if you add new properties!

	/**
	 * Refers back to the real database-connected SQLColumn that this
	 * column was originally derived from.
	 */
	protected SQLColumn sourceColumn;
	
	/**
	 * This is the native name for the column's type in its source
	 * database.  See {@link #type} for system-independant type.
	 */
	private String sourceDataTypeName;

	// set to empty string so that we don't generate spurious undos
	protected String remarks ="";
	
    /**
     * This property is a hint to the DDL generators to tell them what name
     * to give to the database sequence that generates values for this column.
     * Not all platforms need (or support) sequences, so setting this value
     * doesn't guarantee it will be used.  If the value of this field is left
     * null, the getter method will auto-generate a sequence name based on
     * the table and column names.
     */
    private String autoIncrementSequenceName;

	// *** REMEMBER *** update the copyProperties method if you add new properties!

	
    //  These are not specific Column properties.Instead,they are default column
    //  user settings.
   	private static String defaultName = "New_Column";
    
    private static int defaultType = Types.INTEGER;
    
    private static int defaultPrec = 10;
    
    private static int defaultScale;
    
    private static boolean defaultInPK;
    
    private static boolean defaultNullable;
    
    private static boolean defaultAutoInc;
    
    private static String defaultRemarks = "";
    
    private static String defaultForDefaultValue;   
    
	/**
	 * referenceCount is meant to keep track of how many containers (i.e. 
	 * folders and relationships) have a reference to a column.  A new 
	 * SQLColumn always starts off life with a reference count of 1. (it
	 * is set in the constructors).
	 * 
	 * When creating a new relationship which reuses a column, the 
	 * call addReference() on the column to increment the referenceCount.
	 * 
	 * When removing a relationship, call removeReference() on the column to
	 * decrement the referenceCount.  If the referenceCount falls to zero, it 
	 * removes itself from the containing table (because it imported by 
	 * the creation of a relationship.    
	 */
	protected int referenceCount;
	
	public SQLColumn() {
		userDefinedSQLType = new UserDefinedSQLType();
		userDefinedSQLType.setParent(this);
		setPlatform(SQLTypePhysicalPropertiesProvider.GENERIC_PLATFORM);
		logger.debug("NEW COLUMN (noargs) @"+hashCode());
		logger.debug("SQLColumn() set ref count to 1");
		referenceCount = 1;
		setName(defaultName);
		setPhysicalName("");
		userDefinedSQLType.setType(defaultType);
		userDefinedSQLType.setPrecision(platform, defaultPrec);
		userDefinedSQLType.setScale(platform, defaultScale);
		if (defaultNullable) {
			userDefinedSQLType.setNullability(DatabaseMetaData.columnNullable);
		} else {
			userDefinedSQLType.setNullability(DatabaseMetaData.columnNoNulls);
		}
		userDefinedSQLType.setAutoIncrement(defaultAutoInc);
		userDefinedSQLType.setDefaultValue(platform, defaultForDefaultValue);
		this.remarks = defaultRemarks;
		setPopulated(true);
	}

	public SQLColumn(SQLTable parentTable,
			String colName, 
			int dataType,
			String nativeType,
			int precision, 
			int scale, 
			int nullable,
			String remarks, 
			String defaultValue, 
			boolean isAutoIncrement) {
		
		this(parentTable, colName, dataType, nativeType, precision, scale, 
				nullable, remarks, defaultValue, isAutoIncrement, 
				new UserDefinedSQLType());
	}
	
	/**
	 * Constructs a SQLColumn that will be a part of the given SQLTable.
	 *
	 * @param parentTable The table that this column will think it belongs to.
	 * @param colName This column's name.
	 * @param dataType The number that represents this column's type. See java.sql.Types.
	 * @param nativeType The type as it is called in the source database.
	 * @param scale The length of this column.  Size is type-dependant.
	 * @param precision The number of places of precision after the decimal place for numeric types.
	 * @param nullable This column's nullability.  One of:
	 * <ul><li>DatabaseMetaData.columnNoNulls - might not allow NULL values
	 *     <li>DatabaseMetaData.columnNullable - definitely allows NULL values
	 *     <li>DatabaseMetaData.columnNullableUnknown - nullability unknown
	 * </ul>
	 * @param remarks User-defined remarks about this column
	 * @param defaultValue The value this column will have if another value is not specified.
	 * @param primaryKeySeq This column's position in the table's primary key.  Null if it is not in the PK.
	 * @param isAutoIncrement Does this column auto-increment?
	 */
	@Constructor
	public SQLColumn(@ConstructorParameter(propertyName = "parent") SQLTable parentTable,
			@ConstructorParameter(propertyName = "name") String colName,
			@ConstructorParameter(propertyName = "type") int dataType,
			@ConstructorParameter(propertyName = "sourceDataTypeName") String nativeType,
			@ConstructorParameter(propertyName = "precision") int precision,
			@ConstructorParameter(propertyName = "scale") int scale,
			@ConstructorParameter(propertyName = "nullable") int nullable,
			@ConstructorParameter(propertyName = "remarks") String remarks,
			@ConstructorParameter(propertyName = "defaultValue") String defaultValue,
			@ConstructorParameter(propertyName = "autoIncrement") boolean isAutoIncrement,
			@ConstructorParameter(isProperty=ParameterType.CHILD, propertyName="userDefinedSQLType")
			UserDefinedSQLType userDefinedSQLType) {
		if (parentTable != null) {
			logger.debug("NEW COLUMN "+colName+"@"+hashCode()+" parent "+parentTable.getName()+"@"+parentTable.hashCode());
		} else {
			logger.debug("NEW COLUMN "+colName+"@"+hashCode()+" (null parent)");
		}
        if (parentTable != null) {
            setParent(parentTable);
        }
        this.userDefinedSQLType = userDefinedSQLType;
        userDefinedSQLType.setParent(this);
        setPlatform(SQLTypePhysicalPropertiesProvider.GENERIC_PLATFORM);
		this.setName(colName);
		setPopulated(true);
		this.userDefinedSQLType.setType(dataType);
		this.sourceDataTypeName = nativeType;
		this.userDefinedSQLType.setScale(platform, scale);
		this.userDefinedSQLType.setPrecision(platform, precision);
		this.userDefinedSQLType.setNullability(nullable);
		this.userDefinedSQLType.setDefaultValue(platform, defaultValue);
		this.userDefinedSQLType.setAutoIncrement(isAutoIncrement);
		this.remarks = remarks;

		logger.debug("SQLColumn(.....) set ref count to 1");
		this.referenceCount = 1;
	}

	public SQLColumn(SQLTable parent, String colName, int type, int precision, int scale) {
		this(parent, colName, type, null, precision, scale, DatabaseMetaData.columnNullable, null, null, false, new UserDefinedSQLType());
	}
	
	/**
	 * Creates a reasonable facsimile of the given column.
	 */
	public SQLColumn(SQLColumn col) {
		super();
		userDefinedSQLType = new UserDefinedSQLType();
		userDefinedSQLType.setParent(this);
		setPlatform(SQLTypePhysicalPropertiesProvider.GENERIC_PLATFORM);
		setPopulated(true);
		copyProperties(this, col);
		logger.debug("SQLColumn(SQLColumn col ["+col+" "+col.hashCode()+"]) set ref count to 1");
		referenceCount = 1;
	}
	
	/**
	 * Makes a near clone of the given source column.  The new column
	 * you get back will have a parent pointer of addTo.columnsFolder,
	 * but will not be attached as a child (you will normally do that
	 * right after calling this).  It will refer to this column as its
	 * sourceColumn property if the source is in the same session, 
	 * and otherwise be identical to source.
	 * 
     * createInheritingInstance is used for reverse engineering.  
     * Will not preserve listeners.
	 * 
	 */
	public SQLColumn createInheritingInstance(SQLTable addTo) {
		logger.debug("derived instance SQLColumn constructor invocation.");
		SQLColumn c = new SQLColumn();
		copyProperties(c, this);
		c.setParent(addTo);
		if (SQLObjectUtils.isInSameSession(c, this)) {
			c.sourceColumn = this;
		} else {
		    c.sourceColumn = null;
		}
		logger.debug("getDerivedInstance set ref count to 1");
		c.referenceCount = 1;
		return c;
	}
	
	/**
	 * Makes a copy of the given source column.  The new column
	 * you get back will have a parent pointer of addTo.columnsFolder,
	 * but will not be attached as a child (you will normally do that
	 * right after calling this). Listeners will not be added to this
	 * copy.
	 */
	public SQLColumn createCopy(SQLTable addTo, boolean preserveSource) {
		logger.debug("derived instance SQLColumn constructor invocation.");
		SQLColumn c = new SQLColumn();
		copyProperties(c, this);
		c.setParent(addTo);
		if (preserveSource) {
			c.sourceColumn = getSourceColumn();
		}
		logger.debug("getDerivedInstance set ref count to 1");
		c.referenceCount = 1;
		return c;
	}

	/**
	 * Copies all the interesting properties of source into target.  This is a subroutine of
	 * the copy constructor, getDerivedInstance, and updateToMatch.
	 * 
	 * @param target The instance to copy properties into.
	 * @param source The instance to copy properties from.
	 */
	private static final void copyProperties(final SQLColumn target, final SQLColumn source) {
		target.runInForeground(new Runnable() {
			public void run() {
 				target.setPlatform(source.getPlatform());
 				target.setName(source.getName());
 				target.setSourceDataTypeName(source.sourceDataTypeName);
 				target.setPhysicalName(source.getPhysicalName());
 				target.setRemarks(source.remarks);
 				target.setAutoIncrementSequenceName(source.autoIncrementSequenceName);
				try {
					UserDefinedSQLType.copyProperties(target
							.getUserDefinedSQLType(), source
							.getUserDefinedSQLType());
				} catch (IllegalArgumentException e) {
					throw new RuntimeException(
							"An Exception occured while attempting to copy the column's Type's properties",
							e);
				} catch (ObjectDependentException e) {
					throw new RuntimeException(
							"An Exception occured while attempting to copy the column's Type's properties",
							e);
				}
			}
		});
	}

    /**
     * Updates all properties on this column (except the parent) to match the
     * given column's properties.
     * 
     * @param source
     *            The SQLColumn to copy the properties from
     */
	@Override
    public void updateToMatch(SQLObject source) {
        copyProperties(this, (SQLColumn) source);
    }

    /**
     * Creates a list of unparented SQLColumn objects based on the current
     * information from the given DatabaseMetaData.
     * 
     * @return A map of table names to a list of SQLColumns that the table
     *         should contain in the order the columns should appear in the
     *         table. This will contain the names and columns of tables that
     *         are system tables which may not be interesting for all calling
     *         methods.
     */
	static ListMultimap<String, SQLColumn> fetchColumnsForTable(
	                                String catalog,
	                                String schema,
	                                DatabaseMetaData dbmd) 
		throws SQLException, DuplicateColumnException, SQLObjectException {
		ResultSet rs = null;
		final ListMultimap<String, SQLColumn> multimap = ArrayListMultimap.create();
 		try {
			logger.debug("SQLColumn.addColumnsToTables: catalog="+catalog+"; schema="+schema);
			rs = dbmd.getColumns(catalog, schema, null, "%");
			
			int autoIncCol = SQL.findColumnIndex(rs, "is_autoincrement");
			logger.debug("Auto-increment info column: " + autoIncCol);
			
			while (rs.next()) {
				logger.debug("addColumnsToTable SQLColumn constructor invocation.");
				
				String tableName = rs.getString(3);
				
				boolean autoIncrement;
                if (autoIncCol > 0) {
                    autoIncrement = "yes".equalsIgnoreCase(rs.getString(autoIncCol));
                } else {
                    autoIncrement = false;
                }
				
				SQLColumn col = new SQLColumn(null,
											  rs.getString(4),  // col name
											  rs.getInt(5), // data type (from java.sql.Types)
											  rs.getString(6), // native type name
											  rs.getInt(7), // column size (precision)
											  rs.getInt(9), // decimal size (scale)
											  rs.getInt(11), // nullable
											  rs.getString(12) == null ? "" : rs.getString(12), // remarks
											  rs.getString(13), // default value
											  autoIncrement, // isAutoIncrement
											  new UserDefinedSQLType());
				logger.debug("Precision for the column " + rs.getString(4) + " is " + rs.getInt(7));

				// work around oracle 8i bug: when table names are long and similar,
				// getColumns() sometimes returns columns from multiple tables!
				// XXX: should be moved to the JDBC Wrapper for Oracle
				String dbTableName = rs.getString(3);
				if (dbTableName != null) {
					if (!dbTableName.equalsIgnoreCase(tableName)) {
						logger.warn("Got column "+col.getName()+" from "+dbTableName
									+" in metadata for "+tableName+"; not adding this column.");
						continue;
					}
				} else {
					logger.warn("Table name not specified in metadata.  Continuing anyway...");
				}

				logger.debug("Adding column "+col.getName());
				
	        	multimap.put(dbTableName, col);

			}
			
			return multimap;
		} finally {
			try {
				if (rs != null) rs.close();
			} catch (SQLException ex) {
				logger.error("Couldn't close result set", ex);
			}
		}
	}

	/**
	 * A comparator for SQLColumns that only pays attention to the
	 * column names.  For example, if <code>column1</code> has
	 * <code>name = "MY_COLUMN"</code> and <code>type =
	 * VARCHAR(20)</code> and <code>column2</code> has <code>name =
	 * "MY_COLUMN"</code> and type <code>VARCHAR(4)</code>,
	 * <code>compare(column1, column2)</code> will return 0.
	 */
	public static class ColumnNameComparator implements Comparator {
		/**
		 * Forwards to {@link #compare(SQLColumn,SQLColumn)}.
		 *
		 * @throws ClassCastException if o1 or o2 is not of class SQLColumn.
		 */
		public int compare(Object o1, Object o2) {
			return compare((SQLColumn) o1, (SQLColumn) o2);
		}

		/**
		 * See class description for behaviour of this method.
		 */
		public int compare(SQLColumn c1, SQLColumn c2) {
			return c1.getName().compareTo(c2.getName());
		}
	}

	public String toString() {
		return getShortDisplayName();
	}

	// ------------------------- SQLObject support -------------------------

	protected void populateImpl() throws SQLObjectException {
		// SQLColumn: populate is a no-op
	}

	@Transient @Accessor
	public boolean isPopulated() {
		return true;
	}

	/**
	 * Returns a "formatted" version of the datatype including
	 * any necessary parameters e.g. length for VARCHAR or precision and scale
	 * for numeric datatypes
	 */
	@Transient @Accessor
	public String getTypeName() {
		if (sourceDataTypeName != null) {
			if (getPrecision() > 0 && getScale() > 0) {
				return sourceDataTypeName+"("+getPrecision()+","+getScale()+")";
			} else if (getPrecision() > 0) {
				return sourceDataTypeName+"("+getPrecision()+")"; // XXX: should we display stuff like (18,0) for decimals?
			} else {
				return sourceDataTypeName;
			}			
		} else {
			return SQLType.getTypeName(getType()) // XXX: replace with TypeDescriptor
				+"("+getPrecision()+")";
		}
	}

	/**
	 * Returns the column's name and the datatype
	 * @see #getTypeName()
	 * @see #getName()
	 */
	@Transient @Accessor
	public String getShortDisplayName() {
		return getName() + ": " + getTypeName();
	}

	public boolean allowsChildren() {
		return true;
	}

	// ------------------------- accessors and mutators --------------------------

	@Accessor(isInteresting=true)
	public String getPlatform() {
		return platform;
	}

	@Mutator
	public void setPlatform(String newPlatform) {
		String oldPlatform = platform;
		platform = newPlatform;
		fireTransactionStarted("Setting platform");
		firePropertyChange("platform", oldPlatform, newPlatform);
		if (userDefinedSQLType.getPhysicalProperties(newPlatform) == null) {
			userDefinedSQLType.putPhysicalProperties(newPlatform, new SQLTypePhysicalProperties(newPlatform));
		}
		fireTransactionEnded();
	}
	
	@Accessor
	public UserDefinedSQLType getUserDefinedSQLType() {
		return userDefinedSQLType;
	}
	
	@Transient @Accessor(isInteresting = true)
	public String getCheckConstraint() {
		return userDefinedSQLType.getCheckConstraint(getPlatform());
	}
	
	@Transient @Mutator
	public void setCheckConstraint(String argCheckConstraint) {
		begin("Set checkConstraint");
		String oldConstraint = getCheckConstraint();
		userDefinedSQLType.setCheckConstraint(getPlatform(), argCheckConstraint);
		firePropertyChange("checkConstraint", oldConstraint, argCheckConstraint);
		commit();
	}
	
	@Transient @Accessor(isInteresting = true)
	public SQLTypeConstraint getConstraintType() {
		return userDefinedSQLType.getConstraintType(getPlatform());
	}

	@Transient @Mutator
	public void setConstraintType(SQLTypeConstraint constraint) {
		begin("Set constraintType");
		SQLTypeConstraint oldConstraint = getConstraintType();
		userDefinedSQLType.setConstraintType(getPlatform(), constraint);
		firePropertyChange("constraintType", oldConstraint, constraint);
		commit();
	}
	
	@Transient @Accessor(isInteresting = true)
    public String[] getEnumeration() {
        return userDefinedSQLType.getEnumeration(getPlatform());
    }

	@Transient @Mutator
    public void setEnumeration(String[] enumeration) {
		begin("Set enumeration");
		String[] oldEnumeration = getEnumeration();
		userDefinedSQLType.setEnumeration(getPlatform(), enumeration);
		firePropertyChange("enumeration", oldEnumeration, enumeration);
		commit();
	}
	
	@Transient @Accessor(isInteresting = true)
	public PropertyType getPrecisionType() {
		return userDefinedSQLType.getPrecisionType(getPlatform());
	}

	@Transient @Mutator
	public void setPrecisionType(PropertyType precisionType) {
		begin("Set precisionType");
		PropertyType oldPrecisionType = getPrecisionType();
		userDefinedSQLType.setPrecisionType(getPlatform(), precisionType);
		firePropertyChange("precisionType", oldPrecisionType, precisionType);
		commit();
	}
	
	@Transient @Accessor(isInteresting = true)
    public PropertyType getScaleType() {
        return userDefinedSQLType.getScaleType(getPlatform());
    }
    
	@Transient @Mutator
    public void setScaleType(PropertyType scaleType) {
		begin("Set scaleType");
		PropertyType oldScaleType = getScaleType();
		userDefinedSQLType.setScaleType(getPlatform(), scaleType);
		firePropertyChange("scaleType", oldScaleType, scaleType);
        commit();
	}
	
	@Accessor
	public SQLColumn getSourceColumn() {
		return sourceColumn;
	}

	@Mutator
	public void setSourceColumn(SQLColumn col) {
		SQLColumn oldCol = this.sourceColumn;
		sourceColumn = col;
		firePropertyChange("sourceColumn",oldCol,col);
	}


	/**
	 * Gets numeric value for the internal JDBC Type.
	 * This value corresponds to the constants defined in java.sql.Types
	 *
	 * @return the value of type
	 */
	@Transient @Accessor(isInteresting = true)
	public int getType()  {
		return userDefinedSQLType.getType();
	}

	/**
	 * Sets the value of type
	 *
	 * @param argType Value to assign to this.type
	 */
	@Transient @Mutator
	public void setType(int argType) {
		begin("Set type");
		int oldType = getType();
		userDefinedSQLType.setType(argType);
		firePropertyChange("type", oldType, argType);
		commit();
	}

	/**
	 * The data type name as obtained during reverse engineering
	 */
	@Accessor
	public String getSourceDataTypeName() {
		return sourceDataTypeName;
	}

	@Mutator
	public void setSourceDataTypeName(String n) {
		String oldSourceDataTypeName =  sourceDataTypeName;
		sourceDataTypeName = n;
		firePropertyChange("sourceDataTypeName",oldSourceDataTypeName,n);
	}

	/**
	 * Gets the value of scale
	 *
	 * @return the value of scale
	 */
	@Transient @Accessor(isInteresting = true)
	public int getScale()  {
		return userDefinedSQLType.getScale(getPlatform());
	}

	/**
	 * Sets the value of scale
	 *
	 * @param argScale Value to assign to this.scale
	 */
	@Transient @Mutator
	public void setScale(int argScale) {
		begin("Set scale");
		int oldScale = getScale();
		userDefinedSQLType.setScale(getPlatform(), argScale);
		firePropertyChange("scale", oldScale, argScale);
		commit();
	}

	/**
	 * Returns the precision for this column.
	 * The precision only makes sense together with the actual datatype.
	 * For character datatypes this defines the max. length of the column.
	 * For numeric datatypes this needs to be combined with the scale of the column
	 *
	 * @return the value of precision
	 * @see #getScale()
	 * @see #getType()
	 * @see #getTypeName()
	 */
	@Transient @Accessor(isInteresting = true)
	public int getPrecision()  {
		return userDefinedSQLType.getPrecision(getPlatform());
	}

	/**
	 * Sets the value of precision
	 *
	 * @param argPrecision Value to assign to this.precision
	 */
	@Transient @Mutator
	public void setPrecision(int argPrecision) {
		begin("Set precision");
		int oldPrecision = getPrecision();
		userDefinedSQLType.setPrecision(getPlatform(), argPrecision);
		firePropertyChange("precision", oldPrecision, argPrecision);
		commit();
	}

	/**
	 * Figures out this column's nullability
	 *
	 * @return true iff this.nullable == DatabaseMetaData.columnNullable.
	 */
	@NonBound
	public boolean isDefinitelyNullable()  {
		return getNullable() == DatabaseMetaData.columnNullable;
	}

	/**
	 * Returns true if this column is part of the table's primary key
	 *
	 * @return whether or not primaryKeySeq is defined
	 */
	@Transient @Accessor(isInteresting=true)	
	public boolean isPrimaryKey()  {
		return getParent().isInPrimaryKey(this);
	}

	/**
	 * If this column is a foreign key, return the referenced table.
	 *
	 * @return null, if this column is not a FK column, the referenced table otherwise
	 */
	@NonBound
	public SQLTable getReferencedTable() {
	    if (getParent() == null) return null;
	    try {
	        for (SQLImportedKey k : getParent().getImportedKeys()) {
	            if (k.getRelationship().containsFkColumn(this)) {
	                return k.getRelationship().getParent();
	            }
	        }
	        return null;
	    } catch (SQLObjectException ex) {
	        throw new SQLObjectRuntimeException(ex);
	    }
	}
	/**
	 * Indicates whether this column is a foreign key
	 *
	 * @return whether this column exists as a foreign key column in any of
	 * its parent table's imported keys
	 * 
	 * @see #getReferencedTable()
	 */
	@NonBound
	public boolean isForeignKey() {
		return getReferencedTable() != null;
	}
	
	/**
     * Indicates whether or not this column is exported into a foreign key in a
     * child table.
     * 
     * @return Returns true if this column is in an exported key. Otherwise,
     *         returns false.
     */
	@NonBound
	public boolean isExported() {
	    if (getParent() == null) return false;
	    try {
            for (SQLRelationship r : getParent().getExportedKeys()) {
                if (r.containsPkColumn(this)) {
                    return true;
                }
            }
            return false;
        } catch (SQLObjectException ex) {
            throw new SQLObjectRuntimeException(ex);
        }
	}
	
	/**
	 * Returns whether this column is in an index 
	 */
	@NonBound
	public boolean isIndexed() {
	    if (getParent() == null) return false;
	    try {
	        for (SQLIndex ind : getParent().getIndices()) {
	            for (SQLIndex.Column col : ind.getChildren(SQLIndex.Column.class)) {
	                if (this.equals(col.getColumn())) {
	                    return true;
	                }
	            }
	        }
	        return false;
	    } catch (SQLObjectException ex) {
	        throw new SQLObjectRuntimeException(ex);
        }
	}

	/**
	 * Returns whether this column is in an unique index.
	 */
	@NonBound
	public boolean isUniqueIndexed() {
	    if (getParent() == null) return false;
	    try {
	        for (SQLIndex ind : getParent().getIndices()) {
	            if (!ind.isUnique()) continue;
	            for (SQLIndex.Column col : ind.getChildren(SQLIndex.Column.class)) {
	                if (this.equals(col.getColumn())) {
	                    return true;
	                }
	            }
	        }
	        return false;
	    } catch (SQLObjectException ex) {
	        throw new SQLObjectRuntimeException(ex);
	    }
	}

	/**
	 * Returns the parent SQLTable object.
	 */
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
		super.setParent(parent);
	}

	/**
     * Returns this column's nullability.
     * 
     * @return One of:
     * <ul><li>DatabaseMetaData.columnNoNulls - might not allow NULL values
     *     <li>DatabaseMetaData.columnNullable - definitely allows NULL values
     *     <li>DatabaseMetaData.columnNullableUnknown - nullability unknown
     * </ul>
     */
	@Transient @Accessor(isInteresting=true)
	public int getNullable() {
		return userDefinedSQLType.getNullability();
	}

	/**
	 * Sets this column's nullability.
	 * 
	 * @param argNullable One of:
     * <ul><li>DatabaseMetaData.columnNoNulls - might not allow NULL values
     *     <li>DatabaseMetaData.columnNullable - definitely allows NULL values
     *     <li>DatabaseMetaData.columnNullableUnknown - nullability unknown
     * </ul>
	 */
	@Transient @Mutator
	public void setNullable(int argNullable) {
		begin("Set nullable");
		int yeOldeNullable = getNullable();
		userDefinedSQLType.setNullability(argNullable);
		firePropertyChange("nullable", yeOldeNullable, argNullable);
		commit();
	}
    	
    public static String getDefaultName() {
		return defaultName;
	}

	public static void setDefaultName(String defaultName) {
		SQLColumn.defaultName = defaultName;
	}

	public static int getDefaultType() {
		return defaultType;
	}

	public static void setDefaultType(int defaultType) {
		SQLColumn.defaultType = defaultType;
	}

	public static int getDefaultPrec() {
		return defaultPrec;
	}

	public static void setDefaultPrec(int defaultPrec) {
		SQLColumn.defaultPrec = defaultPrec;
	}

	public static int getDefaultScale() {
		return defaultScale;
	}

	public static void setDefaultScale(int defaultScale) {
		SQLColumn.defaultScale = defaultScale;
	}

	public static boolean isDefaultInPK() {
		return defaultInPK;
	}

	public static void setDefaultInPK(boolean defaultInPK) {
		SQLColumn.defaultInPK = defaultInPK;
	}

	public static boolean isDefaultNullable() {
		return defaultNullable;
	}

	public static void setDefaultNullable(boolean defaultNullable) {
		SQLColumn.defaultNullable = defaultNullable;
	}

	public static boolean isDefaultAutoInc() {
		return defaultAutoInc;
	}

	public static void setDefaultAutoInc(boolean defaultAutoInc) {
		SQLColumn.defaultAutoInc = defaultAutoInc;
	}

	public static String getDefaultRemarks() {
		return defaultRemarks;
	}

	public static void setDefaultRemarks(String defaultRemarks) {
		SQLColumn.defaultRemarks = defaultRemarks;
	}

	public static String getDefaultForDefaultValue() {
		return defaultForDefaultValue;
	}

	public static void setDefaultForDefaultValue(String defaultForDefaultValue) {
		SQLColumn.defaultForDefaultValue = defaultForDefaultValue;
	}

	/**
	 * Get the comment/remark defined for this column.
	 *
	 * @return the value of remarks
	 */
	@Accessor(isInteresting=true)
	public String getRemarks()  {
		return this.remarks;
	}

	/**
	 * Sets the value of remarks
	 *
	 * @param argRemarks Value to assign to this.remarks
	 */
	@Mutator
	public void setRemarks(String argRemarks) {
		String oldRemarks = this.remarks;
		this.remarks = argRemarks;
		firePropertyChange("remarks",oldRemarks,argRemarks);
	}

	/**
	 * Returns the default value defined for this value
	 *
	 * @return the value of defaultValue
	 */
	@NonProperty
	public String getDefaultValue()  {
		return userDefinedSQLType.getDefaultValue(getPlatform());
	}

	/**
	 * Sets the value of defaultValue
	 *
	 * @param argDefaultValue Value to assign to this.defaultValue
	 */
	@NonProperty
	public void setDefaultValue(String argDefaultValue) {
		userDefinedSQLType.setDefaultValue(getPlatform(), argDefaultValue);
	}

	/**
	 * Gets the value of autoIncrement
	 *
	 * @return the value of autoIncrement
	 */
	@Transient @Accessor(isInteresting=true)
	public boolean isAutoIncrement()  {
		return userDefinedSQLType.getAutoIncrement();
	}

	/**
	 * Sets the value of autoIncrement
	 *
	 * @param argAutoIncrement Value to assign to this.autoIncrement
	 */
	@Transient @Mutator
	public void setAutoIncrement(boolean argAutoIncrement) {
		begin("Set autoIncrement");
		boolean oldAutoIncrement = isAutoIncrement();
		userDefinedSQLType.setAutoIncrement(argAutoIncrement);
		firePropertyChange("autoIncrement",oldAutoIncrement,argAutoIncrement);
		commit();
	}
    
    /**
     * Returns the auto-increment sequence name, or a made-up
     * default (<code><i>parentTableName</i>_<i>columnName</i>_seq</code>) if the sequence name
     * has not been set explicitly.  The auto-increment sequence
     * name is a hint to DDL generators for platforms that need
     * sequence objects to support auto-incrementing column values.
     */
	@Accessor
    public String getAutoIncrementSequenceName() {
        if (autoIncrementSequenceName == null) {
        	String tableName;
        	if (getParent() == null) {
        		tableName = "";
        	} else if (getParent().getPhysicalName() != null && !getPhysicalName().trim().equals("")) {
        		tableName = getParent().getPhysicalName() + "_";
        	} else {
        		tableName = getParent().getName() +"_";
        	}
            return tableName + getName() + "_seq";
        } else {
            return autoIncrementSequenceName;
        }
    }
    
    /**
     * Only sets the name if it is different from the default name.  This is important
     * in case the table name changes; the name should be expected to update.
     */
	@Mutator
    public void setAutoIncrementSequenceName(String autoIncrementSequenceName) {

        // have to use getter because it produces the default value
        String oldName = getAutoIncrementSequenceName();
        
        if (!oldName.equals(autoIncrementSequenceName)) {
            this.autoIncrementSequenceName = autoIncrementSequenceName;
            firePropertyChange("autoIncrementSequenceName", oldName, autoIncrementSequenceName);
        }
    }

    /**
     * Returns true if the auto-increment sequence name of this column has
     * been changed from its default value.  Code that loads and saves this
     * SQLColumn will want to know if the value is a default or not.
     */
	@NonProperty
    public boolean isAutoIncrementSequenceNameSet() {
        return autoIncrementSequenceName != null;
    }
    
	public void addReference() {
        int oldReference = referenceCount;
		referenceCount++;
		logger.debug("incremented reference count to: " + referenceCount);
        firePropertyChange("referenceCount", oldReference, referenceCount);
	}
	
	public void removeReference() {
		if (logger.isDebugEnabled()) {
			String parentName = "<no parent table>";
			if (getParent() != null && getParent() != null) {
				parentName = getParent().getName();
			}
			logger.debug("Trying to remove reference from "+parentName+"."+getName()+" "+hashCode());
			
		}
		if (referenceCount == 0) {
		    logger.debug("Reference count of "+ this.getParent() +"."+this+" was already 0");
			throw new IllegalStateException("Reference count of is already 0; can't remove any references!");
		}
        int oldReference = referenceCount;
		referenceCount--;
		logger.debug("decremented reference count to: " + referenceCount);
		if (referenceCount == 0) {
			// delete from the parent (columnsFolder) 
			if (getParent() != null){
				logger.debug("reference count is 0, deleting column from parent.");
				try {
					getParent().removeChild(this);
				} catch (IllegalArgumentException e) {
					throw new RuntimeException(e);
				} catch (ObjectDependentException e) {
					throw new RuntimeException(e);
				}
			} else {
				logger.debug("Already removed from parent");
			}
		}
        firePropertyChange("referenceCount", oldReference, referenceCount);
	}
	
	/**
	 * @return Returns the referenceCount.
	 */
	@Transient @Accessor
	public int getReferenceCount() {
		return referenceCount;
	}

    /**
     * WARNING this should not be used by hand coded objects
     * @param referenceCount
     * @deprecated This method exists only to be called reflectively by the undo manager.  You should use addReference() and removeReference().
     */
	@Deprecated
	@NonBound
    public void setReferenceCount(int referenceCount) {
        this.referenceCount = referenceCount;
    }

	@Override
	@NonProperty
	public List<? extends SQLObject> getChildrenWithoutPopulating() {
		return Collections.singletonList(userDefinedSQLType);
	}

	@Override
	protected boolean removeChildImpl(SPObject child) {
		throw new IllegalStateException("Cannot remove children!!!");
	}

	public int childPositionOffset(Class<? extends SPObject> childType) {
		throw new IllegalArgumentException("Cannot retrieve the child position offset of " + 
				childType + " but " + getClass() + " does not allow children.");
	}

	@NonProperty
	public List<? extends SPObject> getDependencies() {
		return Collections.emptyList();
	}

	public void removeDependency(SPObject dependency) {
		// no-op
	}

	@NonProperty
	public List<Class<? extends SPObject>> getAllowedChildTypes() {
		return allowedChildTypes;
	}

}