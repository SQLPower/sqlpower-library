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

import org.xml.sax.SAXException;

import junit.framework.TestCase;

public class UnescapingDefaultHandlerTest extends TestCase {

    /**
     * The DefaultHandler implementation that we're testing here
     */
    private UnescapingDefaultHandler defaultHandler;
    
    /**
     * A stub implementaion of DefaultHandler that we're wrapping with the UnescapingDefaultHandler
     */
    private StubDefaultHandler stubHandler;
    
    protected void setUp() throws Exception {
        super.setUp();
        stubHandler = new StubDefaultHandler();
        defaultHandler = new UnescapingDefaultHandler(stubHandler);
    }

    public void testCharacters() throws SAXException {
        String testString = "abc\\u0000123";
        
        defaultHandler.characters(testString.toCharArray(), 2, 10);
        
        // Note, these test for correct behaviour given the current
        // implementation.  If we modify or improve the implementation,
        // this test may fail even though the implementation is correct.
        assertEquals("c\u0000123", stubHandler.string);
        assertEquals(0, stubHandler.start);
        assertEquals(5, stubHandler.length);
    }

    public void testStartElementStringStringStringAttributes() throws SAXException {
        StubAttributes attr = new StubAttributes();
        defaultHandler.startElement("", "", "", attr);
        
        assertEquals(UnescapingAttributes.class, stubHandler.attr.getClass());
    }

}
