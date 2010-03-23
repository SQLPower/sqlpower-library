package ca.sqlpower.sql;
/**
 * DatabaseListReadException indicates that the list of databases that
 * the user should see could not be read.  The root cause will often
 * be a SAX parse error of the underlying XML or a file not found error.
 *
 * $Id$
 */
public class DatabaseListReadException extends Exception {
	protected Throwable rootCause=null;

	public DatabaseListReadException(Throwable t) {
		rootCause=t;
	}

	public Throwable getRootCause() {
		return rootCause;
	}

	public String getMessage() {
		if (rootCause != null) {
			System.err.println("DatabaseListReadException: nested exception is:");
			rootCause.printStackTrace();
		}
		return "DatabaseListReadException; root cause is: "+rootCause;
	}
}
