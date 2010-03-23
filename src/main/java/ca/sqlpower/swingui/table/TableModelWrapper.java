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

package ca.sqlpower.swingui.table;

import javax.swing.table.TableModel;

/**
 * A uniform interface for table models that wrap other table models. Allows
 * generic code to be written which can find the deepest nested table model
 * inside a clump of wrappers.
 */
public interface TableModelWrapper {

    /**
     * Returns the table model that this table model wrapper is wrapping.
     */
    TableModel getWrappedModel();

    /**
     * Causes this wrapper to wrap the given model instead of the one it was
     * formerly wrapping.
     */
    void setWrappedModel(TableModel model);

}
