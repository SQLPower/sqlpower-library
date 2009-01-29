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

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import javax.swing.table.TableModel;

import au.com.bytecode.opencsv.CSVWriter;

/**
 * This class exports TableModels to a CSV file.
 */
public class TableModelCSVFormatter implements ExportFormatter {
	
	
	public TableModelCSVFormatter(){
		
	}
	/**
	 * Converts the given model into a CSV file and writes it to the output
	 * stream.
	 */	
	public void formatToStream(TableModel model, PrintWriter writer) {
		int[] allRows = new int[model.getRowCount()];
		for (int i = 0; i < model.getRowCount(); i++) {
			allRows[i] = i;
		}
		formatToStream(model, writer, allRows);

	}
	/**
	 * Outputs a table in HTML that represents the TableModel. Only the rows
	 * given will be written to the file.
	 */
	public void formatToStream(TableModel model, PrintWriter writer, int[] selectedRows) {
		try {
			CSVWriter csvWriter = new CSVWriter(writer);
			List<String[]> tableExport = new ArrayList<String[]>();
			int columnCount = model.getColumnCount();
			String[] rowArray = new String[columnCount];
			for (int i = 0; i < columnCount; i++) {
				rowArray[i] = model.getColumnName(i);
			}
			tableExport.add(rowArray);
			for (int row = 0; row < selectedRows.length; row++) {
				rowArray = new String[columnCount];
				for (int col = 0; col < columnCount; col++) {
					Object value = model.getValueAt(selectedRows[row], col);
					if (value != null) {
						rowArray[col] = value.toString();
					} else {
						rowArray[col] = "";
					}
				}
				tableExport.add(rowArray);
			}
			csvWriter.writeAll(tableExport);
			csvWriter.close();

		} catch (IOException ex) {
			throw new RuntimeException("Could not close the CSV Writer", ex);
		}		
	}	  
}
