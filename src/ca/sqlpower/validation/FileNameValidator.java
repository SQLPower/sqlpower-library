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

import java.io.File;
import java.io.IOException;


/**
 * A Validator to ensure that the supplied file path is a valid
 * file, or if it doesn't exist, then checks if it can be created.
 */
public class FileNameValidator implements Validator {
	
	private final String desc;
	
	/**
	 * Creates a validator that creates results according
	 * to the given file description (e.g. "Log" for log file). 
	 */
	public FileNameValidator(String desc) {
		this.desc = desc;
	}
	
	/**
	 * Returns a {@link ValidateResult} with {@link Status#FAIL} if:
	 * <li> the file name is empty</li>
	 * <li> the file path provided is not a file</li>
	 * <li> the file doesn't exist and cannot be created</li>
	 * <p>
	 * Otherwise, it returns a ValidateResult with {@link Status#OK}
	 */
	public ValidateResult validate(Object contents) {
		String name = (String) contents;
		if (name == null || name.length() == 0) {
			return ValidateResult.createValidateResult(Status.FAIL,
					desc + " file is required.");
		}
		File f = new File(name);
		if (f.exists()) {
			if (!f.isFile()) {
				return ValidateResult.createValidateResult(Status.FAIL,
					desc + " file name is invalid.");
			}

			// can't reliably check if file is writable on Windows,
			// so we'll just assume it is.
		} else {
			try {
				if (!f.createNewFile()) {
					return ValidateResult.createValidateResult(Status.FAIL,
							desc + " file can not be created.");
				}
			} catch (IOException e) {
				return ValidateResult.createValidateResult(Status.FAIL,
						desc + " file can not be created.");
			} finally {
				f.delete();
			}
		}
		return ValidateResult.createValidateResult(Status.OK, "");
	}

}