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

import java.util.ArrayList;
import java.util.List;

import javax.swing.ComboBoxModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import org.apache.log4j.Logger;

import ca.sqlpower.sql.DataSourceCollection;
import ca.sqlpower.sql.DatabaseListChangeEvent;
import ca.sqlpower.sql.DatabaseListChangeListener;
import ca.sqlpower.sql.SPDataSource;

/**
 * If you only want to see a specific {@link SPDataSource} type here
 * see #DataSourceWrapper#.
 */
public class ConnectionComboBoxModel implements ComboBoxModel, DatabaseListChangeListener {

    private static final Logger logger = Logger.getLogger(ConnectionComboBoxModel.class); 
    SPDataSource selectedItem;

    List<? extends SPDataSource> connections;

    List<ListDataListener> listenerList;

    DataSourceCollection plini;

    /**
     * Setup a new connection combo box model with the conections found in the
     * PPLDotIni
     */
    public ConnectionComboBoxModel(DataSourceCollection plini) {
        this.plini = plini;
        listenerList = new ArrayList<ListDataListener>();
        connections = plini.getConnections();
        plini.addDatabaseListChangeListener(this);
    }

    public void cleanup() {
        plini.removeDatabaseListChangeListener(this);
    }

    public void setSelectedItem(Object anItem) {
        int selectedIndex = connections.indexOf(anItem);
        if (selectedIndex >= 0) {
            if (anItem instanceof SPDataSource) {
                selectedItem = (SPDataSource) anItem;
            } else if (anItem == null) {
                selectedItem = null;
            }
            fireContentChangedEvent(selectedIndex);
        }
    }

    public void setSelectedItem(String anItem) {
        for (SPDataSource ds : connections) {
            if (ds.getName().equals(anItem)) {
                selectedItem = ds;
                setSelectedItem(selectedItem);
                return;
            }
        }
        logger.debug("warning: set selected item:" + anItem); //$NON-NLS-1$
    }

    public Object getSelectedItem() {
        return selectedItem;
    }

    public int getSize() {
        return connections.size() + 1;
    }

    public Object getElementAt(int index) {
        if (index == 0) {
            return null;
        }
        return connections.get(index - 1);
    }

    public void addListDataListener(ListDataListener l) {
        listenerList.add(l);
    }

    public void removeListDataListener(ListDataListener l) {
        listenerList.remove(l);
    }

    private void fireContentChangedEvent(int index) {

        for (int i = listenerList.size() - 1; i >= 0; i--) {
            listenerList.get(i).contentsChanged(new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, index, index));
        }
    }

    public void databaseAdded(DatabaseListChangeEvent e) {
        connections = plini.getConnections();
        for (int i = listenerList.size() - 1; i >= 0; i--) {
            listenerList.get(i).contentsChanged(
                    new ListDataEvent(this, ListDataEvent.INTERVAL_ADDED, e.getListIndex(), e.getListIndex()));
        }

    }

    public void databaseRemoved(DatabaseListChangeEvent e) {
        connections = plini.getConnections();
        for (int i = listenerList.size() - 1; i >= 0; i--) {
            listenerList.get(i).contentsChanged(
                    new ListDataEvent(this, ListDataEvent.INTERVAL_REMOVED, e.getListIndex(), e.getListIndex()));
        }
    }

}
