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

/**
 * This collection will delegate all methods to the delegate given in the
 * constructor. Extending this collection allows you to change the behaviour of
 * the delegate for specific methods.
 * 
 * @param <T>
 */
public class DataSourceCollectionDelegate<T extends SPDataSource> implements DataSourceCollection<T> {

	private final DataSourceCollection<T> delegate;

	public DataSourceCollectionDelegate(DataSourceCollection<T> delegate) {
		this.delegate = delegate;
	}
	
	@Override
	public void addDataSource(T dbcs) {
		delegate.addDataSource(dbcs);
	}

	@Override
	public void addDataSourceType(JDBCDataSourceType dataSourceType) {
		delegate.addDataSourceType(dataSourceType);
	}

	@Override
	public void addDatabaseListChangeListener(DatabaseListChangeListener l) {
		delegate.addDatabaseListChangeListener(l);
	}

	@Override
	public void addUndoableEditListener(UndoableEditListener l) {
		delegate.addUndoableEditListener(l);
	}

	@Override
	public List<T> getConnections() {
		return delegate.getConnections();
	}

	@Override
	public <C extends T> List<C> getConnections(Class<C> classType) {
		return delegate.getConnections(classType);
	}

	@Override
	public T getDataSource(String name) {
		return delegate.getDataSource(name);
	}

	@Override
	public <C extends T> C getDataSource(String name, Class<C> classType) {
		return delegate.getDataSource(name, classType);
	}

	@Override
	public List<JDBCDataSourceType> getDataSourceTypes() {
		return delegate.getDataSourceTypes();
	}

	@Override
	public URI getMondrianServerBaseURI() {
		return delegate.getMondrianServerBaseURI();
	}

	@Override
	public UserDefinedSQLType getNewSQLType(String name, int jdbcCode) {
		return delegate.getNewSQLType(name, jdbcCode);
	}

	@Override
	public UserDefinedSQLType getSQLType(String name) {
		return delegate.getSQLType(name);
	}

	@Override
	public List<UserDefinedSQLType> getSQLTypes() {
		return delegate.getSQLTypes();
	}

	@Override
	public URI getServerBaseURI() {
		return delegate.getServerBaseURI();
	}

	@Override
	public void mergeDataSource(T dbcs) {
		delegate.mergeDataSource(dbcs);
	}

	@Override
	public void mergeDataSourceType(JDBCDataSourceType dst) {
		delegate.mergeDataSourceType(dst);
	}

	@Override
	public void read(File location) throws IOException {
		delegate.read(location);
	}

	@Override
	public void read(InputStream in) throws IOException {
		delegate.read(in);
	}

	@Override
	public void removeDataSource(T dbcs) {
		delegate.removeDataSource(null);
	}

	@Override
	public boolean removeDataSourceType(JDBCDataSourceType dataSourceType) {
		return delegate.removeDataSourceType(dataSourceType);
	}

	@Override
	public void removeDatabaseListChangeListener(DatabaseListChangeListener l) {
		delegate.removeDatabaseListChangeListener(l);
	}

	@Override
	public void removeUndoableEditListener(UndoableEditListener l) {
		delegate.removeUndoableEditListener(l);
	}

	@Override
	public void write() throws IOException {
		delegate.write();
	}

	@Override
	public void write(File location) throws IOException {
		delegate.write(location);
	}

	@Override
	public void write(OutputStream out) throws IOException {
		delegate.write(out);
	}

}
