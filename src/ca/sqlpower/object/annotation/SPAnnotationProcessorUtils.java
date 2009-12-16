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

package ca.sqlpower.object.annotation;

import ca.sqlpower.dao.SPPersisterHelper;
import ca.sqlpower.object.SPObject;

import com.sun.mirror.type.ClassType;
import com.sun.mirror.type.InterfaceType;
import com.sun.mirror.type.PrimitiveType;
import com.sun.mirror.type.TypeMirror;
import com.sun.mirror.type.PrimitiveType.Kind;

public class SPAnnotationProcessorUtils {
	
	private SPAnnotationProcessorUtils() {
		// Should not instantiate an instance of this class as all methods are static.
	}
	
	/**
	 * Converts a primitive data type to the equivalent {@link Class} type.
	 * 
	 * @param kind
	 *            The {@link PrimitiveType.Kind} to convert.
	 * @return The equivalent {@link Class} of the primitive type.
	 */
	public static Class<?> convertPrimitiveToClass(Kind kind) {
		if (kind.equals(Kind.BOOLEAN)) {
			return Boolean.class;
		} else if (kind.equals(Kind.BYTE)) {
			return Byte.class;
		} else if (kind.equals(Kind.CHAR)) {
			return Character.class;
		} else if (kind.equals(Kind.DOUBLE)) {
			return Double.class;
		} else if (kind.equals(Kind.FLOAT)) {
			return Float.class;
		} else if (kind.equals(Kind.INT)) {
			return Integer.class;
		} else if (kind.equals(Kind.LONG)) {
			return Long.class;
		} else if (kind.equals(Kind.SHORT)) {
			return Short.class;
		} else {
			throw new IllegalStateException("The PrimitiveType Kind " + 
					kind.name() + " is not recognized. This exception " +
							"should never be thrown.");
		}
	}

	/**
	 * Converts a primitive, class or interface {@link TypeMirror} to an actual
	 * class instance.
	 * 
	 * @param type
	 *            The {@link TypeMirror} to convert.
	 * @return The class instance of the converted {@link TypeMirror}.
	 * @throws ClassNotFoundException
	 *             Thrown if the class instance of a class/interface
	 *             {@link TypeMirror} could not be found.
	 */
	public static Class<?> convertTypeMirrorToClass(TypeMirror type) throws ClassNotFoundException {
		if (type instanceof PrimitiveType) {
			return convertPrimitiveToClass(((PrimitiveType) type).getKind());
		} else if (type instanceof ClassType || type instanceof InterfaceType) {
			return Class.forName(type.toString());
		} else {
			return null;
		}
	}

	/**
	 * Converts a JavaBean formatted method name to a property name.
	 * 
	 * @param methodName
	 *            The JavaBean formatted method name. This method name must
	 *            start with either "is", "get", or "set" and there must be
	 *            characters following after it.
	 * @return The converted property name.
	 * @throws IllegalArgumentException
	 *             Thrown if the given method name does not follow the JavaBean
	 *             format.
	 */
	public static String convertMethodToProperty(String methodName) throws IllegalArgumentException {
		String propertyName;
		if (methodName.length() > 2 && methodName.startsWith("is")) {
			propertyName = methodName.substring(2);
		} else if (methodName.length() > 3 && 
				(methodName.startsWith("get") || methodName.startsWith("set"))) {
			propertyName = methodName.substring(3);
		} else {
			throw new IllegalArgumentException("Cannot convert method name \"" + 
					methodName + "\" to property name as it is not a valid JavaBean name.");
		}
		
		if (!propertyName.equals("UUID")) {
			propertyName = propertyName.substring(0, 1).toLowerCase() + 
					((propertyName.length() > 1)? propertyName.substring(1) : "");
		}
		
		return propertyName;
	}

	/**
	 * Converts a property name to its JavaBean getter method name depending on
	 * what type the property is.
	 * 
	 * @param propertyName
	 *            The property name to convert to its JavaBean getter method
	 *            name.
	 * @param type
	 *            The property type.
	 * @return The JavaBean getter method name. If the property type is a
	 *         {@link Boolean} value, than the getter method name is prefixed
	 *         with "is". Otherwise, it is prefixed with "get".
	 */
	public static String convertPropertyToAccessor(String propertyName, Class<?> type) {
		String prefix;
		if (type == Boolean.class) {
			prefix = "is";
		} else {
			prefix = "get";
		}
		
		return prefix + propertyName.substring(0, 1).toUpperCase() + 
				((propertyName.length() > 1)? propertyName.substring(1) : "");
	}

	/**
	 * Converts an {@link SPObject} class to a suitable
	 * {@link SPPersisterHelper} field name. This is essentially the simple name
	 * of the class but prepended with "helperFor".
	 * 
	 * @param clazz
	 *            The {@link SPObject} class to convert.
	 * @return The field name corresponding to the given {@link SPObject} class.
	 */
	public static String convertClassToFieldName(Class<? extends SPObject> clazz) {
		return "helperFor" + clazz.getSimpleName();
	}
	
}
