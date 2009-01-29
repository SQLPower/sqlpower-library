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
