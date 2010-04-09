package ca.sqlpower.sql.jdbcwrapper;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;

public class HSQLDBPreparedStatementDecorator extends
		PreparedStatementDecorator implements PreparedStatement {

	public HSQLDBPreparedStatementDecorator(
			ConnectionDecorator parentConnection, PreparedStatement ps) {
		super(parentConnection, ps);
	}

	@Override
	protected ResultSet makeResultSetDecorator(ResultSet rs) {
		return new HSQLDBResultSetDecorator(this, rs);
	}

	@Override
	protected ResultSetMetaData makeResultSetMetaDataDecorator(
			ResultSetMetaData rsmd) {
		return new HSQLDBResultSetMetaDataDecorator(rsmd);
	}

}
