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
 * Generic interface for a reservoir sampling algorithm. A reservior is a
 * container that reads an indefinite number of items sequentially, and
 * maintains a statistically valid random sample of <i>n</i> items at all
 * times. The value of <i>n</i> has to be chosen up front, but the ultimate
 * number of items being sampled (that is to say, the size of the population
 * being sampled) does not have to be known in advance.
 * <p>
 * In the documentation for the methods, the total number of records considered
 * for sampling (that is, the size of the population being sampled) is referred
 * to as <i>N</i>.
 * 
 * @param T the type of the item being sampled
 */
public interface Reservoir<T> {
    
    /**
     * Creates a random sample and returns it. The length of the returned array
     * will be <tt>min(</tt><i>n</i><tt>,</tt> <i>N</i><tt>)</tt>.
     * 
     * @return A statistically valid random sample of the records in the given
     *         data source.  If <i>n</i> > <i>N</i>, all records from the data
     *         source will be in the array.
     * @throws ReservoirDataException If accessing the given data source throws an exception
     */
    public T[] getSample(ReservoirDataSource<T> dataSource, int n) throws ReservoirDataException;
    
    /**
     * Sets the seed value for random number generation in this reservior. Using
     * the same seed value when taking a sample from the same data source will
     * result in the same set of records being selected for the sample. The
     * default value for the seed is from the default java.util.Random constructor,
     * which will result in a different sample each time.
     * <p>
     * If you are re-using a Reservior instance for taking multiple samples, and
     * you want the same sample in successive uses, call this method before each
     * call to {@link #getSample(ReservoirDataSource, int)}.
     * 
     * @param s
     *            The seed to use for the random number generator
     */
    public void setRandomSeed(long s);
}
