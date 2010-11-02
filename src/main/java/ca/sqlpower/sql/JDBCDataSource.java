/*
 * Copyright (c) 2009, SQL Power Group Inc.
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

package ca.sqlpower.sql;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.Properties;

import org.apache.log4j.Logger;

import ca.sqlpower.sql.jdbcwrapper.ConnectionDecorator;

public class JDBCDataSource extends SPDataSource {

	public static final Logger logger = Logger.getLogger(JDBCDataSource.class);

	/*
	 * constants used as keys to get into the properties map. the shared
	 * heritage of this class explains why some constants use the prefix DBCS_
	 * while others use the prefix PL_.
	 */
	public static final String DBCS_DRIVER_CLASS = "JDBC Driver Class";
	public static final String DBCS_URL = "JDBC URL";
	public static final String DBCS_JAR = "JAR File";
	public static final String DBCS_CONNECTION_TYPE = "Connection Type";

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
	public static URL jarSpecToFile(String jarFileName,
			ClassLoader classLoader, URI serverBaseURI) {
		URL location;
		try {
			if (jarFileName.startsWith(BUILTIN)) {
				String jarName = jarFileName.substring(BUILTIN.length());
				location = classLoader.getResource(jarName);
				if (location == null) {
					logger.warn("Couldn't find built-in system resource \""
							+ jarName + "\". Skipping it.");
				}
			} else if (jarFileName.startsWith(SPDataSource.SERVER)) {
				if (serverBaseURI == null) {
					throw new IllegalArgumentException(
							"The JDBC driver at "
									+ jarFileName
									+ " can't"
									+ " be located because no server base URI was specified");
				}
				String jarFilePath = jarFileName.substring(SPDataSource.SERVER
						.length());

				// Need to decode the URI to a URL to convert escaped characters
				// to their real values,
				// ie spaces described as %20 will be replaced by actual spaces
				location = new URL(URLDecoder.decode(serverBaseURI.toString(),
						"UTF-8"));

				location = new URL(location, jarFilePath);
			} else if (jarFileName.startsWith("file:")) {				
				// this spec has already been turned into a file: url
				location = new URI(jarFileName).toURL();
			} else {
				location = new File(jarFileName).toURI().toURL();
			}
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
		return location;
	}

	/**
	 * Builds a string of the DB connection properties to display to the user
	 * when testing a DB connection or in a log of a SQL query. <br>
	 * NOTE: This method uses the StringBuilder class to create strings which is
	 * not thread safe.
	 * <p>
	 * 
	 * @param ds
	 *            - The SPDataSource for which connection details are wanted
	 *            <p>
	 * @param testConnection
	 *            - This boolean should be true when the string is to be
	 *            displayed when testing a connection. False, otherwise. The
	 *            boolean decides whether or not to display the datasource's
	 *            name. There is no point in displaying the datasource's name if
	 *            it is already shown to the user as it is when testing a
	 *            connection (in one of the text fields)
	 *            <p>
	 * @return A string of the DB connection properties
	 */
	public static String getConnectionInfoString(JDBCDataSource ds,
			boolean testConnection) {
		// If the connection fails, it returns an empty string
		StringBuilder summary = new StringBuilder("");
		Connection con = null;
		try {
			con = ds.createConnection();
			DatabaseMetaData dbmd = con.getMetaData();
			if (!testConnection) {
				summary.append(ds.getDisplayName() + ": \n");
			}
			summary.append("Database Product Name: ").append(
					dbmd.getDatabaseProductName() + "\n");
			summary.append("Database Product Version: ").append(
					dbmd.getDatabaseProductVersion() + "\n");
			summary.append("Database Driver Name: ").append(
					dbmd.getDriverName() + "\n");
			summary.append("Database Driver Version: ").append(
					dbmd.getDriverVersion() + "\n");

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
	 * A property change listener that should be connected to the current
	 * parentType. Its function is to keep the parent type name in the property
	 * map in sync with the parent type's actual name.
	 */
	private class ParentTypeNameSynchronizer implements PropertyChangeListener {

		public void propertyChange(PropertyChangeEvent evt) {
			if (evt.getSource() == parentType
					&& "name".equals(evt.getPropertyName())) {
				put(DBCS_CONNECTION_TYPE, (String) evt.getNewValue());
			}
		}

	}

	private final ParentTypeNameSynchronizer parentTypeUpdater = new ParentTypeNameSynchronizer();

	/**
	 * This is the type (or class, but we don't mean Java class) for this data
	 * source. It provides defaults for all connections to the same type of
	 * database (for instance, all Oracle 8i data sources should have the same
	 * parent type, which knows where to find the Oracle driver, and what an
	 * Oracle JDBC THIN URL looks like).
	 * <p>
	 * Warning: If you modify this value directly (not using setParentType())
	 * then you have to remove the listener from the old parent type and add it
	 * to the new one.
	 */
	private JDBCDataSourceType parentType;

	/**
	 * For purposes of setting the dropdown box in the DBCS panel we have to
	 * know whether a datasource type has a parent or if it is a root. Since we
	 * always set the parent type as a default one, there is no way to use a
	 * parentType == null check to determine if a parent is set. This flag is
	 * set to true when setParent is called.
	 */
	private boolean parentSet = false;

	public JDBCDataSource(DataSourceCollection parentCollection) {
		super(parentCollection);
		setParentType(new JDBCDataSourceType(
				parentCollection.getServerBaseURI()));
		parentSet = false;
	}

	public JDBCDataSource(SPDataSource copy) {
		super(copy);
		if (copy instanceof JDBCDataSource) {
			setParentType(((JDBCDataSource) copy).parentType);
		}
	}

	/**
	 * Creates a new connection to the database described by the properties of
	 * this Data Source. Doesn't do any pooling, so if you want a pool of
	 * connections then make it yourself (the Architect does this in
	 * SQLDatabase.getConnection()).
	 */
	public Connection createConnection() throws SQLException {

		try {
			if (getParentType() == null) {
				throw new SQLException("Data Source \"" + getName()
						+ "\" has no database type.");
			}

			getParentType().checkConnectPrereqs();

			if (getUrl() == null || getUrl().trim().length() == 0) {
				throw new SQLException("Data Source \"" + getName()
						+ "\" has no JDBC URL.");
			}

			// don't check for missing username -- this is allowed when using
			// SQL Server "integrated security"

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
			Driver driver = (Driver) Class.forName(getDriverClass(), true,
					getParentType().getJdbcClassLoader()).newInstance();
			logger.info("Driver Class " + getDriverClass()
					+ " loaded without exception");
			if (!driver.acceptsURL(getUrl())) {
				throw new SQLException("Couldn't connect to database \""
						+ getName() + "\":\n" + "JDBC Driver "
						+ getDriverClass() + "\n" + "does not accept the URL "
						+ getUrl());
			}
			Properties connectionProps = new Properties();
			connectionProps.setProperty("user", getUser());
			connectionProps.setProperty("password", getPass());

			// XXX this platform-specific fix should be moved into the config
			// file
			// once we switch from pl.ini to an XML format and allow arbitrary
			// user-specified connection properties.
			if (getDriverClass().endsWith("OracleDriver")) {
				connectionProps.setProperty("remarksReporting", "true");
			}

			Connection realConnection = driver.connect(getUrl(),
					connectionProps);
			if (realConnection == null) {
				throw new SQLException(
						"JDBC Driver returned a null connection!");
			}
			Connection connection = ConnectionDecorator
					.createFacade(realConnection);
			logger.debug("Connection class is: "
					+ connection.getClass().getName());
			return connection;
		} catch (ClassNotFoundException e) {
			logger.warn("Driver Class not found", e);
			SQLException sqlException = new SQLException("JDBC Driver \""
					+ getDriverClass() + "\" not found.");
			sqlException.initCause(e);
			throw sqlException;
		} catch (InstantiationException e) {
			logger.error(
					"Creating SQL Exception to conform to interface.  Real exception is: ",
					e);
			throw new SQLException("Couldn't create an instance of the "
					+ "JDBC driver '" + getDriverClass() + "'. "
					+ e.getMessage());
		} catch (IllegalAccessException e) {
			logger.error(
					"Creating SQL Exception to conform to interface.  Real exception is: ",
					e);
			throw new SQLException("Couldn't connect to database because the "
					+ "JDBC driver has no public constructor (this is bad). "
					+ e.getMessage());
		}
	}

	public void copyFrom(JDBCDataSource dbcs) {
		super.copyFrom(dbcs);
		setParentType(dbcs.getParentType());
	}

	// ------------------- accessors and mutators for the flat property stuff
	// ------------------------

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
	 * @param argUrl
	 *            Value to assign to this.url
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
	 * @param argUser
	 *            Value to assign to this.user
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
	 * @param argPass
	 *            Value to assign to this.pass
	 */
	public void setPass(String argPass) {
		putImpl(PL_PWD, argPass, "pass");
	}

	public String getPlSchema() {
		return get(PL_SCHEMA_OWNER);
	}

	public void setPlSchema(String schema) {
		putImpl(PL_SCHEMA_OWNER, schema, "plSchema");
	}

	public String getPlDbType() {
		return get(PL_TYPE);
	}

	public void setPlDbType(String type) {
		putImpl(PL_TYPE, type, "plDbType");
	}

	public String getOdbcDsn() {
		return get(PL_DSN);
	}

	public void setOdbcDsn(String dsn) {
		putImpl(PL_DSN, dsn, "odbcDsn");
	}

	/**
	 * Returns the parent type configured for this data source.
	 */
	public JDBCDataSourceType getParentType() {
		return parentType;
	}

	/**
	 * Sets the parent type configured for this data source, and fires a
	 * property change event if the new value differs from the existing one.
	 */
	public void setParentType(JDBCDataSourceType type) {
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

	public boolean isParentSet() {
		return parentSet;
	}

	/**
	 * Prints some info from this data source. For use in debugging.
	 */
	public String toString() {
		return getDisplayName();
	}

}
