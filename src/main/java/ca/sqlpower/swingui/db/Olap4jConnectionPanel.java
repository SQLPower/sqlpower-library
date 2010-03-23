/*
 * Copyright (c) 2009, SQL Power Group Inc.
 *
 * This file is part of Wabit.
 *
 * Wabit is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * Wabit is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>. 
 */

package ca.sqlpower.swingui.db;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.InvalidParameterException;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import net.miginfocom.swing.MigLayout;
import ca.sqlpower.sql.DataSourceCollection;
import ca.sqlpower.sql.JDBCDataSource;
import ca.sqlpower.sql.Olap4jDataSource;
import ca.sqlpower.sql.Olap4jDataSource.Type;
import ca.sqlpower.swingui.ConnectionComboBoxModel;
import ca.sqlpower.swingui.DataEntryPanel;
import ca.sqlpower.swingui.Messages;
import ca.sqlpower.swingui.SPSUtils;

/**
 * Provides a GUI for modifying the properties of an Olap4jDataSource.
 */
public class Olap4jConnectionPanel implements DataEntryPanel {

    private final Olap4jDataSource olapDataSource;
    
    private JPanel panel;
    
    private final JTextField nameField = new JTextField();
    
    private JTextField schemaFileField;
    private JComboBox dataSourceBox;

    private JRadioButton xmlaType;

    private JRadioButton inProcessType;

    private JTextField xmlaUriField;

    public Olap4jConnectionPanel(Olap4jDataSource olapDataSource, DataSourceCollection<JDBCDataSource> dsCollection) {
        this.olapDataSource = olapDataSource;
        panel = new JPanel(new MigLayout("", "[][][grow][]", ""));

        nameField.setText(olapDataSource.getName());
        panel.add(new JLabel(Messages.getString("SPDataSourcePanel.connectionNameLabel")));
        panel.add(nameField, "grow, span 3, wrap, gapbottom unrel");
        
        ButtonGroup connectionTypeGroup = new ButtonGroup();
        inProcessType = new JRadioButton("In-process Mondrian Server");
        connectionTypeGroup.add(inProcessType);
        panel.add(inProcessType, "span 2,wrap");
        
        panel.add(new JLabel("Database Connection"), "span 2, gapbefore 25px");
        dataSourceBox = new JComboBox(new ConnectionComboBoxModel(dsCollection));
        if (olapDataSource.getDataSource() != null) {
            dataSourceBox.setSelectedItem(olapDataSource.getDataSource());
        }
        panel.add(dataSourceBox, "grow,wrap, wmax 500");
        
        panel.add(new JLabel("Mondrian Schema"), "span 2, gapbefore 25px");
        schemaFileField = new JTextField();
        URI initialSchemaURI = olapDataSource.getMondrianSchema();
        if (initialSchemaURI != null && initialSchemaURI.getScheme() != null && initialSchemaURI.getScheme().equals("file")) {
            schemaFileField.setText(initialSchemaURI.getSchemeSpecificPart());
        }
        panel.add(schemaFileField, "growx, wmax 500");
        JButton fileChooserButton = new JButton("...");
        panel.add(fileChooserButton, "wrap paragraph");
        fileChooserButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JFileChooser fc = new JFileChooser(schemaFileField.getText());
                fc.setFileFilter(SPSUtils.XML_FILE_FILTER);
                int choice = fc.showOpenDialog(panel);
                if (choice == JFileChooser.APPROVE_OPTION) {
                    schemaFileField.setText(fc.getSelectedFile().getAbsolutePath());
                }
            }
        });
        
        xmlaType = new JRadioButton("Remote XML/A Server");
        connectionTypeGroup.add(xmlaType);
        panel.add(xmlaType, "span 2,wrap");
        panel.add(new JLabel("XML/A Server URL"), "span 2, gapbefore 25px");
        URI xmlaServerURI = olapDataSource.getXmlaServer();
        xmlaUriField = new JTextField(xmlaServerURI == null ? "" : xmlaServerURI.toString());
        panel.add(xmlaUriField, "growx, wmax 500");
        
        Type type = olapDataSource.getType();
        if (type == null || type == Type.IN_PROCESS) {
            // default type
            inProcessType.setSelected(true);
        } else if (type == Type.XMLA) {
            xmlaType.setSelected(true);
        } else {
            throw new IllegalStateException("Unknown olap4j connection type: " + type);
        }
    }
    
    public boolean applyChanges() {
        if (nameField.getText().trim().length() == 0) {
            JOptionPane.showMessageDialog(panel, Messages.getString("SPDataSourcePanel.blankNameNotAllowed"));
            return false;
        }
        
        olapDataSource.setName(nameField.getText().trim());
        
        if (inProcessType.isSelected()) {
            olapDataSource.setType(Type.IN_PROCESS);
            olapDataSource.setDataSource((JDBCDataSource) dataSourceBox.getSelectedItem());
            olapDataSource.setMondrianSchema(new File(schemaFileField.getText()).toURI());
        } else if (xmlaType.isSelected()) {
            olapDataSource.setType(Type.XMLA);
            try {
                // We validate through both URI and URL.
                olapDataSource.setXmlaServer(new URI(xmlaUriField.getText()).toURL().toExternalForm());
            } catch (MalformedURLException e) {
                JOptionPane.showMessageDialog(panel, "XML/A Server URI is not valid.");
                return false;
            } catch (URISyntaxException e) {
                JOptionPane.showMessageDialog(panel, "XML/A Server URI is not valid.");
                return false;
            } catch (IllegalArgumentException e) {
                JOptionPane.showMessageDialog(panel, "XML/A Server URI is not valid.");
                return false;
            }
        } else {
            throw new IllegalStateException(
                    "Someone added a new connection type but forgot to" +
                    " put in the code for storing it");
        }
        
        return true;
    }

    public void discardChanges() {
        // no op
    }

    public JComponent getPanel() {
        return panel;
    }

    public boolean hasUnsavedChanges() {
        return true;
    }
}
