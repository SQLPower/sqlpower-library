package ca.sqlpower.sql.jdbcwrapper;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;

/**
 * A non-platform-specific ResultSet decorator which does not fiddle with the
 * values returned by the underlying driver. When asked by the superclass to
 * create result set meta data decorators, it creates the generic version of
 * that decorators.
 */
public class GenericResultSetDecorator extends ResultSetDecorator {

	public GenericResultSetDecorator(Statement parentStatement, ResultSet rs) {
		super(parentStatement, rs);
	}

	@Override
	protected ResultSetMetaData makeResultSetMetaDataDecorator(
			ResultSetMetaData rsmd) {
		return new GenericResultSetMetaDataDecorator(rsmd);
	}

}
