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

package ca.sqlpower.swingui;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import ca.sqlpower.util.UserPrompter;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.debug.FormDebugPanel;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;


public class ModalDialogListUserPrompter<T> implements UserPrompter {

	private final T defaultResponse;
	private final JFrame parentFrame;
	private JDialog confirmDialog;
	private JCheckBox applyToAll;
	private JTextArea questionField;
	private JComboBox optionBox;
	private T response;
	private UserPromptResponse responseButton;
	private boolean firstPrompt = true;

	public ModalDialogListUserPrompter(JFrame parentFrame, String question, List<T> responses, T defaultResponse) {
		this.parentFrame = parentFrame;
		this.defaultResponse = defaultResponse;
        applyToAll = new JCheckBox(Messages.getString("ModalDialogUserPrompter.applyToAllOption")); //$NON-NLS-1$
        
        confirmDialog = new JDialog(parentFrame);
        
        // FIXME the title needs to be configurable and/or set itself based on prompt type
        confirmDialog.setTitle(""); //$NON-NLS-1$
        
        // this is just filled with the message pattern template to help with sizing
        questionField = new JTextArea(question);
        questionField.setEditable(false);
        questionField.setBackground(null);
        
        JPanel confirmPanel = new JPanel();
        FormLayout formLayout = new FormLayout("pref:grow" //$NON-NLS-1$
                                                , ""); //$NON-NLS-1$
        
        optionBox = new JComboBox();
        for (T item : responses) {
        	optionBox.addItem(item);
        }
        
        optionBox.setSelectedItem(defaultResponse);
        DefaultFormBuilder builder = new DefaultFormBuilder(formLayout, confirmPanel);
        builder.setDefaultDialogBorder();
        
        builder.append(questionField);

        builder.nextLine();
        builder.append(optionBox);
        
        builder.nextLine();
        ButtonBarBuilder buttonBar = new ButtonBarBuilder();
        buttonBar.addGlue();
        JButton okButton = new JButton();
        okButton.setText("Ok");
        buttonBar.addGridded(okButton);
        buttonBar.addGlue();
        
        okButton.addActionListener(new ActionListener() {
			@SuppressWarnings("unchecked")
			public void actionPerformed(ActionEvent e) {
				responseButton = UserPromptResponse.OK;
                response = (T) optionBox.getSelectedItem();
                confirmDialog.dispose();
            }
        });
        builder.append(""); //$NON-NLS-1$
        builder.append(buttonBar.getPanel());
        builder.nextLine();
        
        builder.append(""); //$NON-NLS-1$
        builder.append(applyToAll);
        
        okButton.requestFocusInWindow();

        confirmDialog.setModal(true);
        confirmDialog.add(builder.getPanel());
	}
	
	public T getUserSelectedResponse() {
		return response;
	}

	public UserPromptResponse promptUser(Object... formatArgs) {
		try {
			if (applyToAll.isSelected()) {
				return UserPromptResponse.OK;
			}
			Runnable runner = new Runnable() {
			    public void run() {
			        if (firstPrompt) {
			            confirmDialog.pack();
			            confirmDialog.setLocationRelativeTo(parentFrame);
			            firstPrompt = false;
			        }
			        confirmDialog.setVisible(true);
			    }
			};
			if (SwingUtilities.isEventDispatchThread()) {
				runner.run();
			} else {
				SwingUtilities.invokeAndWait(runner);
			}
		} catch (InterruptedException e) {
            SPSUtils.showExceptionDialogNoReport(parentFrame,
                    Messages.getString("ModalDialogUserPrompter.showPromptDialogFailed"), e); //$NON-NLS-1$
        } catch (InvocationTargetException e) {
            SPSUtils.showExceptionDialogNoReport(parentFrame,
                    Messages.getString("ModalDialogUserPrompter.showPromptDialogFailed"), e); //$NON-NLS-1$
        }
        return responseButton;
	}
}
