/*
 * Copyright (c) 2008, SQL Power Group Inc.
 * 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in
 *       the documentation and/or other materials provided with the
 *       distribution.
 *     * Neither the name of SQL Power Group Inc. nor the names of its
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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
