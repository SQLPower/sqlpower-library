/*
 * Copyright (c) 2009, SQL Power Group Inc.
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

import java.io.File;
import java.sql.Statement;

import junit.framework.TestCase;
import ca.sqlpower.sql.JDBCDataSource;
import ca.sqlpower.sql.PlDotIni;
import ca.sqlpower.sqlobject.SQLDatabase;

public class GenericStatementDecoratorTest extends TestCase {
	
	private SQLDatabase db;
	PlDotIni plini;

	@Override
	protected void setUp() throws Exception {
		plini = new PlDotIni();
        plini.read(new File("pl.regression.ini"));
        db = new SQLDatabase(new JDBCDataSource(plini.getDataSource("regression_test", JDBCDataSource.class)));
        assertNotNull(db.getDataSource().getParentType());
	}
	
	/**
	 * If a statement decorator is used to execute multiple statements, some that are
	 * not select statements, then null result sets will be returned. This test
	 * ensures null result sets can be returned instead of null pointer exceptions
	 * being thrown.
	 */
	public void testGetResultSetReturnsNull() throws Exception {
		GenericConnectionDecorator conDecorator = new GenericConnectionDecorator(db.getConnection());
		Statement stmt = conDecorator.createStatement();
		assertTrue("The statement made by a GenericConnectionDecorator should be a GenericStatementDecorator " +
				"or this test is testing the wrong class", stmt instanceof GenericStatementDecorator);
		stmt.execute("Create table generic_statement_decorator (col1 varchar (50));");
		stmt.execute("insert into generic_statement_decorator (col1) values ('a');");
		assertNull(stmt.getResultSet()); //This should not throw an exception
	}

}
