package ca.sqlpower.sql.jdbcwrapper;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;

/**
 * A non-platform-specific PreparedStatement decorator which does not fiddle
 * with the values returned by the underlying driver. When asked by the
 * superclass to create result set and result set meta data decorators, it
 * creates the generic variants of those decorators.
 */
public class GenericPreparedStatementDecorator extends
		PreparedStatementDecorator {

	public GenericPreparedStatementDecorator(
			ConnectionDecorator parentConnection, PreparedStatement ps) {
		super(parentConnection, ps);
	}

	@Override
	protected ResultSet makeResultSetDecorator(ResultSet rs) {
		return new GenericResultSetDecorator(this, rs);
	}

	@Override
	protected ResultSetMetaData makeResultSetMetaDataDecorator(
			ResultSetMetaData rsmd) {
		return new GenericResultSetMetaDataDecorator(rsmd);
	}

}
