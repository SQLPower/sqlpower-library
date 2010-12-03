/*
 * Copyright (c) 2010, SQL Power Group Inc.
 */

package ca.sqlpower.dao;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

import junit.framework.TestCase;
import ca.sqlpower.dao.SPPersistenceException;
import ca.sqlpower.dao.SPPersister;
import ca.sqlpower.dao.SPPersister.DataType;

/**
 * A base class for testing methods of persistence. Classes that extend this must define the
 * persister variable that will be used for testing in order for any of the tests to pass. 
 */
public abstract class PersisterTest extends TestCase {

	/**
	 * The persister for which these tests will be applied. Subclasses must define the specific
	 * persister, as it is not defined here.
	 */
	protected SPPersister persister;
	protected byte[] receivedData = new byte[1024];
	protected boolean receivedBoolean;
	protected int receivedInt;
	protected long receivedLong;
	protected double receivedDouble;
	protected String receivedString;
	protected String receivedCareer;
	protected String receivedReference;
	protected int numObjects = 0;
	protected CountDownLatch receiverLatch;
	protected SPPersister receiver = new SPPersister(){
			
			private int transactionCount;
			
			public void begin() throws SPPersistenceException {
				transactionCount++;
			}
			public void commit() throws SPPersistenceException {
				transactionCount--;
				if (transactionCount == 0 && receiverLatch != null) {
					receiverLatch.countDown();
				}
			}
			
			public void persistObject(String parentUUID, String type, String uuid, int index)
					throws SPPersistenceException {
				System.out.println("Received Object " + type + "(" + uuid + ") as child of (" + parentUUID + ")");
				numObjects++;
			}
	
			public void persistProperty(String uuid, String propertyName,
					DataType propertyType, Object oldValue, Object newValue)
					throws SPPersistenceException {}
	
			public void persistProperty(String uuid, String propertyName,
					DataType propertyType, Object newValue)
					throws SPPersistenceException {
				System.out.println("Received property " + propertyName + " as type " + propertyType.getTypeName());
				if (propertyType == DataType.PNG_IMG) {
					assertEquals("image", propertyName);
					try {
						((InputStream) newValue).read(receivedData);
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				} else if (propertyType == DataType.REFERENCE) {
					assertEquals("reference", propertyName);
					receivedReference = (String) newValue;
				} else if (propertyName.equals("Career")) {      
					receivedCareer = (String) newValue;
				} else if (propertyName.equals("name")) {     
					receivedString = (String) newValue;
				} else if (propertyType == DataType.INTEGER) {
					assertEquals("integer", propertyName);
					receivedInt = (Integer) newValue;
				} else if (propertyType == DataType.DOUBLE) {
					assertEquals("double", propertyName);
					receivedDouble = (Double) newValue;
				} else if (propertyType == DataType.BOOLEAN) {
					assertEquals("bool", propertyName);
					receivedBoolean = (Boolean) newValue;
				} else if (propertyType == DataType.LONG) {
					assertEquals("long", propertyName);
					receivedLong = (Long) newValue;
				}
			}
	
			public void removeObject(String parentUUID, String uuid)
					throws SPPersistenceException {
				numObjects--;
			}
	
			public void rollback() {
				// TODO Auto-generated method stub
				
			}
		};
	protected SPPersister orderChecker = new SPPersister() {
			public void begin() throws SPPersistenceException {
			}
			public void commit() throws SPPersistenceException {
			}
			public void persistObject(String parentUUID, String type, String uuid,
					int index) throws SPPersistenceException {
				if (type.equals("ca.sqlpower.testutil.SPObjectRoot")) return;
				assertEquals(index, Integer.parseInt(uuid));
			}
			public void persistProperty(String uuid, String propertyName,
					DataType propertyType, Object oldValue, Object newValue)
					throws SPPersistenceException {
			}
			public void persistProperty(String uuid, String propertyName,
					DataType propertyType, Object newValue)
					throws SPPersistenceException {
				if (uuid.equals(workspaceId)) return;
				assertEquals(uuid, newValue);
			}
			public void removeObject(String parentUUID, String uuid)
					throws SPPersistenceException {
			}
			public void rollback() {
			}
		};
	protected String workspaceId;

	public PersisterTest() {
		super();
	}

	public PersisterTest(String name) {
		super(name);
	}

	public void testBegin() throws Exception {
		System.out.println("\n\nTesting Begin:");
		persister.begin();
	}

	public void testPersistObject() throws Exception {
		System.out.println("\n\nTesting Persist Object:");
		persister.begin();
		persister.persistObject(workspaceId, "ca.sqlpower.sqlobject.SQLColumn", "child", 0);
		persister.persistProperty("child", "name", DataType.STRING, "name");
		persister.commit();
		persister.begin();
		persister.persistObject(workspaceId, "ca.sqlpower.sqlobject.SQLColumn", "otherchild", 0);
		persister.persistProperty("otherchild", "name", DataType.STRING, "name");
		persister.commit();
		loadWorkspace();
		assertEquals(3, numObjects);
	}

	public void testPersistProperty() throws Exception {
		System.out.println("\n\nTesting Persist Property:");
		persister.begin();
		persister.persistObject(workspaceId, "ca.sqlpower.sqlobject.SQLColumn", "child", 0);
		persister.persistProperty("child", "name", DataType.STRING, "Chart Name");
		persister.persistProperty("child", "integer", DataType.INTEGER, 8);
		persister.persistProperty("child", "double", DataType.DOUBLE, 5.15);
		persister.persistProperty("child", "bool", DataType.BOOLEAN, true);
		persister.commit();
		loadWorkspace();
		assertEquals("Chart Name", receivedString);
		assertEquals(8, receivedInt);
		assertTrue(Math.abs(receivedDouble - 5.15) <= .001);
		assertEquals(true, receivedBoolean);
	}

	/**
	 * Since the JCR stores integers in long fields we need to confirm the value
	 * that is retrieved from the JCR is the correct value we put in. Note that this test
	 * is not specific to the JCR persistence layer.
	 */
	public void testPersistNegativeInteger() throws Exception {
	    persister.begin();
	    persister.persistObject(workspaceId, "ca.sqlpower.testutil.SPObjectRoot", "child", 0);
	    persister.persistProperty("child", "name", DataType.STRING, "test negative int");
	    persister.persistProperty("child", "integer", DataType.INTEGER, Integer.valueOf(-1));
	    persister.commit();
	    receivedInt = 0;
	    loadWorkspace();
	    assertEquals(-1, receivedInt);
	}

	/**
	 * Since the JCR stores integers in long fields we need to confirm the value
	 * that is retrieved from the JCR is the correct value we put in. Note that this test
	 * is not specific to the JCR persistence layer.
	 */
	public void testPersistNegativeLong() throws Exception {
	    persister.begin();
	    persister.persistObject(workspaceId, "ca.sqlpower.testutil.SPObjectRoot", "child", 0);
	    persister.persistProperty("child", "name", DataType.STRING, "test negative int");
	    persister.persistProperty("child", "long", DataType.LONG, Long.valueOf(-1));
	    persister.commit();
	    loadWorkspace();
	    assertEquals(-1, receivedLong);
	}

	public void testPersistNull() throws Exception {
		System.out.println("\n\nTesting Persist Null");
		persister.begin();
		persister.persistObject(workspaceId, "ca.sqlpower.sqlobject.SQLColumn", "child", 0);
		persister.persistProperty("child", "name", DataType.STRING, "Name");
		persister.persistProperty("child", "integer", DataType.INTEGER, 42);
		persister.commit();
		persister.begin();
		persister.persistProperty("child", "integer", DataType.NULL, null);
		persister.commit();
		loadWorkspace();
		assertEquals(0, receivedInt);
	}

	public void testConditionalPersistProperty() throws Exception {
		System.out.println("\n\nTesting Conditional Persist Property:");
		persister.begin();
		persister.persistObject(workspaceId, "ca.sqlpower.sqlobject.SQLColumn", "child", 0);
		persister.persistProperty("child", "name", DataType.STRING, "Chart Name");
		persister.persistProperty("child", "integer", DataType.INTEGER, 8);
		persister.commit();
		loadWorkspace();
		assertEquals("Chart Name", receivedString);
		assertEquals(8, receivedInt);
		
		persister.begin();
		persister.persistProperty("child", "name", DataType.STRING, "Chart Name", "Another Name");
		persister.persistProperty("child", "integer", DataType.INTEGER, 42);
		persister.commit();
		loadWorkspace();
		assertEquals("Another Name", receivedString);
		assertEquals(42, receivedInt);
		
		try {
			persister.begin();
			persister.persistProperty("child", "name", DataType.STRING, "Chart Name", "Yet Another Name");
			fail("Persister allowed property \"name\" to be changed without correct old value");
		} catch (SPPersistenceException e) {
			
		}
		try {
			persister.begin();
			persister.persistProperty("child", "integer", DataType.INTEGER, 99, 108);
			fail("Persister allowed property \"integer\" to be changed without correct old value");
		} catch (SPPersistenceException e) {
			
		}
	}

	public void testPersistReference() throws Exception {
		System.out.println("\n\nTesting Persist Reference:");
		persister.begin();
		persister.persistObject(workspaceId, "ca.sqlpower.sqlobject.SQLColumn", "child", 0);
		persister.persistProperty("child", "name", DataType.STRING, "Chart Name");
		persister.persistProperty("child", "reference", DataType.REFERENCE, "Other Wabit Object");
		persister.commit();
		loadWorkspace();
		assertEquals("Other Wabit Object", receivedReference);
	}

	public void testPersistStream() throws Exception {
		System.out.println("\n\nTesting Persist Stream:");
		persister.begin();
		persister.persistObject(workspaceId, "ca.sqlpower.testutil.SPObjectRoot", "child", 0);
		persister.persistProperty("child", "name", DataType.STRING, "Image Name");
		
		byte[] bytes = new byte[receivedData.length];
		Random random = new Random();
		random.nextBytes(bytes);
		
		persister.persistProperty("child", "image", DataType.PNG_IMG, new ByteArrayInputStream(bytes)); // Not really a PNG, but it's the only one that's a stream
		persister.commit();
		
		loadWorkspace();
		assertTrue(Arrays.equals(bytes, receivedData));
	}

	public void testRemoveObject() throws Exception {
		System.out.println("\n\nTesting Remove Object:");
		persister.begin();
		persister.persistObject(workspaceId, "ca.sqlpower.sqlobject.SQLColumn", "child", 0);
		persister.persistProperty("child", "name", DataType.STRING, "Chart Name");
		persister.persistObject(workspaceId, "ca.sqlpower.sqlobject.SQLColumn", "child2", 0);
		persister.persistProperty("child2", "name", DataType.STRING, "Chart 2 Name");
		persister.commit();
		persister.begin();
		persister.removeObject(workspaceId, "child");
		persister.commit();
		loadWorkspace();
		assertEquals(2, numObjects);
		assertEquals("Chart 2 Name", receivedString);
	}
	
	protected abstract void loadWorkspace() throws Exception;

	public void testRollback() throws Exception {
		System.out.println("\n\nTesting Rollback:");
		persister.begin();
		persister.persistObject(workspaceId, "ca.sqlpower.sqlobject.SQLColumn", "child", 0);
		persister.rollback();
		loadWorkspace();
		assertEquals(1, numObjects);
	}

}