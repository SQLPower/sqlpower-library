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

/**
 * 
 */
package ca.sqlpower.sql.jdbcwrapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import ca.sqlpower.sql.CachedRowSet;

/**
 * An optimization on top of CachedRowSet that allows easy extraction of a
 * subset of the rows into a new CachedRowSet instance. The "easy" part means
 * that it is not necessary to iterate over every row and pass it to a filter,
 * as is required in the base CachedRowSet.
 * <p>
 * For reference, This optimization provided an order of magnitude performance
 * increase when used together with {@link OracleDatabaseMetaDataDecorator} when
 * populating SQLObjects of the Oracle 10g SYS schema.
 */
class IndexedCachedRowSet extends CachedRowSet {

    private final Map<String, Integer> tableStartRows;
    private final int tableNameColumn;
    
    public IndexedCachedRowSet(ResultSet rs, int tableNameColumn) throws SQLException {
        super();
        this.tableNameColumn = tableNameColumn;
        super.populate(rs);
        tableStartRows = buildIndex();
    }
    
    /**
     * This is the constructor used internally to create the single-table
     * results.
     */
    private IndexedCachedRowSet() throws SQLException {
        tableStartRows = null;
        tableNameColumn = -1;
    }

    private Map<String, Integer> buildIndex() throws SQLException {
        String currentTableName = "";
        HashMap<String, Integer> index = new HashMap<String, Integer>();
        while (next()) {
            String tableNameThisRow = getString(tableNameColumn);
            if (!currentTableName.equals(tableNameThisRow)) {
                currentTableName = tableNameThisRow;
                index.put(tableNameThisRow, getRow());
            }
        }
        return index;
    }

    public boolean containsTable(String tableName) {
        return tableStartRows.containsKey(tableName);
    }
    
    public synchronized CachedRowSet extractSingleTable(String tableName) throws SQLException {
        Integer row = tableStartRows.get(tableName);
        if (row == null) {
            throw new IllegalArgumentException("Table "+tableName+" is not in this rowset");
        }
        absolute(row - 1);
        IndexedCachedRowSet extracted = new IndexedCachedRowSet();
        extracted.rsmd = rsmd;
        extracted.data = new ArrayList<Object[]>();
        while (next() && tableName.equals(getString(tableNameColumn))) {
            extracted.data.add(curRow);
        }
        return extracted;
    }
    
}