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
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.table.TableModel;
import javax.swing.text.Document;

import ca.sqlpower.sql.WebResultHTMLFormatter;
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
					final FontSelector fontSelector = new FontSelector((JFrame) parentTable.getTopLevelAncestor().getParent(), parentTable.getFont());
					fontSelector.getApplyButton().addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							setFont(fontSelector.getSelectedFont());
							TableUtils.fitColumnWidths(parentTable, 0);
							
							FontRenderContext frc = ((Graphics2D) parentTable.getGraphics()).getFontRenderContext();
							Rectangle2D fontBounds = fontSelector.getSelectedFont().getMaxCharBounds(frc);
							parentTable.setRowHeight((int) fontBounds.getHeight());
						}
					});
					fontSelector.pack();
					fontSelector.setVisible(true);
				}
			});
			menu.add(new AbstractAction("Export Selected to HTML..") {
				public void actionPerformed(ActionEvent e) {
					JFileChooser chooser = new JFileChooser();
					int chooserResult =  chooser.showSaveDialog(parentTable);
					if (chooserResult == JFileChooser.APPROVE_OPTION) {
						File file = chooser.getSelectedFile();
						WebResultHTMLFormatter htmlFormatter = new WebResultHTMLFormatter();
						PrintWriter writer = null;
						try {
							writer = new PrintWriter(file);
							TableModelHTMLFormatter.formatToStream(parentTable.getModel(), writer, parentTable.getSelectedRows());
						} catch (FileNotFoundException ex) {
							throw new RuntimeException("Could not open file " + file.getName(), ex);
						} finally {
							if (writer != null) {
								writer.close();
							}
						}
					}
					
				}
			});
			
			menu.add(new AbstractAction("Export Selected to CSV..") {
				public void actionPerformed(ActionEvent e) {
					JFileChooser chooser = new JFileChooser();
					int chooserResult =  chooser.showSaveDialog(parentTable);
					if (chooserResult == JFileChooser.APPROVE_OPTION) {
						File file = chooser.getSelectedFile();
						PrintWriter writer = null;
						try {
							writer = new PrintWriter(file);
							TableModelCSVFormatter.formatToStream(parentTable.getModel(), writer, parentTable.getSelectedRows());
						} catch (FileNotFoundException ex) {
							throw new RuntimeException("Could not open file " + file.getName(), ex);
						} finally {
							if (writer != null) {
								writer.close();
							}
						}
					}
					
				}
			});
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
			return getModel().getValueAt(row, col).toString();
		}

		public int modelIndex(int viewIndex) {
			return viewIndex;
		}
	};
		
	/**
	 * A mouse listener on the JTable to give the user options like exporting
	 * and changing font.
	 */
	private MouseListener popupMenuMouseListener = new PopupMenuMouseListener(this);

	public FancyExportableJTable(TableModel model, Document doc) {
		model = new TableModelSortDecorator(model, getTableHeader());
		if (doc != null) {
			TableModelSearchDecorator newModel = new TableModelSearchDecorator(model);
			newModel.setDoc(doc);
			newModel.setTableTextConverter(textConverter);
			model = newModel;
		}
		
		setModel(model);
		addMouseListener(popupMenuMouseListener);
		TableUtils.fitColumnWidths(this, 0);
	}
	
	public FancyExportableJTable(TableModel model) {
		this(model, null);
	}

}
