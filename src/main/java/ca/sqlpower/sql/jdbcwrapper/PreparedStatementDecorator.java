package ca.sqlpower.sql.jdbcwrapper;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.Date;
import java.sql.NClob;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;

/**
 * A class that passes through all PreparedStatement method calls to
 * an inner PreparedStatement object which does all the work.  This
 * class is designed to be subclassed by database-specific
 * decorators which can intercept certain method calls and
 * tweak return values.
 */
public abstract class PreparedStatementDecorator implements PreparedStatement{

	public void setAsciiStream(int arg0, InputStream arg1, long arg2)
			throws SQLException {
		throw new UnsupportedOperationException("Currently it is only possible to wrap JDBC 3.");
	}

	public void setAsciiStream(int arg0, InputStream arg1) throws SQLException {
		throw new UnsupportedOperationException("Currently it is only possible to wrap JDBC 3.");
	}

	public void setBinaryStream(int arg0, InputStream arg1, long arg2)
			throws SQLException {
		throw new UnsupportedOperationException("Currently it is only possible to wrap JDBC 3.");
	}

	public void setBinaryStream(int arg0, InputStream arg1) throws SQLException {
		throw new UnsupportedOperationException("Currently it is only possible to wrap JDBC 3.");
	}

	public void setBlob(int arg0, InputStream arg1, long arg2)
			throws SQLException {
		throw new UnsupportedOperationException("Currently it is only possible to wrap JDBC 3.");
	}

	public void setBlob(int arg0, InputStream arg1) throws SQLException {
		throw new UnsupportedOperationException("Currently it is only possible to wrap JDBC 3.");
	}

	public void setCharacterStream(int arg0, Reader arg1, long arg2)
			throws SQLException {
		throw new UnsupportedOperationException("Currently it is only possible to wrap JDBC 3.");
	}

	public void setCharacterStream(int arg0, Reader arg1) throws SQLException {
		throw new UnsupportedOperationException("Currently it is only possible to wrap JDBC 3.");
	}

	public void setClob(int arg0, Reader arg1, long arg2) throws SQLException {
		throw new UnsupportedOperationException("Currently it is only possible to wrap JDBC 3.");
	}

	public void setClob(int arg0, Reader arg1) throws SQLException {
		throw new UnsupportedOperationException("Currently it is only possible to wrap JDBC 3.");
	}

	public void setNCharacterStream(int arg0, Reader arg1, long arg2)
			throws SQLException {
		throw new UnsupportedOperationException("Currently it is only possible to wrap JDBC 3.");
	}

	public void setNCharacterStream(int arg0, Reader arg1) throws SQLException {
		throw new UnsupportedOperationException("Currently it is only possible to wrap JDBC 3.");
	}

	public void setNClob(int arg0, NClob arg1) throws SQLException {
		throw new UnsupportedOperationException("Currently it is only possible to wrap JDBC 3.");
	}

	public void setNClob(int arg0, Reader arg1, long arg2) throws SQLException {
		throw new UnsupportedOperationException("Currently it is only possible to wrap JDBC 3.");
	}

	public void setNClob(int arg0, Reader arg1) throws SQLException {
		throw new UnsupportedOperationException("Currently it is only possible to wrap JDBC 3.");
	}

	public void setNString(int arg0, String arg1) throws SQLException {
		throw new UnsupportedOperationException("Currently it is only possible to wrap JDBC 3.");
	}

	public void setRowId(int arg0, RowId arg1) throws SQLException {
		throw new UnsupportedOperationException("Currently it is only possible to wrap JDBC 3.");
	}

	public void setSQLXML(int arg0, SQLXML arg1) throws SQLException {
		throw new UnsupportedOperationException("Currently it is only possible to wrap JDBC 3.");
	}

	public boolean isClosed() throws SQLException {
		throw new UnsupportedOperationException("Currently it is only possible to wrap JDBC 3.");
	}

	public boolean isPoolable() throws SQLException {
		throw new UnsupportedOperationException("Currently it is only possible to wrap JDBC 3.");
	}

	public void setPoolable(boolean poolable) throws SQLException {
		throw new UnsupportedOperationException("Currently it is only possible to wrap JDBC 3.");
	}

	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		throw new UnsupportedOperationException("Currently it is only possible to wrap JDBC 3.");
	}

	public <T> T unwrap(Class<T> iface) throws SQLException {
		throw new UnsupportedOperationException("Currently it is only possible to wrap JDBC 3.");
	}

	/**
	 * The (decorated) connection that created this prepared statement.
	 */
	private final ConnectionDecorator parentConnection;

	/**
	 * The actual prepared statement that does all the work.
	 */
	private final PreparedStatement preparedStatement;
	
	/**
	 * Creates a new prepared statement decorator for the given result set.
	 */
	public PreparedStatementDecorator(ConnectionDecorator parentConnection, PreparedStatement ps) {
		if (ps == null) throw new NullPointerException("Null prepared statement not allowed");
		this.parentConnection = parentConnection;
		this.preparedStatement = ps;
	}

	protected abstract ResultSet makeResultSetDecorator(ResultSet rs);

	protected abstract ResultSetMetaData makeResultSetMetaDataDecorator(ResultSetMetaData rsmd);

	// ------------ PreparedStatement interface is below this line ------------------
	
	public void addBatch() throws SQLException {
		preparedStatement.addBatch();
	}

	public void addBatch(String sql) throws SQLException {
		preparedStatement.addBatch(sql);
	}

	public void cancel() throws SQLException {
		preparedStatement.cancel();
	}

	public void clearBatch() throws SQLException {
		preparedStatement.clearBatch();
	}

	public void clearParameters() throws SQLException {
		preparedStatement.clearParameters();
	}

	public void clearWarnings() throws SQLException {
		preparedStatement.clearWarnings();
	}

	public void close() throws SQLException {
		preparedStatement.close();
	}

	public boolean execute() throws SQLException {
		return preparedStatement.execute();
	}

	public boolean execute(String sql, int autoGeneratedKeys)
			throws SQLException {
		return preparedStatement.execute(sql, autoGeneratedKeys);
	}

	public boolean execute(String sql, int[] columnIndexes) throws SQLException {
		return preparedStatement.execute(sql, columnIndexes);
	}

	public boolean execute(String sql, String[] columnNames)
			throws SQLException {
		return preparedStatement.execute(sql, columnNames);
	}

	public boolean execute(String sql) throws SQLException {
		return preparedStatement.execute(sql);
	}

	public int[] executeBatch() throws SQLException {
		return preparedStatement.executeBatch();
	}

	public ResultSet executeQuery() throws SQLException {
		return makeResultSetDecorator(preparedStatement.executeQuery());
	}

	public ResultSet executeQuery(String sql) throws SQLException {
		return makeResultSetDecorator(preparedStatement.executeQuery(sql));
	}

	public int executeUpdate() throws SQLException {
		return preparedStatement.executeUpdate();
	}

	public int executeUpdate(String sql, int autoGeneratedKeys)
			throws SQLException {
		return preparedStatement.executeUpdate(sql, autoGeneratedKeys);
	}

	public int executeUpdate(String sql, int[] columnIndexes)
			throws SQLException {
		return preparedStatement.executeUpdate(sql, columnIndexes);
	}

	public int executeUpdate(String sql, String[] columnNames)
			throws SQLException {
		return preparedStatement.executeUpdate(sql, columnNames);
	}

	public int executeUpdate(String sql) throws SQLException {
		return preparedStatement.executeUpdate(sql);
	}

	public Connection getConnection() throws SQLException {
		return parentConnection;
	}

	public int getFetchDirection() throws SQLException {
		return preparedStatement.getFetchDirection();
	}

	public int getFetchSize() throws SQLException {
		return preparedStatement.getFetchSize();
	}

	public ResultSet getGeneratedKeys() throws SQLException {
		return makeResultSetDecorator(preparedStatement.getGeneratedKeys());
	}

	public int getMaxFieldSize() throws SQLException {
		return preparedStatement.getMaxFieldSize();
	}

	public int getMaxRows() throws SQLException {
		return preparedStatement.getMaxRows();
	}

	public ResultSetMetaData getMetaData() throws SQLException {
		return makeResultSetMetaDataDecorator(preparedStatement.getMetaData());
	}

	public boolean getMoreResults() throws SQLException {
		return preparedStatement.getMoreResults();
	}

	public boolean getMoreResults(int current) throws SQLException {
		return preparedStatement.getMoreResults(current);
	}

	public ParameterMetaData getParameterMetaData() throws SQLException {
		return preparedStatement.getParameterMetaData();
	}

	public int getQueryTimeout() throws SQLException {
		return preparedStatement.getQueryTimeout();
	}

	public ResultSet getResultSet() throws SQLException {
		if (preparedStatement.getResultSet() == null) {
			return null;
		}
		return makeResultSetDecorator(preparedStatement.getResultSet());
	}

	public int getResultSetConcurrency() throws SQLException {
		return preparedStatement.getResultSetConcurrency();
	}

	public int getResultSetHoldability() throws SQLException {
		return preparedStatement.getResultSetHoldability();
	}

	public int getResultSetType() throws SQLException {
		return preparedStatement.getResultSetType();
	}

	public int getUpdateCount() throws SQLException {
		return preparedStatement.getUpdateCount();
	}

	public SQLWarning getWarnings() throws SQLException {
		return preparedStatement.getWarnings();
	}

	public void setArray(int i, Array x) throws SQLException {
		preparedStatement.setArray(i, x);
	}

	public void setAsciiStream(int parameterIndex, InputStream x, int length)
			throws SQLException {
		preparedStatement.setAsciiStream(parameterIndex, x, length);
	}

	public void setBigDecimal(int parameterIndex, BigDecimal x)
			throws SQLException {
		preparedStatement.setBigDecimal(parameterIndex, x);
	}

	public void setBinaryStream(int parameterIndex, InputStream x, int length)
			throws SQLException {
		preparedStatement.setBinaryStream(parameterIndex, x, length);
	}

	public void setBlob(int i, Blob x) throws SQLException {
		preparedStatement.setBlob(i, x);
	}

	public void setBoolean(int parameterIndex, boolean x) throws SQLException {
		preparedStatement.setBoolean(parameterIndex, x);
	}

	public void setByte(int parameterIndex, byte x) throws SQLException {
		preparedStatement.setByte(parameterIndex, x);
	}

	public void setBytes(int parameterIndex, byte[] x) throws SQLException {
		preparedStatement.setBytes(parameterIndex, x);
	}

	public void setCharacterStream(int parameterIndex, Reader reader, int length)
			throws SQLException {
		preparedStatement.setCharacterStream(parameterIndex, reader, length);
	}

	public void setClob(int i, Clob x) throws SQLException {
		preparedStatement.setClob(i, x);
	}

	public void setCursorName(String name) throws SQLException {
		preparedStatement.setCursorName(name);
	}

	public void setDate(int parameterIndex, Date x, Calendar cal)
			throws SQLException {
		preparedStatement.setDate(parameterIndex, x, cal);
	}

	public void setDate(int parameterIndex, Date x) throws SQLException {
		preparedStatement.setDate(parameterIndex, x);
	}

	public void setDouble(int parameterIndex, double x) throws SQLException {
		preparedStatement.setDouble(parameterIndex, x);
	}

	public void setEscapeProcessing(boolean enable) throws SQLException {
		preparedStatement.setEscapeProcessing(enable);
	}

	public void setFetchDirection(int direction) throws SQLException {
		preparedStatement.setFetchDirection(direction);
	}

	public void setFetchSize(int rows) throws SQLException {
		preparedStatement.setFetchSize(rows);
	}

	public void setFloat(int parameterIndex, float x) throws SQLException {
		preparedStatement.setFloat(parameterIndex, x);
	}

	public void setInt(int parameterIndex, int x) throws SQLException {
		preparedStatement.setInt(parameterIndex, x);
	}

	public void setLong(int parameterIndex, long x) throws SQLException {
		preparedStatement.setLong(parameterIndex, x);
	}

	public void setMaxFieldSize(int max) throws SQLException {
		preparedStatement.setMaxFieldSize(max);
	}

	public void setMaxRows(int max) throws SQLException {
		preparedStatement.setMaxRows(max);
	}

	public void setNull(int paramIndex, int sqlType, String typeName)
			throws SQLException {
		preparedStatement.setNull(paramIndex, sqlType, typeName);
	}

	public void setNull(int parameterIndex, int sqlType) throws SQLException {
		preparedStatement.setNull(parameterIndex, sqlType);
	}

	public void setObject(int parameterIndex, Object x, int targetSqlType,
			int scale) throws SQLException {
		
		preparedStatement.setObject(parameterIndex, x, targetSqlType, scale);
	}

	public void setObject(int parameterIndex, Object x, int targetSqlType)
			throws SQLException {
		preparedStatement.setObject(parameterIndex, x, targetSqlType);
	}

	public void setObject(int parameterIndex, Object x) throws SQLException {
		preparedStatement.setObject(parameterIndex, applyJavaToJdbcMappings(x));
	}
	
	/**
	 * Most database drivers mess up with the date conversions
	 * and other java to sql object mappings.
	 * Oracle does. SQL Server does. They didn't implement the 
	 * object mappings as specified in the JDBC specs.
	 * This function will java objects to sql ones if needed.
	 * @param x Object to maybe convert.
	 * @return Either the 
	 */
	protected Object applyJavaToJdbcMappings(Object x) {
		
		// Convert java dates to sql dates.
		if (x instanceof java.util.Date) {
			if (x == null) {
				return (java.sql.Date) null;
			} else {
				return new java.sql.Date(((java.util.Date)x).getTime());
			}
		
		// No conversion necessary. Return the original object.
		} else {
			return x;
		}
	}

	public void setQueryTimeout(int seconds) throws SQLException {
		preparedStatement.setQueryTimeout(seconds);
	}

	public void setRef(int i, Ref x) throws SQLException {
		preparedStatement.setRef(i, x);
	}

	public void setShort(int parameterIndex, short x) throws SQLException {
		preparedStatement.setShort(parameterIndex, x);
	}

	public void setString(int parameterIndex, String x) throws SQLException {
		preparedStatement.setString(parameterIndex, x);
	}

	public void setTime(int parameterIndex, Time x, Calendar cal)
			throws SQLException {
		preparedStatement.setTime(parameterIndex, x, cal);
	}

	public void setTime(int parameterIndex, Time x) throws SQLException {
		preparedStatement.setTime(parameterIndex, x);
	}

	public void setTimestamp(int parameterIndex, Timestamp x, Calendar cal)
			throws SQLException {
		preparedStatement.setTimestamp(parameterIndex, x, cal);
	}

	public void setTimestamp(int parameterIndex, Timestamp x)
			throws SQLException {
		preparedStatement.setTimestamp(parameterIndex, x);
	}

	@SuppressWarnings("deprecation")
	public void setUnicodeStream(int parameterIndex, InputStream x, int length)
			throws SQLException {
		preparedStatement.setUnicodeStream(parameterIndex, x, length);
	}

	public void setURL(int parameterIndex, URL x) throws SQLException {
		preparedStatement.setURL(parameterIndex, x);
	}
	
	
}
