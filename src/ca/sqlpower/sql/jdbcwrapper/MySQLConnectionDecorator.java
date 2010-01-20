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
 * The class exists mainly to wrap instances of DatabaseMetaData returned
 * by {@link #getMetaData()} in the appropriate decorator class.
 */
public class MySQLConnectionDecorator extends GenericConnectionDecorator {

    protected MySQLConnectionDecorator(Connection delegate) {
        super(delegate);
    }
    
    public void setCatalog (String catalog) throws SQLException {
    	String oldCatalog;
    	try {
    		oldCatalog = connection.getCatalog();
    	} catch (SQLException sqlEx) {
    		throw sqlEx;
    	}
    	try {
    		connection.setCatalog(catalog);
    	} catch (SQLException sqlEx) {
    		connection.setCatalog(oldCatalog);
    		throw sqlEx;
    	}
    }

    @Override
    public DatabaseMetaData getMetaData() throws SQLException {
       
    	if (databaseMetaDataDecorator == null) {
    		databaseMetaDataDecorator = new MySQLDatabaseMetaDataDecorator(super.getMetaData(), this);
    	}
    	
        return databaseMetaDataDecorator;
    }
}
