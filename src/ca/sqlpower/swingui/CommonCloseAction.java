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

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JDialog;

/**
 * Just setVisible(false) and dispose on a JDialog; used in a bunch of places;
 * instantiate once and resuse, to save creating various Actions and Listeners.
 */
public class CommonCloseAction extends AbstractAction {
	private JDialog d;

	public CommonCloseAction(JDialog d) {
        super(Messages.getString("CommonCloseAction.cancelButton")); //$NON-NLS-1$
		this.d = d;						
	}

	public void actionPerformed(ActionEvent e) {
		d.setVisible(false);
		d.dispose();
	}
}