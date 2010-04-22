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
package ca.sqlpower.swingui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.concurrent.Callable;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.apache.log4j.Logger;

import ca.sqlpower.swingui.ChangeListeningDataEntryPanel.ErrorTextListener;
import ca.sqlpower.validation.Validated;
import ca.sqlpower.validation.swingui.ValidatableDataEntryPanel;
import ca.sqlpower.validation.swingui.ValidationHandler;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.factories.ButtonBarFactory;

public class DataEntryPanelBuilder {
	static Logger logger = Logger.getLogger(DataEntryPanelBuilder.class);
	public static final String OK_BUTTON_LABEL = Messages.getString("DataEntryPanelBuilder.okButton"); //$NON-NLS-1$
	public static final String CANCEL_BUTTON_LABEL = Messages.getString("DataEntryPanelBuilder.cancelButton"); //$NON-NLS-1$
    
    /**
     * Works like
     * {@link #createDataEntryPanelDialog(DataEntryPanel, Window, String, String, Callable, Callable, boolean)}
     * with a <code>oneShot</code> argument of <code>false</code>.
     *  
     * @param dataEntry
     *            The DataEntryPanel implementation
     * @param dialogParent
     *            A Window object to be the dialog's parent
     * @param dialogTitle
     *            The dialog title.
     * @param actionButtonTitle
     *            The label text for the OK button
     * @return The new JDialog, which has the panel in it along with OK and Cancel buttons
     * @param okCall<Boolean> Call to be invoked when the OK/action button is
     *  pressed; does NOT need to dismiss the dialog we will do this if the call returns false
     * @param cancelCall<Boolean> Call to be invoked when the cancel button is
     *  pressed; We will dismiss the dialog if the call returns true
     * @return
     */
    public static JDialog createDataEntryPanelDialog(
            final DataEntryPanel dataEntry,
            final Component dialogParent,
            final String dialogTitle,
            final String actionButtonTitle,
            final Callable<Boolean> okCall,
            final Callable<Boolean> cancelCall) {
        return createDataEntryPanelDialog(
                dataEntry, dialogParent, dialogTitle,
                actionButtonTitle, okCall, cancelCall, false);
    }
    
    /**
     * Builds a JDialog around an object that implements DataEntryPanel, to
     * provide consistent behaviours such as Cancel button, <ESC> to close, and
     * so on.
     * 
     * @param dataEntry
     *            The DataEntryPanel implementation
     * @param dialogParent
     *            A Window object to be the dialog's parent
     * @param dialogTitle
     *            The dialog title.
     * @param actionButtonTitle
     *            The label text for the OK button
     * @param okCall
     *            Call to be invoked when the OK/action button is pressed; does
     *            NOT need to dismiss the dialog we will do this if the call
     *            returns false
     * @param cancelCall
     *            Call to be invoked when the cancel button is pressed; We will
     *            dismiss the dialog if the call returns true. You can specify
     *            null for this parameter if no custom cancel behaviour is required.
     * @param oneShot
     *            When true, the OK and Cancel buttons will be permanently
     *            deactivated once one of them has been pressed. If false, the
     *            buttons will reactivate once the <code>okCall</code> or
     *            <code>cancelCall</code> has returned.
     * @return The new JDialog, which has the panel in it along with OK and
     *         Cancel buttons
     */
    public static JDialog createDataEntryPanelDialog(
            final DataEntryPanel dataEntry,
            final Component dialogParent,
            final String dialogTitle,
            final String actionButtonTitle,
            final Callable<Boolean> okCall,
            final Callable<Boolean> cancelCall,
            final boolean oneShot) {

		final JDialog d = createDialog(dialogParent, dialogTitle);
		JComponent panel = dataEntry.getPanel();
		
		final Action okAction;
		if (dataEntry instanceof MonitorableDataEntryPanel) {
			MonitorableDataEntryPanel mdep = (MonitorableDataEntryPanel)dataEntry;
			mdep.setDialog(d);
			okAction = new AbstractAction() {
				public void actionPerformed(ActionEvent e) {
					try {
						this.setEnabled(false);
						okCall.call();
					} catch (Exception ex) {
						throw new RuntimeException(ex);
					} finally {
                        if (!oneShot) {
                            this.setEnabled(true);
                        }
                    }
				}
			};
		} else {
			okAction = new AbstractAction() {
				public void actionPerformed(ActionEvent e) {
					try {
					    this.setEnabled(false);
						boolean close = okCall.call().booleanValue();
						if (close) {
							d.dispose();
						}
					} catch (Exception ex) {
						throw new RuntimeException(ex);
					} finally {
                        if (!oneShot) {
                            this.setEnabled(true);
                        }
                    }
				}
			};
		}
		
		if (dataEntry instanceof ValidatableDataEntryPanel) {
			ValidatableDataEntryPanel vdep = ((ValidatableDataEntryPanel) dataEntry);
			ValidationHandler handler = vdep.getValidationHandler();
			handler.setValidatedAction(okAction);
		}
		
		final JButton okButton = new JDefaultButton(okAction);
		okButton.setText(actionButtonTitle);
		
		Action closeAction = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				try {
					boolean close = cancelCall.call().booleanValue();
					if (close) {
						d.dispose();
					}
				} catch (Exception ex) {
					throw new RuntimeException(ex);
				}
			}
		};
		
		//attempts to call the cancel action when the user clicks
		//the windows "x" button.
		d.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		d.addWindowListener(new WindowListener() {

			public void windowActivated(WindowEvent e) {}

			public void windowClosed(WindowEvent e) {}

			public void windowClosing(WindowEvent e) {
				try {
					boolean close = cancelCall.call().booleanValue();
					if (close) {
						d.dispose();
					}
				} catch (Exception ex) {
					throw new RuntimeException(ex);
				}
			}

			public void windowDeactivated(WindowEvent e) {}

			public void windowDeiconified(WindowEvent e) {}

			public void windowIconified(WindowEvent e) {}

			public void windowOpened(WindowEvent e) {}
		});
		
		//checks if it is a panel that needs to be validated before save.
		if (dataEntry instanceof Validated) {
			//links the saveAction to the handler
			ValidationHandler handler = ((Validated)dataEntry).getHandler();
			handler.setValidatedAction(okAction);
		}
		
        if (dataEntry instanceof Resizable) {
        	//resizes to the correct dimensions
        	ComponentListener cl = new ComponentListener(){
				public void componentHidden(ComponentEvent e) {}
				public void componentMoved(ComponentEvent e) {}
				public void componentResized(ComponentEvent e) {
					d.setSize(d.getPreferredSize());
				}
				public void componentShown(ComponentEvent e) {}
			};
			((Resizable)dataEntry).addResizeListener(cl);
        }
        
		SPSUtils.makeJDialogCancellable(d, closeAction);

		JButton cancelButton = new JButton(closeAction);
		cancelButton.setText(CANCEL_BUTTON_LABEL);

		// Handle if the user presses Enter in the dialog - do OK action
		d.getRootPane().setDefaultButton(okButton);

		// Now build the GUI.
		final JPanel cp = new JPanel(new BorderLayout());
		final JLabel errorLabel = new JLabel();
		
		errorLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));        
        cp.add(errorLabel, BorderLayout.NORTH);
        errorLabel.setVisible(false);
		
		if (dataEntry instanceof ChangeListeningDataEntryPanel) {		    		    
		    ((ChangeListeningDataEntryPanel) dataEntry).addErrorTextListener(new ErrorTextListener() {                
		        public void textChanged(String s) {
                    errorLabel.setText(s);
                    boolean noError = s == null || s.trim().equals("");
                    errorLabel.setVisible(!noError);
                    okButton.setEnabled(noError);
                    d.pack();
		        }
		    });
		}

		cp.add(panel, BorderLayout.CENTER);
		cp.add(ButtonBarFactory.buildOKCancelBar(okButton, cancelButton),
				BorderLayout.SOUTH);

		cp.setBorder(Borders.DIALOG_BORDER);

		//d.add(cp);
		d.setContentPane(cp);

		d.pack();

		d.setBounds((int) d.getBounds().getX(), (int) d.getBounds().getY(), 
		        (int) Math.min(d.getBounds().getWidth(), d.getToolkit().getScreenSize().getWidth()), 
		        (int) Math.min(d.getBounds().getHeight(), d.getToolkit().getScreenSize().getHeight()));
		
		d.setLocationRelativeTo(dialogParent);
		
		return d;
	}

	/**
	 * Returns a JDialog with the given Window as its parent, and with the given title.
	 * If the dialog parent is neither a Frame or Dialog, it will throw an IllegalArgumentException
	 */
	private static JDialog createDialog(final Component dialogParentComponent, final String dialogTitle) {
		JDialog dialog;
	      
        Window dialogParent;
        if (dialogParentComponent instanceof Window) {
            dialogParent = (Window) dialogParentComponent;
        } else {
            dialogParent = SwingUtilities.getWindowAncestor(dialogParentComponent);
        }
		
		if ( dialogParent == null ) {
            dialog = new JDialog();
            if (logger.isDebugEnabled()) {
            	JOptionPane.showMessageDialog(null,
            	"This action called createDataEntryPanelDialog with DialogParent == null!"); //$NON-NLS-1$
            }
        } else if (dialogParent instanceof Frame) {
			dialog = new JDialog((Frame) dialogParent, dialogTitle);
		} else if (dialogParent instanceof Dialog) {
			dialog = new JDialog((Dialog) dialogParent, dialogTitle);
		} else {
			throw new IllegalArgumentException(
					"The dialogParent you gave me is not a " //$NON-NLS-1$
							+ "Frame or Dialog (it is a " //$NON-NLS-1$
							+ dialogParent.getClass().getName() + ")"); //$NON-NLS-1$
		}
		return dialog;
	}

    
    /**
     * Build a JDialog around an object that implements DataEntryPanel, to
     * provide consistent behaviours such as Cancel button, <ESC> to close, and
     * so on.
     *
     * @param dataEntry
     *            The DataEntryPanel implementation
     * @param dialogParent
     *            A Window class to be the parent, or null
     * @param dialogTitle
     *            The display title.
     * @param actionButtonTitle
     *            The title for the OK button
     * @return The built JDialog
     */
    public static JDialog createSingleButtonDataEntryPanelDialog(
    		final DataEntryPanel dataEntry,
    		final Component dialogParent,
    		final String dialogTitle,
    		final String actionButtonTitle) {
    	
    	Action okAction = new AbstractAction() {
    		public void actionPerformed(ActionEvent e) {
    			dataEntry.applyChanges();
    		}
    	};
    	
    	return createSingleButtonDataEntryPanelDialog(dataEntry, dialogParent, dialogTitle,
    			actionButtonTitle, okAction );
    }

	/**
	 * Build a JDialog around an object that implements DataEntryPanel, to
	 * provide consistent behaviours such as Cancel button, <ESC> to close, and
	 * so on.
	 *
	 * @param dataEntry
	 *            The DataEntryPanel implementation
	 * @param dialogParent
	 *            A Window class to be the parent, or null
	 * @param dialogTitle
	 *            The display title.
	 * @param actionButtonTitle
	 *            The title for the OK button
	 * @return The built JDialog
	 */
	public static JDialog createDataEntryPanelDialog(
			final DataEntryPanel dataEntry,
			final Component dialogParent,
			final String dialogTitle,
			final String actionButtonTitle) {

		Callable<Boolean> okCall = new Callable<Boolean>() {
			public Boolean call() {
				if (dataEntry.hasUnsavedChanges()) {
					return new Boolean(dataEntry.applyChanges());
				}
				return new Boolean(true);
			}
		};
		
		Callable<Boolean> cancelCall = new Callable<Boolean>() {
			public Boolean call() {
				if (dataEntry.hasUnsavedChanges()) {
					//used to give a dialog asking whether you want to discard, but
					//it was weird because you'd expect to discard changes when you
					//pressed cancel on the current dialog to cause this to happen.
					dataEntry.discardChanges();
				}
				//doesn't really make sense for cancel to fail
				return new Boolean(true);
			}
		};
		return createDataEntryPanelDialog(dataEntry, dialogParent, dialogTitle,
				actionButtonTitle, okCall, cancelCall);
	}


    /**
     * Build a JDialog around an object that implements DataEntryPanel, to
     * provide consistent behaviours such as Cancel button, <ESC> to close, and
     * so on.
     * XXX Worry about modal vs non-modal
     * XXX This is not called by anything 
     * @param dataEntry
     *            The DataEntryPanel implementation
     * @param dialogParent
     *            A Window object to be the dialog's parent
     * @param dialogTitle
     *            The dialog title.
     * @param actionButtonTitle
     *            The label text for the OK button
     * @param okAction Action to be invoked when the OK/action button is
     *  pressed; does NOT need to dismiss the dialog (we do that if applyChanges() returns true).
     * @return The new JDialog, which has the panel in it along with OK and Cancel buttons
     */
    public static JDialog createSingleButtonDataEntryPanelDialog(
            final DataEntryPanel dataEntry,
            final Component dialogParent,
            final String dialogTitle,
            final String actionButtonTitle,
            final Action okAction ) {

    	final JDialog d = createDialog(dialogParent, dialogTitle);
        JComponent panel = dataEntry.getPanel();


        
        JButton okButton = new JDefaultButton(okAction);
        okButton.setText(actionButtonTitle);
        // In all cases we have to close the dialog.
        Action closeAction = new CommonCloseAction(d);
        okButton.addActionListener(closeAction);
        SPSUtils.makeJDialogCancellable(d, closeAction);
        okButton.addActionListener(new CommonCloseAction(d));

        // Handle if the user presses Enter in the dialog - do OK action
        d.getRootPane().setDefaultButton(okButton);


        // Now build the GUI.
        JPanel cp = new JPanel(new BorderLayout());
        cp.setBorder(BorderFactory.createEmptyBorder(12,12,12,12));
        cp.add(panel, BorderLayout.CENTER);

        cp.add(ButtonBarFactory.buildCenteredBar(okButton),
                BorderLayout.SOUTH);
        cp.setBorder(Borders.DIALOG_BORDER);

        //d.add(cp);
        d.setContentPane(cp);

        // XXX maybe pass yet another argument for this?
        // d.setLocationRelativeTo(someFrame);

        d.pack();
        return d;
    }
}
