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

    private boolean dropdownsInline;
    private boolean dropdownsAbove;

    public WebResultHTMLFormatter() {
	super();
	dropdownsInline=false;
	dropdownsAbove=true;
    }
    
    /**
     * Get the value of dropdownsInline.
     *
     * @return value of dropdownsInline.
     */
    public boolean isDropdownsInline() {return dropdownsInline;}
    
    /**
     * Set the value of dropdownsInline.  DropdowsInline and
     * DropdownsAbove are not mutually exclusive, so be sure to set
     * them to opposite values unless you want two sets of dropdown
     * boxes.
     *
     * @param v  Value to assign to dropdownsInline.
     */
    public void setDropdownsInline(boolean  v) {this.dropdownsInline = v;}

    /**
     * Get the value of dropdownsAbove.
     *
     * @return value of dropdownsAbove.
     */
    public boolean isDropdownsAbove() {return dropdownsAbove;}
    
    /**
     * Set the value of dropdownsAbove.  DropdowsInline and
     * DropdownsAbove are not mutually exclusive, so be sure to set
     * them to opposite values unless you want two sets of dropdown
     * boxes.
     *
     * @param v  Value to assign to dropdownsAbove.
     */
    public void setDropdownsAbove(boolean  v) {this.dropdownsAbove = v;}
    

    public void formatToStream(WebResultSet wrs, PrintWriter out) 
	throws SQLException {
	int numCols=wrs.getColumnCount();
	boolean fcRowid=wrs.getFirstColumnIsRowid();

	if(dropdownsAbove) {
	    List choices=null;
	    int i=1;
	    if(fcRowid) i++;
	    out.println("<table>");
	    out.println(" <tr>");
	    while(i<=numCols) {
		choices=wrs.getColumnChoicesList(i);
		if(choices != null) {
		    out.println("  <td align=\"center\">");
		    out.print(wrs.getColumnLabel(i));
		    out.println("<br />");
		    out.println(Web.makeSelectionList(wrs.getColumnChoicesName(i),
						      choices,
						      wrs.getColumnDefaultChoice(i),
						      wrs.getColumnHasAnyAll(i)));
		    out.println("  </td>");
		}
		i++;
	    }
	    out.println(" </tr>");
	    out.println("</table>");
	}

	out.println("<table class=\"resultTable\">");

	out.print("<tr class=\"resultTableHeading\">");
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

	if(dropdownsInline) {
	    out.println("<tr class=\"resultTableHeading\">");
	    for(int i=1; i<=numCols; i++) {
		List choices=wrs.getColumnChoicesList(i);
		
		out.println("<td>");
		if(choices != null) {
		    out.print(Web.makeSelectionList(wrs.getColumnChoicesName(i),
						    choices,
						    wrs.getColumnDefaultChoice(i),
						    wrs.getColumnHasAnyAll(i)));
		}
		out.println("</td>");
	    }
	    out.println("</tr>");
	}

	while(wrs.next()) {
	    out.println("<tr class=\"resultTableData\">");
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
