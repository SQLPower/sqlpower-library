package ca.sqlpower.sql;

import java.sql.*;
import java.io.*;

public class WebResultHTMLFormatter extends WebResultFormatter {

    private String rowidParameterName;

    public WebResultHTMLFormatter() {
	rowidParameterName="rowid";
    }

    public String getRowidParameterName() {
	return rowidParameterName;
    }

    public void setRowidParameterName(String newName) {
	rowidParameterName=newName;
    }
    
    public void formatToStream(WebResultSet wrs, PrintWriter out) 
	throws SQLException {
	int numCols=wrs.getColumnCount();
	boolean fcRowid=wrs.getFirstColumnIsRowid();

	out.println("<table>");

	out.println("<tr>");
	for(int i=1; i<numCols; i++) {
	    out.println("<th>");
	    if(fcRowid && i==1) {
		out.println("&nbsp;");
	    } else {
		out.println(wrs.getColumnLabel(i));
	    }
	    out.println("</th>");
	}
	out.println("</tr>");

	while(wrs.next()) {
	    out.println("<tr>");
	    for(int i=1; i<numCols; i++) {
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
