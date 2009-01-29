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

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;

/**
 * A button that can be (and remain as!) the Default button
 * in a JRootPane; works around Java Sun Bug Parade Bug #6199625
 * @see http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6199625
 */
public class JDefaultButton extends JButton {
	public JDefaultButton(Action a) {
		super(a);
	}
	public JDefaultButton(String labelText) {
		super(labelText);
	}
	/** 
	 * Work around Java Sun Bug Parade Bug #6199625
	 * @see java.awt.Component#removeNotify()
	 */
	@Override
	public void removeNotify() {
		JRootPane root = SwingUtilities.getRootPane(this);
		JButton defaultButton = root.getDefaultButton();
		super.removeNotify();
		if (defaultButton == this) {
			root.setDefaultButton(this);
		}
	}
}