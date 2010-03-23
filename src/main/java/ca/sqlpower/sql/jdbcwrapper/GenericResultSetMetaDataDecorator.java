package ca.sqlpower.sql.jdbcwrapper;

import java.sql.ResultSetMetaData;

/**
 * A non-platform-specific ResultSetMetaData decorator which does not
 * fiddle with the values returned by the underlying driver.
 */
public class GenericResultSetMetaDataDecorator extends ResultSetMetaDataDecorator {

	public GenericResultSetMetaDataDecorator(
			ResultSetMetaData rsmd) {
		super(rsmd);
	}

}
