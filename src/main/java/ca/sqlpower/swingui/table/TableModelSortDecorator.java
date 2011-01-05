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

import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import org.apache.log4j.Logger;

/**
 * TableSorter is a decorator for TableModels; adding sorting
 * functionality to a supplied TableModel. TableSorter does
 * not store or copy the data in its TableModel; instead it maintains
 * a map from the row indexes of the view to the row indexes of the
 * model. As requests are made of the sorter (like getValueAt(row, col))
 * they are passed to the underlying model after the row numbers
 * have been translated via the internal mapping array. This way,
 * the TableSorter appears to hold another copy of the table
 * with the rows in a different order.
 * <p/>
 * TableSorter registers itself as a listener to the underlying model,
 * just as the JTable itself would. Events recieved from the model
 * are examined, sometimes manipulated (typically widened), and then
 * passed on to the TableSorter's listeners (typically the JTable).
 * If a change to the model has invalidated the order of TableSorter's
 * rows, a note of this is made and the sorter will resort the
 * rows the next time a value is requested.
 * <p/>
 * When the tableHeader property is set, either by using the
 * setTableHeader() method or the two argument constructor, the
 * table header may be used as a complete UI for TableSorter.
 * The default renderer of the tableHeader is decorated with a renderer
 * that indicates the sorting status of each column. In addition,
 * a mouse listener is installed with the following behavior:
 * <ul>
 * <li>
 * Mouse-click: Clears the sorting status of all other columns
 * and advances the sorting status of that column through three
 * values: {NOT_SORTED, ASCENDING, DESCENDING} (then back to
 * NOT_SORTED again).
 * <li>
 * SHIFT-mouse-click: Clears the sorting status of all other columns
 * and cycles the sorting status of the column through the same
 * three values, in the opposite order: {NOT_SORTED, DESCENDING, ASCENDING}.
 * <li>
 * CONTROL-mouse-click and CONTROL-SHIFT-mouse-click: as above except
 * that the changes to the column do not cancel the statuses of columns
 * that are already sorting - giving a way to initiate a compound
 * sort.
 * </ul>
 * <p/>
 * This is a long overdue rewrite of a class of the same name that
 * first appeared in the swing table demos in 1997.
 *
 * @author Philip Milne
 * @author Brendon McLean
 * @author Dan van Enckevort
 * @author Parwinder Sekhon
 * @version 2.0 02/27/04
 */

public class TableModelSortDecorator extends AbstractTableModel implements CleanupTableModel, TableModelWrapper {
	
	private final AtomicBoolean arraysUpToDate = new AtomicBoolean(false);
	
	private static final Logger logger = Logger.getLogger(TableModelSortDecorator.class);
	
    protected TableModel tableModel;

    public static final int DESCENDING = -1;
    public static final int NOT_SORTED = 0;
    public static final int ASCENDING = 1;

    private static Directive EMPTY_DIRECTIVE = new Directive(-1, NOT_SORTED);

    public static final Comparator COMPARABLE_COMAPRATOR = new Comparator() {
        public int compare(Object o1, Object o2) {
            return ((Comparable) o1).compareTo(o2);
        }
    };
    public static final Comparator LEXICAL_COMPARATOR = new Comparator() {
        public int compare(Object o1, Object o2) {
            return o1.toString().compareTo(o2.toString());
        }
    };

    private Row[] viewToModel;
    private int[] modelToView;

    private JTableHeader tableHeader;
    private MouseListener mouseListener;
    private TableModelListener tableModelListener;
    private Map<Class, Comparator> columnComparators = new HashMap<Class, Comparator>();
    private List<Directive> sortingColumns = new ArrayList<Directive>();

    /**
     * The y location of the header wrapped by the sort decorator.
     * This will change the area allowed to be clicked to sort
     * a column. This will be null if it has not been set yet.
     */
	private Integer headerLabelYLoc = null;

	/**
     * The height of the header wrapped by the sort decorator.
     * This will change the area allowed to be clicked to sort
     * a column. This will be null if it has not been set yet.
     */
	private Integer headerLabelHeight = null;

    public TableModelSortDecorator() {
    	logger.debug("Constructing table model sort decorator");
        this.mouseListener = new MouseHandler();
        this.tableModelListener = new TableModelHandler();
    }

    public TableModelSortDecorator(TableModel tableModel) {
        this();
        setWrappedModel(tableModel);
    }

    public TableModelSortDecorator(TableModel tableModel, JTableHeader tableHeader) {
        this();
        setTableHeader(tableHeader);
        setWrappedModel(tableModel);
    }

    private void clearSortingState() {
        viewToModel = null;
        modelToView = null;
    }

    public TableModel getWrappedModel() {
        return tableModel;
    }

    public void setWrappedModel(TableModel tableModel) {
        if (this.tableModel != null) {
            this.tableModel.removeTableModelListener(tableModelListener);
        }

        arraysUpToDate.set(false);
        this.tableModel = tableModel;
        
        if (this.tableModel != null) {
            this.tableModel.addTableModelListener(tableModelListener);
        }

        clearSortingState();
        fireTableStructureChanged();
    }

    public JTableHeader getTableHeader() {
        return tableHeader;
    }

    public void setTableHeader(JTableHeader tableHeader) {
        if (this.tableHeader != null) {
            this.tableHeader.removeMouseListener(mouseListener);
            TableCellRenderer defaultRenderer = this.tableHeader.getDefaultRenderer();
            if (defaultRenderer instanceof SortableHeaderRenderer) {
                this.tableHeader.setDefaultRenderer(((SortableHeaderRenderer) defaultRenderer).tableCellRenderer);
            }
        }
        this.tableHeader = tableHeader;
        if (this.tableHeader != null) {
            this.tableHeader.addMouseListener(mouseListener);
            this.tableHeader.setDefaultRenderer(
                    new SortableHeaderRenderer(this.tableHeader.getDefaultRenderer()));
        }
    }

    public boolean isSorting() {
        return sortingColumns.size() != 0;
    }

    private Directive getDirective(int column) {
        for (int i = 0; i < sortingColumns.size(); i++) {
            Directive directive = sortingColumns.get(i);
            if (directive.column == column) {
                return directive;
            }
        }
        return EMPTY_DIRECTIVE;
    }

    public int getSortingStatus(int column) {
        return getDirective(column).direction;
    }

    private void sortingStatusChanged() {
        clearSortingState();
        fireTableDataChanged();
        if (tableHeader != null) {
            tableHeader.repaint();
        }
    }

    public void setSortingStatus(int column, int status) {
    	LinkedHashMap<Integer, Integer> columnMap = new LinkedHashMap<Integer, Integer>();
    	columnMap.put(column, status);
        setSortingStatus(columnMap);
    }

	/**
	 * This will set the sorting status of multiple rows at once and then fire
	 * the sorting status changed event. Each entry in columnToStatusMap maps a
	 * column to a sorting status defined in this class.
	 * 
	 * A linked hash map is used here to keep the order of the columns as it is
	 * important for sorting.
	 */
    public void setSortingStatus(LinkedHashMap<Integer, Integer> columnToStatusMap) {
    	for (Map.Entry<Integer, Integer> entry :columnToStatusMap.entrySet()) {
    		logger.debug("Sorting status changed. Setting column number " + entry.getKey() + " to status " + entry.getValue(), new Exception());
    		Directive directive = getDirective(entry.getKey());
    		if (directive != EMPTY_DIRECTIVE) {
    			sortingColumns.remove(directive);
    		}
    		if (entry.getValue() != NOT_SORTED) {
    			sortingColumns.add(new Directive(entry.getKey(), entry.getValue()));
    		}
    	}
    	sortingStatusChanged();
    }

    protected Icon getHeaderRendererIcon(int column, int size) {
        Directive directive = getDirective(column);
        if (directive == EMPTY_DIRECTIVE) {
            return null;
        }
        return new Arrow(directive.direction == DESCENDING, size, sortingColumns.indexOf(directive));
    }

    private void cancelSorting() {
        sortingColumns.clear();
        sortingStatusChanged();
    }

    public void setColumnComparator(Class type, Comparator comparator) {
        if (comparator == null) {
            columnComparators.remove(type);
        } else {
            columnComparators.put(type, comparator);
        }
    }

    protected Comparator getComparator(int column) {
        Class columnType = tableModel.getColumnClass(column);
        Comparator comparator = (Comparator) columnComparators.get(columnType);
        if (comparator != null) {
            return comparator;
        }
        if (Comparable.class.isAssignableFrom(columnType)) {
            return COMPARABLE_COMAPRATOR;
        }
        return LEXICAL_COMPARATOR;
    }
    
    private void updateArrays() {

		viewToModel = null;
		modelToView = null;
		
		synchronized (tableModel) {
			
			int tableModelRowCount = tableModel.getRowCount();
			viewToModel = new Row[tableModelRowCount];
			for (int row = 0; row < tableModelRowCount; row++) {
				viewToModel[row] = new Row(row);
			}
			
			if (isSorting()) {
				Arrays.sort(viewToModel);
			}
			
			int n = viewToModel.length;
			modelToView = new int[n];
			for (int i = 0; i < n; i++) {
				modelToView[viewToModel[i].modelIndex] = i;
			}
		
			arraysUpToDate.set(true);
		}
    }

    private Row[] getViewToModel() {
    	if (!arraysUpToDate.get() || viewToModel == null) {
    		updateArrays();
    	}
    	return viewToModel;
    }

    public int modelIndex(int viewIndex) {
		return getViewToModel()[viewIndex].modelIndex;
    }

    private int[] getModelToView() {
    	if (!arraysUpToDate.get() || modelToView == null) {
    		updateArrays();
    	}
    	return modelToView;
    }

    // TableModel interface methods

    public int getRowCount() {
        return (tableModel == null) ? 0 : tableModel.getRowCount();
    }

    public int getColumnCount() {
        return (tableModel == null) ? 0 : tableModel.getColumnCount();
    }

    public String getColumnName(int column) {
        return tableModel.getColumnName(column);
    }

    public Class<?> getColumnClass(int column) {
        return tableModel.getColumnClass(column);
    }

    public boolean isCellEditable(int row, int column) {
        return tableModel.isCellEditable(modelIndex(row), column);
    }

    public Object getValueAt(int row, int column) {
        return tableModel.getValueAt(modelIndex(row), column);
    }

    public void setValueAt(Object aValue, int row, int column) {
        tableModel.setValueAt(aValue, modelIndex(row), column);
    }

    // Helper classes

    private class Row implements Comparable {
        private int modelIndex;

        public Row(int index) {
            this.modelIndex = index;
        }

        public int compareTo(Object o) {
            int row1 = modelIndex;
            int row2 = ((Row) o).modelIndex;

            for (Iterator<Directive> it = sortingColumns.iterator(); it.hasNext();) {
                Directive directive = (Directive) it.next();
                int column = directive.column;
                Object o1 = tableModel.getValueAt(row1, column);
                Object o2 = tableModel.getValueAt(row2, column);
                int comparison = 0;
                // Define null less than everything, except null.
                if (o1 == null && o2 == null) {
                    comparison = 0;
                } else if (o1 == null) {
                    comparison = -1;
                } else if (o2 == null) {
                    comparison = 1;
                } else {
                    comparison = getComparator(column).compare(o1, o2);
                }
                if (comparison != 0) {
                    return directive.direction == DESCENDING ? -comparison : comparison;
                }
            }
            return 0;
        }
    }

    private class TableModelHandler implements TableModelListener {
        public void tableChanged(TableModelEvent e) {
        	
        	arraysUpToDate.set(false);
        	
            // If we're not sorting by anything, just pass the event along.
            if (!isSorting()) {
                clearSortingState();
                fireTableChanged(e);
                return;
            }

            // If the table structure has changed, cancel the sorting; the
            // sorting columns may have been either moved or deleted from
            // the model.
            if (e.getFirstRow() == TableModelEvent.HEADER_ROW) {
                cancelSorting();
                fireTableChanged(e);
                return;
            }

            // We can map a cell event through to the view without widening
            // when the following conditions apply:
            //
            // a) all the changes are on one row (e.getFirstRow() == e.getLastRow()) and,
            // b) all the changes are in one column (column != TableModelEvent.ALL_COLUMNS) and,
            // c) we are not sorting on that column (getSortingStatus(column) == NOT_SORTED) and,
            // d) a reverse lookup will not trigger a sort (modelToView != null)
            //
            // Note: INSERT and DELETE events fail this test as they have column == ALL_COLUMNS.
            //
            // The last check, for (modelToView != null) is to see if modelToView
            // is already allocated. If we don't do this check; sorting can become
            // a performance bottleneck for applications where cells
            // change rapidly in different parts of the table. If cells
            // change alternately in the sorting column and then outside of
            // it this class can end up re-sorting on alternate cell updates -
            // which can be a performance problem for large tables. The last
            // clause avoids this problem.
            int column = e.getColumn();
            if (e.getFirstRow() == e.getLastRow()
                    && column != TableModelEvent.ALL_COLUMNS
                    && getSortingStatus(column) == NOT_SORTED
                    && modelToView != null) {
                int viewIndex = getModelToView()[e.getFirstRow()];
                fireTableChanged(new TableModelEvent(TableModelSortDecorator.this,
                                                     viewIndex, viewIndex,
                                                     column, e.getType()));
                return;
            }

            // Something has happened to the data that may have invalidated the row order.
            clearSortingState();
            fireTableDataChanged();
            return;
        }
    }

    private class MouseHandler extends MouseAdapter {
    	public void mouseClicked(MouseEvent e) {
    		JTableHeader h = (JTableHeader) e.getSource();
    		TableColumnModel columnModel = h.getColumnModel();
    		Integer height = headerLabelHeight;
    		if (height == null) {
    			height = h.getHeight();
    		}
    		logger.debug("Y mouse click at " + e.getY() + " header label y location " + headerLabelYLoc + " header label height " + height);
    		if (e.getY() > headerLabelYLoc && e.getY() < headerLabelYLoc + height){
    			logger.debug("Table header was clicked");
    			int viewColumn = columnModel.getColumnIndexAtX(e.getX());
    			
    			if(viewColumn < 0){
    				return;
    			}
    			int column = columnModel.getColumn(viewColumn).getModelIndex();
    			if (column != -1) {
    				int status = getSortingStatus(column);
    				LinkedHashMap<Integer, Integer> newSortingStatus = new LinkedHashMap<Integer, Integer>();
    				if (!e.isControlDown()) {
    					for (Directive d : sortingColumns) {
    						if (d.getDirection() != NOT_SORTED) {
    							newSortingStatus.put(d.getColumn(), NOT_SORTED);
    						}
    					}
    				}
    				// Cycle the sorting states through {NOT_SORTED, ASCENDING, DESCENDING} or
    				// {NOT_SORTED, DESCENDING, ASCENDING} depending on whether shift is pressed.
    				status = status + (e.isShiftDown() ? -1 : 1);
    				status = (status + 4) % 3 - 1; // signed mod, returning {-1, 0, 1}
    				newSortingStatus.put(column, status);
    				setSortingStatus(newSortingStatus);
    			}
    		}
    	}
    }

    private class SortableHeaderRenderer implements TableCellRenderer {
        private TableCellRenderer tableCellRenderer;

        public SortableHeaderRenderer(TableCellRenderer tableCellRenderer) {
            this.tableCellRenderer = tableCellRenderer;
        }
        
        public Component getTableCellRendererComponent(JTable table,
                                                       Object value,
                                                       boolean isSelected,
                                                       boolean hasFocus,
                                                       int row,
                                                       int column) {
            Component c = tableCellRenderer.getTableCellRendererComponent(table,
                    value, isSelected, hasFocus, row, column);
            if (c instanceof JLabel) {
                JLabel l = (JLabel) c;
                if (headerLabelYLoc == null) {
                	headerLabelYLoc = 0;
                }
                l.setHorizontalTextPosition(JLabel.LEFT);
                int modelColumn = table.convertColumnIndexToModel(column);
                l.setIcon(getHeaderRendererIcon(modelColumn, l.getFont().getSize()));
            }
            return c;
        }
    }

    private static class Directive {
        private int column;
        private int direction;

        public Directive(int column, int direction) {
            this.column = column;
            this.direction = direction;
        }
        
        public int getColumn() {
			return column;
		}
        
        public int getDirection() {
			return direction;
		}
    }
    
    /**
     * This will allow setting the y location and height of the
     * table header if only part of the table header should be
     * clickable to sort.
     */
    public void setTableHeaderYBounds(int yLoc, int height) {
    	headerLabelYLoc = yLoc;
    	headerLabelHeight = height;
    }

	public void cleanup() {
		if (tableModel instanceof CleanupTableModel) {
			((CleanupTableModel) tableModel).cleanup();
		}
	}
}
