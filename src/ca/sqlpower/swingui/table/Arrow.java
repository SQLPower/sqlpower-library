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

/**
 * 
 */
package ca.sqlpower.swingui.table;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;

import javax.swing.Icon;

/**
 * This class creates a grey arrow that can point up or down. Multiple arrows
 * can be created with a different priority where the greater the priority the
 * smaller the arrow.
 */
public class Arrow implements Icon {
    
    /**
     * If true the arrow will point downwards. If false the arrow will point up.
     */
    private boolean descending;
    
    /**
     * The width and height of the arrow.
     */
    private int size;
    
    /**
     * The priority of the arrow, used if multiple arrows exist. If there are
     * multiple arrows to describe the sorting of multiple values at the same
     * time the first arrow will be of the given size while each following 
     * arrow in the priority will be slightly smaller.
     */
    private int priority;
    
    /**
     * A shift in the x position for the arrow. If the arrow is to be rendered
     * at the origin of the given graphics then this can be set to 0. Otherwise
     * a non-zero value stored here will shift the arrow left or right from the
     * origin when drawn.
     */
    private final int xShift;
    
    /**
     * A shift in the y position for the arrow. If the arrow is to be rendered
     * at the origin of the given graphics then this can be set to 0. Otherwise
     * a non-zero value stored here will shift the arrow up or down from the
     * origin when drawn.
     */
    private final int yShift;
    
    public Arrow(boolean descending, int size, int priority, int xShift, int yShift) {
        this.descending = descending;
        this.size = size;
        this.priority = priority;
        this.xShift = xShift;
        this.yShift = yShift;
    }

    public Arrow(boolean descending, int size, int priority) {
        this(descending, size, priority, 0, 0);
    }

    public void paintIcon(Component c, Graphics g, int x, int y) {
    	Color color = c == null ? Color.GRAY : c.getBackground();
    	// In a compound sort, make each succesive triangle 20%
    	// smaller than the previous one.
    	int dx = (int)(size/2*Math.pow(0.8, priority));
    	int dy = descending ? dx : -dx;
    	// Align icon (roughly) with font baseline.
    	y = y + 5*size/6 + (descending ? -dy : 0);
    	int shift = descending ? 1 : -1;
    	g.translate(x + xShift, y + yShift);
    	g.setColor(color.darker());
    	
    	//Create the Triangle
    	g.fillPolygon(new int[]{dx, 0, dx/2}, new int[]{0, 0, dy+ shift}, 3);
    	g.setColor(color);
    	g.translate(-x - xShift, -y - yShift);
    }

    public int getIconWidth() {
        return size;
    }

    public int getIconHeight() {
        return size;
    }
}