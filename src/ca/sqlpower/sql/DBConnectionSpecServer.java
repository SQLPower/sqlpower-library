package ca.sqlpower.sql;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.*;


/**
 * Remote interface used to obtain the list of database connections
 * from a central server (for clustering this web application).
 * 
 * Will eventually be extended to allow modification of the remote 
 * list.
 */
public interface DBConnectionSpecServer extends Remote {

	/**
	 * Returns a collection of DBConnectionSpec objects.
	 */
	public Collection getAvailableDatabases() throws RemoteException;

}

