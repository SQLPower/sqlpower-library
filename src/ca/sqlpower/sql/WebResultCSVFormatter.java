package ca.sqlpower.sql;

import java.io.*;
import java.text.*;
import java.sql.SQLException;

public class WebResultCSVFormatter extends WebResultFormatter {
    public WebResultCSVFormatter() {
	super();
    }

    public void formatToStream(WebResultSet wrs, PrintWriter out) 
	throws SQLException, NoRowidException, IllegalStateException {
	int numCols=wrs.getColumnCount();
	StringBuffer sb=new StringBuffer(256);
	StringBuffer contents=new StringBuffer(256);
	StringBuffer align=new StringBuffer(32);

	// FORMAT HEADINGS
	boolean thisIsTheFirstColumn=true;
	for(int i=1; i<=numCols; i++) {
	    sb.setLength(0);
	    try {
		if(!thisIsTheFirstColumn) {
		    sb.append(",");
		}

		sb.append(makeStringSafe(beautifyHeading(wrs.getColumnLabel(i))));
		out.print(sb);
		thisIsTheFirstColumn=false;
	    } catch(ColumnNotDisplayableException e) {
		// Column didn't get printed (which is good)
	    }
	}
	out.println("");

	// FORMAT BODY
	while(wrs.next()) {
	    thisIsTheFirstColumn=true;
	    for(int i=1; i<=numCols; i++) {
		contents.setLength(0);
		align.setLength(0);
		try {
		    getColumnFormatted(wrs, i, contents, align);
		    if(!thisIsTheFirstColumn) {
			out.print(",");
		    }
		    out.print(makeStringSafe(contents.toString()));
		    thisIsTheFirstColumn=false;
		} catch(ColumnNotDisplayableException e) {
		    // Column didn't get printed (which is good)
		} catch(UnsupportedOperationException e) {
		    // Column didn't get printed, but we wanted it to
		    out.print(makeStringSafe(wrs.getString(i)));
		}
	    }
	    out.println("");
	}
    }

    /**
     * Makes a CSV-file-safe version of a given string.  This includes
     * escaping CR, LF, and double-quote characters with backslashes,
     * and quoting strings that contain whitespace.
     *
     * @param original The original, unsafe string.
     * @return The original string if it contained none of the special
     * characters mentioned above; an empty string if the original was
     * null; otherwise, a quoted version of the original string with
     * the necessary characters backslash-escaped.
     */
    public static String makeStringSafe(String original) {
	if(original==null) {
	    return "";
	}

	StringBuffer escaped = new StringBuffer(original.length());
	StringCharacterIterator it = new StringCharacterIterator(original);
	boolean escapedDiffersFromOriginal=false;
	char ch;
	
	escaped.append("\"");
	
	do {
	    ch=it.current();
	    switch(ch) {
	    case '\n':
		escaped.append("\\n");
		escapedDiffersFromOriginal=true;
		break;
		
	    case '\r':
		escaped.append("\\r");
		escapedDiffersFromOriginal=true;
		break;
		
 	    case '"':
		escaped.append("\\\"");
		escapedDiffersFromOriginal=true;
		break;

	    case '\\':
		escaped.append("\\\\");
		escapedDiffersFromOriginal=true;
		break;

	    case ' ':
	    case '\f':
	    case '\t':
	    case ',':
		// Whitespace and commas don't need to be escaped
		// explicitly, but the whole string will need to be
		// quoted.
		escaped.append(ch);
		escapedDiffersFromOriginal=true;
		break;

	    default:
		escaped.append(ch);
		break;
	    }
	} while( it.next() != StringCharacterIterator.DONE);
	
	escaped.append("\"");
	
	return (escapedDiffersFromOriginal ? escaped.toString() : original);
    }
}
