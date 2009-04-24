/*
 * Copyright (c) 2008, SQL Power Group Inc.
 *
 * This file is part of SQL Power Library.
 *
 * SQL Power Library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * SQL Power Library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>. 
 */
/*
 * Created on Jun 28, 2005
 *
 * This code belongs to SQL Power.
 */
package ca.sqlpower.sql;

/**
 * The SPDataSource represents a database that the Power Loader or
 * the Architect can connect to.  It holds all the information required for
 * making JDBC, ODBC, or native Oracle connections (depending on what type
 * of database the connection is for).
 *
 * @see ca.sqlpower.architect.PlDotIni
 * @author jack, jonathan
 */

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;

import ca.sqlpower.sql.jdbcwrapper.ConnectionDecorator;

public class SPDataSource {

    private static final Logger logger = Logger.getLogger(SPDataSource.class);

    /**
     * JDBC driver pathname prefix that says to look for a JAR file resource on
     * the classpath.
     */
    public static final String BUILTIN = "builtin:";

    /**
     * JDBC driver pathname prefix that says to look for a JAR file resource on
     * the remote SQL Power Enterprise server we're attached to.
     */
    private static final String SERVER = "server:";
    
	/**
	 * Compares this data source to the given data source by comparing
	 * the respecitive fields in the following order:
	 * <ol>
	 *  <li>name
	 *  <li>url
	 *  <li>driver class
	 *  <li>user
	 *  <li>password
	 * </ol>
	 *
	 * <p>Note: the SPDataSource doesn't implement Comparable itself
	 * because it would be difficult and unnecessary to do a complete comparison
	 * which is consistent with equals and hashCode.
	 */
	public static class DefaultComparator implements Comparator<SPDataSource> {

		/**
		 * Performs the comparison described in the class comment.
		 *
		 * @param o the SPDataSource object to compare with.
		 * @return &lt;0 if this data source comes before o; 0 if they
		 *  are equal; &gt;0 otherwise.
		 * @throws NullPointerException if o==null
		 * @throws ClassCastException if o is not an instance of SPDataSource
		 */
		public int compare(SPDataSource ds1, SPDataSource ds2) {
			if (ds1 == ds2) return 0;
			int tmp;
			String v1, v2;

			v1 = ds1.getName();
			v2 = ds2.getName();
			if (v1 == null && v2 != null) return -1;
			else if (v1 != null && v2 == null) return 1;
			else if (v1 != null && v2 != null) {
				tmp = v1.compareToIgnoreCase(v2);
			} else {
				tmp = 0;
			}
			if (tmp != 0) return tmp;

			v1 = ds1.getUrl();
			v2 = ds2.getUrl();
			if (v1 == null && v2 != null) return -1;
			else if (v1 != null && v2 == null) return 1;
			else if (v1 != null && v2 != null) {
				tmp = v1.compareTo(v2);
			} else {
				tmp = 0;
			}
			if (tmp != 0) return tmp;

			v1 = ds1.getDriverClass();
			v2 = ds2.getDriverClass();
			if (v1 == null && v2 != null) return -1;
			else if (v1 != null && v2 == null) return 1;
			else if (v1 != null && v2 != null) {
				tmp = v1.compareTo(v2);
			} else {
				tmp = 0;
			}
			if (tmp != 0) return tmp;

			v1 = ds1.getUser();
			v2 = ds2.getUser();
			if (v1 == null && v2 != null) return -1;
			else if (v1 != null && v2 == null) return 1;
			else if (v1 != null && v2 != null) {
				tmp = v1.compareTo(v2);
			} else {
				tmp = 0;
			}
			if (tmp != 0) return tmp;

			v1 = ds1.getPass();
			v2 = ds2.getPass();
			if (v1 == null && v2 != null) return -1;
			else if (v1 != null && v2 == null) return 1;
			else if (v1 != null && v2 != null) {
				tmp = v1.compareTo(v2);
			} else {
				tmp = 0;
			}
			if (tmp != 0) return tmp;

			return 0;
		}
	}

    /**
     * The custom JDBC classloaders in this app support special "builtin:" and
     * "server:" filename prefixes, which means a JAR file as a resource on the
     * classpath or on a remote HTTP server rather than an absolute local path
     * name. This method will take a string that might be a builtin reference
     * and return the java.io.File object that points to the builtin file's real
     * location in the filesystem. If the given string doesn't start with
     * builtin: or server:, it will be treated as a normal (absolute or
     * relative) file pathname.
     * 
     * @param jarFileName
     *            The builtin: or server: file spec or a normal local path name
     * @param classLoader
     *            The classloader against which to resolve the builtin resource
     *            names. If no builtin: resources are expected, this may be
     *            null.
     * @param serverBaseURI
     *            The base URI against which to resolve server: resourcve names.
     *            If no server: resources are expected, this may be null.
     * @return a URL object that refers to the given filespec. It is not tested
     *         for existence, so trying to read it may cause an IO Exception.
     */
    public static URL jarSpecToFile(String jarFileName, ClassLoader classLoader, URI serverBaseURI) {
        URL location;
        try {
            if (jarFileName.startsWith(BUILTIN)) {
                String jarName = jarFileName.substring(BUILTIN.length());
                location = classLoader.getResource(jarName);
                if (location == null) {
                    logger.warn("Couldn't find built-in system resource \""+jarName+"\". Skipping it.");
                }
            } else if (jarFileName.startsWith(SERVER)) {
                if (serverBaseURI == null) {
                    throw new IllegalArgumentException(
                            "The JDBC driver at " + jarFileName + " can't" +
                    " be located because no server base URI was specified");
                }
                String jarFilePath = jarFileName.substring(SERVER.length());
                location = serverBaseURI.toURL();
                location = new URL(location, jarFilePath);
            } else {
                location = new File(jarFileName).toURL();
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        return location;
    }
    
    /**
	 * Builds a string of the DB connection properties to display to the user when 
	 * testing a DB connection or in a log of a SQL query. <br>
	 * NOTE: This method uses the StringBuilder class to create strings which is not 
     * thread safe.<p>
	 * @param ds - The SPDataSource for which connection details are wanted<p>
	 * @param testConnection - This boolean should be true when the string is to be 
	 * displayed when testing a connection. False, otherwise. The boolean decides whether 
	 * or not to display the datasource's name. There is no point in displaying the 
	 * datasource's name if it is already shown to the user as it is when testing a 
	 * connection (in one of the text fields)<p>
	 * @return A string of the DB connection properties
	 */
    public static String getConnectionInfoString(SPDataSource ds, boolean testConnection)
	{
		//If the connection fails, it returns an empty string
		StringBuilder summary = new StringBuilder("");
        Connection con = null;
        try {
            con = ds.createConnection();
            DatabaseMetaData dbmd = con.getMetaData();
            if(!testConnection) {
            	summary.append(ds.getDisplayName()+": \n");
            }
            summary.append("Database Product Name: ").append(dbmd.getDatabaseProductName()+"\n");
            summary.append("Database Product Version: ").append(dbmd.getDatabaseProductVersion()+"\n");
            summary.append("Database Driver Name: ").append(dbmd.getDriverName()+"\n");
            summary.append("Database Driver Version: ").append(dbmd.getDriverVersion()+"\n");
            
        } catch (SQLException e) {
            logger.error("Couldn't get database metadata!", e);
        } finally {
            try {
                if (con != null) {
                    con.close();
                }
            } catch (SQLException ex) {
                logger.warn("Couldn't close connection", ex);
            } 
            }
        return summary.toString();
      }

    /**
     * A property change listener that should be connected to the current parentType.
     * Its function is to keep the parent type name in the property map in sync with
     * the parent type's actual name.
     */
    private class ParentTypeNameSynchronizer implements PropertyChangeListener {

        public void propertyChange(PropertyChangeEvent evt) {
            if (evt.getSource() == parentType && "name".equals(evt.getPropertyName())) {
                properties.put(DBCS_CONNECTION_TYPE, (String) evt.getNewValue());
            }
        }
        
    }
    
    private final ParentTypeNameSynchronizer parentTypeUpdater = new ParentTypeNameSynchronizer();
    
    /**
     * These are the actual properties that appear in the file for this data source.
     * The getters for various properties will consult the parent type where appropriate
     * (for example, when a value is missing from this map).
     */
	private Map<String,String> properties;

    /**
     * This is the type (or class, but we don't mean Java class) for this data source.
     * It provides defaults for all connections to the same type of database (for instance,
     * all Oracle 8i data sources should have the same parent type, which knows where to
     * find the Oracle driver, and what an Oracle JDBC THIN URL looks like).
     * <p>
     * Warning: If you modify this value directly (not using setParentType()) then you have
     * to remove the listener from the old parent type and add it to the new one.
     */
    private SPDataSourceType parentType;
    
    /**
     * For purposes of setting the dropdown box in the DBCS panel we have to know
     * whether a datasource type has a parent or if it is a root. Since we always set
     * the parent type as a default one, there is no way to use a parentType == null
     * check to determine if a parent is set. This flag is set to true when setParent is called.
     */
    private boolean parentSet = false;
    
    /**
     * The collection of data sources that this data source belongs to.
     */
    private final DataSourceCollection parentCollection;
	
    /*
	 * constants used as keys to get into the properties
	 * map.  the shared heritage of this class explains why
	 * some constants use the prefix DBCS_ while others use
	 * the prefix PL_.
	 */
	public static final String DBCS_DRIVER_CLASS = "JDBC Driver Class";
	public static final String DBCS_URL = "JDBC URL";
	public static final String DBCS_JAR = "JAR File";
    public static final String DBCS_CONNECTION_TYPE = "Connection Type";
    
	public static final String PL_LOGICAL = "Logical";
	public static final String PL_TYPE = "Type";
	public static final String PL_DSN = "DSN";
	public static final String PL_SCHEMA_OWNER = "PL Schema Owner";
	public static final String PL_UID = "UID";
	public static final String PL_PWD = "PWD";
	public static final String PL_TNS = "TNS Name";
	public static final String PL_DATABASE_NAME = "Database Name";
	public static final String PL_IP = "IP";
	public static final String PL_PORT = "PORT";

    /**
	 * This field is transient; don't access it directly becuase it
	 * will disappear when this instance is serialized.
	 */
	private transient PropertyChangeSupport pcs;

	/**
	 * Returns this DataSource's property change support, creating
	 * a new one if necessary.
	 */
	private PropertyChangeSupport getPcs() {
		if (pcs == null) pcs = new PropertyChangeSupport(this);
		return pcs;
	}

    /**
     * Creates a new SPDataSource with a blank parent type and all other properties set to null.
     */
	public SPDataSource(DataSourceCollection parentCollection) {
		properties = new LinkedHashMap<String,String>();
		setParentType(new SPDataSourceType());
		parentSet = false;
        this.parentCollection = parentCollection;
	}

    /**
     * Copy constructor. Creates a semi-independent copy of the given data source.
     * <p>
     * This is for testing only!  The ramifications of using this constructor
     * in production have not been thought through!
     *
     * @param copyMe the SPDataSource to make a copy of.
     */
    public SPDataSource(SPDataSource copyMe) {
        properties = new LinkedHashMap<String, String>(copyMe.properties);
        setParentType(copyMe.parentType);
        parentCollection = copyMe.parentCollection;
    }

	/**
	 * The method that actually modifies the property map.
	 *
	 * @param key The key to use in the map (this will be a PL.INI property name)
	 * @param value The value that corresponds with the key
	 * @param propertyName The name of the Java Beans property that changed.  This will
	 * be the property name in the resulting PropertyChangeEvent.
	 * @return The old value of the property.
	 */
	private String putImpl(String key, String value, String propertyName) {
		String oldValue = get(key);
		properties.put(key, value);
		getPcs().firePropertyChange(propertyName, oldValue, value);
		return oldValue;
	}

	/**
	 * Adds the given key to the map.
	 *
	 * @param key The key to use.
	 * @param value The value to associate with key.
	 * @return The old value of the property.
	 */
	public String put(String key, String value) {
		return putImpl(key, value, key);
	}

	public String get(String key) {
		return properties.get(key);
	}

	/**
	 * Returns a read-only view of this data source's properties.
	 */
	public Map<String,String> getPropertiesMap() {
		return Collections.unmodifiableMap(properties);
	}

	/**
	 * Prints some info from this data source.  For use in debugging.
	 */
	public String toString() {
		return getDisplayName();
	}

	/**
	 * Compares all properties of this data source to those of the other.
	 * If there are any differences, returns false.  Otherwise, returns true.
	 */
	@Override
	public boolean equals(Object o) {
		if (o == null)
            return false;
        if (!(o instanceof SPDataSource))
            return false;
		SPDataSource other = (SPDataSource) o;
		return this.properties.equals(other.properties);
	}

	/**
	 * Returns a hash that depends on all property values.
	 */
	@Override
	public int hashCode() {
		return properties.hashCode();
	}
    
    /**
     * Creates a new connection to the database described by the properties
     * of this Data Source.  Doesn't do any pooling, so if you want a pool
     * of connections then make it yourself (the Architect does this in SQLDatabase.getConnection()).
     */
    public Connection createConnection() throws SQLException {
        
        try {
            if (getParentType() == null) {
                throw new SQLException("Data Source \""+getName()+"\" has no database type.");
            }

            getParentType().checkConnectPrereqs();
            
            if (getUrl() == null
                    || getUrl().trim().length() == 0) {
                throw new SQLException("Data Source \""+getName()+"\" has no JDBC URL.");
            }

            // don't check for missing username -- this is allowed when using SQL Server "integrated security"
            
            if (logger.isDebugEnabled()) {
                ClassLoader cl = this.getClass().getClassLoader();
                StringBuffer loaders = new StringBuffer();
                loaders.append("Local Classloader chain: ");
                while (cl != null) {
                    loaders.append(cl).append(", ");
                    cl = cl.getParent();
                }
                logger.debug(loaders);
            }
            Driver driver = (Driver) Class.forName(getDriverClass(), true, parentType.getJdbcClassLoader()).newInstance();
            logger.info("Driver Class "+getDriverClass()+" loaded without exception");
            if (!driver.acceptsURL(getUrl())) {
                throw new SQLException("Couldn't connect to database \""+getName()+"\":\n"
                        +"JDBC Driver "+getDriverClass()+"\n"
                        +"does not accept the URL "+getUrl());
            }
            Properties connectionProps = new Properties();
            connectionProps.setProperty("user", getUser());
            connectionProps.setProperty("password", getPass());
            
            // XXX this platform-specific fix should be moved into the config file
            //     once we switch from pl.ini to an XML format and allow arbitrary
            //     user-specified connection properties.
            if (getDriverClass().endsWith("OracleDriver")) {
                connectionProps.setProperty("remarksReporting", "true");
            }
            
            Connection realConnection = driver.connect(getUrl(), connectionProps);
            if (realConnection == null) {
                throw new SQLException("JDBC Driver returned a null connection!");
            }
            Connection connection = ConnectionDecorator.createFacade(realConnection);
            logger.debug("Connection class is: " + connection.getClass().getName());
            return connection;
        } catch (ClassNotFoundException e) {
            logger.warn("Driver Class not found", e);
            throw new SQLException("JDBC Driver \""+getDriverClass()+"\" not found.");
        } catch (InstantiationException e) {
            logger.error("Creating SQL Exception to conform to interface.  Real exception is: ", e);
            throw new SQLException("Couldn't create an instance of the " +
                    "JDBC driver '"+getDriverClass()+"'. "+e.getMessage());
        } catch (IllegalAccessException e) {
            logger.error("Creating SQL Exception to conform to interface.  Real exception is: ", e);
            throw new SQLException("Couldn't connect to database because the " +
                    "JDBC driver has no public constructor (this is bad). "+e.getMessage());
        }
    }

	// --------------------- property change ---------------------------

	/**
	 * Registers the given object as a listener to property changes on this
	 * SPDataSource.
	 */
	public void addPropertyChangeListener(PropertyChangeListener l) {
		getPcs().addPropertyChangeListener(l);
	}

	/**
	 * Removes the given object from the listener list, if it was on that list.
	 * Does nothing if l is not a property change listener of this data source.
	 */
	public void removePropertyChangeListener(PropertyChangeListener l) {
		getPcs().removePropertyChangeListener(l);
	}

	/**
	 * Returns an unmodifiable view of the list of property change listeners.
	 */
	public List<PropertyChangeListener> getPropertyChangeListeners() {
		return Collections.unmodifiableList(Arrays.asList(pcs.getPropertyChangeListeners()));
	}

    // ------------------- accessors and mutators for actual instance variables ------------------------

    /**
     * Returns the parent type configured for this data source.
     */
    public SPDataSourceType getParentType() {
        return parentType;
    }

    /**
     * Sets the parent type configured for this data source, and fires a
     * property change event if the new value differs from the existing one.
     */
    public void setParentType(SPDataSourceType type) {
        if (parentType != null) {
            parentType.removePropertyChangeListener(parentTypeUpdater);
        }

        parentType = type;
        
        if (parentType != null) {
            parentType.addPropertyChangeListener(parentTypeUpdater);
        }
        parentSet = true;
        putImpl(DBCS_CONNECTION_TYPE, type.getName(), "parentType");
    }

	// ------------------- accessors and mutators for the flat property stuff ------------------------

	/**
	 * Gets the value of name
	 *
	 * @return the value of name
	 */
	public String getName() {
		return get(PL_LOGICAL);
	}

	/**
	 * Sets the value of name. 
	 * @param argName Value to assign to this.name
	 */
	public void setName(String argName){
		putImpl(PL_LOGICAL, argName, "name");
	}

	/**
	 * Gets the value of displayName
	 *
	 * @return the value of displayName
	 */
	public String getDisplayName() {
		return get(PL_LOGICAL);
	}

	/**
	 * Sets the value of displayName
	 *
	 * @param argDisplayName Value to assign to this.displayName
	 */
	public void setDisplayName(String argDisplayName){
		putImpl(PL_LOGICAL, argDisplayName, "name");
	}

	/**
	 * Gets the value of url
	 *
	 * @return the value of url
	 */
	public String getUrl() {
		return get(DBCS_URL);
	}

	/**
	 * Sets the value of url
	 *
	 * @param argUrl Value to assign to this.url
	 */
	public void setUrl(String argUrl) {
		putImpl(DBCS_URL, argUrl, "url");
	}

	/**
	 * Gets the value of driverClass
	 *
	 * @return the value of driverClass
	 */
	public String getDriverClass() {
        if (getParentType() == null) {
            return null;
        } else {
            return getParentType().getJdbcDriver();
        }
	}

	/**
	 * Gets the value of user
	 *
	 * @return the value of user
	 */
	public String getUser() {
		return get(PL_UID);
	}

	/**
	 * Sets the value of user
	 *
	 * @param argUser Value to assign to this.user
	 */
	public void setUser(String argUser) {
		putImpl(PL_UID, argUser, "user");
	}

	/**
	 * Gets the value of pass
	 *
	 * @return the value of pass
	 */
	public String getPass() {
		return get(PL_PWD);
	}

	/**
	 * Sets the value of pass
	 *
	 * @param argPass Value to assign to this.pass
	 */
	public void setPass(String argPass){
		putImpl(PL_PWD, argPass, "pass");
	}

	public String getPlSchema() {
        return properties.get(PL_SCHEMA_OWNER);
    }

    public void setPlSchema(String schema) {
        putImpl(PL_SCHEMA_OWNER, schema, "plSchema");
    }

	public String getPlDbType() {
        return properties.get(PL_TYPE);
    }

	public void setPlDbType(String type) {
        putImpl(PL_TYPE, type, "plDbType");
    }
    
	public String getOdbcDsn() {
        return properties.get(PL_DSN);
    }

    public void setOdbcDsn(String dsn) {
        putImpl(PL_DSN, dsn, "odbcDsn");
    }

    public boolean isParentSet() {
        return parentSet;
    }

    /**
     * Copies all properties from the given data source into this one.
     * After this method returns, this data source will specify the same
     * target database as the given data source.
     * 
     * @param dbcs The connection spec to copy from (must not be null).
     */
    public void copyFrom(SPDataSource dbcs) {
        properties.clear();
        
        // This is extremely, stupidly cheap.  The tree doesn't notice the change unless there's
        // a property change event for the data source's name.
        setName(dbcs.getName());

        for (Map.Entry<String, String> entry : dbcs.getPropertiesMap().entrySet()) {
            // this is non-ideal, because the property change events will not have correct property names
            put(entry.getKey(), entry.getValue());
        }
        
        setParentType(dbcs.getParentType());
    }

    /**
     * Returns the data source collection that this data source belongs to.
     */
    public DataSourceCollection getParentCollection() {
        return parentCollection;
    }
}
