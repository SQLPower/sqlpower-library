package ca.sqlpower.sql;

import java.sql.*;


/**
 * Selects the appropriate error converter based on database type.
 */

public class ErrorConverterFactory {

	private static PLErrorConverter plErrorConverter = new PLErrorConverter();
	private static OracleErrorConverter oracleErrorConverter = new OracleErrorConverter();
	private static SQLServerErrorConverter sqlServerErrorConverter = new SQLServerErrorConverter();

	/**
	 * Returns a reference to a error converter object based on the
	 * databaseProductName attribute of the connection specified.
	 * 
	 * @throws IllegalArgumentException if the database type is unrecognized.
	 */
	public static AbstractErrorConverter getInstance(SQLException e) {
		String message = e.getMessage();

		if (message.indexOf("PLSchemaException") >= 0) {
			return plErrorConverter;
		} else if (message.indexOf("icrosoft") >= 0) {
			return sqlServerErrorConverter;
		} else if (message.startsWith("ORA")
				   || message.indexOf("THIN") >= 0
				   || message.indexOf("Io exception: The Network Adapter") >= 0) {
			return oracleErrorConverter;
		}
		throw new IllegalArgumentException("unrecognized database type for message: "+message);
	}

}

