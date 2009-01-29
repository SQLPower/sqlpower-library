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

/**
 * Composes multiple icons into the same icon. The icons will be placed
 * on top of each other, the first being the icon in the background.
 */
public class ComposedIcon implements Icon {
	
	private final List<? extends Icon> icons;

	public ComposedIcon(List<? extends Icon> icons) {
		this.icons = icons;
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
