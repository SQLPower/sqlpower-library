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

package ca.sqlpower.swingui;

import javax.swing.JComponent;
import javax.swing.JPanel;

import org.apache.log4j.Logger;


public abstract class AbstractNoEditDataEntryPanel extends JPanel implements DataEntryPanel {

	private static final Logger logger = Logger.getLogger(AbstractNoEditDataEntryPanel.class);
	
	/**
	 * doSave() is supposed to return the succesfull-ness of a save operation.
	 * Since nothing changes, nothing needs to be saved, so we just say that
	 * saving worked.
	 */
	public boolean applyChanges() {
		logger.error("Cannot apply changes because this pane is not editable."); //$NON-NLS-1$
		return false;
	}

	/**
	 * Since nothing changes, no changes are discarded.
	 */
	public void discardChanges() {
		logger.error("Cannot discard changes because this pane is not editable."); //$NON-NLS-1$
	}
	
	/**
	 * Always returns false because, since nothing is being edited, there are
	 * never changes, nevermind changes that haven't been saved.
	 */
	public boolean hasUnsavedChanges() {
		return false;
	}

	public JComponent getPanel() {
		return this;
	}


}
