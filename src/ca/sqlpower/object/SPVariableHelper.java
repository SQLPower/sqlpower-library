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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

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
 * once it reaches the root. It will therefore iterate over all children,
 * starting at the root, until it finds a resolver for the given variable key.
 * 
 * <b>Be aware that this mode is very costly in computational times</b> yet
 * it might be required if the variable comes from a node that is not directly
 * in the path of the source node to the root of the tree.
 * 
 * <p>There is also a flag to make this helper aggregate all results in finds
 * in the tree. This means that even if it does find a resolver for a variable,
 * it will keep searching and add to the collections of resolved values for the 
 * variable.  It will search the whole tree for all 
 * {@link SPVariableResolver} instances and resolve with everything it finds.
 * 
 * <p>For example, if you have database queries which provide variables,
 * the helper will indirectly trigger the execution of each of those queries in order
 * to obtain the column names and thus decide if it can resolve a given variable.
 * One easy way to optimize the performance of such operations is to 
 * use namespaces. This will prevent effective resolution of matches if
 * the namespace is not supported by the encountered {@link SPVariableResolver}.
 * 
 * @see {@link SPVariableResolver}
 * @author Luc Boudreau
 */
public class SPVariableHelper implements SPVariableResolver {

	private static final Logger logger = Logger.getLogger(SPVariableHelper.class);
	
	/**
	 * Tells if this helper is supposed to walk back down the tree.
	 */
	private boolean walkDown = false;
	
	/**
	 * Tells if we want to search everywhere in the tree when we are
	 * resolving collections of variable values.
	 */
	private boolean globalCollectionResolve = false;
	
	/**
	 * This is the node onto which this helper is bonded. Searches will
	 * always start at this node, go up the tree to the root, then go down if
	 * {@link SPVariableHelper#walkDown} is true.
	 */
	private final SPObject contextSource;
	
	private class NotFoundException extends Exception {};
	private class CompletedException extends Exception {};
    
    /**
     * Substitutes any number of variable references in the given string, returning
     * the resultant string with all variable references replaced by the corresponding
     * variable values.
     * 
     * @param textWithVars
     * @param variableContext
     * @return
     */
    public static String substitute(String textWithVars, SPVariableHelper variableHelper) {
        Pattern p = Pattern.compile("\\$\\{([$a-zA-Z0-9"+NAMESPACE_DELIMITER_REGEXP+"\\-_.]+)\\}");
        
        logger.debug("Performing variable substitution on " + textWithVars);
        
        StringBuilder text = new StringBuilder();
        Matcher matcher = p.matcher(textWithVars);
        
        int currentIndex = 0;
        while (!matcher.hitEnd()) {
            if (matcher.find()) {
                String variableName = matcher.group(1);
                Object variableValue;
                if (variableName.equals("$")) {
                    variableValue = "$";
                } else {
                    variableValue = variableHelper.resolve(variableName, (Object) ("MISSING_VAR:" + variableName));
                }
                logger.debug("Found variable " + variableName + " = " + variableValue);
                text.append(textWithVars.substring(currentIndex, matcher.start()));
                text.append(variableValue);
                currentIndex = matcher.end();
            }  
        }
        
        text.append(textWithVars.substring(currentIndex));
        
        return text.toString();
    }
    
    /**
     * Returns the namespace of a variable. If there is
     * no variable namespace, null is returned.
     * @param key The key for which we want the namespace.
     * @return The namespace value, or null if none.
     */
    public static String getNamespace(String key) {
		int index = key.indexOf(NAMESPACE_DELIMITER);
		if (index != -1) {
			return key.substring(0, index);
		}
		return null;
	}
    
    
    /**
     * Returns the variable name without the namespace.
     * @param key
     * @return
     */
    public static String stripNamespace(String key) {
    	int index = key.indexOf(NAMESPACE_DELIMITER);
		if (index != -1) {
			return key.substring(index + NAMESPACE_DELIMITER.length(), key.length());
		}
		return key;
    }
    

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
		return this.recursiveNamespaceResolverFinder(contextSource, namespace, true);
	}
	
	/**
	 * Tells this helper if we should walk back down the tree in order
	 * to resolve variables once we reach the root.
	 */
	public void setWalkDown(boolean walkDown) {
		this.walkDown = walkDown;
	}
	
	/**
	 * Tells if we want to search everywhere in the tree when we are
	 * resolving collections of variable values.
	 * 
	 * <p>Setting this property to true makes means that when you call
	 * {@link SPVariableHelper#resolveCollection(String)} or 
	 * {@link SPVariableHelper#matches(String, String)}, even if it finds
	 * a resolver for the provided key, the search will continue and
	 * all resolvers on the tree will append to the returned results.
	 * Setting it to false (the default behavior) makes it stop and return
	 * the results as soon as one resolver has resolved the variable.
	 */
	public void setGlobalCollectionResolve(boolean globalCollectionResolve) {
		this.globalCollectionResolve = globalCollectionResolve;
	}
	
	// *************************  Resolver Implementation  *****************************//

	public Object resolve(String key) {
		return this.resolve(key, null);
	}

	public Object resolve(String key, Object defaultValue) {
		try {
			return 
				this.recursivelyResolveSingleValue(
					this.contextSource,
					getNamespace(key),
					key, 
					defaultValue,
					true);
		} catch (NotFoundException e) {
			return defaultValue;
		}
	}

	public Collection<Object> resolveCollection(String key) {
		return this.resolveCollection(key, null);
	}

	public Collection<Object> resolveCollection(String key, Object defaultValue) {
		Collection<Object> results = new HashSet<Object>();
		try {
			this.recursivelyResolveCollection(
					results,
					this.contextSource, 
					getNamespace(key),
					key, 
					defaultValue,
					true);
		} catch (CompletedException e) {
			return results;
		}
		if (results.size() == 0) {
			if (defaultValue == null) {
				return Collections.emptySet();
			} else {
				return Collections.singleton(defaultValue);			
			}
		} else {
			return results;
		}
	}

	public boolean resolves(String key) {
		return
			this.recursiveResolveCheck(
				this.contextSource, 
				getNamespace(key),
				key,
				true);
	}

	public boolean resolvesNamespace(String namespace) {
		return (this.recursiveNamespaceResolverFinder(contextSource, namespace, true) != null);
	}
	
	public Collection<Object> matches(String key, String partialValue) {
		Collection<Object> matches = new HashSet<Object>();
		try {
			this.recursiveMatch(
					matches, 
					this.contextSource, 
					getNamespace(key),
					key, 
					partialValue,
					true);
		} catch (CompletedException e) {
			// no op
		}
		return matches;
	}
	
	
	public Collection<String> keySet(String namespace) {
		Collection<String> keys = new HashSet<String>();
		this.recursiveKeySet(
				this.contextSource,
				keys, 
				namespace,
				true);
		return keys;
	}


// *******************  Private helper methods **********************************

	
	private Object recursivelyResolveSingleValue(
			SPObject currentNode, 
			String namespace,
			String key, 
			Object defaultValue,
			boolean upwards) throws NotFoundException 
	{
		
		// First, verify if the current node is a variable resolver implementation.
		if (currentNode instanceof SPVariableResolverProvider) {
			// Turns out it is. Let's ask it if it can help us.
			SPVariableResolver resolver = ((SPVariableResolverProvider)currentNode).getVariableResolver();
			
			if (resolver.resolvesNamespace(namespace) &&
					resolver.resolves(key)) {
				// Kewl. We found the correct variable value.
				return resolver.resolve(key, defaultValue);
			}
		}
		
		if (upwards) {
			// If we can't climb anymore and we still haven't found it, 
			// we return the default value.
			if (currentNode.getParent() == null)
			{
				if (this.walkDown) {
					// The current node still has a parent. Let's recursively
					// ask it to resolve the variable.
					return 
						this.recursivelyResolveSingleValue(
							currentNode, 
							namespace,
							key, 
							defaultValue,
							false);
				} else {
					throw new NotFoundException();
				}
			}
			
			// The current node still has a parent. Let's recursively
			// ask it to resolve the variable.
			return 
				this.recursivelyResolveSingleValue(
					currentNode.getParent(), 
					namespace,
					key, 
					defaultValue,
					true);
		} else {
			for (SPObject child : currentNode.getChildren()) {
				// We are going downwards. Let's iterate over children.
				Object result;
				try {
					result = this.recursivelyResolveSingleValue(
								child, 
								namespace,
								key, 
								defaultValue,
								false);
				} catch (NotFoundException e) {
					continue;
				}
				// Something was found. Return.
				return result;
			}
			// we iterated over all children without success. 
			// throw a nfe.
			throw new NotFoundException();
		}
	}
	
	private void recursivelyResolveCollection(
			Collection<Object> results,
			SPObject currentNode, 
			String namespace,
			String key, 
			Object defaultValue,
			boolean upwards) throws CompletedException 
	{
		// First, verify if the current node is a variable resolver implementation.
		if (currentNode instanceof SPVariableResolverProvider) {
			// Turns out it is. Let's ask it if it can help us.
			SPVariableResolver resolver = ((SPVariableResolverProvider)currentNode).getVariableResolver();
			if (resolver.resolvesNamespace(namespace) &&
					resolver.resolves(key)) {
				// Kewl. We found a valid variable resolver.
				results.addAll(resolver.resolveCollection(key, defaultValue));
				if (!globalCollectionResolve) {
					throw new CompletedException();
				}
			}
		}
		
		if (upwards) {
			// If we can't climb anymore and we still haven't found it...
			if (currentNode.getParent() == null) {
				if (this.walkDown) {
					this.recursivelyResolveCollection(
							results,
							currentNode, 
							namespace,
							key, 
							defaultValue,
							false);
				} else {
					return;
				}
			} else {
				this.recursivelyResolveCollection(
						results,
						currentNode.getParent(), 
						namespace,
						key, 
						defaultValue,
						true);
			}
		} else {
			for (SPObject child : currentNode.getChildren()) {
				// We are going downwards. Let's iterate over children.
				this.recursivelyResolveCollection(
						results,
						child, 
						namespace,
						key, 
						defaultValue,
						false);
			}
		}
	}
	
	private boolean recursiveResolveCheck(
			SPObject currentNode, 
			String namespace, 
			String key,
			boolean upwards) 
	{
		// First, verify if the current node is a variable resolver implementation.
		if (currentNode instanceof SPVariableResolverProvider) {
			// Turns out it is. Let's ask it if it can help us.
			SPVariableResolver resolver = ((SPVariableResolverProvider)currentNode).getVariableResolver();
			if (resolver.resolvesNamespace(namespace) && resolver.resolves(key)) {
				// Kewl. We found at least someone to resolve it.
				return true;
			}
		}
		
		if (upwards) {
			if (currentNode.getParent() == null) {
				if (walkDown) {
					return 
						this.recursiveResolveCheck(
							currentNode, 
							namespace,
							key,
							false);
				} else {
					return false;
				}
			} else {
				// The current node still has a parent. Let's recursively
				// ask it if it can resolve the variable
				return 
					this.recursiveResolveCheck(
						currentNode.getParent(), 
						namespace,
						key,
						true);
			}
		} else {
			for (SPObject child : currentNode.getChildren()) {
				// We are going downwards. Let's iterate over children.
				boolean result = this.recursiveResolveCheck(
									child, 
									namespace,
									key,
									false);
				if (result) {
					// we found a resolver. Break and return.
					return true;
				}
			}
			return false;
		}
	}
	
	
	private SPVariableResolver recursiveNamespaceResolverFinder(
			SPObject currentNode, 
			String namespace,
			boolean upwards)
	{
		// First, verify if the current node is a variable resolver implementation.
		if (currentNode instanceof SPVariableResolverProvider) {
			// Turns out it is. Let's check if it is the correct resolver
			// for the desired namespace
			SPVariableResolver resolver = ((SPVariableResolverProvider)currentNode).getVariableResolver();
			if (resolver.resolvesNamespace(namespace)) {
				return resolver;
			}
		}
		
		if (upwards) {
			// If we can't climb anymore and we still haven't found it...
			if (currentNode.getParent() == null) {
				if (walkDown) {
					return this.recursiveNamespaceResolverFinder(
							currentNode, 
							namespace,
							false);
				} else {
					return null;
				}
			} else {
				return 
					this.recursiveNamespaceResolverFinder(
						currentNode.getParent(), 
						namespace,
						true);
			}
		} else {
			for (SPObject child : currentNode.getChildren()) {
				// We are going downwards. Let's iterate over children.
				SPVariableResolver result = this.recursiveNamespaceResolverFinder(
													child, 
													namespace,
													false); 
				if (result!=null) {
					return result;
				}
			}
			return null;
		}
	}
	
	
	private void recursiveMatch(
			Collection<Object> matches, 
			SPObject currentNode, 
			String namespace, 
			String key, 
			String partialValue,
			boolean upwards) throws CompletedException
	{
		// First, verify if the current node is a variable resolver implementation.
		if (currentNode instanceof SPVariableResolverProvider) {
			// Turns out it is. Let's ask it if it can help us.
			SPVariableResolver resolver = ((SPVariableResolverProvider)currentNode).getVariableResolver();
			if (resolver.resolves(key)) {
				matches.addAll(
						resolver.matches(
								key, 
								partialValue));
				if (!globalCollectionResolve) {
					throw new CompletedException();
				}
			}
		}
		
		if (upwards) {
			// If we can't climb anymore and we still haven't found it
			if (currentNode.getParent() == null) {
				if (walkDown) {
					this.recursiveMatch(
						matches,
						currentNode, 
						namespace,
						key,
						partialValue,
						false);
				} else {
					throw new CompletedException();
				}
			} else {
				// The current node still has a parent. Let's recursively
				// ask it to resolve the variable.
				this.recursiveMatch(
					matches,
					currentNode.getParent(), 
					namespace,
					key,
					partialValue,
					true);
			}
		} else {
			for (SPObject child : currentNode.getChildren()) {
				// We are going downwards. Let's iterate over children.
				this.recursiveMatch(
						matches,
						child, 
						namespace,
						key,
						partialValue,
						false);
			}
		}
	}
	
	
	private void recursiveKeySet(
			SPObject currentNode, 
			Collection<String> keys,
			String namespace,
			boolean upwards) 
	{
		
		// First, verify if the current node is a variable resolver implementation.
		if (currentNode instanceof SPVariableResolverProvider) {
			// Turns out it is. Let's ask it if it can help us.
			SPVariableResolver resolver = ((SPVariableResolverProvider)currentNode).getVariableResolver();
			if (resolver.resolvesNamespace(namespace)) {
				keys.addAll(
					resolver.keySet(namespace));
			}
		}
		
		if (upwards) {
			// If we can't climb anymore and we still haven't found it, 
			// we return the default value.
			if (currentNode.getParent() == null) {
				if (walkDown) {
					this.recursiveKeySet(
							currentNode, 
							keys,
							namespace,
							false);
				} else {
					return;
				}
			} else {
				// The current node still has a parent. Let's recursively
				// ask it to resolve the variable.
				this.recursiveKeySet(
						currentNode.getParent(), 
						keys,
						namespace,
						true);
			}
		} else {
			for (SPObject child : currentNode.getChildren()) {
				// The current node still has a parent. Let's recursively
				// ask it to resolve the variable.
				this.recursiveKeySet(
						child, 
						keys,
						namespace,
						false);
			}
		}
	}
}
