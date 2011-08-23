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
package ca.sqlpower.object.undo;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;

import org.apache.log4j.Logger;

import ca.sqlpower.object.ObjectDependentException;
import ca.sqlpower.object.SPChildEvent;
import ca.sqlpower.object.SPObject;
import ca.sqlpower.object.SPChildEvent.EventType;
import ca.sqlpower.sqlobject.SQLColumn;
import ca.sqlpower.sqlobject.SQLIndex;
import ca.sqlpower.sqlobject.SQLObjectException;
import ca.sqlpower.sqlobject.SQLRelationship;
import ca.sqlpower.sqlobject.SQLTable;

/**
 * This is an edit for SPObject events involving children.
 */
public class SPObjectChildEdit extends AbstractUndoableEdit {

	private static final Logger logger = Logger
			.getLogger(SPObjectChildEdit.class);
	
	protected final SPChildEvent e;
	protected String toolTip;
	
	public SPObjectChildEdit(SPChildEvent e) {
		super();
		this.e = e;
		createToolTip();
	}
	
	public void removeChild(){
		logger.debug("Removing child " + e.getChildType().getSimpleName() + " from parent " + e.getSource().getClass().getSimpleName());
		SPObject source = e.getSource();
		SPObject parent = source.getParent();
		try {
			if (parent != null) {
				parent.setMagicEnabled(false);
			}
			source.setMagicEnabled(false);
			try {
				source.removeChild(e.getChild());
			} catch (ObjectDependentException e) {
				throw new RuntimeException("Could not undo because added object is depended on by another object");
			}
		}finally {
			source.setMagicEnabled(true);
			if (parent != null) {
				parent.setMagicEnabled(true);
			}
		}
	}
	
	
	public void addChild() throws SQLObjectException {
		logger.debug("Adding child " + e.getChildType().getSimpleName() + " to parent " + e.getSource().getClass().getSimpleName());
		SPObject source = e.getSource();
		SPObject parent = source.getParent();
		try{
			if (parent != null) {
				parent.setMagicEnabled(false);
			}
			source.setMagicEnabled(false);
			source.addChild(e.getChild(), e.getIndex());
		}finally {
			if (parent != null) {
				parent.setMagicEnabled(true);
			}
			source.setMagicEnabled(true);
		}
	}
	
	@Override
	public void redo() throws CannotRedoException {
		logger.debug("Redoing " + e);
		try {
			if (e.getType() == EventType.ADDED) {
				addChild();
			} else if (e.getType() == EventType.REMOVED) {
				removeChild();
			}
		} catch (SQLObjectException e) {
			logger.error("redo: caught exception", e);
			throw new CannotRedoException();
		}
	}
	
	@Override
	public void undo() throws CannotRedoException {
		logger.debug("Undoing " + e);
		try {
			if (e.getType() == EventType.ADDED) {
				removeChild();
			} else if (e.getType() == EventType.REMOVED) {
				addChild();
			}
		} catch (SQLObjectException e) {
			logger.error("redo: caught exception", e);
			throw new CannotRedoException();
		}
	}
	
	public void createToolTip() {
		if (e.getType() == EventType.ADDED) {
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
		} else if (e.getType() == EventType.REMOVED) {
			if (e.getChild() instanceof SQLTable) {
				toolTip = "Remove table";
			} else if (e.getChild() instanceof SQLColumn) {
				toolTip = "Remove column";
			} else if (e.getChild() instanceof SQLRelationship) {
				toolTip = "Remove relation";
			} else {
				toolTip = "Remove child";
			}
		}
	}
	
	@Override
	public boolean canRedo() {
		return true;
	}
	
	@Override
	public boolean canUndo() {
		return true;
	}
	
	@Override
	public String getPresentationName() {
		
		return toolTip;
	}
	
	@Override
	public String toString() {
		return "Child Edit: Event=[" + e + "]";
	}
	
}
