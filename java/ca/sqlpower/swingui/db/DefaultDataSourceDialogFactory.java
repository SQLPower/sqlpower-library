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

import java.awt.Window;
import java.util.concurrent.Callable;

import javax.swing.JDialog;

import ca.sqlpower.sql.DataSourceCollection;
import ca.sqlpower.sql.JDBCDataSource;
import ca.sqlpower.sql.Olap4jDataSource;
import ca.sqlpower.sql.SPDataSource;
import ca.sqlpower.sql.SpecificDataSourceCollection;
import ca.sqlpower.swingui.DataEntryPanel;
import ca.sqlpower.swingui.DataEntryPanelBuilder;
import ca.sqlpower.swingui.JDBCDataSourcePanel;

/**
 * A default factory implementation that just shows a plain data source edit
 * panel.
 */
public class DefaultDataSourceDialogFactory implements DataSourceDialogFactory {

    /**
     * Creates and shows a non-modal dialog box for editing the properties
     * of the given data source.
     */
    public JDialog showDialog(Window parentWindow, JDBCDataSource ds, final Runnable onAccept) {
        final DataEntryPanel dataSourcePanel = new JDBCDataSourcePanel(ds);
        
        return createDataSourceDialog(parentWindow, onAccept, dataSourcePanel);
    }

    /**
     * Helper method that takes a {@link DataEntryPanel} for editing a specific
     * type of {@link SPDataSource} and returns a dialog containing the panel.
     * 
     * @param parentWindow
     *            The parent of the dialog returned.
     * @param onAccept
     *            An action that can be placed on the accept/OK button.
     * @param dataSourcePanel
     *            The {@link DataEntryPanel} for editing an {@link SPDataSource}
     *            .
     */
    private JDialog createDataSourceDialog(Window parentWindow,
            final Runnable onAccept, final DataEntryPanel dataSourcePanel) {
        Callable<Boolean> okCall = new Callable<Boolean>() {
            public Boolean call() {
                if (dataSourcePanel.applyChanges()) {
                    if (onAccept != null) {
                        onAccept.run();
                    }
                    return Boolean.TRUE;
                }
                return Boolean.FALSE;
            }
        };
    
        Callable<Boolean> cancelCall = new Callable<Boolean>() {
            public Boolean call() {
            	dataSourcePanel.discardChanges();
                return Boolean.TRUE;
            }
        };
        
        JDialog d = DataEntryPanelBuilder.createDataEntryPanelDialog(
                dataSourcePanel, parentWindow, "Data Source Properties", "OK", okCall, cancelCall);
        d.pack();
        d.setLocationRelativeTo(parentWindow);
        d.setVisible(true);
        return d;
    }

    public JDialog showDialog(Window parentWindow, Olap4jDataSource dataSource,
            DataSourceCollection<? super JDBCDataSource> dsCollection,
            Runnable onAccept) {
        final DataEntryPanel dataSourcePanel = new Olap4jConnectionPanel(dataSource, new SpecificDataSourceCollection<JDBCDataSource>(dsCollection, JDBCDataSource.class));
        return createDataSourceDialog(parentWindow, onAccept, dataSourcePanel);
    }

}
