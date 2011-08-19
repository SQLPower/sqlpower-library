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

package ca.sqlpower.object.undo;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.log4j.Logger;

/**
 * This is the generic edit class that dynamically modifies bean properties
 * according to the PropertyChangeEvent source and the property name.
 * 
 * @author kaiyi
 *
 */
public class PropertyChangeEdit extends AbstractUndoableEdit {
    
    private static final Logger logger = Logger.getLogger(PropertyChangeEdit.class);

    private final PropertyChangeEvent sourceEvent;

    public PropertyChangeEdit(PropertyChangeEvent e) {
        this.sourceEvent = e;
    }

    /**
     * Sets the value of the property to be the old value
     */
    @Override
    public void undo() throws CannotUndoException {
    	logger.debug("Undoing Property change: Setting " + sourceEvent.getPropertyName() + " from " + sourceEvent.getNewValue() + " to " + sourceEvent.getOldValue());
        super.undo();
        try {
            final PropertyDescriptor propertyDescriptor = PropertyUtils.getPropertyDescriptor(sourceEvent.getSource(), sourceEvent.getPropertyName());
            logger.debug("Found property descriptor " + propertyDescriptor);
            if (logger.isDebugEnabled()) {
            	PropertyDescriptor[] propertyDescriptors = PropertyUtils.getPropertyDescriptors(sourceEvent.getSource());
            	logger.debug("Descriptor has write method " + propertyDescriptor.getWriteMethod());
            }
			Method setter = PropertyUtils.getWriteMethod(propertyDescriptor);
            logger.info("Found setter: " + setter.getName());
            setter.invoke(sourceEvent.getSource(), sourceEvent.getOldValue());

        } catch (Exception ex) {
            CannotUndoException wrapper = new CannotUndoException();
            wrapper.initCause(ex);
            throw wrapper;
        }
    }

    /**
     * Sets the value of the property to be the new value
     */
    @Override
    public void redo() throws CannotRedoException {
    	logger.debug("Undoing Property change: Setting " + sourceEvent.getPropertyName() + " from " + sourceEvent.getOldValue() + " to " + sourceEvent.getNewValue());
        super.redo();
        try {
            Method setter = PropertyUtils.getWriteMethod(PropertyUtils.getPropertyDescriptor(sourceEvent.getSource(), sourceEvent.getPropertyName()));
            logger.info("Found setter: " + setter.getName());
            setter.invoke(sourceEvent.getSource(), sourceEvent.getNewValue());

        } catch (Exception ex) {
            CannotRedoException wrapper = new CannotRedoException();
            wrapper.initCause(ex);
            throw wrapper;
        }
    }

    @Override
    public String getPresentationName() {
        return "property change edit";
    }
    
    /**
     * Returns the name of the property that this edit represents a change to.
     */
    public String getPropertyName() {
        return sourceEvent.getPropertyName();
    }

    /**
     * Returns the property's new value. This is the value that this edit will redo to.
     */
    public Object getNewValue() {
        return sourceEvent.getNewValue();
    }

    /**
     * Returns the property's old value. This is the value that this edit will undo to.
     */
    public Object getOldValue() {
        return sourceEvent.getOldValue();
    }

    /**
     * Returns the object whose property changed.
     * @return
     */
    public Object getSource() {
        return sourceEvent.getSource();
    }

    @Override
    public String toString() {
        return "Changing property: \"" + sourceEvent.getPropertyName() + "\" by "+sourceEvent;
    }
}
