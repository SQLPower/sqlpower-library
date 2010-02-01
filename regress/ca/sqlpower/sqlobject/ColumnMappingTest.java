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
import ca.sqlpower.sqlobject.SQLRelationship.ColumnMapping;

public class ColumnMappingTest extends BaseSQLObjectTestCase {
	
	/**
     * Set up by {@link #setUp()} to have this structure:
     * <pre>
     * CREATE TABLE parent (
     *   pkcol_1 INTEGER NOT NULL,
     *   pkcol_2 INTEGER NOT NULL,
     *   attribute_1 INTEGER NOT NULL
     * );
     * </pre>
     * <p>
     * Note there are no columns in this table's primary key.
     */
	private SQLTable parentTable;
    
    /**
     * Set up by {@link #setUp()} to have this structure:
     * <pre>
     * CREATE TABLE parent (
     *   child_pkcol_1 INTEGER NOT NULL,
     *   child_pkcol_2 INTEGER NOT NULL,
     *   child_attribute INTEGER NOT NULL
     * );
     * </pre>
     * <p>
     * Note there are no columns in this table's primary key.
     */
	private SQLTable childTable1;
    
    /**
     * Set up by {@link #setUp()} to have this structure:
     * <pre>
     * CREATE TABLE parent (
     *   child2_pkcol_1 INTEGER NOT NULL,
     *   child2_pkcol_2 INTEGER NOT NULL,
     *   child2_attribute INTEGER NOT NULL
     * );
     * </pre>
     * <p>
     * Note there are no columns in this table's primary key.
     */
	private SQLTable childTable2;
    
	private SQLRelationship rel1;
    
    /**
     * The SQLDatabase that contains parentTable, childTable1, childTable2,
     * rel1, and rel2 after {@link #setUp()} has run.
     */
	private SQLDatabase database;

	/**
	 * The object under test.
	 */
	private ColumnMapping columnMapping;

	public ColumnMappingTest(String name) throws Exception {
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
		
		SQLIndex parentTablePK = new SQLIndex();
		parentTablePK.setPrimaryKeyIndex(true);
		parentTablePK.addChild(new Column(pkcol1, AscendDescend.UNSPECIFIED));
		parentTablePK.addChild(new Column(pkcol2, AscendDescend.UNSPECIFIED));
		parentTablePK.setName("parentTable_pk");
		parentTable.addIndex(parentTablePK);
		database.addChild(parentTable);
		
		childTable1 = new SQLTable(database, "child_1", null, "TABLE", true);
		childTable1.addColumn(new SQLColumn(childTable1, "child_pkcol_1", Types.INTEGER, 10, 0));
		childTable1.addColumn(new SQLColumn(childTable1, "child_pkcol_2", Types.INTEGER, 10, 0));
		childTable1.addColumn(new SQLColumn(childTable1, "child_attribute", Types.INTEGER, 10, 0));
		database.addChild(childTable1);
		
		childTable2 = new SQLTable(database, "child_2", null, "TABLE", true);
		childTable2.addColumn(new SQLColumn(childTable2, "child2_pkcol_1", Types.INTEGER, 10, 0));
		childTable2.addColumn(new SQLColumn(childTable2, "child2_pkcol_2", Types.INTEGER, 10, 0));
		childTable2.addColumn(new SQLColumn(childTable2, "child2_attribute", Types.INTEGER, 10, 0));
		database.addChild(childTable2);
		
		rel1 = new SQLRelationship();
		rel1.setIdentifying(true);
		rel1.attachRelationship(parentTable,childTable1,false);
		rel1.setName("rel1");
		rel1.addMapping(parentTable.getColumn(0), childTable1.getColumn(0));
		rel1.addMapping(parentTable.getColumn(1), childTable1.getColumn(1));
	
		columnMapping = rel1.getMappingByPkCol(parentTable.getColumn(0));
	}

	@Override
	protected Class<? extends SPObject> getChildClassType() {
		return null;
	}

	@Override
	protected SQLObject getSQLObjectUnderTest() throws SQLObjectException {
		return columnMapping;
	}

}
