/*
 * Copyright (c) 2009, SQL Power Group Inc.
 *
 * This file is part of Wabit.
 *
 * Wabit is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * Wabit is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>. 
 */

package ca.sqlpower.sql;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;

import ca.sqlpower.sql.DataSourceCollection;
import ca.sqlpower.sql.JDBCDataSource;
import ca.sqlpower.sql.SPDataSource;

/**
 * A class akin to {@link SPDataSource}, but for specifying the connection
 * parameters for an olap4j connection.
 * <p>
 * olap4j currently has two types of connections: Mondrian in-process, and XML/A
 * to a remote server. This data source allows either type of connection to be
 * specified.
 */
public class Olap4jDataSource extends SPDataSource {
    
    public final static String XMLA_DRIVER_CLASS_NAME = "org.olap4j.driver.xmla.XmlaOlap4jDriver";
    
    public final static String IN_PROCESS_DRIVER_CLASS_NAME = "mondrian.olap4j.MondrianOlap4jDriver";
	
	private static final String JDBC_DATA_SOURCE_NAME = "jdbcDataSourceName";
    
    private static final String MONDRIAN_SCHEMA = "mondrianSchema";
    
    private static final String XMLA_SERVER = "xmlaServer";
    
    private static final String TYPE = "type";

    public static enum Type {
        IN_PROCESS, XMLA;
    }
    
    /**
     * Creates a data source initially configured for nothing.
     */
    public Olap4jDataSource(DataSourceCollection<SPDataSource> dsCollection) {
        super(dsCollection);
    }
    
    /**
     * Creates a data source initially configured for in-process Mondrian.
     */
    public Olap4jDataSource(DataSourceCollection<SPDataSource> dsCollection, JDBCDataSource dataSource, URI mondrianSchema) {
        super(dsCollection);
        setDataSource(dataSource);
        setMondrianSchema(mondrianSchema);
        setType(Type.IN_PROCESS);
    }
    
    /**
     * Creates a data source initially configured for connection to a remote XML/A server.
     */
    public Olap4jDataSource(DataSourceCollection<SPDataSource> dsCollection, URI xmlaServer) {
        super(dsCollection);
        setXmlaServer(xmlaServer);
        setType(Type.XMLA);
    }
    
    public JDBCDataSource getDataSource() {
        final String jdbcName = get(JDBC_DATA_SOURCE_NAME);
        if (jdbcName == null) return null;
        return getParentCollection().getDataSource(jdbcName, JDBCDataSource.class);
    }
    public void setDataSource(JDBCDataSource dataSource) {
        if (dataSource == null) return;
        put(JDBC_DATA_SOURCE_NAME, dataSource.getName());
    }
    
    public URI getMondrianSchema() {
        final String uriPath = get(MONDRIAN_SCHEMA);
        if (uriPath == null || uriPath.trim().length() == 0) return null;
        try {
            return new URI(uriPath);
        } catch (URISyntaxException e) {
            throw new RuntimeException("This should not happen as it should be the same URI path passed in earlier.", e);
        }
    }
    public void setMondrianSchema(URI mondrianSchema) {
        put(MONDRIAN_SCHEMA, mondrianSchema.toString());
    }
    
    public URI getXmlaServer() {
        final String uriPath = get(XMLA_SERVER);
        if (uriPath == null || uriPath.trim().length() == 0) return null;
        try {
            return new URI(uriPath);
        } catch (URISyntaxException e) {
            throw new RuntimeException("This should not happen as it should be the same URI path passed in earlier.", e);
        }
    }
    public void setXmlaServer(URI xmlaServer) {
        try {
            put(XMLA_SERVER, xmlaServer.toURL().toExternalForm());
        } catch (MalformedURLException e) {
            // The only reason such an exception should happen is if
            // the user selected an in-process connection and the field
            // for XMLA server is blank. We can safely ignore.
        } catch (IllegalArgumentException e) {
            // The only reason such an exception should happen is if
            // the user selected an in-process connection and the field
            // for XMLA server is blank. We can safely ignore.
        }
    }

    public Type getType() {
        final String typeName = get(TYPE);
        if (typeName == null) return null;
        return Type.valueOf(typeName);
    }
    public void setType(Type type) {
        put(TYPE, type.name());
    }
    
    @Override
    public String toString() {
        return getName();
    }
}
