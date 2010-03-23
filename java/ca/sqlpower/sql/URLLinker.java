package ca.sqlpower.sql;

import java.util.StringTokenizer;

/**
 * The URLLinker class is a filter that inserts HTML markup to make
 * any subtrings that are URLs clickable when viewed in a web browser.
 *
 * @version $Id$
 * @author Jonathan Fuerth
 */
public class URLLinker implements ColumnFilter {
	
	protected String extraAttributes;

	public URLLinker() {
		this("");
	}

	/**
	 * Creates a URLLinker which will insert the given string after
	 * the href attribute in the generated HTML a tags.  You can use
	 * it to make the links appear in a named window, or to set the
	 * anchor's CSS class, or anything else.
	 *
	 * @param extraAttributes A string that will be inserted after the
	 * href attribute in the generated HTML a tags.  Null is not
	 * allowed, but the empty string is ok.
	 */
	public URLLinker(String extraAttributes) {
		if(extraAttributes == null) {
			throw new NullPointerException();
		}
		this.extraAttributes = extraAttributes;
	}

	/**
	 * Makes HTML href anchors out of words in the input string which
	 * begin with http://, https://, ftp://, mailto:, gopher://, or
	 * telnet:.  A word is defined as a maximal string of consecutive
	 * characters which are not whitespace.  Whitespace characters are
	 * space, tab, linefeed, carriage return, and form feed (the
	 * default delimiter set for java.util.StringTokenizer).
	 */
	public String filter(String in) {
		StringBuffer out = new StringBuffer(in.length());
		StringTokenizer words = new StringTokenizer(in, " \t\n\r\f", true);
		while(words.hasMoreTokens()) {
			String curWord = words.nextToken();
			if(curWord.startsWith("http://")
			   || curWord.startsWith("https://")
			   || curWord.startsWith("ftp://")
			   || curWord.startsWith("mailto:")
			   || curWord.startsWith("gopher://")
			   || curWord.startsWith("telnet:")) {
				out.append("<a href=\"")
					.append(curWord).append("\" ").append(extraAttributes).append(">")
					.append(curWord).append("</a>");
			} else {
				out.append(curWord);
			}
		}
		return out.toString();
	}
}
