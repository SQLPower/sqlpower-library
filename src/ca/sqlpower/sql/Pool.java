package ca.sqlpower.sql;

import org.apache.commons.dbcp.*;
import org.apache.commons.pool.*;
import org.apache.commons.pool.impl.*;

import ca.sqlpower.dashboard.*;

import java.util.*;
import java.sql.*;

/**
 * Connection pool wrapper class, providing a simple connection pool interface
 * to SQL Power applications without exposing the underlying pool's interface.
 * 
 * In this case, it is implemented using Apache Commons DBCP, plus some
 * customized classes such as the ca.sqlpower.sql.PoolableStatementClosingConnection
 * and the ca.sqlpower.sql.StatementClosingPoolableConnectionFactory.
 * 
 * @author Dan Fraser
 * @version $Id$
 */
public class Pool {

	private static Map pools = new HashMap();	
	private static Map objPools = new HashMap(); // for debugging and stats tracking
	private DBConnectionSpec dbcs;
	private String poolName;
	private PoolingDataSource dataSource;
	ObjectPool connectionPool;
	
	/** 
	 * Prepares a connection pool for the specified DBConnectionSpec. Adds
	 * the pool to a cache of pools for re-use whenever the DBConnectionSpec,
	 * username, and password are re-used. This essentially creates one pool
	 * per unique database/user combination.
	 */
	public Pool(LoginSession ls, DBConnectionSpec dbcs) throws Exception {
		this.dbcs = dbcs;
        poolName =  ls.getUsername()+"-"+ls.getPassword()+"-"+dbcs.getName();
        dataSource = (PoolingDataSource) pools.get(poolName);
        connectionPool = (ObjectPool) objPools.get(poolName); // for debugging
        
        if (dataSource == null) {
        	GenericObjectPool.Config poolConfig = new GenericObjectPool.Config();
        	System.out.println("Pool creating new pool for "+poolName);
  
			// XXX: this should come from a properties file.
        	poolConfig.maxActive = 25;
        	poolConfig.maxIdle = 2;
        	poolConfig.maxWait = 10000;
        	poolConfig.minEvictableIdleTimeMillis = 1000*60*5;
        	poolConfig.timeBetweenEvictionRunsMillis = 10000;
        	poolConfig.numTestsPerEvictionRun = 100;

		    connectionPool = new GenericObjectPool(null,poolConfig);
			System.out.println("dbUrl = "+dbcs.getUrl());

		    ConnectionFactory connectionFactory = new DriverManagerConnectionFactory(dbcs.getUrl(),ls.getUsername(),ls.getPassword());
        	PoolableConnectionFactory poolableConnectionFactory = new StatementClosingPoolableConnectionFactory(connectionFactory,connectionPool,null,null,false,true);
	        this.dataSource = new PoolingDataSource(connectionPool);
	        pools.put(poolName, dataSource);	
	        objPools.put(poolName, connectionPool);   // for debugging      
        } else {
        	System.out.println("Pool found cached pool for "+poolName);
        }
	}
		
	/**
	 * Returns a Connection from the pool.  Be sure to close it when you're done 
	 * with it!
	 */
	public Connection getConnection() throws SQLException {
		Connection con;
		System.out.println("getting connection from the pool:");
		System.out.println("before: "+connectionPool.numActive()+" active, "+connectionPool.numIdle()+" idle.");
		con = dataSource.getConnection();		
		System.out.println("after: "+connectionPool.numActive()+" active, "+connectionPool.numIdle()+" idle.");
		return con;
	}		
}
