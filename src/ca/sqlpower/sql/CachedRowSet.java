package ca.sqlpower.sql;

import java.util.Hashtable;
import java.sql.ResultSet;
import java.sql.SQLException;
import com.sun.rowset.CachedRowSetImpl;

/**
 * This class is in a state of flux.  It will always implement at
 * least the ResultSet interface.  In the future, it may or may not
 * implement the RowSet interface.  It probably will not continue to
 * extend CachedRowSetImpl directly, but the populate(resultset)
 * method will remain.
 */
public class CachedRowSet extends CachedRowSetImpl implements ResultSet {
	
	/**
	 * Makes an empty cached rowset.
	 *
	 * IMPORTANT NOTE: The JDBC RowSet reference implementation
	 * jarfile MUST be in the SYSTEM classpath for this to work.  The
	 * easiest way is to put it in $JAVA_HOME/jre/lib/ext.  Being in
	 * the servlet container's lib directory or the application's
	 * WEB-INF/lib directory is not enough!
	 */
	public CachedRowSet() throws SQLException {
		super();
	}

// 	public void populate(ResultSet rs) {
		
// 	}
}
