package ca.sqlpower.security;

import ca.sqlpower.sql.DatabaseObject;

/**
 * The PLSecurityException class represents a specific "access denied"
 * scenario for a certain operation.  The type of operation and the
 * reason for denying access are given.  Both strings should be
 * specified as constants from the PLSecurityManager class.
 *
 * @author Jonathan Fuerth
 * @see PLSecurityManager
 */
public class PLSecurityException extends RuntimeException {

	/**
	 * The permission that was denied.
	 */
	protected String perm;

	/**
	 * The reason for denying the permission.
	 */
	protected String reason;
	
	/**
	 * The object that we were attempting to access/modify.
	 */
	protected DatabaseObject obj;

	/**
	 * Constructs a new PLSecurityException.
	 */
	public PLSecurityException(String perm, String reason, DatabaseObject obj) {
		super();
		this.perm = perm;
		this.reason = reason;
		this.obj = obj;
	}

	public String getPermission() {
		return perm;
	}

	public String getReason() {
		return reason;
	}

	public DatabaseObject getDatabaseObject() {
		return obj;
	}

	public String getMessage() {
		if (obj == null) {
			return "Denied "+perm+" because "+reason;
		} else {
			return "Denied "+perm+" on "+obj.getObjectName()+" because "+reason;			
		}
	}
}
