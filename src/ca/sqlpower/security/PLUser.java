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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import ca.sqlpower.sql.DBConnection;
import ca.sqlpower.sql.DatabaseObject;
import ca.sqlpower.sql.SQL;
import ca.sqlpower.util.ByteColonFormat;
import edu.umd.cs.findbugs.annotations.SuppressWarnings;

/**
 * The User class represents a people who can log in to an application
 * which uses the PL schema.
 * 
 * @author Gillian Mereweather
 * @author Jonathan Fuerth
 * @version $Id$
 */
public class PLUser implements DatabaseObject, java.io.Serializable {

	private static final Logger logger = Logger.getLogger(PLUser.class);

    protected boolean _alreadyInDatabase;
	protected Set groupNameFilter;
    protected String userId;
	protected String password;
    protected String userName;
	protected String emailAddress;
    protected java.sql.Date lastUpdateDate;
    protected String lastUpdateUser;
    protected String lastUpdateOsUser;
	protected Boolean omniscient;
	protected Boolean superuser;
	protected boolean loaderUser;
	protected boolean summarizerUser;
	protected boolean matchmakerUser;
	protected boolean dashboardUser;

	/**
	 * Creates a new user object with no user id.  For internal use
	 * only.
	 */
    protected PLUser() {
        _alreadyInDatabase=false;
		groupNameFilter = null;
        userId=null;
        userName=null;
		emailAddress=null;
        lastUpdateDate=null;
        lastUpdateUser=null;
		lastUpdateOsUser=null;
		omniscient=null;
		superuser=null;
		loaderUser=false;
		summarizerUser=false;
		matchmakerUser=false;
		dashboardUser=false;
    }

	/**
	 * Creates a new user object with the given user id.
	 */
	public PLUser(String userId) {
		this();
		this.userId = userId;
	}

	/**
	 * Stores this user back to the database. THE INSERT HAS NOT BEEN
	 * TESTED.
	 *
	 * @throws SQLException when a database error occurs.  The caller
	 * should rollback the current transaction in this case.
	 */
	@SuppressWarnings(value={"SQL_NONCONSTANT_STRING_PASSED_TO_EXECUTE"}, 
					  justification="Input values are quoted to prevent SQL injection," +
								    "and the sql statements in this do not get called repeatedly, so" +
									"changing it to a prepared statement does not add much benefit")
    public void storeNoCommit(Connection con) throws SQLException {
        Statement stmt = con.createStatement();
		StringBuffer sql = new StringBuffer();
		if(_alreadyInDatabase) {
			sql.append("UPDATE pl_user");
			sql.append(" SET user_name=").append(SQL.quote(getUserName()));
			if (password != null) {
				sql.append(", password=").append(SQL.quote(encryptPassword(password)));
			}
			sql.append(", email_address=").append(SQL.quote(getEmailAddress()));
			sql.append(", last_update_date=").append(DBConnection.getSystemDate(con));
			sql.append(", last_update_user=").append(SQL.quote(DBConnection.getUser(con).toUpperCase()));
			sql.append(", last_update_os_user=").append(SQL.quote("DASHBOARD_FRONTEND"));
			sql.append(", use_loader_ind='").append(loaderUser ? 'Y':'N').append("'");
			sql.append(", use_matchmaker_ind='").append(matchmakerUser ? 'Y':'N').append("'");
			sql.append(", use_summarizer_ind='").append(summarizerUser ? 'Y':'N').append("'");
			sql.append(", use_dashboard_ind='").append(dashboardUser ? 'Y':'N').append("'");
			sql.append(" WHERE user_id = ").append(SQL.quote(getUserId()));
			logger.debug("store query:" +sql);
			stmt.executeUpdate(sql.toString());
		} else {
			sql.append("INSERT INTO pl_user(");
			sql.append(" user_id,");
			if (password != null) {
				sql.append(" password,");
			}
			sql.append(" user_name, email_address,");
			sql.append(" last_update_date, last_update_user, last_update_os_user,");
			sql.append(" use_loader_ind, use_matchmaker_ind, use_summarizer_ind, use_dashboard_ind)");
			sql.append(" VALUES( ");
			sql.append(SQL.quote(userId.toUpperCase())).append(",");
			if (password != null) {
				sql.append(SQL.quote(encryptPassword(password))).append(", ");
			}
			sql.append(SQL.quote(userName)).append(",");
			sql.append(SQL.quote(emailAddress)).append(",");
			sql.append(DBConnection.getSystemDate(con)).append(",");
			sql.append(SQL.quote(DBConnection.getUser(con).toUpperCase())).append(",");
			sql.append(SQL.quote("DASHBOARD_FRONTEND")).append(",");
			sql.append("'").append(loaderUser ? 'Y' : 'N').append("',");
			sql.append("'").append(matchmakerUser ? 'Y' : 'N').append("',");
			sql.append("'").append(summarizerUser ? 'Y' : 'N').append("',");
			sql.append("'").append(dashboardUser ? 'Y' : 'N').append("')");
				
			stmt.executeUpdate(sql.toString());

			// Create privileges for the new user
			sql.setLength(0);
			sql.append("INSERT INTO user_object_privs(");
			sql.append("user_id,");
			sql.append("object_type, object_name,");
			sql.append("modify_ind, delete_ind, execute_ind, grant_ind,");
			sql.append("last_update_date, last_update_user, last_update_os_user)");
			sql.append("values(");
			sql.append(SQL.quote(userId.toUpperCase())).append(",");
			sql.append("'USER', ").append(SQL.quote(userId).toUpperCase()).append(",");
			sql.append("'Y', 'Y', 'Y', 'Y',");
			sql.append(DBConnection.getSystemDate(con)).append(",");
			sql.append(SQL.quote(DBConnection.getUser(con).toUpperCase())).append(",");
			sql.append(SQL.quote("DASHBOARD_FRONTEND")).append(")");
				
			stmt.executeUpdate(sql.toString());

			_alreadyInDatabase=true;
		}
	    
		if(stmt != null) {
			stmt.close();
		}
	}

	/**
	 * A convenience wrapper for the {@link
	 * #storeNoCommit(Connection)} method.  Sets the given database
	 * connection to non-autocommit mode, calls
	 * <code>storeNoCommit</code>, then commits.  If
	 * <code>storeNoCommit</code> throws an exception, the transaction
	 * will be rolled back and the exception will be re-thrown.
	 *
	 * @throws SQLException if there is a database error.  The
	 * transaction will have been rolled back in this case.
	 */
    public void store(Connection con) throws SQLException {
		boolean oldACVal=con.getAutoCommit();
        try {
			con.setAutoCommit(false);
			storeNoCommit(con);
			con.commit();

		} catch(SQLException e) {
			logger.debug("PLUser: caught "+e);
			e.printStackTrace();
			con.rollback();
			throw e;
		} finally {
			con.setAutoCommit(oldACVal);
		}
	}

	/**
	 * Right now, we don't remove records from pl_user through the Web facilities
	 * THIS IS INCOMPLETE AND HAS NOT BEEN TESTED
	 *
	 * Removes this object from the database. If a SQLException is thrown, 
	 * it is expected that the caller will perform a rollback() on the connection.
	 * 
	 * @param con a valid database connection with AutoCommit turned off.
	 */
    public void removeNoCommit(Connection con) throws SQLException {
        Statement stmt = con.createStatement();

		// Delete the pl_user header 
		StringBuffer sql=new StringBuffer();
		sql.append("DELETE FROM pl_user");
		sql.append(" WHERE user_id=").append(SQL.quote(getUserId()));
			
		stmt.executeUpdate(sql.toString());
		_alreadyInDatabase=false;
    }

	/**
	 * Don't use this, because it relies on the incomplete
	 * <code>removeNoCommit</code> method.
	 */
    public void remove(Connection con) 
		throws SQLException {

		boolean oldACVal = con.getAutoCommit();
		con.setAutoCommit(false);

        try {
			removeNoCommit(con);
        } catch(SQLException e) {
			logger.debug("PLUser.remove: caught "+e);
			e.printStackTrace();
			con.rollback();
			throw e;
		} finally {
            con.setAutoCommit(oldACVal);
        }
    }

    /**
	 * Loads the named user from the database referenced by
	 * <code>con</code>.
	 *
	 * @param con An open Connection to a database conforming to the
	 * latest PL schema.
	 * @param userId The name of the user which should be retrieved.
	 * @param password If non-null, the user will only be loaded if
	 * the password matches the password in the database.  If null,
	 * the user will be loaded regardless of password.
	 *
	 * @return A new, populated PLUser object which corresponds to the
	 * named database record, or <code>null</code> if no such record
	 * exists.
	 * @exception SQLException if a database error occurs
	 * @exception PLSecurityException if the user id and/or password
	 * are invalid for the given connection.
	 */
	public static PLUser findByPrimaryKey(Connection con,
										  String userId,
										  String password)

		throws SQLException, ca.sqlpower.util.UnknownFreqCodeException,
			   PLSecurityException {
		
		if (userId == null) {
			throw new NullPointerException("You must specify a userId");
		}

		List oneUser = find(con, userId, password, false);
		return (PLUser) oneUser.get(0);
	}



    /**
	 * Loads the named user from the database referenced by
	 * <code>con</code>.
	 *
	 * @param con An open Connection to a database conforming to the
	 * latest PL schema.
	 * @param userId The name of the user which should be retrieved.
	 * @param password If non-null, the user will only be loaded if
	 * the password matches the password in the database.  If null,
	 * the user will be loaded regardless of password.
     * @param passwordRequired if this is false, then don't match on
     * password.  This may seem to defeat the whole purpose of the 
     * the security model, but this is exactly how all of the other 
     * client tools work.  A stern logger.error statement will
     * be issued in the event that this security mode is being used.
     * It should never be used like this in a webapp.
	 *
	 * @return A new, populated PLUser object which corresponds to the
	 * named database record, or <code>null</code> if no such record
	 * exists.
	 * @exception SQLException if a database error occurs
	 * @exception PLSecurityException if the user id and/or password
	 * are invalid for the given connection.
     */
	public static PLUser findByPrimaryKeyDoNotUse(Connection con,
										  String userId,
										  String password, 
                                          boolean passwordRequired)

		throws SQLException, ca.sqlpower.util.UnknownFreqCodeException,
			   PLSecurityException {
		
		if (userId == null) {
			throw new NullPointerException("You must specify a userId");
		}
		
		List oneUser = null;
		if (passwordRequired) {
			// pass through to the ordinary call
			oneUser = find(con, userId, password, false);
		} else {
			// use the special call which does not check passwords
			oneUser = find(con, userId, password, false, false);
		}

		if (oneUser != null) {
			return (PLUser) oneUser.get(0);		
		} else {
			throw new IllegalStateException("find() user did not return anything!");
		}	
	}


	/**
	 * Returns a List of PLUsers comprising all users defined in the
	 * given database.
	 */
	public static List findAll(Connection con)
		throws SQLException, ca.sqlpower.util.UnknownFreqCodeException,
			   PLSecurityException{
		return find(con, null, null, false);
	}

	public static List findByPrefix(Connection con, String searchPrefix)
		throws SQLException, ca.sqlpower.util.UnknownFreqCodeException,
			   PLSecurityException {
		return find(con, searchPrefix, null, true);
	}


	/**
	 * Does the database grovelling for findByPrimaryKey and findAll.
	 *
	 * @param con An open connection
	 * @param userId The user to loop up, or null for all users
	 * @param password The password for this user (if this is for
	 * logging in a specific user), or null to retrieve users
	 * regardless of password.  Also note that a NULL password in the
	 * database is considered a match for anything passed to the
	 * find() method (including but not limited to null).
	 * @return A List of PLUser objects with:
	 * <ul>
	 * <li>0 PLUser objects if userId is non-null and there is no such 
	 * username/password combination
	 * <li>1 PLUser object if userId is non-null, password is null,
	 * and userId is a valid ID of a user in the database
	 * <li>1 PLUser object if userId is a valid userid and password is
	 * that user's password
	 * <li>0 or more PLUser objects if userId is null
	 * </ul>
	 * @throws IllegalArgumentException if userId is null and password
	 * is non-null.
	 * @throws PLSecurityException if a userId was specified but no
	 * such user exists (also if userid and password are specified and
	 * there is no such userid/password combination).
	 */
	protected static List find(Connection con, String userId, String password,
							   boolean searchByPrefix)
		throws SQLException, ca.sqlpower.util.UnknownFreqCodeException,
			   PLSecurityException {
			return find(con,userId,password,searchByPrefix,true);
	}

	/**
	 * Does the database grovelling for findByPrimaryKey and findAll.
	 *
	 * @param con An open connection
	 * @param userId The user to loop up, or null for all users
	 * @param password The password for this user (if this is for
	 * logging in a specific user), or null to retrieve users
	 * regardless of password.  Also note that a NULL password in the
	 * database is considered a match for anything passed to the
	 * find() method (including but not limited to null).
     * @param passwordRequired if this is false, then don't match on
     * password.  This may seem to defeat the whole purpose of the 
     * the security model, but this is exactly how all of the other 
     * client tools work.  A stern logger.error statement will
     * be issued in the event that this security mode is being used.
     * It should never be used like this in a webapp.
	 * @return A List of PLUser objects with:
	 * <ul>
	 * <li>0 PLUser objects if userId is non-null and there is no such 
	 * username/password combination
	 * <li>1 PLUser object if userId is non-null, password is null,
	 * and userId is a valid ID of a user in the database
	 * <li>1 PLUser object if userId is a valid userid and password is
	 * that user's password
	 * <li>0 or more PLUser objects if userId is null
	 * </ul>
	 * @throws IllegalArgumentException if userId is null and password
	 * is non-null.
	 * @throws PLSecurityException if a userId was specified but no
	 * such user exists (also if userid and password are specified and
	 * there is no such userid/password combination).
	 */
	protected static List find(Connection con, String userId, String password,
							   boolean searchByPrefix, boolean passwordRequired)
		throws SQLException, ca.sqlpower.util.UnknownFreqCodeException,
			   PLSecurityException {

		if (!passwordRequired) {
			logger.error("WARNING: YOU ARE USING THE PL USER IN INSECURE MODE!!!");
		}			

		if (userId == null && password != null) {
			throw new IllegalArgumentException("You can't look up a user by password");
		}
		
		String cryptedPassword = null;
		if (password != null) {
			cryptedPassword = encryptPassword(password);
		}

		List results = new LinkedList();
        Statement stmt = null;
        try {
			StringBuffer sql = new StringBuffer();
			sql.append("SELECT");
			sql.append(" user_id, user_name,");
			sql.append(" email_address, default_kpi_frequency,");
			sql.append(" show_red_ind, show_yellow_ind, show_green_ind, show_grey_ind,");
			sql.append(" last_update_date, last_update_user, last_update_os_user,");
			sql.append(" use_loader_ind, use_matchmaker_ind, use_summarizer_ind, use_dashboard_ind");
			sql.append(" FROM pl_user");
			if (userId != null) {
				if (searchByPrefix) {
					sql.append(" WHERE user_id LIKE ").append(SQL.quote(userId+"%"));
				} else {
					sql.append(" WHERE user_id = ").append(SQL.quote(userId));
				}
				// don't check the password unless we're told to (yes, I know this is weird)
				if (passwordRequired) {					
					if (password != null) {
						sql.append(" AND (password = ").append(SQL.quote(cryptedPassword));
						sql.append(" OR password IS NULL)");
					}
				}
			}
			sql.append(" ORDER BY user_id");

			if (logger.isDebugEnabled()) logger.debug("Finding user: "+sql.toString());
            stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery(sql.toString());
			
			boolean hasRows = rs.next();
            if ( (!hasRows) && (userId != null) ) { 
				throw new PLSecurityException(PLSecurityManager.LOGIN_PERMISSION,
											  PLSecurityManager.BAD_CREDENTIALS, null);
			}

			if (!hasRows) {
				return Collections.EMPTY_LIST;
			}

			// this is do..while because we already called rs.next in the check above
			do {
				PLUser newBean = new PLUser();
				newBean.userId = rs.getString("user_id");
				newBean.password = password;
				newBean.userName = rs.getString("user_name");
				newBean.emailAddress = rs.getString("email_address");
				newBean.lastUpdateDate = rs.getDate("last_update_date");
				newBean.lastUpdateUser = rs.getString("last_update_user");
				newBean.lastUpdateOsUser = rs.getString("last_update_os_user");
				newBean.loaderUser = SQL.decodeInd(rs.getString("use_loader_ind"));
				newBean.matchmakerUser = SQL.decodeInd(rs.getString("use_matchmaker_ind"));
				newBean.summarizerUser = SQL.decodeInd(rs.getString("use_summarizer_ind"));
				newBean.dashboardUser = SQL.decodeInd(rs.getString("use_dashboard_ind"));
				newBean._alreadyInDatabase = true;
				
				results.add(newBean);
			} while(rs.next());

        } finally {
            if(stmt != null) {
                stmt.close();
            }
        }

		if (userId != null && !searchByPrefix && results.size() > 1) {
			throw new IllegalStateException("Got more than one user for userId "+userId);
		}

		return results;
	}

	/**
	 * Returns a list of PLGroup objects representing all the groups
	 * that this user belongs to.
	 */
	public List getGroups(Connection con) throws SQLException {
		return PLGroup.findByUser(con, this);
	}

	/**
	 * Returns a list of group names to which this user belongs.  If
	 * there is a groupNameFilter installed on this user, it is
	 * applied to the returned list.
	 */
	public List getGroupNames(Connection con) throws SQLException {
		return getGroupNamesWithFilter(con, groupNameFilter);
	}

	/**
	 * Returns a list of group names to which this user belongs which
	 * are also present in the given filter set.  This method is
	 * useful if you want a list of all groups this user belongs to
	 * regardless of its current groupNameFilter.  In that case, call
	 * this method with <code>filter = null</code>.
	 */
	public List getGroupNamesWithFilter(Connection con, Set filter) throws SQLException {
		Statement stmt = null;
		ResultSet rs = null;
		List results = new LinkedList();
		StringBuffer sql = new StringBuffer();
		sql.append("SELECT group_name FROM user_group WHERE user_id=")
			.append(SQL.quote(getUserId()));
		if (filter != null) {
			sql.append(" AND group_name IN(").append(SQL.quoteCollection(filter)).append(")");
		}

		try {
			stmt = con.createStatement();
			rs = stmt.executeQuery(sql.toString());
			while (rs.next()) {
				results.add(rs.getString(1));
			}
		} finally {
			if (rs != null) rs.close();
			if (stmt != null) stmt.close();
		}

		return results;
	}

	/**
	 * Refreshes this object's properties from its associated database
	 * record. Also resets the internal state of the superuser and
	 * omniscient properties to null so the getters will re-query the
	 * database next time they are called.
	 *
	 * @throws IllegalStateException if the user cannot be found in the database.
	 */
	public void refresh(Connection con) throws SQLException, IllegalStateException {
		List singleUser = find(con, this.getUserId(), null, false);
		if (singleUser.size() != 1) {
			throw new IllegalStateException("Couldn't find myself while attempting to refresh");
		}
		PLUser fresh = (PLUser) singleUser.get(0);
		this.userName = fresh.userName;
		this.emailAddress = fresh.emailAddress;
		this.lastUpdateDate = fresh.lastUpdateDate;
		this.lastUpdateUser = fresh.lastUpdateUser;
		this.lastUpdateOsUser = fresh.lastUpdateOsUser;
		this.omniscient = null;
		this.superuser = null;
		this.loaderUser = fresh.loaderUser;
		this.summarizerUser = fresh.summarizerUser;
		this.matchmakerUser = fresh.matchmakerUser;
		this.dashboardUser = fresh.dashboardUser;
	}

	protected static String encryptPassword(String plainPassword) {
		MessageDigest md5 = null;
		try {
			md5 = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			throw new PLSecurityException(PLSecurityManager.LOGIN_PERMISSION,
										  PLSecurityManager.INVALID_MANAGER,
										  null);
		}
		byte[] hashBytes = md5.digest(plainPassword.getBytes());
		ByteColonFormat bcf = new ByteColonFormat();
		bcf.setUsingColons(false);
		StringBuffer cryptedPassword = new StringBuffer(32);
		bcf.format(hashBytes, cryptedPassword, null);
		return cryptedPassword.toString();
	}

	// GET and SET methods go below this line

	/**
	 * Returns the current group name filter that applies to this
	 * user.
	 *
	 * @return a Set of strings which was previously installed using
	 * {@link #setGroupNameFilter(Set)}.  Returns null if there is no
	 * filter in place.
	 */
	public Set getGroupNameFilter() {
		return groupNameFilter;
	}

	/**
	 * Sets the current group name filter (discards previous filter).
	 * When a non-null filter is installed, the PLUser will appear to
	 * belong only to groups which are in the intersection of this
	 * filter and its actual group list.  This functionality lets more
	 * powerful users choose to temporarily give up some of their
	 * group privs in order to simulate logging in as less powerful
	 * users.
	 *
	 * @param namesToInclude A Set of Strings, each string
	 * corresponding to the name of a group which the current user
	 * belongs to.  null is an acceptable value, and means "don't
	 * filter".  The empty set means to pretend this user doesn't
	 * belong to any groups.  Although it is not an error to include
	 * in this set names of groups to which this user does not belong,
	 * such entries will have no effect.
	 */
	public void setGroupNameFilter(Set namesToInclude) {
		this.groupNameFilter = namesToInclude;
		omniscient = null;
		superuser = null;
	}

    public String getUserId() {
        return userId;
    }

	public String getPassword() {
		return password;
	}

	public void setPassword(String v) {
		password = v;
	}

    public String getUserName() {
        return userName;
    }

    public void setUserName(String v) {
        userName=v;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String v) {
        emailAddress=v;
    }

    public java.sql.Date getLastUpdateDate() {
        return lastUpdateDate;
    }

    public String getLastUpdateUser() {
        return lastUpdateUser;
    }

    public String getLastUpdateOsUser() {
        return lastUpdateOsUser;
    }

	/**
	 * Checks if this user is omniscient.  Omniscient users can see
	 * all data regardless of data filtering/subsetting restrictions.
	 * This method caches the answer, so feel free to call it many
	 * times.
	 * 
	 * <p>Note that the groupNameFilter is consulted before returning the
	 * cached value.  If there is a filter in place and it does not
	 * name the PLGroup.OMNISCIENT_GROUP_NAME, this method will return
	 * false regardless of the cached value.
	 */
	public boolean isOmniscient(Connection con) throws SQLException {
		if (groupNameFilter != null
			&& !groupNameFilter.contains(PLGroup.OMNISCIENT_GROUP_NAME)) {
			return false;
		} else if (omniscient == null) {
			Statement stmt = null;
			ResultSet rs = null;
			try {
				stmt = con.createStatement();
				rs = stmt.executeQuery("SELECT 1 FROM user_group WHERE user_id="
									   +SQL.quote(getUserId())+" AND group_name="
									   +SQL.quote(PLGroup.OMNISCIENT_GROUP_NAME));
				if (rs.next()) {
					omniscient = Boolean.TRUE;
				} else {
					omniscient = Boolean.FALSE;
				}
			} finally {
				if (rs != null) rs.close();
				if (stmt != null) stmt.close();
			}
		}
		return omniscient.booleanValue();
	}

	/**
	 * Checks if this user is a superuser.  Superusers are the users
	 * who belong to the special PL_ADMIN group.  They typically have
	 * full rights to everything in the system.This method caches the
	 * answer, so feel free to call it many times.
	 * 
	 * <p>Note that the groupNameFilter is consulted before returning the
	 * cached value.  If there is a filter in place and it does not
	 * name the PLGroup.OMNISCIENT_GROUP_NAME, this method will return
	 * false regardless of the cached value.
	 */
	public boolean isSuperuser(Connection con) throws SQLException {
		if (groupNameFilter != null
			&& !groupNameFilter.contains(PLGroup.ADMIN_GROUP)) {
			return false;
		} else if (superuser == null) {
			Statement stmt = null;
			ResultSet rs = null;
			try {
				stmt = con.createStatement();
				String sql = "SELECT 1 FROM user_group WHERE user_id="
					+SQL.quote(getUserId())+" AND group_name="
					+SQL.quote(PLGroup.ADMIN_GROUP);
				logger.debug("isSuperuser query "+sql);
				rs = stmt.executeQuery(sql);
				if (rs.next()) {
					superuser = Boolean.TRUE;
				} else {
					superuser = Boolean.FALSE;
				}
			} finally {
				if (rs != null) rs.close();
				if (stmt != null) stmt.close();
			}
		}
		logger.debug("isSuperuser returns "+superuser.booleanValue());
		return superuser.booleanValue();
	}

	/**
	 * For the DatabaseObject interface.
	 *
	 * @return The string "USER"
	 */
	public String getObjectType() {
		return "USER";
	}

	/**
	 * For the DatabaseObject interface.
	 *
	 * @return This user's ID.
	 */
	public String getObjectName() {
		return getUserId();
	}

	/**
	 * Gets the value of loaderUser
	 *
	 * @return the value of loaderUser
	 */
	public boolean isLoaderUser()  {
		return this.loaderUser;
	}

	/**
	 * Sets the value of loaderUser
	 *
	 * @param argLoaderUser Value to assign to this.loaderUser
	 */
	public void setLoaderUser(boolean argLoaderUser) {
		this.loaderUser = argLoaderUser;
	}

	/**
	 * Gets the value of summarizerUser
	 *
	 * @return the value of summarizerUser
	 */
	public boolean isSummarizerUser()  {
		return this.summarizerUser;
	}

	/**
	 * Sets the value of summarizerUser
	 *
	 * @param argSummarizerUser Value to assign to this.summarizerUser
	 */
	public void setSummarizerUser(boolean argSummarizerUser) {
		this.summarizerUser = argSummarizerUser;
	}

	/**
	 * Gets the value of matchmakerUser
	 *
	 * @return the value of matchmakerUser
	 */
	public boolean isMatchmakerUser()  {
		return this.matchmakerUser;
	}

	/**
	 * Sets the value of matchmakerUser
	 *
	 * @param argMatchmakerUser Value to assign to this.matchmakerUser
	 */
	public void setMatchmakerUser(boolean argMatchmakerUser) {
		this.matchmakerUser = argMatchmakerUser;
	}

	/**
	 * Gets the value of dashboardUser
	 *
	 * @return the value of dashboardUser
	 */
	public boolean isDashboardUser()  {
		return this.dashboardUser;
	}

	/**
	 * Sets the value of dashboardUser
	 *
	 * @param argDashboardUser Value to assign to this.dashboardUser
	 */
	public void setDashboardUser(boolean argDashboardUser) {
		this.dashboardUser = argDashboardUser;
	}

	public String toString() {
		StringBuffer meString=new StringBuffer();
		meString.append("[PLUser: ");
		meString.append("userId=").append(userId);
		meString.append(", userName=").append(userName);
		meString.append(", password=[").append(password != null ? "not " : "").append("null]");
		meString.append(", groupNameFilter=").append(groupNameFilter);
		meString.append(", omniscient=").append(omniscient);
		meString.append(", superuser=").append(superuser);
		meString.append(", emailAddress=").append(emailAddress);
		meString.append(", loaderUser=").append(loaderUser);
		meString.append(", summarizerUser=").append(summarizerUser);
		meString.append(", matchmakerUser=").append(matchmakerUser);
		meString.append(", dashboardUser=").append(dashboardUser);
		meString.append("]");
		return meString.toString();
	}
}
