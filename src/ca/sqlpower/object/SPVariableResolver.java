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

/**
 * Defines all methods to implement to make an object capable of resolving
 * variables into actual values.
 * 
 * <p>Variables keys are {@link String} values and might be prefixed by a 
 * namespace value. Typically, the namespace will be a specific object UUID.
 * You can ask the resolver directly if he is capable of resolving variables
 * in a given namespace by calling {@link SPVariableResolver#resolvesNamespace(String)}.
 * 
 * <p>For example, one might expect a lookup key to be something like:
 * 
 * <blockquote><code>w1234-1234-1234-1234:myVariableKey</code></blockquote>
 * 
 * <p>For performance reasons, it is strongly suggested of using those namespaces
 * and cache a map of namespaces<->resolvers. Large object models will benefit greatly
 * of this.
 * 
 * @author Luc Boudreau
 */
public interface SPVariableResolver {

	/**
	 * Resolves a variable to it's value by a String key.
	 * Returns null if the variable cannot be resolved.
	 * 
	 * <p>If more than one value is matched by the key, the actual returned
	 * value is not guaranteed to be neither the first match or anything
	 * alike. You should use {@link SPVariableResolver#resolveCollection(String)}
	 * if you expect there might be more than one possible returned values.
	 * 
	 * @param key The variable key to resolve
	 * @return The variable object, or null if it cannot be resolved.
	 */
	public Object resolve(String key);
	
	/**
	 * Resolves a variable to it's value by a String key.
	 * Returns the default supplied value if the variable cannot be resolved.
	 * 
	 * <p>If more than one value is matched by the key, the actual returned
	 * value is not guaranteed to be neither the first match or anything
	 * alike. You should use {@link SPVariableResolver#resolveCollection(String, Object)}
	 * if you expect there might be more than one possible returned values.
	 * 
	 * <p>A common mistake done by using this call is to receive the default
	 * value without verifying prior to the call if the implementing resolver
	 * can actually resolve it via the {@link SPVariableResolver#resolves(String)} 
	 * function.
	 * 
	 * @param key The variable key to resolve
	 * @param defaultValue The default value to return if the variable 
	 * cannot be resolved
	 * @return The variable object, or the default value passed as
	 * a parameter if it cannot be resolved.
	 */
	public Object resolve(String key, Object defaultValue);
	
	/**
	 * Resolves a variable to it's value by a String key.
	 * Returns an empty {@link Collection} if the variable cannot be resolved.
	 * 
	 * @param key The variable key to resolve
	 * @return The variable object, or an empty {@link Collection}
	 * if it cannot be resolved.
	 */
	public Collection<Object> resolveCollection(String key);
	
	/**
	 * Resolves a variable to it's value by a String key.
	 * Returns a {@link Collection} containing only the supplied default value
	 * if the variable cannot be resolved.
	 * 
	 * <p>A common mistake done by using this call is to receive the default
	 * value without verifying prior to the call if the implementing resolver
	 * can actually resolve it via the {@link SPVariableResolver#resolves(String)} 
	 * function.
	 * 
	 * @param key The variable key to resolve
	 * @return The variable object, or an empty {@link Collection}
	 * if it cannot be resolved.
	 */
	public Collection<Object> resolveCollection(String key, Object defaultValue);
	
	/**
	 * Verifies if this variable resolver can effectively resolve the
	 * supplied variable.
	 * @param key The key of the variable we would like to know if it 
	 * can be resolved.
	 * @return True or false.
	 */
	public boolean resolves(String key);
	
	/**
	 * Verifies if this variable resolver can effectively resolve 
	 * variables in the provided namespace value.
	 * @param namespace The namespace for which we want to know if this resolver
	 * is capable of resolving variables.
	 * @return True or false.
	 */
	public boolean resolvesNamespace(String namespace);
	
	/**
	 * For a given key and the first characters of a variable value,
	 * this method returns all matches. This is a auto-complete function.
	 * 
	 * For example, if the key "foo" and the values "bar" and "bar2" are 
	 * resolved by a given variable resolver, sending to this function
	 * "foo" as the key and "ba" as the partial match would return both
	 * variable values.
	 *  
	 * @param key The key of the variable we want all corresponding matches.
	 * @param partialValue The first characters of a variable value to match
	 * against all possible values.
	 * @return A collection of matching variable values.
	 */
	public Collection<Object> matches(String key, String partialValue);
}