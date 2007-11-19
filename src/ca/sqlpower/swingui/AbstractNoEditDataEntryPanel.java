/*
 * Copyright (c) 2007, SQL Power Group Inc.
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
		logger.error("Cannot apply changes because this pane is not editable.");
		return false;
	}

	/**
	 * Since nothing changes, no changes are discarded.
	 */
	public void discardChanges() {
		logger.error("Cannot discard changes because this pane is not editable.");
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
