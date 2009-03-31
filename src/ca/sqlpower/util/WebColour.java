package ca.sqlpower.util;


/**
 * The WebColour class is a small wrapper around the Java AWT Color
 * class.  Its main enhancement is the existence of a constructor that
 * takes HTML "#[RGB Triple]" argument, and a corresponding toString()
 * method that outputs the colour in the same format.  The coolest
 * part is that you can use all the awt colour manipulation methods
 * (doing things like <code>out.println("&lt;tr
 * bgcolor=\"+myWebColour.brighter()+\"&gt;")</code>)
 *
 * @author Jonathan Fuerth
 * @version $Id$
 */
public class WebColour extends java.awt.Color {

    // XXX: Should parse and format web colour strings like this:
    //static private NumberFormat htmlRGBFormat=new WebColourFormat();

    public WebColour(int rgb) {
	super(rgb);
    }

    public WebColour(int r, int g, int b) {
	super(r, g, b);
    }

    /**
     * Makes a new WebColour object representing the colour specified
     * by a regular HTML RGB triple.
     *
     * @param htmlRGBString The regular HTML representation of an RGB
     * triple.  A leading '#' mark is optional.  This string will
     * therefore be 6 or 7 characters in length.  Shorter and longer
     * values are accepted, but are probably not going to Do What You
     * Want.
     * @throws NumberFormatException when the parse of the string fails.
     */
    public WebColour(String htmlRGBString) throws NumberFormatException {
	super(parseWebColour(htmlRGBString));
    }

    public String toString() {
	return "#"+Integer.toHexString(getRGB() & 0xffffff);
    }

    /**
     * Takes a String representing a 24-bit RGB value in hexadecimal
     * notation, with an optional leading '#' mark.
     *
     * @param colour The regular HTML representation of an RGB triple.
     * A leading '#' mark is optional.  This string will therefore be
     * 6 or 7 characters in length.  Shorter and longer values are
     * accepted, but are probably not going to Do What You Want.
     * @return The integer representation of the <code>colour</code>
     * argument.
     * @throws NumberFormatException when the parse of the string fails.
     */
    protected static
	int parseWebColour(String colour) throws NumberFormatException {
	int offset=0;
	
	if(colour.charAt(0)=='#') {
	    colour=colour.substring(1);
	}

	return Integer.parseInt(colour, 16);
    }
}
