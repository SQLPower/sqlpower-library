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
import java.sql.SQLException;

import javax.swing.table.AbstractTableModel;

/**
 * This is a basic table model that takes in a result set to be displayed in a
 * table. This model can export a given set of rows to a CSV or HTML file. The
 * result set is not allowed to be modified in this table.
 */
public class ResultSetTableModel extends AbstractTableModel {

	/**
	 * This result set holds the cell entries in the table. 
	 */
	private final ResultSet rs;
	
	private final int rowCount;
	
	/**
	 * The result set passed in here must be scrollable. If it is not
	 * it should be wrapped in a CachedRowSet first.
	 */
	public ResultSetTableModel(ResultSet result) {
		this.rs = result;
		try {
			int prevRow = rs.getRow();
			rs.afterLast();
			rowCount = rs.getRow() - 1;
			rs.absolute(prevRow);
		} catch (SQLException e) {
			throw new RuntimeException("Could not access the result set given to the table model", e);
		}
	}
	
	public int getColumnCount() {
		try {
			return rs.getMetaData().getColumnCount();
		} catch (SQLException e) {
			throw new RuntimeException("Could not get the column count from the result set meta data.", e);
		}
	}

	public int getRowCount() {
		return rowCount;
	}

	public Object getValueAt(int rowIndex, int columnIndex) {
		try {
			int prevRow = rs.getRow();
			rs.absolute(rowIndex + 1);
			Object objectAtPosition = rs.getObject(columnIndex + 1);
			rs.absolute(prevRow);
			return objectAtPosition;
		} catch (SQLException e) {
			throw new RuntimeException(" Could not access the result set given the rowIndex or columnIndex.", e);
		}
	}
	
	@Override
	public int findColumn(String columnName) {
		try {
			for (int i = 0; i < rs.getMetaData().getColumnCount(); i++) {
				if (rs.getMetaData().getColumnName(i).equals(columnName)) {
					return i;
				}
			}
		} catch (SQLException e) {
			throw new RuntimeException("Could not access the result set.", e);
		}
		return -1;
	}
	
	@Override
	public String getColumnName(int column){
		try {
			String colName = rs.getMetaData().getColumnLabel(column + 1);
			if (colName == null || colName.equals("")) {
				colName = rs.getMetaData().getColumnName(column + 1);
			}
			return colName;
		} catch (SQLException e) {
			throw new RuntimeException("Could not get the column name.", e);
		}
	}

}
