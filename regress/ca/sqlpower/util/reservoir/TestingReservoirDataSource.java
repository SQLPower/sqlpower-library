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
