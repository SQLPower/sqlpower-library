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

import ca.sqlpower.sql.JDBCDataSourceType;
import ca.sqlpower.swingui.DataEntryPanel;

/**
 * A DataEntryPanel which is used as a tab in the 
 * {@link ca.sqlpower.swingui.db.DataSourceTypeEditorPanel}
 * It adds a new method {@link #editDsType(JDBCDataSourceType)} that will modify
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
    public void editDsType(JDBCDataSourceType dsType);
}
