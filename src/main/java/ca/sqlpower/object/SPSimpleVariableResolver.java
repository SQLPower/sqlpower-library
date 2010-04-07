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
public class SPSimpleVariableResolver implements SPVariableResolver {
	
	protected final MultiValueMap variables = new MultiValueMap();
	private String namespace = null;
	private String userFriendlyName;
	private final SPObject owner;
	
	public SPSimpleVariableResolver(SPObject owner, String namespace, String userFriendlyName) {
		this(owner, namespace, userFriendlyName, true);
	}
	
	public SPSimpleVariableResolver(
			SPObject owner, 
			String namespace, 
			String userFriendlyName,
			boolean register) 
	{
		this.owner = owner;
		this.namespace = namespace;
		this.userFriendlyName = userFriendlyName;
		if (register) {
			SPResolverRegistry.register(owner, this);
		}
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
	 * Clears all currently stored variables.
	 */
	public void clear() {
		this.variables.clear();
	}
	
	/**
	 * Closes this resolver and removes it from the registry
	 */
	public void cleanup() {
		SPResolverRegistry.deregister(owner, this);
		this.clear();
	}
	
	public void store(String key, Object value) {
		if (this.namespace != null
				&& SPVariableHelper.getNamespace(key) != null
				&& !this.namespace.equals(SPVariableHelper.getNamespace(key))) {
			throw new IllegalArgumentException("Cannot store a namespaced variable of a different namespace than this resolver is configured with.");
		}
		this.variables.put(SPVariableHelper.getKey(key), value);
	}
	
	public void update(String key, Object value) {
		if (this.variables.containsKey(SPVariableHelper.getKey(key))) {
			this.variables.remove(SPVariableHelper.getKey(key));
		}
		this.variables.put(SPVariableHelper.getKey(key), value);
	}
	
	public void delete(String key) {
		this.variables.remove(SPVariableHelper.getKey(key));
	}

	public Collection<Object> matches(String key, String partialValue) {
		// Call the subclass hook.
		this.beforeLookups(key);
		
		String namespace = SPVariableHelper.getNamespace(key);
		if (this.resolvesNamespace(namespace)) {
			Set<Object> matches = new HashSet<Object>();
			Collection<Object> values  = this.resolveCollection(key);
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
			Collection<Object> value = this.variables.getCollection(SPVariableHelper.getKey(key));
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
			Collection<Object> value = this.variables.getCollection(SPVariableHelper.getKey(key));
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
		if (namespace == null) {
			return true;
		}
		return namespace.equals(this.namespace);
	}

	public Collection<String> keySet(String namespace) {
		// Call the subclass hook.
		this.beforeKeyLookup(namespace);
		
		if (this.resolvesNamespace(namespace)) {
			if (this.namespace == null) {
				return this.variables.keySet();
			} else {
				Set<String> keys = new HashSet<String>();
				for (Object key : this.variables.keySet()) {
					keys.add(this.namespace.concat(NAMESPACE_DELIMITER).concat(key.toString()));
				}
				return keys;
			}
		}
		return Collections.emptySet();
	}
	
	public String getNamespace() {
		return this.namespace;
	}
	
	public String getUserFriendlyName() {
		return this.userFriendlyName;
	}
	
	public void setUserFriendlyName(String userFriendlyName) {
		this.userFriendlyName = userFriendlyName;
	}
}
