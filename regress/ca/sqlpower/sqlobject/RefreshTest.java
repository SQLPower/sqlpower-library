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

import java.sql.Types;

/**
 * This test suite cuts across all the populatable SQLObjects, and sets a number
 * of scenarios for refresh, including where multiple related items change
 * simultaneously.
 * <p>
 * Refreshing tables with foreign key constraints (SQLRelationship) is not tested
 * in this class because it needs a more complicated setup and I didn't want to
 * make these simpler things overly complex. See {@link RefreshFKTest} for the
 * tests involving refresh with foreign keys.
 */
public class RefreshTest extends DatabaseConnectedTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        
        sqlx("CREATE TABLE public.moose (" +
             "\n moose_pk INTEGER NOT NULL," +
             "\n name VARCHAR(10) NOT NULL," +
             "\n antler_length INTEGER NOT NULL," +
             "\n CONSTRAINT moose_pk PRIMARY KEY (moose_pk)" +
             "\n);");
        
        // Hey you! Yeah, you--the person about to add a new table and a foreign key
        // to this setup! Go to the RefreshFKTest class. It already has the setup
        // you're looking for.
    }
    
    public void testAddNonPkCol() throws Exception {
        SQLSchema s = db.getSchemaByName("public");
        SQLTable moose = s.findTableByName("moose");
        moose.populateColumns();
        moose.populateExportedKeys();
        moose.populateIndices();

        sqlx("ALTER TABLE moose ADD COLUMN tail_length INTEGER");
        db.refresh();
        
        assertEquals("Wrong number of columns ("+moose.getChildNames(SQLColumn.class)+")",
                4, moose.getColumns().size());
        assertEquals("TAIL_LENGTH", moose.getColumns().get(3).getName());
    }
    
    public void testRemoveNonPkCol() throws Exception {
        SQLSchema s = db.getSchemaByName("public");
        SQLTable moose = s.findTableByName("moose");
        moose.populateColumns();
        moose.populateExportedKeys();
        moose.populateIndices();
        
        sqlx("ALTER TABLE moose DROP COLUMN antler_length");
        db.refresh();
        
        assertEquals("Wrong number of columns ("+moose.getChildNames(SQLColumn.class)+")",
                2, moose.getColumns().size());
        assertEquals("MOOSE_PK", moose.getColumns().get(0).getName());
        assertEquals("NAME", moose.getColumns().get(1).getName());
    }

    public void testRemoveAllCols() throws Exception {
        SQLSchema s = db.getSchemaByName("public");
        SQLTable moose = s.findTableByName("moose");
        moose.populateColumns();
        moose.populateExportedKeys();
        moose.populateIndices();
        
        sqlx("ALTER TABLE moose DROP COLUMN antler_length");
        sqlx("ALTER TABLE moose DROP COLUMN name");
        sqlx("ALTER TABLE moose DROP COLUMN moose_pk");
        db.refresh();
        
        assertEquals("Wrong number of columns ("+moose.getChildNames(SQLColumn.class)+")",
                0, moose.getColumns().size());
        assertEquals("PK no longer disappears", 1, moose.getIndices().size());
    }

    public void testModifyNonPkCol() throws Exception {
        SQLSchema s = db.getSchemaByName("public");
        SQLTable moose = s.findTableByName("moose");
        moose.populateColumns();
        moose.populateExportedKeys();
        moose.populateIndices();
        SQLColumn antlerLength = moose.getColumns().get(2);

        assertEquals(Types.INTEGER, antlerLength.getType());
        assertEquals(antlerLength.getName() + " Precision: " + antlerLength.getPrecision() + " Scale: " + antlerLength.getScale(),
                10, antlerLength.getPrecision());
        assertEquals(0, antlerLength.getScale());

        sqlx("ALTER TABLE moose ALTER COLUMN antler_length NUMERIC(10,2)");
        db.refresh();
        
        assertEquals("Wrong number of columns ("+moose.getChildNames(SQLColumn.class)+")",
                3, moose.getColumns().size());
        assertSame(antlerLength, moose.getColumns().get(2));
        assertEquals("ANTLER_LENGTH", antlerLength.getName());
        assertEquals(Types.NUMERIC, antlerLength.getType());
        assertEquals(10, antlerLength.getPrecision());
        assertEquals(2, antlerLength.getScale());
    }
    
    public void testAddIndex() throws Exception {
        SQLSchema s = db.getSchemaByName("public");
        SQLTable moose = s.findTableByName("moose");
        moose.populateColumns();
        moose.populateExportedKeys();
        moose.populateIndices();
        
        // NOTE this will fail on the raw HSQL driver. be sure you're using our jdbc wrapper!
        assertEquals("Unexpected indexes: " + moose.getChildNames(SQLIndex.class),
                1, moose.getIndices().size());

        sqlx("CREATE INDEX moose_idx ON moose (name)");
        db.refresh();
        
        assertEquals("Unexpected indexes: " + moose.getChildNames(SQLIndex.class),
                2, moose.getIndices().size());
        SQLIndex mooseIdx = moose.getIndices().get(1);
        assertEquals("MOOSE_IDX", mooseIdx.getName());
        assertEquals(1, mooseIdx.getChildCount());
        assertEquals("NAME", mooseIdx.getChild(0).getName());
    }

    public void testRemoveIndex() throws Exception {
        sqlx("CREATE INDEX moose_idx ON moose (name)");

        SQLSchema s = db.getSchemaByName("public");
        SQLTable moose = s.findTableByName("moose");
        moose.populateColumns();
        moose.populateExportedKeys();
        moose.populateIndices();
        
        // NOTE this will fail on the raw HSQL driver. be sure you're using our jdbc wrapper!
        assertEquals("Unexpected indexes: " + moose.getChildNames(SQLIndex.class),
                2, moose.getIndices().size());

        sqlx("DROP INDEX moose_idx");
        db.refresh();
        
        assertEquals("Unexpected indexes: " + moose.getChildNames(SQLIndex.class),
                1, moose.getIndices().size());
    }

    public void testAddColumnToIndex() throws Exception {
        sqlx("CREATE INDEX moose_idx ON moose (name)");

        SQLSchema s = db.getSchemaByName("public");
        SQLTable moose = s.findTableByName("moose");
        moose.populateColumns();
        moose.populateExportedKeys();
        moose.populateIndices();
        
        // NOTE this will fail on the raw HSQL driver. be sure you're using our jdbc wrapper!
        assertEquals("Unexpected indexes: " + moose.getChildNames(SQLIndex.class),
                2, moose.getIndices().size());
        SQLIndex mooseIdx = (SQLIndex) moose.getIndices().get(1);
        assertEquals(1, mooseIdx.getChildCount());
        assertEquals("MOOSE_IDX", mooseIdx.getName());

        sqlx("DROP INDEX moose_idx");
        sqlx("CREATE INDEX moose_idx ON moose (name, antler_length)");
        db.refresh();
        
        assertEquals("Unexpected indexes: " + moose.getChildNames(SQLIndex.class),
                2, moose.getIndices().size());
        assertSame(mooseIdx, moose.getIndices().get(1));
        assertEquals(2, mooseIdx.getChildCount());
        assertEquals("NAME", mooseIdx.getChild(0).getName());
        assertEquals("ANTLER_LENGTH", mooseIdx.getChild(1).getName());
    }

    public void testRemoveColumnFromIndex() throws Exception {
        sqlx("CREATE INDEX moose_idx ON moose (name, antler_length)");

        SQLSchema s = db.getSchemaByName("public");
        SQLTable moose = s.findTableByName("moose");
        moose.populateColumns();
        moose.populateExportedKeys();
        moose.populateIndices();
        
        // NOTE this will fail on the raw HSQL driver. be sure you're using our jdbc wrapper!
        assertEquals("Unexpected indexes: " + moose.getChildNames(SQLIndex.class),
                2, moose.getIndices().size());
        SQLIndex mooseIdx = (SQLIndex) moose.getIndices().get(1);
        assertEquals(2, mooseIdx.getChildCount());
        assertEquals("MOOSE_IDX", mooseIdx.getName());
        
        sqlx("DROP INDEX moose_idx");
        sqlx("CREATE INDEX moose_idx ON moose (antler_length)");
        db.refresh();
        
        assertEquals("Unexpected indexes: " + moose.getChildNames(SQLIndex.class),
                2, moose.getIndices().size());
        assertSame(mooseIdx, moose.getIndices().get(1));
        assertEquals(1, mooseIdx.getChildCount());
        assertEquals("ANTLER_LENGTH", mooseIdx.getChild(0).getName());
    }

    public void testRemoveColumnThatWasIndexed() throws Exception {
        sqlx("CREATE INDEX moose_idx ON moose (name, antler_length)");

        SQLSchema s = db.getSchemaByName("public");
        SQLTable moose = s.findTableByName("moose");
//        moose.getColumnsFolder().populate();
//        moose.getExportedKeysFolder().populate();
//        moose.getIndicesFolder().populate();
        moose.populate();
        
        // NOTE this will fail on the raw HSQL driver. be sure you're using our jdbc wrapper!
        assertEquals("Unexpected indexes: " + moose.getChildNames(SQLIndex.class),
                2, moose.getIndices().size());
        SQLIndex mooseIdx = (SQLIndex) moose.getIndices().get(1);
        assertEquals(2, mooseIdx.getChildCount());
        assertEquals("MOOSE_IDX", mooseIdx.getName());
        
        // note to test maintainers: dropping this column causes the whole index to be dropped
        sqlx("ALTER TABLE moose DROP COLUMN antler_length");
        db.refresh();
        
        assertEquals("Unexpected indexes: " + moose.getChildNames(SQLIndex.class),
                1, moose.getIndices().size());
        
        assertEquals(2, moose.getColumns().size());
    }

}
