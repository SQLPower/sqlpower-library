package ca.sqlpower.sql.jdbcwrapper;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;

public class OracleResultSetMetaDataDecorator extends ResultSetMetaDataDecorator {

	public OracleResultSetMetaDataDecorator(
			ResultSetMetaData rsmd) {
		super(rsmd);
	}

	/**
	 * Ensures returned scale values are not negative by replacing
	 * negative scale values from the underlying driver with a 0.
	 */
	@Override
	public int getScale(int column) throws SQLException {
		int scale = super.getScale(column);
		if (scale < 0) {
			return 0;
		} else {
			return scale;
		}
	}
}
