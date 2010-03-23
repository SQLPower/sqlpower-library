package ca.sqlpower.swingui.db;

import java.awt.Window;

import javax.swing.JDialog;

import ca.sqlpower.sql.DataSourceCollection;
import ca.sqlpower.sql.JDBCDataSource;
import ca.sqlpower.sql.Olap4jDataSource;

/**
 * Generic interface for creating and showing a dialog box that allows users to
 * edit a database connection spec (SPDatasource) object.
 * <p>
 * Different applications have different extra fields they want the user to fill
 * out, so the library can't just create a dialog box on its own for doing this.
 */
public interface DataSourceDialogFactory {

	/**
	 * Shows a dialog box for editing the properties of the given JDBCDataSource
	 * object. It is up to the application to decide if it's ok to display
	 * multiple data source dialogs at the same time, or to reuse the same one
	 * over and over.
	 * 
	 * @param parentWindow
	 *            The window that should own the dialog.
	 * @param dataSource
	 *            The data source to edit.
	 * @param onAccept
	 *            The action to perform when the user presses the OK button of
	 *            the dialog. Regardless of what this Runnable does, the dialog
	 *            will always update the data source with the information
	 *            entered in the GUI then hide itself. The Runnable might want
	 *            to do something such as adding the data source to a
	 *            DataSourceCollection when the user presses OK. This argument
	 *            may be given as null, in which case only the default action is
	 *            taken.
	 * @return The dialog which has been made visible.
	 */
	public JDialog showDialog(Window parentWindow, JDBCDataSource dataSource, Runnable onAccept);

    /**
     * Shows a dialog box for editing the properties of the given
     * Olap4jDataSource object. It is up to the application to decide if it's ok
     * to display multiple data source dialogs at the same time, or to reuse the
     * same one over and over.
     * 
     * @param parentWindow
     *            The window that should own the dialog.
     * @param dataSource
     *            The data source to edit.
     * @param dsCollection
     *            This collection contains all of the JDBCDataSource objects
     *            that can be used in the {@link Olap4jDataSource} with an XML
     *            schema.
     * @param onAccept
     *            The action to perform when the user presses the OK button of
     *            the dialog. Regardless of what this Runnable does, the dialog
     *            will always update the data source with the information
     *            entered in the GUI then hide itself. The Runnable might want
     *            to do something such as adding the data source to a
     *            DataSourceCollection when the user presses OK. This argument
     *            may be given as null, in which case only the default action is
     *            taken.
     * @return The dialog which has been made visible.
     */
    public JDialog showDialog(Window parentWindow, Olap4jDataSource dataSource, DataSourceCollection<? super JDBCDataSource> dsCollection, Runnable onAccept);
}

