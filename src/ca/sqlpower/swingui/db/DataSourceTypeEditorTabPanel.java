package ca.sqlpower.swingui.db;

import ca.sqlpower.sql.SPDataSourceType;
import ca.sqlpower.swingui.DataEntryPanel;

/**
 * A DataEntryPanel which is used as a tab in the 
 * {@link ca.sqlpower.swingui.db.DataSourceTypeEditorPanel}
 * It adds a new method {@link #editDsType(SPDataSourceType)} that will modify
 * panel contained in the DataEntryPanel implementation when the user changes the 
 * current SPDataSourceType that they are editing in the DataSourceTypeEditor.
 * Classes that implement this interface can be added onto the DataSourceTypeEditorPanel
 * to edit application-specific SPDataSourceType properties, using 
 * {@link ca.sqlpower.swingui.db.DataSourceTypeEditorPanel#addTab(String, DataSourceTypeEditorTabPanel)}
 */
public interface DataSourceTypeEditorTabPanel extends DataEntryPanel {
    
    /**
     * Modifies the panel based on the SPDataSourceType that this panel is editing
     */
    public void editDsType(SPDataSourceType dsType);
}
