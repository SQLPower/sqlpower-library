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

package ca.sqlpower.swingui.table;

import javax.swing.JTable;
import javax.swing.table.TableModel;

/**
 * This {@link EditableJTable} uses a {@link TableModelSortDecorator} to produce
 * a {@link JTable} that can be edited as well as sorted by clicking the table
 * headers. Extra {@link TableModelWrapper}s can be set to this table, where it
 * is wrapped underneath the {@link TableModelSortDecorator}.
 */
public class SortableEditableJTable extends EditableJTable {

	/**
	 * Creates a new {@link SortableEditableJTable}.
	 * 
	 * @param model
	 *            The {@link TableModel} to use to display the table.
	 */
	public SortableEditableJTable(TableModel model) {
		setModel(new TableModelSortDecorator(model, getTableHeader()));
	}

	/**
	 * Sets the {@link TableModel} of this {@link SortableEditableJTable}. This
	 * model is set to the lowest possible {@link TableModelWrapper}.
	 */
	@Override
	public void setModel(TableModel model) {
		TableModel m = getModel();
		
		if (!(m instanceof TableModelWrapper)) {
			super.setModel(model);
		} else {
			TableModelWrapper lowestWrapper = (TableModelWrapper) m;
			
			// down the rabbit hole as far as it goes
			while (lowestWrapper.getWrappedModel() instanceof TableModelWrapper) {
				lowestWrapper = (TableModelWrapper) lowestWrapper.getWrappedModel();
			}

			lowestWrapper.setWrappedModel(model);
		}
	}

}
