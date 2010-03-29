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
import java.util.List;

import javax.sql.RowSetMetaData;

import ca.sqlpower.object.SPObject;
import ca.sqlpower.sql.Olap4jDataSource.Type;
import ca.sqlpower.sqlobject.SQLTypePhysicalProperties.SQLTypeConstraint;

/**
 * Provides a set of physical properties for a SQL column type.
 */
public interface SQLTypePhysicalPropertiesProvider extends SPObject {
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
	 * Get the precision physical property for this type. {@link RowSetMetaData}
	 * {@link #setPrecision(int)} defines it as 'the total number of decimal
	 * digits'.
	 * 
	 * @return The precision physical property for this type.
	 */
	public int getPrecision();

	/**
	 * Sets the precision physical property for this type.
	 * {@link RowSetMetaData} {@link #setPrecision(int)} defines it as 'the
	 * total number of decimal digits'.
	 * 
	 * @param precision
	 *            The new precision value
	 */
	public void setPrecision(int precision);

	/**
	 * Get the precision physical property for this type. {@link RowSetMetaData}
	 * {@link #setScale(int)} defines it as 'the number of digits to right of
	 * decimal point'.
	 * 
	 * @return The scale physical property for this type.
	 */
	public int getScale();

	/**
	 * Sets the scale physical property for this type. {@link RowSetMetaData}
	 * {@link #setScale(int)} defines it as 'the number of digits to right of
	 * decimal point'.
	 * 
	 * @param precision
	 *            The new scale value
	 */
	public void setScale(int scale);

	/**
	 * Returns the default value of this type
	 * 
	 * @return The default type as a String
	 */
	public String getDefaultValue();

	/**
	 * Set the default value of this type
	 * 
	 * @param defaultValue
	 *            The new default value as a String
	 */
	public void setDefaultValue(String defaultValue);

	/**
	 * Gets the check constraint value of this type. The check constraint is
	 * only valid if {@link #getConstraint()} returns
	 * {@link SQLTypeConstraint#CHECK}
	 * 
	 * @return The check constraint as a String
	 */
	public String getCheckConstraint();

	/**
	 * Sets the check constraint value of this type. The check constraint is
	 * only valid if {@link #getConstraint()} returns
	 * {@link SQLTypeConstraint#CHECK}
	 * 
	 * @param checkConstraint
	 */
	public void setCheckConstraint(String checkConstraint);

	/**
	 * Gets the List ('enumeration') of allowed values of this type. The
	 * enumeration is only valid if {@link #getConstraint()} returns
	 * {@link SQLTypeConstraint#ENUM}
	 * 
	 * @return The enumeration constraint as a String
	 */
	public List<String> getEnumeration();

	/**
	 * Gets the List ('enumeration') of allowed values of this type. The
	 * enumeration is only valid if {@link #getConstraint()} returns
	 * {@link SQLTypeConstraint#ENUM}
	 * 
	 * @return The enumeration constraint as a String
	 */
	public void setEnumeration(List<String> enumeration);

	/**
	 * Sets the {@link SQLTypeConstraint} for this type.
	 * 
	 * @param constraint
	 *            The new {@link SQLTypeConstraint} value for this type.
	 */
	public void setConstraint(SQLTypeConstraint constraint);

	/**
	 * Gets the {@link SQLTypeConstraint} value for this type.
	 * 
	 * @return the {@link SQLTypeConstraint} value for this type
	 */
	public SQLTypeConstraint getConstraint();

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
}
