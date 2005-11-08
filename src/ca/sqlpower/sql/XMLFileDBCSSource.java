package ca.sqlpower.sql;

import java.io.*;
import java.util.*;

/**
 * A plain XML-file-based source for the list of DBConnectionSpec
 * objects that should be presented to the user when they need to pick
 * a database to connect to.
 * <p>
 * Each instance of this class keeps a cached copy of the connection info
 * list, and will automatically reload the list from the file if its 
 * modification time changes. 
 *
 * @author Jonathan Fuerth
 * @version $Id$
 */
public class XMLFileDBCSSource implements DBCSSource, Serializable {

	private String xmlFileName;
    
    /**
     * A cached copy of the last DBCS List we returned.  Profiling shows that
     * parsing the XML file every time we get to the login screen is quite
     * wasteful of memory.
     */
    private List cachedDbcsList;

    /**
     * The time (from System.currentTimeMillis()) that the cachedDbcsList was
     * last read from the file.  We compare this with the file's modification
     * time to see if it needs to be reloaded.
     */
    private long cachedDbcsListRefreshTime;
    
	public XMLFileDBCSSource(String xmlFileName) {
		this.xmlFileName = xmlFileName;
	}

	/**
	 * Returns a list of DBConnectionSpec objects which were retrieved
	 * based on configuration information given to the constructor.
	 *
	 * @throws DatabaseListReadException if there is a problem reading
	 * the database list from the XML file.
	 */
	public synchronized List getDBCSList() throws IllegalStateException, DatabaseListReadException {
	    File dbcsFile = new File(xmlFileName);
	    if (cachedDbcsList == null || dbcsFile.lastModified() >= cachedDbcsListRefreshTime) {
	        InputStream dbXMLFile = null;
	        
	        try {
	            dbXMLFile = new BufferedInputStream(new FileInputStream(dbcsFile));
	            cachedDbcsList = new ArrayList(DBCSSourceSupport.getListUsingXMLStream(dbXMLFile));
	            cachedDbcsListRefreshTime = System.currentTimeMillis();
	        } catch (IOException e) {
	            throw new DatabaseListReadException(e);
	        } finally {
	            try {
	                dbXMLFile.close();
	            } catch(IOException e) {
	                throw new DatabaseListReadException(e);
	            }
	        }
	    }
	    return cachedDbcsList;
	}
}
