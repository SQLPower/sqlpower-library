package ca.sqlpower.sql;

public class SchemaVersionFormatException extends java.text.ParseException {
	protected Throwable cause;
	public SchemaVersionFormatException(String message, int position, Throwable cause) {
		super(message, position);
		this.cause = cause;
	}
	
	/**
	 * Gets the cause of this exception
	 *
	 * @return the cause, or null if this exception was not
	 * triggered by a different exception.
	 */
	public Throwable getCause() {
		return this.cause;
	}
	
	/**
	 * Sets the exception that caused this exception.
	 *
	 * @param argCause he cause, or null if this exception was not
	 * triggered by a different exception.
	 */
	public void setCause(Throwable argCause){
		this.cause = argCause;
	}
}
