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

package ca.sqlpower.query;

public class SelectedItemEvent {
	
	private final Query source;
	
	private final QueryItem itemSelected;
	
	/**
	 * True if the item was selected, false if the item was unselected.
	 */
	private final boolean selected;
	
	private final int index;

	public SelectedItemEvent(Query source, QueryItem itemSelected, int index, boolean selected) {
		this.source = source;
		this.itemSelected = itemSelected;
		this.index = index;
		this.selected = selected;
	}

	public Query getSource() {
		return source;
	}

	public QueryItem getItemSelected() {
		return itemSelected;
	}

	public boolean isSelected() {
		return selected;
	}

	public int getIndex() {
		return index;
	}
}
