package ca.sqlpower.sql;

import java.sql.*;
import java.util.*;
import javax.servlet.*;

public class DBConnection {

    /**
     * This class cannot be instantiated
     */
    private DBConnection()
    {
    }

	/**
	 * Parses the WEB-INF/databases.ini file into the given lists.
	 *
	 * @deprecated Use the <code>getAvailableDatabases()</code> method
	 * to retrieve the set of available databases.
	 */
	public static String readINIFile(ServletContext servletContext,
									 String match_db_name,
									 StringBuffer db_url,
									 StringBuffer db_class,
									 ArrayList db_names) {
		return "Available databases are no longer specified by this method.";
	}
	
    /**
     * Checks if this connection refers to an Oracle database.
     *
     * @return True if Oracle, otherwise False
     */
	public static boolean isOracle(Connection con) {
		DatabaseMetaData dmd;
		String url="";
		
		try {
			dmd = con.getMetaData();
			url = dmd.getURL();
		} catch(SQLException e) {
			System.out.println("problem in DBConnection.isOracle: "+e.getMessage());
		}

		if(url.toUpperCase().indexOf("ORACLE") == -1){
			return false;
		} else {
			return true;
		}
	}

	
    /**
     * Checks if this connection refers to a PostgreSQL database.
     *
     * @return True if postgresql, otherwise False
     */
	public static boolean isPostgres(Connection con) {
		DatabaseMetaData dmd;
		
		try {
			dmd = con.getMetaData();
			if(dmd.getDatabaseProductName().indexOf("PostgreSQL") >= 0) {
				return true;
			} else {
				return false;
			}
		} catch(SQLException e) {
			System.out.println("problem in DBConnection.isPostgres: "+e.getMessage());
		}
		return false;
	}

	public static String getSystemDate(Connection con) {
		String systemDate="";
		
		if(isOracle(con)){
			systemDate = "SYSDATE";
		} else if (isPostgres(con)) {
			systemDate = "now()";
		} else {
			systemDate = "{fn NOW()}";
		}
		
		return systemDate;
	}
	
	
	public static String getUser(Connection con)
		throws SQLException
	{
		String user = null;
		if (con instanceof PoolableStatementClosingConnection) {
			user = ((PoolableStatementClosingConnection) con).getPlUsername();
		}

		if (user != null) return user;

		DatabaseMetaData dmd = con.getMetaData();
		user = dmd.getUserName();

		return user;
	}
	
	
	public static String ifNull(String valueString,
								String ifNullString)
	{
		String newValueString;
		
		if(valueString==null || valueString.equals("")){
			newValueString=ifNullString;
		} else {
			newValueString = valueString;
		}
		
		return newValueString;
	} 
}
