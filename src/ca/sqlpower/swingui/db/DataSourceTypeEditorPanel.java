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
package ca.sqlpower.swingui.db;

import java.awt.BorderLayout;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;

import org.apache.log4j.Logger;

import ca.sqlpower.sql.DataSourceCollection;
import ca.sqlpower.sql.SPDataSourceType;
import ca.sqlpower.swingui.DataEntryPanel;
import ca.sqlpower.swingui.Messages;
import ca.sqlpower.swingui.PlatformSpecificConnectionOptionPanel;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class DataSourceTypeEditorPanel implements DataEntryPanel {

    private static final Logger logger = Logger.getLogger(DataSourceTypeEditorPanel.class);
    
    private SPDataSourceType dsType;
    private JPanel panel;
    private JTabbedPane tabbedPane;
    final private JTextField name = new JTextField();
    final private JTextField connectionStringTemplate = new JTextField();
    final private JTextField driverClass = new JTextField();
    final private PlatformSpecificConnectionOptionPanel template =
        new PlatformSpecificConnectionOptionPanel(new JTextField());
    
    final private JComboBox dsTypeDefaultCombo;
    
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
    
    public DataSourceTypeEditorPanel(DataSourceCollection collection) {
    	jdbcPanel = new JDBCDriverPanel();
    	
    	jdbcPanel.addDriverTreeSelectionListener(new TreeSelectionListener() {
			public void valueChanged(TreeSelectionEvent e) {
				if (e.getNewLeadSelectionPath() != null && e.getNewLeadSelectionPath().getPathCount() > JDBCDriverPanel.DRIVER_LEVEL) {
					driverClass.setText(e.getNewLeadSelectionPath().getLastPathComponent().toString());
				}
			}
		});
    	
    	dsTypeDefaultCombo = new JComboBox(collection.getDataSourceTypes().toArray());
    	dsTypeDefaultCombo.setRenderer(new DefaultListCellRenderer() {
		
			public Component getListCellRendererComponent(JList list, Object value,
					int index, boolean isSelected, boolean cellHasFocus) {
				Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				if (c instanceof JLabel && value != null) {
					((JLabel) c).setText(((SPDataSourceType) value).getName());
				}
				return c;
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
                    dsTypeDefaultCombo.setSelectedItem(dsType);
                } else {
                	dsTypeDefaultCombo.setSelectedIndex(-1);
                }
            }
            
        });
        
        
        tabbedPane = new JTabbedPane();
        
        PanelBuilder pb = new PanelBuilder(new FormLayout(
                "4dlu,pref,4dlu,pref:grow,4dlu", //$NON-NLS-1$
                "4dlu,pref,4dlu,pref,4dlu,pref,4dlu,pref,4dlu,pref,4dlu,pref,4dlu, pref:grow, 4dlu")); //$NON-NLS-1$
        
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
        pb.addLabel(Messages.getString("DataSourceTypeEditorPanel.defaultDSTypeProperties"), cl.xy(2, row), dsTypeDefaultCombo, cc.xy(4, row));
        row += 2;
        pb.add(jdbcPanel, cc.xyw(2, row, 3));
        
        tabbedPane.addTab(Messages.getString("DataSourceTypeEditorPanel.generalTab"), pb.getPanel()); //$NON-NLS-1$
        
        
        
        panel = new JPanel(new BorderLayout());
        panel.add(tabbedPane, BorderLayout.CENTER);
    }
    
    /**
     * Modifies the fields in the editor panel and all tabbed panels to match those of the given SPDataSourceType
     * @param dst The SPDataSourceType that is being edited
     */
    public void editDsType(SPDataSourceType dst) {
        dsType = dst;
        if (dst == null) {
            name.setText(""); //$NON-NLS-1$
            name.setEnabled(false);
            
            driverClass.setText(""); //$NON-NLS-1$
            driverClass.setEnabled(false);
            
            connectionStringTemplate.setText(""); //$NON-NLS-1$
            connectionStringTemplate.setEnabled(false);
            
            dsTypeDefaultCombo.setSelectedIndex(-1);

            // template will get updated by document listener
        } else {
            name.setText(dst.getName());
            name.setEnabled(true);
            
            driverClass.setText(dst.getJdbcDriver());
            driverClass.setEnabled(true);
            
            connectionStringTemplate.setText(dst.getJdbcUrl());
            connectionStringTemplate.setEnabled(true);
            
            dsTypeDefaultCombo.setSelectedItem(dst);
            if (dsTypeDefaultCombo.getSelectedItem() != dst) {
            	dsTypeDefaultCombo.setSelectedIndex(-1);
            }
            
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
            
            SPDataSourceType defaultDSType = (SPDataSourceType) dsTypeDefaultCombo.getSelectedItem();
            if (defaultDSType != null && defaultDSType != dsType) {
            	for (String key : defaultDSType.getPropertyNames()) {
            		dsType.putProperty(key, defaultDSType.getProperty(key));
            	}
            }
            
            dsType.setName(name.getText());
            dsType.setJdbcDriver(driverClass.getText());
            dsType.setJdbcUrl(connectionStringTemplate.getText());
        }
        jdbcPanel.applyChanges();
        return true;
    }

    public void discardChanges() {
        // no action needed
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
