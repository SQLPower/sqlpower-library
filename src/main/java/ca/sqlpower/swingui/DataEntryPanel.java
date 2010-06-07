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

/**
 * The DataEntryPanel interface defines the contract between a panel
 * of components that help the user edit the data model and its parent
 * frame.  Classes that implement DataEntryPanel require that exactly
 * one of the two methods {@link #applyChanges()} or {@link
 * #discardChanges()} are called at the end of the panel's lifetime.
 * After affecting the model in the specified way, these methods will
 * free up resources associated with the panel (such as removing the
 * panel from listener lists).  After calling {@link #applyChanges()}
 * or {@link #discardChanges()} on an instance of DataEntryPanel, it
 * may not be used anymore.
 *
 * <p>Remember that it's important to call one of these methods
 * (usually discardChanges()) when a containing frame's window gets
 * closed by the native window system.
 * 
 * XXX: We do not like the name of this interface. Anyone who thinks
 * of a better one is encouraged to apply it. This used to be called
 * architect panel, but since it is being moved to the library, this
 * name no longer applies.
 */
public interface DataEntryPanel {

	/** 
     * Performs the editor save.
     * 
     * <p><b>IMPORTANT NOTE:</b> Make sure this method does not blindly return true
     * just so that it has a valid return type, it is essential that it
     * returns if the object is saved properly or not.  This is required
     * since if the save does fail, the swing session needs to know to restore
     * the interface back and reselect the lastTreePath in the JTree.  You have
     * officially been warned...
     * </p>
     * @return the success of the saving process (do not fake it!)
	 */ 
	public boolean applyChanges();

	/**
	 * A cancel button in the panel's containing frame should invoke
	 * this method.
	 */
	public void discardChanges();
	
	/**
	 * @return This DataEntryPanel's JPanel
	 */
	public JComponent getPanel();
	
	/** 
     * True if this Pane has any changes; will usually delegate
	 * to the Panel's Validator's hasValidated() method.
	 * @return
	 */
	public boolean hasUnsavedChanges();

}
