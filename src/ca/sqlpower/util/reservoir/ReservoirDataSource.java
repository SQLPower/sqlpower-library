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
 * An interface used by {@link Reservoir} implementations to obtain data for the
 * sample. If none of the provided implementations suit your needs, you will
 * have to implement this interface yourself in order to use a Reservoir.
 * 
 * @param <T>
 *            The type of the item being read from the data source.
 */
public interface ReservoirDataSource<T> {

    /**
     * Reads the record at the cursor and advances the cursor to point to the
     * next record.
     * 
     * @return The record that was at the current cursor position.
     */
    public T readNextRecord() throws ReservoirDataException;

    /**
     * Advances the cursor past the given number of records. A count of 0 has no
     * effect, a count of 1 skips past a single record, and so on.
     * <p>
     * If the given skip count would move the cursor past the last record, this
     * call returns normally, leaving the cursor positioned past the last
     * record. As a result, {@link #hasNext()} will return <tt>false</tt>.
     * 
     * @param count
     *            The number of records to skip. Must be >= 0, but may exceed
     *            the actual number of records remaining in the data source.
     */
    public void skipRecords(int count) throws ReservoirDataException;

    /**
     * Returns true if and only if there is at least one more record available
     * to be read by {@link #readNextRecord()}.
     * 
     * @return Whether or not there are more records remaining to be read from
     *         the data source.
     */
    public boolean hasNext() throws ReservoirDataException;
    
    /**
     * Returns the class of the element type produced by this data source. 
     */
    public Class<T> getElementType();
}
