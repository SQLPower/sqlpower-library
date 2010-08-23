package ca.sqlpower.util;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;

/**
 * Utility class for dealing with Web Browsers.
 */
public class BrowserUtil {

    /** Launches the default browser to display a URL.
     * @throws IOException
     */
    public static void launch(String uri) throws IOException {
    	if (Desktop.isDesktopSupported()) {
    		Desktop.getDesktop().browse(URI.create(uri));
    	}
    }
}
