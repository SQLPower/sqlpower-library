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
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import ca.sqlpower.sql.CachedRowSet;
import ca.sqlpower.util.SQLPowerUtils;


public class HSQLDBDatabaseMetaDataDecorator extends DatabaseMetaDataDecorator {

    private static final Logger logger = Logger.getLogger(HSQLDBDatabaseMetaDataDecorator.class);
    
    public HSQLDBDatabaseMetaDataDecorator(DatabaseMetaData delegate, ConnectionDecorator connectionDecorator) {
        super(delegate, connectionDecorator);
    }

    @Override
    public ResultSet getIndexInfo(String catalog, String schema, String table,
            boolean unique, boolean approximate) throws SQLException {
        
        logger.debug("Creating index info for " + makeKey(catalog, schema, table));
        
        // PHASE 1: get the raw information about the indexes (with "incorrect" PK index names)
        ResultSet rs = super.getIndexInfo(catalog, schema, table, unique, approximate);
        CachedRowSet indexInfo = new CachedRowSet();
        indexInfo.populate(rs);
        rs.close();
        rs = null;
        
        
        // PHASE 2: collect PK information for the same tables
        
        // populating this with the primary key name for each table in question
        Map<String, String> pkNames = new HashMap<String, String>();
        
        // populating this with the columns of the primary keys for each table in question
        Map<String, Set<String>> pkCols = new HashMap<String, Set<String>>();
        
        // these are all temp variables
        Set<String> cols = null;
        rs = super.getPrimaryKeys(catalog, schema, table);
        String cat = null;
        String sch = null;
        String tab = null;
        
        while (rs.next()) {
            if (SQLPowerUtils.areEqual(cat, rs.getString("TABLE_CAT"))
                    && SQLPowerUtils.areEqual(sch, rs.getString("TABLE_SCHEM"))
                    && SQLPowerUtils.areEqual(tab, rs.getString("TABLE_NAME"))) {
                cols.add(rs.getString("COLUMN_NAME"));
            } else {
                cat = rs.getString("TABLE_CAT");
                sch = rs.getString("TABLE_SCHEM");
                tab = rs.getString("TABLE_NAME");
                String key = makeKey(cat, sch, tab);
                pkNames.put(key, rs.getString("PK_NAME"));
                cols = new TreeSet<String>();
                cols.add(rs.getString("COLUMN_NAME"));
                pkCols.put(key, cols);
            }
        }
        rs.close();
        rs = null;
        
        if (logger.isDebugEnabled()) {
            logger.debug("Discovered PK names: " + pkNames);
            logger.debug("Discovered PK cols:  " + pkCols);
        }

        // PHASE 3: determine index->pk name mappings using the information cached during phase 1
        Map<String, String> indexToPkName = discoverIndexToPKMappings(indexInfo, pkNames, pkCols);
        
        // PHASE 4: update the index names to their corresponding PK names
        indexInfo.beforeFirst();
        while (indexInfo.next()) {
            String pkName = indexToPkName.get(indexInfo.getString("INDEX_NAME"));
            if (pkName != null) {
                indexInfo.updateString("INDEX_NAME", pkName);
            }
        }
        
        indexInfo.beforeFirst();
        return indexInfo;
    }

    private Map<String,String> discoverIndexToPKMappings(CachedRowSet indexInfo,
            Map<String, String> pkNames, Map<String, Set<String>> pkCols)
            throws SQLException {
        Map<String,String> indexToPKName = new HashMap<String, String>();
        indexInfo.beforeFirst();
        String cat = null;
        String sch = null;
        String tab = null;
        String idx = null;
        Set<String> cols = null;
        while (indexInfo.next()) {
            logger.debug("Considering index " + indexInfo.getString("INDEX_NAME") + ", col " + indexInfo.getString("COLUMN_NAME"));
            if (indexInfo.getBoolean("NON_UNIQUE")) continue;
            if (indexInfo.getInt("TYPE") == tableIndexStatistic) continue;
            if (!indexInfo.getString("INDEX_NAME").startsWith("SYS_IDX_")) continue;
            
            if (SQLPowerUtils.areEqual(cat, indexInfo.getString("TABLE_CAT"))
                    && SQLPowerUtils.areEqual(sch, indexInfo.getString("TABLE_SCHEM"))
                    && SQLPowerUtils.areEqual(tab, indexInfo.getString("TABLE_NAME"))
                    && SQLPowerUtils.areEqual(idx, indexInfo.getString("INDEX_NAME"))) {
                cols.add(indexInfo.getString("COLUMN_NAME"));
            } else {
                // just finished reading all columns of an index. let's see if it was the table's PK.
                String key = makeKey(cat, sch, tab);
                Set<String> colsOfPk = pkCols.get(key);
                if (colsOfPk != null && colsOfPk.equals(cols)) {
                    // we found the index that implements the PK!
                    logger.debug("Found index name mapping " + idx + " -> " + pkNames.get(key));
                    indexToPKName.put(idx, pkNames.get(key));
                    
                    // remove this entry in case there's another unique index on
                    // this table with the same columns (they can't both be the PK!)
                    pkNames.remove(key);
                } else {
                    logger.debug("Index is not a PK: " + idx + "(my cols: " + cols + "; colsOfPk: " + colsOfPk + ")");
                }
                
                // prepare for next index
                cat = indexInfo.getString("TABLE_CAT");
                sch = indexInfo.getString("TABLE_SCHEM");
                tab = indexInfo.getString("TABLE_NAME");
                idx = indexInfo.getString("INDEX_NAME");
                cols = new TreeSet<String>();
                cols.add(indexInfo.getString("COLUMN_NAME"));
            }
        }
        
        logger.debug("Finished main loop. doing final check...");
        // this is the same as above. need to repeat to handle last index in the result set
        String key = makeKey(cat, sch, tab);
        Set<String> colsOfPk = pkCols.get(key);
        if (colsOfPk != null && colsOfPk.equals(cols)) {
            // we found the index that implements the PK!
            logger.debug("Found index name mapping " + idx + " -> " + pkNames.get(key));
            indexToPKName.put(idx, pkNames.get(key));
            
            // remove this entry in case there's another unique index on
            // this table with the same columns (they can't both be the PK!)
            pkNames.remove(key);
        } else {
            logger.debug("Index is not a PK: " + idx + "(my cols: " + cols + "; colsOfPk: " + colsOfPk + ")");
        }
        
        return indexToPKName;
    }

    private String makeKey(String cat, String sch, String tab) {
        String key = cat + "." + sch + "." + tab;
        return key;
    }
    
    /**
     * Fixes a problem where integer columns are marked as having a precision of 0.
     */
    @Override
    public ResultSet getColumns(String catalog, String schemaPattern,
            String tableNamePattern, String columnNamePattern)
            throws SQLException {
        ResultSet columns = null;
        try {
            columns = super.getColumns(catalog, schemaPattern, tableNamePattern, columnNamePattern);
            CachedRowSet crs = new CachedRowSet();
            crs.populate(columns);
            while (crs.next()) {
                int type = crs.getInt(5);
                if (type == Types.INTEGER && crs.getInt(7) == 0) {
                    crs.updateInt(7, 10);
                }
            }
            
            crs.beforeFirst();
            return crs;
            
        } finally {
            if (columns != null) {
                try {
                    columns.close();
                } catch (SQLException ex) {
                    logger.error("Failed to close original result set", ex);
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
