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
import java.sql.SQLException;

/**
 * Wraps instances of DatabaseMetaData returned by {@link #getMetaData()} in the
 * real Red Brick JDBC driver. Otherwise behaves exactly the same as the driver
 * it's wrapping.
 */
public class RedBrickConnectionDecorator extends GenericConnectionDecorator {

    /**
     * Wraps the given JDBC connection object, which is assumed to be a Red Brick
     * connection.
     * 
     * @param delegate The Red Brick connection to wrap.
     */
    protected RedBrickConnectionDecorator(Connection delegate) {
        super(delegate);
    }

    @Override
    public DatabaseMetaData getMetaData() throws SQLException {
        return new RedBrickDatabaseMetaDataDecorator(super.getMetaData());
    }
}
