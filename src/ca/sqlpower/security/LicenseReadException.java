package ca.sqlpower.security;

/**
 * This exception wraps all problems that could occur while loading
 * a SQLPower product license file.
 *
 * @author Jonathan Fuerth
 * @version $Id$
 */
public class LicenseReadException extends Exception {

	Throwable cause;
	String message;

	public LicenseReadException(String message, Throwable cause) {
		this.cause = cause;
		this.message = message;
	}

	public Throwable getCause() { return cause; }
	public String getMessage()  { return message; }
}
