/*
 * Copyright (c) 2010, SQL Power Group Inc.
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
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ca.sqlpower.dao.SPPersister;
import ca.sqlpower.object.ObjectDependentException;
import ca.sqlpower.object.SPObject;
import ca.sqlpower.object.annotation.Accessor;
import ca.sqlpower.object.annotation.Constructor;
import ca.sqlpower.object.annotation.ConstructorParameter;
import ca.sqlpower.object.annotation.Mutator;
import ca.sqlpower.object.annotation.NonProperty;
import ca.sqlpower.object.annotation.Transient;
import ca.sqlpower.object.annotation.ConstructorParameter.ParameterType;
import ca.sqlpower.sql.JDBCDataSourceType;
import ca.sqlpower.sqlobject.SQLTypePhysicalProperties.SQLTypeConstraint;

/**
 * An implementation of {@link SQLTypePhysicalPropertiesProvider}
 * that provides support for multiple SQLTypePhysicalProperties,
 * keyed by each physical platform's logical name (Typically this would be
 * retrieved from the platform's corresponding
 * {@link JDBCDataSourceType#getName()}. It will also be annotated to allow
 * generation of a corresponding {@link SPPersister}.
 */
public class UserDefinedSQLType extends SQLObject implements SQLTypePhysicalPropertiesProvider {

    /**
     * An unmodifiable {@link List} of allowed child types
     */
    public static final List<Class<? extends SPObject>> allowedChildTypes = 
        Collections.<Class<? extends SPObject>>singletonList(SQLTypePhysicalProperties.class);
    
    /**
     * The {@link UserDefinedSQLType} may have an 'upstream' type from which it
     * inherits properties from.
     */
    private UserDefinedSQLType upstreamType;
    
    /**
     * A generic, platform-independent, high-level data type that describes this
     * particular SQLType.
     */
    private BasicSQLType basicType;
    
	/**
	 * The Default {@link SQLTypePhysicalProperties} for this type. By default,
	 * requests for physical properties for all platforms from this type will
	 * use this instance, UNLESS an overriding instance exists for the requested
	 * platform in the {@link #overridingPhysicalProperties} list. All requests
	 * for platform of
	 * {@link SQLTypePhysicalPropertiesProvider#GENERIC_PLATFORM} will return
	 * this default instance.
	 */
    private final SQLTypePhysicalProperties defaultPhysicalProperties;

	/**
	 * A list of {@link SQLTypePhyisicalProperties} that are the children of
	 * this object. These {@link SQLTypePhysicalProperties} instances are
	 * overrides for the {@link #defaultPhysicalProperties} specific to a given
	 * platform. Only one physical properties object should be listed per
	 * platform.
	 */
	private List<SQLTypePhysicalProperties> overridingPhysicalProperties = new ArrayList<SQLTypePhysicalProperties>();

    /**
     * A textual description of this type for documentation purposes.
     */
    private String description;
    
    /**
     * The int value that corresponds to a type defined in {@link Types} that
     * this {@link UserDefinedSQLType} represents.
     */
    private Integer type;

	/**
	 * Specifies whether this type accepts NULL as a value, based on the values
	 * specified by {@link DatabaseMetaData}. These include:
	 * <ul>
	 * <li>{@link DatabaseMetaData#columnNoNulls}</li>
	 * <li>{@link DatabaseMetaData#columnNullable}</li>
	 * <li>{@link DatabaseMetaData#columnNullableUnknown}</li>
	 * </ul>
	 */
    private Integer myNullability;
    
    /**
     * This property indicates that values stored in this column should
     * default to some automatically-incrementing sequence of values.  Every
     * database platform handles the specifics of this a little differently,
     * but the DDL generators are responsible for taking care of that.
     */
    private Boolean myAutoIncrement;
    
	/**
	 * Constructs a {@link UserDefinedSQLType} with a default
	 * {@link SQLTypePhysicalProperties} set with a platform of
	 * {@link SQLTypePhysicalPropertiesProvider#GENERIC_PLATFORM}
	 */
    public UserDefinedSQLType() {
    	this("UserDefinedSQLType", null, null, null, null, new SQLTypePhysicalProperties(GENERIC_PLATFORM));
	}
    
	/**
	 * Constructs a {@link UserDefinedSQLType} with the
	 * {@link #defaultPhysicalProperties} set to the
	 * {@link SQLTypePhysicalProperties} in the argument.
	 * 
	 * @param defaultPhysicalProperties
	 *            Sets the defaultPhysicalProperties to this instance
	 */
    @Constructor
    public UserDefinedSQLType(
    		@ConstructorParameter(propertyName = "name") String name,
            @ConstructorParameter(propertyName = "myNullability") Integer nullability,
            @ConstructorParameter(propertyName = "myAutoIncrement") Boolean autoIncrement,
            @ConstructorParameter(propertyName = "basicType") BasicSQLType basicType,
            @ConstructorParameter(propertyName = "upstreamType") UserDefinedSQLType upstreamType,
    		@ConstructorParameter(isProperty = ParameterType.CHILD, 
    		propertyName = "primaryKeyIndex") SQLTypePhysicalProperties defaultPhysicalProperties) {
    	super();
    	this.defaultPhysicalProperties = defaultPhysicalProperties;
    	setName(name);
    	defaultPhysicalProperties.setParent(this);
    	setPopulated(true);
    	setMyNullability(nullability);
    	setMyAutoIncrement(autoIncrement);
    	setBasicType(basicType);
    	setUpstreamType(upstreamType);
    }
    
	/**
	 * Returns an overriding {@link SQLTypePhysicalProperties} for the given
	 * platform name if one exists. Otherwise, it returns the
	 * {@link #defaultPhysicalProperties}.
	 * 
	 * @param platformName The platform name to return a {@link SQLTypePhysicalProperties} for.
	 * @return If an overriding instance exists in this type, then return it. Otherwise, it returns the
	 * 
	 */
    @NonProperty
    public SQLTypePhysicalProperties getPhysicalProperties(String platformName) {
    	if (!GENERIC_PLATFORM.equals(platformName)) {
	        for (SQLTypePhysicalProperties properties : overridingPhysicalProperties) {
	        	if (properties.getPlatform().equals(platformName)) return properties;
	        }
    	}
        return defaultPhysicalProperties;
    }
    
    @Override
    public boolean allowsChildren() {
        return true;
    }

    @Override
    public List<? extends SQLObject> getChildrenWithoutPopulating() {
    	ArrayList<SQLTypePhysicalProperties> properties = new ArrayList<SQLTypePhysicalProperties>();
    	properties.add(defaultPhysicalProperties);
    	properties.addAll(overridingPhysicalProperties);
		return Collections.unmodifiableList(properties);
    }

    @Override
    public String getShortDisplayName() {
        return getName();
    }

    @Override
    protected void populateImpl() throws SQLObjectException {}

    @Override
    protected boolean removeChildImpl(SPObject child) {
        int childIndex = overridingPhysicalProperties.indexOf(child);
    	boolean removedFromList = overridingPhysicalProperties.remove(child);
		
		if (child != null && removedFromList) {
			//The defaultPhysicalProperties is the first physical properties in the first position.
			fireChildRemoved(SQLTypePhysicalProperties.class, child, childIndex + 1);
			child.setParent(null);
            return true;
        } else {
            return false;
        }
    }

    public int childPositionOffset(Class<? extends SPObject> childType) {
        return 0;
    }

    public List<Class<? extends SPObject>> getAllowedChildTypes() {
        return allowedChildTypes;
    }

    public List<? extends SPObject> getDependencies() {
        return Collections.singletonList(getUpstreamType());
    }

    public void removeDependency(SPObject dependency) {
        if (dependency == getUpstreamType()) {
        	setUpstreamType(null);
        }
    }

    @Mutator
    public void setBasicType(BasicSQLType basicType) {
    	begin("Setting basic type.");
    	BasicSQLType oldValue = this.basicType;
        this.basicType = basicType;
        firePropertyChange("basicType", oldValue, basicType);
        
        // Hack to set the appropriate "type" property given a basic type
        // At current, we are using the most generic SQL type so that all
        // the properties within the data type, such as precision and scale,
        // can be used.
        if (getType() == null) {
        	setType(BasicSQLType.convertFromBasicSQLType(basicType));
        }
        
        commit();
    }

    @Accessor
    public BasicSQLType getBasicType() {
    	if (basicType == null && upstreamType != null) {
    		return upstreamType.getBasicType();
    	} else {
    		return basicType;
    	}
    }

    @NonProperty
    public String getCheckConstraint(String platform) {
        String checkConstraint = null;
        SQLTypePhysicalProperties properties = getPhysicalProperties(platform);
        
        if (properties != null) {
            checkConstraint = properties.getCheckConstraint();
            if (checkConstraint == null && getUpstreamType() != null) {
                checkConstraint = getUpstreamType().getCheckConstraint(platform);
            }
        } else if (getUpstreamType() != null) {
            checkConstraint = getUpstreamType().getCheckConstraint(platform);
        }
        
        return checkConstraint;
    }

    @NonProperty
    public SQLTypeConstraint getConstraintType(String platform) {
        SQLTypeConstraint constraintType = null;
        SQLTypePhysicalProperties properties = getPhysicalProperties(platform);
        
        if (properties != null) {
            constraintType = properties.getConstraintType();
            if (constraintType == null && getUpstreamType() != null) {
                constraintType = getUpstreamType().getConstraintType(platform);
            }
        } else if (getUpstreamType() != null) {
            constraintType = getUpstreamType().getConstraintType(platform);
        }
        
        return constraintType;
    }

    @NonProperty
    public String getDefaultValue(String platform) {
        String defaultValue = null;
        SQLTypePhysicalProperties properties = getPhysicalProperties(platform);
        
        if (properties != null) {
            defaultValue = properties.getDefaultValue();
            if (defaultValue == null && getUpstreamType() != null) {
                defaultValue = getUpstreamType().getDefaultValue(platform);
            }
        } else if (getUpstreamType() != null) {
            defaultValue = getUpstreamType().getDefaultValue(platform);
        }
        
        return defaultValue;
    }

    @NonProperty
    public String[] getEnumeration(String platform) {
        String[] enumeration = null;
        SQLTypePhysicalProperties properties = getPhysicalProperties(platform);
        
        if (properties != null) {
        	String[] array = properties.getEnumeration();
        	if (array != null) {
        		enumeration = array;
        	} else if (getUpstreamType() != null) {
                enumeration = getUpstreamType().getEnumeration(platform);
            }
        } else if (getUpstreamType() != null) {
            enumeration = getUpstreamType().getEnumeration(platform);
        }
        
        return enumeration;
    }

    @NonProperty
    public int getPrecision(String platform) {
        Integer precision = null;
        SQLTypePhysicalProperties properties = getPhysicalProperties(platform);
        
        if (properties != null) {
            precision = properties.getPrecision();
            if (precision == null && getUpstreamType() != null) {
            	precision = getUpstreamType().getPrecision(platform);
            }
        } else if (getUpstreamType() != null) {
            precision = getUpstreamType().getPrecision(platform);
        }
        
        // If precision is null and all upstream types also return null, then just return 0 to prevent an NPE
        return precision == null ? 0 : precision;
    }

    @NonProperty
    public int getScale(String platform) {
        Integer scale = null;
        SQLTypePhysicalProperties properties = getPhysicalProperties(platform);
        
        if (properties != null) {
            scale = properties.getScale();
            if (scale == null && getUpstreamType() != null) {
                scale = getUpstreamType().getScale(platform);
            }
        } else if (getUpstreamType() != null) {
            scale = getUpstreamType().getScale(platform);
        }
        
        // If scale is null and all upstream types also return null, then just return 0 to prevent an NPE
        return scale == null ? 0 : scale;
    }

    @Accessor
    public Integer getType() {
    	if (type == null && upstreamType != null) {
    		return upstreamType.getType();
    	} else {
    		return type;
    	}
    }

    @NonProperty
    public void setCheckConstraint(String platform, String checkConstraint) {
    	begin("Setting checkConstraint");
        getOrCreatePhysicalProperties(platform).setCheckConstraint(checkConstraint);
        commit();
    }

    @NonProperty
    public void setConstraintType(String platform, SQLTypeConstraint constraint) {
    	begin("Setting constraintType");
        getOrCreatePhysicalProperties(platform).setConstraintType(constraint);
        commit();
    }

    @NonProperty
    public void setDefaultValue(String platform, String defaultValue) {
    	begin("Setting defaultValue");
        getOrCreatePhysicalProperties(platform).setDefaultValue(defaultValue);
        commit();
    }

    @NonProperty
    public void setEnumeration(String platform, String[] enumeration) {
    	begin("Setting enumeration");
        getOrCreatePhysicalProperties(platform).setEnumeration(enumeration);
        commit();
    }

	/**
	 * Sets the name of the {@link SQLTypePhysicalProperties} object keyed with
	 * the given platform
	 * 
	 * @param platform
	 *            The platform key that identifies the
	 *            {@link SQLTypePhysicalProperties} to name.
	 * @param name
	 *            The new name value to set to
	 */
    @NonProperty
    public void setPhysicalTypeName(String platform, String name) {
    	begin("Setting physicalTypeName");
        getOrCreatePhysicalProperties(platform).setName(name);
        commit();
    }

    @NonProperty
    public void setPrecision(String platform, Integer precision) {
    	begin("Setting precision");
        getOrCreatePhysicalProperties(platform).setPrecision(precision);
        commit();
    }

    @NonProperty
    public void setScale(String platform, Integer scale) {
    	begin("Setting scale");
        getOrCreatePhysicalProperties(platform).setScale(scale);
        commit();
    }

    @Mutator
    public void setType(Integer type) {
    	Integer oldValue = getType();
        this.type = type;
        firePropertyChange("type", oldValue, type);
    }

    @NonProperty
    public PropertyType getPrecisionType(String platform) {
        PropertyType precisionType = null;
        SQLTypePhysicalProperties properties = getPhysicalProperties(platform);
        
        if (properties != null) {
            precisionType = properties.getPrecisionType();
            if (precisionType == null && getUpstreamType() != null) {
                precisionType = getUpstreamType().getPrecisionType(platform);
            }
        } else if (getUpstreamType() != null) {
            precisionType = getUpstreamType().getPrecisionType(platform);
        }
        
        if (precisionType == null) {
        	precisionType = PropertyType.NOT_APPLICABLE;
        }
        
        return precisionType;
    }

    @NonProperty
    public PropertyType getScaleType(String platform) {
        PropertyType scaleType = null;
        SQLTypePhysicalProperties properties = getPhysicalProperties(platform);
        
        if (properties != null) {
            scaleType = properties.getScaleType();
            if (scaleType == null && getUpstreamType() != null) {
                scaleType = getUpstreamType().getScaleType(platform);
            }
        } else if (getUpstreamType() != null) {
            scaleType = getUpstreamType().getScaleType(platform);
        }
        
        if (scaleType == null) {
        	scaleType = PropertyType.NOT_APPLICABLE;
        }
        
        return scaleType;
    }

    @NonProperty
    public void setPrecisionType(String platform, PropertyType precisionType) {
    	begin("Setting precisionType");
        getOrCreatePhysicalProperties(platform).setPrecisionType(precisionType);
        commit();
    }

    @NonProperty
    public void setScaleType(String platform, PropertyType scaleType) {
    	begin("Setting scaleType");
        getOrCreatePhysicalProperties(platform).setScaleType(scaleType);
        commit();
    }

    @Mutator
    public void setDescription(String description) {
        String oldValue = this.description;
    	this.description = description;
        firePropertyChange("description", oldValue, description);
    }

    @Accessor
    public String getDescription() {
        return description == null && upstreamType != null ? upstreamType.getDescription() : description;
    }
    
    
    /**
     * Gets a {@link SQLTypePhysicalProperties} instance for the given platform
     * from this type's map of {@link SQLTypePhysicalProperties}. If none
     * exists, then create a fresh one.
     * 
     * @param platform
     *            The platform name to search physical properties for
     * @return Either an existing {@link SQLTypePhysicalProperties} for the
     *         given platform, or a new one created if none existed previously
     */
    private SQLTypePhysicalProperties getOrCreatePhysicalProperties(String platform) {
        SQLTypePhysicalProperties properties = getPhysicalProperties(platform);
        if (properties == null) {
            properties = new SQLTypePhysicalProperties(platform);
            properties.setName(getName());
            putPhysicalProperties(platform, properties);
        }
        return properties;
    }

    @Mutator
	public void setUpstreamType(UserDefinedSQLType upstreamType) {
    	begin("setting upstream type");
    	
    	UserDefinedSQLType oldValue = this.upstreamType;
    	this.upstreamType = upstreamType;
		firePropertyChange("upstreamType", oldValue, upstreamType);
		
		if (upstreamType != null) {
			for (String platform : platforms()) {
				if (upstreamType.getPhysicalProperties(platform) != null) {
					getPhysicalProperties(platform)
					.setName(upstreamType.getPhysicalProperties(platform).getName());
				}
			}
			// Fix/hack to make sure the type is set to the same as the upstream
			// type.
			setType(upstreamType.getType());
		}
		commit();
	}
    
    @Accessor
	public UserDefinedSQLType getUpstreamType() {
		return upstreamType;
	}
    
	/**
	 * An important note of this implementation of
	 * {@link #addChildImpl(SPObject, int)} is that it will not accept adding
	 * {@link SQLTypePhysicalProperties} for the
	 * {@link SQLTypePhysicalPropertiesProvider#GENERIC_PLATFORM) The
	 * SQLTypePhysicalProperties for GENERIC_PLATFORM is always {
	 * @link #defaultPhysicalProperties}, and it cannot ever be changed.
	 */
    public void putPhysicalProperties(String platform, SQLTypePhysicalProperties properties) {
		if (platform.equals(GENERIC_PLATFORM)) {
			throw new IllegalArgumentException(
					"The SQLTypePhysicalProperties object for "
							+ GENERIC_PLATFORM
							+ " cannot be overwritten. Instead, use getDefaultPhysicalProperties and modify its properties");
		}
    	SQLTypePhysicalProperties oldProperties = getPhysicalProperties(platform);
    	// Add new properties
    	overridingPhysicalProperties.add(properties);
    	int index = overridingPhysicalProperties.indexOf(properties) + 1;
    	properties.setParent(this);
    	fireChildAdded(SQLTypePhysicalProperties.class, properties, index);
    	// Remove old properties
    	if (oldProperties != null && oldProperties != defaultPhysicalProperties) {
    		int oldIndex = overridingPhysicalProperties.indexOf(oldProperties);
    		overridingPhysicalProperties.remove(oldProperties);
    		fireChildRemoved(SQLTypePhysicalProperties.class, oldProperties, oldIndex);
    		oldProperties.setParent(null);
    	}
    }

	/**
	 * An important note of this implementation of
	 * {@link #addChildImpl(SPObject, int)} is that it will not accept adding
	 * children at index 0. Index 0 is the index of the
	 * {@link #defaultPhysicalProperties}, and it cannot ever be moved.
	 */
    @Override
    protected void addChildImpl(SPObject child, int index) {
    	// super.addChild() should already be checking for type
    	if (child instanceof SQLTypePhysicalProperties) {
    		SQLTypePhysicalProperties newProperties = (SQLTypePhysicalProperties) child;
    		if (newProperties == defaultPhysicalProperties) return;
			if (index == 0) {
				throw new IllegalArgumentException(
						"Cannot insert child at index 0, as this is where the default physical properties must always be.");
			}
    		SQLTypePhysicalProperties oldProperties = getPhysicalProperties(newProperties.getPlatform());
			// Add new properties. Insert at index - 1 is because
			// defaultPhysicalProperties is an always existent child at index 0.
    		overridingPhysicalProperties.add(index - 1, newProperties);
    		newProperties.setParent(this);
    		fireChildAdded(SQLTypePhysicalProperties.class, newProperties, index);
    		// Remove old properties
    		if (oldProperties != null && oldProperties != defaultPhysicalProperties) {
    			int oldIndex = overridingPhysicalProperties.indexOf(oldProperties);
    			overridingPhysicalProperties.remove(oldProperties);
    			fireChildRemoved(SQLTypePhysicalProperties.class, oldProperties, oldIndex);
    			oldProperties.setParent(null);
    		}
    	} else {
    		throw new IllegalArgumentException("Children Must be of type SQLTypePhysicalProperties");
    	}
    }
    
    @Override
    public String toString() {
    	return getName();
    }

    @Mutator
	public void setMyNullability(Integer nullability) {
    	Integer oldValue = getMyNullability();
		this.myNullability = nullability;
		firePropertyChange("myNullability", oldValue, nullability);
	}

    @Transient @Accessor
	public Integer getNullability() {
    	if (myNullability == null && upstreamType != null) {
    		return upstreamType.getNullability();
    	} else {
    		return myNullability;
    	}
	}
    
    @Accessor
    public Integer getMyNullability() {
        return myNullability;
    }

    @Mutator
	public void setMyAutoIncrement(Boolean autoIncrement) {
    	Boolean oldValue = getMyAutoIncrement();
		this.myAutoIncrement = autoIncrement;
		firePropertyChange("myAutoIncrement", oldValue, autoIncrement);
	}

    @Transient @Accessor
	public Boolean getAutoIncrement() {
		return (myAutoIncrement == null && upstreamType != null) ? upstreamType.getAutoIncrement() : myAutoIncrement;
	}
    
    @Accessor
    public Boolean getMyAutoIncrement() {
        return myAutoIncrement;
    }

	/**
	 * goes through each of the UserDefinedSQLType's children appending each's
	 * platform to the list of names
	 * 
	 * @return a list of platform names
	 */
    public List<String> platforms() {
    	List<String> platforms = new ArrayList<String>();
    	for (SQLTypePhysicalProperties properties : overridingPhysicalProperties) {
    		platforms.add(properties.getPlatform());
    	}
    	platforms.add(defaultPhysicalProperties.getPlatform());
    	return platforms;
    }
    
	/**
	 * Updates each of this UserDefinedSQLType's properties to match the input
	 * type. This includes the properties of the physical properties. The
	 * changes are made in a single transaction.
	 * 
	 * @param matchMe
	 *            the UserDefinedSQLType who's properties we are copying
	 */
    @Override
    public void updateToMatch(SQLObject matchMe) {
    	if (!(matchMe instanceof UserDefinedSQLType)) {
    		throw new ClassCastException();
    	}
    	
    	UserDefinedSQLType typeToMatch = (UserDefinedSQLType) matchMe;
    	
    	begin("Copying Properties");
    	
    	// Properties of UserDefinedSQLType
    	setName(typeToMatch.getName());
    	setType(typeToMatch.getType());
    	setBasicType(typeToMatch.getBasicType());
    	setMyNullability(typeToMatch.getNullability());
    	setMyAutoIncrement(typeToMatch.getAutoIncrement());
    	setDescription(typeToMatch.getDescription());
    	setUpstreamType(typeToMatch.getUpstreamType());
    	
    	// Children of UserDefinedSQLType
    	List<String> oldPlatforms = platforms();
    	List<String> newPlatforms = typeToMatch.platforms();
    	for (String platform : oldPlatforms) {
    		if (!newPlatforms.contains(platform)) {
    			try {
    				removeChild(getPhysicalProperties(platform));
    			} catch (Exception e) {
    				throw new RuntimeException("Cannot remove child!!!!");
    			}
    		}
    	}
    	for (String platform : newPlatforms) {
    		if (!oldPlatforms.contains(platform)) {
    			putPhysicalProperties(platform, new SQLTypePhysicalProperties(platform));
    		}
    	}
    	
    	// Properties of the children
    	for (String platform : newPlatforms) {
    		getPhysicalProperties(platform).updateToMatch(typeToMatch.getPhysicalProperties(platform));
    	}
    	
    	commit();
    }
    
    @Transient @Accessor
    public SQLTypePhysicalProperties getDefaultPhysicalProperties() {
		return defaultPhysicalProperties;
	}

	static final void copyProperties(final UserDefinedSQLType target,
			final UserDefinedSQLType source) throws IllegalArgumentException,
			ObjectDependentException {
		target.setUpstreamType(source.getUpstreamType());
		target.setName(source.getName());
		target.setPhysicalName(source.getPhysicalName());
		// Don't use getters as they will refer to the upstreamType if the
		// values are null
		target.setMyAutoIncrement(source.myAutoIncrement);
		target.setBasicType(source.basicType);
		target.setDescription(source.description);
		target.setMyNullability(source.myNullability);
		target.setType(source.type);

		SQLTypePhysicalProperties targetProperties = target
				.getDefaultPhysicalProperties();
		SQLTypePhysicalProperties sourceProperties = source
				.getDefaultPhysicalProperties();

		targetProperties.setCheckConstraint(sourceProperties
				.getCheckConstraint());
		targetProperties
				.setConstraintType(sourceProperties.getConstraintType());
		targetProperties.setDefaultValue(sourceProperties.getDefaultValue());
		targetProperties.setEnumeration(sourceProperties.getEnumeration());
		targetProperties.setName(sourceProperties.getName());
		targetProperties.setPhysicalName(sourceProperties.getPhysicalName());
		targetProperties.setPrecision(sourceProperties.getPrecision());
		targetProperties.setPrecisionType(sourceProperties.getPrecisionType());
		targetProperties.setScale(sourceProperties.getScale());
		targetProperties.setScaleType(sourceProperties.getScaleType());

		for (SQLTypePhysicalProperties overridingProperties : target.overridingPhysicalProperties) {
			target.removeChild(overridingProperties);
		}

		for (SQLTypePhysicalProperties overridingProperties : source.overridingPhysicalProperties) {
			String platform = overridingProperties.getPlatform();
			target.setCheckConstraint(platform, overridingProperties
					.getCheckConstraint());
			target.setConstraintType(platform, overridingProperties
					.getConstraintType());
			target.setDefaultValue(platform, overridingProperties
					.getDefaultValue());
			target.setEnumeration(platform, overridingProperties
					.getEnumeration());
			target.setName(overridingProperties.getName());
			target.setPhysicalName(overridingProperties.getPhysicalName());
			target.setPrecision(platform, overridingProperties.getPrecision());
			target.setPrecisionType(platform, overridingProperties
					.getPrecisionType());
			target.setScale(platform, overridingProperties.getScale());
			target.setScaleType(platform, overridingProperties.getScaleType());
		}
	}

	/**
	 * Returns the physical name of this type under the given platform.
	 * 
	 * @param platform
	 *            The name of the platform (usually what is returned by
	 *            {@link JDBCDataSourceType#getName()}
	 * @return The physical name of this type for the given platform. If a
	 *         platform specific override doesn't exist for that platform, it
	 *         returns the name under the 'default' platform.
	 */
	@NonProperty
	public String getPhysicalName(String platform) {
		String physicalName = null;
        SQLTypePhysicalProperties properties = getPhysicalProperties(platform);
        
        if (properties != null) {
            physicalName = properties.getName();
            if (physicalName == null && getUpstreamType() != null) {
                physicalName = getUpstreamType().getPhysicalName(platform);
            }
        } else if (getUpstreamType() != null) {
            physicalName = getUpstreamType().getPhysicalName(platform);
        }
        
        return physicalName;
	}
	
	@Override @Mutator
	public void setName(String name) {
		begin("Setting name");
		super.setName(name);
		defaultPhysicalProperties.setName(name);
		commit();
	}
}
