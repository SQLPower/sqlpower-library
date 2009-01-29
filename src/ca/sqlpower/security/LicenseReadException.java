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

package ca.sqlpower.security;

/**
 * This exception wraps all problems that could occur while loading
 * a SQLPower product license file.
 *
 * @author Jonathan Fuerth
 * @version $Id$
 */
public class LicenseReadException extends Exception {

	Throwable cause;
	String message;

	public LicenseReadException(String message, Throwable cause) {
		this.cause = cause;
		this.message = message;
	}

	public Throwable getCause() { return cause; }
	public String getMessage()  { return message; }
}
