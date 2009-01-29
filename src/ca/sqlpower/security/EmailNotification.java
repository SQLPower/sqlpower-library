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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import ca.sqlpower.sql.DatabaseObject;
import ca.sqlpower.sql.SQL;

/**
 * The EmailNotification class answers questions about email
 * notification for users or groups on specific objects, and allows
 * updating of notification preferences.
 * 
 * @author Gillian Mereweather
 * @author Jonathan Fuerth
 * @version $Id$
 */
public class EmailNotification implements java.io.Serializable {

	/**
	 * The name of the column indicating whether to send emails at red status
	 * on either pl_user_notification or pl_group_notification.
	 */
	public static final String RED_STATUS = "EMAIL_RED_IND";
	
	/**
	 * The name of the column indicating whether to send emails at yellow status
	 * on either pl_user_notification or pl_group_notification.
	 */
	public static final String YELLOW_STATUS = "EMAIL_YELLOW_IND";
	
	/**
	 * The name of the column indicating whether to send emails at green status
	 * on either pl_user_notification or pl_group_notification.
	 */
	public static final String GREEN_STATUS = "EMAIL_GREEN_IND";

	/**
	 * Stores a pl_user_notification record in the database.  
	 * This could update an existing record or insert a new one.
	 *
	 * <p>Security requirements: setter requires modify permission on notifyUser.
	 *
	 * @param sm The current logged-in user's security manager.
	 */
	public static void setPref(Connection con,
			PLSecurityManager sm,
			PLUser notifyUser,
			DatabaseObject notifyAbout,
			String viewKpi,
			String emailRed,
			String emailYellow,
			String emailGreen)
	throws SQLException, PLSecurityException {

		sm.checkModify(con, notifyUser);
		setPref(con, notifyUser.getUserId(), true, notifyAbout, 
				viewKpi, emailRed, emailYellow, emailGreen);
	}

	/**
	 * Stores a pl_group_notification record in the database.  
	 * This may update an existing record or insert a new one.
	 *
	 * <p>Security requirements: setter requires modify permission on notifyGroup.
	 *
	 * @param sm The current logged-in user's security manager.
	 */
	public static void setPref(Connection con,
			PLSecurityManager sm,
			PLGroup notifyGroup,
			DatabaseObject notifyAbout,
			String viewKpi,
			String emailRed,
			String emailYellow,
			String emailGreen)
	throws SQLException, PLSecurityException {

		sm.checkModify(con, notifyGroup);
		setPref(con, notifyGroup.getGroupName(), false, notifyAbout, 
				viewKpi, emailRed, emailYellow, emailGreen);
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
			String viewKpi,
			String emailRed,
			String emailYellow,
			String emailGreen)
	throws SQLException {

		Statement stmt = null;
		ResultSet rs = null;
		boolean bFirst=true;
		try {
			stmt = con.createStatement();
			StringBuffer sql=new StringBuffer();

			sql.setLength(0);
			if (nameIsUser) {
				sql.append("SELECT count(*) FROM pl_user_notification");
				sql.append(" WHERE user_id=");
			} else {
				sql.append("SELECT count(*) FROM pl_group_notification");
				sql.append(" WHERE group_name=");
			}
			sql.append(SQL.quote(notifyName));
			sql.append(" AND object_type=").append(SQL.quote(notifyAbout.getObjectType()));
			sql.append(" AND object_name=").append(SQL.quote(notifyAbout.getObjectName()));

			rs = stmt.executeQuery(sql.toString());

			sql.setLength(0);

			// If there is already a row, update the flags
			if (rs.next()) {
				int rowCount = rs.getInt(1);

				/* If the record exists */
				if (rowCount > 0) {
					if (nameIsUser) {
						sql.append("UPDATE pl_user_notification");
					} else {
						sql.append("UPDATE pl_group_notification");
					}
					sql.append(" SET");

					if(!viewKpi.equals("")){
						sql.append(" view_kpi_ind=").append(SQL.quote(viewKpi));
						bFirst=false;
					}
					if(!emailRed.equals("")){
						if(!bFirst){
							sql.append(",");
						}
						sql.append(" email_red_ind=").append(SQL.quote(emailRed));
						bFirst=false;
					}
					if(!emailYellow.equals("")){
						if(!bFirst){
							sql.append(",");
						}
						sql.append(" email_yellow_ind=").append(SQL.quote(emailYellow));
						bFirst=false;
					}
					if(!emailGreen.equals("")){
						if(!bFirst){
							sql.append(",");
						}
						sql.append(" email_green_ind=").append(SQL.quote(emailGreen));
						bFirst=false;
					}

					if (nameIsUser) {
						sql.append(" WHERE user_id=");
					} else {
						sql.append(" WHERE group_name=");
					}
					sql.append(SQL.quote(notifyName));
					sql.append(" AND object_type=").append(SQL.quote(notifyAbout.getObjectType()));
					sql.append(" AND object_name=").append(SQL.quote(notifyAbout.getObjectName()));

					// The row does not exist - insert a record
				} else {
					if (nameIsUser) {
						sql.append("INSERT INTO pl_user_notification(user_id,");
					} else {
						sql.append("INSERT INTO pl_group_notification(group_name,");
					}
					sql.append(" object_type, object_name, view_kpi_ind,");
					sql.append(" email_red_ind, email_yellow_ind, email_green_ind)");
					sql.append(" VALUES( ");
					sql.append(SQL.quote(notifyName)).append(",");
					sql.append(SQL.quote(notifyAbout.getObjectType())).append(",");
					sql.append(SQL.quote(notifyAbout.getObjectName())).append(",");
					sql.append(SQL.quote(viewKpi)).append(",");
					sql.append(SQL.quote(emailRed)).append(",");
					sql.append(SQL.quote(emailYellow)).append(",");
					sql.append(SQL.quote(emailGreen)).append(")");
				} // end if (the record exists)
			} // end if (the rs has a value)

			stmt.executeUpdate(sql.toString());
		} finally {
			if (rs != null) {
				rs.close();
			}
			if(stmt != null) {
				stmt.close();
			}
		}
	}

	/**
	 * This checks the email setting of a given status for a user,
	 * with associating groups in addition. The class constants
	 * should be used to identify status. 
	 */
	public static boolean checkUserWithGroupEmailSetting(Connection con, 
			PLUser user, DatabaseObject dbObj, String statusType) throws SQLException {
		if (checkEmailSetting(con, user.getUserId(), true, dbObj, statusType)) {
			return true;
		}
		for (Object g : user.getGroups(con)) {
			PLGroup group = (PLGroup) g;
			if (checkEmailSetting(con, group.getGroupName(), false, dbObj, statusType)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * This checks the email setting of a given status for a user,
	 * without associating groups. The class constants should be
	 * used to identify status. 
	 */
	public static boolean checkUserEmailSetting(Connection con,
			PLUser user, DatabaseObject dbObj,
			String statusType) throws SQLException {
		return checkEmailSetting(con, user.getUserId(), true, dbObj, statusType);
	}
	
	/**
	 * This checks the email setting of a given status for a group.
	 * The class constants should be used to identify status. 
	 */
	public static boolean checkGroupEmailSetting(Connection con,
			PLGroup group, DatabaseObject dbObj,
			String statusType) throws SQLException {
		return checkEmailSetting(con, group.getGroupName(), false, dbObj, statusType);
	}
	
	/**
	 * Internally called class that checks the email setting of
	 * a given status for the given user or group. 
	 */
	private static boolean checkEmailSetting(Connection con,
			String name, boolean isUser, DatabaseObject dbObj,
			String statusType) throws SQLException {
		Statement stmt = null;
		try {
			StringBuffer sql = new StringBuffer();
			
			sql.append("SELECT " + statusType + " FROM");
			
			if (isUser) {
				sql.append(" pl_user_notification WHERE user_id = " + SQL.quote(name));
			} else {
				sql.append(" pl_group_notification WHERE group_name = " + SQL.quote(name));
			}
			
			sql.append(" object_type = " + SQL.quote(dbObj.getObjectType()));
			sql.append(" AND object_name = " + SQL.quote(dbObj.getObjectName()));
			
			stmt = con.createStatement();
			ResultSet rs = stmt.executeQuery(sql.toString());
			while(rs.next()) {
				return (rs.getString(1).equals("Y"));
			}
			return false;
		} finally {
			if(stmt != null) {
				stmt.close();
			}
		}
	}

	/**
	 * Checks the latest run_status of a given {@link DatabaseObject}
	 * from the pl_stats table.   
	 */
	public static String checkDBObjStatus(Connection con,
			DatabaseObject dbObj) throws SQLException {
		Statement stmt = null;
		try {
			StringBuffer sql = new StringBuffer();

			sql.append("SELECT run_status FROM pl_status WHERE");
			sql.append(" object_type = " + SQL.quote(dbObj.getObjectType()));
			sql.append(" AND object_name = " + SQL.quote(dbObj.getObjectName()));
			sql.append("  ORDER BY start_date_time DESC");

			stmt = con.createStatement();
			ResultSet rs = stmt.executeQuery(sql.toString());
			while(rs.next()) {
				String prefStatus = rs.getString(1);
				if (prefStatus != null) {
					return prefStatus;
				}
			}

			// There wasn't a matching entry in the table
			return null;

		} finally {
			if(stmt != null) {
				stmt.close();
			}
		}
	}
	
	/**
	 * Returns a list of EmailRecipients containing the email and 
	 * name of users who are indicated for receiving emails of given
	 * status of given DatabaseObject. The class constants should be
	 * used for status. 
	 */
	public static List<EmailRecipient> findEmailRecipients(Connection con, DatabaseObject dbObj,
			String statusType) throws SQLException {
		List<EmailRecipient> emailRecipients = new ArrayList<EmailRecipient>();
		
        Statement stmt = null;
        try {
			StringBuffer sql = new StringBuffer();
			sql.append("SELECT user_name, email_address FROM pl_user WHERE user_id in");
			sql.append(" (SELECT user_id FROM pl_user_notification WHERE");
			sql.append(" " + statusType + " = " + SQL.quote("Y"));
			sql.append("  AND object_type = " + SQL.quote(dbObj.getObjectType()));
			sql.append("  AND object_name = " + SQL.quote(dbObj.getObjectName()) + ")");

            stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery(sql.toString());
			while (rs.next()) {
				EmailRecipient er = new EmailRecipient(rs.getString(1), rs.getString(2));
				emailRecipients.add(er);
			}
			
			sql.setLength(0);
			sql.append("SELECT user_name, email_address FROM pl_user WHERE user_id in");
			sql.append(" (SELECT user_id FROM user_group WHERE group_name in");
			sql.append("  (SELECT group_name FROM pl_group_notification WHERE");
			sql.append("   " + statusType + " = " + SQL.quote("Y"));
			sql.append("   AND object_type = " + SQL.quote(dbObj.getObjectType()));
			sql.append("   AND object_name = " + SQL.quote(dbObj.getObjectName()) + "))");
			
			rs = stmt.executeQuery(sql.toString());
			while (rs.next()) {
				EmailRecipient er = new EmailRecipient(rs.getString(1), rs.getString(2));
				if (!emailRecipients.contains(er)) {
					emailRecipients.add(er);
				}
			}
        } finally {
            if(stmt != null) {
                stmt.close();
            }
        }
		
		return emailRecipients;
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

			sql.setLength(0);
			sql.append("DELETE FROM pl_group_notification");
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
	 *  <li>PL_GROUP_NOTIFICATION
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
	
	
	/**
	 * A simple inner class to hold the name and email of an
	 * user that has been indicated as an email recipient
	 */
	public static class EmailRecipient {
		
		private String name;
		private String email;
		
		public EmailRecipient(String name, String email) {
			this.name = name;
			this.email = email;
		}
		
		public String getName() {
			return name;
		}
		
		public void setName(String name) {
			this.name = name;
		}
		
		public String getEmail() {
			return email;
		}
		
		public void setEmail(String email) {
			this.email = email;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj instanceof EmailRecipient) {
				EmailRecipient other = (EmailRecipient) obj;
				boolean nameEquals = (name == null? other.name == null : name.equals(other.name));
				if (!nameEquals) {
					return false;
				} else {
					return (email == null? other.email == null : email.equals(other.email));
				}
			} else {
				return false;
			}
		}
		
		@Override
		public int hashCode() {		
			int result = 17;
			result = 31 * result + email.hashCode();
			result = 31 * result + name.hashCode();
			return result;			
		}
		
		@Override
		public String toString() {
			return "<" + this.name + "> " + this.email;
		}
	}
}

