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

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.LayoutManager;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.apache.log4j.Logger;

import ca.sqlpower.sql.JDBCDataSourceType;

/**
 * The PlatformSpecificConnectionOptionPanel creates and modifies a
 * panel that contains a Jlabel and JTextField for each parameter 
 * in the template defined for the object.  
 */
public class PlatformSpecificConnectionOptionPanel {

    private class JDBCURLUpdater implements DocumentListener {

        public void insertUpdate(DocumentEvent e) {
            updateUrlFromFields();
        }

        public void removeUpdate(DocumentEvent e) {
            updateUrlFromFields();
        }

        public void changedUpdate(DocumentEvent e) {
            updateUrlFromFields();
        }
    }
    
    private static class PlatformOptionsLayout implements LayoutManager {

        /** The number of pixels to leave before each label except the first one. */
        int preLabelGap = 10;

        /** The number of pixels to leave between every component. */
        int gap = 5;

        public void addLayoutComponent(String name, Component comp) {
            // nothing to do
        }

        public void removeLayoutComponent(Component comp) {
            // nothing to do
        }

        public Dimension preferredLayoutSize(Container parent) {
            int height = 0;
            int width = 0;
            for (int i = 0; i < parent.getComponentCount(); i++) {
                Component c = parent.getComponent(i);
                height = Math.max(height, c.getPreferredSize().height);
                width += c.getPreferredSize().getWidth();
            }
            return new Dimension(width, height);
        }

        public Dimension minimumLayoutSize(Container parent) {
            int height = 0;
            for (int i = 0; i < parent.getComponentCount(); i++) {
                Component c = parent.getComponent(i);
                height = Math.max(height, c.getMinimumSize().height);
            }
            return new Dimension(0, height);
        }

        public void layoutContainer(Container parent) {

            // compute total width of all labels
            int labelSize = 0;
            int labelCount = 0;
            for (int i = 0; i < parent.getComponentCount(); i++) {
                Component c = parent.getComponent(i);
                if (c instanceof JLabel) {
                    if (i > 0) labelSize += preLabelGap;
                    labelSize += c.getPreferredSize().width;
                    labelCount += 1;
                }
            }

            int gapSize = gap * (parent.getComponentCount() - 1);

            // compute how wide each non-label component should be (if there are any non-labels)
            int nonLabelWidth = 0;
            if (parent.getComponentCount() != labelCount) {
                nonLabelWidth = (parent.getWidth() - labelSize - gapSize) / (parent.getComponentCount() - labelCount);
            }

            // impose a minimum so the non-labels at least show up when we're tight on space
            if (nonLabelWidth < 20) {
                nonLabelWidth = 20;
            }

            // lay out the container
            int x = 0;
            for (int i = 0; i < parent.getComponentCount(); i++) {
                Component c = parent.getComponent(i);

                if (i > 0) x += gap;

                if (c instanceof JLabel) {
                    if (i > 0) x += preLabelGap;
                    c.setBounds(x, 0, c.getPreferredSize().width, parent.getHeight());
                    x += c.getPreferredSize().width;
                } else {
                    c.setBounds(x, 0, nonLabelWidth, parent.getHeight());
                    x += nonLabelWidth;
                }
            }
        }
    }
    
    private static Logger logger = Logger.getLogger(PlatformSpecificConnectionOptionPanel.class);
    
    private JDBCURLUpdater urlUpdater = new JDBCURLUpdater();

    private boolean updatingUrlFromFields = false;
    private boolean updatingFieldsFromUrl = false;
    private JTextField dbUrlField;
    private JPanel platformSpecificOptionPanel;
    private JDBCDataSourceType template;
    
    public PlatformSpecificConnectionOptionPanel(JTextField dbUrlField) {
        platformSpecificOptionPanel = new JPanel();
        platformSpecificOptionPanel.setLayout(new PlatformOptionsLayout());
        platformSpecificOptionPanel.setBorder(BorderFactory.createEmptyBorder());
        platformSpecificOptionPanel.add(new JLabel(Messages.getString("PlatformSpecificConnectionOptionPanel.noOptionsForDriver"))); //$NON-NLS-1$

        this.dbUrlField = dbUrlField;
  

        dbUrlField.getDocument().addDocumentListener(new DocumentListener() {

            public void insertUpdate(DocumentEvent e) {
                updateFieldsFromUrl();
            }

            public void removeUpdate(DocumentEvent e) {
                updateFieldsFromUrl();
            }

            public void changedUpdate(DocumentEvent e) {
                updateFieldsFromUrl();
            }
        });
    }
    
    /**
     * Copies the values from the platform-specific url fields into the main
     * url.
     */
    private void updateUrlFromFields() {
        if (updatingFieldsFromUrl) return;

        if (template == null || template.getJdbcUrl() == null) return;
        try {
            updatingUrlFromFields = true;
            StringBuffer newUrl = new StringBuffer();
            Pattern p = Pattern.compile("<(.*?)>"); //$NON-NLS-1$
            Matcher m = p.matcher(template.getJdbcUrl());
            while (m.find()) {
                String varName = m.group(1);
                if (varName.indexOf(':') != -1) {
                    varName = varName.substring(0, varName.indexOf(':'));
                }
                String varValue = getPlatformSpecificFieldValue(varName);
                varValue = escapeDollarBackslash(varValue);
                m.appendReplacement(newUrl, varValue);
            }
            m.appendTail(newUrl);
            dbUrlField.setText(newUrl.toString());
        } finally {
            updatingUrlFromFields = false;
        }
    }
    
    /**
     * Escapes all instances of backslash and dollar-sign in the given input
     * string by preceding them with a backslash character.  This is necessary
     * before passing user input into {@link Matcher#appendReplacement(StringBuffer, String)}.
     * 
     * @param varValue The string to escape metacharacters in.  Null is allowable.
     * @return The escaped string, or null if the input string is null.
     */
    private String escapeDollarBackslash(String varValue) {
    	if (varValue == null) return null;
    	StringBuilder sb = new StringBuilder();
    	for (int i = 0; i < varValue.length(); i++) {
    		char ch = varValue.charAt(i);
    		if (ch == '\\' || ch == '$') {
    			sb.append('\\');
    		}
    		sb.append(ch);
    	}
    	return sb.toString();
	}

	/**
     * Retrieves the named platform-specific option by looking it up in the
     * platformSpecificOptionPanel component.
     */
    private String getPlatformSpecificFieldValue(String varName) {
        // we're looking for the contents of the JTextField that comes after a JLabel with the same text as varName
        for (int i = 0; i < platformSpecificOptionPanel.getComponentCount(); i++) {
            if (platformSpecificOptionPanel.getComponent(i) instanceof JLabel
                    && ((JLabel) platformSpecificOptionPanel.getComponent(i)).getText().equals(varName)
                    && platformSpecificOptionPanel.getComponentCount() >= i+1) {
                return ((JTextField) platformSpecificOptionPanel.getComponent(i+1)).getText();
            }
        }
        return ""; //$NON-NLS-1$
    }
    
    
    
    /**
     * Parses the main url against the current template (if possible) and fills in the
     * individual fields with the values it finds.
     */
    private void updateFieldsFromUrl() {
        if (updatingUrlFromFields) return;
        if (template == null || template.getJdbcUrl() == null) return;
        try {
            updatingFieldsFromUrl = true;

            for (int i = 0; i < platformSpecificOptionPanel.getComponentCount(); i++) {
                platformSpecificOptionPanel.getComponent(i).setEnabled(true);
            }

            logger.debug("template is " + template); //$NON-NLS-1$
            logger.debug("dbUrlField is " + dbUrlField); //$NON-NLS-1$
            Map<String, String> map = template.retrieveURLParsing(dbUrlField.getText());
            if (!map.isEmpty()) {
                platformSpecificOptionPanel.setEnabled(true);
                for (int g = 0; g < map.size(); g++) {
                    ((JTextField) platformSpecificOptionPanel.getComponent(2*g+1)).setText((String)map.values().toArray()[g]);
                }
            } else {
                for (int i = 0; i < platformSpecificOptionPanel.getComponentCount(); i++) {
                    platformSpecificOptionPanel.getComponent(i).setEnabled(false);
                }
            }
        } finally {
            updatingFieldsFromUrl = false;
        }
    }
    

    
 
    
    /**
     * Sets up the platformSpecificOptionPanel component to contain labels and
     * text fields associated with each variable in the current template.
     */
    private void createFieldsFromTemplate() {
        for (int i = 0; i < platformSpecificOptionPanel.getComponentCount(); i++) {
            Component c = platformSpecificOptionPanel.getComponent(i);
            if (c instanceof JTextField) {
                ((JTextField) c).getDocument().removeDocumentListener(urlUpdater);
            }
        }
        platformSpecificOptionPanel.removeAll();

        if (template != null) {
        	
            Map<String, String> map = template.retrieveURLParsing(dbUrlField.getText());
            if (map.size() == 0) {
            	map = template.retrieveURLDefaults();
            }

            for(String key : map.keySet()) {
                String var = key;
                String def = map.get(key);

                platformSpecificOptionPanel.add(new JLabel(var));
                JTextField field = new JTextField(def);
                platformSpecificOptionPanel.add(field);
                field.getDocument().addDocumentListener(urlUpdater);
                logger.debug("The default value for key " + key + " is: " + def); //$NON-NLS-1$ //$NON-NLS-2$
            }


        } else {
            platformSpecificOptionPanel.add(new JLabel(Messages.getString("PlatformSpecificConnectionOptionPanel.unknownDriverClass"))); //$NON-NLS-1$

        }

        platformSpecificOptionPanel.revalidate();
        platformSpecificOptionPanel.repaint();
    }

    public JPanel getPanel() {
        return platformSpecificOptionPanel;
    }

    public JDBCDataSourceType getTemplate() {
        return template;
    }

    public void setTemplate(JDBCDataSourceType template) {
        this.template = template;
        createFieldsFromTemplate();
        updateUrlFromFields();
    }
    
}
