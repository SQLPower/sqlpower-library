package ca.sqlpower.util;

import javax.servlet.*;
import javax.servlet.http.*;
import java.util.*;

/**
 * Some static methods that help with generating web applications.
 *
 * @author Jonathan Fuerth
 * @version $CVS$
 */
public class Web {
    /**
     * generates an html table of the paramater names and values of an
     * HttpServletRequest object.  This method is not expected to be
     * used in production; it is simply a debugging tool.
     *
     * @param req the request whose fields should be tabulated
     * @return an html <code>TABLE</code> element describing the request
     * @see {@link #formatSessionAsTable(HttpSession)}
     */
    public static String formatRequestAsTable(HttpServletRequest req) {
        StringBuffer sb = new StringBuffer(200);
        sb.append("<table border=\"1\">");
        for (Enumeration e = req.getParameterNames();e.hasMoreElements() ;) {
            String thisElement = (String)e.nextElement();
            sb.append("<tr><td>")
                .append(thisElement)
                .append("</td><td>[");
            String[] values=req.getParameterValues(thisElement);
            for(int i=0; i<values.length; i++) {
                sb.append(values[i]);
                if(i != values.length-1) {
                    sb.append(", ");
                }
            }
            sb.append("]</td></tr>");
        }
        sb.append("</table>");
        return sb.toString();
    }

    /**
     * generates an html table of the attribute names and classes of
     * an HttpSeession object.  This method is not expected to be used
     * in production; it is simply a debugging tool.
     *
     * @param s the session whose fields should be tabulated
     * @return an html <code>TABLE</code> element describing the session
     * @see {@link #formatRequestAsTable(HttpServletRequest)}
     */
    public static String formatSessionAsTable(HttpSession s) {
	Enumeration enum = s.getAttributeNames();
	StringBuffer sb = new StringBuffer(200);
	sb.append("<table border=\"1\">");
        while(enum.hasMoreElements()) {
            String thisElement = (String)enum.nextElement();
	    if(thisElement == null) {
		sb.append("<tr><td>NULL ELEMENT!</td></tr>");
	 continue;
	    }
            sb.append("<tr><td>")
                .append(thisElement)
                .append("</td><td>");
	    if(s.getAttribute(thisElement) == null) {
		sb.append("NULL!");
	    } else {
		sb.append(s.getAttribute(thisElement).getClass().getName());
	    }
            sb.append("</td></tr>");
	}
        sb.append("</table>");
        return sb.toString();
    }

    public static String makeSelectionList(String name, List options, String defaultSelection, boolean hasAnyAll) {
	StringBuffer out=new StringBuffer();
	String thisOption;

	out.append("<select size=\"1\" name=\"");
	out.append(name);
	out.append("\">");
	
	if(hasAnyAll) {
	    appendOption(out, "---Any---", defaultSelection.equals("---Any---"));
	    appendOption(out, "---All---", defaultSelection.equals("---All---"));
	}

	ListIterator i=options.listIterator();
	while(i.hasNext()) {
	    thisOption=(String)i.next();

	    appendOption(out, thisOption, thisOption.equals(defaultSelection));
	}
	out.append("</select>");
        return out.toString();
    }

    private static void appendOption(StringBuffer sb, String optionName, boolean selected) {
	sb.append(" <option");
	if(selected) {
	    sb.append(" selected");
	}
	sb.append(">");
	sb.append(optionName);
	sb.append("</option>");
    }
}
