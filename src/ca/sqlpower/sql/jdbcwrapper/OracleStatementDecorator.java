package ca.sqlpower.sql.jdbcwrapper;

import java.sql.ResultSet;
import java.sql.Statement;

public class OracleStatementDecorator extends StatementDecorator {
	
	protected OracleStatementDecorator(
			ConnectionDecorator parentConnection,
			Statement statement) {
		super(parentConnection, statement);
	}

	@Override
	protected ResultSet makeResultSetDecorator(ResultSet rs) {
		return new OracleResultSetDecorator(this, rs);
	}

}
