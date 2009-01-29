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
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class StubDefaultHandler extends DefaultHandler {
    
    /**
     * The Attributes that are passed into the stub implementation of the
     * startElement method. For example, the tests will check the class
     * of the attribute in UnescapingDefaultHandlerTest to ensure that a
     * UnescapingAttributes is being passed into the startElements method.
     * Access is package-private so that the tests can access it directly.
     */
    Attributes attr;
    
    /**
     * The arguments that get passed into the stub implementation of the
     * characters method. For example, we will check the values of these in
     * UnescapingDefaultHandlerTest to make sure the right values are being passed
     * in UnescapingDefaultHandler's implementation of the characters method.
     * Access is package-private so that the tests can access it directly.
     */
    String string;
    
    /**
     * The most recent start arg value passed to characters().
     * Access is package-private so that the tests can access it directly.
     */
    int start;
    
    /**
     * The most recent length arg value passed to characters().
     * Access is package-private so that the tests can access it directly.
     */
    int length;
  
    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        this.string = new String(ch);
        this.start = start;
        this.length = length;
    }
    
    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        attr = attributes;
    }
}
