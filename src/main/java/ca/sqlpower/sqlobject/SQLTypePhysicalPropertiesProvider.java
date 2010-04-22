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

import javax.sql.RowSetMetaData;

import ca.sqlpower.object.SPObject;
import ca.sqlpower.object.annotation.Accessor;
import ca.sqlpower.object.annotation.Mutator;
import ca.sqlpower.sql.Olap4jDataSource.Type;
import ca.sqlpower.sqlobject.SQLTypePhysicalProperties.SQLTypeConstraint;

/**
 * Provides a set of physical properties for a SQL column type.
 */
public interface SQLTypePhysicalPropertiesProvider extends SPObject {
	
	public static final String GENERIC_PLATFORM = "GENERIC";
	
	/**
	 * A enumeration of platform-independent high-level basic SQL data types
	 */
	public enum BasicSQLType {
		/**
		 * Any text or character-string data, like VARCHAR, etc
		 */
		TEXT,

		/**
		 * Any numerical data, including integers and floating point numbers
		 */
		NUMBER,

		/**
		 * Any data representing a specific date and/or time
		 */
		DATETIME,

		/**
		 * Any data representing a boolean value (0 or 1, true or false)
		 */
		BOOLEAN,

		/**
		 * Any data that does not fit the above classifications (ex. binary
		 * data)
		 */
		OTHER;

		/**
		 * Takes an int that should represent a value from {@link Types}, and
		 * converts it to a higher level basic type as represented by
		 * {@link BasicSQLType}.
		 * 
		 * @param type
		 *            An int that corresponds with one of the constant values in
		 *            {@link Types}.
		 * @return A {@link BasicSQLType} that corresponds to the given
		 *         {@link Type} value
		 */
		public static BasicSQLType convertToBasicSQLType(int type) {
			switch (type) {
			case Types.BINARY:
			case Types.BIT:
			case Types.BOOLEAN:
				return BOOLEAN;

			case Types.DATE:
			case Types.TIME:
			case Types.TIMESTAMP:
				return DATETIME;

			case Types.BIGINT:
			case Types.DECIMAL:
			case Types.DOUBLE:
			case Types.FLOAT:
			case Types.INTEGER:
			case Types.NUMERIC:
			case Types.REAL:
			case Types.SMALLINT:
			case Types.TINYINT:
				return NUMBER;

			case Types.CHAR:
			case Types.LONGVARCHAR:
			case Types.VARCHAR:
				return TEXT;

			case Types.ARRAY:
			case Types.BLOB:
			case Types.CLOB:
			case Types.DATALINK:
			case Types.DISTINCT:
			case Types.JAVA_OBJECT:
			case Types.LONGVARBINARY:
			case Types.NULL:
			case Types.OTHER:
			case Types.REF:
			case Types.STRUCT:
			case Types.VARBINARY:
			default:
				return OTHER;

			}
		}
	}

	/**
	 * Primarily used to specify how a type's uses certain properties, in
	 * particular, scale and precisions, as not all types use them, and some may
	 * want to set them as constant values.
	 */
	public enum PropertyType {
		/**
		 * This type uses a constant value for this property and it cannot be
		 * changed by client code
		 */
		CONSTANT,

		/**
		 * The client using this type will set the value for this field
		 */
		VARIABLE,

		/**
		 * This type does not use this field
		 */
		NOT_APPLICABLE
	}

	/**
	 * Indicates how this type uses the precision property.
	 * {@link RowSetMetaData} {@link #setPrecision(int)} defines it as 'the
	 * total number of decimal digits'.
	 * 
	 * @return <ul>
	 *         <li>If it returns {@value PropertyType#CONSTANT}, then the value
	 *         returned by {@link #getPrecision()} is constant and should not be
	 *         changed. {@link #setPrecision(int)} will have no effect.</li>
	 *         <li>If it returns {@link PropertyType#VARIABLE}, then the value
	 *         is variable, so using {@link #setPrecision(int)} can change its
	 *         value</li>
	 *         <li>If it returns {@link PropertyType#NOT_APPLICABLE}, then the
	 *         precision is not used, and its value has no meaning for this
	 *         type, and anything that {@link #getPrecision()} returns should be
	 *         ignored.</li>
	 *         </ul>
	 */
	public PropertyType getPrecisionType(String platform);
	
	/**
	 * Sets how this . {@link RowSetMetaData}
	 * {@link #setPrecision(int)} defines it as 'the total number of decimal
	 * digits'. This also determines whether the value returned by
	 * {@link #getPrecision()} means anything.
	 * 
	 * @param usingPrecision
	 *            Set to true if this type uses precision. False if it does not.
	 */
	public void setPrecisionType(String platform, PropertyType precisionType);
	
	/**
	 * Get the precision physical property for this type. {@link RowSetMetaData}
	 * {@link #setPrecision(int)} defines it as 'the total number of decimal
	 * digits'.
	 * 
	 * @return The precision physical property for this type.
	 */
	public int getPrecision(String platform);

	/**
	 * Sets the precision physical property for this type.
	 * {@link RowSetMetaData} {@link #setPrecision(int)} defines it as 'the
	 * total number of decimal digits'.
	 * 
	 * @param precision
	 *            The new precision value
	 */
	public void setPrecision(String platform, int precision);

	/**
	 * Indicates how this type uses the scale property. {@link RowSetMetaData}
	 * {@link #setScale(int)} defines it as 'the number of digits to right of
	 * decimal point'.
	 * 
	 * @return <ul>
	 *         <li>If it returns {@value PropertyType#CONSTANT}, then the value
	 *         returned by {@link #getScale()} is constant and should not be
	 *         changed. {@link #setScale(int)} will have no effect.</li>
	 *         <li>If it returns {@link PropertyType#VARIABLE}, then the value
	 *         is variable, so using {@link #setScale(int)} can change its
	 *         value</li>
	 *         <li>If it returns {@link PropertyType#NOT_APPLICABLE}, then the
	 *         precision is not used, and its value has no meaning for this
	 *         type, and anything that {@link #getScale()} returns should be
	 *         ignored.</li>
	 *         </ul>
	 */
	public PropertyType getScaleType(String platform);
	
	/**
	 * Sets how this . {@link RowSetMetaData}
	 * {@link #setScale(int)} defines it as 'the number of digits to right of
	 * decimal point'. This also determines whether the value returned by
	 * {@link #getScale()} means anything.
	 * 
	 * @param usingPrecision
	 *            Set to true if this type uses precision. False if it does not.
	 */
	public void setScaleType(String platform, PropertyType scaleType);
	
	/**
	 * Get the precision physical property for this type. {@link RowSetMetaData}
	 * {@link #setScale(int)} defines it as 'the number of digits to right of
	 * decimal point'.
	 * 
	 * @return The scale physical property for this type.
	 */
	public int getScale(String platform);

	/**
	 * Sets the scale physical property for this type. {@link RowSetMetaData}
	 * {@link #setScale(int)} defines it as 'the number of digits to right of
	 * decimal point'.
	 * 
	 * @param precision
	 *            The new scale value
	 */
	public void setScale(String platform, int scale);

	/**
	 * Returns the default value of this type
	 * 
	 * @return The default type as a String
	 */
	public String getDefaultValue(String platform);

	/**
	 * Set the default value of this type
	 * 
	 * @param defaultValue
	 *            The new default value as a String
	 */
	public void setDefaultValue(String platform, String defaultValue);

	/**
	 * Gets the check constraint value of this type. The check constraint is
	 * only valid if {@link #getConstraint()} returns
	 * {@link SQLTypeConstraint#CHECK}
	 * 
	 * @return The check constraint as a String
	 */
	public String getCheckConstraint(String platform);

	/**
	 * Sets the check constraint value of this type. The check constraint is
	 * only valid if {@link #getConstraint()} returns
	 * {@link SQLTypeConstraint#CHECK}
	 * 
	 * @param checkConstraint
	 */
	public void setCheckConstraint(String platform, String checkConstraint);

	/**
	 * Gets the List ('enumeration') of allowed values of this type. The
	 * enumeration is only valid if {@link #getConstraint()} returns
	 * {@link SQLTypeConstraint#ENUM}
	 * 
	 * @return The enumeration constraint as a String
	 */
	public String[] getEnumeration(String platform);

	/**
	 * Gets the List ('enumeration') of allowed values of this type. The
	 * enumeration is only valid if {@link #getConstraint()} returns
	 * {@link SQLTypeConstraint#ENUM}
	 * 
	 * @return The enumeration constraint as a String
	 */
	public void setEnumeration(String platform, String[] enumeration);

	/**
	 * Sets the {@link SQLTypeConstraint} for this type.
	 * 
	 * @param constraint
	 *            The new {@link SQLTypeConstraint} value for this type.
	 */
	public void setConstraintType(String platform, SQLTypeConstraint constraint);

	/**
	 * Gets the {@link SQLTypeConstraint} value for this type.
	 * 
	 * @return the {@link SQLTypeConstraint} value for this type
	 */
	public SQLTypeConstraint getConstraintType(String platform);

	/**
	 * Gets the JDBC type of this SQLType as defined in {@link Types}
	 * 
	 * @return An int that corresponds to one of the type constants in
	 *         {@link Types}.
	 */
	public int getType();

	/**
	 * Sets the JDBC type of this SQLType as defined in {@link Types}
	 * 
	 * @param type
	 *            An int that corresponds to one of the type constants in
	 *            {@link Types}
	 */
	public void setType(int type);

	/**
	 * Sets the nullability of this type.
	 * 
	 * @param nullability
	 *            An int corresponding to the nullability behaviour, as
	 *            specified in {@link DatabaseMetaData}. These include:
	 *            <ul>
	 *            <li>{@link DatabaseMetaData#columnNoNulls}</li>
	 *            <li>{@link DatabaseMetaData#columnNullable}</li>
	 *            <li>{@link DatabaseMetaData#columnNullableUnknown}</li>
	 *            </ul>
	 */
	public void setNullability(int nullability);

	/**
	 * Specifies whether this type accepts NULL as a value, based on the values
	 * specified by {@link DatabaseMetaData}. These include:
	 * <ul>
	 * <li>{@link DatabaseMetaData#columnNoNulls}</li>
	 * <li>{@link DatabaseMetaData#columnNullable}</li>
	 * <li>{@link DatabaseMetaData#columnNullableUnknown}</li>
	 * </ul>
	 * 
	 * @return An int corresponding to one of the nullability values specified
	 *         in DatabaseMetaData
	 */
	public int getNullability();

	/**
	 * Set whether or not this type defaults to an automatically-incrementing
	 * sequence of values
	 * 
	 * @param autoIncrement
	 *            If set to true, then this type's value defaults to an
	 *            automatically-incrementing sequence of values. If set to
	 *            false, then it does not.
	 */
	public void setAutoIncrement(boolean autoIncrement);
    
	/**
	 * This property indicates that values stored in this column should default
	 * to some automatically-incrementing sequence of values.
	 * 
	 * @return A boolean indicating whether this type defaults to an
	 *         incrementing sequence of values.
	 */
	public boolean isAutoIncrement();
}
