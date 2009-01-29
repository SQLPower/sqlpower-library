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
     * Returns the number of rows read or skipped so far. Once
     * {@link #hasNext()} has returned false, this will be the total number of
     * rows that were returned by the query.
     */
    private int rowCount;
    
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
    public JDBCReserviorDataSource(Connection con, String query) throws SQLException {
        con.setAutoCommit(false);
        stmt = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
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
            return !rs.isLast() && !(rs.getRow() == 0 && !rs.isBeforeFirst());
        } catch (SQLException e) {
            throw new ReservoirDataException(e);
        }
    }

    public Object[] readNextRecord() throws ReservoirDataException {
        try {
            boolean hasNext = rs.next();
            if (!hasNext) throw new ReservoirDataException("Attempted to read past last record");
            rowCount++;
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
            boolean onValidRow = rs.relative(count);
            if (onValidRow) {
                rowCount = rs.getRow();
            } else {
                rowCount = rs.getRow() - 1;
            }
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
    
    /**
     * Returns the number of rows read or skipped so far. Once
     * {@link #hasNext()} has returned false, this will be the total number of
     * rows that were returned by the query.
     */
    public int getRowCount() {
        return rowCount;
    }
}
