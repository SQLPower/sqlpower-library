import junit.framework.Test;
import junit.framework.TestSuite;

/*
 * Created on Jul 31, 2007
 *
 * This code belongs to SQL Power Group Inc.
 */

public class LibraryMegaTest {

    public static Test suite() {
        TestSuite suite = new TestSuite("Test for default package");
        //$JUnit-BEGIN$
        suite.addTest(ca.sqlpower.sql.AllTests.suite());
        suite.addTest(ca.sqlpower.swingui.table.AllTests.suite());
        suite.addTest(ca.sqlpower.util.AllTests.suite());
        suite.addTest(ca.sqlpower.validation.AllTests.suite());
        suite.addTest(ca.sqlpower.validation.swingui.AllTests.suite());
        suite.addTest(ca.sqlpower.xml.AllTests.suite());
        //$JUnit-END$
        return suite;
    }

}
