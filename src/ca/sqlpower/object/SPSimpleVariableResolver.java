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

/**
 * This is a default implementation of a variable resolver the {@link SPObject}
 * implementations can use. It implements the {@link SPVariableResolver} 
 * and you can use it to store variables.
 * 
 * <p>It is backed by a MultiMap object. Keep that in mind ;)
 * 
 * @see {@link SPVariableResolver}
 * @author Luc Boudreau
 *
 */
@SuppressWarnings("unchecked")
public class SPSimpleVariableResolver implements SPVariableResolver {
	
	protected final MultiValueMap variables = new MultiValueMap();
	private String namespace = null;
	private boolean snobbyResolver = true;
	
	public SPSimpleVariableResolver(String namespace) {
		this.namespace = namespace;
	}
	
	/**
	 * Stores a variable value. If a value with the same key already exists,
	 * a new value will be added.
	 * 
	 * If you try to store a variable with a different namespace than
	 * the one it is configured to answer to, you get an {@link IllegalStateException}
	 * 
	 * @param key The key to store the value under.
	 * @param value The value to store.
	 */
	public void store(String key, Object value) {
		if (SPVariableHelper.getNamespace(key)!=null && !this.resolvesNamespace(SPVariableHelper.getNamespace(key))) {
			throw new IllegalStateException("Cannot store a variable of namespace '" + SPVariableHelper.getNamespace(key) + "' because this resolver is configured to operate under the namespace '" + this.namespace + "'");
		}
		this.variables.put(SPVariableHelper.getKey(key), value);
	}
	
	/**
	 * Updates a variable value. This means that if a variable with the
	 * same key was already stored, it will be wiped and replaced by
	 * the new value.
	 * 
	 * If you try to store a variable with a different namespace than
	 * the one it is configured to answer to, you get an {@link IllegalStateException}
	 * 
	 * @param key The key to store the value under.
	 * @param value The value to store.
	 */
	public void update(String key, Object value) {
		if (!this.resolvesNamespace(SPVariableHelper.getNamespace(key))) {
			throw new IllegalStateException("Cannot store a variable of namespace '" + SPVariableHelper.getNamespace(key) + "' because this resolver is configured to operate under the namespace '" + this.namespace + "'");
		}
		if (this.variables.containsKey(SPVariableHelper.getKey(key))) {
			this.variables.remove(SPVariableHelper.getKey(key));
		}
		this.store(key, value);
	}
	
	/**
	 * Removes all values associated with the specified key. 
	 * @param key The key for which we want to delete all occurences.
	 */
	public void delete(String key) {
		this.variables.remove(SPVariableHelper.getKey(key));
	}
	
	/**
	 * This is a hook method that sub classes can override. It gets called
	 * before all lookup operations (resolve, match and resolves).
	 * @param key The key we were asked to lookup info on.
	 */
	protected void beforeLookups(String key) {
		// Nothing to do here. Only sub-classes implement this.
	}
	
	/**
	 * This is a hook method that sub classes can override. It gets called
	 * before the keySet operation.
	 * @param namespace The namespace we were asked to lookup info on.
	 */
	protected void beforeKeyLookup(String namespace) {
		// Nothing to do here. Only sub-classes implement this.
	}
	
	/**
	 * Defines this variable resolver's namespace.
	 * @param namespace The namespace to use. Can be null.
	 */
	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}
	
	/**
	 * Turns this resolver in a snobby one. This affects the namespace 
	 * resolution decisions. When true, the resolver will ignore all requests
	 * for variables whose key is not explicitely prefixed with the proper
	 * namespace.
	 * @param snobbyResolver Wether or not to be snobby.
	 */
	public void setSnobbyResolver(boolean snobbyResolver) {
		this.snobbyResolver = snobbyResolver;
	}
	
	/**
	 * This tells if this resolver is in snobby mode or not.
	 * When true, the resolver will ignore all requests
	 * for variables whose key is not explicitely prefixed with the proper
	 * namespace.
	 * @return True if snobby. False otherwise.
	 */
	public boolean isSnobbyResolver() {
		return snobbyResolver;
	}

	public Collection<Object> matches(String key, String partialValue) {
		// Call the subclass hook.
		this.beforeLookups(key);
		
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
		// Call the subclass hook.
		this.beforeLookups(key);
		
		String namespace = SPVariableHelper.getNamespace(key);
		if (this.resolvesNamespace(namespace)) {
			Collection value = this.variables.getCollection(SPVariableHelper.getKey(key));
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
		// Call the subclass hook.
		this.beforeLookups(key);
		
		String namespace = SPVariableHelper.getNamespace(key);
		if (this.resolvesNamespace(namespace)) {
			Collection value = this.variables.getCollection(SPVariableHelper.getKey(key));
			if (value != null) {
				return value;
			}
		}
		return Collections.singleton(defaultValue);
	}

	public boolean resolves(String key) {
		// Call the subclass hook.
		this.beforeLookups(key);
		
		String namespace = SPVariableHelper.getNamespace(key);
		if (this.resolvesNamespace(namespace)) {
			return this.variables.containsKey(SPVariableHelper.getKey(key));
		}
		return false;
	}

	public boolean resolvesNamespace(String namespace) {
		if (namespace != null && this.namespace != null) {
			return this.namespace.equals(namespace);
		} else if (namespace != null && this.namespace == null) {
			if (this.snobbyResolver) {
				return false;
			} else {
				return true;
			}
		} else if (namespace == null && this.namespace != null && this.snobbyResolver) {
			return false;
		}
		return true;
	}

	public Collection<String> keySet(String namespace) {
		// Call the subclass hook.
		this.beforeKeyLookup(namespace);
		
		if (namespace == null &&
				this.resolvesNamespace(namespace)) {
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
