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

import java.awt.Component;
import java.awt.Graphics;
import java.util.List;

import javax.swing.Icon;

import net.jcip.annotations.Immutable;

/**
 * An icon whose appearance is made up of one or more icons sitting on top of
 * each other.
 */
@Immutable
public final class ComposedIcon implements Icon {
	
	private final Icon[] icons;

	/**
	 * @deprecated use the static factory method instead
	 */
	public ComposedIcon(List<? extends Icon> icons) {
		this(icons.toArray(new Icon[icons.size()]));
	}

	private ComposedIcon(Icon ... icons) {
	    this.icons = icons;
	}

    /**
     * Returns a ComposedIcon of the given icons. When painted, the icons will
     * be placed on top of each other, the first being the icon in the
     * background.
     * 
     * @param backToFront
     *            The icons that make up this ComposedIcon. When this icon
     *            paints itself, it does so by painting the given icons in the
     *            order they appear in the argument list. The first icon in the
     *            list is painted first, so it will appear underneath the
     *            subsequent icons.
     * @return A ComposedIcon instance that consists of the given icons in the
     *         given order. This method may return a cached instance, but
     *         ComposedIcons are immutable and therefore thread safe.
     */
	public static ComposedIcon getInstance(Icon ... backToFront) {
	    return new ComposedIcon(backToFront);
	}
	
	public int getIconHeight() {
		int maxHeight = 0;
		for (Icon i : icons) {
			maxHeight = Math.max(maxHeight, i.getIconHeight());
		}
		return maxHeight;
	}

	public int getIconWidth() {
		int maxWidth = 0;
		for (Icon i : icons) {
			maxWidth = Math.max(maxWidth, i.getIconWidth());
		}
		return maxWidth;
	}

	public void paintIcon(Component c, Graphics g, int x, int y) {
		for (Icon i : icons) {
			i.paintIcon(c, g, x, y);
		}
	}

}
