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
