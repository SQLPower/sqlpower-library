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
public class SQLServer2008DatabaseMetaDataDecorator extends SQLServerDatabaseMetaDataDecorator {

    public SQLServer2008DatabaseMetaDataDecorator(DatabaseMetaData delegate) {
        super(delegate);
    }

    /**
     * Works around a user-reported bug in the SQL Server JDBC driver 2.0. Note
     * this is a different workaround than the one for SQL Server 2000 and 2005!
     * Microsoft has been busy. See <a
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
                "SELECT s.name AS TABLE_SCHEM, db_name() AS TABLE_CATALOG" +
                "\nFROM sys.schemas s" +
                "\nWHERE EXISTS (" +
                "\n SELECT 1 FROM sys.database_principals p" +
                "\n WHERE p.is_fixed_role=0 AND s.principal_id=p.principal_id" +
                "\n) OR EXISTS (" +
                "\n SELECT 1 FROM sys.tables t WHERE t.schema_id = s.schema_id" +
                "\n)" +
                "\nORDER BY 1";
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
