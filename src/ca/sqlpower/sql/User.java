package ca.sqlpower.sql;

//import ca.sqlpower.util.*;
import java.beans.*;
import java.sql.*;
import java.io.*;

/**
 * represents a user with a connection to a jdbc-accessible database.
 * It is expected that implementors will extend this class, overriding
 * the methods dealing with access levels.
 * <p>Note that this class is a composition of itself with
 * java.sql.Connection.  Composition is a Java technique where an
 * instance of one class (in this case, Connection) is kept within
 * another, and the containing class exposes a subset of the contained
 * class's methods and fields.
 *
 * @author Jonathan Fuerth
 * @version $Id$
 * @see java.sql.Connection
 * @deprecated This whole thing needs to be re-thought.  There is not
 * currently a suitable replacement.
 */
public abstract class User implements java.io.Serializable {

    protected String dburl;
    protected String ID;
    protected String pass;
    protected int accessLevel;
    protected static String[] accessLevelNames = {"Not Logged In",  //0
						  "Super User",     //1
						  "Read Only"};     //2
    protected Connection con;

    /**
     * makes a database connection, using the database's
     * authentication mechanisms. An database driver appropriate to
     * the given dburl must already be loaded and registered.
     *
     * @param dburl the connection URL for the desired target database
     * @param userid the userid to supply to the target database
     * @param password the password to supply to the target database
     * @throws SQLExcpetion if there is a database error during connection
     * @throws NotSerializableException if the Connection object returned by <code>DriverManager.getConnection()</code> is not serializable
     */
    protected User(String dburl, String userid, String password) throws SQLException {
	this();
	this.dburl=dburl;
	ID=userid;
	pass=password;

	connectToDB();  // does authentication
	
	// XXX: should we do something fancier here, or leave access
	// levels to the overriding class?
	accessLevel=1;
    }

    /**
     * initialises all the non-static fields to sane defaults.  Note
     * that there is no public consturctor for making a User object
     * without a valid database connection.
     */
    private User() {
	dburl=null;
	ID=null;
	accessLevel = 0;
	con=null;
    }

    private void connectToDB() throws SQLException {
	// Authenticate here (and only here)
        con=DriverManager.getConnection(dburl, ID, pass);
    }
    
    public String getID() {
	return ID;
    }

    public int getAccessLevel() {
	return accessLevel;
    }

    public void setAccessLevel(int level) throws PropertyVetoException{
	if(level >= accessLevelNames.length) {
	    PropertyChangeEvent evt = new PropertyChangeEvent(this, "accessLevel",
							      new Integer(accessLevel), 
							      new Integer(level));
	    throw new PropertyVetoException("Invalid User Access Level", evt);
	}
	accessLevel = level;
    }

    public String getAccessLevelName(int level) {
	return accessLevelNames[level];
    }

    /**
     * render an HTML representation of this object (for debugging).
     *
     * @return this object's attributes formatted as an HTML table
     */
    public String toHtml() {
	StringBuffer sb = new StringBuffer(200);
	sb.append("<table border=\"1\">")
	    .append("<tr><td colspan=\"2\">User Fields</td>")
	    .append("<tr><td>ID</td><td>")
	    .append(getID())
	    .append("</td></tr><tr><td>AccessLevel</td><td>")
	    .append(getAccessLevel())
	    .append("</td></tr><tr><td>AccessLevelName</td><td>")
	    .append(getAccessLevelName(getAccessLevel()))
	    .append("</td></tr></table>");
	return sb.toString();
    }

    /**
     * exposed from java.sql.Connection
     *
     * @return a Statement for interacting with the target database
     * @throws SQLException whenever java.sql.Connection.createStatement() would
     * @see java.sql.Connection
     */
    public java.sql.Statement createStatement() throws SQLException {
	return con.createStatement();
    }
}
