package ca.sqlpower.util;

import javax.servlet.*;
import javax.servlet.http.*;
import java.util.*;

/**
 * Some static methods that help with generating web applications.
 *
 * @author Jonathan Fuerth
 * @version $Id$
 */
public class Web {
    /**
     * generates an html table of the paramater names and values of an
     * HttpServletRequest object.  This method is not expected to be
     * used in production; it is simply a debugging tool.
     *
     * @param req the request whose fields should be tabulated
     * @return an html <code>TABLE</code> element describing the request
     * @see #formatSessionAsTable(HttpSession)
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
     * @see #formatRequestAsTable(HttpServletRequest)
     */
    public static String formatSessionAsTable(HttpSession s) {
	Enumeration enm = s.getAttributeNames();
	StringBuffer sb = new StringBuffer(200);
	sb.append("<table border=\"1\">");
        while(enm.hasMoreElements()) {
            String thisElement = (String)enm.nextElement();
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

    /**
     * @deprecated use the version that splits up the argument
     * hasAnyAll into hasAny and hasAll
     */
    public static String makeSelectionList(String name,
					   List options,
					   String defaultSelection,
					   boolean hasAnyAll) {
	return makeSelectionList(name, options, defaultSelection,
				 hasAnyAll, hasAnyAll);
    }

    public static String makeSelectionList(String name,
					   List options,
					   String defaultSelection,
					   boolean hasAny,
					   boolean hasAll) {
	StringBuffer out=new StringBuffer();
	String thisOption;
	if(defaultSelection == null) {
	    defaultSelection="!@#$%^&*() NO DEFAULT ()*&^%$#@!";
	}
	out.append("<select size=\"1\" name=\"");
	out.append(name);
	out.append("\">");
	
	if(hasAny) {
	    appendOption(out, "---Total---", defaultSelection.equals("---Total---"));
	}
	if(hasAll) {
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

	/** Check if the given string contains any special characters (<, >, &) */
	public static boolean containsHTMLMarkup(String testMe){
		for(int i=0; i<testMe.length(); i++){
			if(testMe.charAt(i)=='<' ||
			   testMe.charAt(i)=='>' ||
			   testMe.charAt(i)=='&'){

				return true;
			}
		} // end for (loop through testMe)
		return false;
	}

	/**
	 * Escapes an HTML attribute value (dobule quotes and ampersands
	 * are converted to their character reference equivalents).
	 */
	public static String escapeAttribute(String attval) {
		return escapeImpl(attval, false, false, true, true, false);
	}

	/**
	 * Works like escapeAttribute, but converts spaces to non-breaking
	 * spaces. 
	 */
	public static String escapeAttributeNbsp(String attval) {
		return escapeImpl(attval, false, false, true, true, true);
	}

	/**
	 * Escapes HTML body CDATA (ampersands, greater-than, and
	 * less-than symbols are converted to their character reference
	 * equivalents).
	 */
	public static String escapeHtml(String cdata) {
		return escapeImpl(cdata, true, true, true, false, false);
	}

	/**
	 * Performs the actual escaping for the escapeXXX methods.
	 */
	static String escapeImpl(String str,
							 boolean lt,
							 boolean gt,
							 boolean amp,
							 boolean quot,
							 boolean nbsp) {
		if (str == null) return null;
		StringBuffer escaped = new StringBuffer();
		for (int i = 0, n = str.length(); i < n; i++) {
			switch (str.charAt(i)) {
			case '<':
				if (lt) {
					escaped.append("&lt;");
				} else {
					escaped.append('<');
				}
				break;
				
			case '>':
				if (lt) {
					escaped.append("&gt;");
				} else {
					escaped.append('>');
				}
				break;
				
			case '&':
				if (amp) {
					escaped.append("&amp;");
				} else {
					escaped.append('&');
				}
				break;
				
			case '\"':
				if (quot) {
					escaped.append("&quot;");
				} else {
					escaped.append('"');
				}
				break;
				
			case ' ':
				if (nbsp) {
					escaped.append("&nbsp;");
				} else {
					escaped.append(' ');
				}
				break;

			default:
				escaped.append(str.charAt(i));
				break;
			}
		}
		return escaped.toString();
	}

} // end class
