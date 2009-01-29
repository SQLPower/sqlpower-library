/*
 * Copyright (c) 2008, SQL Power Group Inc.
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
package ca.sqlpower.xml;

import org.xml.sax.Attributes;

/**
 * A stub implementation of the Attributes class for unit testing purposes.
 *
 */
public class StubAttributes implements Attributes {
    
    public int getIndex(String qName) {
        return 0;
    }

    public int getIndex(String uri, String localName) {
        return 0;
    }

    public int getLength() {
        return 0;
    }

    public String getLocalName(int index) {
        return null;
    }

    public String getQName(int index) {
        return null;
    }

    public String getType(int index) {
        return null;
    }

    public String getType(String qName) {
        return null;
    }

    public String getType(String uri, String localName) {
        return null;
    }

    public String getURI(int index) {
        return null;
    }

    /**
     * Always returns a null character, regardless of the argument value.
     */
    public String getValue(int index) {
        return "abc\\u0000123";
    }

    /**
     * Always returns a null character, regardless of the argument value.
     */
    public String getValue(String qName) {
        return "abc\\u0000123";
    }

    /**
     * Always returns a null character, regardless of the argument value.
     */
    public String getValue(String uri, String localName) {
        return "abc\\u0000123";
    }

}
