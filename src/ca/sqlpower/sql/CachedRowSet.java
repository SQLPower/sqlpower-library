package ca.sqlpower.sql;

import java.util.List;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Collections;
import java.sql.*;
import java.math.BigDecimal;
import org.apache.log4j.Logger;

/**
 * The CachedRowSet is a serializable container for holding the
 * results of a SQL query.  It supports read-only random access of
 * rows and columns, and only depends on serializable core J2SE
 * classes (no proprietary vendor-supplied classes).
 *
 * <p>Note that this is not a complete implementation of the ResultSet
 * interface.  Methods marked as "Not supported" will generally throw
 * an UnsupportedOperationException or SQLException when you call
 * them.  This is preferable to just ignoring the call or returning a
 * dummy value because you will notice that you used an unimplemented
 * call, and can implement functionality that you need piece-by-piece.
 *
 * @author Jonathan Fuerth
 * @version $Id$
 */
public class CachedRowSet implements ResultSet, java.io.Serializable {

	private static final Logger logger = Logger.getLogger(CachedRowSet.class);

	/**
	 * The current row number in the result set.  Calling next() will
	 * increment this (if there are more rows to go).
	 *
	 * <p>The first row is 0, which is consistent with the java.util
	 * Collections clases and most other stuff, but is different from
	 * JDBC which uses 1-based indexing.
	 */
	private int rownum = -1;

	/**
	 * The data from the original result set.  One list item per row.
	 * Each row is of type Object[].  data.get(0)[0] would be the first
	 * row's first column; data.get(0)[1] would be the first row's second
	 * column, and so on.
	 */
	protected List data;

	/**
	 * The current row.  This gets updated by next().
	 */
	private Object[] curRow;

	/**
	 * The current column.  This gets set to -1 (invalid) in next(),
	 * and to the most recently requested column index in the getXXX()
	 * methods.  Used by wasNull().
	 */
	private int curCol = -1;

	/**
	 * Our cached copy of the original ResultSetMetaData.
	 */
	protected CachedResultSetMetaData rsmd;

	/**
	 * Makes an empty cached rowset.
	 */
	public CachedRowSet() throws SQLException {
	}

	/**
	 * Fills this row set with all the data of the given result set.
	 * After populating this row set, you can safely call rs.close().
	 */
	public void populate(ResultSet rs) throws SQLException {
		rsmd = new CachedResultSetMetaData(rs.getMetaData());
		ArrayList newData = new ArrayList();
		int colCount = rsmd.getColumnCount();
		while (rs.next()) {
			Object[] row = new Object[colCount];
			for (int i = 0; i < colCount; i++) {
				Object o = rs.getObject(i+1);
				if (o != null) {
					if (o instanceof BigDecimal) {	
						BigDecimal bd = (BigDecimal) o;
						if (bd.scale() > 0) {
							 o = new Double(bd.doubleValue()); 
						}
						else {
							 o = new Integer(bd.intValue());
						}
					}
				}				
				row[i] = o;
			}
			newData.add(row);
		}
		data = newData;
	}

	/**
	 * Creates a new instance of CachedRowSet that shares the same
	 * underlying data as this instance.  This method is useful for
	 * iterating over the same data in parallel, or in caching result
	 * sets for re-use.
	 */
	public CachedRowSet createShared() throws SQLException {
		return createSharedSorted(null);
	}

	/**
	 * Creates a new instance of CachedRowSet that shares the same
	 * underlying data as this instance, but that data will be
	 * returned in the order determined by the given sort qualifier.
	 */
	public CachedRowSet createSharedSorted(RowComparator c) throws SQLException {
		CachedRowSet newRowSet = new CachedRowSet();
		newRowSet.rsmd = rsmd;
		if (c != null) {
			logger.info("[34mCREATING NEW ARRAYLIST[0m");
			newRowSet.data = new ArrayList(data);
			Collections.sort(newRowSet.data, c);
		} else {
			newRowSet.data = data;
		}

		return newRowSet;
	}

	public static class RowComparator implements Comparator, java.io.Serializable {

		private ArrayList sortCols;

		/**
		 * Creates a RowComparator which returns rows in their natural
		 * order.
		 */
		public RowComparator() {
			sortCols = new ArrayList();
		}

		/**
		 * Adds a column to sort by (in ascending or descending
		 * order).  If you call this method multiple times, the order
		 * that columns are given in is sigificant: the first column
		 * given is the primary sort column, the second is the
		 * secondary, and so on.
		 */
		public void addSortColumn(int columnIndex, boolean ascending) {
			sortCols.add(new SortCol(columnIndex, ascending));
		}

		public int compare(Object row1, Object row2) {
			Object[] r1 = (Object[]) row1;
			Object[] r2 = (Object[]) row2;

			int diff = 0;

			Iterator it = sortCols.iterator();
			while (it.hasNext()) {
				SortCol sc = (SortCol) it.next();
				
				if (r1 == null && r2 == null) diff = 0;
				else if (r1 == null) diff = -1;
				else if (r2 == null) diff = 1;
				else diff = ((Comparable) r1[sc.columnIndex - 1]).compareTo(r2[sc.columnIndex - 1]);

				if (diff != 0) {
					if (sc.ascending) break;
					else { diff = 0 - diff; break; }
				}
			}

			return diff;
		}

		/**
		 * Returns true iff there are no sort columns specified in this comparator.
		 */
		public boolean isEmpty() {
			return sortCols.isEmpty();
		}

		private static class SortCol implements java.io.Serializable {
			public int columnIndex;
			public boolean ascending;

			public SortCol(int columnIndex, boolean ascending) {
				this.columnIndex = columnIndex;
				this.ascending = ascending;
			}
		}
	}

	/**
	 * Tells how many rows are in this row set.
	 */
	public int size() {
		return data.size();
	}

	/**
	 * Returns the list of rows in this result set.
	 */
	public List getData() {
		return data;
	}

	/**
	 * Returns the index of the column having the given name or -1 if
	 * there is no such column.  The comparison is case insensitive.
	 *
	 * <p>The findColumn in the ResultSet interface throws an
	 * exception if the column does not exist.
	 *
	 * @throws SQLException if the ResultSetMetaData operations result
	 * in a SQLException.  No exception will be thrown if the named
	 * column simply doesn't exist in the result set.
	 */
	public int getColumnIndex(String columnName) throws SQLException {
		int idx = -1;
		for (int i = 0; i < rsmd.getColumnCount(); i++) {
			if (rsmd.getColumnName(i + 1).equalsIgnoreCase(columnName)) {
				idx = i + 1;
				break;
			}
		}
		if (logger.isDebugEnabled()) {
			logger.debug("getColumnIndex("+columnName+") returns "+idx);
		}
		return idx;
	}

	// =============================================
	// RESULTSET INTERFACE IS BELOW THIS LINE
	// =============================================

	/**
	 * Does nothing, since a cached row set holds no SQL database
	 * resources.  If you want to free the memory used by the cached
	 * data in this row set, delete all references to this row set and
	 * it will be garbage collected like any other normal object.
	 */
    public void close() throws SQLException {
		// XXX: any point in setting data = null?
		return;
	}

	/**
	 * Returns true if the previous getXXX() method referenced a null
	 * value.  wasNull() is especially useful when working with the
	 * get methods for native java types (which can't be null).
	 */
    public boolean wasNull() throws SQLException {
		if (curRow == null || curCol < 0)
			throw new SQLException("You haven't accessed a value with a getXXX() method yet!");
		return curRow[curCol] == null;
	}
    
    //==============================================
    // RESULTS BY COLUMN INDEX
    //==============================================

	/**
	 * Returns the value in the current row at the given column index
	 * (the first column number is 1, not 0).
	 */
    public String getString(int columnIndex) throws SQLException {
		curCol = columnIndex - 1;
		if (curRow[columnIndex - 1] == null) {
			return null;
		} else {
			return curRow[columnIndex - 1].toString();
		}
	}

	/**
	 * Returns the value in the current row at the given column index
	 * (the first column number is 1, not 0).
	 */
    public boolean getBoolean(int columnIndex) throws SQLException {
		curCol = columnIndex - 1;
		if (curRow[columnIndex - 1] == null) {
			return false;
		} else {
			return ((Boolean) curRow[columnIndex - 1]).booleanValue();
		}
	}

	/**
	 * Returns the value in the current row at the given column index
	 * (the first column number is 1, not 0).
	 */
    public byte getByte(int columnIndex) throws SQLException {
		curCol = columnIndex - 1;
		if (curRow[columnIndex - 1] == null) {
			return (byte) 0;
		} else {
			return ((Number) curRow[columnIndex - 1]).byteValue();
		}
	}

	/**
	 * Returns the value in the current row at the given column index
	 * (the first column number is 1, not 0).
	 */
    public short getShort(int columnIndex) throws SQLException {
		curCol = columnIndex - 1;
		if (curRow[columnIndex - 1] == null) {
			return (short) 0;
		} else {
			return ((Number) curRow[columnIndex - 1]).shortValue();
		}
	}

	/**
	 * Returns the value in the current row at the given column index
	 * (the first column number is 1, not 0).
	 */
    public int getInt(int columnIndex) throws SQLException {
		curCol = columnIndex - 1;
		if (curRow[columnIndex - 1] == null) {
			return (int) 0;
		} else {
			return ((Number) curRow[columnIndex - 1]).intValue();
		}
	}

	/**
	 * Returns the value in the current row at the given column index
	 * (the first column number is 1, not 0).
	 */
    public long getLong(int columnIndex) throws SQLException {
		curCol = columnIndex - 1;
		if (curRow[columnIndex - 1] == null) {
			return (long) 0;
		} else {
			return ((Number) curRow[columnIndex - 1]).longValue();
		}
	}

	/**
	 * Returns the value in the current row at the given column index
	 * (the first column number is 1, not 0).
	 */
    public float getFloat(int columnIndex) throws SQLException {
		curCol = columnIndex - 1;
		if (curRow[columnIndex - 1] == null) {
			return (float) 0;
		} else {
			return ((Number) curRow[columnIndex - 1]).floatValue();
		}
	}

	/**
	 * Returns the value in the current row at the given column index
	 * (the first column number is 1, not 0).
	 */
    public double getDouble(int columnIndex) throws SQLException {
		curCol = columnIndex - 1;
		if (curRow[columnIndex - 1] == null) {
			return (double) 0;
		} else {
			return ((Number) curRow[columnIndex - 1]).doubleValue();
		}
	}

	/**
	 * Not supported.
	 */
    public BigDecimal getBigDecimal(int columnIndex, int scale) throws SQLException {
		throw new UnsupportedOperationException("This method is not supported by CachedRowSet");
	}

	/**
	 * Not supported.
	 */
    public byte[] getBytes(int columnIndex) throws SQLException {
		throw new UnsupportedOperationException("This method is not supported by CachedRowSet");
	}

	/**
	 * Returns the value in the current row at the given column index
	 * (the first column number is 1, not 0).
	 */
    public java.sql.Date getDate(int columnIndex) throws SQLException {
		curCol = columnIndex - 1;
		if (curRow[columnIndex - 1] == null) {
			return null;
		} else {
			return (java.sql.Date) curRow[columnIndex - 1];
		}
	}

	/**
	 * Returns the value in the current row at the given column index
	 * (the first column number is 1, not 0).
	 */
    public java.sql.Time getTime(int columnIndex) throws SQLException {
		curCol = columnIndex - 1;
		if (curRow[columnIndex - 1] == null) {
			return null;
		} else {
			return (java.sql.Time) curRow[columnIndex - 1];
		}
	}

	/**
	 * Returns the value in the current row at the given column index
	 * (the first column number is 1, not 0).
	 */
    public java.sql.Timestamp getTimestamp(int columnIndex) throws SQLException {
		curCol = columnIndex - 1;
		if (curRow[columnIndex - 1] == null) {
			return null;
		} else {
			return (java.sql.Timestamp) curRow[columnIndex - 1];
		}
	}

	/**
	 * Not supported.
	 */
    public java.io.InputStream getAsciiStream(int columnIndex) throws SQLException {
		throw new UnsupportedOperationException("This method is not supported by CachedRowSet");
	}

	/**
	 * Not supported.
	 */
    public java.io.InputStream getUnicodeStream(int columnIndex) throws SQLException {
		throw new UnsupportedOperationException("This method is not supported by CachedRowSet");
	}

	/**
	 * Not supported.
	 */
    public java.io.InputStream getBinaryStream(int columnIndex) throws SQLException {
		throw new UnsupportedOperationException("This method is not supported by CachedRowSet");
	}

	/**
	 * Returns the value in the current row at the given column index
	 * (the first column number is 1, not 0).
	 */
    public Object getObject(int columnIndex) throws SQLException {
		curCol = columnIndex - 1;
		return curRow[columnIndex - 1];
	}

    /**
	 * User-defined type maps aren't supported.
	 */
    public Object getObject(int i, java.util.Map map) throws SQLException {
		throw new UnsupportedOperationException
			("This CachedRowSet does not support user-defined type mappings.");	}

	/**
	 * Returns the value in the current row at the given column index
	 * (the first column number is 1, not 0).
	 */
    public Ref getRef(int i) throws SQLException {
		curCol = i - 1;
		return (Ref) curRow[i - 1];
	}

	/**
	 * Returns the value in the current row at the given column index
	 * (the first column number is 1, not 0).
	 */
    public Blob getBlob(int i) throws SQLException {
		curCol = i - 1;
		return (Blob) curRow[i - 1];
	}

	/**
	 * Returns the value in the current row at the given column index
	 * (the first column number is 1, not 0).
	 */
    public Clob getClob(int i) throws SQLException {
		curCol = i - 1;
		return (Clob) curRow[i - 1];
	}

	/**
	 * Returns the value in the current row at the given column index
	 * (the first column number is 1, not 0).
	 */
    public Array getArray(int i) throws SQLException {
		curCol = i - 1;
		return (Array) curRow[i - 1];
	}

    /**
	 * Works like getDate(int).  Ignores cal.
     */
    public java.sql.Date getDate(int columnIndex, Calendar cal) throws SQLException {
		return getDate(columnIndex);
	}

    /**
	 * Works like getTime(int).  Ignores cal.
     */
    public java.sql.Time getTime(int columnIndex, Calendar cal) throws SQLException {
		return getTime(columnIndex);
	}

    /**
	 * Works like getTimestamp(int).  Ignores cal.
     */
    public java.sql.Timestamp getTimestamp(int columnIndex, Calendar cal) throws SQLException {
		return getTimestamp(columnIndex);
	}

	/**
	 * Tries to convert the string representation of the value at
	 * columnIndex in the current row to a URL object.  If successful,
	 * the URL is returned.
	 */
    public java.net.URL getURL(int columnIndex) throws SQLException {
		curCol = columnIndex - 1;
		if (curRow[curCol] == null) {
			return null;
		} else if (curRow[curCol] instanceof java.net.URL) {
			return (java.net.URL) curRow[curCol];
		} else try {
			return new java.net.URL(getString(columnIndex));
		} catch (java.net.MalformedURLException e) {
			throw new SQLException("Not a URL: <"+getString(columnIndex)+">");
		}
	}

    //==========================================
    // RESULTS BY COLUMN NAME
    //==========================================

	/**
	 * See {@link #getString(int)}.
	 */
    public String getString(String columnName) throws SQLException {
		return getString(findColumn(columnName));
	}

	/**
	 * See {@link #getBoolean(int)}.
	 */
    public boolean getBoolean(String columnName) throws SQLException {
		return getBoolean(findColumn(columnName));
	}

	/**
	 * See {@link #getByte(int)}.
	 */
    public byte getByte(String columnName) throws SQLException {
		return getByte(findColumn(columnName));
	}

	/**
	 * See {@link #getShort(int)}.
	 */
    public short getShort(String columnName) throws SQLException {
		return getShort(findColumn(columnName));
	}

	/**
	 * See {@link #getInt(int)}.
	 */
    public int getInt(String columnName) throws SQLException {
		return getInt(findColumn(columnName));
	}

	/**
	 * See {@link #getLong(int)}.
	 */
    public long getLong(String columnName) throws SQLException {
		return getLong(findColumn(columnName));
	}

	/**
	 * See {@link #getFloat(int)}.
	 */
    public float getFloat(String columnName) throws SQLException {
		return getFloat(findColumn(columnName));
	}

	/**
	 * See {@link #getDouble(int)}.
	 */
    public double getDouble(String columnName) throws SQLException {
		return getDouble(findColumn(columnName));
	}

	/**
	 * See {@link #getBigDecimal(int)}.
	 */
    public BigDecimal getBigDecimal(String columnName, int scale) throws SQLException {
		return getBigDecimal(findColumn(columnName));
	}

	/**
	 * See {@link #getBytes(int)}.
	 */
    public byte[] getBytes(String columnName) throws SQLException {
		return getBytes(findColumn(columnName));
	}

	/**
	 * See {@link #getDate(int)}.
	 */
    public java.sql.Date getDate(String columnName) throws SQLException {
		return getDate(findColumn(columnName));
	}

	/**
	 * See {@link #getTime(int)}.
	 */
    public java.sql.Time getTime(String columnName) throws SQLException {
		return getTime(findColumn(columnName));
	}

	/**
	 * See {@link #getTimestamp(int)}.
	 */
    public java.sql.Timestamp getTimestamp(String columnName) throws SQLException {
		return getTimestamp(findColumn(columnName));
	}

	/**
	 * See {@link #getAsciiStream(int)}.
	 */
    public java.io.InputStream getAsciiStream(String columnName) throws SQLException {
		return getAsciiStream(findColumn(columnName));
	}

	/**
	 * See {@link #getUnicodeStream(int)}.
	 */
    public java.io.InputStream getUnicodeStream(String columnName) throws SQLException {
		return getUnicodeStream(findColumn(columnName));
	}

	/**
	 * See {@link #getBinaryStream(int)}.
	 */
    public java.io.InputStream getBinaryStream(String columnName) throws SQLException {
		return getBinaryStream(findColumn(columnName));
	}

	/**
	 * See {@link #getObject(int)}.
	 */
    public Object getObject(String columnName) throws SQLException {
		return getObject(findColumn(columnName));
	}

    /**
	 * User-defined type maps aren't supported.
	 */
    public Object getObject(String colName, java.util.Map map) throws SQLException {
		return getObject(findColumn(colName), map);
	}

	/**
	 * See {@link #getRef(int)}.
	 */
    public Ref getRef(String colName) throws SQLException {
		return getRef(findColumn(colName));
	}

	/**
	 * See {@link #getBlob(int)}.
	 */
    public Blob getBlob(String colName) throws SQLException {
		return getBlob(findColumn(colName));
	}

	/**
	 * See {@link #getClob(int)}.
	 */
    public Clob getClob(String colName) throws SQLException {
		return getClob(findColumn(colName));
	}

	/**
	 * See {@link #getArray(int)}.
	 */
    public Array getArray(String colName) throws SQLException {
		return getArray(findColumn(colName));
	}

    /**
	 * Works like {@link #getDate(String)}.  Ignores cal.
     */
    public java.sql.Date getDate(String columnName, Calendar cal) throws SQLException {
		return getDate(columnName);
	}

    /**
	 * Works like {@link #getTime(String)}.  Ignores cal.
     */
    public java.sql.Time getTime(String columnName, Calendar cal) throws SQLException {
		return getTime(columnName);
	}

    /**
	 * Works like {@link #getTimestamp(String)}.  Ignores cal.
     */
    public java.sql.Timestamp getTimestamp(String columnName, Calendar cal) throws SQLException {
		return getTimestamp(columnName);
	}

	/**
	 * See {@link #net(int)}.
	 */
    public java.net.URL getURL(String columnName) throws SQLException {
		return getURL(findColumn(columnName));
	}

    // ====================================
    // META DATA
    // ====================================

	/**
	 * Returns our cached copy of the original ResultSet's meta data.
	 */
    public ResultSetMetaData getMetaData() throws SQLException {
		return rsmd;
	}

	/**
	 * Returns the index of the named column.  Column name matches are
	 * case insensitive.
	 *
	 * @throws SQLException if there is no such column.
	 */
    public int findColumn(String columnName) throws SQLException {
		int index = getColumnIndex(columnName);
		if (index == -1) {
			throw new SQLException("No such column '"+columnName+"' in this result set.");
		} else {
			return index;
		}
	}

    // ====================================
    // POSITIONING
    // ====================================

	/**
	 * Tells whether or not the current row pointer is before the
	 * first row.  If so, that means calling next() will put the
	 * cursor on the first row.
	 */
    public boolean isBeforeFirst() throws SQLException {
		if (data == null) throw new SQLException("This CachedRowSet is not populated yet");
		return rownum == -1;
	}
      
	/**
	 * Tells whether or not the current row pointer is after the last
	 * row.  If so, that means calling next() will return false and
	 * leave the cursor on an invalid row.
	 */
	public boolean isAfterLast() throws SQLException {
		if (data == null) throw new SQLException("This CachedRowSet is not populated yet");
		return rownum >= data.size();
	}

 	/**
	 * Tells whether or not the current row pointer is on the first
	 * row.
	 */
    public boolean isFirst() throws SQLException {
		if (data == null) throw new SQLException("This CachedRowSet is not populated yet");
		return rownum == 0;
	}
 
 	/**
	 * Tells whether or not the current row pointer is on the last
	 * row.
	 */
    public boolean isLast() throws SQLException {
		if (data == null) throw new SQLException("This CachedRowSet is not populated yet");
		return rownum == (data.size() - 1);
	}

	/**
	 * Repositions the cursor to its default position.  Same as
	 * calling absolute(0).
	 */ 
    public void beforeFirst() throws SQLException {
		absolute(0);
	}

	/**
	 * Repositions the cursor just past the last row.  Same as
	 * calling absolute(size()).
	 */ 
    public void afterLast() throws SQLException {
		absolute(data.size());
	}

	/**
	 * Repositions the cursor to the first row.  Same as calling
	 * absolute(1).
	 */ 
    public boolean first() throws SQLException {
		return absolute(1);
	}

	/**
	 * Repositions the cursor to the last row.  Same as calling
	 * absolute(-1).
	 */ 
	public boolean last() throws SQLException {
		return absolute(-1);
	}

	/**
	 * Returns the current row number that the cursor points to.  1 is
	 * the first row, 0 is before the first row.
	 */ 
    public int getRow() throws SQLException {
		if (rownum < 0) return 0;
		else return rownum + 1;
	}

	/**
	 * Moves the current row pointer to the given position.
	 *
     * @param row the number of the row to which the cursor should
     * move.  A positive number indicates the row number counting from
     * the beginning of the result set; a negative number indicates
     * the row number counting from the end of the result set
     * @return <code>true</code> if the reposition operation left the
     * cursor on a valid row (the getXXX() methods will work).
     */
    public boolean absolute(int row) throws SQLException {
		if (data == null) throw new SQLException("This CachedRowSet is not populated yet");

		curCol = -1;
		curRow = null;

		// adjust row to be a 0-based index from beginning of data set
		if (row < 0) {
			rownum = data.size() + row;
		} else {
			rownum = row - 1;
		}

		// same as beforeFirst()
		if (rownum < 0) {
			rownum = -1;
			return false;
		}

		// same as afterLast()
		if (rownum >= data.size()) {
			rownum = data.size();
			return false;
		}

		// now do the positioning
		if (data.size() > 0) {
			curRow = (Object[]) data.get(rownum);
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Increments the current row pointer.  This method is the
	 * "normal" way of iterating through the result set, but it is
	 * equivalent to calling relative(1).
	 */
    public boolean next() throws SQLException {
		return relative(1);
	}

	/**
	 * Decrements the current row pointer.  This method is equivalent
	 * to calling relative(-1).
	 */
    public boolean previous() throws SQLException {
		return relative(-1);
	}

    /**
     * Moves the current row pointer ahead by the given number of rows
     * (actually moves it back if rows is negative).  An argument of
     * rows = 0 does not move the current row pointer, but does reset
     * the current column pointer, which means wasNull() will not work
     * until you call getXXX() again.
	 *
	 * <p>If the specified offset moves the cursor outside the data
	 * set, this method is like beforeFirst() or afterLast() depending
	 * on which side of the data set you said to move to.
     *
     * @param rows the number of rows to move from the current row.
     * Positive means to move the cursor forward; negative means to
     * move the cursor backward.
     * @return <code>true</code> if the cursor is left on a valid row;
     * <code>false</code> otherwise.
     */
    public boolean relative(int rows) throws SQLException {
		if (data == null) throw new SQLException("This CachedRowSet is not populated yet.");

		curCol = -1;
		curRow = null;

		rownum += rows;

		if (rownum < 0) {
			rownum = -1;
			return false;
		}

		if (rownum >= data.size()) {
			rownum = data.size();
			return false;
		}

		curRow = (Object[]) data.get(rownum);
		return true;
	}

	/**
	 * Not supported.
	 *
	 * @throws UnsupportedOperationException if direction != FETCH_FORWARD.
	 */
    public void setFetchDirection(int direction) throws SQLException {
		if (direction != FETCH_FORWARD) {
			throw new UnsupportedOperationException("Fetch direction is always FORWARD.");
		}
	}

	/**
	 * Always returns FETCH_FORWARD.
	 */
    public int getFetchDirection() throws SQLException {
		return FETCH_FORWARD;
	}
	
    // ====================================
	// MISC UNSUPPORTED STUFF
    // ====================================

	/**
	 * Not supported.
	 */
    public Statement getStatement() throws SQLException {
		throw new UnsupportedOperationException("This CachedRowSet does not have a reference to the Statement object.");
	}

	/**
	 * Not supported.
	 */
    public SQLWarning getWarnings() throws SQLException {
		throw new UnsupportedOperationException("This method is not supported by CachedRowSet");
	}

	/**
	 * Not supported.
	 */
    public void clearWarnings() throws SQLException {
		throw new UnsupportedOperationException("This method is not supported by CachedRowSet");
	}

	/**
	 * Not supported.
	 */
    public String getCursorName() throws SQLException {
		throw new SQLException("Updates are not supported by CachedRowSet");
	}

	/**
	 * Not supported.
	 */
    public java.io.Reader getCharacterStream(int columnIndex) throws SQLException {
		throw new UnsupportedOperationException("This method is not supported by CachedRowSet");
	}

	/**
	 * Not supported.
	 */
    public java.io.Reader getCharacterStream(String columnName) throws SQLException {
		throw new UnsupportedOperationException("This method is not supported by CachedRowSet");
	}

	/**
	 * Not supported.
	 */
    public BigDecimal getBigDecimal(int columnIndex) throws SQLException {
		throw new UnsupportedOperationException("This method is not supported by CachedRowSet");
	}

	/**
	 * Not supported.
	 */
    public BigDecimal getBigDecimal(String columnName) throws SQLException {
		throw new UnsupportedOperationException("This method is not supported by CachedRowSet");
	}

	/**
	 * This setting is not important for a cached row set.  It will
	 * not throw an exception, but calling it has no effect.
	 */
    public void setFetchSize(int rows) throws SQLException {
		// doesn't matter
	}

	/**
	 * This setting is not important for a cached row set.
	 *
	 * @return The number of rows in this result set.
	 */
    public int getFetchSize() throws SQLException {
		return data.size();
	}

	/**
	 * This is a cached row set, so it always returns the same type:
	 * TYPE_SCROLL_INSENSITIVE.
	 *
	 * @return TYPE_SCROLL_INSENSITIVE
	 */
    public int getType() throws SQLException {
		return TYPE_SCROLL_INSENSITIVE;
	}

	/**
	 * This is a cached row set that will not accept changes.  Always
	 * returns CONCUR_READ_ONLY.
	 *
	 * @return CONCUR_READ_ONLY
	 */
    public int getConcurrency() throws SQLException {
		return CONCUR_READ_ONLY;
	}

    // ====================================
    // UPDATES BY INDEX (NOT SUPPORTED)
    // ====================================

	/**
	 * Returns false (updates are not supported).
	 */
    public boolean rowUpdated() throws SQLException {
		return false;
	}

	/**
	 * Returns false (updates are not supported).
	 */
    public boolean rowInserted() throws SQLException {
		return false;
	}
   
	/**
	 * Returns false (updates are not supported).
	 */
    public boolean rowDeleted() throws SQLException {
		return false;
	}

	/**
	 * Not supported.
	 */
    public void updateNull(int columnIndex) throws SQLException {
		throw new UnsupportedOperationException("Updates not supported!");
	}

	/**
	 * Not supported.
	 */
    public void updateBoolean(int columnIndex, boolean x) throws SQLException {
		throw new UnsupportedOperationException("Updates not supported!");
	}

	/**
	 * Not supported.
	 */
    public void updateByte(int columnIndex, byte x) throws SQLException {
		throw new UnsupportedOperationException("Updates not supported!");
	}

	/**
	 * Not supported.
	 */
    public void updateShort(int columnIndex, short x) throws SQLException {
		throw new UnsupportedOperationException("Updates not supported!");
	}

	/**
	 * Not supported.
	 */
    public void updateInt(int columnIndex, int x) throws SQLException {
		throw new UnsupportedOperationException("Updates not supported!");
	}

	/**
	 * Not supported.
	 */
    public void updateLong(int columnIndex, long x) throws SQLException {
		throw new UnsupportedOperationException("Updates not supported!");
	}

	/**
	 * Not supported.
	 */
    public void updateFloat(int columnIndex, float x) throws SQLException {
		throw new UnsupportedOperationException("Updates not supported!");
	}

	/**
	 * Not supported.
	 */
    public void updateDouble(int columnIndex, double x) throws SQLException {
		throw new UnsupportedOperationException("Updates not supported!");
	}

	/**
	 * Not supported.
	 */
    public void updateBigDecimal(int columnIndex, BigDecimal x) throws SQLException {
		throw new UnsupportedOperationException("Updates not supported!");
	}

	/**
	 * Not supported.
	 */
    public void updateString(int columnIndex, String x) throws SQLException {
		throw new UnsupportedOperationException("Updates not supported!");
	}

	/**
	 * Not supported.
	 */
    public void updateBytes(int columnIndex, byte x[]) throws SQLException {
		throw new UnsupportedOperationException("Updates not supported!");
	}

	/**
	 * Not supported.
	 */
    public void updateDate(int columnIndex, java.sql.Date x) throws SQLException {
		throw new UnsupportedOperationException("Updates not supported!");
	}

	/**
	 * Not supported.
	 */
    public void updateTime(int columnIndex, java.sql.Time x) throws SQLException {
		throw new UnsupportedOperationException("Updates not supported!");
	}

	/**
	 * Not supported.
	 */
    public void updateTimestamp(int columnIndex, java.sql.Timestamp x)
		throws SQLException {
		throw new UnsupportedOperationException("Updates not supported!");
	}

	/**
	 * Not supported.
	 */
    public void updateAsciiStream(int columnIndex, java.io.InputStream x, int length)
		throws SQLException {
		throw new UnsupportedOperationException("Updates not supported!");
	}

	/**
	 * Not supported.
	 */
    public void updateBinaryStream(int columnIndex, java.io.InputStream x,	int length)
		throws SQLException {
		throw new UnsupportedOperationException("Updates not supported!");
	}

	/**
	 * Not supported.
	 */
    public void updateCharacterStream(int columnIndex, java.io.Reader x, int length)
		throws SQLException {
		throw new UnsupportedOperationException("Updates not supported!");
	}

	/**
	 * Not supported.
	 */
    public void updateObject(int columnIndex, Object x, int scale) throws SQLException {
		throw new UnsupportedOperationException("Updates not supported!");
	}

	/**
	 * Not supported.
	 */
    public void updateObject(int columnIndex, Object x) throws SQLException {
		throw new UnsupportedOperationException("Updates not supported!");
	}

	/**
	 * Not supported.
	 */
    public void updateArray(int columnIndex, java.sql.Array x) throws SQLException {
		throw new UnsupportedOperationException("Updates not supported!");
	}

	/**
	 * Not supported.
	 */
	public void updateClob(int columnIndex, java.sql.Clob x) throws SQLException {
		throw new UnsupportedOperationException("Updates not supported!");
	}

	/**
	 * Not supported.
	 */
	public void updateBlob(int columnIndex, java.sql.Blob x) throws SQLException {
		throw new UnsupportedOperationException("Updates not supported!");
	}

	/**
	 * Not supported.
	 */
    public void updateRef(int columnIndex, java.sql.Ref x) throws SQLException {
		throw new UnsupportedOperationException("Updates not supported!");
	}
	
    // ====================================
    // UPDATES BY NAME (FORWARD TO UPDATE-BY-INDEX)
    // ====================================

	/**
	 * Not supported.
	 */
    public void updateNull(String columnName) throws SQLException {
		updateNull(findColumn(columnName));
	}

	/**
	 * Not supported.
	 */
    public void updateBoolean(String columnName, boolean x) throws SQLException {
		updateBoolean(findColumn(columnName), x);
	}

	/**
	 * Not supported.
	 */
    public void updateByte(String columnName, byte x) throws SQLException {
		updateByte(findColumn(columnName), x);
	}

	/**
	 * Not supported.
	 */
    public void updateShort(String columnName, short x) throws SQLException {
		updateShort(findColumn(columnName), x);
	}

	/**
	 * Not supported.
	 */
    public void updateInt(String columnName, int x) throws SQLException {
		updateInt(findColumn(columnName), x);
	}

	/**
	 * Not supported.
	 */
    public void updateLong(String columnName, long x) throws SQLException {
		updateLong(findColumn(columnName), x);
	}

	/**
	 * Not supported.
	 */
    public void updateFloat(String columnName, float x) throws SQLException {
		updateFloat(findColumn(columnName), x);
	}

	/**
	 * Not supported.
	 */
    public void updateDouble(String columnName, double x) throws SQLException {
		updateDouble(findColumn(columnName), x);
	}

	/**
	 * Not supported.
	 */
    public void updateBigDecimal(String columnName, BigDecimal x) throws SQLException {
		updateBigDecimal(findColumn(columnName), x);
	}

	/**
	 * Not supported.
	 */
    public void updateString(String columnName, String x) throws SQLException {
		updateString(findColumn(columnName), x);
	}

	/**
	 * Not supported.
	 */
    public void updateBytes(String columnName, byte x[]) throws SQLException {
		updateBytes(findColumn(columnName), x);
	}

	/**
	 * Not supported.
	 */
    public void updateDate(String columnName, java.sql.Date x) throws SQLException {
		updateDate(findColumn(columnName), x);
	}

	/**
	 * Not supported.
	 */
    public void updateTime(String columnName, java.sql.Time x) throws SQLException {
		updateTime(findColumn(columnName), x);
	}

	/**
	 * Not supported.
	 */
    public void updateTimestamp(String columnName, java.sql.Timestamp x) throws SQLException {
		updateTimestamp(findColumn(columnName), x);
	}

	/**
	 * Not supported.
	 */
    public void updateAsciiStream(String columnName, java.io.InputStream x, int length) throws SQLException {
		updateAsciiStream(findColumn(columnName), x, length);
	}

	/**
	 * Not supported.
	 */
    public void updateBinaryStream(String columnName, java.io.InputStream x, int length) throws SQLException {
		updateBinaryStream(findColumn(columnName), x, length);
	}

	/**
	 * Not supported.
	 */
    public void updateCharacterStream(String columnName, java.io.Reader reader, int length) throws SQLException {
		updateCharacterStream(findColumn(columnName), reader, length);
	}

	/**
	 * Not supported.
	 */
    public void updateObject(String columnName, Object x, int scale) throws SQLException {
		updateObject(findColumn(columnName), x, scale);
	}

	/**
	 * Not supported.
	 */
    public void updateObject(String columnName, Object x) throws SQLException {
		updateObject(findColumn(columnName), x);
	}

	/**
	 * Not supported.
	 */
    public void updateRef(String columnName, java.sql.Ref x) throws SQLException {
		updateRef(findColumn(columnName), x);
	}

	/**
	 * Not supported.
	 */
    public void updateBlob(String columnName, java.sql.Blob x) throws SQLException {
		updateBlob(findColumn(columnName), x);
	}

	/**
	 * Not supported.
	 */
    public void updateClob(String columnName, java.sql.Clob x) throws SQLException {
		updateClob(findColumn(columnName), x);
	}

	/**
	 * Not supported.
	 */
    public void updateArray(String columnName, java.sql.Array x) throws SQLException {
		updateArray(findColumn(columnName), x);
	}
	
    // ====================================
    // UPDATE COMMIT METHODS (NOT SUPPORTED)
    // ====================================

	/**
	 * Not supported.
	 */
    public void insertRow() throws SQLException {
		throw new UnsupportedOperationException("Updates not supported!");
	}

	/**
	 * Not supported.
	 */
    public void updateRow() throws SQLException {
		throw new UnsupportedOperationException("Updates not supported!");
	}

	/**
	 * Not supported.
	 */
    public void deleteRow() throws SQLException {
		throw new UnsupportedOperationException("Updates not supported!");
	}

	/**
	 * Not supported.
	 */
    public void refreshRow() throws SQLException {
		throw new UnsupportedOperationException("Updates not supported!");
	}

	/**
	 * Not supported.
	 */
	public void cancelRowUpdates() throws SQLException {
		throw new UnsupportedOperationException("Updates not supported!");
	}

	/**
	 * Not supported.
	 */
    public void moveToInsertRow() throws SQLException {
		throw new UnsupportedOperationException("Updates not supported!");
	}

	/**
	 * Not supported.
	 */
    public void moveToCurrentRow() throws SQLException {
		throw new UnsupportedOperationException("Updates not supported!");
	}
}
