/*
 * Created on Jul 31, 2007
 *
 * This code belongs to SQL Power Group Inc.
 */
package ca.sqlpower.xml;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {

    public static Test suite() {
        TestSuite suite = new TestSuite("Test for ca.sqlpower.xml");
        //$JUnit-BEGIN$
        suite.addTestSuite(UnescapingAttributesTest.class);
        suite.addTestSuite(UnescapingDefaultHandlerTest.class);
        suite.addTestSuite(XMLHelperTest.class);
        suite.addTestSuite(UnescapingSaxParserTest.class);
        suite.addTestSuite(UnescapingXMLReaderTest.class);
        //$JUnit-END$
        return suite;
    }

}
