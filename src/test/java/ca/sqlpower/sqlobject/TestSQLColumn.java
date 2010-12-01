/*
 * Copyright (c) 2008, SQL Power Group Inc.
 *
 * This file is part of Power*Architect.
 *
 * Power*Architect is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * Power*Architect is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>. 
 */
package ca.sqlpower.sqlobject;

import java.sql.DatabaseMetaData;
import java.sql.Types;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.beanutils.BeanUtils;

import ca.sqlpower.dao.SPPersisterListener;
import ca.sqlpower.object.CountingSPPersister;
import ca.sqlpower.object.SPObject;
import ca.sqlpower.sql.JDBCDataSource;
import ca.sqlpower.sql.JDBCDataSourceType;
import ca.sqlpower.sql.PlDotIni;
import ca.sqlpower.sqlobject.SQLTypePhysicalPropertiesProvider.PropertyType;
import ca.sqlpower.testutil.TestUtils;


public class TestSQLColumn extends BaseSQLObjectTestCase {

	// ============ Instance Variables =============

	/**
	 * A table with one primary key column.  Gets set up in setUp().
	 */
	private SQLTable table1pk;

	/**
	 * A table with no primary key.  Gets set up in setUp().
	 */
	private SQLTable table0pk;
	
	/**
	 * A table with three primary key columns.  Gets set up in setUp().
	 */
	private SQLTable table3pk;

	
	// ============= SetUp/TearDown and Utility Methods =============
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		
        sqlx("CREATE TABLE SQL_COLUMN_TEST_1PK (\n" +
                " cow numeric(11),\n" +
                " moo varchar(10),\n" +
                " foo char(10),\n" +
                " CONSTRAINT test1pk PRIMARY KEY (cow))");
        
        sqlx("CREATE TABLE SQL_COLUMN_TEST_3PK (\n" +
                " cow numeric(11) NOT NULL,\n" +
                " moo varchar(10) NOT NULL,\n" +
                " foo char(10) NOT NULL,\n" +
                " CONSTRAINT test3pk PRIMARY KEY (cow, moo, foo))");
        
        sqlx("CREATE TABLE SQL_COLUMN_TEST_0PK (\n" +
                " cow numeric(11),\n" +
                " moo varchar(10),\n" +
                " foo char(10))");

		table1pk = db.getTableByName("SQL_COLUMN_TEST_1PK");
		table0pk = db.getTableByName("SQL_COLUMN_TEST_0PK");
		table3pk = db.getTableByName("SQL_COLUMN_TEST_3PK");
	}

	/**
	 * Returns one of the columns of one of the tables that setUp made.  Right now, it's
	 * table1pk's first column.
	 * @throws SQLObjectException 
	 */
	@Override
	protected SQLObject getSQLObjectUnderTest() throws SQLObjectException {
		return table1pk.getColumn(0);
	}
	
	@Override
    protected Class<? extends SPObject> getChildClassType() {
    	return null;
    }
	
	// ================= Constructor ====================
	
	public TestSQLColumn(String name) throws Exception {
		super(name);
	}

	
	// =============== The tests! ================
	
	public void testPopulateTable() throws SQLObjectException {
		
		assertFalse("Table columns should not have been populated already",
				table1pk.isColumnsPopulated());
		table1pk.populateColumns();
		assertTrue("Table columns should be populated",
				table1pk.isColumnsPopulated());

		// spot-check that expected columns exist
		assertNotNull("cow column not found", table1pk.getColumnByName("cow"));
		assertNotNull("moo column not found", table1pk.getColumnByName("moo"));
		assertNotNull("foo column not found", table1pk.getColumnByName("foo"));
	}
	
	public void testColumnOwnership() throws Exception {
		SQLColumn cowCol = table1pk.getColumnByName("cow");
		SQLColumn mooCol = table1pk.getColumnByName("moo");
		SQLColumn fooCol = table1pk.getColumnByName("foo");
		
		// check that all columns are owned by the correct table
		assertEquals("column doesn't belong to correct parent!", table1pk, cowCol.getParent());
		assertEquals("column doesn't belong to correct parent!", table1pk, mooCol.getParent());
		assertEquals("column doesn't belong to correct parent!", table1pk, fooCol.getParent());
	}
	
    public void testReferenceCountFiresEvents() {
        SQLColumn col = new SQLColumn();
        TestingSQLObjectListener testListener = new TestingSQLObjectListener();
        col.addSPListener(testListener);
        assertEquals("Strange the test listener has recieved events",testListener.getChangedCount(),0);
        col.addReference();
        assertEquals("Incorrect number of change events!",testListener.getChangedCount(),1);
        col.removeReference();
        assertEquals("Incorrect number of change events!",testListener.getChangedCount(),2);
    }
    
	public void testPKAttributes() throws Exception {
		SQLColumn cowCol = table1pk.getColumnByName("cow");
		SQLColumn fooCol = table1pk.getColumnByName("foo");

		// check for PK vs non PK attributes
		assertTrue("table1pk.cow should have been flagged as PK", cowCol.isPrimaryKey());
		assertEquals("table1pk.cow nullability incorrect", cowCol.getNullable(), DatabaseMetaData.columnNoNulls);
		assertFalse("table1pk.cow isDefinitelyNullable incorrect", cowCol.isDefinitelyNullable());

		assertFalse("table1pk.foo should NOT have been flagged as PK", fooCol.isPrimaryKey());
		assertEquals("table1pk.foo nullability incorrect", fooCol.getNullable(), DatabaseMetaData.columnNullable);
		assertTrue("table1pk.foo isDefinitelyNullable incorrect", fooCol.isDefinitelyNullable());
	}
	
	public void testCompareTo() throws Exception {
		SQLColumn cowCol = table1pk.getColumnByName("cow");
		SQLColumn mooCol = table1pk.getColumnByName("moo");
		SQLColumn fooCol = table1pk.getColumnByName("foo");

		// check column name comparator
		Comparator<SQLColumn> nameComp = new SQLColumn.ColumnNameComparator();
		assertTrue(nameComp.compare(cowCol, mooCol) < 0);
		assertTrue(nameComp.compare(mooCol, fooCol) > 0);
		assertTrue(nameComp.compare(fooCol, cowCol) > 0);
		assertTrue(nameComp.compare(cowCol, fooCol) < 0);
		assertTrue(nameComp.compare(cowCol, cowCol) == 0);
		cowCol.setName(mooCol.getName());
		assertTrue(nameComp.compare(cowCol, mooCol) == 0);
	}
	
	public void testNoArgConstructor() throws Exception {
		SQLColumn col = new SQLColumn();
		assertEquals("Reference count init", 1, col.getReferenceCount());
		assertEquals(1, col.getChildCount());
	}
	public void testSmallConstructor() throws Exception {
		SQLColumn col = new SQLColumn(table0pk, "test_column", Types.INTEGER, 10, 30);
		assertEquals(table0pk, col.getParent());
		assertEquals("test_column", col.getName());
		assertEquals(Types.INTEGER, col.getType());
		assertEquals(10, col.getPrecision());
		assertEquals(30, col.getScale());
		assertEquals(1, col.getReferenceCount());
		assertEquals(1, col.getChildCount());
	}
	
	public void testMegaConstructor() throws Exception {
		SQLColumn col = new SQLColumn(table0pk,
				"test_column_2", Types.INTEGER, "my_test_integer",
				44, 33, DatabaseMetaData.columnNullable, "test remarks",
				"test default", true);
		assertEquals(table0pk, col.getParent());
		assertEquals("test_column_2", col.getName());
		assertEquals(Types.INTEGER, col.getType());
		assertEquals("my_test_integer", col.getSourceDataTypeName());
		assertEquals(44, col.getPrecision());
		assertEquals(33, col.getScale());
		assertEquals(DatabaseMetaData.columnNullable, col.getNullable());
		assertEquals("test remarks", col.getRemarks());
		assertEquals("test default", col.getDefaultValue());
		assertTrue(col.isAutoIncrement());
		assertEquals(1, col.getReferenceCount());
		assertEquals(1, col.getChildCount());
	}
	
	public void testGetDerivedInstance() throws Exception {
		SQLColumn origCol = table1pk.getColumn(0);
		table1pk.addChild(origCol);

		Set<String> propsToIgnore = new HashSet<String>();
		propsToIgnore.add("parentTable");
		propsToIgnore.add("parent");
		propsToIgnore.add("SQLParent");
		propsToIgnore.add("sourceColumn");
		propsToIgnore.add("sourceDataTypeName");
		propsToIgnore.add("SPListeners");
		propsToIgnore.add("foreignKey");
		propsToIgnore.add("indexed");
        propsToIgnore.add("uniqueIndexed");
        propsToIgnore.add("magicEnabled");
        propsToIgnore.add("referenceCount");
        propsToIgnore.add("UUID");
        propsToIgnore.add("primaryKey");
        
        propsToIgnore.add("children");
        propsToIgnore.add("childrenWithoutPopulating");
        propsToIgnore.add("userDefinedSQLType");
        propsToIgnore.add("variableResolver");

		TestUtils.setAllInterestingProperties(origCol, propsToIgnore);
		origCol.setSourceDataTypeName("NUMERIC");
		
		origCol.setAutoIncrementSequenceName("custom_sequence_name");  // supress auto-generate behaviour
		SQLColumn derivCol = origCol.createInheritingInstance(table3pk);
		table3pk.addChild(derivCol);
		table3pk.setPopulated(true);
		
		// These should be the only differences between origCol and derivCol
		assertEquals(table3pk, derivCol.getParent());
		assertEquals(origCol, derivCol.getSourceColumn());
		assertEquals("NUMERIC", derivCol.getSourceDataTypeName());
        
        Map<String,Object> origProps = (Map<String,Object>) BeanUtils.describe(origCol);
        Map<String,Object> derivProps = (Map<String,Object>) BeanUtils.describe(derivCol);
        
        origProps.keySet().removeAll(propsToIgnore);
        derivProps.keySet().removeAll(propsToIgnore);
        
        for (Map.Entry<String, Object> property : origProps.entrySet()) {
			assertEquals("Property \"" + property.getKey() + "\" differs", property.getValue(), derivProps.get(property.getKey()));
		}
	}
	

	/*
	 * Test method for 'ca.sqlpower.sqlobject.SQLColumn.getName()'
	 */
	public void testGetName() throws Exception {
		SQLColumn tmpCol = new SQLColumn();
		tmpCol.setName("cow");
		assertEquals(tmpCol.getName(),"cow");
		
		SQLColumn cowCol = table0pk.getColumnByName("COW");
		assertEquals(cowCol.getName(),"COW");
		
		SQLColumn mooCol = table0pk.getColumn(1);
		assertEquals(mooCol.getName(), "MOO");
		
	}

	/*
	 * Test method for 'ca.sqlpower.sqlobject.SQLColumn.getPhysicalName()'
	 */
	public void testGetPhysicalName() throws Exception {
		SQLColumn tmpCol = new SQLColumn();
		tmpCol.setPhysicalName("cow");
		assertEquals(tmpCol.getPhysicalName(),"cow");
	}

	/*
	 * Test method for 'ca.sqlpower.sqlobject.SQLColumn.getParent()'
	 */
	public void testGetParent() throws SQLObjectException {
		SQLColumn tmpCol = new SQLColumn();
		assertEquals(tmpCol.getParent(),null);
		table0pk.addColumn(tmpCol);
		assertEquals(table0pk,tmpCol.getParent());
	}

	/*
	 * Test method for 'ca.sqlpower.sqlobject.SQLColumn.populate()'
	 */
	public void testPopulate() throws Exception {
		SQLColumn tmpCol = new SQLColumn();
		assertEquals(true,tmpCol.isPopulated());
		tmpCol.populate();
		assertEquals(true,tmpCol.isPopulated());
		
		SQLColumn cowCol = table1pk.getColumn(0);
		assertEquals(true,cowCol.isPopulated());
		cowCol.populate();
		assertEquals(true,cowCol.isPopulated());
	}
	
	/*
	 * Test method for 'ca.sqlpower.sqlobject.SQLColumn.getShortDisplayName()'
	 */
	public void testGetShortDisplayName() throws Exception {
		SQLColumn cowCol = table1pk.getColumn(0);
		int idx = cowCol.getShortDisplayName().indexOf("COW");
		assertFalse(idx<0);		
	}

	/*
	 * Test method for 'ca.sqlpower.sqlobject.SQLColumn.allowsChildren()'
	 */
	public void testAllowsChildren() throws Exception {
		SQLColumn tmpCol = new SQLColumn();
		assertEquals(true,tmpCol.allowsChildren());
		
		SQLColumn cowCol = table1pk.getColumn(0);
		assertEquals(true,cowCol.allowsChildren());
	}

	/*
	 * Test method for 'ca.sqlpower.sqlobject.SQLColumn.addColumnsToTable(SQLTable, String, String, String)'
	 */
	public void testAddColumnsToTable() throws Exception {
		SQLColumn mooCol = table3pk.getColumn(1);
		assertEquals(table3pk, mooCol.getParent());
		assertEquals(1, mooCol.getChildCount());
		assertEquals("MOO", mooCol.getName());
		assertEquals(0, mooCol.getScale());
		assertEquals(10, mooCol.getPrecision());
		assertEquals(Types.VARCHAR, mooCol.getType());
		assertEquals("VARCHAR", mooCol.getSourceDataTypeName());
		assertEquals("", mooCol.getRemarks());
		assertEquals(null, mooCol.getDefaultValue());
		assertEquals(true, mooCol.isPrimaryKey());
		assertEquals(false, mooCol.isDefinitelyNullable());
	}

	/*
	 * Test method for 'ca.sqlpower.sqlobject.SQLColumn.toString()'
	 */
	public void testToString() throws Exception {
		SQLColumn cowCol = table1pk.getColumn(0);
		int idx = cowCol.getShortDisplayName().indexOf("COW");
		assertFalse(idx<0);	
	}


	/*
	 * Test method for 'ca.sqlpower.sqlobject.SQLColumn.getSourceColumn()'
	 */
	public void testGetSourceColumn() throws Exception {
		SQLColumn tmpCol = new SQLColumn();
		assertNull(tmpCol.getSourceColumn());
		SQLColumn cowCol = table1pk.getColumn(0);
		assertNull(cowCol.getSourceColumn());
		
		tmpCol = cowCol.createInheritingInstance(table3pk);
		assertEquals(table3pk, tmpCol.getParent());
		assertEquals(cowCol, tmpCol.getSourceColumn());
		
		tmpCol = new SQLColumn().createInheritingInstance(table3pk);
		assertNull(tmpCol.getSourceColumn());
	}

	/*
	 * Test method for 'ca.sqlpower.sqlobject.SQLColumn.setSourceColumn(SQLColumn)'
	 */
	public void testSetSourceColumn() throws Exception {
		SQLColumn tmpCol = new SQLColumn();
		assertNull(tmpCol.getSourceColumn());
		SQLColumn cowCol = table1pk.getColumn(0);
		assertNull(cowCol.getSourceColumn());
		
		tmpCol.setSourceColumn(cowCol);
		assertEquals(cowCol, tmpCol.getSourceColumn());
	}

	/*
	 * Test method for 'ca.sqlpower.sqlobject.SQLColumn.getType()'
	 */
	public void testGetType() throws Exception {
		SQLColumn tmpCol = new SQLColumn();
		tmpCol.setType(Types.CHAR);
		assertEquals(Types.CHAR,tmpCol.getType());
		SQLColumn cowCol = table1pk.getColumn(0);
		assertEquals(Types.NUMERIC,cowCol.getType());
		cowCol.setType(Types.CHAR);
		assertEquals(Types.CHAR,cowCol.getType());
	}

	
	/*
	 * Test method for 'ca.sqlpower.sqlobject.SQLColumn.getSourceDataTypeName()'
	 */
	public void testGetSourceDataTypeName() throws Exception {
		SQLColumn tmpCol = new SQLColumn();
		tmpCol.setSourceDataTypeName("xxx");
		assertEquals("xxx",tmpCol.getSourceDataTypeName());
		SQLColumn cowCol = table1pk.getColumn(0);
		assertEquals("NUMERIC",cowCol.getSourceDataTypeName());
		cowCol.setSourceDataTypeName("yyy");
		assertEquals("yyy",cowCol.getSourceDataTypeName());
	}

	/*
	 * Test method for 'ca.sqlpower.sqlobject.SQLColumn.getScale()'
	 */
	public void testGetScale() throws Exception {
		SQLColumn tmpCol = new SQLColumn();
		assertEquals(0,tmpCol.getScale());
		tmpCol.setScaleType(PropertyType.VARIABLE);
		tmpCol.setScale(123);
		assertEquals(123,tmpCol.getScale());
		SQLColumn cowCol = table1pk.getColumn(0);
		assertEquals(0,cowCol.getScale());
		cowCol.setScaleType(PropertyType.VARIABLE);
		cowCol.setScale(321);
		assertEquals(321,cowCol.getScale());
	}

	/*
	 * Test method for 'ca.sqlpower.sqlobject.SQLColumn.getPrecision()'
	 */
	public void testGetPrecision() throws Exception {
		SQLColumn tmpCol = new SQLColumn();
		tmpCol.setPrecision(123);
		assertEquals(123,tmpCol.getPrecision());
		SQLColumn cowCol = table1pk.getColumn(0);
		assertEquals(11,cowCol.getPrecision());
		cowCol.setPrecision(321);
		assertEquals(321,cowCol.getPrecision());
	}

	/*
	 * Test method for 'ca.sqlpower.sqlobject.SQLColumn.isDefinitelyNullable()'
	 */
	public void testIsDefinitelyNullable() throws Exception {
		SQLColumn tmpCol = new SQLColumn();
		assertEquals(false,tmpCol.isDefinitelyNullable());
		tmpCol.setNullable(DatabaseMetaData.columnNullable);
		assertEquals(true,tmpCol.isDefinitelyNullable());
		tmpCol.setNullable(DatabaseMetaData.columnNoNulls);
		assertEquals(false,tmpCol.isDefinitelyNullable());
		tmpCol.setNullable(DatabaseMetaData.columnNullableUnknown);
		assertEquals(false,tmpCol.isDefinitelyNullable());
		
		SQLColumn cowCol = table1pk.getColumn(0);

		assertEquals(false,cowCol.isDefinitelyNullable());
		cowCol.setNullable(DatabaseMetaData.columnNullable);
		assertEquals(true,cowCol.isDefinitelyNullable());
		cowCol.setNullable(DatabaseMetaData.columnNoNulls);
		assertEquals(false,cowCol.isDefinitelyNullable());
		cowCol.setNullable(DatabaseMetaData.columnNullableUnknown);
		assertEquals(false,cowCol.isDefinitelyNullable());
		
	}

	/*
	 * Test method for 'ca.sqlpower.sqlobject.SQLColumn.isPrimaryKey()'
	 */
	public void testIsPrimaryKey() throws Exception {
		SQLColumn tmpCol = new SQLColumn();
		SQLTable table = new SQLTable(db, true);
		table.addColumnWithoutPopulating(tmpCol, false, 0);
		assertEquals(false,tmpCol.isPrimaryKey());
		table.addToPK(tmpCol);
		assertEquals(true,tmpCol.isPrimaryKey());
		table.moveAfterPK(tmpCol);
		assertEquals(false,tmpCol.isPrimaryKey());
		
		SQLColumn cowCol = table3pk.getColumn(0);
		assertEquals(true,cowCol.isPrimaryKey());
		table3pk.addToPK(cowCol);
		assertEquals(true,cowCol.isPrimaryKey());
		table3pk.moveAfterPK(cowCol);
		assertEquals(false,cowCol.isPrimaryKey());
		
	}

	/*
	 * Test method for 'ca.sqlpower.sqlobject.SQLColumn.getParentTable()'
	 */
	public void testGetParentTable() throws Exception {
		SQLColumn tmpCol = new SQLColumn();
		assertEquals(null,tmpCol.getParent());
		table0pk.addColumn(tmpCol);
		assertEquals(table0pk,tmpCol.getParent());
		table0pk.removeChild(tmpCol);
		assertEquals(null,tmpCol.getParent());

		SQLColumn cowCol = table3pk.getColumn(0);
		assertEquals(table3pk,cowCol.getParent());
		table3pk.removeChild(cowCol);
		assertEquals(null,cowCol.getParent());
		
		table0pk.addColumn(cowCol);
		assertEquals(table0pk,cowCol.getParent());
		table0pk.removeChild(cowCol);
		assertEquals(null,cowCol.getParent());
	}

	/*
	 * Test method for 'ca.sqlpower.sqlobject.SQLColumn.getNullable()'
	 */
	public void testGetNullable() throws Exception {
		SQLColumn tmpCol = new SQLColumn();
		assertEquals(DatabaseMetaData.columnNoNulls,tmpCol.getNullable());
		tmpCol.setNullable(DatabaseMetaData.columnNullable);
		assertEquals(DatabaseMetaData.columnNullable,tmpCol.getNullable());
		tmpCol.setNullable(DatabaseMetaData.columnNullableUnknown);
		assertEquals(DatabaseMetaData.columnNullableUnknown,tmpCol.getNullable());
		tmpCol.setNullable(DatabaseMetaData.columnNoNulls);
		assertEquals(DatabaseMetaData.columnNoNulls,tmpCol.getNullable());
		
		SQLColumn cowCol = table1pk.getColumn(0);
		assertEquals(DatabaseMetaData.columnNoNulls,cowCol.getNullable());
		cowCol.setNullable(DatabaseMetaData.columnNullable);
		assertEquals(DatabaseMetaData.columnNullable,cowCol.getNullable());
		cowCol.setNullable(DatabaseMetaData.columnNullableUnknown);
		assertEquals(DatabaseMetaData.columnNullableUnknown,cowCol.getNullable());
		cowCol.setNullable(DatabaseMetaData.columnNoNulls);
		assertEquals(DatabaseMetaData.columnNoNulls,cowCol.getNullable());
	}

	/*
	 * Test method for 'ca.sqlpower.sqlobject.SQLColumn.getRemarks()'
	 */
	public void testGetRemarks() throws Exception {
		SQLColumn tmpCol = new SQLColumn();
		tmpCol.setRemarks("xxx");
		assertEquals("xxx",tmpCol.getRemarks());
		SQLColumn cowCol = table1pk.getColumn(0);
		assertEquals("",cowCol.getRemarks());
		cowCol.setRemarks("yyy");
		assertEquals("yyy",cowCol.getRemarks());
	}

	/*
	 * Test method for 'ca.sqlpower.sqlobject.SQLColumn.getDefaultValue()'
	 */
	public void testGetDefaultValue() throws Exception {
		SQLColumn tmpCol = new SQLColumn();
		assertEquals(null,tmpCol.getDefaultValue());
		tmpCol.setDefaultValue("xxx");
		assertEquals("xxx",tmpCol.getDefaultValue());
		SQLColumn cowCol = table1pk.getColumn(0);
		assertEquals(null,cowCol.getDefaultValue());
		cowCol.setDefaultValue("yyy");
		assertEquals("yyy",cowCol.getDefaultValue());
	}

	/*
	 * Test method for 'ca.sqlpower.sqlobject.SQLColumn.isAutoIncrement()'
	 */
	public void testIsAutoIncrement() throws Exception {
		SQLColumn tmpCol = new SQLColumn();
		assertEquals(false,tmpCol.isAutoIncrement());
		tmpCol.setAutoIncrement(true);
		assertEquals(true,tmpCol.isAutoIncrement());
		tmpCol.setAutoIncrement(false);
		assertEquals(false,tmpCol.isAutoIncrement());
		
		SQLColumn cowCol = table1pk.getColumn(0);
		assertEquals(false,cowCol.isAutoIncrement());
		cowCol.setAutoIncrement(true);
		assertEquals(true,cowCol.isAutoIncrement());
		cowCol.setAutoIncrement(false);
		assertEquals(false,cowCol.isAutoIncrement());
	}

	/*
	 * Test method for SQLColumn(SQLColumn)
	 */
	public void testCopyConstructor() throws Exception {
		SQLColumn cowCol = table1pk.getColumn(0);
		SQLTable table = new SQLTable(new StubSQLObject(), "", "", "", true);
		cowCol.setParent(table);
        cowCol.setAutoIncrementSequenceName("custom_sequence_name"); // supress auto-generate behaviour
		SQLColumn tmpCol = new SQLColumn(cowCol);
		tmpCol.setParent(table);
		
		Set<String> propsToIgnore = new HashSet<String>();
		propsToIgnore.add("parentTable");
		propsToIgnore.add("parent");
        propsToIgnore.add("SPListeners");
        propsToIgnore.add("foreignKey");
        propsToIgnore.add("indexed");
        propsToIgnore.add("uniqueIndexed");
        propsToIgnore.add("UUID");
		
        propsToIgnore.add("children");
        propsToIgnore.add("childrenWithoutPopulating");
        propsToIgnore.add("userDefinedSQLType");
        propsToIgnore.add("variableResolver");
        
		Map<String,Object> origProps = (Map<String,Object>) BeanUtils.describe(cowCol);
		Map<String,Object> derivProps = (Map<String,Object>) BeanUtils.describe(tmpCol);
		
		origProps.keySet().removeAll(propsToIgnore);
		derivProps.keySet().removeAll(propsToIgnore);

		for (Map.Entry<String, Object> property : origProps.entrySet()) {
			assertEquals("Property \"" + property.getKey() + "\" differs", property.getValue(), derivProps.get(property.getKey()));
		}
	}

	/*
	 * Test method for 'ca.sqlpower.sqlobject.SQLColumn.addReference()'
	 */
	public void testAddReference() throws Exception {
		int count = 1;
		SQLColumn tmpCol = new SQLColumn();
		assertEquals(count,tmpCol.getReferenceCount());
		tmpCol.addReference();
		count++;
		assertEquals(count,tmpCol.getReferenceCount());
		
		tmpCol.addReference();
		count++;
		assertEquals(count,tmpCol.getReferenceCount());
		
		tmpCol.removeReference();
		count--;
		assertEquals(count,tmpCol.getReferenceCount());
		
	
		SQLColumn cowCol = table1pk.getColumn(0);
		count = 1;
		assertEquals(count,cowCol.getReferenceCount());
		cowCol.addReference();
		count++;
		assertEquals(count,cowCol.getReferenceCount());
		
		cowCol.addReference();
		count++;
		assertEquals(count,cowCol.getReferenceCount());
		
		cowCol.removeReference();
		count--;
		assertEquals(count,cowCol.getReferenceCount());
		
	}


	/*
	 * Test method for 'ca.sqlpower.sqlobject.SQLObject.setPopulated(boolean)'
	 */
	public void testSetPopulated() throws Exception {
		SQLColumn tmpCol = new SQLColumn();
		tmpCol.setPopulated(false);
		assertEquals(tmpCol.isPopulated(),true);
		tmpCol.setPopulated(true);
		assertEquals(tmpCol.isPopulated(),true);
		
		SQLColumn cowCol = table3pk.getColumn(0);
		cowCol.setPopulated(false);
		assertEquals(cowCol.isPopulated(),true);
		cowCol.setPopulated(true);
		assertEquals(cowCol.isPopulated(),true);
	}

	/*
	 * Test method for 'ca.sqlpower.sqlobject.SQLObject.getChildren()'
	 */
	public void testGetChildren() throws Exception {
		SQLColumn tmpCol = new SQLColumn();
		
		try {
			tmpCol.addChild(new SQLColumn(),0);
			fail("SQLColumn should not have child");
		} catch (IllegalArgumentException e) {
			/* it's normal */
		}
		assertEquals(tmpCol.getChildren().size(),1);
		
		SQLColumn cowCol = table3pk.getColumn(0);
		try {
			cowCol.addChild(new SQLColumn(),1);
			fail("SQLColumn should not have child");
		} catch (IllegalArgumentException e) {
			/* it's normal */
		}
		assertEquals(cowCol.getChildren().size(),1);
		
	}

	/*
	 * Test method for 'ca.sqlpower.sqlobject.SQLObject.getChild(int)'
	 */
	public void testGetChild() throws Exception {
		SQLColumn tmpCol = new SQLColumn();
		try {
			tmpCol.getChild(0);
			tmpCol.getChild(1);
		} catch (IndexOutOfBoundsException e1) {
			// it's normal
		}
				
		SQLColumn cowCol = table3pk.getColumn(0);
		try {
			cowCol.getChild(0);
			cowCol.getChild(1);
		} catch (IndexOutOfBoundsException e1) {
			// it's normal
		}
	}

	/*
	 * Test method for 'ca.sqlpower.sqlobject.SQLObject.getChildCount()'
	 */
	public void testGetChildCount() throws Exception {
		SQLColumn tmpCol = new SQLColumn();
		int count = tmpCol.getChildCount();
		assertEquals(count,1);

		SQLColumn cowCol = table3pk.getColumn(0);
		count = cowCol.getChildCount();
		assertEquals(count,1);
	}

	/*
	 * Test method for 'ca.sqlpower.sqlobject.SQLObject.addChild(int, SQLObject)'
	 */
	public void testAddChildIntSQLObject() throws Exception {
		SQLColumn tmpCol = new SQLColumn();
		try {
			tmpCol.addChild(table1pk,0);
			fail();
			tmpCol.addChild(table3pk,2);
			fail();
			
		} catch ( IllegalArgumentException e ) {
		}


		SQLColumn cowCol = table3pk.getColumn(0);
		try {
			cowCol.addChild(table1pk,0);
			fail();
			cowCol.addChild(table3pk,2);
			fail();
		} catch ( IllegalArgumentException e ) {
		}
		
	}

	/*
	 * Test method for 'ca.sqlpower.sqlobject.SQLObject.addChild(SQLObject)'
	 */
	public void testAddChildSQLObject() throws Exception {
		SQLColumn tmpCol = new SQLColumn();
		try {
			tmpCol.addChild(table1pk);
			fail();
			tmpCol.addChild(table3pk);
			fail();
		} catch ( IllegalArgumentException e ) {
		}


		SQLColumn cowCol = table3pk.getColumn(0);
		try {
			cowCol.addChild(table1pk);
			fail();
			cowCol.addChild(table3pk);
			fail();
		} catch ( IllegalArgumentException e ) {
		}
	}

    public void testAutoGenerateSequenceNameWithParentTable() throws Exception {
        SQLColumn col = table1pk.getColumn(0);
        
        assertEquals(table1pk.getName()+"_"+col.getName()+"_seq", col.getAutoIncrementSequenceName());
    }

    public void testAutoGenerateSequenceNameNoParentTable() throws Exception {
        SQLColumn col = table1pk.getColumn(0);
        table1pk.removeColumn(0);
        
        assertEquals(col.getName()+"_seq", col.getAutoIncrementSequenceName());
    }
    
    public void testAutoIncrementSequenceNameNoStickyDefault() throws Exception {
        SQLColumn col = table1pk.getColumn(0);
        col.setAutoIncrementSequenceName(col.getAutoIncrementSequenceName());
        assertFalse(col.isAutoIncrementSequenceNameSet());
    }
    
    public void testReverseEngineerAutoInc() throws Exception {
        PlDotIni plini = new PlDotIni();

        JDBCDataSourceType dst = new JDBCDataSourceType();
        dst.setJdbcDriver("ca.sqlpower.testutil.MockJDBCDriver");
        plini.addDataSourceType(dst);

        JDBCDataSource ds = new JDBCDataSource(plini);
        String url = "jdbc:mock:tables=table1" +
        		"&columns.table1=pkcol,normalcol" +
        		"&autoincrement_cols=table1.pkcol";
        ds.setUrl(url);
        ds.setParentType(dst);
        ds.setUser("x");
        ds.setPass("x");
        plini.addDataSource(ds);
        
        SQLDatabase db = new SQLDatabase(ds);
        SQLTable t = db.getTableByName("table1");
        SQLColumn pkcol = t.getColumnByName("pkcol");
        SQLColumn normalcol = t.getColumnByName("normalcol");
        
        assertTrue(pkcol.isAutoIncrement());
        assertFalse(normalcol.isAutoIncrement());
    }
    
    @Override
    public void testAllChildHandlingMethods() throws SQLObjectException {
    	/*
    	 * Make sure that SQLColumn cannot add or remove children.
    	 */
    	SQLColumn col = (SQLColumn) getSQLObjectUnderTest();
    	
    	Exception cannotAddChild = null;
    	try {
    		col.addChild(new UserDefinedSQLType());
    	} catch (Exception e) {
    		cannotAddChild = e;
    	}
    	assertNotNull(cannotAddChild);
    }

    /**
     * Tests the SQLColumn and {@link UserDefinedSQLType} at the same time. If
     * you set the column's type through its setter the persister should see the
     * events come through correctly and not throw an exception. Previously the
     * old value of the last property change was not matching the new value of
     * the first property change when persisting the object.
     */
    public void testSetNullabilityAfterCreation() throws Exception {
        final CountingSPPersister persister = new CountingSPPersister();
        SPPersisterListener listener = new SPPersisterListener(persister, getConverter());
        SQLTable table = (SQLTable) createNewValueMaker(getRootObject(), new PlDotIni()).
            makeNewValue(SQLTable.class, null, "");
        table.addSPListener(listener);
        
        UserDefinedSQLType underlyingType = new UserDefinedSQLType();
        underlyingType.setMyAutoIncrement(false);
        underlyingType.setMyNullability(DatabaseMetaData.columnNullableUnknown);
        underlyingType.setType(Types.VARCHAR);
        
        table.begin("Transaction for testing");
        SQLColumn col = new SQLColumn(underlyingType);
        table.addColumn(col);
        
        int nullable = col.getNullable();
        if (nullable == DatabaseMetaData.columnNullable) {
            nullable = DatabaseMetaData.columnNullableUnknown;
        } else {
            nullable = DatabaseMetaData.columnNullable;
        }
        col.setNullable(nullable);
        
        table.commit();
        
    }
    
    @Override
    public Set<String> getPropertiesToIgnoreForEvents() {
    	Set<String> ignored = super.getPropertiesToIgnoreForEvents();
    	ignored.add("autoIncrement");
    	ignored.add("constraintType");
    	ignored.add("defaultValue");
    	ignored.add("enumeration");
    	ignored.add("nullable");
    	ignored.add("precision");
    	ignored.add("precisionType");
    	ignored.add("scale");
    	ignored.add("scaleType");
    	ignored.add("sourceDataTypeName");
    	ignored.add("type");
    	return ignored;
    }
    
    @Override
    public void testFiresAddEvent() {}
    @Override
    public void testPreRemoveEventNoVeto() {}
    @Override
    public void testPreRemoveEventVeto() {}
}
