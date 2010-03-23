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

package ca.sqlpower.sql.jdbcwrapper;

import junit.framework.TestCase;

public class SQLServer2005DatabaseMetaDataDecoratorTest extends TestCase {

    public void testStripParensOneSet() throws Exception {
        String original = "('cows moo')";
        assertEquals("'cows moo'", SQLServerDatabaseMetaDataDecorator.stripParens(original));
    }
    
    public void testStripParensTwoSets() throws Exception {
        String original = "((42.42))";
        assertEquals("42.42", SQLServerDatabaseMetaDataDecorator.stripParens(original));
    }

    public void testStripParensUnbalanced() throws Exception {
        String original = "(42.42))";
        assertEquals("42.42)", SQLServerDatabaseMetaDataDecorator.stripParens(original));
    }

    public void testStripParensFunctionCall() throws Exception {
        String original = "(myfunc(42.42))";
        assertEquals("myfunc(42.42)", SQLServerDatabaseMetaDataDecorator.stripParens(original));
    }

    public void testStripParensEmptyFunctionCall() throws Exception {
        String original = "(myfunc())";
        assertEquals("myfunc()", SQLServerDatabaseMetaDataDecorator.stripParens(original));
    }

    public void testStripParensEmptyString() throws Exception {
        String original = "";
        assertEquals("", SQLServerDatabaseMetaDataDecorator.stripParens(original));
    }

    public void testStripParensNoOp() throws Exception {
        String original = "42.42";
        assertEquals("42.42", SQLServerDatabaseMetaDataDecorator.stripParens(original));
    }

    public void testStripParensNull() throws Exception {
        String original = null;
        assertNull(SQLServerDatabaseMetaDataDecorator.stripParens(original));
    }

}
