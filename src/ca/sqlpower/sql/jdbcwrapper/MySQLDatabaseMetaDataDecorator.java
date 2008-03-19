/*
 * Copyright (c) 2008, SQL Power Group Inc.
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

	public MySQLDatabaseMetaDataDecorator(DatabaseMetaData delegate) {
		super(delegate);
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
	 * This will augment the index type as a string to the ResultSet obtained
	 * from the database.
	 */
	@Override
	public ResultSet getIndexInfo(String catalog, String schema, String table,
			boolean unique, boolean approximate) throws SQLException {
		ResultSet rs = super.getIndexInfo(catalog, schema, table, unique,
				approximate);
		CachedRowSet crs = new CachedRowSet();
		crs.populate(rs, null, "INDEX_TYPE");
		rs.close();
		Map<String, String> indexTypes = getIndexType(table);
		while (crs.next()) {
			if ("true".equals(crs.getString(4))) {
				crs.updateBoolean(4, true);
			} else {
				crs.updateBoolean(4, false);
			}
			crs.updateShort(7, Short.valueOf(crs.getString(7)));
			crs.updateString("INDEX_TYPE", indexTypes.get(crs.getString(6)));

			logger.debug("Name: " + crs.getString(6));
			logger.debug("JDBC Type?: " + crs.getShort(7));
			logger.debug("Unique?: " + crs.getBoolean(4));
			logger.debug("Index Type?: " + crs.getString("INDEX_TYPE"));
		}
		crs.beforeFirst();
		return crs;
	}

	/**
	 * This uses an index name and a table name to find out the index type. The
	 * index type is returned as a map of index names to index types
	 */
	private Map<String, String> getIndexType(String tableName)
			throws SQLException {
		Map<String, String> indexTypes = new HashMap<String, String>();
		Statement stmt = null;
		ResultSet rs = null;
		String type = "OTHER";
		String name = "";
		try {
			stmt = getConnection().createStatement();
			String sql = "SHOW INDEXES FROM " + tableName;
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
}