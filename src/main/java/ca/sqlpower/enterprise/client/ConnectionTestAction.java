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

package ca.sqlpower.enterprise.client;

import java.awt.Component;
import java.util.HashSet;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.Action;

import ca.sqlpower.swingui.enterprise.client.SPServerInfoPanel;

/**
 * This {@link Action} is used for testing server connections, by using
 * information entered into a {@link SPServerInfoPanel}. This {@link Action} is
 * performed when a test button on the {@link SPServerInfoPanel} is clicked.
 */
public abstract class ConnectionTestAction extends AbstractAction {

	/**
	 * A {@link Set} of {@link SPServerInfoPanel}s that are open. Each of these
	 * panels should contain a {@link Component} that performs this
	 * {@link ConnectionTestAction}.
	 */
    private Set<SPServerInfoPanel> panels = new HashSet<SPServerInfoPanel>();
    
    /**
     * @see AbstractAction#AbstractAction()
     */
    public ConnectionTestAction() {
    	super();
	}
    
    /**
     * @see AbstractAction#AbstractAction(String) 
     */
    public ConnectionTestAction(final String name) {
    	super(name);
	}

	/**
	 * Adds an {@link SPServerInfoPanel} to a {@link Set} of opened panels
	 * containing a {@link Component} which this {@link ConnectionTestAction} is
	 * assigned to. This panel reference is used when this {@link Action} is
	 * performed, to check which panel performed this {@link Action}.
	 * 
	 * @param panel
	 *            The {@link SPServerInfoPanel} containing a {@link Component}
	 *            that this {@link ConnectionTestAction} is assigned to.
	 */
    public void addPanel(SPServerInfoPanel panel) {
    	panels.add(panel);
    }

	/**
	 * Removes an {@link SPServerInfoPanel} from the {@link Set} of stored
	 * opened panels containing a {@link Component} which this
	 * {@link ConnectionTestAction} is assigned to.
	 * 
	 * @param panel
	 *            The {@link SPServerInfoPanel} to remove.
	 * @return true if the {@link SPServerInfoPanel} to remove exists.
	 */
    public boolean removePanel(SPServerInfoPanel panel) {
    	return panels.remove(panel);
    }

	/**
	 * Finds the {@link SPServerInfoPanel} which contains the {@link Component}
	 * this {@link ConnectionTestAction} is assigned to.
	 * 
	 * @param c
	 *            The {@link Component} this {@link ConnectionTestAction} is
	 *            assigned to.
	 * @return The {@link SPServerInfoPanel} which contains the
	 *         {@link Component} this {@link ConnectionTestAction} is assigned
	 *         to. If it does not exist, null is returned.
	 */
    public SPServerInfoPanel findPanel(Component c) {
    	for (SPServerInfoPanel panel : panels) {
    		if (panel.getPanel().isAncestorOf(c)) {
    			return panel;
    		}
    	}
    	return null;
    }
    
}
