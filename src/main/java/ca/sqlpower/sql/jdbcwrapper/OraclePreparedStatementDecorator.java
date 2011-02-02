package ca.sqlpower.sql.jdbcwrapper;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;

public class OraclePreparedStatementDecorator extends
		PreparedStatementDecorator implements PreparedStatement {

	public OraclePreparedStatementDecorator(
			ConnectionDecorator parentConnection, PreparedStatement ps) {
		super(parentConnection, ps);
	}

	@Override
	protected ResultSet makeResultSetDecorator(ResultSet rs) {
		return new OracleResultSetDecorator(this, rs);
	}

	@Override
	protected ResultSetMetaData makeResultSetMetaDataDecorator(
			ResultSetMetaData rsmd) {
		return new OracleResultSetMetaDataDecorator(rsmd);
	}

	/**
	 * Oracle doesn't recognize the Boolean type, so translate it to
	 * something it does, Bit.
	 * 
	 */
	@Override
	public void setNull(int parameterIndex, int sqlType) throws SQLException {
		int type = sqlType;
		
		if (sqlType == Types.BOOLEAN) {
			type = Types.BIT;
		}
		super.setNull(parameterIndex, type);
	}

	/**
	 * Oracle doesn't recognize the Boolean type, so translate it to
	 * something it does, Bit.
	 * 
	 */
	@Override
	public void setNull(int paramIndex, int sqlType, String typeName)	throws SQLException {
		int type = sqlType;
		
		if (sqlType == Types.BOOLEAN) {
			type = Types.BIT;
		}
		
	    super.setNull(paramIndex, type, typeName);
	}
	
	
	

}
