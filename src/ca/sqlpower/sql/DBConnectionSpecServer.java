package ca.sqlpower.sql;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.*;


/**
 * Remote interface used to obtain the list of database connections
 * from a central server (for clustering this web application).
 *
 * @author Dan Fraser
 * @version $Id$
 */
public interface DBConnectionSpecServer extends Remote {

	/**
	 * Returns a collection of DBConnectionSpec objects.
	 */
	public Collection getAvailableDatabases() throws RemoteException;

	/** 
	 * Returns true if the password in the argument matches the administrative
	 * password on the RMI server.  Returns false otherwise.
	 */
	public boolean checkPassword(String password) throws RemoteException;
	
	/**
	 * This sets the list of available databases on the RMI server.
	 * 
	 * @param dbList a Collection of DBConnectionSpec objects.
	 * @param oldPass the current administrative password (required)
	 * @param newPass if non-null and non-empty, the password will
	 * be changed to this password.
	 */
	public void setAvailableDatabases(Collection dbList, String oldPass, String newPass) 
		throws RemoteException;

}

