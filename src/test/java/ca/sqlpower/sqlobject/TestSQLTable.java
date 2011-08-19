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

import java.beans.PropertyChangeEvent;
import java.lang.reflect.InvocationTargetException;
import java.sql.DatabaseMetaData;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EventObject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.beanutils.BeanUtils;

import ca.sqlpower.object.AbstractPoolingSPListener;
import ca.sqlpower.object.SPChildEvent;
import ca.sqlpower.object.SPObject;
import ca.sqlpower.object.undo.CompoundEvent;
import ca.sqlpower.sql.JDBCDataSource;
import ca.sqlpower.sqlobject.SQLIndex.AscendDescend;
import ca.sqlpower.sqlobject.SQLIndex.Column;
import ca.sqlpower.sqlobject.SQLTable.TransferStyles;
import ca.sqlpower.sqlobject.TestSQLTable.EventLogger.SQLObjectSnapshot;
import ca.sqlpower.testutil.MockJDBCDriver;
import ca.sqlpower.util.SQLPowerUtils;

public class TestSQLTable extends BaseSQLObjectTestCase {
    
    private SQLTable table;    
    
    public TestSQLTable(String name) throws Exception {
        super(name);
    }
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();

        sqlx("CREATE TABLE REGRESSION_TEST1 (t1_c1 numeric(10), t1_c2 numeric(5))");
        sqlx("CREATE TABLE REGRESSION_TEST2 (t2_c1 char(10))");
        sqlx("CREATE VIEW REGRESSION_TEST1_VIEW AS SELECT * FROM REGRESSION_TEST1");
        
		sqlx("CREATE TABLE SQL_TABLE_POPULATE_TEST (\n" +
		        " cow numeric(10) NOT NULL, \n" +
		        " CONSTRAINT test4pk PRIMARY KEY (cow))");
		sqlx("CREATE TABLE SQL_TABLE_1_POPULATE_TEST (\n" +
		        " cow numeric(10) NOT NULL, \n" +
		        " CONSTRAINT test5pk PRIMARY KEY(cow))");
		sqlx("ALTER TABLE SQL_TABLE_1_POPULATE_TEST " +
		        "ADD CONSTRAINT TEST_FK FOREIGN KEY (cow) " +
		        "REFERENCES SQL_TABLE_POPULATE_TEST (cow)");

        table = new SQLTable(null, true);
        table.setParent(new StubSQLObject());
        
        table.addColumn(new SQLColumn(table, "one", Types.INTEGER, 10, 0));
        table.addColumn(new SQLColumn(table, "two", Types.INTEGER, 10, 0));
        table.addColumn(new SQLColumn(table, "three", Types.INTEGER, 10, 0));
        table.addColumn(new SQLColumn(table, "four", Types.INTEGER, 10, 0));
        table.addColumn(new SQLColumn(table, "five", Types.INTEGER, 10, 0));
        table.addColumn(new SQLColumn(table, "six", Types.INTEGER, 10, 0));
        table.getPrimaryKeyIndex().addIndexColumn(table.getColumn(0));
        table.getPrimaryKeyIndex().addIndexColumn(table.getColumn(1));
        table.getPrimaryKeyIndex().addIndexColumn(table.getColumn(2));
        table.getColumn(0).setNullable(DatabaseMetaData.columnNullable);
        table.getColumn(0).setAutoIncrement(true); 
        
        db.addTable(table);
    }
    
    @Override
    protected SQLObject getSQLObjectUnderTest() {
        return table;
    }
    
    @Override
    protected Class<? extends SPObject> getChildClassType() {
    	return SQLColumn.class;
    }
    
    public void testConstructor() {
        // FIXME need to test both constructors!
    }
    
    public void testGetDerivedInstance() throws Exception {
        SQLTable derivedTable;
        SQLTable table1;
        // Check to make sure it can be added to a playpen like database
        SQLDatabase pp = new SQLDatabase();
        pp.setPlayPenDatabase(true);
        pp.setParent(new StubSQLObject());
        assertNotNull(table1 = db.getTableByName("REGRESSION_TEST1"));
        derivedTable = table1.createInheritingInstance(pp);
        
        TreeMap<String, Object> derivedPropertyMap = new TreeMap<String, Object>(BeanUtils.describe(derivedTable));
        TreeMap<String, Object> table1PropertyMap = new TreeMap<String, Object>(BeanUtils.describe(table1));
        
        table1PropertyMap.remove("parent");
        table1PropertyMap.remove("SQLParent");
        table1PropertyMap.remove("schemaName");
        table1PropertyMap.remove("schema");
        table1PropertyMap.remove("parentDatabase");
        table1PropertyMap.remove("shortDisplayName");
        table1PropertyMap.remove("UUID");
        table1PropertyMap.remove("workspaceContainer");
        table1PropertyMap.remove("runnableDispatcher");
        table1PropertyMap.remove("SPListeners");
        
        for (Map.Entry<String, Object> property : table1PropertyMap.entrySet()) {
        	assertEquals("Property \"" + property.getKey() + "\" has changed;", property.getValue(), derivedPropertyMap.get(property.getKey()));
        }
        
    }
    
    public void testRenamePhysicalNameOfTableRenamesPK() throws SQLObjectException{
        assertNotNull("Table has null logical name",table.getName());
        String newName = "newTableName";
        String newPhysicalName = "new_Table_Name";
        table.setName(newName);
        
        assertNotNull("Table has null physical name", table.getPhysicalName());
        assertEquals("The physical name does not match the logical", newName, table.getPhysicalName().trim());
        
        table.setPhysicalName(newPhysicalName);
        assertEquals(newPhysicalName+"_pk",table.getPrimaryKeyIndex().getName());
    }
    
    public void testRenamePhysicalNameOfTableDoesntRenamePKIfPKRenamed() throws SQLObjectException {
        assertNotNull("Table has null name",table.getName());
        String newTableName = "newTableName";
        String newPKName = "NewPKName";
        String newPhysicalName = "new_Table_Name";
        table.getPrimaryKeyIndex().setName(newPKName);
        table.setName(newTableName);
        table.setPhysicalName(newPhysicalName);
        assertEquals(newPKName, table.getPrimaryKeyIndex().getName());
    }
    
    public void testRenamePhysicalNameOfTableRenamesSequences() throws Exception {
        table.setPhysicalName("old name");
        table.getColumn(0).setPhysicalName("cow");
        table.getColumn(0).setAutoIncrementSequenceName(table.getPhysicalName() + "_cow");
        table.setPhysicalName("new name");
        assertTrue(table.getColumn(0).isAutoIncrementSequenceNameSet());
        assertEquals(table.getPhysicalName() + "_cow_seq", table.getColumn(0).getAutoIncrementSequenceName());
    }

    public void testRenameTableDoesNotRenameUnnamedSequences() throws Exception {
        table.setName("old name");
        table.getColumn(0).setAutoIncrementSequenceName("moo_" + table.getName() + "_cow");
        table.setName("new name");
        assertTrue(table.getColumn(0).isAutoIncrementSequenceNameSet());
        for (int i = 1; i < table.getColumns().size(); i++) {
            assertFalse(table.getColumn(i).isAutoIncrementSequenceNameSet());
        }
    }
    
    public void testInherit() throws SQLObjectException {
        SQLTable table1;
        SQLTable table2;
        table1 = db.getTableByName("REGRESSION_TEST1");
        table2 = db.getTableByName("REGRESSION_TEST2");
        
        // the tables need to load properly
        assertEquals(2, table1.getColumns().size());
        assertEquals(1, table2.getColumns().size());
        
        table2.inherit(table1, TransferStyles.REVERSE_ENGINEER, true);
        assertEquals("The wrong 1st column was inherited",
                table1.getColumn(0).toString(), table2.getColumn(1).toString());
        assertEquals("The wrong 2nd column was inherited",
                table1.getColumn(1).toString(), table2.getColumn(2).toString());
        assertEquals("The wrong number of columns were inherited",
                table2.getColumns().size(), 3);
        try {
            table2.inherit(table2, TransferStyles.REVERSE_ENGINEER, true);
        } catch (SQLObjectException ae) {
            if ("Cannot inherit from self".equals(ae.getMessage())) {
                System.out.println("Expected Behaviour is to not be able to inherit from self");
            } else {
                throw ae;
            }
        }
    }
    
    public void testGetColumnByName() throws SQLObjectException {
        SQLTable table1;
        SQLColumn col1;
        SQLColumn col2;
        table1 = db.getTableByName("REGRESSION_TEST1");
        col2 = table1.getColumnByName("t1_c2");
        assertNotNull(col2);
        assertEquals("The wrong colomn us returned", col2, table1.getColumn(1));
        
        col1 = table1.getColumnByName("t1_c1");
        assertNotNull(col1);
        assertEquals("The wrong colomn us returned", col1, table1.getColumn(0));
        
        assertNull(col1 = table1.getColumnByName("This_is_a_non_existant_column"));
        assertNull("Invalid column name", col1 = table1.getColumnByName("$#  #$%#%"));
    }
    
    public void testAddColumn() throws SQLObjectException {
        SQLTable table1 = db.getTableByName("REGRESSION_TEST1");
        assertEquals(2, table1.getColumns().size());
        SQLColumn newColumn = new SQLColumn(table1, "my new column", Types.INTEGER, 10, 0);
        table1.addColumn(newColumn, 2);
        SQLColumn addedCol = table1.getColumn(2);
        assertSame("Column at index 2 isn't same object as we added", newColumn, addedCol);
    }
    
    public void testAddColumnReference() throws SQLObjectException {
        SQLTable table = db.getTableByName("REGRESSION_TEST1");
        SQLColumn col = table.getColumn(0);
        assertEquals("Existing column had refcount != 1", 1, col.getReferenceCount());
        table.addColumn(col);
        assertEquals("refcount didn't increase", 2, col.getReferenceCount());
    }
    
    public void testRemoveColumnByZeroRefs() throws SQLObjectException {
        SQLTable table = db.getTableByName("REGRESSION_TEST1");
        SQLColumn col = table.getColumn(0);
        table.addColumn(col);
        table.addColumn(col);
        col.removeReference();
        assertTrue(table.getColumns().contains(col));
        col.removeReference();
        assertTrue(table.getColumns().contains(col));
        col.removeReference();
        assertFalse(table.getColumns().contains(col));
    }
    
    /** this tests for a real bug.. the column was showing up one index above the end of the pk  */
    public void testAddColumnAtEndOfPK() throws SQLObjectException {
        SQLTable t = new SQLTable(null, true);
        t.setName("Test Table");
        SQLColumn pk1 = new SQLColumn(t, "PKColumn1", Types.INTEGER, 10, 0);
        SQLColumn pk2 = new SQLColumn(t, "PKColumn2", Types.INTEGER, 10, 0);
        SQLColumn pk3 = new SQLColumn(t, "PKColumn3", Types.INTEGER, 10, 0);
        SQLColumn at1 = new SQLColumn(t, "AT1", Types.INTEGER, 10, 0);
        SQLColumn at2 = new SQLColumn(t, "AT2", Types.INTEGER, 10, 0);
        SQLColumn at3 = new SQLColumn(t, "AT3", Types.INTEGER, 10, 0);
        
        t.addColumn(pk1,0);
        t.addColumn(pk2,1);
        t.addColumn(pk3,2);
        t.addColumn(at1,3);
        t.addColumn(at2,4);
        t.addColumn(at3,5);
        
        t.getPrimaryKeyIndex().addIndexColumn(pk1);
        t.getPrimaryKeyIndex().addIndexColumn(pk2);
        t.getPrimaryKeyIndex().addIndexColumn(pk3);
        
        assertEquals(3, t.getPkSize());
        
        SQLColumn newcol = new SQLColumn(t, "newcol", Types.INTEGER, 10, 0);
        t.addColumn(newcol, 3);
        assertEquals("New column should be at requested position", 3, t.getColumnIndex(newcol));
        t.getPrimaryKeyIndex().addIndexColumn(newcol);
        assertEquals("New column should still be at requested position", 3, t.getColumnIndex(newcol));
    }
    
    public void testMoveToPKClearsNullability() throws SQLObjectException{             
        SQLTable t = db.getTableByName("REGRESSION_TEST1");
        SQLColumn c = t.getColumnByName("t1_c1");
        assertFalse("Column shouldn't be in PK to begin", c.isPrimaryKey());
        c.setNullable(DatabaseMetaData.columnNullable);

        // Now c is not in the PK and is nullable.  Let's add it to PK
        t.changeColumnIndex(0,0,true);      
        
        assertTrue(c.isPrimaryKey());
        assertEquals(DatabaseMetaData.columnNoNulls, c.getNullable());
    }
    
    public void testRemoveColumnOutBounds() throws SQLObjectException {
        SQLTable table1;
        
        table1 = db.getTableByName("REGRESSION_TEST1");
        Exception exc = null;
        try {
            table1.removeColumn(16);
        } catch (IndexOutOfBoundsException e) {
            System.out.println("Method throws proper error");
            exc = e;
        }
        
        assertNotNull("Should have thrown an exception", exc);
    }
    
    public void testRemoveColumn() throws SQLObjectException {
        SQLTable table1;
        SQLColumn col1;
        SQLColumn col2;
        
        table1 = db.getTableByName("REGRESSION_TEST1");
        col1 = table1.getColumn(0);
        col2 = table1.getColumn(1);
        
        assertEquals("We removed a column when we shouldn't have",
                table1.getColumns().size(), 2);
        table1.removeColumn(col1);
        assertEquals("Either 0 or 2+ columns were removed",
                table1.getColumns().size(), 1);
        assertEquals("The wrong column was removed", col2, table1.getColumn(0));
        table1.removeColumn(0);
        assertEquals("Last Column failed to be removed",
                table1.getColumns().size(), 0);
        Exception exc = null;
        try {
            table1.removeColumn(0);
        } catch (IndexOutOfBoundsException e) {
            System.out.println("Method throws proper error");
            exc = e;
        }
        assertNotNull("Should have thrown an exception", exc);
    }
    
    public void testGetPrimaryKey() throws SQLObjectException {
    	SQLIndex i1 = new SQLIndex("name",true,null, "BTREE",null);
        SQLTable t1 = new SQLTable(null,true, i1);
        SQLColumn c1 = new SQLColumn(t1,"col1",1,0,0);
        t1.addChild(c1);
        i1.addIndexColumn(c1, AscendDescend.UNSPECIFIED);
        SQLIndex i2 = new SQLIndex("name 2",true,null, "BTREE",null);
        i2.addChild(new Column("Index column string",AscendDescend.UNSPECIFIED));
        t1.addChild(i2);
        
        assertEquals(i1, t1.getPrimaryKeyIndex());
        assertEquals(1, t1.getPrimaryKeyIndex().getChildrenWithoutPopulating().size());
    }
    
    public void testFireDbChildrenInserted() throws Exception {
        SQLTable table1 = new SQLTable();
        table1.setPopulated(true);
        
        TestingSQLObjectListener testListener = new TestingSQLObjectListener();
        table1.addSPListener(testListener);
        
        SQLColumn col = new SQLColumn();
        table1.addChild(col);
        assertEquals("Children inserted event not fired!", 1, testListener.getInsertedCount());
    }
    
    public void testFireDbChildrenRemoved() throws Exception {
        SQLTable table1 = new SQLTable();
        table1.setPopulated(true);
        SQLColumn col = new SQLColumn();
        table1.addChild(col);
        
        TestingSQLObjectListener testListener = new TestingSQLObjectListener();
        table1.addSPListener(testListener);
        
        table1.removeChild(col);
        assertEquals("Children removed event not fired!", 1, testListener.getRemovedCount());
    }
    
    public void testDeleteLockedColumn() throws SQLObjectException {
        SQLTable parentTable = new SQLTable(null, "parent", null, "TABLE", true);
        parentTable.addColumn(new SQLColumn(parentTable, "pkcol_1", Types.INTEGER, 10, 0));
        parentTable.addColumn(new SQLColumn(parentTable, "pkcol_2", Types.INTEGER, 10, 0));
        parentTable.addColumn(new SQLColumn(parentTable, "attribute_1", Types.INTEGER, 10, 0));
        
        SQLTable childTable1 = new SQLTable(null, "child_1", null, "TABLE", true);
        childTable1.addColumn(new SQLColumn(childTable1, "child_pkcol_1", Types.INTEGER, 10, 0));
        childTable1.addColumn(new SQLColumn(childTable1, "child_pkcol_2", Types.INTEGER, 10, 0));
        childTable1.addColumn(new SQLColumn(childTable1, "child_attribute", Types.INTEGER, 10, 0));
        
        SQLRelationship rel1 = new SQLRelationship();
        rel1.attachRelationship(parentTable,childTable1,false);
        rel1.addMapping(parentTable.getColumn(0), childTable1.getColumn(0));
        rel1.addMapping(parentTable.getColumn(1), childTable1.getColumn(1));
        
        
        try {
            SQLColumn inheritedCol = childTable1.getColumnByName("child_pkcol_1");
            childTable1.removeChild(inheritedCol);
            fail("Remove should have thrown LockedColumnException");
        } catch (LockedColumnException ex) {
            // good
        } catch (Exception ex) {
            fail("Didn't get the exception we were expecting: " + ex);
        }
    }
    
    public void testRemovePKColumn() throws SQLObjectException{
        assertEquals("There should be 6 columns to start",6, 
                table.getColumns().size());
        table.removeColumn((table.getColumnIndex(table.getColumnByName("two"))));
        
        assertEquals("A column should have been removed", 
                5, table.getColumns().size());
        
        assertEquals(2, table.getPkSize());        
        
        assertEquals(0, table.getColumnIndex(table.getColumnByName("one")));
        assertEquals(1, table.getColumnIndex(table.getColumnByName("three")));
        assertEquals(2, table.getColumnIndex(table.getColumnByName("four")));
        assertEquals(3, table.getColumnIndex(table.getColumnByName("five")));
        assertEquals(4, table.getColumnIndex(table.getColumnByName("six")));        
        
    }
    
    public void testRemoveFKColumn() throws Exception {
        assertEquals("There should be 6 columns to start",6, 
                table.getColumns().size());
        table.removeColumn(table.getColumnIndex(table.getColumnByName("five")));
        assertEquals("A column should have been removed", 
                5, table.getColumns().size());
        
        assertEquals(3, table.getPkSize());
        
        assertEquals(0, table.getColumnIndex(table.getColumnByName("one")));
        assertEquals(1, table.getColumnIndex(table.getColumnByName("two")));
        assertEquals(2, table.getColumnIndex(table.getColumnByName("three")));
        assertEquals(3, table.getColumnIndex(table.getColumnByName("four")));
        assertEquals(4, table.getColumnIndex(table.getColumnByName("six")));
    }
    
    public void testAddColAtFirstIdx() throws SQLObjectException{
        table.addColumn(new SQLColumn(table, "zero", Types.INTEGER, 10, 0), 0);        
        assertEquals(7, table.getColumns().size());
        assertEquals(4, table.getPkSize());
        
        assertTrue(table.getColumnByName("zero").isPrimaryKey());
        assertEquals(0, table.getColumnIndex(table.getColumnByName("zero")));
        assertEquals(1, table.getColumnIndex(table.getColumnByName("one")));
        assertEquals(2, table.getColumnIndex(table.getColumnByName("two")));
        assertEquals(3, table.getColumnIndex(table.getColumnByName("three")));
        assertEquals(4, table.getColumnIndex(table.getColumnByName("four")));
        assertEquals(5, table.getColumnIndex(table.getColumnByName("five")));
        assertEquals(6, table.getColumnIndex(table.getColumnByName("six")));
    }
    
    public void testAddColAbovePK() throws SQLObjectException{
        table.addColumn(new SQLColumn(table, "indextwo", Types.INTEGER, 10, 0), 2);        
        assertEquals(7, table.getColumns().size());
        assertEquals(4, table.getPkSize());
        
        assertTrue(table.getColumnByName("indextwo").isPrimaryKey());
        assertEquals(0, table.getColumnIndex(table.getColumnByName("one")));
        assertEquals(1, table.getColumnIndex(table.getColumnByName("two")));
        assertEquals(2, table.getColumnIndex(table.getColumnByName("indextwo")));
        assertEquals(3, table.getColumnIndex(table.getColumnByName("three")));
        assertEquals(4, table.getColumnIndex(table.getColumnByName("four")));
        assertEquals(5, table.getColumnIndex(table.getColumnByName("five")));
        assertEquals(6, table.getColumnIndex(table.getColumnByName("six")));
    }
    
    public void testAddColBelowPK() throws SQLObjectException{
        table.addColumn(new SQLColumn(table, "indexfour", Types.INTEGER, 10, 0), 4);        
        assertEquals(7, table.getColumns().size());
        assertEquals(3, table.getPkSize());
        
        assertFalse(table.getColumnByName("indexfour").isPrimaryKey());
        
        assertEquals(0, table.getColumnIndex(table.getColumnByName("one")));
        assertEquals(1, table.getColumnIndex(table.getColumnByName("two")));        
        assertEquals(2, table.getColumnIndex(table.getColumnByName("three")));
        assertEquals(3, table.getColumnIndex(table.getColumnByName("four")));
        assertEquals(4, table.getColumnIndex(table.getColumnByName("indexfour")));
        assertEquals(5, table.getColumnIndex(table.getColumnByName("five")));
        assertEquals(6, table.getColumnIndex(table.getColumnByName("six")));
    }
    
    public void testChangeFirstColumnIdx() throws SQLObjectException{
        table.changeColumnIndex(0, 1, true);
        assertEquals(3, table.getPkSize());
        
        assertEquals(0, table.getColumnIndex(table.getColumnByName("two")));
        assertEquals(1, table.getColumnIndex(table.getColumnByName("one")));
        assertEquals(2, table.getColumnIndex(table.getColumnByName("three")));
        assertEquals(3, table.getColumnIndex(table.getColumnByName("four")));        
        assertEquals(4, table.getColumnIndex(table.getColumnByName("five")));
        assertEquals(5, table.getColumnIndex(table.getColumnByName("six")));
    }
    
    public void testChangeSecondColumnIdx() throws SQLObjectException{
        table.changeColumnIndex(1, 0, true);
        assertEquals(3, table.getPkSize());
        
        assertEquals(0, table.getColumnIndex(table.getColumnByName("two")));
        assertEquals(1, table.getColumnIndex(table.getColumnByName("one")));
        assertEquals(2, table.getColumnIndex(table.getColumnByName("three")));
        assertEquals(3, table.getColumnIndex(table.getColumnByName("four")));        
        assertEquals(4, table.getColumnIndex(table.getColumnByName("five")));
        assertEquals(5, table.getColumnIndex(table.getColumnByName("six")));
    }
    
    public void testChangeSecondColumnIdxToFifth() throws SQLObjectException{
        table.changeColumnIndex(1, 4, true);
        assertEquals(2, table.getPkSize());
        
        assertEquals(0, table.getColumnIndex(table.getColumnByName("one")));
        assertEquals(1, table.getColumnIndex(table.getColumnByName("three")));
        assertEquals(2, table.getColumnIndex(table.getColumnByName("four")));
        assertEquals(3, table.getColumnIndex(table.getColumnByName("five")));
        assertEquals(4, table.getColumnIndex(table.getColumnByName("two")));
        assertEquals(5, table.getColumnIndex(table.getColumnByName("six")));
    }
    
    public void testChangeSFifthColumnIdxToSecond() throws Exception {
        EventLogger l = new EventLogger();
        SQLObjectSnapshot original = l.makeSQLObjectSnapshot(table);
        
        SQLPowerUtils.listenToHierarchy(table, l);
        table.changeColumnIndex(4, 1, true);        
        SQLPowerUtils.unlistenToHierarchy(table, l);

        assertEquals(4, table.getPkSize());
        
        assertEquals(0, table.getColumnIndex(table.getColumnByName("one")));
        assertEquals(1, table.getColumnIndex(table.getColumnByName("five")));
        assertEquals(2, table.getColumnIndex(table.getColumnByName("two")));
        assertEquals(3, table.getColumnIndex(table.getColumnByName("three")));
        assertEquals(4, table.getColumnIndex(table.getColumnByName("four")));       
        assertEquals(5, table.getColumnIndex(table.getColumnByName("six")));

        System.out.println("Event log:\n"+l);

        SQLObjectSnapshot afterChange = l.makeSQLObjectSnapshot(table);

        System.out.println("Original: "+original);
        System.out.println("After: "+afterChange);
        
        l.rollBack(afterChange);
        
        assertEquals(original.toString(), afterChange.toString());
    }
    
    public void testChangeSFifthColumnIdxToTop() throws Exception {
        EventLogger l = new EventLogger();
        SQLObjectSnapshot original = l.makeSQLObjectSnapshot(table);
        
        SQLPowerUtils.listenToHierarchy(table, l);
        table.changeColumnIndex(4, 0, true);        
        SQLPowerUtils.unlistenToHierarchy(table, l);

        assertEquals(4, table.getPkSize());
        
        assertEquals(0, table.getColumnIndex(table.getColumnByName("five")));
        assertEquals(1, table.getColumnIndex(table.getColumnByName("one")));
        assertEquals(2, table.getColumnIndex(table.getColumnByName("two")));
        assertEquals(3, table.getColumnIndex(table.getColumnByName("three")));
        assertEquals(4, table.getColumnIndex(table.getColumnByName("four")));       
        assertEquals(5, table.getColumnIndex(table.getColumnByName("six")));

        System.out.println("Event log:\n"+l);

        SQLObjectSnapshot afterChange = l.makeSQLObjectSnapshot(table);

        System.out.println("Original: "+original);
        System.out.println("After: "+afterChange);
        
        l.rollBack(afterChange);
        
        assertEquals(original.toString(), afterChange.toString());
    }
    
    public void testChangeSFifthColumnIdxToThird() throws SQLObjectException{
        table.changeColumnIndex(4, 2, true);        
        assertEquals(4, table.getPkSize());
        
        assertEquals(0, table.getColumnIndex(table.getColumnByName("one")));
        assertEquals(1, table.getColumnIndex(table.getColumnByName("two")));
        assertEquals(2, table.getColumnIndex(table.getColumnByName("five")));
        assertEquals(3, table.getColumnIndex(table.getColumnByName("three")));
        assertEquals(4, table.getColumnIndex(table.getColumnByName("four")));
        assertEquals(5, table.getColumnIndex(table.getColumnByName("six")));
    }
    
    public void testChangeSForthColumnIdxToThird() throws SQLObjectException{
        table.changeColumnIndex(3, 2, true);        
        assertEquals(4, table.getPkSize());
        
        assertEquals(0, table.getColumnIndex(table.getColumnByName("one")));
        assertEquals(1, table.getColumnIndex(table.getColumnByName("two")));
        assertEquals(2, table.getColumnIndex(table.getColumnByName("four")));
        assertEquals(3, table.getColumnIndex(table.getColumnByName("three")));
        assertEquals(4, table.getColumnIndex(table.getColumnByName("five")));
        assertEquals(5, table.getColumnIndex(table.getColumnByName("six")));
    }
    
    public void testChangeSThirdColumnIdxToForth() throws SQLObjectException{
        table.changeColumnIndex(2, 3, true);        
        assertEquals(2, table.getPkSize());
        
        assertEquals(0, table.getColumnIndex(table.getColumnByName("one")));
        assertEquals(1, table.getColumnIndex(table.getColumnByName("two")));
        assertEquals(2, table.getColumnIndex(table.getColumnByName("four")));
        assertEquals(3, table.getColumnIndex(table.getColumnByName("three")));
        assertEquals(4, table.getColumnIndex(table.getColumnByName("five")));
        assertEquals(5, table.getColumnIndex(table.getColumnByName("six")));
    }
    
    public void testChangeFirstColumnKey() throws SQLObjectException{
        SQLColumn col1 = table.getColumnByName("one");
        assertNotNull(col1);
        table.moveAfterPK(col1);
        assertEquals(2, table.getPkSize());
        //We just want to make sure it's no longer the first or second 
        //column where the PK column lies
        assertTrue(table.getColumnIndex(col1) > 1);        
    }
    
    public void testChangeThirdColumnKey() throws SQLObjectException{
        SQLColumn col3 = table.getColumnByName("three");
        assertNotNull(col3);
        table.moveAfterPK(col3);
        assertEquals(2, table.getPkSize());
        
        assertEquals(0, table.getColumnIndex(table.getColumnByName("one")));
        assertEquals(1, table.getColumnIndex(table.getColumnByName("two")));
        assertEquals(2, table.getColumnIndex(table.getColumnByName("three")));
        assertEquals(3, table.getColumnIndex(table.getColumnByName("four")));
        assertEquals(4, table.getColumnIndex(table.getColumnByName("five")));
        assertEquals(5, table.getColumnIndex(table.getColumnByName("six")));
    }
    
    public void testChangeFourthColumnKey() throws SQLObjectException{
        SQLColumn col4 = table.getColumnByName("four");
        assertNotNull(col4);
        table.changeColumnIndex(table.getColumnIndex(col4), 1, true);
        assertEquals(4, table.getPkSize());
        
        assertEquals(0, table.getColumnIndex(table.getColumnByName("one")));
        assertEquals(1, table.getColumnIndex(table.getColumnByName("four")));
        assertEquals(2, table.getColumnIndex(table.getColumnByName("two")));
        assertEquals(3, table.getColumnIndex(table.getColumnByName("three")));
        assertEquals(4, table.getColumnIndex(table.getColumnByName("five")));
        assertEquals(5, table.getColumnIndex(table.getColumnByName("six")));
    }
    
    public void testChangeFifthColumnKey() throws Exception {
        EventLogger l = new EventLogger();
        SQLColumn col5 = table.getColumnByName("five");
        assertNotNull(col5);

        SQLObjectSnapshot original = l.makeSQLObjectSnapshot(table);
        
        SQLPowerUtils.listenToHierarchy(table, l);
        table.changeColumnIndex(table.getColumnIndex(col5), 1, true);
        SQLPowerUtils.unlistenToHierarchy(table, l);

        System.out.println("Event log:\n"+l);

        SQLObjectSnapshot afterChange = l.makeSQLObjectSnapshot(table);

        assertEquals(4, table.getPkSize());
        
        assertEquals(0, table.getColumnIndex(table.getColumnByName("one")));
        assertEquals(1, table.getColumnIndex(table.getColumnByName("five")));
        assertEquals(2, table.getColumnIndex(table.getColumnByName("two")));
        assertEquals(3, table.getColumnIndex(table.getColumnByName("three")));
        assertEquals(4, table.getColumnIndex(table.getColumnByName("four")));
        assertEquals(5, table.getColumnIndex(table.getColumnByName("six")));
        
        System.out.println("Original: "+original);
        System.out.println("After: "+afterChange);
        
        l.rollBack(afterChange);
        
        assertEquals(original.toString(), afterChange.toString());
        
        // also roll forward original and compare to afterChange
    }
    
    public void testPopulateColumnsCaseSensitive() throws Exception {
        JDBCDataSource ds = new JDBCDataSource(getPLIni());
        ds.setDisplayName("tableWithMixedColumnCase");
        ds.getParentType().setJdbcDriver(MockJDBCDriver.class.getName());
        ds.setUser("fake");
        ds.setPass("fake");
        ds.setUrl("jdbc:mock:" +
                "tables=tab1" +
                "&columns.tab1=this_is_my_column,THIS_IS_MY_COLUMN");
        SQLDatabase db = new SQLDatabase(ds);
        SQLTable t = db.getTableByName("tab1");
        
        // this shouldn't throw a DuplicateColumnException
        assertEquals(2, t.getColumns().size());
    }
    
    public void testPopulateViewIndices() throws Exception {
        JDBCDataSource ds = getJDBCDataSource();
        
        SQLDatabase db = new SQLDatabase(ds);
          
        SQLTable t = db.getTableByName("REGRESSION_TEST1_VIEW");
        
        // Should not throw an ArchitectException
        t.populateColumns();
        t.populateIndices();
    }
    
    /**
     * Utility class that can log events for a tree of SQLObjects, and make snapshots of the
     * state of that tree on demand.  The snapshots can be rolled back using the event log
     * (this is similar functionality to the undo manager, but does not depend on the undo manager
     * in any way), and compared to older snapshots.  Old snapshots can also be rolled forward
     * (like an undo manager redo operation) to test that a stream of events is fully redoable.
     */
    public static class EventLogger extends AbstractPoolingSPListener {

        /**
         * The list of events captured from the SQLObject tree.  Events are stored in the order they
         * are received, oldest first.
         */
        private List<LogItem> log = new ArrayList<LogItem>();

        /**
         * Enumerates the event types for items that are stored in the log.
         */
        private enum LogItemType { INSERT, REMOVE, CHANGE, STRUCTURE_CHANGE }

        /**
         * An item in the log.  It has a type (based on which listener method was invoked) and the
         * event that the listener received.
         */
        private static class LogItem {
            private LogItemType type;
            private EventObject event;
            public LogItem(LogItemType type, EventObject event) {
                this.type = type;
                this.event = event;
            }
            
            public LogItemType getType() {
                return type;
            }
            
            public EventObject getEvent() {
                return event;
            }
            
            @Override
            public String toString() {
                return type+": "+event;
            }
        }
        
        /**
         * Adds an item to the end of the event log.
         *
         * @param type The event type (based on which listener method was called)
         * @param e The event.  Must not be null.
         */
        private void addToLog(LogItemType type, EventObject e) {
            if (e == null) throw new NullPointerException("Can't add null events, dude");
            log.add(new LogItem(type, e));
        }
        
        /**
         * Listener method.  Adds the received event to the log.
         */
        @Override
        public void childAddedImpl(SPChildEvent e) {
            addToLog(LogItemType.INSERT, e);
        }

        /**
         * Listener method.  Adds the received event to the log.
         */
        @Override
        public void childRemovedImpl(SPChildEvent e) {
            addToLog(LogItemType.REMOVE, e);
        }

        /**
         * Listener method.  Adds the received event to the log.
         */
        @Override
        public void propertyChangeImpl(PropertyChangeEvent e) {
            addToLog(LogItemType.CHANGE, e);
            // FIXME have to unlisten to old objects and listen to new ones
        }

        public void transactionStarted(CompoundEvent e) {
            // whatever
        }

        public void transactionEnded(CompoundEvent e) {
            // whatever
        }
        
        /**
         * Holds a snapshot of all property values in a SQLObject instance at one particular point
         * in time. Also allows rolling property change-type events forward and backward.  This is
         * different from the UndoManager in that it does not operate on actual SQLObjects, just
         * snapshots of their properties.
         */
        public static class SQLObjectSnapshot {
            
            /**
             * A collection of properties to ignore when creating a snaphsot of
             * a SQLObject. (These ignored properties interfere with the comparison
             * process which checks if the roll forward/roll back operation reproduced
             * the identical object state).
             */
            final static Map<Class, Set<String>> ignoreProperties;
            
            static {
                ignoreProperties = new HashMap<Class, Set<String>>();
                
                Set<String> set = new HashSet<String>();
                set.add("columns");  // tracked by the snapshot's "children" list
                set.add("children"); // tracked by the snapshot's "children" list, is the same as the columns
                set.add("childrenWithoutPopulating"); // the same as "children" except it does not populate the child list.
                set.add("columnsWithoutPopulating"); // the same as "children" except it does not populate the child list.
                set.add("columnsFolder"); // now equivalent to "columns".
                set.add("pkSize");   // depends on number of columns with non-null PKSeq
                set.add("SQLObjectListeners"); // interferes with EventLogger, which listens to all objects
                set.add("SPListeners"); // interferes with EventLogger, which listens to all objects
                ignoreProperties.put(SQLTable.class, set);

//                set = new HashSet<String>();
//                set.add("children");  // tracked by the snapshot's "children" list
//                set.add("childCount"); // tracked by the snapshot's "children" list
//                set.add("SQLObjectListeners"); // interferes with EventLogger, which listens to all objects
//                ignoreProperties.put(SQLTable.Folder.class, set);

                set = new HashSet<String>();
                set.add("definitelyNullable");  // secondary property depends on nullable
                set.add("primaryKey");          // secondary property depends on position in parent is isInPk
                set.add("SQLObjectListeners"); // interferes with EventLogger, which listens to all objects
                set.add("SPListeners"); // interferes with EventLogger, which listens to all objects
                set.add("foreignKey");         // secondary property depends on position in parent
                set.add("indexed");            // secondary property depends on position in parent
                set.add("uniqueIndexed");      // secondary property depends on position in parent
            	set.add("autoIncrement");      // depends on user defined SQL type
            	set.add("constraintType");     // depends on user defined SQL type
            	set.add("defaultValue");       // depends on user defined SQL type
            	set.add("enumeration");        // depends on user defined SQL type
            	set.add("nullable");           // depends on user defined SQL type
            	set.add("precisionType");      // depends on user defined SQL type
            	set.add("scaleType");          // depends on user defined SQL type
            	set.add("sourceDataTypeName"); // depends on user defined SQL type
            	set.add("type");               // depends on user defined SQL type
                ignoreProperties.put(SQLColumn.class, set);
                
                set = new HashSet<String>();
                set.add("children");  // tracked by the snapshot's "children" list
                set.add("childrenWithoutPopulating");  // tracked by the snapshot's "children" list
                set.add("childCount"); // tracked by the snapshot's "children" list
                set.add("SQLObjectListeners"); // interferes with EventLogger, which listens to all objects
                set.add("SPListeners"); // interferes with EventLogger, which listens to all objects
                ignoreProperties.put(SQLIndex.class, set);
                
                set = new HashSet<String>();
                set.add("SQLObjectListeners"); // interferes with EventLogger, which listens to all objects
                set.add("SPListeners"); // interferes with EventLogger, which listens to all objects
                ignoreProperties.put(SQLIndex.Column.class, set);
                
                set = new HashSet<String>();
                set.add("nullability"); // retrieved from underlying domain or type which is not tested here.
                ignoreProperties.put(UserDefinedSQLType.class, set);

            }
            
            /**
             * The snapshot (BeanUtils.describe format) of the snapshotted object's properties.
             * we use the sortedMap there because we need the key sorted, so the toString() of 
             * the properties will come out in a consistant order.
             */
            private SortedMap<String,Object> properties;
            
            /**
             * Snapshots of the snapshotted object's children at the time of the snapshot.
             */
            private List<SQLObjectSnapshot> children;
            
            /**
             * The snapshotted object's identity hash code (from System.identityHashCode()).
             */
            private int snapshottedObjectIdentity;
            
            /**
             * The class of the snapshotted object.
             */
            private Class snapshottedObjectClass;
            
    		/**
    		 * When rolling back changes, values may be changed to one object and
    		 * then that object may be removed and added at a different index in its
    		 * parent. When this occurs we cannot just make a new snapshot of the
    		 * current object as we changed values in the snapshot we want to keep.
    		 * This list stores the removed snapshots so we can reuse them as we
    		 * roll back. This would not happen in a normal event system as they use
    		 * the actual objects.
    		 */
            private final List<SQLObjectSnapshot> removedSnapshots = new ArrayList<SQLObjectSnapshot>();
            
            public SQLObjectSnapshot(SQLObject object) 
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException, SQLObjectException {
                snapshottedObjectClass = object.getClass();
                snapshottedObjectIdentity = System.identityHashCode(object);
                children = new ArrayList<SQLObjectSnapshot>();
                
                properties = new TreeMap<String, Object>(BeanUtils.describe(object));
                if (ignoreProperties.containsKey(object.getClass())) {
                    for (String propName : ignoreProperties.get(object.getClass())) {
                        properties.remove(propName);
                    }
                }
                
                for (SQLObject c : (List<SQLObject>) object.getChildren()) {
                    children.add(new SQLObjectSnapshot(c));
                }
            }
            
            public void clearRollbackSnapshots() {
            	removedSnapshots.clear();
            }
            
            @Override
            public String toString() {
                StringBuffer sb = new StringBuffer();
                getPropertiesToString(sb,0);
                return sb.toString();
            }
            
            private void getPropertiesToString(StringBuffer buffer, int indent) {
                buffer.append(snapshottedObjectClass.getName());
                buffer.append("@").append(snapshottedObjectIdentity);
                buffer.append(" \"").append(properties.get("name")).append("\" ");
                
                // buffer.append(properties.toString());
                for (String key : properties.keySet()) {
                    buffer.append(key).append("=");
                    if (snapshottedObjectClass.getName().equals("ca.sqlpower.sqlobject.SQLColumn") &&
                            key.equals("SQLObjectListeners")) {
                        buffer.append("xxx");
                    } else {
                        buffer.append(properties.get(key));
                    }
                    buffer.append(" ");
                }
                
                if (children.size() > 0) {
                    buffer.append("\n");
                    appendSpaces(buffer, indent);
                    buffer.append(children.size());
                    buffer.append(" children:");
                    for (SQLObjectSnapshot c : children) {
                        buffer.append("\n");
                        appendSpaces(buffer, indent + 1);
                        c.getPropertiesToString(buffer, indent + 1);
                    }
                }
            }
            
            /**
             * Appends the given number of spaces to the end of the given string buffer.
             */
            private void appendSpaces(StringBuffer sb, int spaces) {
                for (int i = 0; i < spaces; i++) {
                    sb.append(" ");
                }
            }
            

            public void insertChild(int i, SQLObject object, boolean rollForward) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException, SQLObjectException {
                SQLObjectSnapshot snapshot = null;
                for (SQLObjectSnapshot removedSnapshot : removedSnapshots) {
                	if (removedSnapshot.getSnapshottedObjectIdentity() == 
                		System.identityHashCode(object)) {
                		snapshot = removedSnapshot;
                		removedSnapshots.remove(removedSnapshot);
                		break;
                	}
                }
                if (snapshot == null) {
                	snapshot = new SQLObjectSnapshot(object);
                }
                children.add(i,snapshot);
            }

            public void removeChild(int i, SQLObject object, boolean rollForward) {
                SQLObjectSnapshot removed = children.remove(i);
                checkSnapshottedObject(object, removed, "removing child "+i);
                if (!rollForward) {
                	removedSnapshots.add(removed);
                }
            }

            private void checkSnapshottedObject(SQLObject object, SQLObjectSnapshot snapshot, String message) {
                StringBuffer wrongThing = new StringBuffer();
                if (!snapshot.getSnapshottedClass().equals(object.getClass())) {
                    wrongThing.append(" class");
                }
                
                // Skip the identity check for SQLIndex.Column because normalizePrimaryKey
                // recreates the objects every time it is called (which changes their identities)
                if (!snapshot.getSnapshottedClass().equals(SQLIndex.Column.class)) {
                    if (snapshot.getSnapshottedObjectIdentity() != System.identityHashCode(object)) {
                        wrongThing.append(" identity");
                    }
                }
                
                if (wrongThing.length() > 0) {
                    throw new IllegalStateException(
                            "Snapshot "+wrongThing+" mismatch. Expected: " +
                            snapshot.getSnapshottedClass().getName() + "@" + snapshot.getSnapshottedObjectIdentity() +
                            "; actual: " + object.getClass().getName() + "@" + System.identityHashCode(object) +
                            " while " + message);
                }
            }

            private int getSnapshottedObjectIdentity() {
                return snapshottedObjectIdentity;
            }

            private Class getSnapshottedClass() {
                return snapshottedObjectClass;
            }

            public void applyChange(PropertyChangeEvent e) {
                if (!properties.containsKey(e.getPropertyName())) {
                    throw new IllegalStateException("the snapshotted object does not contain this property: " +
                            e.getPropertyName());
                }
                checkSnapshottedObject((SQLObject) e.getSource(), this, "applying a property modification");
                properties.put(e.getPropertyName(),e.getNewValue());                
            }

            public void revertChange(PropertyChangeEvent e) {
                if (!properties.containsKey(e.getPropertyName())) {
                    throw new IllegalStateException("this snapshotted object does not contain property: " +
                            e.getPropertyName());
                }
                checkSnapshottedObject((SQLObject) e.getSource(), this, "reversing a property midification");
                properties.put(e.getPropertyName(), e.getOldValue());
            }

            /**
             * Applies the given SQLObjectEvent to this snapshot, or the appropriate
             * descendant snapshot object.  If the appropriate snapshot is not a descendant
             * of this snapshot, no changes will be applied, and the return value of this
             * method will be false.  Otherwise, the change will be applied and this method
             * will return true.
             * 
             * @param type The event type
             * @param e The event itself
             * @param rollForward Controls whether this event is applied as a "roll-forward" event
             * (like a redo operation would do), or a roll-back (like an undo).
             * @return True if the snapshot tree rooted at this snapshot contains a snapshot
             * of the SQLObject e.getSource(); false if it does not.
             */
            public boolean applyEvent(LogItemType type, EventObject e, boolean rollForward) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException, SQLObjectException {
                if (System.identityHashCode(e.getSource()) != getSnapshottedObjectIdentity()) {
                    for (SQLObjectSnapshot snap : children) {
                        if (snap.applyEvent(type, e, rollForward)) return true;
                    }
                    return false;
                } else {
                    if (type == LogItemType.STRUCTURE_CHANGE) {
                        throw new UnsupportedOperationException("Structure changes are not repeatable");
                    }
                    if (rollForward) {
                        System.out.println("Rolling forward a "+type+": "+e);
                        if (type == LogItemType.INSERT) {
                        	SPChildEvent event = (SPChildEvent) e;
                        	insertChild(event.getIndex(), (SQLObject) event.getChild(), rollForward);
                        	
                        } else if (type == LogItemType.REMOVE) {
                        	SPChildEvent event = (SPChildEvent) e;
                        	removeChild(event.getIndex(), (SQLObject) event.getChild(), rollForward);
                        	
                        } else if (type == LogItemType.CHANGE) {
                            applyChange((PropertyChangeEvent) e);
                        } else {
                            throw new UnsupportedOperationException("Unknown log item type "+type);
                        }
                    } else {
                        System.out.println("Rolling back a "+type+": "+e);
                        if (type == LogItemType.INSERT) {
                        	SPChildEvent event = (SPChildEvent) e;
                        	removeChild(event.getIndex(), (SQLObject) event.getChild(), rollForward);
                        	
                        } else if (type == LogItemType.REMOVE) {
                        	SPChildEvent event = (SPChildEvent) e;
                        	insertChild(event.getIndex(), (SQLObject) event.getChild(), rollForward);
                        	
                        } else if (type == LogItemType.CHANGE) {
                            revertChange((PropertyChangeEvent) e);
                        } else {
                            throw new UnsupportedOperationException("Unknown log item type "+type);
                        }
                    }
                    return true;
                }
            }
        }

        public SQLObjectSnapshot makeSQLObjectSnapshot(SQLTable t) throws Exception {
            return new SQLObjectSnapshot(t);
        }
        
        /**
         * Applies all the changes in this log to the given snapshot.
         * @param snapshot
         */
        public void rollForward(SQLObjectSnapshot snapshot) throws Exception {
            for (LogItem li : log) {
                LogItemType type = li.getType();
                EventObject e = li.getEvent();
                snapshot.applyEvent(type, e, true);
            }
        }
        
        /**
         * Reverts all the changes in this log on the given snapshot, in reverse order.
         * @param snapshot
         */
        public void rollBack(SQLObjectSnapshot snapshot) throws Exception {
        	snapshot.clearRollbackSnapshots();
            List<LogItem> revlog = new ArrayList(log);
            Collections.reverse(revlog);
            for (LogItem li : revlog) {
                LogItemType type = li.getType();
                EventObject e = li.getEvent();
                snapshot.applyEvent(type, e, false);
            }
        }
        
        @Override
        public String toString() {
            StringBuffer sb = new StringBuffer();
            for (LogItem li : log) {
                sb.append(li.getType()).append(": ").append(li.getEvent()).append("\n");
            }
            return sb.toString();
        }
    }

	/**
	 * Tests for a regression case where populating a table's exported keys,
	 * where that would cause recursive calls to populate other tables. Ideally,
	 * only one connection should ever be opened. This test was originally
	 * written for TestFolder, but the Folder class no longer exists.
	 */
	public void testPopulateActiveConnections() throws Exception{
	    SQLDatabase db = getDb();
	    assertEquals(0, db.getMaxActiveConnections());
        SQLTable t = db.getTableByName("SQL_TABLE_POPULATE_TEST");
        t.getExportedKeys();
	    assertEquals(1, db.getMaxActiveConnections());
	}

    /**
     * This test was intended to be a regression test for bug 1640.
     * Unfortunately, it doesn't fail when the problem described in bug 1640 can
     * be reproduced manually. We've committed this test because it's still
     * useful in testing an important part of the SQLObjectListener contract.
     * <p>
     * A failure in this test would happen when the InsertListener gets
     * notified of a child insertion at an index that no longer exists in
     * the parent, or that the new child is no longer at the index the event
     * says it was inserted at. In either case, this would likely happen
     * only because another listener (i.e. RelationshipManager) has modified
     * the child list before the other listener(s) was/were notified.
     */
    public void testInsertEventsConsistentWithReality() throws Exception {
        
        class InsertListener extends AbstractPoolingSPListener {

        	@Override
            public void childAddedImpl(SPChildEvent e) {
        		if (e.getChildType() == SQLColumn.class) {
        			try {
        				assertTrue(e.getIndex() < table.getColumns().size());
        				assertSame(table.getColumn(e.getIndex()), e.getChild());
        			} catch (SQLObjectException ex) {
        				throw new SQLObjectRuntimeException(ex);
        			}
        		}
        	}

        }
        table.addSPListener(new InsertListener());
        SQLRelationship selfref = new SQLRelationship();
        table.addSPListener(new InsertListener());
        selfref.attachRelationship(table, table, true);
        assertEquals(9, table.getColumns().size());
        assertEquals(3, selfref.getChildCount());

        table.addSPListener(new InsertListener());
        table.changeColumnIndex(2, 0, true);
        assertEquals(9, table.getColumns().size());
        assertEquals(3, selfref.getChildCount());
        
        System.out.println(table.getSPListeners());
    }

	/**
	 * This tests inheriting a column at the end of the primary key list and
	 * defining the inherited column to be a primary key does indeed set the
	 * column to be a member of the primary key.
	 */
    public void testInheritDefinesPK() throws Exception {
    	SQLTable t2 = new SQLTable(table.getParentDatabase(), true);
		t2.setName("Another Test Table");
		SQLColumn newcol = new SQLColumn(t2, "newcol", Types.INTEGER, 10, 0);
		t2.addColumn(newcol, 0);
		t2.addToPK(newcol);
		assertTrue("Column should start in primary key", newcol.isPrimaryKey());
		
		List<SQLColumn> columns = new ArrayList<SQLColumn>(table.getColumns());
		
		//Defining the column to be a primary key
		table.inherit(table.getPkSize(), newcol, true, TransferStyles.COPY, true);
		
		List<SQLColumn> newColumns = new ArrayList<SQLColumn>(table.getColumns());
		newColumns.removeAll(columns);

		assertEquals(1, newColumns.size());
		SQLColumn copyCol = newColumns.get(0);
		assertTrue(copyCol.isPrimaryKey());
	}

	/**
	 * Tests if the first column in the primary key is moved to be the last
	 * column in the primary key it actually becomes the last column in the
	 * primary key and not the first column not inside the primary key.
	 */
    public void testFirstPKMovedToLastPK() throws Exception {
    	SQLColumn col = table.getColumn(0);
    	assertEquals(3, table.getPkSize());
		assertTrue(col.isPrimaryKey());
		
		table.changeColumnIndex(0, 2, true);
		
		assertTrue(col.isPrimaryKey());
		Column wrapperForCol = null;
		for (Column wrapper : table.getPrimaryKeyIndex().getChildren(Column.class)) {
			if (wrapper.getColumn() == col) {
				wrapperForCol = wrapper;
				break;
			}
		}
		assertEquals(2, table.getPrimaryKeyIndex().getChildren().indexOf(wrapperForCol));
		assertEquals(3, table.getPkSize());
	}

    /**
     * This tests that a physical name of a table will be updated to match its
     * name correctly if they both match when they change.
     */
    public void testNamesInSync() throws Exception {
        SQLTable table = new SQLTable();
        
        assertEquals(table.getName(), table.getPhysicalName());
        
        //test changing the name updates physical and primary key names
        table.setName("Testing1");
        assertEquals("Testing1", table.getPhysicalName());
        assertTrue(table.getPrimaryKeyIndex().getName().startsWith("Testing1"));
        
        //you should still be able to change the physical name independent of the logical.
        table.setPhysicalName("something else");
        assertEquals("Testing1", table.getName());
        assertEquals("something else", table.getPhysicalName());
        assertTrue(table.getPrimaryKeyIndex().getName().startsWith("something else"));
        
        //when the names are not the same, changing the logical will leave the physical alone
        table.setName("Test2");
        assertEquals("something else", table.getPhysicalName());
        
        //if the names get back to be the same the logical should update the physical again.
        table.setName("something else");
        table.setName("Trial");
        assertEquals("Trial", table.getPhysicalName());
        assertTrue(table.getPrimaryKeyIndex().getName().startsWith("Trial"));
    }
}