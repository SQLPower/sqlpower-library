package ca.sqlpower.swingui;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.Popup;
import javax.swing.PopupFactory;
import javax.swing.ScrollPaneConstants;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.filechooser.FileFilter;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;

import net.miginfocom.swing.MigLayout;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.http.params.BasicHttpParams;
import org.apache.log4j.Logger;

import ca.sqlpower.util.BrowserUtil;
import ca.sqlpower.util.SQLPowerUtils;
import ca.sqlpower.util.Version;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

public class SPSUtils {
	
	private static final Logger logger = Logger.getLogger(SPSUtils.class);
	
	/**
	 * The Key for Multiple select on different Operating System.
	 */
	public static final int MULTISELECT_MASK;
    static{
        if (System.getProperty("mrj.version") != null) {
            MULTISELECT_MASK = InputEvent.META_DOWN_MASK;
        } else {
            MULTISELECT_MASK = InputEvent.CTRL_DOWN_MASK;
        }
    }
    
    /**
     * The URL for the SQL Power Architect page.
     */
    public static final String SQLP_ARCHITECT_URL = "http://www.sqlpower.ca/page/architect"; //$NON-NLS-1$
	
	/**
     * The URL for the SQL Power main page.
     */
    public static final String SQLP_URL = "http://www.sqlpower.ca/"; //$NON-NLS-1$
	
    /**
     * The URL for the SQL Power forum where users can get help and ask questions.
     */
    public static final String FORUM_URL = "http://www.sqlpower.ca/page/enter_forum"; //$NON-NLS-1$
    
    
    
    
    /**
     * The URL for the Wabit FAQ page
     */
    public static final String WABIT_FAQ_URL = "http://www.sqlpower.ca/page/wabit-faq"; //$NON-NLS-1$
    
    /**
     * The URL for the DQGURU FAQ page
     */
    public static final String DQGURU_FAQ_URL = "http://www.sqlpower.ca/page/dqguru-faq"; //$NON-NLS-1$
    
    /**
     * The URL for the Architect FAQ page
     */
    public static final String ARCHITECT_FAQ_URL = "http://www.sqlpower.ca/page/architect-faq"; //$NON-NLS-1$
    
    
    
    
    /**
     * The URL for the Wabit getting started page
     */
    public static final String WABIT_GS_URL = "http://www.sqlpower.ca/page/wabit-start"; //$NON-NLS-1$
    
    /**
     * The URL for the Architect getting started page
     */
    public static final String ARCHITECT_GS_URL = "http://www.sqlpower.ca/page/architect-start"; //$NON-NLS-1$
    
    /**
     * The URL for the Architect getting started page
     */
    public static final String DQGURU_GS_URL = "http://www.sqlpower.ca/page/dqguru-start"; //$NON-NLS-1$
    
    
    
   
    
    
    /**
     * The URL for the Wabit demo page
     */
    public static final String WABIT_DEMO_URL = "http://www.sqlpower.ca/page/wabit-demos"; //$NON-NLS-1$
    
    /**
     * The URL for the DQGuru demo page
     */
    public static final String DQGURU_DEMO_URL = "http://www.sqlpower.ca/page/dqguru-demos"; //$NON-NLS-1$
    
    /**
     * The URL for the Architect demo page
     */
    public static final String ARCHITECT_DEMO_URL = "http://www.sqlpower.ca/page/architect-demos"; //$NON-NLS-1$
    
    
    
    
    
    
    
    /**
     * The URL for the Wabit user guide page
     */
    public static final String WABIT_UG_URL = "http://www.sqlpower.ca/page/wabit-userguide"; //$NON-NLS-1$
    
    /**
     * The URL for the Architect user guide page
     */
    public static final String ARCHITECT_UG_URL = "http://www.sqlpower.ca/page/architect-userguide"; //$NON-NLS-1$
    
    /**
     * The URL for the DQGuru user guide page
     */
    public static final String DQGURU_UG_URL = "http://www.sqlpower.ca/page/dqguru-userguide"; //$NON-NLS-1$
    
    
    
    
    
    
    
    
    
    /**
     * The URL for the Wabit enterprise upgrade
     */
    public static final String WABIT_UPGRADE_URL = "http://www.sqlpower.ca/page/wabit-ep"; //$NON-NLS-1$
    
    /**
     * The URL for the Wabit enterprise upgrade
     */
    public static final String ARCHITECT_UPGRADE_URL = "http://www.sqlpower.ca/page/architect-e"; //$NON-NLS-1$
    
    /**
     * The URL for the Wabit enterprise upgrade
     */
    public static final String DQGURU_UPGRADE_URL = "http://www.sqlpower.ca/page/dqguru-e"; //$NON-NLS-1$
    
    
    
    
    
    
    
    /**
     * The URL for the Wabit premium support
     */
    public static final String WABIT_PS_URL = "http://www.sqlpower.ca/page/wabit_support"; //$NON-NLS-1$
    
    /**
     * The URL for the Architect premium support
     */
    public static final String ARCHITECT_PS_URL = "http://www.sqlpower.ca/page/architect_support"; //$NON-NLS-1$
    
    /**
     * The URL for the DQGuru premium support
     */
    public static final String DQGURU_PS_URL = "http://www.sqlpower.ca/page/dqguru_support"; //$NON-NLS-1$
    
    
    
    
	
	public static final Action forumAction = new AbstractAction(Messages.getString("SPSUtils.webSupportActionName"), //$NON-NLS-1$
            // Alas this is now static so the size can't be gotten from sprefs...
            SPSUtils.createIcon("world","New Project")) { //$NON-NLS-1$ //$NON-NLS-2$
        public void actionPerformed(ActionEvent evt) {
            try {
                BrowserUtil.launch(FORUM_URL);
            } catch (IOException e) {
                SPSUtils.showExceptionDialogNoReport(getFrameFromActionEvent(evt),
                        Messages.getString("SPSUtils.couldNotLaunchBrowser"), e); //$NON-NLS-1$
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
                    "The window argument has to be either a JFrame or JDialog." + //$NON-NLS-1$
                    "  You provided a " + (w == null ? null : w.getClass().getName())); //$NON-NLS-1$
        }

    	InputMap inputMap = c.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    	ActionMap actionMap = c.getActionMap();

    	inputMap.put(KeyStroke.getKeyStroke("ESCAPE"), "cancel"); //$NON-NLS-1$ //$NON-NLS-2$
    	actionMap.put("cancel", new AbstractAction() { //$NON-NLS-1$
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
        String realPath = "/icons/"+name+".png"; //$NON-NLS-1$ //$NON-NLS-2$
		logger.debug("Loading resource "+realPath); //$NON-NLS-1$
		java.net.URL imgURL = SPSUtils.class.getResource(realPath);
        if (imgURL == null) {
            realPath = realPath.replace(".png", ".gif"); //$NON-NLS-1$ //$NON-NLS-2$
            imgURL = SPSUtils.class.getResource(realPath);
        }
		if (imgURL != null) {
			return new ImageIcon(imgURL, description);
		} else {
			logger.debug("Couldn't find file: " + realPath); //$NON-NLS-1$
			return null;
		}
	}
    
	public static final FileFilter ARCHITECT_FILE_FILTER =
		new FileExtensionFilter(Messages.getString("SPSUtils.architectFileType"), new String[] {"arc", "architect"}); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

	public static final FileFilter TEXT_FILE_FILTER =
		new FileExtensionFilter(Messages.getString("SPSUtils.textFileType"), new String[] {"txt"}); //$NON-NLS-1$ //$NON-NLS-2$

	public static final FileFilter SQL_FILE_FILTER =
		new FileExtensionFilter(Messages.getString("SPSUtils.sqlFileType"), new String[] {"sql","ddl"}); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

	public static final FileFilter INI_FILE_FILTER =
		new FileExtensionFilter(Messages.getString("SPSUtils.iniFileType"), new String[] {"ini"}); //$NON-NLS-1$ //$NON-NLS-2$

	public static final FileFilter EXE_FILE_FILTER =
		new FileExtensionFilter(Messages.getString("SPSUtils.exeFileType"), new String[] {"exe"}); //$NON-NLS-1$ //$NON-NLS-2$

	public static final FileFilter JAR_ZIP_FILE_FILTER =
		new FileExtensionFilter(Messages.getString("SPSUtils.jarFileType"), new String[] {"jar", "zip"}); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

	public static final FileFilter LOG_FILE_FILTER =
		new FileExtensionFilter(Messages.getString("SPSUtils.logFileType"), new String[] {"log"}); //$NON-NLS-1$ //$NON-NLS-2$

    public static final FileFilter XSLT_FILE_FILTER =
        new FileExtensionFilter(Messages.getString("SPSUtils.xsltFileType"), new String[] {"xsl", "xslt"}); //$NON-NLS-1$ //$NON-NLS-2$

    public static final FileFilter VELOCITY_FILE_FILTER =
        new FileExtensionFilter(Messages.getString("SPSUtils.velocityFileType"), new String[] {"vm"}); //$NON-NLS-1$ //$NON-NLS-2$

    public static final FileFilter XML_FILE_FILTER =
        new FileExtensionFilter(Messages.getString("SPSUtils.xmlFileType"), new String[] {"xml"}); //$NON-NLS-1$ //$NON-NLS-2$

    public static final FileFilter PDF_FILE_FILTER =
        new FileExtensionFilter(Messages.getString("SPSUtils.pdfFileType"), new String[] {"pdf"}); //$NON-NLS-1$ //$NON-NLS-2$

    public static final FileFilter CSV_FILE_FILTER =
        new FileExtensionFilter(Messages.getString("SPSUtils.csvFileType"), new String[] {"csv"}); //$NON-NLS-1$ //$NON-NLS-2$

    public static final FileFilter HTML_FILE_FILTER =
        new FileExtensionFilter(Messages.getString("SPSUtils.htmlFileType"), new String[] {"html"}); //$NON-NLS-1$ //$NON-NLS-2$

    public static final FileFilter BATCH_FILE_FILTER =
        new FileExtensionFilter(Messages.getString("SPSUtils.batchFileType"), new String[] {"bat"}); //$NON-NLS-1$ //$NON-NLS-2$
    
    public static final FileFilter WABIT_FILE_FILTER = 
    	new FileExtensionFilter(Messages.getString("SPSUtils.wabitFileType"), new String[] {"wabit"}); //$NON-NLS-1$ //$NON-NLS-2$
    
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
			s.append(":"); //$NON-NLS-1$
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
			String ext = ""; //$NON-NLS-1$
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
    private static ImageIcon masterIcon;

    /**
     * Sets the default icon on the exception dialogs. This is
     * mostly used for when the exception dialogs are handled by
     * SPSUtils instead of ASUtils or MMSUtils. The session context
     * should set this at creation.
     */
	public static void setMasterIcon(ImageIcon masterIcon) {
		SPSUtils.masterIcon = masterIcon;
	}
    
    /**
     * Displays a dialog box with the given message and submessage and exception,
     * allowing the user to examine the stack trace, but do NOT generate
     * a report back to SQLPower web site.
     * 
     * @param parent The parent component that will own the dialog
     * @param message A user visible string that should explain the problem
     * @param subMessage A second string to give finer-grained detail to the user
     * @param throwable The exception that caused the problem. Should not be null, but this
     * method will handle that case gracefully since this is the last line of defense.
     * 
     * @return The JDialog to be displayed with the exception.
     */
    private static JDialog displayExceptionDialog(
            final Component parent,
            final String message,
            final String subMessage,
            Throwable givenThrowable) {
        
        final Throwable throwable;
        if (givenThrowable == null) {
            // this is a strange case, but we should handle everything here without blowing up
            throwable = new Error("The throwable passed in to the exception handler was null. This is just a placeholder.");
        } else {
            throwable = givenThrowable;
        }
        
        throwable.printStackTrace();
        
        JDialog dialog;
        Window owner = parent == null? null: getWindowInHierarchy(parent);
        if (owner instanceof JFrame) {
            JFrame frame = (JFrame) owner;
            dialog = new JDialog(frame, Messages.getString("SPSUtils.errorDialogTitle")); //$NON-NLS-1$
        } else if (owner instanceof Dialog) {
            dialog = new JDialog((Dialog)owner, Messages.getString("SPSUtils.errorDialogTitle")); //$NON-NLS-1$
        } else {
            logger.error(
                    String.format("dialog parent component %s is neither JFrame nor JDialog", owner)); //$NON-NLS-1$
            
            // last desperate attempt to set the icon for the dialog
            JFrame frame = new JFrame();
            if (masterIcon != null) {
            	frame.setIconImage(masterIcon.getImage());
            }
            dialog = new JDialog(frame, Messages.getString("SPSUtils.errorDialogTitle")); //$NON-NLS-1$
            
        }
        logger.debug("displayExceptionDialog: showing exception dialog for:", throwable); //$NON-NLS-1$

        ((JComponent)dialog.getContentPane()).setBorder(
                BorderFactory.createEmptyBorder(10, 10, 5, 5));

        String exceptionString = SQLPowerUtils.exceptionStackToString(throwable);

        JPanel top = new JPanel(new GridLayout(0, 1, 5, 5));

        StringBuilder labelText = new StringBuilder();
        labelText.append("<html><font color='red' size='+1'>"); //$NON-NLS-1$
        labelText.append(message == null ? "Unexpected error" : nlToBR(message)); //$NON-NLS-1$
        labelText.append("</font>"); //$NON-NLS-1$
        if (subMessage != null) {
            labelText.append("<p>"); //$NON-NLS-1$
            labelText.append(subMessage);
        }
        JLabel messageLabel = new JLabel(labelText.toString());
        top.add(messageLabel);

        JLabel errClassLabel =
            new JLabel("<html><b>Exception type</b>: " + nlToBR(throwable.getClass().getName())); //$NON-NLS-1$
        top.add(errClassLabel);
        String excDetailMessage = throwable.getMessage();
        excDetailMessage = trimToClosestNL(excDetailMessage, 100, 25);
        if (excDetailMessage != null) {
            top.add(new JLabel("<html><b>Detail string</b>: " + nlToBR(excDetailMessage))); //$NON-NLS-1$
            if (throwable.getCause() != null) {
                Throwable root;
                for (root = throwable.getCause(); root.getCause() != null; root = root.getCause()) {
                	if (root.getMessage() != null) {
                		String rootCauseMessage = root.getMessage();
                        rootCauseMessage = trimToClosestNL(rootCauseMessage, 100, 25);
                		top.add(new JLabel("<html><b>Root Cause</b>: " + nlToBR(rootCauseMessage))); //$NON-NLS-1$
                	}
                }
            }

        }

        final JButton detailsButton = new JButton(Messages.getString("SPSUtils.showExceptionDetailsButton")); //$NON-NLS-1$
        final JPanel detailsButtonPanel = new JPanel();
        detailsButtonPanel.add(detailsButton);

        final JButton forumButton = new JButton(forumAction);
        detailsButtonPanel.add(forumButton);
        top.add(detailsButtonPanel);

        dialog.add(top, BorderLayout.NORTH);
        final JScrollPane detailScroller =
            new JScrollPane(new JTextArea(exceptionString));

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
                    detailsButton.setText(Messages.getString("SPSUtils.hideExceptionDetailsButton")); //$NON-NLS-1$
                } else /* hide details */ {
                    finalDialogReference.remove(messageComponent);
                    finalDialogReference.add(fakeMessageComponent, BorderLayout.CENTER);
                    detailsButton.setText(Messages.getString("SPSUtils.showExceptionDetailsButton")); //$NON-NLS-1$
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
        JButton okButton = new JButton(Messages.getString("SPSUtils.okButton")); //$NON-NLS-1$
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
        	int lastNL = msg.indexOf("\n", msgLimit - offset); //$NON-NLS-1$
        	int nextNL = msg.indexOf("\n", msgLimit); //$NON-NLS-1$
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
        	msg = msg.substring(0, endIndex) + " ..."; //$NON-NLS-1$
        }
    	return msg;
    }
    
    /**
     * Simple convenience routine to replace all \n's with <br>
     * @param s
     * @return
     */
    static String nlToBR(String s) {
        // Do NOT xml-ify the BR tag until Swing's HTML supports this.
    	logger.debug("String s is " + s); //$NON-NLS-1$
        return s.replaceAll("\n", "<br>"); //$NON-NLS-1$ //$NON-NLS-2$
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
                    file = new File(fileName + "." //$NON-NLS-1$
                            + filter.getFilterExtension(new Integer(0)));
                }
                if (file.exists()) {
                    int choice = JOptionPane.showOptionDialog(
                                        owner,
                                        Messages.getString("SPSUtils.fileOverwriteConfirmation"), //$NON-NLS-1$
                                        Messages.getString("SPSUtils.confirmOverwriteButton"), //$NON-NLS-1$
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
            SPSUtils.showExceptionDialogNoReport(Messages.getString("SPSUtils.couldNotSaveFileError"), e); //$NON-NLS-1$
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
        logger.debug("The base point is (" + xBase + ", " + yBase + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

        double mInv = -1 / m;
        double nInv = yBase - mInv * xBase;
       
        a = mInv * mInv + 1;
        b = -2 * xBase - 2 * yBase * mInv + 2 * mInv * nInv;
        c = xBase * xBase + yBase * yBase - 2 * yBase * nInv + nInv * nInv - (width / 2) * (width /2);
       
        int xPoint = (int)((- b + Math.sqrt(b * b - 4 * a * c)) / (2 * a));
        int yPoint = (int)(mInv * xPoint + nInv);
        logger.debug(" x is " + xPoint + " y is " + yPoint); //$NON-NLS-1$ //$NON-NLS-2$
        polygon.addPoint(xPoint, yPoint);
       
        xPoint = (int)((- b - Math.sqrt(b * b - 4 * a * c)) / (2 * a));
        yPoint = (int)(mInv * xPoint + nInv);
        polygon.addPoint(xPoint, yPoint);

        return polygon;
    }

    /**
     * Updates a potentially-long JMenu with the nth-last items replaced by
     * sub-menus. This is done iteratively, so if the menu has lots of items or
     * the given height reference is very small, you will end up with an
     * arbitrarily-long chain of submenus.
     * <p>
     * If the menu fits the given window, it will not be modified by this
     * method.
     * 
     * @param heightReference
     *            The size of this window is used to compute where to break up
     *            the menu. Eventually, this method will also install a listener
     *            on this window that will reflow the menu when the window
     *            changes size, but this is not implemented at present.
     * @param input
     *            The JMenu.
     */
    public static void breakLongMenu(final Window heightReference, final JMenu input) {

        if ( input.getItemCount() <= 0 )
            return;

        final int windowHeight = heightReference.getSize().height;
        final int totalRows = input.getItemCount();
        final int preferredHeight = input.getItem(0).getPreferredSize().height;
        final int FUDGE = 3; // XXX find a better way to compute this...

        int rowsPerSubMenu = (windowHeight/ preferredHeight) - FUDGE;
        if ( rowsPerSubMenu < 3 )
            rowsPerSubMenu = 3;
        if (totalRows <= rowsPerSubMenu) {
            return;
        }

        JMenu parentMenu = input;
        JMenu subMenu = new JMenu(Messages.getString("SPSUtils.moreSubmenu")); //$NON-NLS-1$
        parentMenu.add(subMenu);

        while (input.getItemCount() > rowsPerSubMenu + 1) {
            final JMenuItem item = input.getItem(rowsPerSubMenu);
            subMenu.add(item);  // Note that this removes it from the original menu!

            if (subMenu.getItemCount() >= rowsPerSubMenu &&
                input.getItemCount() > rowsPerSubMenu + 1 ) {
                parentMenu = subMenu;
                subMenu = new JMenu(Messages.getString("SPSUtils.moreSubmenu")); //$NON-NLS-1$
                parentMenu.add(subMenu);
            }
        }


        /** TODO: Resizing the main window does not change the height of the menu.
         * This is left as an exercise for the reader:
         * frame.addComponentListener(new ComponentAdapter() {
         * @Override
         * public void componentResized(ComponentEvent e) {
         * JMenu oldMenu = fileMenu;
         * // Loop over oldMenu, if JMenu, replace with its elements, recursively...!
         * ASUtils.breakLongMenu(fileMenu);
         * }
         * });
         */
    }

    /**
     * Adjusts the given stroke so its line thickness and dash setup do not
     * appear to change regardless of the scale factor in effect. For example, a
     * stroke with a line thickness of 1 will take up 1 pixel when drawn at
     * viewScale.
     * 
     * @param original
     *            The original BasicStroke object
     * @param viewScale
     *            The current scale amount for the graphics being drawn in
     * @return A new BasicStroke object adjusted to appear the same when drawn
     *         subject to viewScale as the original looks when drawn subject to the
     *         identity transform.
     */
    public static BasicStroke getAdjustedStroke(BasicStroke original, double viewScale) {
        float[] adjustedDashArray = original.getDashArray();
        if (adjustedDashArray != null) {
            for (int i = 0; i < adjustedDashArray.length; i++) {
                adjustedDashArray[i] /= viewScale;
            }
        }
        return new BasicStroke(
                (float) (original.getLineWidth() / viewScale),
                original.getEndCap(),
                original.getLineJoin(),
                Math.max(1f, (float) (original.getMiterLimit() / viewScale)),
                adjustedDashArray,
                original.getDashPhase());
    }

	/**
	 * Returns a JPanel containing the given JTree and a JLabel positioned at
	 * the bottom of the panel containing the SQL Power Logo that when clicked
	 * on, will open up the SQL Power website in a web browser.
	 * 
	 * @param tree
	 *            The JTree that will be added to the panel.
	 * @return A JPanel containing the given JTree and the SQL Power Logo
	 *         branding
	 */
    public static JPanel getBrandedTreePanel(final JTree tree) {
    	class ScrollableDelegatePanelClassThingThatsNotAnonymous extends JPanel implements Scrollable {
    		Scrollable scrollable = tree;

    		@Override
    		public Dimension getPreferredSize() {
    			Dimension realPrefSize = super.getPreferredSize();
				JViewport viewport = (JViewport) SwingUtilities.getAncestorOfClass(JViewport.class, this);
				if (viewport != null) {
					realPrefSize.width = Math.max(viewport.getWidth(), realPrefSize.width);
					realPrefSize.height = Math.max(viewport.getHeight(), realPrefSize.height);
				}
				return realPrefSize;
    		}
    		
			public Dimension getPreferredScrollableViewportSize() {
				return scrollable.getPreferredScrollableViewportSize();
			}

			public int getScrollableBlockIncrement(Rectangle visibleRect,
					int orientation, int direction) {
				return scrollable.getScrollableBlockIncrement(visibleRect,
						orientation, direction);
			}

			public boolean getScrollableTracksViewportHeight() {
				return scrollable.getScrollableTracksViewportHeight();
			}

			public boolean getScrollableTracksViewportWidth() {
				return scrollable.getScrollableTracksViewportWidth();
			}

			public int getScrollableUnitIncrement(Rectangle visibleRect,
					int orientation, int direction) {
				return scrollable.getScrollableUnitIncrement(visibleRect,
						orientation, direction);
			}
    		
    	};
    	JPanel panel = new ScrollableDelegatePanelClassThingThatsNotAnonymous();
    	DefaultFormBuilder treeBuilder = new DefaultFormBuilder(new FormLayout("fill:pref:grow", "fill:pref:grow, pref"), panel);
    	treeBuilder.add(tree);
    	treeBuilder.nextLine();
    	JLabel sqlpLabel = getSQLPowerLogoLabel();
		treeBuilder.add(sqlpLabel);
		return panel;
    }

	/**
	 * Gets a JLabel containing the SQL Power Logo that opens up a web browser
	 * to the SQL Power website when clicked. By default it has a
	 * {@link Color#WHITE} background, and opaque set to true.
	 */
	public static JLabel getSQLPowerLogoLabel() {
		JLabel sqlpLabel = new JLabel(new ImageIcon(SPSUtils.class.getClassLoader().getResource("ca/sqlpower/swingui/SQLP-90x80.png")));
    	sqlpLabel.setBackground(Color.WHITE);
    	sqlpLabel.setOpaque(true);
    	sqlpLabel.setHorizontalAlignment(SwingConstants.LEFT);
    	sqlpLabel.addMouseListener(new MouseAdapter() {
    		@Override
    		public void mouseReleased(MouseEvent e) {
    			try {
    				BrowserUtil.launch(SPSUtils.SQLP_URL);
    			} catch (IOException e1) {
    				throw new RuntimeException("Unexpected error in launch", e1); //$NON-NLS-1$
    			}
    		}
		});
		return sqlpLabel;
	}

	/**
	 * Modifies the given JSpinner so that when the textfield gains focus, the
	 * entire text will be selected.
	 * <p>
	 * This is a workaround for an existing bug in Swing in which calling
	 * selectAll() on the TextField of a JSpinner does not select all the text.
	 * <p>
	 * The existing bug report is available here: <a
	 * href="http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4699955"
	 * >http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4699955</a>
	 * 
	 * @param spinner
	 *            The JSpinner instance that you want to make select all text in
	 *            its text field when it gains focus.
	 */
    public static void makeJSpinnerSelectAllTextOnFocus(JSpinner spinner) {
        JSpinner.DefaultEditor editor = (JSpinner.DefaultEditor)spinner.getEditor();
        editor.getTextField().addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Focus Gained: " + e);
                }
                if (e.getSource() instanceof JTextComponent) {
                    final JTextComponent textComponent = ((JTextComponent)e.getSource());
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            textComponent.selectAll();
                        }
                    });
                }
            }
            
            public void focusLost(FocusEvent e) {
                if(logger.isDebugEnabled()) {
                    logger.debug("Focus Lost:" + e);
                }
            }
        });
    }

    /**
     * Runs the given Runnable on the Swing event dispatch thread. If the
     * calling thread <i>is</i> the event dispatch thread, doRun is run
     * immediately (before this method returns). Otherwise, doRun is appended to
     * the end of the Swing event queue.
     * <p>
     * This method should be particularly handy in event handler code that has
     * to deal with the possibility that an event is being received on a thread
     * other than the Swing event dispatch thread.
     * 
     * @param doRun
     *            The doRun run run (the doRun run).
     */
    public static void runOnSwingThread(Runnable doRun) {
        if (SwingUtilities.isEventDispatchThread()) {
            doRun.run();
        } else {
            SwingUtilities.invokeLater(doRun);
        }
    }

	/**
	 * Creates a {@link Popup} containing a given {@link JComponent} embedded
	 * inside a {@link JScrollPane}. Also creates a {@link PopupListenerHandler}
	 * that handles events dealing with the {@link Popup}. When display, it will
	 * popup the window at the given location, and it will add scrollbars in the
	 * right circumstances as well.
	 * 
	 * @param owningFrame
	 *            The {@link Component} which the popup is being popped up on.
	 * @param componentToEmbed
	 *            The {@link JComponent} to display.
	 * @param windowLocation
	 *            The location of where the popup should be displayed.
	 * @return The {@link PopupListenerHandler} that handles the created
	 *         {@link Popup} window.
	 */
    public static PopupListenerHandler popupComponent(
    		final Component owningFrame, 
    		final JComponent componentToEmbed, 
    		final Point windowLocation) {
        JScrollPane treeScroll = new JScrollPane(componentToEmbed);
        treeScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        treeScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        Point frameLocation = new Point(0, 0);
        SwingUtilities.convertPointToScreen(frameLocation, owningFrame);
        
        int popupScreenSpaceY = (windowLocation.y - frameLocation.y);
        int maxHeight = (int)(owningFrame.getSize().getHeight() - popupScreenSpaceY);
        
        int width = (int) Math.min(treeScroll.getPreferredSize().getWidth(), owningFrame.getSize().getWidth());
        int height = (int) Math.min(treeScroll.getPreferredSize().getHeight(), maxHeight);
        treeScroll.setPreferredSize(new java.awt.Dimension(width, height));
        
        double popupWidth = treeScroll.getPreferredSize().getWidth();
        int popupScreenSpaceX = (int) (owningFrame.getSize().getWidth() - (windowLocation.x - frameLocation.x));
        int x;
        if (popupWidth > popupScreenSpaceX) {
            x = (int) (windowLocation.x - (popupWidth - popupScreenSpaceX));
        } else {
            x = windowLocation.x;
        }
        
        treeScroll.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED, Color.GRAY, Color.GRAY));
        
        JComponent glassPane;
        if (owningFrame instanceof JFrame) {
        	JFrame frame = (JFrame) owningFrame;
        	if (frame.getGlassPane() == null) {
        		glassPane = new JPanel();
        		frame.setGlassPane(glassPane);
        	} else {
        		glassPane = (JComponent) frame.getGlassPane();
        	}
        	glassPane.setVisible(true);
        	glassPane.setOpaque(false);
        } else {
        	glassPane = (JComponent) owningFrame;
        }
        
        PopupFactory pFactory = new PopupFactory();
        final Popup popup = pFactory.getPopup(glassPane, treeScroll, x, windowLocation.y);
        
        return new PopupListenerHandler(popup, glassPane, owningFrame);
    }
    
    public static Throwable getRootCause(Throwable t) {
		Throwable rootCause = t;
		while (rootCause.getCause() != null
				&& rootCause != rootCause.getCause()) {
			rootCause = rootCause.getCause();
		}
		return rootCause;
	}
    
    private static String convertToHex(byte[] data) {
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < data.length; i++) {
            int halfbyte = (data[i] >>> 4) & 0x0F;
            int two_halfs = 0;
            do {
                if ((0 <= halfbyte) && (halfbyte <= 9)) {
                    buf.append((char) ('0' + halfbyte));
                } else {
                    buf.append((char) ('a' + (halfbyte - 10)));
                }
                halfbyte = data[i] & 0x0F;
            } while (two_halfs++ < 1);
        }
        return buf.toString();
    }

    public static String encodeSha1(String text) {
    	
    	/*
    	 * Thanks to the olap4j project for this code.
    	 * www.olap4j.org
    	 */
    	
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            try {
                md = MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException e1) {
                throw new RuntimeException(e1);
            }
        }

        byte[] sha1hash = new byte[40];

        md.update(text.getBytes(), 0, text.length());

        sha1hash = md.digest();

        return convertToHex(sha1hash);
    }

	/**
	 * Checks for a newer version of the product.
	 * 
	 * @param owner
	 *            A parent for any dialog we create.
	 * @param version
	 *            The current version of the product.
	 * @param latestVersionUrl
	 *            A URL to a file location that we can retrieve the latest
	 *            version of this product from. The URL must return some kind of
	 *            XML in the body of the response. The XML must contain the
	 *            following properties.
	 *            <ul>
	 *            <li>currentVersion - The latest version of the product that
	 *            has been released</li>
	 *            <li>downloadUrl - The location of where to download the latest
	 *            version</li>
	 *            <li>releaseNotes - The changes made in the last release</li>
	 *            </ul>
	 * @param silent
	 *            If false the user will be notified if there are no updates
	 *            available. If true the user will be notified if no updates are
	 *            available. If there is an update available the user will be
	 *            notified regardless of this flag.
	 * @param stopAutoChecking
	 *            If the user checks the 'stop automatically checking for
	 *            updates' box and this runnable is not null the runnable will
	 *            be called to set the necessary parameters to stop the auto
	 *            check feature when the dialog closes. This value can be null
	 *            if there is no way to stop the auto check.
	 */
	public static void checkForUpdate(JFrame owner, String productName, Version version, 
			String latestVersionUrl, boolean silent, boolean setTimeout, final Runnable stopAutoChecking) {
		try {
			GetMethod request = new GetMethod(latestVersionUrl);
			if (setTimeout) {
				BasicHttpParams params = new BasicHttpParams();
				params.setIntParameter("http.socket.timeout", new Integer(1000));
			}
			HttpClient connection = new HttpClient();
			connection.executeMethod(request);

			final Properties results = new Properties();
			results.loadFromXML(
					new ByteArrayInputStream(
							request.getResponseBody()));
			
			Version currentVersion = new Version(
					results.getProperty("currentVersion"));
			
			if (currentVersion.compareTo(version) > 0) {
				
				final JDialog dialog = new JDialog(owner, "New " + productName + " version available!");
				dialog.setAlwaysOnTop(true);
				dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
				
				JPanel panel = new JPanel(new MigLayout("fill", "[grow]", "[shrink][grow][shrink][shrink]"));
				dialog.setContentPane(panel);
				
				JLabel title = new JLabel("A new version of " + productName + " is available for download.");
				title.setFont(title.getFont().deriveFont(16f));
				panel.add(
						title,
						"wrap, gapbottom 10px, center");
				
				JLabel notes = new JLabel(results.getProperty("releaseNotes"));
				notes.setBackground(Color.WHITE);
				notes.setOpaque(true);
				Border gap = BorderFactory.createEmptyBorder(4, 4, 4, 4);
			    Border blackline = BorderFactory.createLineBorder(Color.black);
			    Border compound = BorderFactory.createCompoundBorder(blackline, gap);
				notes.setBorder(compound);
				panel.add(notes, "wrap, center, grow");
				
				final JCheckBox autoCheckCheckBox = new JCheckBox("Stop automatic updates");
				if (stopAutoChecking != null) {
					panel.add(autoCheckCheckBox, "wrap, left");
				}
				
				Box buttons = Box.createHorizontalBox();
				JButton downloadButton = new JButton(new AbstractAction("Download Now") {
					public void actionPerformed(ActionEvent event) {
						try {
							BrowserUtil.launch(results.getProperty("downloadUrl"));
						} catch (IOException e) {
							throw new RuntimeException("Error attempting to launch web browser", e);
						} finally {
							dialog.dispose();
							if (stopAutoChecking != null && autoCheckCheckBox.isSelected()) {
								stopAutoChecking.run();
							}
						}
					}
				});
				JButton cancelButton = new JButton(new AbstractAction("No thanks") {
					public void actionPerformed(ActionEvent e) {
						dialog.dispose();
						if (stopAutoChecking != null && autoCheckCheckBox.isSelected()) {
							stopAutoChecking.run();
						}
					}
				});
				buttons.add(downloadButton);
				buttons.add(cancelButton);
				panel.add(buttons, "center");
				
				dialog.pack();
				dialog.setLocationRelativeTo(owner);
				dialog.setVisible(true);
			} else if (!silent) {
				JOptionPane.showMessageDialog(
						owner, 
						"No updates available.", 
						"Update checker", 
						JOptionPane.INFORMATION_MESSAGE);
			}
		} catch (Exception ex) {
			logger.warn("Failed to check for update.", ex);
			if (!silent) {
				JOptionPane.showMessageDialog(
						owner, 
						"Failed to check for updates. Are you connected to the internet?", 
						"Update checker", 
						JOptionPane.INFORMATION_MESSAGE);
			}
		}
	}
}
