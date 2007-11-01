package ca.sqlpower.util;

import junit.framework.TestCase;
import ca.sqlpower.swingui.SPSUtils;

public class SPSUtilsTest extends TestCase {

	final String FAKE_CLASS_NAME = "my.test";
	
	public void testClassNameStuff() {
        assertEquals("String", SPSUtils.niceClassName(""));
        assertEquals("Object", SPSUtils.niceClassName(new Object()));
    }

}
