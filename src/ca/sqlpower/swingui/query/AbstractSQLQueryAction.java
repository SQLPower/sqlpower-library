/*
 * Copyright (c) 2009, SQL Power Group Inc.
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

package ca.sqlpower.swingui.query;


import java.awt.Component;

import javax.swing.AbstractAction;

/**
 * This is a small extension of {@link AbstractAction} that keeps a
 * reference to the {@link Component} that should own dialogs popped
 * up by this action.
 */
public abstract class AbstractSQLQueryAction extends AbstractAction {
    
    protected final Component dialogOwner;
    
    /**
	 * Stores the given dialog owner.
	 * 
	 * @param dialogOwner
	 *            The component whose nearest Window ancestor will own any
	 *            dialogs created by this action.
     */
    public AbstractSQLQueryAction(Component dialogOwner) {
        super();
        this.dialogOwner = dialogOwner;
    }
    
    /**
	 * Stores the given dialog owner and action name.
	 * 
	 * @param dialogOwner
	 *            The component whose nearest Window ancestor will own any
	 *            dialogs created by this action.
	 * @param name
	 *            The user-visible name of this action. Will appear on button
	 *            faces and menu items.
	 */
    public AbstractSQLQueryAction(Component dialogOwner, String name) {
        super(name);
        this.dialogOwner = dialogOwner;
    }

}
