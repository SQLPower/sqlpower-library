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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.collections.map.MultiValueMap;

@SuppressWarnings("unchecked")
public class SPSimpleVariableResolver implements SPVariableResolver {
	
	private final MultiValueMap variables = new MultiValueMap();
	private String namespace;
	
	public SPSimpleVariableResolver(String namespace) {
		this.namespace = namespace;
	}
	
	/**
	 * Stores a variable value. If a value with the same key already exists,
	 * a new value will be added.
	 * @param key The key to store the value under.
	 * @param value The value to store.
	 */
	public void store(String key, Object value) {
		this.variables.put(key, value);
	}
	
	/**
	 * Updates a variable value. This means that if a variable with the
	 * same key was already stored, it will be wiped and replaced by
	 * the new value.
	 * @param key The key to store the value under.
	 * @param value The value to store.
	 */
	public void update(String key, Object value) {
		if (this.variables.containsKey(key)) {
			this.variables.remove(key);
		}
		this.store(key, value);
	}
	
	/**
	 * Defines this variable resolver's namespace.
	 * @param namespace The namespace to use. Can be null.
	 */
	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	public Collection<Object> matches(String key, String partialValue) {
		String namespace = SPVariableHelper.getNamespace(key);
		if (this.resolvesNamespace(namespace)) {
			Set matches = new HashSet();
			Collection values  = this.resolveCollection(key);
			for (Object obj : values) {
				String stringRep = obj.toString();
				if (stringRep.startsWith(partialValue)) {
					matches.add(obj);
				}
			}
			return matches;
		}
		return Collections.emptySet();
	}

	public Object resolve(String key) {
		return this.resolve(key, null);
	}

	public Object resolve(String key, Object defaultValue) {
		String namespace = SPVariableHelper.getNamespace(key);
		if (this.resolvesNamespace(namespace)) {
			Collection value = this.variables.getCollection(SPVariableHelper.stripNamespace(key));
			if (value == null || value.size() == 0) {
				return defaultValue;
			} else {
				return value.iterator().next();
			}
		} else {
			return defaultValue;
		}
	}

	public Collection<Object> resolveCollection(String key) {
		return this.resolveCollection(key, null);
	}

	public Collection<Object> resolveCollection(String key, Object defaultValue) {
		String namespace = SPVariableHelper.getNamespace(key);
		if (this.resolvesNamespace(namespace)) {
			Collection value = this.variables.getCollection(SPVariableHelper.stripNamespace(key));
			if (value != null) {
				return value;
			}
		}
		return Collections.singleton(defaultValue);
	}

	public boolean resolves(String key) {
		String namespace = SPVariableHelper.getNamespace(key);
		if (this.resolvesNamespace(namespace)) {
			return this.variables.containsKey(SPVariableHelper.stripNamespace(key));
		}
		return false;
	}

	public boolean resolvesNamespace(String namespace) {
		if (namespace != null && this.namespace != null) {
			return this.namespace.equals(namespace);
		} else if (namespace != null && this.namespace == null) {
			return false;
		}
		return true;
	}

	public Collection<String> keySet(String namespace) {
		if (this.resolvesNamespace(namespace)) {
			if (this.namespace == null) {
				return this.variables.keySet();
			} else {
				Set keys = new HashSet();
				for (Object key : this.variables.keySet()) {
					keys.add(this.namespace.concat(NAMESPACE_DELIMITER).concat(key.toString()));
				}
				return keys;
			}
		}
		return Collections.emptySet();
	}
}
