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
import java.sql.SQLException;

import org.apache.log4j.Logger;

/**
 * Tries to ensure catalogs and schemas are understood not to be supported.
 */
public class RedBrickDatabaseMetaDataDecorator extends DatabaseMetaDataDecorator {

    @SuppressWarnings("unused")
	private static final Logger logger = Logger
			.getLogger(RedBrickDatabaseMetaDataDecorator.class);

    /**
     * Wraps the given database metadata object. This is normally done for you
     * by the {@link RedBrickConnectionDecorator}.
     * 
     * @param delegate The database metadata object to wrap.
     */
	public RedBrickDatabaseMetaDataDecorator(DatabaseMetaData delegate) {
		super(delegate);
	}
	
	/**
	 * Returns null, because Red Brick doesn't support catalogs.
	 */
    @Override
    public String getCatalogTerm() throws SQLException {
        return null;
    }

    /**
     * Returns null, because Red Brick doesn't support schemas.
     */
    @Override
    public String getSchemaTerm() throws SQLException {
        return null;
    }
}
