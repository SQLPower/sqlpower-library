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
import ca.sqlpower.sql.SPDataSourceType;
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
	private final SPDataSourceType dsType;
	
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
		this.dsType = new SPDataSourceType();
        dsType.setName(Messages.getString("DataSourceTypeEditor.defaultDataSourceName")); //$NON-NLS-1$
		
		existingDSTypes = new JComboBox(collection.getDataSourceTypes().toArray());
		existingDSTypes.setRenderer(new DefaultListCellRenderer() {
			@Override
			public Component getListCellRendererComponent(JList list, Object value,
					int index, boolean isSelected, boolean cellHasFocus) {
				Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				if (c instanceof JLabel && value != null) {
					((JLabel) c).setText(((SPDataSourceType) value).getName());
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
			SPDataSourceType defaultDSType = (SPDataSourceType) existingDSTypes.getSelectedItem();
			if (defaultDSType != null && defaultDSType != dsType) {
				for (String key : defaultDSType.getPropertyNames()) {
					if (SPDataSourceType.TYPE_NAME.equals(key)) {
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
	void setCopyDSType(SPDataSourceType copyType) {
		existingDSTypes.setSelectedItem(copyType);
	}

}
