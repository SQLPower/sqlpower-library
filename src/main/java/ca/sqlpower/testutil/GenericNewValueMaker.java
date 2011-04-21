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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import ca.sqlpower.object.SPObject;
import ca.sqlpower.query.SQLGroupFunction;
import ca.sqlpower.sql.DataSourceCollection;
import ca.sqlpower.sql.JDBCDataSource;
import ca.sqlpower.sql.Olap4jDataSource;
import ca.sqlpower.sql.PlDotIni;
import ca.sqlpower.sql.SPDataSource;
import ca.sqlpower.sqlobject.SQLCatalog;
import ca.sqlpower.sqlobject.SQLCheckConstraint;
import ca.sqlpower.sqlobject.SQLCheckConstraintContainer;
import ca.sqlpower.sqlobject.SQLColumn;
import ca.sqlpower.sqlobject.SQLDatabase;
import ca.sqlpower.sqlobject.SQLEnumeration;
import ca.sqlpower.sqlobject.SQLIndex;
import ca.sqlpower.sqlobject.SQLIndex.AscendDescend;
import ca.sqlpower.sqlobject.SQLIndex.Column;
import ca.sqlpower.sqlobject.SQLObject;
import ca.sqlpower.sqlobject.SQLObjectException;
import ca.sqlpower.sqlobject.SQLObjectRoot;
import ca.sqlpower.sqlobject.SQLObjectRuntimeException;
import ca.sqlpower.sqlobject.SQLRelationship;
import ca.sqlpower.sqlobject.SQLRelationship.ColumnMapping;
import ca.sqlpower.sqlobject.SQLRelationship.Deferrability;
import ca.sqlpower.sqlobject.SQLRelationship.SQLImportedKey;
import ca.sqlpower.sqlobject.SQLRelationship.UpdateDeleteRule;
import ca.sqlpower.sqlobject.SQLSchema;
import ca.sqlpower.sqlobject.SQLTable;
import ca.sqlpower.sqlobject.SQLTypePhysicalProperties;
import ca.sqlpower.sqlobject.SQLTypePhysicalProperties.SQLTypeConstraint;
import ca.sqlpower.sqlobject.SQLTypePhysicalPropertiesProvider.BasicSQLType;
import ca.sqlpower.sqlobject.SQLTypePhysicalPropertiesProvider.PropertyType;
import ca.sqlpower.sqlobject.UserDefinedSQLType;
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
	protected final SPObject root;
	protected final DataSourceCollection<SPDataSource> pl;

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
        } else if (valueType == Long.TYPE) {
        	newVal = ((Long) oldVal) + 1;
        } else if (valueType == Long.class) {
        	if (oldVal == null) {
        		newVal = 1L;
        	} else {
        		newVal = ((Long) oldVal) + 1;
        	}
        } else if (valueType == Short.class) {
        	if (oldVal == null) {
        		newVal = (short) 1;
        	} else {
        		newVal = ((Short) oldVal) + 1;
        	}
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
        } else if (valueType == BigDecimal.class) {
        	if (oldVal != null) {
        		newVal = new BigDecimal(1).add((BigDecimal) oldVal);
        	} else {
        		return new BigDecimal(1);
        	}
        } else if (valueType == Character.TYPE || valueType == Character.class) {
        	Character c = (Character) oldVal;
        	if (c == null || c == 'a') {
        		newVal = 'b';
        	} else {
        		newVal = 'a';
        	}
        } else if (valueType == String.class) {
            // make sure it's unique
            newVal = "new" + oldVal;
        } else if (valueType == Boolean.TYPE){
            newVal = new Boolean(! ((Boolean) oldVal).booleanValue());
        } else if (valueType == Boolean.class) {
            if (oldVal == null) {
                newVal = Boolean.TRUE;
            } else {
                newVal = new Boolean(! ((Boolean) oldVal).booleanValue());
            }
        } else if (valueType == Date.class) {
        	newVal = new Date(System.currentTimeMillis());
        } else if (valueType == File.class) {
            newVal = new File("temp" + System.currentTimeMillis());
        } else if (valueType == JDBCDataSource.class || valueType == SPDataSource.class) {
            String name = "regression_test";
            if (oldVal != null && ((SPDataSource) oldVal).getName().equals(name)) {
                name = "Testing data source";
                if (pl.getDataSource(name) != null) {
                    newVal = pl.getDataSource(name);
                } else {
                    newVal = new JDBCDataSource(this.pl);
                    ((SPDataSource)newVal).setName(name);
                    this.pl.addDataSource((JDBCDataSource)newVal);
                }
            } else {
                newVal = pl.getDataSource(name);
                if (newVal == null) {
                    newVal = new JDBCDataSource(this.pl);
                    ((SPDataSource)newVal).setName(name);
                    this.pl.addDataSource((JDBCDataSource)newVal);
                }
            }
        } else if (valueType == Font.class) {
            newVal = Font.decode("Dialog");
            if (newVal.equals(oldVal)) {
                newVal = ((Font) newVal).deriveFont(((Font) newVal).getSize2D() + 1f);
            }
        } else if (valueType.isAssignableFrom(NumberFormat.class)) {
        	if(oldVal == NumberFormat.getCurrencyInstance()) {
        		newVal = NumberFormat.getPercentInstance();
        	} else {
        		newVal = NumberFormat.getCurrencyInstance();
        	}
        } else if(valueType == SimpleDateFormat.class) {
        	if(oldVal == SimpleDateFormat.getDateTimeInstance()) {
        		newVal = SimpleDateFormat.getDateInstance();
        	} else {
        		newVal = SimpleDateFormat.getDateTimeInstance();
        	}        	
        } else if (SQLColumn.class.isAssignableFrom(valueType)) {
            // Objects like SPObject or SQLObject may come in here
        	SQLColumn sqlCol = new SQLColumn();
        	sqlCol.setName("testing!");
        	SQLTable table = (SQLTable) makeNewValue(SQLTable.class, null, "Parent of column");
        	try {
				table.addColumn(sqlCol);
			} catch (SQLObjectException e) {
				throw new SQLObjectRuntimeException(e);
			}
        	table.setPopulated(true);
        	newVal = sqlCol;
        } else if (valueType == SQLTable.class) {
        	SQLTable table = new SQLTable();
        	table.setName("Generated testing table");
        	table.setPopulated(true);
        	SQLDatabase db = (SQLDatabase) makeNewValue(SQLDatabase.class, null, "parent of table");
        	db.addTable(table);
        	newVal = table;
        } else if (valueType == SQLDatabase.class) {
        	JDBCDataSource argDataSource = new JDBCDataSource(this.pl);
            argDataSource.setName("Testing data source");
        	SQLDatabase db = new SQLDatabase();
        	db.setDataSource(argDataSource);
        	SQLObjectRoot sqlRoot = (SQLObjectRoot) makeNewValue(SQLObjectRoot.class, null, "parent of db");
        	sqlRoot.addDatabase(db, 0);
        	newVal = db;
        } else if (valueType == SQLObjectRoot.class) {
        	SQLObjectRoot sqlRoot = new SQLObjectRoot();
        	root.addChild(sqlRoot, 0);
        	newVal = sqlRoot;
        } else if (valueType == Column.class) {
        	Column col = new Column();
        	col.setName("Generated testing column index");
        	SQLIndex index = (SQLIndex) makeNewValue(SQLIndex.class, null, "parent of column");
        	index.setPopulated(true);
        	index.addIndexColumn(col);
        	newVal = col;
        } else if (valueType == SQLIndex.class) {
        	SQLIndex index = new SQLIndex();
        	index.setName("a new index");
        	SQLTable table = (SQLTable) makeNewValue(SQLTable.class, null, "parent of index");
        	table.addIndex(index);
        	table.setPopulated(true);
        	newVal = index;
        } else if (valueType == ColumnMapping.class) {
        	ColumnMapping mapping = new ColumnMapping();
        	mapping.setName("Generated testing mapping");
        	SQLRelationship rel = (SQLRelationship) makeNewValue(SQLRelationship.class, null, "parent of column mapping");
        	rel.addMapping(mapping);
        	SQLColumn pkCol = (SQLColumn) makeNewValue(SQLColumn.class, null, "pk column");
        	SQLColumn fkCol = (SQLColumn) makeNewValue(SQLColumn.class, null, "pk column");
        	mapping.setPkColumn(pkCol);
        	mapping.setFkColumn(fkCol);
        	newVal = mapping;
        } else if (valueType == SQLRelationship.class) {
        	SQLRelationship rel = new SQLRelationship();
        	SQLTable parent = (SQLTable) makeNewValue(SQLTable.class, null, "parent of relationship");
        	SQLTable child = (SQLTable) makeNewValue(SQLTable.class, null, "child of relationship");
        	try {
				rel.attachRelationship(parent, child, true);
			} catch (SQLObjectException e) {
				throw new RuntimeException("Trying to create a new relationship for testing", e);
			}
        	newVal = rel;
        } else if (valueType == SQLSchema.class) {
        	SQLSchema schema = new SQLSchema(true);
        	schema.setName("A new schema for testing");
        	SQLDatabase db = (SQLDatabase) makeNewValue(SQLDatabase.class, null, "Schema database");
        	db.addSchema(schema);
        	newVal = schema;
        } else if (valueType == SQLCatalog.class) {
        	SQLDatabase db = (SQLDatabase) makeNewValue(SQLDatabase.class, null, "Schema database");
        	SQLCatalog catalog = new SQLCatalog(db, "catalog for test", true);
        	db.addCatalog(catalog);
        	newVal = catalog;
        } else if (valueType == SPObject.class) {
            newVal = makeNewValue(SQLDatabase.class, null, "SPObject of some kind");
        } else if (valueType == SQLObject.class) {
        	newVal = makeNewValue(SQLColumn.class, null, "SQLObject of some kind");
        } else if (valueType == SQLCheckConstraint.class) {
        	SQLTypePhysicalProperties properties =
        		(SQLTypePhysicalProperties) makeNewValue(SQLTypePhysicalProperties.class, null, "SQLTypePhysicalProperties as parent of SQLCheckConstraint");
        	SQLCheckConstraint constraint = new SQLCheckConstraint(
        			(String) makeNewValue(String.class, null, "SQLCheckConstraint - name"),
        			(String) makeNewValue(String.class, null, "SQLCheckConstraint - constraint"));
        	properties.addCheckConstraint(constraint);
        	newVal = constraint;
        } else if (valueType == SQLEnumeration.class) {
        	SQLTypePhysicalProperties properties =
        		(SQLTypePhysicalProperties) makeNewValue(SQLTypePhysicalProperties.class, null, "SQLTypePhysicalProperties as parent of SQLEnumeration");
        	SQLEnumeration enumeration = new SQLEnumeration((String) makeNewValue(String.class, null, "SQLEnumeration - name"));
        	properties.addEnumeration(enumeration);
        	newVal = enumeration;
        } else if (valueType == SQLCheckConstraintContainer.class) {
        	newVal = makeNewValue(SQLTypePhysicalProperties.class, oldVal, propName);
        } else if (valueType == Throwable.class) {
        	newVal = new SQLObjectException("Test Exception");
        } else if (valueType == UserPrompter.class) {
            newVal = new DefaultUserPrompter(UserPromptOptions.OK_NEW_NOTOK_CANCEL, UserPromptResponse.CANCEL, null);            
        } else if (valueType == Point.class) {
            Point p = new Point(24, 42);
            if (p.equals(oldVal)) {
                p.translate(1, 1);
            }
            newVal = p;
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
			newVal = point;
        } else if (valueType.equals(Image.class)) {
        	newVal = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
        } else if (valueType.equals(Olap4jDataSource.class)) {
        	Olap4jDataSource ds = new Olap4jDataSource(new StubDataSourceCollection<SPDataSource>());
        	ds.setName("Testing OLAP data source");
        	newVal = ds;
        } else if (valueType.equals(SQLGroupFunction.class)) {
        	if (oldVal.equals(SQLGroupFunction.COUNT)) {
        		newVal = SQLGroupFunction.GROUP_BY;
        	} else {
        		newVal = SQLGroupFunction.COUNT;
        	}
        } else if (valueType == Deferrability.class) {
        	if (oldVal.equals(Deferrability.INITIALLY_DEFERRED)) {
        		newVal = Deferrability.INITIALLY_IMMEDIATE;
        	} else {
        		newVal = Deferrability.INITIALLY_DEFERRED;
        	}
        } else if (valueType == UpdateDeleteRule.class) {
        	if (oldVal.equals(UpdateDeleteRule.CASCADE)) {
        		newVal = UpdateDeleteRule.NO_ACTION;
        	} else {
        		newVal = UpdateDeleteRule.CASCADE;
        	}
        } else if (valueType == SQLImportedKey.class) {
        	SQLRelationship relationship = (SQLRelationship) makeNewValue(SQLRelationship.class, null, "parent of importedKey");
        	SQLImportedKey key = relationship.getForeignKey();
        	SQLTable table = (SQLTable) makeNewValue(SQLTable.class, null, "parent of imported key");
        	table.addImportedKey(key);
        	newVal = key;
        } else if (valueType == AscendDescend.class) {
        	if (oldVal.equals(AscendDescend.ASCENDING)) {
        		newVal = AscendDescend.DESCENDING;
        	} else {
        		newVal = AscendDescend.ASCENDING;
        	}
        } else if (valueType == Rectangle.class) {
            Rectangle r = new Rectangle(12, 34, 56, 78);
            if (r.equals(oldVal)) {
                r.translate(1, 1);
            }
            newVal = r;
        } else if (valueType == Color.class) {
            Color rgb = new Color(33, 66, 99);
            if (rgb.getRGB() == ((Color) oldVal).getRGB()) {
                rgb = rgb.brighter();
            }
            newVal = rgb;
        } else if (valueType == Dimension.class) {
            Dimension d = new Dimension(12, 34);
            if (d.equals(oldVal)) {
                d.width++;
            }
            newVal = d;
        } else if (valueType == BasicSQLType.class) {
        	if (oldVal != BasicSQLType.OTHER) {
        		newVal = BasicSQLType.OTHER;
        	} else {
        		newVal = BasicSQLType.TEXT;
        	}
        } else if (valueType == UserDefinedSQLType.class) {
        	UserDefinedSQLType userDefinedSQLType = new UserDefinedSQLType();
        	UserDefinedSQLType oldType = (UserDefinedSQLType) oldVal;
        	if (oldType != null && oldType.getType() != null) {
        	    userDefinedSQLType.setType(oldType.getType() + 1);
        	} else {
        	    userDefinedSQLType.setType(1);
        	}
			newVal = userDefinedSQLType;
        	root.addChild((SPObject) newVal, root.getChildren(UserDefinedSQLType.class).size());
        } else if (valueType == SQLTypePhysicalProperties.class) {
        	// XXX Uses a random string so that each platform will be different. The interaction
        	// of identical platforms is tested for specifically.
        	SQLTypePhysicalProperties properties = new SQLTypePhysicalProperties(UUID.randomUUID().toString());
        	UserDefinedSQLType udt = 
        		(UserDefinedSQLType) makeNewValue(UserDefinedSQLType.class, null, "UserDefinedSQLType as parent of SQLTypePhysicalProperties");
        	try {
				udt.addChild(properties);
			} catch (SQLObjectException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			newVal = properties;
        } else if (valueType == SQLTypeConstraint.class) {
        	if (oldVal != SQLTypeConstraint.NONE) {
        		newVal = SQLTypeConstraint.NONE;
        	} else {
        		newVal = SQLTypeConstraint.CHECK;
        	}
        } else if (valueType == String[].class) {
        	String[] array = {"test1", "test2", "test3"};
        	newVal = array;
        } else if (valueType == PropertyType.class) {
        	if (oldVal != PropertyType.NOT_APPLICABLE) {
        		newVal = PropertyType.NOT_APPLICABLE;
        	} else {
        		newVal = PropertyType.VARIABLE;
        	}
        } else if (Exception.class.isAssignableFrom(valueType)) {
        	newVal = new Exception("Testing Exception");
        } else if (valueType == List.class) {
        	newVal = Arrays.asList("one","two","three");
        } else {
            throw new RuntimeException(
                    "This new value maker doesn't handle type " + valueType.getName() +
                    " (for property " + propName + ")");
        }

        return newVal;
    }

    
}
