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
        Connection con = _connFactory.createConnection();
        try {
            con.setAutoCommit(_defaultAutoCommit);
        } catch(SQLException e) {
            ; // ignored for now
        }

        try {
            con.setReadOnly(_defaultReadOnly);
        } catch(SQLException e) {
            ; // ignored for now
        }

        if(null != _stmtPoolFactory) {
            KeyedObjectPool stmtpool = _stmtPoolFactory.createPool();
            con = new PoolingConnection(con,stmtpool);
            stmtpool.setFactory((PoolingConnection)con);
        }
		
		// check for requisite version of PL schema
		Statement stmt = null;
		try {
			stmt = con.createStatement();
			ResultSet rs = stmt.executeQuery("select schema_version from def_param");
			if(!rs.next()) {
				throw new SQLException("def_param table has no rows");
			}

			String schemaVersion = rs.getString(1);
			if(schemaVersion.compareTo("4.1.2") < 0) {
				throw new PLSchemaException("You have Power*Loader Schema version "+rs.getString(1)+" but you need a version of 4.1.2");
			}
		} finally {
			if(stmt != null) {
				stmt.close();
			}
		}

		System.out.println("returning new PoolableStatementClosingConnection");
        return new PoolableStatementClosingConnection(con,_pool);
    } // end makeObject
} // end class


