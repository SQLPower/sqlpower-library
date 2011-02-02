/*
 * Copyright (c) 2008, SQL Power Group Inc.
 *
 * This file is part of SQL Power Library.
 *
 * SQL Power Library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * SQL Power Library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>. 
 */
package ca.sqlpower.swingui.db;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.apache.log4j.Logger;

import ca.sqlpower.sql.JDBCDataSource;
import ca.sqlpower.sql.JDBCDataSourceType;
import ca.sqlpower.sql.SPDataSource;
import ca.sqlpower.swingui.DataEntryPanel;
import ca.sqlpower.swingui.Messages;
import ca.sqlpower.swingui.ProgressWatcher;
import ca.sqlpower.swingui.SPSUtils;
import ca.sqlpower.util.Monitorable;

public class JDBCDriverPanel extends JPanel implements DataEntryPanel {
	
	/**
	 * The level in the tree that the drivers are located at. 
	 */
	public static final int DRIVER_LEVEL = 2;

	private static class DriverTreeCellRenderer extends DefaultTreeCellRenderer implements TreeCellRenderer {
        
        private Icon jarFileIcon =
            new ImageIcon(JDBCDriverPanel.class.getClassLoader().getResource("ca/sqlpower/swingui/db/folder_wrench.png")); //$NON-NLS-1$
        private Icon driverIcon =
            new ImageIcon(JDBCDriverPanel.class.getClassLoader().getResource("ca/sqlpower/swingui/db/wrench.png")); //$NON-NLS-1$
        private Icon jarFileErrorIcon =
            new ImageIcon(JDBCDriverPanel.class.getClassLoader().getResource("ca/sqlpower/swingui/db/folder_error.png")); //$NON-NLS-1$
        private Icon driverErrorIcon =
            new ImageIcon(JDBCDriverPanel.class.getClassLoader().getResource("ca/sqlpower/swingui/db/error.png")); //$NON-NLS-1$
        
	    @Override
	    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
            int level = node.getLevel();
            if (level == 0) {
                // root is invisible in the driver tree
            } else if (level == 1) {
                setIcon(jarFileIcon);
                for (int i = 0; i < node.getChildCount(); i++){
                    if(((DefaultMutableTreeNode) node.getChildAt(i)).getUserObject() instanceof Throwable){
                        setIcon(jarFileErrorIcon);
                        break;
                    }
                }
            } else if (level == DRIVER_LEVEL) {
                if (node.getUserObject() instanceof Throwable) {
                    setForeground(Color.RED);
                    setIcon(driverErrorIcon);
                    setText(Messages.getString("JDBCDriverPanel.jarFileNotFound"));
                } else {
                    setIcon(driverIcon);
                }
            } else {
                throw new IllegalStateException("This renderer doesn't know how to handle node depth "+level); //$NON-NLS-1$
            }
            return this;
	    }
    }

	private static final Logger logger = Logger.getLogger(JDBCDriverPanel.class);

	/**
	 * The current data source type (whose JDBC driver search path we're editting).
     * This value will be null when there is no "current" data source type to edit.
	 */
	private JDBCDataSourceType dataSourceType;

	/**
	 * This view shows the driver JAR files and the JDBC drivers they
	 * contain.
	 */
    private JTree driverTree;

	/**
	 * This tree model holds the registered JAR files under the root,
	 * and lists the JDBC driver classes as children of each JAR file.
	 */
    private DefaultTreeModel dtm;

	/**
	 * The file choosed used by the add action.
	 */
    private JFileChooser fileChooser;

	/**
	 * progress bar stuff
	 */
    private JProgressBar progressBar;
    private JLabel progressLabel;

    private JButton delButton;
    private JButton addButton;
    private DefaultMutableTreeNode rootNode;

    private final URI serverBaseURI;

    /**
     * Creates an interactive GUI for users to find JDBC drivers inside a
     * collection of JAR files. The JAR files can be specified as built-in JARs
     * on the classpath, JARs that reside on a remote server (such as a SQL
     * Power Enterprise Server), or local files.
     * <p>
     * This is part of a larger system rooted at {@link DataSourceTypeDialogFactory}.
     * 
     * @param serverBaseURI
     *            The base URI to the server where JAR files are stored. Can be
     *            null if server JAR specs are not in use.
     * @see {@link SPDataSource#jarSpecToFile(String, ClassLoader, URI)} for a
     *      description of which types of file specifications are allowed.
     */
	public JDBCDriverPanel(URI serverBaseURI) {
	    this.serverBaseURI = serverBaseURI;
	    
        // TODO default to most recent JDBC driver location
		fileChooser = new JFileChooser();

		setLayout(new BorderLayout());
		rootNode = new DefaultMutableTreeNode("The Root"); //$NON-NLS-1$
		dtm = new DefaultTreeModel(rootNode);
		driverTree = new JTree(dtm);
		driverTree.setRootVisible(false);
        // Let the user delete multiple driver jars at once
        driverTree.getSelectionModel().setSelectionMode(
                TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
		driverTree.addTreeSelectionListener(new TreeSelectionListener() {
			public void valueChanged(TreeSelectionEvent e) {
				// enabled when a driver has been selected
				delButton.setEnabled(driverTree.getSelectionPath() != null);
			}
		});
        driverTree.setCellRenderer(new DriverTreeCellRenderer());
        
		add(new JScrollPane(driverTree), BorderLayout.CENTER);

		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		buttonPanel.add(addButton = new JButton(new AddAction()));
		buttonPanel.add(delButton = new JButton(new DelAction()));
		delButton.setEnabled(false);
		addButton.setEnabled(false);
		add(buttonPanel, BorderLayout.NORTH);

		JPanel progressPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		progressBar = new JProgressBar();
		progressBar.setStringPainted(true); //get space for the string
		progressBar.setVisible(false);
		progressPanel.add(progressBar);
		progressLabel = new JLabel(Messages.getString("JDBCDriverPanel.scanningForJdbcDrivers")); //$NON-NLS-1$
		progressLabel.setVisible(false);
		progressPanel.add(progressLabel);
        progressPanel.setPreferredSize(new Dimension(300, progressBar.getPreferredSize().height + 20));
		add(progressPanel, BorderLayout.SOUTH);
        
        setPreferredSize(new Dimension(400, 400));
	}

	/**
	 * Copies the pathnames to all the JAR files to
	 * ArchitectSession.addDriverJar().
	 */
	public boolean applyChanges() {
		logger.debug("applyChanges"); //$NON-NLS-1$
        
        List<String> driverList = new ArrayList<String>();
		
		for (int i = 0, n = dtm.getChildCount(dtm.getRoot()); i < n; i++) {
			driverList.add(((DefaultMutableTreeNode) dtm.getChild(dtm.getRoot(), i)).getUserObject().toString());
		}
		if (dataSourceType != null) dataSourceType.setJdbcJarList(driverList);
		return true;
	}

	/**
	 * Does nothing.
	 */
	public void discardChanges() {
		// really do nothing! 
		//editDsType(dataSourceType);
	}

    /**
     * Switches to edit the given data source type.
     */
	public void editDsType(JDBCDataSourceType dst) {
        dataSourceType = dst;
	    dtm.setRoot(new DefaultMutableTreeNode());
        if (dst != null) {
            doLoad(dataSourceType.getJdbcJarList());
        } 
        
        // enabled when a datasource has been selected
        addButton.setEnabled(dst != null);
    }
    
	protected class AddAction extends AbstractAction {
		public AddAction() {
			super(Messages.getString("JDBCDriverPanel.addJarActionName")); //$NON-NLS-1$
		}

		public void actionPerformed(ActionEvent e) {
				fileChooser.addChoosableFileFilter(SPSUtils.JAR_ZIP_FILE_FILTER);
				fileChooser.setMultiSelectionEnabled(true);
				int returnVal = fileChooser.showOpenDialog(JDBCDriverPanel.this);
				if(returnVal == JFileChooser.APPROVE_OPTION) {
					File[] files = fileChooser.getSelectedFiles();
					List list = new ArrayList();
					for(int ii=0; ii < files.length;ii++) {
						list.add(files[ii].getAbsolutePath());
					}
                    
                    // always add the files to the data source type. if there are problems,
                    // they will be made visible to the user via the tree UI
					for (int i = 0; i < files.length; i++) {
					    dataSourceType.addJdbcJar(files[i].getAbsolutePath());
					}
                    
					doLoad(list);
				}
		}
	}

	/**
	 * Loads the given List of driver names into the tree, then starts
     * a worker thread that searches for implementations of the JDBC Driver
     * interface in them.  As the worker finds JDBC Drivers in the JARs,
     * it will add them to the tree.
	 */
	private void doLoad(List<String> list) {
        logger.debug("about to start a worker", new Exception()); //$NON-NLS-1$
		LoadJDBCDrivers ljd = new LoadJDBCDrivers(list);
		LoadJDBCDriversWorker worker = new LoadJDBCDriversWorker(ljd);
		
		// Create a progress bar to show JDBC driver load progress, and hide when finished
		ProgressWatcher pw = new ProgressWatcher(progressBar,ljd,progressLabel);
		pw.setHideLabelWhenFinished(true);
		pw.start();
		
		new Thread(worker).start();
	}

	private class DelAction extends AbstractAction {
		public DelAction() {
			super(Messages.getString("JDBCDriverPanel.removeJarActionName")); //$NON-NLS-1$
		}

		public void actionPerformed(ActionEvent e) {
            for (TreePath p : driverTree.getSelectionPaths()) {
                logger.debug(String.format("DelAction: p=%s, pathCount=%d", p, p.getPathCount())); //$NON-NLS-1$
                if (p != null && p.getPathCount() >= 2) {
                    logger.debug("Removing: " + p.getPathComponent(1)); //$NON-NLS-1$
                    dtm.removeNodeFromParent((MutableTreeNode) p.getPathComponent(1));
                    dataSourceType.removeJdbcJar(p.getPathComponent(1).toString());
                }
            }
			delButton.setEnabled(false);
		}
	}

    private class LoadJDBCDriversWorker implements Runnable {
		LoadJDBCDrivers ljd;
		LoadJDBCDriversWorker (LoadJDBCDrivers ljd) {
			this.ljd = ljd;
		}
		public void run() {
			ljd.execute();
		}
	}

    private class LoadJDBCDrivers implements Monitorable  {

		public boolean hasStarted = false;
		public boolean finished = false;
		private List<String> driverJarList = null;


		private int jarCount = 0; // which member of the JAR file list are we currently processing
		private JDBCScanClassLoader cl = null;

        /**
         * A monitorable scanner that finds all JDBC drivers within a given set
         * of JAR files.
         * 
         * @param driverJarList
         *            The list of JAR locations. These can be absolute or
         *            relative path names, or special prefixed values as
         *            specified in
         *            {@link SPDataSource#jarSpecToFile(String, ClassLoader, URI)}
         *            .
         * @see {@link SPDataSource#jarSpecToFile(String, ClassLoader, URI)}
         */
		public LoadJDBCDrivers(List<String> driverJarList) {
            this.driverJarList = driverJarList;
			logger.debug("in constructor, setting finished to false..."); //$NON-NLS-1$
			finished = false;
		}

		public Integer getJobSize() {
			return new Integer(driverJarList.size() * 1000);
		}

		public int getProgress() {
			double fraction = 0.0;
			if (cl != null) {
				fraction = cl.getFraction();
			}
		    int progress = (jarCount - 1) * 1000 + (int) (fraction * 1000.0);
			if (logger.isDebugEnabled()) logger.debug("******************* progress is: " + progress + " of " + getJobSize()); //$NON-NLS-1$ //$NON-NLS-2$
			return progress;
		}

		public boolean isFinished() {
			return finished;
		}

        /**
         * The driver scan cannot be cancelled.  This method has no effect.
         */
        public void setCancelled(boolean cancelled) {
            // job not cancellable, do nothing
        }

        /**
         * The driver scan cannot be cancelled.  This method always returns
         * false.
         */
        public boolean isCancelled() {
            return false;
        }

		public boolean hasStarted() {
			return hasStarted;
		}

		public String getMessage () {
			return null; // no messages returned from this job
		}

		public void execute() {
			hasStarted = true;
			try {
				Iterator it = driverJarList.iterator();
				while (it.hasNext()) {
					// initialize counters
					jarCount++;
					logger.debug("**************** processing file #" + jarCount + " of " + driverJarList.size()); //$NON-NLS-1$ //$NON-NLS-2$
					String path = (String) it.next();
                    URL jarLocation = JDBCDataSource.jarSpecToFile(path, getClass().getClassLoader(), serverBaseURI);
					if (jarLocation != null) {
                        addJarLocation(jarLocation);
                    }
				}
				finished = true;
				logger.debug("done loading (normal operation), setting finished to true."); //$NON-NLS-1$
			} catch ( Exception exp ) {
				logger.error("something went wrong in LoadJDBCDrivers worker thread!",exp); //$NON-NLS-1$
			} finally {
				finished = true;
				hasStarted = false;
				logger.debug("done loading (error condition), setting finished to true."); //$NON-NLS-1$
			}
		}

		private void addJarLocation(URL url) {
			DefaultMutableTreeNode root = (DefaultMutableTreeNode) dtm.getRoot();
            DefaultMutableTreeNode node = new DefaultMutableTreeNode(url.toString());
            dtm.insertNodeInto(node, root, root.getChildCount());
			try {
			    cl = new JDBCScanClassLoader(url);
				List driverClasses = cl.scanForDrivers();
				logger.info("Found drivers: "+driverClasses); //$NON-NLS-1$
				Iterator it = driverClasses.iterator();
				while (it.hasNext()) {
					DefaultMutableTreeNode child = new DefaultMutableTreeNode(it.next());
					dtm.insertNodeInto(child, node, node.getChildCount());
				}
			} catch (IOException ex) {
				logger.warn("I/O Error reading JAR file",ex); //$NON-NLS-1$
                DefaultMutableTreeNode child = new DefaultMutableTreeNode(ex);
                dtm.insertNodeInto(child, node, node.getChildCount());
			}
            TreePath path = new TreePath(node.getPath());
            driverTree.expandPath(path);
            driverTree.scrollPathToVisible(path);
		}
	}

	/**
	 * Scans a jar file for instances of java.sql.Driver.
	 */
	private class JDBCScanClassLoader extends ClassLoader {

		private List drivers;
		private int count = 0;
        private JarURLConnection jarConnection;
        private JarFile jf;

        /**
         * Creates a class loader that can scan the given JAR for JDBC drivers.
         * Uses this class's class loader as its parent.
         * 
         * @param jarLocation
         *            The JAR to scan. This URL must <i>not</i> be a jar: URL;
         *            it will be converted to one within this constructor.
         * @throws IOException
         */
		public JDBCScanClassLoader(URL jarLocation) throws IOException {
			super();
			URL jarURL = new URL("jar:" + jarLocation + "!/");
            jarConnection = (JarURLConnection) jarURL.openConnection();
            jf = jarConnection.getJarFile();
		}

		public synchronized double getFraction() {
			double retval = 0.0;
			if (jf != null) {
				retval = (double)count/(double)jf.size();
			}
			return retval;
		}

		/**
		 * Returns a list of Strings naming the subclasses of
		 * java.sql.Driver which exist in this class loader's jar
		 * file.
		 */
		public List scanForDrivers() {
			drivers = new LinkedList();
			logger.debug("********* " + jf.getName() + " has " + jf.size() + " files."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			for (Enumeration entries = jf.entries(); entries.hasMoreElements(); ) {
				count++;
				ZipEntry ent = (ZipEntry) entries.nextElement();
				if (ent.getName().endsWith(".class")) { //$NON-NLS-1$
					try {
						// drop the .class from the name
						String [] s = ent.getName().split("\\."); //$NON-NLS-1$
						// look for the class using dots instead of slashes
						findClass(s[0].replace('/','.'));
					} catch (ClassFormatError ex) {
						logger.warn("JAR entry "+ent.getName()+" ends in .class but is not a class", ex); //$NON-NLS-1$ //$NON-NLS-2$
					} catch (NoClassDefFoundError ex) {
						logger.warn("JAR does not contain dependency needed by: " + ent.getName()); //$NON-NLS-1$
					} catch (Throwable ex) {
						logger.warn("Unexpected exception while scanning JAR file "+jf.getName(), ex); //$NON-NLS-1$
					}
				}
			}
			//jf.close();
			return drivers;
		}

		/**
		 * Searches this ClassLoader's jar file for the given class.
		 *
		 * @throws ClassNotFoundException if the class can't be
		 * located.
		 */
		protected Class findClass(String name)
			throws ClassNotFoundException {
			//logger.debug("Looking for class "+name);
			try {
				ZipEntry ent = jf.getEntry(name.replace('.', '/')+".class"); //$NON-NLS-1$
				if (ent == null) {
					throw new ClassNotFoundException("No class file "+name+" is in my jar file"); //$NON-NLS-1$ //$NON-NLS-2$
				}
				// can we find out here if it was already loaded???
				Class clazz = findLoadedClass(name);
				if (clazz != null) {
					return clazz;
				}
				// haven't seen this before, so go get it...
				InputStream is = jf.getInputStream(ent);
				return readAndCheckClass(is, (int) ent.getSize(), name);
			} catch (IOException ex) {
				throw new ClassNotFoundException("IO Exception reading class from jar file", ex); //$NON-NLS-1$
			}
		}

		private Class readAndCheckClass(InputStream is, int size, String expectedName)
			throws IOException, ClassFormatError {
			byte[] buf = new byte[size];
			int offs = 0, n;
            
			while ( (n = is.read(buf, offs, size-offs)) >= 0 && offs < size) {
				offs += n;
			}
            final int total = offs;
			if (total != size) {
				logger.warn("Only read "+total+" bytes of class " //$NON-NLS-1$ //$NON-NLS-2$
							+expectedName+" from JAR file; exptected "+size); //$NON-NLS-1$
			}
			Class clazz = defineClass(expectedName, buf, 0, total);
			if (java.sql.Driver.class.isAssignableFrom(clazz)) {
				logger.info("Found jdbc driver "+clazz.getName()); //$NON-NLS-1$
				drivers.add(clazz.getName());
			}
			return clazz;
		}

	}

	public JPanel getPanel() {
		return this;
	}

	public boolean hasUnsavedChanges() {
        // TODO return whether this panel has been changed
		return true;
	}
	
	public void addDriverTreeSelectionListener(TreeSelectionListener tsl) {
		driverTree.addTreeSelectionListener(tsl);
	}
	
	public void removeDriverTreeSelectionListener(TreeSelectionListener tsl) {
		driverTree.removeTreeSelectionListener(tsl);
	}
}
