/*
 * Copyright (c) 2009, SQL Power Group Inc.
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

package ca.sqlpower.sqlobject;

import junit.framework.TestCase;

import org.apache.log4j.Logger;

import ca.sqlpower.sql.DataSourceCollection;
import ca.sqlpower.sql.PlDotIni;
import ca.sqlpower.sql.SPDataSource;
import ca.sqlpower.sql.SPDataSourceType;
import ca.sqlpower.testutil.MockJDBCConnection;
import ca.sqlpower.testutil.MockJDBCDriver;

/**
 * Tests all the permutations of refreshing table containers where tables
 * could be found as children of SQLDatabase, SQLCatalog, or SQLSchema objects.
 */
public class RefreshTablesTest extends TestCase {

    private static final Logger logger = Logger.getLogger(RefreshTablesTest.class);
    
    private SQLDatabase db;

    /**
     * The connection that backs db. Stored here because db will give you one that's
     * wrapped by the connection pool, and you need the actual MockJDBCConnection in
     * order to manipulate the data structures.
     */
    private MockJDBCConnection con;
    
    /**
     * Sets up a MockJDBC database with nothing in it. Individual tests can specify
     * the structure as required.
     */
    @Override
    protected void setUp() throws Exception {
        logger.debug("=====setUp=====");
        super.setUp();
        
        DataSourceCollection dscol = new PlDotIni();
        SPDataSourceType dstype = new SPDataSourceType();
        dstype.setJdbcDriver("ca.sqlpower.testutil.MockJDBCDriver");
        SPDataSource ds = new SPDataSource(dscol);
        ds.setParentType(dstype);
        ds.setUrl("jdbc:mock:name=refresh_test");
        ds.setUser("");
        ds.setPass("");
        db = new SQLDatabase(ds);
        db.getConnection().close(); // just make sure the connection gets made
        con = MockJDBCDriver.getConnection("refresh_test");
        assertNotNull(con);
    }

    
    // ---------------- DATABASE ONLY SECTION ----------------------
    
    /**
     * this one is tricky, because it's hard to know if something is really a table container when it's got no children
     */
    public void testAddTableInEmptyDatabase() throws Exception {
        assertEquals(0, db.getChildCount());
        con.setProperty("tables", "cows");
        
        db.refresh();
        
        assertEquals(1, db.getChildCount());
        assertEquals(SQLTable.class, db.getChild(0).getClass());
        assertEquals("cows", db.getChild(0).getName());
    }

    public void testAddTableInNonEmptyDatabase() throws Exception {
        con.setProperty("tables", "cows");
        assertEquals(1, db.getChildCount());
        
        con.setProperty("tables", "cows,chickens");
        db.refresh();
        
        assertEquals(2, db.getChildCount());
        assertEquals(SQLTable.class, db.getChild(0).getClass());
        assertEquals("cows", db.getChild(0).getName());
        assertEquals("chickens", db.getChild(1).getName());
    }
    
    public void testRemoveTableInDatabase() throws Exception {
        con.setProperty("tables", "cows,chickens");
        assertEquals(2, db.getChildCount());
        
        con.setProperty("tables", "cows");
        db.refresh();
        
        assertEquals(1, db.getChildCount());
        assertEquals(SQLTable.class, db.getChild(0).getClass());
        assertEquals("cows", db.getChild(0).getName());
    }

    public void testRemoveLastTableInDatabase() throws Exception {
        con.setProperty("tables", "cows");
        assertEquals(1, db.getChildCount());
        
        con.setProperty("tables", "");
        db.refresh();
        
        assertEquals("Unexpected tables in database: " + db.getChildNames(),
                0, db.getChildCount());
    }

    
    // ---------------- DATABASE.CATALOG SECTION ----------------------
    
    /**
     * this one is tricky, because it's hard to know if something is really a table container when it's got no children
     */
    public void testAddTableInEmptyCatalog() throws Exception {
        con.setProperty("dbmd.catalogTerm", "Catalog");
        con.setProperty("catalogs", "moo");
        SQLCatalog cat = (SQLCatalog) db.getChildByName("moo");
        assertNotNull("Didn't find catalog in db: "+db.getChildNames(), cat);
        
        assertEquals(0, cat.getChildCount());
        con.setProperty("tables.moo", "cows");
        
        db.refresh();
        
        assertEquals(1, cat.getChildCount());
        assertEquals(SQLTable.class, cat.getChild(0).getClass());
        assertEquals("cows", cat.getChild(0).getName());
    }

    public void testAddTableInNonEmptyCatalog() throws Exception {
        con.setProperty("dbmd.catalogTerm", "Catalog");
        con.setProperty("catalogs", "moo");
        SQLCatalog cat = (SQLCatalog) db.getChildByName("moo");
        assertNotNull("Didn't find catalog in db: "+db.getChildNames(), cat);

        con.setProperty("tables.moo", "cows");
        assertEquals(1, cat.getChildCount());
        
        con.setProperty("tables.moo", "cows,chickens");
        db.refresh();
        
        assertEquals(2, cat.getChildCount());
        assertEquals(SQLTable.class, cat.getChild(0).getClass());
        assertEquals("cows", cat.getChild(0).getName());
        assertEquals("chickens", cat.getChild(1).getName());
    }
    
    public void testRemoveTableInCatalog() throws Exception {
        con.setProperty("dbmd.catalogTerm", "Catalog");
        con.setProperty("catalogs", "moo");
        SQLCatalog cat = (SQLCatalog) db.getChildByName("moo");
        assertNotNull("Didn't find catalog in db: "+db.getChildNames(), cat);

        con.setProperty("tables.moo", "cows,chickens");
        assertEquals(2, cat.getChildCount());
        
        con.setProperty("tables.moo", "cows");
        db.refresh();
        
        assertEquals(1, cat.getChildCount());
        assertEquals(SQLTable.class, cat.getChild(0).getClass());
        assertEquals("cows", cat.getChild(0).getName());
    }

    public void testRemoveLastTableInCatalog() throws Exception {
        con.setProperty("dbmd.catalogTerm", "Catalog");
        con.setProperty("catalogs", "moo");
        SQLCatalog cat = (SQLCatalog) db.getChildByName("moo");
        assertNotNull("Didn't find catalog in db: "+db.getChildNames(), cat);

        con.setProperty("tables.moo", "cows");
        assertEquals(1, cat.getChildCount());
        
        con.setProperty("tables.moo", "");
        db.refresh();
        
        assertEquals("Unexpected tables in database: " + cat.getChildNames(),
                0, cat.getChildCount());
    }
    
    public void testAddCatalogInDatabase() throws Exception {
        con.setProperty("dbmd.catalogTerm", "Catalog");
        con.setProperty("catalogs", "moo");
        SQLCatalog cat = (SQLCatalog) db.getChildByName("moo");
        assertNotNull("Didn't find catalog in db: "+db.getChildNames(), cat);
        assertEquals(1, db.getChildCount());
        
        con.setProperty("catalogs", "moo,cluck");
        db.refresh();
        
        assertEquals(2, db.getChildCount());
        assertEquals("moo", db.getChild(0).getName());
        assertEquals("cluck", db.getChild(1).getName());
    }

    public void testAddCatalogInEmptyDatabase() throws Exception {
        con.setProperty("dbmd.catalogTerm", "Catalog");
        con.setProperty("catalogs", "");
        assertEquals("Unexpected catalogs: " + db.getChildNames(),
                0, db.getChildCount());
        
        con.setProperty("catalogs", "cows");
        db.refresh();
        
        assertEquals(1, db.getChildCount());
        assertEquals("cows", db.getChild(0).getName());
    }

    public void testRemoveCatalogInDatabase() throws Exception {
        con.setProperty("dbmd.catalogTerm", "Catalog");
        con.setProperty("catalogs", "moo,splorch");
        assertEquals(2, db.getChildCount());
        
        con.setProperty("catalogs", "splorch");
        db.refresh();
        
        assertEquals(1, db.getChildCount());
        assertEquals("splorch", db.getChild(0).getName());
    }

    public void testRemoveOnlyCatalogInDatabase() throws Exception {
        con.setProperty("dbmd.catalogTerm", "Catalog");
        con.setProperty("catalogs", "moo");
        assertEquals(1, db.getChildCount());
        
        con.setProperty("catalogs", "");
        db.refresh();
        
        assertEquals(0, db.getChildCount());
    }

    
    // ---------------- DATABASE.SCHEMA SECTION ----------------------
    
    public void testAddTableInEmptySchema() throws Exception {
        con.setProperty("dbmd.schemaTerm", "Schema");
        con.setProperty("schemas", "moo");
        SQLSchema schema = (SQLSchema) db.getChildByName("moo");
        assertNotNull("Didn't find schema in db: "+db.getChildNames(), schema);
        
        assertEquals(0, schema.getChildCount());
        con.setProperty("tables.moo", "cows");
        
        db.refresh();
        
        assertEquals(1, schema.getChildCount());
        assertEquals(SQLTable.class, schema.getChild(0).getClass());
        assertEquals("cows", schema.getChild(0).getName());
    }

    public void testAddTableInNonEmptySchema() throws Exception {
        con.setProperty("dbmd.schemaTerm", "Schema");
        con.setProperty("schemas", "moo");
        SQLSchema schema = (SQLSchema) db.getChildByName("moo");
        assertNotNull("Didn't find schema in db: "+db.getChildNames(), schema);

        con.setProperty("tables.moo", "cows");
        assertEquals(1, schema.getChildCount());
        
        con.setProperty("tables.moo", "cows,chickens");
        db.refresh();
        
        assertEquals(2, schema.getChildCount());
        assertEquals(SQLTable.class, schema.getChild(0).getClass());
        assertEquals("cows", schema.getChild(0).getName());
        assertEquals("chickens", schema.getChild(1).getName());
    }
    
    public void testRemoveTableInSchema() throws Exception {
        con.setProperty("dbmd.schemaTerm", "Schema");
        con.setProperty("schemas", "moo");
        SQLSchema schema = (SQLSchema) db.getChildByName("moo");
        assertNotNull("Didn't find schema in db: "+db.getChildNames(), schema);

        con.setProperty("tables.moo", "cows,chickens");
        assertEquals(2, schema.getChildCount());
        
        con.setProperty("tables.moo", "cows");
        db.refresh();
        
        assertEquals(1, schema.getChildCount());
        assertEquals(SQLTable.class, schema.getChild(0).getClass());
        assertEquals("cows", schema.getChild(0).getName());
    }

    public void testRemoveLastTableInSchema() throws Exception {
        con.setProperty("dbmd.schemaTerm", "Schema");
        con.setProperty("schemas", "moo");
        SQLSchema schema = (SQLSchema) db.getChildByName("moo");
        assertNotNull("Didn't find schema in db: "+db.getChildNames(), schema);

        con.setProperty("tables.moo", "cows");
        assertEquals(1, schema.getChildCount());
        
        con.setProperty("tables.moo", "");
        db.refresh();
        
        assertEquals("Unexpected tables in database: " + schema.getChildNames(),
                0, schema.getChildCount());
    }
    
    public void testAddSchemaInDatabase() throws Exception {
        con.setProperty("dbmd.schemaTerm", "Schema");
        con.setProperty("schemas", "moo");
        SQLSchema schema = (SQLSchema) db.getChildByName("moo");
        assertNotNull("Didn't find schema in db: "+db.getChildNames(), schema);
        assertEquals(1, db.getChildCount());
        
        con.setProperty("schemas", "moo,cluck");
        db.refresh();
        
        assertEquals(2, db.getChildCount());
        assertEquals("moo", db.getChild(0).getName());
        assertEquals("cluck", db.getChild(1).getName());
    }

    public void testAddSchemaInEmptyDatabase() throws Exception {
        con.setProperty("dbmd.schemaTerm", "Schema");
        con.setProperty("schemas", "");
        assertEquals("Unexpected schemas: " + db.getChildNames(),
                0, db.getChildCount());
        
        con.setProperty("schemas", "cows");
        db.refresh();
        
        assertEquals(1, db.getChildCount());
        assertEquals("cows", db.getChild(0).getName());
    }

    public void testRemoveSchemaInDatabase() throws Exception {
        con.setProperty("dbmd.schemaTerm", "Schema");
        con.setProperty("schemas", "moo,splorch");
        assertEquals(2, db.getChildCount());
        
        con.setProperty("schemas", "splorch");
        db.refresh();
        
        assertEquals(1, db.getChildCount());
        assertEquals("splorch", db.getChild(0).getName());
    }

    public void testRemoveOnlySchemaInDatabase() throws Exception {
        con.setProperty("dbmd.schemaTerm", "Schema");
        con.setProperty("schemas", "moo");
        assertEquals(1, db.getChildCount());
        
        con.setProperty("schemas", "");
        db.refresh();
        
        assertEquals(0, db.getChildCount());
    }

}
