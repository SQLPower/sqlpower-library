package ca.sqlpower.sql;

import java.io.*;
import java.util.*;

/**
 * A plain XML-file-based source for the list of DBConnectionSpec
 * objects that should be presented to the user when they need to pick
 * a database to connect to.
 *
 * @author Jonathan Fuerth
 * @version $Id$
 */
public class XMLFileDBCSSource implements DBCSSource, Serializable {

	protected String xmlFileName=null;

	public XMLFileDBCSSource(String xmlFileName) {
		this.xmlFileName=xmlFileName;
	}

	/**
	 * Returns a list of DBConnectionSpec objects which were retrieved
	 * based on configuration information given to the constructor.
	 *
	 * @throws DatabaseListReadException if there is a problem reading
	 * the database list from the XML file.
	 */
	public List getDBCSList()
		throws IllegalStateException, DatabaseListReadException {

		List dbcsList=null;
		InputStream dbXMLFile=null;

		try {
			dbXMLFile = new BufferedInputStream(new FileInputStream(xmlFileName));
			dbcsList=new ArrayList(DBCSSourceSupport.getListUsingXMLStream(dbXMLFile));
		} catch(IOException e) {
			throw new DatabaseListReadException(e);
		} finally {
			try {
				dbXMLFile.close();
			} catch(IOException e) {
				throw new DatabaseListReadException(e);
			}
		}
		return dbcsList;
	}
}
