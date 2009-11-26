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

import ca.sqlpower.sqlobject.SQLObjectException;
import ca.sqlpower.sqlobject.SQLColumn;
import ca.sqlpower.sqlobject.SQLObject;
import ca.sqlpower.sqlobject.SQLRelationship;
import ca.sqlpower.sqlobject.SQLTable;

public class SQLObjectRemoveChildren extends SQLObjectChildEdit {
	private static final Logger logger = Logger.getLogger(SQLObjectRemoveChildren.class);

	@Override
	public void undo() throws CannotUndoException {
		try {
			addChild();
		} catch (SQLObjectException e) {
			logger.error("Can't undo: caught exception", e);
			throw new CannotUndoException();
		}
	}
	
	@Override
	public void redo() throws CannotRedoException {
		removeChild();
	}
	
	@Override
	public void createToolTip() {
		if (e.getChild() instanceof SQLTable) {
			toolTip = "Remove table";
		}
		if (e.getChild() instanceof SQLColumn) {
			toolTip = "Remove column";
		}
		if (e.getChild() instanceof SQLRelationship) {
			toolTip = "Remove relation";
		}
	}
	@Override
	public String toString() {
		return "Remove "+e.getChild()+" from "+e.getSource();
	}
}
