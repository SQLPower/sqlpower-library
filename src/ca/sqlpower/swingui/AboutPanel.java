/*
 * Copyright (c) 2008, SQL Power Group Inc.
 *
 * This file is part of Power*Architect.
 *
 * Power*Architect is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * Power*Architect is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>. 
 */
package ca.sqlpower.swingui;

import java.awt.FlowLayout;
import java.util.Arrays;
import java.util.Properties;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.table.AbstractTableModel;

import net.miginfocom.swing.MigLayout;

import org.apache.log4j.Logger;

/**
 * A generic 'About' panel that displays the application's name, version, and
 * icon in one tab, and also includes a system properties tab.
 */
public class AboutPanel extends AbstractNoEditDataEntryPanel {

    /**
     * Traditional computer value for "1 megabyte" (1024^2).
     */
    private static final long MEBIBYTE = 1024 * 1024;

    private final Logger logger = Logger.getLogger(AboutPanel.class);
    
	private JTextArea content;

	/**
	 * Creates an AboutPanel with a given icon, product name, and product
	 * version. The product version is provided using a properties file which
	 * must contain a property named 'app.version' with the value of the version
	 * of the application.
	 * 
	 * @param icon
	 *            The icon that will be displayed in the About dialog
	 * @param productName
	 *            The name of the product
	 * @param versionPropertiesPath
	 *            The path to a properties file that contains a property named
	 *            'app.version' with the application's version number
	 * @param defaultAppVersion
	 *            If there is an exception while trying to read the version
	 *            properties file, it will default to this value.
	 * 
	 */
	public AboutPanel(ImageIcon icon, String productName, String versionPropertiesPath, String defaultAppVersion) {
		JTabbedPane tabs = new JTabbedPane();
        
        tabs.add(Messages.getString("AboutPanel.aboutTab"), initAboutTab(icon, productName, versionPropertiesPath, defaultAppVersion)); //$NON-NLS-1$
        tabs.add(Messages.getString("AboutPanel.systemPropertiesTab"), new JScrollPane(initSysPropsTab())); //$NON-NLS-1$

        tabs.setSelectedIndex(0);
        add(tabs);
	}
	
	private static final JLabel sqlpLabel = new JLabel(new ImageIcon(AboutPanel.class.getClassLoader().getResource("ca/sqlpower/swingui/SQLP-90x80.png")));

    private JComponent initAboutTab(ImageIcon icon, String productName, String versionPropertiesPath, String defaultAppVersion) {
        JPanel panel = new JPanel();
		panel.setLayout(new FlowLayout());

		
        // Include the Icon!
		if (icon != null) {
			JPanel logoPanel = new JPanel(new MigLayout("", "[center]"));
			logoPanel.add(new JLabel(icon), "wrap, gapbottom 50");
			logoPanel.add(sqlpLabel);
			panel.add(logoPanel);
		}

        String version;
        
        Properties properties = new Properties();
        try {
            properties.load(AboutPanel.class.getClassLoader().getResourceAsStream(versionPropertiesPath));
            version = properties.get("app.version").toString(); //$NON-NLS-1$
        } catch (Exception ex) {
            logger.error("Exception occured while trying to read version properties file. Falling back to default", ex); //$NON-NLS-1$
            version = defaultAppVersion;
        }
        
		content = new JTextArea();
		content.setText(createEnvironmentSynposis(productName, version));
		content.setEditable(false);
		content.setOpaque(false);
		panel.add(content);
		
        return panel;
	}

    /**
     * Creates the runtime environment synopsis that appears in the About
     * Panel's default tab. This is available as public method so this
     * information can be obtained when generating support requests and
     * performing similar operations.
     * <p>
     * The message will be localized to the user's preferred locale if possible.
     * 
     * @param productName
     *            The product name to include in the synposis
     * @param productVersion
     *            The product version string to include in the synopsis
     * @return A localized multi-line string with information about the product,
     *         java runtime, java vm, and current memory stats.
     */
    public static String createEnvironmentSynposis(String productName, String productVersion) {
        String maxMemoryMiB = String.valueOf(Runtime.getRuntime().maxMemory() / MEBIBYTE);
		String totalMemoryMiB = String.valueOf(Runtime.getRuntime().totalMemory() / MEBIBYTE);
		String freeMemoryMiB = String.valueOf(Runtime.getRuntime().freeMemory() / MEBIBYTE);
        String versionInfo = productName + " " + productVersion + "\n" + //$NON-NLS-1$ //$NON-NLS-2$
		    Messages.getString("AboutPanel.copyright") + "\n" + //$NON-NLS-1$ //$NON-NLS-2$
		    "\n" + //$NON-NLS-1$
		    Messages.getString("AboutPanel.operatingSystem") + "\n" + //$NON-NLS-1$ //$NON-NLS-2$
		    System.getProperty("os.name") + " " + System.getProperty("os.version") + " (" + System.getProperty("os.arch") + ")\n" + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
		    "\n" + //$NON-NLS-1$
		    Messages.getString("AboutPanel.runtimeEnvironment") + "\n" + //$NON-NLS-1$ //$NON-NLS-2$
		    System.getProperty("java.runtime.name") + "\n" + //$NON-NLS-1$ //$NON-NLS-2$
		    System.getProperty("java.runtime.version") + "\n" + //$NON-NLS-1$ //$NON-NLS-2$
		    "\n" + //$NON-NLS-1$
		    Messages.getString("AboutPanel.vmInfo") + "\n" + //$NON-NLS-1$ //$NON-NLS-2$
		    System.getProperty("java.vm.name") + "\n" + //$NON-NLS-1$ //$NON-NLS-2$
		    System.getProperty("java.vm.version") + "\n" + //$NON-NLS-1$ //$NON-NLS-2$
		    System.getProperty("java.vm.vendor") + "\n" + //$NON-NLS-1$ //$NON-NLS-2$
		    "\n" + //$NON-NLS-1$
		    Messages.getString("AboutPanel.memory") + "\n" + //$NON-NLS-1$ //$NON-NLS-2$
		    Messages.getString("AboutPanel.memoryAllocMaxFree", totalMemoryMiB, maxMemoryMiB, freeMemoryMiB); //$NON-NLS-1$
        return versionInfo;
    }

    /**
     * A JTable model to display the system properties
     */
    private class SystemPropertiesTableModel extends AbstractTableModel {
        private Properties props = System.getProperties();
        private String[] keys;

        SystemPropertiesTableModel() {
            keys = props.keySet().toArray(new String[0]);
            Arrays.sort(keys);
        }

        public int getRowCount() {
            return keys.length;
        }

        public int getColumnCount() {
            return 2;
        }

        public Object getValueAt(int row, int column) {
            switch(column) {
            case 0: return keys[row];
            case 1: return props.get(keys[row]);
            default: throw new IllegalArgumentException("Column count"); //$NON-NLS-1$
            }
        }

        @Override
        public String getColumnName(int column) {
            switch(column) {
            case 0: return Messages.getString("AboutPanel.systemProperty"); //$NON-NLS-1$
            case 1: return Messages.getString("AboutPanel.systemPropertyValue"); //$NON-NLS-1$
            default: throw new IllegalArgumentException("Column count"); //$NON-NLS-1$
            }
        }
    }

    private JComponent initSysPropsTab() {
        JTable table = new JTable(new SystemPropertiesTableModel());

        // tailor column 1 width
        table.getColumnModel().getColumn(0).setMinWidth(200);

        // Want column 2 wide enough to show CLASSPATH
        table.getColumnModel().getColumn(1).setMinWidth(2000);

        table.setSize(table.getPreferredSize());

        return table;
    }
}