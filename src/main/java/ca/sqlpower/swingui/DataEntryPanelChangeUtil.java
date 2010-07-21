/*
 * Copyright (c) 2010, SQL Power Group Inc.
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

import java.awt.Color;
import java.beans.PropertyChangeEvent;
import java.util.Enumeration;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.text.JTextComponent;

import ca.sqlpower.sqlobject.SQLType;
import ca.sqlpower.sqlobject.UserDefinedSQLType;

/**
 * This class has several methods for JComponents of different types
 * to check for conflicts and change the UI in an appropriate way.
 * 
 * It is not meant to be used by all DataEntryPanels, just ChangeListeningDataEntryPanels.
 */
public class DataEntryPanelChangeUtil {
    
    public final static String ERROR_MESSAGE = 
        "<html>This object has been changed by another user.<br>" +
        "You must re-open this window before making changes.</html>";
        
    public final static Color NONCONFLICTING_COLOR = new Color(255, 255, 200);
    public final static Color DARK_NONCONFLICTING_COLOR = new Color(255, 200, 0);
    public final static Color CONFLICTING_COLOR = new Color(255, 200, 200);
    public final static Color DARK_CONFLICTING_COLOR = new Color(255, 100, 100);    
    
    /**
     * Sets the background color of the text field in case of an incoming change/conflict.
     */
    public static boolean incomingChange(JTextComponent field, PropertyChangeEvent e) {
        return changeBackground(field, field.getText(), e);
    }
    
    /**
     * Sets the background color and creates a colored border in case of an incoming change/conflict.
     */
    public static boolean incomingChange(JComboBox field, PropertyChangeEvent e) {
        Object fieldValue = field.getSelectedItem();
        if (field.getSelectedItem() instanceof String) {
            changeBackground(field, (String) fieldValue, e);
        } else if (field.getSelectedItem() instanceof SQLType) {
            SQLType type = (SQLType) field.getSelectedItem();
            fieldValue = type.getType();
            changeBackground(field, fieldValue, e);
        }
        return changeBorder(field, fieldValue, e);
    }
    
    /**
     * Sets the background color of the text field in case of an incoming change/conflict.
     */
    public static boolean incomingChange(JSpinner field, PropertyChangeEvent e) {        
        return changeBackground(
                ((JSpinner.DefaultEditor) field.getEditor()).getTextField(), field.getValue(), e);      
    }
    
    /**
     * Creates a colored border around the given component in case of incoming change/conflict.
     */
    public static boolean incomingChange(JCheckBox field, PropertyChangeEvent e) {       
        return changeBorder(field, field.isSelected(), e);
    }
    
    /**
     * Creates a colored border around the given component in case of incoming change/conflict.
     */
    public static boolean incomingChange(ButtonGroup field, Object fieldValue, PropertyChangeEvent e) {
        return changeBorder(getSelectedButton(field), fieldValue, e);
    }
    
    /**
     * Sets the background colour of the JTree in case of an incoming change/conflict. 
     */
    public static boolean incomingChange(JTree field, PropertyChangeEvent e) {
    	Object selection = field.getLastSelectedPathComponent();
		if (selection instanceof UserDefinedSQLType) {
    		return changeBackground(field, ((UserDefinedSQLType) selection).getType(), e);
    	} else {
    		return false;
    	}
    }

	/**
	 * Sets the background colour of the {@link JTable} in case of an incoming
	 * change/conflict.
	 * 
	 * @param table
	 *            The JTable to which the background colour should change on.
	 * @param evt
	 *            The {@link PropertyChangeEvent} that is the cause of the
	 *            incoming change or conflict.
	 * @return true if the background colour was set.
	 */
    public static boolean incomingChange(JTable table, PropertyChangeEvent evt) {
    	return changeBackground(table, null, evt);
    }
    
    /**
     * Sets the background color of the given component in case of incoming change/conflict.
     * @param field Any JComponent
     * @param fieldValue The value in the field that is of the same type as the values in e
     * @param e The old and new values will be checked against the fieldValue
     * @return True if the color was set due to an incoming change/conflict.
     */
    public static boolean changeBackground(JComponent field, Object fieldValue, PropertyChangeEvent e) {
        Object oldValue = e.getOldValue();        
        Object incomingValue = e.getNewValue();
        
        if (fieldValue instanceof String) {
            oldValue = (oldValue != null) ? ((String) oldValue).trim() : "";
            incomingValue = (incomingValue != null) ? ((String) incomingValue).trim() : "";
            fieldValue = (fieldValue != null) ? ((String) fieldValue).trim() : "";
        }
        
        if (incomingValue.equals(fieldValue)) return false;
        
        if (oldValue.equals(fieldValue)) {
            field.setBackground(NONCONFLICTING_COLOR);
        } else {
            field.setBackground(CONFLICTING_COLOR);            
        }
        return true;
    }
    
    /**
     * Creates a colored border around the given component in case of incoming change/conflict.
     * @param field Any JComponent
     * @param fieldValue The value in the field that is of the same type as the values in e
     * @param e The old and new values will be checked against the fieldValue
     * @return True if the border was made due to an incoming change/conflict.
     */
    public static boolean changeBorder(JComponent field, Object fieldValue, PropertyChangeEvent e) {
        Object oldValue = e.getOldValue();
        Object incomingValue = e.getNewValue();
        
        if (fieldValue instanceof String) {
            oldValue = (oldValue != null) ? ((String) oldValue).trim() : "";
            incomingValue = (incomingValue != null) ? ((String) incomingValue).trim() : "";
            fieldValue = (fieldValue != null) ? ((String) fieldValue).trim() : "";
        }
        
        if (incomingValue.equals(fieldValue)) return false;
        
        if (oldValue.equals(fieldValue)) {
            field.setBorder(BorderFactory.createLineBorder(DARK_NONCONFLICTING_COLOR, 3));
        } else {
            field.setBorder(BorderFactory.createLineBorder(DARK_CONFLICTING_COLOR, 3));        
        }        
        if (field instanceof AbstractButton) {
            ((AbstractButton) field).setBorderPainted(true);       
        }
        
        return true;
    }
    
    private static AbstractButton getSelectedButton(ButtonGroup field) {
        Enumeration<AbstractButton> buttons = field.getElements();
        while (buttons.hasMoreElements()) {
            AbstractButton b = buttons.nextElement();
            if (b.isSelected()) return b;
        }
        throw new IllegalStateException("No button is selected");        
    }
}
