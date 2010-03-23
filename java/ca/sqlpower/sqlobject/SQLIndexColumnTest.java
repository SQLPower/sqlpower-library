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

package ca.sqlpower.sqlobject;

import java.sql.Types;

import ca.sqlpower.object.SPObject;
import ca.sqlpower.sqlobject.SQLIndex.AscendDescend;
import ca.sqlpower.sqlobject.SQLIndex.Column;

public class SQLIndexColumnTest extends BaseSQLObjectTestCase {

	private SQLDatabase database;
	private SQLTable parentTable;
	private Column column;

	public SQLIndexColumnTest(String name) throws Exception {
		super(name);
	}
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		
		database = new SQLDatabase();
		getRootObject().addChild(database, 0);
		parentTable = new SQLTable(database, "parent", null, "TABLE", true);
		SQLColumn pkcol1 = new SQLColumn(parentTable, "pkcol_1", Types.INTEGER, 10, 0);
		SQLColumn pkcol2 = new SQLColumn(parentTable, "pkcol_2", Types.INTEGER, 10, 0);
        parentTable.addColumn(pkcol1);
        parentTable.addColumn(pkcol2);
		parentTable.addColumn(new SQLColumn(parentTable, "attribute_1", Types.INTEGER, 10, 0));
		
		SQLIndex parentTablePK = parentTable.getPrimaryKeyIndex();
		column = new Column(pkcol1, AscendDescend.UNSPECIFIED);
		parentTablePK.addChild(column);
		parentTablePK.addChild(new Column(pkcol2, AscendDescend.UNSPECIFIED));
		parentTablePK.setName("parentTable_pk");
		database.addChild(parentTable);
		
	}

	@Override
	protected Class<? extends SPObject> getChildClassType() {
		return null;
	}

	@Override
	protected SQLObject getSQLObjectUnderTest() throws SQLObjectException {
		return column;
	}

}
