package ca.sqlpower.sql;

import org.apache.commons.dbcp.*;
import org.apache.commons.pool.*;
import org.apache.log4j.Logger;

import java.sql.*;

/**
 * Extends the orginal factory to return fancy new 
 * PoolableStatementClosingConnections instead of the boring 
 * old PoolableConnections.
 * 
 * @author dfraser
 * @version $Id$
 */
public class StatementClosingPoolableConnectionFactory extends PoolableConnectionFactory {

	private static final Logger logger
		= Logger.getLogger(StatementClosingPoolableConnectionFactory.class);

	/**
	 * Default superclass constructor.
	 *
	 * @throws Exception When there is an error in the PoolableConnectionFactory constructor.
	 */
	public StatementClosingPoolableConnectionFactory(ConnectionFactory factory,
													 ObjectPool pool,
													 KeyedObjectPoolFactory stmtPoolFactory,
													 String validationQuery,
													 boolean defaultReadOnly,
													 boolean defaultAutoCommit)
		throws Exception {

		super(factory, pool, stmtPoolFactory, validationQuery,
			  defaultReadOnly, defaultAutoCommit);
	}

   
	/**
	 * Creates a new connection using the connFactory, then sets its
	 * autoCommit and readOnly values to the settings on this
	 * factory.
	 */
	synchronized public Object makeObject() throws Exception {
        Connection con = _connFactory.createConnection();
        try {
            con.setAutoCommit(_defaultAutoCommit);
            con.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
        } catch (SQLException e) {
            logger.error("Couldn't set autoCommit to "+_defaultAutoCommit+"!", e);
			// continue anyway
        }

        try {
            con.setReadOnly(_defaultReadOnly);
        } catch (SQLException e) {
            logger.error("Couldn't set readOnly to "+_defaultReadOnly+"!", e);
			// continue anyway
        }

        if (_stmtPoolFactory != null) {
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
			
			// the schema version check used to be here.
			// it's moved to a static attribute of the Dashboard class.
			// it should be right near the top of the Dashboard.java file.
			// thank you for your interest!  have a nice day.
			//             -- Dan
			

		} finally {
			if(stmt != null) {
				stmt.close();
			}
		}

        return new PoolableStatementClosingConnection(con, _pool);
    }
}


