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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.sql.RowSetMetaData;

import ca.sqlpower.object.SPObject;
import ca.sqlpower.object.annotation.Accessor;
import ca.sqlpower.object.annotation.Constructor;
import ca.sqlpower.object.annotation.ConstructorParameter;
import ca.sqlpower.object.annotation.ConstructorParameter.ParameterType;
import ca.sqlpower.object.annotation.Mutator;
import ca.sqlpower.object.annotation.NonProperty;
import ca.sqlpower.sql.JDBCDataSourceType;
import ca.sqlpower.sqlobject.SQLTypePhysicalPropertiesProvider.PropertyType;
import ca.sqlpower.util.SQLPowerUtils;

/**
 * A container class that stores several specific database platform-dependent
 * physical properties for a {@link JDBCSQLType} for a single specific physical platform
 */
public class SQLTypePhysicalProperties extends SQLObject implements SQLCheckConstraintContainer {
    
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
	 * Check constraints on this SQLType. Note that you cannot use both this AND
	 * the enumeration.
	 */
    private List<SQLCheckConstraint> checkConstraints = new ArrayList<SQLCheckConstraint>();

    /**
     * Enumeration constraint. It is a list Strings representing values that the
     * type is constrained to. Note that you cannot use both this AND the
     * checkConstraints.
     */
    private List<SQLEnumeration> enumerations = new ArrayList<SQLEnumeration>();

    /**
     * The logical name of the physical database platform.
     * 
     * This would be the same value that you would get if you called
     * {@link JDBCDataSourceType#getName()} on the {@link JDBCDataSourceType}
     * representing the physical platform of this type.
     */
    private String platform = "";
    
    /**
     * The {@link PropertyType} of the precision. It determines if it is meant to
	 * be variable ({@link PropertyType#VARIABLE}), have a constant value (
	 * {@link PropertyType#CONSTANT}), or it doesn't apply to this type (
	 * {@link PropertyType#NOT_APPLICABLE}).
     */
    private PropertyType precisionType;
    
	/**
	 * The {@link PropertyType} value for scale. It determines if it is meant to
	 * be variable ({@link PropertyType#VARIABLE}), have a constant value (
	 * {@link PropertyType#CONSTANT}), or it doesn't apply to this type (
	 * {@link PropertyType#NOT_APPLICABLE}).
	 */
    private PropertyType scaleType;
    
	/**
	 * List of allowed child types, which is empty since
	 * {@link SQLTypePhysicalProperties} has no children
	 */
	public static final List<Class<? extends SPObject>> allowedChildTypes = 
		Collections.unmodifiableList(new ArrayList<Class<? extends SPObject>>(
				Arrays.asList(SQLCheckConstraint.class, SQLEnumeration.class)));

    @Constructor
    public SQLTypePhysicalProperties(@ConstructorParameter(isProperty=ParameterType.PROPERTY, propertyName="platform") String platformName) {
    	this.platform = platformName;
    	setName("SQLTypePhysicalProperties for " + platform);
    }
    
    @Override
    protected void addChildImpl(SPObject child, int index) {
    	if (child instanceof SQLCheckConstraint) {
    		addCheckConstraint((SQLCheckConstraint) child, index);
    	} else if (child instanceof SQLEnumeration) {
    		addEnumeration((SQLEnumeration) child, index);
    	} else {
			throw new IllegalArgumentException("The child " + child.getName() + 
					" of type " + child.getClass() + " is not a valid child type of " + 
					getClass() + ".");
		}
    }
    
    public void addCheckConstraint(SQLCheckConstraint checkConstraint) {
    	addCheckConstraint(checkConstraint, getChildrenWithoutPopulating(SQLCheckConstraint.class).size());
    }
    
    public void addCheckConstraint(SQLCheckConstraint checkConstraint, int index) {
    	checkConstraints.add(index, checkConstraint);
    	checkConstraint.setParent(this);
    	fireChildAdded(SQLCheckConstraint.class, checkConstraint, index);
    }
    
    public void addEnumeration(SQLEnumeration child) {
    	addEnumeration(child, getChildrenWithoutPopulating(SQLEnumeration.class).size());
    }
    
    public void addEnumeration(SQLEnumeration child, int index) {
    	enumerations.add(index, child);
    	child.setParent(this);
    	fireChildAdded(SQLEnumeration.class, child, index);
    }
    
	/**
	 * Returns the precision property as an Integer.
	 * 
	 * @return An Integer instance representing the precision property value. If it
	 *         returns null, then the precision has not been set for this
	 *         {@link SQLTypePhysicalProperties} instance.
	 */
    @Accessor
    public Integer getPrecision() {
        return precision;
    }

    @Mutator
    public void setPrecision(Integer precision) {
    	begin("Setting precision.");
    	Integer oldValue = this.precision;
        this.precision = precision;
        firePropertyChange("precision", (Integer) oldValue, (Integer) precision);
        commit();
    }

	/**
	 * Returns the scale property as an Integer.
	 * 
	 * @return An Integer instance representing the scale property value. If it
	 *         returns null, then the scale has not been set for this
	 *         {@link SQLTypePhysicalProperties} instance.
	 */
    @Accessor
    public Integer getScale() {
        return scale;
    }

    @Mutator
    public void setScale(Integer scale) {
    	begin("Setting scale.");
    	Integer oldValue = this.scale;
        this.scale = scale;
        firePropertyChange("scale", (Integer) oldValue, (Integer) scale);
        commit();
    }

    @Accessor
    public String getDefaultValue() {
        return defaultValue;
    }

    @Mutator
    public void setDefaultValue(String defaultValue) {
    	begin("Setting default value.");
    	String oldValue = this.defaultValue;
        this.defaultValue = defaultValue;
        firePropertyChange("defaultValue", oldValue, defaultValue);
        commit();
    }

    @Accessor
    public SQLTypeConstraint getConstraintType() {
    	return constraintType;
    }

    @Mutator
    public void setConstraintType(SQLTypeConstraint constraint) {
    	begin("Setting constraint type.");
    	SQLTypeConstraint oldValue = this.constraintType;
        this.constraintType = constraint;
        firePropertyChange("constraintType", oldValue, constraintType);
        commit();
    }

    @Override
    public boolean allowsChildren() {
        return true;
    }

    @Override
    public List<? extends SQLObject> getChildrenWithoutPopulating() {
    	List<SQLObject> children = new ArrayList<SQLObject>();
    	children.addAll(checkConstraints);
    	children.addAll(enumerations);
    	return Collections.unmodifiableList(children);
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
    	if (child instanceof SQLCheckConstraint) {
    		return removeCheckConstraint((SQLCheckConstraint) child);
    	} else if (child instanceof SQLEnumeration) {
    		return removeEnumeration((SQLEnumeration) child);
    	}
    	return false;
    }
    
    public boolean removeCheckConstraint(SQLCheckConstraint child) {
    	int index = checkConstraints.indexOf(child);
    	if (index != -1) {
    		checkConstraints.remove(index);
    		fireChildRemoved(SQLCheckConstraint.class, child, index);
    		child.setParent(null);
    		return true;
    	}
    	return false;
    }
    
    public boolean removeEnumeration(SQLEnumeration child) {
    	int index = enumerations.indexOf(child);
    	if (index != -1) {
    		enumerations.remove(index);
    		fireChildRemoved(SQLEnumeration.class, child, index);
    		child.setParent(null);
    		return true;
    	}
    	return false;
    }

    public int childPositionOffset(Class<? extends SPObject> childType) {
    	int offset = 0;
    	
    	if (childType == SQLCheckConstraint.class) return offset;
        offset += checkConstraints.size();
        
        if (childType == SQLEnumeration.class) return offset;
		
		throw new IllegalArgumentException("The type " + childType + 
				" is not a valid child type of " + getName());
    }

    @NonProperty
    public List<Class<? extends SPObject>> getAllowedChildTypes() {
        return allowedChildTypes;
    }

    public List<? extends SPObject> getDependencies() {
        return Collections.emptyList();
    }

    public void removeDependency(SPObject dependency) {
        // No dependencies, so no-op
    }

    @Accessor
    public String getPlatform() {
        return platform;
    }
    
    @Accessor
    public PropertyType getPrecisionType() {
        return precisionType;
    }

    @Mutator
    public void setPrecisionType(PropertyType precisionType) {
    	begin("Setting precision type.");
    	PropertyType oldValue = this.precisionType;
    	this.precisionType = precisionType;
    	firePropertyChange("precisionType", oldValue, precisionType);
    	commit();
    }

    @Accessor
    public PropertyType getScaleType() {
        return scaleType;
    }
    
    @Mutator
    public void setScaleType(PropertyType scaleType) {
    	begin("Setting scale type.");
    	PropertyType oldValue = this.scaleType;
		this.scaleType = scaleType;
		firePropertyChange("scaleType", oldValue, scaleType);
		commit();
	}
    
	/**
	 * Updates the properties of this properties object to match the parameter.
	 * Does not change this properties platform or parent.
	 * 
	 * @param matchMe
	 *            the SQLTypePhysicalProperties to match
	 */
    @Override
    public void updateToMatch(SQLObject matchMe) {
    	if (!(matchMe instanceof SQLTypePhysicalProperties)) {
    		throw new ClassCastException();
    	}
    	
    	SQLTypePhysicalProperties propsToMatch = (SQLTypePhysicalProperties) matchMe;

    	if (!areEqual(this, propsToMatch)) {
    		begin("Matching properties");
    		setName(propsToMatch.getName());
    		setPhysicalName(propsToMatch.getPhysicalName());
    		setPrecision(propsToMatch.getPrecision());
    		setPrecisionType(propsToMatch.getPrecisionType());
    		setScale(propsToMatch.getScale());
    		setScaleType(propsToMatch.getScaleType());
    		setDefaultValue(propsToMatch.getDefaultValue());
    		setConstraintType(propsToMatch.getConstraintType());

    		List<SQLCheckConstraint> newConstraints = propsToMatch.getChildrenWithoutPopulating(SQLCheckConstraint.class);
    		for (int i = checkConstraints.size() - 1; i >= 0; i--) {
    			SQLCheckConstraint constraint = checkConstraints.get(i);
    			
    			boolean found = false;
    			for (SQLCheckConstraint newConstraint : newConstraints) {
    				if (SQLPowerUtils.areEqual(newConstraint.getName(), constraint.getName()) &&
    						SQLPowerUtils.areEqual(newConstraint.getConstraint(), constraint.getConstraint())) {
    					found = true;
    					break;
    				}
    			}
    			if (!found) {
    				removeCheckConstraint(constraint);
    			}
    		}
    		for (SQLCheckConstraint newConstraint : newConstraints) {
    			boolean found = false;
    			for (SQLCheckConstraint constraint : checkConstraints) {
    				if (SQLPowerUtils.areEqual(newConstraint.getName(), constraint.getName()) &&
    						SQLPowerUtils.areEqual(newConstraint.getConstraint(), constraint.getConstraint())) {
    					found = true;
    					break;
    				}
    			}
    			if (!found) {
    				addCheckConstraint(new SQLCheckConstraint(newConstraint));
    			}
    		}
    		
    		List<SQLEnumeration> newEnumerations = propsToMatch.getChildrenWithoutPopulating(SQLEnumeration.class);
    		for (int i = enumerations.size() - 1; i >= 0; i--) {
    			SQLEnumeration enumeration = enumerations.get(i);
    			
    			boolean found = false;
    			for (SQLEnumeration newEnum : newEnumerations) {
    				if (SQLPowerUtils.areEqual(enumeration.getName(), newEnum.getName())) {
    					found = true;
    					break;
    				}
    			}
    			if (!found) {
    				removeEnumeration(enumeration);
    			}
    		}
    		for (SQLEnumeration newEnum : newEnumerations) {
    			boolean found = false;
    			for (SQLEnumeration enumeration : enumerations) {
    				if (SQLPowerUtils.areEqual(enumeration.getName(), newEnum.getName())) {
    					found = true;
    					break;
    				}
    			}
    			if (!found) {
    				addEnumeration(new SQLEnumeration(newEnum));
    			}
    		}

    		commit();
    	}
    }

	/**
	 * Compares two {@link SQLTypePhysicalProperties} objects to see if they are
	 * equal in all properties (except UUID) and children (
	 * {@link SQLCheckConstraint}s).
	 * 
	 * @param prop1
	 *            The first of two {@link SQLTypePhysicalProperties} objects to
	 *            compare.
	 * @param prop2
	 *            The second of two {@link SQLTypePhysicalProperties} objects to
	 *            compare.
	 * @return true iff the two {@link SQLTypePhysicalProperties} objects are
	 *         equal.
	 */
    public static boolean areEqual(SQLTypePhysicalProperties prop1, SQLTypePhysicalProperties prop2) {
		boolean equal = SQLPowerUtils.areEqual(prop1.getName(), prop2.getName())
				&& SQLPowerUtils.areEqual(prop1.getPhysicalName(), prop2.getPhysicalName())
				&& SQLPowerUtils.areEqual(prop1.getPrecision(), prop2.getPrecision())
				&& SQLPowerUtils.areEqual(prop1.getPrecisionType(), prop2.getPrecisionType())
				&& SQLPowerUtils.areEqual(prop1.getScale(), prop2.getScale())
				&& SQLPowerUtils.areEqual(prop1.getScaleType(), prop2.getScaleType())
				&& SQLPowerUtils.areEqual(prop1.getDefaultValue(), prop2.getDefaultValue())
				&& SQLPowerUtils.areEqual(prop1.getConstraintType(), prop2.getConstraintType());
		
		if (equal) {
			List<SQLCheckConstraint> checkConstraints1 = prop1.getChildrenWithoutPopulating(SQLCheckConstraint.class);
			List<SQLCheckConstraint> checkConstraints2 = prop2.getChildrenWithoutPopulating(SQLCheckConstraint.class);
			List<SQLEnumeration> enumerations1 = prop1.getChildrenWithoutPopulating(SQLEnumeration.class);
			List<SQLEnumeration> enumerations2 = prop2.getChildrenWithoutPopulating(SQLEnumeration.class);
			
			equal &= checkConstraints1.size() == checkConstraints2.size();
			equal &= enumerations1.size() == enumerations2.size();

			for (int i = 0; i < checkConstraints1.size() && equal; i++) {
				SQLCheckConstraint constraint1 = checkConstraints1.get(i);
				SQLCheckConstraint constraint2 = checkConstraints2.get(i);
				equal &= SQLPowerUtils.areEqual(constraint1.getName(), constraint2.getName());
				equal &= SQLPowerUtils.areEqual(constraint1.getConstraint(), constraint2.getConstraint());
			}
			
			for (int i = 0; i < enumerations1.size() && equal; i++) {
				SQLEnumeration enum1 = enumerations1.get(i);
				SQLEnumeration enum2 = enumerations2.get(i);
				equal &= SQLPowerUtils.areEqual(enum1.getName(), enum2.getName());
			}
		}
		return equal;
    }

    @NonProperty
	public List<SQLCheckConstraint> getCheckConstraints() {
		return getChildrenWithoutPopulating(SQLCheckConstraint.class);
	}

}

