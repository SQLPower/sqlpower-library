package ca.sqlpower.sql;

import org.apache.commons.dbcp.*;
import org.apache.commons.pool.*;

import java.sql.*;

/**
 * Extends the orginal factory to return fancy new 
 * PoolableStatementClosingConnections instead of the boring 
 * old PoolableConnections.
 * 
 * @author dfraser
 * @version $Id$
 */
public class StatementClosingPoolableConnectionFactory
	extends PoolableConnectionFactory {
	
	/**
	 * Default superclass constructor.
	 * @param arg0
	 * @param arg1
	 * @param arg2
	 * @param arg3
	 * @param arg4
	 * @param arg5
	 * @throws Exception
	 */
	public StatementClosingPoolableConnectionFactory(
		ConnectionFactory arg0,
		ObjectPool arg1,
		KeyedObjectPoolFactory arg2,
		String arg3,
		boolean arg4,
		boolean arg5)
		throws Exception {
		super(arg0, arg1, arg2, arg3, arg4, arg5);
	}

   
	/**
	 * Get a fancy new connection.
	 */
	synchronized public Object makeObject() throws Exception {
        Connection conn = _connFactory.createConnection();
        try {
            conn.setAutoCommit(_defaultAutoCommit);
        } catch(SQLException e) {
            ; // ignored for now
        }
        try {
            conn.setReadOnly(_defaultReadOnly);
        } catch(SQLException e) {
            ; // ignored for now
        }
        if(null != _stmtPoolFactory) {
            KeyedObjectPool stmtpool = _stmtPoolFactory.createPool();
            conn = new PoolingConnection(conn,stmtpool);
            stmtpool.setFactory((PoolingConnection)conn);
        }
		System.out.println("returning new PoolableStatementClosingConnection");
        return new PoolableStatementClosingConnection(conn,_pool);
    }
}
