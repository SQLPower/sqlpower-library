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
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.beanutils.PropertyUtils;

import ca.sqlpower.object.SPObject;
import ca.sqlpower.object.annotation.Accessor;
import ca.sqlpower.object.annotation.Mutator;
import ca.sqlpower.object.annotation.NonBound;
import ca.sqlpower.object.annotation.NonProperty;
import ca.sqlpower.object.annotation.Transient;

import junit.framework.TestCase;

public class TestUtils extends TestCase {
	
    public static void testPropertiesFireEvents(Object target, Collection<String> propertiesToIgnore, NewValueMaker valueMaker) {
        
    }

	/**
	 * Sets all the settable properties on the given target object which are not
	 * in the given ignore set.
	 * 
	 * @param target
	 *            The object to change the properties of
	 * @param propertiesToIgnore
	 *            The properties of target not to modify or read
	 * @return A Map describing the new values of all the non-ignored, readable
	 *         properties in target.
	 */
    public static Map<String,Object> setAllInterestingProperties(Object target,
    		Set<String> propertiesToIgnore) throws Exception {
    	return setAllInterestingProperties(target, propertiesToIgnore, 
    			new GenericNewValueMaker(new SPObjectRoot()));
    }

	/**
	 * Sets all the settable properties on the given target object which are not
	 * in the given ignore set.
	 * <p>
	 * TODO merge this with what is in Architect's TestUtils class. This was
	 * originally refactored out of there.
	 * 
	 * @param target
	 *            The object to change the properties of
	 * @param propertiesToIgnore
	 *            The properties of target not to modify or read
	 * @return A Map describing the new values of all the non-ignored, readable
	 *         properties in target.
	 */
    public static Map<String,Object> setAllInterestingProperties(Object target,
    		Set<String> propertiesToIgnore, NewValueMaker valueMaker) throws Exception {
    	
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
    
    
    public static Set<String> findPersistableBeanProperties(SPObject objectUnderTest, boolean includeTransient, boolean includeConstructorMutators) throws Exception {
		Set<String> getters = new HashSet<String>();
		Set<String> setters = new HashSet<String>();
		for (Method m : objectUnderTest.getClass().getMethods()) {
			if (m.getName().equals("getClass")) continue;
			
			//skip non-public methods as they are not visible for persisting anyways.
			if (!Modifier.isPublic(m.getModifiers())) continue;
			//skip static methods
			if (Modifier.isStatic(m.getModifiers())) continue;
			
			if (m.getName().startsWith("get") || m.getName().startsWith("is")) {
				Class<?> parentClass = objectUnderTest.getClass();
				boolean accessor = false;
				boolean ignored = false;
				boolean isTransient = false;
				parentClass.getMethod(m.getName(), m.getParameterTypes());//test
				while (parentClass != null) {
					Method parentMethod;
					try {
						parentMethod = parentClass.getMethod(m.getName(), m.getParameterTypes());
					} catch (NoSuchMethodException e) {
						parentClass = parentClass.getSuperclass();
						continue;
					}
					if (parentMethod.getAnnotation(Accessor.class) != null) {
						accessor = true;
						if (parentMethod.getAnnotation(Transient.class) != null) {
							isTransient = true;
						}
						break;
					} else if (parentMethod.getAnnotation(NonProperty.class) != null ||
							parentMethod.getAnnotation(NonBound.class) != null) {
						ignored = true;
						break;
					}
					parentClass = parentClass.getSuperclass();
				}
				if (accessor) {
					if (includeTransient || !isTransient) {
						if (m.getName().startsWith("get")) {
							getters.add(m.getName().substring(3));
						} else if (m.getName().startsWith("is")) {
							getters.add(m.getName().substring(2));
						}
					}
				} else if (ignored) {
					//ignored so skip
				} else {
					fail("The method " + m.getName() + " of " + objectUnderTest.toString() + " is a getter that is not annotated " +
							"to be an accessor or transient. The exiting annotations are " + 
							Arrays.toString(m.getAnnotations()));
				}
			} else if (m.getName().startsWith("set")) {
				if (m.getAnnotation(Mutator.class) != null) {
					if ((includeTransient || m.getAnnotation(Transient.class) == null)
							&& (includeConstructorMutators || !m.getAnnotation(Mutator.class).constructorMutator())) {
						setters.add(m.getName().substring(3));
					}
				} else if (m.getAnnotation(NonProperty.class) != null ||
						m.getAnnotation(NonBound.class) != null) {
					//ignored so skip and pass
				} else {
					fail("The method " + m.getName() + " is a setter that is not annotated " +
							"to be a mutator or transient.");
				}
			}
		}
		
		Set<String> beanNames = new HashSet<String>();
		for (String beanName : getters) {
			if (setters.contains(beanName)) {
				String firstLetter = new String(new char[]{beanName.charAt(0)});
				beanNames.add(beanName.replaceFirst(firstLetter, firstLetter.toLowerCase()));
			}
		}
		return beanNames;
	}
}
