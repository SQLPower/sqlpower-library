/*
 * Copyright (c) 2010, SQL Power Group Inc.
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

package ca.sqlpower.swingui.object;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.JDialog;
import javax.swing.KeyStroke;

import ca.sqlpower.object.SPVariableHelper;
import ca.sqlpower.swingui.DataEntryPanelBuilder;

public class InsertVariableAction extends AbstractAction {

	private final SPVariableHelper variablesHelper;
	private final VariableInserter callback;
	private final Component dialogOwner;
	private final String windowTitle;
	private final String defaultVarKey;

	public InsertVariableAction(
			String actionLabel,
			SPVariableHelper variablesHelper, 
			String variableNamespace,
			VariableInserter callback,
			Component dialogOwner) 
	{
		this(
			actionLabel,
			"Insert a variable",
			"",
			variablesHelper,
			variableNamespace,
			callback,
			dialogOwner);
	}
	
	public InsertVariableAction(
			String actionLabel,
			String windowTitle,
			String defaultVarKey,
			SPVariableHelper variablesHelper, 
			String variableNamespace,
			VariableInserter callback,
			Component dialogOwner) 
	{
		super(actionLabel);
		this.windowTitle = windowTitle;
		this.defaultVarKey = defaultVarKey;
		this.variablesHelper = variablesHelper;
		this.callback = callback;
		this.dialogOwner = dialogOwner;
		
		// Maps this action to CTRL+SPACE
		putValue(
				ACCELERATOR_KEY,
                KeyStroke.getKeyStroke(
                		KeyEvent.VK_SPACE, 
                		InputEvent.CTRL_DOWN_MASK));
	}
	
	public void actionPerformed(ActionEvent e) {
		
		VariablesPanel vp = 
			new VariablesPanel(
					this.variablesHelper,
					this.callback,
					this.defaultVarKey);
		
		JDialog dialog = 
			DataEntryPanelBuilder.createDataEntryPanelDialog(
				vp,
		        this.dialogOwner, 
		        this.windowTitle, 
		        "Insert");
		
		dialog.setVisible(true);
	}
}
