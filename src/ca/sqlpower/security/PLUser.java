package ca.sqlpower.security;

import java.sql.*;
import java.util.*;

import ca.sqlpower.sql.*;
import ca.sqlpower.dashboard.DBConnection;

/**
 * The User class represents a people who can log in to an application
 * which uses the PL schema.
 * 
 * @author Gillian Mereweather
 * @author Jonathan Fuerth
 * @version $Id$
 */
public class PLUser implements DatabaseObject, java.io.Serializable {
    protected boolean _alreadyInDatabase;
    protected String userId;
	protected String password;
    protected String userName;
	protected String emailAddress;
	protected ca.sqlpower.dashboard.Frequency defaultKpiFrequency;
	protected boolean redVisible;
	protected boolean yellowVisible;
	protected boolean greenVisible;
	protected boolean greyVisible;
    protected java.sql.Date lastUpdateDate;
    protected String lastUpdateUser;
    protected String lastUpdateOsUser;

	/**
	 * Creates a new user object with no user id.  For internal use
	 * only.
	 */
    protected PLUser() {
        _alreadyInDatabase=false;
        userId=null;
        userName=null;
		emailAddress=null;
		defaultKpiFrequency=null;
		redVisible=false;
		yellowVisible=false;
		greenVisible=false;
        greyVisible=false;
        lastUpdateDate=null;
        lastUpdateUser=null;
		lastUpdateOsUser=null;
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
    public void storeNoCommit(Connection con) throws SQLException {
        Statement stmt = con.createStatement();
		StringBuffer sql = new StringBuffer();
		if(_alreadyInDatabase) {
			sql.append("UPDATE pl_user");
			sql.append(" SET user_name=").append(SQL.quote(getUserName())).append(",");
			sql.append("   email_address=").append(SQL.quote(getEmailAddress())).append(",");
			sql.append("   default_kpi_frequency=").append(SQL.quote(getDefaultKpiFrequency().toString())).append(",");
			sql.append("   show_red_ind=").append(SQL.quote(isRedVisible()?"Y":"N")).append(",");
			sql.append("   show_yellow_ind=").append(SQL.quote(isYellowVisible()?"Y":"N")).append(",");
			sql.append("   show_green_ind=").append(SQL.quote(isGreenVisible()?"Y":"N")).append(",");
			sql.append("   show_grey_ind=").append(SQL.quote(isGreyVisible()?"Y":"N")).append(",");
			sql.append("   last_update_date=").append(DBConnection.getSystemDate(con)).append(",");
			sql.append("   last_update_user=").append(SQL.quote(DBConnection.getUser(con).toUpperCase())).append(",");
			sql.append("   last_update_os_user=").append(SQL.quote("DASHBOARD_FRONTEND"));
			sql.append(" WHERE user_id = ").append(SQL.quote(getUserId()));
			stmt.executeUpdate(sql.toString());
		} else {
			sql.append("INSERT INTO pl_user(");
			sql.append(" user_id, user_name,");
			sql.append(" email_address, default_kpi_frequency,");
			sql.append(" show_red_ind, show_yellow_ind, show_green_ind, show_grey_ind,");
			sql.append(" last_update_date, last_update_user, last_update_os_user)");
			sql.append(" VALUES( ");
			sql.append(SQL.quote(userId.toUpperCase())).append(",");
			sql.append(SQL.quote(userName)).append(",");
			sql.append(SQL.quote(emailAddress)).append(",");
			sql.append(SQL.quote(defaultKpiFrequency.toString())).append(",");
			sql.append(SQL.quote(isRedVisible()?"Y":"N")).append(",");
			sql.append(SQL.quote(isYellowVisible()?"Y":"N")).append(",");
			sql.append(SQL.quote(isGreenVisible()?"Y":"N")).append(",");
			sql.append(SQL.quote(isGreyVisible()?"Y":"N")).append(",");
			sql.append(DBConnection.getSystemDate(con)).append(",");
			sql.append(SQL.quote(DBConnection.getUser(con).toUpperCase())).append(",");
			sql.append(SQL.quote("DASHBOARD_FRONTEND")).append(")");
				
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
			System.out.println("PLUser: caught "+e);
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
			System.out.println("PLUser.remove: caught "+e);
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

		throws SQLException, ca.sqlpower.dashboard.UnknownFreqCodeException,
		PLSecurityException {
		
		if (userId == null) {
			throw new NullPointerException("You must specify a userId");
		}

		List oneUser = find(con, userId, password);
		return (PLUser) oneUser.get(0);
	}

	/**
	 * Returns a List of PLUsers comprising all users defined in the
	 * given database.
	 */
	public static List findAll(Connection con)
		throws SQLException, ca.sqlpower.dashboard.UnknownFreqCodeException,
		PLSecurityException{
		return find(con, null, null);
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
	protected static List find(Connection con, String userId, String password)
		throws SQLException, ca.sqlpower.dashboard.UnknownFreqCodeException,
		PLSecurityException {

		if (userId == null && password != null) {
			throw new IllegalArgumentException("You can't look up a user by password");
		}

		List results = new LinkedList();
        Statement stmt = null;
        try {
			StringBuffer sql = new StringBuffer();
			sql.append("SELECT");
			sql.append(" user_id, user_name,");
			sql.append(" email_address, default_kpi_frequency,");
			sql.append(" show_red_ind, show_yellow_ind, show_green_ind, show_grey_ind,");
			sql.append(" last_update_date, last_update_user, last_update_os_user");
			sql.append(" FROM pl_user");
			if (userId != null) {
				sql.append(" WHERE user_id = ").append(SQL.quote(userId));
				if (password != null) {
					sql.append(" AND (password = ").append(SQL.quote(password));
					sql.append(" OR password IS NULL)");
				}
			}
			sql.append(" ORDER BY user_id");

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
				String freqCode = rs.getString("default_kpi_frequency");
				if (freqCode != null) {
					newBean.defaultKpiFrequency =
						new ca.sqlpower.dashboard.Frequency(ca.sqlpower.dashboard.Frequency.freqCodeToFreq(freqCode));
				}
				newBean.redVisible = SQL.decodeInd(rs.getString("show_red_ind"));
				newBean.yellowVisible = SQL.decodeInd(rs.getString("show_yellow_ind"));
				newBean.greenVisible = SQL.decodeInd(rs.getString("show_green_ind"));
				newBean.greyVisible = SQL.decodeInd(rs.getString("show_grey_ind"));
				newBean.lastUpdateDate = rs.getDate("last_update_date");
				newBean.lastUpdateUser = rs.getString("last_update_user");
				newBean.lastUpdateOsUser = rs.getString("last_update_os_user");
				newBean._alreadyInDatabase = true;
				
				results.add(newBean);
			} while(rs.next());

        } finally {
            if(stmt != null) {
                stmt.close();
            }
        }

		if (userId != null && results.size() > 1) {
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

	// GET and SET methods go below this line

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

    public ca.sqlpower.dashboard.Frequency getDefaultKpiFrequency() {
        return defaultKpiFrequency;
    }

    public void setDefaultKpiFrequency(ca.sqlpower.dashboard.Frequency v) {
        defaultKpiFrequency=v;
    }

	public boolean isRedVisible() {
		return this.redVisible;
	}

	public void setRedVisible(boolean argRedVisible){
	    this.redVisible=argRedVisible;
	}

	public boolean isYellowVisible() {
		return this.yellowVisible;
	}

    public void setYellowVisible(boolean argYellowVisible){
	    this.yellowVisible=argYellowVisible;
	}

	public boolean isGreenVisible() {
		return this.greenVisible;
	}

	public void setGreenVisible(boolean argGreenVisible){
	    this.greenVisible=argGreenVisible;
	}

	public boolean isGreyVisible() {
		return this.greyVisible;
	}

    public void setGreyVisible(boolean argGreyVisible){
	    this.greyVisible=argGreyVisible;
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

	public String toString() {
		StringBuffer meString=new StringBuffer();
		meString.append("[PLUser: ");
		meString.append("userId=").append(userId);
		meString.append(", userName=").append(userName);
		meString.append(", emailAddress=").append(emailAddress);
		meString.append(", defaultKpiFrequency=").append(defaultKpiFrequency);
		meString.append(", redVisible=").append(redVisible);
		meString.append(", yellowVisible=").append(yellowVisible);
		meString.append(", greenVisible=").append(greenVisible);
		meString.append(", greyVisible=").append(greyVisible);
		meString.append("]");
		return meString.toString();
	}
}
