package ca.sqlpower.sql;

import org.apache.commons.dbcp.*;
import org.apache.commons.pool.*;
import org.apache.commons.pool.impl.*;

import ca.sqlpower.dashboard.*;

import java.util.*;
import java.sql.*;

/**
 * @author dfraser
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class Pool {

	private static Map pools = new HashMap();	
	private static Map objPools = new HashMap(); // for debugging and stats tracking
	private DBConnectionSpec dbcs;
	private String poolName;
	private PoolingDataSource dataSource;
	ObjectPool connectionPool;
	
	/** 
	 * Prepares a connection pool for the specified DBConnectionSpec.
	 */
	public Pool(LoginSession ls, DBConnectionSpec dbcs) throws Exception {
		this.dbcs = dbcs;
        poolName =  ls.getUsername()+"-"+ls.getPassword()+"-"+dbcs.getName();
        dataSource = (PoolingDataSource) pools.get(poolName);
        connectionPool = (ObjectPool) objPools.get(poolName); // for debugging
        
        if (dataSource == null) {
        	GenericObjectPool.Config poolConfig = new GenericObjectPool.Config();
        	System.out.println("Pool creating new pool for "+poolName);
  
        	poolConfig.maxActive = 25;
        	poolConfig.maxIdle = 2;
        	poolConfig.maxWait = 10000;
        	poolConfig.minEvictableIdleTimeMillis = 5000;
        	poolConfig.timeBetweenEvictionRunsMillis = 10000;
        	poolConfig.numTestsPerEvictionRun = 100;

		    connectionPool = new GenericObjectPool(null,poolConfig);
			System.out.println("dbUrl = "+dbcs.getUrl());

		    ConnectionFactory connectionFactory = new DriverManagerConnectionFactory(dbcs.getUrl(),ls.getUsername(),ls.getPassword());
        	PoolableConnectionFactory poolableConnectionFactory = new PoolableConnectionFactory(connectionFactory,connectionPool,null,null,false,true);
	        this.dataSource = new PoolingDataSource(connectionPool);
	        pools.put(poolName, dataSource);	
	        objPools.put(poolName, connectionPool);   // for debugging      
        } else {
        	System.out.println("Pool found cached pool for "+poolName);
        }
	}
		
	/**
	 * Returns a connection from a pool.
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
