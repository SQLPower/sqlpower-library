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
package ca.sqlpower.swingui.table;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
/**
 * This class is used to manipulate the sizing on columns. 
 * By ctrl + selecting the header, the column will automatically resize to fit
 * all the elements under it.  However at the moment, it will not expand fully
 * to its desired size due to the fact that the maximum amount of space a column
 * has is bounded by the other columns as well. This problem could be fixed
 * if the table is in a JScrollPane
 */
public class TableModelColumnAutofit extends AbstractTableModel{

    private TableModel tableModel;
    private MouseListener mouseListener;
    private JTable table;
    private JTableHeader tableHeader;

    public TableModelColumnAutofit(TableModel tableModel, JTable table){
        this.tableModel = tableModel;
        this.table = table;
        tableHeader = table.getTableHeader();
        mouseListener = new MouseListener();  
        tableHeader.addMouseListener(mouseListener);
    }
    
    public int getRowCount() {
        if (tableModel == null) return 0;
        else return tableModel.getRowCount();
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        if (tableModel == null) return null;
        return tableModel.getColumnClass(columnIndex);
    }
    
    public int getColumnCount() {
        if (tableModel == null) return 0;
        else return tableModel.getColumnCount();
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        if (tableModel == null) return null;
        else return tableModel.getValueAt(rowIndex, columnIndex);
    }
    @Override
    public String getColumnName(int column) {
        if (tableModel == null) return null;
        return tableModel.getColumnName(column);
    }

    public JTableHeader getTableHeader() {
        return tableHeader;
    }

    public void setTableHeader(JTableHeader tableHeader) {
        this.tableHeader.removeMouseListener(mouseListener);
        this.tableHeader = tableHeader;
        this.tableHeader.addMouseListener(mouseListener);
    }
  
    /*
     * This method picks good column sizes.
     * If all column heads are wider than the column's cells'
     * contents, then you can just use column.sizeWidthToFit().
     */
    public void initColumnSizes() {
        for (int i = 0; i < getColumnCount(); i++) {
            initSingleColumnSize(i);
        }
    }

    public void initSingleColumnSize(int colIndex) {
        TableUtils.fitColumnWidth(table, colIndex, 0);
    }
    
    private class MouseListener extends MouseAdapter {
        public void mouseClicked(MouseEvent e) {
            JTableHeader h = (JTableHeader) e.getSource();
            TableColumnModel columnModel = h.getColumnModel();
            int viewColumn = columnModel.getColumnIndexAtX(e.getX());
            //XXX: Should change to a better condition for size editting
            //     for now, it's just ctrl click on the header
            if (e.isControlDown()){                                                    
                initSingleColumnSize(viewColumn);
            }            
        }
    }
    
}
