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

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Implementation of Reservoir using a slightly modified version of <i>Algorithm
 * R</i> as described in "<a
 * href="http://www.cs.duke.edu/~jsv/Papers/Vit85.Reservoir.pdf">Random
 * Sampling with a Reservoir</a>" by J. S. Vitter.
 * <p>
 * Note that Algorithm R was presented in this paper as the basic well-known
 * reservoir sampling technique, and was used as the base line for comparison
 * to a number of more efficient algorithms (called X, Y, and Z).  The more
 * efficient algorithms use the same amount of I/O (a sequential scan of the input),
 * but significantly less CPU time.  The more efficient algorithms are also
 * significantly more complicated, and require some experimentation with threshold
 * values to yield maximum benefit.  If you choose to implement Algorithm Z,
 * and testing shows it to be faster, let us know and we'll add it to the
 * library.
 */
public class BasicReservoir<T> implements Reservoir<T> {

    Random r = new Random();
    
    public T[] getSample(ReservoirDataSource<T> dataSource, int n) throws ReservoirDataException {
        if (n == 0) {
            return makeArray(dataSource.getElementType(), 0);
        }
        // The reservoir.
        List<T> C = new ArrayList<T>(n);
        
        // Make the first n records candidates for the sample 
        for (int j = 0; j < n && dataSource.hasNext(); j++) {
            C.add(dataSource.readNextRecord());
        }
        int t = n; // t is the number of records processed so far
        
        // Process the rest of the records 
        while (dataSource.hasNext()) {
            t++;
            int m = (int) (t * r.nextDouble()); // m is random in the range 0 <= m <= t - 1
            if (m < n) {
                // Make the next record a candidate, replacing one at random 
                C.set(m, dataSource.readNextRecord());
            } else {
                // Skip over the next record
                dataSource.skipRecords(1);
            }
        }
        
        return C.toArray(makeArray(dataSource.getElementType(), C.size()));
    }

    public void setRandomSeed(long s) {
        r.setSeed(s);
    }

    /**
     * Creates an array of the given size having elements of the given type.
     * This is in a separate method because it uses a cast that causes a type
     * safety warning.  Don't worry though: it's type safe.
     * 
     * @param elemType The type of the array elements
     * @param size The number of elements in the array
     * @return A new array of the size and type requested.  Every element will
     * have the value <tt>null</tt>.
     */
    @SuppressWarnings("unchecked")
    private T[] makeArray(Class<T> elemType, int size) {
        return (T[]) Array.newInstance(elemType, size);
    }
}
