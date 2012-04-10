package ca.sqlpower.dao;

/**
 * A veto exception that indicates one of the {@link VetoableSPListener}
 * listeners vetoed the change.
 */
public class SPObjectVetoException extends Exception {

	private static final long serialVersionUID = 1L;
	
	public SPObjectVetoException() {
		super();
	}

	public SPObjectVetoException(String message, Throwable cause) {
		super(message, cause);
	}

	public SPObjectVetoException(String message) {
		super(message);
	}

	public SPObjectVetoException(Throwable cause) {
		super(cause);
	}

}
