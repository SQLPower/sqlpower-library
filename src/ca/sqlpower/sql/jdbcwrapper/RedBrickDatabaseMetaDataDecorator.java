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

package ca.sqlpower.sql.jdbcwrapper;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.log4j.Logger;

import ca.sqlpower.sql.CachedRowSet;

/**
 * Tries to ensure catalogs and schemas are understood not to be supported.
 */
public class RedBrickDatabaseMetaDataDecorator extends DatabaseMetaDataDecorator {

	private static final Logger logger = Logger
			.getLogger(RedBrickDatabaseMetaDataDecorator.class);

    /**
     * Wraps the given database metadata object. This is normally done for you
     * by the {@link RedBrickConnectionDecorator}.
     * 
     * @param delegate The database metadata object to wrap.
     */
	public RedBrickDatabaseMetaDataDecorator(DatabaseMetaData delegate) {
		super(delegate);
	}
	
	/**
	 * Returns null, because Red Brick doesn't support catalogs.
	 */
    @Override
    public String getCatalogTerm() throws SQLException {
        return null;
    }

    /**
     * Returns null, because Red Brick doesn't support schemas.
     */
    @Override
    public String getSchemaTerm() throws SQLException {
        return null;
    }

    /**
     * Returns a result set with the correct column setup for the getCatalogs()
     * method, but with 0 rows. This is in contrast to the Red Brick driver
     * we're wrapping, which throws an exception for the getCatalogs() method.
     */
    @Override
    public ResultSet getCatalogs() throws SQLException {
        Statement stmt = null;
        ResultSet rs = null;
        try {
            stmt = getConnection().createStatement();
            rs = stmt.executeQuery("SELECT 1 AS TABLE_CAT FROM rbw_tables WHERE 1 = 0");
            
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

    /**
     * Returns a result set with the correct column setup for the getSchemas()
     * method, but with 0 rows. This is in contrast to the Red Brick driver
     * we're wrapping, which throws an exception for the getSchemas() method.
     */
    @Override
    public ResultSet getSchemas() throws SQLException {
        Statement stmt = null;
        ResultSet rs = null;
        try {
            stmt = getConnection().createStatement();
            rs = stmt.executeQuery("SELECT 1 AS TABLE_SCHEM, 1 AS TABLE_CATALOG FROM rbw_tables WHERE 1 = 0");
            
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
}
