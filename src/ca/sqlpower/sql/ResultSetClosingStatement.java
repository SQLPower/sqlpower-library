package ca.sqlpower.sql;

import java.sql.*;
import org.apache.log4j.Logger;

/**
 * The ResultSetClosingStatement class helps enforce the documented
 * JDBC behaviour of "when a statement closes, so does its resultset."
 */
public class ResultSetClosingStatement implements Statement {

	/**
	 * Just a log4j logger named after this class.
	 */
	static Logger logger = Logger.getLogger(ResultSetClosingStatement.class);

	/**
	 * The actual statement that performs all the operations.
	 */
	protected Statement actualStatement;

	/**
	 * The connection object that created this statement.
	 */
	protected Connection con;

	/**
	 * The current result set (this implementation allows only one at a time).
	 */
	protected ResultSet results;

	/**
	 * The last query that was attempted on this Statement.  Useful if
	 * you get a SQLException and want to know what the query was.
	 */
	protected String lastQuery;

	ResultSetClosingStatement(Connection con, Statement actualStmt) {
		this.con=con;
		this.actualStatement=actualStmt;
		this.results=null;
	}

	public String getLastQuery() {
		return lastQuery;
	}
	
	// ------------- java.sql.Statement interface -----------

	/**
	 * Closes the current resultset (if there is one) then executes
	 * the given query.  Also logs the given sql statement at DEBUG
	 * level before executing it.
	 */
    public ResultSet executeQuery(String sql) throws SQLException {
		if (results != null) {
			results.close();
		}
		long startTime = 0L;
		long queryTime = 0L;
		lastQuery = sql;
		// 
		if (logger.isDebugEnabled()) {
			logger.debug(sql.hashCode() + ", " + sql);
			startTime = System.currentTimeMillis();
		}
		results = actualStatement.executeQuery(sql);
		//
		if (logger.isDebugEnabled()) {
			queryTime = System.currentTimeMillis() - startTime;
			logger.debug("low level query time for " + sql.hashCode() + " (ms): " + queryTime);
		}
		return results;
	}

	/**
	 * Executes the given statement without touching the existing
	 * resultset.  Also logs the given sql statement at DEBUG level
	 * before executing it.
	 */
    public int executeUpdate(String sql) throws SQLException {
		logger.debug(sql);
		lastQuery = sql;
		int rowCount = actualStatement.executeUpdate(sql);
		logger.debug("Affected "+rowCount+" rows");
		return rowCount;
	}
	    
    public void close() throws SQLException {
		if(results != null) {
			results.close();
		}
		actualStatement.close();
	}
	    
    public int getMaxFieldSize() throws SQLException {
		return actualStatement.getMaxFieldSize();
	}
        
    public void setMaxFieldSize(int max) throws SQLException {
		actualStatement.setMaxFieldSize(max);
	}
	    
    public int getMaxRows() throws SQLException {
		return actualStatement.getMaxRows();
	}
	    
    public void setMaxRows(int max) throws SQLException {
		actualStatement.setMaxRows(max);
	}
	    
    public void setEscapeProcessing(boolean enable) throws SQLException {
		actualStatement.setEscapeProcessing(enable);
	}
	    
    public int getQueryTimeout() throws SQLException {
		return actualStatement.getQueryTimeout();
	}
	    
    public void setQueryTimeout(int seconds) throws SQLException {
		actualStatement.setQueryTimeout(seconds);
	}
	    
    public void cancel() throws SQLException {
		actualStatement.cancel();
	}
	    
    public SQLWarning getWarnings() throws SQLException {
		return actualStatement.getWarnings();
	}
	    
    public void clearWarnings() throws SQLException {
		actualStatement.clearWarnings();
	}
	    
    public void setCursorName(String name) throws SQLException {
		actualStatement.setCursorName(name);
	}
	    
	/**
	 * Not implemented because we allow only one result set per
	 * statement (at a time).
	 */
    public boolean execute(String sql) throws SQLException {
		throw new UnsupportedOperationException("Not allowed by ResultSetClosingStatement");
	}
	    
    public ResultSet getResultSet() throws SQLException {
		return results;
	}
	    
    public int getUpdateCount() throws SQLException {
		return actualStatement.getUpdateCount();
	}
	    
	/**
	 * Not implemented because we allow only one result set per
	 * statement (at a time).
	 */
    public boolean getMoreResults() throws SQLException {
		throw new UnsupportedOperationException("Not allowed by ResultSetClosingStatement");
	}
	    
    public void setFetchDirection(int direction) throws SQLException {
		actualStatement.setFetchDirection(direction);
	}
	    
    public int getFetchDirection() throws SQLException {
		return actualStatement.getFetchDirection();
	}
	    
    public void setFetchSize(int rows) throws SQLException {
		actualStatement.setFetchSize(rows);
	}
	    
    public int getFetchSize() throws SQLException {
		return actualStatement.getFetchSize();
	}
	    
    public int getResultSetConcurrency() throws SQLException {
		return actualStatement.getResultSetConcurrency();
	}
	    
    public int getResultSetType()  throws SQLException {
		return actualStatement.getResultSetType();
	}
	    
    public void addBatch( String sql ) throws SQLException {
		actualStatement.addBatch(sql);
	}
	    
    public void clearBatch() throws SQLException {
		actualStatement.clearBatch();
	}
	    
   public  int[] executeBatch() throws SQLException {
		return actualStatement.executeBatch();
	}
	    
    public Connection getConnection() throws SQLException {
		return con;
	}

	/**
	 * Closes the result set.  If a SQLException results, it is logged
	 * at WARN level.
	 */
	public void finalize() {
		try {
			if(results != null) {
				results.close();
			}
		} catch (SQLException e) {
			logger.warn("Couldn't close result set in finalizer: "+e);
		}
	}
	/**
	 * @see java.sql.Statement#execute(String, int)
	 */
	public boolean execute(String sql, int autoGeneratedKeys)
		throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet...");
	}

	/**
	 * @see java.sql.Statement#execute(String, int[])
	 */
	public boolean execute(String sql, int[] columnIndexes)
		throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet...");
	}

	/**
	 * @see java.sql.Statement#execute(String, String[])
	 */
	public boolean execute(String sql, String[] columnNames)
		throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet...");
	}

	/**
	 * @see java.sql.Statement#executeUpdate(String, int)
	 */
	public int executeUpdate(String sql, int autoGeneratedKeys)
		throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet...");
	}

	/**
	 * @see java.sql.Statement#executeUpdate(String, int[])
	 */
	public int executeUpdate(String sql, int[] columnIndexes)
		throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet...");
	}

	/**
	 * @see java.sql.Statement#executeUpdate(String, String[])
	 */
	public int executeUpdate(String sql, String[] columnNames)
		throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet...");
	}

	/**
	 * @see java.sql.Statement#getGeneratedKeys()
	 */
	public ResultSet getGeneratedKeys() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet...");
	}

	/**
	 * @see java.sql.Statement#getMoreResults(int)
	 */
	public boolean getMoreResults(int current) throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet...");
	}

	/**
	 * @see java.sql.Statement#getResultSetHoldability()
	 */
	public int getResultSetHoldability() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet...");
	}

}	
