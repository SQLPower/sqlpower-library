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
