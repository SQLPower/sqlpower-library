package ca.sqlpower.sql;

import ca.sqlpower.util.*;

import java.io.*;
import java.text.*;
import java.util.*;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.awt.Color;
import javax.servlet.jsp.JspWriter;

/**
 * The base class for utilities that format a {@link WebResultSet}
 * into various human-readable formats.  Currently, HTML and CSV
 * output is available.  The Power*Dashboard extends this class to
 * provide an alternate HTML view as well as an applet-based graph
 * view.
 *
 * @author Jonathan Fuerth and Gillian Mereweather
 * @version $Id$
 */
public abstract class WebResultFormatter {

    protected String rowidParameterName;
    protected String checkboxYesValue;
    protected NumberFormat numberFormatter;
    protected NumberFormat moneyFormatter;
    protected NumberFormat percentFormatter;
    protected DateFormat dateFormatter;
    protected Color rowHighlightColour;
    protected Color rowNormalColour;

    public WebResultFormatter() {
        rowidParameterName="row_id";
        checkboxYesValue="YES";
        numberFormatter=new DecimalFormat("#,##0.##############");
        moneyFormatter=new DecimalFormat("$#,##0.00");
        percentFormatter=new DecimalFormat("0%");
        dateFormatter=new SimpleDateFormat("yyyy-MM-dd");
        rowHighlightColour=Color.yellow;
        rowNormalColour=new Color(0xEE, 0xEE, 0xEE);
    }

    /**
     * Gets the desired parameter name for the row identifier column.
     *
     * @return The row identifier parameter name.
     */
    public String getRowidParameterName() {
        return rowidParameterName;
    }

    /**
     * Gets the desired parameter name for the row identifier column.
     *
     * @param newName The new row identifier parameter name.
     */
    public void setRowidParameterName(String newName) {
        rowidParameterName=newName;
    }

    /**
     * Gets the value that indicates a "checked" (on) indicator in the
     * database.
     *
     * @return the string representing "true"
     */
    public String getCheckboxYesValue() {
        return checkboxYesValue;
    }
    
    /**
     * Sets the value that indicates a "checked" (on) indicator in the
     * database. All other values are considered to mean "false."
     *
     * @param v The new value of the string representing "true."
     */
    public void setCheckboxYesValue(String  v) {
        this.checkboxYesValue = v;
    }    

    public void setNumberFormatter(NumberFormat v) {
        numberFormatter=v;
    }

    public void setMoneyFormatter(NumberFormat v) {
        moneyFormatter=v;
    }

    public void setPercentFormatter(NumberFormat v) {
        percentFormatter=v;
    }

    public void setDateFormatter(DateFormat v) {
        dateFormatter=v;
    }

    /**
     * Gets the current setting of the row highlighting colour.
     *
     * @return the current row highlighting colour
     */
    public Color getRowHighlightColour() {return rowHighlightColour;}
    
    /**
     * Sets the value of the row highlight colour.
     *
     * @param v Value to assign to row highlight colour.
     */
    public void setRowHighlightColour(Color  v) {this.rowHighlightColour = v;}
    
    /**
     * Gets the current setting of the row normal colour.
     *
     * @return the current row normal colour
     */
    public Color getRowNormalColour() {return rowNormalColour;}
    
    /**
     * Sets the value of the row normal colour.
     *
     * @param v Value to assign to row normal colour.
     */
    public void setRowNormalColour(Color  v) {this.rowNormalColour = v;}
    
    protected String beautifyHeading(String heading) {
        StringBuffer newHeading=new StringBuffer(heading);

        for(int i=0; i<newHeading.length(); i++) {
            if(newHeading.charAt(i) == '_') {
                newHeading.setCharAt(i, ' ');
            }
        }
        return newHeading.toString();
    }

    public static String beautifyColumnName(String colName) {
        StringBuffer newColName=new StringBuffer(colName);
		final int CAP_NEXT=1;
		final int LOWER_NEXT=2;
		int state=CAP_NEXT;
        for(int i=0; i<colName.length(); i++) {
			if(newColName.charAt(i) == '_') {
				newColName.setCharAt(i, ' ');
				state=CAP_NEXT;
				continue;
			}

			switch(state) {
			case CAP_NEXT:
				newColName.setCharAt(i, Character.toUpperCase(colName.charAt(i)));
				state=LOWER_NEXT;
				break;
				
			case LOWER_NEXT:
				newColName.setCharAt(i, Character.toLowerCase(colName.charAt(i)));
				break;

			}
        }
        return newColName.toString();
    }

  	/**
 	 * Examines the <code>i</code>th column of with Web Result Set,
 	 * and fills the <code>contents</code> and <code>align</code>
 	 * buffers with the appropriate string values. The type of
 	 * formatting done depends on the web result set's idea of the
 	 * <code>i</code>th column's type.
  	 *
 	 * @param wrs The web result set whose current row's ith column
 	 * should be rendered.
 	 * @param i The column number to render.
 	 * @param contents The textual contents that should be displayed
 	 * to the user are appended to this StringBuffer.
 	 * @param align The alignment information ("left", "center",
 	 * "right") for this column is appended to this StringBuffer.  It
 	 * is not a coincidence that they are the same as HTML 3.2 align
 	 * attributes, but you could/should use them for other output
 	 * formats.
 	 * @throws SQLException if a database error occurrs while
 	 * retrieving the contents of the WebResultSet record.
 	 * @throws NoRowidException if the field type needs rowid
 	 * information to render, and no column in <code>wrs</code> was
 	 * defined as supplying a rowid.  <code>contents</code> and
 	 * <code>align</code> may have already been modified if this
 	 * exception is thrown.
 	 * @throws ColumnNotDisplayableException if the type of this field
 	 * is such that it can't (or shouldn't) be displayed.  If this
 	 * exception is thrown, <code>contents</code> and
 	 * <code>align</code> are guaranteed to be unmodified.
 	 * @throws IllegalStateException if this is a HYPERLINK field and
 	 * <code>wrs.getColumnHyperlinks(i)</code> returns null.
 	 * @see FieldTypes
  	 */
    protected void getColumnFormatted(WebResultSet wrs,
                                      int i,
                                      StringBuffer contents,
                                      StringBuffer align) 
        throws SQLException, NoRowidException, ColumnNotDisplayableException,
		IllegalStateException {
        int type=wrs.getColumnType(i);
        
        switch(type) {
        case FieldTypes.NUMBER:
            align.append("right");
            contents.append(numberFormatter.format(wrs.getFloat(i)));
            break;
        	
        case FieldTypes.NAME:
            align.append("left");
            contents.append(wrs.getString(i));
            break;
            
        case FieldTypes.TEXT_DEFAULT_NA:
            align.append("left");
            if(wrs.getString(i) == null || wrs.getString(i).equals("")){
				contents.append("n/a");
			} else {
				contents.append(wrs.getString(i));
			}
            break;
            
        case FieldTypes.TEXT_DEFAULT_UNKNOWN:
            align.append("left");
            if(wrs.getString(i) == null || wrs.getString(i).equals("")){
				contents.append("Unknown");
			} else {
				contents.append(wrs.getString(i));
			}
            break;
            
        case FieldTypes.TEXT_DEFAULT_NONE:
            align.append("left");
            if(wrs.getString(i) == null || wrs.getString(i).equals("")){
				contents.append("None");
			} else {
				contents.append(wrs.getString(i));
			}
            break;
            
        case FieldTypes.MONEY:
            align.append("right");
            contents.append(moneyFormatter.format(wrs.getFloat(i)));
            break;

        case FieldTypes.BOOLEAN:
            align.append("center");
            String tmp=wrs.getString(i);
            if(tmp != null && SQL.decodeInd(tmp)) {
                contents.append("True");
            } else {
                contents.append("False");
            }
            break;
            

        case FieldTypes.YESNO_DEFAULT_NO:
            align.append("center");
            if(wrs.getString(i) == null || wrs.getString(i).equals("")){
                contents.append("N");
            } else {
                contents.append(wrs.getString(i));
            }
            break;

        case FieldTypes.YESNO_DEFAULT_YES:
            align.append("center");
            if(wrs.getString(i) == null || wrs.getString(i).equals("")){
                contents.append("Y");
            } else {
                contents.append(wrs.getString(i));
            }
            break;

        case FieldTypes.PERCENT:
            align.append("right");
            try {
                contents.append(percentFormatter.format(wrs.getFloat(i)/100));
            } catch(SQLException e) {
                // Non-numeric values cause a number-conversion problem
                contents.append(wrs.getString(i));
            }
            break;
            
        case FieldTypes.DATE:
            align.append("center");
            java.sql.Date date=wrs.getDate(i);
            if(date==null) {
                // leave empty
            } else {
                contents.append(
                    dateFormatter.format(new java.util.Date(date.getTime()))
                    );
            }
            break;

        case FieldTypes.ALPHANUM_CODE:
            align.append("center");
            contents.append(wrs.getString(i));
            break;

 		case FieldTypes.HYPERLINK:
			align.append("center");
 			List hyperlinks=wrs.getColumnHyperlinks(i);
			String style=wrs.getColumnHyperlinkStyle(i);
 			if(hyperlinks == null) {
 				throw new IllegalStateException
 					("You must supply hyperlink specs in the WebResultSet.");
 			}
 			Iterator hlIter=hyperlinks.iterator();
 			while(hlIter.hasNext()) {
 				Hyperlink link=(Hyperlink)hlIter.next();
 				LongMessageFormat textFormat=new LongMessageFormat(link.getText());
 				LongMessageFormat hrefFormat=new LongMessageFormat(link.getHref());
 				int colCount=wrs.getColumnCount();
 				String[] rowValues=new String[colCount+1];
 				for(int col=1; col<=colCount; col++) {
 					rowValues[col]=wrs.getString(col);
 				}
 				contents.append("<a href=\"");
 				hrefFormat.format(rowValues, contents, null);
				if(style!=null) {
					contents.append("\" class=\"").append(style);
				}
 				contents.append("\">");
 				textFormat.format(rowValues, contents, null);
 				contents.append("</a>");
 				if (hlIter.hasNext()) {
 					contents.append("<br>");
 				}
 			}
			break;

        case FieldTypes.ROWID:
        case FieldTypes.DUMMY:
            throw new ColumnNotDisplayableException();
            //no break because throw makes it unnecessary

        case FieldTypes.RADIO:
        case FieldTypes.CHECKBOX:
        case FieldTypes.MUTEX_CHECKBOX:
            //There is no generic way to return a field of this type..
            // So it's left up to the concrete subclasses
            throw new UnsupportedOperationException();

        default:
			ResultSetMetaData md = wrs.getRsmd();
			switch (md.getColumnType(i)) {
				case java.sql.Types.BIGINT:
				case java.sql.Types.INTEGER:
				case java.sql.Types.DECIMAL:
				case java.sql.Types.DOUBLE:
				case java.sql.Types.FLOAT:
				case java.sql.Types.NUMERIC:
				case java.sql.Types.REAL:
				case java.sql.Types.SMALLINT:
				case java.sql.Types.TINYINT:
		            align.append("right");
		            contents.append(numberFormatter.format(wrs.getFloat(i)));
		            break;
		        case java.sql.Types.TIMESTAMP:
		            align.append("center");
        		    java.sql.Date tsDate=wrs.getDate(i);
		            if(tsDate==null) {
		                // leave empty
		            } else {
		                contents.append(
	                    dateFormatter.format(new java.util.Date(tsDate.getTime())));
    		        }
	            break;
				default: 	        	
				     align.append("left");
            		 contents.append(wrs.getString(i));
                     break;
			}

        }


    }
    
    public abstract void formatToStream(WebResultSet wrs, PrintWriter out) 
        throws SQLException, NoRowidException;
    
    public void formatToStream(WebResultSet wrs, JspWriter out) 
        throws SQLException, NoRowidException {
        formatToStream(wrs, new PrintWriter(out));
    }   

	public String format(WebResultSet wrs)
		throws SQLException, NoRowidException {
		StringWriter sout = new StringWriter();
		formatToStream(wrs, new PrintWriter(sout));
		return sout.toString();
	}
}
