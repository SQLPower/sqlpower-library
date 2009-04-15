package ca.sqlpower.sql;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import ca.sqlpower.util.Cache;
import ca.sqlpower.util.LabelValueBean;
import ca.sqlpower.util.LeastRecentlyUsedCache;

public class SQL {

	private static final Logger logger = Logger.getLogger(SQL.class);
	
	/**
	 * A cache of the last 20 column types we looked up in the
	 * database.  See {@link #columnType(Connection,String,String,String)}.
	 */
	private static Cache colTypeCache = new LeastRecentlyUsedCache(20);

    /**
     * This class cannot be instantiated
     */
    private SQL() {
        // no-op
    }
	
	/**
	 * Returns the java.sql.Types type of the given owner+table+column
	 * combination.  Caches the N most recently used answers, so
	 * calling this method on repeated requests shouldn't be a
	 * significant slowdown.
	 */
	public static synchronized int columnType(Connection con, String owner, String table, String column)
		throws SQLException {
		String cacheKey = con.getMetaData().getURL()+owner+"."+table+"."+column;
		Integer colType = (Integer) colTypeCache.get(cacheKey);
		if(colType == null) {
			Statement stmt=null;
			try {
				StringBuffer sql = new StringBuffer();
				sql.append("SELECT ").append(column);
				sql.append(" FROM ");
				if(owner != null) {
					sql.append(owner).append(".");
				}
				sql.append(table);
				sql.append(" WHERE 0=1");
				stmt = con.createStatement();
				ResultSet rs = stmt.executeQuery(sql.toString());
				ResultSetMetaData rsmd = rs.getMetaData();
				colType = new Integer(rsmd.getColumnType(1));
			} finally {
				if(stmt != null) {
					stmt.close();
				}
			}
			colTypeCache.put(cacheKey, colType);
		}

		return colType.intValue();
	}

    /**
     * A convenient way of using escapeStatement.  This method does the
     * same thing as escapeStatement, but also encloses the returned
     * string in single-quotes.  If the argument is null, the unquoted
     * string "NULL" is returned.
     *
     * @param string The string that you want escaped and quoted. (or
     * "NULL")
     * @return The same as escapeStatement would, but enclosed in quotes.
     */
    public static String quote(String string) {
		if(string==null) {
			return "NULL";
		} else {
			return "'"+escapeStatement(string)+"'";
		}
    }

    public static String quote(char myChar) {
		String string=String.valueOf(myChar);
		return "'"+escapeStatement(string)+"'";
    }

	/**
	 * Alias for {@link #quoteCollection()}.
	 */
	public static String quoteList(List strings) {
		return quoteCollection(strings);
	}

	/**
	 * Creates a String of comma-separated, quoted SQL strings.  Null
	 * values in the collection appear in the list as the unquoted
	 * string <i>null</i>.
	 */
	public static String quoteCollection(Collection strings) {
		StringBuffer outputString = new StringBuffer(100);
		boolean firstItem = true;
		Iterator it = strings.iterator();
		while (it.hasNext()) {
			if (!firstItem) outputString.append(", ");
			Object item = it.next();
			outputString.append(item==null?"NULL":quote(item.toString()));
			firstItem = false;
		}
		return outputString.toString();
	}

	/**
	 * Creates a SQL "IN" expression. Each item in the collection
	 * <code>values</code> is individually quoted.  If one or more of
	 * the values is null, the returned expression will be in
	 * parentheses and contain OR colName IS NULL.  In the special
	 * case where all the values are null, the expression is just
	 * "colName IS NULL".  In the degenerate case where values is an
	 * empty collection, the expression "1=1" is returned so that the
	 * calling code doesn't need special checks.
	 *
	 * @return a String of the form "(colName IN (s1, s2, s3, ...) 
	 * OR colName IS NULL)" or "colName IS NULL" or "1=1".
	 * @throws NullPointerException if colName is null.
	 */
	public static String in(String colName, Collection values) {
		if (colName == null) {
			throw new NullPointerException("colName argument must be non-null");
		}
		boolean foundNull = false;
		StringBuffer outputString = new StringBuffer(100);
		boolean firstItem = true;
		Iterator it = values.iterator();
		while (it.hasNext()) {
			Object item = it.next();
			if (item == null) {
				foundNull = true;
				continue;
			}
			if (!firstItem) outputString.append(", ");
			outputString.append(quote(item.toString()));
			firstItem = false;
		}
		if (firstItem) {
			// nothing made it into outputString
			if (foundNull) {
				return colName+" IS NULL";
			} else {
				return "1=1";
			}
		} else {
			if (foundNull) {
				return "("+colName+" IN("+outputString+") OR "+colName+" IS NULL)";
			} else {
				return colName+" IN("+outputString+")";
			}
		}
	}

	/**
     * Returns the string <code>"NULL"</code> if the argument is
     * either null or the empty string.  Otherwise returns the
     * argument unchanged.
	 *
	 * <p>This method was presumably designed for use only with
	 * strings which represent numeric values.  It has no value when
	 * used with strings you want to store in a VARCHAR database
	 * column, because it doesn't quote non-null strings.  Use
	 * <code>SQL.quote()</code> for that.
	 *
	 * <p>Having said that, you should also keep in mind that this
	 * method does not ensure the argument can be converted to a
	 * number.  You'll still get an SQLException if the string is an
	 * invalid number and you use it in a query after filtering it
	 * through this method.
     *
     * @param string The string that you want to translate.
     */
    public static String nvl(String string) {
		if(string==null || string.equals("")) {
			return "NULL";
		} else {
			return string;
		}
	}

	/**
	 * Returns the appropriate ifnull/nvl syntax for the database
	 * system that <code>con</code> points to.
	 *
	 * <p>For databases that fully support JDBC, this is <code>{fn
	 * ifnull(<i>sqlExpr</i>, <i>valueWhenNull</i>)}</code>, but
	 * Oracle and PostgreSQL need special treatment.
	 *
	 * @param con The connection is used to determing database syntax.
	 * @param sqlExpr The expression that should be tested for
	 * nullness (by the database).  Quote this if you want to use a
	 * constant value!  Unquoted sqlExpr will be evaluated as column
	 * names or functions.
	 * @param valueWhenNull The value that the database should use
	 * when sqlExpr is null.  Quote this if you want to use a constant
	 * value!
	 */
	public static String ifnull(Connection con, String sqlExpr, String valueWhenNull) {
		if (DBConnection.isOracle(con)) {
			return "NVL("+sqlExpr+","+valueWhenNull+")";
		} else if (DBConnection.isPostgres(con)) {
			return "COALESCE("+sqlExpr+","+valueWhenNull+")";
		} else {
			// no special case; assume JDBC compliance for fn ifnull
			return "{fn IFNULL("+sqlExpr+","+valueWhenNull+")}";
		}
	}

	/** 
	 * Returns a string with the first letter capitalized, and the rest lower case
	 *
	 * @param string The string you want to change the case of
	 */
	public static String initcap(String string){
		String newString = "";

		if(string==null || string.equals("")) {
			return "";
		} else {
			newString=string.substring(0,1).toUpperCase();
			newString=newString.concat(string.substring(1).toLowerCase());
			return newString;
		}		
	} // end initcap
	
    /**
     * Makes the input string safe to enclose in single-quotes in an
     * Oracle SQL expression.  Currently, this only means turning all
     * "'" characters into the "''" escape sequence.
     *
     * @param old The original string
     * @return The awk/perl substitution on <code>old</code> of
     * <code>s/'/''/g</code>
     */
    public static String escapeStatement(String old) 
    { 
		if(old==null) {
			return "null";
		}
		
		// a premature optimisation 
		if(old.lastIndexOf('\'') == -1) { 
			return old; 
		} 
		
		int i=0; 
		StringBuffer escaped=new StringBuffer(old); 
		
		while(i < escaped.length()) 
			{ 
				if(escaped.charAt(i)=='\'') { 
					escaped.insert(i, '\''); 
					i++;  // skip over the added quote 
				} 
				i++; 
			} 
		return(escaped.toString()); 
    } 
	
	/**
	 * Supplies a SQL expression that should evaluate to the given
	 * date.  Only accurate to the day (drops time-of-day information).
	 */
	public static String escapeDate(Connection con, java.util.Date date) throws SQLException {
		if (date == null) {
			return "null";
		} else {
			SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
			if (DBConnection.isOracle(con)) {
				return "TO_DATE('"+df.format(date)+"','YYYY-MM-DD')";
			} else {
				// most JDBC drivers support {d 'yyyy-MM-dd'} style escape
				return "{d '"+df.format(date)+"'}";
			}
		}
	}

	/**
	 * Supplies a SQL expression that evaulates to a date with time to the nearest
     * second.
	 */
	public static String escapeDateTime(Connection con, java.util.Date date) throws SQLException {
		if (date == null) {
			return "null";
		} else {
			SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			if (DBConnection.isOracle(con)) {
				return "TO_DATE('"+df.format(date)+"','YYYY-MM-DD HH24:MI:SS')";
			} else {
				// most JDBC drivers support {ts 'yyyy-MM-dd hh:mm:ss'} style escape
				return "{ts '"+df.format(date)+"'}";
			}
		}
	}


    /**
     * Converts the character representation of a YES/NO value into
     * boolean.
     *
     * @param indicator A string that says YES or NO or Y or N, or
     * <code>null</code>, which is interpred as no.
     * @return true iff <code>indicator.charAt(0)=='Y'</code>.
     */
    public static boolean decodeInd(String indicator) {
		if(indicator != null && indicator.charAt(0) == 'Y') {
			return true;
		}
		return false;
    }
	
    /**
     * Returns a list of ca.sqlpower.util.LabelValueBean's [sic]
     * representing all the 1st (label) and 2nd (value) columns in the
     * given result set.
     *
     * @param rs The result set you want listified.
     * @throws SQLException if a database error occurs.
     */
    public static List makeListFromRS(ResultSet rs) throws SQLException {
		List list=new LinkedList();
		
		while(rs.next()) {
			//System.err.println(rs.getString(1)+","+rs.getString(2));
			list.add(new LabelValueBean(rs.getString(1),rs.getString(2)));
		}
		
		return list;
    }
	
	/**
	 * Identical to <code>findPrimaryKey(con, "", tableName)</code>.
	 */
	public static List findPrimaryKey(Connection con, String tableName) throws SQLException {
		return findPrimaryKey(con, "", tableName);
	}
	
	/**
	 * Looks up the primary key of the given table using JDBC
	 * DatabaseMetaData.
	 *
	 * @param con A connection to an Oracle database.
	 * @param schemaName The name of the schema that the desired table
	 * belongs to, or "" to ignore schemas. CASE SENSITIVE!
	 * @param tableName The name of the table for which you want to
	 * know the primary key. CASE SENSITIVE!
	 * @return A List of the column names that make up the primary key
	 * of the table.  All elements in the list are guaranteed to be of
	 * type String.
	 * @throws SQLException if a database error occurs.
	 */
	public static List findPrimaryKey(Connection con, String schemaName, String tableName) 
		throws SQLException {
		ResultSet rs = null;
		try {
			rs = con.getMetaData().getPrimaryKeys("", schemaName, tableName);
			List prikey=new LinkedList();
			while(rs.next()) {
				prikey.add(rs.getString("COLUMN_NAME"));
			}
			return prikey;
		} finally {
			if(rs != null) {
				rs.close();
			}
		}
	}

	public static List getDatabaseOwners(Connection con) throws SQLException {
		Statement stmt=null;
		List list=new LinkedList();
		
		try {
			stmt=con.createStatement();
			ResultSet rs=stmt.executeQuery("SELECT DISTINCT owner FROM pl_tables ORDER BY owner");
			
			while(rs.next()) {
				list.add(rs.getString("OWNER"));
			}
			
		} finally {
			if(stmt!=null) {
				stmt.close();
			}
		}
		return list;
	}

	public static List getDatabaseTables(Connection con, String owner) throws SQLException {
		Statement stmt=null;
		List list=new LinkedList();
		
		try {
			StringBuffer sql=new StringBuffer();
			stmt=con.createStatement();
			sql.append("SELECT DISTINCT table_name");
			sql.append(" FROM pl_tables");
			if(owner != null && owner.length()>0) {
				sql.append(" WHERE owner=").append(quote(owner));
			}
			sql.append(" ORDER BY table_name");
			ResultSet rs=stmt.executeQuery(sql.toString());
			
			while(rs.next()) {
				list.add(rs.getString("TABLE_NAME"));
			}
			
		} finally {
			if(stmt!=null) {
				stmt.close();
			}
		}
		return list;
	}

	/**
	 * Returns the columns for a given owner.table in the database.
	 * Uses the special pl_tab_columns view from SQLPower.
	 *
	 * @param con The database connection.
	 * @param owner The desired table's owner.
	 * @param table The desired table's name.
	 * @param notLike a <code>List</code> of patterns which the column
	 * name should not match.  '%' is a wildcard; '_' matches any
	 * single character.
	 * @param like a <code>List</code> of patterns which the column
	 * name should match.  '%' is a wildcard; '_' matches any single
	 * character.
	 * @return The column names as a List of strings.
	 * @exception SQLException if a database error occurs
	 */
	public static List getDatabaseColumns(Connection con, String owner, String table, 
										  List notLike, List like)
		throws SQLException {
		Statement stmt=null;
		List list=new LinkedList();

		if(owner==null) {
			throw new NullPointerException("owner must be non-null");
		}
		if(table==null) {
			throw new NullPointerException("table must be non-null");
		}
		
		try {
			StringBuffer sql=new StringBuffer();
			stmt=con.createStatement();
			sql.append("SELECT DISTINCT column_name");
			sql.append(" FROM pl_tab_columns");
			sql.append(" WHERE owner=").append(quote(owner));
			sql.append(" AND table_name=").append(quote(table));
			Iterator it=notLike.iterator();
			while(it.hasNext()) {
				sql.append(" AND column_name NOT LIKE ").append(quote((String)it.next()));
			}
			it=like.iterator();
			while(it.hasNext()) {
				sql.append(" AND column_name LIKE ").append(quote((String)it.next()));
			}
			sql.append(" ORDER BY column_name");
			ResultSet rs=stmt.executeQuery(sql.toString());
			
			while(rs.next()) {
				list.add(rs.getString("COLUMN_NAME").toUpperCase());
			}
			
		} finally {
			if(stmt!=null) {
				stmt.close();
			}
		}
		return list;
	}
	
	/**
	 * Searches the given Result Set for a column of the given name (case
	 * insensitive). Returns true if the given result set has the named column.
	 * This method is similar to {@link ResultSet#findColumn(String)}, but it
	 * returns -1 instead of throwing an exception when the requested column
	 * does not exist.
	 * 
	 * @param rs
	 *            The Result Set to consult. Must not be null.
	 * @param colName
	 *            The name of the column to look for. Must not be null.
	 * @return The column number of the column having the given name, or -1 if
	 *         there is no such column. This number is in the JDBC 1-based
	 *         numbering system (the first column number is 1), so the value 0
	 *         will never be returned.
	 * @throws SQLException
	 *             if a database error occurs.
	 */
	public static int findColumnIndex(ResultSet rs, String colName) throws SQLException {
		ResultSetMetaData rsmd = rs.getMetaData();
		for (int i = 1, n = rsmd.getColumnCount(); i <= n; i++) {
			String columnName = rsmd.getColumnName(i);
			logger.debug("Column " + i + ": " + columnName);
			if (colName.equalsIgnoreCase(columnName)) {
				return i;
			}
		}
		return -1;
	}
	
	/**
	 * Returns true if the JDBC type given is a numeric value, returns false otherwise.
	 */
	public static boolean isNumeric(int jdbcType) {
		return new ArrayList<Integer>(Arrays.asList(new Integer[]{Types.BIGINT, Types.BINARY, Types.BIT, Types.DECIMAL, Types.DOUBLE, Types.FLOAT, Types.INTEGER, Types.NUMERIC, Types.SMALLINT, Types.TINYINT})).contains(jdbcType);
	}
	
	/**
	 * Returns true if the JDBC type given is a date value, returns false otherwise.
	 * @param jdbcType
	 * @return
	 */
	public static boolean isDate(int jdbcType) {
		return new ArrayList<Integer>(Arrays.asList(new Integer[]{Types.DATE, Types.TIMESTAMP})).contains(jdbcType);
	}
	
	/**
	 * Returns true if the JDBC type give is a boolean value, returns false otherwise.
	 */
	public static boolean isBoolean(int jdbcType) {
		return new ArrayList<Integer>(Arrays.asList(new Integer[]{Types.BOOLEAN})).contains(jdbcType);
	}
}
