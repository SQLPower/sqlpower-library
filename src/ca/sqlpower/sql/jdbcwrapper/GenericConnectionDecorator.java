package ca.sqlpower.sql.jdbcwrapper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;

/**
 * A non-platform-specific connection decorator which does not
 * fiddle with the values returned by the underlying driver.
 * When asked by the superclass to create prepared statement
 * and statement decorators, it creates the generic variants
 * of those decorators.
 */
public class GenericConnectionDecorator extends ConnectionDecorator {

	protected GenericConnectionDecorator(Connection delegate) {
		super(delegate);
	}

	@Override
	protected PreparedStatement makePreparedStatementDecorator(
			PreparedStatement pstmt) {
		return new GenericPreparedStatementDecorator(this, pstmt);
	}

	@Override
	protected Statement makeStatementDecorator(Statement stmt) {
		return new GenericStatementDecorator(this, stmt);
	}

}
