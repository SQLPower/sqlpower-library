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

package ca.sqlpower.sqlobject;

import junit.framework.Test;
import junit.framework.TestSuite;
import ca.sqlpower.sqlobject.undo.ArchitectPropertyChangeUndoableEditTest;
import ca.sqlpower.sqlobject.undo.SQLObjectChildrenTest;
import ca.sqlpower.sqlobject.undo.TestSQLObjectChildrenInsert;
import ca.sqlpower.sqlobject.undo.TestUndoManager;

public class AllTests {

    public static Test suite() {
        TestSuite suite = new TestSuite("Core SQLObject tests");
        //$JUnit-BEGIN$
        suite.addTestSuite(RefreshTablesTest.class);
        suite.addTestSuite(RefreshFKTest.class);
        suite.addTestSuite(RefreshTest.class);
        suite.addTestSuite(SQLObjectExceptionTest.class);
        suite.addTestSuite(SQLObjectMagicTest.class);
        suite.addTestSuite(SQLObjectTest.class);
        suite.addTestSuite(SQLTableLazyLoadTest.class);
        suite.addTestSuite(TestSQLCatalog.class);
        suite.addTestSuite(TestSQLColumn.class);
        suite.addTestSuite(TestSQLDatabase.class);
        suite.addTestSuite(TestSQLIndex.class);
        suite.addTestSuite(TestSQLIndexColumn.class);
        suite.addTestSuite(TestSQLRelationship.class);
        suite.addTestSuite(TestSQLSchema.class);
        suite.addTestSuite(TestSQLTable.class);
        suite.addTestSuite(ArchitectPropertyChangeUndoableEditTest.class);
        suite.addTestSuite(SQLObjectChildrenTest.class);
        suite.addTestSuite(TestSQLObjectChildrenInsert.class);
        suite.addTestSuite(TestUndoManager.class);
        //$JUnit-END$
        return suite;
    }
}
