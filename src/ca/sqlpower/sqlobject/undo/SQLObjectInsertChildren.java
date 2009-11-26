/*
 * Copyright (c) 2008, SQL Power Group Inc.
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
package ca.sqlpower.sqlobject.undo;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

import org.apache.log4j.Logger;

import ca.sqlpower.sqlobject.SQLColumn;
import ca.sqlpower.sqlobject.SQLIndex;
import ca.sqlpower.sqlobject.SQLObjectException;
import ca.sqlpower.sqlobject.SQLRelationship;
import ca.sqlpower.sqlobject.SQLTable;

public class SQLObjectInsertChildren extends SQLObjectChildEdit {
	private static final Logger logger = Logger.getLogger(SQLObjectInsertChildren.class);
	
	
	@Override
	public void undo() throws CannotUndoException {
		removeChild();
	}
	
	@Override
	public void redo() throws CannotRedoException {
		try {
			addChild();
		} catch (SQLObjectException e) {
			logger.error("redo: caught exception", e);
			throw new CannotRedoException();
		}
	}

	@Override
	public void createToolTip() {
		if (e.getChild() instanceof SQLTable) {
			toolTip = "Add table";
		} else if (e.getChild() instanceof SQLColumn) {
			toolTip = "Add column";
		} else if (e.getChild() instanceof SQLRelationship) {
			toolTip = "Add relationship";
		} else if (e.getChild() instanceof SQLIndex) {
			toolTip = "Add index";
		} else {
			toolTip = "Add child";
		}
	}
	
	@Override
	public String toString() {
		return "Insert "+e.getChild()+" into "+e.getSource();
	}
}
