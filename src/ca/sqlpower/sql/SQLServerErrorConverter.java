package ca.sqlpower.sql;

import java.sql.SQLException;

/**
 * Converts SQLExceptions from SQL Server into SQLPower error numbers.
 */
public class SQLServerErrorConverter extends AbstractErrorConverter {

public int convert(SQLException e) {
		switch (e.getErrorCode()) {
			default :
				return UNKNOWN_ERROR;
		}
	}
}

