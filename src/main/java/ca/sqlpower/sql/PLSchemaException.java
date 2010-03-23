package ca.sqlpower.sql;


public class PLSchemaException extends Exception {

	String currentVersion;
	String requiredVersion;
	
	public PLSchemaException(String message) {
		super(message);
	}
	
	/**
	 * Creates a new PLSchemaException storing the required version and current version of the PL schema
	 * for better error message display as it bubbles up through the app.
	 * 
	 * @param currentVersion the version of the currently installed PL schema
	 * @param requiredVersion the minimum version of the PL Schema required to run this app.
	 */
	public PLSchemaException(String message, String currentVersion, String requiredVersion) {
		super(message);
		this.currentVersion = currentVersion;
		this.requiredVersion = requiredVersion;
	}
	
	/**
	 * Returns the currentVersion.
	 * @return String
	 */
	public String getCurrentVersion() {
		return currentVersion;
	}

	/**
	 * Returns the requiredVersion.
	 * @return String
	 */
	public String getRequiredVersion() {
		return requiredVersion;
	}

}
