/*
 * Copyright (c) 2007, SQL Power Group Inc.
 * 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in
 *       the documentation and/or other materials provided with the
 *       distribution.
 *     * Neither the name of SQL Power Group Inc. nor the names of its
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
/*
 * Created on Jun 8, 2005
 *
 * This code belongs to SQL Power Group Inc.
 */
package ca.sqlpower.sql.jdbcwrapper;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Savepoint;
import java.sql.Statement;
import java.util.Map;

import org.apache.log4j.Logger;


/**
 * The ConnectionDecorator wraps a JDBC Connection object and delegates all operations to it.
 * Some calls are intercepted for some types of database connections when the delegate connection
 * is not fully conformant to the JDBC standard.
 *
 * @author fuerth
 * @version $Id$
 */
public abstract class ConnectionDecorator implements Connection {
	private static Logger logger = Logger.getLogger(ConnectionDecorator.class);
	
	private int openStatementCount;
	
	/**
	 * The object to which all JDBC operations are delegated. 
	 */
	private Connection connection;
	
	/**
	 * Creates a new ConnectionDecorator which delegates to the given connection.
	 * Outside users can create a ConnectionDecorator using the public {@link #createFacade(Connection)} method.
	 * 
	 * @param delegate The object to which all JDBC operations will be delegated.
	 */
	protected ConnectionDecorator(Connection delegate) {
		this.connection = delegate;
	}
	
	/**
	 * Creates a new ConnectionDecorator (or appropriate subclass) which delegates to the given connection.
	 * Outside users can create a ConnectionDecorator using the public {@link #createFacade(Connection)} method.
	 * 
	 * @param delegate The object to which all JDBC operations will be delegated.
	 */
	public static Connection createFacade(Connection delegate) throws SQLException {
		logger.debug("static createFacade, driver class is: " + delegate.getClass().getName());
		logger.debug("static createFacade, driver name is: " + delegate.getMetaData().getDriverName());
		if (delegate.getMetaData().getDriverName().equals("PostgreSQL Native Driver")) {
			return new PostgresConnectionDecorator(delegate);
        } else if (delegate.getMetaData().getDriverName().equals("Oracle JDBC driver")) {
            return new OracleConnectionDecorator(delegate);
        } else if (delegate.getMetaData().getDriverName().equals("SQLServer")) {
            return new SQLServerConnectionDecorator(delegate);
        } else if (delegate.getMetaData().getDriverName().equals("MySQL-AB JDBC Driver")) {
            return new MySQLConnectionDecorator(delegate);
		} else if (delegate.getMetaData().getDriverName().equals("SQL Power Mock JDBC Database Driver")) {
			// we don't want to decorate these at all
			return delegate;
		} else {
			return new GenericConnectionDecorator(delegate);
		}
	}
    
	/**
	 * Returns the number of statements that have been opened but not closed on this connection.
	 * 
	 * @return
	 */
	public int getOpenStatementCount() {
		return openStatementCount;
	}
	
	/**
	 * Increments the count of open statements.  This is normally done from the StatementDecorator.
	 */
	protected void incrementOpenStatements() {
		openStatementCount++;
		logger.debug("New Statement opened: Count is "+openStatementCount);
	}

	/**
	 * Decrements the count of open statements.  This is normally done from the StatementDecorator.
	 */
	protected void decrementOpenStatements() {
		openStatementCount--;
		logger.debug("Existing Statement closed: Count is "+openStatementCount);
	}
	
	/**
	 * Subclasses must implement this method by creating and returning a new
	 * Statement decorator appropriate for the database platform.
	 */
	protected abstract Statement makeStatementDecorator(Statement stmt);

	/**
	 * Subclasses must implement this method by creating and returning a new
	 * PreparedStatement decorator appropriate for the database platform.
	 */
	protected abstract PreparedStatement makePreparedStatementDecorator(PreparedStatement pstmt);

	// =================== Connection Interface implemented below this line ==============
	/**
	 * @throws java.sql.SQLException
	 */
	public void clearWarnings() throws SQLException {
		connection.clearWarnings();
	}

	/**
	 * @throws java.sql.SQLException
	 */
	public void close() throws SQLException {
		connection.close();
	}

	/**
	 * @throws java.sql.SQLException
	 */
	public void commit() throws SQLException {
		connection.commit();
	}
	
	/**
	 * @return
	 * @throws java.sql.SQLException
	 */
	public Statement createStatement() throws SQLException {
		Statement stmt = makeStatementDecorator(connection.createStatement());
		return stmt;
	}
	
	/**
	 * @param resultSetType
	 * @param resultSetConcurrency
	 * @return
	 * @throws java.sql.SQLException
	 */
	public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
		Statement stmt = makeStatementDecorator(
				connection.createStatement(
						resultSetType,
						resultSetConcurrency));
		return stmt;
	}
	
	/**
	 * @param resultSetType
	 * @param resultSetConcurrency
	 * @param resultSetHoldability
	 * @return
	 * @throws java.sql.SQLException
	 */
	public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability)
	throws SQLException {
		Statement stmt = makeStatementDecorator(
				connection.createStatement(
						resultSetType,
						resultSetConcurrency,
						resultSetHoldability));
		return stmt;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		return connection.equals(obj);
	}
	
	/**
	 * @return
	 * @throws java.sql.SQLException
	 */
	public boolean getAutoCommit() throws SQLException {
		return connection.getAutoCommit();
	}
	
	/**
	 * @return
	 * @throws java.sql.SQLException
	 */
	public String getCatalog() throws SQLException {
		return connection.getCatalog();
	}
	
	/**
	 * @return
	 * @throws java.sql.SQLException
	 */
	public int getHoldability() throws SQLException {
		return connection.getHoldability();
	}
	/**
	 * @return
	 * @throws java.sql.SQLException
	 */
	public DatabaseMetaData getMetaData() throws SQLException {
		return connection.getMetaData();
	}
	/**
	 * @return
	 * @throws java.sql.SQLException
	 */
	public int getTransactionIsolation() throws SQLException {
		return connection.getTransactionIsolation();
	}
	/**
	 * @return
	 * @throws java.sql.SQLException
	 */
	public Map<String, Class<?>> getTypeMap() throws SQLException {
		return connection.getTypeMap();
	}
	/**
	 * @return
	 * @throws java.sql.SQLException
	 */
	public SQLWarning getWarnings() throws SQLException {
		return connection.getWarnings();
	}
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return connection.hashCode();
	}
	/**
	 * @return
	 * @throws java.sql.SQLException
	 */
	public boolean isClosed() throws SQLException {
		return connection.isClosed();
	}
	/**
	 * @return
	 * @throws java.sql.SQLException
	 */
	public boolean isReadOnly() throws SQLException {
		return connection.isReadOnly();
	}
	/**
	 * @param sql
	 * @return
	 * @throws java.sql.SQLException
	 */
	public String nativeSQL(String sql) throws SQLException {
		return connection.nativeSQL(sql);
	}
	/**
	 * @param sql
	 * @return
	 * @throws java.sql.SQLException
	 */
	public CallableStatement prepareCall(String sql) throws SQLException {
		return connection.prepareCall(sql);
	}
	/**
	 * @param sql
	 * @param resultSetType
	 * @param resultSetConcurrency
	 * @return
	 * @throws java.sql.SQLException
	 */
	public CallableStatement prepareCall(String sql, int resultSetType,
			int resultSetConcurrency) throws SQLException {
		return connection.prepareCall(sql, resultSetType, resultSetConcurrency);
	}
	/**
	 * @param sql
	 * @param resultSetType
	 * @param resultSetConcurrency
	 * @param resultSetHoldability
	 * @return
	 * @throws java.sql.SQLException
	 */
	public CallableStatement prepareCall(String sql, int resultSetType,
			int resultSetConcurrency, int resultSetHoldability)
	throws SQLException {
		return connection.prepareCall(sql, resultSetType, resultSetConcurrency,
				resultSetHoldability);
	}
	/**
	 * @param sql
	 * @return
	 * @throws java.sql.SQLException
	 */
	public PreparedStatement prepareStatement(String sql) throws SQLException {
		return makePreparedStatementDecorator(connection.prepareStatement(sql));
	}
	/**
	 * @param sql
	 * @param autoGeneratedKeys
	 * @return
	 * @throws java.sql.SQLException
	 */
	public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys)
	throws SQLException {
		return makePreparedStatementDecorator(connection.prepareStatement(sql, autoGeneratedKeys));
	}
	/**
	 * @param sql
	 * @param resultSetType
	 * @param resultSetConcurrency
	 * @return
	 * @throws java.sql.SQLException
	 */
	public PreparedStatement prepareStatement(String sql, int resultSetType,
			int resultSetConcurrency) throws SQLException {
		return makePreparedStatementDecorator(connection.prepareStatement(sql, resultSetType,
				resultSetConcurrency));
	}
	/**
	 * @param sql
	 * @param resultSetType
	 * @param resultSetConcurrency
	 * @param resultSetHoldability
	 * @return
	 * @throws java.sql.SQLException
	 */
	public PreparedStatement prepareStatement(String sql, int resultSetType,
			int resultSetConcurrency, int resultSetHoldability)
	throws SQLException {
		return makePreparedStatementDecorator(connection.prepareStatement(sql, resultSetType,
				resultSetConcurrency, resultSetHoldability));
	}
	/**
	 * @param sql
	 * @param columnIndexes
	 * @return
	 * @throws java.sql.SQLException
	 */
	public PreparedStatement prepareStatement(String sql, int[] columnIndexes)
	throws SQLException {
		return makePreparedStatementDecorator(connection.prepareStatement(sql, columnIndexes));
	}
	/**
	 * @param sql
	 * @param columnNames
	 * @return
	 * @throws java.sql.SQLException
	 */
	public PreparedStatement prepareStatement(String sql, String[] columnNames)
	throws SQLException {
		return makePreparedStatementDecorator(connection.prepareStatement(sql, columnNames));
	}
	/**
	 * @param savepoint
	 * @throws java.sql.SQLException
	 */
	public void releaseSavepoint(Savepoint savepoint) throws SQLException {
		connection.releaseSavepoint(savepoint);
	}
	/**
	 * @throws java.sql.SQLException
	 */
	public void rollback() throws SQLException {
		connection.rollback();
	}
	/**
	 * @param savepoint
	 * @throws java.sql.SQLException
	 */
	public void rollback(Savepoint savepoint) throws SQLException {
		connection.rollback(savepoint);
	}
	/**
	 * @param autoCommit
	 * @throws java.sql.SQLException
	 */
	public void setAutoCommit(boolean autoCommit) throws SQLException {
		connection.setAutoCommit(autoCommit);
	}
	/**
	 * @param catalog
	 * @throws java.sql.SQLException
	 */
	public void setCatalog(String catalog) throws SQLException {
		connection.setCatalog(catalog);
	}
	/**
	 * @param holdability
	 * @throws java.sql.SQLException
	 */
	public void setHoldability(int holdability) throws SQLException {
		connection.setHoldability(holdability);
	}
	/**
	 * @param readOnly
	 * @throws java.sql.SQLException
	 */
	public void setReadOnly(boolean readOnly) throws SQLException {
		connection.setReadOnly(readOnly);
	}
	/**
	 * @return
	 * @throws java.sql.SQLException
	 */
	public Savepoint setSavepoint() throws SQLException {
		return connection.setSavepoint();
	}
	/**
	 * @param name
	 * @return
	 * @throws java.sql.SQLException
	 */
	public Savepoint setSavepoint(String name) throws SQLException {
		return connection.setSavepoint(name);
	}
	/**
	 * @param level
	 * @throws java.sql.SQLException
	 */
	public void setTransactionIsolation(int level) throws SQLException {
		connection.setTransactionIsolation(level);
	}
	/**
	 * @param map
	 * @throws java.sql.SQLException
	 */
	public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
		connection.setTypeMap(map);
	}
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return connection.toString();
	}
}
