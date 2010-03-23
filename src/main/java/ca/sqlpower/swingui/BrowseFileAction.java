package ca.sqlpower.swingui;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.io.File;

import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import javax.swing.JTextField;

/**
 * An action for popping up a file chooser to edit a path name in a JTextField.
 * This action can be used to provide the typical "..." button found beside so
 * many fine text fields these days.
 */
public class BrowseFileAction extends AbstractAction {

	/**
	 * The component that should be the parent component of the file
	 * chooser dialog.
	 */
	private final Component parentComponent;
	
	/**
	 * The field to use for the file path.
	 */
	private final JTextField field;

	public BrowseFileAction(Component parentComponent, JTextField field) {
		super("..."); //$NON-NLS-1$
		if (field == null) throw new NullPointerException("You have to specify a field"); //$NON-NLS-1$
		this.parentComponent = parentComponent;
		this.field = field;
	}
	
	public void actionPerformed(ActionEvent e) {
		JFileChooser fileChooser = new JFileChooser();
		if (field.getText() != null && field.getText().length() > 0) {
			File defaultFile = new File(field.getText());
			fileChooser.setCurrentDirectory(defaultFile);
			fileChooser.setSelectedFile(defaultFile);
		}
		// Reason I didn't just use showOpenDialog here is because on OS X, it
		// doesn't allow for a custom filename to be given, meaning that you
		// would have to specify an existing file as the log file, which is not
		// what we want.
		int returnVal = fileChooser.showDialog(parentComponent, "Open");
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			final File file = fileChooser.getSelectedFile();
			field.setText(file.getPath());
		}
	}
	
}