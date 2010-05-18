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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections.map.MultiValueMap;
import org.apache.log4j.Logger;
import org.olap4j.OlapConnection;
import org.olap4j.PreparedOlapStatement;

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
	
	private final static Pattern varPattern = Pattern.compile("\\$\\{([^\\}]+)\\}");

	private static final Logger logger = Logger.getLogger(SPVariableHelper.class);
	
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
	
	
	/**
	 * Builds a variable helper to help resolve variables as values.
	 * @param contextSource The source node from which to start
	 * resolving variables.
	 */
	public SPVariableHelper(SPObject contextSource) {
		this.contextSource = contextSource;
	}
	
	/**
	 * Returns the node onto which this helper is pinned.
	 */
	public SPObject getContextSource() {
		return contextSource;
	}
	
	
	
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
        
        // Make sure that the registry is ready.
        SPResolverRegistry.init(variableHelper.getContextSource());
        
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
     * converts all that in a nifty prepared statement ready for execution, on time for Christmas.
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
     * converts all that in a nifty prepared statement ready for execution, on time for Christmas.
     * @param connection A connection object to use in order to generate the prepared statement.
     * @param sql A SQL string which might include variables.
     * @param variableHelper A {@link SPVariableHelper} object to resolve the variables.
     * @return A {@link PreparedStatement} object ready for execution.
     * @throws SQLException Might get thrown if we cannot generate a {@link PreparedStatement} with the supplied connection.
     */
    public static PreparedStatement substituteForDb(Connection connection, String sql, SPVariableHelper variableHelper) throws SQLException {
    	
    	// Make sure that the registry is ready.
        SPResolverRegistry.init(variableHelper.getContextSource());
        
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
     * Helper method that takes a connection and a MDX statement which includes variable and 
     * converts all that in a nifty prepared statement ready for execution, on time for Christmas.
     * @param connection A connection object to use in order to generate the prepared statement.
     * @param sql A MDX string which might include variables.
     * @return A {@link PreparedStatement} object ready for execution.
     * @throws SQLException Might get thrown if we cannot generate a {@link PreparedStatement} with the supplied connection.
     */
    public PreparedOlapStatement substituteForDb(
    		OlapConnection connection, 
    		String mdxQuery) throws SQLException 
    {
    	return substituteForDb(connection, mdxQuery, this);
    }
    
    
    /**
     * Helper method that takes a connection and a MDX statement which includes variable and 
     * converts all that in a nifty prepared statement ready for execution, on time for Christmas.
     * @param connection A connection object to use in order to generate the prepared statement.
     * @param sql A MDX string which might include variables.
     * @param variableHelper A {@link SPVariableHelper} object to resolve the variables.
     * @return A {@link PreparedStatement} object ready for execution.
     * @throws SQLException Might get thrown if we cannot generate a {@link PreparedStatement} with the supplied connection.
     */
    public static PreparedOlapStatement substituteForDb(
    		OlapConnection connection, 
    		String mdxQuery, 
    		SPVariableHelper variableHelper) throws SQLException 
    {
    	
    	// Make sure that the registry is ready.
        SPResolverRegistry.init(variableHelper.getContextSource());
        
        StringBuilder text = new StringBuilder();
        Matcher matcher = varPattern.matcher(mdxQuery);
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
                text.append(mdxQuery.substring(currentIndex, matcher.start()));
                text.append("?");
                currentIndex = matcher.end();
            }  
        }
        text.append(mdxQuery.substring(currentIndex));
        
        // Now generate a prepared statement and inject it's variables.
        PreparedOlapStatement ps = connection.prepareOlapStatement(text.toString());
        for (int i = 0; i < vars.size(); i++) {
    		ps.setObject(i+1, vars.get(i));
        }
        
        return ps;
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
    	
    	String returnValue = varDef;
    	
    	int namespaceIndex = varDef.indexOf(NAMESPACE_DELIMITER);
		if (namespaceIndex != -1) {
			returnValue = returnValue.substring(namespaceIndex + NAMESPACE_DELIMITER.length(), varDef.length());
		}
		
		int defValueIndex = returnValue.indexOf(DEFAULT_VALUE_DELIMITER);
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
	 * Searches and returns the first resolver for a given namespace
	 * it can find in the tree. If none can be found, NULL is returned.
	 * @param namespace The namespace for which we want the resolver.
	 * @return Either a proper resolver for the given namespace or null
	 * if none can be found.
	 */
	public SPVariableResolver getResolverForNamespace(String namespace) {
		return SPResolverRegistry.getResolver(this.contextSource, namespace);
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
		
		String namespace = getNamespace(key);
		
		try {
			if (namespace != null) {
				SPVariableResolver resolver = 
					SPResolverRegistry.getResolver(this.contextSource, namespace);
				if (resolver==null) {
					return defaultValue;
				} else {
					return resolver.resolve(key, defaultValue);
				}
			}
			
			SPObject node = this.contextSource;
			while (true) {	
				if (node instanceof SPVariableResolverProvider) {
					SPVariableResolver resolver = ((SPVariableResolverProvider)node).getVariableResolver();
					if (resolver.resolves(key)) {
						return resolver.resolve(key, defaultValue);
					}
				}
				node = node.getParent();
				if (node == null) return defaultValue;
			}
		} catch (StackOverflowError soe) {
			throw new RecursiveVariableException();
		}
		
	}

	
	
	public Collection<Object> resolveCollection(String key) {
		return this.resolveCollection(stripDefaultValue(key), getDefaultValue(key));
	}

	public Collection<Object> resolveCollection(String key, Object defaultValue) {
		
		LinkedHashSet<Object> results = new LinkedHashSet<Object>();
		String namespace = getNamespace(key);
		
		try {
			if (namespace != null) {
				List<SPVariableResolver> resolvers = SPResolverRegistry.getResolvers(this.contextSource, namespace);
				for (SPVariableResolver resolver : resolvers) {
					if (resolver.resolves(key)) {
						results.addAll(resolver.resolveCollection(key));
						if (!globalCollectionResolve) {
							break;
						}
					}
				}
			} else {
				SPObject node = this.contextSource;
				while (true) {	
					if (node instanceof SPVariableResolverProvider) {
						SPVariableResolver resolver = ((SPVariableResolverProvider)node).getVariableResolver();
						if (resolver.resolves(key)) {
							results.addAll(resolver.resolveCollection(key));
							if (!globalCollectionResolve) {
								break;
							}
						}
					}
					node = node.getParent();
					if (node == null) break;
				}
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
		} catch (StackOverflowError soe) {
			throw new RecursiveVariableException();
		}
		
	}

	
	
	public boolean resolves(String key) {
		String namespace = getNamespace(key);
		if (namespace != null) {
			return SPResolverRegistry.getResolver(this.contextSource, namespace) != null;
		} else {
			SPObject node = this.contextSource;
			while (true) {	
				if (node instanceof SPVariableResolverProvider) {
					SPVariableResolver resolver = ((SPVariableResolverProvider)node).getVariableResolver();
					if (resolver.resolves(key)) {
						return true;
					}
				}
				node = node.getParent();
				if (node == null) return false;
			}
		}
	}

	
	
	public boolean resolvesNamespace(String namespace) {
		return SPResolverRegistry.getResolver(this.contextSource, namespace) != null;
	}
	
	
	
	public Collection<Object> matches(String key, String partialValue) {
		
		Collection<Object> matches = new HashSet<Object>();
		String namespace = getNamespace(key);
		
		try {
			if (namespace != null) {
				for (SPVariableResolver resolver : SPResolverRegistry.getResolvers(contextSource, namespace)) {
					if (resolver.resolves(key)) {
						matches.addAll(resolver.matches(key, partialValue));
						if (!globalCollectionResolve) {
							break;
						}
					}
				}
				return matches;
			} else {
				SPObject node = this.contextSource;
				while (true) {	
					if (node instanceof SPVariableResolverProvider) {
						SPVariableResolver resolver = ((SPVariableResolverProvider)node).getVariableResolver();
						if (resolver.resolves(key)) {
							matches.addAll(resolver.matches(key, partialValue));
							if (!globalCollectionResolve) {
								break;
							}
						}
					}
					node = node.getParent();
					if (node == null) break;
				}
				return matches;
			}
		} catch (StackOverflowError soe) {
			throw new RecursiveVariableException();
		}
		
	}
	
	public Collection<String> keySet(String namespace) {
		
		List<String> results = new ArrayList<String>();
		
		if (namespace != null) {
			for (SPVariableResolver resolver : SPResolverRegistry.getResolvers(this.contextSource, namespace)) {
				if (resolver.resolvesNamespace(namespace)) {
					results.addAll(resolver.keySet(namespace));
				}
			}
			return results;
		} else {
			SPObject node = this.contextSource;
			while (true) {	
				if (node instanceof SPVariableResolverProvider) {
					SPVariableResolver resolver = ((SPVariableResolverProvider)node).getVariableResolver();
					results.addAll(resolver.keySet(namespace));
				}
				node = node.getParent();
				if (node == null) break;
			}
			return results;
		}
	}
	
	public String getNamespace() {
		throw new UnsupportedOperationException("SPVariableHelper is not bound to a namespace.");
	}
	
	/**
	 * Creates a list of user friendly names->namespaces.
	 * If you want only the namespaces, do {@link MultiValueMap#values()}
	 * @return
	 */
	public MultiValueMap getNamespaces() {
		return SPResolverRegistry.getNamespaces(this.contextSource);
	}

	public String getUserFriendlyName() {
		return null;
	}
	
	/**
	 * Wraps the RuntimeException to identify recursive variables resolutions.
	 */
	public class RecursiveVariableException extends RuntimeException {
	}

	public void delete(String key) {
		throw new UnsupportedOperationException("SPVariableHelper cannot store variables.");
	}

	public void store(String key, Object value) {
		throw new UnsupportedOperationException("SPVariableHelper cannot store variables.");
	}

	public void update(String key, Object value) {
		throw new UnsupportedOperationException("SPVariableHelper cannot store variables.");
	}
}
