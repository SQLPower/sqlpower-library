package ca.sqlpower.swingui.db;

import java.awt.Window;


/**
 * Generic interface for creating and showing a dialog box that allows users to
 * edit a database connection type (SPDatasourceType) object.
 * <p>
 * Different applications have different extra fields they want the user to fill
 * out, so the library can't just create a dialog box on its own for doing this.
 */
public interface DataSourceTypeDialogFactory {

	/**
	 * Shows the user interface for maintaining the data source types.
	 * If the interface is already visible it is brought to the front and
	 * given focus.
	 * 
	 * @param owner The dialog or frame that owns the dialog that this factory creates 
	 * @return The dialog that has been shown
	 */
	public Window showDialog(Window owner);
}
