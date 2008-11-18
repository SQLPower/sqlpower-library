/*
 * Copyright (c) 2008, SQL Power Group Inc.
 *
 * This file is part of Power*MatchMaker.
 *
 * Power*MatchMaker is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * Power*MatchMaker is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>. 
 */

package ca.sqlpower.swingui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.Panel;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import ca.sqlpower.swingui.DataEntryPanel;

/**
 * This class is a {@link DataEntryPanel} that asks the user to pick a font.
 */
public class FontSelector implements DataEntryPanel {

    /** The font the user has chosen */
    protected Font resultFont;

    /** The resulting font name */
    protected String resultName;

    /** The resulting font size */
    protected int resultSize;

    /** Indicates if resulting font is bold */
    protected boolean isBold;

    /** Indicates if resulting font is italic */
    protected boolean isItalic;

    /** Display text */
    protected String displayText = "Preview Font";

    /** The list of Fonts */
    protected String fontList[];

    /** The font name chooser */
    protected JList fontNameChoice;

    /** The font size chooser */
    protected JList fontSizeChoice;

    /** The bold and italic choosers */
    private JCheckBox bold, italic;

    /** The list of font sizes */
    protected String fontSizes[] = { "8", "10", "11", "12", "13", "14","15","16", "18",
            "20", "24", "30", "36", "40", "48", "60", "72" };

    /** The index of the default size (e.g., 14 point == 4) */
    protected static final int DEFAULT_SIZE = 4;
    
    /**
     * The "Apply button"
     */
    protected JButton okButton;

    /**
     * The display area. Use a JLabel as the AWT label doesn't always honor
     * setFont() in a timely fashion :-)
     */
    protected JLabel previewArea;

    private JPanel panel;

    private final Font originalFont;
    
    /**
     * This constructor will create the Font Selector given a parent frame and a
     * previous font.
     * @param f
     * @param font
     */
    public FontSelector(Font font) {
        panel = new JPanel();
        
        //if the font given happened to be null, make a new font from this JDialog
        if(font == null) {
            font = new Font(panel.getFont().getName(), panel.getFont().getStyle(), panel.getFont().getSize());
        }
        this.originalFont = font;

        Panel top = new Panel();
        top.setLayout(new FlowLayout());

        fontNameChoice = new JList();
        top.add(fontNameChoice);

        fontList = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getAvailableFontFamilyNames();

        fontNameChoice = new JList(fontList);
        JScrollPane fontPane;
        top.add(fontPane = new JScrollPane(fontNameChoice));
        JScrollPane sizePane;
        fontSizeChoice = new JList(fontSizes);
        top.add(sizePane = new JScrollPane(fontSizeChoice));
        
        if(findIndexOf(fontList, font.getName()) >=0) {
            fontNameChoice.setSelectedIndex(findIndexOf(fontList, font.getName()));
            fontPane.getViewport().scrollRectToVisible(fontNameChoice.getCellBounds(findIndexOf(fontList, font.getName())-1, findIndexOf(fontList, font.getName())-1));
        }
        else {
            fontNameChoice.setSelectedIndex(0);
        }
        fontNameChoice.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                previewFont();
            }
        });
        if(findIndexOf(fontSizes, String.valueOf(font.getSize())) >=0) {
            fontSizeChoice.setSelectedIndex(findIndexOf(fontSizes, String.valueOf(font.getSize())));
            sizePane.getViewport().scrollRectToVisible(fontSizeChoice.getCellBounds(findIndexOf(fontSizes, String.valueOf(font.getSize()))-1,findIndexOf(fontSizes, String.valueOf(font.getSize()))-1));
        }
        else {
            fontSizeChoice.setSelectedIndex(0);
        }
        fontSizeChoice.addListSelectionListener(new ListSelectionListener() {
        
            public void valueChanged(ListSelectionEvent e) {
                previewFont();
            }
        });

        panel.add(top, BorderLayout.NORTH);

        Panel attrs = new Panel();
        top.add(attrs);
        attrs.setLayout(new GridLayout(0, 1));
        attrs.add(bold = new JCheckBox("Bold", false));
        if(font.isBold()){
            bold.setSelected(true);
        }
        bold.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                previewFont();
            }
        });
        attrs.add(italic = new JCheckBox("Italic", false));
        if(font.isItalic()){
            italic.setSelected(true);
        }
        italic.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                previewFont();
            }
        });
        previewArea = new JLabel(displayText, JLabel.CENTER);
        previewArea.setSize(200, 50);
        panel.add(previewArea, BorderLayout.CENTER);

        previewFont(); // ensure view is up to date!
    }

    public JButton getApplyButton(){
        return this.okButton;
    }
    
    /**
     * This method will find the index of a String "s" in a string array "array".
     * @param array The array we will be doing the search on
     * @param s The string we are searching for.
     * @return Return the index of the string in the array if found, otherwise return "-1".
     */
    private int findIndexOf(String [] array, String s) {
        int i=0;
        for(String str : array){
            if(str.equals(s)) return i;
            i++;
        }
        return -1;
    }
 
    /**
     * Called from the action handlers to get the font info, build a font, and
     * set it.
     */
    protected void previewFont() {
        resultName = (String) fontNameChoice.getSelectedValue();
        String resultSizeName = (String) fontSizeChoice.getSelectedValue();
        int resultSize = Integer.parseInt(resultSizeName);
        isBold = bold.isSelected();
        isItalic = italic.isSelected();
        int attrs = Font.PLAIN;
        if (isBold)
            attrs = Font.BOLD;
        if (isItalic)
            attrs |= Font.ITALIC;
        resultFont = new Font(resultName, attrs, resultSize);
        previewArea.setFont(resultFont);
        panel.setSize(panel.getPreferredSize());
        panel.repaint();
    }

    /** Retrieve the selected font name. */
    public String getSelectedName() {
        return resultName;
    }

    /** Retrieve the selected size */
    public int getSelectedSize() {
        return resultSize;
    }

    /** Retrieve the selected font, or null */
    public Font getSelectedFont() {
        return resultFont;
    }
    
    public JPanel getPanel() {
        return panel;
    }

    public boolean applyChanges() {
        return true;
    }

    public void discardChanges() {
        resultFont = originalFont;
    }

    public boolean hasUnsavedChanges() {
        return false;
    }
}
