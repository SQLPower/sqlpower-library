package ca.sqlpower.sql;

import javax.xml.parsers.*;
import org.w3c.dom.*;
import java.io.*;
import java.util.*;
import org.apache.xml.serialize.*;
import java.rmi.Naming;

/**
 * A collection of support methods for implementing a
 * <code>DBCSSource</code>.  Code in here was harvested from various
 * places in the web-development classes where it shouldn't have been.
 *
 * <p>As the class name implies, you shouldn't use this class directly
 * unless you are implementing a <code>DBCSSource</code>.  If you're
 * tempted to use this class directly, you probably need to do one of
 * two things:
 *
 * <ol>
 *  <li>Use an existing <code>DBCSSource</code> implementation
 *  <li>Write a new <code>DBCSSource</code> implementation and use that.
 * </ol>
 *
 * @author Jonathan Fuerth, Dan Fraser, Gillian Mereweather
 * @version $Id$
 */
public class DBCSSourceSupport {

	/**
	 * Returns a Collection of DBConnectionSpec objects which
	 * represent all database entries known to the named RMI host.
	 *
	 * @return a Collection of databases the user can try to connect
	 * to.  All elements will be of type
	 * ca.sqlpower.sql.DBConnectionSpec.  The Collection will be empty
	 * if no databases could be found.
	 * @throws DatabaseListReadException if anything goes wrong on the
	 * remote end or with the RMI communication itself.
	 */
	public static Collection getListUsingRMI(String host)
		throws DatabaseListReadException {
		Collection databases = null;
		DBConnectionSpecServer obj = null;

		try {
			obj = (DBConnectionSpecServer) Naming.lookup
				("//"+host+"/DBConnectionSpecServer");
			databases = obj.getAvailableDatabases();
		} catch (Exception e) {
			// something bad happened getting the list of databases.
			throw new DatabaseListReadException(e);
		}
		return databases;
	}

	/**
	 * Returns a Collection of DBConnectionSpec objects which
	 * represent all database entries in the XML document available
	 * through the given <code>InputStream</code>.
	 *
	 * @return a Collection of databases the user can try to connect
	 * to.  All elements will be of type
	 * ca.sqlpower.sql.DBConnectionSpec.  The Collection will be empty
	 * if no databases could be found.
	 * @throws DatabaseListReadException if the XML document is
	 * invalid or contains unexpected elements.
	 */
	public static Collection getListUsingXMLStream(InputStream xmlStream)
		throws DatabaseListReadException {
  	    return getDBSpecsFromInputStream(xmlStream);
	}
	
	/**
	 * Uses a list of available databases (set up by a sysadmin) to
	 * generate a Collection of DBConnectionSpec objects.
	 *
	 * @param xmlStream An input stream which contains a valid XML
	 * document describing the list of available databases.  Typically
	 * a FileInputStream from /WEB-INF/databases.xml.
	 * @return a Collection of DBConnectionSpec objects describing all
	 * available databases.
	 * @throws DatabaseListReadException when the underlying list
	 * could not be loaded and parsed.
	 */
	public static Collection getDBSpecsFromInputStream(InputStream xmlStream) 
		throws DatabaseListReadException {
		try {
			DocumentBuilder db=DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Document d=db.parse(xmlStream);
			NodeList databaseList=d.getElementsByTagName("database");
			LinkedList dbcsList=new LinkedList();
			for(int i=0; i<databaseList.getLength(); i++) {
				Element databaseNode=(Element)databaseList.item(i);
				String databaseName=databaseNode.getAttribute("name");
				dbcsList.add(makeSpecFromDBNode(databaseNode));
			}
			return(dbcsList);
		} catch(Exception e){
			throw new DatabaseListReadException(e);
		}
	}


	/**
	 * Reads the values from the children of the given DOM element,
	 * populating a DBConnectionSpec bean with the corresponding
	 * values.  The child elements it looks for are:
	 * <ul>
	 *  <li>database element attribute <code>name</code>
	 *  <li><code>display-name</code>
	 *  <li><code>driver-class</code>
	 *  <li><code>url</code>
	 * </ul>
	 *
	 * @return The populated DBConnectionSpec bean.
	 * @throws IllegalArgumentException if the given element isn't a
	 * <code>database</code> element.
	 */
	protected static DBConnectionSpec makeSpecFromDBNode(Element dbElem) {
 		if(!dbElem.getNodeName().equals("database")) {
 			throw new IllegalArgumentException("This method only supports nodes of type 'database'.");
 		}
		DBConnectionSpec spec=new DBConnectionSpec();
		spec.setName(dbElem.getAttributes().getNamedItem("name").getNodeValue());
		spec.setSeqNo(Integer.parseInt(dbElem.getAttributes().getNamedItem("seqNo").getNodeValue()));
		Node singleLoginAttr = dbElem.getAttributes().getNamedItem("singleLogin");
		if (singleLoginAttr != null) {
			spec.setSingleLogin(Boolean.valueOf(singleLoginAttr.getNodeValue()).booleanValue());
		} else {
			spec.setSingleLogin(false);
		}
		NodeList databaseProperties=dbElem.getChildNodes();
		for(int j=0; j<databaseProperties.getLength(); j++) {
			Node databaseProperty=databaseProperties.item(j);
			if(databaseProperty.getNodeType() != Node.ELEMENT_NODE) continue;
			databaseProperty.normalize();
			if(databaseProperty.getNodeName().equals("display-name")) {
				spec.setDisplayName(databaseProperty.getFirstChild().getNodeValue());
			} else if(databaseProperty.getNodeName().equals("driver-class")) {
				spec.setDriverClass(databaseProperty.getFirstChild().getNodeValue());
			} else if(databaseProperty.getNodeName().equals("url")) {
				spec.setUrl(databaseProperty.getFirstChild().getNodeValue());
			} else if(databaseProperty.getNodeName().equals("user")) {
				spec.setUser(databaseProperty.getFirstChild().getNodeValue());
			} else if(databaseProperty.getNodeName().equals("pass")) {
				spec.setPass(databaseProperty.getFirstChild().getNodeValue());
			}
		}
		return spec;
	}
}
