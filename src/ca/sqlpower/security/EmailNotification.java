package ca.sqlpower.security;

import java.sql.*;
import java.util.*;

import ca.sqlpower.sql.*;

/**
 * The EmailNotification class answers questions about email
 * notification for users or groups on spe*cific objects, and allows
 * updating of notification preferences.
 * 
 * @author Gillian Mereweather
 * @author Jonathan Fuerth
 * @version $Id$
 */
public class EmailNotification implements java.io.Serializable {

	public final static String RUN_STATUS_SUCCESS="SUCCESS";
	public final static String RUN_STATUS_WARNING="WARNING";
	public final static String RUN_STATUS_FAILED="FAILED";

	/**
	 * Stores a pl_user_notification record in the database.  We never
	 * update (except for a rename).  You can delete/add only.
	 *
	 * <p>Security requirements: setter requires modify permission on notifyUser.
	 *
	 * @param sm The current logged-in user's security manager.
	 * @param onStatus The status on which to notify the user.  A null
	 * means to remove notification records pertaining to the given
	 * user/object combination.
	 */
    public static void setPref(Connection con,
							   PLSecurityManager sm,
							   PLUser notifyUser,
							   DatabaseObject notifyAbout,
							   String onStatus)
		throws SQLException, PLSecurityException {

		sm.checkModify(con, notifyUser);
		setPref(con, notifyUser.getUserId(), true, notifyAbout, onStatus);
	}

	/**
	 * Stores a pl_group_notification record in the database.  We
	 * never update (except for a rename).  You can delete/add only.
	 *
	 * <p>Security requirements: setter requires modify permission on notifyGroup.
	 *
	 * @param onStatus The status on which to notify the user.  A null
	 * means to remove notification records pertaining to the given
	 * user/object combination.
	 */
    public static void setPref(Connection con,
							   PLSecurityManager sm,
							   PLGroup notifyGroup,
							   DatabaseObject notifyAbout,
							   String onStatus)
		throws SQLException, PLSecurityException {

		sm.checkModify(con, notifyGroup);
		setPref(con, notifyGroup.getGroupName(), false, notifyAbout, onStatus);
	}

	/**
	 * Used by the other setPref methods.  Doesn't do a security
	 * check, so you can't call it from outside.
	 *
	 * @param nameIsUser True if this is for user notifications; false
	 * if this is for group notifications.
	 */
	protected static void setPref(Connection con,
								  String notifyName,
								  boolean nameIsUser,
								  DatabaseObject notifyAbout,
								  String onStatus)
		throws SQLException {
		// XXX: may need to check if notifyUser already has a
		// notification preference on notifyAbout, and switch to update.

        Statement stmt = null;
		try {
			StringBuffer sql=new StringBuffer();

			if (onStatus != null) {
				if (nameIsUser) {
					sql.append("INSERT INTO pl_user_notification(user_id,");
				} else {
					sql.append("INSERT INTO pl_group_notification(group_name,");
				}
				sql.append(" object_type, object_name, run_status, last_update_os_user)");
				sql.append(" VALUES( ");
				sql.append(SQL.quote(notifyName)).append(",");
				sql.append(SQL.quote(notifyAbout.getObjectType())).append(",");
				sql.append(SQL.quote(notifyAbout.getObjectName())).append(",");
				sql.append(SQL.quote(onStatus)).append(",");
				sql.append("'Power*Dashboard Web Facility')");
			} else {
				if (nameIsUser) {
					sql.append("DELETE FROM pl_user_notification WHERE user_id=");
				} else {
					sql.append("DELETE FROM pl_group_notification WHERE group_name=");
				}
				sql.append(SQL.quote(notifyName));
				sql.append(" AND object_type=").append(SQL.quote(notifyAbout.getObjectType()));
				sql.append(" AND object_name=").append(SQL.quote(notifyAbout.getObjectName()));
			}

			stmt = con.createStatement();
			stmt.executeUpdate(sql.toString());
		} finally {
			if(stmt != null) {
				stmt.close();
			}
		}
	}

    /**
	 * Returns the email notification preferences for the given user
	 * on the given object.  Does nothing about groups that this user
	 * may belong to.
	 */
	public static boolean checkUserStatus(Connection con, 
									  PLUser user,
									  DatabaseObject dbObj,
									  String runStatus) 
	throws SQLException {
		return checkStatus(con, user.getUserId(), true, dbObj, runStatus);
	}

    /**
	 * Returns the email notification preferences for the given user
	 * on the given object, including the additional checks for the
	 * groups the user belongs to.
	 */
	public static boolean checkStatus(Connection con, 
									  PLUser user,
									  DatabaseObject dbObj,
									  String runStatus) 
	throws SQLException {
		if (checkStatus(con, user.getUserId(), true, dbObj, runStatus)) {
			return true;
		}
		Iterator groups = user.getGroups(con).iterator();
		while (groups.hasNext()) {
			PLGroup g = (PLGroup) groups.next();
			if (checkStatus(con, g.getGroupName(), false, dbObj, runStatus)) {
				return true;
			}
		}
		return false;
	}

    /**
	 * Returns the email notification preferences for the given group
	 * on the given object.
	 */
	public static boolean checkStatus(Connection con, 
									  PLGroup group,
									  DatabaseObject dbObj,
									  String runStatus) 
	throws SQLException {
		return checkStatus(con, group.getGroupName(), false, dbObj, runStatus);
	}

	/**
	 * Used internally by the user and group versions of checkStatus.
	 */
	protected static boolean checkStatus(Connection con,
										 String name,
										 boolean nameIsUser,
										 DatabaseObject dbObj,
										 String runStatus) throws SQLException {
        Statement stmt = null;
        try {
			StringBuffer sql = new StringBuffer();

			sql.append("SELECT run_status");

			if(nameIsUser){
				sql.append(" FROM pl_user_notification");
				sql.append(" WHERE user_id=").append(SQL.quote(name));
			} else {
				sql.append(" FROM pl_group_notification");
				sql.append(" WHERE group_name=").append(SQL.quote(name));
			}
			sql.append(" AND object_type=").append(SQL.quote(dbObj.getObjectType()));
			sql.append(" AND object_name=").append(SQL.quote(dbObj.getObjectName()));

            stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery(sql.toString());
			while(rs.next()) {
				String prefStatus = rs.getString(1);
				if (prefStatus != null && prefStatus.equals(runStatus)) {
					return true;
				}
			}

			// There wasn't a matching entry in the table
            return false;

        } finally {
            if(stmt != null) {
                stmt.close();
            }
        }
    }

	/**
	 * This method is normally called from {@link
	 * PLSecurityManager#deleteDatabaseObject}, but you can call it
	 * directly if you want.  It removes everything email
	 * notification-ish about a database object.  It will not
	 * magically remove the object's own data, but it will zap all the
	 * necessary rows from the following:
	 * 
	 * <ul>
	 *  <li>PL_USER_NOTIFICATION_LOG
	 *  <li>PL_USER_NOTIFICATION
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
			sql.append("DELETE FROM pl_user_notification WHERE object_type=");
			sql.append(SQL.quote(obj.getObjectType()));
			sql.append(" AND object_name=").append(SQL.quote(obj.getObjectName()));
			stmt.executeUpdate(sql.toString());

			sql.setLength(0);
			sql.append("DELETE FROM pl_user_notification_log WHERE object_type=");
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
	 * This method is normally called from {@link
	 * PLSecurityManager#renameDatabaseObject}, but you can call it
	 * directly if you want.  It renames (points to a new object name)
	 * everything email notification-ish about a database object.  It
	 * will not magically move the object's own data, but it will
	 * repoint all the necessary rows from the following:
	 * 
	 * <ul>
	 *  <li>PL_USER_NOTIFICATION_LOG
	 *  <li>PL_USER_NOTIFICATION
	 * </ul>
	 *
	 * <p>It is expected that the given connection will <b>not</b> be
	 * in autocommit mode.
	 *
	 * <p>SECURITY REQUIREMENT: sm must allow MODIFY permission on obj.
	 *
	 * @param con An open connection to the database in question.
	 * Should not be in autocommit mode, but this is not enforced.
	 * @param sm A security manager that allows deletion of the object in question.
	 * @param obj The database object we're in the process of
	 * renaming.  obj.getObjectName() must return the old object name!
	 * @param newName the new name we are assigning to the object.
	 */
	public static void renameDatabaseObject(Connection con, PLSecurityManager sm,
											DatabaseObject obj, String newName)
		throws PLSecurityException, SQLException {

		sm.checkModify(con, obj);

		Statement stmt = null;
		try {
			stmt = con.createStatement();

			StringBuffer sql = new StringBuffer();
			sql.append("UPDATE pl_user_notification SET object_name=").append(SQL.quote(newName));
			sql.append(" WHERE object_type=").append(SQL.quote(obj.getObjectType()));
			sql.append(" AND object_name=").append(SQL.quote(obj.getObjectName()));
			stmt.executeUpdate(sql.toString());

			sql.setLength(0);
			sql.append("UPDATE pl_user_notification_log SET object_name=").append(SQL.quote(newName));
			sql.append(" WHERE object_type=").append(SQL.quote(obj.getObjectType()));
			sql.append(" AND object_name=").append(SQL.quote(obj.getObjectName()));
			stmt.executeUpdate(sql.toString());

			sql.setLength(0);
			sql.append("UPDATE pl_group_notification SET object_name=").append(SQL.quote(newName));
			sql.append(" WHERE object_type=").append(SQL.quote(obj.getObjectType()));
			sql.append(" AND object_name=").append(SQL.quote(obj.getObjectName()));
			stmt.executeUpdate(sql.toString());

		} finally {
			if (stmt != null) {
				stmt.close();
			}
		}
	}
}
