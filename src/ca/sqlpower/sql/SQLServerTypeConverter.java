package ca.sqlpower.sql;

import java.sql.Types;

/**
 * This class helps with SQL syntax when creating and altering columns
 * in SQLServer.
 *
 * @author Jonathan Fuerth
 * @version $Id$
 */
public class SQLServerTypeConverter extends SqlTypeConverter {

	/**
	 * Overrides the superclass's convertType method where required by
	 * SqlServer syntax.
	 */
	public String convertType(int sqlType, int precision, int scale) {
		switch (sqlType) {

		case Types.SMALLINT:
			return "SMALLINT";

		case Types.INTEGER:
			return "INTEGER";

		case Types.FLOAT:
			return "REAL";

		case Types.DOUBLE:
		case Types.REAL:
			return "DOUBLE";

		case Types.NUMERIC:
			if (precision > 0 && scale > 0) {
				return "NUMERIC("+precision+","+scale+")";
			} else if (precision > 0) {
				return "NUMERIC("+precision+")";
			} else {
				return "NUMERIC";
			}

		case Types.TIMESTAMP:
			return "DATETIME";

		default:
			return super.convertType(sqlType, precision, scale);
		}
	}
}
