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

import ca.sqlpower.dao.SPPersister.DataType;

public final class PersistedPropertiesEntry {
	
	final Object rollbackValue;
	final String propertyName;
	final String uuid;
	
	public PersistedPropertiesEntry(
			String uuid, 
			String propertyName,
			DataType propertyType, 
			Object rollbackValue) {
				this.uuid = uuid;
				this.propertyName = propertyName;
				this.rollbackValue = rollbackValue;
	}
	
	public Object getRollbackValue() {
		return rollbackValue;
	}
	
	public String getPropertyName() {
		return propertyName;
	}
	
	public String getUUID() {
		return uuid;
	}
	
	@Override
	public String toString() {
		return "PersistedPropertiesEntry [uuid=" + uuid + ", propertyName="
				+ propertyName + ", rollbackValue=" + rollbackValue + "]";
	}
	
}
