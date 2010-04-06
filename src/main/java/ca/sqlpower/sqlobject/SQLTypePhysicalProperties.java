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

import java.util.Collections;
import java.util.List;

import javax.sql.RowSetMetaData;

import ca.sqlpower.object.SPObject;
import ca.sqlpower.sql.JDBCDataSourceType;
import ca.sqlpower.sqlobject.SQLTypePhysicalPropertiesProvider.PropertyType;

/**
 * A container class that stores several specific database platform-dependent
 * physical properties for a {@link JDBCSQLType} for a single specific physical platform
 */
public class SQLTypePhysicalProperties extends SQLObject {
    
    /**
     * {@link SQLTypePhysicalProperties} supports check constraints, and
     * enumeration constraints, but not both
     */
    public enum SQLTypeConstraint {
		/**
		 * This SQLType has neither check nor enumeration constraints on this
		 * platform
		 */
        NONE,

        /**
         * This SQLType has a check constraint for this platform
         */
        CHECK,

        /**
         * This SQLType has an enumeration constraint for this platform
         */
        ENUM
    }

    /**
     * The physical precision property for this type. {@link RowSetMetaData}
     * {@link #setPrecision(int)} defines it as 'the total number of decimal
     * digits'.
     */
    private Integer precision;
    
    /**
     * The physical scale property for this type. {@link RowSetMetaData}
     * {@link #setScale(int)} defines it as 'the number of digits to right of
     * decimal point'.
     */
    private Integer scale;
    
    /**
     * A String representation of the Default value for the physical type. Note
     * that the proper String representation will be dependent on the type and
     * physical platform.
     */
    private String defaultValue;

    /**
     * Indicates which constraint type applies to this physical type.
     */
    private SQLTypeConstraint constraintType;
    
    /**
     * Check constraint on this SQLType. Note that you cannot use both this AND
     * the checkConstraint
     */
    private String checkConstraint;

    /**
     * Enumeration constraint. It is a list Strings representing values that the
     * type is constrained to. Note that you cannot use both this AND the
     * checkConstraint.
     */
    private List<String> enumeration;

    /**
     * The logical name of the physical database platform.
     * 
     * This would be the same value that you would get if you called
     * {@link JDBCDataSourceType#getName()} on the {@link JDBCDataSourceType}
     * representing the physical platform of this type.
     */
    private String platform = "";
    
    private PropertyType precisionType;
    
    private PropertyType scaleType;
    
    public SQLTypePhysicalProperties() {
	}
    
    public SQLTypePhysicalProperties(String platformName) {
        this.platform = platformName;
    }
    
	/**
	 * Returns the precision property as an Integer.
	 * 
	 * @return An Integer instance representing the precision property value. If it
	 *         returns null, then the precision has not been set for this
	 *         {@link SQLTypePhysicalProperties} instance.
	 */
    public Integer getPrecision() {
        return precision;
    }

    public void setPrecision(int precision) {
        this.precision = precision;
    }

	/**
	 * Returns the scale property as an Integer.
	 * 
	 * @return An Integer instance representing the scale property value. If it
	 *         returns null, then the scale has not been set for this
	 *         {@link SQLTypePhysicalProperties} instance.
	 */
    public Integer getScale() {
        return scale;
    }

    public void setScale(int scale) {
        this.scale = scale;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public String getCheckConstraint() {
        return checkConstraint;
    }

    public void setCheckConstraint(String checkConstraint) {
        this.checkConstraint = checkConstraint;
    }

    public List<String> getEnumeration() {
        return enumeration;
    }

    public void setEnumeration(List<String> enumeration) {
        this.enumeration = enumeration;
    }

    public void setConstraintType(SQLTypeConstraint constraint) {
        this.constraintType = constraint;
    }

    public SQLTypeConstraint getConstraintType() {
        return constraintType;
    }

    @Override
    public boolean allowsChildren() {
        return false;
    }

    @Override
    public List<? extends SQLObject> getChildrenWithoutPopulating() {
        return Collections.emptyList();
    }

    @Override
    public String getShortDisplayName() {
        return getName();
    }

    @Override
    protected void populateImpl() throws SQLObjectException {
        // no children, so no-op
    }

    @Override
    protected boolean removeChildImpl(SPObject child) {
        // allowed child types is none, so removeChild should throw exception
        return false;
    }

    public int childPositionOffset(Class<? extends SPObject> childType) {
        return 0;
    }

    public List<Class<? extends SPObject>> getAllowedChildTypes() {
        return Collections.emptyList();
    }

    public List<? extends SPObject> getDependencies() {
        return Collections.emptyList();
    }

    public void removeDependency(SPObject dependency) {
        // No dependencies, so no-op
    }

    public String getPlatform() {
        return platform;
    }
    
    public PropertyType getPrecisionType() {
        return precisionType;
    }

    public PropertyType getScaleType() {
        return scaleType;
    }
    
    public void setPrecisionType(PropertyType precisionType) {
		this.precisionType = precisionType;
	}
    
    public void setScaleType(PropertyType scaleType) {
		this.scaleType = scaleType;
	}
}

