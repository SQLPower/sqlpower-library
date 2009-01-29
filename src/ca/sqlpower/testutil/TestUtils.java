/*
 * Copyright (c) 2008, SQL Power Group Inc.
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
