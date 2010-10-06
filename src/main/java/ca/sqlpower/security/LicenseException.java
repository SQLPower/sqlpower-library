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

import java.util.List;
import java.util.ArrayList;

/**
 * This exception (or a subclass) is meant to be thrown whenever an
 * application detects a violation of a valid license.  It is not
 * intended to be thrown when an invalid (corrupt, tampered-with)
 * license is detected.  See {@link LicenseReadException}.
 *
 * @author Jonathan Fuerth
 * @version $Id$
 */
public class LicenseException extends Exception {
	protected String reasonCode;
	protected List errorParams;
	protected Throwable cause;

	/**
	 * The most generic constructor.  Takes an ApplicationResources
	 * key (the reason code), a List of substitution objects (which
	 * should be used for the substitution parameters that format this
	 * exception for the user), and an optional nested cause for this
	 * license exception.
	 */
	public LicenseException(String reasonCode, List errorParams, Throwable cause) {
		super();
		this.reasonCode = reasonCode;
		this.errorParams = errorParams;
		this.cause = cause;
	}

	public LicenseException(String reasonCode, Throwable cause) {
		this(reasonCode, null, cause);
	}

	public LicenseException(String reasonCode, Object errorParam) {
		this(reasonCode, null, null);
		if (errorParam != null) {
			errorParams = new ArrayList(1);
			errorParams.add(errorParam);
		}
	}

	public LicenseException(String reasonCode, List errorParamList) {
		this(reasonCode, errorParamList, null);
	}

	public LicenseException(String reasonCode) {
		this(reasonCode, null, null);
	}

	public Throwable getCause() {
		return cause;
	}

	/**
	 * Returns the reason code, which should be translated to
	 * human-readable form via a language-specific
	 * ApplicationResources.properties file.
	 */
	public String getReasonCode() {
		return reasonCode;
	}

	/**
	 * Returns the ith error parameter, or null if there is no such
	 * parameter.
	 */
	public Object getErrorParam(int i) { 
		if (errorParams != null && i < errorParams.size()) {
			return errorParams.get(i);
		} else {
			return null;
		}
	}

	/**
	 * Tells how many error parameters there are in this exception.
	 */
	public int getErrorParamCount() {
		if (errorParams != null) {
			return errorParams.size();
		} else {
			return 0;
		}
	}

	/**
	 * Useful if you're using the Jakarta Struts ActionMessage class
	 * for formatting.
	 */
	public Object[] getErrorParamArray() {
		int n = getErrorParamCount();
		if (n == 0) {
			return null;
		} else {
			Object[] params = new Object[n];
			for (int i = 0; i < n; i++) {
				params[i] = errorParams.get(i);
			}
			return params;
		}
	}
}
