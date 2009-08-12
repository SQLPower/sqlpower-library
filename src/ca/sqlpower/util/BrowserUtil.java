package ca.sqlpower.util;

import java.io.IOException;

/**
 * Utility class for dealing with Web Browsers.
 * For now uses operating system browser and knows about os.name;
 * when Java 6 becomes common, should delegate to java.awt.Desktop.
 */
public class BrowserUtil {

    final static String OS_NAME = System.getProperty("os.name");
    final static String OS_VER = System.getProperty("os.version");

    /** Launches the default browser to display a URL.
     * @throws IOException
     */
    public static void launch(String uri) throws IOException {
        Runtime runtime = Runtime.getRuntime();
        
        if (OS_NAME.contains("Windows")) {
            runtime.exec("cmd /C \"start " + uri + "\"");
        } else if (OS_NAME.startsWith("Mac OS")) {
            runtime.exec("open " + uri);
        } else {
        	// Build a list of browsers to try, in this order. dont know for sure on linux so lets try a bunch
        	String[] browsers = {"epiphany", "firefox", "mozilla", "konqueror",
        			"netscape","opera","links","lynx"};

        	// Build a command string which looks like "browser1 "url" || browser2 "url" ||..."
        	StringBuffer cmd = new StringBuffer();
        	for (int i=0; i<browsers.length; i++)
        		cmd.append( (i==0  ? "" : " || " ) + browsers[i] +" \"" + uri + "\" ");

        	runtime.exec(new String[] { "sh", "-c", cmd.toString() });
        }
    }
}
