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

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JPopupMenu;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;

import org.apache.commons.collections.map.MultiValueMap;
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
	
	private final static Pattern varPattern = Pattern.compile("\\$\\{([$a-zA-Z0-9"+NAMESPACE_DELIMITER_REGEXP+"\\-_.\\>]+)\\}");

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
    
	
	public String substitute(String textWithVars) {
		return SPVariableHelper.substitute(textWithVars, this);
	}
	
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
        
        logger.debug("Performing variable substitution on " + textWithVars);
        
        StringBuilder text = new StringBuilder();
        Matcher matcher = varPattern.matcher(textWithVars);
        
        int currentIndex = 0;
        while (!matcher.hitEnd()) {
            if (matcher.find()) {
                String variableName = matcher.group(1);
                Object variableValue;
                if (variableName.equals("$")) {
                    variableValue = "$";
                } else {
                    variableValue = variableHelper.resolve(variableName);
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
     * Helper method that takes a connection and a SQL statement which includes variable and 
     * converts all that in a nifty prepared statement ready for execution, on time for christmas.
     * @param connection A connection object to use in order to generate the prepared statement.
     * @param sql A SQL string which might include variables.
     * @return A {@link PreparedStatement} object ready for execution.
     * @throws SQLException Might get thrown if we cannot generate a {@link PreparedStatement} with the supplied connection.
     */
    public PreparedStatement substituteForDb(Connection connection, String sql) throws SQLException {
    	return SPVariableHelper.substituteForDb(connection, sql, this);
    }
    
    /**
     * Helper method that takes a connection and a SQL statement which includes variable and 
     * converts all that in a nifty prepared statement ready for execution, on time for christmas.
     * @param connection A connection object to use in order to generate the prepared statement.
     * @param sql A SQL string which might include variables.
     * @param variableHelper A {@link SPVariableHelper} object to resolve the variables.
     * @return A {@link PreparedStatement} object ready for execution.
     * @throws SQLException Might get thrown if we cannot generate a {@link PreparedStatement} with the supplied connection.
     */
    public static PreparedStatement substituteForDb(Connection connection, String sql, SPVariableHelper variableHelper) throws SQLException {
    	
        StringBuilder text = new StringBuilder();
        Matcher matcher = varPattern.matcher(sql);
        List<Object> vars = new LinkedList<Object>();
        
        // First, change all vars to '?' markers.
        int currentIndex = 0;
        while (!matcher.hitEnd()) {
            if (matcher.find()) {
                String variableName = matcher.group(1);
                if (variableName.equals("$")) {
                	vars.add("$");
                } else {
                	vars.add(variableHelper.resolve(variableName));
                }
                text.append(sql.substring(currentIndex, matcher.start()));
                text.append("?");
                currentIndex = matcher.end();
            }  
        }
        text.append(sql.substring(currentIndex));
        
        // Now generate a prepared statement and inject it's variables.
        PreparedStatement ps = connection.prepareStatement(text.toString());
        for (int i = 0; i < vars.size(); i++) {
    		ps.setObject(i+1, vars.get(i));
        }
        
        return ps;
    }
    
    
    /**
     * Searches for all available variables in a given namespace then builds a JMenu
     * with configured actions so a user can pick a variable from a list and insert it
     * in a target {@link JTextComponent}.
     * @param variableNamespace The namespace in which to search. In accordance with the
     * {@link SPVariableResolver} interface, passing a null namespace value means that
     * we search through all namespaces.
     * @param invoker The {@link JComponent} that invokes the menu. This is used for positioning.
     * @param target The target into which to place the variable name.
     */
    public void promptAndInsertVariable(
    		String variableNamespace, 
    		Component invoker, 
    		JTextComponent target)
    {
    	SPVariableHelper.promptAndInsertVariable(this, variableNamespace, invoker, target);
    }
    
    /**
     * Searches for all available variables in a given namespace then builds a JMenu
     * with configured actions so a user can pick a variable from a list and insert it
     * in a target {@link JTextComponent}.
     * @param variableHelper The variable helper to use as a resolver.
     * @param variableNamespace The namespace in which to search. In accordance with the
     * {@link SPVariableResolver} interface, passing a null namespace value means that
     * we search through all namespaces.
     * @param invoker The {@link JComponent} that invokes the menu. This is used for positioning.
     * @param target The target into which to place the variable name.
     */
    public static void promptAndInsertVariable(
    		SPVariableHelper variableHelper, 
    		String variableNamespace, 
    		Component invoker, 
    		JTextComponent target) 
    {
    	// Step 1. Get the keys and object names.
    	MultiValueMap keys = new MultiValueMap();
		variableHelper.recursiveKeySet(
				variableHelper.contextSource,
				keys, 
				variableNamespace,
				true);
		
		// Sort the names.
		List<String> sortedNames = new ArrayList<String>(keys.keySet().size());
		sortedNames.addAll(keys.keySet());
		Collections.sort(sortedNames, new Comparator<String>() {
			public int compare(String o1, String o2) {
				return o1.compareTo(o2);
			};
		});
		
		// Now build the menu
    	JPopupMenu menu = new JPopupMenu();
        for (String name : sortedNames) {
        	JMenu subMenu = new JMenu(name.toString());
    		menu.add(subMenu);
    		for (Object key : keys.getCollection(name)) {
    			subMenu.add(new InsertVariableAction(getKey(key.toString()), key.toString(), target));
    		}
        }
    	
        // All done. Show the menu.
        menu.show(invoker, 0, invoker.getHeight());
    }
    
    
    
    /**
     * Returns the namespace of a variable. If there is
     * no variable namespace, null is returned.
     * @param varDef The complete variable key lookup value. Something like : '1234-1234::myVar->defValue'
     * @return The namespace value, '1234-1234' in the above example, or null if none.
     */
    public static String getNamespace(String varDef) {
		int index = varDef.indexOf(NAMESPACE_DELIMITER);
		if (index != -1) {
			return varDef.substring(0, index);
		}
		return null;
	}
    
    
    /**
     * Returns the variable name without the namespace nor
     * the default value.
     * @param varDef The complete variable key lookup value. Something like : '1234-1234::myVar->defValue'
     * @return Only the key part, 'myVar' in the above example.
     */
    public static String getKey(String varDef) {
    	
    	int namespaceIndex = varDef.indexOf(NAMESPACE_DELIMITER);
    	int defValueIndex = varDef.indexOf(DEFAULT_VALUE_DELIMITER);
    	String returnValue = varDef;
    	
		if (namespaceIndex != -1) {
			returnValue = returnValue.substring(namespaceIndex + NAMESPACE_DELIMITER.length(), varDef.length());
		}
		
		if (defValueIndex != -1) {
			returnValue = returnValue.substring(0, defValueIndex);
		}
		
		return returnValue;
    }
    
    /**
     * Extracts the default value from an inserted variable key.
     * @param varDef The complete variable key lookup value. Something like : '1234-1234::myVar->defValue'
     * @return Only the default value part. 'defValue' in the above example.
     */
    public static String getDefaultValue(String varDef) {
    	int defValueIndex = varDef.indexOf(DEFAULT_VALUE_DELIMITER);
    	if (defValueIndex != -1) {
    		return varDef.substring(defValueIndex + DEFAULT_VALUE_DELIMITER.length(), varDef.length());
    	} else {
    		return null;
    	}
    }

    /**
     * Returns an inserted variable lookup key stripped from it's default
     * value part.
     * @param varDef The complete variable key lookup value. Something like : '1234-1234::myVar->defValue'
     * @return Would return '1234-1234::myVar' in the above example
     */
    public static String stripDefaultValue(String varDef) {
    	int defValueIndex = varDef.indexOf(DEFAULT_VALUE_DELIMITER);
    	if (defValueIndex != -1) {
    		return varDef.substring(0, defValueIndex);
    	} else {
    		return varDef;
    	}
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
		return this.resolve(stripDefaultValue(key), getDefaultValue(key));
	}

	public Object resolve(String key, Object defaultValue) {
		try {
			return
				this.recursivelyResolveSingleValue(
					this.contextSource,
					getNamespace(key),
					getKey(key),
					true);
		} catch (NotFoundException e) {
			return defaultValue;
		}
	}

	public Collection<Object> resolveCollection(String key) {
		return this.resolveCollection(stripDefaultValue(key), getDefaultValue(key));
	}

	public Collection<Object> resolveCollection(String key, Object defaultValue) {
		Collection<Object> results = new HashSet<Object>();
		try {
			this.recursivelyResolveCollection(
					results,
					this.contextSource, 
					getNamespace(key),
					getKey(key),
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
	
	
	@SuppressWarnings("unchecked")
	public Collection<String> keySet(String namespace) {
		// We use a spobject.name->spobject.key map in order to 
		// find all variables keys and names because the 
		// recursive version of this method is used to build a menu
		// as well. We will only return the keys though.
		MultiValueMap keys = new MultiValueMap();
		this.recursiveKeySet(
				this.contextSource,
				keys, 
				namespace,
				true);
		
		// Only return the values.
		return keys.values();
	}


// *******************  Private helper methods **********************************

	
	private Object recursivelyResolveSingleValue(
			SPObject currentNode, 
			String namespace,
			String key,
			boolean upwards) throws NotFoundException 
	{
		
		// First, verify if the current node is a variable resolver implementation.
		if (currentNode instanceof SPVariableResolverProvider) {
			// Turns out it is. Let's ask it if it can help us.
			SPVariableResolver resolver = ((SPVariableResolverProvider)currentNode).getVariableResolver();
			
			if (resolver.resolvesNamespace(namespace) &&
					resolver.resolves(namespace != null ? namespace + NAMESPACE_DELIMITER + key : key)) {
				// Kewl. We found the correct variable value.
				return resolver.resolve(namespace != null ? namespace + NAMESPACE_DELIMITER + key : key);
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
			boolean upwards) throws CompletedException 
	{
		// First, verify if the current node is a variable resolver implementation.
		if (currentNode instanceof SPVariableResolverProvider) {
			// Turns out it is. Let's ask it if it can help us.
			SPVariableResolver resolver = ((SPVariableResolverProvider)currentNode).getVariableResolver();
			if (resolver.resolvesNamespace(namespace) &&
					resolver.resolves(namespace != null ? namespace + NAMESPACE_DELIMITER + key : key)) {
				// Kewl. We found a valid variable resolver.
				results.addAll(resolver.resolveCollection(namespace != null ? namespace + NAMESPACE_DELIMITER + key : key));
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
	
	
	void recursiveKeySet(
			SPObject currentNode, 
			MultiValueMap keys,
			String namespace,
			boolean upwards) 
	{
		
		// First, verify if the current node is a variable resolver implementation.
		if (currentNode instanceof SPVariableResolverProvider) {
			// Turns out it is. Let's ask it if it can help us.
			SPVariableResolver resolver = ((SPVariableResolverProvider)currentNode).getVariableResolver();
			if (resolver.resolvesNamespace(namespace)) {
				for (String key : resolver.keySet(namespace)) {
					if (keys.getCollection(currentNode.getName()) == null
							||
								(
									keys.getCollection(currentNode.getName()) != null
									&& !keys.getCollection(currentNode.getName()).contains(key))
								)
					{
						keys.put(currentNode.getName(), key);
					}
				}
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
	
	private static class InsertVariableAction extends AbstractAction {
        private final String varName;
		private final JTextComponent target;
        InsertVariableAction(String label, String varName, JTextComponent target) {
            super(label);
            this.varName = varName;
			this.target = target;
        }
        public void actionPerformed(ActionEvent e) {
            try {
                target.getDocument().insertString(target.getCaretPosition(), "${" + varName + "}", null);
            } catch (BadLocationException ex) {
                throw new RuntimeException("Unexpected bad location exception", ex);
            }
        }
    }
}
