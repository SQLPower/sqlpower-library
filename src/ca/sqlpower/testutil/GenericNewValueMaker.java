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

package ca.sqlpower.testutil;

import java.awt.Font;
import java.awt.Image;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;

import ca.sqlpower.object.SPObject;
import ca.sqlpower.query.SQLGroupFunction;
import ca.sqlpower.sql.DataSourceCollection;
import ca.sqlpower.sql.JDBCDataSource;
import ca.sqlpower.sql.Olap4jDataSource;
import ca.sqlpower.sql.PlDotIni;
import ca.sqlpower.sql.SPDataSource;
import ca.sqlpower.sqlobject.SQLCatalog;
import ca.sqlpower.sqlobject.SQLColumn;
import ca.sqlpower.sqlobject.SQLDatabase;
import ca.sqlpower.sqlobject.SQLIndex;
import ca.sqlpower.sqlobject.SQLObjectException;
import ca.sqlpower.sqlobject.SQLObjectRoot;
import ca.sqlpower.sqlobject.SQLRelationship;
import ca.sqlpower.sqlobject.SQLSchema;
import ca.sqlpower.sqlobject.SQLTable;
import ca.sqlpower.sqlobject.SQLIndex.AscendDescend;
import ca.sqlpower.sqlobject.SQLIndex.Column;
import ca.sqlpower.sqlobject.SQLRelationship.ColumnMapping;
import ca.sqlpower.sqlobject.SQLRelationship.Deferrability;
import ca.sqlpower.sqlobject.SQLRelationship.SQLImportedKey;
import ca.sqlpower.sqlobject.SQLRelationship.UpdateDeleteRule;
import ca.sqlpower.util.DefaultUserPrompter;
import ca.sqlpower.util.UserPrompter;
import ca.sqlpower.util.UserPrompter.UserPromptOptions;
import ca.sqlpower.util.UserPrompter.UserPromptResponse;

/**
 * An implementation of NewValueMaker that recognizes classes in the Java SE
 * library and the SQL Power library. Apps can extend this class to also provide
 * awareness for their own types that show up in bean properties.
 */
public class GenericNewValueMaker implements NewValueMaker {
	
	/**
	 * See doc comment on constructor.
	 */
	private final SPObject root;
	private final DataSourceCollection<SPDataSource> pl;

	/**
	 * @param root
	 *            The absolute root object of the new value maker. All SPObjects
	 *            made by this class must be attached to this root object in
	 *            some way so they can be traversed by tests. The objects
	 *            created by this test do not have to be an immediate child of
	 *            the root for cases where they may need a specific type of
	 *            parent or ancestor which can be a child of the root. This object
	 *            must be able to accept children of any type that can be produced by
	 *            this new value maker.
	 */
	public GenericNewValueMaker(SPObject root) {
		this(root, new PlDotIni());
	}
	
	public GenericNewValueMaker(SPObject root, DataSourceCollection<SPDataSource> pl) {
		this.root = root;
		this.pl = pl;
	}
	
	protected SPObject getRootObject() {
		return root;
	}

    public Object makeNewValue(Class<?> valueType, Object oldVal, String propName) {
        Object newVal;  // don't init here so compiler can warn if the following code doesn't always give it a value
        if (valueType == Integer.TYPE) {
            newVal = ((Integer) oldVal)+1;
        } else if (valueType == Double.TYPE) {
            newVal = ((Double) oldVal)+1;
        } else if (valueType == Integer.class) {
            if (oldVal == null) {
                newVal = new Integer(1);
            } else {
                newVal = new Integer((Integer)oldVal+1);
            }
        } else if (valueType == Double.class) {
        	if (oldVal == null) {
                newVal = new Double(1);
            } else {
                newVal = new Double((Double)oldVal+1);
            }
        } else if (valueType == String.class) {
            // make sure it's unique
            newVal = "new " + oldVal;
        } else if (valueType == Boolean.TYPE){
            newVal = new Boolean(! ((Boolean) oldVal).booleanValue());
        } else if (valueType == File.class) {
            newVal = new File("temp" + System.currentTimeMillis());
        } else if (valueType == JDBCDataSource.class || valueType == SPDataSource.class) {
            newVal = new JDBCDataSource(this.pl);
            String name = "Testing data source";
            while (pl.getDataSource(name) != null) {
            	name = (String) makeNewValue(String.class, name, "");
            }
            ((SPDataSource)newVal).setName(name);
            this.pl.addDataSource((JDBCDataSource)newVal);
        } else if (valueType == Font.class) {
            newVal = Font.decode("Dialog");
            if (newVal.equals(oldVal)) {
                newVal = ((Font) newVal).deriveFont(((Font) newVal).getSize2D() + 1f);
            }
        }  else if (valueType.isAssignableFrom(NumberFormat.class)) {
        	if(oldVal == NumberFormat.getCurrencyInstance()) {
        		newVal = NumberFormat.getPercentInstance();
        	} else {
        		newVal = NumberFormat.getCurrencyInstance();
        	}
        } else if(valueType.isAssignableFrom(SimpleDateFormat.class)) {
        	if(oldVal == SimpleDateFormat.getDateTimeInstance()) {
        		newVal = SimpleDateFormat.getDateInstance();
        	} else {
        		newVal = SimpleDateFormat.getDateTimeInstance();
        	}
        } else if (valueType == SQLColumn.class) {
        	SQLColumn sqlCol = new SQLColumn();
        	sqlCol.setName("testing!");
        	SQLTable table = (SQLTable) makeNewValue(SQLTable.class, null, "Parent of column");
        	table.addColumnWithoutPopulating(sqlCol);
        	table.setPopulated(true);
        	newVal = sqlCol;
        } else if (valueType.isAssignableFrom(SQLTable.class)) {
        	SQLTable table = new SQLTable();
        	table.setName("Generated testing table");
        	table.setPopulated(true);
        	SQLDatabase db = (SQLDatabase) makeNewValue(SQLDatabase.class, null, "parent of table");
        	db.addTable(table);
        	newVal = table;
        } else if (valueType.isAssignableFrom(SQLDatabase.class)) {
        	SQLDatabase db = new SQLDatabase();
        	SQLObjectRoot sqlRoot = (SQLObjectRoot) makeNewValue(SQLObjectRoot.class, null, "parent of db");
        	sqlRoot.addDatabase(db, 0);
        	newVal = db;
        } else if (valueType.isAssignableFrom(SQLObjectRoot.class)) {
        	SQLObjectRoot sqlRoot = new SQLObjectRoot();
        	root.addChild(sqlRoot, 0);
        	newVal = sqlRoot;
        } else if (valueType.isAssignableFrom(Column.class)) {
        	Column col = new Column();
        	col.setName("Generated testing column index");
        	SQLIndex index = (SQLIndex) makeNewValue(SQLIndex.class, null, "parent of column");
        	index.setPopulated(true);
        	index.addIndexColumn(col);
        	newVal = col;
        } else if (valueType.isAssignableFrom(SQLIndex.class)) {
        	SQLIndex index = new SQLIndex();
        	index.setName("a new index");
        	SQLTable table = (SQLTable) makeNewValue(SQLTable.class, null, "parent of index");
        	table.addIndex(index);
        	table.setPopulated(true);
        	newVal = index;
        } else if (valueType.isAssignableFrom(ColumnMapping.class)) {
        	ColumnMapping mapping = new ColumnMapping();
        	mapping.setName("Generated testing mapping");
        	SQLRelationship rel = (SQLRelationship) makeNewValue(SQLRelationship.class, null, "parent of column mapping");
        	rel.addMapping(mapping);
        	newVal = mapping;
        } else if (valueType.isAssignableFrom(SQLRelationship.class)) {
        	SQLRelationship rel = new SQLRelationship();
        	SQLTable parent = (SQLTable) makeNewValue(SQLTable.class, null, "parent of relationship");
        	SQLTable child = (SQLTable) makeNewValue(SQLTable.class, null, "child of relationship");
        	try {
				rel.attachRelationship(parent, child, true);
			} catch (SQLObjectException e) {
				throw new RuntimeException("Trying to create a new relationship for testing", e);
			}
        	newVal = rel;
        } else if (valueType.isAssignableFrom(SQLSchema.class)) {
        	SQLSchema schema = new SQLSchema(true);
        	schema.setName("A new schema for testing");
        	SQLDatabase db = (SQLDatabase) makeNewValue(SQLDatabase.class, null, "Schema database");
        	db.addSchema(schema);
        	newVal = schema;
        } else if (valueType.isAssignableFrom(SQLCatalog.class)) {
        	SQLDatabase db = (SQLDatabase) makeNewValue(SQLDatabase.class, null, "Schema database");
        	SQLCatalog catalog = new SQLCatalog(db, "catalog for test", true);
        	db.addCatalog(catalog);
        	newVal = catalog;
        } else if (valueType.isAssignableFrom(Throwable.class)) {
        	newVal = new SQLObjectException("Test Exception");
        } else if (valueType == UserPrompter.class) {
            newVal = new DefaultUserPrompter(UserPromptOptions.OK_NEW_NOTOK_CANCEL, UserPromptResponse.CANCEL, null);
        } else if (valueType == Point2D.class) {
        	Point2D point = new Point2D(){
			
        		private double x;
        		private double y;
        		
				@Override
				public void setLocation(double x, double y) {
					this.x = x;
					this.y = y;
				}
			
				@Override
				public double getY() {
					return y;
				}
			
				@Override
				public double getX() {
					return x;
				}
			};
			if (oldVal == null) {
				point.setLocation(0, 0);
			} else {
				point.setLocation(((Point2D) oldVal).getX() + 1, ((Point2D) oldVal).getY() - 1);
			}
			return point;
        } else if (valueType.equals(Image.class)) {
            return new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
        } else if (valueType.equals(Olap4jDataSource.class)) {
        	Olap4jDataSource ds = new Olap4jDataSource(new StubDataSourceCollection<SPDataSource>());
        	ds.setName("Testing OLAP data source");
            return ds;
        } else if (valueType.equals(SQLGroupFunction.class)) {
        	if (oldVal.equals(SQLGroupFunction.COUNT)) {
        		newVal = SQLGroupFunction.GROUP_BY;
        	} else {
        		newVal = SQLGroupFunction.COUNT;
        	}
        } else if (valueType.isAssignableFrom(Deferrability.class)) {
        	if (oldVal.equals(Deferrability.INITIALLY_DEFERRED)) {
        		newVal = Deferrability.INITIALLY_IMMEDIATE;
        	} else {
        		newVal = Deferrability.INITIALLY_DEFERRED;
        	}
        } else if (valueType.isAssignableFrom(UpdateDeleteRule.class)) {
        	if (oldVal.equals(UpdateDeleteRule.CASCADE)) {
        		newVal = UpdateDeleteRule.NO_ACTION;
        	} else {
        		newVal = UpdateDeleteRule.CASCADE;
        	}
        } else if (valueType.isAssignableFrom(SQLImportedKey.class)) {
        	SQLImportedKey key = new SQLImportedKey();
        	SQLTable table = (SQLTable) makeNewValue(SQLTable.class, null, "parent of imported key");
        	table.addImportedKey(key);
        	newVal = key;
        } else if (valueType.isAssignableFrom(AscendDescend.class)) {
        	if (oldVal.equals(AscendDescend.ASCENDING)) {
        		newVal = AscendDescend.DESCENDING;
        	} else {
        		newVal = AscendDescend.ASCENDING;
        	}
        } else {
            throw new RuntimeException(
                    "This new value maker doesn't handle type " + valueType.getName() +
                    " (for property " + propName + ")");
        }

        return newVal;
    }

    
}
