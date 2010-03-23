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

package ca.sqlpower.object;

/** 
 * This is the usual interface that SPObject objects
 * implement. This avoids having to implement the complete
 * {@link SPVariableResolver} interface and instead use the
 * default implementation.
 * 
 * It tells other variable resolvers that this object is capable
 * of resolving variables and it can thus provide a resolver.
 * 
 * @see {@link SPVariableResolver}
 * @author Luc Boudreau
 */
public interface SPVariableResolverProvider {

	/**
	 * Returns this object's {@link SPVariableResolver}
	 * <p>Implementing classes are allowed to return
	 * a null object if they desire not to act as a variable
	 * provider no more.
	 * @return this object's {@link SPVariableResolver}
	 */
	public SPVariableResolver getVariableResolver();
	
}
