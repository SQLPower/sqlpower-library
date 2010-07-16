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

import ca.sqlpower.dao.SPPersister;
import ca.sqlpower.object.SPObject;
import ca.sqlpower.object.annotation.ConstructorParameter.ParameterType;

/**
 * This class represents a parameter annotated with {@link ConstructorParameter}
 * in a constructor annotated with {@link Constructor}. This stores information
 * about whether the parameter refers to a property or not, and the value to use
 * as the constructor argument if persisting through an {@link SPPersister}.
 * 
 * @see ConstructorParameter 
 */
public class ConstructorParameterObject {
	
	/**
	 * @see #getProperty()
	 */
	private final ParameterType property;
	
	/**
	 * @see #getType()
	 */
	private final Class<?> type;
	
	/**
	 * @see #getName()
	 */
	private final String name;
	
	public ConstructorParameterObject(ParameterType property, Class<?> type, String name) {
		this.property = property;
		this.type = type;
		this.name = name;
	}
	
	/**
	 * Returns true if this constructor parameter refers to an {@link SPObject} property.
	 * 
	 * @see ConstructorParameter#parameterType()
	 */
	public ParameterType getProperty() {
		return property;
	}
	
	/**
	 * Returns the type of the constructor parameter.
	 */
	public Class<?> getType() {
		return type;
	}

	/**
	 * Returns the property name this constructor refers to if
	 * {@link #getProperty()} is true. Otherwise, it returns the name of the
	 * constructor parameter.
	 * 
	 * @see ConstructorParameter#propertyName()
	 */
	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return "name: " + name + ", property: " + property + ", type: " + type;
	}
}
