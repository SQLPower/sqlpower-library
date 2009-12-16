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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.PropertyUtils;

import ca.sqlpower.sql.JDBCDataSource;
import ca.sqlpower.sql.SPDataSource;
import ca.sqlpower.sqlobject.SQLRelationship.SQLImportedKey;
import ca.sqlpower.sqlobject.undo.SQLObjectUndoManager;
import ca.sqlpower.testutil.GenericNewValueMaker;
import ca.sqlpower.testutil.MockJDBCDriver;
import ca.sqlpower.testutil.NewValueMaker;

/**
 * Extends the basic database-connected test class with some test methods that
 * should be applied to every SQLObject implementation. If you are making a test
 * for a new SQLObject implementation, this is the test class you should extend!
 */
public abstract class BaseSQLObjectTestCase extends DatabaseConnectedTestCase {

    Set<String>propertiesToIgnoreForUndo = new HashSet<String>();
    Set<String>propertiesToIgnoreForEventGeneration = new HashSet<String>();

	public BaseSQLObjectTestCase(String name) throws Exception {
		super(name);
	}
	
	protected abstract SQLObject getSQLObjectUnderTest() throws SQLObjectException;
	
	/**
	 * Returns a class that is one of the child types of the object under test. An
	 * object of this type must be able to be added as a child to the object without
	 * error. If the object under test does not allow children or all of the children
	 * of the object are final so none can be added, null will be returned.
	 */
	protected abstract Class<?> getChildClassType();
	
	public void testAllSettersGenerateEvents()
	throws IllegalArgumentException, IllegalAccessException, 
	InvocationTargetException, NoSuchMethodException, SQLObjectException {
		
		SQLObject so = getSQLObjectUnderTest();
		so.populate();
		
        propertiesToIgnoreForEventGeneration.add("referenceCount");
		propertiesToIgnoreForEventGeneration.add("populated");
		propertiesToIgnoreForEventGeneration.add("SQLObjectListeners");
		propertiesToIgnoreForEventGeneration.add("children");
		propertiesToIgnoreForEventGeneration.add("parent");
		propertiesToIgnoreForEventGeneration.add("parentDatabase");
		propertiesToIgnoreForEventGeneration.add("class");
		propertiesToIgnoreForEventGeneration.add("childCount");
		propertiesToIgnoreForEventGeneration.add("undoEventListeners");
		propertiesToIgnoreForEventGeneration.add("connection");
		propertiesToIgnoreForEventGeneration.add("typeMap");
		propertiesToIgnoreForEventGeneration.add("secondaryChangeMode");	
		propertiesToIgnoreForEventGeneration.add("zoomInAction");
		propertiesToIgnoreForEventGeneration.add("zoomOutAction");
        propertiesToIgnoreForEventGeneration.add("magicEnabled");
        propertiesToIgnoreForEventGeneration.add("tableContainer");
        propertiesToIgnoreForEventGeneration.add("session");
        propertiesToIgnoreForEventGeneration.add("foregroundThread");
		
		if (so instanceof SQLDatabase) {
			// should be handled in the Datasource
			propertiesToIgnoreForEventGeneration.add("name");
		}
		
		CountingSQLObjectListener listener = new CountingSQLObjectListener();
		so.addSPListener(listener);

		List<PropertyDescriptor> settableProperties;
		
		settableProperties = Arrays.asList(PropertyUtils.getPropertyDescriptors(so.getClass()));
		
		for (PropertyDescriptor property : settableProperties) {
			Object oldVal;
			if (propertiesToIgnoreForEventGeneration.contains(property.getName())) continue;
			
			try {
				oldVal = PropertyUtils.getSimpleProperty(so, property.getName());
				// check for a setter
				if (property.getWriteMethod() == null)
				{
					continue;
				}
				
			} catch (NoSuchMethodException e) {
				System.out.println("Skipping non-settable property "+property.getName()+" on "+so.getClass().getName());
				continue;
			}
			Object newVal;  // don't init here so compiler can warn if the following code doesn't always give it a value
			if (property.getPropertyType() == Integer.TYPE ||property.getPropertyType() == Integer.class ) {
				newVal = ((Integer)oldVal)+1;
			} else if (property.getPropertyType() == String.class) {
				// make sure it's unique
				newVal ="new " + oldVal;
				
			} else if (property.getPropertyType() == Boolean.TYPE){
				newVal = new Boolean(! ((Boolean) oldVal).booleanValue());
			} else if (property.getPropertyType() == SQLCatalog.class) {
				newVal = new SQLCatalog(new SQLDatabase(),"This is a new catalog");
			} else if (property.getPropertyType() == SPDataSource.class) {
				newVal = new JDBCDataSource(getPLIni());
				((SPDataSource)newVal).setName("test");
				((SPDataSource)newVal).setDisplayName("test");
				((JDBCDataSource)newVal).setUser("a");
				((JDBCDataSource)newVal).setPass("b");
				((JDBCDataSource)newVal).getParentType().setJdbcDriver(MockJDBCDriver.class.getName());
				((JDBCDataSource)newVal).setUrl("jdbc:mock:tables=tab1");
			} else if (property.getPropertyType() == JDBCDataSource.class) {
                newVal = new JDBCDataSource(getPLIni());
                ((SPDataSource)newVal).setName("test");
                ((SPDataSource)newVal).setDisplayName("test");
                ((JDBCDataSource)newVal).setUser("a");
                ((JDBCDataSource)newVal).setPass("b");
                ((JDBCDataSource)newVal).getParentType().setJdbcDriver(MockJDBCDriver.class.getName());
                ((JDBCDataSource)newVal).setUrl("jdbc:mock:tables=tab1");
			} else if (property.getPropertyType() == SQLTable.class) {
				newVal = new SQLTable();
            } else if ( property.getPropertyType() == SQLColumn.class){
                newVal = new SQLColumn();
            } else if ( property.getPropertyType() == SQLIndex.class){
                newVal = new SQLIndex();
            } else if (property.getPropertyType() == SQLRelationship.SQLImportedKey.class) {
            	newVal = new SQLImportedKey();
            } else if ( property.getPropertyType() == SQLRelationship.Deferrability.class){
                if (oldVal == SQLRelationship.Deferrability.INITIALLY_DEFERRED) {
                    newVal = SQLRelationship.Deferrability.NOT_DEFERRABLE;
                } else {
                    newVal = SQLRelationship.Deferrability.INITIALLY_DEFERRED;
                }
            } else if ( property.getPropertyType() == SQLRelationship.UpdateDeleteRule.class){
                if (oldVal == SQLRelationship.UpdateDeleteRule.CASCADE) {
                    newVal = SQLRelationship.UpdateDeleteRule.RESTRICT;
                } else {
                    newVal = SQLRelationship.UpdateDeleteRule.CASCADE;
                }
            } else if (property.getPropertyType() == Throwable.class) {
                newVal = new Throwable();
            } else {
				throw new RuntimeException("This test case lacks a value for "+
						property.getName()+
						" (type "+property.getPropertyType().getName()+") from "+so.getClass()+" on property"+property.getDisplayName());
			}
			
			int oldChangeCount = listener.getChangedCount();
			
			try {
                System.out.println("Setting property '"+property.getName()+"' to '"+newVal+"' ("+newVal.getClass().getName()+")");
				BeanUtils.copyProperty(so, property.getName(), newVal);
				
				// some setters fire multiple events (they change more than one property)
				assertTrue("Event for set "+property.getName()+" on "+so.getClass().getName()+" didn't fire!",
						listener.getChangedCount() > oldChangeCount);
				if (listener.getChangedCount() == oldChangeCount + 1) {
					assertEquals("Property name mismatch for "+property.getName()+ " in "+so.getClass(),
							property.getName(),
							((PropertyChangeEvent) listener.getLastEvent()).getPropertyName());
					assertEquals("New value for "+property.getName()+" was wrong",
					        newVal,
					        ((PropertyChangeEvent) listener.getLastEvent()).getNewValue());  
				}
			} catch (InvocationTargetException e) {
				System.out.println("(non-fatal) Failed to write property '"+property.getName()+" to type "+so.getClass().getName());
			}
		}
	}

	
	
	/**
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 * @throws NoSuchMethodException
	 * @throws SQLObjectException 
	 */
	public void testAllSettersAreUndoable() 
	throws IllegalArgumentException, IllegalAccessException, 
	InvocationTargetException, NoSuchMethodException, SQLObjectException {
		
		SQLObject so = getSQLObjectUnderTest();
        propertiesToIgnoreForUndo.add("referenceCount");
		propertiesToIgnoreForUndo.add("populated");
		propertiesToIgnoreForUndo.add("SQLObjectListeners");
		propertiesToIgnoreForUndo.add("children");
		propertiesToIgnoreForUndo.add("parent");
		propertiesToIgnoreForUndo.add("parentDatabase");
		propertiesToIgnoreForUndo.add("class");
		propertiesToIgnoreForUndo.add("childCount");
		propertiesToIgnoreForUndo.add("undoEventListeners");
		propertiesToIgnoreForUndo.add("connection");
		propertiesToIgnoreForUndo.add("typeMap");
		propertiesToIgnoreForUndo.add("secondaryChangeMode");		
		propertiesToIgnoreForUndo.add("zoomInAction");
		propertiesToIgnoreForUndo.add("zoomOutAction");
        propertiesToIgnoreForUndo.add("magicEnabled");
        propertiesToIgnoreForUndo.add("deleteRule");
        propertiesToIgnoreForUndo.add("updateRule");
        propertiesToIgnoreForUndo.add("tableContainer");
        propertiesToIgnoreForUndo.add("session");
        propertiesToIgnoreForUndo.add("foregroundThread");

		if(so instanceof SQLDatabase)
		{
			// should be handled in the Datasource
			propertiesToIgnoreForUndo.add("name");
		}
		SQLObjectUndoManager undoManager= new SQLObjectUndoManager(so);
		List<PropertyDescriptor> settableProperties;
		settableProperties = Arrays.asList(PropertyUtils.getPropertyDescriptors(so.getClass()));
		if(so instanceof SQLDatabase)
		{
			// should be handled in the Datasource
			settableProperties.remove("name");
		}
		for (PropertyDescriptor property : settableProperties) {
			Object oldVal;
			if (propertiesToIgnoreForUndo.contains(property.getName())) continue;
			
			try {
				oldVal = PropertyUtils.getSimpleProperty(so, property.getName());
				if (property.getWriteMethod() == null)
				{
					continue;
				}
			} catch (NoSuchMethodException e) {
				System.out.println("Skipping non-settable property "+property.getName()+" on "+so.getClass().getName());
				continue;
			}
			Object newVal;  // don't init here so compiler can warn if the following code doesn't always give it a value
			if (property.getPropertyType() == Integer.TYPE  || property.getPropertyType() == Integer.class) {
				newVal = ((Integer)oldVal)+1;
			} else if (property.getPropertyType() == String.class) {
				// make sure it's unique
				newVal ="new " + oldVal;
				
			} else if (property.getPropertyType() == Boolean.TYPE){
				newVal = new Boolean(! ((Boolean) oldVal).booleanValue());
			} else if (property.getPropertyType() == SQLCatalog.class) {
				newVal = new SQLCatalog(new SQLDatabase(),"This is a new catalog");
			} else if (property.getPropertyType() == SPDataSource.class) {
				newVal = new JDBCDataSource(getPLIni());
				((SPDataSource)newVal).setName("test");
				((SPDataSource)newVal).setDisplayName("test");
				((JDBCDataSource)newVal).setUser("a");
				((JDBCDataSource)newVal).setPass("b");
				((JDBCDataSource)newVal).getParentType().setJdbcDriver(MockJDBCDriver.class.getName());
				((JDBCDataSource)newVal).setUrl("jdbc:mock:tables=tab1,tab2");
			} else if (property.getPropertyType() == JDBCDataSource.class) {
                newVal = new JDBCDataSource(getPLIni());
                ((SPDataSource)newVal).setName("test");
                ((SPDataSource)newVal).setDisplayName("test");
                ((JDBCDataSource)newVal).setUser("a");
                ((JDBCDataSource)newVal).setPass("b");
                ((JDBCDataSource)newVal).getParentType().setJdbcDriver(MockJDBCDriver.class.getName());
                ((JDBCDataSource)newVal).setUrl("jdbc:mock:tables=tab1,tab2");
			} else if (property.getPropertyType() == SQLTable.class) {
				newVal = new SQLTable();
            } else if (property.getPropertyType() == SQLColumn.class) {
                newVal = new SQLColumn();
            } else if (property.getPropertyType() == SQLIndex.class) {
                newVal = new SQLIndex();
            } else if (property.getPropertyType() == SQLRelationship.SQLImportedKey.class) {
            	newVal = new SQLImportedKey();
            } else if ( property.getPropertyType() == SQLRelationship.Deferrability.class){
                if (oldVal == SQLRelationship.Deferrability.INITIALLY_DEFERRED) {
                    newVal = SQLRelationship.Deferrability.NOT_DEFERRABLE;
                } else {
                    newVal = SQLRelationship.Deferrability.INITIALLY_DEFERRED;
                }
            } else if (property.getPropertyType() == Throwable.class) {
                newVal = new Throwable();
            } else {
				throw new RuntimeException("This test case lacks a value for "+
						property.getName()+
						" (type "+property.getPropertyType().getName()+") from "+so.getClass());
			}
			
			int oldChangeCount = undoManager.getUndoableEditCount();
			
			try {
				BeanUtils.copyProperty(so, property.getName(), newVal);
				
				// some setters fire multiple events (they change more than one property)  but only register one as an undo
				assertEquals("Event for set "+property.getName()+" on "+so.getClass().getName() +
                        " added multiple ("+undoManager.printUndoVector()+") undos!",
						oldChangeCount+1,undoManager.getUndoableEditCount());
				
			} catch (InvocationTargetException e) {
				System.out.println("(non-fatal) Failed to write property '"+property.getName()+" to type "+so.getClass().getName());
			}
		}
	}
    
    /**
     * The child list should never be null for any SQL Object, even if
     * that object's type is childless.
     */
    public void testChildrenNotNull() throws SQLObjectException {
        assertNotNull(getSQLObjectUnderTest().getChildren());
    }
    
    /**
     * Adding a child to any SQL Object should not force the object to populate.
     */
    public void testAddChildDoesNotPopulate() throws Exception {
    	SQLObject o = getSQLObjectUnderTest();
    	
    	if (!o.allowsChildren()) return;
    	
    	o.setPopulated(false);
    	Class<?> childClassType = getChildClassType();
    	if (childClassType == null) return;
    	
    	NewValueMaker valueMaker = new GenericNewValueMaker();
    	SQLObject newChild = (SQLObject) valueMaker.makeNewValue(childClassType, null, "child");
    	
    	o.addChild(newChild);
    	
    	assertFalse(o.isPopulated());
    }

}