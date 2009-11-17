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

package ca.sqlpower.dao;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.client.HttpClient;

import ca.sqlpower.enterprise.client.SPServerInfo;
import ca.sqlpower.object.SPObject;

/**
 * An abstract {@link MessageSender} implementation that specifically
 * transmits messages using the HTTP protocol.
 *
 * @param <T> An Object type that represents the content of the message
 */
public abstract class HttpMessageSender<T> implements MessageSender<T> {

	/**
	 * An {@link HttpClient} object that will be used to send HTTP requests
	 */
	private final HttpClient httpClient;
	
	/**
	 * A {@link SPServerInfo} instance containing information on a given
	 * server necessary to send HTTP requests to it
	 */
	private final SPServerInfo serverInfo;

	/**
	 * The UUID of the root {@link SPObject} that this MessagePasser will be
	 * sending HTTP requests about
	 */
	private final String rootUUID;

	/**
	 * Creates an HttpMessagePasser configured to send HTTP requests according
	 * to the settings contained in the {@link SPServerInfo} parameter. All HTTP
	 * requests will be made regarding the  with the given UUID.
	 * 
	 * @param serverInfo
	 *            A {@link SPServerInfo} instance containing the connection
	 *            information for the HTTP server that the HTTP requests will be
	 *            sent to
	 * @param rootUUID
	 *            The UUID that identifies the root {@link SPObject} that
	 *            the HTTP requests will be referring to
	 */
	public HttpMessageSender(HttpClient httpClient, SPServerInfo serverInfo, String rootUUID) {
        this.httpClient = httpClient;
		this.serverInfo = serverInfo;
		this.rootUUID = rootUUID;
	}
	
    protected URI getServerURI() throws URISyntaxException {
        String contextPath = serverInfo.getPath();
        return new URI("http", null, serverInfo.getServerAddress(), serverInfo.getPort(),
                contextPath + "workspaces/" + rootUUID, null, null);
    }

    /**
     * Returns the {@link HttpClient} to be used to send HTTP requests
     */
	protected HttpClient getHttpClient() {
		return httpClient;
	}

	/**
	 * Returns a {@link SPServerInfo} instance containing the connection
	 * information of the HTTP server that HTTP request will be sent to
	 */
	protected SPServerInfo getServerInfo() {
		return serverInfo;
	}
}
