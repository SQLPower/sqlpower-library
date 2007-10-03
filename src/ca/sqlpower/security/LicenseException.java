/*
 * Copyright (c) 2007, SQL Power Group Inc.
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
