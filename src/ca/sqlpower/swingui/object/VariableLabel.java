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
import java.awt.Component;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import ca.sqlpower.object.SPVariableHelper;
import ca.sqlpower.swingui.DataEntryPanelBuilder;



public class VariableLabel extends JLabel {
	
	public final static String STYLE_NAME = "sqlp-variable-style";
	public final static String VAR_VALUE = "sqlp-variable-value";
	public final static String UUID = "sqlp-variable-uuid";
	public final static String VARIABLE_CHAR = "\u0001";
	
	
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
	
	public void setVariableDef(String variableDef) {
		this.variableDef = variableDef;
		this.setText("  " + SPVariableHelper.getKey(VariableLabel.this.variableDef) + "  > ");
	}
	
	/**
	 * Helper method to insert and register a styled label in
	 * a AWT StyledDocument
	 * @param variableDef The variable unique key, including namespace, 
	 * name and default value, but exclusing ${} markers.
	 * @param helper The helper to use if this variable needs to be modified.
	 * @param target The terget {@link StyledDocument} into which to insert
	 * this variable.
	 * @param insertPosition The position at whch to insert the variable.
	 * @throws BadLocationException If the position supplied is not a valid
	 * position within the target document.
	 */
	public static void insertLabel(
			final String variableDef, 
			final SPVariableHelper helper,
			final StyledDocument target,
			final int insertPosition,
			final Component dialogOwner) throws BadLocationException 
	{
		final String uuid = java.util.UUID.randomUUID().toString();
		final Style varStyle = target.addStyle(STYLE_NAME, null);
		
		varStyle.addAttribute(VAR_VALUE, variableDef);
		varStyle.addAttribute(UUID, uuid);
		final VariableLabel labelToInsert = new VariableLabel(variableDef, helper);
		StyleConstants.setComponent(varStyle, labelToInsert);
		target.insertString(insertPosition, VARIABLE_CHAR, varStyle);
		
		// Open the editor if the user clicks on the label
		labelToInsert.addMouseListener(new MouseListener() {
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
									// We don't know where the var is now, but we can search it
									// by UUID attribute.
									for (int i = 0; i < target.getLength(); i++) {
										try {
											if (target.getCharacterElement(i).getAttributes().isDefined(UUID)
													&& target.getCharacterElement(i).getAttributes().getAttribute(UUID).equals(uuid)
													&& target.getText(i, 1).equals(VARIABLE_CHAR)) {
												// Gotcha!
												target.remove(i, 1);
												String newVariableDef = variable.replaceFirst("\\$", "").replaceFirst("\\{", "").replaceFirst("\\}", "");
												insertLabel(newVariableDef, helper, target, i, dialogOwner);
											}
										} catch (BadLocationException e) {
											throw new AssertionError(e);
										}
									}
								}
							},
							variableDef);
				
				JDialog dialog = 
					DataEntryPanelBuilder.createDataEntryPanelDialog(
						vp,
				        dialogOwner, 
				        "Edit variable", 
				        "Update");
				
				dialog.setVisible(true);
			}
		});
	}
	
	
	/**
	 * Takes a {@link StyledDocument} object and switches all the
	 * variables labels for textual representations of the variables.
	 * @param target
	 */
	public static void removeLabels(StyledDocument target) {
		try {
			for (int i = 0; i < target.getLength(); i++) {
				if (target.getCharacterElement(i).getAttributes().isDefined(VAR_VALUE)
						&& target.getText(i, 1).equals(VARIABLE_CHAR)) {
					String varValue = (String)target.getCharacterElement(i).getAttributes().getAttribute(VAR_VALUE);
					target.remove(i, 1);
					target.insertString(
							i, 
							"${"+varValue+"}", 
							null);
				}
			}
		
		} catch (BadLocationException e) {
			throw new RuntimeException(e);
		}
	}

	
	/**
	 * This helper method parses the contents of an AWT StyledDocument
	 * and inserts pretty variables labels in it.
	 * @param helper The var helper to use if the variables are
	 * modified after being inserted.
	 * @param target The StyledDocument into which to perform the
	 * substitution
	 * @param dialogOwner The owner of the dialog that will be displayed 
	 * if the variables are edited.
	 */
	public static void insertLabels(
			SPVariableHelper helper,
			StyledDocument target,
			Component dialogOwner)
	{
		try {
			
			while (true) 
			{
				String text = target.getText(0, target.getLength());
				int indexStart = text.indexOf("${");
				int indexEnd = text.indexOf("}");
				
				if (indexStart == -1 || indexEnd == -1) {
					break;
				}
				
				String var = text.substring(indexStart, indexEnd + 1);
				var = var.replaceFirst("\\$", "").replaceFirst("\\{", "").replaceFirst("\\}", "");
				
				target.remove(indexStart, indexEnd - indexStart + 1);
				
				insertLabel(var, helper, target, indexStart, dialogOwner);
			}

		} catch (BadLocationException e) {
			throw new AssertionError(e);
		}
	}
	
	
	
	/**
	 * Helper method to insert and register a styled label in
	 * a AWT StyledDocument that will be rendered by the Picollo library.
	 * @param variableDef The variable unique key, including namespace, 
	 * name and default value, but exclusing ${} markers.
	 * @param target The terget {@link StyledDocument} into which to insert
	 * this variable.
	 * @param insertPosition The position at whch to insert the variable.
	 * @throws BadLocationException If the position supplied is not a valid
	 * position within the target document.
	 */
	public static void insertLabelForPicollo(
			final String variableDef, 
			final StyledDocument target,
			final int insertPosition) throws BadLocationException 
	{
		final Style varStyle = target.addStyle(STYLE_NAME, null);
		
		// These attributes will help us convert to/from the nice label format
		varStyle.addAttribute(VAR_VALUE, variableDef);
		
		// Now we add attributes to make picollo display the variables in a nice way
		StyleConstants.setBackground(varStyle, Color.BLUE);
		StyleConstants.setForeground(varStyle, Color.WHITE);
		StyleConstants.setBold(varStyle, true);
		
		target.insertString(insertPosition, " " + SPVariableHelper.getKey(variableDef) + " ", varStyle);
	}
	
	/**
	 * This helper method parses the contents of an AWT StyledDocument
	 * which will be rendered by the Picollo library
	 * and inserts pretty variables labels in it.
	 * @param target The StyledDocument into which to perform the
	 * substitution
	 */
	public static void insertLabelsForPicollo(StyledDocument target)
	{
		try {
			
			while (true) 
			{
				String text = target.getText(0, target.getLength());
				int indexStart = text.indexOf("${");
				int indexEnd = text.indexOf("}");
				
				if (indexStart == -1 || indexEnd == -1) {
					break;
				}
				
				String var = text.substring(indexStart, indexEnd + 1);
				var = var.replaceFirst("\\$", "").replaceFirst("\\{", "").replaceFirst("\\}", "");
				
				target.remove(indexStart, indexEnd - indexStart + 1);
				
				insertLabelForPicollo(var, target, indexStart);
			}

		} catch (BadLocationException e) {
			throw new AssertionError(e);
		}
	}
	
	/**
	 * Takes a {@link StyledDocument} object who was crafted to be displayed
	 * by the Picollo library and switches all the
	 * variables labels for textual representations of the variables.
	 * @param target
	 */
	public static void removeLabelsForPicollo(StyledDocument target) {
		try {
			for (int i = 0; i < target.getLength(); i++) {
				if (target.getCharacterElement(i).getAttributes().isDefined(VAR_VALUE)) {
					
					// We must figure out the end of the var string.
					int endIndex = i;
					while (endIndex < target.getLength()
							&& target.getCharacterElement(endIndex).getAttributes().isDefined(VAR_VALUE)) {
						endIndex++;
					}
					
					String varValue = (String)target.getCharacterElement(i).getAttributes().getAttribute(VAR_VALUE);
					target.remove(i, endIndex - i);
					target.insertString(
							i, 
							"${"+varValue+"}", 
							null);
				}
			}
		
		} catch (BadLocationException e) {
			throw new RuntimeException(e);
		}
	}
}