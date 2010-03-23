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
 * This interface is for any type of formatter that converts a table into a file
 *
 */
public interface ExportFormatter {
	
	
	/**
	 * Converts the given model into a a specific type of file and writes it to the output
	 * stream.
	 */	
	public void formatToStream(TableModel model, PrintWriter writer);
	
	
	/**
	 * Converts the given model into a a specific type of file and writes only the selected rows into a file
	 */	
	public void formatToStream(TableModel model, PrintWriter writer, int[] selectedRows);

}
