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
	 * Performs the query directly instead of delegating to the Oracle implementation.
	 * This is in order to avoid the ANALYZE TABLE that the Oracle driver performs as
	 * part of this call.
	 * <p>
	 * Additionally, this method augments the standard JDBC result set with an extra
	 * column, SPG_INDEX_TYPE, which contains the Oracle index type for each entry.
	 * <p>
	 * The result set returned by this method will not contain the extra 
	 * "indexTypeStatistic" row which the Oracle driver does produce.
	 */
	@Override
	public ResultSet getIndexInfo(String catalog, String schema, String table,
			boolean unique, boolean approximate) throws SQLException {
	    Statement stmt = null;
	    ResultSet rs = null;
	    try {
	        stmt = getConnection().createStatement();
	        StringBuilder sql = new StringBuilder();

	        /*
	         * Oracle's JDBC drivers have, for many years, issued an ANALYZE TABLE .. ESTIMATE STATISTICS
	         * against the table in question. See http://www.sqlpower.ca/forum/posts/list/1908.page for
	         * an example of an angry German who thinks this is a bad idea.
	         * 
	         * Oracle's driver does this in order to provide better current table statistics in the special
	         * "tableIndexStatistic" row. The only information produced by the whole exercise is an
	         * approximate row count and the number of disk blocks the table occupies, which we just ignore
	         * anyway.
	         * 
	         * The following query is based on the query Oracle's driver would issue if we called
	         * super.getIndexInfo(), minus the parts that make it slow and dangerous (the table stats part).
	         */
	        sql.append("SELECT NULL AS table_cat,\n");
	        sql.append("       i.owner AS table_schem,\n");
	        sql.append("       i.table_name,\n");
	        sql.append("       DECODE(i.uniqueness, 'UNIQUE', 0, 1) AS non_unique,\n");
	        sql.append("       NULL AS index_qualifier,\n");
	        sql.append("       i.index_name,\n");
	        sql.append("       1 AS type,\n");
	        sql.append("       c.column_position AS ordinal_position,\n");
	        sql.append("       c.column_name,\n");
	        sql.append("       NULL AS asc_or_desc,\n");
	        sql.append("       i.distinct_keys AS cardinality,\n");
	        sql.append("       i.leaf_blocks AS pages,\n");
	        sql.append("       NULL AS filter_condition\n");
	        sql.append("FROM all_indexes i, all_ind_columns c\n");
	        sql.append("WHERE i.table_name = ").append(SQL.quote(table)).append("\n");
	        sql.append("  AND i.owner = ").append(SQL.quote(schema)).append("\n");
	        sql.append("  AND i.index_name = c.index_name\n");
	        sql.append("  AND i.table_owner = c.table_owner\n");
	        sql.append("  AND i.table_name = c.table_name\n");
	        sql.append("  AND i.owner = c.index_owner\n");
	        sql.append("ORDER BY non_unique, type, index_name, ordinal_position");

	        rs = stmt.executeQuery(sql.toString());
			CachedRowSet crs = new CachedRowSet();
			crs.populate(rs, null, "SPG_INDEX_TYPE");
			rs.close();
			rs = null;
			stmt.close();
			stmt = null;
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
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException ex) {
                    logger.error("Failed to close result set! Squishing this exception: ", ex);
                }
            }
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException ex) {
                    logger.error("Failed to close statement! Squishing this exception: ", ex);
                }
            }
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

        ResultSet rs = null;
        CachedRowSet crs = new CachedRowSet();
        
        try {
            rs = super.getTables(catalog, schemaPattern, tableNamePattern, types);

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
            
        } finally {
            if (rs != null) {
                rs.close();
            }
        }
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
			
			// FIXME have to query all_indexes and specify owner in where clause (and take owner as method param)
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
			if (rs != null) {
			    try {
			        rs.close();
			    } catch (SQLException ex) {
			        logger.warn("Failed to close result set. Squishing this exception: ", ex);
			    }
			}
			if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException ex) {
                    logger.warn("Failed to close statement. Squishing this exception: ", ex);
                }
			}
		}
	}
}