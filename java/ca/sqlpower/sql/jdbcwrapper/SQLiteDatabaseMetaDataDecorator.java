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

package ca.sqlpower.sql.jdbcwrapper;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.log4j.Logger;

import ca.sqlpower.sql.CachedRowSet;

/**
 * SQLite's meta data does not have imported or exported keys. We are returning
 * an empty result set if the keys are obtained.
 */
public class SQLiteDatabaseMetaDataDecorator extends DatabaseMetaDataDecorator {

    private static final Logger logger = Logger.getLogger(SQLiteDatabaseMetaDataDecorator.class);
    
    public SQLiteDatabaseMetaDataDecorator(DatabaseMetaData delegate, ConnectionDecorator connectionDecorator) {
		super(delegate, connectionDecorator);
	}
    
    @Override
    public ResultSet getImportedKeys(String catalog, String schema, String table)
            throws SQLException {
        Statement stmt = null;
        ResultSet rs = null;
        try {
            stmt = getConnection().createStatement();
            rs = stmt.executeQuery("select sqlite_version() where 1 = 0");
            
            CachedRowSet crs = new CachedRowSet();
            crs.populate(rs);
            return crs;
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException ex) {
                    logger.error("Failed to close result set. Squishing this exception: ", ex);
                }
            }
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException ex) {
                    logger.error("Failed to close statement. Squishing this exception: ", ex);
                }
            }
        }
    }
    
    @Override
    public ResultSet getExportedKeys(String catalog, String schema, String table)
            throws SQLException {
        Statement stmt = null;
        ResultSet rs = null;
        try {
            stmt = getConnection().createStatement();
            rs = stmt.executeQuery("select sqlite_version() where 1 = 0");
            
            CachedRowSet crs = new CachedRowSet();
            crs.populate(rs);
            return crs;
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException ex) {
                    logger.error("Failed to close result set. Squishing this exception: ", ex);
                }
            }
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException ex) {
                    logger.error("Failed to close statement. Squishing this exception: ", ex);
                }
            }
        }
    }

    @Override
    public ResultSet getIndexInfo(String catalog, String schema, String table,
            boolean unique, boolean approximate) throws SQLException {
        Statement stmt = null;
        ResultSet rs = null;
        try {
            stmt = getConnection().createStatement();
            rs = stmt.executeQuery("select sqlite_version() where 1 = 0");
            
            CachedRowSet crs = new CachedRowSet();
            crs.populate(rs);
            return crs;
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException ex) {
                    logger.error("Failed to close result set. Squishing this exception: ", ex);
                }
            }
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException ex) {
                    logger.error("Failed to close statement. Squishing this exception: ", ex);
                }
            }
        }
    }
    
    @Override
    protected ResultSetDecorator wrap (ResultSet rs) throws SQLException {	
    	return new GenericResultSetDecorator(wrap(rs.getStatement()), rs);
    }
    
    @Override
    protected StatementDecorator wrap (Statement statement) {
    	return new GenericStatementDecorator(connectionDecorator, statement);
    }
}
