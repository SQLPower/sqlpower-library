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

public class SPVariableHelper implements SPVariableResolver {

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
	 * @param contextSource
	 */
	public SPVariableHelper(SPObject contextSource) {
		this.contextSource = contextSource;
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
	
	
	
	
	
	
	// *************************  Resolver Implementation  *****************************//

	public Object resolve(String key) {
		return 
			this.recursivelyResolveSingleValue(
				this.contextSource, 
				key, 
				null);
	}

	public Object resolve(String key, Object defaultValue) {
		return 
			this.recursivelyResolveSingleValue(
				this.contextSource, 
				key, 
				defaultValue);
	}

	public Collection<Object> resolveCollection(String key) {
		return 
			this.recursivelyResolveCollection(
				this.contextSource, 
				key, 
				null);
	}

	public Collection<Object> resolveCollection(String key, Object defaultValue) {
		return 
			this.recursivelyResolveCollection(
				this.contextSource, 
				key, 
				defaultValue);
	}

	public boolean resolves(String key) {
		return
			this.recursiveResolveCheck(
				this.contextSource, 
				key);
	}

	public boolean resolvesNamespace(String namespace) {
		return (this.recursiveNamespaceResolverFinder(contextSource, namespace) != null);
	}
	
	public Collection<Object> matches(String key) {
		Collection<Object> matches = new HashSet<Object>();
		this.recursiveMatch(matches, this.contextSource, key);
		return matches;
	}
	
	
	
// *******************  Private helper methods **********************************

	
	private Object recursivelyResolveSingleValue(
				SPObject currentNode, 
				String key, 
				Object defaultValue) {
		
		// First, verify if the current node is a variable resolver implementation.
		if (currentNode instanceof SPVariableResolver) {
			// Turns out it is. Let's ask it if it can help us.
			SPVariableResolver resolver = (SPVariableResolver) currentNode;
			if (resolver.resolves(key)) {
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
			this.recursivelyResolveSingleValue(
				currentNode.getParent(), 
				key, 
				defaultValue);
	}
	
	private Collection<Object> recursivelyResolveCollection(
			SPObject currentNode, 
			String key, 
			Object defaultValue) {
	
		// First, verify if the current node is a variable resolver implementation.
		if (currentNode instanceof SPVariableResolver) {
			// Turns out it is. Let's ask it if it can help us.
			SPVariableResolver resolver = (SPVariableResolver) currentNode;
			if (resolver.resolves(key)) {
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
			this.recursivelyResolveCollection(
				currentNode.getParent(), 
				key, 
				defaultValue);
	}
	
	private boolean recursiveResolveCheck(SPObject currentNode, String key) {
		// First, verify if the current node is a variable resolver implementation.
		if (currentNode instanceof SPVariableResolver) {
			// Turns out it is. Let's ask it if it can help us.
			SPVariableResolver resolver = (SPVariableResolver) currentNode;
			if (resolver.resolves(key)) {
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
			this.recursiveResolveCheck(
				currentNode.getParent(), 
				key);
	}
	
	
	private SPVariableResolver recursiveNamespaceResolverFinder(SPObject currentNode, String namespace) {
		// First, verify if the current node is a variable resolver implementation.
		if (currentNode instanceof SPVariableResolver) {
			// Turns out it is. Let's check if it is the correct resolver
			// for the desired namespace
			SPVariableResolver resolver = (SPVariableResolver) currentNode;
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
			this.recursiveNamespaceResolverFinder(
				currentNode.getParent(), 
				namespace);
	}
	
	
	private void recursiveMatch(Collection<Object> matches, SPObject currentNode, String key) {
		// First, verify if the current node is a variable resolver implementation.
		if (currentNode instanceof SPVariableResolver) {
			// Turns out it is. Let's ask it if it can help us.
			SPVariableResolver resolver = (SPVariableResolver) currentNode;
			matches.addAll(resolver.matches(key));
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
			key);
	}
	
	
}
