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
 * Fixes known JDBC compatibility problems that exist in the Sybase jConnect
 * driver.
 */
public class SybaseDatabaseMetaDataDecorator extends DatabaseMetaDataDecorator {

    private static final Logger logger = Logger
    .getLogger(SybaseDatabaseMetaDataDecorator.class);
    
	public SybaseDatabaseMetaDataDecorator(DatabaseMetaData delegate, ConnectionDecorator connectionDecorator) {
		super(delegate, connectionDecorator);
        logger.debug("Creating new DatabaseMetaDataDecorator for Sybase connection");
    }

	/**
	 * The jConnect driver has a bug where it reports the primary key name as
	 * the column name. This wrapper replaces that information with TABLE_NAME_pk.
	 */
	@Override
	public ResultSet getPrimaryKeys(String catalog, String schema, String table)
	        throws SQLException {
	    ResultSet rs = null;
	    try {
	        rs = super.getPrimaryKeys(catalog, schema, table);
	        CachedRowSet crs = new CachedRowSet();
	        crs.populate(rs);
	        while (crs.next()) {
	            // rename primary key to TABLE_NAME_pk
	            crs.updateString(6, crs.getString(3) + "_pk");
	        }
	        crs.beforeFirst();
	        return crs;
	    } finally {
	        if (rs != null) {
	            try {
	                rs.close();
	            } catch (SQLException ex) {
	                logger.warn("Failed to close result set. Squishing this exception:", ex);
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
