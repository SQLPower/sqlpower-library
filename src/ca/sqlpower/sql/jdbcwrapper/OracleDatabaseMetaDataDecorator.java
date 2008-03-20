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
package ca.sqlpower.sql.jdbcwrapper;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import ca.sqlpower.sql.CachedRowSet;
import ca.sqlpower.sql.RowFilter;
import ca.sqlpower.sql.SQL;

/**
 * Decorate an Oracle Connection to handle the evil error "ORA-1722" on getTypeMap() when
 * the user is using an Oracle 10 driver on Oracle 8i. 
 */
public class OracleDatabaseMetaDataDecorator extends DatabaseMetaDataDecorator {
	
	private static final Logger logger = Logger
	.getLogger(OracleDatabaseMetaDataDecorator.class);


    /**
     * If true, this decorator's getTables method will omit tables in the
     * recycle bin. The "recycle bin" concept was introduced in Oracle 10. It
     * boils down to the DROP TABLE command just renaming the table to a long
     * randomly-generated name that starts with BIN$.  The table is only dropped
     * for real when a user purges the recycle bin, or the tablespace fills up
     * (in which case the oldest recycle bin table is dropped).
     */
    private boolean hidingRecycleBinTables = true;
    
	public OracleDatabaseMetaDataDecorator(DatabaseMetaData delegate) {
		super(delegate);
	}
	
	private static final int DREADED_ORACLE_ERROR_CODE_1722 = 1722;
	
	private static final String ORACLE_1722_MESSAGE =
		"Encountered Oracle error ORA-1722. This normally means that you are using the " +
		"Oracle 10 driver with Oracle 8. Please check your driver settings.";

    private static final int DREADED_ORACLE_ERROR_CODE_1031 = 1031;
    
    private static final String ORACLE_1031_MESSAGE =
        "Encountered Oracle error ORA-1031. This normally means that you are accessing " +
        "Indices without having the 'analyze any' permission.";
    
    
	@Override
	public ResultSet getTypeInfo() throws SQLException {
		try {
			return super.getTypeInfo();
		} catch (SQLException e) {
			if (e.getErrorCode() == DREADED_ORACLE_ERROR_CODE_1722) {
				SQLException newE = new SQLException(ORACLE_1722_MESSAGE);
				newE.setNextException(e);
				throw newE;
			} else {
				throw e;
			}
		}
	}
	
	/**
	 * This is used to properly reverse engineer index types from the database.
	 */
	@Override
	public ResultSet getIndexInfo(String catalog, String schema, String table,
			boolean unique, boolean approximate) throws SQLException {
		try {
			ResultSet rs = super.getIndexInfo(catalog, schema, table, unique,
					approximate);
			CachedRowSet crs = new CachedRowSet();
			crs.populate(rs, null, "SPG_INDEX_TYPE");
			rs.close();
			Map<String, String> indexTypes = new HashMap<String, String>();
			indexTypes = getIndexType(table);
			while (crs.next()) {
				crs.updateShort(7, Short.valueOf(crs.getString(7)));
				//Oracle is stupid...never represent a boolean as a 0 or 1 of type Long
				if (crs.getLong(4) == 0) {
					crs.updateBoolean(4, false);
				} else if (crs.getLong(4) == 1) {
					crs.updateBoolean(4, true);
				}
				if (crs.getString(6) == null
						|| indexTypes.get(crs.getString(6)) == null)
					continue;
				logger.debug("crs.getString(6) is returning "
						+ crs.getString(6));
				logger.debug("Setting index type to "
						+ indexTypes.get(crs.getString(6)));
				logger.debug("JDBC Type?: " + crs.getShort(7));
				if (indexTypes.get(crs.getString(6)).toUpperCase().equals(
						"FUNCTION-BASED NORMAL")) {
					crs.updateString("SPG_INDEX_TYPE", "FUNCTION-BASED");
				} else if (indexTypes.get(crs.getString(6)).toUpperCase()
						.equals("NORMAL")) {
					crs.updateString("SPG_INDEX_TYPE", "BTREE");
				} else {
					crs.updateString("SPG_INDEX_TYPE", indexTypes.get(
							crs.getString(6)).toUpperCase());					
				}
			}
			crs.beforeFirst();
			return crs;
		} catch (SQLException e) {
			if (e.getErrorCode() == DREADED_ORACLE_ERROR_CODE_1031) {
				SQLException newE = new SQLException(ORACLE_1031_MESSAGE);
				newE.setNextException(e);
				throw newE;
			} else {
				throw e;
			}
		}
    }
    
    @Override
    public ResultSet getTables(String catalog, String schemaPattern, String tableNamePattern, String[] types) throws SQLException {
        ResultSet rs = super.getTables(catalog, schemaPattern, tableNamePattern, types);
        CachedRowSet crs = new CachedRowSet();

        RowFilter noRecycledTablesFilter = null;
        
        if (hidingRecycleBinTables) {
            noRecycledTablesFilter = new RowFilter() {
                public boolean acceptsRow(Object[] row) {
                    String tableName = (String) row[2];
                    return !tableName.startsWith("BIN$");
                }
            };
        }
        
        crs.populate(rs, noRecycledTablesFilter);
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
		String type = "";
		String name = "";
		try {
			stmt = getConnection().createStatement();
			String sql = "SELECT INDEX_NAME, INDEX_TYPE FROM user_indexes WHERE TABLE_NAME=" +SQL.quote(tableName);

			logger.debug("SQL statement was " + sql);
			rs = stmt.executeQuery(sql);
			while (rs.next()) {
				name = rs.getString("INDEX_NAME");
				type = rs.getString("INDEX_TYPE");
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