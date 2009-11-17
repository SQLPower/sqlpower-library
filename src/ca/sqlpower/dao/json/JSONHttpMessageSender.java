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

package ca.sqlpower.dao.json;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.json.JSONArray;
import org.json.JSONObject;

import ca.sqlpower.dao.HttpMessageSender;
import ca.sqlpower.dao.SPPersistenceException;
import ca.sqlpower.enterprise.client.SPServerInfo;

/**
 * An {@link HttpMessageSender} implementation that specifically sends it's
 * message content in the JSON format. (see <a
 * href="http://www.json.org">www.json.org</a>).
 */
public class JSONHttpMessageSender extends HttpMessageSender<JSONObject> {
	
	private JSONArray messageArray;
	
	public JSONHttpMessageSender(HttpClient httpClient, SPServerInfo serverInfo,
			String rootUUID) {
		super(httpClient, serverInfo, rootUUID);
		messageArray = new JSONArray();
	}

	public void send(JSONObject content) throws SPPersistenceException {
		messageArray.put(content);
	}
	
	public void flush() throws SPPersistenceException {
		try {
			URI serverURI = getServerURI();
			HttpPost postRequest = new HttpPost(serverURI);
			postRequest.setEntity(new StringEntity(messageArray.toString()));
			postRequest.setHeader("Content-Type", "application/json");
			HttpUriRequest request = postRequest;
	        getHttpClient().execute(request, new ResponseHandler<Void>() {
				public Void handleResponse(HttpResponse response)
						throws ClientProtocolException, IOException {
					StatusLine statusLine = response.getStatusLine();
					if (statusLine.getStatusCode() >= 400) {
						throw new ClientProtocolException( 
								"HTTP Post request returned an error: " +
								"Code = " + statusLine.getStatusCode() + ", " +
								"Reason = " + statusLine.getReasonPhrase());
					}
					return null;
				}
	        });
		} catch (URISyntaxException e) {
			throw new SPPersistenceException(null, e);
		} catch (ClientProtocolException e) {
			throw new SPPersistenceException(null, e);
		} catch (IOException e) {
			throw new SPPersistenceException(null, e);
		} finally {
			clearMessageArray();
		}
	}
	
	public void clear() {
		this.clearMessageArray();
	}
	
	private void clearMessageArray() {
		for (int i = messageArray.length() - 1; i >= 0; i--) {
			messageArray.remove(i);
		}
	}
}
