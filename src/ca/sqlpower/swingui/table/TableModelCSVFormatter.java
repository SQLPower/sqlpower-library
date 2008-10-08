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
