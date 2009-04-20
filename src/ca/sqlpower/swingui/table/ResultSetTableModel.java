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

package ca.sqlpower.swingui.table;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import javax.swing.event.TableModelEvent;
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
	
	/**
	 * The result set passed in here must be scrollable. If it is not
	 * it should be wrapped in a CachedRowSet first.
	 */
	public ResultSetTableModel(ResultSet result) {
		this.rs = result;
	}
	
	public int getColumnCount() {
		try {
			return rs.getMetaData().getColumnCount();
		} catch (SQLException e) {
			throw new RuntimeException("Could not get the column count from the result set meta data.", e);
		}
	}

	public int getRowCount() {
		int newRowCount;
		try {
			int prevRow = rs.getRow();
			rs.afterLast();
			newRowCount = rs.getRow();
			rs.absolute(prevRow);
		} catch (SQLException e) {
			throw new RuntimeException("Could not access the result set given to the table model", e);
		}
		return newRowCount;
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
	
	@Override
	public Class<?> getColumnClass(int columnIndex) {
		try {
			if (columnIndex < 0 || columnIndex >= rs.getMetaData().getColumnCount()) {
				return Object.class;
			}
		} catch (SQLException e1) {
			throw new RuntimeException(e1);
		}
		int columnType;
		try {
			columnType = rs.getMetaData().getColumnType(columnIndex + 1);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
		if (columnType == Types.VARCHAR) {
			return String.class;
		} else if (columnType == Types.BIT || columnType == Types.INTEGER || columnType == Types.SMALLINT || columnType == Types.TINYINT) {
			return Integer.class;
		} else if (columnType == Types.DECIMAL || columnType == Types.DOUBLE || columnType == Types.NUMERIC) {
			return Double.class;
		} else if (columnType == Types.FLOAT) {
			return Float.class;
		}
		return Object.class;
		
	}

	/**
	 * This method should be called when a row is added to the result set in
	 * this table model.
	 */
	public void rowAdded(int rowNumber) {
		fireTableChanged(new TableModelEvent(this, rowNumber));
	}
	
}
