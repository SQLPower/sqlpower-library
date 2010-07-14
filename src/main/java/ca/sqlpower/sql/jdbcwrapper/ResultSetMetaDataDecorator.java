package ca.sqlpower.sql.jdbcwrapper;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;

/**
 * A class that passes through all ResultSetMetaData method calls to
 * an inner ResultSetMetaData object which does all the work.  This
 * class is designed to be subclassed by database-specific
 * decorators which can intercept certain method calls and
 * tweak return values.
 */
public abstract class ResultSetMetaDataDecorator implements ResultSetMetaData{
	
	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		throw new UnsupportedOperationException("Currently it is only possible to wrap JDBC 3.");
	}

	public <T> T unwrap(Class<T> iface) throws SQLException {
		throw new UnsupportedOperationException("Currently it is only possible to wrap JDBC 3.");
	}

	/**
	 * The actual result set meta data that does all the work.
	 */
	private final ResultSetMetaData resultSetMetaData;

	/**
	 * Creates a new result set decorator for the given result set.
	 */
	public ResultSetMetaDataDecorator(ResultSetMetaData rsmd) {
		if (rsmd == null) throw new NullPointerException("Null result set meta data not allowed");
		this.resultSetMetaData = rsmd;
	}
	
	// ------------ ResultSetMetaData interface is below this line ------------------
	public String getCatalogName(int column) throws SQLException {
		return resultSetMetaData.getCatalogName(column);
	}

	public String getColumnClassName(int column) throws SQLException {
		return resultSetMetaData.getColumnClassName(column);
	}

	public int getColumnCount() throws SQLException {
		return resultSetMetaData.getColumnCount();
	}

	public int getColumnDisplaySize(int column) throws SQLException {
		return resultSetMetaData.getColumnDisplaySize(column);
	}

	public String getColumnLabel(int column) throws SQLException {
		return resultSetMetaData.getColumnLabel(column);
	}

	public String getColumnName(int column) throws SQLException {
		return resultSetMetaData.getColumnName(column);
	}

	public int getColumnType(int column) throws SQLException {
		return resultSetMetaData.getColumnType(column);
	}

	public String getColumnTypeName(int column) throws SQLException {
		return resultSetMetaData.getColumnTypeName(column);
	}

	public int getPrecision(int column) throws SQLException {
		return resultSetMetaData.getPrecision(column);
	}

	public int getScale(int column) throws SQLException {
		return resultSetMetaData.getScale(column);
	}

	public String getSchemaName(int column) throws SQLException {
		return resultSetMetaData.getSchemaName(column);
	}

	public String getTableName(int column) throws SQLException {
		return resultSetMetaData.getTableName(column);
	}

	public boolean isAutoIncrement(int column) throws SQLException {
		return resultSetMetaData.isAutoIncrement(column);
	}

	public boolean isCaseSensitive(int column) throws SQLException {
		return resultSetMetaData.isCaseSensitive(column);
	}

	public boolean isCurrency(int column) throws SQLException {
		return resultSetMetaData.isCurrency(column);
	}

	public boolean isDefinitelyWritable(int column) throws SQLException {
		return resultSetMetaData.isDefinitelyWritable(column);
	}

	public int isNullable(int column) throws SQLException {
		return resultSetMetaData.isNullable(column);
	}

	public boolean isReadOnly(int column) throws SQLException {
		return resultSetMetaData.isReadOnly(column);
	}

	public boolean isSearchable(int column) throws SQLException {
		return resultSetMetaData.isSearchable(column);
	}

	public boolean isSigned(int column) throws SQLException {
		return resultSetMetaData.isSigned(column);
	}

	public boolean isWritable(int column) throws SQLException {
		return resultSetMetaData.isWritable(column);
	}
	
}
