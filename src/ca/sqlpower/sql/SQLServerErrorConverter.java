package ca.sqlpower.sql;

import java.sql.SQLException;

/**
 * Converts SQLExceptions from SQL Server into SQLPower error numbers.
 */
public class SQLServerErrorConverter extends AbstractErrorConverter {
	
	public int convert(SQLException e) {
		switch (e.getErrorCode()) {
		case 207:
			return UNKNOWN_COLUMN;
		case 18456:
			return INVALID_LOGON;
		default:
			return UNKNOWN_ERROR;
		}
	}
}

