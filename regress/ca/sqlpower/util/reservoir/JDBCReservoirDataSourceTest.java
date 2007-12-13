/*
 * Copyright (c) 2007, SQL Power Group Inc.
 * 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in
 *       the documentation and/or other materials provided with the
 *       distribution.
 *     * Neither the name of SQL Power Group Inc. nor the names of its
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package ca.sqlpower.util.reservoir;

import java.sql.Connection;
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
    }
}
