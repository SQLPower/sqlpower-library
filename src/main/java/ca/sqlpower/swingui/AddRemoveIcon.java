/*
 * Originally by Jonathan Fuerth. Copyright for this copy assigned
 * to SQL Power on July 27, 2007.
 * 
 * Copyright (c) 2007, SQL Power
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in
 *       the documentation and/or other materials provided with the
 *       distribution.
 *     * Neither the name of Jonathan Fuerth nor the names of other
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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

/**
 * The AddRemoveIcon is a simple icon that draws a thick plus or minus sign,
 * usually for little "Add" and "Remove" buttons under a list component.
 */
public class AddRemoveIcon implements Icon {

    public static enum Type {
        ADD, REMOVE;
    }
    
    /**
     * This icon's type (add or remove).
     */
    private final Type type;
    
    /**
     * This icon's width and height in pixels.
     */
    private final int size = 8;
    
    /**
     * The width of a stroke (the horizontal and/or vertical line this
     * icon draws) in pixels.
     */
    private final float strokeWidth = 1.49f;
    
    public AddRemoveIcon(Type type) {
        this.type = type;
    }
    
    public int getIconHeight() {
        return size;
    }

    public int getIconWidth() {
        return size;
    }

    /**
     * Paints a "+" or "-" symbol, depending on this icon's type.
     */
    public void paintIcon(Component c, Graphics g, int x, int y) {
        float xf = x;
        float yf = y;
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(Color.BLACK);
        g2.setStroke(new BasicStroke(strokeWidth, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_BEVEL));
        Line2D horiz = new Line2D.Float(
                xf,      yf+(size/2f),
                xf+size, yf+(size/2f));
        g2.draw(horiz);
        if (type == Type.ADD) {
            Line2D vert = new Line2D.Float(
                    xf+(size/2f), yf,
                    xf+(size/2f), yf+size);
            g2.draw(vert);
        }
        g2.dispose();
    }

}
