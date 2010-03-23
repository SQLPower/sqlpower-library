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

import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;

import ca.sqlpower.sql.DataSourceCollection;
import ca.sqlpower.sql.JDBCDataSourceType;
import ca.sqlpower.swingui.DataEntryPanel;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

/**
 * This panel is used to copy all of the properties from a user selected data source type to a
 * given data source type. This allows users to quickly duplicate existing ds types and allows
 * creating new connections faster that are similar to existing ones.
 */
public class DataSourceTypeCopyPropertiesPanel implements DataEntryPanel {
	
	private final JPanel panel;
	
	/**
	 * The target ds type to copy the properties to.
	 */
	private final JDBCDataSourceType dsType;
	
	/**
	 * The combo box used for selecting the ds type to copy from.
	 */
	private final JComboBox dsTypesComboBox;
	
	public DataSourceTypeCopyPropertiesPanel(JDBCDataSourceType type, DataSourceCollection collection) {
		this.dsType = type;
		
		List<JDBCDataSourceType> types = new ArrayList<JDBCDataSourceType>(collection.getDataSourceTypes());
		types.remove(dsType);
		dsTypesComboBox = new JComboBox(types.toArray());
		dsTypesComboBox.setRenderer(new DefaultListCellRenderer() {
			public Component getListCellRendererComponent(JList list, Object value,
					int index, boolean isSelected, boolean cellHasFocus) {
				Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				if (c instanceof JLabel && value != null) {
					((JLabel) c).setText(((JDBCDataSourceType) value).getName());
				}
				return c;
			}
		});
		
		panel = buildUI();
	}
	
	private JPanel buildUI() {
		DefaultFormBuilder builder = new DefaultFormBuilder(new FormLayout("pref"));
		builder.setDefaultDialogBorder();
		builder.append(new JLabel("Copy all properties from which data source type?"));
		builder.append(dsTypesComboBox);
		builder.append(new JLabel("Note: This will overwrite all current settings on " + dsType.getName() + "."));
		return builder.getPanel();
	}
	
	public boolean applyChanges() {
		JDBCDataSourceType defaultDSType = (JDBCDataSourceType) dsTypesComboBox.getSelectedItem();
        if (defaultDSType != null && defaultDSType != dsType) {
        	for (String key : defaultDSType.getPropertyNames()) {
        		if (JDBCDataSourceType.TYPE_NAME.equals(key)) {
        			continue;
        		}
        		dsType.putProperty(key, defaultDSType.getProperty(key));
        	}
        }	
		return true;
	}

	public void discardChanges() {
		//do nothing
	}

	public JComponent getPanel() {
		return panel;
	}

	public boolean hasUnsavedChanges() {
		return true;
	}
	
	/**
	 * This is package private and only exists for testing.
	 */
	JComboBox getDsTypesComboBox() {
		return dsTypesComboBox;
	}

}
