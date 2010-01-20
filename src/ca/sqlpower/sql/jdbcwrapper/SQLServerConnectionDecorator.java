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
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.log4j.Logger;

/**
 * The class exists mainly to wrap instances of DatabaseMetaData returned
 * by {@link #getMetaData()} in the appropriate decorator class.
 */
public class SQLServerConnectionDecorator extends ConnectionDecorator {

    private static final Logger logger = Logger.getLogger(SQLServerConnectionDecorator.class);
    
    protected SQLServerConnectionDecorator(Connection delegate) {
        super(delegate);
    }

    @Override
    public DatabaseMetaData getMetaData() throws SQLException {
        
    	if (databaseMetaDataDecorator == null) {
    		DatabaseMetaData rawDBMD = super.getMetaData();
            if (rawDBMD.getDatabaseProductVersion().startsWith("8")) {
            	databaseMetaDataDecorator = new SQLServer2000DatabaseMetaDataDecorator(rawDBMD, this);
            } else if (rawDBMD.getDatabaseProductVersion().startsWith("9")) {
            	databaseMetaDataDecorator = new SQLServer2005DatabaseMetaDataDecorator(rawDBMD, this);
            } else if (rawDBMD.getDatabaseProductVersion().startsWith("10")) {
            	databaseMetaDataDecorator = new SQLServer2008DatabaseMetaDataDecorator(rawDBMD, this);
            } else {
                logger.warn("Unknown database product version: " +
                        rawDBMD.getDatabaseProductVersion() +
                        " -- returning generic SQL Server wrapper");
                databaseMetaDataDecorator = new SQLServerDatabaseMetaDataDecorator(rawDBMD, this);
            }
    	}
    	
    	return databaseMetaDataDecorator;
    }

	@Override
	protected PreparedStatement makePreparedStatementDecorator(PreparedStatement pstmt) {
		return new SQLServerPreparedStatementDecorator(this, pstmt);
	}

	@Override
	protected Statement makeStatementDecorator(Statement stmt) {
		return new SQLServerStatementDecorator(this, stmt);
	}
}
