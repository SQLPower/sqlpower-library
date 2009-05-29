/*
 * Copyright (c) 2008, SQL Power Group Inc.
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
package ca.sqlpower.sql;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.List;

import javax.swing.event.UndoableEditListener;

public interface DataSourceCollection<T extends SPDataSource> {

    public static final String DOS_CR_LF = "\r\n";

    /**
     * Reads the PL.INI file at the given location into a new fileSections list.
     * Also updates the fileTime.
     */
    public void read(File location) throws IOException;

    /**
     * Reads a PL.INI-style data stream into a new fileSections list.
     * Does not update the fileTime, or start a thread to reload the file
     * (since there isn't a file that we know of).
     */
    public void read(InputStream in) throws IOException;

    /**
     * Writes out the file as {@link #write(File)} does, using the
     * same location as the last file that was successfully read or
     * written using this instance.
     * 
     * @throws IOException if the file can't be written.
     * @throws IllegalStateException if the file to save to can't be determined. 
     */
    public void write() throws IOException;
    
    /**
     * Works the same as {@link #write(OutputStream)}, but writes the data to a file.
     *
     * @param location The location to write to.
     * @throws IOException if the location is not writeable for any reason.
     */
    public void write(File location) throws IOException;

    /**
     * Writes out the entire contents of this DataSourceCollection in a format
     * that can be understood by {@link #read(InputStream)}.
     * 
     * @param out The output stream to write to.
     * @throws IOException if writing fails
     */
    public void write(OutputStream out) throws IOException;
    
    /**
     * Searches the list of connections for one with the given name.
     *
     * @param name The Logical datbabase name to look for.
     * @return the first SPDataSource in the file whose name matches the
     * given name, or null if no such datasource exists.
     */
    public T getDataSource(String name);

    /**
     * Searches the list of connections for one with the given name. The data
     * source will be of the given type as well.
     * 
     * @param name
     *            The Logical datbabase name to look for.
     * @param classType
     *            The type of {@link SPDataSource} being searched for.
     * @return the first SPDataSource in the file whose name matches the given
     *         name, or null if no such datasource exists.
     */
    public <C extends T> C getDataSource(String name, Class<C> classType);

    /**
     * @return a sorted List of all the data sources in this pl.ini.
     */
    public List<T> getConnections();
    
    public <C extends T> List<C> getConnections(Class<C> classType);

    public String toString();

    /**
     * Adds a new data source to the end of this file's list of sections.
     * Fires an add event.
     *
     * @param dbcs The new data source to add
     */
    public void addDataSource(T dbcs);

    /**
     * Make sure an SPDataSource is in the master list; either copy its properties
     * to one with the same name found in the list, OR, add it to the list.
     * Matching is performed by logical name and is case insensitive.  If the data
     * source is added (rather than updated), there will be an add event.
     * @param dbcs
     */
    public void mergeDataSource(T dbcs);

    public void removeDataSource(T dbcs);

    /**
     * Returns an unmodifiable list of all data source types in this
     * collection of data sources.
     */
    public List<JDBCDataSourceType> getDataSourceTypes();

    /**
     * Returns the base URI that server: type jar specifications are resolved
     * against. May be set to null if server-based JAR lookup is not in use.
     */
    public URI getServerBaseURI();
    
    /**
     * Adds the new data source type to this collection.  See also
     * {@link #mergeDataSourceType(JDBCDataSourceType)}
     * for a method that can update an existing entry.
     */
    void addDataSourceType(JDBCDataSourceType dataSourceType);

    /**
     * Removes the given data source type from this collection.
     * If the given type is not in this collection, does nothing.
     * 
     * @return True if the item was present in the list (and hence
     * it was removed).  False otherwise.
     */
    boolean removeDataSourceType(JDBCDataSourceType dataSourceType);

    /**
     * Adds or updates the given data source type properties in this data source collection.
     * Matching is performed by name and is case insensitive.
     * @param dst
     */
    public void mergeDataSourceType(JDBCDataSourceType dst);
    
    public void addDatabaseListChangeListener(DatabaseListChangeListener l);

    public void removeDatabaseListChangeListener(DatabaseListChangeListener l);
    
    public void addUndoableEditListener(UndoableEditListener l);
    
    public void removeUndoableEditListener(UndoableEditListener l);


}