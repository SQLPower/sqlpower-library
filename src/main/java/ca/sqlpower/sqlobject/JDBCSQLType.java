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
import java.util.List;

import ca.sqlpower.object.SPObject;
import ca.sqlpower.sqlobject.SQLTypePhysicalProperties.SQLTypeConstraint;

/**
 * Base implemention of {@link SQLType}
 */
//TODO: With the implementation UserDefinedSQLType, this class's days may be numbered.
public class JDBCSQLType extends SQLObject implements SQLTypePhysicalPropertiesProvider {

    /**
     * A generic, platform-independent, high-level data type that describes this
     * particular SQLType.
     */
    private BasicSQLType basicType;
    
    /**
     * A {@link SQLTypePhysicalProperties} instance representing the physical
     * properties of this type.
     */
    private SQLTypePhysicalProperties physicalProperties;
    
    private int type;
    
    /**
     * An unmodifiable {@link List} of allowed child types
     */
    public static List<Class<? extends SPObject>> allowedChildTypes = 
        Collections.unmodifiableList(new ArrayList<Class<? extends SPObject>>(Arrays.asList(SQLTypePhysicalProperties.class)));    
    
    public JDBCSQLType() {
	}
    
    /**
     * Create a new {@link JDBCSQLType} with a given name and basic type.
     * 
     * @param name
     *            The name to give to the {@link JDBCSQLType}. As a
     *            platform-independent data type name, it would ideally be one
     *            of the values in {@link Types}.
     * @param basicType
     *            The {@link BasicSQLType} of the {@link JDBCSQLType}
     */
    public JDBCSQLType(String name, BasicSQLType basicType) {
        super();
        setName(name);
        this.setBasicType(basicType);
        physicalProperties = new SQLTypePhysicalProperties("default");
        physicalProperties.setParent(this);
    }
    
    public SQLTypePhysicalProperties getPhysicalProperties(String dsTypeName) {
        return physicalProperties;
    }
    
    @Override
    public boolean allowsChildren() {
        return true;
    }

    @Override
    public List<? extends SQLObject> getChildrenWithoutPopulating() {
        return Collections.singletonList(physicalProperties);
    }

    @Override
    public String getShortDisplayName() {
        return getName();
    }

    @Override
    protected void populateImpl() throws SQLObjectException {
		// SQLTable's populate implementation includes column population code
		// that takes care of a lot of this already
	}

    @Override
    protected boolean removeChildImpl(SPObject child) {
        return false;
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
        return physicalProperties.getCheckConstraint();
    }

    public SQLTypeConstraint getConstraintType(String platform) {
        return physicalProperties.getConstraintType();
    }

    public String getDefaultValue(String platform) {
        return physicalProperties.getDefaultValue();
    }

    public String[] getEnumeration(String platform) {
        return physicalProperties.getEnumeration();
    }

    public int getPrecision(String platform) {
        return physicalProperties.getPrecision();
    }

    public int getScale(String platform) {
        return physicalProperties.getScale();
    }

    public void setCheckConstraint(String platform, String checkConstraint) {
        physicalProperties.setCheckConstraint(checkConstraint);
    }

    public void setConstraintType(String platform, SQLTypeConstraint constraint) {
        physicalProperties.setConstraintType(constraint);
    }

    public void setDefaultValue(String platform, String defaultValue) {
        physicalProperties.setDefaultValue(defaultValue);
    }

    public void setEnumeration(String platform, String[] enumeration) {
        physicalProperties.setEnumeration(enumeration);
    }

    public void setPrecision(String platform, int precision) {
        physicalProperties.setPrecision(precision);
    }

    public void setScale(String platform, int scale) {
        physicalProperties.setScale(scale);
    }

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public PropertyType getPrecisionType(String platform) {
		return PropertyType.VARIABLE;
	}

	public PropertyType getScaleType(String platform) {
		return PropertyType.VARIABLE;
	}

	public void setPrecisionType(String platform, PropertyType precisionType) {
		// Base type does not support changing Precision Type
	}

	public void setScaleType(String platform, PropertyType precisionType) {
		// Base type does not support changing Precision Type
	}
}
