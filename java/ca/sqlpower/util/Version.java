/*
 * Created on Aug 23, 2007
 * 
 * This code originally came from grodbots, but has been donated to
 * SQL Power by Jonathan Fuerth.
 * 
 * Copyright (c) 2009, SQL Power Group Inc.
 *
 * This file is part of SQL Power Library.
 *
 * SQL Power Library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * SQL Power Library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>. 
 */

package ca.sqlpower.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;

/**
 * The Version class represents a dotted version number with an arbitrary number
 * of numeric parts and an optional alphanumeric suffix.
 * 
 * @author fuerth
 * @version $Id$
 */
public class Version implements Comparable<Version> {

    /**
     * The components of this version, from most major to least major. Parts
     * will either be Integer values or String values. If there is a String
     * part, it will be the last part, and is referred to as the "Suffix."
     */
    private Object[] parts;
    
    /**
     * Creates a new Version object from the given string. The format is
     * <tt>a1.a2.(...).aN[suffix]</tt>.  Examples: <tt>1.2.3alpha</tt>
     * or <tt>1.3</tt> or <tt>2</tt>.  The version number must have at
     * least one numeric component, so <tt>1suffix</tt> is legal but
     * <tt>suffix</tt> on its own is not.
     * 
     * @param v The version string, cannot be null.
     */
    public Version(@Nonnull String v) {
        String[] rawParts = v.split("\\.");
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
                    throw new VersionParseException("Bad version format \""+v+"\"");
                }
            } else {
                throw new VersionParseException("Bad version format \""+v+"\"");
            }
        }
        parts = parsedParts.toArray();
    }

    /**
     * Creates a copy of the given version that can have some of the minor
     * version numbers stripped off of it.
     * 
     * @param copyMe
     *            The version object to copy.
     * @param numPartsToCopy
     *            The number of version parts to copy. For example, this can be
     *            2 to take only the two most important version numbers or 1
     *            less than copyMe's version number to strip off the minor
     *            version or suffix. This must be equal to or less than the
     *            given version's number of parts or an
     *            {@link IndexOutOfBoundsException} will be thrown.
     */
    public Version(Version copyMe, int numPartsToCopy) {
        Object[] newVersionParts = new Object[numPartsToCopy];
        Object[] oldVersions = copyMe.getParts();
        for (int i = 0; i < numPartsToCopy; i++) {
            newVersionParts[i] = oldVersions[i];
        }
        parts = newVersionParts;
    }
    
    /**
     * Returns the String representation of this version number in the same format
     * accepted by the {@link #Version(String)} constructor.
     */
    @Override
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

    /**
     * Returns a copy of the parts that make up this version value.
     * 
     * @see #parts
     */
    public Object[] getParts() {
        Object[] partsCopy = new Object[parts.length];
        for (int i = 0; i < parts.length; i++) {
            partsCopy[i] = parts[i];
        }
        return partsCopy;
    }

    /**
     * Version numbers are mutually comparable even if they have different
     * numbers of parts, and in that case, version <tt>2.0</tt> is older
     * than <tt>2.0.0</tt> or <tt>2.0.1</tt> but still newer than
     * <tt>1.0.0</tt>.
     * <p>
     * If two versions differ only as far as one having a suffix and the other
     * not having a suffix, the one without the suffix is considered newer. This
     * allows the natural idea that the following are in chronological order:
     * <ul>
     *  <li>1.0alpha
     *  <li>1.0beta
     *  <li>1.0rc1
     *  <li>1.0rc2
     *  <li>1.0
     *  <li>1.1alpha
     *  <li>1.1
     * </ul>
     */
    public int compareTo(Version o) {
        int i;
        for (i = 0; i < parts.length && i < o.parts.length; i++) {
            if (parts[i] instanceof Integer && o.parts[i] instanceof Integer) {
                int v = (Integer) parts[i];
                int ov = (Integer) o.parts[i];
                if (v > ov) return 1;
                if (v < ov) return -1;
            } else if (parts[i] instanceof String && o.parts[i] instanceof String) {
                String v = (String) parts[i];
                String ov = (String) o.parts[i];
                int diff = v.compareTo(ov);
                if (diff != 0) return diff;
            } else if (parts[i] instanceof Integer && o.parts[i] instanceof String) {
                return 1;
            } else if (parts[i] instanceof String && o.parts[i] instanceof Integer) {
                return -1;
            } else {
                throw new IllegalStateException("Found a version part that's not a String or Integer");
            }
        }
        
        // check for special case where comparing 1.0a to 1.0 (1.0 should be newer)
        if (parts.length == o.parts.length + 1 && parts[parts.length-1] instanceof String) return -1;
        if (o.parts.length == parts.length + 1 && o.parts[o.parts.length-1] instanceof String) return 1;
        
        // otherwise if one version has more integer parts, it's newer.
        if (parts.length > o.parts.length) return 1;
        if (parts.length < o.parts.length) return -1;
        
        // they're actually the same
        return 0;
    }
    
    /**
     * Returns true if and only if obj is an instance of Version and
     * compareTo(obj) would return 0.
     */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Version)) {
            return false;
        }
        return compareTo((Version) obj) == 0;
    }
    
    /**
     * Returns a hash code that depends on every part of this version.
     */
    @Override
    public int hashCode() {
        final int PRIME = 31;
        int hashCode = 0;
        for (Object part : parts) {
            hashCode += PRIME * part.hashCode();
        }
        return hashCode;
    }
}
