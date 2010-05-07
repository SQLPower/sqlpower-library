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

package ca.sqlpower.testutil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.event.UndoableEditListener;

import ca.sqlpower.sql.DataSourceCollection;
import ca.sqlpower.sql.DatabaseListChangeListener;
import ca.sqlpower.sql.JDBCDataSourceType;
import ca.sqlpower.sql.SPDataSource;
import ca.sqlpower.sqlobject.UserDefinedSQLType;

public class StubDataSourceCollection<T extends SPDataSource> implements DataSourceCollection<T> {
	
	private final List<T> dataSources = new ArrayList<T>();
	private final List<JDBCDataSourceType> dsTypes = new ArrayList<JDBCDataSourceType>();
	private final List<UndoableEditListener> undoableEdits = new ArrayList<UndoableEditListener>();
	private final List<DatabaseListChangeListener> dbListChangeListeners = new ArrayList<DatabaseListChangeListener>();
    private URI serverBaseURI;

	public void addDataSource(T dbcs) {
		dataSources.add(dbcs);
	}

	public void addDataSourceType(JDBCDataSourceType dataSourceType) {
		dsTypes.add(dataSourceType);
	}

	public void addDatabaseListChangeListener(DatabaseListChangeListener l) {
		dbListChangeListeners.add(l);
	}

	public void addUndoableEditListener(UndoableEditListener l) {
		undoableEdits.add(l);
	}

	public List<T> getConnections() {
		return Collections.unmodifiableList(dataSources);
	}

    public <C extends T> C getDataSource(String name,
            Class<C> classType) {
        for (SPDataSource ds : dataSources) {
            if (ds.getName().equals(name) && classType.isInstance(ds)) {
                return classType.cast(ds);
            }
        }
        return null;
    }

	public List<JDBCDataSourceType> getDataSourceTypes() {
		return Collections.unmodifiableList(dsTypes);
	}

	public void mergeDataSource(T dbcs) {
		dataSources.add(dbcs);
	}

	public void mergeDataSourceType(JDBCDataSourceType dst) {
		dsTypes.add(dst);
	}

	public void read(File location) throws IOException {
		throw new UnsupportedOperationException("Unsupported in the current stub implementation");
	}

	public void read(InputStream in) throws IOException {
		throw new UnsupportedOperationException("Unsupported in the current stub implementation");
	}

	public void removeDataSource(SPDataSource dbcs) {
		dataSources.remove(dbcs);
	}

	public boolean removeDataSourceType(JDBCDataSourceType dataSourceType) {
		return dsTypes.remove(dataSourceType);
	}

	public void removeDatabaseListChangeListener(DatabaseListChangeListener l) {
		dbListChangeListeners.remove(l);
	}

	public void removeUndoableEditListener(UndoableEditListener l) {
		undoableEdits.remove(l);
	}

	public void write() throws IOException {
		throw new UnsupportedOperationException("Unsupported in the current stub implementation");
	}

	public void write(File location) throws IOException {
		throw new UnsupportedOperationException("Unsupported in the current stub implementation");
	}

	public void write(OutputStream out) throws IOException {
        throw new UnsupportedOperationException("Unsupported in the current stub implementation");
	}

    public URI getServerBaseURI() {
        return serverBaseURI;
    }

    public void setServerBaseURI(URI serverBaseURI) {
        this.serverBaseURI = serverBaseURI;
    }

    public <C extends T> List<C> getConnections(Class<C> classType) {
        List<C> matchingDataSources = new ArrayList<C>();
        for (T ds : dataSources) {
            if (classType.isInstance(ds)) {
                matchingDataSources.add(classType.cast(ds));
            }
        }
        return matchingDataSources;
    }

    public T getDataSource(String name) {
        for (T ds : dataSources) {
            if (ds.getName().equals(name)) {
                return ds;
            }
        }
        return null;
    }
    
    public URI getMondrianServerBaseURI() {
    	return null;
    }

	public UserDefinedSQLType getSQLType(String name) {
		// TODO Auto-generated method stub
		return null;
	}

	public List<UserDefinedSQLType> getSQLTypes() {
		// TODO Auto-generated method stub
		return null;
	}
	
	public UserDefinedSQLType getNewSQLType(String name, int jdbcCode) {
		// TODO Auto-generated method stub
		return null;
	}
}
