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

package ca.sqlpower.dao;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.imageio.ImageIO;

import com.google.common.collect.Multimap;

import ca.sqlpower.dao.SPPersister.DataType;
import ca.sqlpower.dao.session.BidirectionalConverter;
import ca.sqlpower.dao.session.SessionPersisterSuperConverter;
import ca.sqlpower.object.SPObject;
import ca.sqlpower.object.annotation.Accessor;
import ca.sqlpower.sqlobject.SQLObject;

/**
 * Utilities that are used by {@link SPPersister}s. 
 */
public class PersisterUtils {
	
	private PersisterUtils() {
		//cannot instantiate this class as it is just static utility methods.
	}

	/**
	 * Converts an image to an output stream to be persisted in some way.
	 * 
	 * @param img
	 *            The image to convert to an output stream for persisting.
	 * @return An output stream containing an encoding of the image as PNG.
	 */
	public static ByteArrayOutputStream convertImageToStreamAsPNG(Image img) {
		BufferedImage image;
        if (img instanceof BufferedImage) {
            image = (BufferedImage) img;
        } else {
            image = new BufferedImage(img.getWidth(null), 
            		img.getHeight(null), BufferedImage.TYPE_INT_ARGB); 
            final Graphics2D g = image.createGraphics();
            g.drawImage(img, 0, 0, null);
            g.dispose();
        }
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        if (image != null) {
        	try {
				ImageIO.write(image, "PNG", byteStream);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
        }
        return byteStream;
	}
	
	/**
	 * Splits a string by the converter delimiter and checks that the correct
	 * number of pieces are returned or it throws an
	 * {@link IllegalArgumentException}. This is a simple place to do general
	 * error checking when first converting a string into an object.
	 * 
	 * @param toSplit
	 *            The string to split by the delimiter.
	 * @param numPieces
	 *            The number of pieces the string must be split into.
	 * @return The pieces the string is split into.
	 */
	public static String[] splitByDelimiter(String toSplit, int numPieces) {
		String[] pieces = toSplit.split(BidirectionalConverter.DELIMITER);

		if (pieces.length > numPieces) {
			throw new IllegalArgumentException("Cannot convert string \""
					+ toSplit + "\" with an invalid number of properties.");
		} else if (pieces.length < numPieces) {
			//split will strip off empty space that comes after a delimiter instead of
			//appending an empty string to the array so we have to do that ourselves.
			String[] allPieces = new String[numPieces];
			
			int i = 0;
			for (String piece : pieces) {
				allPieces[i] = piece;
				i++;
			}
			for (; i < numPieces; i++) {
				allPieces[i] = "";
			}
			return allPieces;
		}
		return pieces;
	}
	
	/**
	 * Returns a set of all the interesting property names of the given SQLObject type.
	 * @param type
	 * @return
	 * @throws SecurityException
	 * @throws IllegalArgumentException
	 * @throws ClassNotFoundException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	public static Set<String> getInterestingPropertyNames(String type)
	throws SecurityException, IllegalArgumentException, ClassNotFoundException, IllegalAccessException, InvocationTargetException {
	    return getInterestingProperties(type, null, null).keySet();
	}
	
    /**
     * Returns a map containing all the interesting properties of the class type
     * given by the fully qualified name. An interesting property is a
     * non-transient accessor with the isInteresting flag set to true. The
     * properties are mapped by their name, and contain their value, converted
     * to a non-complex type
     * 
     * @param className
     * @param converter
     * @return
     * @throws SecurityException
     * @throws ClassNotFoundException
     * @throws InvocationTargetException 
     * @throws IllegalAccessException 
     * @throws IllegalArgumentException 
     */
	public static Map<String, Object> getInterestingProperties(SQLObject object, SessionPersisterSuperConverter converter)
    throws SecurityException, ClassNotFoundException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
	    return getInterestingProperties(object.getClass().getName(), object, converter);
	}
	
	/**
	 * Does what the other getInterestingProperties says it does if passed a non-null object
	 * Otherwise, it will return a map with a key set of all the property names, but no values.
	 * 
	 * @param type
	 * @param object
	 * @param converter
	 * @return
	 * @throws SecurityException
	 * @throws ClassNotFoundException
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
    private static Map<String, Object> getInterestingProperties(
            String type, SQLObject object, SessionPersisterSuperConverter converter)
	throws SecurityException, ClassNotFoundException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
	    Map<String, Object> propertyMap = new HashMap<String, Object>();
	    
	    Class<? extends Object> objectClass = Class.forName(type, true, PersisterUtils.class.getClassLoader());
	    for (Method m : objectClass.getMethods()) {
	        
	        if (m.getAnnotation(Accessor.class) != null
	                && m.getAnnotation(Accessor.class).isInteresting()) {
	            String propertyName;
	            if (m.getName().startsWith("get")) {
                    propertyName = m.getName().substring(3);
                } else if (m.getName().startsWith("is")) {
                    propertyName = m.getName().substring(2);
                } else {
                    throw new RuntimeException("Accessor class with improper prefix");
                }
	            String firstCharacter = String.valueOf(propertyName.charAt(0));
	            
	            propertyName = propertyName.replaceFirst(
	                    firstCharacter, firstCharacter.toLowerCase());
	            
	            if (object != null) {
	                propertyMap.put(propertyName, 
	                        converter.convertToBasicType(m.invoke(object)));
	            } else {
	                propertyMap.put(propertyName, "");
	            }
	        }
	        
	    }
	    return propertyMap;
	    
	}
	
    /**
     * Gets the correct data type based on the given class for the {@link SPPersister}.
     */
    public static DataType getDataType(Class<? extends Object> classForDataType) {
    	if (classForDataType == null) return DataType.NULL;
    	
    	if (Integer.class.isAssignableFrom(classForDataType)) {
    		return DataType.INTEGER;
    	} else if (Long.class.isAssignableFrom(classForDataType)) {
    	    return DataType.LONG;
    	}  else if (Short.class.isAssignableFrom(classForDataType)) {
    		return DataType.SHORT;
    	} else if (Boolean.class.isAssignableFrom(classForDataType)) {
    		return DataType.BOOLEAN;
    	} else if (Double.class.isAssignableFrom(classForDataType)) {
    		return DataType.DOUBLE;
    	} else if (Float.class.isAssignableFrom(classForDataType)) {
    		return DataType.FLOAT;
    	} else if (String.class.isAssignableFrom(classForDataType)) {
    		return DataType.STRING;
    	} else if (Image.class.isAssignableFrom(classForDataType)) {
    		return DataType.PNG_IMG;
    	} else if (SPObject.class.isAssignableFrom(classForDataType)) {
    		return DataType.REFERENCE;
    	} else if (Void.class.isAssignableFrom(classForDataType)) {
    		return DataType.NULL;
    	} else {
    		return DataType.STRING;
    	}
    }
    
    /**
     * Produces and int array from a String containing comma-separated values
     * @param data A String containing comma-separated values, eg: "-1, 7, 2", "-1,7,2", "-1", ""
     * @return {-1, 7, 2}, {-1, 7, 2}, {-1}, {}
     */
    public static int[] convertStringToIntArray(String data) {
        String[] s = data.split(",");
        int[] ints = new int[s.length];
        for (int i = 0; i < s.length; i++) {
            try {
                ints[i] = Integer.parseInt(s[i]);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(e);
            }
        }
        return ints;
    }
    
    /**
     * Produces a String containing the given integers, separated by commas.
     * @param data An int array, eg: {-1, 7, 2}, {-1}, {}
     * @return A String of comma-separated values, eg: "-1,7,2", "-1", ""
     */
    public static String convertIntArrayToString(int[] data) {
        String s = "";
        for (int i = 0; i < data.length; i++) {
            s += String.valueOf(data[i]);
            if (i != data.length - 1) {
                s += ",";
            }
        }
        return s;
    }
    
    /**
     * Returns the first index of this childType in the child type list of
     * the parentType. If a superclass or interface of the childType exists
     * in the list of acceptable child types of the parent this index may be
     * returned, depending if it is the first. If the childType is not a
     * valid child type of the parentType -1 will be returned.
     */
    @SuppressWarnings("unchecked")
    public static int getTypePosition(String childClassName, String parentClassName) 
            throws IllegalArgumentException, SecurityException, IllegalAccessException, NoSuchFieldException, ClassNotFoundException {
        Class<? extends SPObject> childType = (Class<? extends SPObject>) PersisterUtils.class.getClassLoader().loadClass(childClassName);
        Class<? extends SPObject> parentType = (Class<? extends SPObject>) PersisterUtils.class.getClassLoader().loadClass(parentClassName);
        
        List<Class<? extends SPObject>> allowedChildTypes = (List<Class<? extends SPObject>>) 
            parentType.getDeclaredField("allowedChildTypes").get(null);
        for (int i = 0; i < allowedChildTypes.size(); i++) {
            Class<? extends SPObject> allowedType = allowedChildTypes.get(i);
            if (allowedType.isAssignableFrom(childType)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Returns the class type that the given parent class allows that is either
     * the same or a superclass or interface of the given child class. Returns
     * the child class if the parent class is null or the empty string. Returns
     * null if the parent class has no valid child type of the given child
     * class.
     */
    @SuppressWarnings("unchecked")
    public static Class<? extends SPObject> getParentAllowedChildType(String childClassName, String parentClassName) 
            throws IllegalArgumentException, SecurityException, IllegalAccessException, NoSuchFieldException, ClassNotFoundException {
        Class<? extends SPObject> childType = (Class<? extends SPObject>) PersisterUtils.class.getClassLoader().loadClass(childClassName);
        
        if (parentClassName == null || parentClassName.trim().length() == 0) {
            return childType;
        }
        
        Class<? extends SPObject> parentType = (Class<? extends SPObject>) PersisterUtils.class.getClassLoader().loadClass(parentClassName);
        return getParentAllowedChildType(childType, parentType);
    }

	@SuppressWarnings("unchecked")
	public static Class<? extends SPObject> getParentAllowedChildType(
			Class<? extends SPObject> childType,
			Class<? extends SPObject> parentType)
			throws IllegalAccessException, NoSuchFieldException {
		List<Class<? extends SPObject>> allowedChildTypes = (List<Class<? extends SPObject>>) 
            parentType.getDeclaredField("allowedChildTypes").get(null);
        if (allowedChildTypes.contains(childType)) return childType;
        for (int i = 0; i < allowedChildTypes.size(); i++) {
            Class<? extends SPObject> allowedType = allowedChildTypes.get(i);
            if (allowedType.isAssignableFrom(childType)) {
                return allowedType;
            }
        }
        return null;
	}
    
    /**
     * A way to get the allowed child list from a class object that is an SPObject.
     */
    @SuppressWarnings("unchecked")
    public static List<Class<? extends SPObject>> getAllowedChildTypes(Class<? extends SPObject> parentClass) 
    		throws IllegalArgumentException, SecurityException, IllegalAccessException, NoSuchFieldException {
		return (List<Class<? extends SPObject>>) parentClass.getDeclaredField("allowedChildTypes").get(null);
    }

    /**
     * Returns true if there is a boolean property persisted on the given UUID
     * with the given property name and the property is being set to true.
     * Returns false if there is a boolean property persisted on the given UUID
     * with the given property name and the property is being set to false.
     * Returns null if the persist call does not exist.
     * 
     * @param persistedProperties
     *            The list of properties that have been persisted to search for
     *            the persist call.
     * @param parentUUID
     *            The UUID of the object we are looking for the boolean property
     *            of.
     * @param propName
     *            The property name that describes a boolean property. The
     *            property if it exists must represent a boolean.
     * @return
     */
    public static Boolean findPersistedBooleanProperty(
            Multimap<String, PersistedSPOProperty> persistedProperties,
            String parentUUID, String propName) {
        Collection<PersistedSPOProperty> properties = persistedProperties.get(parentUUID);
        if (properties == null || properties.isEmpty()) return null;
        for (PersistedSPOProperty property : properties) {
            if (property.getPropertyName().equals(propName)) {
                return (Boolean) property.getNewValue();
            }
        }
        return null;
    }

}
