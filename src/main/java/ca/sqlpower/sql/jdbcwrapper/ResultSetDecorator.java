package ca.sqlpower.sql.jdbcwrapper;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Map;

/**
 * A class that passes through all ResultSet method calls to
 * an inner ResultSet object which does all the work.  This
 * class is designed to be subclassed by database-specific
 * decorators which can intercept certain method calls and
 * tweak return values.
 */
public abstract class ResultSetDecorator implements ResultSet {
	
	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		throw new UnsupportedOperationException("Currently it is only possible to wrap JDBC 3.");
	}

	public <T> T unwrap(Class<T> iface) throws SQLException {
		throw new UnsupportedOperationException("Currently it is only possible to wrap JDBC 3.");
	}

	public int getHoldability() throws SQLException {
		throw new UnsupportedOperationException("Currently it is only possible to wrap JDBC 3.");
	}

	public Reader getNCharacterStream(int columnIndex) throws SQLException {
		throw new UnsupportedOperationException("Currently it is only possible to wrap JDBC 3.");
	}

	public Reader getNCharacterStream(String columnLabel) throws SQLException {
		throw new UnsupportedOperationException("Currently it is only possible to wrap JDBC 3.");
	}

	public NClob getNClob(int columnIndex) throws SQLException {
		throw new UnsupportedOperationException("Currently it is only possible to wrap JDBC 3.");
	}

	public NClob getNClob(String columnLabel) throws SQLException {
		throw new UnsupportedOperationException("Currently it is only possible to wrap JDBC 3.");
	}

	public String getNString(int columnIndex) throws SQLException {
		throw new UnsupportedOperationException("Currently it is only possible to wrap JDBC 3.");
	}

	public String getNString(String columnLabel) throws SQLException {
		throw new UnsupportedOperationException("Currently it is only possible to wrap JDBC 3.");
	}

	public RowId getRowId(int columnIndex) throws SQLException {
		throw new UnsupportedOperationException("Currently it is only possible to wrap JDBC 3.");
	}

	public RowId getRowId(String columnLabel) throws SQLException {
		throw new UnsupportedOperationException("Currently it is only possible to wrap JDBC 3.");
	}

	public SQLXML getSQLXML(int columnIndex) throws SQLException {
		throw new UnsupportedOperationException("Currently it is only possible to wrap JDBC 3.");
	}

	public SQLXML getSQLXML(String columnLabel) throws SQLException {
		throw new UnsupportedOperationException("Currently it is only possible to wrap JDBC 3.");
	}

	public boolean isClosed() throws SQLException {
		throw new UnsupportedOperationException("Currently it is only possible to wrap JDBC 3.");
	}

	public void updateAsciiStream(int columnIndex, InputStream x, long length)
			throws SQLException {
		throw new UnsupportedOperationException("Currently it is only possible to wrap JDBC 3.");
	}

	public void updateAsciiStream(int columnIndex, InputStream x)
			throws SQLException {
		throw new UnsupportedOperationException("Currently it is only possible to wrap JDBC 3.");
	}

	public void updateAsciiStream(String columnLabel, InputStream x, long length)
			throws SQLException {
		throw new UnsupportedOperationException("Currently it is only possible to wrap JDBC 3.");
	}

	public void updateAsciiStream(String columnLabel, InputStream x)
			throws SQLException {
		throw new UnsupportedOperationException("Currently it is only possible to wrap JDBC 3.");
	}

	public void updateBinaryStream(int columnIndex, InputStream x, long length)
			throws SQLException {
		throw new UnsupportedOperationException("Currently it is only possible to wrap JDBC 3.");
	}

	public void updateBinaryStream(int columnIndex, InputStream x)
			throws SQLException {
		throw new UnsupportedOperationException("Currently it is only possible to wrap JDBC 3.");
	}

	public void updateBinaryStream(String columnLabel, InputStream x,
			long length) throws SQLException {
		throw new UnsupportedOperationException("Currently it is only possible to wrap JDBC 3.");
	}

	public void updateBinaryStream(String columnLabel, InputStream x)
			throws SQLException {
		throw new UnsupportedOperationException("Currently it is only possible to wrap JDBC 3.");
	}

	public void updateBlob(int columnIndex, InputStream inputStream, long length)
			throws SQLException {
		throw new UnsupportedOperationException("Currently it is only possible to wrap JDBC 3.");
	}

	public void updateBlob(int columnIndex, InputStream inputStream)
			throws SQLException {
		throw new UnsupportedOperationException("Currently it is only possible to wrap JDBC 3.");
	}

	public void updateBlob(String columnLabel, InputStream inputStream,
			long length) throws SQLException {
		throw new UnsupportedOperationException("Currently it is only possible to wrap JDBC 3.");
	}

	public void updateBlob(String columnLabel, InputStream inputStream)
			throws SQLException {
		throw new UnsupportedOperationException("Currently it is only possible to wrap JDBC 3.");
	}

	public void updateCharacterStream(int columnIndex, Reader x, long length)
			throws SQLException {
		throw new UnsupportedOperationException("Currently it is only possible to wrap JDBC 3.");
	}

	public void updateCharacterStream(int columnIndex, Reader x)
			throws SQLException {
		throw new UnsupportedOperationException("Currently it is only possible to wrap JDBC 3.");
	}

	public void updateCharacterStream(String columnLabel, Reader reader,
			long length) throws SQLException {
		throw new UnsupportedOperationException("Currently it is only possible to wrap JDBC 3.");
	}

	public void updateCharacterStream(String columnLabel, Reader reader)
			throws SQLException {
		throw new UnsupportedOperationException("Currently it is only possible to wrap JDBC 3.");
	}

	public void updateClob(int columnIndex, Reader reader, long length)
			throws SQLException {
		throw new UnsupportedOperationException("Currently it is only possible to wrap JDBC 3.");
	}

	public void updateClob(int columnIndex, Reader reader) throws SQLException {
		throw new UnsupportedOperationException("Currently it is only possible to wrap JDBC 3.");
	}

	public void updateClob(String columnLabel, Reader reader, long length)
			throws SQLException {
		throw new UnsupportedOperationException("Currently it is only possible to wrap JDBC 3.");
	}

	public void updateClob(String columnLabel, Reader reader)
			throws SQLException {
		throw new UnsupportedOperationException("Currently it is only possible to wrap JDBC 3.");
	}

	public void updateNCharacterStream(int columnIndex, Reader x, long length)
			throws SQLException {
		throw new UnsupportedOperationException("Currently it is only possible to wrap JDBC 3.");
	}

	public void updateNCharacterStream(int columnIndex, Reader x)
			throws SQLException {
		throw new UnsupportedOperationException("Currently it is only possible to wrap JDBC 3.");
	}

	public void updateNCharacterStream(String columnLabel, Reader reader,
			long length) throws SQLException {
		throw new UnsupportedOperationException("Currently it is only possible to wrap JDBC 3.");
	}

	public void updateNCharacterStream(String columnLabel, Reader reader)
			throws SQLException {
		throw new UnsupportedOperationException("Currently it is only possible to wrap JDBC 3.");
	}

	public void updateNClob(int columnIndex, NClob clob) throws SQLException {
		throw new UnsupportedOperationException("Currently it is only possible to wrap JDBC 3.");
	}

	public void updateNClob(int columnIndex, Reader reader, long length)
			throws SQLException {
		throw new UnsupportedOperationException("Currently it is only possible to wrap JDBC 3.");
	}

	public void updateNClob(int columnIndex, Reader reader) throws SQLException {
		throw new UnsupportedOperationException("Currently it is only possible to wrap JDBC 3.");
	}

	public void updateNClob(String columnLabel, NClob clob) throws SQLException {
		throw new UnsupportedOperationException("Currently it is only possible to wrap JDBC 3.");
	}

	public void updateNClob(String columnLabel, Reader reader, long length)
			throws SQLException {
		throw new UnsupportedOperationException("Currently it is only possible to wrap JDBC 3.");
	}

	public void updateNClob(String columnLabel, Reader reader)
			throws SQLException {
		throw new UnsupportedOperationException("Currently it is only possible to wrap JDBC 3.");
	}

	public void updateNString(int columnIndex, String string)
			throws SQLException {
		throw new UnsupportedOperationException("Currently it is only possible to wrap JDBC 3.");
	}

	public void updateNString(String columnLabel, String string)
			throws SQLException {
		throw new UnsupportedOperationException("Currently it is only possible to wrap JDBC 3.");
	}

	public void updateRowId(int columnIndex, RowId x) throws SQLException {
		throw new UnsupportedOperationException("Currently it is only possible to wrap JDBC 3.");
	}

	public void updateRowId(String columnLabel, RowId x) throws SQLException {
		throw new UnsupportedOperationException("Currently it is only possible to wrap JDBC 3.");
	}

	public void updateSQLXML(int columnIndex, SQLXML xmlObject)
			throws SQLException {
		throw new UnsupportedOperationException("Currently it is only possible to wrap JDBC 3.");
	}

	public void updateSQLXML(String columnLabel, SQLXML xmlObject)
			throws SQLException {
		throw new UnsupportedOperationException("Currently it is only possible to wrap JDBC 3.");
	}

	public static void checkInterrupted() throws SQLException {
		if (Thread.interrupted()) {
			throw new SQLException("Operation canceled due to interrupt request.");
		}
	}

	/**
	 * The statement that created this result set.
	 */
	private final Statement parentStatement;
	
	/**
	 * The actual result set that does all the work.
	 */
	protected final ResultSet resultSet;

	/**
	 * Creates a new result set decorator for the given result set.
	 */
	public ResultSetDecorator(Statement parentStatement, ResultSet rs) {
		if (rs == null) throw new NullPointerException("Null result set not allowed");
		if (parentStatement == null) throw new NullPointerException("Null statement not allowed");
		this.parentStatement = parentStatement;
		this.resultSet = rs;
	}
	
	protected abstract ResultSetMetaData makeResultSetMetaDataDecorator(
			ResultSetMetaData rsmd);
	
	// ------------ ResultSet interface is below this line ------------------
	
	public boolean absolute(int row) throws SQLException {
		ResultSetDecorator.checkInterrupted();
		return resultSet.absolute(row);
	}

	public void afterLast() throws SQLException {
		ResultSetDecorator.checkInterrupted();
		resultSet.afterLast();
	}

	public void beforeFirst() throws SQLException {
		ResultSetDecorator.checkInterrupted();
		resultSet.beforeFirst();
	}

	public void cancelRowUpdates() throws SQLException {
		resultSet.cancelRowUpdates();
	}

	public void clearWarnings() throws SQLException {
		resultSet.clearWarnings();
	}

	public void close() throws SQLException {
		resultSet.close();
	}

	public void deleteRow() throws SQLException {
		resultSet.deleteRow();
	}

	public int findColumn(String columnName) throws SQLException {
		ResultSetDecorator.checkInterrupted();
		return resultSet.findColumn(columnName);
	}

	public boolean first() throws SQLException {
		ResultSetDecorator.checkInterrupted();
		return resultSet.first();
	}

	public Array getArray(int i) throws SQLException {
		return resultSet.getArray(i);
	}

	public Array getArray(String colName) throws SQLException {
		return resultSet.getArray(colName);
	}

	public InputStream getAsciiStream(int columnIndex) throws SQLException {
		return resultSet.getAsciiStream(columnIndex);
	}

	public InputStream getAsciiStream(String columnName) throws SQLException {
		return resultSet.getAsciiStream(columnName);
	}

	@SuppressWarnings("deprecation")
	public BigDecimal getBigDecimal(int columnIndex, int scale)
			throws SQLException {
		return resultSet.getBigDecimal(columnIndex, scale);
	}

	public BigDecimal getBigDecimal(int columnIndex) throws SQLException {
		return resultSet.getBigDecimal(columnIndex);
	}

	@SuppressWarnings("deprecation")
	public BigDecimal getBigDecimal(String columnName, int scale)
			throws SQLException {
		return resultSet.getBigDecimal(columnName, scale);
	}

	public BigDecimal getBigDecimal(String columnName) throws SQLException {
		return resultSet.getBigDecimal(columnName);
	}

	public InputStream getBinaryStream(int columnIndex) throws SQLException {
		return resultSet.getBinaryStream(columnIndex);
	}

	public InputStream getBinaryStream(String columnName) throws SQLException {
		return resultSet.getBinaryStream(columnName);
	}

	public Blob getBlob(int i) throws SQLException {
		return resultSet.getBlob(i);
	}

	public Blob getBlob(String colName) throws SQLException {
		return resultSet.getBlob(colName);
	}

	public boolean getBoolean(int columnIndex) throws SQLException {
		return resultSet.getBoolean(columnIndex);
	}

	public boolean getBoolean(String columnName) throws SQLException {
		return resultSet.getBoolean(columnName);
	}

	public byte getByte(int columnIndex) throws SQLException {
		return resultSet.getByte(columnIndex);
	}

	public byte getByte(String columnName) throws SQLException {
		return resultSet.getByte(columnName);
	}

	public byte[] getBytes(int columnIndex) throws SQLException {
		return resultSet.getBytes(columnIndex);
	}

	public byte[] getBytes(String columnName) throws SQLException {
		return resultSet.getBytes(columnName);
	}

	public Reader getCharacterStream(int columnIndex) throws SQLException {
		return resultSet.getCharacterStream(columnIndex);
	}

	public Reader getCharacterStream(String columnName) throws SQLException {
		return resultSet.getCharacterStream(columnName);
	}

	public Clob getClob(int i) throws SQLException {
		return resultSet.getClob(i);
	}

	public Clob getClob(String colName) throws SQLException {
		return resultSet.getClob(colName);
	}

	public int getConcurrency() throws SQLException {
		return resultSet.getConcurrency();
	}

	public String getCursorName() throws SQLException {
		return resultSet.getCursorName();
	}

	public Date getDate(int columnIndex, Calendar cal) throws SQLException {
		return resultSet.getDate(columnIndex, cal);
	}

	public Date getDate(int columnIndex) throws SQLException {
		return resultSet.getDate(columnIndex);
	}

	public Date getDate(String columnName, Calendar cal) throws SQLException {
		return resultSet.getDate(columnName, cal);
	}

	public Date getDate(String columnName) throws SQLException {
		return resultSet.getDate(columnName);
	}

	public double getDouble(int columnIndex) throws SQLException {
		return resultSet.getDouble(columnIndex);
	}

	public double getDouble(String columnName) throws SQLException {
		return resultSet.getDouble(columnName);
	}

	public int getFetchDirection() throws SQLException {
		return resultSet.getFetchDirection();
	}

	public int getFetchSize() throws SQLException {
		return resultSet.getFetchSize();
	}

	public float getFloat(int columnIndex) throws SQLException {
		return resultSet.getFloat(columnIndex);
	}

	public float getFloat(String columnName) throws SQLException {
		return resultSet.getFloat(columnName);
	}

	public int getInt(int columnIndex) throws SQLException {
		return resultSet.getInt(columnIndex);
	}

	public int getInt(String columnName) throws SQLException {
		return resultSet.getInt(columnName);
	}

	public long getLong(int columnIndex) throws SQLException {
		return resultSet.getLong(columnIndex);
	}

	public long getLong(String columnName) throws SQLException {
		return resultSet.getLong(columnName);
	}

	public ResultSetMetaData getMetaData() throws SQLException {
		return makeResultSetMetaDataDecorator(resultSet.getMetaData());
	}

	public Object getObject(int i, Map<String, Class<?>> map)
			throws SQLException {
		return resultSet.getObject(i, map);
	}

	public Object getObject(int columnIndex) throws SQLException {
		return resultSet.getObject(columnIndex);
	}

	public Object getObject(String colName, Map<String, Class<?>> map)
			throws SQLException {
		return resultSet.getObject(colName, map);
	}

	public Object getObject(String columnName) throws SQLException {
		return resultSet.getObject(columnName);
	}

	public Ref getRef(int i) throws SQLException {
		return resultSet.getRef(i);
	}

	public Ref getRef(String colName) throws SQLException {
		return resultSet.getRef(colName);
	}

	public int getRow() throws SQLException {
		return resultSet.getRow();
	}

	public short getShort(int columnIndex) throws SQLException {
		return resultSet.getShort(columnIndex);
	}

	public short getShort(String columnName) throws SQLException {
		return resultSet.getShort(columnName);
	}

	public Statement getStatement() throws SQLException {
		return parentStatement;
	}

	public String getString(int columnIndex) throws SQLException {
		return resultSet.getString(columnIndex);
	}

	public String getString(String columnName) throws SQLException {
		return resultSet.getString(columnName);
	}

	public Time getTime(int columnIndex, Calendar cal) throws SQLException {
		return resultSet.getTime(columnIndex, cal);
	}

	public Time getTime(int columnIndex) throws SQLException {
		return resultSet.getTime(columnIndex);
	}

	public Time getTime(String columnName, Calendar cal) throws SQLException {
		return resultSet.getTime(columnName, cal);
	}

	public Time getTime(String columnName) throws SQLException {
		return resultSet.getTime(columnName);
	}

	public Timestamp getTimestamp(int columnIndex, Calendar cal)
			throws SQLException {
		return resultSet.getTimestamp(columnIndex, cal);
	}

	public Timestamp getTimestamp(int columnIndex) throws SQLException {
		return resultSet.getTimestamp(columnIndex);
	}

	public Timestamp getTimestamp(String columnName, Calendar cal)
			throws SQLException {
		return resultSet.getTimestamp(columnName, cal);
	}

	public Timestamp getTimestamp(String columnName) throws SQLException {
		return resultSet.getTimestamp(columnName);
	}

	public int getType() throws SQLException {
		return resultSet.getType();
	}

	@SuppressWarnings("deprecation")
	public InputStream getUnicodeStream(int columnIndex) throws SQLException {
		return resultSet.getUnicodeStream(columnIndex);
	}

	@SuppressWarnings("deprecation")
	public InputStream getUnicodeStream(String columnName) throws SQLException {
		return resultSet.getUnicodeStream(columnName);
	}

	public URL getURL(int columnIndex) throws SQLException {
		return resultSet.getURL(columnIndex);
	}

	public URL getURL(String columnName) throws SQLException {
		return resultSet.getURL(columnName);
	}

	public SQLWarning getWarnings() throws SQLException {
		return resultSet.getWarnings();
	}

	public void insertRow() throws SQLException {
		ResultSetDecorator.checkInterrupted();
		resultSet.insertRow();
	}

	public boolean isAfterLast() throws SQLException {
		return resultSet.isAfterLast();
	}

	public boolean isBeforeFirst() throws SQLException {
		return resultSet.isBeforeFirst();
	}

	public boolean isFirst() throws SQLException {
		return resultSet.isFirst();
	}

	public boolean isLast() throws SQLException {
		return resultSet.isLast();
	}

	public boolean last() throws SQLException {
		ResultSetDecorator.checkInterrupted();
		return resultSet.last();
	}

	public void moveToCurrentRow() throws SQLException {
		ResultSetDecorator.checkInterrupted();
		resultSet.moveToCurrentRow();
	}

	public void moveToInsertRow() throws SQLException {
		ResultSetDecorator.checkInterrupted();
		resultSet.moveToInsertRow();
	}

	public boolean next() throws SQLException {
		ResultSetDecorator.checkInterrupted();
		return resultSet.next();
	}

	public boolean previous() throws SQLException {
		ResultSetDecorator.checkInterrupted();
		return resultSet.previous();
	}

	public void refreshRow() throws SQLException {
		resultSet.refreshRow();
	}

	public boolean relative(int rows) throws SQLException {
		ResultSetDecorator.checkInterrupted();
		return resultSet.relative(rows);
	}

	public boolean rowDeleted() throws SQLException {
		return resultSet.rowDeleted();
	}

	public boolean rowInserted() throws SQLException {
		return resultSet.rowInserted();
	}

	public boolean rowUpdated() throws SQLException {
		return resultSet.rowUpdated();
	}

	public void setFetchDirection(int direction) throws SQLException {
		resultSet.setFetchDirection(direction);
	}

	public void setFetchSize(int rows) throws SQLException {
		resultSet.setFetchSize(rows);
	}

	public void updateArray(int columnIndex, Array x) throws SQLException {
		resultSet.updateArray(columnIndex, x);
	}

	public void updateArray(String columnName, Array x) throws SQLException {
		resultSet.updateArray(columnName, x);
	}

	public void updateAsciiStream(int columnIndex, InputStream x, int length)
			throws SQLException {
		resultSet.updateAsciiStream(columnIndex, x, length);
	}

	public void updateAsciiStream(String columnName, InputStream x, int length)
			throws SQLException {
		resultSet.updateAsciiStream(columnName, x, length);
	}

	public void updateBigDecimal(int columnIndex, BigDecimal x)
			throws SQLException {
		resultSet.updateBigDecimal(columnIndex, x);
	}

	public void updateBigDecimal(String columnName, BigDecimal x)
			throws SQLException {
		resultSet.updateBigDecimal(columnName, x);
	}

	public void updateBinaryStream(int columnIndex, InputStream x, int length)
			throws SQLException {
		resultSet.updateBinaryStream(columnIndex, x, length);
	}

	public void updateBinaryStream(String columnName, InputStream x, int length)
			throws SQLException {
		resultSet.updateBinaryStream(columnName, x, length);
	}

	public void updateBlob(int columnIndex, Blob x) throws SQLException {
		resultSet.updateBlob(columnIndex, x);
	}

	public void updateBlob(String columnName, Blob x) throws SQLException {
		resultSet.updateBlob(columnName, x);
	}

	public void updateBoolean(int columnIndex, boolean x) throws SQLException {
		resultSet.updateBoolean(columnIndex, x);
	}

	public void updateBoolean(String columnName, boolean x) throws SQLException {
		resultSet.updateBoolean(columnName, x);
	}

	public void updateByte(int columnIndex, byte x) throws SQLException {
		resultSet.updateByte(columnIndex, x);
	}

	public void updateByte(String columnName, byte x) throws SQLException {
		resultSet.updateByte(columnName, x);
	}

	public void updateBytes(int columnIndex, byte[] x) throws SQLException {
		resultSet.updateBytes(columnIndex, x);
	}

	public void updateBytes(String columnName, byte[] x) throws SQLException {
		resultSet.updateBytes(columnName, x);
	}

	public void updateCharacterStream(int columnIndex, Reader x, int length)
			throws SQLException {
		resultSet.updateCharacterStream(columnIndex, x, length);
	}

	public void updateCharacterStream(String columnName, Reader reader,
			int length) throws SQLException {
		resultSet.updateCharacterStream(columnName, reader, length);
	}

	public void updateClob(int columnIndex, Clob x) throws SQLException {
		resultSet.updateClob(columnIndex, x);
	}

	public void updateClob(String columnName, Clob x) throws SQLException {
		resultSet.updateClob(columnName, x);
	}

	public void updateDate(int columnIndex, Date x) throws SQLException {
		resultSet.updateDate(columnIndex, x);
	}

	public void updateDate(String columnName, Date x) throws SQLException {
		resultSet.updateDate(columnName, x);
	}

	public void updateDouble(int columnIndex, double x) throws SQLException {
		resultSet.updateDouble(columnIndex, x);
	}

	public void updateDouble(String columnName, double x) throws SQLException {
		resultSet.updateDouble(columnName, x);
	}

	public void updateFloat(int columnIndex, float x) throws SQLException {
		resultSet.updateFloat(columnIndex, x);
	}

	public void updateFloat(String columnName, float x) throws SQLException {
		resultSet.updateFloat(columnName, x);
	}

	public void updateInt(int columnIndex, int x) throws SQLException {
		resultSet.updateInt(columnIndex, x);
	}

	public void updateInt(String columnName, int x) throws SQLException {
		resultSet.updateInt(columnName, x);
	}

	public void updateLong(int columnIndex, long x) throws SQLException {
		resultSet.updateLong(columnIndex, x);
	}

	public void updateLong(String columnName, long x) throws SQLException {
		resultSet.updateLong(columnName, x);
	}

	public void updateNull(int columnIndex) throws SQLException {
		resultSet.updateNull(columnIndex);
	}

	public void updateNull(String columnName) throws SQLException {
		resultSet.updateNull(columnName);
	}

	public void updateObject(int columnIndex, Object x, int scale)
			throws SQLException {
		resultSet.updateObject(columnIndex, x, scale);
	}

	public void updateObject(int columnIndex, Object x) throws SQLException {
		resultSet.updateObject(columnIndex, x);
	}

	public void updateObject(String columnName, Object x, int scale)
			throws SQLException {
		resultSet.updateObject(columnName, x, scale);
	}

	public void updateObject(String columnName, Object x) throws SQLException {
		resultSet.updateObject(columnName, x);
	}

	public void updateRef(int columnIndex, Ref x) throws SQLException {
		resultSet.updateRef(columnIndex, x);
	}

	public void updateRef(String columnName, Ref x) throws SQLException {
		resultSet.updateRef(columnName, x);
	}

	public void updateRow() throws SQLException {
		resultSet.updateRow();
	}

	public void updateShort(int columnIndex, short x) throws SQLException {
		resultSet.updateShort(columnIndex, x);
	}

	public void updateShort(String columnName, short x) throws SQLException {
		resultSet.updateShort(columnName, x);
	}

	public void updateString(int columnIndex, String x) throws SQLException {
		resultSet.updateString(columnIndex, x);
	}

	public void updateString(String columnName, String x) throws SQLException {
		resultSet.updateString(columnName, x);
	}

	public void updateTime(int columnIndex, Time x) throws SQLException {
		resultSet.updateTime(columnIndex, x);
	}

	public void updateTime(String columnName, Time x) throws SQLException {
		resultSet.updateTime(columnName, x);
	}

	public void updateTimestamp(int columnIndex, Timestamp x)
			throws SQLException {
		resultSet.updateTimestamp(columnIndex, x);
	}

	public void updateTimestamp(String columnName, Timestamp x)
			throws SQLException {
		resultSet.updateTimestamp(columnName, x);
	}

	public boolean wasNull() throws SQLException {
		return resultSet.wasNull();
	}

}
