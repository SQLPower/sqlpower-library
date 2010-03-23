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

package ca.sqlpower.swingui;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import ca.sqlpower.sql.DataSourceCollection;
import ca.sqlpower.sql.SPDataSource;
import ca.sqlpower.swingui.db.DatabaseConnectionManager;
import ca.sqlpower.swingui.db.DefaultDataSourceDialogFactory;
import ca.sqlpower.swingui.db.DefaultDataSourceTypeDialogFactory;
import ca.sqlpower.util.UserPrompter;
import ca.sqlpower.util.UserPrompterFactory.UserPromptType;

public class DataSourceUserPrompter implements UserPrompter {

	private SPDataSource selectedDataSource;
	private UserPromptResponse response;
	private DatabaseConnectionManager connectionManager;
	private JDialog dialog;
	private JFrame owner;
	
	public DataSourceUserPrompter(String question, UserPromptOptions optionType, 
			UserPromptResponse defaultResponseType, SPDataSource defaultResponse, final JFrame owner, 
			String questionMessage, DataSourceCollection<SPDataSource> dsCollection, 
			List<Class<? extends SPDataSource>> dsTypes, String ...  buttonNames) {
		if(optionType.getButtonCount() != buttonNames.length) {
			throw new IllegalStateException("Expecting " + optionType.getButtonCount() + 
					"arguments for the optionType " + optionType + "Recieved only " + buttonNames.length + "arguments\n" +
					Arrays.toString(buttonNames));
		}
		this.owner = owner;
		
		List<JComponent> additionalComponents = new ArrayList<JComponent>();
		additionalComponents.add(new JSeparator(SwingConstants.HORIZONTAL));
		
		if(optionType == UserPromptOptions.OK_NEW_NOTOK_CANCEL || optionType == UserPromptOptions.OK_NOTOK_CANCEL
        		|| optionType == UserPromptOptions.OK_NEW_CANCEL || optionType == UserPromptOptions.OK_CANCEL) {
			JButton okButton = new JButton();
			okButton.setAction(new AbstractAction(buttonNames[0]) {
				public void actionPerformed(ActionEvent e) {
					selectedDataSource = (SPDataSource) connectionManager.getSelectedConnection();
					response = UserPromptResponse.OK;
					dialog.setVisible(false);
				}
			});
			additionalComponents.add(okButton);
		}
		
		if(optionType == UserPromptOptions.OK_NEW_NOTOK_CANCEL || optionType == UserPromptOptions.OK_NOTOK_CANCEL) {
			Action skipAction = new AbstractAction((optionType == UserPromptOptions.OK_NOTOK_CANCEL)? buttonNames[1] : buttonNames[2]) {
				public void actionPerformed(ActionEvent e) {
					selectedDataSource = null;
					response = UserPromptResponse.NOT_OK;
					dialog.setVisible(false);
				}
			};
			JButton skipButton = new JButton(skipAction);
			additionalComponents.add(skipButton);
		}
		
		if(optionType == UserPromptOptions.OK_NEW_NOTOK_CANCEL || optionType == UserPromptOptions.OK_NOTOK_CANCEL
        		|| optionType == UserPromptOptions.OK_NEW_CANCEL || optionType == UserPromptOptions.OK_CANCEL) {
			JButton cancelButton = new JButton();
		    cancelButton.setAction(new AbstractAction(buttonNames[buttonNames.length-1]) {
		    	public void actionPerformed(ActionEvent e) {
		    		selectedDataSource = null;
		    		response = UserPromptResponse.CANCEL;
		    		dialog.setVisible(false);
		    	}
		    });
		    additionalComponents.add(cancelButton);
		}
		
		dialog = new JDialog();
		boolean showCloseButton = false;
		ArrayList<Action> additionalActions = new ArrayList<Action>();
		connectionManager = new DatabaseConnectionManager(dsCollection, new DefaultDataSourceDialogFactory(), 
				new DefaultDataSourceTypeDialogFactory(dsCollection), additionalActions, 
				additionalComponents, owner, showCloseButton, dsTypes);
		JPanel connectionPanel = connectionManager.createPanelStandalone(additionalActions, additionalComponents, showCloseButton, question, dialog);
		connectionPanel.setVisible(true);
		dialog.add(connectionPanel);
		dialog.setTitle("Data Source Replacement Tool");
	}

	public Object getUserSelectedResponse() {
		return selectedDataSource;
	}

	public UserPromptResponse promptUser(Object... formatArgs) {
        // The default response, in case the user closes the dialog without
        // pressing one of the buttons
        response = UserPromptResponse.NOT_OK;
        
        Runnable promptUser = new Runnable() {
            public void run() {
            	dialog.pack();
            	dialog.invalidate();
            	if (owner != null) {
            		dialog.setLocationRelativeTo(owner);
            	} else {
            		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            		int x = (screenSize.width / 2) - (dialog.getSize().width / 2);
            		int y = (screenSize.height / 2) - (dialog.getSize().height / 2);
					dialog.setLocation(x, y);
            	}
            	dialog.setVisible(true);
//                questionLabel.setText(questionFormat.format(formatArgs));
            }
        };
    	
        dialog.setModal(true);
        if (SwingUtilities.isEventDispatchThread()) {
        	promptUser.run();
        	dialog.requestFocusInWindow();
        } else {
            try {
                SwingUtilities.invokeAndWait(promptUser);
            } catch (InterruptedException e) {
                SPSUtils.showExceptionDialogNoReport(null,
                        Messages.getString("ModalDialogUserPrompter.showPromptDialogFailed"), e); //$NON-NLS-1$
            } catch (InvocationTargetException e) {
                SPSUtils.showExceptionDialogNoReport(null,
                        Messages.getString("ModalDialogUserPrompter.showPromptDialogFailed"), e); //$NON-NLS-1$
            }
        }
        
        return response;
	}
	
}
