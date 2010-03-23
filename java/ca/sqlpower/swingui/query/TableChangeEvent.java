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

package ca.sqlpower.swingui.query;

import javax.swing.JTable;

/**
 * An event object for a listener that stores the
 * table modified in the event and the component
 * class the table belongs to.
 */
public class TableChangeEvent {
	
	private final SQLQueryUIComponents parent;
	private final JTable changedTable;

	public TableChangeEvent(SQLQueryUIComponents parent, JTable changedTable) {
		this.parent = parent;
		this.changedTable = changedTable;
		
	}

	public SQLQueryUIComponents getParent() {
		return parent;
	}

	public JTable getChangedTable() {
		return changedTable;
	}

}
