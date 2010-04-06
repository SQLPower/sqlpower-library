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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ca.sqlpower.dao.SPPersister;
import ca.sqlpower.object.SPObject;
import ca.sqlpower.sql.JDBCDataSourceType;
import ca.sqlpower.sqlobject.SQLObject;
import ca.sqlpower.sqlobject.SQLObjectException;
import ca.sqlpower.sqlobject.SQLTypePhysicalProperties;
import ca.sqlpower.sqlobject.SQLTypePhysicalPropertiesProvider;
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
     * A Map of a {@link JDBCDataSourceType}'s Logical Name to
     * {@link SQLTypePhysicalProperties}.
     * 
     * The String value is expected to be the Logical Name of a
     * {@link JDBCDataSourceType}, which you would get using
     * {@link JDBCDataSourceType#getName()}.
     */
    private Map<String, SQLTypePhysicalProperties> physicalProperties = new HashMap<String, SQLTypePhysicalProperties>();
    
    /**
     * A textual description of this type for documentation purposes.
     */
    private String description;
    
    /**
     * The int value that corresponds to a type defined in {@link Types} that
     * this {@link UserDefinedSQLType} represents.
     */
    private int type;
    
    /**
     * An unmodifiable {@link List} of allowed child types
     */
    public static List<Class<? extends SPObject>> allowedChildTypes = 
        Collections.unmodifiableList(new ArrayList<Class<? extends SPObject>>(Arrays.asList(SQLTypePhysicalProperties.class)));
    
    public UserDefinedSQLType() {
    }
    
    public UserDefinedSQLType(UserDefinedSQLType upstreamType) {
        this.upstreamType = upstreamType;
    }
    
    public SQLTypePhysicalProperties getPhysicalProperties(String platformName) {
        return physicalProperties.get(platformName);
    }
    
    @Override
    public boolean allowsChildren() {
        return true;
    }

    @Override
    public List<? extends SQLObject> getChildrenWithoutPopulating() {
        return null;
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
        if (child != null && physicalProperties.remove(child.getName()) == null) {
            return false;
        } else {
            return true;
        }
    }

    public int childPositionOffset(Class<? extends SPObject> childType) {
        return 0;
    }

    public List<Class<? extends SPObject>> getAllowedChildTypes() {
        return allowedChildTypes;
    }

    public List<? extends SPObject> getDependencies() {
        return Collections.emptyList();
    }

    public void removeDependency(SPObject dependency) {
        // no-op
    }

    public void setBasicType(BasicSQLType basicType) {
        this.basicType = basicType;
    }

    public BasicSQLType getBasicType() {
        return basicType;
    }

    public String getCheckConstraint(String platform) {
        String checkConstraint = null;
        SQLTypePhysicalProperties properties = physicalProperties.get(platform);
        
        if (properties != null) {
            checkConstraint = properties.getCheckConstraint();
            if (checkConstraint == null && upstreamType != null) {
                checkConstraint = upstreamType.getCheckConstraint(platform);
            }
        } else if (upstreamType != null) {
            checkConstraint = upstreamType.getCheckConstraint(platform);
        }
        
        return checkConstraint;
    }

    public SQLTypeConstraint getConstraintType(String platform) {
        SQLTypeConstraint constraintType = null;
        SQLTypePhysicalProperties properties = physicalProperties.get(platform);
        
        if (properties != null) {
            constraintType = properties.getConstraintType();
            if (constraintType == null && upstreamType != null) {
                constraintType = upstreamType.getConstraintType(platform);
            }
        } else if (upstreamType != null) {
            constraintType = upstreamType.getConstraintType(platform);
        }
        
        return constraintType;
    }

    public String getDefaultValue(String platform) {
        String defaultValue = null;
        SQLTypePhysicalProperties properties = physicalProperties.get(platform);
        
        if (properties != null) {
            defaultValue = properties.getDefaultValue();
            if (defaultValue == null && upstreamType != null) {
                defaultValue = upstreamType.getDefaultValue(platform);
            }
        } else if (upstreamType != null) {
            defaultValue = upstreamType.getDefaultValue(platform);
        }
        
        return defaultValue;
    }

    public List<String> getEnumeration(String platform) {
        List<String> enumeration = null;
        SQLTypePhysicalProperties properties = physicalProperties.get(platform);
        
        if (properties != null) {
            enumeration = properties.getEnumeration();
            if (enumeration == null && upstreamType != null) {
                enumeration = upstreamType.getEnumeration(platform);
            }
        } else if (upstreamType != null) {
            enumeration = upstreamType.getEnumeration(platform);
        }
        
        return enumeration;
    }

    public int getPrecision(String platform) {
        Integer precision = null;
        SQLTypePhysicalProperties properties = physicalProperties.get(platform);
        
        if (properties != null) {
            precision = properties.getPrecision();
            if (precision == null && upstreamType != null) {
            	precision = upstreamType.getPrecision(platform);
            }
        } else if (upstreamType != null) {
            precision = upstreamType.getPrecision(platform);
        }
        
        // If precision is null and all upstream types also return null, then just return 0 to prevent an NPE
        return precision == null ? 0 : precision;
    }

    public int getScale(String platform) {
        Integer scale = null;
        SQLTypePhysicalProperties properties = physicalProperties.get(platform);
        
        if (properties != null) {
            scale = properties.getScale();
            if (scale == null && upstreamType != null) {
                scale = upstreamType.getScale(platform);
            }
        } else if (upstreamType != null) {
            scale = upstreamType.getScale(platform);
        }
        
        // If scale is null and all upstream types also return null, then just return 0 to prevent an NPE
        return scale == null ? 0 : scale;
    }

    public int getType(String platform) {
         return type;
    }

    public void setCheckConstraint(String platform, String checkConstraint) {
        getOrCreatePhysicalProperties(platform).setCheckConstraint(checkConstraint);
    }

    public void setConstraintType(String platform, SQLTypeConstraint constraint) {
        getOrCreatePhysicalProperties(platform).setConstraintType(constraint);
    }

    public void setDefaultValue(String platform, String defaultValue) {
        getOrCreatePhysicalProperties(platform).setDefaultValue(defaultValue);
    }

    public void setEnumeration(String platform, List<String> enumeration) {
        getOrCreatePhysicalProperties(platform).setEnumeration(enumeration);
    }

    public void setPhysicalDataType(String platform, String physicalDataType) {
        getOrCreatePhysicalProperties(platform).setPhysicalName(physicalDataType);
    }

    public void setPrecision(String platform, int precision) {
        getOrCreatePhysicalProperties(platform).setPrecision(precision);
    }

    public void setScale(String platform, int scale) {
        getOrCreatePhysicalProperties(platform).setScale(scale);
    }

    public void setType(int type) {
        this.type = type;
    }

    public PropertyType getPrecisionType(String platform) {
        PropertyType precisionType = null;
        SQLTypePhysicalProperties properties = physicalProperties.get(platform);
        
        if (properties != null) {
            precisionType = properties.getPrecisionType();
            if (precisionType == null && upstreamType != null) {
                precisionType = upstreamType.getPrecisionType(platform);
            }
        } else if (upstreamType != null) {
            precisionType = upstreamType.getPrecisionType(platform);
        }
        
        return precisionType;
    }

    public PropertyType getScaleType(String platform) {
        PropertyType scaleType = null;
        SQLTypePhysicalProperties properties = physicalProperties.get(platform);
        
        if (properties != null) {
            scaleType = properties.getScaleType();
            if (scaleType == null && upstreamType != null) {
                scaleType = upstreamType.getScaleType(platform);
            }
        } else if (upstreamType != null) {
            scaleType = upstreamType.getScaleType(platform);
        }
        
        return scaleType;
    }

    public void setPrecisionType(String platform, PropertyType precisionType) {
        getOrCreatePhysicalProperties(platform).setPrecisionType(precisionType);
    }

    public void setScaleType(String platform, PropertyType scaleType) {
        getOrCreatePhysicalProperties(platform).setScaleType(scaleType);
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
    
    public void putPhysicalProperties(String platform, SQLTypePhysicalProperties properties) {
        physicalProperties.put(platform, properties);
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
        SQLTypePhysicalProperties properties = physicalProperties.get(platform);
        if (properties == null) {
            properties = new SQLTypePhysicalProperties(platform);
            properties.setParent(this);
            physicalProperties.put(platform, properties);
        }
        return properties;
    }
}
