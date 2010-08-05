package ca.sqlpower.swingui.table;

import java.awt.Component;
import java.awt.Dimension;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import org.apache.log4j.Logger;

/**
 * This class implements the workarounds for the bugs in the jTable
 *
 */
public class EditableJTable extends JTable implements TableTextConverter {

	private static final Logger logger = Logger.getLogger(EditableJTable.class);
	
	public EditableJTable() {
		this(null);
	}

	public EditableJTable(TableModel model) {
		super(model);
		putClientProperty("terminateEditOnFocusLost", true);
		setTableHeader(createDefaultTableHeader());
	}
	
	@Override
	public void removeNotify() {
		super.removeNotify();
		logger.debug("Table removed from hierarchy.  Cleaning up model...");
		if (getModel() instanceof CleanupTableModel) {
			((CleanupTableModel) getModel()).cleanup();
		}
	}
	
	@Override
	public void columnMarginChanged(ChangeEvent pE) {
		if (getEditingColumn() != -1 || getEditingRow() != -1) {
			editCellAt(0, 0);
		}
		super.columnMarginChanged(pE);
	}

	public String getTextForCell(Object o) {
		if (o != null) {
			return o.toString();
		} else {
			return "N/A";
		}
	}

	public int modelIndex(int viewIndex) {
		return viewIndex;
	}

    /**
     * Just logs the call and forwards to superclass implementation.
     */
    @Override
    public void setModel(TableModel dataModel) {
        logger.debug("Table Model change for EditableJTable "+this+
                ". Old model: "+getModel()+" new model: "+dataModel);
        super.setModel(dataModel);
    }
    
    /**
     * Just logs the call and forwards to superclass implementation.
     */
    @Override
    public void setSelectionModel(ListSelectionModel newModel) {
        logger.debug("Table Selection Model change for EditableJTable "+this+
                ". Old model: "+getSelectionModel()+" new model: "+newModel);
        super.setSelectionModel(newModel);
    }
    
    @Override
    public TableCellEditor getCellEditor() {
        TableCellEditor editor = super.getCellEditor();
        logger.debug("EditableJTable.getCellEditor(): returning "+editor);
        return editor;
    }
    
    @Override
    public TableCellEditor getCellEditor(int row, int column) {
        TableColumn tableColumn = getColumnModel().getColumn(column);

        TableCellEditor returnVal = super.getCellEditor(row, column);
        logger.debug("EditableJTable.getCellEditor("+row+","+column+"): returning "+returnVal
                +" (tableColumn.getCellEditor()="+tableColumn.getCellEditor()+")");
        return returnVal;
    }
    
	/**
	 * Taken from the Bug ID:	4292511 of bug parade as a workaround
	 * 
	 * Thanks milnep1!!
	 */
	protected JTableHeader createDefaultTableHeader() {
		return new JTableHeader(columnModel) {
			private int preferredHeight = -1;

			private Component getHeaderRenderer(int columnIndex) {
				TableColumn aColumn = getColumnModel().getColumn(columnIndex);
				TableCellRenderer renderer = aColumn.getHeaderRenderer();
				if (renderer == null) {
					renderer = getDefaultRenderer();
				}
				return renderer.getTableCellRendererComponent(getTable(), aColumn.getHeaderValue(), false, false, -1,
						columnIndex);
			}

			private int getPreferredHeight() {
				if (preferredHeight == -1) {
					preferredHeight = 0;
					TableColumnModel columnModel = getColumnModel();
					for (int column = 0; column < columnModel.getColumnCount(); column++) {
						Component comp = getHeaderRenderer(column);
						int rendererHeight = comp.getPreferredSize().height;
						preferredHeight = Math.max(preferredHeight, rendererHeight);
					}
				}
				return preferredHeight;
			}

			public Dimension getPreferredSize() {
				return new Dimension(super.getPreferredSize().width, getPreferredHeight());
			}

			public void columnAdded(TableColumnModelEvent e) {
				preferredHeight = -1;
				super.columnAdded(e);
			}

			public void columnRemoved(TableColumnModelEvent e) {
				preferredHeight = -1;
				super.columnRemoved(e);
			}
		};
	}

}
