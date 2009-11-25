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

import java.util.ArrayList;
import java.util.List;

import ca.sqlpower.sqlobject.SQLTable.Folder;

public class TestFolder extends BaseSQLObjectTestCase {

	public TestFolder(String name) throws Exception {
		super(name);
	}
    
	/**
	 * A table with one primary key column.  Gets set up in setUp().
	 */
	private SQLTable table1pk;

	protected void setUp() throws Exception {		
		super.setUp();
		
		sqlx("CREATE TABLE SQL_COLUMN_TEST_1PK (\n" +
		        " cow numeric(11),\n" +
		        " moo varchar(10),\n" +
		        " foo char(10),\n" +
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

		sqlx("CREATE TABLE SQL_TABLE_POPULATE_TEST (\n" +
		        " cow numeric(10) NOT NULL, \n" +
		        " CONSTRAINT test4pk PRIMARY KEY (cow))");
		sqlx("CREATE TABLE SQL_TABLE_1_POPULATE_TEST (\n" +
		        " cow numeric(10) NOT NULL, \n" +
		        " CONSTRAINT test5pk PRIMARY KEY(cow))");
		sqlx("ALTER TABLE SQL_TABLE_1_POPULATE_TEST " +
		        "ADD CONSTRAINT TEST_FK FOREIGN KEY (cow) " +
		        "REFERENCES SQL_TABLE_POPULATE_TEST (cow)");

		table1pk = getDb().getTableByName("SQL_COLUMN_TEST_1PK");
		getDb().getTableByName("SQL_COLUMN_TEST_0PK");
		getDb().getTableByName("SQL_COLUMN_TEST_3PK");
	}

	/**
	 * Returns a random folder from a random test table.  Right now,
	 * this is table1pk.columns folder.  Whatever.
	 */
	@Override
	protected SQLObject getSQLObjectUnderTest() {
        return table1pk.getColumnsFolder();
	}
	
	/*
	 * Test method for 'ca.sqlpower.sqlobject.SQLTable.Folder.getName()'
	 */
	public void testGetName() {
		Folder f1;
		f1 = new Folder(Folder.COLUMNS, true);
		assertEquals (f1.getName(), "Columns");
		f1 = new Folder(Folder.IMPORTED_KEYS, true);
		assertEquals (f1.getName(), "Imported Keys");
		f1 = new Folder(Folder.EXPORTED_KEYS, true);
		assertEquals (f1.getName(), "Exported Keys");
		f1.setName("xyz");
		assertEquals (f1.getName(), "xyz");
		f1.setName(null);
		assertNull (f1.getName());
	}

	/*
	 * Test method for 'ca.sqlpower.sqlobject.SQLTable.Folder.getParent()'
	 */
	public void testGetParent() throws SQLObjectException {
		Folder f1 = new Folder(Folder.COLUMNS, true);
		Folder f2 = new Folder(Folder.IMPORTED_KEYS, true);
		Folder f3 = new Folder(Folder.EXPORTED_KEYS, true);
		assertNull (f1.getParent());
		SQLTable t1 = new SQLTable();
		t1.addChild(f1);
		t1.addChild(f2);
		t1.addChild(f3);
		assertEquals (f1.getParent(), t1);		
		assertEquals (f2.getParent(), t1);	
		assertEquals (f3.getParent(), t1);
		t1.removeChild(1);
		assertEquals (t1.getChild(1), f3);
		t1.removeChild(f1);
		assertEquals (t1.getChild(0), f3);
		assertEquals (Folder.COLUMNS, f1.getType());
		assertEquals (Folder.IMPORTED_KEYS, f2.getType());
		assertEquals (Folder.EXPORTED_KEYS, f3.getType());
	}


	/**
	 * Tests for a regression case where populating a table's
	 * exported keys folder, where that would cause recursive
	 * calls to populate other tables. Ideally, only one connection
	 * should ever be opened.
	 */
	public void testPopulateActiveConnections() throws Exception{
	    SQLDatabase db = getDb();
	    assertEquals(0, db.getMaxActiveConnections());
        SQLTable t = db.getTableByName("SQL_TABLE_POPULATE_TEST");
        t.getExportedKeys();
	    assertEquals(1, db.getMaxActiveConnections());
	}


	/*
	 * Test method for 'ca.sqlpower.sqlobject.SQLTable.Folder.addChild(int, SQLObject)'
	 */
	public void testAddChild() throws SQLObjectException {
		Folder main1 = new Folder(Folder.COLUMNS,true);
		SQLColumn c1 = new SQLColumn();
		SQLColumn c2 = new SQLColumn();
		SQLColumn c3 = new SQLColumn();
		
		main1.addChild(c1);
		assertEquals (main1.getChild(0), c1);
		main1.addChild(c2);
		assertEquals (main1.getChild(1), c2);
		main1.addChild(c3,1);
		
		List children = new ArrayList(main1.getChildren());
		
		assertTrue (children.contains(c1));
		assertTrue (children.contains(c2));
		assertTrue (children.contains(c3));
		assertEquals (children.size(), 3);
		assertEquals (main1.getChildCount(),3);
				
		assertEquals (main1.getChild(1), c3);	
		main1.removeChild(c3);
		assertEquals(main1.getChild(1), c2);
		main1.removeChild(0);
		assertEquals(main1.getChild(0), c2);
	}

	public void testFireDbChildrenInserted() throws SQLObjectException {
		Folder f1 = new Folder (Folder.COLUMNS, true);

		TestingSQLObjectListener testListener = new TestingSQLObjectListener();
		f1.addSQLObjectListener(testListener);

		SQLColumn tmpCol = new SQLColumn();
		f1.addChild(tmpCol);
		assertEquals("Children inserted event not fired!", 1, testListener.getInsertedCount());
	}

	public void testFireDbChildrenRemoved() throws SQLObjectException {
	    Folder f1 = new Folder (Folder.COLUMNS, true);
	    
        SQLColumn tmpCol = new SQLColumn();
        f1.addChild(tmpCol);
        
        TestingSQLObjectListener testListener = new TestingSQLObjectListener();
        f1.addSQLObjectListener(testListener);
        f1.removeChild(tmpCol);
        
        assertEquals("Children removed event not fired!", 1, testListener.getRemovedCount());
	}
}