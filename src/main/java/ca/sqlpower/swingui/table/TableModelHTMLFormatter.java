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
import java.text.Format;
import java.util.HashMap;
import java.util.Map;

import javax.swing.table.TableModel;

/**
 * This class exports TableModels to a very basic HTML table.
 */
public class TableModelHTMLFormatter implements ExportFormatter{
	
	
	private static final String NULL_STRING = "";
	
    /**
     * Formatters are given an object from the table model and must output a
     * string representation of that object. This allows classes using the
     * formatter to define different strings to represent an object as other
     * than just the toString version of the object.
     */
    private final Map<Integer, Format> columnFormatters = new HashMap<Integer, Format>();

	public TableModelHTMLFormatter() {

	}
	
	/**
	 * Sets a formatter for the given column of a table model. If the column does
	 * not exist because the table is too small the formatter will not be used.
	 */
	public void setFormatter(int column, Format formatter) {
	    columnFormatters.put(column, formatter);
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
					final Object value = model.getValueAt(selectedRows[i], j);
                    if (value != null) {
                        if (columnFormatters.get(j) != null) {
                            writer.print(columnFormatters.get(j).format(value));
                        } else {
                            writer.print(value.toString());
                        }
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
