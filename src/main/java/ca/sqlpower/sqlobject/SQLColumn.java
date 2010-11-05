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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import ca.sqlpower.object.ObjectDependentException;
import ca.sqlpower.object.SPObject;
import ca.sqlpower.object.SPVariableResolver;
import ca.sqlpower.object.SPVariableResolverProvider;
import ca.sqlpower.object.annotation.Accessor;
import ca.sqlpower.object.annotation.Constructor;
import ca.sqlpower.object.annotation.ConstructorParameter;
import ca.sqlpower.object.annotation.Mutator;
import ca.sqlpower.object.annotation.NonBound;
import ca.sqlpower.object.annotation.NonProperty;
import ca.sqlpower.object.annotation.Transient;
import ca.sqlpower.object.annotation.ConstructorParameter.ParameterType;
import ca.sqlpower.sql.DataSourceCollection;
import ca.sqlpower.sql.JDBCDataSourceType;
import ca.sqlpower.sql.SPDataSource;
import ca.sqlpower.sql.SQL;
import ca.sqlpower.sqlobject.SQLRelationship.SQLImportedKey;
import ca.sqlpower.sqlobject.SQLTypePhysicalProperties.SQLTypeConstraint;
import ca.sqlpower.sqlobject.SQLTypePhysicalPropertiesProvider.PropertyType;
import ca.sqlpower.util.UserPrompter;
import ca.sqlpower.util.UserPrompterFactory;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;

public class SQLColumn extends SQLObject implements java.io.Serializable, SPVariableResolverProvider, SQLCheckConstraintContainer {

	private static Logger logger = Logger.getLogger(SQLColumn.class);
	
    public static final List<Class<? extends SPObject>> allowedChildTypes = 
        Collections.<Class<? extends SPObject>>singletonList(UserDefinedSQLType.class);

	protected final UserDefinedSQLType userDefinedSQLType;
	protected String platform;
	
	// *** REMEMBER *** update the copyProperties method if you add new properties!

	/**
	 * Refers back to the real database-connected SQLColumn that this
	 * column was originally derived from.
	 */
	protected SQLColumn sourceColumn;
	
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

	/**
	 * @see #getEtlTransformationLogic()
	 */
    private String etlTransformationLogic;
    
    /**
     * @see #getEtlDefaultValue()
     */
    private String etlDefaultValue;
    
    /**
     * @see #getEtlAuditable()
     */
    private Boolean etlAuditable;

	/**
	 * @see #getEtlNotes()
	 */
    private String etlNotes;
    

	// *** REMEMBER *** update the copyProperties method if you add new properties!

	
    //  These are not specific Column properties.Instead,they are default column
    //  user settings.
   	private static String defaultName;
    
    private static int defaultType;
    
    private static int defaultPrec;
    
    private static int defaultScale;
    
    private static boolean defaultInPK;
    
    private static boolean defaultNullable;
    
    private static boolean defaultAutoInc;
    
    private static String defaultRemarks;
    
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

	/**
	 * Helper objects to resolve variables in check constraints.
	 */
    private final SQLCheckConstraintVariableResolver variableResolver = 
    	new SQLCheckConstraintVariableResolver(this);
	
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
		
		if (defaultPrec > 0) {
			userDefinedSQLType.setPrecisionType(platform, PropertyType.VARIABLE);
		} else {
			userDefinedSQLType.setPrecisionType(platform, PropertyType.NOT_APPLICABLE);
		}
		
		userDefinedSQLType.setScale(platform, defaultScale);
		if (defaultScale > 0) {
			userDefinedSQLType.setScaleType(platform, PropertyType.VARIABLE);
		} else {
			userDefinedSQLType.setScaleType(platform, PropertyType.NOT_APPLICABLE);
		}
		
		if (defaultNullable) {
			userDefinedSQLType.setMyNullability(DatabaseMetaData.columnNullable);
		} else {
			userDefinedSQLType.setMyNullability(DatabaseMetaData.columnNoNulls);
		}
		userDefinedSQLType.setMyAutoIncrement(defaultAutoInc);
		userDefinedSQLType.setDefaultValue(platform, defaultForDefaultValue);
		this.remarks = defaultRemarks;
		setPopulated(true);
	}

	public SQLColumn(UserDefinedSQLType startingType) {
		userDefinedSQLType = new UserDefinedSQLType();
		userDefinedSQLType.setParent(this);
		userDefinedSQLType.setUpstreamType(startingType);
		setPlatform(SQLTypePhysicalPropertiesProvider.GENERIC_PLATFORM);
		setName(defaultName);
		setPhysicalName("");
		setRemarks(defaultRemarks);
		referenceCount = 1;
		setPopulated(true);
	}

    /**
     * A constructor for testing purposes, and reverse engineering. You normally do not want to call this
     * constructor because it will override all of your domain or type values.
     */
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

		if (parentTable != null) {
			logger.debug("NEW COLUMN "+colName+"@"+hashCode()+" parent "+parentTable.getName()+"@"+parentTable.hashCode());
		} else {
			logger.debug("NEW COLUMN "+colName+"@"+hashCode()+" (null parent)");
		}
		if (parentTable != null) {
			setParent(parentTable);
		}
		this.userDefinedSQLType = new UserDefinedSQLType();
		userDefinedSQLType.setParent(this);
		setPlatform(SQLTypePhysicalPropertiesProvider.GENERIC_PLATFORM);
		this.setName(colName);
		setPopulated(true);
		
		this.userDefinedSQLType.setType(dataType);
		
		// A scale/precision value of 0 does not mean anything, 
		// which means the scale/precision type should be not applicable.
		// Otherwise, set the scale/precision type to a default of variable.
		// Reverse engineered tables will override the scale/precision types
		// by looking through existing user types and inheriting their 
		// scale/precision properties.
		this.userDefinedSQLType.setScale(platform, scale);
		if (scale > 0) {
			this.userDefinedSQLType.setScaleType(platform, PropertyType.VARIABLE);
		} else {
			this.userDefinedSQLType.setScaleType(platform, PropertyType.NOT_APPLICABLE);
		}
		this.userDefinedSQLType.setPrecision(platform, precision);
		if (precision > 0) {
			this.userDefinedSQLType.setPrecisionType(platform, PropertyType.VARIABLE);
		} else {
			this.userDefinedSQLType.setPrecisionType(platform, PropertyType.NOT_APPLICABLE);
		}
		this.userDefinedSQLType.setMyNullability(nullable);
		this.userDefinedSQLType.setDefaultValue(platform, defaultValue);
		this.userDefinedSQLType.setMyAutoIncrement(isAutoIncrement);
		this.userDefinedSQLType.setName(nativeType);
        this.remarks = remarks;

        logger.debug("SQLColumn(.....) set ref count to 1");
        this.referenceCount = 1;
	} 
	
	/**
     * A constructor for testing purposes, and reverse engineering. You normally do not want to call this
     * constructor because it will override all of your domain or type values.
     */
	public SQLColumn(SQLTable parentTable,
			String colName,
			UserDefinedSQLType type,
			int precision, 
			int scale,
			boolean isAutoIncrement) {

		if (parentTable != null) {
			setParent(parentTable);
		}
		this.userDefinedSQLType = new UserDefinedSQLType("UserDefinedSQLType",
										DatabaseMetaData.columnNullable,
										isAutoIncrement,
										null,
										type,
										new SQLTypePhysicalProperties(SQLTypePhysicalPropertiesProvider.GENERIC_PLATFORM));
		userDefinedSQLType.setParent(this);
		setPlatform(SQLTypePhysicalPropertiesProvider.GENERIC_PLATFORM);
		this.setName(colName);
		setPopulated(true);
		
		// A scale/precision value of 0 does not mean anything, 
		// which means the scale/precision type should be not applicable.
		// Otherwise, set the scale/precision type to a default of variable.
		// Reverse engineered tables will override the scale/precision types
		// by looking through existing user types and inheriting their 
		// scale/precision properties.
		this.userDefinedSQLType.setScale(platform, scale);
		if (scale > 0) {
			this.userDefinedSQLType.setScaleType(platform, PropertyType.VARIABLE);
		} else {
			this.userDefinedSQLType.setScaleType(platform, PropertyType.NOT_APPLICABLE);
		}
		this.userDefinedSQLType.setPrecision(platform, precision);
		if (precision > 0) {
			this.userDefinedSQLType.setPrecisionType(platform, PropertyType.VARIABLE);
		} else {
			this.userDefinedSQLType.setPrecisionType(platform, PropertyType.NOT_APPLICABLE);
		}
		this.userDefinedSQLType.setMyNullability(DatabaseMetaData.columnNullable);

        logger.debug("SQLColumn(.....) set ref count to 1");
        this.referenceCount = 1;
	}
	
	/**
	 * Constructs a SQLColumn that will be a part of the given SQLTable.
	 *
	 * @param parentTable The table that this column will think it belongs to.
	 * @param colName This column's name.
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
			@ConstructorParameter(propertyName = "sourceDataTypeName") String nativeType,
			@ConstructorParameter(propertyName = "remarks") String remarks,
			@ConstructorParameter(parameterType=ParameterType.CHILD, propertyName="userDefinedSQLType")
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
	    this.remarks = remarks;

	    logger.debug("SQLColumn(.....) set ref count to 1");
	    this.referenceCount = 1;
	}

	public SQLColumn(SQLTable parent, String colName, int type, int precision, int scale) {
		this(parent, colName, type, null, precision, scale, DatabaseMetaData.columnNullable, null, null, false);
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
				target.begin("Copying SQLColumn.");
 				target.setPlatform(source.getPlatform());
 				target.setName(source.getName());
 				target.setPhysicalName(source.getPhysicalName());
 				target.setRemarks(source.remarks);
 				target.setAutoIncrementSequenceName(source.autoIncrementSequenceName);
 				target.setEtlAuditable(source.getEtlAuditable());
 				target.setEtlDefaultValue(source.getEtlDefaultValue());
 				target.setEtlNotes(source.getEtlNotes());
 				target.setEtlTransformationLogic(source.getEtlTransformationLogic());
 				UserDefinedSQLType.copyProperties(
 						target.getUserDefinedSQLType(), 
 						source.getUserDefinedSQLType());
 				target.commit();
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
	                                String table,
	                                DatabaseMetaData dbmd) 
		throws SQLException, DuplicateColumnException, SQLObjectException {
		ResultSet rs = null;
		final ListMultimap<String, SQLColumn> multimap = ArrayListMultimap.create();
 		try {
			logger.debug("SQLColumn.addColumnsToTables: catalog="+catalog+"; schema="+schema);
			rs = dbmd.getColumns(catalog, schema, table, "%");
			
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
                String nativeTypeName = rs.getString(6);

				if(nativeTypeName.indexOf('(') >= 0) {
					nativeTypeName = nativeTypeName.substring(0, nativeTypeName.indexOf('('));
				}
                
				SQLColumn col = new SQLColumn(null,
											  rs.getString(4),  // col name
											  rs.getInt(5), // data type (from java.sql.Types)
											  nativeTypeName, // native type name
											  rs.getInt(7), // column size (precision)
											  rs.getInt(9), // decimal size (scale)
											  rs.getInt(11), // nullable
											  rs.getString(12) == null ? "" : rs.getString(12), // remarks
											  rs.getString(13), // default value
											  autoIncrement // isAutoIncrement
											  );
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
	 * Takes in information about the current UserDefinedSQLTypes and assigns an
	 * upstream type to each of the columns. This is a many step process. Each
	 * step falls through to the next one if it can't find a type.
	 * <p>
	 * 1. Search the types by type name. If there are any number of matches, use
	 * one. We don't really care which. <br>
	 * 2. Search the types by forward engineering name. This means what each
	 * type would forward engineer to on the given platform. If there is exactly
	 * one result, we're done. If there are multiple results, prompt the user. <br>
	 * 3. Search the types by JDBC type code. Follow the same guidelines as step
	 * 2. <br>
	 * 4. If we are connected to an enterprise server, create a new type with
	 * the same name and JDBC type as the reverse engineered column. <br>
	 * 5. Assign the unknown type as the parent. This type is fairly clueless,
	 * and won't forward engineer correctly on its own.
	 * <p>
	 * By the end of this process, all columns will have best match upstream
	 * types which, barring some fiddling on the user's part (or the unknown
	 * type coming into play), will forward engineer back to the type they came
	 * from. They will also have appropriate values overridden from their new
	 * upstream type. Any values that match up with defaults will not be
	 * overridden, so that changing the type will change all of these columns.
	 * 
	 * @param columns
	 *            A list of columns to assign types to. These should all be from
	 *            the same database platform. If any columns in the list already
	 *            have types assigned, they will be safely ignored.
	 * @param dsCollection
	 *            The datasource collection that contains the user defined
	 *            types.
	 * @param fromPlatform
	 *            The name of the platform these columns were reverse engineered
	 *            from, obtained using {@link JDBCDataSourceType#getName()}.
	 * @param upf
	 *            A UserPrompterFactory, used for asking the user to
	 *            decide between multiple appropriate types.
	 */
	public static void assignTypes(
			List<SQLColumn> columns, 
			DataSourceCollection<? extends SPDataSource> dsCollection, 
			String fromPlatform, 
			UserPrompterFactory upf) {
		if (fromPlatform == null) return; // Dropped from within the PlayPen
		List<UserDefinedSQLType> types = dsCollection.getSQLTypes();
		
		Map<String, UserDefinedSQLType> typeMapByReverseName = new HashMap<String, UserDefinedSQLType>();
		ListMultimap<String, UserDefinedSQLType> typeMapByForwardName = LinkedListMultimap.create();
		ListMultimap<Integer, UserDefinedSQLType> typeMapByCode = LinkedListMultimap.create();
		for (UserDefinedSQLType type : types) {
				typeMapByReverseName.put(type.getName().toLowerCase(), type);
				typeMapByForwardName.put(type.getPhysicalProperties(fromPlatform).getName().toLowerCase(), type);
				typeMapByCode.put(type.getType(), type);
		}
		
		Map<String, UserPrompter> userPrompters = new HashMap<String, UserPrompter>();
		
		for (SQLColumn column : columns) {
			if (column.getUserDefinedSQLType().getUpstreamType() != null) continue;
            String nativeType = column.getSourceDataTypeName();
            if (nativeType != null) {
            	nativeType = nativeType.toLowerCase();
            }
            logger.info("Column has type " + nativeType);
            
            UserDefinedSQLType upstreamType;
            
            if (typeMapByReverseName.containsKey(nativeType)) {
            	upstreamType = typeMapByReverseName.get(nativeType);
            } else {
            	List<UserDefinedSQLType> upstreamTypes;
            	if (nativeType == null) {
            		upstreamTypes = typeMapByForwardName.get("");
            	} else {
            		upstreamTypes = typeMapByForwardName.get(nativeType.toLowerCase());
            	}

            	if (upstreamTypes.size() == 0) {
            		int jdbcCode = column.getType();
            		upstreamTypes = typeMapByCode.get(jdbcCode);
            		if (upstreamTypes.size() == 0) {
            			upstreamType = dsCollection.getNewSQLType(nativeType, jdbcCode);
            			typeMapByForwardName.put(nativeType, upstreamType);
            			typeMapByCode.put(jdbcCode, upstreamType);
            		} else if (upstreamTypes.size() == 1) {
            			upstreamType = upstreamTypes.get(0);
            		} else {
            			UserPrompter prompt;
            			if (userPrompters.get(nativeType) != null) {
            				prompt = userPrompters.get(nativeType);
            			} else {
            				prompt = upf.createListUserPrompter("Choose a type for " + column.getShortDisplayName(), upstreamTypes, upstreamTypes.get(0));
            				userPrompters.put(nativeType, prompt);
            			}
            			prompt.promptUser();
            			upstreamType = (UserDefinedSQLType) prompt.getUserSelectedResponse();
            		}
            	} else if (upstreamTypes.size() == 1) {
            		upstreamType = upstreamTypes.get(0);
            	} else {
            		UserPrompter prompt;
            		if (userPrompters.containsKey(nativeType)) {
            			prompt = userPrompters.get(nativeType);
            		} else {
            			prompt = upf.createListUserPrompter("Choose a type for " + column.getShortDisplayName(), upstreamTypes, upstreamTypes.get(0));
            			userPrompters.put(nativeType, prompt);
            		}
            		prompt.promptUser();
            		upstreamType = (UserDefinedSQLType) prompt.getUserSelectedResponse();
            	}
            }
            
            UserDefinedSQLType type = column.getUserDefinedSQLType();
            
            if (upstreamType != null) {
            	type.setScaleType(fromPlatform, upstreamType.getScaleType(fromPlatform));
            	type.setPrecisionType(fromPlatform, upstreamType.getPrecisionType(fromPlatform));
            	
            	if (upstreamType.getScale(fromPlatform) == type.getScale(fromPlatform) || upstreamType.getScaleType(fromPlatform) == PropertyType.NOT_APPLICABLE) {
            		type.setScale(fromPlatform, null);
            	}

            	if (upstreamType.getPrecision(fromPlatform) == type.getPrecision(fromPlatform) || upstreamType.getPrecisionType(fromPlatform) == PropertyType.NOT_APPLICABLE) {
            		type.setPrecision(fromPlatform, null);
            	}

            	if (upstreamType.getNullability() != null
            			&& upstreamType.getNullability().equals(type.getNullability())) {
            		type.setMyNullability(null);
            	}

            	if (upstreamType.getDefaultValue(fromPlatform) != null
            			&& upstreamType.getDefaultValue(fromPlatform).equals(type.getDefaultValue(fromPlatform))) {
            		type.setDefaultValue(fromPlatform, null);
            	}

            	if (upstreamType.getAutoIncrement() != null
            			&& upstreamType.getAutoIncrement().equals(type.getAutoIncrement())) {
            		type.setMyAutoIncrement(null);
            	}
            	
            	type.setUpstreamType(upstreamType);
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
	public static class ColumnNameComparator implements Comparator<SQLColumn> {

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
		// Type proxy should never override its upstream type's name.
		UserDefinedSQLType upstreamType = userDefinedSQLType.getUpstreamType();

		StringBuilder name;
		
		if (upstreamType == null) {
			if (userDefinedSQLType.getName() == null) {
				return "";
			} else {
				name = new StringBuilder(userDefinedSQLType.getName());
			}
		} else {
			name = new StringBuilder(upstreamType.getName());
		}

		String defaultPlatform = SQLTypePhysicalPropertiesProvider.GENERIC_PLATFORM;
		int precision = userDefinedSQLType.getPrecision(defaultPlatform);
		int scale = userDefinedSQLType.getScale(defaultPlatform);
		PropertyType precisionType = userDefinedSQLType.getPrecisionType(defaultPlatform);
		PropertyType scaleType = userDefinedSQLType.getScaleType(defaultPlatform);

		if (precisionType != PropertyType.NOT_APPLICABLE
				&& scaleType != PropertyType.NOT_APPLICABLE
				&& precision > 0 && scale > 0) {
			name.append("(" + userDefinedSQLType.getPrecision(defaultPlatform)
					+ ", " + userDefinedSQLType.getScale(defaultPlatform) + ")");
		} else if (precisionType != PropertyType.NOT_APPLICABLE && precision > 0) {
			name.append("(" + userDefinedSQLType.getPrecision(defaultPlatform)
					+ ")");
		} else if (scaleType != PropertyType.NOT_APPLICABLE && scale > 0) {
			name.append("(" + userDefinedSQLType.getScale(defaultPlatform)
					+ ")");
		}

		return name.toString();
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

	// ------------------------- accessors and mutators --------------------------

	@Accessor(isInteresting=true)
	public String getPlatform() {
		return platform;
	}

	@Mutator
	public void setPlatform(String newPlatform) {
		String oldPlatform = platform;
		platform = newPlatform;
		begin("Setting platform");
		firePropertyChange("platform", oldPlatform, newPlatform);
		if (userDefinedSQLType.getPhysicalProperties(newPlatform) == null) {
			userDefinedSQLType.putPhysicalProperties(newPlatform, new SQLTypePhysicalProperties(newPlatform));
		}
		commit();
	}
	
	@Accessor
	public UserDefinedSQLType getUserDefinedSQLType() {
		return userDefinedSQLType;
	}
	
	@NonBound
	public List<SQLCheckConstraint> getCheckConstraints() {
		return Collections.unmodifiableList(
				userDefinedSQLType.getCheckConstraints(getPlatform()));
	}
	
	public void addCheckConstraint(SQLCheckConstraint checkConstraint) {
		userDefinedSQLType.addCheckConstraint(getPlatform(), checkConstraint);
	}
	
	public boolean removeCheckConstraint(SQLCheckConstraint checkConstraint) {
		return userDefinedSQLType.removeCheckConstraint(getPlatform(), checkConstraint);
	}
	
	@Transient @Accessor(isInteresting = true)
	public SQLTypeConstraint getConstraintType() {
		return userDefinedSQLType.getConstraintType(getPlatform());
	}

	@Transient @Mutator
	public void setConstraintType(SQLTypeConstraint constraint) {
		userDefinedSQLType.setConstraintType(getPlatform(), constraint);
	}
	
	@NonBound
    public List<SQLEnumeration> getEnumerations() {
        return userDefinedSQLType.getEnumerations(getPlatform());
    }

    public void addEnumeration(SQLEnumeration enumeration) {
		userDefinedSQLType.addEnumeration(getPlatform(), enumeration);
	}
    
    public void removeEnumeration(SQLEnumeration enumeration) {
    	userDefinedSQLType.removeEnumeration(getPlatform(), enumeration);
    }
	
	@Transient @Accessor(isInteresting = true)
	public PropertyType getPrecisionType() {
		return userDefinedSQLType.getPrecisionType(getPlatform());
	}

	@Transient @Mutator
	public void setPrecisionType(PropertyType precisionType) {
		userDefinedSQLType.setPrecisionType(getPlatform(), precisionType);
	}
	
	@Transient @Accessor(isInteresting = true)
    public PropertyType getScaleType() {
        return userDefinedSQLType.getScaleType(getPlatform());
    }
    
	@Transient @Mutator
    public void setScaleType(PropertyType scaleType) {
		userDefinedSQLType.setScaleType(getPlatform(), scaleType);
	}
	
	@Accessor
	public SQLColumn getSourceColumn() {
		return sourceColumn;
	}

	@Mutator
	public void setSourceColumn(SQLColumn col) {
		begin("Setting source column.");
		SQLColumn oldCol = this.sourceColumn;
		sourceColumn = col;
		firePropertyChange("sourceColumn",oldCol,col);
		commit();
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
	public void setType(UserDefinedSQLType type) {
		userDefinedSQLType.setUpstreamType(type);
	}

	/**
	 * Sets the value of type
	 *
	 * @param argType Value to assign to this.type
	 */
	@Transient @Mutator
	public void setType(int type) {
		userDefinedSQLType.setType(type);
	}

	/**
	 * The data type name as obtained during reverse engineering
	 */
	@Transient @Accessor
	public String getSourceDataTypeName() {
		return userDefinedSQLType.getName();
	}

	@Transient @Mutator
	public void setSourceDataTypeName(String n) {
		userDefinedSQLType.setName(n);
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
		userDefinedSQLType.setScale(getPlatform(), argScale);
		
		// Need to change scale type here according to the scale value
		// that is passed in. It is possible that when the project gets
		// loaded initially, the default SQLColumn() constructor gets called,
		// which sets the scale value and type to default. Since scale type
		// is not saved, only the scale value is set here. When getScale()
		// is called, it may return 0 because it will try to read the
		// scale type which could be NOT_APPLICABLE by default.
		if (argScale > 0 && getScaleType() == PropertyType.NOT_APPLICABLE) {
			setScaleType(PropertyType.VARIABLE);
		} else if (argScale == 0 && getScaleType() == PropertyType.VARIABLE) {
			setScaleType(null);
		}
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
		userDefinedSQLType.setPrecision(getPlatform(), argPrecision);
		
		// Need to change precision type here according to the precision value
		// that is passed in. It is possible that when the project gets
		// loaded initially, the default SQLColumn() constructor gets called,
		// which sets the precision value and type to default. Since precision type
		// is not saved, only the precision value is set here. When getPrecision()
		// is called, it may return 0 because it will try to read the
		// precision type which could be NOT_APPLICABLE by default
		if (argPrecision > 0 && getPrecisionType() == PropertyType.NOT_APPLICABLE) {
			setPrecisionType(PropertyType.VARIABLE);
		} else if (argPrecision == 0 && getPrecisionType() == PropertyType.VARIABLE) {
			setPrecisionType(null);
		}
		
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
		logger.debug(userDefinedSQLType);
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
		userDefinedSQLType.setMyNullability(argNullable);
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
		userDefinedSQLType.setMyAutoIncrement(argAutoIncrement);
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
        	return makeAutoIncrementSequenceName();
        } else {
            return autoIncrementSequenceName;
        }
    }

	/**
	 * Creates an auto-increment sequence name based on table and column names.
	 * 
	 * @return The properly formatted sequence name for the column.
	 */
	public String makeAutoIncrementSequenceName() {
		String tableName;
    	if (getParent() == null) {
    		tableName = "";
    	} else if (getParent().getPhysicalName() != null && !getPhysicalName().trim().equals("")) {
    		tableName = getParent().getPhysicalName() + "_";
    	} else {
    		tableName = getParent().getName() +"_";
    	}
        return tableName + getPhysicalName() + "_seq";
	}
    
	public String discoverSequenceNameFormat(String tableName, String colName) {
		String seqName = getAutoIncrementSequenceName();
		String seqNamePrefix, seqNameSuffix;
		String newName = "";
		int prefixEnd = seqName.indexOf(colName);
		
		if ((prefixEnd != -1 && seqName.substring
                (prefixEnd + colName.length()).indexOf(colName) == -1)) {
            seqNamePrefix = seqName.substring(0, prefixEnd);
            seqNameSuffix = seqName.substring(prefixEnd + colName.length());
        } else if (seqName.equals(tableName + "_" + colName + "_seq")) {
            seqNamePrefix = tableName + "_";
            seqNameSuffix = "_seq";
        } else {
            seqNamePrefix = null;
            seqNameSuffix = null;
        }
		if (seqNamePrefix != null && seqNameSuffix != null) {
            newName = seqNamePrefix + colName + seqNameSuffix;
        }
		return newName;
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
        	begin("Setting autoIncrementSequenceName.");
            this.autoIncrementSequenceName = autoIncrementSequenceName;
            firePropertyChange("autoIncrementSequenceName", oldName, autoIncrementSequenceName);
            commit();
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
		return false;
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

	@NonProperty
	public SPVariableResolver getVariableResolver() {
		return variableResolver;
	}

	public void addCheckConstraint(SQLCheckConstraint checkConstraint, int index) {
		userDefinedSQLType.addCheckConstraint(getPlatform(), checkConstraint, index);
	}

	@Override
	@Mutator
	public void setName(String name) {
	    try {
	        begin("Setting name and possibly physical or primary key name.");
	        String oldName = getName();
	        super.setName(name);
	        if (isMagicEnabled()) {
	            updatePhysicalNameToMatch(oldName, name);
	        }
	        commit();
	    } catch (Throwable t) {
	        rollback(t.getMessage());
	        throw new RuntimeException(t);
	    }
	}

	/**
	 * Returns the ETL transformation logic used to get from the source column
	 * to this target column. Currently, this property is only used in SQL Power
	 * Architect Enterprise Edition.
	 */
	@Accessor(isInteresting=true)
	public String getEtlTransformationLogic() {
		return etlTransformationLogic;
	}

	/**
	 * Sets the ETL transformation logic used to get from the source column to
	 * this target column. Currently, this property is only used in SQL Power
	 * Architect Enterprise Edition.
	 * 
	 * @param etlTransformationLogic
	 *            The ETL transformation logic.
	 */
	@Mutator
	public void setEtlTransformationLogic(String etlTransformationLogic) {
		String oldTransformationLogic = this.etlTransformationLogic;
		this.etlTransformationLogic = etlTransformationLogic;
		firePropertyChange("etlTransformationLogic", oldTransformationLogic, etlTransformationLogic);
	}

	/**
	 * Gets the ETL mapping default value. This column should be set to this
	 * default value during an ETL transformation process.
	 * 
	 * @return The ETL mapping default value.
	 */
	@Accessor(isInteresting=true)
	public String getEtlDefaultValue() {
		return etlDefaultValue;
	}

	/**
	 * Sets the ETL mapping default value. This column should be set to this
	 * default value during an ETL transformation process.
	 * 
	 * @param etlDefaultValue
	 *            The ETL mapping default value.
	 */
	@Mutator
	public void setEtlDefaultValue(String etlDefaultValue) {
		String oldDefaultValue = this.etlDefaultValue;
		this.etlDefaultValue = etlDefaultValue;
		firePropertyChange("etlDefaultValue", oldDefaultValue, etlDefaultValue);
	}

	/**
	 * Returns a {@link Boolean} that determines if the ETL mapping from this
	 * target column to the source column is auditable. Auditable means that a
	 * comparison should be done outside the ETL after the process has been
	 * running for a long period of time (e.g. 1 month). Currently, this
	 * property is only used in SQL Power Architect Enterprise Edition.
	 * 
	 * If true is returned, the ETL mapping is auditable. If null is returned,
	 * there is no ETL mapping.
	 */
	@Accessor(isInteresting=true)
	public Boolean getEtlAuditable() {
		return etlAuditable;
	}

	/**
	 * Sets a {@link Boolean} that determines if the ETL mapping from this
	 * target column to the source column is auditable. Currently, this property
	 * is only used in SQL Power Architect Enterprise Edition.
	 * 
	 * @param etlAuditable
	 *            The {@link Boolean} auditable value. If true, then the ETL
	 *            mapping is auditable. This value can be null, which indicates
	 *            that there is no ETL mapping.
	 */
	@Mutator
	public void setEtlAuditable(Boolean etlAuditable) {
		Boolean oldAuditable = this.etlAuditable;
		this.etlAuditable = etlAuditable;
		firePropertyChange("etlAuditable", oldAuditable, etlAuditable);
	}
	
	/**
	 * Returns a {@link String} description about the ETL mapping from this
	 * column to the source column, extra to the transformation logic.
	 * Currently, this property is only used in SQL Power Architect Enterprise
	 * Edition.
	 */
	@Accessor(isInteresting=true)
	public String getEtlNotes() {
		return etlNotes;
	}
	
	/**
	 * Sets a description regarding anything about the ETL mapping from this
	 * column to this source column that is not already defined in the ETL
	 * transformation logic. Currently, this property is only used in SQL Power
	 * Architect Enterprise Edition.
	 * 
	 * @param etlNotes
	 *            The description about the ETL mapping.
	 */
	@Mutator
	public void setEtlNotes(String etlNotes) {
		String oldNotes = this.etlNotes;
		this.etlNotes = etlNotes;
		firePropertyChange("etlNotes", oldNotes, etlNotes);
	}
	
}