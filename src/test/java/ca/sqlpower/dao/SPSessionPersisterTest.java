/*
 * Copyright (c) 2010, SQL Power Group Inc.
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

package ca.sqlpower.dao;

import ca.sqlpower.dao.session.SessionPersisterSuperConverter;
import ca.sqlpower.object.SPObject;
import ca.sqlpower.sql.PlDotIni;
import ca.sqlpower.sqlobject.SQLDatabase;
import ca.sqlpower.sqlobject.SQLRelationship;
import ca.sqlpower.sqlobject.SQLTable;
import ca.sqlpower.sqlobject.SQLRelationship.SQLImportedKey;
import ca.sqlpower.util.WorkspaceContainer;
import junit.framework.TestCase;

public class SPSessionPersisterTest extends TestCase {

    /**
     * Tests that calling remove on an object whose parent has already been
     * removed in the same transaction is successfully removed.
     */
    public void testRemoveOfRemovedObjects() throws Exception {
        final SQLDatabase testDatabase = new SQLDatabase();
        SQLTable table1 = new SQLTable(testDatabase, true);
        testDatabase.addTable(table1);
        SQLTable table2 = new SQLTable(testDatabase, true);
        testDatabase.addTable(table2);
        SQLRelationship relationship = new SQLRelationship();
        relationship.attachRelationship(table1, table2, true);
        
        assertEquals(1, table1.getExportedKeys().size());
        assertEquals(relationship, table1.getExportedKeys().get(0));
        assertEquals(1, table2.getImportedKeys().size());
        assertEquals(relationship.getForeignKey(), table2.getImportedKeys().get(0));
        
        SPSessionPersister sessionPersister = new SPSessionPersister(
                "Testing persister", testDatabase, new SessionPersisterSuperConverter(
                        new PlDotIni(), testDatabase)) {
        
            @Override
            protected void refreshRootNode(PersistedSPObject pso) {
                //do nothing
            }
        };
        sessionPersister.setWorkspaceContainer(new WorkspaceContainer() {
            public SPObject getWorkspace() {
                return testDatabase;
            }
        });
        
        sessionPersister.begin();
        sessionPersister.removeObject(testDatabase.getUUID(), table1.getUUID());
        sessionPersister.removeObject(table1.getUUID(), relationship.getUUID());
        sessionPersister.commit();
        
        assertEquals(1, testDatabase.getChildCount());
        assertEquals(table2, testDatabase.getTables().get(0));
        
        //Although this leaves the table in an invalid state there was no persist call
        //to actually remove the imported key so it should stay there.
        assertEquals(1, table2.getChildren(SQLImportedKey.class).size());
    }
    
}
