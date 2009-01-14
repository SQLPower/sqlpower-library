/*
 * Copyright (c) 2009, SQL Power Group Inc.
 * 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in
 *       the documentation and/or other materials provided with the
 *       distribution.
 *     * Neither the name of SQL Power Group Inc. nor the names of its
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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
