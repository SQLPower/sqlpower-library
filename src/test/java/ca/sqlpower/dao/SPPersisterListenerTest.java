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

import junit.framework.TestCase;
import ca.sqlpower.dao.session.SessionPersisterSuperConverter;
import ca.sqlpower.object.CountingSPPersister;
import ca.sqlpower.object.SPObject;
import ca.sqlpower.sqlobject.SQLColumn;
import ca.sqlpower.sqlobject.SQLTable;
import ca.sqlpower.testutil.GenericNewValueMaker;
import ca.sqlpower.testutil.NewValueMaker;
import ca.sqlpower.testutil.SPObjectRoot;
import ca.sqlpower.util.RunnableDispatcher;
import ca.sqlpower.util.SessionNotFoundException;
import ca.sqlpower.util.StubWorkspaceContainer;
import ca.sqlpower.util.WorkspaceContainer;

public class SPPersisterListenerTest extends TestCase {
    
    private SPObjectRoot root;
    
    private StubWorkspaceContainer workspaceContainer;
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        workspaceContainer = new StubWorkspaceContainer() {
            @Override
            public SPObject getWorkspace() {
                return root;
            }
        };
        root = new SPObjectRoot() {
            @Override
            public RunnableDispatcher getRunnableDispatcher()
                    throws SessionNotFoundException {
                return workspaceContainer;
            }
            
            @Override
            public WorkspaceContainer getWorkspaceContainer()
                    throws SessionNotFoundException {
                return workspaceContainer;
            }
        };
    }

    /**
     * Tests an object can go through an add, remove, and add again set of calls
     * in one transaction in a persister listener. Test regression for bug 2830.
     */
    public void testAddRemoveAddObject() throws Exception {
        NewValueMaker valueMaker = new GenericNewValueMaker(root);
        SQLTable table = (SQLTable) valueMaker.makeNewValue(SQLTable.class, null, "");
        SQLColumn col = new SQLColumn();
        
        CountingSPPersister targetPersister = new CountingSPPersister();
        SessionPersisterSuperConverter converter = new SessionPersisterSuperConverter(null, table);
        SPPersisterListener listener = new SPPersisterListener(targetPersister, converter);
        table.addSPListener(listener);
        
        table.begin("Test transaction");
        table.addColumn(col);
        
        //Contains SQLTable, its UserDefinedSQLType and its SQLTypePhysicalProperties
        assertEquals(3, listener.getPersistedObjects().size());
        
        table.removeColumn(col);
        
        assertEquals(0, listener.getPersistedObjects().size());
        assertEquals(0, listener.getPersistedProperties().size());
        
        table.addColumn(col);
        
        assertEquals(3, listener.getPersistedObjects().size());
        assertTrue(!listener.getPersistedProperties().isEmpty());
        table.commit();
    }
    
}
