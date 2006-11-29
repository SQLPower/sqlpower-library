package ca.sqlpower.sql;

import java.sql.*;
import java.util.*;

/**
 * Interface to the all-knowing, all-powerful DEF_PARAM table.
 *
 * <p><code>END OF LINE.</code>
 */
public class DefaultParameters {

	Map params;

	public DefaultParameters(Connection con) throws SQLException, PLSchemaException {
		StringBuffer sql = new StringBuffer();
		sql.append("SELECT * FROM DEF_PARAM");

		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = con.createStatement();
			rs = stmt.executeQuery(sql.toString());
			if(!rs.next()) {
				throw new PLSchemaException("There are no rows in DEF_PARAM!!!!!!!!!?!?!!!!");
			}
			params = new HashMap();
			ResultSetMetaData rsmd = rs.getMetaData();
			for(int col=1, ncols=rsmd.getColumnCount(); col<=ncols; col++) {
				params.put(rsmd.getColumnName(col).toLowerCase(), rs.getString(col));
			}
		} finally {
			if(rs != null) {
				rs.close();
			}
			if(stmt != null) {
				stmt.close();
			}
		}
	}

	/**
	 * Gets the specified parameter from (our snapshot of) the
	 * DEF_PARAM table.
	 *
	 * @param paramName The column name from DEF_PARAM.
	 * @throws IllegalArgumentException if the given parameter isn't a
	 * column name in DEF_PARAM
	 */
	public String get(String paramName) {
		String lcParamName = paramName.toLowerCase();
		if(!params.containsKey(lcParamName)) {
			throw new IllegalArgumentException("DEF_PARAM version "+params.get("SCHEMA_VERSION")+" doesn't have the parameter "+paramName);
		}
		return (String) params.get(paramName.toLowerCase());
	}
	
	public String getEmailReturnAddress() {
		return get("email_notification_return_adrs");
	}
	
	public String getEmailServerName() {
		return get("mail_server_name");
	}
	
}
