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

package ca.sqlpower.validation.swingui;

import java.util.Collection;

import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

import ca.sqlpower.validation.ValidateResult;

/**
 * A {@link TableModel} implementation specifically for {@link ValidateResult}
 * objects. It has two columns: Status and Details. Status contains the return
 * value of {@link ValidateResult#getStatus()} while Details contains the return
 * value of {@link ValidateResult#getMessage()}.
 */	
public class ValidateResultTableModel extends AbstractTableModel {
	
	private String[] columnNames = {"Status", "Details"};
	private String[][] data;
			
	/**
	 * Creates a {@link ValidateResultTableModel} populated with the provided
	 * {@link Collection} of {@link ValidateResult}.
	 * 
	 * @param validateResult
	 *            A Collection of ValidateResult instances to populate this
	 *            TableModel with
	 */
	public ValidateResultTableModel(Collection<ValidateResult> validateResult) {
		int i = 0;
		data = new String[validateResult.size()][2];
		for (ValidateResult result: validateResult) {
			data[i][0] = result.getStatus().toString();
			data[i++][1] = result.getMessage();
		}
	}

	public int getColumnCount() {
		return columnNames.length;
	}
	
	public String getColumnName(int col) {
		return columnNames[col];
	}
	
	public int getRowCount() {
		return data.length;
	}
	
	public Object getValueAt(int rowIndex, int columnIndex) {
		return data[rowIndex][columnIndex];
	}
}