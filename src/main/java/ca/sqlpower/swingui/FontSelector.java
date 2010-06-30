/*
 * Copyright (c) 2008, SQL Power Group Inc.
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
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Arrays;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.apache.log4j.Logger;

import ca.sqlpower.dao.session.SPFontLoader;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

/**
 * This class is a {@link DataEntryPanel} that asks the user to pick a font.
 */
public class FontSelector implements DataEntryPanel {

    private static final Logger logger = Logger.getLogger(FontSelector.class);
    
    private static enum FontStyle {
        PLAIN("Plain", Font.PLAIN),
        BOLD("Bold", Font.BOLD),
        ITALIC("Italic", Font.ITALIC),
        BOLD_ITALIC("Bold Italic", Font.BOLD | Font.ITALIC);
        
        private final String humanName;
        private final int styleCode;

        FontStyle(String name, int styleCode) {
            humanName = name;
            this.styleCode = styleCode;
        }

        /**
         * Returns a font derived from the given font which has the style
         * specified by this enum constant.
         * 
         * @param f
         *            The original font
         * @return A new font with the same size and family as f, but with this
         *         enum constant's style.
         */
        public Font apply(Font f) {
            return f.deriveFont(styleCode);
        }
        
        @Override
        public String toString() {
            return humanName;
        }
        
        public static FontStyle forCode(int styleCode) {
            for (FontStyle sty : values()) {
                if (styleCode == sty.styleCode) {
                    return sty;
                }
            }
            throw new IllegalArgumentException("Unknown font style code: " + styleCode);
        }

        public int getStyleCode() {
            return styleCode;
        }
    }
    
    private PropertyChangeSupport pcs = new PropertyChangeSupport(this);
    
    /**
     * The font the user has chosen. This is updated whenever any of the
     * font choosing components fires a change event.
     */
    private Font selectedFont;

    /**
     * A list populated with all font names available on the local system.
     */
    private final JList fontNameList;

    /**
     * A list populated with a number of common font sizes the user might want
     * to pick. Choosing an item on this list fills in the value in the font
     * size text field.
     */
    private final JList fontSizeList;

    /**
     * The field that actually specifies the font size. Can be updated directly
     * to any positive integer value, or by clicking a preset value in the
     * {@link #fontSizeList}.
     */
    private final JSpinner fontSizeSpinner;
    
    /**
     * The font style chooser (bold, italic, bold italic, plain).
     */
    private final JList styleChoice;
    
    /** The list of font sizes */
    private static final Integer[] FONT_SIZES = {
            4, 5, 6, 7, 8, 10, 11, 12, 13, 14, 15, 16, 18,
            20, 24, 30, 36, 40, 48, 60, 72, 96, 144 };

    /**
     * The display area.
     */
    private final JTextArea previewArea = new JTextArea("The Quick Wabit Architected the Loaded Matchmaker");

    /**
     * The panel we return as this data entry panel's GUI.
     */
    private final JPanel panel;

    /**
     * The original font we started with. If this data entry panel is canceled,
     * the user's current choice will revert to this value.
     */
    private final Font originalFont;

	private final SPFontLoader fontLoader;
    
    private class SelectionHandler implements ChangeListener, ListSelectionListener {

        boolean updatingListSelection = false;
        
        public void stateChanged(ChangeEvent e) {
            if (updatingListSelection) return;
            try {
                updatingListSelection = true;
                Integer newSize = (Integer) fontSizeSpinner.getValue();
                int newSizeIndexInList = Arrays.binarySearch(FONT_SIZES, newSize);
                if (newSizeIndexInList >= 0) {
                    fontSizeList.setSelectedIndex(newSizeIndexInList);
                    fontSizeList.ensureIndexIsVisible(newSizeIndexInList);
                } else {
                    fontSizeList.clearSelection();
                }
            } finally {
                updatingListSelection = false;
            }
            previewFont();
        }

        public void valueChanged(ListSelectionEvent e) {
            previewFont();
        }
        
    }
    
    /**
     * This constructor will create the Font Selector given a parent frame and a
     * previous font.
     * 
     * @param font The font to start with in the preview dialog. If null, the system
     * default font will be used.
     */
    public FontSelector(Font font) {
    	this(
			font == null
				? Font.decode(null)
				: font, 
			GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames(),
			null);
    }
    
    public FontSelector(Font font, String[] fontList, SPFontLoader fontLoader) {
    	
    	this.fontLoader = fontLoader;
		if (font == null) {
    		if (fontList == null
    				|| fontList.length == 0)
    		{
    			throw new IllegalArgumentException("The fontList parameter requires at least one valid font.");
    		}
			font = Font.decode(fontList[0]);
			if (font == null) {
				throw new IllegalArgumentException("The fontList[0] element cannot be loaded.");
			}
    	}
    	
        logger.debug("Creating new font selector with given font: " + font);
        
        this.originalFont = font;
        
        SelectionHandler selectionHandler = new SelectionHandler();
        fontNameList = new JList(fontList);
        fontNameList.addListSelectionListener(selectionHandler);
        
        fontSizeSpinner = new JSpinner(new SpinnerNumberModel(font.getSize(), 1, 200, 1));
        fontSizeSpinner.addChangeListener(selectionHandler);
        
        fontSizeList = new JList(FONT_SIZES);
        fontSizeList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                if (fontSizeList.getSelectedValue() != null) {
                    fontSizeSpinner.setValue((Integer) fontSizeList.getSelectedValue());
                }
            }
        });
        
        styleChoice = new JList(FontStyle.values());
        styleChoice.setSelectedValue(FontStyle.forCode(font.getStyle()), true);
        styleChoice.addListSelectionListener(selectionHandler);
        
        FormLayout layout = new FormLayout(
                "pref:grow, 4dlu, pref, 4dlu, pref",
                "pref, 4dlu, pref, 4dlu, fill:pref:grow");
        layout.setHonorsVisibility(true);
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        CellConstraints cc = new CellConstraints();
        
        builder.add(new JScrollPane(fontNameList),  cc.xywh(1, 1, 1, 3));
        builder.add(fontSizeSpinner,                cc.xywh(3, 1, 1, 1));
        builder.add(new JScrollPane(fontSizeList),  cc.xywh(3, 3, 1, 1));
        builder.add(new JScrollPane(styleChoice),   cc.xywh(5, 1, 1, 3));
        
        previewArea.setBackground(Color.WHITE);
        previewArea.setPreferredSize(new Dimension(300, 100));
        builder.add(previewArea,                    cc.xywh(1, 5, 5, 1));

        // Set defaults after creating layout so the "scroll to visible" works
        fontSizeList.setSelectedValue(Integer.valueOf(font.getSize()), true);
        fontNameList.setSelectedValue(font.getFamily(), true);
        logger.debug("Set family list to \"" + font.getFamily() +
                "\" and size to " + Integer.valueOf(font.getSize()));
        
        panel = builder.getPanel();
        
        previewFont(); // ensure view is up to date!
    }

    /**
     * Updates the {@link #selectedFont} and the font in the preview area. This
     * method gets called from the change handlers installed on all the chooser
     * components.
     */
    private void previewFont() {
        String name = (String) fontNameList.getSelectedValue();
        FontStyle style = (FontStyle) styleChoice.getSelectedValue();
        int size = ((Integer) fontSizeSpinner.getValue()).intValue();
        
        if (fontLoader == null) {
        	setSelectedFont(new Font(name, style.getStyleCode(), size));
        } else {
        	Font font = fontLoader.loadFontFromName(name);
        	font = font.deriveFont(style.getStyleCode());
        	font = font.deriveFont(Float.valueOf(String.valueOf(size)));
        	setSelectedFont(font);
        }
    }

    /**
     * Returns the font the user has constructed through the choices in this
     * font selector.
     */
    public Font getSelectedFont() {
        return selectedFont;
    }
    
    public void setSelectedFont(Font selectedFont) {
        Font oldFont = this.selectedFont;
        this.selectedFont = selectedFont;
        pcs.firePropertyChange("selectedFont", oldFont, selectedFont);
        previewArea.setFont(selectedFont);
    }
    
    public JPanel getPanel() {
        return panel;
    }

    public boolean applyChanges() {
        return true;
    }

    /**
     * Reverts to the original font.
     */
    public void discardChanges() {
        setSelectedFont(originalFont);
    }

    public boolean hasUnsavedChanges() {
        return false;
    }
    
    /**
     * Sets the text that will appear in the preview area. If you do not
     * call this method, the font selector will show its catchy default
     * phrase.
     */
    public void setPreviewText(String text) {
        previewArea.setText(text);
    }
    
    /**
     * Returns the current preview text. Keep in mind the text is in an
     * editable text area, so the user could have modified the text by the
     * time you call this method, and the entered text could be multi-line.
     */
    public String getPreviewText() {
        return previewArea.getText();
    }
    
    public void setShowingPreview(boolean show) {
        previewArea.setVisible(show);
    }

    public boolean isShowingPreview() {
        return previewArea.isVisible();
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                final FontSelector fs = new FontSelector(Font.decode("Courier bold 20"));
                fs.addPropertyChangeListener(new PropertyChangeListener() {
                    public void propertyChange(PropertyChangeEvent evt) {
                        System.out.println("New font selection: " + fs.getSelectedFont());
                    }
                });
                JFrame dummyFrame = new JFrame();
                JDialog d = DataEntryPanelBuilder.createDataEntryPanelDialog(
                        fs, dummyFrame, "Font Selector Demo!", "Yeehaw");
                d.setModal(true);
                d.setVisible(true);
                System.out.println("Selected font: " + fs.getSelectedFont());
                d.dispose();
                dummyFrame.dispose();
            }
        });
    }

    /**
     * Adds a property change listener that will be notified every time
     * the selected font has changed. The property name is "selectedFont".
     */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(listener);
    }

    public PropertyChangeListener[] getPropertyChangeListeners() {
        return pcs.getPropertyChangeListeners();
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(listener);
    }
    
    
}
