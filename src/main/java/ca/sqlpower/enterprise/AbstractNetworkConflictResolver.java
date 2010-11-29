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

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.springframework.security.AccessDeniedException;

import ca.sqlpower.dao.PersistedSPOProperty;
import ca.sqlpower.dao.PersistedSPObject;
import ca.sqlpower.dao.RemovedObjectEntry;
import ca.sqlpower.dao.SPPersistenceException;
import ca.sqlpower.dao.SPPersister.DataType;
import ca.sqlpower.dao.SPPersisterListener;
import ca.sqlpower.dao.json.SPJSONMessageDecoder;
import ca.sqlpower.dao.session.SessionPersisterSuperConverter;
import ca.sqlpower.enterprise.client.ProjectLocation;
import ca.sqlpower.object.SPObject;
import ca.sqlpower.sqlobject.SQLObject;
import ca.sqlpower.util.RunnableDispatcher;
import ca.sqlpower.util.SQLPowerUtils;
import ca.sqlpower.util.UserPrompter.UserPromptOptions;
import ca.sqlpower.util.UserPrompter.UserPromptResponse;
import ca.sqlpower.util.UserPrompterFactory;
import ca.sqlpower.util.UserPrompterFactory.UserPromptType;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;

public abstract class AbstractNetworkConflictResolver extends Thread {


	/**
	 * A class that will take a ConflictCase and parameters insert
	 * them into the ConflictCase's message.
	 */
	protected class ConflictMessage {
	    
	    private final ConflictCase conflict;
	    private final String message;
	    private final List<String> objectIds = new LinkedList<String>();
	    private final List<String> objectNames = new LinkedList<String>();
	    
	    /**
	     * Create a conflict message using the ConflictCase's message
	     * and String.format() to put the given arguments in
	     * @param conflict
	     * @param uuidsAndNames A list of the relevant object ids and names, in pairs.
	     * ie: "id1", "table1", "id2", "table2"
	     */
	    public ConflictMessage(ConflictCase conflict, String ... uuidsAndNames) {
	        this.conflict = conflict;
	        for (int i = 0; i < uuidsAndNames.length; i += 2) {
	            objectIds.add(uuidsAndNames[i]);
	            objectNames.add(uuidsAndNames[i+1]);
	        }
	        if (objectIds.size() != conflict.numArgs()) {
	            throw new IllegalArgumentException(
	                "Number of arguments passed in does not match number requested by conflict type");
	        }
	        try {
	            message = String.format(conflict.message, objectNames.toArray());
	        } catch (Throwable t) {
	            throw new RuntimeException(t);
	        }
	    }
	    
	    /**
	     * This constructor is used for custom messages.
	     * Will not call String.format().
	     * @param message
	     * @param conflict
	     * @param uuids
	     */
	    public ConflictMessage(String message, ConflictCase conflict, String ... uuids) {            
	        this.message = message;
	        this.conflict = conflict;
	        objectIds.addAll(Arrays.asList(uuids));
	    }
	    
	    public String getObjectId(int index) {
	        return objectIds.get(index);
	    }
	    
	    public ConflictCase getConflictCase() {
	        return conflict;
	    }
	    
	    public String getMessage() {
	        return message;
	    }
	    
	    public String toString() {
	        return message;
	    }
	    
	}

    
    /**
     * Defines conflict cases as well as messages for each.
     */
    protected static enum ConflictCase {
        NO_CONFLICT ("There were no conflicts"),
        
        ADDITION_UNDER_REMOVAL ("The %s you tried adding could not be added because its ancestor was removed"),
        
        MOVE_OF_REMOVED ("Could not move %s because it was removed"),
        
        CHANGE_OF_REMOVED ("Could not change %s because it was removed"),
        
        SIMULTANEOUS_ADDITION ("Could not add %s because a sibling was added/removed"),
        
        ADDITION_UNDER_CHANGE ("Could not add %s because its parent %s was modified"),
        
        CHANGE_AFTER_ADDITION ("Could not change %s because child %s was added"),       
        
        CHANGE_UNDER_CHANGE ("Could not change %s because its parent/child %s was modified"),        
        
        REMOVAL_OF_DEPENDENCY ("Could not remove %s because another object %s is now dependent on it"),
        
        SIMULTANEOUS_OBJECT_CHANGE ("Could not change %s because it was changed by another user"),
        
        DIFFERENT_MOVE ("Could not move %s because it was moved somewhere else"),
        
        SPECIAL_CASE ("");
        
        private final String message;
        
        ConflictCase(String s) {
            message = s;
        }
        
        /**
         * Returns the number of parameters the enum expects in its message.
         */
        public int numArgs() {
            return message.length() - message.replace("%s", "1").length();
        }
    }
    
	public static interface UpdateListener {
	    /**
	     * Fired when an update from the server has been performed on the client
	     * @param resolver The NetworkConflictResolver that received the update 
	     * @return true if the listener should be removed from
	     * listener list and should not receive any more calls
	     */
	    public boolean updatePerformed(AbstractNetworkConflictResolver resolver);
	
	    /**
	     * Called when an exception is thrown from the server on an update. One
	     * special exception passed in this call is the
	     * {@link AccessDeniedException}. If the exception is an
	     * {@link AccessDeniedException} then the user does not have valid
	     * permissions to do the operation they were attempting and the
	     * exception is not a fatal one.
	     */
	    public boolean updateException(AbstractNetworkConflictResolver resolver, Throwable t);
	    
	    /**
	     * Notifies listeners that the workspace was deleted.
	     * Swing sessions should listen for this to disable the enterprise session.
	     * The listener is removed after this method is called.
	     */
	    public void workspaceDeleted();
	
	    /**
	     * Called just before an update will be performed by the
	     * {@link ArchitectNetworkConflictResolver}. This gives objects the chance to be
	     * aware of incoming changes from the server if necessary.
	     * 
	     * @param resolver
	     *            The {@link ArchitectNetworkConflictResolver} that received the
	     *            update.
	     */
	    public void preUpdatePerformed(AbstractNetworkConflictResolver resolver);
	}
	
	/**
     * If conflicts are found when trying to decide if an incoming change
     * conflicts with the last user change only this many conflicting properties
     * or reasons will be displayed at max to prevent the dialog from growing
     * too large.
     */
    protected static final int MAX_CONFLICTS_TO_DISPLAY = 10;

    /**
     * This is currently a static average wait time for how long each change to
     * the server will take. This will let the progress bar update at a decent
     * rate.
     */
    protected static final int AVG_WAIT_TIME_FOR_PERSIST = 12;
    
    private static final Logger logger = Logger.getLogger(AbstractNetworkConflictResolver.class);
    protected AtomicBoolean postingJSON = new AtomicBoolean(false);
    protected boolean updating = false;
    
    protected SPPersisterListener listener;
    protected SessionPersisterSuperConverter converter;
    
    protected UserPrompterFactory upf;
    
    protected int currentRevision = 0;
    protected long serverTimestamp = 0;
    
    protected long retryDelay = 1000;

    /**
     * This double will store and be updated with the average wait time for each
     * persist calls so the progress of the progress bar is on average correct.
     * At some point we may want the server to respond with the actual progress
     * but this is a start.
     */
    protected double currentWaitPerPersist = AVG_WAIT_TIME_FOR_PERSIST;
    
    protected final SPJSONMessageDecoder jsonDecoder;

    protected final ProjectLocation projectLocation;
    protected final HttpClient outboundHttpClient;
    protected final HttpClient inboundHttpClient;
    
    protected String contextRelativePath;

    protected volatile boolean cancelled;

    protected JSONArray messageBuffer = new JSONArray();
    
    protected HashMap<String, PersistedSPObject> inboundObjectsToAdd = new HashMap<String, PersistedSPObject>();
    protected Multimap<String, PersistedSPOProperty> inboundPropertiesToChange = LinkedListMultimap.create();
    protected HashMap<String, RemovedObjectEntry> inboundObjectsToRemove = new HashMap<String, RemovedObjectEntry>();
    
    protected Map<String, PersistedSPObject> outboundObjectsToAdd = new LinkedHashMap<String, PersistedSPObject>();
    protected Multimap<String, PersistedSPOProperty> outboundPropertiesToChange = LinkedListMultimap.create();
    protected Map<String, RemovedObjectEntry> outboundObjectsToRemove = new LinkedHashMap<String, RemovedObjectEntry>();    
    
    protected List<UpdateListener> updateListeners = new ArrayList<UpdateListener>();

	private final RunnableDispatcher runnable;
	
	public AbstractNetworkConflictResolver(
            ProjectLocation projectLocation, 
            SPJSONMessageDecoder jsonDecoder, 
            HttpClient inboundHttpClient, 
            HttpClient outboundHttpClient,
            RunnableDispatcher runnable) 
    {
        super("updater-" + projectLocation.getUUID());
        
        this.jsonDecoder = jsonDecoder;
        this.projectLocation = projectLocation;
        this.inboundHttpClient = inboundHttpClient;
        this.outboundHttpClient = outboundHttpClient;
		this.runnable = runnable;
        
        contextRelativePath = "/" + ClientSideSessionUtils.REST_TAG + "/project/" + projectLocation.getUUID();
    }
    
    public int getRevision() {
        return currentRevision;
    }
    
    public long getServerTimestamp() {
    	return serverTimestamp;
    }
    
    public void addListener(UpdateListener listener) {
        updateListeners.add(listener);
    }
    
    protected String getPersistedObjectName(PersistedSPObject o) {
        for (PersistedSPOProperty p : outboundPropertiesToChange.get(o.getUUID())) {
            if (p.getPropertyName().equals("name")) {
                return (String) p.getNewValue();
            }
        }
        throw new IllegalArgumentException("Persisted Object with UUID " + o.getUUID() + " and type " + o.getType() + " has no name property");
    }

    public void clear() {
        clear(false);
    }
    
    protected void clear(boolean reflush) {
        messageBuffer = new JSONArray();
        
        if (reflush) {
            inboundObjectsToAdd.clear();
            inboundPropertiesToChange.clear();  // XXX does this cause lists to retain old objects?
            inboundObjectsToRemove.clear();
            
            outboundObjectsToAdd.clear();
            outboundPropertiesToChange.clear();
            outboundObjectsToRemove.clear();
        }
    }
    
    public void send(JSONObject content) throws SPPersistenceException {
        messageBuffer.put(content);
    }
    
    public void setUserPrompterFactory(UserPrompterFactory promptSession) {
        this.upf = promptSession;
    }
    
    public UserPrompterFactory getUserPrompterFactory() {
        return upf;
    }
    
    public void setListener(SPPersisterListener listener) {
        this.listener = listener;
    }

    public void setConverter(SessionPersisterSuperConverter converter) {
        this.converter = converter;
    }
    
    public List<UpdateListener> getListeners() {
        return updateListeners;
    }
    
    public void flush() {
        flush(false);
    }

    /**
     * Exists for code reuse.
     * 
     * @param tokener
     *            {@link JSONTokener} that tokenizes multiple persister calls.
     * @param newRevision
     *            The new revision number.
     * @throws SPPersistenceException
     */
    protected void decodeMessage(JSONTokener tokener, int newRevision, long timestamp) {
        try {
            if (currentRevision < newRevision) {
                List<UpdateListener> updateListenersCopy = new ArrayList<UpdateListener>(updateListeners);
                for (UpdateListener listener : updateListeners) {
                    listener.preUpdatePerformed(AbstractNetworkConflictResolver.this);
                }
                // Now we can apply the update ...
                jsonDecoder.decode(tokener);
                currentRevision = newRevision;
                serverTimestamp = timestamp;
                
                if (logger.isDebugEnabled())
                	logger.debug("Setting currentRevision to: " + currentRevision + 
                				" and serverTimestamp to: " + serverTimestamp);
                
                for (UpdateListener listener : updateListenersCopy) {
                    if (listener.updatePerformed(this)) {
                        updateListeners.remove(listener);
                    }
                }
            } 
        } catch (Exception e) {
            throw new RuntimeException("Failed to decode the message from the server.", e);
        }
    }
    
    protected void fillOutboundPersistedLists() {
        for (PersistedSPObject obj : listener.getPersistedObjects()) {
            outboundObjectsToAdd.put(obj.getUUID(), obj);
        }
        for (PersistedSPOProperty prop : listener.getPersistedProperties()) {
            outboundPropertiesToChange.put(prop.getUUID(), prop);
        }
        for (RemovedObjectEntry rem : listener.getObjectsToRemove().values()) {
            outboundObjectsToRemove.put(rem.getRemovedChild().getUUID(), rem);
        }
    }
    
    public void interrupt() {
        super.interrupt();
        cancelled = true;
    }
    
    @Override
    public void run() {
        try {
            while (!this.isInterrupted() && !cancelled) {
               try { 
                   
                   while (updating) { // this should wait for persisting to server as well.
                       synchronized (this) {
                           wait();
                       }
                   }                   
                   updating = true;                   
                   // Request an update from the server using the current revision number.                   
                   JSONMessage message = getJsonArray(inboundHttpClient);
                   
                   // Status 410 (Gone) means the workspace was deleted                   
                   if (message.getStatusCode() == 410) {
                       for (UpdateListener listener : updateListeners) {
                           listener.workspaceDeleted();                           
                       }
                       updateListeners.clear();
                       interrupt();
                   } else if (message.getStatusCode() == 412) { //Precondition failed
                	   upf.createUserPrompter(message.getBody(), UserPromptType.MESSAGE, UserPromptOptions.OK, 
                			   UserPromptResponse.OK, null, "OK").promptUser();
                	   continue;
                   } else if (message.getStatusCode() == 403) { // FORBIDDEN, timestamp is older than server
            		   updateListeners.clear();
            		   interrupt();
                	   if (projectLocation.getUUID().equals("system")) {
                		   upf.createUserPrompter("Server at " + projectLocation.getServiceInfo().getServerAddress() + "has failed since your session began." +
                    			   " Please restart the program to synchronize the system workspace with the server." , 
                    			   UserPromptType.MESSAGE, 
                    			   UserPromptOptions.OK, 
                    			   UserPromptResponse.OK, 
                    			   null, "OK").promptUser();                		   
                	   } else {
                		   upf.createUserPrompter("Server at "  + projectLocation.getServiceInfo().getServerAddress() + 
                				   " has failed since your session began." +
                				   " Please use the refresh button to synchronize workspace " + projectLocation.getName() + 
                				   " with the server.", 
                				   UserPromptType.MESSAGE, 
                				   UserPromptOptions.OK, 
                				   UserPromptResponse.OK, 
                				   null, "OK").promptUser();
                	   }
                   }
                   
                   // The updater may have been interrupted/closed/deleted while waiting for an update.
                   if (this.isInterrupted() || cancelled) break;
                   
                   JSONObject json = new JSONObject(message.getBody());
                   final JSONTokener tokener = new JSONTokener(json.getString("data"));
                   final int jsonRevision = json.getInt("currentRevision");
                   final long jsonTimestamp = json.getLong("serverTimestamp");
                   
                   runnable.runInForeground(new Runnable() {
                       public void run() {
                           try {
                               if (!postingJSON.get()) {
                                   decodeMessage(tokener, jsonRevision, jsonTimestamp);
                               }
                           } catch (AccessDeniedException e) {
                               interrupt();
                               List<UpdateListener> listenersToRemove = new ArrayList<UpdateListener>();
                               for (UpdateListener listener : updateListeners) {
                                   if (listener.updateException(AbstractNetworkConflictResolver.this, e)) {
                                       listenersToRemove.add(listener);
                                   }
                               }
                               updateListeners.removeAll(listenersToRemove);
                               if (upf != null) {
                                   upf.createUserPrompter(
                                           "You do not have sufficient privileges to perform that action. " +
                                           "Please hit the refresh button to synchronize with the server.", 
                                           UserPromptType.MESSAGE, 
                                           UserPromptOptions.OK, 
                                           UserPromptResponse.OK, 
                                           "OK", "OK").promptUser("");
                               } else {
                                   throw e;
                               }
                           } catch (Exception e) {
                               // TODO: Discard corrupt workspace and start again from scratch.
                               interrupt();
                               List<UpdateListener> listenersToRemove = new ArrayList<UpdateListener>();
                               for (UpdateListener listener : updateListeners) {
                                   if (listener.updateException(AbstractNetworkConflictResolver.this, e)) {
                                       listenersToRemove.add(listener);
                                   }
                               }
                               updateListeners.removeAll(listenersToRemove);
                               throw new RuntimeException("Update from server failed! Unable to decode the message: ", e);
                           } finally {
                               synchronized (AbstractNetworkConflictResolver.this) {
                                   updating = false;
                                   AbstractNetworkConflictResolver.this.notify();
                               }
                           }
                       }
                   });
               } catch (Exception ex) {
            	   Throwable root = ex;
            	   while (root != null) {
            		   if (root instanceof SPPersistenceException) {
            			   getUserPrompterFactory().createUserPrompter(
            					   "An exception occurred while updating from the server. See logs for more details.", 
            					   UserPromptType.MESSAGE, UserPromptOptions.OK, UserPromptResponse.OK, true, "OK").promptUser();
            			   break;
            		   }
            		   root = root.getCause();
            	   }
                   logger.error("Failed to contact server. Will retry in " + retryDelay + " ms.", ex);
                   Thread.sleep(retryDelay);
               }
            }
        } catch (InterruptedException ex) {
            logger.info("Updater thread exiting normally due to interruption.");
        }
        
        inboundHttpClient.getConnectionManager().shutdown();
    }
    
    /**
     * Creates and executes an HttpGet request for an update from the server.
     * @return A JSONMessage holding the successfulness and message body of the server's response
     */
    protected JSONMessage getJsonArray(HttpClient client) {
        try {
            URI uri = new URI("http", null, 
                    projectLocation.getServiceInfo().getServerAddress(), 
                    projectLocation.getServiceInfo().getPort(),
                    projectLocation.getServiceInfo().getPath() + contextRelativePath, 
                    "oldRevisionNo=" + currentRevision + "&serverTimestamp=" + serverTimestamp, null);
            logger.debug("GETting URI: " + uri.toString());
            HttpUriRequest request = new HttpGet(uri);
            return client.execute(request, new JSONResponseHandler());
        } catch (AccessDeniedException ade) {
            throw new AccessDeniedException("Access Denied");
        } catch (Exception ex) {
            throw new RuntimeException("Unable to get json from server", ex);
        }
    }
    
    /**
     * Creates and executes an HttpPost request containing the json of whatever
     * transaction was completed last.
     * @param jsonArray Typically created by calling toString() on a JSONArray
     * @return A JSONMessage holding the successfulness and message body of the server's response
     */
    protected JSONMessage postJsonArray(String jsonArray) {
        try {
            URI serverURI = new URI("http", null, 
                    projectLocation.getServiceInfo().getServerAddress(), 
                    projectLocation.getServiceInfo().getPort(),
                    projectLocation.getServiceInfo().getPath() + 
                    "/" + ClientSideSessionUtils.REST_TAG + "/project/" + projectLocation.getUUID(), 
                    "currentRevision=" + currentRevision + "&serverTimestamp=" + serverTimestamp, null);
            logger.debug("POSTing URI: " + serverURI.toString());
            HttpPost postRequest = new HttpPost(serverURI);
            postRequest.setEntity(new StringEntity(jsonArray)); 
            postRequest.setHeader("Content-Type", "application/json");
            HttpUriRequest request = postRequest;
            return outboundHttpClient.execute(request, new JSONResponseHandler());
        } catch (AccessDeniedException ade) {
            throw ade;
        } catch (Exception ex) {
            throw new RuntimeException("Unable to post json to server", ex);
        }
    }
    
    protected void fillInboundPersistedLists(String json) {
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                
                if (obj.getString("method").equals("persistObject")) {
                    
                    String parentUUID = obj.getString("parentUUID");
                    String type = obj.getString("type");
                    String uuid = obj.getString("uuid");
                    int index = obj.getInt("index");
                    
                    inboundObjectsToAdd.put(uuid, new PersistedSPObject(parentUUID, type, uuid, index));
                    
                } else if (obj.getString("method").equals("persistProperty")) {
                    
                    String uuid = obj.getString("uuid");
                    String propertyName = obj.getString("propertyName");
                    DataType type = DataType.valueOf(obj.getString("type"));
                    Object oldValue = null;
                    try {
                        oldValue = SPJSONMessageDecoder.getWithType(obj, type, "oldValue");
                    } catch (Exception e) {}
                    Object newValue = SPJSONMessageDecoder.getWithType(obj, type, "newValue");
                    boolean unconditional = false;
                    
                    PersistedSPOProperty property = new PersistedSPOProperty(uuid, propertyName, type, oldValue, newValue, unconditional);
                    
                    if (inboundPropertiesToChange.keySet().contains(uuid)) {
                        inboundPropertiesToChange.asMap().get(uuid).add(property);
                    } else {
                        inboundPropertiesToChange.put(uuid, property);
                    }
                    
                } else if (obj.getString("method").equals("removeObject")) {
                    
                    String parentUUID = obj.getString("parentUUID");
                    String uuid = obj.getString("uuid");
                    
                    SPObject objectToRemove = SQLPowerUtils.findByUuid(getWorkspace(), uuid, SPObject.class);

                    inboundObjectsToRemove.put(uuid, new RemovedObjectEntry(parentUUID, objectToRemove, 
                            objectToRemove.getParent().getChildren().indexOf(objectToRemove)));
                }
            }
        } catch (Exception ex) {
            throw new RuntimeException("Unable to create persisted lists: ", ex);
        }
    }

    /**
     * Goes through all the inbound and outbound change lists and
     * determines whether the outbound changes should be allowed to continue.
     * The reasons to prevent the outbound changes are usually cases where
     * as a result of the incoming change, the outbound change would not be
     * possible through the UI anymore, and/or are impossible in such a state.
     * 
     * See ConflictCase for all the cases that are looked for in this method.
     * A Google Docs spreadsheet called Conflict rules has been shared
     * with the psc group. For more information, see that.
     */
    protected List<ConflictMessage> checkForSimultaneousEdit() {                        
        
        List<ConflictMessage> conflicts = new LinkedList<ConflictMessage>();
        
        Set<String> inboundAddedObjectParents = new HashSet<String>();
        Set<String> inboundRemovedObjectParents = new HashSet<String>();
        
        Set<String> inboundChangedObjects = new HashSet<String>();
        HashMap<String, String> inboundCreatedDependencies = new HashMap<String, String>();
        
        Set<String> duplicateMoves = new HashSet<String>();
        

        // ----- Populate the inbound sets / maps -----        
        
        for (String uuid : inboundPropertiesToChange.keys()) {
            inboundChangedObjects.add(uuid);
            for (PersistedSPOProperty p : inboundPropertiesToChange.get(uuid)) {
                if (p.getDataType() == DataType.REFERENCE) {
                    inboundCreatedDependencies.put((String) p.getNewValue(), p.getUUID()); 
                }
            }
        }
        
        for (PersistedSPObject o : inboundObjectsToAdd.values()) {
            inboundAddedObjectParents.add(o.getParentUUID());
        }      
        
        for (RemovedObjectEntry o : inboundObjectsToRemove.values()) {          
            inboundRemovedObjectParents.add(o.getParentUUID());
        }
        
        // ----- Iterate through outbound additions -----
        
        Set<String> checkedIfCanAddToTree = new HashSet<String>();        
        Iterator<PersistedSPObject> addedObjects = outboundObjectsToAdd.values().iterator();
        while (addedObjects.hasNext()) {
            PersistedSPObject o = addedObjects.next();            
            
            // Can't add object to a parent that already had a child added or removed.
            // This will also include incoming and/or outgoing moves, which are conflicts too.
            if (inboundAddedObjectParents.contains(o.getParentUUID()) || 
                    inboundRemovedObjectParents.contains(o.getParentUUID())) {              
                conflicts.add(new ConflictMessage(ConflictCase.SIMULTANEOUS_ADDITION, 
                        o.getUUID(), getPersistedObjectName(o)));
            }
            
            // Can't add an object if the direct parent was changed.
            if (inboundChangedObjects.contains(o.getParentUUID())) {
                conflicts.add(new ConflictMessage(ConflictCase.ADDITION_UNDER_CHANGE, 
                        o.getUUID(), getPersistedObjectName(o), 
                        o.getParentUUID(), SQLPowerUtils.findByUuid(getWorkspace(), o.getParentUUID(), SPObject.class).getName()));
            }
            
            // Make sure we are not adding an object that had an ancestor removed.
            // First iterate up ancestors that are being added in the same transaction.
            PersistedSPObject highestAddition = o;
            while (outboundObjectsToAdd.containsKey(highestAddition.getParentUUID()) &&
                    !checkedIfCanAddToTree.contains(highestAddition.getParentUUID())) {
                checkedIfCanAddToTree.add(highestAddition.getUUID());
                highestAddition = outboundObjectsToAdd.get(highestAddition.getParentUUID());                
            }
            checkedIfCanAddToTree.add(highestAddition.getUUID());
            if (checkedIfCanAddToTree.add(highestAddition.getParentUUID()) &&
            		SQLPowerUtils.findByUuid(getWorkspace(),highestAddition.getParentUUID(), SPObject.class) == null) {
                conflicts.add(new ConflictMessage(ConflictCase.ADDITION_UNDER_REMOVAL, 
                        highestAddition.getUUID(), getPersistedObjectName(highestAddition)));
            }
            
            // Check if both clients are adding the same object.
            // It could mean they both undid a deletion of this object,
            // or are both trying to move the same object.
            // If they are identical, remove the outbound add from this list.
            // If it was a move and has a corresponding remove call, that
            // must be taken care of in the following outbound removals loop.
            if (inboundObjectsToAdd.containsKey(o.getUUID())) {
                if (inboundObjectsToAdd.get(o.getUUID()).equals(o)) {
                    addedObjects.remove();
                    outboundPropertiesToChange.removeAll(o.getUUID());
                    duplicateMoves.add(o.getUUID());
                } else {
                    conflicts.add(new ConflictMessage(ConflictCase.DIFFERENT_MOVE, 
                            o.getUUID(), getPersistedObjectName(o)));
                }
            }                             
        }
        
        
        // ----- Iterate through outbound removals -----
             
        Iterator<RemovedObjectEntry> removedObjects = outboundObjectsToRemove.values().iterator();        
        while (removedObjects.hasNext()) {
            RemovedObjectEntry object = removedObjects.next();
            final String uuid = object.getRemovedChild().getUUID();
            
            // Check if the object the outbound client is trying to remove does not exist.
            SPObject removedObject = SQLPowerUtils.findByUuid(getWorkspace(), uuid, SPObject.class);
            if (removedObject == null) {
                // Check if this remove has a corresponding add, meaning it is a move.
                // The incoming remove will override the outgoing move.
                if (outboundObjectsToAdd.containsKey(uuid)) {
                    conflicts.add(new ConflictMessage(ConflictCase.MOVE_OF_REMOVED, 
                            object.getRemovedChild().getUUID(), object.getRemovedChild().getName()));
                } else {
                    // Both clients removed the same object, either directly or indirectly.
                    removedObjects.remove();
                }
            } else if (inboundCreatedDependencies.containsKey(uuid)) {
                // Can't remove an object that was just made a dependency
                String uuidOfDependent = inboundCreatedDependencies.get(uuid);
                conflicts.add(new ConflictMessage(ConflictCase.REMOVAL_OF_DEPENDENCY, 
                        uuid, removedObject.getName(),
                        uuidOfDependent, SQLPowerUtils.findByUuid(getWorkspace(), uuid, SPObject.class).getName()));
            } else if (duplicateMoves.contains(uuid)) {
                removedObjects.remove();
            }
            
        }   
        
        
        // ----- Iterate through outbound properties -----
        
        for (String uuid : outboundPropertiesToChange.keys()) {            
            SPObject changedObject = SQLPowerUtils.findByUuid(getWorkspace(),uuid, SPObject.class);            
            
            // If this object is being newly added, the rest of the loop body does not matter.
            if (outboundObjectsToAdd.containsKey(uuid)) continue;
            
            // Cannot change a property on an object that no longer exists (due to inbound removal).
            if (changedObject == null) {
                conflicts.add(new ConflictMessage(ConflictCase.CHANGE_OF_REMOVED, uuid, uuid));
                continue;
            }
            
            // Cannot change the property of an object whose direct parent was also changed.
            if (changedObject.getParent() != null && 
                    inboundChangedObjects.contains(changedObject.getParent().getUUID())) {
                conflicts.add(new ConflictMessage(ConflictCase.CHANGE_UNDER_CHANGE, 
                        uuid, changedObject.getName(),
                        changedObject.getParent().getUUID(), changedObject.getParent().getName()));
            }
            
            // You cannot change the property of an object that had a property already changed,
            // unless any and all property changes are identical, in which case the duplicate
            // property changes will be removed from the outgoing list.
                        
            if (inboundChangedObjects.contains(uuid)) {                
                ConflictMessage message = new ConflictMessage(ConflictCase.SIMULTANEOUS_OBJECT_CHANGE, 
                        uuid, SQLPowerUtils.findByUuid(getWorkspace(),uuid, SPObject.class).getName());
                
                HashMap<String, Object> inboundPropertiesMap = 
                    new HashMap<String, Object>();                
                for (PersistedSPOProperty p : inboundPropertiesToChange.get(uuid)) {
                    inboundPropertiesMap.put(p.getPropertyName(), p.getNewValue());
                }
                                
                Iterator<PersistedSPOProperty> properties = outboundPropertiesToChange.get(uuid).iterator();                
                while (properties.hasNext()) {
                    PersistedSPOProperty p = properties.next();
                    // Check if there is a corresponding inbound property.
                    // If not, this is a conflict since there are non-identical properties.
                    if (inboundPropertiesMap.containsKey(p.getPropertyName())) {
                        if (inboundPropertiesMap.get(p.getPropertyName()).equals(p.getNewValue())) {
                            properties.remove();
                        } else {
                            conflicts.add(message);
                            break;
                        }
                    } else {
                        conflicts.add(message);
                        break;
                    }
                }
            }
            
            // Cannot change the property of a parent whose direct child was either:
            List<SPObject> children = new ArrayList<SPObject>();
            if (changedObject instanceof SQLObject) {
            	children.addAll(((SQLObject) changedObject).getChildrenWithoutPopulating());
            } else {
            	children.addAll(changedObject.getChildren());
            }
            for (SPObject child : children) {                                        
                // also changed
                if (inboundChangedObjects.contains(child.getUUID())) {
                    conflicts.add(new ConflictMessage(ConflictCase.CHANGE_UNDER_CHANGE,
                            uuid, changedObject.getName(),
                            child.getUUID(), child.getName()));                    
                }
                
                // or just added (moved is okay, though).
                if (inboundObjectsToAdd.containsKey(child.getUUID()) &&
                        !inboundObjectsToRemove.containsKey(child.getUUID())){
                    conflicts.add(new ConflictMessage(ConflictCase.CHANGE_AFTER_ADDITION,
                            uuid, changedObject.getName(),
                            child.getUUID(), child.getName()));
                }
            }
        }
        return conflicts;
    }
    
    protected abstract void flush(boolean reflush);
    protected abstract List<ConflictMessage> detectConflicts();
    protected abstract SPObject getWorkspace();

	/**
	 * Returns the persister listener used to send changes to the server. This
	 * is mainly used for testing.
	 */
    public SPPersisterListener getPersisterListener() {
    	return listener;
    }
}
