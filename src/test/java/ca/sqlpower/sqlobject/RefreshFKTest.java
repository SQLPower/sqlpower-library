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

import ca.sqlpower.sqlobject.SQLRelationship.ColumnMapping;


public class RefreshFKTest extends DatabaseConnectedTestCase {

    private SQLTable parent;
    private SQLTable child;
    private SQLTable grandchild;
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        
        sqlx("CREATE TABLE public.parent (" +
             "\n parent_id INTEGER NOT NULL," +
             "\n name VARCHAR(10) NOT NULL," +
             "\n CONSTRAINT parent_pk PRIMARY KEY (parent_id)" +
             "\n);");
        
        sqlx("CREATE TABLE public.child (" +
                "\n child_id INTEGER NOT NULL," +
                "\n parent_id INTEGER NOT NULL," +
                "\n name VARCHAR(10) NOT NULL," +
                "\n CONSTRAINT child_pk PRIMARY KEY (child_id)," +
                "\n CONSTRAINT child_uidx UNIQUE (parent_id, child_id)" +
                "\n);");

        // this FK has multiple mappings for testing purposes, not because it's good design :)
        sqlx("CREATE TABLE public.grandchild (" +
                "\n grandchild_id INTEGER NOT NULL," +
                "\n parent_id INTEGER NOT NULL," +
                "\n child_id INTEGER NOT NULL," +  // actually a parent reference!
                "\n name VARCHAR(10) NOT NULL," +
                "\n CONSTRAINT grandchild_pk PRIMARY KEY (child_id)" +
                "\n);");
        
        sqlx("ALTER TABLE public.child" +
        	 "\n ADD CONSTRAINT parent_child_fk FOREIGN KEY (parent_id)" +
        	 "\n REFERENCES public.parent (parent_id)");
        
        sqlx("ALTER TABLE public.grandchild" +
        	 "\n ADD CONSTRAINT child_grandchild_fk FOREIGN KEY (parent_id, child_id)" +
        	 "\n REFERENCES public.child (parent_id, child_id)");
        
        parent = db.getTableByName("PARENT");
        assertNotNull(parent);
        
        child = db.getTableByName("CHILD");
        assertNotNull(child);
        
        grandchild = db.getTableByName("GRANDCHILD");
        assertNotNull(grandchild);
    }
    
    /**
     * This one removes an FK between two existing tables.
     */
    public void testRemoveFK() throws Exception {
        assertEquals(1, parent.getExportedKeys().size());
        assertEquals(1, child.getImportedKeys().size());
        assertTrue(child.isImportedKeysPopulated());
        
        sqlx("ALTER TABLE public.child DROP CONSTRAINT parent_child_fk");
        
        db.refresh();
        
        assertEquals("Child imported keys: " + child.getImportedKeys(),
                0, child.getImportedKeys().size());
        assertEquals("Parent exported keys: " + parent.getExportedKeys(),
                0, parent.getExportedKeys().size());
        
    }
    
    /**
     * This one adds an FK between two existing tables.
     */
    public void testAddFK() throws Exception {
        sqlx("ALTER TABLE public.child DROP CONSTRAINT parent_child_fk");

        assertEquals(0, parent.getExportedKeys().size());
        assertEquals(0, child.getImportedKeys().size());
        
        sqlx("ALTER TABLE public.child" +
                "\n ADD CONSTRAINT parent_child_fk FOREIGN KEY (parent_id)" +
                "\n REFERENCES public.parent (parent_id)");
        db.refresh();
        
        assertEquals(1, child.getImportedKeys().size());
        assertEquals("Parent exported keys: " + parent.getExportedKeys(),
                1, parent.getExportedKeys().size());
        
    }

    /** This one recreates a FK between two tables with the same name, but different column mappings */
    public void testChangeFKMapping() throws Exception {
        assertEquals(1, parent.getExportedKeys().size());
        assertEquals(1, child.getImportedKeys().size());
        SQLRelationship parentChildFk = child.getImportedKeys().get(0).getRelationship();
        assertSame(parentChildFk, parent.getExportedKeys().get(0));
        assertEquals(1, parentChildFk.getChildCount());

        sqlx("ALTER TABLE public.child DROP CONSTRAINT parent_child_fk");
        sqlx("ALTER TABLE public.parent DROP PRIMARY KEY");
        sqlx("ALTER TABLE public.parent ADD PRIMARY KEY (parent_id, name)");

        sqlx("ALTER TABLE public.child" +
                "\n ADD CONSTRAINT parent_child_fk FOREIGN KEY (parent_id, name)" +
                "\n REFERENCES public.parent (parent_id, name)");
        db.refresh();
        
        assertEquals(1, child.getImportedKeys().size());
        assertEquals("Parent exported keys: " + parent.getExportedKeys(),
                1, parent.getExportedKeys().size());
        
        // We're testing here that the relationship was refreshed in place, not simply replaced
        assertSame(parentChildFk, child.getImportedKeys().get(0).getRelationship());
        assertSame(parentChildFk, parent.getExportedKeys().get(0));
        
        assertEquals(2, parentChildFk.getChildCount());
        assertEquals("PARENT_ID", ((ColumnMapping) parentChildFk.getChild(0)).getPkColumn().getName());
        assertEquals("NAME", ((ColumnMapping) parentChildFk.getChild(1)).getPkColumn().getName());
    }

    /** This one adds a whole new table with an FK to an existing table, in one step! */
    public void testAddTableWithFK() throws Exception {
        assertEquals(1, parent.getExportedKeys().size());
        
        sqlx("CREATE TABLE public.grandparent (" +
             "\n grandparent_id INTEGER NOT NULL," +
             "\n name VARCHAR(20)," +
             "\n CONSTRAINT grandparent_pk PRIMARY KEY (grandparent_id))");
        sqlx("ALTER TABLE public.parent ADD COLUMN grandparent_id INTEGER");
        sqlx("ALTER TABLE public.parent ADD CONSTRAINT grandparent_parent_fk" +
             "\n FOREIGN KEY (grandparent_id) REFERENCES public.grandparent (grandparent_id)");
        db.refresh();
        
        assertEquals(1, parent.getImportedKeys().size());
        SQLTable grandparent = db.getTableByName("GRANDPARENT");
        assertEquals(0, grandparent.getImportedKeys().size());
        assertEquals(1, grandparent.getExportedKeys().size());
        assertSame(parent.getImportedKeys().get(0).getRelationship(), grandparent.getExportedKeys().get(0));
    }

    /** This one removes a whole table that had an FK to an existing table, in one step! */
    public void testRemoveTableWithFK() throws Exception {
        assertEquals(1, parent.getExportedKeys().size());
        assertEquals(0, parent.getImportedKeys().size());
        assertEquals(1, child.getExportedKeys().size());
        assertEquals(1, child.getImportedKeys().size());

        sqlx("ALTER TABLE public.child DROP CONSTRAINT parent_child_fk");
        sqlx("DROP TABLE public.parent");
        db.refresh();
        
        assertEquals(0, child.getImportedKeys().size());
        assertEquals(1, child.getExportedKeys().size());
    }
}
