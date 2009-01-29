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
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import ca.sqlpower.sql.DBConnection;
import ca.sqlpower.sql.DatabaseObject;
import ca.sqlpower.sql.SQL;

/**
 * The PLGroup class represents groups that can be granted specific
 * permissions, and whose member users inherit those permissions.
 * 
 * @author Gillian Mereweather
 * @author Jonathan Fuerth
 * @version $Id$
 */
public class PLGroup implements DatabaseObject, java.io.Serializable {
    protected boolean _alreadyInDatabase;
    protected String groupName;
    protected String groupDesc;
    protected java.sql.Date lastUpdateDate;
    protected String lastUpdateUser;
	protected String lastUpdateOsUser;
	protected java.sql.Date createDate;

	public final static String ADMIN_GROUP="PL_ADMIN";
	public final static String OMNISCIENT_GROUP_NAME="PL_SUPERUSER";

	/**
	 * Creates a new group object with all-null attributes.
	 */
    protected PLGroup() {
		super();
        _alreadyInDatabase=false;
    }

	/**
	 * Creates a new group object with the given name and otherwise
	 * null attributes.
	 */
	public PLGroup(String groupName) {
		this();
		this.groupName = groupName;
	}

	/**
	 * Right now, we don't insert records in pl_group through the Web
	 * facilities - only update THE INSERT HAS NOT BEEN TESTED
	 */
    public void storeNoCommit(Connection con) throws SQLException {
        Statement stmt=null;
			
		stmt=con.createStatement();
		StringBuffer sql=new StringBuffer();
		if(_alreadyInDatabase) {
			sql.append("UPDATE pl_group");
			sql.append(" SET group_desc=").append(SQL.quote(getGroupDesc()));
			sql.append(", last_update_date=").append(DBConnection.getSystemDate(con));
			sql.append(", last_update_user=").append(SQL.quote(DBConnection.getUser(con).toUpperCase()));
			sql.append(", last_update_os_user='Power*Dashboard Web Frontend'");
			sql.append(" WHERE group_name = ").append(SQL.quote(getGroupName()));
			stmt.executeUpdate(sql.toString());
		} else {
			sql.append("INSERT INTO pl_group(");
			sql.append(" group_name, group_desc, last_update_date,");
			sql.append(" last_update_user, last_update_os_user, create_date)");
			sql.append(" VALUES( ");
			sql.append(SQL.quote(groupName.toUpperCase())).append(",");
			sql.append(SQL.quote(groupDesc)).append(",");
			sql.append(DBConnection.getSystemDate(con)).append(",");
			sql.append(SQL.quote(DBConnection.getUser(con).toUpperCase())).append(",");
			sql.append("'Power*Dashboard Web Frontend'").append(",");
			sql.append(DBConnection.getSystemDate(con)).append(")");
			
			stmt.executeUpdate(sql.toString());

			// Create privileges for the new group
			sql.setLength(0);
			sql.append("INSERT INTO group_object_privs(");
			sql.append("group_name,");
			sql.append("object_type, object_name,");
			sql.append("modify_ind, delete_ind, execute_ind, grant_ind,");
			sql.append("last_update_date, last_update_user)");
			sql.append(" VALUES(");
			sql.append(SQL.quote(groupName.toUpperCase())).append(",");
			sql.append("'GROUP', ").append(SQL.quote(groupName).toUpperCase()).append(",");
			sql.append("'Y', 'Y', 'Y', 'Y',");
			sql.append(DBConnection.getSystemDate(con)).append(",");
			sql.append(SQL.quote(DBConnection.getUser(con).toUpperCase())).append(")");
			
			stmt.executeUpdate(sql.toString());

			_alreadyInDatabase=true;
		}
	    
		if(stmt != null) {
			stmt.close();
		}
	}

	/**
	 * This is a convenience method for calling storeNoCommit().
	 */
    public void store(Connection con) throws SQLException {
		boolean oldACVal=con.getAutoCommit();
        try {
			con.setAutoCommit(false);
			storeNoCommit(con);
			con.commit();
		} catch(SQLException e) {
			System.out.println("PLGroup: caught "+e);
			e.printStackTrace();
			con.rollback();
			throw e;
		} finally {
			con.setAutoCommit(oldACVal);
		}
	}

    /**
	 * Loads the named group from the database
	 */
	public static PLGroup findByPrimaryKey(Connection con, 
										   String groupName)
		throws SQLException {

		List oneGroup = find(con, groupName, null, false, null);
		return (PLGroup) oneGroup.get(0);
    }

	/**
	 * Gives back a List of PLGroup objects that the given user
	 * belongs to.
	 *
	 * @see PLUser.getGroups
	 */
	public static List findByUser(Connection con, PLUser user) throws SQLException {
		return find(con, null, user.getUserId(), false, user.getGroupNameFilter());
	}

	/**
	 * Returns a list of PLGroup objects which represent all groups in
	 * the given database.
	 */
	public static List findAll(Connection con) throws SQLException {
		return find(con, null, null, false, null);
	}

	/**
	 * Returns the list of PLGroup objects whose group name starts
	 * with the given string.
	 */
	public static List findByPrefix(Connection con, String nameStartsWith)
		throws SQLException {
		return find(con, nameStartsWith, null, true, null);
	}

	/**
	 * Retrieves a list of PLGroup objects from the database.  Lookup
	 * can be based on group name (which will give at most one record
	 * for non-prefix searches), user name (which will give all groups
	 * that the given user belongs to), or neither (gives all groups),
	 * but not both simultaneously.
	 *
	 * @param filter If non-null, the returned list of groups will be
	 * restricted to those named in this set.  All members of the set
	 * must be of type String.  You can only use a non-null filter if
	 * you specify a non-null userName.  (which implies a required
	 * null groupName).
	 */
	protected static List find(Connection con, String groupName, String userName,
							   boolean searchByPrefix, Set filter)
	throws SQLException {

		if (groupName != null && userName != null) {
			throw new IllegalArgumentException
				("Cannot specify both userName and groupName");
		}
		
		if (userName == null && filter != null) {
			throw new IllegalArgumentException
				("filter is not allowed unless non-null userName is specified");
		}

		List results = new LinkedList();
        Statement stmt = null;
        try {
			StringBuffer sql = new StringBuffer(500);
			sql.append("SELECT g.group_name, g.group_desc, g.last_update_date,");
			sql.append(" g.last_update_user, g.last_update_os_user, g.create_date");
			if (groupName != null) {
				sql.append(" FROM pl_group g");
				if (searchByPrefix) {
					sql.append(" WHERE g.group_name LIKE ").append(SQL.quote(groupName+"%"));
				} else {
					sql.append(" WHERE g.group_name=").append(SQL.quote(groupName));
				}
			} else if (userName != null) {
				sql.append(" FROM pl_group g, user_group ug");
				sql.append(" WHERE g.group_name = ug.group_name");
				if (searchByPrefix) {
					sql.append(" AND ug.user_id LIKE ").append(SQL.quote(userName+"%"));
				} else {
					sql.append(" AND ug.user_id=").append(SQL.quote(userName));
				}
				if (filter != null) {
					sql.append(" AND g.group_name IN(")
						.append(SQL.quoteCollection(filter))
						.append(")");
				}
			} else {
				sql.append(" FROM pl_group g");
			}
			sql.append(" ORDER BY g.group_name");

            stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery(sql.toString());

			boolean hasRows = rs.next();
            if( (!hasRows) && (groupName != null) ) { 
				throw new IllegalArgumentException("No such group '"+groupName+"'");
			}

			if (!hasRows) {
				return Collections.EMPTY_LIST;
			}

			// This is a do..while becuase the check above calls rs.next
			do {
				PLGroup newBean = new PLGroup();
				newBean.groupName = rs.getString("group_name");
				newBean.groupDesc = rs.getString("group_desc");
				newBean.lastUpdateDate = rs.getDate("last_update_date");
				newBean.lastUpdateUser = rs.getString("last_update_user");
				newBean.lastUpdateOsUser = rs.getString("last_update_os_user");
				newBean.createDate = rs.getDate("create_date");
				newBean._alreadyInDatabase = true;

				results.add(newBean);
			} while(rs.next());

        } finally {
            if(stmt != null) {
                stmt.close();
            }
        }

		if (groupName != null && !searchByPrefix && results.size() > 1) {
			throw new IllegalStateException("Got more than one result for group "+groupName);
		}

		return results;
	}
	
	/**
	 * Returns a list of Strings which enumerate all user id's
	 * belonging to this group.
	 */
	public List getMemberNames(Connection con) throws SQLException {
		StringBuffer sql = new StringBuffer(100);
		sql.append("SELECT user_id FROM user_group");
		sql.append(" WHERE group_name=").append(SQL.quote(getGroupName()));

		Statement stmt = null;

		try {
			stmt = con.createStatement();
			ResultSet rs = stmt.executeQuery(sql.toString());
			List members = new LinkedList();
			while (rs.next()) {
				members.add(rs.getString(1));
			}
			return members;
		} finally {
            if(stmt != null) {
                stmt.close();
            }
        }
	}

	/**
	 * Adds the given userid to the list of members for this group.
	 *
	 * <p>SECURITY REQUIREMENT: principal must have modify permission
	 * on this PLGroup.
	 *
	 * @param con A connection to the database in question
	 * @param secContext A security manager for the user who is performing the action.
	 * @param newUserId The name of the new user to add to this group.
	 * It is an error to specify a user who is already a member.
	 */
	public void addMember(Connection con, PLSecurityManager secContext, String newUserId)
		throws SQLException, PLSecurityException {
		
		secContext.checkModify(con, this);

		Statement stmt = null;
		try {
			StringBuffer sql = new StringBuffer(200);
			sql.append("INSERT INTO user_group (user_id");
			sql.append(", group_name, last_update_date, last_update_user");
			sql.append(", last_update_os_user, create_date");
			sql.append(") VALUES (").append(SQL.quote(newUserId));
			sql.append(", ").append(SQL.quote(getGroupName()));
			sql.append(", ").append(DBConnection.getSystemDate(con));
			sql.append(", ").append(SQL.quote(secContext.getPrincipal().getUserName()));
			sql.append(", 'Power*Dashboard Web Facility'");
			sql.append(", ").append(DBConnection.getSystemDate(con));
			sql.append(")");
			
			stmt = con.createStatement();
			int updateCount = stmt.executeUpdate(sql.toString());

			if (updateCount != 1) {
				throw new IllegalStateException("Insert statement affected "+updateCount
												+" rows; should have been 1");
			}
		} finally {
			if (stmt != null) {
				stmt.close();
			}
		}
	}

	/**
	 * Removes the given userid from the list of members for this group.
	 *
	 * <p>SECURITY REQUIREMENT: principal must have modify permission
	 * on this PLGroup.
	 *
	 * @param con A connection to the database in question
	 * @param secContext A security manager for the user who is performing the action.
	 * @param newUserId The name of the user remove from this group.
	 * It is an error to specify a user who is not already a member.
	 * A value of null means to delete all users.
	 */
	public void removeMember(Connection con, PLSecurityManager secContext, String removeUserId)
		throws SQLException, PLSecurityException {
		
		secContext.checkModify(con, this);

		Statement stmt = null;
		try {
			StringBuffer sql = new StringBuffer(100);
			sql.append("DELETE FROM user_group");
			sql.append(" WHERE group_name=").append(SQL.quote(getGroupName()));
			if (removeUserId != null) {
				sql.append(" AND user_id=").append(SQL.quote(removeUserId));
			}

			stmt = con.createStatement();
			int updateCount = stmt.executeUpdate(sql.toString());

			if (removeUserId != null && updateCount != 1) {
				throw new IllegalStateException("Delete statement for group "+getGroupName()
												+" and user "+removeUserId
												+" affected "+updateCount
												+" rows; should have been 1");
			}
		} finally {
			if (stmt != null) {
				stmt.close();
			}
		}
	}

	/**
	 * Returns true if this group is the special superuser group,
	 * PL_ADMIN.
	 */
	public boolean isAdminGroup() {
		return _alreadyInDatabase && groupName.equals(ADMIN_GROUP);
	}

	/**
	 * Gets the value of groupName
	 *
	 * @return the value of groupName
	 */
	public String getGroupName() {
		return this.groupName;
	}

	/**
	 * Sets the value of groupName
	 *
	 * @param argGroupName Value to assign to this.groupName
	 */
	public void setGroupName(String argGroupName){
		this.groupName = argGroupName;
	}

	/**
	 * Gets the value of groupDesc
	 *
	 * @return the value of groupDesc
	 */
	public String getGroupDesc() {
		return this.groupDesc;
	}

	/**
	 * Sets the value of groupDesc
	 *
	 * @param argGroupDesc Value to assign to this.groupDesc
	 */
	public void setGroupDesc(String argGroupDesc){
		this.groupDesc = argGroupDesc;
	}

	/**
	 * Gets the value of lastUpdateDate
	 *
	 * @return the value of lastUpdateDate
	 */
	public java.sql.Date getLastUpdateDate() {
		return this.lastUpdateDate;
	}

	/**
	 * Gets the value of lastUpdateUser
	 *
	 * @return the value of lastUpdateUser
	 */
	public String getLastUpdateUser() {
		return this.lastUpdateUser;
	}

	/**
	 * Gets the value of lastUpdateOsUser
	 *
	 * @return the value of lastUpdateOsUser
	 */
	public String getLastUpdateOsUser() {
		return this.lastUpdateOsUser;
	}

	/**
	 * Gets the value of createDate
	 *
	 * @return the value of createDate
	 */
	public java.sql.Date getCreateDate() {
		return this.createDate;
	}

	/**
	 * For the DatabaseObject interface.
	 *
	 * @return The string "USER"
	 */
	public String getObjectType() {
		return "GROUP";
	}

	/**
	 * For the DatabaseObject interface.
	 *
	 * @return This group's name.
	 */
	public String getObjectName() {
		return getGroupName();
	}

	public String toString() {
		StringBuffer meString=new StringBuffer();
		meString.append("[Group: ");
		meString.append("groupName=").append(groupName).append(", ");
		meString.append("groupDesc=").append(groupDesc).append(", ");
		meString.append("lastUpdateDate=").append(lastUpdateDate).append(", ");
		meString.append("lastUpdateUser=").append(lastUpdateUser).append(", ");
		meString.append("lastUpdateOsUser=").append(lastUpdateOsUser).append(", ");
		meString.append("createDate=").append(createDate).append("]");
		return meString.toString();
	}
}
