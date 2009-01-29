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

/**
 * Allow for workarounds for dealing with Oracle Driver issues.
*/
public class OracleConnectionDecorator extends ConnectionDecorator {
	
	public DatabaseMetaData getMetaData() throws SQLException {
        return new OracleDatabaseMetaDataDecorator(super.getMetaData());
    }

	public OracleConnectionDecorator(Connection conn) {
		super(conn);
	}

	@Override
	protected PreparedStatement makePreparedStatementDecorator(
			PreparedStatement pstmt) {
		return new OraclePreparedStatementDecorator(this, pstmt);
	}

	@Override
	protected Statement makeStatementDecorator(Statement stmt) {
		return new OracleStatementDecorator(this, stmt);
	}

}
