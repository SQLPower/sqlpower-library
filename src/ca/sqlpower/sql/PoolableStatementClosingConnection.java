package ca.sqlpower.sql;

import java.sql.*;
import java.util.*;

import org.apache.commons.dbcp.PoolableConnection;
import org.apache.commons.pool.ObjectPool;

/**
 * Extension to the Apache Commons DBCP PoolableConnection that keeps
 * a list of all statements that it has given out and closes them all 
 * when its close method is called. When it closes statements, it ignores 
 * SQLExceptions, since the
 * statements could have (and should have) already been closed by
 * the outside world.
 * 
 * @author Dan Fraser
 * @version $Id$
 */
public class PoolableStatementClosingConnection extends PoolableConnection {

	List openStatements = new LinkedList();

	/**
	 * Just call the superclass constructor
	 * @param arg0
	 * @param arg1
	 */
	public PoolableStatementClosingConnection(
		Connection arg0,
		ObjectPool arg1) {
		super(arg0, arg1);
	}


	/**
	 * Creates a new java.sql.Statement, adds it to the list of open statements, 
	 * and returns it.
	 */
	public Statement createStatement() throws SQLException {
		Statement stmt = super.createStatement();
		openStatements.add(stmt);
		return super.createStatement();
	}
	
	/**
	 * Iterates over the list of open statements and calls close() on each one, 
	 * ignoring any resulting SQLExceptions.
	 */
	public void close() throws SQLException {
		System.out.println("PSCC: closing statements");
		Iterator stmtIt = openStatements.iterator();
		int attemptedToClose = 0;
		int closed = 0;
		while (stmtIt.hasNext()) {
			Statement stmt = (Statement) stmtIt.next();
			if (stmt != null) {
				try {
					attemptedToClose++;
					closed++;
					stmt.close();
				} catch (SQLException e) {
					closed--;
					// statement probably already closed
				}
			}
		}
		System.out.println("PSCC: attempted "+attemptedToClose+", closed "+closed+", closing super");
		super.close();
		System.out.println("PSCC: done");
	}
}

