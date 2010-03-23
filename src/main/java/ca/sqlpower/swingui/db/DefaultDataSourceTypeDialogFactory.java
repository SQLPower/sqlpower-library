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
