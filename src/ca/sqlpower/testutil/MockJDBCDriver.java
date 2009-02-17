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
package ca.sqlpower.testutil;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;

/**
 * A driver for the MockJDBCDatabase, which we can use for testing.
 * 
 * <p>The properties you pass to connect() are really important.  They define how the
 * resulting database "connection" will behave.
 * 
 * <dl>
 *  <dd>name</dd>
 *   <dt>The name for the connection instance being created. The connection can be
 *       retrieved via </dt>
 *  <dd>dbmd.*</dd>
 *   <dt>These properties define the return value of various DatabaseMetaData methods</dt>
 *  <dd>dbmd.catalogTerm</dd>
 *   <dt>The name this database uses for catalogs.  If not present, this
 *       database will not support catalogs.<dt>
 *  <dd>dbmd.schemaTerm</dd>
 *   <dt>The name this database uses for schemas.  If not present, this
 *       database will not support schemas. <dt>
 *  <dd>catalogs={list}</dd>
 *    <dt>A comma-separated list of catalogs this database claims to have<dt>
 *  <dd>schemas[.catalog]={list}</dd>
 *    <dt>A comma-separated list of schemas this database claims to have
 *        in catalog.  If the database doesn't have catalogs, leave out the ".catalog" part.
 *  <dd>tables[.catalog][.schema]={list}</dd>
 *    <dt>A comma-separated list of tables in the named schema.catalog.  Leave out the ".catalog"
 *        or ".schema" part if you've configured this database to be schemaless or catalogless.</dt>
 *  <dd>autoincrement_cols={list}</dd>
 *    <dt>A comma-separated list of fully-qualified column names (catalog.schema.table.column)
 *        which will be reported by DatabaseMetaData.getColumns as being auto-increment.
 * </dl>
 * 
 * <p>
 * If you want to change this setup after creating the connection (in order to simulate
 * DDL activity in a database), you can.
 * 
 * @author fuerth
 * @version $Id: MockJDBCDriver.java 1600 2007-07-05 18:49:10Z fuerth $
 */
public class MockJDBCDriver implements Driver {

	private static final Logger logger = Logger.getLogger(MockJDBCDriver.class);
	
	private static final Map<String, MockJDBCConnection> namedConnections =
	    new HashMap<String, MockJDBCConnection>();
	
	public Connection connect(String url, Properties info) throws SQLException {
		String params = url.substring("jdbc:mock:".length());
		String keyValuePairs[] = params.split("&");
		for (String keyvalue : Arrays.asList(keyValuePairs)) {
			String kv[] = keyvalue.split("=");
			logger.debug("Found URL property '"+kv[0]+"' = '"+kv[1]+"'");
			info.put(kv[0], kv[1]);
		}
		
		MockJDBCConnection newConnection = new MockJDBCConnection(url, info);
		if (info.getProperty("name") != null) {
		    namedConnections.put(info.getProperty("name"), newConnection);
		}
		
        return newConnection;
	}

	public boolean acceptsURL(String url) throws SQLException {
		return url.startsWith("jdbc:mock");
	}

	public DriverPropertyInfo[] getPropertyInfo(String url, Properties info)
			throws SQLException {
		return new DriverPropertyInfo[0];
	}

	public int getMajorVersion() {
		return 0;
	}

	public int getMinorVersion() {
		return 0;
	}

	public boolean jdbcCompliant() {
		return false;
	}

    /**
     * Returns the existing connection with the given name, which must have
     * already been created via the {@link #connect(String, Properties)} method.
     * 
     * @param name
     *            The connection name as originally specified in
     *            {@link #connect(String, Properties)}.
     * @return The named connection, or null if no connection with that name has
     *         been created.
     */
	public static MockJDBCConnection getConnection(String name) {
	    return namedConnections.get(name);
	}
}
