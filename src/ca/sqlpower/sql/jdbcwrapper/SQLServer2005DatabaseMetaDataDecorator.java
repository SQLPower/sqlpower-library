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

package ca.sqlpower.sql.jdbcwrapper;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import ca.sqlpower.sql.CachedRowSet;

/**
 * Extends the base SQL Server database meta data decorator by providing a more accurate
 * method of retrieving schema names on SQL Server 2005.
 */
public class SQLServer2005DatabaseMetaDataDecorator extends SQLServerDatabaseMetaDataDecorator {

    public SQLServer2005DatabaseMetaDataDecorator(DatabaseMetaData delegate, ConnectionDecorator connectionDecorator) {
        super(delegate, connectionDecorator);
    }

    /**
     * Works around a user-reported bug in the SQL Server JDBC driver. Note this
     * is a different workaround than the one for SQL Server 2000, and it was updated again
     * just before the 0.9.13 release.
     * <p>
     * See <a href="http://www.sqlpower.ca/forum/posts/list/0/1788.page">the original post</a>
     * for details on the original SS2k5 query. See
     * <a href="http://www.sqlpower.ca/forum/posts/list/15/2271.page">the new post</a>
     * for the reasoning behind the change to this query (which is the same one we're using
     * for SQL Server 2008).
     */
    @Override
    public ResultSet getSchemas() throws SQLException {
        Statement stmt = null;
        ResultSet rs = null;
        try {
            stmt = getConnection().createStatement();
            String sql = 
                "SELECT DISTINCT" +
                "\n  w.schema_name AS TABLE_SCHEM," +
                "\n  w.catalog_name AS TABLE_CAT" +
                "\nFROM sys.schemas u" +
                "\nJOIN sys.objects v ON u.schema_id = v.schema_id" +
                "\nJOIN INFORMATION_SCHEMA.SCHEMATA w ON w.schema_name = u.name" +
                "\nORDER BY table_schem";
            rs = stmt.executeQuery(sql);
            
            CachedRowSet crs = new CachedRowSet();
            crs.populate(rs);
            return crs;
            
        } finally {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
        }
    }
    
}
