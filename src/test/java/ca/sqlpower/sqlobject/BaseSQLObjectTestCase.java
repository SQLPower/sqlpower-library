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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.PropertyUtils;

import ca.sqlpower.object.ObjectDependentException;
import ca.sqlpower.object.PersistedSPObjectTest;
import ca.sqlpower.object.SPObject;
import ca.sqlpower.object.undo.SPObjectUndoManager;
import ca.sqlpower.sql.JDBCDataSource;
import ca.sqlpower.sql.SPDataSource;
import ca.sqlpower.sqlobject.SQLTypePhysicalProperties.SQLTypeConstraint;
import ca.sqlpower.sqlobject.SQLTypePhysicalPropertiesProvider.BasicSQLType;
import ca.sqlpower.sqlobject.SQLTypePhysicalPropertiesProvider.PropertyType;
import ca.sqlpower.testutil.GenericNewValueMaker;
import ca.sqlpower.testutil.MockJDBCDriver;
import ca.sqlpower.testutil.NewValueMaker;
import ca.sqlpower.util.SQLPowerUtils;

/**
 * Extends the basic database-connected test class with some test methods that
 * should be applied to every SQLObject implementation. If you are making a test
 * for a new SQLObject implementation, this is the test class you should extend!
 */
public abstract class BaseSQLObjectTestCase extends PersistedSPObjectTest {

    Set<String>propertiesToIgnoreForUndo = new HashSet<String>();
    Set<String>propertiesToIgnoreForEventGeneration = new HashSet<String>();

	public BaseSQLObjectTestCase(String name) throws Exception {
		super(name);
	}
	
	protected abstract SQLObject getSQLObjectUnderTest() throws SQLObjectException;
	
	@Override
	public SPObject getSPObjectUnderTest() {
		try {
			return getSQLObjectUnderTest();
		} catch (SQLObjectException e) {
			throw new RuntimeException("This should not happen!", e);
		}
	}
	
	public Set<String> getPropertiesToIgnoreForEvents() {
		Set<String> ignored = new HashSet<String>();
		ignored.add("referenceCount");
		ignored.add("populated");
		ignored.add("SQLObjectListeners");
		ignored.add("children");
		ignored.add("parent");
		ignored.add("parentDatabase");
		ignored.add("class");
		ignored.add("childCount");
		ignored.add("undoEventListeners");
		ignored.add("connection");
		ignored.add("typeMap");
		ignored.add("secondaryChangeMode");	
		ignored.add("zoomInAction");
		ignored.add("zoomOutAction");
        ignored.add("magicEnabled");
        ignored.add("tableContainer");
        ignored.add("session");
        ignored.add("workspaceContainer");
        ignored.add("runnableDispatcher");
        ignored.add("foregroundThread");
		return ignored;
	}
	
	/**
	 * Returns a class that is one of the child types of the object under test. An
	 * object of this type must be able to be added as a child to the object without
	 * error. If the object under test does not allow children or all of the children
	 * of the object are final so none can be added, null will be returned.
	 */
	protected abstract Class<? extends SPObject> getChildClassType();
	
	/**
	 * XXX This test should use the {@link GenericNewValueMaker} as it has it's own mini
	 * version inside it. This test should also be using the annotations to decide which 
	 * setters can fire events.
	 * 
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 * @throws NoSuchMethodException
	 * @throws SQLObjectException
	 */
	public void testAllSettersGenerateEvents()
	throws IllegalArgumentException, IllegalAccessException, 
	InvocationTargetException, NoSuchMethodException, SQLObjectException {
		SQLObject so = getSQLObjectUnderTest();
		so.populate();
		
        propertiesToIgnoreForEventGeneration.addAll(getPropertiesToIgnoreForEvents());
        
        //Ignored because we expect the object to be populated.
        propertiesToIgnoreForEventGeneration.add("exportedKeysPopulated");
        propertiesToIgnoreForEventGeneration.add("importedKeysPopulated");
        propertiesToIgnoreForEventGeneration.add("columnsPopulated");
        propertiesToIgnoreForEventGeneration.add("indicesPopulated");
		
        CountingSQLObjectListener listener = new CountingSQLObjectListener();
        SQLPowerUtils.listenToHierarchy(so, listener);
        
        if (so instanceof SQLDatabase) {
			// should be handled in the Datasource
			propertiesToIgnoreForEventGeneration.add("name");
		}
		
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
				if (oldVal != null) {
					newVal = ((Integer)oldVal)+1;
				} else {
					newVal = 1;
				}
			} else if (property.getPropertyType() == String.class) {
				// make sure it's unique
				newVal ="new " + oldVal;
				
			} else if (property.getPropertyType() == Boolean.TYPE || property.getPropertyType() == Boolean.class) {
				if (oldVal == null) {
					newVal = Boolean.TRUE;
				} else {
					newVal = new Boolean(! ((Boolean) oldVal).booleanValue());
				}
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
            	SQLRelationship rel = new SQLRelationship();
            	newVal = rel.getForeignKey();
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
            } else if (property.getPropertyType() == SQLIndex.AscendDescend.class) {
            	if (oldVal == SQLIndex.AscendDescend.ASCENDING) {
            		newVal = SQLIndex.AscendDescend.DESCENDING;
            	} else {
            		newVal = SQLIndex.AscendDescend.ASCENDING;
            	}
            } else if (property.getPropertyType() == Throwable.class) {
                newVal = new Throwable();
            } else if (property.getPropertyType() == BasicSQLType.class) {
            	if (oldVal != BasicSQLType.OTHER) {
            		newVal = BasicSQLType.OTHER;
            	} else {
            		newVal = BasicSQLType.TEXT;
            	}
            } else if (property.getPropertyType() == UserDefinedSQLType.class) {
            	newVal = new UserDefinedSQLType();
            } else if (property.getPropertyType() == SQLTypeConstraint.class) {
            	if (oldVal != SQLTypeConstraint.NONE) {
            		newVal = SQLTypeConstraint.NONE;
            	} else {
            		newVal = SQLTypeConstraint.CHECK;
            	}
            } else if (property.getPropertyType() == String[].class) {
            	newVal = new String[3];
            } else if (property.getPropertyType() == PropertyType.class) {
            	if (oldVal != PropertyType.NOT_APPLICABLE) {
            		newVal = PropertyType.NOT_APPLICABLE;
            	} else {
            		newVal = PropertyType.VARIABLE;
            	}
            } else if (property.getPropertyType() == List.class) {
            	newVal = Arrays.asList("one", "two");
            } else {
				throw new RuntimeException("This test case lacks a value for "+
						property.getName()+
						" (type "+property.getPropertyType().getName()+") from "+so.getClass()+" on property "+property.getDisplayName());
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
		propertiesToIgnoreForUndo.add("exportedKeysPopulated");
		propertiesToIgnoreForUndo.add("importedKeysPopulated");
		propertiesToIgnoreForUndo.add("columnsPopulated");
		propertiesToIgnoreForUndo.add("indicesPopulated");
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
        propertiesToIgnoreForUndo.add("workspaceContainer");
        propertiesToIgnoreForUndo.add("runnableDispatcher");
        propertiesToIgnoreForUndo.add("foregroundThread");

		if(so instanceof SQLDatabase)
		{
			// should be handled in the Datasource
			propertiesToIgnoreForUndo.add("name");
		}
		
		SPObjectUndoManager undoManager= new SPObjectUndoManager(so);
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
				if (oldVal != null) {
					newVal = ((Integer) oldVal) + 1;
				} else {
					newVal = 1;
				}
			} else if (property.getPropertyType() == String.class) {
				// make sure it's unique
				newVal ="new " + oldVal;
				
			} else if (property.getPropertyType() == Boolean.TYPE || property.getPropertyType() == Boolean.class) {
				if (oldVal == null) {
					newVal = Boolean.TRUE;
				} else {
					newVal = new Boolean(! ((Boolean) oldVal).booleanValue());
				}
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
            	SQLRelationship rel = new SQLRelationship();
            	newVal = rel.getForeignKey();
            } else if ( property.getPropertyType() == SQLRelationship.Deferrability.class){
                if (oldVal == SQLRelationship.Deferrability.INITIALLY_DEFERRED) {
                    newVal = SQLRelationship.Deferrability.NOT_DEFERRABLE;
                } else {
                    newVal = SQLRelationship.Deferrability.INITIALLY_DEFERRED;
                }
            } else if (property.getPropertyType() == SQLIndex.AscendDescend.class) {
            	if (oldVal == SQLIndex.AscendDescend.ASCENDING) {
            		newVal = SQLIndex.AscendDescend.DESCENDING;
            	} else {
            		newVal = SQLIndex.AscendDescend.ASCENDING;
            	}
            } else if (property.getPropertyType() == Throwable.class) {
                newVal = new Throwable();
            } else if (property.getPropertyType() == BasicSQLType.class) {
            	if (oldVal != BasicSQLType.OTHER) {
            		newVal = BasicSQLType.OTHER;
            	} else {
            		newVal = BasicSQLType.TEXT;
            	}
            } else if (property.getPropertyType() == UserDefinedSQLType.class) {
            	newVal = new UserDefinedSQLType();
            } else if (property.getPropertyType() == SQLTypeConstraint.class) {
            	if (oldVal != SQLTypeConstraint.NONE) {
            		newVal = SQLTypeConstraint.NONE;
            	} else {
            		newVal = SQLTypeConstraint.CHECK;
            	}
            } else if (property.getPropertyType() == SQLCheckConstraint.class) {
            	newVal = new SQLCheckConstraint("check constraint name", "check constraint condition");
            } else if (property.getPropertyType() == SQLEnumeration.class) {
            	newVal = new SQLEnumeration("some enumeration");
            } else if (property.getPropertyType() == String[].class) {
            	newVal = new String[3];
            } else if (property.getPropertyType() == PropertyType.class) {
            	if (oldVal != PropertyType.NOT_APPLICABLE) {
            		newVal = PropertyType.NOT_APPLICABLE;
            	} else {
            		newVal = PropertyType.VARIABLE;
            	}
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
    	
    	//isPopulated always returns true, skip this test.
    	if (o.isPopulated()) return;
    	
    	Class<?> childClassType = getChildClassType();
    	if (childClassType == null) return;
    	
    	NewValueMaker valueMaker = new GenericNewValueMaker(getRootObject());
    	SQLObject newChild = (SQLObject) valueMaker.makeNewValue(childClassType, null, "child");
    	
    	o.addChild(newChild);
    	
    	assertFalse(o.isPopulated());
    }
    
	/*
	 * Test method for 'ca.sqlpower.sqlobject.SQLObject.setPopulated(boolean)'
	 */
	public void testSetPopulated() throws Exception {
		getSQLObjectUnderTest().setPopulated(true);
		assertTrue(getSQLObjectUnderTest().isPopulated());
	}
	
	/*
	 * Test method for 'ca.sqlpower.sqlobject.SQLObject.setChildren(List)'
	 * Note that setChildren copies elements, does not assign the list, and
	 * getChildren returns an unmodifiable copy of the current list.
	 */
	public void testAllChildHandlingMethods() throws SQLObjectException, IllegalArgumentException, ObjectDependentException {
		if (!getSQLObjectUnderTest().allowsChildren()) return;

		getSQLObjectUnderTest().populate();
		
		NewValueMaker newValueMaker = new GenericNewValueMaker(getRootObject());
		Class<? extends SPObject> childType = getSQLObjectUnderTest().getAllowedChildTypes().get(0);
		
		int childCount = getSQLObjectUnderTest().getChildCount();
		List<SPObject> children = new ArrayList<SPObject>();
		children.addAll(getSQLObjectUnderTest().getChildren(childType));

		SQLObject x = (SQLObject) newValueMaker.makeNewValue(childType, null, "");
		
		getSQLObjectUnderTest().addChild(x);
		assertEquals(childCount + 1, getSQLObjectUnderTest().getChildCount());
		assertEquals(x, getSQLObjectUnderTest().getChildren(childType).get(
				getSQLObjectUnderTest().getChildren(childType).size() - 1));
		
		SQLObject y = (SQLObject) newValueMaker.makeNewValue(childType, null, "");
		
		// Test addChild(SQLObject, int)
		getSQLObjectUnderTest().addChild(y, 0);
		assertEquals(y, getSQLObjectUnderTest().getChildren(y.getClass()).get(0));
		assertEquals(x, getSQLObjectUnderTest().getChildren(childType).get(
				getSQLObjectUnderTest().getChildren(childType).size() - 1));
		
		getSQLObjectUnderTest().removeChild(x);
		children.add(0, y);
		assertTrue(getSQLObjectUnderTest().getChildren(childType).containsAll(children));
		
		getSQLObjectUnderTest().removeChild(y);
		assertEquals(childCount, getSQLObjectUnderTest().getChildCount());
	}

	public void testFiresAddEvent() throws SQLObjectException {
		if (!getSQLObjectUnderTest().allowsChildren()) return;
		
		CountingSQLObjectListener l = new CountingSQLObjectListener();
		getSQLObjectUnderTest().addSPListener(l);
		
		Class<? extends SPObject> childType = getSQLObjectUnderTest().getAllowedChildTypes().get(0);
		NewValueMaker newValueMaker = new GenericNewValueMaker(getRootObject());
		SQLObject x = (SQLObject) newValueMaker.makeNewValue(childType, null, "");
		
		getSQLObjectUnderTest().addChild(x);
		assertEquals(1, l.getInsertedCount());
        assertEquals(0, l.getRemovedCount());
        assertEquals(0, l.getChangedCount());
        assertEquals(0, l.getStructureChangedCount());
    }

    public void testFireChangeEvent() throws Exception {
        CountingSQLObjectListener l = new CountingSQLObjectListener();
        
        class NewStubSQLObject extends StubSQLObject {
    		public void fakeObjectChanged(String string,Object oldValue, Object newValue) {
    			firePropertyChange(string,oldValue,newValue);
    		}
		};
		NewStubSQLObject stub = new NewStubSQLObject();

		stub.addSPListener(l);
		
        stub.fakeObjectChanged("fred","old value","new value");
        assertEquals(0, l.getInsertedCount());
        assertEquals(0, l.getRemovedCount());
        assertEquals(1, l.getChangedCount());
        assertEquals(0, l.getStructureChangedCount());
    }
    
    /** make sure "change" to same value doesn't fire useless event */
    public void testDontFireChangeEvent() throws Exception {
        CountingSQLObjectListener l = new CountingSQLObjectListener();
        
        class NewStubSQLObject extends StubSQLObject {
    		public void fakeObjectChanged(String string,Object oldValue, Object newValue) {
    			firePropertyChange(string,oldValue,newValue);
    		}
		};
		NewStubSQLObject stub = new NewStubSQLObject();
		
        stub.addSPListener(l);

        stub.fakeObjectChanged("fred","old value","old value");
        assertEquals(0, l.getInsertedCount());
        assertEquals(0, l.getRemovedCount());
        assertEquals(0, l.getChangedCount());
        assertEquals(0, l.getStructureChangedCount());
    }

    public void testFireStructureChangeEvent() throws Exception {
        CountingSQLObjectListener l = new CountingSQLObjectListener();
        getSQLObjectUnderTest().addSPListener(l);
        assertEquals(0, l.getInsertedCount());
        assertEquals(0, l.getRemovedCount());
        assertEquals(0, l.getChangedCount());
    }
    
    public void testAddRemoveListener() throws Exception {
        CountingSQLObjectListener l = new CountingSQLObjectListener();
        
        int listenerSize = getSQLObjectUnderTest().getSPListeners().size();
        getSQLObjectUnderTest().addSPListener(l);
        assertEquals(listenerSize + 1, getSQLObjectUnderTest().getSPListeners().size());
		
        getSQLObjectUnderTest().removeSPListener(l);
		assertEquals(listenerSize, getSQLObjectUnderTest().getSPListeners().size());
	}
	
	public void testAllowMixedChildrenThatAreSubclassesOfEachOther() throws Exception {
		SQLObject stub = new StubSQLObject();
		
		SQLObject subImpl = new StubSQLObject() {};
		stub.addChild(new StubSQLObject());
		stub.addChild(subImpl);
		
		// now test the other direction
		stub.removeChild(stub.getChild(0));
		stub.addChild(new StubSQLObject());
        
        // test passes if no exceptions were thrown
	}
	
    public void testPreRemoveEventNoVeto() throws Exception {
    	if (!getSQLObjectUnderTest().allowsChildren()) return;
    	
    	getSQLObjectUnderTest().populate();
    	
		Class<? extends SPObject> childType = getSQLObjectUnderTest().getAllowedChildTypes().get(0);
		NewValueMaker newValueMaker = new GenericNewValueMaker(getRootObject());
		SQLObject x = (SQLObject) newValueMaker.makeNewValue(childType, null, "");
		
		int childCount = getSQLObjectUnderTest().getChildCount();
        getSQLObjectUnderTest().addChild(x);

        CountingSQLObjectPreEventListener l = new CountingSQLObjectPreEventListener();
        getSQLObjectUnderTest().addSQLObjectPreEventListener(l);
        
        l.setVetoing(false);
        
        getSQLObjectUnderTest().removeChild(getSQLObjectUnderTest().getChild(0));
        
        assertEquals("Event fired", 1, l.getPreRemoveCount());
        assertEquals("Child removed", childCount, getSQLObjectUnderTest().getChildren().size());
    }
    
    public void testPreRemoveEventVeto() throws Exception {
    	if (!getSQLObjectUnderTest().allowsChildren()) return;
    	
    	getSQLObjectUnderTest().populate();
    	
		Class<? extends SPObject> childType = getSQLObjectUnderTest().getAllowedChildTypes().get(0);
		NewValueMaker newValueMaker = new GenericNewValueMaker(getRootObject());
		SQLObject x = (SQLObject) newValueMaker.makeNewValue(childType, null, "");
		
		int childCount = getSQLObjectUnderTest().getChildCount();
        getSQLObjectUnderTest().addChild(x);

        CountingSQLObjectPreEventListener l = new CountingSQLObjectPreEventListener();
        getSQLObjectUnderTest().addSQLObjectPreEventListener(l);
        
        l.setVetoing(true);
        
        getSQLObjectUnderTest().removeChild(getSQLObjectUnderTest().getChild(0));
        
        assertEquals("Event fired", 1, l.getPreRemoveCount());
        assertEquals("Child not removed", childCount + 1, getSQLObjectUnderTest().getChildren().size());
    }
    
    public void testClientPropertySetAndGet() throws Exception {
        getSQLObjectUnderTest().putClientProperty(this.getClass(), "testProperty", "test me");
        assertEquals("test me", getSQLObjectUnderTest().getClientProperty(this.getClass(), "testProperty"));
    }
    
    public void testClientPropertyFiresEvent() throws Exception {
        CountingSQLObjectListener listener = new CountingSQLObjectListener();
        getSQLObjectUnderTest().addSPListener(listener);
        getSQLObjectUnderTest().putClientProperty(this.getClass(), "testProperty", "test me");
        assertEquals(1, listener.getChangedCount());
    }
    
    public void testChildrenInaccessibleReasonSetOnPopulateError() throws Exception {
        final RuntimeException e = new RuntimeException("freaky!");
        SQLObject o = new StubSQLObject() {
            @Override
            protected void populateImpl() throws SQLObjectException {
                throw e;
            }
        };
        
        try {
        	o.populate();
        	fail("Failing on populate should throw an exception as well as store it in the children inaccessible reason.");
        } catch (RuntimeException ex) {
        	assertEquals(e, ex);
        }
        
        assertEquals(e, o.getChildrenInaccessibleReason(SQLObject.class));
            
    }

}