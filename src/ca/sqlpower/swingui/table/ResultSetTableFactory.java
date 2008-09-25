/*
 * Copyright (c) 2008, SQL Power Group Inc.
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

package ca.sqlpower.swingui.table;

import java.sql.ResultSet;

import javax.swing.JTable;
import javax.swing.text.Document;

/**
 * A factory to create JTables. These tables can be sorted by clicking on their
 * headers and export selected rows to files.
 */
public class ResultSetTableFactory {
	
	/**
	 * Returns a JTable to display the result set. The table can be sorted and
	 * selections can be exported.
	 */
	public static JTable createResultSetJtable(ResultSet rs) {
		ResultSetTableModel model = new ResultSetTableModel(rs);
		return new FancyExportableJTable(model);
	}
	

	/**
	 * Returns a JTable to display the result set. The table can be sorted and
	 * selections can be exported. The table can also be filtered as you enter
	 * text into the document provided.
	 */
	public static JTable createResultSetJTableWithSearch(ResultSet rs, Document doc) {
		ResultSetTableModel model = new ResultSetTableModel(rs);
		return new FancyExportableJTable(model, doc);
	}

}
