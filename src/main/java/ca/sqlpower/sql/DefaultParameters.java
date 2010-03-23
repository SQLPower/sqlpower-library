package ca.sqlpower.sql;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import ca.sqlpower.util.Version;
import ca.sqlpower.util.VersionFormatException;

/**
 * Interface to the all-knowing, all-powerful DEF_PARAM table.
 *
 * <p><code>END OF LINE.</code>
 */
public class DefaultParameters {

	private Map params;

    /**
     * Creates a DefaultParameters instance based on the values stored in
     * the DEF_PARAM table in the given connection's current catalog and schema.
     * 
     * @param con The connection to read parameters from.
     * @throws SQLException If the DEF_PARAM table is missing or otherwise not accessible
     * @throws PLSchemaException If the DEF_PARAM.SCHEMA_VERSION column does not contain
     *          a major.minor.tiny style version number.
     */
    public DefaultParameters(Connection con) throws SQLException, PLSchemaException {
        this(con, null, null);
    }

    /**
     * Creates a DefaultParameters instance based on the values stored in
     * the DEF_PARAM table in the given catalog and schema.
     * 
     * @param con The connection to read parameters from.
     * @param plCatalog The catalog to read from (null means omit the catalog from the query)
     * @param plSchema The schema to read from (null means omit the schema from the query)
     * @throws SQLException If the DEF_PARAM table is missing or otherwise not accessible
     * @throws PLSchemaException If the DEF_PARAM.SCHEMA_VERSION column does not contain
     *          a major.minor.tiny style version number.
     */
    public DefaultParameters(Connection con, String plCatalog, String plSchema) throws SQLException, PLSchemaException {
		StringBuffer sql = new StringBuffer();
		sql.append("SELECT * FROM ");
        if (plCatalog != null) {
            sql.append(plCatalog).append(".");
        }
        if (plSchema != null) {
            sql.append(plSchema).append(".");
        }
        sql.append("DEF_PARAM");

		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = con.createStatement();
			rs = stmt.executeQuery(sql.toString());
			if (!rs.next()) {
				throw new PLSchemaException("There are no rows in DEF_PARAM?!");
			}
			params = new HashMap();
			ResultSetMetaData rsmd = rs.getMetaData();
			for (int col=1, ncols=rsmd.getColumnCount(); col<=ncols; col++) {
				params.put(rsmd.getColumnName(col).toLowerCase(), rs.getString(col));
			}
		} finally {
			if (rs != null) {
				rs.close();
			}
			if (stmt != null) {
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
		if (!params.containsKey(lcParamName)) {
			throw new IllegalArgumentException(
                    "DEF_PARAM version " + params.get("SCHEMA_VERSION") +
                    " doesn't have the parameter " + paramName);
		}
		return (String) params.get(paramName.toLowerCase());
	}
	
	public String getEmailReturnAddress() {
		return get("email_notification_return_adrs");
	}
	
	public String getEmailServerName() {
		return get("mail_server_name");
	}
	
    public Version getPLSchemaVersion() throws VersionFormatException {
        Version v = new Version(get("schema_version"));
        return v;
    }
}
