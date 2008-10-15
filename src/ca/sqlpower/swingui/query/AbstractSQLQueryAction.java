/*
 * Copyright (c) 2008, SQL Power Group Inc.
 * 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in
 *       the documentation and/or other materials provided with the
 *       distribution.
 *     * Neither the name of SQL Power Group Inc. nor the names of its
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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
