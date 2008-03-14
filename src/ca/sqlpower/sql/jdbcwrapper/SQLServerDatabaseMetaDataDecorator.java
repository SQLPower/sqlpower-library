/*
 * Copyright (c) 2008, SQL Power Group Inc.
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

package ca.sqlpower.sql.jdbcwrapper;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.log4j.Logger;

import ca.sqlpower.sql.CachedRowSet;
import ca.sqlpower.sql.SQL;

public class SQLServerDatabaseMetaDataDecorator extends DatabaseMetaDataDecorator {

	private static final Logger logger = Logger.getLogger(SQLServerDatabaseMetaDataDecorator.class);
	
    public SQLServerDatabaseMetaDataDecorator(DatabaseMetaData delegate) {
        super(delegate);
    }
    
    /**
     * Works around a user-reported bug in the SQL Server JDBC driver. See <a
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
    

    /**
     * Augments the Microsoft-supplied getColumns() result set with the JDBC4
     * IS_AUTOINCREMENT column. The value of this extra column is determined by
     * the presence of the substring <code>" identity"</code> in the column's type name.
     */
    @Override
    public ResultSet getColumns(String catalog, String schemaPattern, String tableNamePattern, String columnNamePattern) throws SQLException {
        ResultSet rs = super.getColumns(catalog, schemaPattern, tableNamePattern, columnNamePattern);
        CachedRowSet crs = new CachedRowSet();
        boolean fudgeAutoInc = SQL.findColumnIndex(rs, "IS_AUTOINCREMENT") == -1;
        if (fudgeAutoInc) {
        	crs.populate(rs, null, "IS_AUTOINCREMENT");
        } else {
        	crs.populate(rs);
        }
        rs.close();
        if (fudgeAutoInc) {
        	
        	int autoIncColNum = crs.findColumn("IS_AUTOINCREMENT");

        	while (crs.next()) {
        		if (logger.isDebugEnabled()) {
        			logger.debug("Examining col " + crs.getString(4) + " (" + crs.getString(6) + ")");
        		}
        		if (crs.getString(6) != null && crs.getString(6).toLowerCase().indexOf(" identity") >= 0) {
        			crs.updateString(autoIncColNum, "YES");
        			logger.debug("  AUTO-INC!");
        		} else {
        			crs.updateString(autoIncColNum, "NO");
        			logger.debug("  NOT AUTO-INC!");
        		}
        	}
        	crs.beforeFirst();
        }
        return crs;
    }
}
