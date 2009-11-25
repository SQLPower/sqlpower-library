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



public class TestSQLSchema extends BaseSQLObjectTestCase {

	private SQLSchema s;
	
	public TestSQLSchema(String name) throws Exception {
		super(name);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		s = new SQLSchema(true);
	}
	
	@Override
	protected SQLObject getSQLObjectUnderTest() {
		return s;
	}
	
	/*
	 * Test method for 'ca.sqlpower.sqlobject.SQLSchema.getName()'
	 */
	public void testGetName() {
		assertNull(s.getName());
		
		SQLSchema s2 = new SQLSchema(db,"xxx",true);
		assertEquals("xxx",s2.getName());
	}

	/*
	 * Test method for 'ca.sqlpower.sqlobject.SQLSchema.getParent()'
	 */
	public void testGetParent() {
		assertNull(s.getParent());
		
		SQLSchema s2 = new SQLSchema(db,"xxx",true);
		assertEquals(db,s2.getParent());
		
	}

	/*
	 * Test method for 'ca.sqlpower.sqlobject.SQLSchema.populate()'
	 */
	public void testPopulate() throws Exception {
		assertEquals(s.isPopulated(),true);
		s.populate();
		assertEquals(s.isPopulated(),true);
		s.setPopulated(false);
		assertEquals(s.isPopulated(),false);
		s.setPopulated(true);
		assertEquals(s.isPopulated(),true);
		
		
		SQLSchema s2 = new SQLSchema(new SQLDatabase(db.getDataSource()),
									"xx",false);
		assertEquals(s2.isPopulated(),false);
		s2.populate();
		assertEquals(s2.isPopulated(),true);
		s2.setPopulated(false);
		assertEquals(s2.isPopulated(),false);
		s2.setPopulated(true);
		assertEquals(s2.isPopulated(),true);
	}

	/*
	 * Test method for 'ca.sqlpower.sqlobject.SQLSchema.allowsChildren()'
	 */
	public void testAllowsChildren() throws Exception {
		assertEquals(s.allowsChildren(),true);
		
		SQLSchema s2 = new SQLSchema(new SQLDatabase(db.getDataSource()),
									"xx",false);
		assertEquals(s2.allowsChildren(),true);
	}

	/*
	 * Test method for 'ca.sqlpower.sqlobject.SQLSchema.getTableByName(String)'
	 */
	public void testGetTableByName() throws Exception {
		SQLTable t1 = s.getTableByName("REGRESSION_TEST1");
		assertNull(t1);
		
		t1 = new SQLTable();
		t1.setName("xx1");
		s.addChild(t1);
		
		t1 = new SQLTable();
		t1.setName("xx2");
		s.addChild(t1);
		
		t1 = new SQLTable();
		t1.setName("xx3");
		s.addChild(t1);
		
		t1 = new SQLTable();
		t1.setName("xx2");
		s.addChild(t1);
		
		t1 = s.getTableByName("xx1");
		assertNotNull(t1);
		assertEquals(t1.getName(),"xx1");
		
		t1 = s.getTableByName("xx2");
		assertNotNull(t1);
		assertEquals(t1.getName(),"xx2");
		
		t1 = s.getTableByName("xx3");
		assertNotNull(t1);
		assertEquals(t1.getName(),"xx3");
	}

	/*
	 * Test method for 'ca.sqlpower.sqlobject.SQLSchema.isParentTypeDatabase()'
	 */
	public void testIsParentTypeDatabase() {
		assertEquals(s.isParentTypeDatabase(),false);
		
		SQLSchema s2 = new SQLSchema(new SQLDatabase(db.getDataSource()),
									"xx",false);
		assertEquals(s2.isParentTypeDatabase(),true);
	}

	/*
	 * Test method for 'ca.sqlpower.sqlobject.SQLSchema.setSchemaName(String)'
	 */
	public void testSetSchemaName() {
		assertNull(s.getName());
		s.setName("xx23");
		assertEquals(s.getName(),"xx23");
		
		SQLSchema s2 = new SQLSchema(db,"xxx",true);
		s2.setName("xx23");
		assertEquals(s2.getName(),"xx23");
	}

	/*
	 * Test method for 'ca.sqlpower.sqlobject.SQLSchema.getNativeTerm()'
	 */
	public void testGetNativeTerm() {
		assertEquals(s.getNativeTerm(),"schema");
		s.setNativeTerm(null);
		assertNull(s.getNativeTerm());
		s.setNativeTerm("AAA");
		assertEquals(s.getNativeTerm(),"aaa");
		
		SQLSchema s2 = new SQLSchema(db,"xxx",true);
		assertEquals(s2.getNativeTerm(),"schema");
		s2.setNativeTerm(null);
		assertNull(s2.getNativeTerm());
		s2.setNativeTerm("AAA");
		assertEquals(s2.getNativeTerm(),"aaa");
	}

	/*
	 * Test method for 'ca.sqlpower.sqlobject.SQLObject.getChildren()'
	 */
	public void testGetChildren() throws Exception {
		int cnt = 0;
		assertEquals(0,s.getChildren().size());
		assertEquals(cnt,s.getChildCount());
		
		s.addChild(new SQLTable(s,"","","TABLE", true));
		assertEquals(++cnt,s.getChildren().size());
		assertEquals(cnt,s.getChildCount());
		
		s.addChild(new SQLTable(s,"","","TABLE", true));
		assertEquals(++cnt,s.getChildren().size());
		assertEquals(cnt,s.getChildCount());
		
		s.addChild(new SQLTable(s,"","","TABLE", true));
		assertEquals(++cnt,s.getChildren().size());
		assertEquals(cnt,s.getChildCount());
		
		s.addChild(new SQLTable(s,"","","TABLE", true));
		assertEquals(++cnt,s.getChildren().size());
		assertEquals(cnt,s.getChildCount());
		
		s.addChild(new SQLTable(s,"","","TABLE", true));
		assertEquals(++cnt,s.getChildren().size());
		assertEquals(cnt,s.getChildCount());
		
		s.removeChild(0);
		assertEquals(--cnt,s.getChildren().size());
		assertEquals(cnt,s.getChildCount());
		s.removeChild(0);
		assertEquals(--cnt,s.getChildren().size());
		assertEquals(cnt,s.getChildCount());
		s.removeChild(0);
		assertEquals(--cnt,s.getChildren().size());
		assertEquals(cnt,s.getChildCount());
		s.removeChild(0);
		assertEquals(--cnt,s.getChildren().size());
		assertEquals(cnt,s.getChildCount());
	}

	
	/*
	 * Test method for 'ca.sqlpower.sqlobject.SQLObject.getChild(int)'
	 */
	public void testGetChild() throws Exception {
		for ( int i=0; i<5; i++ ) {
			s.addChild(new SQLTable(s,"","","TABLE", true));
		}
		
		assertEquals(5,s.getChildren().size());
		assertEquals(5,s.getChildCount());
		
		SQLTable t = (SQLTable) s.getChild(1);
		assertNotNull(t);
		assertTrue(t instanceof SQLTable);
		assertEquals(t.getName(),"" );
	}

	/*
	 * Test method for 'ca.sqlpower.sqlobject.SQLObject.addChild(int, SQLObject)'
	 */
	public void testAddChildIntSQLObject() throws Exception {
		for ( int i=0; i<5; i++ ) {
			s.addChild(new SQLTable(s,"","","TABLE", true),i);
			assertEquals(i+1,s.getChildren().size());
			assertEquals(i+1,s.getChildCount());
		}
		SQLTable t = new SQLTable(s,"xxx","","TABLE", true);
		s.addChild(t,0);
		assertEquals(6,s.getChildren().size());
		assertEquals(6,s.getChildCount());
		
		s.removeChild(t);
		assertEquals(5,s.getChildren().size());
		assertEquals(5,s.getChildCount());
		
		for ( int i=4; i>=0; i-- ) {
			s.removeChild(0);
			assertEquals(i,s.getChildren().size());
			assertEquals(i,s.getChildCount());
		}
		
		
	}

	/*
	 * Test method for 'ca.sqlpower.sqlobject.SQLObject.addChild(SQLObject)'
	 */
	public void testAddChildSQLObject() throws Exception {
		for ( int i=0; i<5; i++ ) {
			s.addChild(new SQLTable(s,"","","TABLE", true));
			assertEquals(i+1,s.getChildren().size());
			assertEquals(i+1,s.getChildCount());
		}
		
		SQLTable t = new SQLTable(s,"xxx","","TABLE", true);
		s.addChild(t);
		
		for ( int i=5; i>0; i-- ) {
			s.removeChild(0);
			assertEquals(i,s.getChildren().size());
			assertEquals(i,s.getChildCount());
		}
		
		s.removeChild(t);
		assertEquals(0,s.getChildren().size());
		assertEquals(0,s.getChildCount());
	}

	public void testFireDbChildrenInserted() throws Exception {
		TestingSQLObjectListener test1 = new TestingSQLObjectListener();
		s.addSQLObjectListener(test1);
		
		s.addChild(new SQLTable(s,"","","TABLE", true));
		assertEquals("Children inserted event not fired!", 1, test1.getInsertedCount());
	}

	public void testFireDbChildrenRemoved() throws Exception {
	    SQLTable tempTable = new SQLTable(s,"","","TABLE", true);
        s.addChild(tempTable);
	    
	    TestingSQLObjectListener test1 = new TestingSQLObjectListener();
	    s.addSQLObjectListener(test1);

	    s.removeChild(tempTable);
	    assertEquals("Children removed event not fired!", 1, test1.getRemovedCount());
	}
}
