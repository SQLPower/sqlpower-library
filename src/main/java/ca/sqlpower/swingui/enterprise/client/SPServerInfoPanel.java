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

package ca.sqlpower.swingui.enterprise.client;

import java.awt.Component;
import java.net.URI;
import java.net.URISyntaxException;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import ca.sqlpower.enterprise.client.ConnectionTestAction;
import ca.sqlpower.enterprise.client.SPServerInfo;
import ca.sqlpower.swingui.DataEntryPanel;
import ca.sqlpower.util.Version;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

/**
 * Creates a panel for setting the properties of a SPServerInfo. Since
 * instances of SPServerInfo are not mutable, calling applyChanges() will not
 * modify the original SPServerInfo object provided in the constructor. You
 * must obtain a new SPServerInfo object by calling getServerInfo().
 */
public class SPServerInfoPanel implements DataEntryPanel {

    private final Component dialogOwner;

    private final JPanel panel;

    private JTextField name;
    private JTextField host;
    private JTextField port;
    private JTextField path;
    private JTextField username;
    private JPasswordField password;
    private JButton testButton;
    
    /**
     * The version of this client to compare to each server version to determine compatibility
     */
    private final Version clientVersion;
    
    private final boolean passwordAllowed;
    
    private final String defaultScheme;
    
	/**
	 * Create a {@link SPServerInfoPanel} populated with the given default
	 * settings
	 * 
	 * @param dialogOwner
	 *            The parent {@link Component} for the dialog containing this
	 *            {@link SPServerInfoPanel}
	 * @param clientVersion
	 *            The version of the client to be used to determine
	 *            compatibility with the server
	 * @param defaultSettings
	 *            A {@link SPServerInfo} instance set with the default
	 *            configuration
	 */
    public SPServerInfoPanel(Component dialogOwner, Version clientVersion, SPServerInfo defaultSettings) {
        this.dialogOwner = dialogOwner;
        panel = buildUI(defaultSettings);
        defaultScheme = defaultSettings.getScheme();
        passwordAllowed = defaultSettings.isPasswordAllowed();
        this.clientVersion = clientVersion;
    }
    
    private JPanel buildUI(SPServerInfo si) {
        DefaultFormBuilder builder = new DefaultFormBuilder(new FormLayout("pref, 4dlu, max(100dlu; pref):grow")); //$NON-NLS-1$
        
        builder.append(Messages.getString("SPServerInfoPanel.displayNameField"), name = new JTextField(si.getName())); //$NON-NLS-1$
        builder.append(Messages.getString("SPServerInfoPanel.hostField"), host = new JTextField(si.getServerAddress())); //$NON-NLS-1$
        builder.append(Messages.getString("SPServerInfoPanel.portField"), port = new JTextField(String.valueOf(si.getPort()))); //$NON-NLS-1$
        builder.append(Messages.getString("SPServerInfoPanel.pathField"), path = new JTextField(si.getPath())); //$NON-NLS-1$
        builder.append(Messages.getString("SPServerInfoPanel.usernameField"), username = new JTextField(si.getUsername())); //$NON-NLS-1$
        if (si.isPasswordAllowed()) {
        	builder.append(Messages.getString("SPServerInfoPanel.passwordField"), password = new JPasswordField(si.getPassword())); //$NON-NLS-1$
        }
        
        builder.append(testButton = new JButton(Messages.getString("SPServerInfoPanel.testConnectionButton"))); //$NON-NLS-1$
        builder.appendParagraphGapRow();
        
        return builder.getPanel();
    }
    
    public void setTestAction(ConnectionTestAction action) {
    	testButton.setAction(action);
    }
    
    /**
     * Returns a new SPServerInfo object which has been configured based on the
     * settings currently in this panel's fields.
     */
    public SPServerInfo getServerInfo() {
//    	lookupServerInfo(false); TODO: re-enable/replace when connection testing is implemented
        int port = Integer.parseInt(this.port.getText());
        SPServerInfo si;
        if (passwordAllowed) {
        	si = new SPServerInfo(
                name.getText(), host.getText(), port, path.getText(), 
                username.getText(), new String(password.getPassword()));
        } else {
        	si = new SPServerInfo(defaultScheme,
                    name.getText(), host.getText(), port, path.getText(), 
                    username.getText());
        }
        return si;
    }
    
    public JComponent getPanel() {
        return panel;
    }
    
    /**
     * Checks fields for validity, but does not modify the SPServerInfo given in
     * the constructor (this is not possible because it's immutable). If any of
     * the fields contain inappropriate entries, the user will be told so in a
     * dialog.
     * 
     * @return true if all the fields contain valid values; false if there are
     *         invalid fields.
     */
    public boolean applyChanges() {
    	
    	if (this.name.getText()==null||this.name.getText().equals("")) { //$NON-NLS-1$
    		JOptionPane.showMessageDialog(
    				dialogOwner, Messages.getString("SPServerInfoPanel.giveConnectionName"), //$NON-NLS-1$
    				Messages.getString("SPServerInfoPanel.nameRequired"), JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
    		return false;
    	}
    	
    	String port = this.port.getText();
    	try {
    		Integer.parseInt(port);
    	} catch (NumberFormatException ex) {
    		JOptionPane.showMessageDialog(
    				dialogOwner, Messages.getString("SPServerInfoPanel.portMustBeNumber"), //$NON-NLS-1$
    				Messages.getString("SPServerInfoPanel.invalidPortNumber"), JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
    		return false;
    	}
    	
    	if (!this.path.getText().startsWith("/")) { //$NON-NLS-1$
    		this.path.setText("/".concat(this.path.getText()==null?"":this.path.getText())); //$NON-NLS-1$ //$NON-NLS-2$
    	}
    	String path = this.path.getText();
    	if (path == null || path.length() < 2) {
    		JOptionPane.showMessageDialog(
    				dialogOwner, Messages.getString("SPServerInfoPanel.pathMustBeginWithSlash"), //$NON-NLS-1$
    				Messages.getString("SPServerInfoPanel.invalidSetting"), JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
    		return false;
    	}
    	
    	if (this.host.getText().startsWith("http://")) { //$NON-NLS-1$
    		this.host.setText(this.host.getText().replace("http://", "")); //$NON-NLS-1$ //$NON-NLS-2$
    	}
    	String host = this.host.getText();
    	try {
    		new URI("http", null, host, Integer.parseInt(port), path, null, null); //$NON-NLS-1$
    	} catch (URISyntaxException e) {
    		JOptionPane.showMessageDialog(
    				dialogOwner, Messages.getString("SPServerInfoPanel.problemWithHostName"), //$NON-NLS-1$
    				"", JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
    		return false;
    	}
        
        return true;
    }

    public void discardChanges() {
        // nothing to do
    }

    public boolean hasUnsavedChanges() {
        return true;
    }    
}
