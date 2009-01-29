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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.event.UndoableEditListener;

public class StubDataSourceCollection implements DataSourceCollection {
	
	private final List<SPDataSource> dataSources = new ArrayList<SPDataSource>();
	private final List<SPDataSourceType> dsTypes = new ArrayList<SPDataSourceType>();
	private final List<UndoableEditListener> undoableEdits = new ArrayList<UndoableEditListener>();

	public void addDataSource(SPDataSource dbcs) {
		dataSources.add(dbcs);
	}

	public void addDataSourceType(SPDataSourceType dataSourceType) {
		dsTypes.add(dataSourceType);
	}

	public void addDatabaseListChangeListener(DatabaseListChangeListener l) {
		throw new UnsupportedOperationException("Unsupported in the current stub implementation");
	}

	public void addUndoableEditListener(UndoableEditListener l) {
		undoableEdits.add(l);
	}

	public List<SPDataSource> getConnections() {
		return Collections.unmodifiableList(dataSources);
	}

	public SPDataSource getDataSource(String name) {
		for (SPDataSource ds : dataSources) {
			if (ds.getName().equals(name)) {
				return ds;
			}
		}
		return null;
	}

	public List<SPDataSourceType> getDataSourceTypes() {
		return Collections.unmodifiableList(dsTypes);
	}

	public void mergeDataSource(SPDataSource dbcs) {
		dataSources.add(dbcs);
	}

	public void mergeDataSourceType(SPDataSourceType dst) {
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

	public boolean removeDataSourceType(SPDataSourceType dataSourceType) {
		return dsTypes.remove(dataSourceType);
	}

	public void removeDatabaseListChangeListener(DatabaseListChangeListener l) {
		throw new UnsupportedOperationException("Unsupported in the current stub implementation");
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

}
