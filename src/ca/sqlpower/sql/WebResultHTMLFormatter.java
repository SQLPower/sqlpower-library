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
    private int dropdownsPerRow;

    public WebResultHTMLFormatter() {
	super();
	dropdownsInline=false;
	dropdownsAbove=true;
	dropdownsPerRow=3;
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
    
    /**
     * Get the value of dropdownsPerRow.
     *
     * @return value of dropdownsPerRow.
     */
    public int getDropdownsPerRow() {return dropdownsPerRow;}
    
    /**
     * Set the value of dropdownsPerRow.
     *
     * @param v  Value to assign to dropdownsPerRow.
     */
    public void setDropdownsPerRow(int  v) {this.dropdownsPerRow = v;}
    
    public void formatToStream(WebResultSet wrs, PrintWriter out) 
	throws SQLException, NoRowidException, IllegalStateException {
	int numCols=wrs.getColumnCount();
	StringBuffer sb=new StringBuffer(256);

	if(dropdownsAbove) {
	    List choices=null;
	    int i=1;
	    int numRenderedCells=0;

	    out.println("<table align=\"center\">");
	    out.println(" <tr>");
	    while(i<=numCols) {
		sb.setLength(0);
		try {
		    choices=wrs.getColumnChoicesList(i);
		    if(choices != null) {
			sb.append("\n  <td align=\"right\" class=\"searchForm\">");
			sb.append(wrs.getColumnLabel(i));
			sb.append("</td>\n  <td align=\"left\" class=\"searchForm\">");
			sb.append(Web.makeSelectionList(
					  wrs.getColumnChoicesName(i),
					  choices,
					  wrs.getColumnDefaultChoice(i),
					  wrs.getColumnHasAny(i),
					  wrs.getColumnHasAll(i)));
			sb.append("\n  </td>");
			numRenderedCells++;
			if(numRenderedCells >= dropdownsPerRow) {
			    numRenderedCells=0;
			    sb.append("\n </tr>\n <tr>");
			}
		    }
		    out.println(sb);
		} catch(ColumnNotDisplayableException e) {
		    // Column didn't get printed (which is good)
		}
		i++;
	    }
	    out.println(" </tr>");
	    out.println("</table>");
	}

	out.println("<table class=\"resultTable\" align=\"center\">");

	out.println(" <tr class=\"resultTableHeading\">");
	for(int i=1; i<=numCols; i++) {
	    sb.setLength(0);
	    int columnType = wrs.getColumnType(i);
	    try {
		if(columnType != FieldTypes.DUMMY &&
		   columnType != FieldTypes.ROWID) {
		    sb.append("  <th valign=\"bottom\">");
		    sb.append(beautifyHeading(wrs.getColumnLabel(i)));
		    sb.append("</th>");
		    out.println(sb);
		}
	    } catch(ColumnNotDisplayableException e) {
		// Column didn't get printed (which is good)
	    }
	}
	out.println(" </tr>");

	if(dropdownsInline) {
	    out.println(" <tr class=\"resultTableHeading\">");
	    for(int i=1; i<=numCols; i++) {
		sb.setLength(0);
		try {
		    List choices=wrs.getColumnChoicesList(i);
		    sb.append("  <td>");
		    if(choices != null) {
			sb.append(Web.makeSelectionList(
					  wrs.getColumnChoicesName(i),
					  choices,
					  wrs.getColumnDefaultChoice(i),
					  wrs.getColumnHasAny(i),
					  wrs.getColumnHasAll(i)));
		    }
		    sb.append("\n  </td>");
		    out.println(sb);
		} catch(ColumnNotDisplayableException e) {
		    // Column didn't get printed (which is good)
		}
	    }
	    out.println(" </tr>");
	}

	StringBuffer align=new StringBuffer(10);
	StringBuffer contents=new StringBuffer(50);
	boolean mutexOnThisRow=false;
	int mutexRowNum=0;

	while(wrs.next()) {
	    out.println(" <tr class=\"resultTableData\">");
	    for(int i=1; i<=numCols; i++) {
		sb.setLength(0);

		if(wrs.getColumnType(i) == FieldTypes.MUTEX_CHECKBOX) {
		    try {
			sb.append("<td align=\"center\">");
			if(wrs.getString(i) != null) {
			    mutexOnThisRow=true;
			    sb.append("<input type=\"checkbox\" name=\"");
			    sb.append(wrs.getColumnLabel(i));
			    sb.append("\" value=\"");
			    sb.append(wrs.getRowid());
			    sb.append("\" onClick=\"");
			    sb.append(mutexBoxes(wrs.getColumnMutexList(i), mutexRowNum));
			    sb.append("\"");
			    if(wrs.getString(i).equals(checkboxYesValue)) {
				sb.append(" checked");
			    }
			    sb.append(" />");
			}
			sb.append("</td>");
			out.println(sb);			
		    } catch(ColumnNotDisplayableException e) {
			// Never happens
			throw new IllegalStateException("Unexpected ColumnNotDisplayableException caught on MUTEX_CHECKBOX");
		    }
		} else try {
		    align.setLength(0);
		    contents.setLength(0);
		    getColumnFormatted(wrs, i, contents, align);
		    sb.append("  <td align=\"");
		    sb.append(align);
		    sb.append("\">");
		    sb.append(contents);
		    sb.append("</td>");
		    out.println(sb);
		} catch(ColumnNotDisplayableException e) {
		    // Column didn't get printed (which is good)
		}
	    }
	    out.println(" </tr>");

	    // Increment the rows-with-mutexes count if necessary
	    if(mutexOnThisRow) {
		mutexRowNum++;
	    }
	    mutexOnThisRow=false;
	}
	out.println("</table>");

	// Output dummy form-elements to make mutex checkboxes work
	// if there was only one row.
	if(mutexRowNum==1) {
	    for(int i=1; i<numCols; i++) {
		try {
		    if(wrs.getColumnType(i) == FieldTypes.MUTEX_CHECKBOX) {
			out.print("<input type=\"hidden\" name=\"");
			out.print(wrs.getColumnLabel(i));
			out.print("\" value=\"");
			out.println("\" />");
		    }
		} catch(ColumnNotDisplayableException e) {
		    // Never happens for MUTEX_CHECKBOX
		    throw new IllegalStateException("Unexpected ColumnNotDisplayableException on MUTEX_CHECKBOX");
		}
	    }
	}

	out.flush();
    }

    protected StringBuffer mutexBoxes(List mutexList, int mutexRowNum) {
	StringBuffer sb=new StringBuffer(40);
	ListIterator it=mutexList.listIterator();
	String curVal;

	while(it.hasNext()) {
	    curVal=(String)it.next();
	    sb.append("form.").append(curVal).append("[")
		.append(mutexRowNum).append("].checked=false; ");
	}
	return sb;
    }
}
