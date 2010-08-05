/*
 * Copyright (c) 2009, SQL Power Group Inc.
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

import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.text.Format;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JPopupMenu;
import javax.swing.table.TableModel;
import javax.swing.text.Document;

import ca.sqlpower.swingui.DataEntryPanelBuilder;
import ca.sqlpower.swingui.FontSelector;
import ca.sqlpower.swingui.SPSUtils;

/**
 * This class contains a JTable that displays the given model. The JTable can be sorted
 * by clicking on headers, the font can be changed and selected rows can be exported.
 * 
 * The class also contains a text area that can be used to filter the table as a user
 * types into the text area.
 */
public class FancyExportableJTable extends EditableJTable {
	
	/**
	 * This mouse listener will make a pop-up menu appear to allow users to modify properties
	 * of the table or export selected regions.
	 */
	private class PopupMenuMouseListener implements MouseListener {

		private JPopupMenu menu;
		
		public PopupMenuMouseListener() {
			menu = new JPopupMenu();
			menu.add(new AbstractAction("Change Font Size..") {
				public void actionPerformed(ActionEvent arg0) {
					final FontSelector fontSelector = new FontSelector(getFont());
					Callable<Boolean> okCall = new Callable<Boolean>() {
					    public Boolean call() {
							setFont(fontSelector.getSelectedFont());
							TableUtils.fitColumnWidths(FancyExportableJTable.this, 15);
							
							FontRenderContext frc = ((Graphics2D) getGraphics()).getFontRenderContext();
							Rectangle2D fontBounds = fontSelector.getSelectedFont().getMaxCharBounds(frc);
							setRowHeight((int) fontBounds.getHeight());
							
							return true;
						}
					};
					Callable<Boolean> cancelCall = new Callable<Boolean>() {
					    public Boolean call() throws Exception {
					        return true;
					    }
					};
					JDialog d = DataEntryPanelBuilder.createDataEntryPanelDialog(
					        fontSelector,
					        getTopLevelAncestor(),
					        "Choose a font",
					        "OK",
					        okCall,
					        cancelCall);
					d.setVisible(true);
				}
			});
			menu.add(exportHTMLAction);
			
			menu.add(exportCSVAction);
			menu.pack();
		}
		
		public void mouseClicked(MouseEvent e) {
			if (e.getButton() == MouseEvent.BUTTON3) {
				menu.show(e.getComponent(), e.getX(), e.getY());
			} else {
				menu.setVisible(false);
			}
		}

		public void mouseEntered(MouseEvent e) {}
		public void mouseExited(MouseEvent e) {}
		public void mousePressed(MouseEvent e) {}
		public void mouseReleased(MouseEvent e) {}
	}
	
	/**
	 * This action will export the table in its current state to HTML.
	 */
	private final Action exportHTMLAction = new AbstractAction("Export Selected to HTML..") {
        public void actionPerformed(ActionEvent e) {
            TableModelHTMLFormatter htmlFormatter= new TableModelHTMLFormatter();
            for (Map.Entry<Integer, Format> entry : columnFormatters.entrySet()) {
                htmlFormatter.setFormatter(entry.getKey(), entry.getValue());
            }
            
            JFileChooser chooser = new JFileChooser();
            chooser.addChoosableFileFilter(SPSUtils.HTML_FILE_FILTER);
            chooser.setFileFilter(SPSUtils.HTML_FILE_FILTER);
            int chooserResult =  chooser.showSaveDialog(FancyExportableJTable.this);
            if (chooserResult == JFileChooser.APPROVE_OPTION) {
                File file = chooser.getSelectedFile();
                try {
                    PrintWriter writer = new PrintWriter(file);
                    if (getSelectedRows().length != 0){
                        htmlFormatter.formatToStream(getModel(), writer, getSelectedRows());                        
                    } else {
                        htmlFormatter.formatToStream(getModel(), writer);
                    }
                } catch (FileNotFoundException ex) {
                    throw new RuntimeException("Could not open file " + file.getName(), ex);
                } 
            }
        }
    };
    
    /**
     * Formatters for the export for columns.
     */
    private final Map<Integer, Format> columnFormatters = new HashMap<Integer, Format>();
    
    /**
     * This action will export the table in its current state to CSV.
     */
    private final Action exportCSVAction = new AbstractAction("Export Selected to CSV..") {
        public void actionPerformed(ActionEvent e) {
            TableModelCSVFormatter csvFormatter = new TableModelCSVFormatter();
            for (Map.Entry<Integer, Format> entry : columnFormatters.entrySet()) {
                csvFormatter.setFormatter(entry.getKey(), entry.getValue());
            }
            
            JFileChooser chooser = new JFileChooser();
            chooser.addChoosableFileFilter(SPSUtils.CSV_FILE_FILTER);
            chooser.setFileFilter(SPSUtils.CSV_FILE_FILTER);
            int chooserResult =  chooser.showSaveDialog(FancyExportableJTable.this);
            if (chooserResult == JFileChooser.APPROVE_OPTION) {
                File file = chooser.getSelectedFile();
                try {
                    PrintWriter writer = new PrintWriter(file);
                    if (getSelectedRows().length != 0) {
                        csvFormatter.formatToStream(getModel(), writer, getSelectedRows());                     
                    } else {
                        csvFormatter.formatToStream(getModel(), writer);
                    }
                } catch (FileNotFoundException ex) {
                    throw new RuntimeException("Could not open file " + file.getName(), ex);
                } 
            }
        }
    };
		
	/**
	 * The text converter for the search table model. This is used to get the strings
	 * of cells in the table to compare them against the text field used in searching.
	 */
	private TableTextConverter textConverter = new TableTextConverter() {
		public String getTextForCell(Object cellValue) {
			if (cellValue != null) {
				return cellValue.toString();
			} else {
				return "";
			}
		}

		public int modelIndex(int viewIndex) {
			return viewIndex;
		}
	};
		

	/**
	 * The sort decorator attached to this table.
	 */
	private TableModelSortDecorator sortDecorator;

	public FancyExportableJTable(TableModel model, Document doc) {
		sortDecorator = new TableModelSortDecorator(model, getTableHeader());
		model = sortDecorator;
		if (doc != null) {
			TableModelSearchDecorator newModel = new TableModelSearchDecorator(model);
			newModel.setDoc(doc);
			newModel.setTableTextConverter(textConverter);
			model = newModel;
		}
		
		setModel(model);
		addMouseListener(new PopupMenuMouseListener());
		TableUtils.fitColumnWidths(this, 15);
	}
	
	public FancyExportableJTable(TableModel model) {
		this(model, null);
	}
	
	@Override
	public void setModel(TableModel model) {
		TableModel m = getModel();
		if (! (m instanceof TableModelWrapper)) {
		    super.setModel(model);
		} else {
		    TableModelWrapper lowestWrapper = (TableModelWrapper) m;
		    
		    // down the rabbit hole as far as it goes
		    while (lowestWrapper.getWrappedModel() instanceof TableModelWrapper) {
		        lowestWrapper = (TableModelWrapper) lowestWrapper.getWrappedModel();
		    }
		    
		    lowestWrapper.setWrappedModel(model);
		}
	}
	
	@Override
	public void createDefaultColumnsFromModel() {
		super.createDefaultColumnsFromModel();
		TableUtils.fitColumnWidths(this, 15);
	}
	
	
	/**
	 * This will get the {@link TableModelSortDecorator} attached to this 
	 * table if it exists. If this table does not have a sort decorator
	 * null will be returned.
	 * @return
	 */
	public TableModelSortDecorator getTableModelSortDecorator() {
		return sortDecorator;
	}
	
	/**
	 * Returns the action that will export the table to HTML. This is
	 * the same action that appears when right clicking on the table.
	 */
	public Action getExportHTMLAction() {
	    return exportHTMLAction;
	}
	
	/**
     * Returns the action that will export the table to CSV. This is
     * the same action that appears when right clicking on the table.
     */
	public Action getExportCSVAction() {
	    return exportCSVAction;
	}

    /**
     * Sets a formatter for the given column of a table model for exporting to
     * CSV or HTML. If the column does not exist because the table is too small
     * the formatter will not be used.
     */
	public void setColumnFormatter(int column, Format formatter) {
	    columnFormatters.put(column, formatter);
	}
}
