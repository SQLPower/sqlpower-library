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
 * method of retrieving schema names on SQL Server 2000.
 */
public class SQLServer2000DatabaseMetaDataDecorator extends SQLServerDatabaseMetaDataDecorator {

    public SQLServer2000DatabaseMetaDataDecorator(DatabaseMetaData delegate, ConnectionDecorator connectionDecorator) {
        super(delegate, connectionDecorator);
    }

    /**
     * Works around a user-reported bug in the Microsoft JDBC drivers for SQL
     * Server 2000. Note this is a different fix from the one used in SQL Server
     * 2005. See <a
     * href="http://www.sqlpower.ca/forum/posts/list/0/1788.page">the post</a>
     * for details.
     */
    @Override
    public ResultSet getSchemas() throws SQLException {
        Statement stmt = null;
        ResultSet rs = null;
        try {
            stmt = getConnection().createStatement();
            String sql = 
                "SELECT name AS 'TABLE_SCHEM', db_name() AS 'TABLE_CATALOG'\n" +
                "FROM sysusers\n" +
                "WHERE issqlrole = 0\n" +
                "ORDER BY name";
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
