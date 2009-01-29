/*
 * Copyright (c) 2008, SQL Power Group Inc.
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
package ca.sqlpower.validation;

import ca.sqlpower.validation.swingui.ValidationHandler;

/**
 * This is a interface use for creating dialogs. Since the 
 * dialog along with the actions are created in the 
 * DataEntryPanelBuilder, the original panel has no way of
 * disabling those actions. Thus, the DataEntryPanel will
 * be responsible for linking the actions to the handler. 
 */
public interface Validated {

	/** 
     * @return the handler that validates the current form.
	 */ 
	public ValidationHandler getHandler();
}
