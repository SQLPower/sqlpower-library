package ca.sqlpower.sql;

import ca.sqlpower.util.*;
import java.text.*;
import java.util.*;
import java.sql.*;
import java.io.*;
import java.awt.Color;

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
    private boolean rowHighlightingOn;
    private String[] extraJavaScript;
    private int repeatHeaderRow;

    /**
     * A javascript function that gets included in the output code if
     * rowHighlightingOn is true at the time the format() method is
     * called.
     */
    private static final String JS_HIGHLIGHT_CODE = 
	"<script language=\"JavaScript\">\n"
	+"// Derived from code found at javascript.faqts.com\n"
	+"function highlightRow (boolean_element, h_color, n_color) {\n"
	+" while (boolean_element.tagName.toUpperCase() != 'TR' && boolean_element != null)\n"
	+" boolean_element = document.all?boolean_element.parentElement:boolean_element.parentNode;\n"
	+" if (boolean_element) {\n"
	+"  if(boolean_element.value) {\n"
	+"   boolean_element.bgColor = h_color;\n"
	+"  } else {\n"
	+"   boolean_element.bgColor = n_color;\n"
	+"  }\n"
	+" }\n"
	+"}\n"
	+"</script>\n";


    /**
     * Public constructor.  Sets up defaults as follows:
     * <pre>
     * Dropdowns Inline    = false;
     * Dropdowns Above     = true;
     * Dropdowns Per Row   = 3;
     * Row Highlighting On = true;
     * Extra JavaScript    = null for all FieldTypes;
     * Repeat Header Row   = 0.
     * </pre>
     */
    public WebResultHTMLFormatter() {
	super();
	dropdownsInline=false;
	dropdownsAbove=true;
	dropdownsPerRow=3;
	rowHighlightingOn=false;
	extraJavaScript=new String[FieldTypes.LAST_TYPE];
	repeatHeaderRow=0;
    }
    
    /**
     * Gets the value of dropdownsInline.
     *
     * @return value of dropdownsInline.
     */
    public boolean isDropdownsInline() {return dropdownsInline;}
    
    /**
     * Sets the value of dropdownsInline.  DropdowsInline and
     * DropdownsAbove are not mutually exclusive, so be sure to set
     * them to opposite values unless you want two sets of dropdown
     * boxes.
     *
     * @param v  Value to assign to dropdownsInline.
     */
    public void setDropdownsInline(boolean  v) {this.dropdownsInline = v;}

    /**
     * Gets the value of dropdownsAbove.
     *
     * @return value of dropdownsAbove.
     */
    public boolean isDropdownsAbove() {return dropdownsAbove;}
    
    /**
     * Sets the value of dropdownsAbove.  DropdowsInline and
     * DropdownsAbove are not mutually exclusive, so be sure to set
     * them to opposite values unless you want two sets of dropdown
     * boxes.
     *
     * @param v  Value to assign to dropdownsAbove.
     */
    public void setDropdownsAbove(boolean  v) {this.dropdownsAbove = v;}
    
    /**
     * Gets the value of dropdownsPerRow.
     *
     * @return value of dropdownsPerRow.
     */
    public int getDropdownsPerRow() {return dropdownsPerRow;}
    
    /**
     * Sets the value of dropdownsPerRow.
     *
     * @param v  Value to assign to dropdownsPerRow.
     */
    public void setDropdownsPerRow(int  v) {this.dropdownsPerRow = v;}
        
    /**
     * Finds out if the Javascript row-highlighting code is enabled.
     *
     * @return true if row highlighting is enabled; false otherwise.
     */
    public boolean isRowHighlightingOn() {return rowHighlightingOn;}
    
    /**
     * Sets the status of whether or not the row-highlighting
     * Javascript code will be included in the generated HTML.  Row
     * highlighting will turn a row a different colour (controlled by
     * the property "rowHighlightColour") when its radio box or
     * checkbox is checked.
     *
     * @param v A value of true will enable row highlighting.  False
     * will disable it.
     */
    public void setRowHighlightingOn(boolean  v) {this.rowHighlightingOn = v;}
        

    /**
     * Gets the user-supplied extra javascript code that will be
     * included in the resulting element's "onClick" event handler.
     * This doesn't really apply to most element types.
     *
     * @param type The FieldTypes type that you wish to inquire on.
     * @throws IndexOutOfBoundsException if the specified type is not
     * in the range [0..FieldTypes.MAX_TYPE].
     */
    public String getExtraJavaScript(int type) {
	return extraJavaScript[type];
    }

    /**
     * Sets the extra javascript code that will be included in the
     * resulting element's "onClick" event handler.  This doesn't
     * really apply to most element types, and the existence of code
     * in this property doesn't guarantee that this code will appear
     * in the resulting HTML.  For instance, extra javascript code
     * associated with FieldTypes.TEXT has no effect.
     *
     * @param type The FieldTypes type that you wish to associate the
     * javascript statements with.
     * @throws IndexOutOfBoundsException if the specified type is not
     * in the range [0..FieldTypes.MAX_TYPE].
     */
    public void setExtraJavaScript(int type, String js) {
	extraJavaScript[type]=js;
    }
    
    /**
     * Get the current setting for the frequency of Header Row
     * repetition. For instance, if repeatHeaderRow is 10, then every
     * 10th row in the result table will be the header row.
     *
     * @return value of repeatHeaderRow.
     */
    public int getRepeatHeaderRow() {
	return repeatHeaderRow;
    }
    
    /**
     * Set a new value for the frequency of Header Row repetition. For
     * instance, if repeatHeaderRow is 10, then every 10th row in the
     * result table will be the header row. A value of 0 disables
     * header repetition.
     *
     * @param v Value to assign to repeatHeaderRow.
     */
    public void setRepeatHeaderRow(int v) {
	this.repeatHeaderRow = v;
    }
    
    public String format(WebResultSet wrs)
	throws SQLException, NoRowidException, IllegalStateException {
	StringWriter out=new StringWriter();
	formatToStream(wrs, out);
	return out.toString();
    }

    public void formatToStream(WebResultSet wrs, PrintWriter out)
	throws SQLException, NoRowidException, IllegalStateException {
	formatToStream(wrs, out);
    }

    public void formatToStream(WebResultSet wrs, Writer writer)
	throws SQLException, NoRowidException, IllegalStateException {
	PrintWriter out=new PrintWriter(writer);
	int numCols=wrs.getColumnCount();
	StringBuffer sb=new StringBuffer(256);
	int countdownToNextHeader=repeatHeaderRow;
	if(countdownToNextHeader==0) {
	    countdownToNextHeader=-1;
	}

	if(rowHighlightingOn) {
	    out.print(JS_HIGHLIGHT_CODE);
	}

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

	outputHeaderRow(wrs, out);

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
		    boolean valueIsDefault = true;
		    String colDefault = wrs.getColumnDefaultValue(i);
		    String rawContents = wrs.getString(i);
		    align.setLength(0);
		    contents.setLength(0);

		    getColumnFormatted(wrs, i, contents, align);
		    if(colDefault != null && rawContents != null) {
			valueIsDefault = rawContents.equals(colDefault);
		    }
		    sb.append("  <td align=\"");
		    sb.append(align);
		    sb.append("\">");
		    if(!valueIsDefault) {
			sb.append("<font color=\"red\">");
		    }
		    sb.append(contents);
		    if(!valueIsDefault) {
			sb.append("</font>");
		    }
		    sb.append("</td>");
		    out.println(sb);
		} catch(ColumnNotDisplayableException e) {
		    // Column didn't get printed (which is good)
		}
	    }
	    out.println(" </tr>");

	    countdownToNextHeader--;
	    if(countdownToNextHeader==0) {
		outputHeaderRow(wrs, out);
		countdownToNextHeader=repeatHeaderRow;
	    }

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

    protected void getColumnFormatted(WebResultSet wrs,
				      int i,
				      StringBuffer contents,
				      StringBuffer align) 
	throws SQLException, NoRowidException, ColumnNotDisplayableException {
	int type=wrs.getColumnType(i);
	
	switch(type) {
	case FieldTypes.RADIO:
	    if(wrs.getString(i) != null) {
		align.append("center");
		contents.append("<input type=\"radio\" name=\"")
		    .append(wrs.getColumnLabel(i))
		    .append("\" value=\"")
		    .append(wrs.getRowid())
		    .append("\" onClick=\"");
		if(extraJavaScript[FieldTypes.RADIO] != null) {
		    contents.append(extraJavaScript[FieldTypes.RADIO]);
		}
		if(rowHighlightingOn) {
		contents.append("; highlightRow(this, ")
		    .append("'00ff00',").append("'ff00ff'")
		    .append(")");
		}
		contents.append("; this.form.submit()\" />");
	    }
	    break;
		
	case FieldTypes.CHECKBOX:
	    align.append("center");
	    if(wrs.getString(i) != null) {
		contents.append("<input type=\"checkbox\" name=\"")
		    .append(wrs.getColumnLabel(i))
		    .append("\" value=\"")
		    .append(wrs.getRowid())
		    .append("\"");
		if(extraJavaScript[FieldTypes.CHECKBOX] != null) {
		    contents.append(" onClick=\"")
			.append(extraJavaScript[FieldTypes.RADIO])
			.append("\"");
		}
		if(wrs.getString(i).equals(checkboxYesValue)) {
		    contents.append(" checked");
		}
		contents.append(" />");
	    }
	    break;

	default:
	    super.getColumnFormatted(wrs, i, contents, align);
	    break;
	}
    }

    protected void outputHeaderRow(WebResultSet wrs, PrintWriter out)
	throws SQLException {
	StringBuffer sb=new StringBuffer();
	int numCols=wrs.getColumnCount();

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
    }
}
