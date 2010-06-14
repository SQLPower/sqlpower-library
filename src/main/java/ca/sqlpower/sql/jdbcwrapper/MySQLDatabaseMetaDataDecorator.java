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
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import ca.sqlpower.sql.CachedRowSet;
import ca.sqlpower.sql.SQL;

public class MySQLDatabaseMetaDataDecorator extends DatabaseMetaDataDecorator {

	private static final Logger logger = Logger
			.getLogger(MySQLDatabaseMetaDataDecorator.class);

	public MySQLDatabaseMetaDataDecorator(DatabaseMetaData delegate, ConnectionDecorator connectionDecorator) {
		super(delegate, connectionDecorator);
	}

	/**
	 * Overriding the getColumns method to properly quote varchar defaults as
	 * they are retrieved without quotes which prevents using them in later SQL
	 * statements.
	 */
	@Override
	public ResultSet getColumns(String catalog, String schemaPattern,
			String tableNamePattern, String columnNamePattern)
			throws SQLException {
		ResultSet rs = super.getColumns(catalog, schemaPattern,
				tableNamePattern, columnNamePattern);
		CachedRowSet crs = new CachedRowSet();
		crs.populate(rs);
		rs.close();
		while (crs.next()) {
			if (crs.getInt(5) == Types.VARCHAR && crs.getString(13) != null) {
				crs.updateString(13, SQL.quote(crs.getString(13)));
			}
		}
		crs.beforeFirst();
		return crs;
	}

	/**
	 * This wrapper has a several functions:
	 * <ul>
	 *  <li>augments the result set with an SPG_INDEX_TYPE column which contains
	 *      the native index type as a string
     *  <li>ensures the type of column 4 (NON_UNIQUE) is <code>boolean</code> (the MySQL
     *      driver was complaining when we called getBoolean(4))
     *  <li>ensures the type of column 7 (TYPE) is <code>short</code> (the MySQL
     *      driver was complaining when we called getShort(7))
	 *  <li>Name-mangles the primary key index name in the same way as our wrapper for
	 *      {@link #getPrimaryKeys(String, String, String)}
	 * </ul>
	 */
	@Override
	public ResultSet getIndexInfo(String catalog, String schema, String table,
			boolean unique, boolean approximate) throws SQLException {
		ResultSet rs = super.getIndexInfo(catalog, schema, table, unique,
				approximate);
		CachedRowSet crs = new CachedRowSet();
		crs.populate(rs, null, "SPG_INDEX_TYPE");
		rs.close();
		Map<String, String> indexTypes = getIndexType(catalog, table);
		while (crs.next()) {
			if ("true".equals(crs.getString(4))) {
				crs.updateBoolean(4, true);
			} else {
				crs.updateBoolean(4, false);
			}
			crs.updateShort(7, Short.valueOf(crs.getString(7)));
			
			if ("PRIMARY".equals(crs.getString(6))) {
			    String tableName = crs.getString(3);
			    crs.updateString(6, pkNameForTable(tableName));
			}
			
			crs.updateString("SPG_INDEX_TYPE", indexTypes.get(crs.getString(6)));

			logger.debug("Name: " + crs.getString(6));
			logger.debug("JDBC Type?: " + crs.getShort(7));
			logger.debug("Unique?: " + crs.getBoolean(4));
			logger.debug("Index Type?: " + crs.getString("SPG_INDEX_TYPE"));
		}
		crs.beforeFirst();
		return crs;
	}

	/**
	 * This uses an index name and a table name to find out the index type. The
	 * index type is returned as a map of index names to index types
	 */
	private Map<String, String> getIndexType(String catalog, String tableName)
			throws SQLException {
		Map<String, String> indexTypes = new HashMap<String, String>();
		Statement stmt = null;
		ResultSet rs = null;
		String type = "";
		String name = "";
		try {
			stmt = getConnection().createStatement();
			String sql = "SHOW INDEXES FROM " + (catalog == null ? "" : catalog + ".") + tableName;
			logger.debug("SQL statement was " + sql);
			rs = stmt.executeQuery(sql);
			while (rs.next()) {
				name = rs.getString("key_name");
				type = rs.getString("index_type");
				indexTypes.put(name, type);
			}
			return indexTypes;

		} finally {
			if (rs != null)
				rs.close();
			if (stmt != null)
				stmt.close();
		} 
	}

    /**
     * Augments the primary key info with made-up PK names based on the name of
     * the table they belong to. In InnoDB, all primary keys are called PRIMARY.
     */
	@Override
	public ResultSet getPrimaryKeys(String catalog, String schema, String table)
	        throws SQLException {
	    CachedRowSet crs = new CachedRowSet();
	    ResultSet origRS = null;
	    try {
	        origRS = super.getPrimaryKeys(catalog, schema, table);
	        crs.populate(origRS);
	    } finally {
	    	if (origRS != null) {
	    		origRS.close();
	    	}
	    }
	    
	    while (crs.next()) {
	        String tableName = crs.getString(3);
	        if ("PRIMARY".equals(crs.getString(6))) {
	            crs.updateString(6, pkNameForTable(tableName));
	        }
	    }
	    
	    crs.beforeFirst();
	    return crs;
	}
	
	private String pkNameForTable(String tableName) {
	    return tableName + "_PK";
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