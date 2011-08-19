/*
 * Copyright (c) 2008, SQL Power Group Inc.
 *
 * This file is part of Power*Architect.
 *
 * Power*Architect is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * Power*Architect is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>. 
 */
package ca.sqlpower.object.undo;

import junit.framework.TestCase;
import ca.sqlpower.object.SPChildEvent;
import ca.sqlpower.object.SPObject;
import ca.sqlpower.object.SPChildEvent.EventType;
import ca.sqlpower.object.undo.SPObjectChildEdit;
import ca.sqlpower.object.undo.SPObjectUndoManager;
import ca.sqlpower.sqlobject.SQLColumn;
import ca.sqlpower.sqlobject.SQLDatabase;
import ca.sqlpower.sqlobject.SQLIndex;
import ca.sqlpower.sqlobject.SQLRelationship;
import ca.sqlpower.sqlobject.SQLTable;
import ca.sqlpower.sqlobject.StubSQLObject;


public class TestSQLObjectChildrenInsert extends TestCase {
	
	
	
	
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
	}
	
	
	public void testFolderInsert(){
		
	}
	
	public void testDatabaseInsert() throws Exception {
		
		// setup a playpen like database
		SQLDatabase db = new SQLDatabase();
		SPObjectUndoManager undoManager = new SPObjectUndoManager(db);
		db.setPlayPenDatabase(true);
		SQLTable table1 = new SQLTable(db,"table1","remark1","TABLE",true);
		SQLTable table2 = new SQLTable(db,"table2","remark2","TABLE",true);
		SQLTable table3 = new SQLTable(db,"table3","remark3","TABLE",true);
		SQLTable table4 = new SQLTable(db,"table4","remark4","TABLE",true);
		db.addChild(table1);
		db.addChild(table2);
		db.addChild(table3);
		db.addChild(table4);
		db.removeChild(db.getChild(2));
		undoManager.undo();
		assertEquals("There should be 4 children",4,db.getChildCount());
		assertEquals("The first table is in the wrong position",table1,db.getChild(0));
		assertEquals("The Second table is in the wrong position",table2,db.getChild(1));
		assertEquals("The Third table is in the wrong position",table3,db.getChild(2));
		assertEquals("The Fourth table is in the wrong position",table4,db.getChild(3));
		

		undoManager.redo();
		assertEquals("There should be 3 children",3,db.getChildCount());
		assertEquals("The first table is in the wrong position",table1,db.getChild(0));
		assertEquals("The Second table is in the wrong position",table2,db.getChild(1));
		assertEquals("The Third table is in the wrong position",table4,db.getChild(2));
		
	}
	
    public void testPresentationNameGeneric() throws Exception {
        StubSQLObject parent = new StubSQLObject();
        StubSQLObject child = new StubSQLObject();
        parent.addChild(child);
        SPChildEvent evt = new SPChildEvent(parent, StubSQLObject.class, child, 0, EventType.ADDED);
        SPObjectChildEdit edit = new SPObjectChildEdit(evt);
        assertEquals("Add child", edit.getPresentationName());
    }

    public void testPresentationNameSQLTable() throws Exception {
        StubSQLObject parent = new StubSQLObject();
        SQLTable child = new SQLTable();
        parent.addChild(child);
        SPChildEvent evt = new SPChildEvent(parent, SQLTable.class, child, 0, EventType.ADDED);
        SPObjectChildEdit edit = new SPObjectChildEdit(evt);
        assertEquals("Add table", edit.getPresentationName());
    }
    
    public void testPresentationNameSQLColumn() throws Exception {
        StubSQLObject parent = new StubSQLObject();
        SQLTable table = new SQLTable(parent, "table", "", "", true); // SQLColumns are very picky about their parents.
        SQLColumn child = new SQLColumn();
        table.addChild(child);
        SPChildEvent evt = new SPChildEvent(parent, SQLColumn.class, child, 0, EventType.ADDED);
        SPObjectChildEdit edit = new SPObjectChildEdit(evt);
        assertEquals("Add column", edit.getPresentationName());
    }
    
    public void testPresentationNameSQLIndex() throws Exception {
        StubSQLObject parent = new StubSQLObject();
        SQLIndex child = new SQLIndex() {
            @Override
            public void setParent(SPObject parent) {
            	// no-op
            }
        };
        parent.addChild(child);
        SPChildEvent evt = new SPChildEvent(parent, SQLIndex.class, child, 0, EventType.ADDED);
        SPObjectChildEdit edit = new SPObjectChildEdit(evt);
        assertEquals("Add index", edit.getPresentationName());
    }
    
    public void testPresentationNameSQLRelationship() throws Exception {
        StubSQLObject parent = new StubSQLObject();
        SQLRelationship child = makeSQLRelationship();
        parent.addChild(child);
        SPChildEvent evt = new SPChildEvent(parent, SQLRelationship.class, child, 0, EventType.ADDED);
        SPObjectChildEdit edit = new SPObjectChildEdit(evt);
        assertEquals("Add relationship", edit.getPresentationName());
    }
    
    /**
     * Creates a SQLIndex that doesn't care what kind of parent it belongs to.
     */
    private SQLIndex makeSQLIndex() {
        return new SQLIndex() {
        	@Override
        	public void setParent(SPObject parent) {
        		// no-op
        	}
        };
    }

    /**
     * Creates a SQLRelationship that doesn't care what kind of parent it belongs to.
     */
    private SQLRelationship makeSQLRelationship() {
        return new SQLRelationship() {
            @Override
            public void setParent(SPObject parent) {
                // no op!
            }
        };
    }
}
