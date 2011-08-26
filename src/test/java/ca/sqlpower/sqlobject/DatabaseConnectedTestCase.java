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

package ca.sqlpower.sqlobject;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import junit.framework.TestCase;
import ca.sqlpower.sql.DataSourceCollection;
import ca.sqlpower.sql.JDBCDataSource;
import ca.sqlpower.sql.PlDotIni;
import ca.sqlpower.sql.SPDataSource;

/**
 * This is the test class to extend if your test class needs a connection to the
 * HSQLDB testing database. The setUp method creates a connection to the
 * database, which will be fresh and empty for every test, and the tearDown
 * method shuts it down (which wipes out that database).
 */
public abstract class DatabaseConnectedTestCase extends TestCase {

    private DataSourceCollection<SPDataSource> plini = new PlDotIni();
    protected SQLDatabase db;
    
    /**
     * If set to false the database will not be setup and left as null.
     * This is for products that don't connect to databases. (Mainly our
     * XBRL stuff.)
     */
    protected final boolean setupDB;
    
    public DatabaseConnectedTestCase() {
        this("", true);
    }

    public DatabaseConnectedTestCase(String name, boolean setupDB) {
        super(name);
		this.setupDB = setupDB;
    }
    
    public DatabaseConnectedTestCase(String name) {
        this(name, true);
    }
    
    /**
     * Looks up and returns an SPDataSource that represents the testing
     * database. Uses a PL.INI file located in the current working directory, 
     * called "pl.regression.ini" and creates a connection to the database called
     * "regression_test".
     * <p>
     * You are welcome to override this method in order to provide your own pl.ini
     * and/or your own data source to the {@link #setUp()} method.
     * 
     * <p>FIXME: Need to parameterise this so that we can test each supported
     * database platform!
     * @throws IOException 
     * @throws SQLObjectException 
     */
    protected JDBCDataSource getJDBCDataSource() throws IOException {
        plini.read(new File("pl.regression.ini"));
    	return plini.getDataSource("regression_test", JDBCDataSource.class);
    }
    
    /**
     * Sets up the instance variable <code>db</code>, which will be a SQLDatabase
     * connected to the data source returned by {@link #getJDBCDataSource()}.
     */
    @Override
    protected void setUp() throws Exception {
    	if (setupDB) {
    		db = new SQLDatabase(new JDBCDataSource(getJDBCDataSource()));
    		db.setParent(new StubSQLObject());
    		assertNotNull(db.getDataSource().getParentType());
    	} else {
    		db = null;
    		plini = null;
    	}
    }
    
    @Override
    protected void tearDown() throws Exception {
    	if (setupDB) {
    		try {
    			sqlx("SHUTDOWN");
    			db.disconnect();
    			db = null;
    		} catch (Exception ex) {
    			System.err.println("Shutdown failed. Test case probably modified the database connection! Retrying...");
    			DataSourceCollection<SPDataSource> dscol = new PlDotIni();
    			dscol.read(new File("pl.regression.ini"));
    			db.setDataSource(dscol.getDataSource("regression_test", JDBCDataSource.class));
    			sqlx("SHUTDOWN");
    			db.disconnect();
    			db = null;
    		}
    	}
    }

    public SQLDatabase getDb() {
        return new SQLDatabase(new JDBCDataSource(db.getDataSource()));
    }
    
    /**
     * Returns the data source collection that the current connection was obtained
     * from. If you have not overridden the {@link #getJDBCDataSource()} method, this
     * will be the data source collection specified by the file pl.regression.ini
     * in the current working directory. If you have overridden that method, this
     * method will return whatever plini value you set up in your override (which will
     * be null if you didn't set the {@link #plini} variable).
     */
    public DataSourceCollection<SPDataSource> getPLIni() {
        return plini;
    }

    /**
     * Executes the given SQL statement in {@link #db}.
     * 
     * @param sql
     *            The SQL statement to execute. It should be an "update" type
     *            statement (that is, one that does not return a result set).
     * @return The "row count" returned by the database.
     * @throws SQLException If the statement fails.
     * @throws SQLObjectException If getting the connection from {@link #db} fails.
     */
    protected int sqlx(String sql) throws SQLException, SQLObjectException {
        Connection con = null;
        Statement stmt = null;
        try {
            con = db.getConnection();
            stmt = con.createStatement();
            return stmt.executeUpdate(sql);
        } catch (SQLException ex) {
            System.err.println("Got SQL Exception when executing: " + sql);
            throw ex;
        } finally {
            try {
                if (stmt != null) stmt.close();
            } catch (SQLException ex) {
                System.err.println("Failed to close statement; squishing this exception:");
                ex.printStackTrace();
            }
            try {
                if (con != null) con.close();
            } catch (SQLException ex) {
                System.err.println("Failed to close connection; squishing this exception:");
                ex.printStackTrace();
            }
        }
    }
    

}
