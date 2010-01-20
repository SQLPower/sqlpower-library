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

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import ca.sqlpower.sql.CachedRowSet;
import ca.sqlpower.sql.SQL;

public class SQLServerDatabaseMetaDataDecorator extends DatabaseMetaDataDecorator {

	private static final Logger logger = Logger.getLogger(SQLServerDatabaseMetaDataDecorator.class);
	
    public SQLServerDatabaseMetaDataDecorator(DatabaseMetaData delegate, ConnectionDecorator connectionDecorator) {
		super(delegate, connectionDecorator);
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
    
    @Override
    protected ResultSetDecorator wrap (ResultSet rs) throws SQLException {	
    	return new SQLServerResultSetDecorator(wrap(rs.getStatement()), rs);
    }
    
    @Override
    protected StatementDecorator wrap (Statement statement) {
    	return new SQLServerStatementDecorator(connectionDecorator, statement);
    }
}
