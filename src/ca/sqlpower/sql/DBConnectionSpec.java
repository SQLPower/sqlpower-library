package ca.sqlpower.dashboard;

import javax.xml.parsers.*;
import org.w3c.dom.*;
import java.io.*;
import java.util.*;

/**
 * The DBConnectionSpec class is a simple bean whose instances
 * represent a database that the application user is allowed to
 * connect to.  With an instnace of this bean, plus a database
 * username/password pair, you have all the information you need to
 * attempt to make a JDBC Connection to a target database.
 *
 * @version $Id$
 */
public class DBConnectionSpec {
	String name;
	String displayName;
	String driverClass;
	String url;

	public String toString() {
		return "DBConnectionSpec: "+name+", "+displayName+", "+driverClass+", "+url;
	}

	/**
	 * Gets the value of name
	 *
	 * @return the value of name
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Sets the value of name
	 *
	 * @param argName Value to assign to this.name
	 */
	public void setName(String argName){
		this.name = argName;
	}

	/**
	 * Gets the value of displayName
	 *
	 * @return the value of displayName
	 */
	public String getDisplayName() {
		return this.displayName;
	}

	/**
	 * Sets the value of displayName
	 *
	 * @param argDisplayName Value to assign to this.displayName
	 */
	public void setDisplayName(String argDisplayName){
		this.displayName = argDisplayName;
	}

	/**
	 * Gets the value of url
	 *
	 * @return the value of url
	 */
	public String getUrl() {
		return this.url;
	}

	/**
	 * Sets the value of url
	 *
	 * @param argUrl Value to assign to this.url
	 */
	public void setUrl(String argUrl){
		this.url = argUrl;
	}

	/**
	 * Gets the value of driverClass
	 *
	 * @return the value of driverClass
	 */
	public String getDriverClass() {
		return this.driverClass;
	}

	/**
	 * Sets the value of driverClass
	 *
	 * @param argDriverClass Value to assign to this.driverClass
	 */
	public void setDriverClass(String argDriverClass){
		this.driverClass = argDriverClass;
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
 			throw new IllegalArgumentException(
                  "This method only supports nodes of type 'database'.");
 		}
		DBConnectionSpec spec=new DBConnectionSpec();
		spec.setName(dbElem.getAttributes().getNamedItem("name").getNodeValue());
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
			}
		}
		return spec;
	}
}
