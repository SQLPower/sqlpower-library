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

package ca.sqlpower.dao.session;

import ca.sqlpower.object.SPObject;
import ca.sqlpower.util.SPSession;

/**
 * Converts any known object into a simple type of object that can be
 * pushed through an HTTP request and persisted on the server. This also
 * contains a way to get the object back based on the simple type that can be
 * passed and stored.
 */
public class SessionPersisterSuperConverter {
	
	private final SPObjectConverter spObjectConverter;

	/**
	 * This converter will allow changes between any complex object in the
	 * session's workspace and a simple type that can be passed between
	 * persisters.
	 * 
	 * @param session
	 *            The session used to find necessary parts for converting
	 *            between simple and complex types. The session may be used to
	 *            look up connections, cubes, and {@link SPObject}s in the
	 *            workspace.
	 */
	public SessionPersisterSuperConverter(SPSession session, SPObject root) {
		spObjectConverter = new SPObjectConverter(root);
	}

	/**
	 * Converts a complex object to a basic type or reference value that can
	 * then be passed on to other persisters. To reverse this process see
	 * {@link #convertToComplexType}. If a basic object is given to this method
	 * it will be returned without modification.
	 * 
	 * @param convertFrom
	 *            The value to convert to a basic type
	 * @param fromType
	 *            the type that the basic type will be defined as
	 * @param additionalInfo
	 *            any additional information that is required by the converters
	 *            for specific object types. The ONLY class that currently
	 *            requires an additional type is the cube converter. If we can
	 *            remove the need to pass the data source type with the cube
	 *            then this value can go away.
	 */
	@SuppressWarnings("unchecked")
	public Object convertToBasicType(Object convertFrom, Object ... additionalInfo) {
		if (convertFrom == null) {
			return null;
		} else if (convertFrom instanceof SPObject) {
			SPObject wo = (SPObject) convertFrom;
			return spObjectConverter.convertToSimpleType(wo);
			
		} else if (convertFrom instanceof String) {
			return convertFrom;
			
		} else if (convertFrom instanceof Integer) {
			return convertFrom;
			
		} else if (convertFrom instanceof Double) {
			return convertFrom;
			
		} else if (convertFrom instanceof Boolean) {
			return convertFrom;
			
		} else if (convertFrom.getClass().isEnum()) {
			return new EnumConverter(convertFrom.getClass()).convertToSimpleType((Enum) convertFrom);
			
		} else {
			throw new IllegalArgumentException("Cannot convert " + convertFrom + " of type " + 
					convertFrom.getClass());
		}
		
	}
	
	public Object convertToComplexType(Object o, Class<? extends Object> type) {
		if (o == null) {
			return null;
			
		} else if (SPObject.class.isAssignableFrom(type)) {
			return spObjectConverter.convertToComplexType((String) o);
			
		} else if (String.class.isAssignableFrom(type)) {
			return (String) o;
			
		} else if (Integer.class.isAssignableFrom(type)) {
			return (Integer) o;
			
		} else if (Double.class.isAssignableFrom(type)) {
			return (Double) o;
			
		} else if (Boolean.class.isAssignableFrom(type)) {
			return (Boolean) o;
			
		} else {
			throw new IllegalArgumentException("Cannot convert " + o + " of type " + 
					o.getClass() + " to the type " + type);
		}
	}

}
