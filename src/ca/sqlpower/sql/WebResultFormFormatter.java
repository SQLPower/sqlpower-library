package ca.sqlpower.sql;

import ca.sqlpower.util.*;
import java.util.*;
import java.sql.*;
import java.io.*;
import java.text.*;

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
	moneyFormatter=new ca.sqlpower.util.NaanSafeNumberFormat("#0.00");
	numberFormatter=new ca.sqlpower.util.NaanSafeNumberFormat("#.#");
    }

    public void setNumHTMLCols(int numCols) {
	numHTMLCols=numCols;
    }

    public int getNumHTMLCols() {
	return numHTMLCols;
    }

    public void formatToStream(WebResultSet wrs, PrintWriter out)
	throws SQLException, NoRowidException {
	int numCols=wrs.getColumnCount();
	int cell=0, col=0;

	StringBuffer contents=new StringBuffer(60);
	StringBuffer align=new StringBuffer(10);
	StringBuffer label=new StringBuffer(20);

	if(!wrs.next()) {
	    return;
	}

	out.println("<table>");
	out.println(" <tr>");
	do {
	    contents.setLength(0);
	    align.setLength(0);
	    label.setLength(0);

	    try {
		getColumnFormatted(wrs, cell+1, contents, align);
		label.append(wrs.getColumnLabel(cell+1));
	    } catch(ColumnNotDisplayableException e) {
		cell++;
		continue;
	    }

	    out.println("  <td align=\"right\" class=\"searchForm\">");
	    out.println(beautifyHeading(label.toString()));
	    out.println("  </td>");
	    out.println("  <td>");
	    switch(wrs.getColumnType(cell+1)) {
	    case FieldTypes.PASSWORD:
		out.print("   <input type=\"password\" length=\"30\" name=\"");
		out.print(label);
		out.print("\" />");
		break;

	    case FieldTypes.BOOLEAN:
		out.print("   <input type=\"checkbox\" name=\"");
		out.print(label);
		out.print("\" ");
		if(contents.toString().equals("True")) {
		    out.print("checked=\"checked\"");
		}
		out.print(" />");
		break;

	    default:
		out.print("   <input type=\"text\" length=\"30\" name=\"");
		out.print(label);
		out.print("\" value=\"");
		out.print(contents.toString());
		out.println("\" />");
		break;
	    }
	    out.println("  </td>");
	    if(col==numHTMLCols-1) {
		col=0;
		out.println(" </tr>");
		out.println(" <tr>");
	    } else {
		col++;
	    }
	    cell++;
	} while(cell<numCols);
	out.println(" </tr>");
	out.println("</table>");
	out.flush();
    }
}
