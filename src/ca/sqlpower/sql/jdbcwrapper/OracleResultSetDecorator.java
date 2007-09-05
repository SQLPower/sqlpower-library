package ca.sqlpower.sql.jdbcwrapper;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;

public class OracleResultSetDecorator extends ResultSetDecorator {

	public OracleResultSetDecorator(
			Statement parentStatement,
			ResultSet rs) {
		super(parentStatement, rs);
	}

	@Override
	protected ResultSetMetaData makeResultSetMetaDataDecorator(
			ResultSetMetaData rsmd) {
		return new OracleResultSetMetaDataDecorator(rsmd);
	}

}
