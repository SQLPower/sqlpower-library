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
/*
 * Created on Jun 8, 2005
 *
 * This code belongs to SQL Power Group Inc.
 */
package ca.sqlpower.sql.jdbcwrapper;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * The PostgresConnectionDecorator makes sure that the special PostgresDatabaseMetaDataDecorator
 * class wraps the database metadata returned by the PostgreSQL driver.
 *
 * @author fuerth
 * @version $Id$
 */
public class PostgresConnectionDecorator extends ConnectionDecorator {
    
    /**
     * Creates a new PostgresConnectionDecorator.
     * 
     * @param delegate an instance of the PostgreSQL Connection object.
     */
    public PostgresConnectionDecorator(Connection delegate) {
        super(delegate);
    }
    
    public DatabaseMetaData getMetaData() throws SQLException {
        return new PostgresDatabaseMetaDataDecorator(super.getMetaData());
    }

	@Override
	protected PreparedStatement makePreparedStatementDecorator(
			PreparedStatement pstmt) {
		return new GenericPreparedStatementDecorator(this, pstmt);
	}

	@Override
	protected Statement makeStatementDecorator(Statement stmt) {
		return new GenericStatementDecorator(this, stmt);
	}
}
