package ca.sqlpower.util;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

/**
 * 
 * NaanSafeDecimalFormat
 *
 * The default unicode character for NaN doesn't render nicely 
 * on some platforms.  Replace it with "NaN" (and when we have time,
 * with a localized string for NaN);
 * 
 * Bear in mind that this will not parse back nicely!
 * 
 */
public class NaanSafeNumberFormat extends DecimalFormat {
	public NaanSafeNumberFormat(String theFormat) {
		super(theFormat);
		DecimalFormatSymbols dfs = getDecimalFormatSymbols();
		dfs.setNaN("NaN"); // FIXME: make this a localized String when we have more time
		setDecimalFormatSymbols(dfs);
	}
}
