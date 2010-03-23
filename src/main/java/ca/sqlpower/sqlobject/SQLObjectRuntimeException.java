/*
 * Copyright (c) 2008, SQL Power Group Inc.
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
package ca.sqlpower.sqlobject;

/**
 * The SQLObjectRuntimeException is designed to wrap an
 * SQLObjectException in cases where a method which is not allowed to
 * throw checked exceptions must propogate an SQLObjectException.
 */
public class SQLObjectRuntimeException extends RuntimeException {

	/**
	 * Creates an unchecked exception wrapper for the given
	 * ArchitectException.
	 */
	public SQLObjectRuntimeException(SQLObjectException cause) {
	    super(cause);
	}
}
