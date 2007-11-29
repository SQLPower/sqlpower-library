package ca.sqlpower.swingui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;
import javax.swing.text.Document;

import org.apache.log4j.Logger;

import ca.sqlpower.util.BrowserUtil;

public class SPSUtils {
	
	private static final Logger logger = Logger.getLogger(SPSUtils.class);
	
    /**
     * The URL for the SQL Power forum where users can get help and ask questions.
     */
    public static final String FORUM_URL = "http://www.sqlpower.ca/forum/";
	
	public static Action forumAction = new AbstractAction("Support on the Web",
            // Alas this is now static so the size can't be gotten from sprefs...
            SPSUtils.createIcon("world","New Project")) {
        public void actionPerformed(ActionEvent evt) {
            try {
                BrowserUtil.launch(FORUM_URL);
            } catch (IOException e) {
                SPSUtils.showExceptionDialogNoReport(getFrameFromActionEvent(evt),
                        "Could not launch browser for Forum View", e);
            }
        }
    };
    
    private static JFrame getFrameFromActionEvent(ActionEvent e) {
        if (e.getSource() instanceof Component) {
            Component c = (Component)e.getSource();
            while(c != null) {
                if (c instanceof Frame) {
    	        return (JFrame)c;
                }
                c = (c instanceof JPopupMenu) ? ((JPopupMenu)c).getInvoker() : c.getParent();
            }
        }
        return null;
    }
    
    /**
	 * Short-form convenience method for
	 * <code>new ArchitectSwingUtils.LabelValueBean(label,value)</code>.
	 */
	public static LabelValueBean lvb(String label, Object value) {
		return new LabelValueBean(label, value);
	}
	
    /**
	 * Useful for combo boxes where you want the user to see the label
	 * but the code needs the value (only useful when the value's
     * toString() method isn't).
	 */
	public static class LabelValueBean {
		String label;
		Object value;

		public LabelValueBean(String label, Object value) {
			this.label = label;
			this.value = value;
		}

		public String getLabel()  {
			return this.label;
		}

		public void setLabel(String argLabel) {
			this.label = argLabel;
		}

		public Object getValue()  {
			return this.value;
		}

		public void setValue(Object argValue) {
			this.value = argValue;
		}

		/**
		 * Just returns the label.
		 */
		public String toString() {
			return label;
		}
	}
	
	/**
     * Arrange for an existing JDialog or JFrame to close nicely when the ESC
     * key is pressed. Called with an Action, which will become the cancelAction
     * of the dialog.
     * <p>
     * Note: we explicitly close the dialog from this code.
     *
     * @param w The Window which you want to make cancelable with the ESC key.  Must
     * be either a JFrame or a JDialog.
     * @param cancelAction The action to invoke on cancelation, or null for nothing
     * @param disposeOnCancel If true, the window will be disposed after invoking the provided
     * action when the ESC key is pressed.  Otherwise, the provided action will be invoked,
     * but the window won't be closed.  If you set this to false, and don't provide an action,
     * nothing interesting will happen when ESC is pressed in your dialog.
     */
    public static void makeJDialogCancellable(
    		final Window w,
    		final Action cancelAction,
            final boolean disposeOnCancel) {

        JComponent c;
        if (w instanceof JFrame) {
            c = (JComponent) ((JFrame) w).getRootPane();
        } else if (w instanceof JDialog) {
            c = (JComponent) ((JDialog) w).getRootPane();
        } else {
            throw new IllegalArgumentException(
                    "The window argument has to be either a JFrame or JDialog." +
                    "  You provided a " + (w == null ? null : w.getClass().getName()));
        }

    	InputMap inputMap = c.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    	ActionMap actionMap = c.getActionMap();

    	inputMap.put(KeyStroke.getKeyStroke("ESCAPE"), "cancel");
    	actionMap.put("cancel", new AbstractAction() {
    		public void actionPerformed(ActionEvent e) {
                if ( cancelAction != null ) {
                    cancelAction.actionPerformed(e);
                }
                if (disposeOnCancel){
                    w.dispose();
                }
    		}
    	});
    }

    /**
     * Works like {@link #makeJDialogCancellable(Window, Action, boolean)}
     * with disposeOnCancel set to true.
     *
     * @param w The Window to attach the ESC event handler to
     * @param cancelAction The action to perform.  null is allowed: no custom
     * action will be performed, but the dialog will still be disposed on ESC.
     */
    public static void makeJDialogCancellable(
            final Window w,
            final Action cancelAction){
        makeJDialogCancellable(w, cancelAction, true);
    }
	
	/**
	 * Returns an ImageIcon with an image from the collection of
	 * icons in the classpath, or null if the path was invalid.  Copied from the Swing
	 * Tutorial.
	 *
	 * @param name The base of the filename from our graphics repository, such as
	 * "NewTable".  See the icons directory.
	 * @param size Either 16 or 24.
	 */
    public static ImageIcon createIcon(String name,
                                       String description,
                                       int size) {
        return createIcon(name+size, description);
    }

    /**
     * Returns an ImageIcon with an image from the collection of
     * icons in the classpath, or null if the path was invalid.  Copied from the Swing
     * Tutorial.
     *  
     * @param name The base of the filename from our graphics repository, such as
     * "NewTable".  See the icons directory.
     * @param description The description of the icon (maybe not used for anything).
     * @return
     */
    public static ImageIcon createIcon(String name,
                                       String description) {
        String realPath = "/icons/"+name+".png";
		logger.debug("Loading resource "+realPath);
		java.net.URL imgURL = SPSUtils.class.getResource(realPath);
        if (imgURL == null) {
            realPath = realPath.replace(".png", ".gif");
            imgURL = SPSUtils.class.getResource(realPath);
        }
		if (imgURL != null) {
			return new ImageIcon(imgURL, description);
		} else {
			logger.debug("Couldn't find file: " + realPath);
			return null;
		}
	}
    
	public static final FileFilter ARCHITECT_FILE_FILTER =
		new FileExtensionFilter("Architect Project Files", new String[] {"arc", "architect"});

	public static final FileFilter TEXT_FILE_FILTER =
		new FileExtensionFilter("Text Files ", new String[] {"txt"});

	public static final FileFilter SQL_FILE_FILTER =
		new FileExtensionFilter("SQL Script Files", new String[] {"sql","ddl"});

	public static final FileFilter INI_FILE_FILTER =
		new FileExtensionFilter(".INI Files", new String[] {"ini"});

	public static final FileFilter EXE_FILE_FILTER =
		new FileExtensionFilter(".EXE Files", new String[] {"exe"});

	public static final FileFilter JAR_ZIP_FILE_FILTER =
		new FileExtensionFilter("Java JAR Files", new String[] {"jar", "zip"});

	public static final FileFilter LOG_FILE_FILTER =
		new FileExtensionFilter("Log Files", new String[] {"log"});

    public static final FileFilter XML_FILE_FILTER =
        new FileExtensionFilter("XML Files", new String[] {"xml"});

    public static final FileFilter PDF_FILE_FILTER =
        new FileExtensionFilter("Portable Document (PDF) Files", new String[] {"pdf"});

    public static final FileFilter CSV_FILE_FILTER =
        new FileExtensionFilter("Comma-Separated Value Files", new String[] {"csv"});

    public static final FileFilter HTML_FILE_FILTER =
        new FileExtensionFilter("HTML Files", new String[] {"html"});

    public static final FileFilter BATCH_FILE_FILTER =
        new FileExtensionFilter("Batch Scripts", new String[] {"bat"});
    
    public static class FileExtensionFilter extends FileFilter {

		protected LinkedHashSet<String> extensions;
		protected String name;

		/**
		 * Creates a new filter which only accepts directories and
		 * files whose names end with a dot "." followed by one of the
		 * given strings.
		 *
		 * @param name The name of this filter to show to the user
		 * @param extensions an array of lowercase filename extensions.
		 */
		public FileExtensionFilter(String name, String[] extensions) {
			this.name = name;
			this.extensions = new LinkedHashSet<String>(Arrays.asList(extensions));
		}

		public String toString() {
			StringBuffer s = new StringBuffer();
			s.append(name);
			s.append(":");
			s.append(extensions.toString());
			return s.toString();
		}
		public boolean accept(File f) {
			return f.isDirectory() || extensions.contains(getExtension(f));
		}

		public String getDescription() {
			return name;
		}

		/*
		 * Get the extension of a file.
		 */
		public static String getExtension(File f) {
			String ext = "";
			String s = f.getName();
			int i = s.lastIndexOf('.');

			if (i > 0 &&  i < s.length() - 1) {
				ext = s.substring(i+1).toLowerCase();
			}
			return ext;
		}

		/*
		 * Get the extension of a filter.
		 */
		public String getFilterExtension(Integer index) {
			List<String> l = new ArrayList<String>(extensions);
			int i;

			if ( index == null ||
					index.intValue() < 0 ||
					index.intValue() >= l.size() )
				i = 0;
			else
				i = index.intValue();

			if ( l.size() > 0 )
				return l.get(i);
			return null;


		}
	}

	/**
	 * Tries very hard to create a JDialog which is owned by the parent
	 * Window of the given component.  However, if the component does not
	 * have a Window ancestor, or the component has a Window ancestor that
	 * is not a Frame or Dialog, this method instead creates an unparented
	 * JDialog which is always-on-top.
	 * <P>
	 * This method was shamelessly stolen from the grodbots project,
	 * http://grodbots.googlecode.com/svn/trunk/src/net/bluecow/robot/RobotUtils.java
	 * 
	 * @param owningComponent The component that should own this dialog.
	 * @param title The title for the dialog.
	 * @return A JDialog that is either owned by the Frame or Dialog ancestor of
	 * owningComponent, or not owned but set to be alwaysOnTop.
	 */
	public static JDialog makeOwnedDialog(Component owningComponent, String title) {
	    Window owner = getWindowInHierarchy(owningComponent);
	    if (owner instanceof Frame) {
	        return new JDialog((Frame) owner, title);
	    } else if (owner instanceof Dialog) {
	        return new JDialog((Dialog) owner, title);
	    } else {
	        JDialog d = new JDialog();
	        d.setTitle(title);
	        d.setAlwaysOnTop(true);
	        return d;
	    }
	}
	
	 /**
     * Returns the first Window in the hierarchy above or at c,
     * or null if c is not contained inside a Window. Different from 
     * {@link SwingUtilities#getWindowAncestor(Component)} in that
     * it checks first if c is a Window. Use this if c could be
     * a Window.
     */
    public static Window getWindowInHierarchy(Component c) {
        if (c instanceof Window) {
        	return (Window) c;
        } else {
        	return SwingUtilities.getWindowAncestor(c);
        }
    }
	
    /**
     * Displays a dialog box with the given message and exception,
     * allowing the user to examine the stack trace, but do NOT generate
     * a report back to SQLPower web site.  The dialog's
     * parent component will be set to null.
     * 
     * @deprecated This method will display a dialog box that is not properly
     * parented. Use {@link #showExceptionDialogNoReport(Component, String, Throwable)} instead.
     */
	public static JDialog showExceptionDialogNoReport(String string, Throwable ex) {
        return displayExceptionDialog(null, string, null, ex);
	}
	
	/** Displays a dialog box with the given message and exception,
	 * returning focus to the given component. Intended for use
	 * on panels like the CompareDMPanel, so focus works better.
	 * 
	 * @param parent
	 * @param message
	 * @param throwable
	 */
	public static JDialog showExceptionDialogNoReport(Component parent, String string, Throwable ex) {
		return displayExceptionDialog(parent, string, null, ex);
	}
	
    /**
     * Displays a dialog box with the given message and submessage and exception,
     * allowing the user to examine the stack trace, but do NOT generate
     * a report back to SQLPower web site.
     * 
     * @param parent The parent component that will own the dialog
     * @param message A user visible string that should explain the problem
     * @param subMessage A second string to give finer-grained detail to the user
     * @param throwable The exception that caused the problem
     */
    public static JDialog showExceptionDialogNoReport(Component parent, String message, String subMessage, Throwable throwable) {
        return displayExceptionDialog(parent, message, subMessage, throwable);
    }

    /**
     * XXX To get rid of this ugly static variable,
     * the Session should handle all errors, and have
     * all these methods require an Icon as an argument.
     */
    static ImageIcon masterIcon;
    
    /**
     * Displays a dialog box with the given message and submessage and exception,
     * allowing the user to examine the stack trace, but do NOT generate
     * a report back to SQLPower web site.
     * 
     * @param parent The parent component that will own the dialog
     * @param message A user visible string that should explain the problem
     * @param subMessage A second string to give finer-grained detail to the user
     * @param throwable The exception that caused the problem
     * 
     * @return The JDialog to be displayed with the exception.
     */
    private static JDialog displayExceptionDialog(
            final Component parent,
            final String message,
            final String subMessage,
            final Throwable throwable) {
        JDialog dialog;
        Window owner = parent == null? null: getWindowInHierarchy(parent);
        if (owner instanceof JFrame) {
            JFrame frame = (JFrame) owner;
            dialog = new JDialog(frame, "Error Report");
            if (masterIcon != null) {
                // Ugly temporary workaround for the fact that MM uses
                // some Architect code, which we think is creating a
                // JFrame with the Architect icon on it...
                frame.setIconImage(masterIcon.getImage());
            }
        } else if (owner instanceof Dialog) {
            dialog = new JDialog((Dialog)owner, "Error Report");
        } else {
            logger.error(
                    String.format("dialog parent component %s is neither JFrame nor JDialog", owner));
            dialog = new JDialog((Frame)null, "Error report");
        }
        logger.debug("displayExceptionDialog: showing exception dialog for:", throwable);

        ((JComponent)dialog.getContentPane()).setBorder(
                BorderFactory.createEmptyBorder(10, 10, 5, 5));

        // Details information
        Throwable t = throwable;
        StringWriter stringWriter = new StringWriter();
        final PrintWriter traceWriter = new PrintWriter(stringWriter);
        do {
            t.printStackTrace(traceWriter);
            if (rootCause(t) instanceof SQLException) {
                t = ((SQLException) rootCause(t)).getNextException();
                if (t != null) {
                    traceWriter.println("Next Exception:");
                }
            } else {
                t = null;
            }
        } while (t != null);
        traceWriter.close();

        JPanel top = new JPanel(new GridLayout(0, 1, 5, 5));

        StringBuilder labelText = new StringBuilder();
        labelText.append("<html><font color='red' size='+1'>");
        labelText.append(message == null ? "Unexpected error" : nlToBR(message));
        labelText.append("</font>");
        if (subMessage != null) {
            labelText.append("<p>");
            labelText.append(subMessage);
        }
        JLabel messageLabel = new JLabel(labelText.toString());
        top.add(messageLabel);

        JLabel errClassLabel =
            new JLabel("<html><b>Exception type</b>: " + nlToBR(throwable.getClass().getName()));
        top.add(errClassLabel);
        String excDetailMessage = throwable.getMessage();
        excDetailMessage = trimToClosestNL(excDetailMessage, 100, 25);
        if (excDetailMessage != null) {
            top.add(new JLabel("<html><b>Detail string</b>: " + nlToBR(excDetailMessage)));
            if (throwable.getCause() != null) {
                Throwable root;
                for (root = throwable.getCause(); root.getCause() != null; root = root.getCause()) {
                	if (root.getMessage() != null) {
                		top.add(new JLabel("<html><b>Root Cause</b>: " + nlToBR(root.getMessage())));
                	}
                }
            }

        }

        final JButton detailsButton = new JButton("Show Details");
        final JPanel detailsButtonPanel = new JPanel();
        detailsButtonPanel.add(detailsButton);

        final JButton forumButton = new JButton(forumAction);
        detailsButtonPanel.add(forumButton);
        top.add(detailsButtonPanel);

        dialog.add(top, BorderLayout.NORTH);
        final JScrollPane detailScroller =
            new JScrollPane(new JTextArea(stringWriter.toString()));

        final JPanel messageComponent = new JPanel(new BorderLayout());
        messageComponent.add(detailScroller, BorderLayout.CENTER);
        messageComponent.setPreferredSize(new Dimension(700, 400));

        final JComponent fakeMessageComponent = new JComponent() {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(700, 0);
            }
        };

        final JDialog finalDialogReference = dialog;
        finalDialogReference.add(fakeMessageComponent, BorderLayout.CENTER);
        ActionListener detailsAction = new ActionListener() {
            boolean showDetails = true;
            public void actionPerformed(ActionEvent e) {
                if (showDetails) {
                    finalDialogReference.remove(fakeMessageComponent);
                    finalDialogReference.add(messageComponent, BorderLayout.CENTER);
                    detailsButton.setText("Hide Details");
                } else /* hide details */ {
                    finalDialogReference.remove(messageComponent);
                    finalDialogReference.add(fakeMessageComponent, BorderLayout.CENTER);
                    detailsButton.setText("Show Details");
                }
                finalDialogReference.pack();

                Rectangle dialogBounds = finalDialogReference.getBounds();
                Rectangle screenBounds = finalDialogReference.getGraphicsConfiguration().getBounds();
                if ( !screenBounds.contains(dialogBounds) ) {
                    int x = dialogBounds.x;
                    int y = dialogBounds.y;
                    if (screenBounds.x+screenBounds.width < dialogBounds.x + dialogBounds.width){
                        x = dialogBounds.x - (dialogBounds.x + dialogBounds.width - screenBounds.x - screenBounds.width);
                    }
                    if (screenBounds.y+screenBounds.height < dialogBounds.y + dialogBounds.height){
                        y = dialogBounds.y - (dialogBounds.y + dialogBounds.height - screenBounds.y - screenBounds.height);
                    }
                    if (screenBounds.x > x){
                        x = screenBounds.x;
                    }
                    if (screenBounds.y > y){
                        y = screenBounds.y;
                    }
                    finalDialogReference.setLocation(x,y);
                }
                showDetails = ! showDetails;
            }
        };
        detailsButton.addActionListener(detailsAction);
        JButton okButton = new JButton("OK");
        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                finalDialogReference.dispose();
                finalDialogReference.setVisible(false);
            }
        });
        JPanel bottom = new JPanel();
        bottom.add(okButton);
        dialog.add(bottom, BorderLayout.SOUTH);
        dialog.pack();
        dialog.setLocationRelativeTo(owner);

        dialog.setVisible(true);
        return dialog;
    }
    
    /**
     * Trims the given message if it's longer than the given limit by finding
     * the closest new line within the given offset.
     * 
     * @param msg
     *            The message to trim. Null is allowed, but obviously no
     *            trimming will be performed.
     * @param msgLimit
     *            The threshold length for msg. If msg.length() > msgLimit,
     *            trimming will be performed. Otherwise, no trimming will be
     *            performed.
     * @param offset
     *            The number of characters beyond msgLimit which can be
     *            tolerated in the trimmed string. This allows the message to be
     *            trimmed at the next newline, as long as that newline falls
     *            within a reasonable distance from the threshold limit.
     * @return If msg was shorter than msgLimit, msg is returned. Otherwise, if
     *         msg was longer than msgLimit, a trimmed version of msg with "..."
     *         appended will be returned. Finally, if msg was null, null will be
     *         returned.
     */
    private static String trimToClosestNL(String msg, int msgLimit, int offset) {
        if (msg == null) return null;
    	if (msg.length() > msgLimit) {
        	int lastNL = msg.indexOf("\n", msgLimit - offset);
        	int nextNL = msg.indexOf("\n", msgLimit);
        	int endIndex = lastNL;
        	if (lastNL < 0 || lastNL > msgLimit + offset) {
        		endIndex = msgLimit;
        	} else if ((lastNL >= msgLimit && lastNL <= msgLimit + offset) ||
        			nextNL < 0 || nextNL > msgLimit + offset){
        		// use lastNL as endIndex
        	} else {
        		 if (msgLimit - lastNL > nextNL - msgLimit) {
        			 endIndex = nextNL;
        		 }
        	}
        	msg = msg.substring(0, endIndex) + " ...";
        }
    	return msg;
    }
    
    /**
     * Follows the chain of exceptions (using the getCause() method) to find the
     * root cause, which is the exception whose getCause() method returns null.
     * 
     * @param t The Throwable for which you want to know the root cause.  Must not
     * be null.
     * @return The ultimate cause of t.  This may be t itself.
     */
    private static Throwable rootCause(Throwable t) {
        while (t.getCause() != null) t = t.getCause();
        return t;
    }

    /**
     * Simple convenience routine to replace all \n's with <br>
     * @param s
     * @return
     */
    static String nlToBR(String s) {
        // Do NOT xml-ify the BR tag until Swing's HTML supports this.
    	logger.debug("String s is " + s);
        return s.replaceAll("\n", "<br>");
    }
	
	/**
	 * Returns the unqualified name (no package name) of the given object's class.
	 * 
	 * @param o The object whose class name to extract and return.  This argument
	 * must not be null.
	 * @return The part of the full class name that follows the last "." character,
	 * or the whole name if there are no dots (because o's class is in the default
	 * package).
	 */
	public static String niceClassName(Object o) {
        Class<?> c = o.getClass();
        String name = c.getName();
        int lastDot = name.lastIndexOf('.');
        if (lastDot == -1)
            return name;
        return name.substring(lastDot + 1);
    }
    
    /**
     * Shows a file chooser and saves the given Document in the user-selected location.
     * If the user chooses to overwrite an existing file, they will be prompted to
     * confirm the overwrite.
     * 
     * @param owner The component that will own the file chooser dialog
     * @param doc the document to save
     * @param filter The filename extension filter
     */
    public static boolean saveDocument(Component owner, Document doc, FileExtensionFilter filter) {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(filter);
        int returnVal = fc.showSaveDialog(owner);

        while (true) {
            if (returnVal == JFileChooser.CANCEL_OPTION) {
                return false;
            } else if (returnVal == JFileChooser.APPROVE_OPTION) {

                File file = fc.getSelectedFile();
                String fileName = file.getPath();
                String fileExt = SPSUtils.FileExtensionFilter.getExtension(file);
                if (fileExt.length() == 0) {
                    file = new File(fileName + "."
                            + filter.getFilterExtension(new Integer(0)));
                }
                if (file.exists()) {
                    int choice = JOptionPane.showOptionDialog(
                                        owner,
                                        "Are your sure you want to overwrite this file?",
                                        "Confirm Overwrite",
                                        JOptionPane.YES_NO_OPTION,
                                        JOptionPane.QUESTION_MESSAGE, null,
                                        null, null);
                    boolean wantToOverwrite = (choice == JOptionPane.YES_OPTION);
                    if (!wantToOverwrite) {
                        returnVal = fc.showSaveDialog(owner);
                        continue;
                    }
                }
                return writeDocument(doc, file);
            }
        }
    }
    
    /**
     * Writes the text of the given document to the given file.  Reports any
     * exceptions encountered via the {@link #showExceptionDialogNoReport(String, Throwable)}
     * dialog.
     * 
     * @param doc The document to save
     * @param file The file to save to
     * @return True if the save was successful; false otherwise.
     */
    public static boolean writeDocument(Document doc, File file) {
        PrintWriter out = null;
        try {
            StringReader sr = new StringReader(doc.getText(0, doc.getLength()));
            BufferedReader br = new BufferedReader(sr);
            out = new PrintWriter(file);
            String s;
            while ((s = br.readLine()) != null) {
                out.println(s);
            }
            out.flush();
            return true;
        } catch (Exception e) {
            SPSUtils.showExceptionDialogNoReport("Could not save file", e);
            return false;
        } finally {
            if (out != null) out.close();
        }
    }

    /**
     * This method creates an arrowhead for a line. The formula used is derived
     * from solving the system of equations (x2 - x1)^2 + (y2 -y1)^2 = c^2 and
     * y1 = m * x1 + n. After solving the equation we get:
     * (m^2 + 1) * x1^2 + (-2 * x2 - 2 * y2 * m + 2 * m * n) * x1 +
     * (x2 ^ 2 + y2^2 - 2 * y2 * n + n^2 - c^2) = 0.
     * We can use this to find roots using the equation:
     * -b +/- root(b^2 -4ac) / 2a
     *
     * @param xHead
     *            The x position of the head of the line.
     * @param yHead
     *            The y position of the head of the line.
     * @param xTail
     *            The x position of the tail of the line.
     * @param yTail
     *            The y position of the tail of the line.
     * @param height
     *            The height of the arrowhead.
     * @param width
     *            The width of the base of the arrowhead.
     * @return A polygon containing the three points for the arrowhead.
     */
    public static Polygon createArrowhead(int xHead, int yHead, int xTail, int yTail, int height,
            int width) {
        Polygon polygon = new Polygon();
        polygon.addPoint(xHead, yHead);
        if (yHead == yTail) {
            if (xHead > xTail) {
       
                polygon.addPoint(xHead - height, yHead - width / 2);
                polygon.addPoint(xHead - height, yHead + width / 2);
            } else {
                polygon.addPoint(xHead + height, yHead - width / 2);
                polygon.addPoint(xHead + height, yHead + width / 2);
            }
            return polygon;
        }
       
        if (xHead == xTail) {
            if (yHead > yTail) {
                polygon.addPoint(xHead - width / 2, yHead - height);
                polygon.addPoint(xHead + width / 2, yHead - height);
            } else {
                polygon.addPoint(xHead - width / 2, yHead + height);
                polygon.addPoint(xHead + width / 2, yHead + height);
            }
            return polygon;
        }
       
        double m = (yHead - yTail) / (xHead - xTail);
        double n = yHead - m * xHead;
       
        double a = m * m + 1;
        double b = -2 * xHead - 2 * yHead * m + 2 * m * n;
        double c = xHead * xHead + yHead * yHead - 2 * yHead * n + n * n - height * height;
       
        //Get the root between the head and tail
        double xBase = (- b + Math.sqrt(b * b - 4 * a * c)) / (2 * a);
        if (!((xBase < xHead && xBase > xTail) || (xBase > xHead && xBase < xTail))) {
            xBase = (- b - Math.sqrt(b * b - 4 * a * c)) / (2 * a);
        }
        double yBase = m * xBase + n;
        logger.debug("The base point is (" + xBase + ", " + yBase + ")");

        double mInv = -1 / m;
        double nInv = yBase - mInv * xBase;
       
        a = mInv * mInv + 1;
        b = -2 * xBase - 2 * yBase * mInv + 2 * mInv * nInv;
        c = xBase * xBase + yBase * yBase - 2 * yBase * nInv + nInv * nInv - (width / 2) * (width /2);
       
        int xPoint = (int)((- b + Math.sqrt(b * b - 4 * a * c)) / (2 * a));
        int yPoint = (int)(mInv * xPoint + nInv);
        logger.debug(" x is " + xPoint + " y is " + yPoint);
        polygon.addPoint(xPoint, yPoint);
       
        xPoint = (int)((- b - Math.sqrt(b * b - 4 * a * c)) / (2 * a));
        yPoint = (int)(mInv * xPoint + nInv);
        polygon.addPoint(xPoint, yPoint);

        return polygon;
    }

}
