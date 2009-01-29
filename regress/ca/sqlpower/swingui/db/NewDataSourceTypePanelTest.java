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

package ca.sqlpower.swingui.db;

import ca.sqlpower.sql.DataSourceCollection;
import ca.sqlpower.sql.SPDataSourceType;
import ca.sqlpower.sql.StubDataSourceCollection;
import junit.framework.TestCase;

public class NewDataSourceTypePanelTest extends TestCase {

	/**
	 * The panel to test against.
	 */
	private NewDataSourceTypePanel newDSTypePanel;
	
	/**
	 * The editor attached to the panel.
	 */
	private DataSourceTypeEditor editor;

	private DataSourceCollection collection;

	private SPDataSourceType firstDSType;

	private SPDataSourceType secondDSType;
	
	@Override
	protected void setUp() throws Exception {
		
		collection = new StubDataSourceCollection();
		firstDSType = new SPDataSourceType();
		firstDSType.setJdbcUrl("First Testing URL");
		firstDSType.setSupportsUpdatableResultSet(true);
		firstDSType.setComment("First testing comment");
		collection.addDataSourceType(firstDSType);
		
		secondDSType = new SPDataSourceType();
		secondDSType.setJdbcUrl("Second Testing URL");
		secondDSType.setSupportsUpdatableResultSet(false);
		secondDSType.setComment("Second testing comment");
		
		editor = new DataSourceTypeEditor(collection, null);
		
		newDSTypePanel = new NewDataSourceTypePanel(editor, collection);
	}
	
	public void testCreatingNewBlankDSType() throws Exception {
		newDSTypePanel.setBlankOptionSelected(true);
		newDSTypePanel.applyChanges();
		
		SPDataSourceType newType = null;
		for (SPDataSourceType type : collection.getDataSourceTypes()) {
			if (type != firstDSType && type != secondDSType) {
				newType = type;
				break;
			}
		}
		
		assertNull(newType.getComment());
		assertNull(newType.getJdbcUrl());
		assertNull(newType.getJdbcDriver());
	}
	
	public void testCreatingNewCopiedDSType() throws Exception {
		newDSTypePanel.setBlankOptionSelected(false);
		newDSTypePanel.setCopyDSType(firstDSType);
		newDSTypePanel.applyChanges();
		
		SPDataSourceType newType = null;
		for (SPDataSourceType type : collection.getDataSourceTypes()) {
			if (type != firstDSType && type != secondDSType) {
				newType = type;
				break;
			}
		}
		
		assertEquals(firstDSType.getComment(), newType.getComment());
		assertEquals(firstDSType.getJdbcUrl(), newType.getJdbcUrl());
		assertEquals(firstDSType.getSupportsUpdateableResultSets(), newType.getSupportsUpdateableResultSets());
	}
}
