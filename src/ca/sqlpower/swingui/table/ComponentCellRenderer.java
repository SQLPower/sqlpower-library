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
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;

import ca.sqlpower.sql.DataSourceCollection;
import ca.sqlpower.sql.PlDotIni;
import ca.sqlpower.sql.SPDataSource;
import ca.sqlpower.sql.SQLGroupFunction;


/* A renderer that extends JPanel.This renderer will add a JComboBox 
 * and a JtextArea to the JHeader of he resultTable for
 * Group By and Having filters.  
 */

public class ComponentCellRenderer extends JPanel implements TableCellRenderer {

	private static final Logger logger = Logger.getLogger(ComponentCellRenderer.class);

	private final TableCellRenderer renderer;
	private final JTableHeader tableHeader;
	private int labelHeight;
	private int comboBoxHeight;
	private int havingFieldHeight;
	private ArrayList<JComboBox> comboBoxes;
	private ArrayList<JTextField> textFields;
	private boolean groupingEnabled;

	public ComponentCellRenderer(JTable t) {
		tableHeader = t.getTableHeader();
		renderer = t.getTableHeader().getDefaultRenderer();
		tableHeader.addMouseListener(new HeaderMouseListener());
		groupingEnabled = false;
		comboBoxes = new ArrayList<JComboBox>();
		Vector<String> comboBoxItems = new Vector<String>();
		Object[] tempGroupItems =SQLGroupFunction.values();
		comboBoxItems.add("(GROUP BY)");

		for(Object item : tempGroupItems) {
			comboBoxItems.add(item.toString());
		}

		textFields = new ArrayList<JTextField>();
		for(int i = 0 ; i < t.getColumnCount(); i++) {
			JTextField textField = new JTextField();
			JComboBox comboBox = new JComboBox(comboBoxItems);
			comboBoxes.add(comboBox);
			textFields.add(textField);

			if(i == 0) {
				// takes the first ComboBoxes and TextField's height
				comboBoxHeight = comboBoxes.get(0).getPreferredSize().height;
				havingFieldHeight = textFields.get(0).getPreferredSize().height;
			}
		}
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
			if(!groupingEnabled) {
				add((JLabel)c, BorderLayout.NORTH);
				labelHeight = c.getPreferredSize().height;			
			} else {
				add((JLabel)c, BorderLayout.NORTH);
				int modelIndex = table.getColumnModel().getColumn(column).getModelIndex();
				add(new JComboBox(new Object[] { comboBoxes.get(modelIndex).getSelectedItem() }), BorderLayout.CENTER);
				add(new JTextField(textFields.get(modelIndex).getText()), BorderLayout.SOUTH);

				// we need to consistently set the size of the TextField in case they resize while its focused
				textFields.get(modelIndex).setBounds(getXPositionOnColumn(table.getColumnModel(),column), labelHeight + comboBoxHeight, 
						table.getColumnModel().getColumn(column).getWidth(), 
						textFields.get(column).getSize().height);

				labelHeight = c.getPreferredSize().height;
				logger.debug("Provided cell renderer for viewIndex="+column+" modelIndex="+modelIndex);
			}
		}
		return this;
	}
	/**
	 * This MouseListener handles clicking on the TableHeader. It will check the position of the click o determine its click
	 * as well as what component is being clicked.
	 */
	private class HeaderMouseListener extends MouseAdapter {

		/*
		 * The JTextField should lose its focus when dragging so i can be set to invisible
		 */
		public void mousePressed(MouseEvent e) {

			int labelY = labelHeight;
			int comboBoxY = labelHeight + comboBoxHeight;
			int havingFieldY =   labelHeight + comboBoxHeight + havingFieldHeight;
			JTableHeader h = (JTableHeader) e.getSource();
			TableColumnModel columnModel = h.getColumnModel();
			int viewIndex = columnModel.getColumnIndexAtX(e.getX());

			logger.debug("viewIndex is:" + viewIndex);

			if ( viewIndex < 0) {
				return;    			
			}

			int modelIndex = columnModel.getColumn(viewIndex).getModelIndex();
			logger.debug("modelIndex is:" + modelIndex);

			// when click anything other than TextField
			if( e.getY() < comboBoxY) {
				//Disable Focus on textField if it presses anywhere else on the header.
				textFields.get(modelIndex).setFocusable(false);
			}

			// when click comboBox
			if(e.getY() > labelHeight && e.getY() < comboBoxY) {

				TableColumn tc = columnModel.getColumn(viewIndex);

				// add a bufferZone So we can resize column and now have the comboBox showing
				if(e.getX()-getXPositionOnColumn(columnModel, viewIndex) < 3 || 
						(getXPositionOnColumn(columnModel, viewIndex) + tc.getWidth()) -e.getX() < 3) {
					return;
				}
				JComboBox tempCB = comboBoxes.get(modelIndex);
				h.add(tempCB);
				tempCB.setBounds(getXPositionOnColumn(columnModel, viewIndex),labelY, tc.getWidth(), comboBoxHeight);
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

			}
			// when click text Field
			else if (e.getY() > comboBoxY && e.getY() < havingFieldY ) {

				if(!textFields.get(modelIndex).isEnabled()) {
					return;
				}
				// reEnable the TextField if they clicked on the TextFieldArea
				textFields.get(modelIndex).setFocusable(true);

				JTextField tempTextField = textFields.get(modelIndex);
				h.add(tempTextField);

				if (!tempTextField.isVisible()) {
					throw new IllegalStateException("tempTextField was not visible");
				}
				TableColumn tc = columnModel.getColumn(viewIndex);
				tempTextField.setBounds(getXPositionOnColumn(columnModel, viewIndex), comboBoxY, tc.getWidth(), havingFieldHeight);
				tempTextField.requestFocus();
				logger.debug("Temporarily placing TextField at " + tempTextField.getBounds());
				tempTextField.addFocusListener(new FocusListener() {

					public void focusGained(FocusEvent e) {
						JTextField tf = (JTextField)e.getSource();
						Container tfparent = tf.getParent();
						tfparent.repaint();
					}
					public void focusLost(FocusEvent e) {
						JTextField tf = (JTextField)e.getSource();
						Container tfparent = tf.getParent();

						logger.debug("child is" + tf);
						logger.debug("parent is"+ tfparent);

						if (tfparent != null) {
							tfparent.remove(tf);
							tfparent.repaint();
						}
					}});	
			}	
		}
	}

	/**
	 * Returns the x position of the given a column index.
	 */
	public int getXPositionOnColumn(TableColumnModel model, int columnIndex) {
		int sum = 0;
		for(int i = 0; i < columnIndex; i ++) {
			sum += model.getColumn(i).getWidth();
		}
		return sum;
	}

	public ArrayList<JComboBox> getComboBoxes () {
		return comboBoxes;
	}

	public ArrayList<JTextField> getTextFields () {
		return textFields;
	}

	public void setGroupingEnabled(boolean flag) {
		
		groupingEnabled = flag;
		
		if (groupingEnabled) {
			tableHeader.getParent().setPreferredSize(new Dimension(
					tableHeader.getParent().getPreferredSize().width,
					labelHeight +comboBoxHeight+ havingFieldHeight));			
		} else {
			tableHeader.getParent().setPreferredSize(new Dimension(
					tableHeader.getParent().getPreferredSize().width, labelHeight));	
		}
		tableHeader.revalidate();
	}

	/**
	 * Just for testing and maybe a quick demo of the way to use this
	 * thingy.
	 */
	public static void main(String[] args) {
		Logger.getRootLogger().addAppender(new ConsoleAppender(new SimpleLayout(), "System.out"));
		logger.setLevel(Level.DEBUG);
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				try {
					DataSourceCollection dscol = new PlDotIni();
					dscol.read(new File(System.getProperty("user.home"), "pl.ini"));
					SPDataSource ds = dscol.getDataSource("thomas on random");
					Connection con = ds.createConnection();
					Statement stmt = con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
					ResultSet rs = stmt.executeQuery("select 'a' as a, 'b' as b, 'c' as c, 'd' as d, 'e' as snuffy");
					ResultSetTableModel tm = new ResultSetTableModel(rs);
					final JTable t = new JTable(tm);
					ComponentCellRenderer headerRenderer = new ComponentCellRenderer(t);
					t.getTableHeader().setDefaultRenderer(headerRenderer);
					JFrame f = new JFrame("Cows moo");
					JPanel panel = new JPanel(new BorderLayout());
					panel.add(new JScrollPane(t), BorderLayout.CENTER);
					panel.add(new JCheckBox(new AbstractAction() {

						public void actionPerformed(ActionEvent e) {
							JCheckBox checkBox = (JCheckBox)e.getSource();
							if(checkBox.isSelected()){
								ComponentCellRenderer renderPanel = (ComponentCellRenderer) t.getTableHeader().getDefaultRenderer();
								renderPanel.setGroupingEnabled(true);
							} else {
								ComponentCellRenderer renderPanel = (ComponentCellRenderer) t.getTableHeader().getDefaultRenderer();
								renderPanel.setGroupingEnabled(false);
							}

						}}), BorderLayout.NORTH);
					f.setContentPane(new JScrollPane(panel));
					f.pack();
					f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
					f.setVisible(true);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		});
	}
}
