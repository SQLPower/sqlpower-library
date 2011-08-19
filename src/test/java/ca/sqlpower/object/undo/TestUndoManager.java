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

import java.beans.PropertyChangeEvent;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CompoundEdit;
import javax.swing.undo.UndoableEdit;

import junit.framework.TestCase;
import ca.sqlpower.object.undo.SPObjectUndoManager;
import ca.sqlpower.object.undo.SPObjectUndoManager.SPObjectUndoableEventAdapter;
import ca.sqlpower.sqlobject.SQLColumn;
import ca.sqlpower.sqlobject.SQLDatabase;
import ca.sqlpower.sqlobject.SQLObjectException;
import ca.sqlpower.sqlobject.SQLObjectRoot;
import ca.sqlpower.sqlobject.SQLRelationship;
import ca.sqlpower.sqlobject.SQLTable;
import ca.sqlpower.sqlobject.StubSQLObject;
import ca.sqlpower.sqlobject.SQLIndex.AscendDescend;
import ca.sqlpower.util.TransactionEvent;

public class TestUndoManager extends TestCase {

    /**
     * Helps test undo manager by logging all calls to setFoo() in the
     * history list.
     */
    public class UndoTester extends StubSQLObject {
        public List<Integer> history = new ArrayList<Integer>();
        
        public void setFoo(Integer v) { history.add(v); }

    }

	SPObjectUndoManager undoManager;
	SQLTable fkTable;
	SQLTable pkTable;
	
	protected void setUp() throws Exception {
		super.setUp();
		System.out.println("-----------------Start setup for "+getName()+"----------------");
		SQLObjectRoot root = new SQLObjectRoot();
		SQLDatabase db = new SQLDatabase();
		fkTable = new SQLTable(db,true);
		fkTable.setName("child");
		root.addChild(fkTable);
		pkTable = new SQLTable(db,true);
		pkTable.setName("parent");
		root.addChild(pkTable);
		undoManager = new SPObjectUndoManager(root);
		pkTable.begin("Setting up test");
		pkTable.addColumn(new SQLColumn());
		pkTable.addColumn(new SQLColumn());
		pkTable.getPrimaryKeyIndex().addIndexColumn(pkTable.getColumn(0), AscendDescend.UNSPECIFIED);
		pkTable.getColumn(0).setName("pk1");
		pkTable.getColumn(0).setType(Types.INTEGER);
		pkTable.getPrimaryKeyIndex().addIndexColumn(pkTable.getColumn(1), AscendDescend.UNSPECIFIED);
		pkTable.getColumn(1).setName("pk2");
		pkTable.getColumn(1).setType(Types.INTEGER);
		pkTable.commit();
		db.addChild(pkTable);
		db.addChild(fkTable);
		System.out.println("-----------------End setup for "+getName()+"----------------");
		
	}
		
	public void testAllowCompoundEdit()
	{
		UndoableEdit stubEdit1 = new AbstractUndoableEdit(); 
		UndoableEdit stubEdit2 = new AbstractUndoableEdit(); 
		UndoableEdit stubEdit3 = new AbstractUndoableEdit(); 
		CompoundEdit ce = new CompoundEdit();
		ce.addEdit(stubEdit1);
		ce.addEdit(stubEdit2);
		ce.addEdit(stubEdit3);
		ce.end();
		undoManager.addEdit(ce);
		assertTrue(undoManager.canUndo());
	}
	
	public void testNestedCompoundEdits() {
		pkTable.setName("old");
		fkTable.setName("old");
		pkTable.setRemarks("old");
		fkTable.setRemarks("old");
		
		undoManager.getEventAdapter().transactionStarted(
				TransactionEvent.createStartTransactionEvent(this, "Starting compoundedit"));
		pkTable.setName("one");
		undoManager.getEventAdapter().transactionStarted(
				TransactionEvent.createStartTransactionEvent(this, "Starting nested compoundedit"));
		fkTable.setName("two");
		undoManager.getEventAdapter().transactionEnded(
				TransactionEvent.createEndTransactionEvent(this));
		pkTable.setRemarks("three");
		undoManager.getEventAdapter().transactionEnded(
				TransactionEvent.createEndTransactionEvent(this));
		fkTable.setRemarks("four");
		
		assertEquals("one", pkTable.getName());
		assertEquals("two", fkTable.getName());
		assertEquals("three", pkTable.getRemarks());
		assertEquals("four", fkTable.getRemarks());
		
		undoManager.undo();
		
		assertEquals("one", pkTable.getName());
		assertEquals("two", fkTable.getName());
		assertEquals("three", pkTable.getRemarks());
		assertEquals("old", fkTable.getRemarks());
		
		undoManager.undo();

		assertEquals("old", pkTable.getName());
		assertEquals("old", fkTable.getName());
		assertEquals("old", pkTable.getRemarks());
		assertEquals("old", fkTable.getRemarks());

	}
    
    /**
     * Makes sure compound edits added through the sql object event adapter
     * are undone in order of most recent to least recent.
     */
    public void testCompoundEditsUndoInCorrectOrder() {
        UndoTester myTester = new UndoTester();
        SPObjectUndoableEventAdapter adapter = undoManager.getEventAdapter();
        myTester.addSPListener(adapter);
        myTester.begin("Test Compound undo");
        adapter.propertyChanged(
                new PropertyChangeEvent(
                        myTester, "foo", 0, 1));
        adapter.propertyChanged(
                new PropertyChangeEvent(
                        myTester, "foo", 1, 2));
        adapter.propertyChanged(
                new PropertyChangeEvent(
                        myTester, "foo", 2, 3));
        myTester.commit();
        
        undoManager.undo();

        // Ensure the compound undo happened last..first
        assertEquals(Integer.valueOf(2), myTester.history.get(0));
        assertEquals(Integer.valueOf(1), myTester.history.get(1));
        assertEquals(Integer.valueOf(0), myTester.history.get(2));
    }
	
	/** Makes sure that the side effects of changing a PK column's attributes are not a separate undo step */
	public void testUndoRelationshipPkAttributeChange() throws SQLObjectException {
		SQLRelationship.createRelationship(pkTable, fkTable, false);
		SQLColumn pk1 = pkTable.getColumnByName("pk1");
		assertEquals("pk1 was already the new type.. makes testing silly", pk1.getType(), Types.INTEGER);
		SQLColumn fk1 = fkTable.getColumnByName("pk1");
		assertNotNull("fk column not in fkTable", fk1);
        assertEquals("pk and fk must start out with same datatype", pk1.getType(), fk1.getType());
		pk1.setType(Types.BINARY);
		assertEquals("fkTable not updated when the pktable was updated",Types.BINARY,fk1.getType());
		undoManager.undo();
		assertEquals("fk1 didn't go back to old type", Types.INTEGER, fk1.getType());
		
		// this is the point of the test
		assertEquals("pk1 didn't go back to old type", Types.INTEGER, pk1.getType());
	}


    /**
     * Test to ensure magic is respected when moving FK index columns from the
     * primary key which changes the relationship identifyingness.
     */
    public void testUndoIndexMoveAndRelationChange() throws Exception {
        pkTable.moveAfterPK(pkTable.getColumn(1));
        
        assertEquals(1, pkTable.getPkSize());
        assertEquals(0, fkTable.getPkSize());
        
        SQLRelationship relationship = SQLRelationship.createRelationship(pkTable, fkTable, true);
        
        assertEquals(1, fkTable.getPkSize());
        assertTrue(relationship.isIdentifying());

        pkTable.getParent().begin("Test");
        relationship.setIdentifying(false);
        fkTable.moveAfterPK(fkTable.getColumn(0));
        pkTable.getParent().commit();
        
        assertEquals(0, fkTable.getPkSize());
        undoManager.undo();
        
        assertEquals(true, relationship.isIdentifying());
        assertEquals(1, fkTable.getPkSize());
    }
    
}
