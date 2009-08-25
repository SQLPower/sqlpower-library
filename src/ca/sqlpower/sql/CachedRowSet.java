package ca.sqlpower.sql;

import java.math.BigDecimal;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.DatabaseMetaData;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

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

	private static final int BEFORE_FIRST_ROW = -1;
    private static final int INSERT_ROW = -2;

	/**
	 * The current row number in the result set.  Calling next() will
	 * increment this (if there are more rows to go).
	 *
	 * <p>The first row is 0, which is consistent with the java.util
	 * Collections clases and most other stuff, but is different from
	 * JDBC which uses 1-based indexing.
	 */
	private int rownum = BEFORE_FIRST_ROW;

	/**
	 * The data from the original result set.  One list item per row.
	 * Each row is of type Object[].  data.get(0)[0] would be the first
	 * row's first column; data.get(0)[1] would be the first row's second
	 * column, and so on.
	 */
	protected List<Object[]> data;

	/**
	 * The current row.  This gets updated by next().
	 */
	protected Object[] curRow;

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
        // Nothing to do
	}

	/**
	 * This is the "populate" method for streaming result sets. This method will
	 * update the cached row set with a new row every time one is inserted into
	 * the result set and notify the listener of the new row. Only rowLimit rows
	 * will be stored in the cached row set and when the row limit is reached
	 * the oldest row in the cached row set will be removed.
	 * 
	 * @param rs
	 *            The result set to track.
	 * @param listener
	 *            A listener that will be notified that a new row was added to
	 *            the cached row set. More listeners can be added from the
	 *            {@link CachedRowSet#addRowSetChangeListener(RowSetChangeListener)}
	 *            method.
	 * @param rowLimit
	 *            The maximum number of rows this cached row set will store. Old
	 *            rows will be removed when necessary to make room for new rows.
	 * @throws SQLException 
	 */
	public void follow(ResultSet rs, RowSetChangeListener listener, int rowLimit, String ... extraColNames) throws SQLException {
		/*
		 * XXX: this upcases all the column names in the metadata for
		 * the Dashboard's benefit.  We should add a switch for this
		 * behaviour to the CachedRowSet API and then use it from the
		 * Dashboard
		 */
		rsmd = new CachedResultSetMetaData(rs.getMetaData(), true);
    	int rsColCount = rsmd.getColumnCount();
    	int colCount = rsColCount + extraColNames.length;
		for (String extraColName : extraColNames) {
			/* bleh */
			rsmd.addColumn(
					false, false, false, false, DatabaseMetaData.columnNullable,
					true, 10, extraColName,	extraColName, null, 10, 0, null,
					null, Types.VARCHAR, "VARCHAR", false, true, true,
					String.class.getName());
		}

		int rowNum = 0;
		data = new ArrayList<Object[]>();
		while (rs.next()) {
		    if (logger.isDebugEnabled()) logger.debug("Populating Row "+rowNum);
			Object[] row = new Object[colCount];
			for (int i = 0; i < rsColCount; i++) {
				Object o = rs.getObject(i+1);
				if (o == null) {
				    if (logger.isDebugEnabled()) logger.debug("   Col "+i+": null");
				} else {
				    if (logger.isDebugEnabled()) logger.debug("   Col "+i+": "+o+" ("+o.getClass()+")");
				}				
				row[i] = o;
			}
            
			data.add(row);
			
			while (data.size() > rowLimit) { 
				data.remove(0);
			}
			
			if (listener != null) {
				listener.rowAdded(new RowSetChangeEvent(this, row, rowNum));
			}
			rowNum++;
		}
	}

    /**
     * Fills this row set with all the data of the given result set.
     * After populating this row set, you can safely call rs.close().
     */
    public void populate(ResultSet rs) throws SQLException {
        populate(rs, null);
    }
    
    /**
     * Fills this row set with all the data of the given result set
     * which is accepted by the given row filter.
     * After populating this row set, you can safely call rs.close().
     * 
     * @param rs The result set to read all data from
     * @param filter the filter to consult about which rows to keep
     */
    public void populate(ResultSet rs, RowFilter filter) throws SQLException {
    	populate(rs, filter, new String[0]);
    }
    
    /**
     * Fills this row set with all the data of the given result set
     * which is accepted by the given row filter.
     * After populating this row set, you can safely call rs.close().
     * 
     * @param rs The result set to read all data from
     * @param filter the filter to consult about which rows to keep
     * @param extraColNames The names of any extra placeholder columns
     * you want to add to the copy of the given result set. The result set
     * metadata for those columns will claim they are all VARCHAR columns,
     * but you can put any type of data in them. After populate() has returned,
     * all rows will contain null values for the extra columns.
     */
    public void populate(ResultSet rs, RowFilter filter, String ... extraColNames) throws SQLException {

    	/*
		 * XXX: this upcases all the column names in the metadata for
		 * the Dashboard's benefit.  We should add a switch for this
		 * behaviour to the CachedRowSet API and then use it from the
		 * Dashboard
		 */
		rsmd = new CachedResultSetMetaData(rs.getMetaData(), true);
    	int rsColCount = rsmd.getColumnCount();
    	int colCount = rsColCount + extraColNames.length;
		for (String extraColName : extraColNames) {
			/* bleh */
			rsmd.addColumn(
					false, false, false, false, DatabaseMetaData.columnNullable,
					true, 10, extraColName,	extraColName, null, 10, 0, null,
					null, Types.VARCHAR, "VARCHAR", false, true, true,
					String.class.getName());
		}

		int rowNum = 0;
		data = new ArrayList<Object[]>();
		while (rs.next()) {
		    if (logger.isDebugEnabled()) logger.debug("Populating Row "+rowNum);
			Object[] row = new Object[colCount];
			for (int i = 0; i < rsColCount; i++) {
				Object o = rs.getObject(i+1);
				if (o == null) {
				    if (logger.isDebugEnabled()) logger.debug("   Col "+i+": null");
				} else {
				    if (logger.isDebugEnabled()) logger.debug("   Col "+i+": "+o+" ("+o.getClass()+")");
				}				
				row[i] = o;
			}
            
            if (filter == null || filter.acceptsRow(row)) {
                data.add(row);
                rowNum++;
            } else {
                logger.debug("Skipped this row (rejected by filter)");
            }
		}
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
			logger.debug("CREATING NEW ARRAYLIST");
			newRowSet.data = new ArrayList<Object[]>(data);
			Collections.sort(newRowSet.data, c);
		} else {
			newRowSet.data = data;
		}

		return newRowSet;
	}

	public static class RowComparator implements Comparator<Object[]>, java.io.Serializable {

		private ArrayList<SortCol> sortCols;

		/**
		 * Creates a RowComparator which returns rows in their natural
		 * order.
		 */
		public RowComparator() {
			sortCols = new ArrayList<SortCol>();
		}

		/**
		 * Adds a column to sort by (in ascending or descending
		 * order).  If you call this method multiple times, the order
		 * that columns are given in is significant: the first column
		 * given is the primary sort column, the second is the
		 * secondary, and so on.
		 */
		public void addSortColumn(int columnIndex, boolean ascending) {
			sortCols.add(new SortCol(columnIndex, ascending));
		}

		@SuppressWarnings("unchecked")
        public int compare(Object[] r1, Object[] r2) {
//			Object[] r1 = (Object[]) row1;
//			Object[] r2 = (Object[]) row2;

			int diff = 0;

			for (SortCol sc : sortCols) {
				if (r1 == null && r2 == null) diff = 0;
				else if (r1 == null) diff = -1;
				else if (r2 == null) diff = 1;
				else if (r1[sc.columnIndex - 1] instanceof Number && r2[sc.columnIndex - 1] instanceof Number) {
					double d1 = ((Number) r1[sc.columnIndex - 1]).doubleValue();
					double d2 = ((Number) r2[sc.columnIndex - 1]).doubleValue();  // see threepio
					if (d1 < d2) diff = -1;
					else if (d1 > d2) diff = 1;
					else diff = 0;
				} else if (r1[sc.columnIndex - 1] instanceof String && r2[sc.columnIndex - 1] instanceof String) {
					String s1 = ((String) r1[sc.columnIndex - 1]);
					String s2 = ((String) r2[sc.columnIndex - 1]);
					diff = s1.compareToIgnoreCase(s2);
				} else if (r1[sc.columnIndex - 1] instanceof Comparable && r2[sc.columnIndex - 1] instanceof Comparable) {
				    Comparable c1 = (Comparable) r1[sc.columnIndex - 1];
				    Comparable c2 = (Comparable) r2[sc.columnIndex - 1];
				    
				    //This may throw an exception if c1 and c2 are not of mutually comparable types.
				    //That would mean the same column contains two different types of objects
				    //that cannot be compared to each other, which we think would be a fault in the JDBC driver.
				    diff = c1.compareTo(c2); 
				} else {
				    Object r1ColVal = r1[sc.columnIndex - 1];
                    Object r2ColVal = r2[sc.columnIndex - 1];
				    if (r1ColVal == null && r2ColVal == null) diff = 0;
				    else if (r1ColVal == null) diff = -1;
				    else if (r2ColVal == null) diff = 1;
				    else diff = 0; // relying on stability of MergeSort to keep rows in order database returned them in
				    
				}

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
	public List<Object[]> getData() {
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
			java.util.Date uDate = (java.util.Date) curRow[columnIndex - 1];
			return new java.sql.Date (uDate.getTime());
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
    public Object getObject(int i, java.util.Map<String, Class<?>> map) throws SQLException {
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
    public Object getObject(String colName, java.util.Map<String, Class<?>> map) throws SQLException {
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
		return rownum == BEFORE_FIRST_ROW;
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
			rownum = BEFORE_FIRST_ROW;
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
			rownum = BEFORE_FIRST_ROW;
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
	 * Returns the value in the current row at the given column index
	 * (the first column number is 1, not 0). If the value retrieved at
	 * the index is not a BigDecimal but can be converted to one it will
	 * be converted and returned in a BigDecimal.
	 */
    public BigDecimal getBigDecimal(int columnIndex) throws SQLException {
		curCol = columnIndex - 1;
		Object curColObject = curRow[columnIndex - 1];
		if (curColObject == null) {
			return new BigDecimal(0);
		} else {
			if (curColObject instanceof BigDecimal) {
				return (BigDecimal) curRow[columnIndex - 1];
			} else if (curColObject instanceof Number) {
				return new BigDecimal(String.valueOf(curColObject));
			} else {
				throw new SQLException("Could not convert column " + columnIndex + " of type " + curColObject.getClass() + " to BigDecimal.");
			}
		}
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
	 * Returns false, even if the row has been modified.
	 */
    public boolean rowUpdated() throws SQLException {
		return false;
	}

	/**
	 * Returns false (inserts and deletes are not supported).
	 */
    public boolean rowInserted() throws SQLException {
		return false;
	}
   
	/**
     * Returns false (inserts and deletes are not supported).
	 */
    public boolean rowDeleted() throws SQLException {
		return false;
	}

    /**
     * Assigns the columnIndex'th item in the current row to null.
     * There is no automatic way to propogate this change to the database, but
     * the change will remain in memory for the life of this CachedRowSet.
     */
    public void updateNull(int columnIndex) throws SQLException {
        curRow[columnIndex - 1] = null;
	}

    /**
     * Assigns the columnIndex'th item in the current row to the given object.
     * There is no automatic way to propogate this change to the database, but
     * the change will remain in memory for the life of this CachedRowSet.
     */
    public void updateBoolean(int columnIndex, boolean x) throws SQLException {
        curRow[columnIndex - 1] = (x ? Boolean.TRUE : Boolean.FALSE);
	}

    /**
     * Assigns the columnIndex'th item in the current row to the given value.
     * There is no automatic way to propogate this change to the database, but
     * the change will remain in memory for the life of this CachedRowSet.
     */
    public void updateByte(int columnIndex, byte x) throws SQLException {
        curRow[columnIndex - 1] = BigDecimal.valueOf(x);
	}

    /**
     * Assigns the columnIndex'th item in the current row to the given value.
     * There is no automatic way to propogate this change to the database, but
     * the change will remain in memory for the life of this CachedRowSet.
     */
    public void updateShort(int columnIndex, short x) throws SQLException {
        curRow[columnIndex - 1] = BigDecimal.valueOf(x);
	}

    /**
     * Assigns the columnIndex'th item in the current row to the given value.
     * There is no automatic way to propogate this change to the database, but
     * the change will remain in memory for the life of this CachedRowSet.
     */
    public void updateInt(int columnIndex, int x) throws SQLException {
		curRow[columnIndex - 1] = BigDecimal.valueOf(x);
	}

    /**
     * Assigns the columnIndex'th item in the current row to the given value.
     * There is no automatic way to propogate this change to the database, but
     * the change will remain in memory for the life of this CachedRowSet.
     */
    public void updateLong(int columnIndex, long x) throws SQLException {
        curRow[columnIndex - 1] = BigDecimal.valueOf(x);
	}

    /**
     * Assigns the columnIndex'th item in the current row to the given value.
     * There is no automatic way to propogate this change to the database, but
     * the change will remain in memory for the life of this CachedRowSet.
     */
    public void updateFloat(int columnIndex, float x) throws SQLException {
        curRow[columnIndex - 1] = BigDecimal.valueOf(x);
	}

    /**
     * Assigns the columnIndex'th item in the current row to the given value.
     * There is no automatic way to propogate this change to the database, but
     * the change will remain in memory for the life of this CachedRowSet.
     */
    public void updateDouble(int columnIndex, double x) throws SQLException {
        curRow[columnIndex - 1] = BigDecimal.valueOf(x);
	}

    /**
     * Assigns the columnIndex'th item in the current row to the given object.
     * There is no automatic way to propogate this change to the database, but
     * the change will remain in memory for the life of this CachedRowSet.
     */
    public void updateBigDecimal(int columnIndex, BigDecimal x) throws SQLException {
        curRow[columnIndex - 1] = x;
	}

    /**
     * Assigns the columnIndex'th item in the current row to the given object.
     * There is no automatic way to propogate this change to the database, but
     * the change will remain in memory for the life of this CachedRowSet.
     */
    public void updateString(int columnIndex, String x) throws SQLException {
        curRow[columnIndex - 1] = x;
	}

    /**
     * Assigns the columnIndex'th item in the current row to the given object.
     * There is no automatic way to propogate this change to the database, but
     * the change will remain in memory for the life of this CachedRowSet.
     */
    public void updateBytes(int columnIndex, byte x[]) throws SQLException {
        curRow[columnIndex - 1] = x;
	}

    /**
     * Assigns the columnIndex'th item in the current row to the given object.
     * There is no automatic way to propogate this change to the database, but
     * the change will remain in memory for the life of this CachedRowSet.
     */
    public void updateDate(int columnIndex, java.sql.Date x) throws SQLException {
        curRow[columnIndex - 1] = x;
	}

    /**
     * Assigns the columnIndex'th item in the current row to the given object.
     * There is no automatic way to propogate this change to the database, but
     * the change will remain in memory for the life of this CachedRowSet.
     */
    public void updateTime(int columnIndex, java.sql.Time x) throws SQLException {
        curRow[columnIndex - 1] = x;
	}

    /**
     * Assigns the columnIndex'th item in the current row to the given object.
     * There is no automatic way to propogate this change to the database, but
     * the change will remain in memory for the life of this CachedRowSet.
     */
    public void updateTimestamp(int columnIndex, java.sql.Timestamp x)
		throws SQLException {
        curRow[columnIndex - 1] = x;
	}

	/**
	 * Not supported.
	 */
    public void updateAsciiStream(int columnIndex, java.io.InputStream x, int length)
		throws SQLException {
		throw new UnsupportedOperationException("Not supported!");
	}

	/**
	 * Not supported.
	 */
    public void updateBinaryStream(int columnIndex, java.io.InputStream x, int length)
		throws SQLException {
		throw new UnsupportedOperationException("Not supported!");
	}

	/**
	 * Not supported.
	 */
    public void updateCharacterStream(int columnIndex, java.io.Reader x, int length)
		throws SQLException {
		throw new UnsupportedOperationException("Not supported!");
	}

    /**
     * Assigns the columnIndex'th item in the current row to the given object.
     * There is no automatic way to propogate this change to the database, but
     * the change will remain in memory for the life of this CachedRowSet.
     */
    public void updateObject(int columnIndex, Object x, int scale) throws SQLException {
        curRow[columnIndex - 1] = x;
	}

    /**
     * Assigns the columnIndex'th item in the current row to the given object.
     * There is no automatic way to propogate this change to the database, but
     * the change will remain in memory for the life of this CachedRowSet.
     */
    public void updateObject(int columnIndex, Object x) throws SQLException {
        curRow[columnIndex - 1] = x;
	}

    /**
     * Assigns the columnIndex'th item in the current row to the given object.
     * There is no automatic way to propogate this change to the database, but
     * the change will remain in memory for the life of this CachedRowSet.
     */
    public void updateArray(int columnIndex, java.sql.Array x) throws SQLException {
        curRow[columnIndex - 1] = x;
	}

    /**
     * Assigns the columnIndex'th item in the current row to the given object.
     * There is no automatic way to propogate this change to the database, but
     * the change will remain in memory for the life of this CachedRowSet.
     */
	public void updateClob(int columnIndex, java.sql.Clob x) throws SQLException {
        curRow[columnIndex - 1] = x;
	}

    /**
     * Assigns the columnIndex'th item in the current row to the given object.
     * There is no automatic way to propogate this change to the database, but
     * the change will remain in memory for the life of this CachedRowSet.
     */
	public void updateBlob(int columnIndex, java.sql.Blob x) throws SQLException {
        curRow[columnIndex - 1] = x;
	}

    /**
     * Assigns the columnIndex'th item in the current row to the given object.
     * There is no automatic way to propogate this change to the database, but
     * the change will remain in memory for the life of this CachedRowSet.
     */
    public void updateRef(int columnIndex, java.sql.Ref x) throws SQLException {
        if (curRow == null) throw new SQLException("Not on a valid row");
        curRow[columnIndex - 1] = x;
	}
	
    // ====================================
    // UPDATES BY NAME (FORWARD TO UPDATE-BY-INDEX)
    // ====================================

	/**
	 * Forwards to the corresponding method that takes a column index.
     * 
     * @throws SQLException if there is no column with the given name.
	 */
    public void updateNull(String columnName) throws SQLException {
		updateNull(findColumn(columnName));
	}

	/**
     * Forwards to the corresponding method that takes a column index.
     * 
     * @throws SQLException if there is no column with the given name.
	 */
    public void updateBoolean(String columnName, boolean x) throws SQLException {
		updateBoolean(findColumn(columnName), x);
	}

	/**
     * Forwards to the corresponding method that takes a column index.
     * 
     * @throws SQLException if there is no column with the given name.
	 */
    public void updateByte(String columnName, byte x) throws SQLException {
		updateByte(findColumn(columnName), x);
	}

	/**
     * Forwards to the corresponding method that takes a column index.
     * 
     * @throws SQLException if there is no column with the given name.
	 */
    public void updateShort(String columnName, short x) throws SQLException {
		updateShort(findColumn(columnName), x);
	}

	/**
     * Forwards to the corresponding method that takes a column index.
     * 
     * @throws SQLException if there is no column with the given name.
	 */
    public void updateInt(String columnName, int x) throws SQLException {
		updateInt(findColumn(columnName), x);
	}

	/**
     * Forwards to the corresponding method that takes a column index.
     * 
     * @throws SQLException if there is no column with the given name.
	 */
    public void updateLong(String columnName, long x) throws SQLException {
		updateLong(findColumn(columnName), x);
	}

	/**
     * Forwards to the corresponding method that takes a column index.
     * 
     * @throws SQLException if there is no column with the given name.
	 */
    public void updateFloat(String columnName, float x) throws SQLException {
		updateFloat(findColumn(columnName), x);
	}

	/**
     * Forwards to the corresponding method that takes a column index.
     * 
     * @throws SQLException if there is no column with the given name.
	 */
    public void updateDouble(String columnName, double x) throws SQLException {
		updateDouble(findColumn(columnName), x);
	}

	/**
     * Forwards to the corresponding method that takes a column index.
     * 
     * @throws SQLException if there is no column with the given name.
	 */
    public void updateBigDecimal(String columnName, BigDecimal x) throws SQLException {
		updateBigDecimal(findColumn(columnName), x);
	}

	/**
     * Forwards to the corresponding method that takes a column index.
     * 
     * @throws SQLException if there is no column with the given name.
	 */
    public void updateString(String columnName, String x) throws SQLException {
		updateString(findColumn(columnName), x);
	}

	/**
     * Forwards to the corresponding method that takes a column index.
     * 
     * @throws SQLException if there is no column with the given name.
	 */
    public void updateBytes(String columnName, byte x[]) throws SQLException {
		updateBytes(findColumn(columnName), x);
	}

	/**
     * Forwards to the corresponding method that takes a column index.
     * 
     * @throws SQLException if there is no column with the given name.
	 */
    public void updateDate(String columnName, java.sql.Date x) throws SQLException {
		updateDate(findColumn(columnName), x);
	}

	/**
     * Forwards to the corresponding method that takes a column index.
     * 
     * @throws SQLException if there is no column with the given name.
	 */
    public void updateTime(String columnName, java.sql.Time x) throws SQLException {
		updateTime(findColumn(columnName), x);
	}

	/**
     * Forwards to the corresponding method that takes a column index.
     * 
     * @throws SQLException if there is no column with the given name.
	 */
    public void updateTimestamp(String columnName, java.sql.Timestamp x) throws SQLException {
		updateTimestamp(findColumn(columnName), x);
	}

	/**
     * Forwards to the corresponding method that takes a column index.
     * 
     * @throws SQLException if there is no column with the given name.
	 */
    public void updateAsciiStream(String columnName, java.io.InputStream x, int length) throws SQLException {
		updateAsciiStream(findColumn(columnName), x, length);
	}

	/**
     * Forwards to the corresponding method that takes a column index.
     * 
     * @throws SQLException if there is no column with the given name.
	 */
    public void updateBinaryStream(String columnName, java.io.InputStream x, int length) throws SQLException {
		updateBinaryStream(findColumn(columnName), x, length);
	}

	/**
     * Forwards to the corresponding method that takes a column index.
     * 
     * @throws SQLException if there is no column with the given name.
	 */
    public void updateCharacterStream(String columnName, java.io.Reader reader, int length) throws SQLException {
		updateCharacterStream(findColumn(columnName), reader, length);
	}

	/**
     * Forwards to the corresponding method that takes a column index.
     * 
     * @throws SQLException if there is no column with the given name.
	 */
    public void updateObject(String columnName, Object x, int scale) throws SQLException {
		updateObject(findColumn(columnName), x, scale);
	}

	/**
     * Forwards to the corresponding method that takes a column index.
     * 
     * @throws SQLException if there is no column with the given name.
	 */
    public void updateObject(String columnName, Object x) throws SQLException {
		updateObject(findColumn(columnName), x);
	}

	/**
     * Forwards to the corresponding method that takes a column index.
     * 
     * @throws SQLException if there is no column with the given name.
	 */
    public void updateRef(String columnName, java.sql.Ref x) throws SQLException {
		updateRef(findColumn(columnName), x);
	}

	/**
     * Forwards to the corresponding method that takes a column index.
     * 
     * @throws SQLException if there is no column with the given name.
	 */
    public void updateBlob(String columnName, java.sql.Blob x) throws SQLException {
		updateBlob(findColumn(columnName), x);
	}

	/**
     * Forwards to the corresponding method that takes a column index.
     * 
     * @throws SQLException if there is no column with the given name.
	 */
    public void updateClob(String columnName, java.sql.Clob x) throws SQLException {
		updateClob(findColumn(columnName), x);
	}

	/**
     * Forwards to the corresponding method that takes a column index.
     * 
     * @throws SQLException if there is no column with the given name.
	 */
    public void updateArray(String columnName, java.sql.Array x) throws SQLException {
		updateArray(findColumn(columnName), x);
	}
	
    // ====================================
    // ROW INSERTION METHODS
    // ====================================

    /**
     * The value of {@link #rownum} before {@link #insertRow()} was called.
     * This value will be restored by {@link #moveToCurrentRow()}.
     */
    private int rownumBeforeInserting;
    
    /**
     * Guards against inserting the same "insert row" multiple times.
     */
    private boolean insertRowAlreadyInserted;
    
    public void moveToInsertRow() throws SQLException {
        curRow = new Object[rsmd.getColumnCount()];
        
        // handle the case of inserting multiple rows without an
        // intervening call to moveToCurrentRow() by preserving
        // original "before insert" row number
        if (rownum != INSERT_ROW) {
            rownumBeforeInserting = rownum;
        }
        
        rownum = INSERT_ROW;
        insertRowAlreadyInserted = false;
    }

    public void insertRow() throws SQLException {
        if (rownum != INSERT_ROW) {
            throw new SQLException("Not on insert row");
        }
        if (insertRowAlreadyInserted) {
            throw new SQLException("The insert row has already been inserted");
        }
        data.add(curRow);
        insertRowAlreadyInserted = true;
    }

    public void moveToCurrentRow() throws SQLException {
        if (rownum == INSERT_ROW) {
            rownum = rownumBeforeInserting;
            rownumBeforeInserting = 0;
        }
    }


    // ====================================
    // UPDATE COMMIT METHODS (NOT SUPPORTED)
    // ====================================

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

}
