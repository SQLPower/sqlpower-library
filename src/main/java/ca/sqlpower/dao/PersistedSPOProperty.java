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

package ca.sqlpower.dao;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;

import ca.sqlpower.dao.SPPersister.DataType;
import ca.sqlpower.object.SPObject;
import ca.sqlpower.util.SQLPowerUtils;

/**
 * A class representing an individual persisted {@link SPObject}
 * property.
 */
public class PersistedSPOProperty implements SPTransactionElement, Comparable<PersistedSPOProperty> {

	private final String uuid;
	private final String propertyName;
	private final DataType dataType;
	private final Object oldValue;
	private final Object newValue;
	private final boolean unconditional;

	/**
	 * Constructor to persist a {@link SPObject} property, keeping track
	 * of all the parameters of the persistProperty(...) method call. These
	 * fields will be necessary for when commit() is called.
	 * 
	 * @param propertyName
	 *            The name of the property to persist
	 * @param newValue
	 *            The property value to persist
	 * @param unconditional
	 *            Whether or not to validate if oldValue matches the actual
	 *            property value before persisting
	 */
	public PersistedSPOProperty(String uuid, String propertyName,
			DataType dataType, Object oldValue, Object newValue,
			boolean unconditional) {
		this.uuid = uuid;
		this.propertyName = propertyName;
		this.newValue = newValue;
		this.oldValue = oldValue;
		this.unconditional = unconditional;
		this.dataType = dataType;
	}

	/**
	 * Accessor for the property name field
	 * 
	 * @return The property name to persist upon
	 */
	public String getPropertyName() {
		return propertyName;
	}

	/**
	 * Accessor for the property value to persist
	 * 
	 * @return The property value to persist
	 */
	public Object getNewValue() {
		return newValue;
	}

	/**
	 * Accessor for the unconditional persist determinant
	 * 
	 * @return The unconditional field
	 */
	public boolean isUnconditional() {
		return unconditional;
	}

	public DataType getDataType() {
		return dataType;
	}

	public Object getOldValue() {
		return oldValue;
	}

	public String getUUID() {
		return uuid;
	}

	@Override
	public String toString() {
		return "SPObjectProperty [uuid=" + uuid + ", propertyName="
				+ propertyName + ", dataType=" + dataType + ", oldValue="
				+ oldValue + ", newValue=" + newValue + ", unconditional="
				+ unconditional + "]";
	}
	
    @Override
    public boolean equals(Object obj) {
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        
        PersistedSPOProperty wop = (PersistedSPOProperty) obj;
        
        if (!isUnconditional() && !wop.isUnconditional() 
                && !SQLPowerUtils.areEqual(getOldValue(), wop.getOldValue())) {
            return false;
        }
        
        if (isUnconditional() == wop.isUnconditional()
                && getUUID().equals(wop.getUUID()) 
                && getPropertyName().equals(wop.getPropertyName()) 
                && getDataType().equals(wop.getDataType())) {
    
            if (getDataType().equals(DataType.PNG_IMG)) {
                return imageObjectsAreEqual(getNewValue(), wop.getNewValue());
            } else {
            	if (getNewValue() == null) {
            		return wop.getNewValue() == null;
            	} else {
            		return getNewValue().equals(wop.getNewValue());
            	}
            }
            
        } else {
            return false;
        }
    
    }
	 
    /**
     * Compare two property values of PersistedSPOProperty objects that are an image type.     
     * 
     * @param a Image property of type ByteArrayInputStream
     * @param b Image property of type ByteArrayInputStream
     * 
     * @throws IllegalArgumentException thrown if one or both parameters are not ByteArrayInputStreams 
     */
    public static boolean imageObjectsAreEqual(Object a, Object b) throws IllegalArgumentException {
        if (a instanceof ByteArrayInputStream) {
            
            ByteArrayInputStream aStream = (ByteArrayInputStream) a;
            ByteArrayInputStream bStream = (ByteArrayInputStream) b;
            
            byte[] aBytes = new byte[aStream.available()];
            byte[] bBytes = new byte[bStream.available()];
            
            try {
                
                aStream.mark(-1); // No mark limit
                aStream.read(aBytes);
                aStream.reset();
                bStream.mark(-1); // No mark limit
                bStream.read(bBytes);
                bStream.reset();
                
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            
            return Arrays.equals(aBytes, bBytes);
            
        } else {
            throw new IllegalArgumentException();
        }                       
    }
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 17;
		
		result = prime * result + (unconditional ? 1 : 0);
		result = prime * result + ((oldValue == null) ? 0 : oldValue.hashCode());
		result = prime * result + uuid.hashCode();
		result = prime * result + propertyName.hashCode();
		result = prime * result + dataType.hashCode();
		result = prime * result + ((newValue == null) ? 0 : newValue.hashCode());
		
		return result;
	}
	
    public int compareTo(PersistedSPOProperty p) {  
        
        
        return (getUUID() + getPropertyName()).compareTo(p.getUUID() + p.getPropertyName());
        
    }	
	
}
