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
package ca.sqlpower.sqlobject;

import java.sql.Types;

import ca.sqlpower.object.ObjectDependentException;
import ca.sqlpower.object.SPObject;
import ca.sqlpower.sqlobject.SQLIndex.AscendDescend;
import ca.sqlpower.sqlobject.SQLIndex.Column;
import ca.sqlpower.util.SQLPowerUtils;

public class TestSQLIndex extends BaseSQLObjectTestCase {

    private SQLIndex index;
    private SQLIndex index2;
    private SQLIndex index3;
    private SQLColumn col1;
    private SQLColumn col2;
    private SQLColumn col3;
    private SQLTable table;
    private SQLTable dbTable;
    
    public TestSQLIndex(String name) throws Exception {
        super(name);
        propertiesToIgnoreForEventGeneration.add("parentTable");
        propertiesToIgnoreForUndo.add("parentTable");
    }

    protected void setUp() throws Exception {
        super.setUp();
        
        sqlx("CREATE TABLE SQL_COLUMN_TEST_1PK (\n" +
                " cow numeric(11),\n" +
                " moo varchar(10),\n" +
                " foo char(10)," +
                " CONSTRAINT test1pk PRIMARY KEY (cow))");
        
        sqlx("CREATE TABLE SQL_COLUMN_TEST_3PK (\n" +
                " cow numeric(11) NOT NULL,\n" +
                " moo varchar(10) NOT NULL,\n" +
                " foo char(10) NOT NULL,\n" +
                " CONSTRAINT test3pk PRIMARY KEY (cow, moo, foo))");
        
        sqlx("CREATE TABLE SQL_COLUMN_TEST_0PK (\n" +
                " cow numeric(11),\n" +
                " moo varchar(10),\n" +
                " foo char(10))");

        index = new SQLIndex("Test Index",true,"a", "HASH","b");
        table = new SQLTable(null, true);
        table.setName("Test Table");
        col1 = new SQLColumn();
        table.addColumn(col1);
        col2 = new SQLColumn();
        table.addColumn(col2);
        col3 = new SQLColumn();
        table.addColumn(col3);
        index.addIndexColumn(col1, AscendDescend.UNSPECIFIED);
        index.addIndexColumn(col2, AscendDescend.DESCENDING);
        index.addIndexColumn(col3, AscendDescend.ASCENDING);
        table.addIndex(index);
        index2 = new SQLIndex("Test Index 2",true,"a", "HASH","b");
        index2.addIndexColumn(col1, AscendDescend.UNSPECIFIED);
        index2.addIndexColumn(col3, AscendDescend.DESCENDING);
        table.addIndex(index2);
        table.setPopulated(true);
        dbTable = db.getTableByName("SQL_COLUMN_TEST_3PK");

        index3 = new SQLIndex("Test Index 3", true, "a", "HASH", "b");
        index3.addIndexColumn(col3, AscendDescend.ASCENDING);
        index3.addIndexColumn(col2, AscendDescend.DESCENDING);
        index3.addIndexColumn(col1, AscendDescend.UNSPECIFIED);
        table.addIndex(index3);
        db.addTable(table);
    }

    @Override
    protected SQLObject getSQLObjectUnderTest() {
       
        return index;
    }
    
    @Override
    protected Class<? extends SPObject> getChildClassType() {
    	return Column.class;
    }

    /**
     * When you add an index column, it should attach a listener to its target column.
     */
    public void testReAddColumnAddsListener() throws Exception {
        System.out.println("Original listeners:       "+col1.getSPListeners());
        int origListeners = col1.getSPListeners().size();
        SQLIndex.Column childToRemove = index.getChild(0);
        index.removeChild(childToRemove);
        index.addChild(childToRemove);
        System.out.println("Post-remove-add listeners: "+col1.getSPListeners());
        assertEquals(origListeners, col1.getSPListeners().size());
    }
    
    /**
     * When you remove a column from an index, it has to unsubscribe its
     * listener from its target column.
     */
    public void testRemoveColumnNoListenerLeak() throws Exception {
        System.out.println("Original listeners:    "+col1.getSPListeners());
        int origListeners = col1.getSPListeners().size();
        index.removeChild(index.getChild(0));
        System.out.println("Post-remove listeners: "+col1.getSPListeners());
        assertEquals(origListeners - 1, col1.getSPListeners().size());
    }
    
    /**
     * This functional test case comes from a post in the forum (#1670).
     */
    public void testIndexRemovedWithPK() throws Exception {
        SQLTable testTable = new SQLTable(null,true);
        testTable.setName("Test Table");
        SQLColumn col = new SQLColumn(testTable, "pk", Types.INTEGER, 10, 0);
        col.setPrimaryKeySeq(0);
        testTable.addColumn(col);
        
        SQLIndex ind = testTable.getPrimaryKeyIndex();
        
        assertTrue("The column should be added to the index", ind.getChildByName("pk") != null);
        
        testTable.removeChild(col);
        
        assertNull("The column was not removed from the index", ind.getChildByName("pk"));
        assertNull("The table should not have a PK index", testTable.getPrimaryKeyIndex());
    }
    
    public void testCopyConstructor() throws SQLObjectException{
        SQLIndex copyIndex = new SQLIndex(index);
        
        assertEquals("Different Name",index.getName(),copyIndex.getName());
        assertEquals("Different uniqueness values", index.isUnique(),copyIndex.isUnique());
        assertEquals("Different index types", index.getType(),copyIndex.getType());
        assertEquals("Different qualifiers", index.getQualifier(),copyIndex.getQualifier());
        assertEquals("Different filters", index.getFilterCondition(),copyIndex.getFilterCondition());
        assertEquals("Different number of children", index.getChildCount(),copyIndex.getChildCount());
        
        for (int i=0; i< index.getChildCount();i++){
            assertEquals("Different columns for index column "+1, index.getChild(i).getColumn(),copyIndex.getChild(i).getColumn());
        }
    }
    
    public void testSetPrimaryKeyIndexTrueWithOnNonPkAndWithNoSetPK() throws SQLObjectException {
        assertFalse("Test Index 1 already set as the pk", index.isPrimaryKeyIndex());
        assertFalse("Test Index 2 already set as the pk", index2.isPrimaryKeyIndex());
        assertNull("Table contained a pk index",table.getPrimaryKeyIndex());
        index.setPrimaryKeyIndex(true);
        assertTrue("Test Index 1 not set as the pk", index.isPrimaryKeyIndex());
        assertFalse("Test Index 2 set as the pk", index2.isPrimaryKeyIndex());
        assertEquals("Index 1 not the table's primary key",index, table.getPrimaryKeyIndex());
    }
    
    public void testSetPrimaryKeyIndexTrueWithOnNonPkAndWithDifferentPKSet() throws SQLObjectException {
        index2.setPrimaryKeyIndex(true);
        assertFalse("Test Index 1 already set as the pk", index.isPrimaryKeyIndex());
        assertTrue("Test Index 2 not set as the pk", index2.isPrimaryKeyIndex());
        assertEquals("Table did not contain index 2 as a pk index",index2,table.getPrimaryKeyIndex());
        index.setPrimaryKeyIndex(true);
        assertTrue("Test Index 1 not set as the pk", index.isPrimaryKeyIndex());
        assertFalse("Test Index 2 set as the pk", index2.isPrimaryKeyIndex());
        assertEquals("Index 1 not the table's primary key",index, table.getPrimaryKeyIndex());
    }
    
    public void testSetPrimaryKeyIndexTrueWithOnNonPkAndWithSameAsPK() throws SQLObjectException {
        index.setPrimaryKeyIndex(true);
        assertTrue("Test Index 1 not set as the pk", index.isPrimaryKeyIndex());
        assertFalse("Test Index 2 set as the pk", index2.isPrimaryKeyIndex());
        assertEquals("Table did not contain index as a pk index",index,table.getPrimaryKeyIndex());
        index.setPrimaryKeyIndex(true);
        assertTrue("Test Index 1 not set as the pk", index.isPrimaryKeyIndex());
        assertFalse("Test Index 2 set as the pk", index2.isPrimaryKeyIndex());
        assertEquals("Index 1 not the table's primary key",index, table.getPrimaryKeyIndex());
    }
    
    public void testSetPrimaryKeyIndexFalseWithOnNonPkAndWithNoSetPK() throws SQLObjectException {
        assertFalse("Test Index 1 already set as the pk", index.isPrimaryKeyIndex());
        assertFalse("Test Index 2 already set as the pk", index2.isPrimaryKeyIndex());
        assertNull("Table contained a pk index",table.getPrimaryKeyIndex());
        index.setPrimaryKeyIndex(false);
        assertFalse("Test Index 1 already set as the pk", index.isPrimaryKeyIndex());
        assertFalse("Test Index 2 already set as the pk", index2.isPrimaryKeyIndex());
        assertNull("Table contained a pk index",table.getPrimaryKeyIndex());
    }
    
    public void testSetPrimaryKeyIndexFalseWithOnNonPkAndWithDifferentPKSet() throws SQLObjectException {
        index2.setPrimaryKeyIndex(true);
        assertFalse("Test Index 1 already set as the pk", index.isPrimaryKeyIndex());
        assertTrue("Test Index 2 not set as the pk", index2.isPrimaryKeyIndex());
        assertEquals("Table did not contain index 2 as a pk index",index2,table.getPrimaryKeyIndex());
        index.setPrimaryKeyIndex(false);
        assertFalse("Test Index 1 already set as the pk", index.isPrimaryKeyIndex());
        assertTrue("Test Index 2 not set as the pk", index2.isPrimaryKeyIndex());
        assertEquals("Table did not contain index 2 as a pk index",index2,table.getPrimaryKeyIndex());
    }
    
    public void testSetPrimaryKeyIndexFalseWithOnNonPkAndWithSameAsPK() throws SQLObjectException {
        index.setPrimaryKeyIndex(true);
        assertTrue("Test Index 1 not set as the pk", index.isPrimaryKeyIndex());
        assertFalse("Test Index 2 set as the pk", index2.isPrimaryKeyIndex());
        assertEquals("Table did not contain index as a pk index",index,table.getPrimaryKeyIndex());
        index.setPrimaryKeyIndex(false);
        assertFalse("Test Index 1 set as the pk", index.isPrimaryKeyIndex());
        assertFalse("Test Index 2 set as the pk", index2.isPrimaryKeyIndex());
        assertNull("The table's primary key is not null", table.getPrimaryKeyIndex());
    }
    
    
    public void testLoadFromDbGetsCorrectPK() throws SQLObjectException{
        assertNotNull("No primary key loaded",dbTable.getPrimaryKeyIndex());
        assertEquals("Wrong indices: " + dbTable.getIndices(),
                1, dbTable.getIndices().size());
        assertEquals("Wrong primary key", "TEST3PK", dbTable.getPrimaryKeyName());
    }
    
    public void testAddStringColumnToPKThrowsException() throws Exception {
        SQLIndex i = new SQLIndex("Index",true,"","BTREE","");
        i.setPrimaryKeyIndex(true);
        try {
            i.addChild(new Column("index column",AscendDescend.UNSPECIFIED));
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("The primary key index must consist of real columns, not expressions",e.getMessage());
            return;
        }
        fail();
    }
    
    public void testAddChangeIndexToPkWithStringColumn() throws SQLObjectException{
        SQLIndex i = new SQLIndex("Index",true,"", "BTREE","");
        i.addChild(new Column("index column",AscendDescend.UNSPECIFIED));
        try {
            i.setPrimaryKeyIndex(true);
            fail();
        } catch (SQLObjectException e) {
            assertEquals("A PK must only refer to Index.Columns that contain SQLColumns",e.getMessage());
            return;
        }
    }
    
    public void testMakeColumnsLikeOtherIndexWhichHasNoColumns() throws SQLObjectException, IllegalArgumentException, ObjectDependentException {
        SQLIndex i = new SQLIndex("Index",true,"", "BTREE","");
        SQLColumn col = new SQLColumn();
        i.addChild(new Column("index column",AscendDescend.UNSPECIFIED));
        i.addChild(new Column(col,AscendDescend.UNSPECIFIED));
        
        SQLIndex i2 = new SQLIndex("Index2",false,"", "HASH","asdfa");
        i.makeColumnsLike(i2);
        assertEquals("Oh no some children are left!",0,i.getChildCount());
    }
    
    public void testMakeColumnsLikeOtherIndexWhichHasColumns() throws SQLObjectException, IllegalArgumentException, ObjectDependentException {
        SQLIndex i = new SQLIndex("Index",true,"", "BTREE","");
        SQLColumn col = new SQLColumn();
        
        SQLIndex i2 = new SQLIndex("Index2",false,"", "HASH","asdfa");
        i2.addChild(new Column("index column",AscendDescend.UNSPECIFIED));
        i2.addChild(new Column(col,AscendDescend.UNSPECIFIED));
        i.makeColumnsLike(i2);
        assertEquals("Wrong number of children!",2,i.getChildCount());
        assertEquals("Oh no wrong child!",i2.getChild(0),i.getChild(0));
        assertEquals("Oh no wrong child!",i2.getChild(1),i.getChild(1));
    }
    
    public void testMakeColumnsLikeOtherIndexReordersColumns() throws SQLObjectException, IllegalArgumentException, ObjectDependentException {
        SQLIndex i = new SQLIndex("Index",true,"", "BTREE","");
        SQLColumn col = new SQLColumn();
        i.addChild(new Column(col,AscendDescend.UNSPECIFIED));
        i.addChild(new Column("index column",AscendDescend.UNSPECIFIED));

        SQLIndex i2 = new SQLIndex("Index2",false,"", "HASH","asdfa");
        i2.addChild(new Column("index column",AscendDescend.UNSPECIFIED));
        i2.addChild(new Column(col,AscendDescend.UNSPECIFIED));
        i.makeColumnsLike(i2);
        assertEquals("Wrong number of children!",2,i.getChildCount());
        assertEquals("Oh no wrong child!",i2.getChild(0),i.getChild(0));
        assertEquals("Oh no wrong child!",i2.getChild(1),i.getChild(1));
    }
    
    // Test to ensure NPE doesn't get thrown for a SQLIndex with no SQLColumn
    public void testGetDerivedInstance() throws Exception {
        SQLIndex derivedIndex;
        Column newColumn = new Column("lower((name)::text))",AscendDescend.UNSPECIFIED);
        index.addChild(newColumn);
        derivedIndex = SQLIndex.getDerivedInstance(index, table);
    }
    
    /**
     * This test case will drop a column from a table and make sure that it
     *  is also dropped from the index of the table.
     */
    public void testDropColumnFromIndex()throws SQLObjectException{

        assertEquals(3, index.getChildCount());
        assertEquals(3, table.getColumns().size());
        assertEquals(2, index2.getChildCount());
        table.removeColumn(0);
        assertEquals(2, table.getColumns().size());
        assertEquals(2, index.getChildCount());
        assertEquals(1, index2.getChildCount());
        assertEquals(2, index3.getChildCount());
    }
    
    /**
     * This is similar to testDropColumnFromIndex, but it will add the columns in different
     * order in two separate indices and it will check that the proper column is removed.
     */
    public void testDropColumnFromIndex2() throws SQLObjectException{
        table.removeColumn(0); 
        assertEquals(2, table.getColumns().size()); 
        assertEquals(2, index.getChildCount()); 
        assertEquals(2, index3.getChildCount());
        assertEquals(col2, index.getChild(0).getColumn());
        assertEquals(col3, index.getChild(1).getColumn());
        assertEquals(col3, index3.getChild(0).getColumn());
        assertEquals(col2, index3.getChild(1).getColumn());
    }
    
    /**
     * Tests if we remove all of the columns in an index from its table
     * that the index is removed from the table as well.
     */
    public void testRemoveIndexWhenColsRemoved() throws Exception {
        assertEquals(index2, table.getIndexByName("Test Index 2"));
        table.removeColumn(2);
        table.removeColumn(0);
        assertEquals(null, table.getIndexByName("Test Index 2"));
    }

    /**
     * Tests if we remove all of the columns in an index from its table
     * that the index is removed from the table as well, and the whole operation
     * is a single compound operation.
     */
    public void testRemoveIndexWhenColsRemovedSingleUndoEvent() throws Exception {
        assertEquals(index2, table.getIndexByName("Test Index 2"));
        CountingCompoundEventListener l = new CountingCompoundEventListener();
        SQLPowerUtils.listenToHierarchy(table, l);
        table.removeColumn(2);
        assertEquals(0, l.getEditsBeforeLastGroup());
        table.removeColumn(0);
        assertEquals(0, l.getEditsBeforeLastGroup());
    }
    
    /**
     * SQLIndex.updateToMatch should not create an index with Column entries
     * that point to columns of other tables. This test ensures that it doesn't.
     */
    public void testUpdateToMatchBadColumnRefs() throws Exception {
        SQLIndex source = dbTable.getPrimaryKeyIndex();
        SQLIndex target = index3;
        
        target.updateToMatch(source);
        
        assertEquals(3, target.getChildCount()); // just to ensure we're testing something!
        
        for (SQLIndex.Column icol : target.getChildren(SQLIndex.Column.class)) {
            if (icol.getColumn() != null) {
                assertNotSame(source, icol.getColumn().getParent());
            }
        }
    }
    
    /**
     * Skipping this test as SQLIndex always returns true when asked if it is populated.
     */
    @Override
    public void testAddChildDoesNotPopulate() throws Exception {
    	//skip test
    }
}
