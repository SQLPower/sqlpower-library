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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Vector;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import org.apache.log4j.Logger;

import ca.sqlpower.sql.SQLGroupFunction;


/* A renderer that extends JPanel.This renderer will add a JComboBox 
 * and a JtextArea to the JHeader of he resultTable for
 * Group By and Having filters.  
 */
 
public class ComponentCellRenderer extends JPanel implements TableCellRenderer {
	
	private static final Logger logger = Logger.getLogger(ComponentCellRenderer.class);
	
	private final TableCellRenderer renderer;
	private final JTextField havingArea;
	private int labelHeight;
	private int comboBoxHeight;
	private int havingAreaHeight;
	private ArrayList<JComboBox> comboBoxes;
	 
	public ComponentCellRenderer(JTable table){
		
		this.renderer = table.getTableHeader().getDefaultRenderer();
		table.getTableHeader().addMouseListener(new HeaderMouseListener());
		
		comboBoxes = new ArrayList<JComboBox>();
		Vector<String> comboBoxItems = new Vector<String>();
		Object[] tempGroupItems =SQLGroupFunction.values();
		comboBoxItems.add("(GROUP BY)");
		
		for(Object item : tempGroupItems){
			comboBoxItems.add(item.toString());
		}
		
		for(int i = 0 ; i < table.getColumnCount(); i++){
			comboBoxes.add(new JComboBox(comboBoxItems));
		}
		havingArea = new JTextField();
		
		setLayout(new BorderLayout());

	}
	
	/*
	 * Implementing the getComponent method on the renderer, this will take the current header
	 * and add a JComboBox as well as a JTextField for Group By and having filters.
	 */
	public Component getTableCellRendererComponent(JTable table, Object value,
			boolean isSelected, boolean hasFocus, int row, int column) {
	
		Component c = renderer.getTableCellRendererComponent(table, value, 
				isSelected, hasFocus, row, column);
		if(c instanceof JLabel) {
			removeAll();
			add((JLabel)c, BorderLayout.NORTH);
			add(new JComboBox(new Object[] { comboBoxes.get(column).getSelectedItem() }), BorderLayout.CENTER);
			System.out.println("component " + column+ " size"+ comboBoxes.get(column).getSize());
			add(havingArea, BorderLayout.SOUTH);
			labelHeight = c.getPreferredSize().height;
			comboBoxHeight = comboBoxes.get(column).getPreferredSize().height;
			havingAreaHeight = havingArea.getPreferredSize().height;
			
		}
		return this;
	}
	/**
	 * This MouseListener handles clicking on the TableHeader. It will check the position of the click o determine its click
	 * as well as what component is being clicked.
	 */
	private class HeaderMouseListener extends MouseAdapter {

		public void mouseClicked(MouseEvent e) {
			int labelY = labelHeight;
			int comboBoxY = labelHeight + comboBoxHeight;
			int havingFieldY =   labelHeight + comboBoxHeight + havingAreaHeight;
    		JTableHeader h = (JTableHeader) e.getSource();
    		TableColumnModel columnModel = h.getColumnModel();
			if(e.getY() > labelHeight && e.getY() < comboBoxY){
				System.out.println(" its a combobox");
				int viewColumn = columnModel.getColumnIndexAtX(e.getX());
				System.out.println(" view column"+ viewColumn);
		
				JComboBox tempCB = comboBoxes.get(viewColumn);
				h.add(tempCB);
				TableColumn tc = columnModel.getColumn(viewColumn);
				
				tempCB.setBounds(getXPositionOnColumn(columnModel, viewColumn), labelHeight, tc.getWidth(), comboBoxHeight);
				logger.debug("Temporarily placing combo box at " + tempCB.getBounds());
				tempCB.setPopupVisible(true);
				
				tempCB.addPopupMenuListener(new PopupMenuListener() {

					public void popupMenuCanceled(PopupMenuEvent e) {
						// don't care
					}

					public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
						JComboBox cb = (JComboBox) e.getSource();
						cb.removePopupMenuListener(this);
						Container cbparent = cb.getParent();
						cbparent.remove(cb);
						cbparent.repaint();
					}

					public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
						JComboBox cb = (JComboBox) e.getSource();
						cb.repaint();
					}
					
				});
				
			} else if (e.getY() > comboBoxY && e.getY() < havingFieldY ) {
				System.out.println("its a field");
			}	
		}
		/**
		 * Returns the x position of the given a column index.
		 */
		public int getXPositionOnColumn(TableColumnModel model, int columnIndex){
			int sum = 0;
			for(int i = 0; i < columnIndex; i ++){
				sum += model.getColumn(i).getWidth();
			}
			return sum;
		}
	}
}
