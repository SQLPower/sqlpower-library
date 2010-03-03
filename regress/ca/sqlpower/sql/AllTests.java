/*
 * Created on Jul 31, 2007
 *
 * This code belongs to SQL Power Group Inc.
 */
package ca.sqlpower.sql;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {

    public static Test suite() {
        TestSuite suite = new TestSuite("Test for ca.sqlpower.sql");
        //$JUnit-BEGIN$
        suite.addTestSuite(SPDataSourceTypeTest.class);
        suite.addTestSuite(SPDataSourceTest.class);
        suite.addTestSuite(PLDotIniTest.class);
        suite.addTestSuite(PlDotIniListenersTest.class);
        //$JUnit-END$
        return suite;
    }

}
