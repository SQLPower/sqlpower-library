package ca.sqlpower.sql;

import java.sql.SQLException;

/**
 * Converts SQLExceptions from PostgreSQL into SQLPower error numbers.
 */
public class PostgreSQLErrorConverter extends AbstractErrorConverter {
	
	public int convert(SQLException e) {
		switch (e.getErrorCode()) {

		default:
			return UNKNOWN_ERROR;
		}
	}
}

