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

package ca.sqlpower.validation.swingui;

import ca.sqlpower.validation.Status;
import ca.sqlpower.validation.ValidateResult;
import ca.sqlpower.validation.Validator;

/**
 * A validator that simply checks if the validated object is null.
 *
 */
public class NotNullValidator implements Validator {
	
	private final String objName;
	
	/**
	 * Creates a validator that will fail when the validated object is null.
	 * 
	 * @param objName Used to describe the validated object in the fail message.
	 */
	public NotNullValidator(String objName) {
		this.objName = objName;
	}

	public ValidateResult validate(Object contents) {
		if (contents == null) {
			return ValidateResult.createValidateResult(Status.FAIL, objName + " cannot be null.");
		} else {
			return ValidateResult.createValidateResult(Status.OK, "");
		}
	}

}
