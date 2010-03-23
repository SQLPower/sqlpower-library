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

import javax.swing.table.TableCellRenderer;

/**
 * The CleanupTableCellRenderer adds a cleanup method to the {@link TableCellRenderer}.
 * This lets a table cell renderer clean up listeners when it is being removed.
 */
public interface CleanupTableCellRenderer extends TableCellRenderer {

	/**
	 * Call this method when the TableCellRenderer is no longer needed.
	 */
	void cleanup(); 

}
