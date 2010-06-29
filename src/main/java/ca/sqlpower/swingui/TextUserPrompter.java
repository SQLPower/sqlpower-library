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

import java.awt.event.ActionEvent;
import java.lang.reflect.InvocationTargetException;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import ca.sqlpower.util.UserPrompter;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.factories.ButtonBarFactory;
import com.jgoodies.forms.layout.FormLayout;

/**
 * Prompts the user for a new string.
 */
public class TextUserPrompter implements UserPrompter {

    /**
     * This text field starts with the default response in it and can be changed
     * to the response the user wants the dialog to have for its question.
     */
    private final JTextField textField;
    
    /**
     * The main panel that will display the prompt and buttons.
     */
    private final JDialog prompt;
    
    /**
     * The response the user gave for this prompter.
     */
    private UserPromptResponse response;

    /**
     * The default response for the prompt as given to the constructor.
     */
    private final UserPromptResponse defaultResponseType;

    /**
     * The owner of the dialog made by this prompter.
     */
    private final JFrame owner;

    /**
     * @param question
     *            A question string that asks the user what the string to be
     *            replaced should actually be.
     * @param defaultResponseType
     *            The default button that will be highlighted.
     * @param defaultResponse
     *            The default response to use if the user wants to click through
     *            the dialog. This will be displayed as the editable default
     *            text and must be a String object.
     * @param okName
     *            The text that goes on the OK button.
     * @param cancelName
     *            The text that goes on the cancel button.
     */
    public TextUserPrompter(JFrame owner, String question, UserPromptResponse defaultResponseType, 
            Object defaultResponse, String okName, String cancelName) {
        this.owner = owner;
        this.defaultResponseType = defaultResponseType;
        textField = new JTextField((String) defaultResponse);
        
        DefaultFormBuilder builder = new DefaultFormBuilder(new FormLayout("pref"));
        builder.setDefaultDialogBorder();
        builder.append(new JLabel(question));
        builder.nextLine();
        builder.append(textField);
        builder.nextLine();
        final JButton okButton = new JButton(new AbstractAction(okName) {
            public void actionPerformed(ActionEvent e) {
                response = UserPromptResponse.OK;
                prompt.dispose();
            }
        });
        final JButton cancelButton = new JButton(new AbstractAction(cancelName) {
            public void actionPerformed(ActionEvent e) {
                response = UserPromptResponse.CANCEL;
                prompt.dispose();
            }
        });
        JPanel okCancelBar = ButtonBarFactory.buildOKCancelBar(okButton, cancelButton);
        builder.append(okCancelBar);
        prompt = new JDialog(owner);
        prompt.add(builder.getPanel());
    }

    public Object getUserSelectedResponse() {
        if (UserPromptResponse.OK.equals(response)) {
            return textField.getText();
        }
        return null;
    }

    public UserPromptResponse promptUser(Object... formatArgs) {
        response = defaultResponseType;
        
        Runnable promptUser = new Runnable() {
            public void run() {
                prompt.pack();
                prompt.setLocationRelativeTo(owner);
                prompt.setVisible(true);
            }
        };

        prompt.setModal(true);
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
