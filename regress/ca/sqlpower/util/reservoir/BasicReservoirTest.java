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
