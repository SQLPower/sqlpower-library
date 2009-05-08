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
	    String driverName = delegate.getMetaData().getDriverName();
		logger.debug("static createFacade, driver class is: " + delegate.getClass().getName());
        logger.debug("static createFacade, driver name is: " + driverName);
		if (driverName.equals("PostgreSQL Native Driver")) {
			return new PostgresConnectionDecorator(delegate);
        } else if (driverName.equals("Oracle JDBC driver")) {
            return new OracleConnectionDecorator(delegate);
        } else if (driverName.equals("SQLServer")
        		|| driverName.equals("Microsoft SQL Server 2005 JDBC Driver")
        		|| driverName.equals("Microsoft SQL Server JDBC Driver 2.0")) {
            return new SQLServerConnectionDecorator(delegate);
        } else if (driverName.equals("MySQL-AB JDBC Driver")) {
            return new MySQLConnectionDecorator(delegate);
        } else if (driverName.equals("HSQL Database Engine Driver")) {
            return new HSQLDBConnectionDecorator(delegate);
		} else if (driverName.equals("SQL Power Mock JDBC Database Driver")) {
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
