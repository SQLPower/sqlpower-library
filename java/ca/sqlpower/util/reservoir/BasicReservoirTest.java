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

import junit.framework.TestCase;

public class BasicReservoirTest extends TestCase {

    /**
     * The number of records in the data source.
     */
    private final int POPULATION_SIZE = 100;
    
    TestingReservoirDataSource ds;
    BasicReservoir<Integer> r;
    
    @Override
    protected void setUp() throws Exception {
        ds = new TestingReservoirDataSource(POPULATION_SIZE);
        r = new BasicReservoir<Integer>();
        
        // This should ensure the tests are the same every time
        r.setRandomSeed(1234L);
    }
    
    public void testSampleSmallerThanPopulation() throws Exception {
        Integer[] s = r.getSample(ds, 50);
        assertEquals(50, s.length);
        
        // This also implicitly tests that none of the samples are null
        for (int i = 0; i < 50; i++) {
            assertTrue("Sample "+i+" outside range 0.."+(POPULATION_SIZE-1),
                        s[i] >= 0 && s[i] < POPULATION_SIZE);
        }
    }

    public void testSampleLargerThanPopulation() throws Exception {
        Integer[] s = r.getSample(ds, POPULATION_SIZE * 2);
        
        // Sample should be the entire population
        assertEquals(POPULATION_SIZE, s.length);
        
        // This also implicitly tests that none of the samples are null
        for (int i = 0; i < POPULATION_SIZE; i++) {
            assertEquals("Sample "+i+" has incorrect value",
                         i, s[i].intValue());
        }
    }

    public void testSampleSameAsPopulation() throws Exception {
        Integer[] s = r.getSample(ds, POPULATION_SIZE);
        
        // Sample should be the entire population
        assertEquals(POPULATION_SIZE, s.length);
        
        // This also implicitly tests that none of the samples are null
        for (int i = 0; i < POPULATION_SIZE; i++) {
            assertEquals("Sample "+i+" has incorrect value",
                         i, s[i].intValue());
        }
    }

    public void testEmptyDataSource() throws Exception {
        ds = new TestingReservoirDataSource(0);
        Integer[] s = r.getSample(ds, POPULATION_SIZE);
        assertEquals(0, s.length);
    }

    public void testZeroSizeSample() throws Exception {
        Integer[] s = r.getSample(ds, 0);
        assertEquals(0, s.length);
    }

    /**
     * Tests that the sample in the reservoir isn't just the first n or last
     * n records from the data source.  This test is repeatable because {@link #setUp()}
     * gives the reservoir a predefined random seed.
     */
    public void testSampleIsRandom() throws Exception {
        Integer[] s = r.getSample(ds, 10);
        
        // If the reservoir's pattern of use for the random number generator
        // changes, the array of expected values will not be correct any more.
        // In that case, use thw following code to print the current results and
        // update the array definition below.
        boolean printResults = false;
        if (printResults) {
            System.out.print("{");
            for (int i = 0; i < s.length; i++) {
                System.out.print(s[i] + ", ");
            }
            System.out.println("}");
        }
        Integer[] expected = {19, 77, 2, 46, 29, 94, 89, 63, 60, 42};
        
        assertEquals(expected.length, s.length);
        for (int i = 0; i < expected.length; i++) {
            assertEquals("Values at index "+i+" differ", expected[i], s[i]);
        }
    }
}
