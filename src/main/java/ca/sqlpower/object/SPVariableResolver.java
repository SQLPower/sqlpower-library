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
 * <p>For example, one might expect an inserted variable to be something like:
 * 
 * <blockquote><code>${w1234-1234-1234-1234::myVariableKey}</code></blockquote>
 * 
 * <p>For performance reasons, it is strongly suggested of using those namespaces
 * and cache a map of namespaces<->resolvers. Large object models will benefit greatly
 * of this.
 *
 * <p>An inserted variable can also specify it's default value. This can be accomplished 
 * by inserting a variable as so.
 * 
 * <blockquote><code>${w1234-1234-1234-1234::myVariableKey->defValue}</code></blockquote>
 * 
 * <p>In the above example, the default value is a string which contents is 'defValue'.
 * 
 * <p>The usual way of using the Variables framework goes like this. {@link SPObject} that want
 * to expose variables must implement the {@link SPVariableResolverProvider} interface.
 * They then instanciate a variable resolver. One default implementation is available as 
 * {@link SPSimpleVariableResolver}. 
 * 
 * <p>The {@link SPSimpleVariableResolver} has a dual role. It is able to store and
 * update variables in a MultiMap. Multiple values can therefore be stored under a 
 * same variable key. It also implements the {@link SPVariableResolver} interface,
 * which allows it to share variable values with it's fellow objects.
 * 
 * <p>In order to search through the tree and resolve variables, one instanciates
 * a {@link SPVariableHelper} object and uses a specific node of the tree as a 
 * constructor argument. This node will serve as a starting point to resolve
 * variables. It will walk the tree and search for implementation of 
 * {@link SPVariableResolverProvider}. There are lots of options available to configure
 * the helper's behavior and optimize it's search routine. Read it's javadoc for more details.
 * 
 * @see {@link SPVariableHelper}
 * @author Luc Boudreau
 */
public interface SPVariableResolver {
	
	/**
	 * The delimiter to use for namespaced variable names.
	 */
	public static final String NAMESPACE_DELIMITER = "::";

	/**
	 * Used to define an inserted variable default value if it cannot be resolved.
	 * @see {@link SPVariableResolver}
	 */
	public static final String DEFAULT_VALUE_DELIMITER = "->";
	
	/**
	 * Stores a variable value. If a value with the same key already exists,
	 * a new value will be added.
	 * 
	 * @param key The key to store the value under.
	 * @param value The value to store.
	 */
	public void store(String key, Object value);
	
	/**
	 * Updates a variable value. This means that if a variable with the
	 * same key was already stored, it will be wiped and replaced by
	 * the new value.
	 * 
	 * @param key The key to store the value under.
	 * @param value The value to store.
	 */
	public void update(String key, Object value);	
	
	/**
	 * Removes all values associated with the specified key. 
	 * @param key The key for which we want to delete all occurences.
	 */
	public void delete(String key);
	
	
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
	 * Resolves a variable to it's value by a String key. Returns a
	 * {@link Collection} containing only the supplied default value if the
	 * variable cannot be resolved.
	 * 
	 * <p>
	 * A common mistake done by using this call is to receive the default value
	 * without verifying prior to the call if the implementing resolver can
	 * actually resolve it via the {@link SPVariableResolver#resolves(String)}
	 * function.
	 * 
	 * @param key
	 *            The variable key to resolve
	 * @return The variable object, or a {@link Collection} containing only the
	 *         supplied default value if the variable cannot be resolved.
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
	 * 
	 * <p>The rules are as follows.
	 * 
	 * <ul>
	 * <li>If this resolver's namespace is null and the namespace passed as
	 * a parameter is null, return true.
	 * <li>If this resolver's namespace is null but we pass a namespace as
	 * a parameter, return false.
	 * <li>If this resolver's namespace is defined and we pass a namespace as a parameter,
	 * we return true IF both namespaces match.
	 * <li>If this resolver's namespace is defined but we pass a null namespace
	 * as a parameter, we return true;
	 * </li>
	 * 
	 * <p>Some resolvers don't follow these rules. For example, the builtin
	 * {@link SPSimpleVariableResolver} by default operates in snobby mode.
	 * This means that if you give it a namespace and ask him for non-namespaced
	 * variables, he won't resolve them.
	 * 
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
	
	/**
	 * This function is used to get all available variable names
	 * for a given namespace. Passing a null value as a namespace
	 * means that we want to ignore namespaces and we want a complete
	 * list of available namespaces/keys.
	 * 
	 * @param namespace Either a namespace or null.
	 * @return A collection of available variable names.
	 */
	public Collection<String> keySet(String namespace);
	
	/**
	 * returns the namespace under which this resolver is registered.
	 * @return The namespace of this resolver implementation.
	 */
	public String getNamespace();
	
	/**
	 * Returns the name of this resolver as to be exposed to
	 * the end user.
	 * @return A user friendly name for this resolver.
	 */
	public String getUserFriendlyName();
}