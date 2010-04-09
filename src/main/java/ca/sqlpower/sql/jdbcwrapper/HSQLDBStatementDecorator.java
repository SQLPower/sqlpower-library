package ca.sqlpower.sql.jdbcwrapper;

import java.sql.ResultSet;
import java.sql.Statement;

public class HSQLDBStatementDecorator extends StatementDecorator {
	
	protected HSQLDBStatementDecorator(
			ConnectionDecorator parentConnection,
			Statement statement) {
		super(parentConnection, statement);
	}

	@Override
	protected ResultSet makeResultSetDecorator(ResultSet rs) {
		if (rs == null) {
			return null;
		}
		return new HSQLDBResultSetDecorator(this, rs);
	}

}
