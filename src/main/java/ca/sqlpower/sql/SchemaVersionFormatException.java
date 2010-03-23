package ca.sqlpower.sql;

public class SchemaVersionFormatException extends ca.sqlpower.util.VersionFormatException {
	protected Throwable cause;
	public SchemaVersionFormatException(String message, int position, Throwable cause) {
		super(message, position, cause);
	}
}
