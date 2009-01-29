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
package ca.sqlpower.validation.swingui;

import javax.swing.Action;
import javax.swing.JComponent;

import ca.sqlpower.validation.Validator;

/**
 * A ValidationHandler is a non-visual component that ties a given
 * Validator (which in turn probably depends on a given
 * JComponent, e.g., a JTextComponent) into the StatusComponent;
 * upon events from the JComponent (such as KeyEvents
 * for the TextComponentValidationHandler) the ValidationHandler
 * invokes the Validator's validate() method and updates the
 * JComponent's appearance accordingly.
 * @see ca.sqlpower.validation.FormValidationHandler
 */
public interface ValidationHandler {
	
    public void addValidateObject(JComponent component, Validator validator);
    
    /**
     * Sets the action to disable if the status is fail
     */
    public void setValidatedAction(Action action);
    
}
