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
import ca.sqlpower.sql.JDBCDataSource;
import ca.sqlpower.sql.JDBCDataSourceType;
import ca.sqlpower.sql.PlDotIni;
import ca.sqlpower.sql.SPDataSource;
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
        
        DataSourceCollection<SPDataSource> dscol = new PlDotIni();
        JDBCDataSourceType dstype = new JDBCDataSourceType();
        dstype.setJdbcDriver("ca.sqlpower.testutil.MockJDBCDriver");
        JDBCDataSource ds = new JDBCDataSource(dscol);
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
        con.setProperty("tables", "");
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
        SQLCatalog cat = db.getChildByName("moo", SQLCatalog.class);
        assertNotNull("Didn't find catalog in db: "+db.getChildNames(), cat);
        
        con.setProperty("tables.moo", "");
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
        SQLCatalog cat = db.getChildByName("moo", SQLCatalog.class);
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
        SQLCatalog cat = db.getChildByName("moo", SQLCatalog.class);
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
        SQLCatalog cat = db.getChildByName("moo", SQLCatalog.class);
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
        SQLCatalog cat = db.getChildByName("moo", SQLCatalog.class);
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
        SQLSchema schema = db.getChildByName("moo", SQLSchema.class);
        assertNotNull("Didn't find schema in db: "+db.getChildNames(), schema);
        
        con.setProperty("tables.moo", "");
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
        SQLSchema schema = db.getChildByName("moo", SQLSchema.class);
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
        SQLSchema schema = db.getChildByName("moo", SQLSchema.class);
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
        SQLSchema schema = db.getChildByName("moo", SQLSchema.class);
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
        SQLSchema schema = db.getChildByName("moo", SQLSchema.class);
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

    
    // ---------------- DATABASE.CATALOG.SCHEMA SECTION ----------------------

    /**
     * this one is tricky, because it's hard to know if something is really a table container when it's got no children
     */
    public void testAddTableInEmptyCatalogSchema() throws Exception {
        con.setProperty("dbmd.catalogTerm", "Catalog");
        con.setProperty("dbmd.schemaTerm", "Schema");
        con.setProperty("catalogs", "moo");
        con.setProperty("schemas.moo", "cow");
        SQLCatalog cat = db.getChildByName("moo", SQLCatalog.class);
        assertNotNull("Didn't find catalog in db: "+db.getChildNames(), cat);
        
        SQLSchema sch = cat.getChildByName("cow", SQLSchema.class);
        assertNotNull("Didn't find schema in catalog: "+cat.getChildNames(), sch);
        
        con.setProperty("tables.moo.cow", "");
        assertEquals(0, sch.getChildCount());
        con.setProperty("tables.moo.cow", "cows");
        
        db.refresh();
        
        assertEquals(1, sch.getChildCount());
        assertEquals(SQLTable.class, sch.getChild(0).getClass());
        assertEquals("cows", sch.getChild(0).getName());
    }

    public void testAddTableInNonEmptyCatalogSchema() throws Exception {
        con.setProperty("dbmd.catalogTerm", "Catalog");
        con.setProperty("dbmd.schemaTerm", "Schema");
        con.setProperty("catalogs", "moo");
        con.setProperty("schemas.moo", "cow");
        SQLCatalog cat = db.getChildByName("moo", SQLCatalog.class);
        assertNotNull("Didn't find catalog in db: "+db.getChildNames(), cat);

        SQLSchema sch = cat.getChildByName("cow", SQLSchema.class);
        assertNotNull("Didn't find schema in catalog: "+cat.getChildNames(), sch);

        con.setProperty("tables.moo.cow", "cows");
        assertEquals(1, sch.getChildCount());
        
        con.setProperty("tables.moo.cow", "cows,chickens");
        db.refresh();
        
        assertEquals(2, sch.getChildCount());
        assertEquals(SQLTable.class, sch.getChild(0).getClass());
        assertEquals("cows", sch.getChild(0).getName());
        assertEquals("chickens", sch.getChild(1).getName());
    }
    
    public void testRemoveTableInCatalogSchema() throws Exception {
        con.setProperty("dbmd.catalogTerm", "Catalog");
        con.setProperty("dbmd.schemaTerm", "Schema");
        con.setProperty("catalogs", "moo");
        con.setProperty("schemas.moo", "cow");
        SQLCatalog cat = db.getChildByName("moo", SQLCatalog.class);
        assertNotNull("Didn't find catalog in db: "+db.getChildNames(), cat);

        SQLSchema sch = cat.getChildByName("cow", SQLSchema.class);
        assertNotNull("Didn't find schema in catalog: "+cat.getChildNames(), sch);

        con.setProperty("tables.moo.cow", "cows,chickens");
        assertEquals(2, sch.getChildCount());
        
        con.setProperty("tables.moo.cow", "cows");
        db.refresh();
        
        assertEquals(1, sch.getChildCount());
        assertEquals(SQLTable.class, sch.getChild(0).getClass());
        assertEquals("cows", sch.getChild(0).getName());
    }

    public void testRemoveLastTableInCatalogSchema() throws Exception {
        con.setProperty("dbmd.catalogTerm", "Catalog");
        con.setProperty("dbmd.schemaTerm", "Schema");
        con.setProperty("catalogs", "moo");
        con.setProperty("schemas.moo", "cow");
        SQLCatalog cat = db.getChildByName("moo", SQLCatalog.class);
        assertNotNull("Didn't find catalog in db: "+db.getChildNames(), cat);

        SQLSchema sch = cat.getChildByName("cow", SQLSchema.class);
        assertNotNull("Didn't find schema in catalog: "+cat.getChildNames(), sch);

        con.setProperty("tables.moo.cow", "cows");
        assertEquals(1, sch.getChildCount());
        
        con.setProperty("tables.moo.cow", "");
        db.refresh();
        
        assertEquals("Unexpected tables in database: " + sch.getChildNames(),
                0, sch.getChildCount());
    }
    
    public void testAddSchemaContainingCatalogInDatabase() throws Exception {
        con.setProperty("dbmd.catalogTerm", "Catalog");
        con.setProperty("dbmd.schemaTerm", "Schema");
        con.setProperty("catalogs", "moo");
        con.setProperty("schemas.moo", "cow");
        SQLCatalog cat = db.getChildByName("moo", SQLCatalog.class);
        assertNotNull("Didn't find catalog in db: "+db.getChildNames(), cat);
        assertEquals(1, db.getChildCount());
        
        con.setProperty("catalogs", "moo,cluck");
        con.setProperty("schemas.cluck", "duck");
        db.refresh();
        
        assertEquals(2, db.getChildCount());
        assertEquals("moo", db.getChild(0).getName());
        assertEquals("cluck", db.getChild(1).getName());
        assertEquals("duck", db.getChild(1).getChild(0).getName());
    }

    public void testAddSchemaContainingCatalogInEmptyDatabase() throws Exception {
        con.setProperty("dbmd.catalogTerm", "Catalog");
        con.setProperty("dbmd.schemaTerm", "Schema");
        con.setProperty("catalogs", "");
        assertEquals("Unexpected catalogs: " + db.getChildNames(),
                0, db.getChildCount());
        
        con.setProperty("catalogs", "cows");
        con.setProperty("schemas.cows", "moo");
        db.refresh();
        
        assertEquals(1, db.getChildCount());
        assertEquals("cows", db.getChild(0).getName());
        assertEquals("moo", db.getChild(0).getChild(0).getName());
    }

    public void testRemoveSchemaContainingCatalogInDatabase() throws Exception {
        con.setProperty("dbmd.catalogTerm", "Catalog");
        con.setProperty("dbmd.schemaTerm", "Schema");
        con.setProperty("catalogs", "moo,splorch");
        con.setProperty("schemas.moo", "cow");
        con.setProperty("schemas.splorch", "poop");
        assertEquals(2, db.getChildCount());
        
        con.setProperty("catalogs", "splorch");
        db.refresh();
        
        assertEquals(1, db.getChildCount());
        assertEquals("splorch", db.getChild(0).getName());
        assertEquals("poop", db.getChild(0).getChild(0).getName());
    }

    public void testRemoveOnlySchemaContainingCatalogInDatabase() throws Exception {
        con.setProperty("dbmd.catalogTerm", "Catalog");
        con.setProperty("dbmd.schemaTerm", "Schema");
        con.setProperty("catalogs", "moo");
        con.setProperty("schemas.moo", "cow");
        assertEquals(1, db.getChildCount());
        
        con.setProperty("catalogs", "");
        db.refresh();
        
        assertEquals(0, db.getChildCount());
    }

}
