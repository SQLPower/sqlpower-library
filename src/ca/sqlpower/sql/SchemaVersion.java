package ca.sqlpower.sql;

import java.sql.*;
import ca.sqlpower.util.VersionFormatException;

public class SchemaVersion extends ca.sqlpower.util.Version {

	/**
	 * Creates a new SchemaVersion object with all three components set to -1.
	 */
	public SchemaVersion() {
		super();
	}

	/**
	 * Creates a new SchemaVersion object with major, minor, tiny
	 * versions as specified.
	 */
	public SchemaVersion(int major, int minor, int tiny) {
		super(major, minor, tiny);
	}

	public static SchemaVersion makeFromDatabase(Connection con)
		throws SchemaVersionFormatException, SQLException {
		SchemaVersion version = null;
		Statement stmt = null;
		try {
			stmt = con.createStatement();
			ResultSet rs = stmt.executeQuery("SELECT schema_version FROM def_param");
			if(rs.next()) {
				String versionString = rs.getString(1);
				version = new SchemaVersion();
				version.setVersion(versionString);
			}
		} catch (VersionFormatException e) {
			throw new SchemaVersionFormatException(e.getMessage(), 0, e.getCause());
		} finally {
			if(stmt != null) {
				stmt.close();
			}
		}
		return version;
	}
}
