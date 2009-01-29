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

import java.io.PrintWriter;

import javax.swing.table.TableModel;

/**
 * This class exports TableModels to a very basic HTML table.
 */
public class TableModelHTMLFormatter implements ExportFormatter{
	
	
	private static final String NULL_STRING = "";

	public TableModelHTMLFormatter() {

	}
	
	/**
	 * Converts the given model into a HTML file and writes it to the output
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
			writer.println("<table>");
			writer.println(" <tr>");
			for (int i = 0; i < model.getColumnCount(); i++) {
				writer.print("  <th>");
				writer.print(model.getColumnName(i));
				writer.println("</th>");
			}
			writer.println(" </tr>");
			
			for (int i = 0; i < selectedRows.length; i++) {
				writer.println(" <tr>");
				
				for (int j = 0; j < model.getColumnCount(); j++ ) {
					writer.print("  <td>");
					if (model.getValueAt(selectedRows[i], j) != null) {
						writer.print(model.getValueAt(selectedRows[i], j).toString());
					} else {
						writer.print(NULL_STRING);
					}
					writer.println("</td>");
				}
				writer.println(" </tr>");
			}
			writer.println("</table>");
			writer.close();
		}	
}
