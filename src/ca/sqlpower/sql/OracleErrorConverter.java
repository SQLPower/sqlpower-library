package ca.sqlpower.sql;

import java.sql.SQLException;

/**
 * Converts SQLExceptions from Oracle into SQLPower error numbers.
 */
public class OracleErrorConverter extends AbstractErrorConverter {

	/**
	 * @see ca.sqlpower.sql.AbstractErrorConverter#convert(SQLException)
	 */
	public int convert(SQLException e) {
		switch (e.getErrorCode()) {
		case 904: 
			return UNKNOWN_COLUMN;
		case 942:
			return UNKNOWN_COLUMN;
		case 1017 :
			return INVALID_LOGON;
		case 17443 :
			return INVALID_LOGON;
		case 17002 :
			return SERVER_UNAVAILABLE;
		default :
			return UNKNOWN_ERROR;
		}
	}
}

