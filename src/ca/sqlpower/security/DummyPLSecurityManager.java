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

import java.sql.Connection;
import ca.sqlpower.sql.DatabaseObject;

/**
 * DummyPLSecurityManager is a dangerous class I had to make to allow
 * certain operations that are inconsistent with the security model:
 *
 * <ul>
 * <li>You can rename an object if you can modify it, but rename entails
 *     both creation and deletion.
 * <li>You need to be able to delete and create dimension and measure
 *     objects, and related data when you are renaming or editting a kpi
 *     for which you don't have delete permission.
 * <li>Other such inconsistencies.
 * </ul>
 *
 * <p>An instance of this class blindly permits all operations.  If
 * you need to use this class under circumstances different from those
 * listed above, discuss it with someone first.  There is probably a
 * better way.
 */
public class DummyPLSecurityManager extends PLSecurityManager {
	
	public DummyPLSecurityManager() {
		super();
		principal = null;
	}

	public void checkCreateAny(Connection con, String objectType) { return; }
	public boolean canCreateAny(Connection con, String objectType) { return true; }
	public void checkModify(Connection con, DatabaseObject obj) { return; }
	public boolean canModify(Connection con, DatabaseObject obj) { return true; }
	public void checkDelete(Connection con, DatabaseObject obj) { return; }
	public boolean canDelete(Connection con, DatabaseObject obj) { return true; }
	public void checkExecute(Connection con, DatabaseObject obj) { return; }
	public boolean canExecute(Connection con, DatabaseObject obj) { return true; }
	public void checkGrant(Connection con, DatabaseObject obj) { return; }
	public boolean canGrant(Connection con, DatabaseObject obj) { return true; }
	public void checkNotify(Connection con, DatabaseObject obj) { return; }
	public boolean canNotify(Connection con, DatabaseObject obj) { return true; }
	public static boolean checkPermission(Connection con,
										  PLUser p,
										  DatabaseObject obj,
										  String perm,
										  boolean exceptionWhenDenied) { return true; }
	public static boolean checkUserPermission(Connection con,
											  PLUser p,
											  DatabaseObject obj,
											  String perm) { return true; }
	public static boolean checkGroupPermission(Connection con,
											   PLGroup p,
											   DatabaseObject obj,
											   String perm) { return true; }
}
