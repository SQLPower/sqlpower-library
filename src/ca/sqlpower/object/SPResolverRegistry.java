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

public class SPResolverRegistry {
	
	private static final Logger logger = Logger.getLogger(SPResolverRegistry.class);
	
	/**
	 * Maps a root SPObject to a list of resolvers.
	 */
	private static Map<String, List<SPVariableResolver>> resolvers = Collections.synchronizedMap(new WeakHashMap<String, List<SPVariableResolver>>());
	private static Map<String, TreeListener> listeners = Collections.synchronizedMap(new WeakHashMap<String, TreeListener>());
	
	
	private static SPObject getRoot(SPObject treeElement) {
		while (true) {
			if (treeElement.getParent() == null) {
				return treeElement;
			} else {
				treeElement = treeElement.getParent();
			}
		}
	}
	
	public static SPObject init(SPObject treeElement) {
		
		SPObject root = getRoot(treeElement);
		
		synchronized (resolvers) {
			// No need to init this tree twice.
			if (!resolvers.containsKey(root.getUUID())) {
				// Init this placeholder with an empty list
				resolvers.put(root.getUUID(), Collections.synchronizedList(new ArrayList<SPVariableResolver>()));
				listeners.put(root.getUUID(), new TreeListener());
				// Now listen to the hierarchy for UUID change
				root.addSPListener(listeners.get(root.getUUID()));
			}
		}
		
		return root;
	}
	
	public static void register(SPObject treeMember, SPVariableResolver resolver) {
		synchronized (resolvers) {
			SPObject root = init(treeMember);
			if (!resolvers.get(root.getUUID()).contains(resolver)) {
				logger.debug("Registering resolver - Namespace:" + resolver.getNamespace() + " bound to node:" + treeMember.getName());
				resolvers.get(root.getUUID()).add(resolver);
			}
		}
	}
	
	public static void deregister(SPObject treeMember, SPVariableResolver resolver) {
		synchronized (resolvers) {
			logger.debug("Deregistering resolver - Namespace:" + resolver.getNamespace() + " bound to node:" + treeMember.getName());
			SPObject root = init(treeMember);
			resolvers.get(root.getUUID()).remove(resolver);
		}
	}
	
	public static List<SPVariableResolver> getResolvers(SPObject treeMember, String namespace) {
		synchronized (resolvers) {
			SPObject root = init(treeMember);
			List<SPVariableResolver> registeredResolvers = resolvers.get(root.getUUID());
			List<SPVariableResolver> matches = new ArrayList<SPVariableResolver>();
			for (SPVariableResolver resolver: registeredResolvers) {
				if (resolver.resolvesNamespace(namespace)) {
					matches.add(resolver);
				}
			}
			return matches;
		}
	}
	
	public static SPVariableResolver getResolver(SPObject treeMember, String namespace) {
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
	
	public static MultiValueMap getNamespaces(SPObject treeMember) {
		synchronized (resolvers) {
			MultiValueMap results = new MultiValueMap();
			SPObject root = init(treeMember);
			for (SPVariableResolver resolver: resolvers.get(root.getUUID())) {
				results.put(resolver.getUserFriendlyName(), resolver.getNamespace());
			}
			return results;
		}
	}
	
	private static class TreeListener extends AbstractSPListener {
		protected void propertyChangeImpl(PropertyChangeEvent e) {
			if (e.getPropertyName().equalsIgnoreCase("uuid")
					&& resolvers.containsKey(e.getOldValue())) {
				// This means that the root object has changed it's UUID.
				// Update the maps accordingly.
				synchronized (resolvers) {
					resolvers.put((String)e.getNewValue(), resolvers.get(e.getOldValue()));
					listeners.put((String)e.getNewValue(), listeners.get(e.getOldValue()));
					resolvers.remove(e.getOldValue());
					listeners.remove(e.getOldValue());
				}
			}
		}
	}
}
