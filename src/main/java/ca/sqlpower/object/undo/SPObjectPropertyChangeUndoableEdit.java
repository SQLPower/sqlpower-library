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

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

import org.apache.log4j.Logger;

import ca.sqlpower.object.SPObject;

/**
 * Represents an undoable property change operation on a SQL Object.
 * <p>
 * XXX This class should take a new kind of event, an SPObjectPropertyEvent or
 * something of the sort but more time is needed to fix this.
 */
public class SPObjectPropertyChangeUndoableEdit extends AbstractUndoableEdit {
	private static final Logger logger = Logger.getLogger(SPObjectPropertyChangeUndoableEdit.class);

	private PropertyChangeEvent event;
	private String toolTip;
	
	public SPObjectPropertyChangeUndoableEdit(PropertyChangeEvent e) {
        if (e == null) throw new NullPointerException("Null event is not allowed");
		event = e;
		toolTip = createToolTip();
	}
	
	private String createToolTip() {
	    return "Set " + event.getPropertyName() + " to " + event.getNewValue();
    }
	
	@Override
	public void redo() throws CannotRedoException {
		try {
            ((SPObject) event.getSource()).setMagicEnabled(false);
		    modifyProperty(event.getNewValue());
		} catch (IllegalAccessException e) {
			logger.error("Couldn't access setter for "+
					event.getPropertyName(), e);
			throw new CannotRedoException();
		} catch (InvocationTargetException e) {
			logger.error("Setter for "+event.getPropertyName()+
					" on "+event.getSource()+" threw exception", e);
			throw new CannotRedoException();
		} catch (IntrospectionException e) {
			logger.error("Couldn't introspect source object "+
					event.getSource(), e);
			throw new CannotRedoException();
		} finally {
            ((SPObject) event.getSource()).setMagicEnabled(true);
        }
		super.redo();
	}
	
	@Override
	public void undo() throws CannotUndoException {
		try {
            ((SPObject) event.getSource()).setMagicEnabled(false);
		    modifyProperty(event.getOldValue());
		} catch (IllegalAccessException e) {
			logger.error("Couldn't access setter for "+
					event.getPropertyName(), e);
			throw (CannotUndoException) new CannotUndoException().initCause(e);
		} catch (InvocationTargetException e) {
			logger.error("Setter for "+event.getPropertyName()+
					" on "+event.getSource()+" threw exception", e);
			throw (CannotUndoException) new CannotUndoException().initCause(e);
		} catch (IntrospectionException e) {
			logger.error("Couldn't introspect source object "+
					event.getSource(), e);
			throw (CannotUndoException) new CannotUndoException().initCause(e);
		} finally {
            ((SPObject) event.getSource()).setMagicEnabled(true);
        }
		super.undo();
	}
	
	private void modifyProperty(Object value) throws IntrospectionException,
            IllegalArgumentException, IllegalAccessException,
            InvocationTargetException {
        // We did this using BeanUtils.copyProperty() before, but the error
        // messages were too vague.
        BeanInfo info = Introspector.getBeanInfo(event.getSource().getClass());

        PropertyDescriptor[] props = info.getPropertyDescriptors();
        for (PropertyDescriptor prop : Arrays.asList(props)) {
            if (prop.getName().equals(event.getPropertyName())) {
                Method writeMethod = prop.getWriteMethod();
                if (writeMethod != null) {
                    writeMethod.invoke(event.getSource(), new Object[] { value });
                }
            }
        }
    }
	
	@Override
	public String getPresentationName() {
		return toolTip;
	}
	
	@Override
	public String toString() {
		return event.getSource() + "."+event.getPropertyName()
        +" changed from ["+event.getOldValue()
        +"] to ["+ event.getNewValue() + "]";
	}
}
