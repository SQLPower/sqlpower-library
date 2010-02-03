/*
 * Copyright (c) 2010, SQL Power Group Inc.
 *
 * This file is part of Wabit.
 *
 * Wabit is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * Wabit is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>. 
 */


package ca.sqlpower.swingui.object;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;

import ca.sqlpower.object.SPVariableHelper;
import ca.sqlpower.swingui.DataEntryPanelBuilder;



public class VariableLabel extends JLabel {
	
	public static String STYLE_NAME = "sqlp-variable-style";
	
	private final static int MARGIN = 10;

	private String variableDef;

	/**
	 * Builds a label object that represents a variable
	 * in a swing {@link Document} object.
	 * @param variableDef The variable key, including namespace and 
	 * default value, but excluding the "${}" characters.
	 */
	public VariableLabel(
			final String variableDef, 
			final SPVariableHelper helper) {
		
		super("  " + SPVariableHelper.getKey(variableDef) + "  > ");
		super.setForeground(super.getBackground());
		this.variableDef = variableDef;
		setAlignmentY(BOTTOM);
		setHorizontalAlignment(JLabel.CENTER);
		
		// Open the editor if the user clicks on the label
		super.addMouseListener(new MouseListener() {
			public void mouseReleased(MouseEvent e) {
			}
			public void mousePressed(MouseEvent e) {
			}
			public void mouseExited(MouseEvent e) {
			}
			public void mouseEntered(MouseEvent e) {
			}
			public void mouseClicked(MouseEvent e) 
			{
				VariablesPanel vp = 
					new VariablesPanel(
							helper,
							new VariableInserter() {
								public void insert(String variable) {
									VariableLabel.this.variableDef = variable;
									VariableLabel.this.setText(variable);
									VariableLabel.this.getParent().repaint();
								}
							});
				
				JDialog dialog = 
					DataEntryPanelBuilder.createDataEntryPanelDialog(
						vp,
				        VariableLabel.this.getParent(), 
				        "Edit variable", 
				        "Update");
				
				dialog.setVisible(true);
			}
		});
	}
	
	@Override
	public void paint(Graphics g) {
		Color colorBackup = g.getColor();
		g.setColor(Color.BLUE);
		g.fillRoundRect( 0, 0, getWidth(), getHeight(), MARGIN, MARGIN );
		g.setColor(Color.WHITE);
		g.drawString("  " + SPVariableHelper.getKey(variableDef) + "  > ", 0, getHeight() - 4);
		g.setColor(colorBackup);
	}
	
	@Override
	public String toString() {
		return "${"+this.variableDef+"}";
	}
	
	/**
	 * Helper method to insert and register a styled label in
	 * a document
	 * @param variableDef The variable unique key, including namespace, 
	 * name and default value, but exclusing ${} markers.
	 * @param helper The helper to use if this variable needs to be modified.
	 * @param target The terget {@link StyledDocument} into which to insert
	 * this variable.
	 * @param insertPosition The position at whch to insert the variable.
	 * @throws BadLocationException If the position supplied is not a valid
	 * position within the target document.
	 */
	public static void insertVariable(
			String variableDef, 
			SPVariableHelper helper,
			StyledDocument target,
			int insertPosition) throws BadLocationException 
	{
		Style varStyle = target.addStyle(STYLE_NAME, target.getStyle(StyleContext.DEFAULT_STYLE));
		StyleConstants.setComponent(varStyle, new VariableLabel(variableDef, helper));
		target.insertString(insertPosition, "e", varStyle);
	}
}