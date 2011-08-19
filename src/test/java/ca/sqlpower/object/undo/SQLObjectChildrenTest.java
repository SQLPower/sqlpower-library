/*
 * Copyright (c) 2009, SQL Power Group Inc.
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

import javax.swing.undo.CompoundEdit;

import junit.framework.TestCase;
import ca.sqlpower.object.SPChildEvent;
import ca.sqlpower.object.SPChildEvent.EventType;
import ca.sqlpower.object.undo.SPObjectChildEdit;
import ca.sqlpower.sqlobject.SQLColumn;
import ca.sqlpower.sqlobject.SQLTable;
import ca.sqlpower.sqlobject.StubSQLObject;

public class SQLObjectChildrenTest extends TestCase {
    
    public class CompEdit extends CompoundEdit {
    	
    	public CompEdit() {
    		super();
    	}
    	
    	public boolean canRedo() {
    		return true;
    	}
    	
    }
    
    public void testAddChild() throws Exception {
    	SQLTable table = new SQLTable(new StubSQLObject(), "table", "", "", true);
    	int childCount = table.getChildCount();
    	
    	SQLColumn col = new SQLColumn();
    	col.setName("cow");
    	
    	SPObjectChildEdit edit = new SPObjectChildEdit(new SPChildEvent(table, SQLColumn.class, col, 0, EventType.ADDED));
    	edit.addChild();
    	
    	assertEquals(childCount + 1, table.getChildCount());
    	assertEquals(col, table.getChild(0));
    }
    
    public void testAddChildren() throws Exception {
    	SQLTable table = new SQLTable(new StubSQLObject(), "table", "", "", true);
    	int childCount = table.getChildCount();
    	
    	CompoundEdit transaction = new CompEdit();
    	
        SQLColumn column1 = new SQLColumn();
        column1.setName("cow");
        transaction.addEdit(new SPObjectChildEdit(new SPChildEvent(table, SQLColumn.class, column1, 0, EventType.ADDED)));
        
        SQLColumn column2 = new SQLColumn();
        column2.setName("chicken");
        transaction.addEdit(new SPObjectChildEdit(new SPChildEvent(table, SQLColumn.class, column2, 1, EventType.ADDED)));
        
        SQLColumn column3 = new SQLColumn();
        column3.setName("fish");
        transaction.addEdit(new SPObjectChildEdit(new SPChildEvent(table, SQLColumn.class, column3, 2, EventType.ADDED)));
        
        SQLColumn column4 = new SQLColumn();
        column4.setName("sheep");
        transaction.addEdit(new SPObjectChildEdit(new SPChildEvent(table, SQLColumn.class, column4, 3, EventType.ADDED)));
        
        SQLColumn column5 = new SQLColumn();
        column5.setName("dog");
        transaction.addEdit(new SPObjectChildEdit(new SPChildEvent(table, SQLColumn.class, column5, 4, EventType.ADDED)));
        
        SQLColumn column6 = new SQLColumn();
        column6.setName("cat");
        transaction.addEdit(new SPObjectChildEdit(new SPChildEvent(table, SQLColumn.class, column6, 5, EventType.ADDED)));
        
        SQLColumn column7 = new SQLColumn();
        column7.setName("bear");
        transaction.addEdit(new SPObjectChildEdit(new SPChildEvent(table, SQLColumn.class, column7, 6, EventType.ADDED)));
        transaction.redo();
        
        assertEquals(childCount + 7, table.getChildCount());
        assertEquals(column1, table.getChild(0));
        assertEquals(column2, table.getChild(1));
    }
    
    public void testRemoveChild() throws Exception {
    	SQLTable table = new SQLTable(new StubSQLObject(), "table", "", "", true);
    	int childCount = table.getChildCount();
    	
    	SQLColumn col = new SQLColumn();
    	col.setName("cow");
    	
    	SPObjectChildEdit edit = new SPObjectChildEdit(new SPChildEvent(table, SQLColumn.class, col, 0, EventType.ADDED));
    	edit.addChild();
    	
    	assertEquals(childCount + 1, table.getChildCount());
    	assertEquals(col, table.getChild(0));
    	
    	edit.removeChild();
    	
    	assertEquals(childCount, table.getChildCount());
    }
    
    public void testRemoveChildren() throws Exception {
    	SQLTable table = new SQLTable(new StubSQLObject(), "table", "", "", true);
    	int childCount = table.getChildCount();
    	
    	CompoundEdit transaction = new CompEdit();
    	
        SQLColumn column1 = new SQLColumn();
        column1.setName("cow");
        transaction.addEdit(new SPObjectChildEdit(new SPChildEvent(table, SQLColumn.class, column1, 0, EventType.ADDED)));
        
        SQLColumn column2 = new SQLColumn();
        column2.setName("chicken");
        transaction.addEdit(new SPObjectChildEdit(new SPChildEvent(table, SQLColumn.class, column2, 1, EventType.ADDED)));
        
        SQLColumn column3 = new SQLColumn();
        column3.setName("fish");
        transaction.addEdit(new SPObjectChildEdit(new SPChildEvent(table, SQLColumn.class, column3, 2, EventType.ADDED)));
        
        SQLColumn column4 = new SQLColumn();
        column4.setName("sheep");
        transaction.addEdit(new SPObjectChildEdit(new SPChildEvent(table, SQLColumn.class, column4, 3, EventType.ADDED)));
        
        SQLColumn column5 = new SQLColumn();
        column5.setName("dog");
        transaction.addEdit(new SPObjectChildEdit(new SPChildEvent(table, SQLColumn.class, column5, 4, EventType.ADDED)));
        
        SQLColumn column6 = new SQLColumn();
        column6.setName("cat");
        transaction.addEdit(new SPObjectChildEdit(new SPChildEvent(table, SQLColumn.class, column6, 5, EventType.ADDED)));
        
        SQLColumn column7 = new SQLColumn();
        column7.setName("bear");
        transaction.addEdit(new SPObjectChildEdit(new SPChildEvent(table, SQLColumn.class, column7, 6, EventType.ADDED)));
        
        transaction.addEdit(new SPObjectChildEdit(new SPChildEvent(table, SQLColumn.class, column2, 1, EventType.REMOVED)));
        transaction.addEdit(new SPObjectChildEdit(new SPChildEvent(table, SQLColumn.class, column3, 1, EventType.REMOVED)));
        transaction.addEdit(new SPObjectChildEdit(new SPChildEvent(table, SQLColumn.class, column6, 3, EventType.REMOVED)));
        transaction.addEdit(new SPObjectChildEdit(new SPChildEvent(table, SQLColumn.class, column7, 3, EventType.REMOVED)));
        
        transaction.redo();
        
        assertEquals(childCount + 3, table.getChildCount());
        assertEquals(column1, table.getChild(0));
        assertEquals(column4, table.getChild(1));
        assertEquals(column5, table.getChild(2));
        
        
    }

}
