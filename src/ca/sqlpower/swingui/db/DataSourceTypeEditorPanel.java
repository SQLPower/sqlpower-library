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
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;

import org.apache.log4j.Logger;

import ca.sqlpower.sql.DataSourceCollection;
import ca.sqlpower.sql.JDBCDataSourceType;
import ca.sqlpower.swingui.DataEntryPanel;
import ca.sqlpower.swingui.DataEntryPanelBuilder;
import ca.sqlpower.swingui.Messages;
import ca.sqlpower.swingui.PlatformSpecificConnectionOptionPanel;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class DataSourceTypeEditorPanel implements DataEntryPanel {

    private static final Logger logger = Logger.getLogger(DataSourceTypeEditorPanel.class);
    
    private JDBCDataSourceType dsType;
    private JPanel panel;
    private JTabbedPane tabbedPane;
    final private JTextField name = new JTextField();
    final private JTextField connectionStringTemplate = new JTextField();
    final private JTextField driverClass = new JTextField();
    final private PlatformSpecificConnectionOptionPanel template =
        new PlatformSpecificConnectionOptionPanel(new JTextField());
    
    final private JButton copyPropertiesButton;
    
    /**
     * The panel that maintains the currently-selected data source type's
     * classpath.
     */
    private final JDBCDriverPanel jdbcPanel;
    
    /**
     * A list of DataSourceTypeEditorTabPanels that are used in addition to the main editor panel.
     * Generally, we use this to add application-specific connection properties.
     */
    private List<DataSourceTypeEditorTabPanel> tabPanels = new ArrayList<DataSourceTypeEditorTabPanel>();

    /**
     * Creates a multi-tabbed panel with facilities for configuring all the
     * database types defined in a particular data source collection.
     * 
     * @param collection
     *            The data source collection to edit.
     * @param serverBaseURI
     *            The base URI to the server the JDBC driver JAR files may be
     *            stored on. If the data source collection doesn't refer to any
     *            JAR files on a server, this URI may be specified as null.
     * @param owner The Window that should own any dialogs created within the editor GUI.
     * @see DataSourceTypeEditor
     * @see DefaultDataSourceTypeDialogFactory
     */
    public DataSourceTypeEditorPanel(final DataSourceCollection collection, final Window owner) {
    	jdbcPanel = new JDBCDriverPanel(collection.getServerBaseURI());
    	
    	jdbcPanel.addDriverTreeSelectionListener(new TreeSelectionListener() {
			public void valueChanged(TreeSelectionEvent e) {
				if (e.getNewLeadSelectionPath() != null && e.getNewLeadSelectionPath().getPathCount() > JDBCDriverPanel.DRIVER_LEVEL) {
					driverClass.setText(e.getNewLeadSelectionPath().getLastPathComponent().toString());
				}
			}
		});
    	
    	copyPropertiesButton = new JButton(new AbstractAction("Copy Properties...") {
			public void actionPerformed(ActionEvent e) {
				final DataSourceTypeCopyPropertiesPanel copyPropertiesPanel = new DataSourceTypeCopyPropertiesPanel(dsType, collection);
				final JDialog d = DataEntryPanelBuilder.createDataEntryPanelDialog(
						copyPropertiesPanel,
						owner,
						"Copy Properties",
						DataEntryPanelBuilder.OK_BUTTON_LABEL);		

				d.pack();
				d.setLocationRelativeTo(owner);
				d.setVisible(true);
				d.addWindowListener(new WindowAdapter() {
					@Override
					public void windowClosed(WindowEvent e) {
						editDsType(dsType);
					}
				});
			}
    	});
    	
        buildPanel();
        editDsType(null);
    }
    
    private void buildPanel() {
        
        connectionStringTemplate.getDocument().addDocumentListener(new DocumentListener() {

            public void changedUpdate(DocumentEvent e) {
                updateTemplate();
            }

            public void insertUpdate(DocumentEvent e) {
                updateTemplate();
            }

            public void removeUpdate(DocumentEvent e) {
                updateTemplate();
            }

            /**
             * Updates the template if dsType is not currently null.
             */
            private void updateTemplate() {
                if (dsType != null) {
                    dsType.setJdbcUrl(connectionStringTemplate.getText());
                    template.setTemplate(dsType);
                }
            }
            
        });
        
        
        tabbedPane = new JTabbedPane();
        
        PanelBuilder pb = new PanelBuilder(new FormLayout(
                "4dlu,pref,4dlu,pref:grow,4dlu", //$NON-NLS-1$
                "4dlu,pref,4dlu,pref,4dlu,pref,4dlu,pref,4dlu,pref,4dlu, pref:grow, 4dlu")); //$NON-NLS-1$
        
        CellConstraints cc = new CellConstraints();
        CellConstraints cl = new CellConstraints();
        int row = 2;
        pb.addLabel(Messages.getString("DataSourceTypeEditorPanel.nameLabel"),cl.xy(2, row), name, cc.xy(4, row)); //$NON-NLS-1$
        row += 2;
        pb.addLabel(Messages.getString("DataSourceTypeEditorPanel.driverClassLabel"),cl.xy(2, row), driverClass, cc.xy(4, row)); //$NON-NLS-1$
        row += 2;
        pb.addLabel(Messages.getString("DataSourceTypeEditorPanel.connectionStringTemplateLabel"),cl.xy(2, row), connectionStringTemplate, cc.xy(4, row)); //$NON-NLS-1$
        row += 2;
        connectionStringTemplate.setToolTipText(Messages.getString("DataSourceTypeEditorPanel.templateToolTip")); //$NON-NLS-1$
        pb.addTitle(Messages.getString("DataSourceTypeEditorPanel.optionsEditorPreview"),cl.xyw(2, row,3)); //$NON-NLS-1$
        row += 2;
        pb.addLabel(Messages.getString("DataSourceTypeEditorPanel.sampleOptions"),cl.xy(2, row), template.getPanel(), cc.xy(4, row)); //$NON-NLS-1$
        row += 2;
        pb.add(jdbcPanel, cc.xyw(2, row, 3));
        
        tabbedPane.addTab(Messages.getString("DataSourceTypeEditorPanel.generalTab"), pb.getPanel()); //$NON-NLS-1$
        
        
        
        panel = new JPanel(new BorderLayout());
        panel.add(tabbedPane, BorderLayout.CENTER);
        
        ButtonBarBuilder copyBar = new ButtonBarBuilder();
        copyBar.addGlue();
        copyBar.addGridded(copyPropertiesButton);
        panel.add(copyBar.getPanel(), BorderLayout.SOUTH);
    }
    
    /**
     * Modifies the fields in the editor panel and all tabbed panels to match those of the given SPDataSourceType
     * @param dst The SPDataSourceType that is being edited
     */
    public void editDsType(JDBCDataSourceType dst) {
        dsType = dst;
        if (dst == null) {
            name.setText(""); //$NON-NLS-1$
            name.setEnabled(false);
            
            driverClass.setText(""); //$NON-NLS-1$
            driverClass.setEnabled(false);
            
            connectionStringTemplate.setText(""); //$NON-NLS-1$
            connectionStringTemplate.setEnabled(false);
            
            copyPropertiesButton.setEnabled(false);
            
            // template will get updated by document listener
        } else {
            name.setText(dst.getName());
            name.setEnabled(true);
            
            driverClass.setText(dst.getJdbcDriver());
            driverClass.setEnabled(true);
            
            connectionStringTemplate.setText(dst.getJdbcUrl());
            connectionStringTemplate.setEnabled(true);
            
            copyPropertiesButton.setEnabled(true);
            
            // template will get updated by document listener
        }
        // Also update all tab panels
        for (DataSourceTypeEditorTabPanel panel: tabPanels) {
            panel.editDsType(dst);
        }
        
        jdbcPanel.editDsType(dst);
    }

    public boolean applyChanges() {
        logger.debug("Applying changes to data source type "+dsType); //$NON-NLS-1$
        if (dsType != null) {
        	
            // Also apply changes for each contained DataEntryPanel
            for (DataSourceTypeEditorTabPanel panel : tabPanels) {
                panel.applyChanges();
            }
            
            dsType.setName(name.getText());
            dsType.setJdbcDriver(driverClass.getText());
            dsType.setJdbcUrl(connectionStringTemplate.getText());
        }
        jdbcPanel.applyChanges();
        return true;
    }

    public void discardChanges() {
    	logger.debug("Discarding changes to data source type " + dsType); //$NON-NLS-1$
        if (dsType != null) {
        	
            // Also discard changes for each contained DataEntryPanel
            for (DataSourceTypeEditorTabPanel panel : tabPanels) {
                panel.discardChanges();
            }
            
            name.setText(dsType.getName());
            driverClass.setText(dsType.getJdbcDriver());
            connectionStringTemplate.setText(dsType.getJdbcUrl());
        }
        jdbcPanel.discardChanges();
    }

    public JPanel getPanel() {
        return panel;
    }
    
    /**
     * Adds a DataSourceTypeEditorTabPanel for editing a SPDataSourceType's properties to the tabbed pane in the editor
     * @param title The title of the panel, displayed in the tab
     * @param tabPanel The panel to add to the tabbed pane
     */
    public void addTab(String title, DataSourceTypeEditorTabPanel tabPanel) {
        tabPanels.add(tabPanel);
        tabbedPane.addTab(title, tabPanel.getPanel());
    }

	public boolean hasUnsavedChanges() {
        // TODO return whether this panel has been changed
		return true;
	}
}
