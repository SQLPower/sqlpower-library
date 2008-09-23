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
        extracted.data = new ArrayList();
        while (next() && tableName.equals(getString(tableNameColumn))) {
            extracted.data.add(curRow);
        }
        return extracted;
    }
    
}