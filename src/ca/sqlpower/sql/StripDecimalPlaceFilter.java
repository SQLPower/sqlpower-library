package ca.sqlpower.sql;

/**
 * StripDecimalPlaceFilter removes everything after the first '.' in
 * the given string.  If the input string is null, "0" is returned.
 *
 * @author Jonathan Fuerth
 * @version $Id$
 */
public class StripDecimalPlaceFilter implements ColumnFilter {

	/**
	 * Constructs a new StripDecimalPlaceFilter,
	 *
	 */	 
	public StripDecimalPlaceFilter() {
	}

	/**
	 * Effectively the same thing as <code>new StripDecimalPlaceFilter</code>.
	 */
    public static ColumnFilter getInstance() {
        return (ColumnFilter)new StripDecimalPlaceFilter();
    }

	/**
	 * Removes everything after the first '.' in the given string.
	 * If the input string is null, "0" is returned.
	 *
	 * @param in The input string
	 * @return All characters leading up to the first "." in the input
	 * string, or the entire string if there is no ".".  If <code>in
	 * == null</code>, "0" is returned.
	 */
    public String filter(String in) {
		if(in==null) {
			return "0";
		} else {
			int firstDot=in.indexOf('.');
			if(firstDot>=0) {
				return in.substring(0, firstDot);
			} else {
				return in;
			}
		}
    }
}
