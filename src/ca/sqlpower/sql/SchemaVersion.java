package ca.sqlpower.sql;

import java.sql.*;
import java.util.*;

public class SchemaVersion implements Comparable {
	int major;
	int minor;
	int tiny;

	/**
	 * Creates a new SchemaVersion object with all three components set to -1.
	 */
	public SchemaVersion() {
		major = -1;
		minor = -1;
		tiny = -1;
	}

	/**
	 * Creates a new SchemaVersion object with major, minor, tiny
	 * versions as specified.
	 */
	public SchemaVersion(int major, int minor, int tiny) {
		this.major = major;
		this.minor = minor;
		this.tiny = tiny;
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
		} finally {
			if(stmt != null) {
				stmt.close();
			}
		}
		return version;
	}
	
	/**
	 * Gets the value of major
	 *
	 * @return the value of major
	 */
	public int getMajor() {
		return this.major;
	}

	/**
	 * Sets the value of major
	 *
	 * @param argMajor Value to assign to this.major
	 */
	public void setMajor(int argMajor){
		this.major = argMajor;
	}

	/**
	 * Gets the value of minor
	 *
	 * @return the value of minor
	 */
	public int getMinor() {
		return this.minor;
	}

	/**
	 * Sets the value of minor
	 *
	 * @param argMinor Value to assign to this.minor
	 */
	public void setMinor(int argMinor){
		this.minor = argMinor;
	}

	/**
	 * Gets the value of tiny
	 *
	 * @return the value of tiny
	 */
	public int getTiny() {
		return this.tiny;
	}

	/**
	 * Sets the value of tiny
	 *
	 * @param argTiny Value to assign to this.tiny
	 */
	public void setTiny(int argTiny){
		this.tiny = argTiny;
	}

	public void setVersion(String versionString) throws SchemaVersionFormatException {
		StringTokenizer st = new StringTokenizer(versionString, ".");
		try {
			major = Integer.parseInt(st.nextToken());
			minor = Integer.parseInt(st.nextToken());
			tiny = Integer.parseInt(st.nextToken());
		} catch(NumberFormatException e) {
			throw new SchemaVersionFormatException("A component of the version string '"
												   +versionString+"' is not an integer",
												   0, e);
		} catch(NoSuchElementException e) {
			throw new SchemaVersionFormatException("The version string '"+versionString
												   +"' has fewer than three components",
												   0, e);
		}

		if(st.hasMoreTokens()) {
			throw new SchemaVersionFormatException("The version string '"+versionString
												   +"' has too many components",
												   0, null);
		}
	}

	public String getVersion() {
		return new StringBuffer()
			.append(major)
			.append(".")
			.append(minor)
			.append(".")
			.append(tiny).toString();
	}

	public String toString() {
		return getVersion();
	}

	public int compareTo(Object other) {
		SchemaVersion otherVersion = (SchemaVersion) other;
		if(this.major < otherVersion.major) {
			return -1;
		}
		if(this.major > otherVersion.major) {
			return 1;
		}
		if(this.minor < otherVersion.minor) {
			return -1;
		}
		if(this.minor > otherVersion.minor) {
			return 1;
		}
		if(this.tiny < otherVersion.tiny) {
			return -1;
		}
		if(this.tiny > otherVersion.tiny) {
			return 1;
		}
		return 0;
	}
}
