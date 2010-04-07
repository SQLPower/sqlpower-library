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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Line2D;

import javax.swing.Icon;

public class LessthanGreaterthanIcon implements Icon {

	public static enum Type {
		LESSTHAN, GREATERTHAN
	}
	
	private final Type type;
	private final float size;
	private final float strokeWidth;
	
	public LessthanGreaterthanIcon(Type type) {
		this.type = type;
		this.size = 8;
		this.strokeWidth = 1.49f;
	}
	
	public int getIconHeight() {
        return (int) size;
    }

    public int getIconWidth() {
        return (int) size;
    }

    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2d = (Graphics2D) g.create();
        
        // Set up graphics
        g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setStroke(new BasicStroke(strokeWidth, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_BEVEL));
        g2d.setColor(Color.BLACK);
        
        Line2D top;
        Line2D bottom;
        
        float xf = (float) x;
        float yf = (float) y;
        
        if (type == Type.LESSTHAN) {
        	// Draw '<'
        	top = new Line2D.Float(xf, yf + (size/2), xf + size, yf);
        	bottom = new Line2D.Float(xf, yf + (size/2), xf + size, yf + size);
        } else {
        	// Draw '>'
        	top = new Line2D.Float(xf, yf, xf + size, yf + (size/2));
        	bottom = new Line2D.Float(xf, yf + size, xf + size, yf + (size/2));
        }
        
        g2d.draw(top);
        g2d.draw(bottom);
        g2d.dispose();
    }
}
