package ca.sqlpower.util;
import java.io.PrintStream;

/**
 * A generic logging facility.  In its current implementation, it does
 * barely anything.  In the future, it'll support lots of useful
 * stuff.
 *
 * @author Jonathan Fuerth
 * @version $Id$
 */
public class Logger {

    /**
     * All log output is sent to this stream.
     */
    protected PrintStream out;

    /**
     * This string should be prepended to all log output lines
     */
    protected String initialString;

    /**
     * Constructs a new logger which will direct all of its output to
     * one place.
     *
     * @param out The output stream which will recieve all log messages
     */
    public Logger(PrintStream out, String initialString) {
	this.initialString=initialString;
	this.out=out;
	if(initialString==null || out==null) {
	    throw new NullPointerException();
	}
    }

    /**
     * Constructs a new logger which will direct all of its output to
     * one place.
     *
     * @param out The output stream which will recieve all log messages
     */
    public Logger(PrintStream out) {
	this(out, "");
    }

    public void log(String message) {
	out.print("[1m");
	out.print(initialString);
	out.print(message);
	out.println("[0m");
    }
}
