package ca.sqlpower.sql;

import java.sql.*;


/**
 * Selects the appropriate error converter based on database type.
 */

public class ErrorConverterFactory {

	private static OracleErrorConverter oracleErrorConverter = new OracleErrorConverter();
	private static SQLServerErrorConverter sqlServerErrorConverter = new SQLServerErrorConverter();

	/**
	 * Returns a reference to a error converter object based on the
	 * databaseProductName attribute of the connection specified.
	 * 
	 * @throws IllegalArgumentException if the database type is unrecognized.
	 */
	public static AbstractErrorConverter getInstance(SQLException e) {
		String driverClass = e.getClass().getName();

		if (driverClass.startsWith("com.microsoft.jdbc.sqlserver")) {
			return sqlServerErrorConverter;
		} 
		else if (driverClass.startsWith("com.merant.datadirect.jdbc.sqlserver")) {
			return sqlServerErrorConverter;
		} 
		else if (driverClass.startsWith("oracle")) {
			return oracleErrorConverter;
		}
		throw new IllegalArgumentException("unrecognized database type: "+driverClass);
	}

}

