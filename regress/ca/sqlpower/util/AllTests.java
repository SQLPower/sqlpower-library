/*
 * Created on Jul 31, 2007
 *
 * This code belongs to SQL Power Group Inc.
 */
package ca.sqlpower.util;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {

    public static Test suite() {
        TestSuite suite = new TestSuite("Test for ca.sqlpower.util");
        //$JUnit-BEGIN$
        suite.addTestSuite(LeastRecentlyUsedCacheTest.class);
        suite.addTestSuite(RecurrenceTest.class);
        //$JUnit-END$
        return suite;
    }

}
