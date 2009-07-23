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
import java.util.HashMap;
import java.util.Map;
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
		logger.debug("Created new OracleDatabaseMetaDataDecorator");
	}
	
	private static final int DREADED_ORACLE_ERROR_CODE_1722 = 1722;
	
	private static final String ORACLE_1722_MESSAGE =
		"Encountered Oracle error ORA-1722. This normally means that you are using the " +
		"Oracle 10 driver with Oracle 8. Please check your driver settings.";

    private static final int DREADED_ORACLE_ERROR_CODE_1031 = 1031;
    
    private static final String ORACLE_1031_MESSAGE =
        "Encountered Oracle error ORA-1031. This normally means that you are accessing " +
        "Indices without having the 'analyze any' permission.";
    
    /**
	 * A cache of the imported and exported key metadata. When querying for
	 * either, we cache the entire key list for a schema and then query the cache
	 * in subsequent queries.
     * <p>
     * This field should be accessed via {@link #getCachedResult(MetaDataCache, CacheKey)}
     * and {@link #putCachedResult(MetaDataCache, CacheKey, Object)}.
	 */
    private static final MetaDataCache<CacheKey, CachedRowSet> importedAndExportedKeysCache =
        new MetaDataCache<CacheKey, CachedRowSet>();
    
    /**
	 * A cache of column metadata. When queried the first time, we cache the
	 * entire column list for a schema and then query the cache in subsequent
	 * queries.
	 * <p>
	 * This field should be accessed via {@link #getCachedResult(MetaDataCache, CacheKey)}
	 * and {@link #putCachedResult(MetaDataCache, CacheKey, Object)}.
	 */
    private static final MetaDataCache<CacheKey, IndexedCachedRowSet> columnsCache =
        new MetaDataCache<CacheKey, IndexedCachedRowSet>();
    
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
	private Map<String, String> getIndexType(String tableName) throws SQLException {
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
	
	@Override
	public ResultSet getImportedKeys(String catalog, final String schema, final String table)
			throws SQLException {
	    CacheKey cacheKey = new CacheKey(getConnection().getMetaData(), catalog, schema);
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = getConnection().createStatement();
	        StringBuilder sql = new StringBuilder();
	        CachedRowSet cachedResult = getCachedResult(importedAndExportedKeysCache, cacheKey);

	        if (cachedResult == null) {
		        /*
				 * Oracle's JDBC drivers does not find relationships on alternate
				 * keys. The following query is based on the query Oracle's driver
				 * would issue if we called super.getImportedKeys(), adding in the
				 * part that makes it find alternate key relationships.
				 */
		        sql.append("SELECT NULL AS pktable_cat,\n");
		        sql.append("       p.owner as pktable_schem,\n");
		        sql.append("       p.table_name as pktable_name,\n");
		        sql.append("       pc.column_name as pkcolumn_name,\n");
		        sql.append("       NULL as fktable_cat,\n");
		        sql.append("       f.owner as fktable_schem,\n");
		        sql.append("       f.table_name as fktable_name,\n");
		        sql.append("       fc.column_name as fkcolumn_name,\n");
		        sql.append("       fc.position as key_seq,\n");
		        sql.append("       NULL as update_rule,\n");
		        sql.append("       decode (f.delete_rule, 'CASCADE', 0, 'SET NULL', 2, 1) as delete_rule,\n");
		        sql.append("       f.constraint_name as fk_name,\n");
		        sql.append("       p.constraint_name as pk_name,\n");
		        sql.append("       decode(f.deferrable, 'DEFERRABLE', 5 ,'NOT DEFERRABLE', 7, 'DEFERRED', 6) deferrability\n");
		        sql.append("FROM all_cons_columns pc, all_constraints p,\n");
		        sql.append("     all_cons_columns fc, all_constraints f\n");
		        sql.append("WHERE 1 = 1\n");
		        if (cacheType.get() == null || cacheType.get().equals(CacheType.NO_CACHE)) {
		        	sql.append("	  AND f.table_name = ").append(SQL.quote(table)).append("\n");
		        	if (schema != null) {
		        		sql.append("      AND f.owner = ").append(SQL.quote(schema)).append("\n");
		        	}
		        }
		        sql.append("      AND f.constraint_type = 'R'\n");
		        sql.append("      AND p.owner = f.r_owner\n");
		        sql.append("      AND p.constraint_name = f.r_constraint_name\n");
		        sql.append("      AND p.constraint_type in ('P', 'U')\n");
		        sql.append("      AND pc.owner = p.owner\n");
		        sql.append("      AND pc.constraint_name = p.constraint_name\n");
		        sql.append("      AND pc.table_name = p.table_name\n");
		        sql.append("      AND fc.owner = f.owner\n");
		        sql.append("      AND fc.constraint_name = f.constraint_name\n");
		        sql.append("      AND fc.table_name = f.table_name\n");
		        sql.append("      AND fc.position = pc.position\n");
		        sql.append("ORDER BY pktable_schem, pktable_name, key_seq");
		        
		        logger.debug("getImportedKeys() sql statement was: " + sql.toString());
		        rs = stmt.executeQuery(sql.toString());
		        
		        CachedRowSet result = new CachedRowSet();
		        result.populate(rs);
		        
		        if (cacheType.get() == null || cacheType.get().equals(CacheType.NO_CACHE)) {
		        	return result;
		        } else {
		        	putCachedResult(importedAndExportedKeysCache, cacheKey, result);
		        	cachedResult = result;
		        }
	        }
	        
			CachedRowSet crs = new CachedRowSet();
			RowFilter filter = new RowFilter() {
				public boolean acceptsRow(Object[] row) {
					boolean result;
					// expecting row[5] to be FK_TABLE_SCHEM
					// expecting row[6] to be FK_TABLE_NAME
					if (schema != null){
						result = (schema.equals(row[5]) && table.equals(row[6]));
					} else {
						result = table.equals(row[6]);
					}
					return result;
				}
			};
			
			synchronized (cachedResult) {
			    crs.populate(cachedResult, filter);
			    cachedResult.beforeFirst();
            }
			
			return crs;
		} finally {
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
		}
	}
	
	@Override
	public ResultSet getExportedKeys(String catalog, final String schema, final String table)
			throws SQLException {
        CacheKey cacheKey = new CacheKey(getConnection().getMetaData(), catalog, schema);
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = getConnection().createStatement();
	        StringBuilder sql = new StringBuilder();
	        CachedRowSet cachedResult = getCachedResult(importedAndExportedKeysCache, cacheKey);

	        if (cachedResult == null) {
		        /*
				 * Oracle's JDBC drivers does not find relationships on alternate
				 * keys. The following query is based on the query Oracle's driver
				 * would issue if we called super.getExportedKeys(), adding in the
				 * part that makes it find alternate key relationships.
				 */
		        sql.append("SELECT NULL AS pktable_cat,\n");
		        sql.append("       p.owner as pktable_schem,\n");
		        sql.append("       p.table_name as pktable_name,\n");
		        sql.append("       pc.column_name as pkcolumn_name,\n");
		        sql.append("       NULL as fktable_cat,\n");
		        sql.append("       f.owner as fktable_schem,\n");
		        sql.append("       f.table_name as fktable_name,\n");
		        sql.append("       fc.column_name as fkcolumn_name,\n");
		        sql.append("       fc.position as key_seq,\n");
		        sql.append("       NULL as update_rule,\n");
		        sql.append("       decode (f.delete_rule, 'CASCADE', 0, 'SET NULL', 2, 1) as delete_rule,\n");
		        sql.append("       f.constraint_name as fk_name,\n");
		        sql.append("       p.constraint_name as pk_name,\n");
		        sql.append("       decode(f.deferrable, 'DEFERRABLE', 5 ,'NOT DEFERRABLE', 7, 'DEFERRED', 6) deferrability\n");
		        sql.append("FROM all_cons_columns pc, all_constraints p,\n");
		        sql.append("     all_cons_columns fc, all_constraints f\n");
		        sql.append("WHERE 1 = 1\n");
		        if (cacheType.get() == null || cacheType.get().equals(CacheType.NO_CACHE)) {
					sql.append("      AND p.table_name = ").append(SQL.quote(table)).append("\n");
					if (schema != null) {
						sql.append("      AND p.owner = ").append(SQL.quote(schema)).append("\n");
		        	}
		        }
		        sql.append("      AND f.constraint_type = 'R'\n");
		        sql.append("      AND p.owner = f.r_owner\n");
		        sql.append("      AND p.constraint_name = f.r_constraint_name\n");
		        sql.append("      AND p.constraint_type in ('P', 'U')\n");
		        sql.append("      AND pc.owner = p.owner\n");
		        sql.append("      AND pc.constraint_name = p.constraint_name\n");
		        sql.append("      AND pc.table_name = p.table_name\n");
		        sql.append("      AND fc.owner = f.owner\n");
		        sql.append("      AND fc.constraint_name = f.constraint_name\n");
		        sql.append("      AND fc.table_name = f.table_name\n");
		        sql.append("      AND fc.position = pc.position\n");
		        sql.append("ORDER BY pktable_schem, pktable_name, key_seq");
		        
		        logger.debug("getExportedKeys() sql statement was: " + sql.toString());
		        rs = stmt.executeQuery(sql.toString());
		        
		        CachedRowSet result = new CachedRowSet();
		        result.populate(rs);
		        
		        if (cacheType.get() == null || cacheType.get().equals(CacheType.NO_CACHE)) {
		        	return result;
		        } else {
		        	putCachedResult(importedAndExportedKeysCache, cacheKey, result);
		        	cachedResult = result;
		        }
	        }
	        
			CachedRowSet crs = new CachedRowSet();
			RowFilter filter = new RowFilter() {
				public boolean acceptsRow(Object[] row) {
					boolean result;
					// expecting row[1] to be PK_TABLE_SCHEM
					// expecting row[2] to be PK_TABLE_NAME
					if (schema != null){
						result = (schema.equals(row[1]) && table.equals(row[2]));
					} else {
						result = table.equals(row[2]);
					}
					return result;
				}
			};

            synchronized (cachedResult) {
                crs.populate(cachedResult, filter);
                cachedResult.beforeFirst();
            }
			
			return crs;
		} finally {
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
		}
	}

	/**
	 * An alternate implementation of
	 * {@link DatabaseMetaData#getColumns(String, String, String, String)} that
	 * caches the results and queries the cache in subsequent requests.
	 */
	@Override
	public ResultSet getColumns(String catalog, final String schemaPattern,
			final String tableNamePattern, final String columnNamePattern)
			throws SQLException {
		
	    logger.debug("getColumns("+catalog+", "+schemaPattern+", "+tableNamePattern+", "+columnNamePattern+") cache mode=" + cacheType.get());
	    
	    final CacheKey cacheKey = new CacheKey(getConnection().getMetaData(), catalog, schemaPattern);
	    
		Statement stmt = null;
		ResultSet rs = null;
		try {
		    IndexedCachedRowSet cachedResult = getCachedResult(columnsCache, cacheKey);
			if (cachedResult == null) {
			    logger.debug("No cached data found. Querying data dictionary...");
				stmt = getConnection().createStatement();
				
				StringBuilder sql = new StringBuilder();
				
				sql.append("SELECT "); 
				sql.append("	NULL AS table_cat,\n");
				sql.append("	t.owner AS table_schem,\n");
				sql.append("	t.table_name AS table_name,\n");
				sql.append("	t.column_name AS column_name,\n");
				sql.append("	DECODE (" +
						"CASE " +
						" WHEN SUBSTR(t.data_type, 1, 9) = 'TIMESTAMP' THEN 'TIMESTAMP' " +
						" ELSE t.data_type " +
						"END " +
						", 'CHAR', 1, 'VARCHAR2', 12, 'NUMBER', 3, 'LONG', -1, 'DATE', 91, 'RAW', -3, 'LONG RAW', -4, 'BLOB', 2004, 'CLOB', 2005, 'BFILE', -13, 'FLOAT', 6, 'TIMESTAMP', 93, 'TIMESTAMP WITH TIME ZONE', -101, 'TIMESTAMP WITH LOCAL TIME ZONE', -102, 'INTERVAL YEAR(2) TO MONTH', -103, 'INTERVAL DAY(2) TO SECOND(6)', -104, 'BINARY_FLOAT', 100, 'BINARY_DOUBLE', 101, 'NVARCHAR2', -9, 'NCHAR', -15, 'NCLOB', 2011, 1111)\n");
				sql.append("	AS data_type,\n"); 
				sql.append("	t.data_type AS type_name,\n");
				sql.append("	DECODE (t.data_precision, null, t.data_length, t.data_precision) AS column_size,\n");
				sql.append("	0 AS buffer_length,\n");
				sql.append("	t.data_scale AS decimal_digits,\n");
				sql.append("	10 AS num_prec_radix,\n");
				sql.append("	DECODE (t.nullable, 'N', 0, 1) AS nullable,\n");
				sql.append("	c.comments AS remarks,\n");
				sql.append("	t.data_default AS column_def,\n");
				sql.append("	0 AS sql_data_type,\n");
				sql.append("	0 AS sql_datetime_sub,\n");
				sql.append("	t.data_length AS char_octet_length,\n");
				sql.append("	t.column_id AS ordinal_position,\n");
				sql.append("	DECODE (t.nullable, 'N', 'NO', 'YES') AS is_nullable\n");
				sql.append("FROM\n");
				sql.append("	all_tab_columns t,\n");
				sql.append("	all_col_comments c\n");
				sql.append("WHERE\n");
				if (schemaPattern != null) sql.append("	t.owner LIKE ").append(SQL.quote(schemaPattern)).append(" ESCAPE '/'\n");
				if (hidingRecycleBinTables) sql.append("	AND t.table_name NOT LIKE 'BIN$%' ESCAPE '/'\n");
				if (cacheType.get() == null || cacheType.get().equals(CacheType.NO_CACHE)) {
					sql.append("	AND t.table_name LIKE ").append(SQL.quote(tableNamePattern)).append(" ESCAPE '/'\n");
					sql.append("	AND t.column_name LIKE ").append(SQL.quote(columnNamePattern)).append(" ESCAPE '/'\n");
				}
				sql.append("	AND t.owner=c.owner (+)\n");
				sql.append("	AND t.table_name=c.table_name (+)\n");
				sql.append("	AND t.column_name = c.column_name (+)\n");
				sql.append("ORDER BY\n");
				sql.append("	table_schem, table_name, ordinal_position");
				
				logger.debug("getColumns() sql statement was: \n" + sql.toString());

				stmt.setFetchSize(1000);
				rs = stmt.executeQuery(sql.toString());
		        
		        if (cacheType.get() == null || cacheType.get().equals(CacheType.NO_CACHE)) {
		            CachedRowSet result = new CachedRowSet();
		            result.populate(rs);
		        	return result;
		        } else {
		            IndexedCachedRowSet result = new IndexedCachedRowSet(rs, 3);
		        	putCachedResult(columnsCache, cacheKey, result);
		        	cachedResult = result;
		        }
			}
	        
			RowFilter filter = new RowFilter() {
			    
			    // Here, we are simulating the behaviour of
			    // t.table_name LIKE 'tableNamePattern'
			    final String tablePattern = tableNamePattern.replaceAll("%", ".*");
			    final Pattern tp = Pattern.compile(tablePattern);
			    
			    // Here, we are simulating the behaviour of
			    // t.column_name LIKE 'columnNamePattern'
			    String columnPattern = columnNamePattern.replace("%", ".*");
			    final Pattern cp = Pattern.compile(columnPattern);
			    
				public boolean acceptsRow(Object[] row) {
					// expecting row[2] to be FK_TABLE_NAME
					
				    return tp.matcher(row[2].toString()).matches() &&
				            cp.matcher(row[3].toString()).matches();
				}
			};
			
			logger.debug("Filtering cache...");
			CachedRowSet filtered;
			synchronized (cachedResult) {
			    if (!tableNamePattern.contains("%")) {
			        // exact match requested--we can use the index for table name
			        // (filter still applies to column name)
			        filtered = cachedResult.extractSingleTable(tableNamePattern);
			    } else {
			        // have to search every row for wildcard match on table name
			        filtered = new CachedRowSet();
			        filtered.populate(cachedResult, filter);
			    }
			    cachedResult.beforeFirst();
            }
			
			return filtered;
		} finally {
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
		}
	}
}