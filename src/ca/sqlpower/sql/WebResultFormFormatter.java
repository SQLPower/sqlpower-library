package ca.sqlpower.sql;

import ca.sqlpower.util.*;
import java.util.*;
import java.sql.*;
import java.io.*;

/**
 * WebResultFormFormatter exists in order to format WebResultSets into
 * a set of HTML form elements.  You may call formatToStream on the
 * same instance as many times as you like; the output settings will
 * remain in effect until you change them.
 *
 * @author Jonathan Fuerth
 * @version $Id$
 */
public class WebResultFormFormatter extends WebResultFormatter {

    private int numHTMLCols;
    
    public WebResultFormFormatter() {
	super();
	numHTMLCols=3;
    }

    public void setNumHTMLCols(int numCols) {
	numHTMLCols=numCols;
    }

    public int getNumHTMLCols() {
	return numHTMLCols;
    }

    public void formatToStream(WebResultSet wrs, PrintWriter out) 
	throws SQLException {
	int numCols=wrs.getColumnCount();
	int cell=0, col=0;;
	boolean fcRowid=wrs.getFirstColumnIsRowid();

	if(!wrs.next()) {
	    return;
	}

	out.println("<table>");
	out.println(" <tr>");
	do {
	    if(fcRowid && cell==0) {
		cell++;
	    }
	    out.println("  <td align=\"right\">");
	    out.println(beautifyHeading(wrs.getColumnLabel(cell+1)));
	    out.println("  </td>");
	    out.println("  <td>");
	    out.print("   <input type=\"text\" length=\"30\" name=\"");
	    out.print(wrs.getColumnLabel(cell+1));
	    out.print("\" value=\"");
	    out.print(wrs.getString(cell+1));
	    out.println("\" />");
	    out.println("  </td>");
	    if(col==numHTMLCols-1) {
		col=0;
		out.println(" </tr>");
		out.println(" <tr>");
	    }
	    col++;
	    cell++;
	} while(cell<numCols);
	out.println(" </tr>");
	out.println("</table>");
	out.flush();
    }
}
