/*
 * Copyright (c) 2009, SQL Power Group Inc.
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

package ca.sqlpower.swingui.db;

import junit.framework.TestCase;
import ca.sqlpower.sql.DataSourceCollection;
import ca.sqlpower.sql.SPDataSourceType;
import ca.sqlpower.sql.StubDataSourceCollection;

public class DataSourceTypeCopyPropertiesPanelTest extends TestCase {
	
	/**
	 * The ds type that will change in this test case.
	 */
	private SPDataSourceType dsType;
	
	/**
	 * The panel to test against.
	 */
	private DataSourceTypeCopyPropertiesPanel dsTypeCopyPanel;
	
	@Override
	protected void setUp() throws Exception {
		
		dsType = new SPDataSourceType();
		dsType.setName("Testing DS Name Shouldn't Change");
		
		DataSourceCollection collection = new StubDataSourceCollection();
		SPDataSourceType firstDSType = new SPDataSourceType();
		firstDSType.setJdbcUrl("First Testing URL");
		firstDSType.setSupportsUpdatableResultSet(true);
		firstDSType.setComment("First testing comment");
		collection.addDataSourceType(firstDSType);
		
		SPDataSourceType secondDSType = new SPDataSourceType();
		secondDSType.setJdbcUrl("Second Testing URL");
		secondDSType.setSupportsUpdatableResultSet(false);
		secondDSType.setComment("Second testing comment");
		
		dsTypeCopyPanel = new DataSourceTypeCopyPropertiesPanel(dsType, collection);
	}
	
	/**
	 * An initial test case to confirm properties do get copied when the dialog's
	 * changes are applied. The name should not change however.
	 */
	public void testCopyDSTypeChanges() {
		String originalDSTypeName = dsType.getName();
		SPDataSourceType dsTypeToCopyFrom = (SPDataSourceType) dsTypeCopyPanel.getDsTypesComboBox().getItemAt(0);
		dsTypeCopyPanel.getDsTypesComboBox().setSelectedItem(dsTypeToCopyFrom);
		dsTypeCopyPanel.applyChanges();
		
		assertEquals(dsTypeToCopyFrom.getJdbcUrl(), dsType.getJdbcUrl());
		assertEquals(dsTypeToCopyFrom.getSupportsUpdateableResultSets(), dsType.getSupportsUpdateableResultSets());
		assertEquals(dsTypeToCopyFrom.getComment(), dsType.getComment());
		assertEquals(originalDSTypeName, dsType.getName());
	}

}
