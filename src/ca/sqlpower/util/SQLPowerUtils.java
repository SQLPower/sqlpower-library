package ca.sqlpower.util;

import java.beans.PropertyChangeEvent;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.log4j.Logger;

import ca.sqlpower.object.CleanupExceptions;
import ca.sqlpower.object.SPObject;

public class SQLPowerUtils {
	
	public static boolean areEqual (Object o1, Object o2) {
		if (o1 == o2) {
			// this also covers (null == null)
			return true;
		} else {
			if (o1 == null || o2 == null) {
				return false;
			} else {
				// pass through to object equals method
				return o1.equals(o2);
			}
		}
	}

	/**
	 * Replaces double quotes, ampersands, and less-than signs with
	 * their character reference equivalents.  This makes the returned
	 * string be safe for use as an XML content data or as an attribute value
	 * enclosed in double quotes. From the XML Spec at http://www.w3.org/TR/REC-xml/#sec-predefined-ent:
	 * 4.6 Predefined Entities
	 * "Definition: Entity and character references may both be used to escape the left angle bracket, ampersand,
	 * and other delimiters. A set of general entities (amp, lt, gt, apos, quot) is specified for this purpose...]
	 * All XML processors must recognize these entities whether they are declared or not. For interoperability,
	 * valid XML documents should declare these entities..."
	 */
	public static String escapeXML(String src) {
	    if (src == null) return "";
		StringBuffer sb = new StringBuffer(src.length()+10);  // arbitrary amount of extra space
		char ch;
	    
		for (int i = 0, n = src.length(); i < n; i++) {
			ch = src.charAt(i);
	        
	        if (ch == '\'') {
				sb.append("&apos;");
	        }
	        else if (ch == '"') {
				sb.append("&quot;");
	        }
	        else if (ch == '&') {
				sb.append("&amp;");
	        }
	        else if (ch == '<') {
				sb.append("&lt;");
	        }
	        else if (ch == '>') {
				sb.append("&gt;");
	        }
	        else {
				sb.append(ch);
			}
		}
		return sb.toString();
	}
	
    /**
     * Logs the given property change event at the DEBUG level. The format is:
     * <pre>
     *  <i>Message</i>: <i>propertyName</i> "<i>oldValue</i>" -&gt; "<i>newValue</i>"
     * </pre>
     * 
     * @param logger The logger to log to
     * @param message The message to prefix the property change information
     * @param evt The event to print the details of
     */
    public static void logPropertyChange(
            @Nonnull Logger logger, @Nullable String message, @Nonnull PropertyChangeEvent evt) {
        if (logger.isDebugEnabled()) {
            logger.debug(message + ": " + evt.getPropertyName() +
                    " \"" + evt.getOldValue() + "\" -> \"" + evt.getNewValue() + "\"");
        }
    }

	/**
	 * Copies the contents of a given {@link InputStream} into a given
	 * {@link OutputStream}.
	 * 
	 * @param source
	 *            The {@link InputStream} to copy data from
	 * @param output
	 *            The {@link OutputStream} to copy the data to
	 * @return The total number of bytes that got copied.
	 * @throws IOException
	 *             If an I/O error occurs
	 */
    public static long copyStream(InputStream source, OutputStream output) throws IOException {
    	int next;
    	long total = 0;
    	while ((next = source.read()) != -1) {
    		output.write(next);
    		total++;
    	}
    	output.flush();
    	return total;
    }

	/**
	 * This method will recursively clean up a given object and all of its
	 * descendants.
	 * 
	 * @param o
	 *            The object to clean up, including its dependencies.
	 * @return A collection of exceptions and errors that occurred during
	 *         cleanup if any occurred.
	 */
    public static CleanupExceptions cleanupSPObject(SPObject o) {
        CleanupExceptions exceptions = new CleanupExceptions();
        exceptions.add(o.cleanup());
        for (SPObject child : o.getChildren()) {
            exceptions.add(cleanupSPObject(child));
        }
        return exceptions;
    }
}