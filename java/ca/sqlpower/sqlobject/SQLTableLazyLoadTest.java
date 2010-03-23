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

/**
 * A collection of tests to ensure the lazy loading is in fact lazy.
 * This is different from the normal SQLTable test, where the test object
 * is already marked as populated when it's first set up.
 */
public class SQLTableLazyLoadTest extends TestCase {

    private static final Logger logger = Logger.getLogger(SQLTableLazyLoadTest.class);
    private SQLDatabase db;
    private SQLTable table;
    
    
    /**
     * Sets up a MockJDBC database with one table in it called "table".
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
        ds.setUrl("jdbc:mock:name=refresh_test&tables=table");
        ds.setUser("");
        ds.setPass("");
        db = new SQLDatabase(ds);
        db.getConnection().close(); // just make sure the connection gets made
        table = db.getTableByName("table");
    }

    public void testNotInitiallyPopulated() throws Exception {
        assertFalse(table.isColumnsPopulated());
        assertFalse(table.isIndicesPopulated());
        assertFalse(table.isRelationshipsPopulated());
    }
    
    public void testSetPhysicalNameDoesNotPopulateColumns() {
        table.setPhysicalName("MY_TABLE");
        assertFalse(table.isColumnsPopulated());
        assertFalse(table.isIndicesPopulated());
        assertFalse(table.isRelationshipsPopulated());
    }
    
}
