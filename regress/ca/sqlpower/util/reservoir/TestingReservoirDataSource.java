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

/**
 * A data source that contains a pre-determined number of integer value.
 * The numeric value of each record is its position (the first record
 * has value 0, next has value 1, and so on).
 */
public class TestingReservoirDataSource implements ReservoirDataSource<Integer> {

    /**
     * The total number of records in this data source.
     */
    private final int recCount;

    /**
     * The current record number. Acts as both the cursor and the value of the
     * next record.
     */
    private int currentRecord = 0;
    
    public TestingReservoirDataSource(int recCount) {
        this.recCount = recCount;
    }
    
    public Class<Integer> getElementType() {
        return Integer.class;
    }

    public boolean hasNext() throws ReservoirDataException {
        return currentRecord < recCount;
    }

    public Integer readNextRecord() throws ReservoirDataException {
        if (currentRecord >= recCount) throw new ReservoirDataException("Already after last record");
        return currentRecord++;
    }

    public void skipRecords(int count) throws ReservoirDataException {
        if (count < 0) throw new ReservoirDataException("Can't skip a negative number of records");
        currentRecord += count;
    }

}
