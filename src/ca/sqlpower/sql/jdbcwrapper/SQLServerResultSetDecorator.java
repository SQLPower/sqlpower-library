/*
 * Copyright (c) 2010, SQL Power Group Inc.
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

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.log4j.Logger;

public class SQLServerResultSetDecorator extends ResultSetDecorator {
	
	public static final Logger logger = Logger.getLogger(SQLServerResultSetDecorator.class);

	public SQLServerResultSetDecorator (Statement parentStatement, ResultSet rs) {
		super(parentStatement, rs);
	}

	@Override
	protected ResultSetMetaData makeResultSetMetaDataDecorator (ResultSetMetaData rsmd) {
		return new SQLServerResultSetMetaDataDecorator(rsmd);
	}
	
	// -----------------------------------------------------------------------------------------------------
	
	/**
	 * first, last, next, and previous are all overridden here because in SQLServer (with the 2005 driver)
	 * they throw null pointer exceptions when the user does not have appropriate permissions.
	 */
	
	// -----------------------------------------------------------------------------------------------------
	
	@Override
	public boolean first () throws SQLException {
		ResultSetDecorator.checkInterrupted();
		try {
			return resultSet.first();
		} catch (NullPointerException npe) {
			logger.debug("NullPointerException (due to permissions [hopefully]) squished in call to 'first'");
			return false;
		}
	}
	
	@Override
	public boolean last () throws SQLException {
		ResultSetDecorator.checkInterrupted();
		try {
			return resultSet.last();
		} catch (NullPointerException npe) {
			logger.debug("NullPointerException (due to permissions [hopefully]) squished in call to 'last'");
			return false;
		}
	}
	
	@Override
	public boolean next () throws SQLException {
		ResultSetDecorator.checkInterrupted();
		try {
			return resultSet.next();
		} catch (NullPointerException npe) {
			logger.debug("NullPointerException (due to permissions [hopefully]) squished in call to 'next'");
			return false;
		}
	}
	
	@Override
	public boolean previous () throws SQLException {
		ResultSetDecorator.checkInterrupted();
		try {
			return resultSet.previous();
		} catch (NullPointerException npe) {
			logger.debug("NullPointerException (due to permissions [hopefully]) squished in call to 'previous'");
			return false;
		}
	}
}
