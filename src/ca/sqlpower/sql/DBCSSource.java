package ca.sqlpower.sql;

import java.util.List;

/**
 * Defines a common way of getting a list of DBConnectionSpec objects
 * to offer the user of an application.  Implementations could use XML
 * files, properties files, RMI servers, JNDI servers, Oracle TNS name
 * interfaces, ODBC interfaces, or anything else to actually generate
 * the list.
 *
 * <p>The initial implementation is an XML file or RMI implementation
 * which is configured via a servlet which in turn gets configuration
 * data from the sysadmin through web.xml init prarmeters.
 *
 * @see ca.sqlpower.servlet.DBCSSourceServlet
 * @see ca.sqlpower.sql.XMLRMIDBCSSource
 *
 * @author Jonathan Fuerth
 * @version $Id$
 */
public interface DBCSSource {

	/**
	 * A call to this method should generate a list of
	 * DBConnectionSpec objects using various implementation-specific
	 * means of creating this list as necessary.
	 *
	 * @return a List of 0 or more DBConnectionSpec objects, never
	 * <code>null</code>.
	 * @throws DatabaseListReadException if the
	 * implementation-specific list creation process could not be
	 * completed.
	 */
	public List getDBCSList() throws DatabaseListReadException;
}
