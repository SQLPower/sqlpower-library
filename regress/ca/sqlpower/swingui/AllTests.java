/*
 * Created on Jul 31, 2007
 *
 * This code belongs to SQL Power Group Inc.
 */
package ca.sqlpower.swingui;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {

    public static Test suite() {
        TestSuite suite = new TestSuite("Test for ca.sqlpower.swingui");
        //$JUnit-BEGIN$
        suite.addTestSuite(SPSUtilsTest.class);
        //$JUnit-END$
        return suite;
    }

}
