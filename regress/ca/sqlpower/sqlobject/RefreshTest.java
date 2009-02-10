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

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.hsqldb.Types;

import junit.framework.TestCase;
import ca.sqlpower.sql.SPDataSource;

/**
 * This test suite cuts across all the populatable SQLObjects, and sets a number
 * of scenarios for refresh, including where multiple related items change
 * simultaneously.
 */
public class RefreshTest extends TestCase {

    /**
     * Connection to an in-memory HSQLDB. MockJDBC isn't complete enough (no
     * FKs) for testing all types of refresh yet.
     */
    private SPDataSource ds;

    private SQLDatabase db;

    @Override
    protected void setUp() throws Exception {
        
        db = new SQLDatabase(SQLTestCase.getDataSource());
        
        sqlx("CREATE TABLE public.moose (" +
             "\n moose_pk INTEGER NOT NULL," +
             "\n name VARCHAR(10) NOT NULL," +
             "\n antler_length INTEGER NOT NULL," +
             "\n CONSTRAINT moose_pk PRIMARY KEY (moose_pk)" +
             "\n);");
    }
    
    @Override
    protected void tearDown() throws Exception {
        sqlx("SHUTDOWN");
        db.disconnect();
    }
    
    private int sqlx(String sql) throws SQLException, SQLObjectException {
        Connection con = null;
        Statement stmt = null;
        try {
            con = db.getConnection();
            stmt = con.createStatement();
            return stmt.executeUpdate(sql);
        } catch (SQLException ex) {
            System.err.println("Got SQL Exception when executing: " + sql);
            throw ex;
        } finally {
            try {
                if (stmt != null) stmt.close();
            } catch (SQLException ex) {
                System.err.println("Failed to close statement; squishing this exception:");
                ex.printStackTrace();
            }
            try {
                if (con != null) con.close();
            } catch (SQLException ex) {
                System.err.println("Failed to close connection; squishing this exception:");
                ex.printStackTrace();
            }
        }
    }
    
    public void testAddNonPkCol() throws Exception {
        SQLSchema s = db.getSchemaByName("public");
        SQLTable moose = s.getTableByName("moose");
        moose.getColumnsFolder().populate();
        moose.getExportedKeysFolder().populate();
        moose.getIndicesFolder().populate();

        sqlx("ALTER TABLE moose ADD COLUMN tail_length INTEGER");
        db.refresh();
        
        assertEquals("Wrong number of columns ("+moose.getColumnsFolder().getChildNames()+")",
                4, moose.getColumns().size());
        assertEquals("TAIL_LENGTH", moose.getColumns().get(3).getName());
    }
    
    public void testRemoveNonPkCol() throws Exception {
        SQLSchema s = db.getSchemaByName("public");
        SQLTable moose = s.getTableByName("moose");
        moose.getColumnsFolder().populate();
        moose.getExportedKeysFolder().populate();
        moose.getIndicesFolder().populate();
        
        sqlx("ALTER TABLE moose DROP COLUMN antler_length");
        db.refresh();
        
        assertEquals("Wrong number of columns ("+moose.getColumnsFolder().getChildNames()+")",
                2, moose.getColumns().size());
        assertEquals("MOOSE_PK", moose.getColumns().get(0).getName());
        assertEquals("NAME", moose.getColumns().get(1).getName());
    }

    public void testRemoveAllCols() throws Exception {
        SQLSchema s = db.getSchemaByName("public");
        SQLTable moose = s.getTableByName("moose");
        moose.getColumnsFolder().populate();
        moose.getExportedKeysFolder().populate();
        moose.getIndicesFolder().populate();
        
        sqlx("ALTER TABLE moose DROP COLUMN antler_length");
        sqlx("ALTER TABLE moose DROP COLUMN name");
        sqlx("ALTER TABLE moose DROP COLUMN moose_pk");
        db.refresh();
        
        assertEquals("Wrong number of columns ("+moose.getColumnsFolder().getChildNames()+")",
                0, moose.getColumns().size());
        assertEquals("PK should have vanished too", 0, moose.getIndices().size());
    }

    public void testModifyNonPkCol() throws Exception {
        SQLSchema s = db.getSchemaByName("public");
        SQLTable moose = s.getTableByName("moose");
        moose.getColumnsFolder().populate();
        moose.getExportedKeysFolder().populate();
        moose.getIndicesFolder().populate();
        SQLColumn antlerLength = moose.getColumns().get(2);

        assertEquals(Types.INTEGER, antlerLength.getType());
        assertEquals(10, antlerLength.getPrecision());
        assertEquals(0, antlerLength.getScale());

        sqlx("ALTER TABLE moose ALTER COLUMN antler_length NUMERIC(10,2)");
        db.refresh();
        
        assertEquals("Wrong number of columns ("+moose.getColumnsFolder().getChildNames()+")",
                3, moose.getColumns().size());
        assertSame(antlerLength, moose.getColumns().get(2));
        assertEquals("ANTLER_LENGTH", antlerLength.getName());
        assertEquals(Types.NUMERIC, antlerLength.getType());
        assertEquals(10, antlerLength.getPrecision());
        assertEquals(2, antlerLength.getScale());
    }
    
    public void testAddIndex() throws Exception {
        SQLSchema s = db.getSchemaByName("public");
        SQLTable moose = s.getTableByName("moose");
        moose.getColumnsFolder().populate();
        moose.getExportedKeysFolder().populate();
        moose.getIndicesFolder().populate();

        sqlx("CREATE INDEX moose_idx ON moose (name)");
        db.refresh();
        
        assertEquals(1, moose.getIndicesFolder().getChildCount());
        SQLIndex mooseIdx = moose.getIndices().get(0);
        assertEquals("MOOSE_IDX", mooseIdx.getName());
        assertEquals(1, mooseIdx.getChildCount());
        assertEquals("NAME", mooseIdx.getChild(0).getName());
    }
    
}