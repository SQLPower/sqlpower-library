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
import ca.sqlpower.sql.JDBCDataSourceType;
import ca.sqlpower.testutil.StubDataSourceCollection;

public class DataSourceTypeCopyPropertiesPanelTest extends TestCase {
	
	/**
	 * The ds type that will change in this test case.
	 */
	private JDBCDataSourceType dsType;
	
	/**
	 * The panel to test against.
	 */
	private DataSourceTypeCopyPropertiesPanel dsTypeCopyPanel;
	
	@Override
	protected void setUp() throws Exception {
		
		dsType = new JDBCDataSourceType();
		dsType.setName("Testing DS Name Shouldn't Change");
		
		DataSourceCollection collection = new StubDataSourceCollection();
		JDBCDataSourceType firstDSType = new JDBCDataSourceType();
		firstDSType.setJdbcUrl("First Testing URL");
		firstDSType.putProperty(JDBCDataSourceType.SUPPORTS_UPDATEABLE_RESULT_SETS, String.valueOf(true));
		firstDSType.setComment("First testing comment");
		collection.addDataSourceType(firstDSType);
		
		JDBCDataSourceType secondDSType = new JDBCDataSourceType();
		secondDSType.setJdbcUrl("Second Testing URL");
        secondDSType.putProperty(JDBCDataSourceType.SUPPORTS_UPDATEABLE_RESULT_SETS, String.valueOf(false));
		secondDSType.setComment("Second testing comment");
		
		dsTypeCopyPanel = new DataSourceTypeCopyPropertiesPanel(dsType, collection);
	}
	
	/**
	 * An initial test case to confirm properties do get copied when the dialog's
	 * changes are applied. The name should not change however.
	 */
	public void testCopyDSTypeChanges() {
		String originalDSTypeName = dsType.getName();
		JDBCDataSourceType dsTypeToCopyFrom = (JDBCDataSourceType) dsTypeCopyPanel.getDsTypesComboBox().getItemAt(0);
		dsTypeCopyPanel.getDsTypesComboBox().setSelectedItem(dsTypeToCopyFrom);
		dsTypeCopyPanel.applyChanges();
		
		assertEquals(dsTypeToCopyFrom.getJdbcUrl(), dsType.getJdbcUrl());
		assertEquals(dsTypeToCopyFrom.getSupportsUpdateableResultSets(), dsType.getSupportsUpdateableResultSets());
		assertEquals(dsTypeToCopyFrom.getComment(), dsType.getComment());
		assertEquals(originalDSTypeName, dsType.getName());
	}

}
