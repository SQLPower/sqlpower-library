package ca.sqlpower.sql;

import java.sql.SQLException;

import org.apache.log4j.Logger;

/**
 * Converts SQLExceptions from PostgreSQL into SQLPower error numbers.
 */
public class PostgreSQLErrorConverter extends AbstractErrorConverter {

	private final static Logger logger = Logger.getLogger(PostgreSQLErrorConverter.class);

	public int convert(SQLException e) {
		String state = e.getSQLState();

		logger.debug("SQL STATE = " + state);

		if ("28000".equals(state))
			return INVALID_LOGON;
		if ("42703".equals(state))
			return UNKNOWN_COLUMN;	// postgresql 8.x, proto version 3

		switch (e.getErrorCode()) {

		default:
			return UNKNOWN_ERROR;
		}
	}
}

