/*
 * Copyright (c) 2007, SQL Power Group Inc.
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
/*
 * Created on Jun 8, 2005
 *
 * This code belongs to SQL Power Group Inc.
 */
package ca.sqlpower.sql.jdbcwrapper;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import ca.sqlpower.sql.CachedRowSet;
import ca.sqlpower.sql.SQL;

/**
 * The PostgresDatabaseMetaDataDecorator makes the following changes to driver
 * behaviour:
 * 
 * <ul>
 *  <li>suppresses the list of databases which the Postgres driver
 *      reports existing, but does not allow access to.
 *  <li>removes quotation marks around index column names that happen
 *      to be keywords.
 * </ul>
 * 
 * @version $Id$
 */
public class PostgresDatabaseMetaDataDecorator extends
		DatabaseMetaDataDecorator {

	private static final Logger logger = Logger
			.getLogger(PostgresDatabaseMetaDataDecorator.class);

	/** XXX Make a Connections Panel extention to let you set this kind of thing. */
	public static final int UGLY_DEFAULT_VARCHAR_SIZE = 1024;
	private static final int DIGITS_IN_INT8 = 20;
	private static final int DIGITS_IN_INT4 = 10;
	private static final int DIGITS_IN_FLOAT4 = 38;
	private static final int DIGITS_IN_FLOAT8 = 308;

	/**
	 * Creates a new facade for PostgreSQL's DatabaseMetaData.
	 */
	public PostgresDatabaseMetaDataDecorator(DatabaseMetaData delegate) {
		super(delegate);
	}

	/**
	 * The Postgres JDBC driver is able to return each Catalog Name, but ignores
	 * requests to view schemas that belong to a Catalog other than the one 
	 * you are connected to and instead always returns the same set of schenms.
	 * 
	 * To minimize confusion, only return the Catalog for the database you are 
	 * connected to.  Use a sqlpower CachedRowSet to ensure resources are freed 
	 * up once the query has been run.
	 */
	public ResultSet getCatalogs() throws java.sql.SQLException {
		// if the connection string had a catalog name, it will be set
		String theCatalog = getConnection().getCatalog();
		// if not, the the catalog name is the user name
		if (theCatalog == null || theCatalog.length() == 0) {
			theCatalog = getUserName();
		}
		Statement st = null;
		ResultSet rs = null;
		CachedRowSet crs = new CachedRowSet();
		st = getConnection().createStatement();
		rs = st.executeQuery("SELECT '" + theCatalog + "'");
		crs.populate(rs);
		rs.close();
		st.close();
		return crs;
	}

	/** Compensates for unlimited length varchar (which is otherwise reported as VARCHAR(0)
	 * by returning a large limit for column_length.
	 * This will also make sure that serial columns are set to auto_increment
	 */
	@Override
	public ResultSet getColumns(String catalog, String schemaPattern,
			String tableNamePattern, String columnNamePattern)
			throws SQLException {
		ResultSet rs = super.getColumns(catalog, schemaPattern,
				tableNamePattern, columnNamePattern);
		CachedRowSet crs = new CachedRowSet();

		boolean fudgeAutoInc;
		if (rs.getMetaData().getColumnCount() <= 22) {
			crs.populate(rs, null, "IS_AUTOINCREMENT");
			fudgeAutoInc = true;
		} else {
			// must be JDBC 4 or newer
			crs.populate(rs);
			fudgeAutoInc = false;
		}

		// This will throw SQLException if someone uses a driver that returns
		// a >22-column result set which does not include an IS_AUTOINCREMENT column.
		// If that ever happens, we should update the above assumption to compensate.
		int autoIncColNum = crs.findColumn("IS_AUTOINCREMENT");

		rs.close();

		while (crs.next()) {
			if (crs.getInt(5) == Types.VARCHAR && crs.getInt(7) <= 0) {
				crs.updateInt(7, UGLY_DEFAULT_VARCHAR_SIZE);
			} else if ("int4".equalsIgnoreCase(crs.getString(6))) {
				crs.updateInt(7, DIGITS_IN_INT4);
			} else if ("int8".equalsIgnoreCase(crs.getString(6))) {
				crs.updateInt(7, DIGITS_IN_INT8);
			} else if ("float4".equalsIgnoreCase(crs.getString(6))) {
				crs.updateInt(7, DIGITS_IN_FLOAT4);
			} else if ("float8".equalsIgnoreCase(crs.getString(6))) {
				crs.updateInt(7, DIGITS_IN_FLOAT8);
			} else if ("bool".equalsIgnoreCase(crs.getString(6))) {
				crs.updateInt(5, Types.BOOLEAN);
			}
			if (fudgeAutoInc) {
				if ("serial".equalsIgnoreCase(crs.getString(6))) {
					crs.updateString(autoIncColNum, "YES");
				} else {
					crs.updateString(autoIncColNum, "NO");
				}
			}
		}
		crs.beforeFirst();
		return crs;
	}

	/**
	 * Strips off double quotes surrounding column names. (The driver quotes column
	 * names that are SQL keywords).
	 */
	@Override
	public ResultSet getIndexInfo(String catalog, String schema, String table,
			boolean unique, boolean approximate) throws SQLException {
		ResultSet rs = super.getIndexInfo(catalog, schema, table, unique,
				approximate);
		CachedRowSet crs = new CachedRowSet();
		crs.populate(rs, null, "SPG_INDEX_TYPE");
		rs.close();
		Map<String, String> indexTypes = new HashMap<String, String>();
		indexTypes = getIndexType(table);
		Pattern p = Pattern.compile("^\"(.*)\"$");
		while (crs.next()) {
			String colName = crs.getString(9);
			Matcher m = p.matcher(colName);
			if (colName != null && m.matches()) {
				crs.updateString(9, m.group(1));
			}
			crs.updateShort(7, Short.valueOf(crs.getString(7)));
			logger.debug("crs.getString(6) is returning " + crs.getString(6));
			logger.debug("Setting index type to " + indexTypes.get(crs.getString(6)));
			logger.debug("JDBC Type?: " + crs.getShort(7));
			crs.updateString("SPG_INDEX_TYPE", indexTypes.get(crs.getString(6)).toUpperCase());
		}
		crs.beforeFirst();
		return crs;
	}

	/**
	 * This uses an index name and a table name to find out the index type. The
	 * index type is returned as a map of Index name and index types
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
			String sql = "SELECT i.tablename as table, "
					+ "i.indexname, am.amname as indextype "
					+ "from pg_indexes i left join pg_class ci "
					+ "on i.indexname = ci.relname " + "left join pg_am am "
					+ "on ci.relam = am.oid " + "where i.tablename="
					+ SQL.quote(tableName);

			logger.debug("SQL statement was " + sql);
			rs = stmt.executeQuery(sql);
			while (rs.next()) {
				name = rs.getString("indexname");
				type = rs.getString("indextype");
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
