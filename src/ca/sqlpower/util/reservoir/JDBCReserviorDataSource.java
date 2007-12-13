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

package ca.sqlpower.util.reservoir;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * A reservoir data source implementation that gets its data from the results of
 * an SQL query. This class attempts to configure the connection and statement
 * so that the rows that are skipped will not be transferred into the JVM,
 * however that is not possible on all database platforms.  If it is possible
 * to skip rows on your platform, but this class isn't achieving that behaviour,
 * please send us a patch that makes it work!
 */
public class JDBCReserviorDataSource implements ReservoirDataSource<Object[]> {

    private final Statement stmt;
    private final ResultSet rs;
    private final ResultSetMetaData rsmd;
    private final int colCount;
    
    /**
     * 
     * @param con The connection to use. WARNING: auto-commit will be turned off
     * for this connection!  If you want auto-commit on, turn it back on when you're
     * finished with this reservoir data source.
     * @param query The query to execute
     * @throws SQLException if there is a problem reading the data from the database.
     * This will most likely be caused by an invalid select statement, but anything
     * is possible!
     */
    JDBCReserviorDataSource(Connection con, String query) throws SQLException {
        con.setAutoCommit(false);
        stmt = con.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
        stmt.setFetchSize(1);
        rs = stmt.executeQuery(query);
        rsmd = rs.getMetaData();
        colCount = rsmd.getColumnCount();
    }

    /**
     * The element type is an array: the column values of a row returned by the query.
     */
    public Class<Object[]> getElementType() {
        return Object[].class;
    }

    public boolean hasNext() throws ReservoirDataException {
        try {
            return !rs.isAfterLast();
        } catch (SQLException e) {
            throw new ReservoirDataException(e);
        }
    }

    public Object[] readNextRecord() throws ReservoirDataException {
        try {
            boolean hasNext = rs.next();
            if (!hasNext) throw new ReservoirDataException("Attempted to read past last record");
            Object[] rowValues = new Object[colCount];
            for (int i = 0; i < colCount; i++) {
                rowValues[i] = rs.getObject(i + 1);
            }
            return rowValues;
        } catch (SQLException e) {
            throw new ReservoirDataException(e);
        }
    }

    public void skipRecords(int count) throws ReservoirDataException {
        try {
            rs.relative(count);
        } catch (SQLException e) {
            throw new ReservoirDataException(e);
        }
    }

    /**
     * This is exposed as package-private so that the tests can examine
     * the statement settings.
     */
    Statement getStatement() {
        return stmt;
    }
    
}
