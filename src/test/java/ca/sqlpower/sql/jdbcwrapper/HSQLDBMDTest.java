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

package ca.sqlpower.sql.jdbcwrapper;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;

import ca.sqlpower.sqlobject.DatabaseConnectedTestCase;

public class HSQLDBMDTest extends DatabaseConnectedTestCase {

    /**
     * Connection to HSQL database. Opened in setUp, and closed in tearDown.
     */
    private Connection con;
    
    /**
     * The database metadata for db. Gets grabbed from con during setUp.
     */
    private DatabaseMetaData dbmd;
    
    /**
     * A resultset you can use if you want. if it's non-null during tearDown,
     * it will be closed.
     */
    private ResultSet rs;
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        con = db.getConnection();
        dbmd = con.getMetaData();
    }
    
    @Override
    protected void tearDown() throws Exception {
        if (rs != null) rs.close();
        con.close();
        super.tearDown();
    }
    
    public void testFixPkNameSingleColPk() throws Exception {
        sqlx("CREATE TABLE public.moose (" +
                "\n moose_id INTEGER NOT NULL," +
                "\n name VARCHAR(10) NOT NULL," +
                "\n antler_length INTEGER NOT NULL," +
                "\n CONSTRAINT moose_pk PRIMARY KEY (moose_id)" +
                "\n);");
        
        rs = dbmd.getIndexInfo(null, "PUBLIC", "MOOSE", true, true);
        while (rs.next()) {
            assertEquals("MOOSE_PK", rs.getString("INDEX_NAME"));
        }
    }

    /**
     * This test ensures that we don't rename a unique index that happens to
     * have the same columns as the PK index.
     */
    public void testFixPkNameSingleColPkWithExtraUniqueIndex() throws Exception {
        sqlx("CREATE TABLE public.moose (" +
                "\n moose_id INTEGER NOT NULL," +
                "\n name VARCHAR(10) NOT NULL," +
                "\n antler_length INTEGER NOT NULL," +
                "\n CONSTRAINT moose_pk PRIMARY KEY (moose_id)" +
                "\n);");
        
        sqlx("CREATE UNIQUE INDEX moose_uidx ON public.moose (moose_id)");
        
        rs = dbmd.getIndexInfo(null, "PUBLIC", "MOOSE", true, true);
        int moosePkCount = 0;
        int mooseUidxCount = 0;
        while (rs.next()) {
            if (rs.getString("INDEX_NAME").equals("MOOSE_PK")) {
                moosePkCount++;
            } else if (rs.getString("INDEX_NAME").equals("MOOSE_UIDX")) {
                mooseUidxCount++;
            } else {
                fail("Unexpected index name in metadata: " + rs.getString("INDEX_NAME"));
            }
        }
        
        assertEquals(1, moosePkCount);
        assertEquals(1, mooseUidxCount);
    }

    public void testFixPkNameCompoundPk() throws Exception {
        sqlx("CREATE TABLE public.moose (" +
                "\n moose_id1 INTEGER NOT NULL," +
                "\n moose_id2 INTEGER NOT NULL," +
                "\n moose_id3 INTEGER NOT NULL," +
                "\n name VARCHAR(10) NOT NULL," +
                "\n antler_length INTEGER NOT NULL," +
                "\n CONSTRAINT moose_pk PRIMARY KEY (moose_id1, moose_id2, moose_id3)" +
                "\n);");
        
        rs = dbmd.getIndexInfo(null, "PUBLIC", "MOOSE", true, true);
        while (rs.next()) {
            assertEquals("MOOSE_PK", rs.getString("INDEX_NAME"));
        }
    }

    /**
     * This test ensures that we don't rename a unique index that happens to
     * have the same columns as the PK index.
     */
    public void testFixPkNameCompoundPkWithExtraUniqueIndex() throws Exception {
        sqlx("CREATE TABLE public.moose (" +
                "\n moose_id1 INTEGER NOT NULL," +
                "\n moose_id2 INTEGER NOT NULL," +
                "\n moose_id3 INTEGER NOT NULL," +
                "\n name VARCHAR(10) NOT NULL," +
                "\n antler_length INTEGER NOT NULL," +
                "\n CONSTRAINT moose_pk PRIMARY KEY (moose_id1, moose_id2, moose_id3)" +
                "\n);");
        
        sqlx("CREATE UNIQUE INDEX moose_uidx ON public.moose (moose_id1, moose_id2, moose_id3)");
        
        rs = dbmd.getIndexInfo(null, "PUBLIC", "MOOSE", true, true);
        int moosePkCount = 0;
        int mooseUidxCount = 0;
        while (rs.next()) {
            if (rs.getString("INDEX_NAME").equals("MOOSE_PK")) {
                moosePkCount++;
            } else if (rs.getString("INDEX_NAME").equals("MOOSE_UIDX")) {
                mooseUidxCount++;
            } else {
                fail("Unexpected index name in metadata: " + rs.getString("INDEX_NAME"));
            }
        }
        
        assertEquals(3, moosePkCount);
        assertEquals(3, mooseUidxCount);
    }
    
    /**
     * Just a corner case test for tables with no index info at all.
     */
    public void testFixPkNameNoIndexes() throws Exception {
        sqlx("CREATE TABLE public.moose (" +
                "\n moose_id1 INTEGER NOT NULL," +
                "\n moose_id2 INTEGER NOT NULL," +
                "\n moose_id3 INTEGER NOT NULL," +
                "\n name VARCHAR(10) NOT NULL," +
                "\n antler_length INTEGER NOT NULL" +
                "\n);");
        
        rs = dbmd.getIndexInfo(null, "PUBLIC", "MOOSE", true, true);
        int rowcount = 0;
        while (rs.next()) {
            rowcount++;
        }
        
        assertEquals(0, rowcount);
    }

    /**
     * Tests that a table with a named unique index but no PK reports correct values.
     */
    public void testFixPkNamesNamedUidxNoPk() throws Exception {
        sqlx("CREATE TABLE public.moose (" +
                "\n moose_id INTEGER NOT NULL," +
                "\n name VARCHAR(10) NOT NULL," +
                "\n antler_length INTEGER NOT NULL" +
                "\n);");
        
        sqlx("CREATE UNIQUE INDEX moose_uidx ON public.moose (moose_id)");

        rs = dbmd.getIndexInfo(null, "PUBLIC", "MOOSE", true, true);
        int mooseUidxCount = 0;
        while (rs.next()) {
            if (rs.getString("INDEX_NAME").equals("MOOSE_UIDX")) {
                mooseUidxCount++;
            } else {
                fail("Unexpected index name in metadata: " + rs.getString("INDEX_NAME"));
            }
        }
        
        assertEquals(1, mooseUidxCount);
    }

    /**
     * Tests that a table with an unnamed unique index but no PK reports correct values.
     */
    public void testFixPkNamesUnnamedUidxNoPk() throws Exception {
        sqlx("CREATE TABLE public.moose (" +
                "\n moose_id INTEGER NOT NULL," +
                "\n name VARCHAR(10) NOT NULL," +
                "\n antler_length INTEGER NOT NULL" +
                "\n);");
        
        sqlx("ALTER TABLE public.moose ADD UNIQUE (moose_id)");

        rs = dbmd.getIndexInfo(null, "PUBLIC", "MOOSE", true, true);
        int sysIdxCount = 0;
        while (rs.next()) {
            if (rs.getString("INDEX_NAME").startsWith("SYS_IDX")) {
                sysIdxCount++;
            } else {
                fail("Unexpected index name in metadata: " + rs.getString("INDEX_NAME"));
            }
        }
        
        assertEquals(1, sysIdxCount);
    }

}
