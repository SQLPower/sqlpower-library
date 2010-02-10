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

package ca.sqlpower.object;

import java.awt.Image;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.log4j.Logger;

import ca.sqlpower.dao.PersistedSPOProperty;
import ca.sqlpower.dao.PersisterUtils;
import ca.sqlpower.dao.SPPersisterListener;
import ca.sqlpower.dao.SPSessionPersister;
import ca.sqlpower.dao.SPPersister.DataType;
import ca.sqlpower.dao.session.SessionPersisterSuperConverter;
import ca.sqlpower.object.SPChildEvent.EventType;
import ca.sqlpower.object.annotation.Accessor;
import ca.sqlpower.object.annotation.Mutator;
import ca.sqlpower.object.annotation.NonBound;
import ca.sqlpower.object.annotation.NonProperty;
import ca.sqlpower.object.annotation.Transient;
import ca.sqlpower.sqlobject.DatabaseConnectedTestCase;
import ca.sqlpower.sqlobject.SQLObject;
import ca.sqlpower.sqlobject.SQLObjectRoot;
import ca.sqlpower.testutil.GenericNewValueMaker;
import ca.sqlpower.testutil.NewValueMaker;
import ca.sqlpower.testutil.SPObjectRoot;
import ca.sqlpower.util.SPSession;
import ca.sqlpower.util.SessionNotFoundException;
import ca.sqlpower.util.StubSPSession;
import ca.sqlpower.util.TransactionEvent;

/**
 * Classes that implement SPObject and need to be persisted must implement
 * a test class that extends this test case.
 */
public abstract class PersistedSPObjectTest extends DatabaseConnectedTestCase {
	
	private static final Logger logger = Logger.getLogger(PersistedSPObjectTest.class);
	
	/**
	 * Returns a class that is one of the child types of the object under test. An
	 * object of this type must be able to be added as a child to the object without
	 * error. If the object under test does not allow children or all of the children
	 * of the object are final so none can be added, null will be returned.
	 */
	protected abstract Class<? extends SPObject> getChildClassType();
	
	/**
	 * This workspace contains the root SPObject made in setup. This is only needed
	 * for connecting the root to a session in setup. If a formal root object
	 * for a session gets created in the library it can replace this stub version. 
	 */
	private class StubWorkspace extends AbstractSPObject {
		
		private final SPSession session;

		public StubWorkspace(SPSession session) {
			this.session = session;
		}

		@Override
		protected boolean removeChildImpl(SPObject child) {
			return false;
		}

		public boolean allowsChildren() {
			return true;
		}

		public int childPositionOffset(Class<? extends SPObject> childType) {
			return 0;
		}

		public List<Class<? extends SPObject>> getAllowedChildTypes() {
			ArrayList<Class<? extends SPObject>> list = new ArrayList<Class<? extends SPObject>>();
			list.add(SPObjectRoot.class);
			return list;
		}

		public List<? extends SPObject> getChildren() {
			return Collections.singletonList(root);
		}

		public List<? extends SPObject> getDependencies() {
			return Collections.emptyList();
		}

		public void removeDependency(SPObject dependency) {
			//do nothing
		}
		
		@Override
		public SPSession getSession() throws SessionNotFoundException {
			return session;
		}
		
	}
	
	/**
	 * Used in roll back tests. If an exception is thrown due to failing a test
	 * the rollback will catch the exception and try to roll back the object.
	 * However, we want to know if the persist failed before rolling back. This
	 * object will store the failed reason during persist.
	 */
	private Throwable failureReason;
	
	private SPObjectRoot root;

	/**
	 * A new value maker that works off the root object and pl.ini in this test.
	 * This will need to be set as a different new value maker in tests that are
	 * outside of the library.
	 */
	private NewValueMaker valueMaker;

	/**
	 * This is a generic converter that works off the root object and pl.ini in
	 * this test. This will need to be set to a different converter in tests
	 * that are outside of the library.
	 */
	private SessionPersisterSuperConverter converter;

	/**
	 * A default session persister that is used in testing, connected to the root
	 * and persister factory.
	 */
	private SPSessionPersister persister;
	
	public PersistedSPObjectTest(String name) {
		super(name);
	}
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		root = new SPObjectRoot();
		SPSession stub = new StubSPSession() {
			private final SPObject workspace = new StubWorkspace(this);
			@Override
			public SPObject getWorkspace() {
				return workspace;
			}
		};
		root.setParent(stub.getWorkspace());
		SQLObjectRoot sqlRoot = new SQLObjectRoot();
		root.addChild(sqlRoot, 0);
		sqlRoot.addDatabase(db, 0);
		
		valueMaker = new GenericNewValueMaker(root, getPLIni());
		converter = new SessionPersisterSuperConverter(
				getPLIni(), root);
		
		persister = new SPSessionPersister(
				"Testing Persister", root, converter);
		persister.setSession(root.getSession());
	}

	/**
	 * Returns an object of the type being tested. Will be used in reflective
	 * tests being done for persisting objects. This must be a descendant of the
	 * root object returned from {@link #getRootObject()}.
	 */
	public abstract SPObject getSPObjectUnderTest();
	
	public SPObject getRootObject() {
		return root;
	}

	/**
	 * All {@link SPObject}s that are going to be persisted need to define
	 * an absolute ordering of their child type classes. This method ensures
	 * that list is retrievable by reflection from the object.
	 */
	@SuppressWarnings("unchecked")
	public void testStaticOrdering() throws Exception {
		Field absoluteChildOrder = getSPObjectUnderTest().getClass().getDeclaredField("allowedChildTypes");
		List<Class<? extends SPObject>> children = (List<Class<? extends SPObject>>) absoluteChildOrder.get(null);
		if (getSPObjectUnderTest().allowsChildren()) {
			assertFalse(children.isEmpty());
		} else {
			assertTrue(children.isEmpty());
		}
	}
	
	/**
	 * Tests the SPPersisterListener will persist a property change to its
	 * target persister.
	 */
	public void testSPListenerPersistsProperty() throws Exception {
		CountingSPPersister countingPersister = new CountingSPPersister();
		SPPersisterListener listener = new SPPersisterListener(countingPersister, converter);
		
		SPObject wo = getSPObjectUnderTest();
        wo.addSPListener(listener);

        List<PropertyDescriptor> settableProperties;
        settableProperties = Arrays.asList(PropertyUtils.getPropertyDescriptors(wo.getClass()));
        
        Set<String> propertiesToPersist = findPersistableBeanProperties(false, false);

        for (PropertyDescriptor property : settableProperties) {
            Object oldVal;
            
            if (!propertiesToPersist.contains(property.getName())) continue;

            countingPersister.clearAllPropertyChanges();
            try {
                oldVal = PropertyUtils.getSimpleProperty(wo, property.getName());

                // check for a setter
                if (property.getWriteMethod() == null) continue;
                
            } catch (NoSuchMethodException e) {
                logger.debug("Skipping non-settable property " + property.getName() + " on " +
                		wo.getClass().getName());
                continue;
            }
            
            Object newVal = valueMaker.makeNewValue(property.getPropertyType(), oldVal, property.getName());
            int oldChangeCount = countingPersister.getPersistPropertyCount();
            
            try {
                logger.debug("Setting property '" + property.getName() + "' to '" + newVal + 
                		"' (" + newVal.getClass().getName() + ")");
                BeanUtils.copyProperty(wo, property.getName(), newVal);

                assertTrue("Did not persist property " + property.getName(), 
                		oldChangeCount < countingPersister.getPersistPropertyCount());
                
                //The first property change at current is always the property change we are
                //looking for, this may need to be changed in the future to find the correct
                //property.
                PersistedSPOProperty propertyChange = null;
                
                for (PersistedSPOProperty nextPropertyChange : countingPersister.getPersistPropertyList()) {
                	if (nextPropertyChange.getPropertyName().equals(property.getName())) {
                		propertyChange = nextPropertyChange;
                		break;
                	}
                }
                assertNotNull("A property change event cannot be found for the property " + 
                		property.getName(), propertyChange);
                
                assertEquals(wo.getUUID(), propertyChange.getUUID());
                assertEquals(property.getName(), propertyChange.getPropertyName());
                
				assertEquals("Old value of property " + property.getName() + " was wrong, value expected was  " + oldVal + 
						" but is " + countingPersister.getLastOldValue(), oldVal, 
                		propertyChange.getOldValue());
				
				//Input streams from images are being compared by hash code not values
				if (Image.class.isAssignableFrom(property.getPropertyType())) {
					logger.debug(propertyChange.getNewValue().getClass());
					assertTrue(Arrays.equals(PersisterUtils.convertImageToStreamAsPNG(
								(Image) newVal).toByteArray(),
							PersisterUtils.convertImageToStreamAsPNG(
								(Image) converter.convertToComplexType(
										propertyChange.getNewValue(), Image.class)).toByteArray()));
				} else {
					assertEquals(newVal, propertyChange.getNewValue());
				}
                Class<? extends Object> classType;
                if (oldVal != null) {
                	classType = oldVal.getClass();
                } else {
                	classType = newVal.getClass();
                }
                assertEquals(PersisterUtils.getDataType(classType), propertyChange.getDataType());
            } catch (InvocationTargetException e) {
                logger.debug("(non-fatal) Failed to write property '"+property.getName()+" to type "+wo.getClass().getName());
            }
        }
	}
	
	/**
	 * Tests the {@link SPSessionPersister} can update every settable property
	 * on an object based on a persist call.
	 */
	public void testSPPersisterPersistsProperties() throws Exception {
		SPObject objectUnderTest = getSPObjectUnderTest();
		
		List<PropertyDescriptor> settableProperties = Arrays.asList(
				PropertyUtils.getPropertyDescriptors(objectUnderTest.getClass()));
		
		Set<String> propertiesToPersist = findPersistableBeanProperties(false, false);
		
		for (PropertyDescriptor property : settableProperties) {
            Object oldVal;

            //Changing the UUID of the object makes it referenced as a different object
            //and would make the check later in this test fail.
            if (property.getName().equals("UUID")) continue;
            
            if (!propertiesToPersist.contains(property.getName())) continue;

            try {
                oldVal = PropertyUtils.getSimpleProperty(objectUnderTest, property.getName());

                // check for a setter
                if (property.getWriteMethod() == null) continue;
                
            } catch (NoSuchMethodException e) {
                logger.debug("Skipping non-settable property " + property.getName() +
                		" on " + objectUnderTest.getClass().getName());
                continue;
            }
            
            //special case for parent types. If a specific wabit object has a tighter parent then
            //WabitObject the getParentClass should return the parent type.
            Class<?> propertyType = property.getPropertyType();
            if (property.getName().equals("parent")) {
            	propertyType = getSPObjectUnderTest().getClass().getMethod("getParent").getReturnType();
            	logger.debug("Persisting parent, type is " + propertyType);
            }
            Object newVal = valueMaker.makeNewValue(propertyType, oldVal, property.getName());
            
            System.out.println("Persisting property \"" + property.getName() + "\" from oldVal \"" + oldVal + "\" to newVal \"" + newVal + "\"");
            
            DataType type = PersisterUtils.getDataType(property.getPropertyType());
			Object basicNewValue = converter.convertToBasicType(newVal);
			persister.begin();
			persister.persistProperty(objectUnderTest.getUUID(), property.getName(), type, 
					converter.convertToBasicType(oldVal), 
					basicNewValue);
			persister.commit();
			
			Object newValAfterSet = PropertyUtils.getSimpleProperty(objectUnderTest, property.getName());
			Object basicExpectedValue = converter.convertToBasicType(newValAfterSet);
			
			assertPersistedValuesAreEqual(newVal, newValAfterSet, basicNewValue, 
					basicExpectedValue, property.getPropertyType());
    	}
	}
	
	/**
	 * This test will be run for each object that extends SPObject and confirms
	 * the SPSessionPersister can create new objects 
	 * @throws Exception
	 */
	public void testPersisterCreatesNewObjects() throws Exception {
		SPObject newRoot = new SPObjectRoot();
		SPSession stub = new StubSPSession() {
			private final SPObject workspace = new StubWorkspace(this);
			@Override
			public SPObject getWorkspace() {
				return workspace;
			}
		};
		newRoot.setParent(stub.getWorkspace());
		
		NewValueMaker newValueMaker = new GenericNewValueMaker(newRoot, getPLIni());
		
		SessionPersisterSuperConverter newConverter = new SessionPersisterSuperConverter(
				getPLIni(), newRoot);
		
		SPSessionPersister persister = new SPSessionPersister("Test persister", newRoot, newConverter);
		persister.setSession(stub);
		
		SPObject objectUnderTest = getSPObjectUnderTest();
		
		Set<String> propertiesToPersist = findPersistableBeanProperties(false, false);
		
		List<PropertyDescriptor> settableProperties = Arrays.asList(
				PropertyUtils.getPropertyDescriptors(objectUnderTest.getClass()));
		
		//set all properties of the object
        for (PropertyDescriptor property : settableProperties) {
            Object oldVal;
            if (!propertiesToPersist.contains(property.getName())) continue;
            if (property.getName().equals("parent")) continue; //Changing the parent causes headaches.
            
            try {
                oldVal = PropertyUtils.getSimpleProperty(objectUnderTest, property.getName());

                // check for a setter
                if (property.getWriteMethod() == null) continue;
                
            } catch (NoSuchMethodException e) {
                logger.debug("Skipping non-settable property " + property.getName() + " on " + 
                		objectUnderTest.getClass().getName());
                continue;
            }
            
            Object newVal = valueMaker.makeNewValue(property.getPropertyType(), oldVal, property.getName());
            Object newValInNewRoot = newValueMaker.makeNewValue(property.getPropertyType(), oldVal, property.getName());
            if (newValInNewRoot instanceof SPObject) {
            	((SPObject) newValInNewRoot).setUUID(((SPObject) newVal).getUUID());
            }
            
            try {
                logger.debug("Setting property '" + property.getName() + "' to '" + newVal + 
                		"' (" + newVal.getClass().getName() + ")");
                BeanUtils.copyProperty(objectUnderTest, property.getName(), newVal);
                
            } catch (InvocationTargetException e) {
                logger.debug("(non-fatal) Failed to write property '" + property.getName() + 
                		" to type " + objectUnderTest.getClass().getName());
            }
        }
		
		//create a new root and parent for the object
        SPObject newParent;
        if (objectUnderTest.getParent() instanceof SPObjectRoot) {
        	newParent = newRoot;
        } else {
        	newParent = (SPObject) newValueMaker.makeNewValue(
        			objectUnderTest.getParent().getClass(), null, "");
        }
        newParent.setUUID(objectUnderTest.getParent().getUUID());
		
        int childCount = newParent.getChildren().size();
        
		//persist the object to the new target root
        new SPPersisterListener(persister, converter).persistObject(objectUnderTest, 
        		objectUnderTest.getParent().getChildren(objectUnderTest.getClass()).indexOf(objectUnderTest));
		
		//check object exists
        assertEquals(childCount + 1, newParent.getChildren().size());
        SPObject newChild = null;
        for (SPObject child : newParent.getChildren()) {
        	if (child.getUUID().equals(objectUnderTest.getUUID())) {
        		newChild = child;
        		break;
        	}
        }
        if (newChild == null) fail("The child was not correctly persisted.");
		
		//check all interesting properties
        for (PropertyDescriptor property : settableProperties) {
            if (!propertiesToPersist.contains(property.getName())) continue;
            if (property.getName().equals("parent")) continue; //Changing the parent causes headaches.
            
            Method readMethod = property.getReadMethod();
            
            Object valueBeforePersist = readMethod.invoke(objectUnderTest);
            Object valueAfterPersist = readMethod.invoke(newChild);
            Object basicValueBeforePersist = converter.convertToBasicType(valueBeforePersist);
            Object basicValueAfterPersist = newConverter.convertToBasicType(valueAfterPersist);
            
            assertPersistedValuesAreEqual(valueBeforePersist, valueAfterPersist, 
            		basicValueBeforePersist, basicValueAfterPersist, readMethod.getReturnType());
        }
	}

	/**
	 * Tests passing an object to an {@link SPPersisterListener} will persist
	 * the object and all of the properties that have setters.
	 */
	public void testSPListenerPersistsNewObjects() throws Exception {
		CountingSPPersister persister = new CountingSPPersister();
		
		SPObject objectUnderTest = getSPObjectUnderTest();
		
		Set<String> propertiesToPersist = findPersistableBeanProperties(false, false);
		
		List<PropertyDescriptor> settableProperties = Arrays.asList(
				PropertyUtils.getPropertyDescriptors(objectUnderTest.getClass()));
		
		//set all properties of the object
        for (PropertyDescriptor property : settableProperties) {
            Object oldVal;
            if (!propertiesToPersist.contains(property.getName())) continue;
            if (property.getName().equals("parent")) continue; //Changing the parent causes headaches.
            
            try {
                oldVal = PropertyUtils.getSimpleProperty(objectUnderTest, property.getName());

                // check for a setter
                if (property.getWriteMethod() == null) continue;
                
            } catch (NoSuchMethodException e) {
                logger.debug("Skipping non-settable property " + property.getName() + " on " + 
                		objectUnderTest.getClass().getName());
                continue;
            }
            
            Object newVal = valueMaker.makeNewValue(property.getPropertyType(), oldVal, property.getName());
            
            try {
                logger.debug("Setting property '" + property.getName() + "' to '" + newVal + 
                		"' (" + newVal.getClass().getName() + ")");
                BeanUtils.copyProperty(objectUnderTest, property.getName(), newVal);
                
            } catch (InvocationTargetException e) {
                logger.debug("(non-fatal) Failed to write property '" + property.getName() + 
                		" to type " + objectUnderTest.getClass().getName());
            }
        }
        
        //persist the object to the new target root
        new SPPersisterListener(persister, converter).persistObject(objectUnderTest, 
        		objectUnderTest.getParent().getChildren(objectUnderTest.getClass()).indexOf(objectUnderTest));
		
        assertTrue(persister.getPersistPropertyCount() > 0);
        
        assertEquals(getSPObjectUnderTest().getUUID(), persister.getPersistObjectList().get(0).getUUID());
        
        //set all properties of the object
        for (PropertyDescriptor property : settableProperties) {
            Object oldVal;
            if (!propertiesToPersist.contains(property.getName())) continue;
            if (property.getName().equals("parent")) continue; //Changing the parent causes headaches.
            
            try {
                oldVal = PropertyUtils.getSimpleProperty(objectUnderTest, property.getName());

                // check for a setter
                if (property.getWriteMethod() == null) continue;
                
            } catch (NoSuchMethodException e) {
                logger.debug("Skipping non-settable property " + property.getName() + " on " + 
                		objectUnderTest.getClass().getName());
                continue;
            }
            
            Object newValue = null;
            
            boolean found = false;
            for (PersistedSPOProperty persistedSPO : persister.getPersistPropertyList()) {
            	if (persistedSPO.getPropertyName().equals(property.getName()) &&
            			persistedSPO.getUUID().equals(getSPObjectUnderTest().getUUID())) {
            		newValue = persistedSPO.getNewValue();
            		found = true;
            		break;
            	}
            }
            
            assertTrue("Could not find the persist call for property " + property.getName(), found);
            
            if (oldVal == null) {
            	assertNull(newValue);
            } else {
            	assertPersistedValuesAreEqual(oldVal, 
            			converter.convertToComplexType(newValue, oldVal.getClass()), 
            			converter.convertToBasicType(oldVal), newValue, property.getPropertyType());
            }
        }
	}

	/**
	 * This test will make changes to the {@link SPObject} under test and then
	 * cause an exception forcing the persister to roll back the changes in the
	 * object.
	 * <p>
	 * Both the changes have to come through the persister initially before the
	 * exception and they have to be reset after the exception.
	 */
	//TODO add this back in after updating the persister helper generators
//	public void testSessionPersisterRollsBackProperties() throws Exception {
//		SPObject objectUnderTest = getSPObjectUnderTest();
//		final Map<PropertyDescriptor, Object> initialProperties = new HashMap<PropertyDescriptor, Object>();
//		final Map<PropertyDescriptor, Object> newProperties = new HashMap<PropertyDescriptor, Object>();
//		
//		List<PropertyDescriptor> settableProperties = Arrays.asList(
//				PropertyUtils.getPropertyDescriptors(objectUnderTest.getClass()));
//		
//		Set<String> propertiesToPersist = findPersistableBeanProperties(false, false);
//		
//		failureReason = null;
//		
//		//Second factory to send the events to a black hole
//		SPPersisterHelperFactory secondFactory = new SPPersisterHelperFactoryImpl(new CountingSPPersister(), converter);
//		
//		SPPersisterListener listener = new SPPersisterListener(secondFactory) {
//			@Override
//			public void transactionEnded(TransactionEvent e) {
//				try {
//					for (Map.Entry<PropertyDescriptor, Object> newProperty : newProperties.entrySet()) {
//						Object objectUnderTest = getSPObjectUnderTest();
//						Object newVal = newProperty.getValue();
//						Object basicNewValue = converter.convertToBasicType(newVal);
//
//						Object newValAfterSet = PropertyUtils.getSimpleProperty(
//								objectUnderTest, newProperty.getKey().getName());
//						Object basicExpectedValue = converter.convertToBasicType(newValAfterSet);
//
//						assertPersistedValuesAreEqual(newVal, newValAfterSet, basicNewValue, 
//								basicExpectedValue, newProperty.getKey().getPropertyType());
//					}
//				} catch (Throwable ex) {
//					failureReason = ex;
//					throw new RuntimeException(ex);
//				}
//				throw new RuntimeException("Forcing rollback.");
//			}
//		};
//		//Transactions begin and commits are currently sent on the workspace.
//		persister.getSession().getWorkspace().addSPListener(listener);
//		
//		persister.begin();
//		for (PropertyDescriptor property : settableProperties) {
//            Object oldVal;
//
//            //Changing the UUID of the object makes it referenced as a different object
//            //and would make the check later in this test fail.
//            if (property.getName().equals("UUID")) continue;
//            
//            if (!propertiesToPersist.contains(property.getName())) continue;
//
//            try {
//                oldVal = PropertyUtils.getSimpleProperty(objectUnderTest, property.getName());
//
//                // check for a setter
//                if (property.getWriteMethod() == null) continue;
//                
//            } catch (NoSuchMethodException e) {
//                logger.debug("Skipping non-settable property " + property.getName() +
//                		" on " + objectUnderTest.getClass().getName());
//                continue;
//            }
//            
//            initialProperties.put(property, oldVal);
//            
//            //special case for parent types. If a specific wabit object has a tighter parent then
//            //WabitObject the getParentClass should return the parent type.
//            Class<?> propertyType = property.getPropertyType();
//            if (property.getName().equals("parent")) {
//            	propertyType = getSPObjectUnderTest().getClass().getMethod("getParent").getReturnType();
//            	logger.debug("Persisting parent, type is " + propertyType);
//            }
//            Object newVal = valueMaker.makeNewValue(propertyType, oldVal, property.getName());
//            
//            DataType type = PersisterUtils.getDataType(property.getPropertyType());
//			Object basicNewValue = converter.convertToBasicType(newVal);
//			persister.begin();
//			persister.persistProperty(objectUnderTest.getUUID(), property.getName(), type, 
//					converter.convertToBasicType(oldVal), 
//					basicNewValue);
//			persister.commit();
//            
//            newProperties.put(property, newVal);
//    	}
//		try {
//			persister.commit();
//			fail("An exception should make the persister hit the exception block.");
//		} catch (Exception e) {
//			//continue, exception expected.
//		}
//		
//		if (failureReason != null) {
//			throw new RuntimeException("Failed when asserting properties were " +
//					"fully persisted.", failureReason);
//		}
//		
//		for (Map.Entry<PropertyDescriptor, Object> entry : initialProperties.entrySet()) {
//			assertEquals("Property " + entry.getKey().getName() + " did not match after rollback.", 
//					entry.getValue(), PropertyUtils.getSimpleProperty(objectUnderTest, entry.getKey().getName()));
//		}
//	}
	
	/**
	 * Taken from AbstractWabitObjectTest. When Wabit is using annotations
	 * remove this method from that class as this class will be doing the reflective
	 * tests.
	 * <p>
	 * Tests that the new value that was persisted is the same as an old value
	 * that was to be persisted. This helper method for the persister tests will
	 * compare the values by their converted type or some other means as not all
	 * values that are persisted have implemented their equals method.
	 * <p>
	 * This will do the asserts to compare if the objects are equal.
	 * 
	 * @param valueBeforePersist
	 *            the value that we are expecting the persisted value to contain
	 * @param valueAfterPersist
	 *            the value that was persisted to the object. This will be
	 *            tested against the valueBeforePersist to ensure that they are
	 *            the same.
	 * @param basicValueBeforePersist
	 *            The valueBeforePersist converted to a basic type by a
	 *            converter.
	 * @param basicValueAfterPersist
	 *            The valueAfterPersist converted to a basic type by a
	 *            converter.
	 * @param valueType
	 *            The type of object the before and after values should contain.
	 */
    private void assertPersistedValuesAreEqual(Object valueBeforePersist, Object valueAfterPersist, 
    		Object basicValueBeforePersist, Object basicValueAfterPersist, 
    		Class<? extends Object> valueType) {
    	
		//Input streams from images are being compared by hash code not values
		if (Image.class.isAssignableFrom(valueType)) {
			assertTrue(Arrays.equals(PersisterUtils.convertImageToStreamAsPNG((Image) valueBeforePersist).toByteArray(),
					PersisterUtils.convertImageToStreamAsPNG((Image) valueAfterPersist).toByteArray()));
		} else {

			//Not all new values are equivalent to their old values so we are
			//comparing them by their basic type as that is at least comparable, in most cases, i hope.
			assertEquals("Persist failed for type " + valueType, basicValueBeforePersist, basicValueAfterPersist);
		}
    }

	/**
	 * Ensures that each getter and setter in the object under test is annotated
	 * in some way. This way methods that need to be annotated to be persisted
	 * will not be missed or will be defined to be skipped. The annotations are either
	 * {@link Accessor} for getters, {@link Mutator} for setters, and {@link NonProperty}
	 * that is neither an accessor or mutator but looks like one.
	 */
	public void testGettersAndSettersPersistedAnnotated() throws Exception {
		findPersistableBeanProperties(false, false);
	}

	/**
	 * This method will iterate through all of the methods in a class and
	 * collect all of the methods defined as setters and getters. The list will
	 * then be filtered out to only contain the bean property names of those
	 * setters and getters where each property has both a getter and setter.
	 * This will also fail if a getter or setter is found that is not properly
	 * annotated.
	 * 
	 * @param includeTransient
	 *            If true the transient properties will be included in the
	 *            resulting list. If false only the property names that are to
	 *            be persisted will be included.
	 * @param includeConstructorMutators
	 *            If true mutators that can only be set shortly after the object
	 *            has been created will be included. These mutators are normally
	 *            used for objects that cannot be given in the constructor but
	 *            should be treated as though they were final.
	 * 
	 * @return A list of bean property names that have both a getter and setter.
	 *         The transient bean properties will be included if the transient
	 *         flag is set to true.
	 * 
	 */
	private Set<String> findPersistableBeanProperties(boolean includeTransient, boolean includeConstructorMutators) throws Exception {
		SPObject objectUnderTest = getSPObjectUnderTest();
		Set<String> getters = new HashSet<String>();
		Set<String> setters = new HashSet<String>();
		for (Method m : objectUnderTest.getClass().getMethods()) {
			if (m.getName().equals("getClass")) continue;
			
			//skip non-public methods as they are not visible for persisting anyways.
			if (!Modifier.isPublic(m.getModifiers())) continue;
			//skip static methods
			if (Modifier.isStatic(m.getModifiers())) continue;
			
			if (m.getName().startsWith("get") || m.getName().startsWith("is")) {
				Class<?> parentClass = objectUnderTest.getClass();
				boolean accessor = false;
				boolean ignored = false;
				boolean isTransient = false;
				parentClass.getMethod(m.getName(), m.getParameterTypes());//test
				while (parentClass != null) {
					Method parentMethod;
					try {
						parentMethod = parentClass.getMethod(m.getName(), m.getParameterTypes());
					} catch (NoSuchMethodException e) {
						parentClass = parentClass.getSuperclass();
						continue;
					}
					if (parentMethod.getAnnotation(Accessor.class) != null) {
						accessor = true;
						if (parentMethod.getAnnotation(Transient.class) != null) {
							isTransient = true;
						}
						break;
					} else if (parentMethod.getAnnotation(NonProperty.class) != null ||
							parentMethod.getAnnotation(NonBound.class) != null) {
						ignored = true;
						break;
					}
					parentClass = parentClass.getSuperclass();
				}
				if (accessor) {
					if (includeTransient || !isTransient) {
						if (m.getName().startsWith("get")) {
							getters.add(m.getName().substring(3));
						} else if (m.getName().startsWith("is")) {
							getters.add(m.getName().substring(2));
						}
					}
				} else if (ignored) {
					//ignored so skip
				} else {
					fail("The method " + m.getName() + " is a getter that is not annotated " +
							"to be an accessor or transient. The exiting annotations are " + 
							Arrays.toString(m.getAnnotations()));
				}
			} else if (m.getName().startsWith("set")) {
				if (m.getAnnotation(Mutator.class) != null) {
					if ((includeTransient || m.getAnnotation(Transient.class) == null)
							&& (includeConstructorMutators || !m.getAnnotation(Mutator.class).constructorMutator())) {
						setters.add(m.getName().substring(3));
					}
				} else if (m.getAnnotation(NonProperty.class) != null ||
						m.getAnnotation(NonBound.class) != null) {
					//ignored so skip and pass
				} else {
					fail("The method " + m.getName() + " is a setter that is not annotated " +
							"to be a mutator or transient.");
				}
			}
		}
		
		Set<String> beanNames = new HashSet<String>();
		for (String beanName : getters) {
			if (setters.contains(beanName)) {
				String firstLetter = new String(new char[]{beanName.charAt(0)});
				beanNames.add(beanName.replaceFirst(firstLetter, firstLetter.toLowerCase()));
			}
		}
		return beanNames;
	}
	
    public void testSPPersisterAddsChild() throws Exception {
    	
    	SPObject spObject = getSPObjectUnderTest();
    	int oldChildCount = spObject.getChildren().size();
    	if (!spObject.allowsChildren()) return;
    	
    	Class<? extends SPObject> childClassType = getChildClassType();
    	if (childClassType == null) return;
    	
    	SPSessionPersister persister = new SPSessionPersister("test", getSPObjectUnderTest(), converter);
    	persister.setSession(getSPObjectUnderTest().getSession());
    	SPPersisterListener listener = new SPPersisterListener(persister, converter);
    	
    	SQLObject newChild = (SQLObject) valueMaker.makeNewValue(childClassType, null, "child");
    	newChild.setParent(spObject);
    	
    	listener.childAdded(new SPChildEvent(spObject, childClassType, newChild, 0, EventType.ADDED));
    	
    	assertEquals(oldChildCount + 1, spObject.getChildren().size());
    	assertEquals(newChild, spObject.getChildren().get(0));
    }
    
    public void testAddChildFiresEvents() throws Exception {
    	SPObject o = getSPObjectUnderTest();
    	
    	if (!o.allowsChildren()) return;
    	
    	Class<?> childClassType = getChildClassType();
    	if (childClassType == null) return;
    	
    	CountingSPListener listener = new CountingSPListener();
		
    	o.addSPListener(listener);
    	
    	NewValueMaker valueMaker = new GenericNewValueMaker(getRootObject());
    	SQLObject newChild = (SQLObject) valueMaker.makeNewValue(childClassType, null, "child");
    	
    	o.addChild(newChild, 0);
    	
    	assertEquals(1, listener.childAddedCount);
    }
    
    
    private class CountingSPListener implements SPListener {

    	private int childAddedCount = 0;
    	private int childRemovedCount = 0;
    	private int transactionEndedCount = 0;
    	private int transactionRollbackCount = 0;
    	private int transactionStartedCount = 0;
    	private int propertyChangedCount = 0;
    	
    	
		public void childAdded(SPChildEvent e) {
			childAddedCount++;
		}

		public void childRemoved(SPChildEvent e) {
			childRemovedCount++;
		}

		public void transactionEnded(TransactionEvent e) {
			transactionEndedCount++;
		}

		public void transactionRollback(TransactionEvent e) {
			transactionRollbackCount++;
		}

		public void transactionStarted(TransactionEvent e) {
			transactionStartedCount++;
		}

		public void propertyChanged(PropertyChangeEvent evt) {
			propertyChangedCount++;
		}
    }
}
