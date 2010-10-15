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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import ca.sqlpower.enterprise.client.ProjectLocation;
import ca.sqlpower.sql.DataSourceCollection;
import ca.sqlpower.sql.DatabaseListChangeEvent;
import ca.sqlpower.sql.DatabaseListChangeListener;
import ca.sqlpower.sql.JDBCDataSource;
import ca.sqlpower.sql.JDBCDataSourceType;
import ca.sqlpower.sql.Olap4jDataSource;
import ca.sqlpower.sql.PlDotIni;
import ca.sqlpower.sql.SPDataSource;

public abstract class DataSourceCollectionUpdater implements DatabaseListChangeListener, PropertyChangeListener, UndoableEditListener {
	
	protected final ProjectLocation projectLocation;

	
	/**
	 * If true this updater is currently posting properties to the server. If
	 * properties are being posted to the server and an event comes in because
	 * of a change during posting the updater should not try to repost the message
	 * it is currently trying to post.
	 */
	protected boolean postingProperties = false;

	protected final ResponseHandler<Void> responseHandler = new ResponseHandler<Void>() {
        public Void handleResponse(HttpResponse response)
        throws ClientProtocolException, IOException {
            if (response.getStatusLine().getStatusCode() != 200) {
                throw new ClientProtocolException(
                        "Failed to create/update data source on server. Reason:\n" +
                        EntityUtils.toString(response.getEntity()));
            } else {
                return null;
            }
        }
    };
    
	public DataSourceCollectionUpdater(ProjectLocation projectLocation) {
		this.projectLocation = projectLocation;
	}
	
	public abstract HttpClient getHttpClient();
	
    public void attach(DataSourceCollection<JDBCDataSource> dsCollection) {
        dsCollection.addDatabaseListChangeListener(this);
        dsCollection.addUndoableEditListener(this);
        
        for (JDBCDataSourceType jdst : dsCollection.getDataSourceTypes()) {
            jdst.addPropertyChangeListener(this);
        }
        
        for (SPDataSource ds : dsCollection.getConnections()) {
            ds.addPropertyChangeListener(this);
        }
    }
    
    public void detach(DataSourceCollection<JDBCDataSource> dsCollection) {
        dsCollection.removeDatabaseListChangeListener(this);
        dsCollection.removeUndoableEditListener(this);
        
        for (JDBCDataSourceType jdst : dsCollection.getDataSourceTypes()) {
            jdst.removePropertyChangeListener(this);
        }
        
        for (SPDataSource ds : dsCollection.getConnections()) {
            ds.removePropertyChangeListener(this);
        }
    }

    /**
     * Handles the addition of a new database entry, relaying its current
     * state to the server. Also begins listening to the new data source as
     * would have happened if the new data source existed before
     * {@link #attach(DataSourceCollection)} was invoked.
     */
    public void databaseAdded(DatabaseListChangeEvent e) {
        SPDataSource source = e.getDataSource();
        source.addPropertyChangeListener(this);
        
        List<NameValuePair> properties = new ArrayList<NameValuePair>();
        for (Map.Entry<String, String> ent : source.getPropertiesMap().entrySet()) {
            properties.add(new BasicNameValuePair(ent.getKey(), ent.getValue()));
        }
        
        databaseAdded(e, source, properties);
    }
    
    public void databaseAdded(DatabaseListChangeEvent e, SPDataSource source, List<NameValuePair> properties) {
        if (source instanceof JDBCDataSource) {
            postJDBCDataSourceProperties((JDBCDataSource) source, properties);
        }
    }
    
    /**
     * Handles deleting of a database entry by requesting that the server
     * deletes it. Also unlistens to the data source to prevent memory
     * leaks.
     */
    @Override
    public void databaseRemoved(DatabaseListChangeEvent e) {
    	HttpClient httpClient = getHttpClient();
        try {
            SPDataSource removedDS = e.getDataSource();
            HttpDelete request = new HttpDelete(jdbcDataSourceURI(removedDS));
			httpClient.execute(request, responseHandler);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
    }
    
    protected URI jdbcDataSourceTypeURI(JDBCDataSourceType jdst) throws URISyntaxException {
        return ClientSideSessionUtils.getServerURI(projectLocation.getServiceInfo(),
                "/" + ClientSideSessionUtils.REST_TAG + "/data-sources/type/" + jdst.getName());
    }
    
    protected URI jdbcDataSourceURI(SPDataSource jds) throws URISyntaxException {
        if (!(jds instanceof JDBCDataSource)) throw new IllegalStateException("DataSource must be an instance of JDBCDataSource");
        
        return ClientSideSessionUtils.getServerURI(projectLocation.getServiceInfo(),
                "/" + ClientSideSessionUtils.REST_TAG + "/data-sources/JDBCDataSource/" + jds.getName());
    }

    protected void postJDBCDataSourceProperties(JDBCDataSource ds,
            List<NameValuePair> properties) {
    	if (postingProperties) return;
    	
        HttpClient httpClient = getHttpClient();
        try {
            URI jdbcDataSourceURI = jdbcDataSourceURI(ds);
            try {
                HttpPost request = new HttpPost(jdbcDataSourceURI);
                request.setEntity(new UrlEncodedFormEntity(properties));
                httpClient.execute(request, responseHandler);
            } catch (IOException ex) {
                throw new RuntimeException("Server request failed at " + jdbcDataSourceURI, ex);
            }
        } catch (URISyntaxException ex) {
            throw new RuntimeException(ex);
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
    }

    protected void postJDBCDataSourceTypeProperties(JDBCDataSourceType jdst,
            List<NameValuePair> properties) {
        if (postingProperties) return;
        
        HttpClient httpClient = getHttpClient();
        try {
            HttpPost request = new HttpPost(jdbcDataSourceTypeURI(jdst));
            request.setEntity(new UrlEncodedFormEntity(properties));
            httpClient.execute(request, responseHandler);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
    }

    public void undoableEditHappened(UndoableEditEvent e) {
        if (e.getEdit() instanceof PlDotIni.AddDSTypeUndoableEdit) {
            JDBCDataSourceType jdst = ((PlDotIni.AddDSTypeUndoableEdit) e.getEdit()).getType();
            jdst.addPropertyChangeListener(this);
            
            List<NameValuePair> properties = new ArrayList<NameValuePair>();
            for (String name : jdst.getPropertyNames()) {
                properties.add(new BasicNameValuePair(name, jdst.getProperty(name)));
            }
            
            postJDBCDataSourceTypeProperties(jdst, properties);
        }
        
        if (e.getEdit() instanceof PlDotIni.RemoveDSTypeUndoableEdit) {
            JDBCDataSourceType jdst = ((PlDotIni.RemoveDSTypeUndoableEdit) e.getEdit()).getType();
            jdst.removePropertyChangeListener(this);
            
            removeJDBCDataSourceType(jdst);
        }
    }
    
    public void removeJDBCDataSourceType(JDBCDataSourceType jdst) {
        HttpClient httpClient = getHttpClient();
        try {
            HttpDelete request = new HttpDelete(jdbcDataSourceTypeURI(jdst));
            httpClient.execute(request, responseHandler);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
    }

    /**
     * Handles changes to individual data sources by relaying their new
     * state to the server.
     * <p>
     * <b>Implementation note:</b> Presently, all properties for the data
     * source are sent back to the server every time one of them changes.
     * This is not the desired behaviour, but without rethinking the
     * SPDataSource event system, there is little else we can do: the
     * property change events tell us JavaBeans property names, but in order
     * to send incremental updates, we's need to know the pl.ini property
     * key names.
     * 
     * @param evt
     *            The event describing the change. Its source must be the
     *            data source object which was modified.
     */
    public void propertyChange(PropertyChangeEvent evt) {
        // Updating all properties is less than ideal, but a property change event does
        // not tell us what the "pl.ini" key for the property is.

        Object source = evt.getSource();
        
        if (source instanceof SPDataSource) {
            SPDataSource ds = (SPDataSource) source;
            ds.addPropertyChangeListener(this);
            
            List<NameValuePair> properties = new ArrayList<NameValuePair>();
            for (Map.Entry<String, String> ent : ds.getPropertiesMap().entrySet()) {
                properties.add(new BasicNameValuePair(ent.getKey(), ent.getValue()));
            }
            
            propertyChange(evt, ds, properties);
        }
        
        if (source instanceof JDBCDataSourceType) {
            JDBCDataSourceType jdst = (JDBCDataSourceType) source;
            jdst.addPropertyChangeListener(this);
            
            List<NameValuePair> properties = new ArrayList<NameValuePair>();
            for (String name : jdst.getPropertyNames()) {
                properties.add(new BasicNameValuePair(name, jdst.getProperty(name)));
            }
            
            postJDBCDataSourceTypeProperties(jdst, properties);
        }
    }
    
    public void propertyChange(PropertyChangeEvent evt, SPDataSource ds, List<NameValuePair> properties) {
        
    	if (ds instanceof JDBCDataSource) {
            postJDBCDataSourceProperties((JDBCDataSource) ds, properties);
    	}
    }
}