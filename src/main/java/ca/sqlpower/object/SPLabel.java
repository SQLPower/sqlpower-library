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

package ca.sqlpower.object;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.util.Collections;
import java.util.List;

import ca.sqlpower.object.annotation.Accessor;
import ca.sqlpower.object.annotation.Mutator;
import ca.sqlpower.object.annotation.Transient;

public abstract class SPLabel extends AbstractSPObject {

	/**
     * The current text of this label. May include variables encoded as
     * described in the class-level docs.
     */
    private String text;

    private HorizontalAlignment hAlignment = HorizontalAlignment.LEFT;
    private VerticalAlignment vAlignment = VerticalAlignment.MIDDLE;
    
    /**
     * The background colour defined for this label.
     */
	private Color backgroundColour;
	
	/**
	 * The foreground colour defined for this label.
	 */
	private Color foregroundColour;
	
	/**
	 * The border colour defined for this label.
	 */
	private Color borderColour;
	
	/**
	 * The padding width defined for this label.
	 */
	private Dimension padding;
	
	@Override
	protected boolean removeChildImpl(SPObject child) {
		throw new IllegalArgumentException("SPLabels do not have children");
	}

	@Override
	public List<Class<? extends SPObject>> getAllowedChildTypes() {
		return Collections.emptyList();
	}

	@Override
	public List<? extends SPObject> getChildren() {
		return Collections.emptyList();
	}

	@Override
	public List<? extends SPObject> getDependencies() {
		return Collections.emptyList();
	}

	@Override
	public void removeDependency(SPObject dependency) {
		// no-op
	}

	@Mutator
	public void setText(String text) {
		String oldVal = getText();
		this.text = text;
		firePropertyChange("text", oldVal, text);
	}

	@Accessor
	public String getText() {
		return text;
	}

	@Mutator
	public void setHorizontalAlignment(HorizontalAlignment hAlignment) {
		Object oldVal = getHorizontalAlignment();
		this.hAlignment = hAlignment;
		firePropertyChange("horizontalAlignment", oldVal, hAlignment);
	}

	@Accessor
	public HorizontalAlignment getHorizontalAlignment() {
		return hAlignment;
	}

	@Mutator
	public void setVerticalAlignment(VerticalAlignment vAlignment) {
		Object oldVal = getVerticalAlignment();
		this.vAlignment = vAlignment;
		firePropertyChange("verticalAlignment", oldVal, vAlignment);
	}

	@Accessor
	public VerticalAlignment getVerticalAlignment() {
		return vAlignment;
	}

	@Mutator
	public abstract void setFont(Font font);

	@Accessor
	public abstract Font getFont();

	@Mutator
	public void setBackgroundColour(Color backgroundColour) {
		Color oldVal = getBackgroundColour();
		this.backgroundColour = backgroundColour;
		firePropertyChange("backgroundColour", oldVal, backgroundColour);
	}

	@Accessor
	public Color getBackgroundColour() {
		return backgroundColour;
	}
	
	@Mutator
	public void setForegroundColour(Color foregroundColour) {
		Color oldVal = getForegroundColour();
		this.foregroundColour = foregroundColour;
		firePropertyChange("foregroundColour", oldVal, foregroundColour);
	}

	@Accessor
	public Color getForegroundColour() {
		return foregroundColour;
	}
	
	@Mutator
	public void setBorderColour(Color borderColour) {
		Color oldVal = getBorderColour();
		this.borderColour = borderColour;
		firePropertyChange("borderColour", oldVal, borderColour);
	}

	@Accessor
	public Color getBorderColour() {
		return borderColour;
	}

	@Mutator @Transient
	public void setPadding(int x, int y) {
		Dimension oldVal = getPadding();
		this.padding = new Dimension(x, y);
		firePropertyChange("padding", oldVal, this.padding);
	}
	
	@Mutator
	public void setPadding(Dimension pad) {
		setPadding(pad.width, pad.height);
	}
	
	@Accessor
	public Dimension getPadding() {
		return (padding == null ? new Dimension(0,0) : padding);
	}
}


