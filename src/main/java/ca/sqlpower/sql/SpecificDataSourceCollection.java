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

package ca.sqlpower.sql;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.List;

import javax.swing.event.UndoableEditListener;

import ca.sqlpower.sqlobject.UserDefinedSQLType;

public class SpecificDataSourceCollection<T extends SPDataSource> implements DataSourceCollection<T> {
    
    private final DataSourceCollection<? super T> delegate;
    private final Class<T> classType;

    public SpecificDataSourceCollection(DataSourceCollection<? super T> delegate, Class<T> classType) {
        this.delegate = delegate;
        this.classType = classType;
    }

    public void addDataSource(T dbcs) {
        delegate.addDataSource(dbcs);
    }

    public void addDataSourceType(JDBCDataSourceType dataSourceType) {
        delegate.addDataSourceType(dataSourceType);
    }

    public void addDatabaseListChangeListener(DatabaseListChangeListener l) {
        delegate.addDatabaseListChangeListener(l);
    }

    public void addUndoableEditListener(UndoableEditListener l) {
        delegate.addUndoableEditListener(l);
    }

    public List<T> getConnections() {
        return delegate.getConnections(classType);
    }

    public <C extends T> List<C> getConnections(Class<C> classType) {
        return delegate.getConnections(classType);
    }

    public T getDataSource(String name) {
        return delegate.getDataSource(name, classType);
    }

    public <C extends T> C getDataSource(String name,
            Class<C> classType) {
        return delegate.getDataSource(name, classType);
    }

    public List<JDBCDataSourceType> getDataSourceTypes() {
        return delegate.getDataSourceTypes();
    }

    public URI getServerBaseURI() {
        return delegate.getServerBaseURI();
    }

    public void mergeDataSource(T dbcs) {
        delegate.mergeDataSource(dbcs);
    }

    public void mergeDataSourceType(JDBCDataSourceType dst) {
        delegate.mergeDataSourceType(dst);
    }

    public void read(File location) throws IOException {
        delegate.read(location);
    }

    public void read(InputStream in) throws IOException {
        delegate.read(in);
    }

    public void removeDataSource(T dbcs) {
        delegate.removeDataSource(dbcs);
    }

    public boolean removeDataSourceType(JDBCDataSourceType dataSourceType) {
        return delegate.removeDataSourceType(dataSourceType);
    }

    public void removeDatabaseListChangeListener(DatabaseListChangeListener l) {
        delegate.removeDatabaseListChangeListener(l);
    }

    public void removeUndoableEditListener(UndoableEditListener l) {
        delegate.removeUndoableEditListener(l);
    }

    public void write() throws IOException {
        delegate.write();
    }

    public void write(File location) throws IOException {
        delegate.write(location);
    }

    public void write(OutputStream out) throws IOException {
        delegate.write(out);
    }

	public URI getMondrianServerBaseURI() {
		return delegate.getMondrianServerBaseURI();
	}

	public UserDefinedSQLType getSQLType(String name) {
		return delegate.getSQLType(name);
	}

	public List<UserDefinedSQLType> getSQLTypes() {
		return delegate.getSQLTypes();
	}

	public UserDefinedSQLType getNewSQLType(String name, int jdbcCode) {
		return delegate.getNewSQLType(name, jdbcCode);
	}

}
