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

package ca.sqlpower.swingui.query;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;

import junit.framework.TestCase;
import ca.sqlpower.sql.JDBCDataSource;
import ca.sqlpower.sql.PlDotIni;
import ca.sqlpower.sql.SPDataSource;
import ca.sqlpower.sqlobject.SQLDatabase;
import ca.sqlpower.sqlobject.SQLDatabaseMapping;
import ca.sqlpower.swingui.SPSwingWorker;
import ca.sqlpower.swingui.SwingWorkerRegistry;
import ca.sqlpower.testutil.StubDataSourceCollection;

public class SQLQueryUIComponentsTest extends TestCase {
	
	private class StubSwingWorkerRegistry implements SwingWorkerRegistry {
		private final List<SPSwingWorker> workers = new ArrayList<SPSwingWorker>();
		
		public void registerSwingWorker(SPSwingWorker worker) {
			workers.add(worker);
		}
		
		public void removeSwingWorker(SPSwingWorker worker) {
			workers.remove(worker);
		}
		
		public int workerCount() {
			return workers.size();
		}
	}
	
	/**
	 * Tests if a connection is closed from outside the class that the class
	 * will reopen the connection.
	 */
	public void testClosedConnectionIsReopened() throws Exception {
	    System.out.println(System.getProperties());
		boolean isSkipped = Boolean.getBoolean("ca.sqlpower.SKIP_DODGY_TESTS");
	    if (isSkipped) {
	        System.out.println("Skipping testClosedConnectionIsReopened");
	        return;
	    } else {
	        System.out.println("Running testClosedConnectionIsReopened");
	    }
		PlDotIni plini = new PlDotIni();
		plini.read(new File("pl.regression.ini"));
		StubDataSourceCollection dsCollection = new StubDataSourceCollection();
		final SPDataSource ds = plini.getDataSource("regression_test", JDBCDataSource.class);
		dsCollection.addDataSource(ds);
		final SQLDatabase db = new SQLDatabase((JDBCDataSource) ds);
		SQLDatabaseMapping mapping = new SQLDatabaseMapping() {
			public SQLDatabase getDatabase(JDBCDataSource dataSource) {
				if (dataSource.equals(ds)) {
					return db;
				}
				return null;
			}
		};
		StubSwingWorkerRegistry swingWorkerReg = new StubSwingWorkerRegistry();
		SQLQueryUIComponents queryUIComponents = new SQLQueryUIComponents(swingWorkerReg, dsCollection, mapping, new JPanel());
		queryUIComponents.addConnection(db);
		queryUIComponents.setCurrentDataSource(ds);
		
		queryUIComponents.executeQuery("create table dummy (col1 varchar(10))");
		while (queryUIComponents.getSqlExecuteWorker() != null) {
			//wait for query to execute and return with a result.
		}
		assertFalse(queryUIComponents.getLogTextArea().getText().contains("Exception"));
		
		queryUIComponents.closeConMap();
		
		queryUIComponents.executeQuery("create table dummy2 (col1 varchar(10))");
		while (queryUIComponents.getSqlExecuteWorker() != null) {
			//wait for query to execute and return with a result.
		}
		System.out.println(queryUIComponents.getLogTextArea().getText());
		assertFalse(queryUIComponents.getLogTextArea().getText().contains("Exception"));
	}
}
