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
package ca.sqlpower.sql;

import java.sql.Connection;
import java.sql.SQLException;

import org.apache.commons.dbcp.ConnectionFactory;
import org.apache.log4j.Logger;

/**
 * A simple wrapper around SPDataSource that lets it implement
 * the Apache DBCP ConnectionFactory interface.  We didn't want to pollute
 * SPDataSource itself with the Apache class dependency.
 */
public class SPDSConnectionFactory implements ConnectionFactory {
	private static final Logger logger = Logger.getLogger(SPDSConnectionFactory.class);
	
	private SPDataSource dataSource;
	
	public SPDSConnectionFactory(SPDataSource dataSource) {
		this.dataSource = dataSource;
	}
	
	public Connection createConnection() throws SQLException {
	    return dataSource.createConnection();
	}
}
