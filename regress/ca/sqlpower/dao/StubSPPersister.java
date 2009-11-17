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

/**
 * Stubbed implementation of a SPPersister.
 */
public class StubSPPersister implements SPPersister {

	public void begin() throws SPPersistenceException {
		// no-op
	}

	public void commit() throws SPPersistenceException {
		// no-op
	}

	public void persistObject(String parentUUID, String type, String uuid,
			int index) throws SPPersistenceException {
		// no-op
	}

	public void persistProperty(String uuid, String propertyName,
			DataType propertyType, Object oldValue, Object newValue)
			throws SPPersistenceException {
		// no-op
	}

	public void persistProperty(String uuid, String propertyName,
			DataType propertyType, Object newValue)
			throws SPPersistenceException {
		// no-op
	}

	public void removeObject(String parentUUID, String uuid)
			throws SPPersistenceException {
		// no-op
	}

	public void rollback() {
		// no-op
	}

}
