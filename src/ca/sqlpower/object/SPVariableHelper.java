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

/**
 * This is a helper class for resolving variables. It is a delegating
 * implementation of {@link SPVariableResolver} that walks up the {@link SPObject}
 * tree and tries the best it can to find a resolver for the provided 
 * variable key.
 * 
 * <p>Typically, a class that needs such an object has to instanciate it
 * and pass a node to the constructor. This node will serve as a starting
 * point for variable resolution. The helper will walk up the tree and
 * ask all the {@link SPVariableResolver} it finds on it's way if they
 * can resolve the variable.
 * 
 * <p>There is an option available to make this helper walk down the tree
 * once it reaches the root. It will search the whole tree for all 
 * {@link SPVariableResolver} instances and resolve with everything it finds.
 * <b>Be aware that this mode is very costly in computational times</b> yet
 * it might be required if the variable comes from a node that is not directly
 * in the path of the source node to the root of the tree.
 * 
 * <p>For example, if you have database queries which provide variables,
 * the helper will indirectly trigger the execution of each of those queries in order
 * to obtain the column names and thus decide if it can resolve a given variable.
 * One easy way to optimize the performance of such operations is to 
 * use namespaces. This will prevent effective resolution of matches if
 * the namespace is not supported by the encountered {@link SPVariableResolver}.
 * 
 * @author Luc Boudreau
 */
public class SPVariableHelper implements SPVariableResolver {

	/**
	 * Tells if this helper is supposed to walk back down the tree.
	 */
	private boolean walkDown = false;
	
	private final SPObject contextSource;

	/**
	 * This constructor is not usable. Please use 
	 * {@link SPVariableHelper#SPVariableHelper(SPObject)}
	 */
	@SuppressWarnings("unused")
	private SPVariableHelper() {
		this.contextSource = null;
	}
	
	/**
	 * Builds a variable helper to help resolve variables as values.
	 * @param contextSource The source node from which to start
	 * resolving variables.
	 */
	public SPVariableHelper(SPObject contextSource) {
		this(contextSource, false);
	}
	
	/**
	 * Builds a variable helper to help resolve variables as values.
	 * @param contextSource
	 */
	public SPVariableHelper(SPObject contextSource, boolean walkBackDown) {
		this.contextSource = contextSource;
		this.walkDown = walkBackDown;
	}
	
	/**
	 * Searches and returns the first resolver for a given namespace
	 * it can find in the tree, starting by climbing it and then searching
	 * everywhere.
	 * If none can be found, NULL is returned.
	 * @param namespace The namespace for which we want the resolver.
	 * @return Either a proper resolver for the given namespace or null
	 * if none can be found.
	 */
	public SPVariableResolver getResolverForNamespace(String namespace) {
		return this.getResolverForNamespace(namespace);
	}
	
	/**
	 * Tells this helper if we should walk back down the tree in order
	 * to resolve variables once we reach the root.
	 */
	public void setWalkDown(boolean walkDown) {
		this.walkDown = walkDown;
	}
	
	
	
	// *************************  Resolver Implementation  *****************************//

	public Object resolve(String key) {
		return 
			this.upwardsRecursivelyResolveSingleValue(
				this.contextSource,
				getNamespace(key),
				key, 
				null);
	}

	public Object resolve(String key, Object defaultValue) {
		return 
			this.upwardsRecursivelyResolveSingleValue(
				this.contextSource,
				getNamespace(key),
				key, 
				defaultValue);
	}

	public Collection<Object> resolveCollection(String key) {
		return 
			this.upwardsRecursivelyResolveCollection(
				this.contextSource,
				getNamespace(key),
				key, 
				null);
	}

	public Collection<Object> resolveCollection(String key, Object defaultValue) {
		return 
			this.upwardsRecursivelyResolveCollection(
				this.contextSource, 
				getNamespace(key),
				key, 
				defaultValue);
	}

	public boolean resolves(String key) {
		return
			this.upwardsRecursiveResolveCheck(
				this.contextSource, 
				getNamespace(key),
				key);
	}

	public boolean resolvesNamespace(String namespace) {
		return (this.upwardsRecursiveNamespaceResolverFinder(contextSource, namespace) != null);
	}
	
	public Collection<Object> matches(String key, String partialValue) {
		Collection<Object> matches = new HashSet<Object>();
		this.recursiveMatch(
				matches, 
				this.contextSource, 
				getNamespace(key),
				key, 
				partialValue);
		return matches;
	}
	
	
	
// *******************  Private helper methods **********************************

	
	public static String getNamespace(String key) {
		int index = key.indexOf(":");
		if (index != -1) {
			return key.substring(0, index);
		}
		return null;
	}
	
	
	private Object upwardsRecursivelyResolveSingleValue(
				SPObject currentNode, 
				String namespace,
				String key, 
				Object defaultValue) {
		
		// First, verify if the current node is a variable resolver implementation.
		if (currentNode instanceof SPVariableResolverProvider) {
			// Turns out it is. Let's ask it if it can help us.
			SPVariableResolver resolver = ((SPVariableResolverProvider)currentNode).getVariableResolver();
			
			if ((namespace == null || (namespace != null && resolver.resolvesNamespace(namespace))) &&
					resolver.resolves(key)) {
				// Kewl. We found the correct variable value.
				return resolver.resolve(key, defaultValue);
			}
		}
		
		// If we can't climb anymore and we still haven't found it, 
		// we return the default value.
		if (currentNode.getParent() == null)
			return defaultValue;
			
		// The current node still has a parent. Let's recursively
		// ask it to resolve the variable.
		return 
			this.upwardsRecursivelyResolveSingleValue(
				currentNode.getParent(), 
				namespace,
				key, 
				defaultValue);
	}
	
	private Collection<Object> upwardsRecursivelyResolveCollection(
			SPObject currentNode, 
			String namespace,
			String key, 
			Object defaultValue) {
	
		// First, verify if the current node is a variable resolver implementation.
		if (currentNode instanceof SPVariableResolverProvider) {
			// Turns out it is. Let's ask it if it can help us.
			SPVariableResolver resolver = ((SPVariableResolverProvider)currentNode).getVariableResolver();
			if ((namespace == null || (namespace != null && resolver.resolvesNamespace(namespace))) &&
					resolver.resolves(key)) {
				// Kewl. We found the correct variable value.
				return resolver.resolveCollection(key, defaultValue);
			}
		}
		
		// If we can't climb anymore and we still haven't found it, 
		// we return the default value.
		if (currentNode.getParent() == null)
			return Collections.singleton(defaultValue);
			
		// The current node still has a parent. Let's recursively
		// ask it to resolve the variable.
		return 
			this.upwardsRecursivelyResolveCollection(
				currentNode.getParent(), 
				namespace,
				key, 
				defaultValue);
	}
	
	private boolean upwardsRecursiveResolveCheck(
			SPObject currentNode, 
			String namespace, 
			String key) {
		// First, verify if the current node is a variable resolver implementation.
		if (currentNode instanceof SPVariableResolverProvider) {
			// Turns out it is. Let's ask it if it can help us.
			SPVariableResolver resolver = ((SPVariableResolverProvider)currentNode).getVariableResolver();
			if ((namespace == null || (namespace != null && resolver.resolvesNamespace(namespace))) &&
					resolver.resolves(key)) {
				// Kewl. We found the correct variable value.
				return resolver.resolves(key);
			}
		}
		
		// If we can't climb anymore and we still haven't found it, 
		// we return false.
		if (currentNode.getParent() == null)
			return false;
			
		// The current node still has a parent. Let's recursively
		// ask it if it can resolve the variable
		return 
			this.upwardsRecursiveResolveCheck(
				currentNode.getParent(), 
				namespace,
				key);
	}
	
	
	private SPVariableResolver upwardsRecursiveNamespaceResolverFinder(
			SPObject currentNode, 
			String namespace) {
		// First, verify if the current node is a variable resolver implementation.
		if (currentNode instanceof SPVariableResolverProvider) {
			// Turns out it is. Let's check if it is the correct resolver
			// for the desired namespace
			SPVariableResolver resolver = ((SPVariableResolverProvider)currentNode).getVariableResolver();
			if (resolver.resolvesNamespace(namespace)) {
				return resolver;
			}
		}
		
		// If we can't climb anymore and we still haven't found it, 
		// we return false.
		if (currentNode.getParent() == null)
			return null;
			
		// The current node still has a parent. Let's recursively
		// ask it to resolve the variable.
		return 
			this.upwardsRecursiveNamespaceResolverFinder(
				currentNode.getParent(), 
				namespace);
	}
	
	
	private void recursiveMatch(
			Collection<Object> matches, 
			SPObject currentNode, 
			String namespace, 
			String key, 
			String partialValue) {
		// First, verify if the current node is a variable resolver implementation.
		if (currentNode instanceof SPVariableResolverProvider) {
			// Turns out it is. Let's ask it if it can help us.
			SPVariableResolver resolver = ((SPVariableResolverProvider)currentNode).getVariableResolver();
			if (namespace == null || (namespace != null && resolver.resolvesNamespace(namespace)))
				matches.addAll(
					resolver.matches(
						key, 
						partialValue));
		}
		
		// If we can't climb anymore and we still haven't found it, 
		// we return the default value.
		if (currentNode.getParent() == null)
			return;
			
		// The current node still has a parent. Let's recursively
		// ask it to resolve the variable.
		this.recursiveMatch(
			matches,
			currentNode.getParent(), 
			namespace,
			key,
			partialValue);
	}
	
	
}
