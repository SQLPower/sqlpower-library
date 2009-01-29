/*
 * Copyright (c) 2008, SQL Power Group Inc.
 *
 * This file is part of Power*Architect.
 *
 * Power*Architect is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * Power*Architect is distributed in the hope that it will be useful,
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
import ca.sqlpower.sqlobject.SQLIndex;
import ca.sqlpower.sqlobject.SQLObject;
import ca.sqlpower.sqlobject.SQLRelationship;
import ca.sqlpower.sqlobject.SQLTable;

public class SQLObjectInsertChildren extends SQLObjectChildren {
	private static final Logger logger = Logger.getLogger(SQLObjectInsertChildren.class);
	
	
	@Override
	public void undo() throws CannotUndoException {
		removeChildren();
	}
	
	@Override
	public void redo() throws CannotRedoException {
		try {
			addChildren();
		} catch (SQLObjectException e) {
			logger.error("redo: caught exception", e);
			throw new CannotRedoException();
		}
	}

	@Override
	public void createToolTip() {
		if (e.getChildren().length > 0) {
		    String pluralSuffix;
            if (e.getChildren()[0] instanceof SQLTable) {
                toolTip = "Add table";
                pluralSuffix = "s";
            } else if (e.getChildren()[0] instanceof SQLColumn) {
                toolTip = "Add column";
                pluralSuffix = "s";
            } else if (e.getChildren()[0] instanceof SQLRelationship) {
                toolTip = "Add relationship";
                pluralSuffix = "s";
            } else if (e.getChildren()[0] instanceof SQLIndex) {
                toolTip = "Add index";
                pluralSuffix = "es";
            } else {
                toolTip = "Add child";
                pluralSuffix = "ren";
            }
            
            if (e.getChildren().length > 1) {
                toolTip += pluralSuffix;
            }
        }
	}
	
	@Override
	public String toString() {
		StringBuffer childList = new StringBuffer();
		childList.append("{");
		for (SQLObject child : e.getChildren()) {
			childList.append(child).append(", ");
		}
		childList.append("}");
		return "Insert "+childList+" into "+e.getSource();
	}
}
