package ca.sqlpower.sql;

import java.io.*;
import java.sql.SQLException;

/**
 * The base class for utilities that format a {@link WebResultSet}
 * into various human-readable formats.  Currently, HTML output is
 * available.  CSV is planned for the near future.
 *
 * @author Jonathan Fuerth
 * @version $Id$
 */
public abstract class WebResultFormatter {

    public WebResultFormatter() {
	rowidParameterName="rowid";
    }

    protected String rowidParameterName;

    public String getRowidParameterName() {
	return rowidParameterName;
    }

    public void setRowidParameterName(String newName) {
	rowidParameterName=newName;
    }

    protected String beautifyHeading(String heading) {
	StringBuffer newHeading=new StringBuffer(heading);

	for(int i=0; i<newHeading.length(); i++) {
	    if(newHeading.charAt(i) == '_') {
		newHeading.setCharAt(i, ' ');
	    }
	}
	return newHeading.toString();
    }

    public abstract void formatToStream(WebResultSet wrs, PrintWriter out) 
	throws SQLException;
}
