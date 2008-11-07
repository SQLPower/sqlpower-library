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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import ca.sqlpower.sql.CachedRowSet;
import ca.sqlpower.sql.SQL;

public class SQLServerDatabaseMetaDataDecorator extends DatabaseMetaDataDecorator {

	private static final Logger logger = Logger.getLogger(SQLServerDatabaseMetaDataDecorator.class);
	
    public SQLServerDatabaseMetaDataDecorator(DatabaseMetaData delegate) {
        super(delegate);
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
        	int defaultColNum = crs.findColumn("COLUMN_DEF");
        	
        	while (crs.next()) {
        	    
        	    // populate auto-increment column
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
        		
        		// strip parentheses from default values (see bug 1693)
        		crs.updateString(defaultColNum, stripParens(crs.getString(defaultColNum)));
        		
        	}
        	crs.beforeFirst();
        }
        return crs;
    }

    /**
     * SQL Server tends to put parentheses around column default values, and we
     * have to strip them off in this wrapper to provide forward-engineering
     * compatibility with other platforms (notably MySQL). This method is used
     * by the getColumns() wrapper to strip off those parentheses.
     * <p>
     * See <a href="http://trillian.sqlpower.ca/bugzilla/show_bug.cgi?id=1693">bug
     * 1693</a> for details.
     * 
     * @param original
     *            The original default value, which might be surrounded by one
     *            or more balanced sets of parentheses. This argument may be null,
     *            in which case null will be returned.
     * @return The original value with all balanced sets of parentheses that
     *         completely surrounded the value removed. If the original value
     *         was null, the return value will be null.
     */
    public static String stripParens(String original) {
        if (original == null) return null;
        Pattern p = Pattern.compile("\\((.*)\\)");
        Matcher m = p.matcher(original);
        while (m.matches()) {
            original = m.group(1);
            m = p.matcher(original);
        }
        return original;
    }
}
