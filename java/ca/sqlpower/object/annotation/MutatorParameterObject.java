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

/**
 * This class represents an extra parameter on a parameter annotated with
 * {@link MutatorParameter} in a setter method annotated with
 * {@link Mutator}. This stores information about the parameter type, name
 * and value to use by a session {@link SPPersister}.
 * 
 * @see MutatorParameter
 */
public class MutatorParameterObject {
	
	/**
	 * @see #getType()
	 */
	private final Class<?> type;
	
	/**
	 * @see #getName()
	 */
	private final String name;
	
	/**
	 * @see #getValue()
	 */
	private final String value;
	
	public MutatorParameterObject(
			Class<?> type,
			String name, 
			String value) {
		this.type = type;
		this.name = name;
		this.value = value;
	}
	
	/**
	 * Returns the type of the setter parameter.
	 */
	public Class<?> getType() {
		return type;
	}

	/**
	 * Returns the setter parameter name.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns the {@link String} representation of the primitive value to use
	 * in an {@link SPPersister} to call the setter this parameter belongs to.
	 * 
	 * @see MutatorParameter#value()
	 */
	public String getValue() {
		return value;
	}
}
