package ca.sqlpower.sql;

import java.sql.*;

/**
 * A class that converts JDBC result set metadata sql types into names
 * that can be used to create and alter columns in a specific
 * database.
 *
 * @author Jonathan Fuerth
 * @version $Id$
 */
public abstract class SqlTypeConverter {
	public static SqlTypeConverter getInstance(Connection con) {
		if (con.getClass().getName().indexOf("postgresql") >= 0) {
			return new PostgreSQLTypeConverter();
		} else {
			throw new UnsupportedOperationException("Unknown database type for driver "
													+con.getClass().getName());
		}
	}

	/**
	 * A generic type converter.  May not work with any database at
	 * all, but provides (what I hope is) a maximally standard set of
	 * defaults.  This way, subclasses will be kept as small as
	 * possible.
	 */
	public String convertType(int sqlType, int precision, int scale) {
		switch (sqlType) {
			
		case Types.CHAR:
			return "CHARACTER("+precision+")";
			
		case Types.DATE:
			return "DATE";

		case Types.DECIMAL:
		case Types.INTEGER:
		case Types.SMALLINT:
		case Types.TINYINT:
			return "DECIMAL("+precision+")";

		case Types.DOUBLE:
		case Types.FLOAT:
		case Types.NUMERIC:
		case Types.REAL:
			return "NUMBER("+precision+","+scale+")";

		case Types.TIME:
			return "TIME";

		case Types.TIMESTAMP:
			return "TIMESTAMP";

		case Types.VARCHAR:
			return "VARCHAR("+precision+")";

			// we don't try to provide generic defaults for these types
		case Types.ARRAY:
		case Types.BIGINT:
		case Types.BINARY:
		case Types.BIT:
		case Types.BLOB:
			//case Types.BOOLEAN: JDBC 3
		case Types.CLOB:
			//case Types.DATALINK: JDBC 3
		case Types.DISTINCT:
		case Types.JAVA_OBJECT:
		case Types.LONGVARBINARY:
		case Types.LONGVARCHAR:
		case Types.NULL:
		case Types.OTHER:
		case Types.REF:
		case Types.STRUCT:
		case Types.VARBINARY:
		default:
			throw new UnsupportedOperationException
				("SQL Type "+sqlType+" is not implemented for this database.");
		}
	}
}
