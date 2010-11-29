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

import java.util.Map;

import org.apache.commons.beanutils.ConversionException;

import ca.sqlpower.object.SPObject;
import ca.sqlpower.util.SQLPowerUtils;

/**
 * Converts {@link SPObject}s to a unique string per SPObject and
 * converts these strings into {@link SPObject}s that are contained in
 * the root.
 */
public class SPObjectConverter implements BidirectionalConverter<String, SPObject> {
	
	/**
	 * This is the root object of the tree of {@link SPObject}s that
	 * will be searched for the given object by unique id.
	 */
	private final SPObject root;
	
	/**
	 * If this cache is not null it will be used to find objects by
	 * uuid before iterating over the tree of objects.
	 */
	private Map<String, SPObject> lookupCache = null;

	public SPObjectConverter(SPObject root) {
		this.root = root;
	}

	public SPObject convertToComplexType(String convertFrom)
			throws ConversionException {
		if (lookupCache != null && lookupCache.get(convertFrom) != null) return lookupCache.get(convertFrom);
		return SQLPowerUtils.findByUuid(root, convertFrom, SPObject.class); 
	}

	public String convertToSimpleType(SPObject convertFrom, Object ... additionalValues) {
		return convertFrom.getUUID();
	}
	
	public void setUUIDCache(Map<String, SPObject> lookupCache) {
		this.lookupCache = lookupCache;
	}
	
	public void removeUUIDCache() {
		this.lookupCache = null;
	}

}
