package ca.sqlpower.sql;

import java.io.Serializable;
import java.util.*;

/**
 * The DBConnectionSpec class is a simple bean whose instances
 * represent a database that the application user is allowed to
 * connect to.  With an instnace of this bean, plus a database
 * username/password pair, you have all the information you need to
 * attempt to make a JDBC Connection to a target database.
 *
 * @version $Id$
 */
public class DBConnectionSpec implements Serializable {
	String name;
	String displayName;
	String driverClass;
	String url;
	String user;
	String pass;

	public String toString() {
		return "DBConnectionSpec: "+name+", "+displayName+", "+driverClass+", "+url;
	}

	/**
	 * Gets the value of name
	 *
	 * @return the value of name
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Sets the value of name
	 *
	 * @param argName Value to assign to this.name
	 */
	public void setName(String argName){
		this.name = argName;
	}

	/**
	 * Gets the value of displayName
	 *
	 * @return the value of displayName
	 */
	public String getDisplayName() {
		return this.displayName;
	}

	/**
	 * Sets the value of displayName
	 *
	 * @param argDisplayName Value to assign to this.displayName
	 */
	public void setDisplayName(String argDisplayName){
		this.displayName = argDisplayName;
	}

	/**
	 * Gets the value of url
	 *
	 * @return the value of url
	 */
	public String getUrl() {
		return this.url;
	}

	/**
	 * Sets the value of url
	 *
	 * @param argUrl Value to assign to this.url
	 */
	public void setUrl(String argUrl){
		this.url = argUrl;
	}

	/**
	 * Gets the value of driverClass
	 *
	 * @return the value of driverClass
	 */
	public String getDriverClass() {
		return this.driverClass;
	}

	/**
	 * Sets the value of driverClass
	 *
	 * @param argDriverClass Value to assign to this.driverClass
	 */
	public void setDriverClass(String argDriverClass){
		this.driverClass = argDriverClass;
	}

	/**
	 * Gets the value of user
	 *
	 * @return the value of user
	 */
	public String getUser() {
		return this.user;
	}

	/**
	 * Sets the value of user
	 *
	 * @param argUser Value to assign to this.user
	 */
	public void setUser(String argUser){
		this.user = argUser;
	}

	/**
	 * Gets the value of pass
	 *
	 * @return the value of pass
	 */
	public String getPass() {
		return this.pass;
	}

	/**
	 * Sets the value of pass
	 *
	 * @param argPass Value to assign to this.pass
	 */
	public void setPass(String argPass){
		this.pass = argPass;
	}

	/**
	 * Looks up a database connection spec by name in the given collection.
	 *
	 * @param dbcsList a java.util.Collection of DBConnectionSpec objects.
	 * @param dbname the name of the database connection you want to
	 * retrieve.
	 * @return The first DBConnectionSpec object returned by the
	 * Collection's iterator whose name matches the dbname argument,
	 * or null if no such connection spec exists in the list.
	 */
	public static DBConnectionSpec searchListForName(List dbcsList, String dbname) {
		DBConnectionSpec dbcs=null;
		Iterator it=dbcsList.iterator();
		while(it.hasNext()) {
			DBConnectionSpec temp=(DBConnectionSpec)it.next();
			if(temp.getName().equals(dbname)) {
				dbcs=temp;
				break;
			}
		}
		return dbcs;
	}
	
	/**
	 * Looks up a database connection spec by hostname, connection
	 * port number, and instance (database) name.  The search is
	 * case-insensitive.
	 *
	 * @param dbcsList a java.util.Collection of DBConnectionSpec objects.
	 * @param dbHostName The DNS name or IP address of the database
	 * server.  Must match whatever was used in the dburl part of the
	 * entry in the xml file.
	 * @param dbPort The TCP port number that the database server
	 * listens for connections on.  The port number must be explicitly
	 * given in the dburl part of the entry in the xml file for this
	 * to work.
	 * @param dbInstanceName The logical database name or instance
	 * name on the specified database server.  Must match whatever was
	 * used in the dburl part of the entry in the xml file.
	 * @return The first DBConnectionSpec object returned by the
	 * Collection's iterator whose dbUrl contains the given dbHost,
	 * dbPort, and dbInstanceName arguments, or null if no such
	 * connection spec exists in the list.
	 */
	public static DBConnectionSpec searchListForServer(List dbcsList,
													   String dbHostName,
													   int dbPort,
													   String dbInstanceName) {
		dbHostName=dbHostName.toUpperCase();
		dbInstanceName=dbInstanceName.toUpperCase();
		DBConnectionSpec dbcs=null;
		Iterator it=dbcsList.iterator();
		while(it.hasNext()) {
			DBConnectionSpec temp=(DBConnectionSpec)it.next();
			String url=temp.getUrl().toUpperCase();
			if(url.indexOf(dbHostName) >= 0 
			   && url.indexOf(String.valueOf(dbPort)) >= 0 
			   && url.indexOf(dbInstanceName) >= 0) {
				dbcs=temp;
				break;
			}
		}
		return dbcs;
	}
}
