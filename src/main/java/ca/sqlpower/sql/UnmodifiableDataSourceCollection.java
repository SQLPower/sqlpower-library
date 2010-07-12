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

package ca.sqlpower.sql;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.List;

import javax.swing.event.UndoableEditListener;

import ca.sqlpower.sqlobject.UserDefinedSQLType;

public class UnmodifiableDataSourceCollection<T extends SPDataSource> implements
		DataSourceCollection<T> {

	private final DataSourceCollection<T> delegate;

	public UnmodifiableDataSourceCollection(DataSourceCollection<T> delegateCollection) {
		this.delegate = delegateCollection;
	}
	
	public void addDataSource(T dbcs) {
		//no-op
	}

	public void addDataSourceType(JDBCDataSourceType dataSourceType) {
		//no-op
	}

	public void addDatabaseListChangeListener(DatabaseListChangeListener l) {
		delegate.addDatabaseListChangeListener(l);
	}

	public void addUndoableEditListener(UndoableEditListener l) {
		delegate.addUndoableEditListener(l);
	}

	public List<T> getConnections() {
		return delegate.getConnections();
	}

	public <C extends T> List<C> getConnections(Class<C> classType) {
		return delegate.getConnections(classType);
	}

	public T getDataSource(String name) {
		return delegate.getDataSource(name);
	}

	public <C extends T> C getDataSource(String name, Class<C> classType) {
		return delegate.getDataSource(name, classType);
	}

	public List<JDBCDataSourceType> getDataSourceTypes() {
		return delegate.getDataSourceTypes();
	}

	public URI getMondrianServerBaseURI() {
		return delegate.getMondrianServerBaseURI();
	}

	public UserDefinedSQLType getNewSQLType(String name, int jdbcCode) {
		return delegate.getNewSQLType(name, jdbcCode);
	}

	public UserDefinedSQLType getSQLType(String name) {
		return delegate.getSQLType(name);
	}

	public List<UserDefinedSQLType> getSQLTypes() {
		return delegate.getSQLTypes();
	}

	public URI getServerBaseURI() {
		return delegate.getServerBaseURI();
	}

	public void mergeDataSource(T dbcs) {
		// no-op
	}

	public void mergeDataSourceType(JDBCDataSourceType dst) {
		// no-op
	}

	public void read(File location) throws IOException {
		// no-op
	}

	public void read(InputStream in) throws IOException {
		// no-op
	}

	public void removeDataSource(T dbcs) {
		// no-op
	}

	public boolean removeDataSourceType(JDBCDataSourceType dataSourceType) {
		// no-op
		return getDataSourceTypes().contains(dataSourceType);
	}

	public void removeDatabaseListChangeListener(DatabaseListChangeListener l) {
		delegate.removeDatabaseListChangeListener(l);
	}

	public void removeUndoableEditListener(UndoableEditListener l) {
		delegate.removeUndoableEditListener(l);
	}

	public void write() throws IOException {
		// no-op
	}

	public void write(File location) throws IOException {
		// no-op
	}

	public void write(OutputStream out) throws IOException {
		// no-op
	}

}
