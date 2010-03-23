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

package ca.sqlpower.swingui.query;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * This interface is used to allow the SQLQueryUIComponents to have a
 * unique query execution specified when it is going to execute a
 * query. This way tools using the SQLQueryUIComponents can have special
 * events occur every execution. The way the results are returned
 * is modeled after the java.sql.Statement and most of this is a subset
 * of that class.
 */
public interface StatementExecutor {

	/**
	 * This will execute the statement within this statement executor.
	 * 
	 * @return True if the first result is a ResultSet object; false if it is an
	 *         update count or there are no results. If true use the
	 *         getResultSets to get a list of the returned result sets. If false
	 *         use getUpdateCount to get the number of rows affected.
	 */
	boolean executeStatement() throws SQLException;

	/**
	 * Retrieves the current result as a ResultSet object. This method should be
	 * called only once per result.
	 */
	ResultSet getResultSet() throws SQLException;

	/**
	 * Retrieves the current result as an update count; if the result is a
	 * ResultSet object or there are no more results, -1 is returned. This
	 * method should be called only once per result.
	 */
	int getUpdateCount();
	
	/**
	 * Moves to this Statement object's next result, returns true if it is a
	 * ResultSet object, and implicitly closes any current ResultSet object(s)
	 * obtained with the method getResultSet.
	 * 
	 * There are no more results when the following is true:
	 * 
	 * // stmt is a Statement object ((stmt.getMoreResults() == false) &&
	 * (stmt.getUpdateCount() == -1))
	 */
	boolean getMoreResults() throws SQLException;
	
	/**
	 * This will return the SQL string that is to be executed by this
	 * executor.
	 */
	String getStatement();
	
	/**
	 * Tells if this executor is currently running a query.
	 * @return True or false, depending if it's running.
	 */
	boolean isRunning();
	
	void addStatementExecutorListener(StatementExecutorListener sel);
	
	void removeStatementExecutorListener(StatementExecutorListener sel);
}
