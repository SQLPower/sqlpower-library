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
