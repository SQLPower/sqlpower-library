/*
 * Copyright (c) 2009, SQL Power Group Inc.
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
import ca.sqlpower.sql.SPDataSourceType;
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
	private final SPDataSourceType dsType;
	
	/**
	 * The combo box used for selecting the ds type to copy from.
	 */
	private final JComboBox dsTypesComboBox;
	
	public DataSourceTypeCopyPropertiesPanel(SPDataSourceType type, DataSourceCollection collection) {
		this.dsType = type;
		
		List<SPDataSourceType> types = new ArrayList<SPDataSourceType>(collection.getDataSourceTypes());
		types.remove(dsType);
		dsTypesComboBox = new JComboBox(types.toArray());
		dsTypesComboBox.setRenderer(new DefaultListCellRenderer() {
			public Component getListCellRendererComponent(JList list, Object value,
					int index, boolean isSelected, boolean cellHasFocus) {
				Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				if (c instanceof JLabel && value != null) {
					((JLabel) c).setText(((SPDataSourceType) value).getName());
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
		SPDataSourceType defaultDSType = (SPDataSourceType) dsTypesComboBox.getSelectedItem();
        if (defaultDSType != null && defaultDSType != dsType) {
        	for (String key : defaultDSType.getPropertyNames()) {
        		if (SPDataSourceType.TYPE_NAME.equals(key)) {
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
