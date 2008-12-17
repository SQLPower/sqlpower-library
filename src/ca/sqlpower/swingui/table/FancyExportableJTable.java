/*
 * Copyright (c) 2008, SQL Power Group Inc.
 * 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in
 *       the documentation and/or other materials provided with the
 *       distribution.
 *     * Neither the name of SQL Power Group Inc. nor the names of its
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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
							TableUtils.fitColumnWidths(parentTable, 0);
							
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
					        (JFrame) parentTable.getTopLevelAncestor().getParent(),
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
		TableUtils.fitColumnWidths(this, 0);
	}
	
	public FancyExportableJTable(TableModel model) {
		this(model, null);
	}
	
	@Override
	public void setModel(TableModel model) {
		
		//Set the inner TableModel wrapped by the search and sort decorators
		//to keep the search and sort decorators attached to their document 
		//and header.
		TableModel innerModel = model;
		if (getModel() instanceof TableModelSearchDecorator) {
			innerModel = ((TableModelSearchDecorator) getModel()).getTableModel();
		}
		if (innerModel instanceof TableModelSortDecorator) {
			((TableModelSortDecorator) innerModel).setTableModel(model);
			super.setModel(getModel());
		} else {
			super.setModel(model);
		}
	}
	
	@Override
	public void createDefaultColumnsFromModel() {
		super.createDefaultColumnsFromModel();
		TableUtils.fitColumnWidths(this, 0);
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
