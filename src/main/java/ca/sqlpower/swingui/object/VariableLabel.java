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
import javax.swing.text.Element;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

import ca.sqlpower.object.SPVariableHelper;
import ca.sqlpower.swingui.DataEntryPanelBuilder;



public class VariableLabel extends JLabel {
	
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
		return formatVariable(variableDef);
	}
	
	public void setVariableDef(String variableDef) {
		this.variableDef = variableDef;
		this.setText("  " + SPVariableHelper.getKey(VariableLabel.this.variableDef) + "  > ");
	}

	/**
	 * Helper method to insert and register a styled label in a AWT Document
	 * 
	 * @param variableDef
	 *            The variable unique key, including namespace, name and default
	 *            value, but excluding ${} markers.
	 * @param helper
	 *            The helper to use if this variable needs to be modified.
	 * @param target
	 *            The target {@link Document} into which to insert this
	 *            variable.
	 * @param insertPosition
	 *            The position at which to insert the variable.
	 * @param dialogOwner
	 *            The owner of the dialog that will be displayed if the
	 *            variable is edited.
	 * @throws BadLocationException
	 *             If the position supplied is not a valid position within the
	 *             target document.
	 */
	public static void insertLabel(
			final String variableDef, 
			final SPVariableHelper helper,
			final Document target,
			final int insertPosition,
			final Component dialogOwner) throws BadLocationException {
		insertLabel(variableDef, helper, target, insertPosition, dialogOwner, true);
	}

	/**
	 * Helper method to insert and register a styled label in a AWT Document
	 * 
	 * @param variableDef
	 *            The variable unique key, including namespace, name and default
	 *            value, but excluding ${} markers.
	 * @param helper
	 *            The helper to use if this variable needs to be modified.
	 * @param target
	 *            The target {@link Document} into which to insert this
	 *            variable.
	 * @param insertPosition
	 *            The position at which to insert the variable.
	 * @param dialogOwner
	 *            The owner of the dialog that will be displayed if the variable
	 *            is edited.
	 * @param showVariablesPanelOnClick
	 *            The determinant of whether to show a {@link VariablesPanel}
	 *            when the label is clicked on.
	 * @throws BadLocationException
	 *             If the position supplied is not a valid position within the
	 *             target document.
	 */
	public static void insertLabel(
			final String variableDef, 
			final SPVariableHelper helper,
			final Document target,
			final int insertPosition,
			final Component dialogOwner,
			boolean showVariablesPanelOnClick) throws BadLocationException 
	{
		final String uuid = java.util.UUID.randomUUID().toString();
		final SimpleAttributeSet varStyle = new SimpleAttributeSet();
		
		varStyle.addAttribute(VAR_VALUE, variableDef);
		varStyle.addAttribute(UUID, uuid);
		final VariableLabel labelToInsert = new VariableLabel(variableDef, helper);
		StyleConstants.setComponent(varStyle, labelToInsert);
		target.insertString(insertPosition, VARIABLE_CHAR, varStyle);
		
		if (showVariablesPanelOnClick) {
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
												if (getCharacterElement(target, i).getAttributes().isDefined(UUID)
														&& getCharacterElement(target, i).getAttributes().getAttribute(UUID).equals(uuid)
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
	}
	
	
	/**
	 * Takes a {@link Document} object and switches all the
	 * variables labels for textual representations of the variables.
	 * @param target
	 */
	public static void removeLabels(Document target) {
		try {
			for (int i = 0; i < target.getLength(); i++) {
				if (getCharacterElement(target, i).getAttributes().isDefined(VAR_VALUE)
						&& target.getText(i, 1).equals(VARIABLE_CHAR)) {
					String varValue = (String)getCharacterElement(target, i).getAttributes().getAttribute(VAR_VALUE);
					target.remove(i, 1);
					target.insertString(
							i, 
							formatVariable(varValue), 
							null);
				}
			}
		
		} catch (BadLocationException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Takes a {@link Document} object and removes all
	 * variables labels and returns the textual representation
	 * of the contents with variables represented as ${bla}.
	 * @param target The document to analyze
	 * @return The string contents.
	 */
	public static String getText(Document target) {
		try {
			StringBuffer sb = new StringBuffer("");
			for (int i = 0; i < target.getLength(); i++) {
				if (getCharacterElement(target, i).getAttributes().isDefined(VAR_VALUE)
						&& target.getText(i, 1).equals(VARIABLE_CHAR)) {
					String varValue = (String)getCharacterElement(target, i).getAttributes().getAttribute(VAR_VALUE);
					sb.append(formatVariable(varValue));
				} else {
					sb.append(target.getText(i, 1));
				}
			}
			return sb.toString();
		} catch (BadLocationException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * This helper method parses the contents of an AWT Document and inserts
	 * pretty variables labels in it.
	 * 
	 * @param helper
	 *            The var helper to use if the variables are modified after
	 *            being inserted.
	 * @param target
	 *            The Document into which to perform the substitution
	 * @param dialogOwner
	 *            The owner of the dialog that will be displayed if the
	 *            variables are edited.
	 */
	public static void insertLabels(
			SPVariableHelper helper,
			Document target,
			Component dialogOwner) {
		insertLabels(helper, target, dialogOwner, true);
	}

	/**
	 * This helper method parses the contents of an AWT Document and inserts
	 * pretty variables labels in it.
	 * 
	 * @param helper
	 *            The var helper to use if the variables are modified after
	 *            being inserted.
	 * @param target
	 *            The Document into which to perform the substitution
	 * @param dialogOwner
	 *            The owner of the dialog that will be displayed if the
	 *            variables are edited.
	 * @param showVariablesPanelOnClick
	 *            The determinant of whether to show a {@link VariablesPanel}
	 *            when the labels are clicked on.
	 */
	public static void insertLabels(
			SPVariableHelper helper,
			Document target,
			Component dialogOwner,
			boolean showVariablesPanelOnClick)
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
				
				insertLabel(var, helper, target, indexStart, dialogOwner, showVariablesPanelOnClick);
			}

		} catch (BadLocationException e) {
			throw new AssertionError(e);
		}
	}
	
	
	
	/**
	 * Helper method to insert and register a styled label in
	 * a AWT Document that will be rendered by the Picollo library.
	 * @param variableDef The variable unique key, including namespace, 
	 * name and default value, but exclusing ${} markers.
	 * @param target The terget {@link Document} into which to insert
	 * this variable.
	 * @param insertPosition The position at whch to insert the variable.
	 * @throws BadLocationException If the position supplied is not a valid
	 * position within the target document.
	 */
	public static void insertLabelForPicollo(
			final String variableDef, 
			final Document target,
			final int insertPosition) throws BadLocationException 
	{
		final SimpleAttributeSet varStyle = new SimpleAttributeSet();
		
		// These attributes will help us convert to/from the nice label format
		varStyle.addAttribute(VAR_VALUE, variableDef);
		
		// Now we add attributes to make picollo display the variables in a nice way
		StyleConstants.setBackground(varStyle, Color.BLUE);
		StyleConstants.setForeground(varStyle, Color.WHITE);
		StyleConstants.setBold(varStyle, true);
		
		target.insertString(insertPosition, " " + SPVariableHelper.getKey(variableDef) + " ", varStyle);
	}
	
	/**
	 * This helper method parses the contents of an AWT Document
	 * which will be rendered by the Picollo library
	 * and inserts pretty variables labels in it.
	 * @param target The Document into which to perform the
	 * substitution
	 */
	public static void insertLabelsForPicollo(Document target)
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
	 * Takes a {@link Document} object who was crafted to be displayed
	 * by the Picollo library and switches all the
	 * variables labels for textual representations of the variables.
	 * @param target
	 */
	public static void removeLabelsForPicollo(Document target) {
		try {
			for (int i = 0; i < target.getLength(); i++) {
				if (getCharacterElement(target, i).getAttributes().isDefined(VAR_VALUE)) {
					
					// We must figure out the end of the var string.
					int endIndex = i;
					while (endIndex < target.getLength()
							&& getCharacterElement(target, endIndex).getAttributes().isDefined(VAR_VALUE)) {
						endIndex++;
					}
					
					String varValue = (String)getCharacterElement(target, i).getAttributes().getAttribute(VAR_VALUE);
					target.remove(i, endIndex - i);
					target.insertString(
							i, 
							formatVariable(varValue), 
							null);
				}
			}
		
		} catch (BadLocationException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Formats a variable name into a form where the variable looks unresolved.
	 * For example, var -> ${var}. This method works in reverse of
	 * {@link #stripVariable(String)}.
	 * 
	 * @param variableName
	 *            The variable name.
	 * @return The formatted unresolved variable.
	 */
	public static String formatVariable(String variableName) {
		return "${" + variableName + "}";
	}

	/**
	 * Strips the characters from an unresolved variable to get the variable
	 * name from it. For example, ${var} -> var. This method works in reverse of
	 * {@link #formatVariable(String)}.
	 * 
	 * @param formattedVariableName
	 *            The unresolved variable.
	 * @return The stripped variable name.
	 */
	public static String stripVariable(String formattedVariableName) {
		int indexStart = formattedVariableName.indexOf("${");
		int indexEnd = formattedVariableName.indexOf("}");
		
		if (indexStart == -1 || indexEnd == -1 || indexStart > indexEnd) {
			return formattedVariableName;
		}
		
		String var = formattedVariableName.substring(indexStart, indexEnd + 1);
		return var.replaceFirst("\\$", "").replaceFirst("\\{", "").replaceFirst("\\}", "");
	}
	
	public static Element getCharacterElement(Document doc, int pos) {
		Element e = null;
		for (e = doc.getDefaultRootElement(); ! e.isLeaf(); ) {
		    int index = e.getElementIndex(pos);
		    e = e.getElement(index);
		}
		return e;
    }
}