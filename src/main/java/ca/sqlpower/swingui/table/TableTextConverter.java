package ca.sqlpower.swingui.table;

/**
 * The TableTextConverter interface is a way for users of a table
 * to find out what text will be displayed on the screen for a particular
 * cell.  This interface can be implemented on a JTable subclass, where
 * implementers should have special knowledge of how values are rendered.
 * For example, if all the cell renderers for your table are JLabels, then
 * you can get the renderer component and cast it to JLabel and get its text.
 */
public interface TableTextConverter {
    
    /**
     * Returns the canonical String value associated with the given
     * row and column of a table.
     */
    String getTextForCell(int row, int col);
    
    /**
     * Returns the index in the model that corresponds to the index
     * that we are looking at in the view. This is for if the rows in
     * the model are rearranged in the view.
     */
    int modelIndex(int viewIndex);
}
