package ca.sqlpower.sql;


/**
 * YesNoFilter helps with displaying the SQLPower standard 'Y'/'N'
 * indicators with internationalization support.
 *
 * <p><i>I say 'yes', you say 'no' / I say 'stop' and you say 'go, go, go..'</i>
 *
 * @author Jonathan Fuerth
 * @version $Id$
 */
public class YesNoFilter implements ColumnFilter {
	
	/**
	 * The string that will be used when a 'Y' is filtered.
	 */
	private String yString;

	/**
	 * The string that will be used when an 'N' is filtered.
	 */
	private String nString;
	
	/**
	 * Creates a new YesNoFilter.
	 * 
	 * @param yesString The string that will be used when a 'Y' is filtered.
	 * @param noString The string that will be used when an 'N' is filtered.
	 */	 
	public YesNoFilter(String yesString, String noString) {
		yString=yesString;
		nString=noString;
	}

	/**
	 * Turns 'Y' into whatever you set yesString to, and 'N' into
	 * whatever you set noString to.  Passes everything else
	 * (including <code>null</code>) as-is.
	 *
	 * @param in The input string
	 * @return as specified above.
	 */
    public String filter(String in) {
    	if (in == null) {
    		return null;
    	} else if (in.equals("Y")) {
    		return yString;
    	} else if (in.equals("N")) {
			return nString;
		} else {
			return in;
		}
    }
}
