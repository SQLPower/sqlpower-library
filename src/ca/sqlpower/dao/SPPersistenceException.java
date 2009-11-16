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

import ca.sqlpower.object.SPObject;

/**
 * A general exception for any exceptions that occur during an attempt to
 * persist a {@link SPObject} and/or its properties by any class that
 * implements {@link SPPersister}. This exception could be subclassed to
 * provide more detail for certain exceptions, such an exception caused by a
 * SPObject having a different old property value than expected.
 */
public class SPPersistenceException extends Exception {

	/**
	 * The UUID of the {@link SPObject} that was being persisted.
	 */
	private String uuid;

	/**
	 * Constructs a {@link SPPersistenceException} with the given UUID for
	 * the {@link SPObject} that was being persisted.
	 * 
	 * @param uuid
	 *            The UUID of the {@link SPObject} that was being persisted.
	 *            If there is no particular SPObject related to this
	 *            Exception, null may be passed in instead.
	 */
	public SPPersistenceException(String uuid) {
		this.uuid = uuid;
	}

	/**
	 * Constructs a {@link SPPersistenceException} with a given UUID and
	 * detailed message
	 * 
	 * @param uuid
	 *            The UUID of the {@link SPObject} that was being persisted.
	 *            If there is no particular SPObject related to this
	 *            Exception, null may be passed in instead.
	 * @param message
	 *            A detailed error message. Can be retrieved with
	 *            {@link #getMessage()}.
	 */
	public SPPersistenceException(String uuid, String message) {
		super(message);
		this.uuid = uuid;
	}

	/**
	 * Constructs a {@link SPPersistenceException} with a given UUID and a
	 * cause {@link Throwable}. This can be used to wrap a more specific
	 * Exception that caused this SPPersistenceException
	 * 
	 * @param uuid
	 *            The UUID of the {@link SPObject} that was being persisted.
	 *            If there is no particular SPObject related to this
	 *            Exception, null may be passed in instead.
	 * @param cause
	 *            A {@link Throwable} that is the specific cause
	 */
	public SPPersistenceException(String uuid, Throwable cause) {
		super(cause);
		this.uuid = uuid;
	}

	/**
	 * Constructs a {@link SPPersistenceException} with a given UUID, a
	 * detailed message, and a cause {@link Throwable}. This can be used to wrap
	 * a more specific Exception that caused this SPPersistenceException
	 * 
	 * @param uuid
	 *            The UUID of the {@link SPObject} that was being persisted.
	 *            If there is no particular SPObject related to this
	 *            Exception, null may be passed in instead.
	 * @param message
	 *            A detailed error message. Can be retrieved with
	 *            {@link #getMessage()}.
	 * @param cause
	 *            A {@link Throwable} that is the specific cause
	 */
	public SPPersistenceException(String uuid, String message, Throwable cause) {
		super(message, cause);
		this.uuid = uuid;
	}

	/**
	 * Returns the UUID of the {@link SPObject} that was being persisted when
	 * this Exception occured
	 * 
	 * @return The UUID of the {@link SPObject} that was being persisted
	 */
	public String getUUID() {
		return uuid;
	}

}
