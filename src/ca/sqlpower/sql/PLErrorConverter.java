package ca.sqlpower.sql;

import java.sql.SQLException;

/**
 * Just returns BAD_PL_SCHEMA.
 */
public class PLErrorConverter extends AbstractErrorConverter {

	/**
	 * Just returns BAD_PL_SCHEMA.
	 */
	public int convert(SQLException e) {
		return BAD_PL_SCHEMA;
	}
}

