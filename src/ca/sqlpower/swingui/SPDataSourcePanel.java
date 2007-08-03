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

import java.awt.Dimension;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import org.apache.log4j.Logger;

import ca.sqlpower.swingui.SPDataSourceTypeListCellRenderer;
import ca.sqlpower.swingui.PlatformSpecificConnectionOptionPanel;
import ca.sqlpower.sql.DataSourceCollection;
import ca.sqlpower.sql.SPDataSource;
import ca.sqlpower.sql.SPDataSourceType;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

public class SPDataSourcePanel implements DataEntryPanel {

	private static final Logger logger = Logger.getLogger(SPDataSourcePanel.class);

	protected static final String EXTRA_FIELD_LABEL_PROP = "ca.sqlpower.swingui.LABEL";
	
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
	private final SPDataSource dbcs;
    
	private JTextField dbNameField;
	private JComboBox dataSourceTypeBox;
	private PlatformSpecificConnectionOptionPanel platformSpecificOptions;
	private JTextField dbUrlField;
	private JTextField dbUserField;
	private JPasswordField dbPassField;

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
	 * Remembers the given data source, but does not build the GUI.  That
	 * gets done the first time getPanel() is called.
	 * 
	 * @param ds The data source to edit.  It is the only data source this instance
	 * will ever be able to edit.
	 */
	public SPDataSourcePanel(SPDataSource ds) {
	    this.dbcs = ds;
	}

    /**
     * Builds and returns a Swing component that has all the general database
     * settings (the ones that are always required no matter what you want to
     * use this connection for).
     */
    private JPanel buildGeneralPanel(SPDataSource dbcs) {
        DataSourceCollection dsCollection = dbcs.getParentCollection();
        List<SPDataSourceType> dataSourceTypes = dsCollection.getDataSourceTypes();
        dataSourceTypes.add(0, new SPDataSourceType());
        dataSourceTypeBox = new JComboBox(dataSourceTypes.toArray());
        dataSourceTypeBox.setRenderer(new SPDataSourceTypeListCellRenderer());
        dataSourceTypeBox.setSelectedIndex(0);
        
        // if this data source has no parent, it is a root data source
        if (dbcs.isParentSet()) {
            System.out.println("A PARENT! setting selected item to: \"" + dbcs.getParentType() + "\"");
            dataSourceTypeBox.setSelectedItem(dbcs.getParentType());
        } else {
            System.out.println("NO PARENT! setting selected item to: \"" + dbcs + "\"");
            dataSourceTypeBox.setSelectedItem(dbcs);
        }
        
        dbNameField = new JTextField(dbcs.getName());
        dbNameField.setName("dbNameField");
        platformSpecificOptions = new PlatformSpecificConnectionOptionPanel(dbUrlField = new JTextField(dbcs.getUrl()));

        //we know this should be set to pref but one of the components seems to be updating the
        //preferred size
        DefaultFormBuilder builder = new DefaultFormBuilder(new FormLayout("pref, 4dlu, 0:grow")); 
        builder.append("Connection &Name", dbNameField);
        builder.append("&Database Type", dataSourceTypeBox);
        builder.append("Connect &Options", platformSpecificOptions.getPanel());
        builder.append("JDBC &URL", dbUrlField);
        builder.append("Use&rname", dbUserField = new JTextField(dbcs.getUser()));
        builder.append("&Password", dbPassField = new JPasswordField(dbcs.getPass()));
        
        // extra fields supplied by subclasses
        for (JComponent extraField : extraFields) {
        	builder.append((String) extraField.getClientProperty(EXTRA_FIELD_LABEL_PROP), extraField);
        }
        
        dataSourceTypeBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                SPDataSourceType parentType =
                    (SPDataSourceType) dataSourceTypeBox.getSelectedItem();
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
    public SPDataSource getDbcs() {
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
    	if (panel != null) throw new IllegalStateException("You can't do this after calling getPanel()");
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
        
        if ("".equals(dbNameField.getText())) {
            JOptionPane.showMessageDialog(panel,
                    "A connection name must have at least 1 character that is not whitespace");
            return false;
        }
        
        SPDataSource existingDSWithThisName = dbcs.getParentCollection().getDataSource(dbNameField.getText());
        if (existingDSWithThisName != null && existingDSWithThisName != dbcs) {
            JOptionPane.showMessageDialog(panel, "A connection with the name \"" +
                    dbNameField.getText() + "\" already exists");
            return false;
        }
        
        logger.debug("Applying changes...");
        
		String name = dbNameField.getText();
		dbcs.setName(name);
		dbcs.setDisplayName(name);
		dbcs.setParentType((SPDataSourceType) dataSourceTypeBox.getSelectedItem());
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
}
