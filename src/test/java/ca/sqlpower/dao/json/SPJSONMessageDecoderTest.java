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

package ca.sqlpower.dao.json;

import junit.framework.TestCase;

import org.json.JSONArray;
import org.json.JSONObject;

import ca.sqlpower.dao.MessageDecoder;
import ca.sqlpower.dao.SPPersistenceException;
import ca.sqlpower.dao.SPPersister;
import ca.sqlpower.dao.SPPersister.DataType;
import ca.sqlpower.dao.SPPersister.SPPersistMethod;

public class SPJSONMessageDecoderTest extends TestCase {

	public void testDecodeBegin() throws Exception {
		SPPersister dummyPersister = new SPPersister() {
			public void rollback() {
				fail("Expected to call begin() but instead called rollback()");
			}
			
			public void removeObject(String parentUUID, String uuid)
					throws SPPersistenceException {
				fail("Expected to call begin() but instead called removeObject()");			
			}
			
			public void persistProperty(String uuid, String propertyName,
					DataType propertyType, Object newValue)
					throws SPPersistenceException {
				fail("Expected to call begin() but instead called persistProperty()");
			}
			
			public void persistProperty(String uuid, String propertyName,
					DataType propertyType, Object oldValue, Object newValue)
					throws SPPersistenceException {
				fail("Expected to call begin() but instead called persistProperty()");
				
			}
			
			public void persistObject(String parentUUID, String type, String uuid, int index)
					throws SPPersistenceException {
				fail("Expected to call begin() but instead called persistObject()");
			}
			
			public void commit() throws SPPersistenceException {
				fail("Expected to call begin() but instead called commit()");				
			}
			
			public void begin() throws SPPersistenceException {
				// We expect this method to get called.
			}
		};

		JSONObject json = new JSONObject();
		json.put(SPJSONPersister.METHOD, SPPersistMethod.begin.getCode());
		json.put("uuid", JSONObject.NULL);
		JSONArray messages = new JSONArray();
		messages.put(json);
		
		MessageDecoder<String> decoder = new SPJSONMessageDecoder(dummyPersister);
		decoder.decode(messages.toString());
	}
	
	public void testDecodeCommit() throws Exception {
		SPPersister dummyPersister = new SPPersister() {
			public void rollback() {
				fail("Expected to call commit() but instead called rollback()");
			}
			
			public void removeObject(String parentUUID, String uuid)
					throws SPPersistenceException {
				fail("Expected to call commit() but instead called removeObject()");			
			}
			
			public void persistProperty(String uuid, String propertyName,
					DataType propertyType, Object newValue)
					throws SPPersistenceException {
				fail("Expected to call commit() but instead called persistProperty()");
			}
			
			public void persistProperty(String uuid, String propertyName,
					DataType propertyType, Object oldValue, Object newValue)
					throws SPPersistenceException {
				fail("Expected to call commit() but instead called persistProperty()");
				
			}
			
			public void persistObject(String parentUUID, String type, String uuid, int index)
					throws SPPersistenceException {
				fail("Expected to call commit() but instead called persistObject()");
			}
			
			public void commit() throws SPPersistenceException {
				// We expect this method to get called.			
			}
			
			public void begin() throws SPPersistenceException {
				fail("Expected to call commit() but instead called begin()");
			}
		};

		JSONObject json = new JSONObject();
		json.put(SPJSONPersister.METHOD, SPPersistMethod.commit.getCode());
		json.put("uuid", JSONObject.NULL);
		JSONArray messages = new JSONArray();
		messages.put(json);
		
		MessageDecoder<String> decoder = new SPJSONMessageDecoder(dummyPersister);
		decoder.decode(messages.toString());
	}

	public void testDecodePersistObject() throws Exception {
		SPPersister dummyPersister = new SPPersister() {
			public void rollback() {
				fail("Expected to call persistObject() but instead called rollback()");
			}
			
			public void removeObject(String parentUUID, String uuid)
					throws SPPersistenceException {
				fail("Expected to call persistObject() but instead called removeObject()");			
			}
			
			public void persistProperty(String uuid, String propertyName,
					DataType propertyType, Object newValue)
					throws SPPersistenceException {
				fail("Expected to call persistObject() but instead called persistProperty()");
			}
			
			public void persistProperty(String uuid, String propertyName,
					DataType propertyType, Object oldValue, Object newValue)
					throws SPPersistenceException {
				fail("Expected to call persistObject() but instead called persistProperty()");
				
			}
			
			public void persistObject(String parentUUID, String type, String uuid, int index)
					throws SPPersistenceException {
				// We expect this method to get called.			
			}
			
			public void commit() throws SPPersistenceException {
				fail("Expected to call persistObject() but instead called commit()");
			}
			
			public void begin() throws SPPersistenceException {
				fail("Expected to call persistObject() but instead called begin()");
			}
		};

		JSONObject json = new JSONObject();
		json.put(SPJSONPersister.METHOD, SPPersistMethod.persistObject.getCode());
		json.put("uuid", "uuid");
		json.put(SPJSONPersister.PARENT_UUID, "parent");
		json.put("type", "type");
		json.put("index", 0);
		JSONArray messages = new JSONArray();
		messages.put(json);
		
		MessageDecoder<String> decoder = new SPJSONMessageDecoder(dummyPersister);
		decoder.decode(messages.toString());
	}
	
	public void testDecodeChangeProperty() throws Exception {
		SPPersister dummyPersister = new SPPersister() {
			public void rollback() {
				fail("Expected to call persistProperty() but instead called rollback()");
			}
			
			public void removeObject(String parentUUID, String uuid)
					throws SPPersistenceException {
				fail("Expected to call persistProperty() but instead called removeObject()");			
			}
			
			public void persistProperty(String uuid, String propertyName,
					DataType propertyType, Object newValue)
					throws SPPersistenceException {
				fail("Expected to call persistProperty() with oldValue but instead called persistProperty() without oldValue");
			}
			
			public void persistProperty(String uuid, String propertyName,
					DataType propertyType, Object oldValue, Object newValue)
					throws SPPersistenceException {
				// We expect this method to get called.			
			}
			
			public void persistObject(String parentUUID, String type, String uuid, int index)
					throws SPPersistenceException {
				fail("Expected to call persistProperty() but instead called persistObject()");
			}
			
			public void commit() throws SPPersistenceException {
				fail("Expected to call persistProperty() but instead called commit()");
			}
			
			public void begin() throws SPPersistenceException {
				fail("Expected to call persistProperty() but instead called begin()");
			}
		};

		JSONObject json = new JSONObject();
		json.put(SPJSONPersister.METHOD, SPPersistMethod.changeProperty.getCode());
		json.put("uuid", "uuid");
		json.put("type", DataType.BOOLEAN);
		json.put(SPJSONPersister.PROPERTY_NAME, "property");
		json.put(SPJSONPersister.NEW_VALUE, true);
		json.put("oldValue", false);
		JSONArray messages = new JSONArray();
		messages.put(json);
		
		MessageDecoder<String> decoder = new SPJSONMessageDecoder(dummyPersister);
		decoder.decode(messages.toString());
	}
	
	public void testDecodePersistProperty() throws Exception {
		SPPersister dummyPersister = new SPPersister() {
			public void rollback() {
				fail("Expected to call persistProperty() but instead called rollback()");
			}
			
			public void removeObject(String parentUUID, String uuid)
					throws SPPersistenceException {
				fail("Expected to call persistProperty() but instead called removeObject()");			
			}
			
			public void persistProperty(String uuid, String propertyName,
					DataType propertyType, Object newValue)
					throws SPPersistenceException {
				// We expect this method to get called.			
			}
			
			public void persistProperty(String uuid, String propertyName,
					DataType propertyType, Object oldValue, Object newValue)
					throws SPPersistenceException {
				fail("Expected to call persistProperty() without oldValue but instead called persistProperty() with oldValue");
			}
			
			public void persistObject(String parentUUID, String type, String uuid, int index)
					throws SPPersistenceException {
				fail("Expected to call persistProperty() but instead called persistObject()");
			}
			
			public void commit() throws SPPersistenceException {
				fail("Expected to call persistProperty() but instead called commit()");
			}
			
			public void begin() throws SPPersistenceException {
				fail("Expected to call persistProperty() but instead called begin()");
			}
		};

		JSONObject json = new JSONObject();
		json.put(SPJSONPersister.METHOD, SPPersistMethod.persistProperty.getCode());
		json.put("uuid", "uuid");
		json.put("type", DataType.BOOLEAN);
		json.put(SPJSONPersister.PROPERTY_NAME, "property");
		json.put(SPJSONPersister.NEW_VALUE, true);
		JSONArray messages = new JSONArray();
		messages.put(json);
		
		MessageDecoder<String> decoder = new SPJSONMessageDecoder(dummyPersister);
		decoder.decode(messages.toString());
	}
	
	public void testDecodeRemoveObject() throws Exception {
		SPPersister dummyPersister = new SPPersister() {
			public void rollback() {
				fail("Expected to call removeObject() but instead called rollback()");
			}
			
			public void removeObject(String parentUUID, String uuid)
					throws SPPersistenceException {
				// We expect this method to get called.			
			}
			
			public void persistProperty(String uuid, String propertyName,
					DataType propertyType, Object newValue)
					throws SPPersistenceException {
				fail("Expected to call removeObject() but instead called persistProperty()");
			}
			
			public void persistProperty(String uuid, String propertyName,
					DataType propertyType, Object oldValue, Object newValue)
					throws SPPersistenceException {
				fail("Expected to call removeObject() but instead called persistProperty()");
			}
			
			public void persistObject(String parentUUID, String type, String uuid, int index)
					throws SPPersistenceException {
				fail("Expected to call removeObject() but instead called removeObject()");			
			}
			
			public void commit() throws SPPersistenceException {
				fail("Expected to call removeObject() but instead called commit()");
			}
			
			public void begin() throws SPPersistenceException {
				fail("Expected to call removeObject() but instead called begin()");
			}
		};

		JSONObject json = new JSONObject();
		json.put(SPJSONPersister.METHOD, SPPersistMethod.removeObject.getCode());
		json.put("uuid", "uuid");
		json.put(SPJSONPersister.PARENT_UUID, "parent");
		JSONArray messages = new JSONArray();
		messages.put(json);
		
		MessageDecoder<String> decoder = new SPJSONMessageDecoder(dummyPersister);
		decoder.decode(messages.toString());
	}
	
	public void testDecodeRollback() throws Exception {
		SPPersister dummyPersister = new SPPersister() {
			public void rollback() {
				// We expect this method to get called.
			}
			
			public void removeObject(String parentUUID, String uuid)
					throws SPPersistenceException {
				fail("Expected to call rollback() but instead called removeObject()");			
			}
			
			public void persistProperty(String uuid, String propertyName,
					DataType propertyType, Object newValue)
					throws SPPersistenceException {
				fail("Expected to call rollback() but instead called persistProperty()");
			}
			
			public void persistProperty(String uuid, String propertyName,
					DataType propertyType, Object oldValue, Object newValue)
					throws SPPersistenceException {
				fail("Expected to call rollback() but instead called persistProperty()");
				
			}
			
			public void persistObject(String parentUUID, String type, String uuid, int index)
					throws SPPersistenceException {
				fail("Expected to call rollback() but instead called persistObject()");
			}
			
			public void commit() throws SPPersistenceException {
				fail("Expected to call rollback() but instead called commit()");				
			}
			
			public void begin() throws SPPersistenceException {
				fail("Expected to call rollback() but instead called begin()");
			}
		};

		JSONObject json = new JSONObject();
		json.put(SPJSONPersister.METHOD, SPPersistMethod.rollback.getCode());
		json.put("uuid", JSONObject.NULL);
		JSONArray messages = new JSONArray();
		messages.put(json);
		
		MessageDecoder<String> decoder = new SPJSONMessageDecoder(dummyPersister);
		decoder.decode(messages.toString());
	}
}
