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

import java.awt.BorderLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.Arrays;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.apache.log4j.Logger;

import ca.sqlpower.sql.DataSourceCollection;
import ca.sqlpower.sql.SPDataSource;
import ca.sqlpower.util.UserPrompter;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

public class DataSourceUserPrompter implements UserPrompter {
	
	private static final Logger logger = Logger.getLogger(DataSourceUserPrompter.class);
	
	/**
	 * The default response type for the current database user prompter.
	 */
	private final UserPromptResponse defaultResponseType;
	
	/**
	 * This is the main dialog created by this prompter which will let
	 * the user select or create a database.
	 */
	private JDialog userPrompt;
	
	/**
	 * The selected data source from the existing data sources or a new data source created by the user.
	 */
	private SPDataSource selectedDataSource;

	/**
	 * A combo box of all existing data sources in the context.
	 */
	private final JComboBox dsComboBox;

	private final JFrame owner;

	/**
	 * The collection of data sources available for selection by the user.
	 */
	private final DataSourceCollection dsCollection;
	
	/**
	 * The response type the user selected.
	 */
	private UserPromptResponse response;

	/**
	 * The label where the question will be placed once modified by additional
	 * arguments in promptUser.
	 */
	private JLabel questionLabel;
	
	/**
	 * Describes if this is the first time the prompt is displayed to decide if
	 * it needs to be packed.
	 */
	private boolean firstPrompt = true;
	
	/**
	 * The question template for this prompt.
	 */
	private final MessageFormat questionFormat;
	
	private Rectangle oldBounds;

	public DataSourceUserPrompter(UserPromptOptions optionType, UserPromptResponse defaultResponseType, SPDataSource defaultResponse, JFrame frame, String questionMessage,
			DataSourceCollection collection, String ...  buttonNames) {
		if(optionType.getButtonNum() != buttonNames.length) {
			throw new IllegalStateException("Expecting " + optionType.getButtonNum() + 
					"arguments for the optionType " + optionType + "Recieved only " + buttonNames.length + "arguments\n" +
					Arrays.toString(buttonNames));
		}
		this.defaultResponseType = defaultResponseType;
		selectedDataSource = defaultResponse;
		this.owner = frame;
		this.dsCollection = collection;
		questionFormat = new MessageFormat(questionMessage);
		
		userPrompt = new JDialog(owner);
		
		FormLayout layout = new FormLayout("pref:grow, 4dlu, pref, 4dlu, pref");
		final DefaultFormBuilder builder = new DefaultFormBuilder(layout);
		builder.setDefaultDialogBorder();
		
		questionLabel = new JLabel();
		builder.append(questionLabel, 5);
		builder.nextLine();
		
		dsComboBox = new JComboBox(collection.getConnections().toArray());
		builder.append(Messages.getString("DataSourceUserPrompter.selectDataSource"), dsComboBox);
		JButton okButton = new JButton();
		if(optionType == UserPromptOptions.OK_NEW_NOTOK_CANCEL || optionType == UserPromptOptions.OK_NOTOK_CANCEL
        		|| optionType == UserPromptOptions.OK_NEW_CANCEL || optionType == UserPromptOptions.OK_CANCEL) {
			okButton.setAction(new AbstractAction(buttonNames[0]) {
				public void actionPerformed(ActionEvent e) {
					selectedDataSource = (SPDataSource) dsComboBox.getSelectedItem();
					response = UserPromptResponse.OK;
					userPrompt.setVisible(false);
			}
		});
		builder.append(okButton);
		}
		builder.nextLine();
		
		final JPanel newDSPanel = new JPanel(new BorderLayout());
		newDSPanel.setVisible(false);
		ButtonBarBuilder bbBuilder = new ButtonBarBuilder();
		bbBuilder.addGlue();
		
		final JButton newDBButton = new JButton();
		if(optionType == UserPromptOptions.OK_NEW_CANCEL || optionType == UserPromptOptions.OK_NEW_NOTOK_CANCEL) {
			newDBButton.setAction(new AbstractAction(buttonNames[1]) {
			public void actionPerformed(ActionEvent e) {
				newDSPanel.setVisible(true);
				userPrompt.validate();
				oldBounds = userPrompt.getBounds();
				Rectangle newBounds = userPrompt.getBounds();
				newBounds.setSize((int) Math.max(oldBounds.getWidth(), newDSPanel.getWidth()), (int) oldBounds.getHeight() + newDSPanel.getHeight());
				userPrompt.setBounds(newBounds);
				
				newDBButton.setEnabled(false);

			}
		});
		bbBuilder.addFixed(newDBButton);
		}
		
		if(optionType == UserPromptOptions.OK_NEW_NOTOK_CANCEL || optionType == UserPromptOptions.OK_NOTOK_CANCEL) {
			bbBuilder.addRelatedGap();
			bbBuilder.addFixed(new JButton(new AbstractAction((optionType == UserPromptOptions.OK_NOTOK_CANCEL)? buttonNames[1] : buttonNames[2]) {
		
			public void actionPerformed(ActionEvent e) {
				selectedDataSource = null;
				response = UserPromptResponse.NOT_OK;
				userPrompt.setVisible(false);
			}
		}));
		}
		
		JButton cancelButton = new JButton();
		if(optionType == UserPromptOptions.OK_NEW_NOTOK_CANCEL || optionType == UserPromptOptions.OK_NOTOK_CANCEL
        		|| optionType == UserPromptOptions.OK_NEW_CANCEL || optionType == UserPromptOptions.OK_CANCEL) {
			bbBuilder.addRelatedGap();
		    cancelButton.setAction(new AbstractAction(buttonNames[buttonNames.length-1]) {
			
		    	public void actionPerformed(ActionEvent e) {
		    		selectedDataSource = null;
		    		response = UserPromptResponse.CANCEL;
		    		userPrompt.setVisible(false);
			}
		});
		bbBuilder.addFixed(cancelButton);
		}
		builder.append(bbBuilder.getPanel(), 5);
		
		final SPDataSource newDS = new SPDataSource(dsCollection);
		
		final SPDataSourcePanel SPDSPanel = new SPDataSourcePanel(newDS);
		newDSPanel.add(SPDSPanel.getPanel(), BorderLayout.CENTER);
		
		bbBuilder = new ButtonBarBuilder();
		bbBuilder.addGlue();
		bbBuilder.addGridded(new JButton(new AbstractAction("Cancel") {
		
			public void actionPerformed(ActionEvent e) {
				newDBButton.setEnabled(true);
				userPrompt.validate();
				userPrompt.setBounds(oldBounds);
				newDSPanel.setVisible(false);
			}
		}));
		
		bbBuilder.addGridded(new JButton(new AbstractAction("OK") {
		
			public void actionPerformed(ActionEvent e) {
				SPDSPanel.applyChanges();
				dsCollection.addDataSource(newDS);
				selectedDataSource = newDS;
				response = UserPromptResponse.NEW;
				userPrompt.setVisible(false);
				newDSPanel.setVisible(false);
			}
		}));
		newDSPanel.add(bbBuilder.getPanel(), BorderLayout.SOUTH);
		newDSPanel.setBorder(BorderFactory.createLoweredBevelBorder());
		
		builder.append(newDSPanel, 5);
		newDSPanel.setVisible(false);
		
		userPrompt.add(builder.getPanel());
		userPrompt.setModal(true);
	}

	public Object getUserSelectedResponse() {
		return selectedDataSource;
	}

	public UserPromptResponse promptUser(final Object... formatArgs) {
		if (logger.isDebugEnabled()) {
            logger.debug("Prompting user. Format Args: " + Arrays.asList(formatArgs)); //$NON-NLS-1$
        }
        
        // The default response, in case the user closes the dialog without
        // pressing one of the buttons
        response = defaultResponseType;
        
        Runnable promptUser = new Runnable() {
            public void run() {
                questionLabel.setText(questionFormat.format(formatArgs));
                userPrompt.validate();
                if (firstPrompt) {
                    userPrompt.pack();
                    userPrompt.setLocationRelativeTo(owner);
                    firstPrompt = false;
                }
                userPrompt.setVisible(true);
            }
        };

        if (SwingUtilities.isEventDispatchThread()) {
          promptUser.run();
        } else {
            try {
                SwingUtilities.invokeAndWait(promptUser);
            } catch (InterruptedException e) {
                SPSUtils.showExceptionDialogNoReport(owner,
                        Messages.getString("ModalDialogUserPrompter.showPromptDialogFailed"), e); //$NON-NLS-1$
            } catch (InvocationTargetException e) {
                SPSUtils.showExceptionDialogNoReport(owner,
                        Messages.getString("ModalDialogUserPrompter.showPromptDialogFailed"), e); //$NON-NLS-1$
            }
        }
        
        return response;
	}

}
