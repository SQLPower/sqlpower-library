package ca.sqlpower.sql;

import java.sql.*;
import java.util.*;
import ca.sqlpower.util.LabelValueBean;

public class SQL {

    /**
     * This class cannot be instantiated
     */
    private SQL()
    {
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
     * convert number string to NULL if null
     *
     * @param string The string that you want to translate 
     */
    public static String nvl(String string) {
		if(string==null || string.equals("")) {
			return "NULL";
		} else {
			return string;
		}
	}


	/** 
	 * Return a string with the first letter capitalized, and the rest lower case
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
     * Converts the character representation of a YES/NO value into
     * boolean.
     *
     * @param indicator A string that says YES or NO or Y or N.
     * @return true iff <code>indicator.charAt(0)=='Y'</code>.
     */
    public static boolean decodeInd(String indicator) {
		if(indicator.charAt(0) == 'Y') {
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
	 * Uses the Oracle data dictionary to determine the primary key of
	 * the given table.  Only works on Oracle databases.
	 *
	 * @param con A connection to an Oracle database.
	 * @param tableName The name of the table for which you want to
	 * know the primary key.
	 * @return A List of the column names that make up the primary key
	 * of the table.  All elements in the list are guaranteed to be of
	 * type String.
	 * @throws SQLException if a database error occurs.  This will
	 * almost certainly happen if you run this method on a non-Oracle
	 * database.
	 */
	public static List findPrimaryKey(Connection con, String tableName) 
		throws SQLException {
		Statement stmt=null;
		try {
			stmt=con.createStatement();
			ResultSet rs=stmt.executeQuery("SELECT cc.column_name"
										   +" FROM user_cons_columns cc, user_constraints c"
										   +" WHERE c.table_name = '"+tableName+"'"
										   +" AND c.constraint_type='P'"
										   +" AND cc.owner = c.owner"
										   +" AND cc.constraint_name = c.constraint_name"
										   +" ORDER BY cc.position");
			List prikey=new LinkedList();
			while(rs.next()) {
				prikey.add(rs.getString(1));
			}
			return prikey;
		} finally {
			if(stmt != null) {
				stmt.close();
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
			if(owner != null || owner.length()>0) {
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
				list.add(rs.getString("COLUMN_NAME"));
			}
			
		} finally {
			if(stmt!=null) {
				stmt.close();
			}
		}
		return list;
	}
}
