/*
 * Copyright (c) 2009, SQL Power Group Inc.
 * 
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
 *     * Neither the name of SQL Power Group Inc. nor the names of its
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
