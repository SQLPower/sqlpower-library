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

package ca.sqlpower.sqlobject;

import ca.sqlpower.sqlobject.undo.ArchitectPropertyChangeUndoableEditTest;
import ca.sqlpower.sqlobject.undo.SQLObjectChildrenTest;
import ca.sqlpower.sqlobject.undo.TestSQLObjectChildrenInsert;
import ca.sqlpower.sqlobject.undo.TestUndoManager;
import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {

    public static Test suite() {
        TestSuite suite = new TestSuite("Test for ca.sqlpower.graph");
        //$JUnit-BEGIN$
        suite.addTestSuite(SQLObjectExceptionTest.class);
        suite.addTestSuite(SQLObjectMagicTest.class);
        suite.addTestSuite(SQLObjectTest.class);
        suite.addTest(TestFolder.suite());
        suite.addTestSuite(TestSQLCatalog.class);
        suite.addTest(TestSQLColumn.suite());
        suite.addTest(TestSQLDatabase.suite());
        suite.addTestSuite(TestSQLIndex.class);
        suite.addTestSuite(TestSQLIndexColumn.class);
        suite.addTestSuite(TestSQLRelationship.class);
        suite.addTestSuite(TestSQLSchema.class);
        suite.addTest(TestSQLTable.suite());
        suite.addTestSuite(ArchitectPropertyChangeUndoableEditTest.class);
        suite.addTestSuite(SQLObjectChildrenTest.class);
        suite.addTestSuite(TestSQLObjectChildrenInsert.class);
        suite.addTestSuite(TestUndoManager.class);
        //$JUnit-END$
        return suite;
    }
}
