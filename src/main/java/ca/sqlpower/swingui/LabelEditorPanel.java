/*
 * Copyright (c) 2010, SQL Power Group Inc.
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

package ca.sqlpower.swingui;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

import javax.swing.Action;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.text.BadLocationException;

import org.apache.log4j.Logger;

import ca.sqlpower.object.HorizontalAlignment;
import ca.sqlpower.object.SPLabel;
import ca.sqlpower.object.SPVariableHelper;
import ca.sqlpower.object.VerticalAlignment;
import ca.sqlpower.swingui.object.InsertVariableAction;
import ca.sqlpower.swingui.object.VariableInserter;
import ca.sqlpower.swingui.object.VariableLabel;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

public abstract class LabelEditorPanel implements DataEntryPanel {

	private static final Logger logger = Logger
			.getLogger(LabelEditorPanel.class);
	
	private JPanel panel;

	private final SPLabel label;

	private FontSelector fontSelector;

	private JTextPane textArea;

	private JToggleButton leftAlign;
	private JToggleButton centreAlign;
	private JToggleButton rightAlign;

	private JToggleButton topAlign;
	private JToggleButton middleAlign;
	private JToggleButton bottomAlign;

	private JComboBox bgColor;
	private JComboBox fgColor;
	private JComboBox bdColor;
	
	private JCheckBox bgIsNull;
	private JCheckBox borderIsNull;
	
	public abstract FontSelector getFontSelector();
	public abstract SPVariableHelper getVariablesHelper();
	public abstract List<Color> getBackgroundColours();
	
	/**
	 * A constructor with the option to override the panel's initial selections
	 * (taken from the label's values) with presets. This is useful for newly
	 * created labels.
	 */
	public LabelEditorPanel(SPLabel label, boolean variables, boolean override) {
		this(label, variables);
		if (override) {
			bgIsNull.setSelected(false);
			borderIsNull.setSelected(false);
			bdColor.setSelectedIndex(0);
			bgColor.setSelectedIndex(0);
			fgColor.setSelectedIndex(0);
		}
	}
	
	public LabelEditorPanel(SPLabel label, boolean variables) {
		this.label = label;
		panel = new JPanel();
		final DefaultFormBuilder fb = new DefaultFormBuilder(new FormLayout
				("pref, 4dlu, pref, 4dlu, pref, 4dlu, pref, 4dlu, 100dlu:grow"), panel);
		fb.getLayout().setColumnGroups(new int[][] { {1, 5}, {3, 7} });
        
        textArea = new JTextPane();
        textArea.setText(label.getText());
        
        JButton variableButton = null;
        if (variables) {
        	VariableLabel.insertLabels(getVariablesHelper(), textArea.getDocument(), textArea);

        	Action insertVariableAction = new InsertVariableAction(
        			"Variables",
        			getVariablesHelper(),
        			null, 
        			new VariableInserter() {
        				public void insert(String variable) {
        					try {
        						VariableLabel.insertLabel(
        								variable.replaceFirst("\\$", "").replaceFirst("\\{", "").replaceFirst("\\}", ""), 
        								getVariablesHelper(), 
        								textArea.getDocument(), 
        								textArea.getCaretPosition(),
        								textArea);
        					} catch (BadLocationException e) {
        						throw new IllegalStateException(e);
        					}
        				}
        			}, 
        			textArea);

			variableButton = new JButton("Variables");
        	variableButton.setAction(insertVariableAction);

        	// Maps CTRL+SPACE to insert variable
        	textArea.getInputMap().put(
        			KeyStroke.getKeyStroke(
        					KeyEvent.VK_SPACE,
        					InputEvent.CTRL_MASK),
        	"insertVariable");
        	textArea.getActionMap().put(
        			"insertVariable", 
        			insertVariableAction);
        	
        }
        
        ButtonGroup hAlignmentGroup = new ButtonGroup();
        leftAlign = new JToggleButton(AlignmentIcons.LEFT_ALIGN_ICON, 
                label.getHorizontalAlignment() == HorizontalAlignment.LEFT);
        hAlignmentGroup.add(leftAlign);
        centreAlign = new JToggleButton(AlignmentIcons.CENTRE_ALIGN_ICON, 
                label.getHorizontalAlignment() == HorizontalAlignment.CENTER);
        hAlignmentGroup.add(centreAlign);
        rightAlign = new JToggleButton(AlignmentIcons.RIGHT_ALIGN_ICON, 
                label.getHorizontalAlignment() == HorizontalAlignment.RIGHT);
        hAlignmentGroup.add(rightAlign);

        ButtonGroup vAlignmentGroup = new ButtonGroup();
        topAlign = new JToggleButton(AlignmentIcons.TOP_ALIGN_ICON, 
                label.getVerticalAlignment() == VerticalAlignment.TOP);
        vAlignmentGroup.add(topAlign);
        middleAlign = new JToggleButton(AlignmentIcons.MIDDLE_ALIGN_ICON, 
                label.getVerticalAlignment() == VerticalAlignment.MIDDLE);
        vAlignmentGroup.add(middleAlign);
        bottomAlign = new JToggleButton(AlignmentIcons.BOTTOM_ALIGN_ICON, 
                label.getVerticalAlignment() == VerticalAlignment.BOTTOM);
        vAlignmentGroup.add(bottomAlign);

        Box alignmentBox = Box.createHorizontalBox();
        alignmentBox.add(leftAlign);
        alignmentBox.add(centreAlign);
        alignmentBox.add(rightAlign);
        alignmentBox.add(Box.createHorizontalStrut(5));
        alignmentBox.add(topAlign);
        alignmentBox.add(middleAlign);
        alignmentBox.add(bottomAlign);
        if (variables) {
        	alignmentBox.add(Box.createHorizontalGlue());
        	alignmentBox.add(variableButton);
        }
        
        fb.append("Alignment", alignmentBox,7);
        fb.appendRelatedComponentsGapRow();
        fb.appendRow("pref");
        fb.nextLine(2);
        
        bgColor = new JComboBox(ColourScheme.BACKGROUND_COLOURS);
        fb.append("Label Colour", bgColor);
        ColorCellRenderer renderer = new ColorCellRenderer(40, 20);
        bgColor.setRenderer(renderer);
        bgColor.setSelectedItem(label.getBackgroundColour());
        if (label.getBackgroundColour() == null) {
        	bgColor.setSelectedIndex(0);
        }
        bgColor.addActionListener(new ActionListener() {
    		public void actionPerformed(ActionEvent e) {
    			bgIsNull.setSelected(false);
    		}
    	});
        
        bgIsNull = new JCheckBox("Transparent",label.getBackgroundColour() == null);
        fb.append(bgIsNull);
        fb.appendRelatedComponentsGapRow();
        fb.appendRow("pref");
        fb.nextLine(2);
        
        fgColor = new JComboBox(ColourScheme.FOREGROUND_COLOURS);
        fb.append("Text Colour", fgColor);
        fgColor.setRenderer(renderer);
        fgColor.setSelectedItem(label.getForegroundColour());
        
        fb.appendRelatedComponentsGapRow();
        fb.appendRow("pref");
        fb.nextLine(2);
        
        bdColor = new JComboBox(ColourScheme.FOREGROUND_COLOURS);
        fb.append("Border Colour", bdColor);
        bdColor.setRenderer(renderer);
        bdColor.setSelectedItem(label.getBorderColour());
        if (label.getBorderColour() == null) {
        	bdColor.setSelectedIndex(0);
        }
        bdColor.addActionListener(new ActionListener() {
    		public void actionPerformed(ActionEvent e) {
    			borderIsNull.setSelected(false);
    		}
    	});
        
        borderIsNull = new JCheckBox("No Border",label.getBorderColour() == null);
        fb.append(borderIsNull);
        
        fb.appendRelatedComponentsGapRow();
        fb.appendRow("fill:90dlu:grow");
        fb.nextLine(2);
        
        textArea.setFont(label.getFont());
        JLabel textLabel = fb.append("Text", new JScrollPane(textArea),7);
        textLabel.setVerticalTextPosition(JLabel.TOP);
        fb.appendRow("pref");
        fb.nextLine();

        fontSelector = getFontSelector();
        if (fontSelector != null) {
        	logger.debug("FontSelector got passed Font " + label.getFont());
        	fontSelector.setShowingPreview(false);
        	fontSelector.addPropertyChangeListener(new PropertyChangeListener() {
        		public void propertyChange(PropertyChangeEvent evt) {
        			logger.debug("Changing font to: " + fontSelector.getSelectedFont());
        			textArea.setFont(fontSelector.getSelectedFont());
        		}
        	});
        	fb.append("Font", fontSelector.getPanel(),5);
        }


	}
	

	@Override
	public boolean applyChanges() {

		if (fontSelector != null) {
			fontSelector.applyChanges();
			label.setFont(fontSelector.getSelectedFont());
		}

        VariableLabel.removeLabels(textArea.getDocument());
        label.setText(textArea.getText());
        
        if (leftAlign.isSelected()) {
            label.setHorizontalAlignment(HorizontalAlignment.LEFT);
        } else if (centreAlign.isSelected()) {
            label.setHorizontalAlignment(HorizontalAlignment.CENTER);
        } else if (rightAlign.isSelected()) {
            label.setHorizontalAlignment(HorizontalAlignment.RIGHT);
        }

        if (topAlign.isSelected()) {
            label.setVerticalAlignment(VerticalAlignment.TOP);
        } else if (middleAlign.isSelected()) {
            label.setVerticalAlignment(VerticalAlignment.MIDDLE);
        } else if (bottomAlign.isSelected()) {
            label.setVerticalAlignment(VerticalAlignment.BOTTOM);
        }
        
        label.setBackgroundColour((Color) (bgIsNull.isSelected() ? 
        		null: bgColor.getSelectedItem()));
        label.setForegroundColour((Color) fgColor.getSelectedItem());
        label.setBorderColour((Color) (borderIsNull.isSelected() ? 
        		null: bdColor.getSelectedItem()));
        
        return true;
    }

	@Override
	public void discardChanges() {
		// no-op
	}

	@Override
	public JComponent getPanel() {
		return panel;
	}

	@Override
	public boolean hasUnsavedChanges() {
		return true;
	}
	
	public SPLabel getLabel() {
	    return label;
	}
	


}
