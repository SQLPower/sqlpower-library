package ca.sqlpower.util;

import java.text.*;
import java.util.*;

/**
 * The LongMessageFormat class is intended to work identically to the
 * java.text.MessageFormat class, except it allows any number of
 * substitution parameters instead of just the single digits 0-9.  I
 * looked into overriding MessageFormat, but it makes private
 * assumptions about the number of {} substitutions not exceeding 10.
 * The extended format syntax like {3,date} is <em>not</em> currently
 * supported (except for {x,number} and
 * {x,number:&lt;customFormat&gt;} which are special cases for the
 * Dashboard application).</p>
 *
 * <p>See java.text.MessageFormat in the J2SE API docs for details.
 *
 * @author Jonathan Fuerth
 * @version $Id$
 * 
 */
public class LongMessageFormat extends Format {
	/**
	 * A state for the parser's FSM.
	 */
	protected static final int OUTSIDE_BRACKETS=0;

	/**
	 * A state for the parser's FSM.
	 */
	protected static final int INSIDE_BRACKETS=1;

	/**
	 * The chunks of text outside the {} brackets. The String at index
	 * 0 comes before the first {, and the String at index 1 comes
	 * between the matching } and the next opening brace.  Empty parts
	 * are stored as the empty String, not <code>null</code>.
	 */
	protected List stringChunks;

	/**
	 * Instances of Format classes that will be used to render each of
	 * the substituted objects.  If one of these is null, the argument
	 * object will be converted to a string implicitly by
	 * <code>StringBuffer.append(Object)</code>.
	 */
	protected Format[] formats;

	/**
	 * There's no guarantee that the format specifiers will be given
	 * in ascending order; this array maps positional breaks to the
	 * given numbers in the original pattern string.  For instance:
	 * <pre>
	 * pattern == "Hello {1}.  Pleased to {2} you.  Have a nice {0}!"
     * formatNumber == {1, 2, 0}
	 * </pre>
	 */
	protected int[] formatNumber;

	/**
	 * Constructs a new <code>LongMessageFormat</code> with the given pattern.
	 *
	 * @param pattern The new format pattern to parse and use.
	 * @throws IllegalArgumentException if the pattern format is invalid.
	 */
	public LongMessageFormat(String pattern) throws IllegalArgumentException {
		applyPattern(pattern);
	}

	/**
	 * Formats the current pattern, substituting the objects in the
	 * given array for the {} substitution specifiers in the pattern.
	 * It's probably handier to use {@link #format(Object[],
	 * StringBuffer, FieldPosition)} in most cases.
	 *
	 * @param objArray An array of Objects
	 * @param toAppendTo
	 * @param ignore Not used.  It is safe to pass in <code>null</code>.
	 * @throws IllegalArgumentException if the objArray is not of type
	 * Object[].
	 */
	public StringBuffer format(Object objArray,
							   StringBuffer toAppendTo,
							   FieldPosition ignore)
		throws IllegalArgumentException {
		return format((Object[])objArray, toAppendTo, ignore);
	}

	/**
	 * Formats the current pattern, substituting the objects in the
	 * given array for the {} substitution specifiers in the pattern.
	 *
	 * @param objArray An array of Objects
	 * @param toAppendTo
	 * @param ignore Not used.  It is safe to pass in <code>null</code>.
	 */
	public StringBuffer format(
		Object[] objArray,
		StringBuffer toAppendTo,
		FieldPosition ignore) {
		int chunkNum = 0;
		Iterator chunkIterator = stringChunks.iterator();
		while (chunkIterator.hasNext()) {
			toAppendTo.append(chunkIterator.next());
			if (chunkIterator.hasNext()) {
				Object insertMe = objArray[formatNumber[chunkNum]];
				Format thisFieldFormat = formats[chunkNum];
				String parsedString = null;
				if (thisFieldFormat != null) {
					Object parsedObject = null;
					try {
						if (thisFieldFormat instanceof NumberFormat) {
							parsedObject = new Float((String) insertMe);
						} else {
							parsedObject = thisFieldFormat.parseObject((String) insertMe);
						}
						parsedString = thisFieldFormat.format(parsedObject);
					} catch (ParseException e) {
						// XXX: We dump these exceptions to allow non-numerics in a number field
						parsedString = insertMe.toString();
					} catch (NumberFormatException e) {
						// XXX: We dump these exceptions to allow non-numerics in a number field
						parsedString = insertMe.toString();
					}
					toAppendTo.append(parsedString);
				} else {
					toAppendTo.append(insertMe);
				}
			}

			chunkNum++;
		}
		return toAppendTo;
	}
	/**
	 * It's not feasible to parse a formatted message; the process is
	 * not reliable.  Therefore, this method is not implemented.
	 *
	 * @param source Unused.
	 * @param ignore Unused.
	 * @throws UnsupportedOperationException when called.
	 */
	public Object parseObject(String source, ParsePosition ignore)
		throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	/**
	 * Sets the Format object to be used for formatting the
	 * <code>i</code>th Object in the array passed to
	 * <code>format()</code>.  A value of <code>null</code> is
	 * allowed, and means to use no Format object.
	 */
	public void setFormat(int i, Format theFormat) {
		formats[i]=theFormat;
	}

	/**
	 * Gets the Format object that will be used for formatting the
	 * <code>i</code>th Object in the array passed to
	 * <code>format()</code>.  A value of <code>null</code> means no
	 * Format object will be used.
	 */
	public Format getFormat(int i) {
		return formats[i];
	}

	/**
	 * Parses the given format pattern so it's ready for use in
	 * subsequent calls to <code>format()</code>.
	 *
	 * @param pattern The new format pattern to parse and use.
	 * @throws IllegalArgumentException if the pattern format is invalid.
	 */
	public void applyPattern(String pattern)
		throws IllegalArgumentException {
		int numSpecifiers=countBraces(pattern);
		formatNumber=new int[numSpecifiers];
		stringChunks=new LinkedList();
		formats=new Format[numSpecifiers];
		parsePattern(pattern);
	}

	/**
	 * Counts the number of braces in a string.  Does not accept
	 * nested or unbalanced brace brackets.
	 *
	 * @param pattern The string which should be syntax-checked and
	 * counted for balanced, unnested brace bracket pairs.
	 * @throws IllegalArgumentException If there are nested or
	 * unbalanced brace brackets.
	 */
	protected int countBraces(String pattern) throws IllegalArgumentException {
		int count=0;
		int state=OUTSIDE_BRACKETS;
		for(int i=0; i<pattern.length(); i++) {
			char ch=pattern.charAt(i);
			switch(state) {
			case OUTSIDE_BRACKETS:
				switch(ch) {
				case '}':
					throw new IllegalArgumentException(
						 "Found '}' without matching '{' at position "+i);

				case '{':
					state=INSIDE_BRACKETS;
					break;

				default:
					break;
				}
				break;

			case INSIDE_BRACKETS:
				switch(ch) {
				case '}':
					state=OUTSIDE_BRACKETS;
					count++;
					break;

				case '{':
					throw new IllegalArgumentException
						("Found '{' after '{' at position "+i);
					
				default:
					break;
				}
			}
		}
		if(state==INSIDE_BRACKETS) {
			throw new IllegalArgumentException("Unterminated '{'");
		}
		return count;
	}

	/**
	 * Parses the given pattern and populates the member variables
	 * stringChunks and formatNumber.  Assumes balanced braces in the
	 * string, so make sure you've already cheched the string with
	 * {@link #countBraces}.  Also assumes the formatNumber array is
	 * big enough to hold the whole format mapping.
	 *
	 * <p>The implementation essentially counts the pattern like
	 * pegging a cribbage board.  It starts at the beginning, then
	 * finds the first '{'.  It adds the chunk between the beginning
	 * and the '{'. Then it pegs to the matching '}' and parses the
	 * number in between. Then it sets the start to be right after the
	 * '}' and loops.
	 *
	 * @param pattern The pattern you want to parse.
	 */
	protected void parsePattern(String pattern) {
		int pos=0;  // Current parse position in the pattern string.
		int nextPos=0; // Next parse position
		int blockNum=0;
		nextPos=pattern.indexOf('{', pos);
		while(nextPos >= 0) {
			stringChunks.add(pattern.substring(pos, nextPos));
			int formatNumStart=nextPos+1;
			int formatNumEnd = 0;
			formatNumEnd=pattern.indexOf('}', formatNumStart);
			String formatNumStr=pattern.substring(formatNumStart, formatNumEnd);
			String formatType = null;
			int commaPos = formatNumStr.indexOf(',');
			if (commaPos != -1) {
				formatType = formatNumStr.substring(commaPos+1);
				formatNumStr = formatNumStr.substring(0,commaPos);
			}							
			int parsedFormatNum=0;
			try {
				parsedFormatNum=Integer.parseInt(formatNumStr);
			} catch(NumberFormatException e) {
				throw new IllegalArgumentException
				   ("The format argument '"+formatNumStr+"' is not a number.");
			}
			if (formatType != null) {
				if (formatType.startsWith("number")) {
					if (formatType.length() == "number".length()) {
						setFormat(blockNum, new DecimalFormat("#,##0.##"));
					} else {
						// custom DecimalFormat pattern was specified
						int formatIdx = formatType.indexOf(':');
						if (formatIdx < 0) {
							throw new IllegalArgumentException
								("Custom number format '"+formatType+"' incorrect. "
								 +"You must use the form \"{number:\"<format>\"}\" "
								 +"where <format> is a DecimalFormat pattern.");
						}
						String formatStr = formatType.substring(formatIdx+1);
						setFormat(blockNum, new DecimalFormat(formatStr));
					}
				} else {
					throw new IllegalArgumentException
						("The format argument '"+formatType+"' is unknown.");
				}
			}
			formatNumber[blockNum]=parsedFormatNum;
			blockNum++;
			pos=pattern.indexOf('}', nextPos)+1;
			nextPos=pattern.indexOf('{', pos);
		}
		stringChunks.add(pattern.substring(pos));
	}

	/**
	 * Just a quick demonstration of usage.
	 */
	public static void main(String args[]) {
		String pattern="0: {0,number}, 1: {1}, 2: {2}, 5: {5}, 6: {6}, 7: {7}, 8: {8}, 9: {9}, 10: {10}, 4: {4}, 11: {11}, 3: {3}, 12: {12}.";
		String[] numbersEN = {
			"234972349587.23947523947",
			null,
			null,
			"three",
			"four",
			"five",
			"six",
			"seven",
			"eight",
			"nine",
			"ten",
			"eleven",
			"twelve",
		};
		Object[] numbersES = {
			"234972349587.23947523947",
			"uno",
			"dos",
			"tres",
			"cuatro",
			"cinquo",
			"seis",
			"siete",
			"ocho",
			"nueve",
			"diez",
			"once",
			"doce",
		};
		LongMessageFormat lmf=new LongMessageFormat(pattern);
		System.out.println("stringChunks="+lmf.stringChunks);
		System.out.print("formatNumber=[");
		for(int i=0; i<lmf.formatNumber.length; i++) {
			System.out.print(lmf.formatNumber[i]);
			System.out.print(", ");
		}
		System.out.println("]");
		
		System.out.print("In English: ");
		System.out.println(lmf.format(numbersEN, new StringBuffer(), null));

		System.out.print("En Espanol: ");
		System.out.println(lmf.format(numbersES, new StringBuffer(), null));
	}
}
