/*
 * Copyright (c) 2009, SQL Power Group Inc.
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

package ca.sqlpower.testutil;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.beanutils.PropertyUtils;

public class TestUtils {
	
    public static void testPropertiesFireEvents(Object target, Collection<String> propertiesToIgnore, NewValueMaker valueMaker) {
        
    }
    
    /**
     * Sets all the settable properties on the given target object
     * which are not in the given ignore set.
     * <p>
     * XXX This method is taken from Architect's TestUtils class for use with
     * SQLObjects. To merge these two methods back together a ValueMaker would
     * need to be passed into the method to allow Architect to specify additional
     * values for ETL functions and to prevent ETL classes from needing to be in 
     * the library.
     * 
     * @param target The object to change the properties of
     * @param propertiesToIgnore The properties of target not to modify or read
     * @return A Map describing the new values of all the non-ignored, readable 
     * properties in target.
     */
    public static Map<String,Object> setAllInterestingProperties(Object target,
    		Set<String> propertiesToIgnore) throws Exception {
    	
    	PropertyDescriptor props[] = PropertyUtils.getPropertyDescriptors(target);
    	for (int i = 0; i < props.length; i++) {
    		Object oldVal = null;
    		if (PropertyUtils.isReadable(target, props[i].getName()) &&
    				props[i].getReadMethod() != null &&
    				!propertiesToIgnore.contains(props[i].getName())) {
    			oldVal = PropertyUtils.getProperty(target, props[i].getName());
    		}
    		if (PropertyUtils.isWriteable(target, props[i].getName()) &&
    				props[i].getWriteMethod() != null &&
    				!propertiesToIgnore.contains(props[i].getName())) {
    		
    		    NewValueMaker valueMaker = new GenericNewValueMaker();
    		    Object newVal = valueMaker.makeNewValue(props[i].getPropertyType(), oldVal, props[i].getName());

    		    System.out.println("Changing property \""+props[i].getName()+"\" to \""+newVal+"\"");
                PropertyUtils.setProperty(target, props[i].getName(), newVal);

    		}
    	}
    	
    	// read them all back at the end in case there were dependencies between properties
    	return TestUtils.getAllInterestingProperties(target, propertiesToIgnore);
    }
    
    /**
     * Gets all the settable properties on the given target object
     * which are not in the given ignore set, and stuffs them into a Map.
     * 
     * @param target The object to change the properties of
     * @param propertiesToIgnore The properties of target not to modify or read
     * @return The aforementioned stuffed map
     */
    public static Map<String, Object> getAllInterestingProperties(Object target, Set<String> propertiesToIgnore) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
    	Map<String,Object> newDescription = new HashMap<String,Object>();
    	PropertyDescriptor[] props = PropertyUtils.getPropertyDescriptors(target);
    	for (int i = 0; i < props.length; i++) {
    		if (PropertyUtils.isReadable(target, props[i].getName()) &&
    				props[i].getReadMethod() != null &&
    				!propertiesToIgnore.contains(props[i].getName())) {
    			newDescription.put(props[i].getName(),
    					PropertyUtils.getProperty(target, props[i].getName()));
    		}
    	}
    	return newDescription;
    }
}
