package ca.sqlpower.sql;

import java.sql.*;

/**
 * Handles the conversion of database-specific error numbers within 
 * SQLExceptions to custom SQLPower error numbers.
 */
public abstract class AbstractErrorConverter {

	/**
	 * Used if database connection fails because of invalid
	 * username or password.
	 */
	public static final int INVALID_LOGON = 1;

	/**
	 * Used if a query fails due to a missing or unknown column name.
	 */
	public static final int UNKNOWN_COLUMN = 2;

	/**
	 * Used if the driver can't connect to the database server at all.
	 */
	public static final int SERVER_UNAVAILABLE = 3;

	/** 
	 * Used if an appropriate conversion for the database error
	 * cannot be found
	 */
	public static final int UNKNOWN_ERROR = -1;
	

	/**
	 * Converts a SQLException to a SQLPower error code.
	 */
	public abstract int convert(SQLException e);

}

