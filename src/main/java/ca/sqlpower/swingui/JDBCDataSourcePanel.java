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
package ca.sqlpower.swingui;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import org.apache.log4j.Logger;

import ca.sqlpower.sql.DataSourceCollection;
import ca.sqlpower.sql.JDBCDataSource;
import ca.sqlpower.sql.JDBCDataSourceType;
import ca.sqlpower.sql.SPDataSource;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

public class JDBCDataSourcePanel implements DataEntryPanel {

	private static final Logger logger = Logger.getLogger(JDBCDataSourcePanel.class);

	protected static final String EXTRA_FIELD_LABEL_PROP = "ca.sqlpower.swingui.LABEL"; //$NON-NLS-1$
	
    /**
     * The panel that holds the GUI.  This panel is built only once,
     * on the first call to getPanel().  It is not initialized in the
     * constructor, because subclasses need a chance to call {@link #addExtraField(JComponent)}
     * before the panel is built.
     */
    private JPanel panel;
    
    /**
     * The data source we're editing.
     */
	private final JDBCDataSource dbcs;
    
	private JTextField dbNameField;
	private JComboBox dataSourceTypeBox;
	private PlatformSpecificConnectionOptionPanel platformSpecificOptions;
	private JTextField dbUrlField;
	private JTextField dbUserField;
	private JPasswordField dbPassField;
	private JButton dbTestConnection;
	private JLabel dbTestResult;
	
	/**
	 * Contains information on the DB connection and system properties.
	 */
	private JTextArea sysprops;

	/**
	 * Extra data entry fields provided by subclasses. These fields will be
	 * added to the layout, but not used in any other way. It's up to subclasses
	 * to set up the component in the constructor, and also override the
	 * applyChanges method to ensure the new field values are captured. The
	 * label given to these fields will be the EXTRA_FIELD_LABEL_PROP
	 * client property. See {@link JComponent#putClientProperty(Object, Object)}.
	 */
	private List<JComponent> extraFields = new ArrayList<JComponent>();

    /**
     * Controls whether or not {@link #applyChanges()} will enforce the policy
     * of requiring data sources to have unique names within their data source
     * collection. You almost always want this to be true (which is the
     * default), but in rare cases such as the "target database properties" in
     * Power*Architect, which is actually letting you modify a copy of the
     * original data source, this check needs to be disabled.
     */
    private boolean enforcingUniqueName = true;
	
	/**
	 * Remembers the given data source, but does not build the GUI.  That
	 * gets done the first time getPanel() is called.
	 * 
	 * @param ds The data source to edit.  It is the only data source this instance
	 * will ever be able to edit.
	 */
	public JDBCDataSourcePanel(JDBCDataSource ds) {
	    this.dbcs = ds;
	}

    /**
     * Builds and returns a Swing component that has all the general database
     * settings (the ones that are always required no matter what you want to
     * use this connection for).
     */
    private JPanel buildGeneralPanel(JDBCDataSource dbcs) {
        DataSourceCollection<SPDataSource> dsCollection = dbcs.getParentCollection();
        List<JDBCDataSourceType> dataSourceTypes = dsCollection.getDataSourceTypes();
        dataSourceTypes.add(0, new JDBCDataSourceType());
        dataSourceTypeBox = new JComboBox(dataSourceTypes.toArray());
        dataSourceTypeBox.setRenderer(new SPDataSourceTypeListCellRenderer());
        dataSourceTypeBox.setSelectedIndex(0);
        
        // if this data source has no parent, it is a root data source
        if (dbcs.isParentSet()) {
            logger.debug("A PARENT! setting selected item to: \"" + dbcs.getParentType() + "\""); //$NON-NLS-1$ //$NON-NLS-2$
            dataSourceTypeBox.setSelectedItem(dbcs.getParentType());
        } else {
            logger.debug("NO PARENT! setting selected item to: \"" + dbcs + "\""); //$NON-NLS-1$ //$NON-NLS-2$
            dataSourceTypeBox.setSelectedItem(dbcs);
        }
        
        dbNameField = new JTextField(dbcs.getName());
        dbNameField.setName("dbNameField"); //$NON-NLS-1$
        
        logger.debug("dbcs.getUrl() returns " + dbcs.getUrl()); //$NON-NLS-1$
        dbUrlField = new JTextField(dbcs.getUrl());
        
        platformSpecificOptions = new PlatformSpecificConnectionOptionPanel(dbUrlField);
        if (dbcs.isParentSet()) {
        	platformSpecificOptions.setTemplate(dbcs.getParentType());
        }
        
        dbTestResult = new JLabel();
        sysprops = new JTextArea();
        sysprops.setBorder( null );
        sysprops.setOpaque( false );
        sysprops.setEditable( false ); 
        sysprops.setFont(dbTestResult.getFont());
        
        dbTestConnection = new JButton(new AbstractAction(Messages.getString("SPDataSourcePanel.testConnectionActionName")) { //$NON-NLS-1$
			public void actionPerformed(ActionEvent e) {
				sysprops.setText("");
				Connection con = null;
				try {
					JDBCDataSource dbcs = new JDBCDataSource(JDBCDataSourcePanel.this.dbcs.getParentCollection());
					String name = dbNameField.getText();
					dbcs.setName(name);
					dbcs.setDisplayName(name);
					dbcs.setParentType((JDBCDataSourceType) dataSourceTypeBox.getSelectedItem());
					dbcs.setUrl(dbUrlField.getText());
					dbcs.setUser(dbUserField.getText());
					dbcs.setPass(new String(dbPassField.getPassword()));
					con = dbcs.createConnection();
					
					// No exception thrown, so success!
					dbTestResult.setText(Messages.getString("SPDataSourcePanel.connectionTestSuccessful")); //$NON-NLS-1$
					sysprops.append(JDBCDataSource.getConnectionInfoString(dbcs, true));
				} catch (SQLException ex) {
					dbTestResult.setText(Messages.getString("SPDataSourcePanel.connectionTestFailed")); //$NON-NLS-1$
					SPSUtils.showExceptionDialogNoReport(panel, Messages.getString("SPDataSourcePanel.connectionTestException"), ex); //$NON-NLS-1$
				} finally {
					if (con != null) {
						try {
							con.close();
						} catch (SQLException ex) {
							logger.error("Failed to close connection!", ex); //$NON-NLS-1$
						}
					}
				}
			}
        });
        
        //we know this should be set to pref but one of the components seems to be updating the
        //preferred size
        DefaultFormBuilder builder = new DefaultFormBuilder(new FormLayout("pref, 4dlu, 0:grow"));  //$NON-NLS-1$
        builder.append(Messages.getString("SPDataSourcePanel.connectionNameLabel"), dbNameField); //$NON-NLS-1$
        builder.append(Messages.getString("SPDataSourcePanel.databaseTypeLabel"), dataSourceTypeBox); //$NON-NLS-1$
        builder.append(Messages.getString("SPDataSourcePanel.connectionOptionsLabel"), platformSpecificOptions.getPanel()); //$NON-NLS-1$
        builder.append(Messages.getString("SPDataSourcePanel.jdbcUrlLabel"), dbUrlField); //$NON-NLS-1$
        builder.append(Messages.getString("SPDataSourcePanel.usernameLabel"), dbUserField = new JTextField(dbcs.getUser())); //$NON-NLS-1$
        builder.append(Messages.getString("SPDataSourcePanel.passwordLabel"), dbPassField = new JPasswordField(dbcs.getPass())); //$NON-NLS-1$
                
        // extra fields supplied by subclasses
        for (JComponent extraField : extraFields) {
        	builder.append((String) extraField.getClientProperty(EXTRA_FIELD_LABEL_PROP), extraField);
        }
        
        builder.append(dbTestConnection, dbTestResult);
        builder.append("\t\t", sysprops );
        
        dataSourceTypeBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                JDBCDataSourceType parentType =
                    (JDBCDataSourceType) dataSourceTypeBox.getSelectedItem();
                platformSpecificOptions.setTemplate(parentType);
            }
        });
        
        // ensure enough width for the platform specific options
        JPanel p = builder.getPanel();
        p.setPreferredSize(new Dimension(600, 300));

        return p;
    }
    
    /**
     * Provides access to the combo box of data source types in this panel.
     * Some outside classes that need to collaborate with this panel need
     * to know when the user has selected a different data source type,
     * and if you've got one, you can use this method to get the combo box
     * and add an ItemListener to it.
     */
    public JComboBox getDataSourceTypeBox() {
        return dataSourceTypeBox;
    }
    
    /**
     * Returns a reference to the data source this panel is editing (that is,
     * the one that will be updated when apply() is called).
     */
    public JDBCDataSource getDbcs() {
        return dbcs;
    }

    /**
     * Adds an extra data entry field which will be laid out after the
     * rest of the components in this panel.  The component must have
     * its EXTRA_FIELD_LABEL_PROP client property set to the label
     * you want the field to have.
     * <p>
     * You can call this method as many times as you want, but only before
     * the first call to {@link #getPanel()}.  After that, it is an error
     * to call this method.
     * 
     * @param component The component to add.
     */
    protected void addExtraField(JComponent component) {
    	if (panel != null) throw new IllegalStateException("You can't do this after calling getPanel()"); //$NON-NLS-1$
    	extraFields.add(component);
    }
    
    // -------------------- DATE ENTRY PANEL INTERFACE -----------------------

	/**
	 * Copies the properties displayed in the various fields back into
	 * the current SPDataSource.  You still need to call getDbcs()
	 * and save the connection spec yourself.
	 */
	public boolean applyChanges() {
        
        dbNameField.setText(dbNameField.getText().trim());
        
        if ("".equals(dbNameField.getText())) { //$NON-NLS-1$
            JOptionPane.showMessageDialog(panel,
                    Messages.getString("SPDataSourcePanel.blankNameNotAllowed")); //$NON-NLS-1$
            return false;
        }
        
        SPDataSource existingDSWithThisName = dbcs.getParentCollection().getDataSource(dbNameField.getText());
        if (enforcingUniqueName  && existingDSWithThisName != null && existingDSWithThisName != dbcs) {
            JOptionPane.showMessageDialog(panel, Messages.getString("SPDataSourcePanel.connectionAlreadyExists", dbNameField.getText())); //$NON-NLS-1$
            return false;
        }
        
        logger.debug("Applying changes..."); //$NON-NLS-1$
        
		String name = dbNameField.getText();
		dbcs.setName(name);
		dbcs.setDisplayName(name);
		dbcs.setParentType((JDBCDataSourceType) dataSourceTypeBox.getSelectedItem());
		dbcs.setUrl(dbUrlField.getText());
		dbcs.setUser(dbUserField.getText());
		dbcs.setPass(new String(dbPassField.getPassword())); // completely defeats the purpose for JPasswordField.getText() being deprecated, but we're saving passwords to the config file so it hardly matters.

        return true;
	}

	/**
	 * Does nothing right now, because there is nothing to discard or clean up.
	 */
	public void discardChanges() {
        // nothing to discard
	}

    /**
     * Returns the panel that holds the user interface for the datatbase
     * connection settings.
     */
    public JPanel getPanel() {
    	if (panel == null) {
    		panel = buildGeneralPanel(dbcs);
    	}
        return panel;
    }

	public boolean hasUnsavedChanges() {
		//TODO: tell the truth
		return true;
	}
	
    /**
     * Controls whether or not {@link #applyChanges()} will enforce the policy
     * of requiring data sources to have unique names within their data source
     * collection. You almost always want this to be true (which is the
     * default), but in rare cases such as the "target database properties" in
     * Power*Architect, which is actually letting you modify a copy of the
     * original data source, this check needs to be disabled.
     */
	public void setEnforcingUniqueName(boolean enforceUniqueName) {
        this.enforcingUniqueName = enforceUniqueName;
    }

    /**
     * Returns the flag indicating whether or not {@link #applyChanges()} will
     * insist that the data source's new name is unique.
     */
	public boolean isEnforcingUniqueName() {
        return enforcingUniqueName;
    }
}
