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

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import javax.swing.Icon;

/**
 * Applies a colour tint over the given icon when painted.
 */
public class ColoredIcon implements Icon {

    private Icon sourceIcon;
    private Color tint;
    private float alpha;

	/**
	 * Creates a new Icon using the given source {@link Icon} and applies the
	 * given {@link Color} as a tint with an alpha
	 * 
	 * @param source
	 *            The source {@link Icon} to apply the tint to
	 * @param tint
	 *            The {@link Color} with which to apply as a tint to the source
	 *            icon
	 * @param alpha
	 *            The constant alpha to be multiplied by the alpha of the source
	 *            Icon when applying the color tint. This is expected to be a
	 *            float value between 0.0 and 1.0. Any other value may cause an
	 *            {@link IllegalArgumentException}.
	 * 
	 */
    public ColoredIcon(Icon source, Color tint, float alpha) {
        this.sourceIcon = source;
        this.tint = tint;
        this.alpha = alpha;
    }

	/**
	 * Creates a new Icon using the given source {@link Icon} and applies the
	 * given {@link Color} as a tint.
	 * 
	 * @param source
	 *            The source {@link Icon} to apply the tint to
	 * @param tint
	 *            The {@link Color} with which to apply as a tint to the source
	 *            icon
	 */
    public ColoredIcon(Icon source, Color tint) {
        this.sourceIcon = source;
        this.tint = tint;
        this.alpha = 0.5f;
    }

    public int getIconHeight() {
        return sourceIcon.getIconHeight();
    }

    public int getIconWidth() {
        return sourceIcon.getIconWidth();
    }

    public void paintIcon(Component c, Graphics g, int x, int y) {
        BufferedImage img = new BufferedImage(getIconWidth(), getIconHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = (Graphics2D) img.getGraphics();
        sourceIcon.paintIcon(c, g2, 0, 0);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, alpha));
        g2.setColor(tint);
        g2.fillRect(0, 0, getIconWidth(), getIconHeight());
        g2.dispose();
        
        g.drawImage(img, x, y, null);
    }
}
