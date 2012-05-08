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

package ca.sqlpower.enterprise.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import ca.sqlpower.util.Version;

/**
 * A class for managing a client application's list of {@link SPServerInfo}.
 * When new {@link SPServerInfo}'s are created, they should be added to the
 * {@link SPServerInfoManager}. It will also check for pre-existing
 * {@link SPServerInfo} defined as children in a Preferences node provided in
 * the constructor. It will also handle saving new {@link SPServerInfo} as
 * children of the same Preferences node.
 */
public class SPServerInfoManager {
	
	/**
	 * List of {@link SPServerInfo} being managed
	 */
	private final List<SPServerInfo> servers;

    /**
     * These listeners will be notified when server information is added or removed from the context.
     */
    private final List<ServerListListener> listeners;
    
    /**
     * Preferences node where the server configurations will be saved 
     */
    private final Preferences serverPrefs;
	
	/**
	 * A {@link Version} with this client's version number to use to determine
	 * compatibility with servers
	 */
	private final Version clientVersion;
	
	/**
	 * Default {@link SPServerInfo} settings for when making new SPServerInfo
	 * settings
	 */
	private final SPServerInfo defaultSettings;
    
	/**
	 * Creates an {@link SPServerInfoManager} and populates it with existing
	 * SPServerInfo configurations loaded from the user's Preferences
	 * 
	 * @param prefs
	 *            The Preferences node under which the manually-configured
	 *            server information is stored.
	 * @param clientVersion
	 *            A Version containing the version of this client to be used to
	 *            determine compatibility with servers
	 * @throws BackingStoreException
	 *             If there is a failure in the backing store while reading the
	 *             Preferences node
	 */
	public SPServerInfoManager(Preferences prefs, Version clientVersion, SPServerInfo defaultSettings) throws BackingStoreException {
		servers = new ArrayList<SPServerInfo>();
		listeners = new ArrayList<ServerListListener>();
		this.serverPrefs = prefs;
		this.clientVersion = clientVersion;
		this.defaultSettings = defaultSettings;
        for (String nodeName : serverPrefs.childrenNames()) {
            Preferences serverNode = serverPrefs.node(nodeName);
            if (defaultSettings.isPasswordAllowed()) {
            	servers.add(new SPServerInfo(
            			serverNode.get("name", null),
            			serverNode.get("serverAddress", null),
            			serverNode.getInt("port", 0),
            			serverNode.get("path", null),
            			serverNode.get("username", ""),
            			serverNode.get("password", "")));
            } else {
            	servers.add(new SPServerInfo(
            			serverNode.get("scheme", "https"),
                        serverNode.get("name", null),
                        serverNode.get("serverAddress", null),
                        serverNode.getInt("port", 0),
                        serverNode.get("path", null),
                        serverNode.get("username", "")));
            }
        }
	}

	/**
	 * Adds the {@link SPServerInfo} to this manager.
	 * 
	 * @param server
	 *            The {@link SPServerInfo} to add
	 */
	public void add(SPServerInfo server) {
		servers.add(server);
		Preferences thisServer = serverPrefs.node(server.getName());
		thisServer.put("scheme", server.getScheme());
        thisServer.put("name", server.getName());
        thisServer.put("serverAddress", server.getServerAddress());
        thisServer.putInt("port", server.getPort());
        thisServer.put("path", server.getPath());
        thisServer.put("username", server.getUsername());
        thisServer.put("password", server.getPassword());
		for (ServerListListener s: listeners) {
			s.serverAdded(new ServerListEvent(server));
		}
	}
	
	/**
	 * Removes the {@link SPServerInfo} from this manager.
	 * 
	 * @param server
	 *            The {@link SPServerInfo} to remove
	 */
	public void remove(SPServerInfo server) {
		servers.remove(server);
		 try {
			 serverPrefs.node(server.getName()).removeNode();
        } catch (BackingStoreException ex) {
            throw new RuntimeException("Failed to remove server from list", ex);
        }
		for (ServerListListener s: listeners) {
			s.serverRemoved(new ServerListEvent(server));
		}
	}

	/**
	 * Gets an unmodifiable list of the servers
	 * 
	 * @param getDiscoveredServers
	 *            If true, it will also return the list of servers discovered by
	 *            Multicast DNS
	 * @return A List of {@link SPServerInfo} instnaces being managed by this
	 *         manager
	 */
	public List<SPServerInfo> getServers(boolean getDiscoveredServers) {
		return Collections.unmodifiableList(servers);
	}
	
	/**
	 * Adds a {@link ServerListListener} to listen for additions and removals to
	 * the list of {@link SPServerInfo}
	 * 
	 * @param l
	 *            The {@link ServerListListener} to attach
	 */
    public void addServerListListener(ServerListListener l) {
    	if (l != null) {
    		listeners.add(l);
    	}
    }
    
    /**
     * Removes a {@link ServerListListener} 
     * @param l
     */
    public void removeServerListListener(ServerListListener l) {
    	if (l != null) {
    		listeners.remove(l);
    	}
    }

	/**
	 * Gets the Version of the client that this {@link SPServerInfoManager} is
	 * managing server instances for.
	 * 
	 * @return A {@link Version} containing the client version.
	 */
	public Version getClientVersion() {
		return clientVersion;
	}

	/**
	 * Gets a default {@link SPServerInfo} configuration for new
	 * {@link SPServerInfo}s.
	 * 
	 * @return An {@link SPServerInfo} instance that contains a default
	 *         configuration that can be used when creating new
	 *         {@link SPServerInfo} instances
	 */
	public SPServerInfo getDefaultSettings() {
		return defaultSettings;
	}
 }
