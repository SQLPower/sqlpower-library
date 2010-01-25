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
import java.util.concurrent.Callable;

import javax.swing.AbstractAction;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import javax.swing.text.Document;

import ca.sqlpower.swingui.DataEntryPanelBuilder;
import ca.sqlpower.swingui.FontSelector;

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
		
		private final JTable parentTable;
		
		public PopupMenuMouseListener(JTable c) {
			menu = new JPopupMenu();
			parentTable = c;
			menu.add(new AbstractAction("Change Font Size..") {
				public void actionPerformed(ActionEvent arg0) {
					final FontSelector fontSelector = new FontSelector(parentTable.getFont());
					Callable<Boolean> okCall = new Callable<Boolean>() {
					    public Boolean call() {
							setFont(fontSelector.getSelectedFont());
							TableUtils.fitColumnWidths(parentTable, 15);
							
							FontRenderContext frc = ((Graphics2D) parentTable.getGraphics()).getFontRenderContext();
							Rectangle2D fontBounds = fontSelector.getSelectedFont().getMaxCharBounds(frc);
							parentTable.setRowHeight((int) fontBounds.getHeight());
							
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
					        parentTable.getTopLevelAncestor(),
					        "Choose a font",
					        "OK",
					        okCall,
					        cancelCall);
					d.setVisible(true);
				}
			});
			menu.add(new AbstractAction("Export Selected to HTML..") {
				public void actionPerformed(ActionEvent e) {
					TableModelHTMLFormatter htmlFormatter= new TableModelHTMLFormatter();
					JFileChooser chooser = new JFileChooser();
					int chooserResult =  chooser.showSaveDialog(parentTable);
					if (chooserResult == JFileChooser.APPROVE_OPTION) {
						File file = chooser.getSelectedFile();
						try {
							PrintWriter writer = new PrintWriter(file);
							if(parentTable.getSelectedRows().length != 0){
								htmlFormatter.formatToStream(parentTable.getModel(), writer, parentTable.getSelectedRows());						
							}
							else {
								htmlFormatter.formatToStream(parentTable.getModel(), writer);
							}
						} catch (FileNotFoundException ex) {
							throw new RuntimeException("Could not open file " + file.getName(), ex);
						} 

					}

				}
					
			});
			
			menu.add(new AbstractAction("Export Selected to CSV..") {
				public void actionPerformed(ActionEvent e) {
					TableModelCSVFormatter csvFormatter = new TableModelCSVFormatter();

					JFileChooser chooser = new JFileChooser();
					int chooserResult =  chooser.showSaveDialog(parentTable);
					if (chooserResult == JFileChooser.APPROVE_OPTION) {
						File file = chooser.getSelectedFile();
						try {
							PrintWriter writer = new PrintWriter(file);
							if(parentTable.getSelectedRows().length != 0){
								csvFormatter.formatToStream(parentTable.getModel(), writer, parentTable.getSelectedRows());						
							}
							else {
								csvFormatter.formatToStream(parentTable.getModel(), writer);
							}
							
						} catch (FileNotFoundException ex) {
							throw new RuntimeException("Could not open file " + file.getName(), ex);
						} 
						
					}

			}});
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
	 * The text converter for the search table model. This is used to get the strings
	 * of cells in the table to compare them against the text field used in searching.
	 */
	private TableTextConverter textConverter = new TableTextConverter() {
		public String getTextForCell(int row, int col) {
			Object cellValue = getModel().getValueAt(row, col);
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

	private final TableModelListener tableListener = new TableModelListener() {
		public void tableChanged(TableModelEvent e) {
			createDefaultColumnsFromModel();
		}
	};
	
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
		addMouseListener(new PopupMenuMouseListener(this));
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
		
		model.addTableModelListener(tableListener);
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

}
