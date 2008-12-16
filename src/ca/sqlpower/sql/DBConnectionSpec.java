package ca.sqlpower.sql;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.Serializable;
import java.util.Iterator;
import java.util.List;

import ca.sqlpower.util.SQLPowerUtils;

/**
 * The DBConnectionSpec class is a simple bean whose instances
 * represent a database that the application user is allowed to
 * connect to.  With an instnace of this bean, plus a database
 * username/password pair, you have all the information you need to
 * attempt to make a JDBC Connection to a target database.
 *
 * <p>Note that all setXXX methods in this class fire property change
 * events.
 *
 * @version $Id$
 */
public class DBConnectionSpec implements Serializable, Comparable {

	/**
	 * Provides compatibility with the DBConnectionSpec that had
	 * properties singleLogin, seqNo, name, displayName, driverClass,
	 * url, user, pass.  If you are updating DBConnectionSpec to
	 * include new non-transient member variables, you will need to
	 * implement a custom readObject method.
	 */
	static final long serialVersionUID = 6238643669579317332L;

	/**
	 * If true, this DBCS describes a single-database-user,
	 * multi-dashboard-user connection.  (This improves connection
	 * pooling performance, and eases max named users enforcement).
	 * Specifically, the username and password in this DBCS describe
	 * the RDBMS username and password, and will be the same for all
	 * users (in all likelihood, they were loaded from databases.xml).
	 * The username and password entered by the user on the login
	 * screen will be user separately to validate against an entry in
	 * the PL_USER table.
	 */
	protected boolean singleLogin;

	/**
	 * The sequence number used to sort in the list.
	 */
	protected int seqNo;

	protected String name;
	protected String displayName;
	protected String driverClass;
	protected String url;

	/**
	 * The RDMBS user name.
	 *
	 * @see #singleLogin
	 */
	protected String user;

	/**
	 * The RDMBS password.
	 *
	 * @see #singleLogin
	 */
	protected String pass;

	protected transient PropertyChangeSupport pcs;
	protected PropertyChangeSupport getPcs() {
		if (pcs == null) pcs = new PropertyChangeSupport(this);
		return pcs;
	}

	public DBConnectionSpec() {
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

	public int compareTo(Object other) {
		return new Integer(this.getSeqNo()).compareTo(new Integer(((DBConnectionSpec) other).getSeqNo()));
	}

	@Override
	public boolean equals(Object other) {
		// identical object reference
		if (this == other) {
			return true;
		}
		// correct type
		if (!(other instanceof DBConnectionSpec)) {
			return false;
		}
		
		DBConnectionSpec otherDbcs = (DBConnectionSpec) other;
	
		// protect from null pointer exceptions by wrapping equality calls
		if (getSeqNo() == otherDbcs.getSeqNo() &&
			SQLPowerUtils.areEqual(getDriverClass(),otherDbcs.getDriverClass()) &&
			SQLPowerUtils.areEqual(getUrl(),otherDbcs.getUrl()) &&
			SQLPowerUtils.areEqual(getUser(),otherDbcs.getUser()) &&
            SQLPowerUtils.areEqual(getPass(),otherDbcs.getPass())) {
				return true;
		} else {
				return false;
		}
	}
	
	@Override
	public int hashCode() {
		int result = 17;
		result = 31 * result + seqNo;
		result = 31 * result + getDriverClass().hashCode();
		result = 31 * result + getUrl().hashCode();
		result = 31 * result + getUser().hashCode();
		result = 31 * result + getPass().hashCode();
		return result;
	}

	/**
	 * Prints some info from this DBCS.  For use in debugging.
	 */
	@Override
	public String toString() {
		return "DBConnectionSpec: singleLogin="+singleLogin+", "+name+", "+displayName+", "+driverClass+", "+url;
	}

	// --------------------- property change ---------------------------
	public void addPropertyChangeListener(PropertyChangeListener l) {
		getPcs().addPropertyChangeListener(l);
	}

	public void removePropertyChangeListener(PropertyChangeListener l) {
		getPcs().removePropertyChangeListener(l);
	}

	// ------------------- accessors and mutators ------------------------

	public boolean isSingleLogin() {
		return singleLogin;
	}

	public void setSingleLogin(boolean v) {
		boolean oldValue = singleLogin;
		singleLogin = v;
		getPcs().firePropertyChange("singleLogin", oldValue, singleLogin);
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
		String oldValue = name;
		this.name = argName;
		getPcs().firePropertyChange("name", oldValue, name);
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
		String oldValue = displayName;
		this.displayName = argDisplayName;
		getPcs().firePropertyChange("displayName", oldValue, displayName);
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
	public void setUrl(String argUrl) {
		String oldValue = url;
		this.url = argUrl;
		getPcs().firePropertyChange("url", oldValue, url);
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
		String oldValue = driverClass;
		this.driverClass = argDriverClass;
		getPcs().firePropertyChange("driverClass", oldValue, driverClass);
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
		String oldValue = user;
		this.user = argUser;
		getPcs().firePropertyChange("user", oldValue, user);
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
		String oldValue = pass;
		this.pass = argPass;
		getPcs().firePropertyChange("pass", oldValue, pass);
	}

	/**
	 * Returns the seqNo.
	 * @return int
	 */
	public int getSeqNo() {
		return seqNo;
	}

	/**
	 * Sets the seqNo.
	 * @param seqNo The seqNo to set
	 */
	public void setSeqNo(int argSeqNo) {
		int oldValue = seqNo;
		this.seqNo = argSeqNo;
		getPcs().firePropertyChange("seqNo", oldValue, seqNo);
	}
}
