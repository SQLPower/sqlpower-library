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
package ca.sqlpower.sql;

import javax.swing.undo.UndoManager;

import junit.framework.TestCase;

public class PlDotIniListenersTest extends TestCase {
	DataSourceCollection pld = new PlDotIni();
	SPDataSource dbcs = new SPDataSource(pld);
	
	@Override
	protected void setUp() throws Exception {
		dbcs.setDisplayName("Goofus");
	}
	
	/*
	 * Test method for 'ca.sqlpower.architect.PlDotIni.addDataSource(SPDataSource)'
	 * Test it without any listeners.
	 */
	public void testAddDataSource() {
		assertEquals(0, pld.getConnections().size());
		pld.addDataSource(dbcs);
		assertEquals(1, pld.getConnections().size());
		try {
			pld.addDataSource(dbcs);	// should fail!
			fail("Didn't fail to add a second copy!");
		} catch (IllegalArgumentException e) {
			System.out.println("Caught expected " + e);
		}
	}

	/*
	 * Test method for 'ca.sqlpower.architect.PlDotIni.mergeDataSource(SPDataSource)'
	 */
	public void testMergeDataSource() {
		pld.addDataSource(dbcs);
		dbcs.getParentType().setJdbcDriver("mock.Driver");
		pld.mergeDataSource(dbcs);
	}

	/*
	 * Test method for 'ca.sqlpower.architect.PlDotIni.removeDataSource(SPDataSource)'
	 */
	public void testRemoveDataSource() {
		assertEquals(0, pld.getConnections().size());
		pld.addDataSource(dbcs);
		assertEquals(1, pld.getConnections().size());
		assertSame(dbcs, pld.getConnections().get(0));
		pld.removeDataSource(dbcs);
		assertEquals(0, pld.getConnections().size());
	}
	
	DatabaseListChangeEvent addNotified;
	DatabaseListChangeEvent removeNotified;
	
	DatabaseListChangeListener liszt = new DatabaseListChangeListener() {

		public void databaseAdded(DatabaseListChangeEvent e) {
			addNotified = e;
		}

		public void databaseRemoved(DatabaseListChangeEvent e) {
			removeNotified = e;
		}
	};

	/*
	 * Test method for 'ca.sqlpower.architect.PlDotIni.addListener(DatabaseListChangeListener)'
	 */
	public void testAddListener() {
		pld.addDatabaseListChangeListener(liszt);
		assertNull(addNotified);
		pld.addDataSource(dbcs);
		assertNotNull(addNotified);
		System.out.println(addNotified);
	}

	/*
	 * Test method for 'ca.sqlpower.architect.PlDotIni.removeListener(DatabaseListChangeListener)'
	 */
	public void testRemoveListener() {

	}
	
	public void testUndoAddDSType() {
		UndoManager manager = new UndoManager();
		pld.addUndoableEditListener(manager);
		SPDataSourceType type = new SPDataSourceType();
		pld.addDataSourceType(type);
		
		assertTrue(manager.canUndo());
		assertEquals(1, pld.getDataSourceTypes().size());
		manager.undo();
		
		assertTrue(manager.canRedo());
		assertTrue(pld.getDataSourceTypes().isEmpty());
	}
	
	public void testUndoRemoveDSType() {
		UndoManager manager = new UndoManager();
		SPDataSourceType type = new SPDataSourceType();
		pld.addDataSourceType(type);
		pld.addUndoableEditListener(manager);
		
		assertFalse(manager.canUndo());
		assertEquals(1, pld.getDataSourceTypes().size());
		pld.removeDataSourceType(type);
		
		assertTrue(manager.canUndo());
		assertTrue(pld.getDataSourceTypes().isEmpty());
		manager.undo();
		
		assertEquals(1, pld.getDataSourceTypes().size());
		assertTrue(manager.canRedo());
		assertEquals(type, pld.getDataSourceTypes().get(0));
		
	}

}
