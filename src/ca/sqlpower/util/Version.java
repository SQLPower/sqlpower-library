package ca.sqlpower.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a dotted-triple version number (major.minor.tiny).
 * Supports converting to and from Strings, as well as comparing with
 * other Version objects.
 */
public class Version implements Comparable<Version>, Serializable, Cloneable {
	 /**
     * The components of this version, from most major to least major. Parts
     * will either be Integer values or String values. If there is a String
     * part, it will be the last part, and is referred to as the "Suffix."
     */
    private Object[] parts;
	
	/**
	 * Creates a new Version object with all three components set to -1.
	 */
	public Version() {
		parts = new Object[3];
		parts[0] = -1;
		parts[1] = -1;
		parts[2] = -1;
	}

	/**
	 * Creates a new Version object with major, minor, tiny
	 * versions as specified.
	 */
	public Version(int major, int minor, int tiny) {
		parts = new Object[3];
		parts[0] = major;
		parts[1] = minor;
		parts[2] = tiny;
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
		if (parts != null && parts[0] instanceof Integer) {
			return (Integer) parts[0];
		} else {
			return -1;
		}
	}

	/**
	 * Sets the value of major
	 *
	 * @param argMajor Value to assign to this.major
	 */
	public void setMajor(int argMajor) {
		if (parts == null) {
			replacePartsArray();
		}
		parts[0] = Integer.valueOf(argMajor);
	}

	/**
	 * Gets the value of minor
	 *
	 * @return the value of minor
	 */
	public int getMinor() {
		if (parts != null && parts[1] instanceof Integer) {
			return (Integer) parts[1];
		} else {
			return -1;
		}
	}

	/**
	 * Sets the value of minor
	 *
	 * @param argMinor Value to assign to this.minor
	 */
	public void setMinor(int argMinor){
		if (parts == null || parts.length < 2) {
			replacePartsArray();
		}
		parts[1] = Integer.valueOf(argMinor);
	}

	/**
	 * Gets the value of tiny
	 *
	 * @return the value of tiny
	 */
	public int getTiny() {
		if (parts != null && parts[2] instanceof Integer) {
			return (Integer) parts[2];
		} else {
			return -1;
		}
	}

	/**
	 * Sets the value of tiny
	 *
	 * @param argTiny Value to assign to this.tiny
	 */
	public void setTiny(int argTiny){
		if (parts == null || parts.length < 3) {
			replacePartsArray();
		}
		parts[2] = Integer.valueOf(argTiny);
	}

	public void setVersion(String versionString) throws VersionFormatException {
		String[] rawParts = versionString.split("\\.");
        List<Object> parsedParts = new ArrayList<Object>();
        Pattern p = Pattern.compile("[0-9]+");
        for (int i = 0; i < rawParts.length; i++) {
            Matcher m = p.matcher(rawParts[i]);
            if (m.matches()) {
                parsedParts.add(Integer.parseInt(rawParts[i]));
            } else if (i == rawParts.length - 1) {
                Pattern suffixPattern = Pattern.compile("([0-9]+)(.+)");
                Matcher suffixMatcher = suffixPattern.matcher((String) rawParts[i]);
                if (suffixMatcher.matches()) {
                    parsedParts.add(Integer.parseInt(suffixMatcher.group(1)));
                    parsedParts.add(suffixMatcher.group(2));
                } else {
                    throw new VersionFormatException(versionString, 0, null);
                }
            } else {
                throw new VersionFormatException(versionString, 0, null);
            }
        }
        parts = parsedParts.toArray();
	}

	public String toString() {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Object part : parts) {
            if (!first && part instanceof Integer) {
                sb.append(".");
            }
            sb.append(part);
            first = false;
        }
        return sb.toString();
	}

	public int compareTo(Version other) {
        int i;
        for (i = 0; i < parts.length && i < other.parts.length; i++) {
            if (parts[i] instanceof Integer && other.parts[i] instanceof Integer) {
                int v = (Integer) parts[i];
                int ov = (Integer) other.parts[i];
                if (v > ov) return 1;
                if (v < ov) return -1;
            } else if (parts[i] instanceof String && other.parts[i] instanceof String) {
                String v = (String) parts[i];
                String ov = (String) other.parts[i];
                int diff = v.compareTo(ov);
                if (diff != 0) return diff;
            } else if (parts[i] instanceof Integer && other.parts[i] instanceof String) {
                return 1;
            } else if (parts[i] instanceof String && other.parts[i] instanceof Integer) {
                return -1;
            } else {
                throw new IllegalStateException("Found a version part that's not a String or Integer");
            }
        }
        
        // check for special case where comparing 1.0a to 1.0 (1.0 should be newer)
        if (parts.length == other.parts.length + 1 && parts[parts.length-1] instanceof String) return -1;
        if (other.parts.length == parts.length + 1 && other.parts[other.parts.length-1] instanceof String) return 1;
        
        // otherwise if one version has more integer parts, it's newer.
        if (parts.length > other.parts.length) return 1;
        if (parts.length < other.parts.length) return -1;
        
        // they're actually the same
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
	
	/**
	 * A convenience method for replacing the parts array with one of Object[3]
	 * and copying the existing parts array into the new one. This is mainly for
	 * if the parts array is too short to contain the major, minor, and tiny
	 * parts.
	 */
	private void replacePartsArray() {
		Object[] dest = new Object[3];
		System.arraycopy(parts, 0, dest, 0, parts.length);
		parts = dest;
	}

}
