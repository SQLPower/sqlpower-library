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

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.undo.UndoManager;

import org.apache.log4j.Logger;

import ca.sqlpower.sql.DataSourceCollection;
import ca.sqlpower.sql.JDBCDataSource;
import ca.sqlpower.sql.JDBCDataSourceType;
import ca.sqlpower.swingui.AddRemoveIcon;
import ca.sqlpower.swingui.DataEntryPanel;
import ca.sqlpower.swingui.DataEntryPanelBuilder;
import ca.sqlpower.swingui.Messages;
import ca.sqlpower.swingui.SPDataSourceTypeListCellRenderer;
import ca.sqlpower.swingui.SPSUtils;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

public class DataSourceTypeEditor implements DataEntryPanel {
    
    private static final Logger logger = Logger.getLogger(DataSourceTypeEditor.class);
    
    /**
     * The panel that this editor's GUI lives in.
     */
    private final JPanel panel;
    
    private final DataSourceCollection<JDBCDataSource> dataSourceCollection;
    
    /**
     * The list of data source types.
     */
    private final JList dsTypeList;
    
    /**
     * Button for adding a new data source type.
     */
    private final JButton addDsTypeButton;
    
    /**
     * Button for deleting the selected data source type.
     */
    private final JButton removeDsTypeButton;
    
    /**
     * The panel that edits the currently-selected data source type.
     */
    private final DataSourceTypeEditorPanel dsTypePanel;
    
    /**
     * An undo manager for DS types.
     */
    private final UndoManager undoManager = new UndoManager();

    /**
     * The model for the list of data source types in this editor.
     */
	private DefaultListModel dsTypeListModel;
    
	/**
	 * If true, then this editor will not try to save settings locally. It assumes that they will be sent to a server.
	 */
	private final boolean enterprise;
	
    /**
     * Creates a multi-tabbed panel with facilities for configuring all the
     * database types defined in a particular data source collection.
     * 
     * @param collection
     *            The data source collection to edit.
     * @param owner The Window that should own any dialogs created within the editor GUI.
     * @see DefaultDataSourceTypeDialogFactory for a more out-of-the-box setup
     */
    public DataSourceTypeEditor(DataSourceCollection<JDBCDataSource> dataSourceCollection, final Window owner, boolean enterprise) {
        this.dataSourceCollection = dataSourceCollection;
        this.enterprise = enterprise;
        
        dsTypeListModel = new DefaultListModel();
        dataSourceCollection.addUndoableEditListener(undoManager);
        for (JDBCDataSourceType type : dataSourceCollection.getDataSourceTypes()) {
            dsTypeListModel.addElement(type);
            type.addUndoableEditListener(undoManager);
        }
        dsTypeList = new JList(dsTypeListModel);
        dsTypeList.setCellRenderer(new SPDataSourceTypeListCellRenderer());
        
        addDsTypeButton = new JButton(new AddRemoveIcon(AddRemoveIcon.Type.ADD));
        addDsTypeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            	final NewDataSourceTypePanel copyPropertiesPanel = new NewDataSourceTypePanel(DataSourceTypeEditor.this, DataSourceTypeEditor.this.dataSourceCollection);
				final JDialog d = DataEntryPanelBuilder.createDataEntryPanelDialog(
						copyPropertiesPanel,
						owner,
						"Copy Properties",
						DataEntryPanelBuilder.OK_BUTTON_LABEL);		

				d.pack();
				d.setLocationRelativeTo(owner);
				d.setVisible(true);
            }
        });
        
        removeDsTypeButton = new JButton(new AddRemoveIcon(AddRemoveIcon.Type.REMOVE));
        removeDsTypeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                removeSelectedDsType();
            }
        });
        removeDsTypeButton.setEnabled(false);
        
        dsTypePanel = new DataSourceTypeEditorPanel(dataSourceCollection, owner);
        
        dsTypeList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    JDBCDataSourceType dst =
                        (JDBCDataSourceType) dsTypeList.getSelectedValue();
                    switchToDsType(dst);
                }
                
                // remove button enabled when a datasource has been selected 
                removeDsTypeButton.setEnabled(dsTypeList.getSelectedIndex() != -1);
            }
        });
        panel = createPanel();
    }
    
    /**
     * Creates a multi-tabbed panel with facilities for configuring all the
     * database types defined in a particular data source collection.
     * 
     * @param collection
     *            The data source collection to edit.
     * @param owner The Window that should own any dialogs created within the editor GUI.
     * @see DefaultDataSourceTypeDialogFactory for a more out-of-the-box setup
     */
    public DataSourceTypeEditor(DataSourceCollection<JDBCDataSource> dataSourceCollection, final Window owner) {
        this(dataSourceCollection, owner, false);
    }
    
    /**
     * Removes the selected data source type from the list.  If there is
     * no selected type, does nothing.
     */
    private void removeSelectedDsType() {
        JDBCDataSourceType type = (JDBCDataSourceType) dsTypeList.getSelectedValue();
        if (type != null) {
            ((DefaultListModel) dsTypeList.getModel()).removeElement(type);
            dataSourceCollection.removeDataSourceType(type);
        }
    }

    public void addDsType(JDBCDataSourceType type) {
        if (type == null) {
            throw new NullPointerException("Don't add null data source types, silly!"); //$NON-NLS-1$
        }
        dataSourceCollection.addDataSourceType(type);
        ((DefaultListModel) dsTypeList.getModel()).addElement(type);
        dsTypeList.setSelectedValue(type, true);
    }

    /**
     * Creates the panel layout. Requires that the GUI components have already
     * been created. Does not fill in any values into the components. See
     * {@link #switchToDsType()} for that.
     */
    private JPanel createPanel() {
        FormLayout layout = new FormLayout("fill:max(60dlu;pref), 6dlu, pref:grow", "pref, 6dlu, pref:grow, 3dlu, pref"); //$NON-NLS-1$ //$NON-NLS-2$
        DefaultFormBuilder fb = new DefaultFormBuilder(layout);
        fb.setDefaultDialogBorder();
        
        JComponent addRemoveBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        addRemoveBar.add(addDsTypeButton);
        addRemoveBar.add(removeDsTypeButton);
        
        JScrollPane dsTypePane = new JScrollPane(dsTypeList);
        //Setting the preferred size to 0 so the add/remove bar and the default size
        //set the width of the column and not the max type name width.
        dsTypePane.setPreferredSize(new Dimension(0, 0));
		fb.add(dsTypePane, "1, 1, 1, 3"); //$NON-NLS-1$
        fb.add(addRemoveBar,                "1, 5"); //$NON-NLS-1$
        fb.add(dsTypePanel.getPanel(),      "3, 1"); //$NON-NLS-1$
        
        return fb.getPanel();
    }
    
    /**
     * Returns this editor's GUI.
     */
    public JPanel getPanel() {
        return panel;
    }

    /**
     * Copies all the data source types and their properties back to the
     * DataSourceCollection we're editing.
     */
    public boolean applyChanges() {
        logger.debug("Applying changes to all data source types"); //$NON-NLS-1$
        applyCurrentChanges();
        ListModel lm = dsTypeList.getModel();
        for (int i = 0; i < lm.getSize(); i++) {
            JDBCDataSourceType dst = (JDBCDataSourceType) lm.getElementAt(i);
            dataSourceCollection.mergeDataSourceType(dst);
        }
        
        if (!enterprise) {
        	try {
        		dataSourceCollection.write();
        	} catch (IOException ex) {
        		SPSUtils.showExceptionDialogNoReport(panel, Messages.getString("DataSourceTypeEditor.errorSavingToPlDotIni"), ex); //$NON-NLS-1$
        	}
        }
        undoManager.discardAllEdits();
        return true;
    }

    /**
     * This method is a no-op implementation, since all we have to do to discard the
     * changes is not copy them back to the model.
     */
    public void discardChanges() {
    	logger.debug("Discarding changes to all data source types.");
    	int undoCount = 0;
    	while (undoManager.canUndo()) {
    		undoCount++;
    		undoManager.undo();
    	}
    	logger.debug("There were " + undoCount + " changes.");
    	
    	dsTypePanel.discardChanges();
    	
    	dsTypeListModel.clear();
    	for (JDBCDataSourceType type : dataSourceCollection.getDataSourceTypes()) {
    		dsTypeListModel.addElement(type);
    	}
    }
    
    /**
     * Call this to disconnect the editor from the DS types.
     */
    public void cleanup() {
    	for (JDBCDataSourceType type : dataSourceCollection.getDataSourceTypes()) {
    		type.removeUndoableEditListener(undoManager);
    	}
    	dataSourceCollection.removeUndoableEditListener(undoManager);
    }
    
    /**
     * Causes this editor to set up all its GUI components to edit the given data source type.
     * Null is an acceptable value, and means to make no DS Type the current type.
     */
    public void switchToDsType(JDBCDataSourceType dst) {
        applyCurrentChanges();
        dsTypeList.setSelectedValue(dst, true);
        dsTypePanel.editDsType(dst);
    }
    
    private void applyCurrentChanges() {
        dsTypePanel.applyChanges();
    }
    
    public void addTab(String title, DataSourceTypeEditorTabPanel dataEntryPanel) {
        dsTypePanel.addTab(title, dataEntryPanel);
    }

	public boolean hasUnsavedChanges() {
		return undoManager.canUndo();
	}
}
