/*
 * Copyright (c) 2008, SQL Power Group Inc.
 *
 * This file is part of SQL Power Library.
 *
 * SQL Power Library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * SQL Power Library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>. 
 */
package ca.sqlpower.xml;

import java.io.PrintWriter;

/**
 * XMLHelper is a simple utility for outputting indented XML markup. It escapes
 * all illegal XML characters using a custom (application-level) escaping
 * mechanism. The rationale behind this is to allow us to save such characters
 * (like from binary data in a project) into a project file and read them back
 * in again when loading a project.
 * <p>
 * Note that <i>all</i> data that passes through the methods in this class will
 * be escaped.  There is no check as to whether or not the illegal characters fall
 * in a CDATA section, a tag name, an attribute name or value, and so on.  We
 * consider this to be a reasonable simplification of the problem because illegal
 * XML characters are just that: Illegal.  If a tag name or attribute value contained
 * an illegal character, the resulting file would not be well-formed XML.  The fact
 * that this class escapes everything that goes through it won't break what would
 * otherwise be well-formed XML. It will only further mangle malformed XML!  Of
 * course, if the illegal characters fall in a CDATA section or attribute value,
 * the escaping will have saved the day. The results will be well-formed XML.
 * <p>
 * NB: When loading back in the data that was written using the XMLHelper, the
 * escaped characters will have to be 'unescaped' again after going through the
 * XML parser. See {@link #escape(String)} for a description of the escape
 * format.
 * <p>
 */
public class XMLHelper {

    public int indent;

    /**
     * Creates a new XMLHelper with an initial indentation amount of 0.
     */
    public XMLHelper() {
        super();
    }

    /**
     * Prints to the output writer {@link #out} indentation spaces
     * (according to {@link #indent}) followed by the given text.
     * @param out
     */
    public void print(PrintWriter out, String text) {
        for (int i = 0; i < indent; i++) {
            out.print(" ");
        }
        out.print(escape(text));
    }

    /**
     * Prints <code>text</code> to the output writer {@link #out} (no
     * indentation).
     */
    public void niprint(PrintWriter out, String text) {
        out.print(escape(text));
    }

    /**
     * Prints <code>text</code> followed by newline to the output
     * writer {@link #out} (no indentation).
     */
    public void niprintln(PrintWriter out, String text) {
        out.println(escape(text));
    }

    /**
     * Prints to the output writer {@link #out} indentation spaces
     * (according to {@link #indent}) followed by the given text
     * followed by a newline.
     */
    public void println(PrintWriter out, String text) {
        for (int i = 0; i < indent; i++) {
            out.print(" ");
        }
        out.println(escape(text));
    }
    
    /**
     * Takes a String argument and returns a string that escapes characters that
     * are illegal in an XML document according to the XML specification. The
     * set of valid XML characters is taken from the <a
     * href="http://www.w3.org/TR/REC-xml/">XML 1.0 specification</a>, section
     * 2.2. Additionally, the backslash character will be considered illegal if
     * it appears immediately before a lowercase u in the input string.
     * <p>
     * Illegal characters will be represented in the output in the "escaped
     * form," the string <tt>\\uNNNN</tt> where NNNN is the four-digit
     * hexadecimal value of the character. There will always be exactly four
     * characters following the \\u, and each of those four characters will be a
     * hex digit.
     * <p>
     * This escaping mechanism is not standard XML markup; it's
     * application-level data. No generic XML processor will unescape it on the
     * way in, so the job of unescaping lies with any application program that
     * wants to consume the XML data. The Architect handles this by wrapping a
     * SAX parser with a layer that detects and unescapes the \\u sequences.
     * <p>
     * 
     * @param text
     *            The input string that we want to check for illegal characters
     * @return Returns a string identical to the input string, except any
     *         character values that fall outside the range of legal XML
     *         characters will appear in the 6-character escaped form described
     *         above.
     */
    static String escape(String text) {
        if (text.equals("")) return "";
        
        // arbitrary amount of extra space
        StringBuilder sb = new StringBuilder(text.length()+10);
        
        for (int i = 0, n = text.length(); i < n; i++) {
            char ch = text.charAt(i);
            char nextch;
            if (i == n - 1) {
                nextch = 0;
            } else {
                nextch = text.charAt(i + 1);
            }
            
            if (ch == 0x09 || ch == 0x0a || ch == 0x0d ||
                    (ch >= 0x20 && ch <= 0xd7ff && ch != '\\') ||
                    (ch >= 0xe000 && ch <=0xfffd) ||
                    (ch == '\\' && nextch != 'u')) {
                sb.append(ch);
            } else {
                sb.append(String.format("\\u%04x", (int)ch));
            }
        }
        return sb.toString();
    }
    
    /**
     * Unescapes the String text according to the format described above in escape(String text)
     * 
     * @param text The String to escape. If the String is null, then we return null.
     * @return The unescaped version of the input string. If the string is null, return null
     */
    static String unescape(String text) {
        if (text == null) return null;
        
        StringBuilder unescapedText = new StringBuilder(text.length());
        
        for (int i = 0, n = text.length(); i < n ; ) {
            char ch = text.charAt(i);
            char nextch;
            if (i == n - 1) {
                nextch = 0;
            } else {
                nextch = text.charAt(i + 1);
            }
            
            if (ch == '\\' && nextch == 'u') {
                int charVal = Integer.parseInt(text.substring(i+2, i+6), 16);
                char unescapedChar = (char)charVal;
                unescapedText.append(unescapedChar);
                i += 6;
            } else {
                unescapedText.append(ch);
                i++;
            }
        }
        
        return unescapedText.toString();
    }
}
