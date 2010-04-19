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

import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ca.sqlpower.dao.SPPersister;
import ca.sqlpower.object.SPObject;
import ca.sqlpower.object.annotation.Accessor;
import ca.sqlpower.object.annotation.Constructor;
import ca.sqlpower.object.annotation.Mutator;
import ca.sqlpower.object.annotation.NonProperty;
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
	 * A list of {@link SQLTypePhyisicalProperties} that are the children of this object.
	 * Only one physical properties object should be listed per platform.
	 */
    private List<SQLTypePhysicalProperties> physicalProperties = new ArrayList<SQLTypePhysicalProperties>(); 
    
    /**
     * A textual description of this type for documentation purposes.
     */
    private String description;
    
    /**
     * The int value that corresponds to a type defined in {@link Types} that
     * this {@link UserDefinedSQLType} represents.
     */
    private int type;
    
    @Constructor
    public UserDefinedSQLType() {
    	super();
    	setName("UserDefinedSQLType");
    	setPopulated(true);
    }
    
    @NonProperty
    public SQLTypePhysicalProperties getPhysicalProperties(String platformName) {
        for (SQLTypePhysicalProperties properties : physicalProperties) {
        	if (properties.getPlatform().equals(platformName)) return properties;
        }
        return null;
    }
    
    @Override
    public boolean allowsChildren() {
        return true;
    }

    @Override
    public List<? extends SQLObject> getChildrenWithoutPopulating() {
    	return Collections.unmodifiableList(new ArrayList<SQLTypePhysicalProperties>(physicalProperties));
    }

    @Override
    public String getShortDisplayName() {
        return getName();
    }

    @Override
    protected void populateImpl() throws SQLObjectException {
        // TODO Auto-generated method stub
    }

    @Override
    protected boolean removeChildImpl(SPObject child) {
        int childIndex = physicalProperties.indexOf(child);
    	boolean removedFromList = physicalProperties.remove(child);
		
		if (child != null && removedFromList) {
			fireChildRemoved(SQLTypePhysicalProperties.class, child, childIndex);
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
    	BasicSQLType oldValue = this.basicType;
        this.basicType = basicType;
        firePropertyChange("basicType", oldValue, basicType);
    }

    @Accessor
    public BasicSQLType getBasicType() {
        return basicType;
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
    public int getType() {
         return type;
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

    @NonProperty
    public void setPhysicalDataType(String platform, String physicalDataType) {
    	begin("Setting physicalName");
        getOrCreatePhysicalProperties(platform).setPhysicalName(physicalDataType);
        commit();
    }

    @NonProperty
    public void setPrecision(String platform, int precision) {
    	begin("Setting precision");
        getOrCreatePhysicalProperties(platform).setPrecision(precision);
        commit();
    }

    @NonProperty
    public void setScale(String platform, int scale) {
    	begin("Setting scale");
        getOrCreatePhysicalProperties(platform).setScale(scale);
        commit();
    }

    @Mutator
    public void setType(int type) {
    	int oldValue = this.type;
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
        return description;
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
            putPhysicalProperties(platform, properties);
        }
        return properties;
    }

    @Mutator
	public void setUpstreamType(UserDefinedSQLType upstreamType) {
    	UserDefinedSQLType oldValue = this.upstreamType;
    	this.upstreamType = upstreamType;
		firePropertyChange("upstreamType", oldValue, upstreamType);
	}

    @Accessor
	public UserDefinedSQLType getUpstreamType() {
		return upstreamType;
	}
    
    public void putPhysicalProperties(String platform, SQLTypePhysicalProperties properties) {
    	SQLTypePhysicalProperties oldProperties = getPhysicalProperties(platform);
    	// Add new properties
    	physicalProperties.add(properties);
    	int index = physicalProperties.indexOf(properties);
    	properties.setParent(this);
    	fireChildAdded(SQLTypePhysicalProperties.class, properties, index);
    	// Remove old properties
    	if (oldProperties != null) {
    		int oldIndex = physicalProperties.indexOf(oldProperties);
    		physicalProperties.remove(oldProperties);
    		fireChildRemoved(SQLTypePhysicalProperties.class, oldProperties, oldIndex);
    		oldProperties.setParent(null);
    	}
    }

    @Override
    protected void addChildImpl(SPObject child, int index) {
    	// super.addChild() should already be checking for type
    	if (child instanceof SQLTypePhysicalProperties) {
    		SQLTypePhysicalProperties newProperties = (SQLTypePhysicalProperties) child;
    		SQLTypePhysicalProperties oldProperties = getPhysicalProperties(newProperties.getPlatform());
    		// Add new properties
    		physicalProperties.add(index, newProperties);
    		newProperties.setParent(this);
    		fireChildAdded(SQLTypePhysicalProperties.class, newProperties, index);
    		// Remove old properties
    		if (oldProperties != null) {
    			int oldIndex = physicalProperties.indexOf(oldProperties);
    			physicalProperties.remove(oldProperties);
    			fireChildRemoved(SQLTypePhysicalProperties.class, oldProperties, oldIndex);
    			oldProperties.setParent(null);
    		}
    	} else {
    		throw new IllegalArgumentException("Children Must be of type SQLTypePhysicalProperties");
    	}
    }
}
