package ca.sqlpower.security;

import ca.sqlpower.sql.DatabaseObject;
import java.sql.*;
import java.util.*;
import ca.sqlpower.sql.*;

import ca.sqlpower.dashboard.DBConnection;

public class PLSecurityManager implements java.io.Serializable {

	public static final String MODIFY_PERMISSION = "permission.modify";
	public static final String DELETE_PERMISSION = "permission.delete";
	public static final String EXECUTE_PERMISSION = "permission.execute";
	public static final String GRANT_PERMISSION = "permission.grant";
	public static final String NOTIFY_PERMISSION = "permission.notify";
	public static final String LOGIN_PERMISSION = "permission.login";
	public static final String CREATE_PERMISSION = "permission.create";

	public static final String BAD_CREDENTIALS = "security.credentials";
	public static final String INVALID_MANAGER = "security.invalid";
	public static final String INSUFFICIENT_ACCESS = "security.insufficient";

	PLUser principal;

	/**
	 * For the dummy subclass.
	 */
	protected PLSecurityManager() {
		super();
	}

	/**
	 * Authenticates the given username/pasword combination against
	 * the PL_USER table.
	 *
	 * @throws PLSecurityException if the username and password
	 * credentials are not valid for the given database connection.
	 */
	public PLSecurityManager(Connection con, String username, String password)
		throws SQLException, PLSecurityException {
		if (password == null) {
			throw new NullPointerException("null password not allowed");
		}
		principal = PLUser.findByPrimaryKey(con, username, password);
	}

	/**
	 * Throws an exception if the current Principal user is not
	 * allowed to create objects.  Useful last-minute
	 * security checks in the business model.
	 *
	 * @param con An open connection to the database that the current
	 * user is a part of.
	 * @throws PLSecurityException if the current user does not have
	 * CREATE_ANY permission.
	 */
	public void checkCreateAny(Connection con, String objectType)
		throws SQLException, PLSecurityException {
		if (principal == null) {
			throw new PLSecurityException(MODIFY_PERMISSION, INVALID_MANAGER, null);
		}
		checkPermission(con, principal, getSystemObject(objectType), CREATE_PERMISSION, true);
	}

	/**
	 * Checks the same way as checkCreate, but returns true or false
	 * rather than throwing an exception.  Useful for deciding whether
	 * or not to disable a feature on a user interface (where
	 * exceptions would be a nuisance).
	 */
	public boolean canCreateAny(Connection con, String objectType)
		throws SQLException {
		if (principal == null) {
			throw new PLSecurityException(MODIFY_PERMISSION, INVALID_MANAGER, null);
		}
		return checkPermission(con, principal, getSystemObject(objectType), CREATE_PERMISSION, false);
	}

	/**
	 * Throws an exception if the current Principal user is not
	 * allowed to modify the given object.  Useful last-minute
	 * security checks in the business model.
	 *
	 * @param con An open connection to the database that the current
	 * user and the given object are a part of.
	 * @param obj The object you want to check permissions on.
	 * @throws PLSecurityException if the current user does not have
	 * modify permission on <code>obj</code>.
	 */
	public void checkModify(Connection con, DatabaseObject obj)
		throws SQLException, PLSecurityException {
		if (principal == null) {
			throw new PLSecurityException(MODIFY_PERMISSION, INVALID_MANAGER, obj);
		}
		checkPermission(con, principal, obj, MODIFY_PERMISSION, true);
	}

	/**
	 * Checks the same way as checkModify, but returns true or false
	 * rather than throwing an exception.  Useful for deciding whether
	 * or not to disable a feature on a user interface (where
	 * exceptions would be a nuisance).
	 */
	public boolean canModify(Connection con, DatabaseObject obj)
		throws SQLException {
		if (principal == null) {
			throw new PLSecurityException(MODIFY_PERMISSION, INVALID_MANAGER, obj);
		}
		return checkPermission(con, principal, obj, MODIFY_PERMISSION, false);
	}

	/**
	 * Throws an exception if the current Principal user is not
	 * allowed to modify the given object.  Useful last-minute
	 * security checks in the business model.
	 *
	 * @param con An open connection to the database that the current
	 * user and the given object are a part of.
	 * @param obj The object you want to check permissions on.
	 * @throws PLSecurityException if the current user does not have
	 * modify permission on <code>obj</code>.
	 */
	public void checkDelete(Connection con, DatabaseObject obj)
		throws SQLException, PLSecurityException {
		if (principal == null) {
			throw new PLSecurityException(DELETE_PERMISSION, INVALID_MANAGER, obj);
		}
		checkPermission(con, principal, obj, DELETE_PERMISSION, true);
	}

	/**
	 * Checks the same way as checkDelete, but returns true or false
	 * rather than throwing an exception.  Useful for deciding whether
	 * or not to disable a feature on a user interface (where
	 * exceptions would be a nuisance).
	 */
	public boolean canDelete(Connection con, DatabaseObject obj)
		throws SQLException {
		if (principal == null) {
			throw new PLSecurityException(DELETE_PERMISSION, INVALID_MANAGER, obj);
		}
		return checkPermission(con, principal, obj, DELETE_PERMISSION, false);
	}

	/**
	 * Throws an exception if the current Principal user is not
	 * allowed to execute the given object.  Useful last-minute
	 * security checks in the business model.
	 *
	 * @param con An open connection to the database that the current
	 * user and the given object are a part of.
	 * @param obj The object you want to check permissions on.
	 * @throws PLSecurityException if the current user does not have
	 * execute permission on <code>obj</code>.
	 */
	public void checkExecute(Connection con, DatabaseObject obj)
		throws SQLException, PLSecurityException {
		if (principal == null) {
			throw new PLSecurityException(EXECUTE_PERMISSION, INVALID_MANAGER, obj);
		}
		checkPermission(con, principal, obj, EXECUTE_PERMISSION, true);
	}

	/**
	 * Checks the same way as checkExecute, but returns true or false
	 * rather than throwing an exception.  Useful for deciding whether
	 * or not to disable a feature on a user interface (where
	 * exceptions would be a nuisance).
	 */
	public boolean canExecute(Connection con, DatabaseObject obj)
		throws SQLException, PLSecurityException {
		if (principal == null) {
			throw new PLSecurityException(EXECUTE_PERMISSION, INVALID_MANAGER, obj);
		}
		return checkPermission(con, principal, obj, EXECUTE_PERMISSION, false);
	}

	/**
	 * Throws an exception if the current Principal user is not
	 * allowed to grant permissions on the given object.  Useful
	 * last-minute security checks in the business model.
	 *
	 * @param con An open connection to the database that the current
	 * user and the given object are a part of.
	 * @param obj The object you want to check permissions on.
	 * @throws PLSecurityException if the current user does not have
	 * grant permission on <code>obj</code>.
	 */
	public void checkGrant(Connection con, DatabaseObject obj)
		throws SQLException, PLSecurityException {
		if (principal == null) {
			throw new PLSecurityException(GRANT_PERMISSION, INVALID_MANAGER, obj);
		}
		checkPermission(con, principal, obj, GRANT_PERMISSION, true);
	}

	/**
	 * Checks the same way as checkGrant, but returns true or false
	 * rather than throwing an exception.  Useful for deciding whether
	 * or not to disable a feature on a user interface (where
	 * exceptions would be a nuisance).
	 */
	public boolean canGrant(Connection con, DatabaseObject obj)
		throws SQLException, PLSecurityException {
		if (principal == null) {
			throw new PLSecurityException(GRANT_PERMISSION, INVALID_MANAGER, obj);
		}
		return checkPermission(con, principal, obj, GRANT_PERMISSION, false);
	}

	/**
	 * Throws an exception if the current Principal user is not
	 * allowed to the given object.  Useful last-minute
	 * security checks in the business model.
	 *
	 * @param con An open connection to the database that the current
	 * user and the given object are a part of.
	 * @param obj The object you want to check permissions on.
	 * @throws PLSecurityException if the current user does not have
	 * modify permission on <code>obj</code>.
	 */
	public void checkNotify(Connection con, DatabaseObject obj)
		throws SQLException, PLSecurityException {
		if (principal == null) {
			throw new PLSecurityException(NOTIFY_PERMISSION, INVALID_MANAGER, obj);
		}
		checkPermission(con, principal, obj, NOTIFY_PERMISSION, true);
	}

	/**
	 * Checks the same way as checkNotify, but returns true or false
	 * rather than throwing an exception.  Useful for deciding whether
	 * or not to disable a feature on a user interface (where
	 * exceptions would be a nuisance).
	 */
	public boolean canNotify(Connection con, DatabaseObject obj)
		throws SQLException {
		if (principal == null) {
			throw new PLSecurityException(NOTIFY_PERMISSION, INVALID_MANAGER, obj);
		}
		return checkPermission(con, principal, obj, NOTIFY_PERMISSION, false);
	}

	/**
	 * Use this method (or the checkXXX methods, which call this one)
	 * to find out if a given user has a certain permission.  All the
	 * checkXXX methods work by calling this one.
	 *
	 * @param exceptionWhenDenied If true, this method will throw a
	 * PLSecurityException instead of returning false.
	 */
	public static boolean checkPermission(Connection con,
									   PLUser p,
									   DatabaseObject obj,
									   String perm,
									   boolean exceptionWhenDenied)
		throws SQLException, PLSecurityException {

		DatabaseObject sysObject = getSystemObject(obj.getObjectType());

		if (checkUserPermission(con, p, obj, perm)
			|| checkUserPermission(con, p, sysObject, perm)) {
			return true;
		}

		Iterator groups = p.getGroups(con).iterator();
		while (groups.hasNext()) {
			PLGroup g = (PLGroup) groups.next();
			if (checkGroupPermission(con, g, obj, perm)
				|| checkGroupPermission(con, g, sysObject, perm)) {
				return true;
			}
		}
		
		if (exceptionWhenDenied) {
			throw new PLSecurityException(perm, INSUFFICIENT_ACCESS, obj);
		}
		return false;
	}

	/**
	 * Use this method to find out if a given group has a certain
	 * permission.  This is different from {@link #checkGroupPermission}
	 * because it checks both specific and system privs
	 * (checkGroupPermission only does one or the other).
	 *
	 * @param exceptionWhenDenied If true, this method will throw a
	 * PLSecurityException instead of returning false.
	 */
	public static boolean checkPermission(Connection con,
									   PLGroup g,
									   DatabaseObject obj,
									   String perm,
									   boolean exceptionWhenDenied)
		throws SQLException, PLSecurityException {

		DatabaseObject sysObject = getSystemObject(obj.getObjectType());

		if (checkGroupPermission(con, g, obj, perm)
			|| checkGroupPermission(con, g, sysObject, perm)) {
			return true;
		}
		
		if (exceptionWhenDenied) {
			throw new PLSecurityException(perm, INSUFFICIENT_ACCESS, obj);
		}
		return false;
	}

	/**
	 * Returns true if and only if the given user has specifically
	 * been granted access to the given database object.
	 *
	 * @param obj The object on which you want to determine the user's
	 * permissions.  If the argument is an AllDatabaseObject then
	 * permissions are looked up in the USER_SYSTEM_PRIVS table.
	 * @see checkGroupPermission
	 */
	public static boolean checkUserPermission(Connection con,
											  PLUser p,
											  DatabaseObject obj,
											  String perm)
		throws SQLException, PLSecurityException {

		if (perm.equals(CREATE_PERMISSION) && !(obj instanceof AllDatabaseObject)) {
			throw new IllegalArgumentException
				("CREATE_PERMISSION is only valid for AllDatabaseObject types.");
		}

		StringBuffer sql = new StringBuffer(500);
		if (obj instanceof AllDatabaseObject) {
			sql.append("SELECT create_any_ind, modify_any_ind, delete_any_ind,");
			sql.append(" execute_any_ind, grant_any_ind, notify_any_ind");
			sql.append(" FROM user_system_privs");
			sql.append(" WHERE user_id=").append(SQL.quote(p.getUserId()));
			sql.append(" AND object_type=").append(SQL.quote(obj.getObjectType()));			
		} else {
			sql.append("SELECT modify_ind, delete_ind, execute_ind, grant_ind, notify_ind");
			sql.append(" FROM user_object_privs");
			sql.append(" WHERE user_id=").append(SQL.quote(p.getUserId()));
			sql.append(" AND object_type=").append(SQL.quote(obj.getObjectType()));
			sql.append(" AND object_name=").append(SQL.quote(obj.getObjectName()));
		}

		Statement stmt = null;
		try {
			stmt = con.createStatement();
			ResultSet rs = stmt.executeQuery(sql.toString());
			if (rs.next()) {
				String ind = null;
				if (obj instanceof AllDatabaseObject) {
					ind = rs.getString(sysPermToColName(perm));
				} else {
					ind = rs.getString(permToColName(perm));
				}
				if (ind != null && ind.equals("Y")) {
					// Access granted
					return true;
				}
			}

			return false;

		} finally {
			if (stmt != null) {
				stmt.close();
			}
		}
	}

	/**
	 * Returns true if and only if the given user has access to the
	 * given database object because of a group permission.
	 *
	 * @see checkUserPermission
	 */
	public static boolean checkGroupPermission(Connection con,
											   PLGroup p,
											   DatabaseObject obj,
											   String perm)
		throws SQLException, PLSecurityException {

		if (perm.equals(CREATE_PERMISSION) && !(obj instanceof AllDatabaseObject)) {
			throw new IllegalArgumentException
				("CREATE_PERMISSION is only valid for AllDatabaseObject types");
		}

		StringBuffer sql = new StringBuffer(500);
		if (obj instanceof AllDatabaseObject) {
			sql.append("SELECT create_any_ind, modify_any_ind, delete_any_ind,");
			sql.append(" execute_any_ind, grant_any_ind, notify_any_ind");
			sql.append(" FROM group_system_privs");
			sql.append(" WHERE group_name=").append(SQL.quote(p.getGroupName()));
			sql.append(" AND object_type=").append(SQL.quote(obj.getObjectType()));			
		} else {
			sql.append("SELECT modify_ind, delete_ind, execute_ind, grant_ind, notify_ind");
			sql.append(" FROM group_object_privs");
			sql.append(" WHERE group_name=").append(SQL.quote(p.getGroupName()));
			sql.append(" AND object_type=").append(SQL.quote(obj.getObjectType()));
			sql.append(" AND object_name=").append(SQL.quote(obj.getObjectName()));
		}

		Statement stmt = null;
		try {
			stmt = con.createStatement();
			ResultSet rs = stmt.executeQuery(sql.toString());
			if (rs.next()) {
				String ind = null;
				if (obj instanceof AllDatabaseObject) {
					ind = rs.getString(sysPermToColName(perm));
				} else {
					ind = rs.getString(permToColName(perm));
				}
				if (ind != null && ind.equals("Y")) {
					// Access granted
					return true;
				}
			}

			return false;

		} finally {
			if (stmt != null) {
				stmt.close();
			}
		}
	}

	/**
	 * Grants or revokes execute permission on obj to grantee if
	 * current principal has the right.
	 */
	public void grantExecute(Connection con,
							 PLUser grantee,
							 DatabaseObject obj,
							 boolean givingPerm)
		throws SQLException, PLSecurityException {
		grantUserPermission(con, principal, grantee, obj, EXECUTE_PERMISSION, givingPerm);
	}

	/**
	 * Grants or revokes execute permission on obj to grantee if
	 * current principal has the right.
	 */
	public void grantExecute(Connection con,
							 PLGroup grantee,
							 DatabaseObject obj,
							 boolean givingPerm)
		throws SQLException, PLSecurityException {
		grantGroupPermission(con, principal, grantee, obj, EXECUTE_PERMISSION, givingPerm);
	}

	/**
	 * Grants or revokes delete permission on obj to grantee if
	 * current principal has the right.
	 */
	public void grantDelete(Connection con,
							PLUser grantee,
							DatabaseObject obj,
							boolean givingPerm)
		throws SQLException, PLSecurityException {
		grantUserPermission(con, principal, grantee, obj, DELETE_PERMISSION, givingPerm);
	}

	/**
	 * Grants or revokes delete permission on obj to grantee if
	 * current principal has the right.
	 */
	public void grantDelete(Connection con,
							PLGroup grantee,
							DatabaseObject obj,
							boolean givingPerm)
		throws SQLException, PLSecurityException {
		grantGroupPermission(con, principal, grantee, obj, DELETE_PERMISSION, givingPerm);
	}

	/**
	 * Grants or revokes modify permission on obj to grantee if
	 * current principal has the right.
	 */
	public void grantModify(Connection con,
							PLUser grantee,
							DatabaseObject obj,
							boolean givingPerm)
		throws SQLException, PLSecurityException {
		grantUserPermission(con, principal, grantee, obj, MODIFY_PERMISSION, givingPerm);
	}

	/**
	 * Grants or revokes modify permission on obj to grantee if
	 * current principal has the right.
	 */
	public void grantModify(Connection con,
							PLGroup grantee,
							DatabaseObject obj,
							boolean givingPerm)
		throws SQLException, PLSecurityException {
		grantGroupPermission(con, principal, grantee, obj, MODIFY_PERMISSION, givingPerm);
	}

	/**
	 * Grants or revokes grant permission on obj to grantee if
	 * current principal has the right.
	 */
	public void grantGrant(Connection con,
						   PLUser grantee,
						   DatabaseObject obj,
						   boolean givingPerm)
		throws SQLException, PLSecurityException {
		grantUserPermission(con, principal, grantee, obj, GRANT_PERMISSION, givingPerm);
	}

	/**
	 * Grants or revokes grant permission on obj to grantee if
	 * current principal has the right.
	 */
	public void grantGrant(Connection con,
						   PLGroup grantee,
						   DatabaseObject obj,
						   boolean givingPerm)
		throws SQLException, PLSecurityException {
		grantGroupPermission(con, principal, grantee, obj, GRANT_PERMISSION, givingPerm);
	}

	/**
	 * Grants the given permission on the given object to the given
	 * user (the <i>grantee</i>) in the given database, if the
	 * principal user has permission to do so.
	 *
	 * <p>SECURITY REQUIREMENTS: principal user must have grant
	 * permission on the database object <code>obj</code>.
	 *
	 * @param con A connection to the database in question.
	 * @param principal The user who wishes to grant a permission.
	 * @param grantee The user to whom the new permission will be granted
	 * @param obj The object of this action: grantee will recieve the
	 * requested permission on this object.
	 * @param perm The permission you wish to grant or revoke (for
	 * example, PLSecurityManager.EXECUTE_PERMISSION).
	 * @param givingPerm If true, the permission will be given to the
	 * grantee, otherwise it will be revoked from the grantee.
	 */
	public static void grantUserPermission(Connection con,
										   PLUser principal,
										   PLUser grantee,
										   DatabaseObject obj,
										   String perm,
										   boolean givingPerm)
		throws PLSecurityException, SQLException {

		grantPermission(con, principal, grantee.getUserId(), true, obj, perm, givingPerm);
	}
	
	/**
	 * Grants the given permission on the given object to the given
	 * group (the <i>grantee</i>) in the given database, if the
	 * principal user has permission to do so.
	 *
	 * <p>SECURITY REQUIREMENTS: principal user must have grant
	 * permission on the database object <code>obj</code>.
	 *
	 * @param con A connection to the database in question.
	 * @param principal The user who wishes to grant a permission.
	 * @param grantee The group to which the new permission will be granted
	 * @param obj The object of this action: grantee will recieve the
	 * requested permission on this object.
	 * @param perm The permission you wish to grant or revoke (for
	 * example, PLSecurityManager.EXECUTE_PERMISSION).
	 * @param givingPerm If true, the permission will be given to the
	 * grantee, otherwise it will be revoked from the grantee.
	 */
	public static void grantGroupPermission(Connection con,
										   PLUser principal,
										   PLGroup grantee,
										   DatabaseObject obj,
										   String perm,
										   boolean givingPerm)
		throws PLSecurityException, SQLException {

		grantPermission(con, principal, grantee.getGroupName(), false, obj, perm, givingPerm);
	}
	
	/**
	 * Does the SQL grovelling for grantUserPermission and
	 * grantGroupPermission. See their docs for details.
	 *
	 * <p>SECURITY REQUIREMENT: the principal user must have GRANT
	 * permission on the object <i>obj</i>.
	 *
	 * @param granteeName The name of a user or group
	 * @param granteeIsUser If true, granteeName is treated as the
	 * name of a user; otherwise granteeName is treated as the name of
	 * a group.
	 *
	 * @see grantUserPermission for details on other parameters
	 */
	public static void grantPermission(Connection con,
									   PLUser principal,
									   String granteeName,
									   boolean granteeIsUser,
									   DatabaseObject obj,
									   String perm,
									   boolean givingPerm)
		throws PLSecurityException, SQLException {

		checkPermission(con, principal, obj, GRANT_PERMISSION, true);

		Statement stmt = null;
		try {
			stmt = con.createStatement();

			StringBuffer sql = new StringBuffer(500);
			if (granteeIsUser) {
				sql.append("UPDATE user_object_privs SET ");
			} else {
				sql.append("UPDATE group_object_privs SET ");
			}
			sql.append(permToColName(perm)).append("=").append(givingPerm?"'Y'":"'N'");
			sql.append(", last_update_date=").append(DBConnection.getSystemDate(con));
			sql.append(", last_update_user=").append(SQL.quote(principal.getUserId()));
			sql.append(", last_update_os_user='Power*Dashboard Web Facility'");
			if (granteeIsUser) {
				sql.append(" WHERE user_id=").append(SQL.quote(granteeName));
			} else {
				sql.append(" WHERE group_name=").append(SQL.quote(granteeName));
			}
			sql.append(" AND object_type=").append(SQL.quote(obj.getObjectType()));
			sql.append(" AND object_name=").append(SQL.quote(obj.getObjectName()));

			int updateCount = stmt.executeUpdate(sql.toString());
			
			if (updateCount > 1) {
				throw new IllegalStateException("Updated "+updateCount
												+" rows (should have been 1 or 0)");
			} else if (updateCount == 0) {
				// Switch to insert
				sql.setLength(0);
				sql.setLength(500);
				if (granteeIsUser) {
					sql.append("INSERT INTO user_object_privs (");
				} else {
					sql.append("INSERT INTO group_object_privs (");
				}
				sql.append(permToColName(perm));
				if (granteeIsUser) {
					sql.append(", user_id");
				} else {
					sql.append(", group_name");
				}
				sql.append(", object_type, object_name");
				sql.append(", last_update_user, last_update_os_user, last_update_date");
				sql.append(") VALUES (").append(givingPerm?"'Y'":"'N'");;
				sql.append(", ").append(SQL.quote(granteeName));
				sql.append(", ").append(SQL.quote(obj.getObjectType()));
				sql.append(", ").append(SQL.quote(obj.getObjectName()));
				sql.append(", ").append(SQL.quote(principal.getUserId()));
				sql.append(", 'Power*Dashboard Web Facility'");

				// XXX: in this specific place, the SQLServer2000 JDBC
				// driver throws NoSuchElementException if the {fn
				// NOW()} escape sequence is present.
				sql.append(", null");//.append(DBConnection.getSystemDate(con));
				sql.append(")");
				
				try {
					updateCount = stmt.executeUpdate(sql.toString());
				} catch (SQLException e) {
					System.out.println("Caught "+e.getMessage());
					System.out.println("Query: "+sql);
					throw e;
				} catch (NoSuchElementException e) {
					// SQLServer2000 JDBC driver throws this for unknown reason
					System.out.println("Caught "+e.getMessage());
					System.out.println("Query: "+sql);
					throw e;
				}
				if (updateCount > 1) {
					throw new IllegalStateException("Updated "+updateCount
													+" rows (should have been 1 or 0)");
				} else if (updateCount == 0) {
					throw new IllegalStateException("Could not update or insert permission!");
				}
			}
		} finally {
			if (stmt != null) {
				stmt.close();
			}
		}
	}

	/**
	 * Bootstraps permissions for a newly-created object.  Grants
	 * MODIFY, DELETE, EXECUTE, and GRANT privs to the user
	 * represented by <code>sm</code> if that user is allowed to
	 * create the type of database object represented by
	 * <code>obj</code>.
	 *
	 * @throws SQLException if there is a database problem.
	 * @throws PLSecurityException if <code>sm</code> denies create
	 * rights on obj.
	 */
	public static void createDatabaseObject(Connection con, PLSecurityManager sm,
											DatabaseObject obj)
		throws SQLException, PLSecurityException {

		sm.checkCreateAny(con, obj.getObjectType());

		StringBuffer sql = new StringBuffer(50);
		sql.append("INSERT INTO user_object_privs (user_id");
		sql.append(", object_type, object_name");
		sql.append(", last_update_user, last_update_os_user, last_update_date");
		sql.append(", execute_ind, modify_ind, delete_ind, grant_ind");
		sql.append(") VALUES (").append(SQL.quote(sm.principal.getUserId()));
		sql.append(", ").append(SQL.quote(obj.getObjectType()));
		sql.append(", ").append(SQL.quote(obj.getObjectName()));
		sql.append(", ").append(SQL.quote(sm.principal.getUserId()));
		sql.append(", 'Power*Dashboard Web Facility'");
		sql.append(", ").append(DBConnection.getSystemDate(con));
		sql.append(", 'Y', 'Y', 'Y', 'Y'");
		sql.append(")");

		Statement stmt = null;
		try {
			stmt = con.createStatement();
			stmt.executeUpdate(sql.toString());
		} catch (SQLException e) {
			System.out.println
				("PLSecurityManager.createDatabaseObject: Error in SQL Statement: "
				 +e.getMessage());
			System.out.println("Query: "+sql);
			throw e;
		} finally {
			if (stmt != null) stmt.close();
		}
	}

	/**
	 * Call this method to remove everything security-ish about a
	 * database object.  It will not magically remove the object's own
	 * data, but it will zap all the necessary rows from the following:
	 * 
	 * <ul>
	 *  <li>USER_OBJECT_PRIVS
	 *  <li>GROUP_OBJECT_PRIVS
	 *  <li>PL_USER_NOTIFICATION_LOG (via EmailNotification class)
	 *  <li>PL_USER_NOTIFICATION (via EmailNotification class)
	 *  <li>PL_TRANSFORM_STATS (only for TRANSACTION type objects)
	 *  <li>PL_STATS
	 * </ul>
	 *
	 * <p>It is expected that the given connection will <b>not</b> be
	 * in autocommit mode.
	 *
	 * <p>SECURITY REQUIREMENT: sm must allow DELETE permission on obj.
	 *
	 * @param con An open connection to the database in question.
	 * Should not be in autocommit mode, but this is not enforced.
	 * @param sm A security manager that allows deletion of the object in question.
	 * @param obj The database object we're evicting.
	 */
	public static void deleteDatabaseObject(Connection con, PLSecurityManager sm,
											DatabaseObject obj)
		throws PLSecurityException, SQLException {

		sm.checkDelete(con, obj);

		Statement stmt = null;
		try {
			stmt = con.createStatement();

			StringBuffer sql = new StringBuffer();
			sql.append("DELETE FROM user_object_privs WHERE object_type=");
			sql.append(SQL.quote(obj.getObjectType()));
			sql.append(" AND object_name=").append(SQL.quote(obj.getObjectName()));
			stmt.executeUpdate(sql.toString());

			sql.setLength(0);
			sql.append("DELETE FROM group_object_privs WHERE object_type=");
			sql.append(SQL.quote(obj.getObjectType()));
			sql.append(" AND object_name=").append(SQL.quote(obj.getObjectName()));
			stmt.executeUpdate(sql.toString());
			
			EmailNotification.deleteDatabaseObject(con, sm, obj);

			if (obj.getObjectType().equals("TRANSACTION")) {
				sql.setLength(0);
				sql.append("DELETE FROM pl_transform_stats");
				sql.append(" WHERE trans_id=").append(SQL.quote(obj.getObjectName()));
				stmt.executeUpdate(sql.toString());
			}

			sql.setLength(0);
			sql.append("DELETE FROM pl_stats WHERE object_type=");
			sql.append(SQL.quote(obj.getObjectType()));
			sql.append(" AND object_name=").append(SQL.quote(obj.getObjectName()));
			stmt.executeUpdate(sql.toString());
			
		} finally {
			if (stmt != null) {
				stmt.close();
			}
		}
	}

	/**
	 * Call this method to rename everything security-ish about a
	 * database object.  It will not magically move the object's own
	 * data, but it will re-point the necessary rows in the following
	 * tables:
	 * 
	 * <ul>
	 *  <li>USER_OBJECT_PRIVS
	 *  <li>GROUP_OBJECT_PRIVS
	 *  <li>PL_USER_NOTIFICATION_LOG (via EmailNotification class)
	 *  <li>PL_USER_NOTIFICATION (via EmailNotification class)
	 *  <li>PL_TRANSFORM_STATS (only for TRANSACTION type objects)
	 *  <li>PL_STATS
	 * </ul>
	 *
	 * <p>It is expected that the given connection will <b>not</b> be
	 * in autocommit mode, so you can roll back if something dies.
	 *
	 * <p>SECURITY REQUIREMENT: sm must allow MODIFY permission on obj.
	 *
	 * @param con An open connection to the database in question.
	 * Should not be in autocommit mode, but this is not enforced.
	 * @param sm A security manager that allows modification of the object in question.
	 * @param obj The database object we're in the process of
	 * renaming.  It is <i>vital</i> that obj.getObjectName() still
	 * returns the object's original name!
	 * @param newName The new name that the object will be getting.
	 */
	public static void renameDatabaseObject(Connection con, PLSecurityManager sm,
											DatabaseObject obj, String newName)
		throws PLSecurityException, SQLException {

		sm.checkModify(con, obj);

		Statement stmt = null;
		try {

			// this must come first, or the user will no longer have permission!
			EmailNotification.renameDatabaseObject(con, sm, obj, newName);

			stmt = con.createStatement();

			StringBuffer sql = new StringBuffer();
			sql.append("UPDATE user_object_privs SET object_name=").append(SQL.quote(newName));
			sql.append(" WHERE object_type=").append(SQL.quote(obj.getObjectType()));
			sql.append(" AND object_name=").append(SQL.quote(obj.getObjectName()));
			stmt.executeUpdate(sql.toString());

			sql.setLength(0);
			sql.append("UPDATE group_object_privs SET object_name=").append(SQL.quote(newName));
			sql.append(" WHERE object_type=").append(SQL.quote(obj.getObjectType()));
			sql.append(" AND object_name=").append(SQL.quote(obj.getObjectName()));
			stmt.executeUpdate(sql.toString());
			
			if (obj.getObjectType().equals("TRANSACTION")) {
				sql.setLength(0);
				sql.append("UPDATE pl_transform_stats SET trans_id=").append(SQL.quote(newName));
				sql.append(" WHERE trans_id=").append(SQL.quote(obj.getObjectName()));
				stmt.executeUpdate(sql.toString());
			}

			sql.setLength(0);
			sql.append("UPDATE pl_stats SET object_name=").append(SQL.quote(newName));
			sql.append(" WHERE object_type=").append(SQL.quote(obj.getObjectType()));
			sql.append(" AND object_name=").append(SQL.quote(obj.getObjectName()));
			stmt.executeUpdate(sql.toString());
			
		} finally {
			if (stmt != null) {
				stmt.close();
			}
		}
	}

	/**
	 * Converts the permission strings (for example,
	 * MODIFY_PERMISSION) to the column names in the user_object_privs
	 * table.  Used by <code>checkUserPermission()</code> and
	 * <code>checkGroupPermission()</code>.
	 */
	protected static String permToColName(String perm) {
		if (perm.equals(MODIFY_PERMISSION)) {
			return "modify_ind";
		} else if (perm.equals(DELETE_PERMISSION)) {
			return "delete_ind";
		} else if (perm.equals(EXECUTE_PERMISSION)) {
			return "execute_ind";
		} else if (perm.equals(GRANT_PERMISSION)) {
			return "grant_ind";
		} else if (perm.equals(NOTIFY_PERMISSION)) {
			return "notify_ind";
		} else {
			throw new IllegalArgumentException("Unknown permission '"+perm+"'");
		}
	}

	/**
	 * Converts the permission strings (for example,
	 * MODIFY_PERMISSION) to the column names in the user_system_privs
	 * table.  Used by <code>checkUserPermission()</code> and
	 * <code>checkGroupPermission()</code>.
	 */
	protected static String sysPermToColName(String perm) {
		if (perm.equals(CREATE_PERMISSION)) {
			return "create_any_ind";
		} else if (perm.equals(MODIFY_PERMISSION)) {
			return "modify_any_ind";
		} else if (perm.equals(DELETE_PERMISSION)) {
			return "delete_any_ind";
		} else if (perm.equals(EXECUTE_PERMISSION)) {
			return "execute_any_ind";
		} else if (perm.equals(GRANT_PERMISSION)) {
			return "grant_any_ind";
		} else if (perm.equals(NOTIFY_PERMISSION)) {
			return "notify_any_ind";
		} else {
			throw new IllegalArgumentException("Unknown permission '"+perm+"'");
		}
	}

	/**
	 * Returns a reference to the user for whom this security manager
	 * checks permissions.
	 */
	public PLUser getPrincipal() {
		return principal;
	}

	/**
	 * Removes this security manager's reference to the principal
	 * user, thereby making it incapable of granting permissions.  All
	 * checkXXX() methods will throw a PLSecurityException with a
	 * reason code of INVALID_MANAGER after the invalidate method is
	 * called.
	 */
	public void invalidate() {
		principal = null;
	}

	/**
	 * Returns an instance of AllDatabaseObject, which represents all
	 * objects of a given type.
	 */
	public static DatabaseObject getSystemObject(String type) {
		return new AllDatabaseObject(type);
	}

	public String toString() {
		return "PLSecurityManager for "+principal;
	}

	/**
	 * A class which represents all database objects.  Used for
	 * granting and checking system permissions.  If you need one, get
	 * a reference to the singleton instance from the
	 * {@link SecurityManager.getSystemObject()} method.
	 */
	static class AllDatabaseObject implements DatabaseObject {
		protected String type = null;

		public AllDatabaseObject(String type) {
			this.type = type;
		}

		public String getObjectName() { return "ALL"; }
		public String getObjectType() { return type; }

		/**
		 * Returns true if and only if the other object is also of
		 * class AllDatabaseObject, and its type property matches.
		 */
		public boolean equals(Object other) {
			if (other instanceof AllDatabaseObject
				&& ((AllDatabaseObject) other).type.equals(type)) {
				return true;
			} else {
				return false;
			}
		}
	}
}
