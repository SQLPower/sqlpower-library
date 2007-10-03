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

import ca.sqlpower.sql.DatabaseObject;

/**
 * The PLSecurityException class represents a specific "access denied"
 * scenario for a certain operation.  The type of operation and the
 * reason for denying access are given.  Both strings should be
 * specified as constants from the PLSecurityManager class.
 *
 * @author Jonathan Fuerth
 * @see PLSecurityManager
 */
public class PLSecurityException extends RuntimeException {

	/**
	 * The permission that was denied.
	 */
	protected String perm;

	/**
	 * The reason for denying the permission.
	 */
	protected String reason;
	
	/**
	 * The object that we were attempting to access/modify.
	 */
	protected DatabaseObject obj;

	/**
	 * Constructs a new PLSecurityException.
	 */
	public PLSecurityException(String perm, String reason, DatabaseObject obj) {
		super();
		this.perm = perm;
		this.reason = reason;
		this.obj = obj;
	}

	public String getPermission() {
		return perm;
	}

	public String getReason() {
		return reason;
	}

	public DatabaseObject getDatabaseObject() {
		return obj;
	}

	public String getMessage() {
		if (obj == null) {
			return "Denied "+perm+" because "+reason;
		} else {
			return "Denied "+perm+" on "+obj.getObjectName()+" because "+reason;			
		}
	}
}
