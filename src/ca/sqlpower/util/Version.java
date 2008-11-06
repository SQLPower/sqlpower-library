package ca.sqlpower.util;

import java.io.Serializable;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

/**
 * Represents a dotted-triple version number (major.minor.tiny).
 * Supports converting to and from Strings, as well as comparing with
 * other Version objects.
 */
public class Version implements Comparable, Serializable, Cloneable {
	int major;
	int minor;
	int tiny;

	/**
	 * Creates a new Version object with all three components set to -1.
	 */
	public Version() {
		major = -1;
		minor = -1;
		tiny = -1;
	}

	/**
	 * Creates a new Version object with major, minor, tiny
	 * versions as specified.
	 */
	public Version(int major, int minor, int tiny) {
		this.major = major;
		this.minor = minor;
		this.tiny = tiny;
	}

	/**
	 * Creates a version object initialized to the given dotted-triple
	 * version string.
	 *
	 * @param version A String of the form major.minor.tiny where all
	 * three components are present and all three are positive integer
	 * values.
	 */
	public Version(String version) throws VersionFormatException {
		setVersion(version);
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

	public void setVersion(String versionString) throws VersionFormatException {
		StringTokenizer st = new StringTokenizer(versionString, ".");
		try {
			major = Integer.parseInt(st.nextToken());
			minor = Integer.parseInt(st.nextToken());
			tiny = Integer.parseInt(st.nextToken());
		} catch(NumberFormatException e) {
			throw new VersionFormatException("A component of the version string '"
											 +versionString+"' is not an integer",
											 0, e);
		} catch(NoSuchElementException e) {
			throw new VersionFormatException("The version string '"+versionString
											 +"' has fewer than three components",
											 0, e);
		}

		if(st.hasMoreTokens()) {
			throw new VersionFormatException("The version string '"+versionString
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
		Version otherVersion = (Version) other;
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

	/**
	 * Returns a copy of this version.
	 */
	public Object clone() {
		try {
			return (Version) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new IllegalStateException("Caught impossible CloneNotSupportedException");
		}
	}
}
