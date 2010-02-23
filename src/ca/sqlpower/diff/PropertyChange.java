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

package ca.sqlpower.diff;

import ca.sqlpower.sqlobject.SQLType;

/**
 * This class is used by DiffInfo to store information about property changes.
 */
public class PropertyChange {
    
    /**
     * The name of the property that was changed
     */
    final private String propertyName;
    
    /**
     * A string representation of the property's old value
     */
    final private String oldValue;

    /**
     * A string representation of the property's new value
     */
    final private String newValue;

    /**
     * @param propertyName
     * @param oldValue
     * @param newValue
     */
    public PropertyChange(String propertyName, String oldValue, String newValue) {
        this.propertyName = propertyName;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public String getOldValue() {
        if (propertyName.equals("type")) {            
            return SQLType.getTypeName(Integer.parseInt(oldValue));          
        } else {
            return oldValue;
        }
    }

    public String getNewValue() {
        if (propertyName.equals("type")) {                        
            return SQLType.getTypeName(Integer.parseInt(newValue));           
        } else {
            return newValue;
        }
    }
    
    public String toString() {
        return propertyName + " changed from " + getOldValue() + " to " + getNewValue();
    }
    
}
