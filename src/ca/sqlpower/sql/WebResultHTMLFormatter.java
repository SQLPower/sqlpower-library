package ca.sqlpower.sql;

import ca.sqlpower.util.*;
import java.text.*;
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
    private NumberFormat numberFormatter;
    private NumberFormat moneyFormatter;

    public WebResultHTMLFormatter() {
	super();
	dropdownsInline=false;
	dropdownsAbove=true;
	numberFormatter=new DecimalFormat("#,##0.#");
	moneyFormatter=new DecimalFormat("$#,##0.00");
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

    public void setNumberFormatter(NumberFormat v) {
	numberFormatter=v;
    }

    public void setMoneyFormatter(NumberFormat v) {
	moneyFormatter=v;
    }

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
		    out.print("  <td align=\"center\">");
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

	out.print(" <tr class=\"resultTableHeading\">");
	for(int i=1; i<=numCols; i++) {
	    out.print("  <th valign=\"bottom\">");
	    if(fcRowid && i==1) {
		out.print("&nbsp;");
	    } else {
		out.print(beautifyHeading(wrs.getColumnLabel(i)));
	    }
	    out.println("  </th>");
	}
	out.println(" </tr>");

	if(dropdownsInline) {
	    out.println(" <tr class=\"resultTableHeading\">");
	    for(int i=1; i<=numCols; i++) {
		List choices=wrs.getColumnChoicesList(i);
		
		out.print("  <td>");
		if(choices != null) {
		    out.print(Web.makeSelectionList(wrs.getColumnChoicesName(i),
						    choices,
						    wrs.getColumnDefaultChoice(i),
						    wrs.getColumnHasAnyAll(i)));
		}
		out.println("  </td>");
	    }
	    out.println(" </tr>");
	}

	while(wrs.next()) {
	    out.println(" <tr class=\"resultTableData\">");
	    for(int i=1; i<=numCols; i++) {
		int type=wrs.getColumnType(i);
		String contents;
		String tAlign;

		switch(type) {
		case FieldTypes.NUMBER:
		    tAlign="right";
		    contents=numberFormatter.format(wrs.getFloat(i));
		    break;

		default:
		case FieldTypes.NAME:
		    tAlign="left";
		    contents=wrs.getString(i);
		    break;

		case FieldTypes.MONEY:
		    tAlign="right";
		    contents=moneyFormatter.format(wrs.getFloat(i));
		    break;

		case FieldTypes.BOOLEAN:
		    tAlign="center";
		    contents=wrs.getString(i);
		    if(contents != null) {
			contents="True";
		    } else {
			contents="False";
		    }
		    break;
		    
		case FieldTypes.RADIO:
		    tAlign="center";
		    contents="<input type=\"radio\" name=\""
			+rowidParameterName
			+"\" value=\""
			+wrs.getString(i)
			+"\" onClick=\"this.form.submit()\" />";
		    break;

		case FieldTypes.CHECKBOX:
		    tAlign="center";
		    contents="<input type=\"checkbox\" name=\""
			+rowidParameterName
			+"\" value=\""
			+wrs.getString(i)
			+"\" />";
		    break;		    
		}

		out.print("  <td align=\"");
		out.print(tAlign);
		out.print("\">");
		out.print(contents);
		out.println("</td>");
	    }
	    out.println(" </tr>");
	}
	out.println("</table>");
	out.flush();
    }
}
