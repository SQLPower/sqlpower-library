/*
 * Copyright (c) 2010, SQL Power Group Inc.
 *
 * This file is part of Power*Architect.
 *
 * Power*Architect is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * Power*Architect is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>. 
 */

package ca.sqlpower.enterprise;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;

import org.apache.http.HttpResponse;
import org.apache.http.client.ResponseHandler;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.springframework.security.AccessDeniedException;

import ca.sqlpower.dao.SPPersistenceException;

public class JSONResponseHandler implements ResponseHandler<JSONMessage> {
    
    private static final Logger logger = Logger.getLogger(JSONResponseHandler.class);

    /*
     * Unsuccessful responses should have information sent in a header, 
     * either as "unsuccessfulResponse" or "exceptionStackTrace"
     */

    public JSONMessage handleResponse(HttpResponse response) {
        try {
            
            int status = response.getStatusLine().getStatusCode();
            if (status == 401) {
                throw new AccessDeniedException("Access Denied");
            }
            
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(response.getEntity().getContent()));
            return handleResponse(reader, status);
        } catch (AccessDeniedException e) {
            throw e;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
    
    public JSONMessage handleResponse(String json, int status) {
    	return handleResponse(new StringReader(json), status);
    }
    
    public JSONMessage handleResponse(Reader reader, int status) {
        if (status == 404) {
            throw new RuntimeException("Server resource is not available.");
        }
        
        JSONTokener tokener = new JSONTokener(reader);
        try {
        	JSONObject message = (JSONObject) tokener.nextValue();
            
            // Does the response contain data? If so, return it. Communication
            // with the resource has been successful.
            if (message.getString("responseKind").equals("data")) {    
                return new JSONMessage(message.getString("data"), status);
            } else {
                // Has the request been unsuccessful?
                if (message.getString("responseKind").equals("unsuccessful")) {
                    return new JSONMessage(message.getString("data"), status);
                } else {
                    // Does the response contain an exception? If so, reconstruct, and then
                    // re-throw it. There has been an exception on the server.
                    if (message.getString("responseKind").equals("exceptionStackTrace")) {

                        JSONArray stackTraceStrings = new JSONArray(message.getString("data"));
                        StringBuffer stackTraceMessage = new StringBuffer();
                        for (int i = 0; i < stackTraceStrings.length(); i++) {
                            stackTraceMessage.append("\n").append(stackTraceStrings.get(i));
                        }
                        
                        throw new SPPersistenceException(null, stackTraceMessage.toString());
                        
                    } else {
                        // This exception represents a(n epic) client-server miscommunication
                        throw new Exception("Unable to parse response ");
                    }
                }
            }
        } catch (Exception ex) {
            throw new RuntimeException("Server returned status " + status + "\n" + tokener, ex);
        }
    }
}
