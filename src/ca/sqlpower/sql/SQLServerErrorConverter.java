package ca.sqlpower.sql;

import java.sql.SQLException;

/**
 * Converts SQLExceptions from SQL Server into SQLPower error numbers.
 */
public class SQLServerErrorConverter extends AbstractErrorConverter {
	
	public int convert(SQLException e) {
		switch (e.getErrorCode()) {
		case 0:
			// this might be a problem: we get error code "0" for
			// "error establishing the socket," which is likely not
			// unique.
			return SERVER_UNAVAILABLE;

		case 207:
			return UNKNOWN_COLUMN;

		case 18456:
		case 4060:
		case 18452:
			return INVALID_LOGON;

		default:
			return UNKNOWN_ERROR;
		}
	}
}

