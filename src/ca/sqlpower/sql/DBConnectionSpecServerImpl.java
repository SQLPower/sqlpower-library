package ca.sqlpower.sql;

import java.rmi.Naming;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.io.*;

import javax.xml.parsers.*;

import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.w3c.dom.*;

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

	Object fileLock = null;

	protected String xmlFileName = null;

	public DBConnectionSpecServerImpl() throws RemoteException {
		super();
	}

	/**
	 * @see ca.sqlpower.sql.DBConnectionSpecServer
	 */
	public Collection getAvailableDatabases() throws RemoteException {
		Collection databases = null;
		try {
			InputStream xmlStream = new FileInputStream(xmlFileName);
			databases = DBConnectionSpec.getDBSpecsFromInputStream(xmlStream);
		} catch (Exception e) {
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
	public static void main(String args[]) throws RemoteException{

		// Create and install a security manager
		if (System.getSecurityManager() == null) {
			System.setSecurityManager(new RMISecurityManager());
		}

		DBConnectionSpecServerImpl obj =
				new DBConnectionSpecServerImpl();

		obj.xmlFileName = System.getProperty("databasesFile");
		if (obj.xmlFileName == null) {
			System.out.println(
				"no databases file specified.  Please add -DdatabaseFile=filename to the command line.");
		} else {
			try {

				Naming.rebind("///DBConnectionSpecServer", obj);
				System.out.println("DBConnectionSpecServer bound in registry");
			} catch (Exception e) {
				System.out.println(
					"DBConnectionSpecImpl err: " + e.getMessage());
				e.printStackTrace();
			}
		}
	}


	/**
	 * @see ca.sqlpower.sql.DBConnectionSpecServer
	 */
	public void setAvailableDatabases(Collection dbList, String oldPass, String newPass)
		throws RemoteException {
		try {
			writeDBSpecsToOutputStream(dbList, oldPass, newPass);
		} catch (Exception e) {
			// not much we can do on server side
			throw new RemoteException("could not set databases",e);
		}
	}

	/**
	 * @see ca.sqlpower.sql.DBConnectionSpecServer
	 */
	public boolean checkPassword(String argPassword) throws RemoteException {
		try {
			InputStream xmlStream = new FileInputStream(xmlFileName);
			DocumentBuilder db =
				DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Document d = db.parse(xmlStream);
			NodeList list = d.getElementsByTagName("databases");
			Element e = (Element) list.item(0);
			String password = e.getAttribute("password");
			return argPassword.equals(password);
		} catch (Exception e) {
			// not much we can do on the remote side.
			throw new RemoteException("could not check password",e);
		}
	}

	/**
	 * This uses Apache Xalan's xml-to-stream converter to build an xml
	 * document from the list of DBConnectionSpecs in the standard
	 * SQLPower format, and saves it to a file on the disk.
	 * 
	 * Note, this uses xml functionality beyond what JAXP offers, so Xalan
	 * is required, not just any JAXP parser.
	 */
	protected synchronized void writeDBSpecsToOutputStream(
		Collection dbspecs, String oldPassword, String newPassword)
		throws ParserConfigurationException, IOException {

		if (!checkPassword(oldPassword)) {
			return;
		}
		
		OutputStream xmlStream = new FileOutputStream(xmlFileName);

		DocumentBuilder db =
			DocumentBuilderFactory.newInstance().newDocumentBuilder();
		Document d = db.newDocument();
		Element databases = d.createElement("databases");
		if (newPassword != null) {
			databases.setAttribute("password",newPassword);
		} else {
			databases.setAttribute("password",oldPassword);
		}
		
		Iterator dbIter = dbspecs.iterator();
		while (dbIter.hasNext()) {
			DBConnectionSpec dbcs = (DBConnectionSpec) dbIter.next();
			Element dbNode = d.createElement("database");
			dbNode.setAttribute("name",dbcs.getName());
			if (dbcs.getDisplayName() != null) {
				Element displayName = d.createElement("display-name");
				displayName.appendChild(d.createTextNode(dbcs.getDisplayName()));
				dbNode.appendChild(displayName);
			}
			if (dbcs.getDriverClass() != null) {
				Element driverClass = d.createElement("driver-class");
				driverClass.appendChild(d.createTextNode(dbcs.getDriverClass()));
				dbNode.appendChild(driverClass);
			}
			if (dbcs.getUrl() != null) {
				Element url = d.createElement("url");
				url.appendChild(d.createTextNode(dbcs.getUrl()));
				dbNode.appendChild(url);
			}
			if (dbcs.getUser() != null) {
				Element user = d.createElement("user");
				user.appendChild(d.createTextNode(dbcs.getUser()));
				dbNode.appendChild(user);
			}
			if (dbcs.getPass() != null) {
				Element pass = d.createElement("pass");
				pass.appendChild(d.createTextNode(dbcs.getPass()));
				dbNode.appendChild(pass);
			}
			databases.appendChild(dbNode);
		}
		d.appendChild(databases);
		OutputFormat of = new OutputFormat();
		of.setIndenting(true);
		XMLSerializer serializer = new XMLSerializer(xmlStream, of);
		serializer.serialize(d);
		xmlStream.close();
	}
}
