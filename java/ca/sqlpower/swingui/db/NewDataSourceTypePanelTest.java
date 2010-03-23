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

import junit.framework.TestCase;
import ca.sqlpower.sql.DataSourceCollection;
import ca.sqlpower.sql.JDBCDataSource;
import ca.sqlpower.sql.JDBCDataSourceType;
import ca.sqlpower.testutil.StubDataSourceCollection;

public class NewDataSourceTypePanelTest extends TestCase {

	/**
	 * The panel to test against.
	 */
	private NewDataSourceTypePanel newDSTypePanel;
	
	/**
	 * The editor attached to the panel.
	 */
	private DataSourceTypeEditor editor;

	private DataSourceCollection<JDBCDataSource> collection;

	private JDBCDataSourceType firstDSType;

	private JDBCDataSourceType secondDSType;
	
	@Override
	protected void setUp() throws Exception {
		
		collection = new StubDataSourceCollection<JDBCDataSource>();
		firstDSType = new JDBCDataSourceType();
		firstDSType.setJdbcUrl("First Testing URL");
        firstDSType.putProperty(JDBCDataSourceType.SUPPORTS_UPDATEABLE_RESULT_SETS, String.valueOf(true));
		firstDSType.setComment("First testing comment");
		collection.addDataSourceType(firstDSType);
		
		secondDSType = new JDBCDataSourceType();
		secondDSType.setJdbcUrl("Second Testing URL");
		secondDSType.putProperty(JDBCDataSourceType.SUPPORTS_UPDATEABLE_RESULT_SETS, String.valueOf(false));
		secondDSType.setComment("Second testing comment");
		
		editor = new DataSourceTypeEditor(collection, null);
		
		newDSTypePanel = new NewDataSourceTypePanel(editor, collection);
	}
	
	public void testCreatingNewBlankDSType() throws Exception {
		newDSTypePanel.setBlankOptionSelected(true);
		newDSTypePanel.applyChanges();
		
		JDBCDataSourceType newType = null;
		for (JDBCDataSourceType type : collection.getDataSourceTypes()) {
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
		
		JDBCDataSourceType newType = null;
		for (JDBCDataSourceType type : collection.getDataSourceTypes()) {
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
