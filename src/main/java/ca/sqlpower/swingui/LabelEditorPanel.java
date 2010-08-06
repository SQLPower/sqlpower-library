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

import java.awt.BorderLayout;
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

	private JComboBox colourCombo;
	
	public LabelEditorPanel(SPLabel label, boolean variables) {
		this.label = label;
		panel = new JPanel();
		final DefaultFormBuilder fb = new DefaultFormBuilder(new FormLayout("pref, 4dlu, 250dlu:grow"), panel);
        
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
        fb.append("Alignment", alignmentBox);

        fb.appendRelatedComponentsGapRow();
        fb.nextLine();
        
        fb.appendRow("fill:90dlu:grow");
        fb.nextLine();
        textArea.setFont(label.getFont());
        JLabel textLabel = fb.append("Text", new JScrollPane(textArea));
        textLabel.setVerticalTextPosition(JLabel.TOP);
        
        fb.nextLine();
        
        fontSelector = getFontSelector();
        
        logger.debug("FontSelector got passed Font " + label.getFont());
        fontSelector.setShowingPreview(false);
        fontSelector.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                logger.debug("Changing font to: " + fontSelector.getSelectedFont());
                textArea.setFont(fontSelector.getSelectedFont());
            }
        });
        fb.append("Font", fontSelector.getPanel());
        
        fb.nextLine();
        final JLabel colourLabel = new JLabel(" ");
        colourLabel.setBackground(label.getBackgroundColour());
        colourLabel.setOpaque(true);
        colourCombo = new JComboBox();
        colourCombo.setRenderer(new ColorCellRenderer(85, 30));
        for (Color bgColour : getBackgroundColours()) {
            colourCombo.addItem(bgColour);
        }
        
        colourCombo.setSelectedItem(label.getBackgroundColour());
        colourCombo.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Color colour = (Color) colourCombo.getSelectedItem();
                colourLabel.setBackground(colour);
            }
        });
        JPanel colourPanel = new JPanel(new BorderLayout());
        colourPanel.add(colourLabel, BorderLayout.CENTER);
        colourPanel.add(colourCombo, BorderLayout.EAST);
        fb.append("Background", colourPanel);
        
	}
	

	@Override
	public boolean applyChanges() {

        fontSelector.applyChanges();
        label.setFont(fontSelector.getSelectedFont());

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
        
        label.setBackgroundColour((Color) colourCombo.getSelectedItem());

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
	
	public abstract FontSelector getFontSelector();
	public abstract SPVariableHelper getVariablesHelper();
	public abstract List<Color> getBackgroundColours();

}
