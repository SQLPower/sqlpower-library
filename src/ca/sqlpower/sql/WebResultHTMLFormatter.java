package ca.sqlpower.sql;

import ca.sqlpower.util.*;
import java.util.*;
import java.sql.*;
import java.io.*;

/**
 * WebResultHTMLFormatter exists in order to format WebResultSets into
 * an HTML table.  A growing list of options are supported, controlled
 * by calling the various get and set methods before a call to
 * formatToStream.  You may call formatToStream on the same instance
 * as many times as you like; the output settings will remain in
 * effect until you change them.
 *
 * @author Jonathan Fuerth
 * @version $Id$
 */
public class WebResultHTMLFormatter extends WebResultFormatter {

    public WebResultHTMLFormatter() {
	rowidParameterName="rowid";
    }

    public void formatToStream(WebResultSet wrs, PrintWriter out) 
	throws SQLException {
	int numCols=wrs.getColumnCount();
	boolean fcRowid=wrs.getFirstColumnIsRowid();

	out.println("<table>");

	out.println("<tr>");
	for(int i=1; i<=numCols; i++) {
	    out.println("<th valign=\"bottom\">");
	    if(fcRowid && i==1) {
		out.println("&nbsp;");
	    } else {
		out.println(beautifyHeading(wrs.getColumnLabel(i)));
	    }
	    out.println("</th>");
	}
	out.println("</tr>");

	out.println("<tr>");
	for(int i=1; i<=numCols; i++) {
	    List choices=wrs.getColumnChoicesList(i);

	    out.println("<td>");
	    if(choices != null) {
		out.print(Web.makeSelectionList(wrs.getColumnChoicesName(i),
						wrs.getColumnChoicesList(i),
						wrs.getColumnDefaultChoice(i),
						wrs.getColumnHasAnyAll(i)));
	    }
	    out.println("</td>");
	}
	out.println("</tr>");

	while(wrs.next()) {
	    out.println("<tr>");
	    for(int i=1; i<=numCols; i++) {
		out.println("<td>");
		if(fcRowid && i==1) {
		    out.print("<input type=\"radio\" name=\"");
		    out.print(rowidParameterName);
		    out.print("\" value=\"");
		    out.print(wrs.getString(i));
		    out.print("\" onClick=\"this.form.submit()\" />");
		} else {
		    out.println(wrs.getString(i));
		}
		out.println("</td>");
	    }
	    out.println("</tr>");
	}
	out.println("</table>");
	out.flush();
    }
}
