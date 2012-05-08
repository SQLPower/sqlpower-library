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

package ca.sqlpower.enterprise;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.security.AccessDeniedException;

import ca.sqlpower.dao.SPPersistenceException;
import ca.sqlpower.dao.json.SPJSONMessageDecoder;
import ca.sqlpower.enterprise.client.ProjectLocation;
import ca.sqlpower.enterprise.client.SPServerInfo;
import ca.sqlpower.util.UserPrompter.UserPromptOptions;
import ca.sqlpower.util.UserPrompter.UserPromptResponse;
import ca.sqlpower.util.UserPrompterFactory;
import ca.sqlpower.util.UserPrompterFactory.UserPromptType;

/**
 * This class contains static methods mainly for posting different things to the server using the resources.
 * Any method in this class that requires a cookieStore should be called from {programName}ClientSideSession.
 * That is where the cookieStore comes from.
 */
public class ClientSideSessionUtils {

	

    /**
     * All requests to the server will contain this tag after the enterprise
     * server name (which is normally architect-enterprise).
     */
	public static final String REST_TAG = "rest";
	
	/**
	 * The UUID of the system workspace.
	 */
	public static final String SYSTEM_UUID = "system";
	
	public static HttpClient createHttpClient(SPServerInfo serviceInfo, CookieStore cookieStore) {
		HttpParams params = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(params, 2000);
        DefaultHttpClient httpClient = new DefaultHttpClient(params);
        httpClient.setCookieStore(cookieStore);
        httpClient.getCredentialsProvider().setCredentials(
            new AuthScope(serviceInfo.getServerAddress(), AuthScope.ANY_PORT), 
            new UsernamePasswordCredentials(serviceInfo.getUsername(), serviceInfo.getPassword()));
        return httpClient;
	}
    
    public static ProjectLocation createNewServerSession(SPServerInfo serviceInfo, String name, 
    		CookieStore cookieStore, UserPrompterFactory userPrompterFactory)
    throws URISyntaxException, ClientProtocolException, IOException, JSONException {
        
    	HttpClient httpClient = ClientSideSessionUtils.createHttpClient(serviceInfo, cookieStore);
    	try {
    		HttpUriRequest request = new HttpGet(getServerURI(serviceInfo, "/" + REST_TAG + "/jcr/projects/new", "name=" + name));
    		JSONMessage message = httpClient.execute(request, new JSONResponseHandler());
    		JSONObject response = new JSONObject(message.getBody());
    		return new ProjectLocation(
    					response.getString("uuid"),
    					response.getString("name"),
    					serviceInfo);
    	} catch (AccessDeniedException e) {
    		userPrompterFactory.createUserPrompter("You do not have sufficient privileges to create a new workspace.", 
                    UserPromptType.MESSAGE, 
                    UserPromptOptions.OK, 
                    UserPromptResponse.OK, 
                    "OK", "OK").promptUser("");
    	    return null;
    	} finally {
    		httpClient.getConnectionManager().shutdown();
    	}
    }
	
	public static URI getServerURI(SPServerInfo serviceInfo, String contextRelativePath) throws URISyntaxException {
	    return getServerURI(serviceInfo, contextRelativePath, null);
	}
	
	public static URI getServerURI(SPServerInfo serviceInfo, String contextRelativePath, String query) throws URISyntaxException {
        String contextPath = serviceInfo.getPath();
        URI serverURI = new URI(serviceInfo.getScheme(), null, serviceInfo.getServerAddress(), serviceInfo.getPort(),
                contextPath + contextRelativePath, query, null);
        return serverURI;
    }
	
	public static List<TransactionInformation> decodeJSONRevisionList(String json) 
	throws JSONException, ParseException {
        JSONArray jsonArray = new JSONArray(json);
        List<TransactionInformation> transactions = new ArrayList<TransactionInformation>();
        
        for (int i = 0; i < jsonArray.length(); i++) {
            
            JSONObject jsonItem = jsonArray.getJSONObject(i);
            TransactionInformation transaction = new TransactionInformation(
                    jsonItem.getLong("number"),                     
                    jsonItem.getLong("time"),
                    jsonItem.getString("author"),
                    jsonItem.getString("description"),
                    jsonItem.getString("simpleDescription"));
            transactions.add(transaction);
            
        }
        
        return transactions;
	}
	
	public static void deleteServerWorkspace(ProjectLocation projectLocation, CookieStore cookieStore, 
			UserPrompterFactory userPrompterFactory) throws URISyntaxException, ClientProtocolException, IOException {
    	SPServerInfo serviceInfo = projectLocation.getServiceInfo();
    	HttpClient httpClient = ClientSideSessionUtils.createHttpClient(serviceInfo, cookieStore);
    	
    	try {
    		executeServerRequest(httpClient, projectLocation.getServiceInfo(),
    		        "/" + ClientSideSessionUtils.REST_TAG + "/jcr/" + projectLocation.getUUID() + "/delete", 
    				new JSONResponseHandler());
    	} catch (AccessDeniedException e) {
    		userPrompterFactory.createUserPrompter("You do not have sufficient privileges to delete the selected workspace.", 
                    UserPromptType.MESSAGE, 
                    UserPromptOptions.OK, 
                    UserPromptResponse.OK, 
                    "OK", "OK").promptUser("");
    	} finally {
    		httpClient.getConnectionManager().shutdown();
    	}
    }
	
	public static <T> T executeServerRequest(HttpClient httpClient, SPServerInfo serviceInfo, 
            String contextRelativePath, ResponseHandler<T> responseHandler)throws IOException, URISyntaxException {
        return executeServerRequest(httpClient, serviceInfo, contextRelativePath, null, responseHandler);
    }	
	
	public static <T> T executeServerRequest(HttpClient httpClient, SPServerInfo serviceInfo, 
	        String contextRelativePath, String query, ResponseHandler<T> responseHandler) throws IOException, URISyntaxException {
	    HttpUriRequest request = new HttpGet(getServerURI(serviceInfo, contextRelativePath, query));  
	    return httpClient.execute(request, responseHandler);
	}
	
	public static List<ProjectLocation> getWorkspaceNames(SPServerInfo serviceInfo, 
			CookieStore cookieStore, UserPrompterFactory upf) 
	throws IOException, URISyntaxException, JSONException {
    	HttpClient httpClient = ClientSideSessionUtils.createHttpClient(serviceInfo, cookieStore);
    	try {
    		HttpUriRequest request = new HttpGet(getServerURI(serviceInfo, "/" + ClientSideSessionUtils.REST_TAG + "/jcr/projects"));
    		JSONMessage message = httpClient.execute(request, new JSONResponseHandler());
    		if (message.getStatusCode() == 412) { //precondition failed
    			upf.createUserPrompter(message.getBody(), UserPromptType.MESSAGE, 
    					UserPromptOptions.OK, UserPromptResponse.OK, null, "OK").promptUser();
    		}
    		List<ProjectLocation> workspaces = new ArrayList<ProjectLocation>();
    		JSONArray response = new JSONArray(message.getBody());
    		for (int i = 0; i < response.length(); i++) {
    			JSONObject workspace = (JSONObject) response.get(i);
    			workspaces.add(new ProjectLocation(
    					workspace.getString("uuid"),
    					workspace.getString("name"),
    					serviceInfo));
    		}
    		return workspaces;
    	} catch (AccessDeniedException e) {
    	    throw e;
    	} finally {
    		httpClient.getConnectionManager().shutdown();
    	}
    }
	
	/**
	 * Requests the server for persist calls from version 0 to the given revision
	 * of the given project, and persists them to the given decoder.
	 * 
	 * @param projectLocation
	 * @param revisionNo Must be greater than zero, and no greater than the current revision number
	 * @param decoder
	 * @throws IOException
	 * @throws URISyntaxException
	 * @throws SPPersistenceException
	 * @throws IllegalArgumentException Thrown if the server rejects the given revisionNo
	 */
	public static void persistRevisionFromServer(ProjectLocation projectLocation, 
	        int revisionNo, 
	        SPJSONMessageDecoder decoder,
	        CookieStore cookieStore)
	throws IOException, URISyntaxException, SPPersistenceException, IllegalArgumentException {
	    
	    SPServerInfo serviceInfo = projectLocation.getServiceInfo();
	    HttpClient httpClient = ClientSideSessionUtils.createHttpClient(serviceInfo, cookieStore);
        
        try {
            JSONMessage response = ClientSideSessionUtils.executeServerRequest(httpClient, serviceInfo,
                    "/" + ClientSideSessionUtils.REST_TAG + "/project/" + projectLocation.getUUID() + "/" + revisionNo,
                    new JSONResponseHandler());            
            
            if (response.isSuccessful()) {
                decoder.decode(response.getBody());                
            } else {
                throw new IllegalArgumentException("The server rejected the revision number " +
                		"(it must be greater than 0, and no greater than the current revision number)");
            }
            
        } finally {
            httpClient.getConnectionManager().shutdown();
        }   
	}
	
	/**
	 * This method reverts the server workspace specified by the given project location
	 * to the specified revision number.
	 * 
	 * All sessions should automatically update to the reverted revision due to their Updater.
	 * 
	 * @returns The new global revision number, right after the reversion, or -1 if the server did not revert.
	 * @throws IOException
	 * @throws URISyntaxException
	 * @throws JSONException 
	 */
	public static int revertServerWorkspace(ProjectLocation projectLocation, int revisionNo, CookieStore cookieStore)
	throws IOException, URISyntaxException, JSONException {
        SPServerInfo serviceInfo = projectLocation.getServiceInfo();
        HttpClient httpClient = ClientSideSessionUtils.createHttpClient(serviceInfo, cookieStore);
        
        try {
            JSONMessage message = ClientSideSessionUtils.executeServerRequest(httpClient, projectLocation.getServiceInfo(),
                    "/" + ClientSideSessionUtils.REST_TAG + "/project/" + projectLocation.getUUID() + "/revert",
                    "revisionNo=" + revisionNo, 
                    new JSONResponseHandler());    
            if (message.isSuccessful()) {
                return new JSONObject(message.getBody()).getInt("currentRevision");
            } else {
                return -1;
            }
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
	}
	
	
	public static ProjectLocation uploadProject(SPServerInfo serviceInfo, String name, File project, UserPrompterFactory session, CookieStore cookieStore) 
    throws URISyntaxException, ClientProtocolException, IOException, JSONException {
        HttpClient httpClient = ClientSideSessionUtils.createHttpClient(serviceInfo, cookieStore);
        try {
            MultipartEntity entity = new MultipartEntity();
            ContentBody fileBody = new FileBody(project);
            ContentBody nameBody = new StringBody(name);
            entity.addPart("file", fileBody);
            entity.addPart("name", nameBody);
            
            HttpPost request = new HttpPost(ClientSideSessionUtils.getServerURI(serviceInfo, "/" + ClientSideSessionUtils.REST_TAG + "/jcr", "name=" + name));
            request.setEntity(entity);
            JSONMessage message = httpClient.execute(request, new JSONResponseHandler());
            JSONObject response = new JSONObject(message.getBody());
            return new ProjectLocation(
                    response.getString("uuid"),
                    response.getString("name"),
                    serviceInfo);
        } catch (AccessDeniedException e) {
            session.createUserPrompter("You do not have sufficient privileges to create a new workspace.", 
                       UserPromptType.MESSAGE, 
                       UserPromptOptions.OK, 
                       UserPromptResponse.OK, 
                       "OK", "OK").promptUser("");
            return null;
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
    }
}
