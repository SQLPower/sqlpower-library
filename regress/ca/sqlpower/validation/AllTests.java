/*
 * Created on Jul 31, 2007
 *
 * This code belongs to SQL Power Group Inc.
 */
package ca.sqlpower.validation;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {

    public static Test suite() {
        TestSuite suite = new TestSuite("Test for ca.sqlpower.validation");
        //$JUnit-BEGIN$
        suite.addTestSuite(RegExValidatorTest.class);
        //$JUnit-END$
        return suite;
    }

}
