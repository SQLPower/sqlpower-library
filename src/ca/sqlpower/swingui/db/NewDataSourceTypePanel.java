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
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

import ca.sqlpower.sql.DataSourceCollection;
import ca.sqlpower.sql.JDBCDataSourceType;
import ca.sqlpower.swingui.DataEntryPanel;
import ca.sqlpower.swingui.Messages;

/**
 * This panel will appear when a user is creating a new data source type
 * to allow the user to choose which data source type to copy properties
 * from if they want to start with non-empty fields.
 */
public class NewDataSourceTypePanel implements DataEntryPanel {

	/**
	 * The new type that the properties will be copied into if the user
	 * selected a data source type to copy from.
	 */
	private final JDBCDataSourceType dsType;
	
	/**
	 * A combo box for users to choose which DS type to copy from.
	 */
	private final JComboBox existingDSTypes;

	/**
	 * The editor the new data source type created will be added to
	 * to allow further customization by the user.
	 */
	private final DataSourceTypeEditor editor;
	
	private final JPanel panel;

	/**
	 * The radio button to indicate that the user wants to create a
	 * new data source type that has no properties set.
	 */
	private JRadioButton blankOption;

	/**
	 * The radio button that indicates the user wants to create a copy
	 * of an existing data source type.
	 */
	private JRadioButton copyOption;
	
	public NewDataSourceTypePanel(DataSourceTypeEditor editor, DataSourceCollection collection) {
		this.editor = editor;
		this.dsType = new JDBCDataSourceType();
        dsType.setName(Messages.getString("DataSourceTypeEditor.defaultDataSourceName")); //$NON-NLS-1$
		
		existingDSTypes = new JComboBox(collection.getDataSourceTypes().toArray());
		existingDSTypes.setRenderer(new DefaultListCellRenderer() {
			@Override
			public Component getListCellRendererComponent(JList list, Object value,
					int index, boolean isSelected, boolean cellHasFocus) {
				Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				if (c instanceof JLabel && value != null) {
					((JLabel) c).setText(((JDBCDataSourceType) value).getName());
				}
				return c;
			}
		});
		
		blankOption = new JRadioButton(new AbstractAction("Blank") {
			public void actionPerformed(ActionEvent e) {
				existingDSTypes.setEnabled(false);
			}
		});
		copyOption = new JRadioButton(new AbstractAction("Copy defaults from..") {
			public void actionPerformed(ActionEvent e) {
				existingDSTypes.setEnabled(true);
			}
		});
		ButtonGroup optionGroup = new ButtonGroup();
		optionGroup.add(blankOption);
		optionGroup.add(copyOption);
		blankOption.setSelected(true);
		existingDSTypes.setEnabled(false);
		
		panel = buildUI();
	}
	
	private JPanel buildUI() {
		DefaultFormBuilder builder = new DefaultFormBuilder(new FormLayout("pref"));
		builder.setDefaultDialogBorder();
		
		builder.append("New Data Source Type");
		builder.append(blankOption);
		builder.append(copyOption);
		builder.append(existingDSTypes);
		
		return builder.getPanel();
	}
	
	public boolean applyChanges() {
		if (copyOption.isSelected()) {
			JDBCDataSourceType defaultDSType = (JDBCDataSourceType) existingDSTypes.getSelectedItem();
			if (defaultDSType != null && defaultDSType != dsType) {
				for (String key : defaultDSType.getPropertyNames()) {
					if (JDBCDataSourceType.TYPE_NAME.equals(key)) {
						continue;
					}
					dsType.putProperty(key, defaultDSType.getProperty(key));
				}
			}
		}

		editor.addDsType(dsType);
		return true;
	}

	public void discardChanges() {
		//Do nothing
	}

	public JComponent getPanel() {
		return panel;
	}

	public boolean hasUnsavedChanges() {
		return true;
	}
	
	/**
	 * Package private method for testing to change the radio buttons.
	 */
	void setBlankOptionSelected(boolean selected) {
		blankOption.setSelected(selected);
		copyOption.setSelected(!selected);
	}
	
	/**
	 * Package private method for testing to define which dsType will
	 * be selected in the existingDSTypes combo box.
	 * @param copyType
	 */
	void setCopyDSType(JDBCDataSourceType copyType) {
		existingDSTypes.setSelectedItem(copyType);
	}

}
