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

import ca.sqlpower.sql.DataSourceCollection;
import ca.sqlpower.sql.PlDotIni;
import ca.sqlpower.sql.SPDataSource;
import ca.sqlpower.sql.SPDataSourceType;
import ca.sqlpower.testutil.MockJDBCConnection;
import ca.sqlpower.testutil.MockJDBCDriver;
import junit.framework.TestCase;

/**
 * Tests all the permutations of refreshing table containers where tables
 * could be found as children of SQLDatabase, SQLCatalog, or SQLSchema objects.
 */
public class RefreshTablesTest extends TestCase {

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
    
    /**
     * XXX this one is tricky, because it's hard to know if something is really a table container when it's got no children
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
}
