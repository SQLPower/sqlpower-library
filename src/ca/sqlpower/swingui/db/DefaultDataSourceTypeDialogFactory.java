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

package ca.sqlpower.swingui.db;

import java.awt.BorderLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;

import com.jgoodies.forms.factories.ButtonBarFactory;

import ca.sqlpower.sql.DataSourceCollection;
import ca.sqlpower.swingui.DataEntryPanelBuilder;
import ca.sqlpower.swingui.JDefaultButton;
import ca.sqlpower.swingui.SPSUtils;

public class DefaultDataSourceTypeDialogFactory implements
        DataSourceTypeDialogFactory {
	
	private JDialog d; 
	private DataSourceTypeEditor editor;
	private final DataSourceCollection dsCollection;
	
	public DefaultDataSourceTypeDialogFactory(DataSourceCollection ds){
		this.dsCollection = ds;
	}

    public Window showDialog(Window owner) {
    	 if (d == null) {
	    		d = SPSUtils.makeOwnedDialog(owner, "JDBC Drivers");
	        	editor = new DataSourceTypeEditor(dsCollection, owner);
	        	
	        	JPanel cp = new JPanel(new BorderLayout(12,12));
	            cp.add(editor.getPanel(), BorderLayout.CENTER);
	            cp.setBorder(BorderFactory.createEmptyBorder(12,12,12,12));
	        	
	        	JDefaultButton okButton = new JDefaultButton(DataEntryPanelBuilder.OK_BUTTON_LABEL);
	            okButton.addActionListener(new ActionListener() {
	                    public void actionPerformed(ActionEvent evt) {
	                        editor.applyChanges();
	                        d.dispose();
	                    }
	                });
	        
	            Action cancelAction = new AbstractAction() {
	                    public void actionPerformed(ActionEvent evt) {
	                        editor.discardChanges();
	                        d.dispose();
	                    }
	            };
	            cancelAction.putValue(Action.NAME, DataEntryPanelBuilder.CANCEL_BUTTON_LABEL);
	            JButton cancelButton = new JButton(cancelAction);
	    
	            JPanel buttonPanel = ButtonBarFactory.buildOKCancelBar(okButton, cancelButton);
	    
	            SPSUtils.makeJDialogCancellable(d, cancelAction);
	            d.getRootPane().setDefaultButton(okButton);
	            cp.add(buttonPanel, BorderLayout.SOUTH);
	        	
	        	d.setContentPane(cp);
	        	d.pack();
	        	d.setLocationRelativeTo(owner);
     	}
    	 
         d.pack();
         d.setVisible(true);
         return d;
    }

}
