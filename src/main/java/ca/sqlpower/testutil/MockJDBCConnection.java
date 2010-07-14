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

import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

/**
 * A fake connection object that we can use for unit testing.
 * 
 * @author fuerth
 * @version $Id: MockJDBCConnection.java 1600 2007-07-05 18:49:10Z fuerth $
 */
public class MockJDBCConnection implements Connection {

    private static final Logger logger = Logger.getLogger(MockJDBCConnection.class);
    
	private boolean autoCommit;
	private boolean readOnly;
	private String currentCatalog;
	private int transactionIsolation;
	private String url;
	private Properties properties;
	private MockJDBCDatabaseMetaData metaData;
	
    private Map<String, MockJDBCResultSet> resultSets = new TreeMap<String, MockJDBCResultSet>();
    
	public MockJDBCConnection(String url, Properties properties) {
		this.url = url;
		this.properties = properties;
		this.metaData = new MockJDBCDatabaseMetaData(this);
	}
	
	String getURL() {
		return url;
	}
	
	Properties getProperties() {
		return properties;
	}
	
    /**
     * Registers the given result set with this connection.  Any statement created
     * by this connection will return the given result set when an executeQuery()
     * call is made with a SQL string matching the regular expression given here.
     * <p>
     * When building the result set, remember that you can manipulate its metadata
     * as well as its rows of actual data.  See {@link MockJDBCResultSet#MockJDBCResultSet(int)}
     * and {@link MockJDBCResultSetMetaData} for details.
     * 
     * @param regex
     * @param rs
     */
    public void registerResultSet(String regex, MockJDBCResultSet rs) {
        resultSets.put(regex, rs);
    }

    /**
     * Allows you to modify or augment the property list that was passed in
     * (from the JDBC URL) when this connection was created. The meaning of the
     * keys and values is described in the documentation for
     * {@link MockJDBCDriver#connect(String, Properties)}.
     * 
     * @param key
     *            The key to set or replace the value of.
     * @param value
     *            The new value.
     */
    public void setProperty(String key, String value) {
        logger.debug("Changing property '"+key+"': '"+properties.getProperty(key)+"' -> '"+value+"'");
        properties.setProperty(key, value);
    }
    
    /**
     * Returns a result set whose registered regex matches the given SQL string.
     * 
     * @param sql The SQL query string to match against registered result sets
     * @return One of the registered result sets whose regex matches the SQL string,
     * or null if none of the registered result sets match.
     */
    MockJDBCResultSet resultsForQuery(String sql) {
        for (Map.Entry<String, MockJDBCResultSet> entry : resultSets.entrySet()) {
            System.out.println("Comparing \""+sql+"\" against \""+entry.getKey()+"\"");
            Pattern p = Pattern.compile(entry.getKey(), Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
            if (p.matcher(sql).matches()) {
                return entry.getValue();
            }
        }
        return null;
    }
    
	// ========= java.sql.Connection interface is below this line ========
	
	public Statement createStatement() throws SQLException {
		return new MockJDBCStatement(this);
	}

	public PreparedStatement prepareStatement(String sql) throws SQLException {
		throw new UnsupportedOperationException("Not implemented");
	}

	public CallableStatement prepareCall(String sql) throws SQLException {
		throw new UnsupportedOperationException("Not implemented");
	}

	public String nativeSQL(String sql) throws SQLException {
		return sql;
	}

	public void setAutoCommit(boolean autoCommit) throws SQLException {
		this.autoCommit = autoCommit;
	}

	public boolean getAutoCommit() throws SQLException {
		return autoCommit;
	}

	public void commit() throws SQLException {
		// do nothing
	}

	public void rollback() throws SQLException {
		// do nothing
	}

	public void close() throws SQLException {
		// do nothing
	}

	public boolean isClosed() throws SQLException {
		return false;
	}

	public DatabaseMetaData getMetaData() throws SQLException {
		return metaData;
	}

	public void setReadOnly(boolean readOnly) throws SQLException {
		this.readOnly = readOnly;
	}

	public boolean isReadOnly() throws SQLException {
		return readOnly;
	}

	public void setCatalog(String catalog) throws SQLException {
		if (getMetaData().getCatalogTerm() != null) {
			currentCatalog = catalog;
		} else {
			throw new SQLException("This Mock Database doesn't have catalogs");
		}
	}

	public String getCatalog() throws SQLException {
		return currentCatalog;
	}

	public void setTransactionIsolation(int level) throws SQLException {
		transactionIsolation = level;
	}

	public int getTransactionIsolation() throws SQLException {
		return transactionIsolation;
	}

	public SQLWarning getWarnings() throws SQLException {
		return null;
	}

	public void clearWarnings() throws SQLException {
		// do nothing
	}

	public Statement createStatement(int resultSetType, int resultSetConcurrency)
			throws SQLException {
		Statement stmt = new MockJDBCStatement(this);
		stmt.setFetchDirection(resultSetType);
		return stmt;
	}

	public PreparedStatement prepareStatement(String sql, int resultSetType,
			int resultSetConcurrency) throws SQLException {
		throw new UnsupportedOperationException("Not implemented");
	}

	public CallableStatement prepareCall(String sql, int resultSetType,
			int resultSetConcurrency) throws SQLException {
		throw new UnsupportedOperationException("Not implemented");
	}

	public Map<String, Class<?>> getTypeMap() throws SQLException {
		throw new UnsupportedOperationException("Not implemented");
	}

	public void setTypeMap(Map<String, Class<?>> arg0) throws SQLException {
		throw new UnsupportedOperationException("Not implemented");
	}

	public void setHoldability(int holdability) throws SQLException {
		throw new UnsupportedOperationException("Not implemented");
	}

	public int getHoldability() throws SQLException {
		throw new UnsupportedOperationException("Not implemented");
	}

	public Savepoint setSavepoint() throws SQLException {
		throw new UnsupportedOperationException("Not implemented");
	}

	public Savepoint setSavepoint(String name) throws SQLException {
		throw new UnsupportedOperationException("Not implemented");
	}

	public void rollback(Savepoint savepoint) throws SQLException {
		// do nothing
	}

	public void releaseSavepoint(Savepoint savepoint) throws SQLException {
		throw new UnsupportedOperationException("Not implemented");
	}

	public Statement createStatement(int resultSetType,
			int resultSetConcurrency, int resultSetHoldability)
			throws SQLException {
		return new MockJDBCStatement(this);
	}

	public PreparedStatement prepareStatement(String sql, int resultSetType,
			int resultSetConcurrency, int resultSetHoldability)
			throws SQLException {
		throw new UnsupportedOperationException("Not implemented");
	}

	public CallableStatement prepareCall(String sql, int resultSetType,
			int resultSetConcurrency, int resultSetHoldability)
			throws SQLException {
		throw new UnsupportedOperationException("Not implemented");
	}

	public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys)
			throws SQLException {
		throw new UnsupportedOperationException("Not implemented");
	}

	public PreparedStatement prepareStatement(String sql, int[] columnIndexes)
			throws SQLException {
		throw new UnsupportedOperationException("Not implemented");
	}

	public PreparedStatement prepareStatement(String sql, String[] columnNames)
			throws SQLException {
		throw new UnsupportedOperationException("Not implemented");
	}

	public Array createArrayOf(String typeName, Object[] elements)
			throws SQLException {
		throw new UnsupportedOperationException("Currently it is only possible to wrap JDBC 3.");
	}

	public Blob createBlob() throws SQLException {
		throw new UnsupportedOperationException("Currently it is only possible to wrap JDBC 3.");
	}

	public Clob createClob() throws SQLException {
		throw new UnsupportedOperationException("Currently it is only possible to wrap JDBC 3.");
	}

	public NClob createNClob() throws SQLException {
		throw new UnsupportedOperationException("Currently it is only possible to wrap JDBC 3.");
	}

	public SQLXML createSQLXML() throws SQLException {
		throw new UnsupportedOperationException("Currently it is only possible to wrap JDBC 3.");
	}

	public Struct createStruct(String typeName, Object[] attributes)
			throws SQLException {
		throw new UnsupportedOperationException("Currently it is only possible to wrap JDBC 3.");
	}

	public Properties getClientInfo() throws SQLException {
		throw new UnsupportedOperationException("Currently it is only possible to wrap JDBC 3.");
	}

	public String getClientInfo(String name) throws SQLException {
		throw new UnsupportedOperationException("Currently it is only possible to wrap JDBC 3.");
	}

	public boolean isValid(int timeout) throws SQLException {
		throw new UnsupportedOperationException("Currently it is only possible to wrap JDBC 3.");
	}

	public void setClientInfo(Properties properties)
			throws SQLClientInfoException {
		throw new UnsupportedOperationException("Currently it is only possible to wrap JDBC 3.");
	}

	public void setClientInfo(String name, String value)
			throws SQLClientInfoException {
		throw new UnsupportedOperationException("Currently it is only possible to wrap JDBC 3.");
	}

	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		throw new UnsupportedOperationException("Currently it is only possible to wrap JDBC 3.");
	}

	public <T> T unwrap(Class<T> iface) throws SQLException {
		throw new UnsupportedOperationException("Currently it is only possible to wrap JDBC 3.");
	}

}
