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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;

public class MockJDBCStatement implements Statement {
	
    /**
     * The connection that owns this statement.
     */
	private MockJDBCConnection connection;
    
	private int maxFieldSize;
	private int maxRows;
	private int queryTimeout;

    /**
     * The fetch size property used by the related getters and setters.  The
     * value of this property does not otherwise affect this instance's behaviour.
     */
    private int fetchSize;

    /**
     * The fetchDirection used by the related getters and setters.  The
     * value of this property does not otherwise affect this instance's behaviour.
     */
    private int fetchDirection;

    /**
     * Creates a new statement belonging to the given connection.
     */
	MockJDBCStatement(MockJDBCConnection connection) {
		this.connection = connection;
	}
	
    /**
     * Looks for a result set whose regular expression matches the given query.
     * The connection that owns this statement may have one or more result sets
     * registered with it. Each registered result set is keyed by a regular expression.
     * See {@link MockJDBCConnection#registerResultSet(String, MockJDBCResultSet)} for
     * details.
     */
	public ResultSet executeQuery(String sql) throws SQLException {
        MockJDBCResultSet rs = connection.resultsForQuery(sql);
        if (rs == null) {
            throw new SQLException("No result set is registered for the query \""+sql+"\"");
        } else {
            rs.statement = this;
            rs.beforeFirst();
            return rs;
        }
	}

	public int executeUpdate(String sql) throws SQLException {
		throw new UnsupportedOperationException("Not implemented");
	}

	public void close() throws SQLException {
		// do nothing
	}

	public int getMaxFieldSize() throws SQLException {
		return maxFieldSize;
	}

	public void setMaxFieldSize(int max) throws SQLException {
		maxFieldSize = max;
	}

	public int getMaxRows() throws SQLException {
		return maxRows;
	}

	public void setMaxRows(int max) throws SQLException {
		maxRows = max;
	}

	public void setEscapeProcessing(boolean enable) throws SQLException {
		throw new UnsupportedOperationException("Not implemented");
	}

	public int getQueryTimeout() throws SQLException {
		return queryTimeout;
	}

	public void setQueryTimeout(int seconds) throws SQLException {
		queryTimeout = seconds;
	}

	public void cancel() throws SQLException {
		throw new UnsupportedOperationException("Not implemented");
	}

	public SQLWarning getWarnings() throws SQLException {
		throw new UnsupportedOperationException("Not implemented");
	}

	public void clearWarnings() throws SQLException {
		throw new UnsupportedOperationException("Not implemented");
	}

	public void setCursorName(String name) throws SQLException {
		throw new UnsupportedOperationException("Not implemented");
	}

	public boolean execute(String sql) throws SQLException {
		throw new UnsupportedOperationException("Not implemented");
	}

    /**
     * Creates a new empty result set.
     */
	public ResultSet getResultSet() throws SQLException {
		return new MockJDBCResultSet(this,0);
	}

	public int getUpdateCount() throws SQLException {
		throw new UnsupportedOperationException("Not implemented");
	}

	public boolean getMoreResults() throws SQLException {
		throw new UnsupportedOperationException("Not implemented");
	}

	public void setFetchDirection(int direction) throws SQLException {
		fetchDirection = direction;
	}

	public int getFetchDirection() throws SQLException {
        return fetchDirection;
	}

    /**
     * Stores the given fetch size value.  This value is not used internally,
     * but it will be returned by {@link #getFetchSize()}.
     */
	public void setFetchSize(int rows) throws SQLException {
        fetchSize = rows;
	}

    /**
     * Returns the value most recently set using {@link #setFetchSize(int)}.
     * If the value has not been set yet, returns 0.
     */
	public int getFetchSize() throws SQLException {
        return fetchSize;
	}

	public int getResultSetConcurrency() throws SQLException {
		throw new UnsupportedOperationException("Not implemented");
	}

	public int getResultSetType() throws SQLException {
		throw new UnsupportedOperationException("Not implemented");
	}

	public void addBatch(String sql) throws SQLException {
		throw new UnsupportedOperationException("Not implemented");
	}

	public void clearBatch() throws SQLException {
		throw new UnsupportedOperationException("Not implemented");
	}

	public int[] executeBatch() throws SQLException {
		throw new UnsupportedOperationException("Not implemented");
	}

	public Connection getConnection() throws SQLException {
		return connection;
	}

	public boolean getMoreResults(int current) throws SQLException {
		throw new UnsupportedOperationException("Not implemented");
	}

	public ResultSet getGeneratedKeys() throws SQLException {
		throw new UnsupportedOperationException("Not implemented");
	}

	public int executeUpdate(String sql, int autoGeneratedKeys)
			throws SQLException {
		throw new UnsupportedOperationException("Not implemented");
	}

	public int executeUpdate(String sql, int[] columnIndexes)
			throws SQLException {
		throw new UnsupportedOperationException("Not implemented");
	}

	public int executeUpdate(String sql, String[] columnNames)
			throws SQLException {
		throw new UnsupportedOperationException("Not implemented");
	}

	public boolean execute(String sql, int autoGeneratedKeys)
			throws SQLException {
		throw new UnsupportedOperationException("Not implemented");
	}

	public boolean execute(String sql, int[] columnIndexes) throws SQLException {
		throw new UnsupportedOperationException("Not implemented");
	}

	public boolean execute(String sql, String[] columnNames)
			throws SQLException {
		throw new UnsupportedOperationException("Not implemented");
	}

	public int getResultSetHoldability() throws SQLException {
		throw new UnsupportedOperationException("Not implemented");
	}

	public boolean isClosed() throws SQLException {
		throw new UnsupportedOperationException("Currently it is only possible to wrap JDBC 3.");
	}

	public boolean isPoolable() throws SQLException {
		throw new UnsupportedOperationException("Currently it is only possible to wrap JDBC 3.");
	}

	public void setPoolable(boolean poolable) throws SQLException {
		throw new UnsupportedOperationException("Currently it is only possible to wrap JDBC 3.");
	}

	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		throw new UnsupportedOperationException("Currently it is only possible to wrap JDBC 3.");
	}

	public <T> T unwrap(Class<T> iface) throws SQLException {
		throw new UnsupportedOperationException("Currently it is only possible to wrap JDBC 3.");
	}

}
