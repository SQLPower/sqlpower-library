package ca.sqlpower.security;

/**
 * This exception is meant to be thrown whenever an application
 * detects a violation of a valid license.  It is not intended to be
 * thrown when an invalid (corrupt, tampered-with) license is
 * detected.  See {@link LicenseReadException}.
 *
 * @author Jonathan Fuerth
 * @version $Id$
 */
public class LicenseException extends Exception {
	Throwable cause;
	String message;

	public LicenseException(String message, Throwable cause) {
		super();
		this.cause = cause;
		this.message = message;
	}

	public LicenseException(String message) {
		super();
		this.cause = null;
		this.message = message;
	}

	public Throwable getCause() { return cause; }
	public String getMessage()  { return message; }
}
