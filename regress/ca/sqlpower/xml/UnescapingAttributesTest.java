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

import junit.framework.TestCase;

public class UnescapingAttributesTest extends TestCase {

    /**
     * The UnescapingAttributes implementation we're testing here. 
     */
    private UnescapingAttributes unescapingAttr;
    
    /**
     * A stub implementation of Attributes that UnescapingAttributes
     * will wrap.
     */
    private StubAttributes stubAttr;

    protected void setUp() throws Exception {
        super.setUp();
        stubAttr = new StubAttributes();
        unescapingAttr = new UnescapingAttributes(stubAttr);
    }

    public void testGetValueInt() {        
        assertEquals(XMLHelper.unescape(stubAttr.getValue(0)), unescapingAttr.getValue(0));
    }

    public void testGetValueStringString() {
        assertEquals(XMLHelper.unescape(stubAttr.getValue("","")), unescapingAttr.getValue("",""));
    }

    public void testGetValueString() {
        assertEquals(XMLHelper.unescape(stubAttr.getValue("")), unescapingAttr.getValue(""));
    }

}
