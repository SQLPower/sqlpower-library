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

import javax.swing.undo.AbstractUndoableEdit;

import ca.sqlpower.object.ObjectDependentException;
import ca.sqlpower.object.SPChildEvent;
import ca.sqlpower.object.SPObject;
import ca.sqlpower.sqlobject.SQLObject;
import ca.sqlpower.sqlobject.SQLObjectException;

public abstract class SQLObjectChildEdit extends AbstractUndoableEdit {

	protected SPChildEvent e;
	protected String toolTip;
	
	public SQLObjectChildEdit() {
		super();
	}
	
	public void createEditFromEvent(SPChildEvent event){
		e = event;
		createToolTip();
	}
	public abstract void createToolTip();
	
	public void removeChild(){
		SQLObject source = (SQLObject) e.getSource();
		SQLObject parent = (SQLObject) source.getParent();
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
	
		SQLObject source = (SQLObject) e.getSource();
		SQLObject parent = source.getParent();
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
	
}
