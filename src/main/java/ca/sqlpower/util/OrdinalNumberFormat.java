package ca.sqlpower.util;

import java.text.Format;
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.text.NumberFormat;

/**
 * The OrdinalNumberFormat takes regular integers and outputs them as
 * strings with the correct English ordinal suffix appended.  For
 * example, given <code>Format f = new OrdinalNumberFormat()</code>,
 * 
 * <ul>
 *  <li><code>f.format(1)</code> returns "1st"
 *  <li><code>f.format(2)</code> returns "2nd"
 *  <li><code>f.format(111)</code> returns "111th"
 * </ul>
 *
 * <p>TODO: Support French and Spanish locales; support parsing; make
 * this a subclass of NumberFormat.
 */
public class OrdinalNumberFormat extends Format {

	/**
	 * This is the FieldPosition id for the number part of the
	 * formatted output.
	 */
	public static final int INTEGER_FIELD = NumberFormat.INTEGER_FIELD;

	/**
	 * This is the FieldPosition id for the non-number part of the
	 * formatted output.
	 */
	public static final int SUFFIX_FIELD = NumberFormat.INTEGER_FIELD + 1;

	
	/**
	 * Appends the ordinal representation of the given object to the
	 * given StringBuffer. See the class-level documentation for
	 * examples.
	 *
	 * @param num The number to format.
	 * @param toAppendTo The formatted result will be concatenated to
	 * this StringBuffer.
	 * @param pos Will be updated to indicate the position of the
	 * numeric (INTEGER_FIELD) or non-numeric (SUFFIX_FIELD) portion
	 * of the formatted string.  If any position id other than
	 * SUFFIX_FIELD or INTEGER_FIELD is specified, pos will remain
	 * untouched.
	 */
	public StringBuffer format(int num, StringBuffer toAppendTo, FieldPosition pos) {

		int startPos = toAppendTo.length();

		// build the formatted string
		switch(num % 100) {
		case 11:
		case 12:
		case 13:
			toAppendTo.append(num).append("th");
			break;

		default:
			switch(num % 10) {
			case 1:
				toAppendTo.append(num).append("st");
				break;

			case 2:
				toAppendTo.append(num).append("nd");
				break;

			case 3:
				toAppendTo.append(num).append("rd");
				break;

			default:
				toAppendTo.append(num).append("th");
				break;
			}
		}
		
		// update the FieldPosition with requested data
		switch (pos.getField()) {
		case INTEGER_FIELD:
			pos.setBeginIndex(startPos);
			pos.setEndIndex(toAppendTo.length());
			break;

		case SUFFIX_FIELD:
			pos.setBeginIndex(toAppendTo.length() - 2);
			pos.setEndIndex(toAppendTo.length());
			break;

		default:
			break;
		}

		return toAppendTo;
	}

	/**
	 * Formats the given number to the given StringBuffer.  See {@link
	 * #format(int,StringBuffer,FieldPosition)}.
	 */
	public StringBuffer format(int num, StringBuffer toAppendTo) {
		final FieldPosition dummyPos = new FieldPosition(INTEGER_FIELD-1);
		return format(num, toAppendTo, dummyPos);
	}

	
	/**
	 * Formats the given number to a new StringBuffer.  See {@link
	 * #format(int,StringBuffer,FieldPosition)}.
	 */
	public StringBuffer format(int num) {
		return format(num, new StringBuffer((int) (Math.log(num)/Math.log(10))+3));
	}

	/**
	 * See {@link #format(int,StringBuffer,FieldPosition)}.  The only
	 * difference is that obj is the number to format, and it has to
	 * be a subclass of Number.
	 */
	public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
		if (! (obj instanceof Number)) {
			throw new IllegalArgumentException
				("OrdinalNumberFormat can only format subclasses of Number");
		}
		return format(((Number) obj).intValue(), toAppendTo, pos);
	}

	/**
	 * Not implemented.
	 */
	public Object parseObject(String source, ParsePosition pos) {
		throw new UnsupportedOperationException("Parsing not implemented");
	}

}
