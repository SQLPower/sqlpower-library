/*
 * Copyright (c) 2008, SQL Power Group Inc.
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

package ca.sqlpower.util.reservoir;

import java.sql.ResultSet;
import java.util.Properties;

import junit.framework.TestCase;
import ca.sqlpower.testutil.MockJDBCConnection;
import ca.sqlpower.testutil.MockJDBCDriver;
import ca.sqlpower.testutil.MockJDBCResultSet;

public class JDBCReservoirDataSourceTest extends TestCase {

    private MockJDBCConnection con;
    
    @Override
    protected void setUp() throws Exception {
        MockJDBCResultSet rs = new MockJDBCResultSet(2);
        rs.addRow(new Object[] { 1, "one" });
        rs.addRow(new Object[] { 2, "two" });
        rs.addRow(new Object[] { 3, "three" });
        rs.addRow(new Object[] { 4, "four" });
        rs.addRow(new Object[] { 5, "five" });
        rs.addRow(new Object[] { 6, "six" });
        rs.addRow(new Object[] { 7, "seven" });
        
        MockJDBCDriver driver = new MockJDBCDriver();
        con = (MockJDBCConnection) driver.connect("jdbc:mock:tables=seven_rows", new Properties());
        
        con.registerResultSet("select \\* from seven_rows", rs);
    }
    
    @Override
    protected void tearDown() throws Exception {
        con.close();
    }
    
    public void testBasic() throws Exception {
        JDBCReserviorDataSource ds = new JDBCReserviorDataSource(con, "select * from seven_rows");
        assertTrue(ds.hasNext());
        assertNotNull(ds.readNextRecord());
    }

    public void testReadAllRows() throws Exception {
        JDBCReserviorDataSource ds = new JDBCReserviorDataSource(con, "select * from seven_rows");
        int rowNum = 0;
        while (ds.hasNext()) {
            rowNum++;
            Object[] row = ds.readNextRecord();
            assertEquals(rowNum, row[0]);
        }
        assertEquals(7, rowNum);
    }
    
    /**
     * For result streaming to work with the PostgreSQL driver, the following preconditions
     * must be met:
     * <ul>
     *  <li>AutoCommit is off
     *  <li>The Statement's fetchSize must be small (we ask for 1 row at a time)
     * </ul>
     */
    public void testPostgreSQLStreaming() throws Exception {
        JDBCReserviorDataSource ds = new JDBCReserviorDataSource(con, "select * from seven_rows");
        assertFalse(con.getAutoCommit());
        assertEquals(1, ds.getStatement().getFetchSize());
        assertEquals(ResultSet.TYPE_SCROLL_INSENSITIVE, ds.getStatement().getFetchDirection());
    }
    
    public void testRowCountMixReadAndSkip() throws Exception {
        JDBCReserviorDataSource ds = new JDBCReserviorDataSource(con, "select * from seven_rows");
        assertEquals(0, ds.getRowCount());
        ds.readNextRecord();
        assertEquals(1, ds.getRowCount());
        ds.skipRecords(3);
        assertEquals(4, ds.getRowCount());
        while (ds.hasNext()) {
            ds.readNextRecord();
        }
        assertEquals(7, ds.getRowCount());
    }
    
    public void testRowCountSkipTooMany() throws Exception {
        JDBCReserviorDataSource ds = new JDBCReserviorDataSource(con, "select * from seven_rows");
        ds.skipRecords(10);
        assertEquals(7, ds.getRowCount());
    }
}
