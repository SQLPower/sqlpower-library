package ca.sqlpower.sql;

import java.rmi.Naming;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.io.*;

/**
 * Implementation of the RMI interface used to get the database connection 
 * list remotely.
 * 
 * @author Dan Fraser
 * @version $Id$
 */
public class DBConnectionSpecServerImpl
	extends UnicastRemoteObject
	implements DBConnectionSpecServer {

	protected static String xmlFileName = null;

	public DBConnectionSpecServerImpl() throws RemoteException {
		super();
	}

	/**
	 * @see ca.sqlpower.sql.DBConnectionSpecServer#getList()
	 */
	public Collection getAvailableDatabases() throws RemoteException {
		Collection databases = null;
		try {
			InputStream xmlStream = new FileInputStream(xmlFileName);
			databases=DBConnectionSpec.getDBSpecsFromInputStream(xmlStream);
		}
		catch (Exception e) {
			databases = null;
			e.printStackTrace();
			// could not get the database list, not much we can do.
		}
		return databases;
	}

	/**
	 * Basic server main.  This class is executable with a command line
	 * something like this:
	 * 
	 * java -Djava.rmi.server.codebase="base.of.classpath" \
	 *      -cp ".;../../../../common/lib/xerces.jar" \
	 *      -Djava.security.policy=rmi.policy \
	 *      -DdatabasesFile=../databases.xml \
	 *      ca.sqlpower.sql.DBConnectionSpecServerImpl
	 */
	public static void main(String args[]) {
	
		// XXX: THIS WILL NEED TO BE PROPERLY CONFIGURED IN THE FUTURE
		// Create and install a security manager
//		if (System.getSecurityManager() == null) {
//		    System.setSecurityManager(new RMISecurityManager());
//		}

		xmlFileName = System.getProperty("databasesFile");
	
		try {
		    DBConnectionSpecServerImpl obj = new DBConnectionSpecServerImpl();
	
		    Naming.rebind("///DBConnectionSpecServer", obj);
		    System.out.println("DBConnectionSpecServer bound in registry");
		} catch (Exception e) {
		    System.out.println("DBConnectionSpecImpl err: " + e.getMessage());
		    e.printStackTrace();
		}
    }

}

