package ca.sqlpower.swingui.table;

/**
 * A JTable table model with an additional method for cleaning up
 * its resources.  Our EditableJTable class is aware of this interface
 * and will ask a CleanupTableModel to clean up when the table becomes
 * undisplayable.
 */
public interface CleanupTableModel {
	
	/**
	 * Asks this table model to permanently clean up its resources.
	 */
	void cleanup();
}
