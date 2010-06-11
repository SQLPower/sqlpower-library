/*
 * Copyright (c) 2010, SQL Power Group Inc.
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

import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import org.apache.commons.collections.map.MultiValueMap;
import org.apache.log4j.Logger;

/**
 * This class keeps track of {@link SPVariableResolver}s that can resolve
 * namespaces and register to {@link SPObject} root objects.
 */
public class SPResolverRegistry {
	
	private static final Logger logger = Logger.getLogger(SPResolverRegistry.class);
	
	/**
	 * Maps a root SPObject to a list of resolvers.
	 */
	private static Map<String, List<SPVariableResolver>> resolvers = Collections.synchronizedMap(new WeakHashMap<String, List<SPVariableResolver>>());
	private static Map<String, TreeListener> listeners = Collections.synchronizedMap(new WeakHashMap<String, TreeListener>());
	
	private SPResolverRegistry() {
		// Everything in this class is static.
		// No need to create instances of it.
	}

	/**
	 * Returns the root {@link SPObject} given a descendant {@link SPObject}.
	 * 
	 * @param treeElement
	 *            The descendant {@link SPObject}.
	 * @return The root {@link SPObject}, or null if the descendant
	 *         {@link SPObject} is null.
	 */
	private static SPObject getRoot(SPObject treeElement) {
		if (treeElement == null) {
			return null;
		}
		
		while (true) {
			if (treeElement.getParent() == null) {
				return treeElement;
			} else {
				treeElement = treeElement.getParent();
			}
		}
	}

	/**
	 * Initializes the root of an {@link SPObject} in the resolver and listener
	 * {@link Map}s by creating a new key (if it does not exist) by UUID, and
	 * adds a tree listener to that root {@link SPObject}.
	 * 
	 * @param treeElement
	 *            The {@link SPObject} whose root object is to be initialized.
	 * @return The root {@link SPObject}, or null if the passed in
	 *         {@link SPObject} is null.
	 */
	public static SPObject init(SPObject treeElement) {
		if (treeElement == null) {
			return null;
		}
		
		synchronized (resolvers) {
			SPObject root = getRoot(treeElement);
			// No need to init this tree twice.
			if (!resolvers.containsKey(root.getUUID())) {
				// Init this placeholder with an empty list
				resolvers.put(root.getUUID(), Collections.synchronizedList(new ArrayList<SPVariableResolver>()));
				listeners.put(root.getUUID(), new TreeListener());
				// Now listen to the hierarchy for UUID change
				root.addSPListener(listeners.get(root.getUUID()));
			}
			return root;
		}
	}

	/**
	 * Registers an {@link SPVariableResolver} for the root of a given
	 * {@link SPObject} if it has not already been registered.
	 * 
	 * @param treeMember
	 *            The {@link SPObject} whose root object the resolver should be
	 *            registered to.
	 * @param resolver
	 *            The {@link SPVariableResolver} to register.
	 */
	public static void register(SPObject treeMember, SPVariableResolver resolver) {
		if (treeMember != null && resolver != null) {
			synchronized (resolvers) {
				SPObject root = init(treeMember);
				if (!resolvers.get(root.getUUID()).contains(resolver)) {
					logger.debug("Registering resolver - Namespace:" + resolver.getNamespace() + " bound to node:" + treeMember.getName());
					resolvers.get(root.getUUID()).add(resolver);
				}
			}
		}
	}

	/**
	 * Deregisters an {@link SPVariableResolver} from the root of a given
	 * {@link SPObject}.
	 * 
	 * @param treeMember
	 *            The {@link SPObject} whose root object the resolver should be
	 *            deregistered from.
	 * @param resolver
	 *            The {@link SPVariableResolver} to deregister.
	 */
	public static void deregister(SPObject treeMember, SPVariableResolver resolver) {
		if (treeMember != null && resolver != null) {
			synchronized (resolvers) {
				SPObject root = init(treeMember);
				logger.debug("Deregistering resolver - Namespace:" + resolver.getNamespace() + " bound to node:" + treeMember.getName());
				resolvers.get(root.getUUID()).remove(resolver);
			}
		}
	}

	/**
	 * Creates a {@link List} of registered {@link SPVariableResolver}s that
	 * resolves a given namespace for the root of a given {@link SPObject}.
	 * 
	 * @param treeMember
	 *            The {@link SPObject} whose root to get the resolvers from.
	 * @param namespace
	 *            The namespace that the resolvers should resolve.
	 * @return The created {@link List}.
	 */
	public static List<SPVariableResolver> getResolvers(SPObject treeMember, String namespace) {
		if (treeMember == null) {
			return Collections.emptyList();
		}
		
		synchronized (resolvers) {
			SPObject root = init(treeMember);
			if (root != null) {
				List<SPVariableResolver> registeredResolvers = resolvers.get(root.getUUID());
				List<SPVariableResolver> matches = new ArrayList<SPVariableResolver>();
				for (SPVariableResolver resolver: registeredResolvers) {
					if (resolver.resolvesNamespace(namespace)) {
						matches.add(resolver);
					}
				}
				return matches;
			} else {
				return Collections.emptyList();
			}
		}
	}

	/**
	 * Finds the first {@link SPVariableResolver} that resolves a given
	 * namespace for the root a given {@link SPObject}.
	 * 
	 * @param treeMember
	 *            The {@link SPObject} whose root to get the resolver from.
	 * @param namespace
	 *            The namespace that the resolver should resolve.
	 * @return The first {@link SPVariableResolver} found.
	 */
	public static SPVariableResolver getResolver(SPObject treeMember, String namespace) {
		if (treeMember == null) {
			return null;
		}
		
		synchronized (resolvers) {
			SPObject root = init(treeMember);
			for (SPVariableResolver resolver: resolvers.get(root.getUUID())) {
				if (resolver.resolvesNamespace(namespace)) {
					return resolver;
				}
			}
			return null;
		}
	}

	/**
	 * Creates a {@link MultiValueMap} of {@link SPVariableResolver} user
	 * friendly names to their respective namespace.
	 * 
	 * @param treeMember
	 *            The {@link SPObject} whose root to get the namespaces from.
	 * @return The created {@link MultiValueMap}.
	 */
	public static MultiValueMap getNamespaces(SPObject treeMember) {
		if (treeMember == null) {
			return new MultiValueMap();
		}
		
		synchronized (resolvers) {
			MultiValueMap results = new MultiValueMap();
			SPObject root = init(treeMember);
			for (SPVariableResolver resolver: resolvers.get(root.getUUID())) {
				results.put(resolver.getUserFriendlyName(), resolver.getNamespace());
			}
			return results;
		}
	}

	/**
	 * This {@link SPListener} listens to {@link SPObject}s whose UUID has
	 * changed and has {@link SPVariableResolver}s registered to that object. It
	 * updates the {@link Map}s of {@link SPResolverRegistry#resolvers} and
	 * {@link SPResolverRegistry#listeners} to this new UUID.
	 */
	private static class TreeListener extends AbstractPoolingSPListener {
		protected void propertyChangeImpl(PropertyChangeEvent e) {
			synchronized (resolvers) {
				if (e.getPropertyName().equalsIgnoreCase("uuid")
						&& resolvers.containsKey(e.getOldValue())) {
					// This means that the root object has changed it's UUID.
					// Update the maps accordingly.
					resolvers.put((String)e.getNewValue(), resolvers.get(e.getOldValue()));
					listeners.put((String)e.getNewValue(), listeners.get(e.getOldValue()));
					resolvers.remove(e.getOldValue());
					listeners.remove(e.getOldValue());
				}
			}
		}
	}
}
