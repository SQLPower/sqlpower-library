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
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import ca.sqlpower.object.ObjectDependentException;
import ca.sqlpower.sql.JDBCDataSource;
import ca.sqlpower.sql.JDBCDataSourceType;
import ca.sqlpower.testutil.MockJDBCDriver;

public class TestSQLDatabase extends BaseSQLObjectTestCase {
	
	public TestSQLDatabase(String name) throws Exception {
		super(name);
	}

	protected void setUp() throws Exception {
		super.setUp();
        sqlx("CREATE TABLE REGRESSION_TEST1 (t1_c1 numeric(10))");
        sqlx("CREATE TABLE REGRESSION_TEST2 (t2_c1 char(10))");
	}
	
	@Override
	protected SQLObject getSQLObjectUnderTest() {
		return db;
	}
	
	@Override
    protected Class<?> getChildClassType() {
    	return SQLTable.class;
    }
	
	/*
	 * Test method for 'ca.sqlpower.sqlobject.SQLDatabase.getName()'
	 */
	public void testGetName() {
		SQLDatabase db1 = new SQLDatabase();
		db1.setPlayPenDatabase(true);
		assertEquals("PlayPen Database", db1.getName());
		assertEquals("PlayPen Database", db1.getShortDisplayName());
		assertEquals(db1.getName(), db1.getPhysicalName());
		
		assertEquals(db.getName(), db.getDataSource().getDisplayName());
		assertEquals(db.getDataSource().getDisplayName(), db.getShortDisplayName());
		assertEquals(db.getName(), db.getPhysicalName());
	}
	
	/*
	 * Test method for 'ca.sqlpower.sqlobject.SQLDatabase.getParent()'
	 */
	public void testGetParent() {
		SQLDatabase db1 = new SQLDatabase();
		assertNull(db1.getParent());
		// db's parent is not null, it is StubSQLObject for testing
	}
	
	public void testGoodConnect() throws SQLObjectException, SQLException, IOException {
	    SQLDatabase mydb = new SQLDatabase(getJDBCDataSource());
		assertFalse("db shouldn't have been connected yet", mydb.isConnected());
		Connection con = mydb.getConnection();
		assertNotNull("db gave back a null connection", con);
		assertTrue("db should have said it is connected", mydb.isConnected());
		con.close();
	}

	public void testPopulateExtremelyBasic() throws SQLObjectException, SQLException {
		Connection con = db.getConnection(); // causes db to actually connect
		assertFalse("even though connected, should not be populated yet", db.isPopulated());
		db.populate();
		assertTrue("should be populated now", db.isPopulated());

		db.populate(); // it must be allowed to call populate multiple times
		
		con.close();
	}
    
    public void testPopulateTablesOnly() throws Exception {
        JDBCDataSource ds = new JDBCDataSource(getPLIni());
        ds.setDisplayName("tablesOnly");
        ds.getParentType().setJdbcDriver(MockJDBCDriver.class.getName());
        ds.setUser("fake");
        ds.setPass("fake");
        ds.setUrl("jdbc:mock:tables=tab1");
        SQLDatabase db = new SQLDatabase(ds);
        db.populate();
        assertEquals(1, db.getChildCount());
        assertTrue(db.allowsChildType(SQLTable.class));
        assertEquals(db.getChild(0).getName(), "tab1");
    }

    public void testPopulateSchemasAndTables() throws Exception {
        JDBCDataSource ds = new JDBCDataSource(getPLIni());
        ds.setDisplayName("schemasAndTables");
        ds.getParentType().setJdbcDriver(MockJDBCDriver.class.getName());
        ds.setUser("fake");
        ds.setPass("fake");
        ds.setUrl("jdbc:mock:dbmd.schemaTerm=Schema&schemas=sch1&tables.sch1=tab1");
        SQLDatabase db = new SQLDatabase(ds);
        db.populate();
        assertEquals(1, db.getChildCount());
        assertTrue(db.allowsChildType(SQLSchema.class));
        assertEquals(db.getChild(0).getName(), "sch1");
    }

    public void testPopulateCatalogsAndTables() throws Exception {
        JDBCDataSource ds = new JDBCDataSource(getPLIni());
        ds.setDisplayName("catalogsAndTables");
        ds.getParentType().setJdbcDriver(MockJDBCDriver.class.getName());
        ds.setUser("fake");
        ds.setPass("fake");
        ds.setUrl("jdbc:mock:dbmd.catalogTerm=Catalog&catalogs=cat1&tables.cat1=tab1");
        SQLDatabase db = new SQLDatabase(ds);
        db.populate();
        assertEquals(1, db.getChildCount());
        assertTrue(db.allowsChildType(SQLCatalog.class));
        assertEquals(db.getChild(0).getName(), "cat1");
    }

    public void testPopulateCatalogsSchemasAndTables() throws Exception {
        JDBCDataSource ds = new JDBCDataSource(getPLIni());
        ds.setDisplayName("catalogsSchemasAndTables");
        ds.getParentType().setJdbcDriver(MockJDBCDriver.class.getName());
        ds.setUser("fake");
        ds.setPass("fake");
        ds.setUrl("jdbc:mock:dbmd.catalogTerm=Catalog&dbmd.schemaTerm=Schema" +
                "&catalogs=cat1" +
                "&schemas.cat1=sch1" +
                "&tables.cat1.sch1=tab1");
        SQLDatabase db = new SQLDatabase(ds);
        db.populate();
        assertEquals(1, db.getChildCount());
        assertTrue(db.allowsChildType(SQLCatalog.class));
        assertEquals(db.getChild(0).getName(), "cat1");
    }

	/*
	 * Test method for 'ca.sqlpower.sqlobject.SQLDatabase.allowsChildren()'
	 */
	public void testAllowsChildren() {
		SQLDatabase db1 = new SQLDatabase();
		assertTrue(db1.allowsChildren());
		assertTrue(db.allowsChildren());
	}
	
	/*
	 * Test method for 'ca.sqlpower.sqlobject.SQLDatabase.getTableByName(String)'
	 */
	public void testGetTableByName() throws SQLObjectException {
		SQLTable table1;
		assertNotNull(table1 = db.getTableByName("REGRESSION_TEST1"));
		assertNotNull(db.getTableByName("REGRESSION_TEST2"));
		assertNull("should get null for nonexistant table", db.getTableByName("no_such_table"));
		
		SQLTable table3 = new SQLTable(db, "xyz", "", "TABLE",true);
		SQLCatalog cat1 = null;
		SQLSchema sch1 = null;
		if (db.isCatalogContainer()){
			cat1 = new SQLCatalog(db,"cat1");
			cat1.addChild(table3);
			db.addChild (cat1);
		}		
		else if (db.isSchemaContainer()){
			sch1 = new SQLSchema(db,"sch1",true);
			sch1.addChild(table3);
			db.addChild(sch1);
		}			
		else{
			db.addChild(table3);
		}
			
		table1 =db.getTableByName (table3.getName());
		assertEquals(table1, table3);
		table1 = null;
		if (cat1 != null){
			table1 = db.getTableByName(cat1.getName(), null, table3.getName());			
		}		
		else if (sch1 != null){
			table1 = db.getTableByName(null, sch1.getName(),table3.getName());
		}		
		else{
			table1 = db.getTableByName(null, null,table3.getName());			
		}
		assertNotNull(table1);		
		assertEquals (table1,table3);		
	}
	
	/*
	 * Test method for 'ca.sqlpower.sqlobject.SQLDatabase.getTables()'
	 */
	public void testGetTables() throws SQLObjectException {
		SQLTable table1, table2;
		assertNotNull(table1 = db.getTableByName("REGRESSION_TEST1"));
		assertNotNull(table2 = db.getTableByName("REGRESSION_TEST2"));
		assertNull("should get null for nonexistant table", db.getTableByName("no_such_table"));
		
		SQLTable table3 = new SQLTable(db, "xyz", "", "TABLE",true);
		SQLCatalog cat1 = null;
		SQLSchema sch1 = null;
		if (db.isCatalogContainer()){
			cat1 = new SQLCatalog(db,"cat1");
			cat1.addChild(table3);
			db.addChild (cat1);
		}		
		else if (db.isSchemaContainer()){
			sch1 = new SQLSchema(db,"sch1",true);
			sch1.addChild(table3);
			db.addChild(sch1);
		}			
		else{
			db.addChild(table3);
		}

		List<SQLTable> getTablesTest = db.getTables();
		assertTrue(getTablesTest.contains(table1));
		assertTrue(getTablesTest.contains(table2));
		assertTrue(getTablesTest.contains(table3));					
	}

	/*
	 * Test method for 'ca.sqlpower.sqlobject.SQLDatabase.getDataSource()'
	 */
	public void testGetDataSource() {
		SQLDatabase db1 = new SQLDatabase();
		assertNull (db1.getDataSource());
		JDBCDataSource data = db.getDataSource();
		db1.setDataSource(data);
		assertEquals (db.getDataSource(), db1.getDataSource());
		db1 = new SQLDatabase(data);
		assertEquals (db.getDataSource(), db1.getDataSource());
	}

	/*
	 * Test method for 'ca.sqlpower.sqlobject.SQLDatabase.getSchemaByName(String)'
	 */
	public void testGetSchemaByName() throws Exception {						
		db.populate();
		if (db.isSchemaContainer()){			
			SQLSchema s = new SQLSchema(db,"a schema name should not exist in database",true);
			db.addChild(s);
			db.addChild (new SQLSchema(true));
			db.addChild (new SQLSchema (false));
			assertEquals(db.getSchemaByName("a schema name should not exist in database"),s);
			assertNull(db.getSchemaByName("a schema name should not exist in database xx2"));			
		}
		
		
		SQLDatabase db1 = new SQLDatabase();
		if (db1.isSchemaContainer()){
			SQLSchema s = new SQLSchema(db1,"a schema name should not exist in database",true);
			db1.addChild(s);
			db1.addChild (new SQLSchema(true));
			db1.addChild (new SQLSchema (false));
			assertEquals(db1.getSchemaByName("a schema name should not exist in database"),s);
			assertNull(db1.getSchemaByName("a schema name should not exist in database xx2"));	
		}		
	}
	
	/*
	 * Test method for 'ca.sqlpower.sqlobject.SQLDatabase.getCatalogByName(String)'
	 */
	public void testGetCatalogByName() throws Exception {
		SQLCatalog cat = new SQLCatalog(db,"a catalog name should not exist in database");
		
		try {
			db.populate();
			db.addChild(cat);
			assertEquals(db.getCatalogByName("a catalog name should not exist in database"),cat);
			assertNull(db.getCatalogByName("a catalog name should not exist in database xx2"));
		} catch ( IllegalArgumentException e ) {
			if ( db.isCatalogContainer() ) {
				throw e;
			}					
		}
						
		SQLDatabase db1 = new SQLDatabase();
		if ( db1.isCatalogContainer() ) {
			db1.addChild(cat);
			assertEquals(db1.getCatalogByName("a catalog name should not exist in database"),cat);
			assertNull(db1.getCatalogByName("a catalog name should not exist in database xx2"));
		}				
	}

		
	public void testIsPlayPen() throws SQLObjectException
	{		
		// Cause db to connect
		db.getChild(0);
		
		db.setPlayPenDatabase(true);
		assertTrue(db.isPlayPenDatabase());
		
		db.setDataSource(db.getDataSource());
		assertTrue(db.isPopulated());
		
		db.setPlayPenDatabase(false);
		assertFalse(db.isPlayPenDatabase());
		db.setDataSource(db.getDataSource());
		assertFalse(db.isPopulated());			
	}
	
	/*
	 * Test method for 'ca.sqlpower.sqlobject.SQLDatabase.reset()'
	 */
	public void testReset() throws SQLObjectException {
		SQLDatabase db1 = new SQLDatabase();
		
		db1.setDataSource(db.getDataSource());
		db1.setDataSource(db.getDataSource());
		
		assertFalse(db1.isPopulated());
		assertFalse (db1.isConnected());		
	}
	
	
	/*
	 * Test method for 'ca.sqlpower.sqlobject.SQLObject.getChild(int)'
	 */
	public void testGetChild() throws SQLObjectException, IllegalArgumentException, ObjectDependentException {
		SQLDatabase db1 = new SQLDatabase();
		SQLTable t1 = new SQLTable(db1,"t1","","TABLE",true);
		SQLTable t2 = new SQLTable(db1,"t2","","TABLE",true);
		SQLTable t3 = new SQLTable(db1,"t3","","TABLE",true);
		db1.addChild(t1);
		db1.addChild(t2,1);
		assertEquals (db1.getChild(0), t1);
		assertEquals (db1.getChild(1), t2);
		db1.addChild(t3,0);
		assertEquals (db1.getChild(1), t1);
		assertEquals (db1.getChild(0), t3);
		db1.removeChild(db1.getChild(1));
		assertEquals (db1.getChild(1), t2);
		db1.removeChild(t3);
		assertEquals (db1.getChild(0), t2);
	}

	public void testFireDbChildrenInserted() throws SQLObjectException {
		SQLDatabase db1 = new SQLDatabase();
		TestingSQLObjectListener test1 = new TestingSQLObjectListener();		
		db1.addSPListener(test1);		

		db1.addChild(new SQLCatalog());
		assertEquals("Children inserted event not fired!", 1, test1.getInsertedCount());
	}
	
	public void testFireDbChildrenRemoved() throws SQLObjectException, IllegalArgumentException, ObjectDependentException {
	    SQLDatabase db1 = new SQLDatabase();
	    SQLCatalog tempCatalog = new SQLCatalog();
	    db1.addChild(tempCatalog);

	    TestingSQLObjectListener test1 = new TestingSQLObjectListener();        
        db1.addSPListener(test1);        

        db1.removeChild(tempCatalog);
        assertEquals("Children removed event not fired!", 1, test1.getRemovedCount());
	}

	/*
	 * Test method for 'ca.sqlpower.sqlobject.SQLDatabase.disconnect()'
	 */
	public void testDisconnect() throws SQLObjectException {
		assertNotNull (db.getChild(0));
		assertTrue (db.isConnected());
		db.disconnect();
		assertFalse (db.isConnected());				
	}
	
	public void testReconnect() throws SQLObjectException, SQLException {
		
		// cause db to actually connect
		assertNotNull(db.getChild(0));

		// cause disconnection
		db.setDataSource(db.getDataSource());
		assertFalse("db shouldn't be connected anymore", db.isConnected());
		assertFalse("db shouldn't be populated anymore", db.isPopulated());

		assertNotNull(db.getChild(1));

		assertTrue("db should be repopulated", db.isPopulated());
		assertTrue("db should be reconnected", db.isConnected());
		Connection con = db.getConnection();
		assertNotNull("db should be reconnected", con);
		con.close();
	}

	public void testMissingDriverConnect() throws SQLException {
		JDBCDataSource ds = db.getDataSource();
		ds.getParentType().setJdbcDriver("ca.sqlpower.xxx.does.not.exist");
		
		SQLDatabase mydb = new SQLDatabase(ds);
		Connection con = null;
		SQLObjectException exc = null;
		try {
			assertFalse("db shouldn't have been connected yet", mydb.isConnected());
			con = mydb.getConnection();
		} catch (SQLObjectException e) {
			exc = e;
		}
		assertNotNull("should have got an ArchitectException", exc);
		// XXX: this test should be re-enabled when the product has I18N implemented.
		//assertEquals("error message should have been dbconnect.noDriver", "dbconnect.noDriver", exc.getMessage());
		if (con != null) con.close(); // but we think it's null
		assertNull("connection should be null", con);
	}

	public void testBadURLConnect() throws Exception {
		JDBCDataSource ds = db.getDataSource();
		ds.setUrl("jdbc:bad:moo");
		
		SQLDatabase mydb = new SQLDatabase(ds);
		Connection con = null;
		SQLObjectException exc = null;
		try {
			assertFalse("db shouldn't have been connected yet", db.isConnected());
			con = mydb.getConnection();
		} catch (SQLObjectException e) {
			exc = e;
		}
		assertNotNull("should have got an ArchitectException", exc);
//		XXX: this test should be re-enabled when the product has I18N implemented.
		//assertEquals("error message should have been dbconnect.connectionFailed", "dbconnect.connectionFailed", exc.getMessage());
		if (con != null) con.close();  // con should be null, but if it isn't we have to put it back in the pool.
		assertNull("connection should be null", con);
	}

	public void testBadPasswordConnect() throws SQLException {
		JDBCDataSource ds = db.getDataSource();
		ds.setPass("foofoofoofoofooSDFGHJK");  // XXX: if this is the password, we lose.
		
		SQLDatabase mydb = new SQLDatabase(ds);
		Connection con = null;
		SQLObjectException exc = null;
		try {
			assertFalse("db shouldn't have been connected yet", mydb.isConnected());
			con = mydb.getConnection();
		} catch (SQLObjectException e) {
			exc = e;
		}
		assertNotNull("should have got an ArchitectException", exc);
		// XXX: this test should be re-enabled when the product has I18N implemented.
		// assertEquals("error message should have been dbconnect.connectionFailed", "dbconnect.connectionFailed", exc.getMessage());
		if (con != null) con.close(); // but we hope it's null
		assertNull("connection should be null", con);
	}
	
	public void testPropertyChange() throws Exception
	{
		try {
			PropertyChangeEvent e = new PropertyChangeEvent(null, null,"1", "2");		
			fail("Property change event didn't reject null source;" + e);
		} catch (IllegalArgumentException ile) {
			System.out.println("Caught expected exception.");
		}
		PropertyChangeEvent e = new PropertyChangeEvent(this, null,"1", "2");		
		db.propertyChange(e);
	}
	
	
	public void testUnpopulatedDB(){
		assertFalse(db.isPopulated());
	}

	public void testAutoPopulate() throws Exception {
		assertFalse(db.isPopulated());		
		SQLObject child = db.getChild(0);
		assertTrue(db.isPopulated());
		assertFalse(child.isPopulated());
	}
    
	public void testConnectionPoolFreesResources() throws SQLException, SQLObjectException {
        assertEquals(0,db.getConnectionPool().getNumActive());
        assertEquals(1,db.getConnectionPool().getNumIdle());
	    Connection con1 = db.getConnection();
        Connection con2 = db.getConnection();
        Connection con3 = db.getConnection();
        Connection con4 = db.getConnection();
        Connection con5 = db.getConnection();
        con1.close();
        con2.close();
        con3.close();
        con4.close();
        con5.close();
        con2 = db.getConnection();
        con3 = db.getConnection();
        assertEquals(2,db.getConnectionPool().getNumActive());
        con2.close();
        con3.close();
        
        
        db.disconnect();
     
        assertEquals(0,db.getConnectionPool().getNumActive());
        assertEquals(0,db.getConnectionPool().getNumIdle());
    }
    
	public void testConnectionsPerThreadAreUnique() throws Exception{
		JDBCDataSource ads = new JDBCDataSource(getPLIni());
        ads.setParentType(new JDBCDataSourceType());
		ads.getParentType().setJdbcDriver(MockJDBCDriver.class.getName());
		ads.setUrl("jdbc:mock:dbmd.catalogTerm=Catalog&dbmd.schemaTerm=Schema&catalogs=farm,yard,zoo&schemas.farm=cow,pig&schemas.yard=cat,robin&schemas.zoo=lion,giraffe&tables.farm.cow=moo&tables.farm.pig=oink&tables.yard.cat=meow&tables.yard.robin=tweet&tables.zoo.lion=roar&tables.zoo.giraffe=***,^%%");
		ads.setUser("fake");
		ads.setPass("fake");
		ads.setDisplayName("test");
		db.setDataSource(ads);
		class ConnectionGetter implements Runnable {
			Connection con;
			public void run() {
				try {
					con = db.getConnection();
				} catch (SQLObjectException e) {
					e.printStackTrace();
				}
			}
		}
		
		ConnectionGetter cg1 = new ConnectionGetter();
		Thread t1 = new Thread(cg1);
		ConnectionGetter cg2 = new ConnectionGetter();
		Thread t2 = new Thread(cg2);
		
		t1.start();
		t2.start();
		
		t1.join();
		t2.join();
		
		if (cg1.con == null) fail("cg1 didn't get a connection");
		if (cg2.con == null) fail("cg2 didn't get a connection");
		
		assertNotSame("Both threads got the same connection!", cg1.con, cg2.con);
		
		cg1.con.close();
		cg2.con.close();
	}
	
}
