package ca.sqlpower.sql;

import ca.sqlpower.sql.*;
import org.apache.struts.util.*;

/**
 * StringTruncateFilter truncates a string to a specific length.
 *
 * @author Jonathan Fuerth and Dan Fraser
 * @version $Id$
 */
public class StringTruncateFilter implements ColumnFilter {
	/**
	 * The length of the final string.
	 */
	int length;
	
	/**
	 * Creates a new StringTruncateFilter.
	 * 
	 * @param length the maximum length of the returned strings.
	 */	 
	public StringTruncateFilter(int length) {
		if (length < 0) {
			throw new IllegalArgumentException("length must be nonnegative");
		}
		this.length = length;
	}

	/**
	 * Filters the input string based on the length set in this filter.
	 *
	 * @param in The input string
	 * @return the truncated string, or null if the input string was null.
	 */
    public String filter(String in) {
    	if (in == null) {
    		return null;
    	}
    	if (in.length() < length) {
    		return in;
    	}
		return in.substring(0,length);
    }
    
	/**
	 * Returns the length.
	 * @return int
	 */
	public int getLength() {
		return length;
	}

	/**
	 * Sets the length.
	 * @param length The length to set
	 */
	public void setLength(int length) {
		this.length = length;
	}

}
