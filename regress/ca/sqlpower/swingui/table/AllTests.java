/*
 * Created on Jul 31, 2007
 *
 * This code belongs to SQL Power Group Inc.
 */
package ca.sqlpower.swingui.table;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {

    public static Test suite() {
        TestSuite suite = new TestSuite("Test for ca.sqlpower.swingui.table");
        //$JUnit-BEGIN$
        suite.addTestSuite(DecimalRendererTest.class);
        suite.addTestSuite(PercentRendererTest.class);
        suite.addTestSuite(DateRendererTest.class);
        //$JUnit-END$
        return suite;
    }

}
