package ca.sqlpower.util;

import java.io.PrintStream;
import java.util.Enumeration;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * Just a collection of methods for dealing with common servlet tasks.
 *
 * @author Jonathan Fuerth
 * @version $Id$
 */
public class Servlets {


	/**
	 * Prints all the parameter names and values, one parameter per
	 * line, to the given print writer.
	 *
	 * @param out The PrintWriter to send the formatted request data to.
	 * @param req The servlet request object you want to examine.
	 */
	public static void printRequest(PrintStream out, ServletRequest req) {
		Enumeration params=req.getParameterNames();
		while(params.hasMoreElements()) {
			String param=(String)params.nextElement();
			out.print(param);
			out.print(": ");
			out.println(arrayToString(req.getParameterValues(param)));
		}
	}

	/**
	 * Prints all the parameter names and values, one parameter per
	 * line, to the given print writer.
	 *
	 * @param out The PrintWriter to send the formatted session data to.
	 * @param req The servlet session object you want to examine.
	 */
	public static void printRequestAttributes(PrintStream out, HttpServletRequest request) {
		Enumeration attrs=request.getAttributeNames();
		while(attrs.hasMoreElements()) {
			String attr=(String)attrs.nextElement();
			out.print(attr);
			out.print(": ");
			out.println(request.getAttribute(attr));
		}
	}

	/**
	 * Prints all the parameter names and values, one parameter per
	 * line, to the given print writer.
	 *
	 * @param out The PrintWriter to send the formatted session data to.
	 * @param req The servlet session object you want to examine.
	 */
	public static void printSession(PrintStream out, HttpSession session) {
		Enumeration attrs=session.getAttributeNames();
		while(attrs.hasMoreElements()) {
			String attr=(String)attrs.nextElement();
			out.print(attr);
			out.print(": ");
			out.println(session.getAttribute(attr));
		}
	}



	/**
	 * Returns a comma-separated list of the <code>toString()</code>
	 * value of each entry of the array.
	 *
	 * @param array An array of objects.  The <code>toString()</code>
	 * method will be called on each non-<code>null</code> entry.
	 * @return A string representation of the array, or <code>"(null
	 * array)"</code> if the <code>array</code> argument is
	 * <code>null</code>.
	 */
	public static String arrayToString(Object[] array) {
		if(array==null) {
			return "(null array)";
		}
		StringBuffer arrayString=new StringBuffer();
		boolean firstItem=true;
		for(int i=0; i<array.length; i++) {
			if(!firstItem) {
				arrayString.append(", ");
			}
			if(array[i]==null) {
				arrayString.append("(null entry)");
			} else {
				arrayString.append(array[i].toString());
			}
		}
		return arrayString.toString();
	}
}
