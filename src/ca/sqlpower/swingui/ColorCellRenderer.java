/*
 * Copyright (c) 2009, SQL Power Group Inc.
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
import java.awt.Component;
import java.awt.Dimension;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;

/**
 * Renders a rectangle of colour in a list cell with the given 
 * cell dimensions. The colour is determined by the list item 
 * value, which must be of type java.awt.Color.
 */
public class ColorCellRenderer extends DefaultListCellRenderer {
    
    private final int cellWidth;
    private final int cellHeight;
    
    public ColorCellRenderer(int cellWidth, int cellHeight) {
        this.cellWidth = cellWidth;
        this.cellHeight = cellHeight;
    }
    
    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
            boolean cellHasFocus) {
        super.getListCellRendererComponent(list, "", index, isSelected, cellHasFocus);
        if (value == null) {
            value = Color.BLACK;
        }
        setBackground((Color) value);
        setOpaque(true);
        setPreferredSize(new Dimension(cellWidth, cellHeight));
        setIcon(new ColorIcon(cellWidth, cellHeight, (Color) value));
        return this;
    }
}
