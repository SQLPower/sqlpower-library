/*
 * Copyright (c) 2016, SQL Power Group Inc.
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

package ca.sqlpower.object;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

import junit.framework.TestCase;
import ca.sqlpower.dao.PersistedSPOProperty;
import ca.sqlpower.dao.PersistedSPObject;
import ca.sqlpower.dao.SPPersister.DataType;
import ca.sqlpower.object.annotation.Accessor;
import ca.sqlpower.object.annotation.NonBound;
import ca.sqlpower.object.annotation.NonProperty;
import ca.sqlpower.object.annotation.Transient;

import com.google.common.collect.ImmutableList;

public class DifferTest extends TestCase {
	
	public static class DiffTestClass extends AbstractSPObject {
		
		public static final List<Class<? extends SPObject>> allowedChildTypes =
				new ImmutableList.Builder<Class<? extends SPObject>>()
					.add(DiffTestClass.class)
					.build();

		@Override
		@NonProperty
		public List<? extends SPObject> getChildren() {
			return null;
		}

		@Override
		public void removeDependency(@Nonnull SPObject dependency) {
		}

		@Override
		@NonBound
		public List<? extends SPObject> getDependencies() {
			return null;
		}

		@Override
		@Transient
		@Accessor
		public List<Class<? extends SPObject>> getAllowedChildTypes() {
			return null;
		}

		@Override
		protected boolean removeChildImpl(SPObject child) {
			return false;
		}
		
	}
	
public static class DiffTestClass2 extends AbstractSPObject {
		
		public static final List<Class<? extends SPObject>> allowedChildTypes =
				new ImmutableList.Builder<Class<? extends SPObject>>()
					.add(DiffTestClass2.class)
					.add(DiffTestClass.class)
					.build();

		@Override
		@NonProperty
		public List<? extends SPObject> getChildren() {
			return null;
		}

		@Override
		public void removeDependency(@Nonnull SPObject dependency) {
		}

		@Override
		@NonBound
		public List<? extends SPObject> getDependencies() {
			return null;
		}

		@Override
		@Transient
		@Accessor
		public List<Class<? extends SPObject>> getAllowedChildTypes() {
			return null;
		}

		@Override
		protected boolean removeChildImpl(SPObject child) {
			return false;
		}
		
	}
	

    Differ diff;
        
    List<PersistedSPObject> testRevisionOneObjects;
    List<PersistedSPObject> testRevisionTwoObjects;
    
    List<PersistedSPOProperty> testRevisionOneProperties;
    List<PersistedSPOProperty> testRevisionTwoProperties;
    
    List<PersistedSPObject> objectsToRemove;
    List<PersistedSPObject> objectsToAdd;
    
    List<PersistedSPOProperty> propertiesToRemove;
    List<PersistedSPOProperty> propertiesToAdd;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        
        testRevisionOneObjects = new ArrayList<PersistedSPObject>();
        testRevisionTwoObjects = new ArrayList<PersistedSPObject>();
        
        testRevisionOneProperties = new ArrayList<PersistedSPOProperty>();
        testRevisionTwoProperties = new ArrayList<PersistedSPOProperty>();
        
        objectsToRemove = new ArrayList<PersistedSPObject>();
        objectsToAdd = new ArrayList<PersistedSPObject>();
        
        propertiesToRemove = new ArrayList<PersistedSPOProperty>();
        propertiesToAdd = new ArrayList<PersistedSPOProperty>();
        
        diff = new Differ();
        
        testRevisionOneObjects.add(new PersistedSPObject(null, DiffTestClass.class.getName(), "1", 0));
        testRevisionOneObjects.add(new PersistedSPObject("1", DiffTestClass.class.getName(), "2", 0));
        testRevisionOneObjects.add(new PersistedSPObject("1", DiffTestClass.class.getName(), "3", 1));
        testRevisionOneObjects.add(new PersistedSPObject("1", DiffTestClass.class.getName(), "4", 2));
        testRevisionOneObjects.add(new PersistedSPObject("2", DiffTestClass.class.getName(), "5", 0));
        testRevisionOneObjects.add(new PersistedSPObject("2", DiffTestClass.class.getName(), "6", 1));
        testRevisionOneObjects.add(new PersistedSPObject("4", DiffTestClass.class.getName(), "7", 0));
                
        testRevisionOneProperties.add(new PersistedSPOProperty("1", "name", DataType.STRING, "A1", "A1", false));
        testRevisionOneProperties.add(new PersistedSPOProperty("2", "name", DataType.STRING, "B1", "B1", false));
        testRevisionOneProperties.add(new PersistedSPOProperty("3", "name", DataType.STRING, "B2", "B2", false));
        testRevisionOneProperties.add(new PersistedSPOProperty("4", "name", DataType.STRING, "B3", "B3", false));
        testRevisionOneProperties.add(new PersistedSPOProperty("5", "name", DataType.STRING, "C1", "C1", false));
        testRevisionOneProperties.add(new PersistedSPOProperty("6", "name", DataType.STRING, "C2", "C2", false));
        testRevisionOneProperties.add(new PersistedSPOProperty("7", "name", DataType.STRING, "D1", "D1", false));
        testRevisionOneProperties.add(new PersistedSPOProperty("2", "value", DataType.INTEGER, 1, 1, false));
        testRevisionOneProperties.add(new PersistedSPOProperty("7", "value", DataType.DOUBLE, 1.1, 1.1, false));
        
        testRevisionTwoObjects.add(new PersistedSPObject(null, DiffTestClass.class.getName(), "1", 0));
        testRevisionTwoObjects.add(new PersistedSPObject("1", DiffTestClass.class.getName(), "2", 0));
        testRevisionTwoObjects.add(new PersistedSPObject("1", DiffTestClass.class.getName(), "3", 1));
        testRevisionTwoObjects.add(new PersistedSPObject("1", DiffTestClass.class.getName(), "4", 2));
        testRevisionTwoObjects.add(new PersistedSPObject("2", DiffTestClass.class.getName(), "5", 0));
        testRevisionTwoObjects.add(new PersistedSPObject("2", DiffTestClass.class.getName(), "6", 1));
        testRevisionTwoObjects.add(new PersistedSPObject("4", DiffTestClass.class.getName(), "7", 0));
        
        testRevisionTwoProperties.add(new PersistedSPOProperty("1", "name", DataType.STRING, "A1", "A1", false));
        testRevisionTwoProperties.add(new PersistedSPOProperty("2", "name", DataType.STRING, "B1", "B1", false));
        testRevisionTwoProperties.add(new PersistedSPOProperty("3", "name", DataType.STRING, "B2", "B2", false));
        testRevisionTwoProperties.add(new PersistedSPOProperty("4", "name", DataType.STRING, "B3", "B3", false));
        testRevisionTwoProperties.add(new PersistedSPOProperty("5", "name", DataType.STRING, "C1", "C1", false));
        testRevisionTwoProperties.add(new PersistedSPOProperty("6", "name", DataType.STRING, "C2", "C2", false));
        testRevisionTwoProperties.add(new PersistedSPOProperty("7", "name", DataType.STRING, "D1", "D1", false));
        testRevisionTwoProperties.add(new PersistedSPOProperty("2", "value", DataType.INTEGER, 1, 1, false));
        testRevisionTwoProperties.add(new PersistedSPOProperty("7", "value", DataType.DOUBLE, 1.1, 1.1, false));              
        
    }
    
    public void testIdenticalRevisions() {
        
        diff.calcDiff(testRevisionOneObjects, testRevisionTwoObjects, testRevisionOneProperties, testRevisionTwoProperties);
        
        assertEquals(diff.getPersistedSPOsToAdd().size(), 0);
        assertEquals(diff.getPersistedSPOsToRemove().size(), 0);
        assertEquals(diff.getPropertyDiffPersists().size(), 0);
        
    }
    
    public void testObjectAdditions() {
           
        objectsToAdd.add(new PersistedSPObject("1", DiffTestClass.class.getName(), "8", 3));
        objectsToAdd.add(new PersistedSPObject("2", DiffTestClass.class.getName(), "9", 2));
        objectsToAdd.add(new PersistedSPObject("8", DiffTestClass.class.getName(), "10", 0));
        testRevisionTwoObjects.addAll(objectsToAdd);
        
        diff.calcDiff(testRevisionOneObjects, testRevisionTwoObjects, testRevisionOneProperties, testRevisionTwoProperties);

        assertEquals(3, diff.getPersistedSPOsToAdd().size());
        assertEquals(diff.getPersistedSPOsToRemove().size(), 0);
        assertEquals(diff.getPropertyDiffPersists().size(), 0);
        
        // Sort and compare the lists of object additions.
        
        Collections.sort(diff.getPersistedSPOsToAdd());
        Collections.sort(objectsToAdd);
        
        assertEquals(diff.getPersistedSPOsToAdd(), objectsToAdd);
        
    }
    
    public void testObjectRemovals() {
        
        objectsToRemove.add(new PersistedSPObject("1", DiffTestClass.class.getName(), "8", 3));
        objectsToRemove.add(new PersistedSPObject("2", DiffTestClass.class.getName(), "9", 2));
        PersistedSPObject child = new PersistedSPObject("8", DiffTestClass.class.getName(), "10", 0);
        objectsToRemove.add(child);             
        testRevisionOneObjects.addAll(objectsToRemove);
        objectsToRemove.remove(child); // this should not be persisted
        
        diff.calcDiff(testRevisionOneObjects, testRevisionTwoObjects, testRevisionOneProperties, testRevisionTwoProperties);

        assertEquals(diff.getPersistedSPOsToAdd().size(), 0);
        assertEquals(diff.getPersistedSPOsToRemove().size(), 2);
        assertEquals(diff.getPropertyDiffPersists().size(), 0); 
        
        Collections.sort(diff.getPersistedSPOsToRemove());
        Collections.sort(objectsToRemove);
        
        assertEquals(diff.getPersistedSPOsToRemove(), objectsToRemove);
        
    }
    
    public void testObjectAdditionsAndRemovals() {
        
        objectsToRemove.add(new PersistedSPObject("1", DiffTestClass.class.getName(), "8", 3));
        objectsToRemove.add(new PersistedSPObject("2", DiffTestClass.class.getName(), "9", 2));
        PersistedSPObject child = new PersistedSPObject("8", DiffTestClass.class.getName(), "10", 0);
        objectsToRemove.add(child);
        testRevisionOneObjects.addAll(objectsToRemove);
        objectsToRemove.remove(child);
        
        objectsToAdd.add(new PersistedSPObject("1", DiffTestClass.class.getName(), "11", 3));
        objectsToAdd.add(new PersistedSPObject("2", DiffTestClass.class.getName(), "12", 2));
        objectsToAdd.add(new PersistedSPObject("4", DiffTestClass.class.getName(), "13", 1));
        objectsToAdd.add(new PersistedSPObject("7", DiffTestClass.class.getName(), "14", 0));
        testRevisionTwoObjects.addAll(objectsToAdd);
        
        diff.calcDiff(testRevisionOneObjects, testRevisionTwoObjects, testRevisionOneProperties, testRevisionTwoProperties);

        assertEquals(diff.getPersistedSPOsToAdd().size(), 4);
        assertEquals(diff.getPersistedSPOsToRemove().size(), 2);
        assertEquals(diff.getPropertyDiffPersists().size(), 0); 
        
        Collections.sort(diff.getPersistedSPOsToRemove());
        Collections.sort(diff.getPersistedSPOsToAdd());
        Collections.sort(objectsToRemove);
        Collections.sort(objectsToAdd);
        
        assertEquals(diff.getPersistedSPOsToRemove(), objectsToRemove);
        assertEquals(diff.getPersistedSPOsToAdd(), objectsToAdd);
        
    }
    
    public void testAddedProperties() {
        
        propertiesToAdd.add(new PersistedSPOProperty("3", "value", DataType.INTEGER, null, 2, true));
        propertiesToAdd.add(new PersistedSPOProperty("4", "value", DataType.DOUBLE, null, 2.1, true));
        testRevisionTwoProperties.addAll(propertiesToAdd);
        
        diff.calcDiff(testRevisionOneObjects, testRevisionTwoObjects, testRevisionOneProperties, testRevisionTwoProperties);
        
        assertEquals(diff.getPersistedSPOsToAdd().size(), 0);
        assertEquals(diff.getPersistedSPOsToRemove().size(), 0);
        assertEquals(diff.getPropertyDiffPersists().size(), 2);        
        
        Collections.sort(diff.getPropertyDiffPersists());
        Collections.sort(propertiesToAdd);
        assertEquals(diff.getPropertyDiffPersists(), propertiesToAdd);
        
    }
    
    public void testNulledProperties() {
        
        testRevisionTwoProperties.remove(testRevisionTwoProperties.size() - 1);
        testRevisionTwoProperties.remove(testRevisionTwoProperties.size() - 1);
        
        diff.calcDiff(testRevisionOneObjects, testRevisionTwoObjects, testRevisionOneProperties, testRevisionTwoProperties);
        
        assertEquals(diff.getPersistedSPOsToAdd().size(), 0);
        assertEquals(diff.getPersistedSPOsToRemove().size(), 0);
        assertEquals(diff.getPropertyDiffPersists().size(), 2);
        
        Collections.sort(diff.getPropertyDiffPersists());
        assertNull(diff.getPropertyDiffPersists().get(0).getNewValue());
        assertEquals(diff.getPropertyDiffPersists().get(0).getUUID(), "2");
        assertNull(diff.getPropertyDiffPersists().get(1).getNewValue());
        assertEquals(diff.getPropertyDiffPersists().get(1).getUUID(), "7");
        
    }
    
    public void testChangedPropeties() {
        
        testRevisionTwoProperties.remove(testRevisionTwoProperties.size() - 1);
        testRevisionTwoProperties.remove(testRevisionTwoProperties.size() - 1);
        testRevisionTwoProperties.remove(testRevisionTwoProperties.size() - 1);
        
        propertiesToAdd.add(new PersistedSPOProperty("7", "name", DataType.STRING, "D1", "D1*", true));
        propertiesToAdd.add(new PersistedSPOProperty("2", "value", DataType.INTEGER, 1, 2, true));
        propertiesToAdd.add(new PersistedSPOProperty("7", "value", DataType.DOUBLE, 1.1, 2.1, true));
        testRevisionTwoProperties.addAll(propertiesToAdd);
        
        diff.calcDiff(testRevisionOneObjects, testRevisionTwoObjects, testRevisionOneProperties, testRevisionTwoProperties);

        assertEquals(diff.getPersistedSPOsToAdd().size(), 0);
        assertEquals(diff.getPersistedSPOsToRemove().size(), 0);
        assertEquals(diff.getPropertyDiffPersists().size(), 3);

        Collections.sort(diff.getPropertyDiffPersists());
        Collections.sort(propertiesToAdd);
        assertEquals(diff.getPropertyDiffPersists(), propertiesToAdd);
        
    }
    
    public void testPropertyAdditionsRemovalsAndChanges() {
        
        objectsToRemove.add(new PersistedSPObject("1", DiffTestClass.class.getName(), "8", 3));
        testRevisionOneObjects.addAll(objectsToRemove);
        
        // Will be removed because of object's removal. However, there will be no property persist call.
        propertiesToRemove.add(new PersistedSPOProperty("8", "name", DataType.STRING, null, "E1", true));
        testRevisionOneProperties.addAll(propertiesToRemove);
        
        objectsToAdd.add(new PersistedSPObject("4", DiffTestClass.class.getName(), "9", 1));
        testRevisionTwoObjects.addAll(objectsToAdd);
       
        propertiesToAdd.add(new PersistedSPOProperty("7", "name", DataType.STRING, "D1", "D1*", true));
        propertiesToAdd.add(new PersistedSPOProperty("2", "value", DataType.INTEGER, 1, 2, true));        
        propertiesToAdd.add(new PersistedSPOProperty("7", "value", DataType.DOUBLE, 1.1, 2.1, true));   
        propertiesToAdd.add(new PersistedSPOProperty("9", "name", DataType.STRING, null, "F1", true));
        testRevisionTwoProperties.addAll(propertiesToAdd);
        
        diff.calcDiff(testRevisionOneObjects, testRevisionTwoObjects, testRevisionOneProperties, testRevisionTwoProperties);

        assertEquals(diff.getPersistedSPOsToAdd().size(), 1);
        assertEquals(diff.getPersistedSPOsToRemove().size(), 1);
        assertEquals(diff.getPropertyDiffPersists().size(), 4);

        assertEquals(diff.getPersistedSPOsToAdd(), objectsToAdd);
        assertEquals(diff.getPersistedSPOsToRemove(), objectsToRemove);
        
        Collections.sort(diff.getPropertyDiffPersists());
        Collections.sort(propertiesToAdd);
        assertEquals(diff.getPropertyDiffPersists(), propertiesToAdd);
        
    }

//    public void testMockPersisting() {     
//        
//        objectsToRemove.add(new PersistedSPObject("1", DiffTestClass.class.getName(), "8", 3));
//        testRevisionOneObjects.addAll(objectsToRemove);
//        
//        // Will be removed because of object's removal. However, there will be no property persist call.
//        propertiesToRemove.add(new PersistedSPOProperty("8", "name", DataType.STRING, null, "E1", true));
//        testRevisionOneProperties.addAll(propertiesToRemove);
//        
//        objectsToAdd.add(new PersistedSPObject("4", DiffTestClass.class.getName(), "9", 1));
//        objectsToAdd.add(new PersistedSPObject("9", DiffTestClass.class.getName(), "10",0));
//        testRevisionTwoObjects.addAll(objectsToAdd);
//                               
//        testRevisionTwoProperties.remove(testRevisionTwoProperties.size() - 1);
//        testRevisionTwoProperties.remove(testRevisionTwoProperties.size() - 1);
//        testRevisionTwoProperties.remove(testRevisionTwoProperties.size() - 1);
//        
//        propertiesToAdd.add(new PersistedSPOProperty("7", "name", DataType.STRING, "D1", "D1*", true));
//        propertiesToAdd.add(new PersistedSPOProperty("2", "value", DataType.INTEGER, 1, 2, true));
//        propertiesToAdd.add(new PersistedSPOProperty("7", "value", DataType.DOUBLE, 1.1, 2.1, true));
//        propertiesToAdd.add(new PersistedSPOProperty("9", "name", DataType.STRING, null, "F1", true));
//        testRevisionTwoProperties.addAll(propertiesToAdd);       
//
//        
//        MockJCRPersister mockJCR = new MockJCRPersister();
//        mockJCR.addRevision(testRevisionOneObjects, testRevisionOneProperties);
//        mockJCR.addRevision(testRevisionTwoObjects, testRevisionTwoProperties);
//        
//        try {
//            diff.calcDiff(mockJCR, SPCredentials.createSystemInstance(), "test-id", 0, 1);
//        } catch (SPPersistenceException e) {
//            throw new RuntimeException(e);
//        }
//
//        assertEquals(diff.getPersistedSPOsToAdd().size(), 2);
//        assertEquals(diff.getPersistedSPOsToRemove().size(), 1);
//        assertEquals(diff.getPropertyDiffPersists().size(), 4);
//
//        assertEquals(diff.getPersistedSPOsToRemove(), objectsToRemove);
//        
//        Collections.sort(diff.getPersistedSPOsToAdd());
//        Collections.sort(objectsToAdd);        
//        assertEquals(diff.getPersistedSPOsToAdd(), objectsToAdd);
//        
//        Collections.sort(diff.getPropertyDiffPersists());
//        Collections.sort(propertiesToAdd);
//        assertEquals(diff.getPropertyDiffPersists(), propertiesToAdd);
//        
//        // Give the mock JCR the older revision again, so that the differ can modify it.
//                
//        mockJCR.addRevision(testRevisionOneObjects, testRevisionOneProperties); // revision 2        
//        
//        // revision 3
//        try {
//            diff.persistTo(mockJCR, false);
//        } catch (SPPersistenceException e) {
//            throw new RuntimeException(e);
//        }
//                
//        // Test that the revision has the right amount of objects and properties.
//        
//        Collections.sort(mockJCR.getObjectsFromRevision(3));
//        Collections.sort(mockJCR.getPropertiesFromRevision(3));
//        Collections.sort(testRevisionTwoObjects);
//        Collections.sort(testRevisionTwoProperties);
//        assertEquals(mockJCR.getObjectsFromRevision(3), testRevisionTwoObjects);
//        assertEquals(mockJCR.getPropertiesFromRevision(3), testRevisionTwoProperties);                
//        
//    }
    
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }
    
    /**
     * Regression test. Previously moving an object and adding a child
     * to the moved object in the same transaction would add the object
     * in twice in the end.
     */
    public void testMoveObjectWithNewChild() throws Exception {
        List<PersistedSPObject> oldList = new ArrayList<PersistedSPObject>();
        oldList.add(new PersistedSPObject(null, DiffTestClass.class.getName(), "1", 0));        
        oldList.add(new PersistedSPObject("1", DiffTestClass.class.getName(), "2", 0));
        oldList.add(new PersistedSPObject("1", DiffTestClass.class.getName(), "3", 1));
        List<PersistedSPObject> newList = new ArrayList<PersistedSPObject>();
        newList.add(new PersistedSPObject(null, DiffTestClass.class.getName(), "1", 0));
        newList.add(new PersistedSPObject("1", DiffTestClass.class.getName(), "3", 0));
        newList.add(new PersistedSPObject("1", DiffTestClass.class.getName(), "2", 1));
        newList.add(new PersistedSPObject("2", DiffTestClass.class.getName(), "4", 0));
        diff.calcDiff(oldList, newList, 
                new ArrayList<PersistedSPOProperty>(), new ArrayList<PersistedSPOProperty>());
        
        int childCount = 0;
        for (PersistedSPObject spo : diff.getPersistedSPOsToAdd()) {
            if (spo.getUUID().equals("4")) {
                childCount++;
            }
        }
        //previously was coming out as 2
        assertEquals(1, childCount);
    }

	/**
	 * If we add an object to the start of an existing list of objects we should
	 * just get the one addition. We shouldn't get the removal of a whole bunch
	 * of other objects
	 */
    public void testInsertWithoutRemovals() throws Exception {
    	List<PersistedSPObject> oldList = new ArrayList<PersistedSPObject>();
        oldList.add(new PersistedSPObject(null, DiffTestClass.class.getName(), "1", 0));        
        oldList.add(new PersistedSPObject("1", DiffTestClass.class.getName(), "2", 0));
        oldList.add(new PersistedSPObject("1", DiffTestClass.class.getName(), "3", 1));
        List<PersistedSPObject> newList = new ArrayList<PersistedSPObject>();
        newList.add(new PersistedSPObject(null, DiffTestClass.class.getName(), "1", 0));
        newList.add(new PersistedSPObject("1", DiffTestClass.class.getName(), "4", 0));
        newList.add(new PersistedSPObject("1", DiffTestClass.class.getName(), "2", 1));
        newList.add(new PersistedSPObject("1", DiffTestClass.class.getName(), "3", 2));
        diff.calcDiff(oldList, newList, 
                new ArrayList<PersistedSPOProperty>(), new ArrayList<PersistedSPOProperty>());
        
        assertEquals(0, diff.getPersistedSPOsToRemove().size());
        assertEquals(1, diff.getPersistedSPOsToAdd().size());
	}
    
    public void testChildrenOfDifferentTypes() throws Exception {
    	List<PersistedSPObject> oldList = new ArrayList<PersistedSPObject>();
        oldList.add(new PersistedSPObject(null, DiffTestClass2.class.getName(), "1", 0));        
        oldList.add(new PersistedSPObject("1", DiffTestClass2.class.getName(), "2", 0));
        oldList.add(new PersistedSPObject("1", DiffTestClass.class.getName(), "3", 0));
        List<PersistedSPObject> newList = new ArrayList<PersistedSPObject>();
        newList.add(new PersistedSPObject(null, DiffTestClass2.class.getName(), "1", 0));
        newList.add(new PersistedSPObject("1", DiffTestClass2.class.getName(), "4", 0));
        newList.add(new PersistedSPObject("1", DiffTestClass2.class.getName(), "2", 1));
        newList.add(new PersistedSPObject("1", DiffTestClass.class.getName(), "3", 0));
        diff.calcDiff(oldList, newList, 
                new ArrayList<PersistedSPOProperty>(), new ArrayList<PersistedSPOProperty>());
        
        assertEquals(0, diff.getPersistedSPOsToRemove().size());
        assertEquals(1, diff.getPersistedSPOsToAdd().size());
	}
    
    public void testMoveWithParentClass() throws Exception {
    	Differ diff = new Differ(DiffTestClass.class.getName());
    	List<PersistedSPObject> oldList = new ArrayList<PersistedSPObject>();
        oldList.add(new PersistedSPObject("0", DiffTestClass.class.getName(), "1", 0));        
        oldList.add(new PersistedSPObject("0", DiffTestClass.class.getName(), "2", 1));
        oldList.add(new PersistedSPObject("0", DiffTestClass.class.getName(), "3", 2));
        List<PersistedSPObject> newList = new ArrayList<PersistedSPObject>();
        newList.add(new PersistedSPObject("0", DiffTestClass.class.getName(), "1", 0));
        newList.add(new PersistedSPObject("0", DiffTestClass.class.getName(), "4", 1));
        newList.add(new PersistedSPObject("0", DiffTestClass.class.getName(), "2", 2));
        newList.add(new PersistedSPObject("0", DiffTestClass.class.getName(), "3", 3));
        diff.calcDiff(oldList, newList, 
                new ArrayList<PersistedSPOProperty>(), new ArrayList<PersistedSPOProperty>());
        
        assertEquals(0, diff.getPersistedSPOsToRemove().size());
        assertEquals(1, diff.getPersistedSPOsToAdd().size());
	}
    
    /**
	 * A regression test for #2708 where a child was moved but its parent was
	 * deleted. The problem that was occurring was the child would be moved but
	 * its properties would not be defined.
	 */
	public void testMoveChildParentDeleted() throws Exception {
		List<PersistedSPObject> oldList = new ArrayList<PersistedSPObject>();
        oldList.add(new PersistedSPObject(null, DiffTestClass.class.getName(), "1", 0));        
        oldList.add(new PersistedSPObject("1", DiffTestClass.class.getName(), "2", 0));
        oldList.add(new PersistedSPObject("1", DiffTestClass.class.getName(), "3", 1));
        oldList.add(new PersistedSPObject("3", DiffTestClass.class.getName(), "4", 0));
        List<PersistedSPOProperty> oldListProperties = new ArrayList<>();
        oldListProperties.add(new PersistedSPOProperty("4", "name", DataType.STRING, "A1", "A1", false));
        List<PersistedSPObject> newList = new ArrayList<PersistedSPObject>();
        newList.add(new PersistedSPObject(null, DiffTestClass.class.getName(), "1", 0));
        newList.add(new PersistedSPObject("1", DiffTestClass.class.getName(), "2", 0));
        newList.add(new PersistedSPObject("2", DiffTestClass.class.getName(), "4", 0));
        List<PersistedSPOProperty> newListProperties = new ArrayList<>();
        newListProperties.add(new PersistedSPOProperty("4", "name", DataType.STRING, "A1", "A1", false));
        diff.calcDiff(oldList, newList, oldListProperties, newListProperties);
        
        assertEquals(1, diff.getPersistedSPOsToRemove().size());
        assertEquals("3", diff.getPersistedSPOsToRemove().get(0).getUUID());
        
        assertEquals(1, diff.getPersistedSPOsToAdd().size());
        assertEquals("4", diff.getPersistedSPOsToAdd().get(0).getUUID());
        
        assertEquals(1, diff.getPropertyDiffPersists().size());
        PersistedSPOProperty nameProperty = diff.getPropertyDiffPersists().get(0);
		assertEquals("name", nameProperty.getPropertyName());
		assertEquals("4", nameProperty.getUUID());
		assertEquals("A1", nameProperty.getNewValue());
	}
	
	public void testAddChildWithProperties() throws Exception {
		List<PersistedSPObject> oldList = new ArrayList<PersistedSPObject>();
        oldList.add(new PersistedSPObject(null, DiffTestClass.class.getName(), "1", 0));        
        oldList.add(new PersistedSPObject("1", DiffTestClass.class.getName(), "2", 0));
        List<PersistedSPOProperty> oldListProperties = new ArrayList<>();
        List<PersistedSPObject> newList = new ArrayList<PersistedSPObject>();
        newList.add(new PersistedSPObject(null, DiffTestClass.class.getName(), "1", 0));
        newList.add(new PersistedSPObject("1", DiffTestClass.class.getName(), "2", 0));
        newList.add(new PersistedSPObject("2", DiffTestClass.class.getName(), "4", 0));
        List<PersistedSPOProperty> newListProperties = new ArrayList<>();
        newListProperties.add(new PersistedSPOProperty("4", "name", DataType.STRING, "A1", "A1", false));
        diff.calcDiff(oldList, newList, oldListProperties, newListProperties);
        
        assertEquals(0, diff.getPersistedSPOsToRemove().size());
        
        assertEquals(1, diff.getPersistedSPOsToAdd().size());
        assertEquals("4", diff.getPersistedSPOsToAdd().get(0).getUUID());
        
        assertEquals(1, diff.getPropertyDiffPersists().size());
        PersistedSPOProperty nameProperty = diff.getPropertyDiffPersists().get(0);
		assertEquals("name", nameProperty.getPropertyName());
		assertEquals("4", nameProperty.getUUID());
		assertEquals("A1", nameProperty.getNewValue());
	}
}
