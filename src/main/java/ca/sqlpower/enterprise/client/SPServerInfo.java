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

package ca.sqlpower.enterprise.client;

import java.net.URI;
import java.net.URISyntaxException;

import javax.annotation.concurrent.Immutable;
import javax.jmdns.ServiceInfo;

/**
 * All the information required to make an enterprise server connection.
 * Can be created either by directly specifying values, or by providing a
 * ServiceInfo object to read the values from.
 */
@Immutable
public class SPServerInfo {

	/**
	 * The scheme to use for the connection, normally "http" or "https".
	 * <p>
	 * The scheme is set to "http" when a password is specified and "https" when
	 * a password is not specified for historical reasons to prevent having to
	 * change the SQL products.
	 */
	private final String scheme;
    private final String name;
    private final String serverAddress;
    private final int port;
    private final String path;
	private final String username;
	private final String password;

	/**
	 * If true then we allow users to specify and store a password for their
	 * connection. If false we will not store the password and instead prompt
	 * for it on every connect attempt.
	 */
	private final boolean passwordAllowed;
	
    /**
     * 
     * @param name The user-visible name for this server
     * @param serverAddress The address for the server (numeric or DNS name)
     * @param port The port number the server listens on
     * @param path The path to the enterprise server. Must begin with a '/'.
     */
    public SPServerInfo(String name, String serverAddress, int port, String path, String username, String password) {
    	this.scheme = "http";
        this.name = name;
        this.serverAddress = serverAddress;
        this.port = port;
        this.path = path;
		this.username = username;
		this.password = password;
		passwordAllowed = true;
        
        if (path == null || path.length() < 1 || path.charAt(0) != '/') {
            throw new IllegalArgumentException("path must begin with a /");
        }
    }

	/**
	 * This version of the server info does not store a password for the user.
	 * 
	 * @param name
	 *            The user-visible name for this server
	 * @param serverAddress
	 *            The address for the server (numeric or DNS name)
	 * @param port
	 *            The port number the server listens on
	 * @param path
	 *            The path to the enterprise server. Must begin with a '/'.
	 */
    public SPServerInfo(String scheme, String name, String serverAddress, int port, String path, String username) {
    	this.scheme = scheme;
    	this.name = name;
        this.serverAddress = serverAddress;
        this.port = port;
        this.path = path;
		this.username = username;
		this.password = "";
		passwordAllowed = false;
        
        if (path == null || path.length() < 1 || path.charAt(0) != '/') {
            throw new IllegalArgumentException("path must begin with a /");
        }
    }

	/**
	 * Clones a server info object but allows us to specify the password for the
	 * new instance.
	 */
    public SPServerInfo(SPServerInfo info, String password) {
    	this.scheme = info.scheme;
    	this.name = info.name;
        this.serverAddress = info.serverAddress;
        this.port = info.port;
        this.path = info.path;
		this.username = info.username;
		this.password = password;
		passwordAllowed = true;
    }
    
    public SPServerInfo(ServiceInfo si) {
    	scheme = "http";
        name = si.getName();
        serverAddress = si.getHostAddress();
        port = si.getPort();
        path = si.getPropertyString("path");
        username = null;
        password = null;
        passwordAllowed = true;
    }

    public String getName() {
        return name;
    }

    public String getServerAddress() {
        return serverAddress;
    }

    public int getPort() {
        return port;
    }

    public String getPath() {
        return path;
    }
    
    public URI toURI() throws URISyntaxException {
        return new URI("http", null, serverAddress, port, path, null, null);
    }
    
    @Override
    public String toString() {
        return name + " (" + serverAddress + ":" + port + path + ")";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((path == null) ? 0 : path.hashCode());
        result = prime * result + port;
        result = prime * result + ((serverAddress == null) ? 0 : serverAddress.hashCode());
        return result;
    }

    /**
     * Determines equality based on name, path, port and server address.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        
        SPServerInfo other = (SPServerInfo) obj;
        if (name == null) {
            if (other.name != null) return false;
        } else if (!name.equals(other.name)) {
            return false;
        }
        
        if (path == null) {
            if (other.path != null) return false;
        } else if (!path.equals(other.path)) {
            return false;
        }
        
        if (port != other.port) return false;
        
        if (serverAddress == null) {
            if (other.serverAddress != null) return false;
        } else if (!serverAddress.equals(other.serverAddress)) {
            return false;
        }
        
        return true;
    }

	public String getUsername() {
		return username;
	}
    
	public String getPassword() {
		return password;
	}

	public boolean isPasswordAllowed() {
		return passwordAllowed;
	}
	
	public String getScheme() {
		return scheme;
	}
	
}
