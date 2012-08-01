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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import ca.sqlpower.dao.MessageSender;
import ca.sqlpower.dao.SPPersistenceException;
import ca.sqlpower.dao.SPPersister;
import ca.sqlpower.util.SQLPowerUtils;

/**
 * A {@link SPPersister} implementation that serializes
 * {@link SPPersister} method calls as {@link JSONObject}s and transmits them
 * to a destination using a {@link MessageSender}. This allows these method
 * calls to be transmitted to other systems, typically (but not necessarily)
 * over a network connection.
 */
public class SPJSONPersister implements SPPersister {

	private static final Logger logger = Logger
			.getLogger(SPJSONPersister.class);
	
	/**
	 * A count of transactions, mainly to keep track of nested transactions.
	 */
	private int transactionCount = 0;
	
	/**
	 * A MessagePasser object that is responsible for transmitting the
	 * JSONObject contents.
	 */
	private final MessageSender<JSONObject> messageSender;

	private final List<JSONObject> messageBuffer;
	
	/**
	 * Create a {@link SPJSONPersister} that uses the given
	 * {@link MessageSender} to transmit the JSON content
	 */
	public SPJSONPersister(MessageSender<JSONObject> messageSender) {
		this.messageSender = messageSender;
		this.messageBuffer = new ArrayList<JSONObject>();
	}
	
	public void begin() throws SPPersistenceException{
		JSONObject jsonObject = new JSONObject();
		try {
			jsonObject.put("method", SPPersistMethod.begin);
			// Need to put this in or anything calling get on the key "uuid"
			// will throw a JSONException
			jsonObject.put("uuid", JSONObject.NULL);
		} catch (JSONException e) {
			logger.error("Exception encountered while building JSON message. Rollback initiated.",e);
			rollback();
			throw new SPPersistenceException(null, e);
		}
		logger.debug(jsonObject);
		messageBuffer.add(jsonObject);
		transactionCount++;
	}

	public void commit() throws SPPersistenceException {
		if (transactionCount == 0) {
			throw new SPPersistenceException(null, "Commit attempted while not in a transaction");
		}
		JSONObject jsonObject = new JSONObject();
		try {
			jsonObject.put("method", SPPersistMethod.commit);
			// Need to put this in or anything calling get on the key "uuid"
			// will throw a JSONException
			jsonObject.put("uuid", JSONObject.NULL);
		} catch (JSONException e) {
			logger.error("Exception encountered while building JSON message. Rollback initiated.",e);
			rollback();
			throw new SPPersistenceException(null, e);
		}
		try {
			logger.debug(jsonObject);
			messageBuffer.add(jsonObject);
			if (transactionCount == 1) {
				for (JSONObject obj: messageBuffer) {
					messageSender.send(obj);
				}
				messageBuffer.clear();
				messageSender.flush();
				transactionCount = 0;
			} else {
				transactionCount--;
			}
		} catch (Throwable t) {
			logger.error("Exception encountered while building JSON message. Rollback initiated.",t);
			messageBuffer.clear();
			messageSender.clear();
			transactionCount = 0;
			rollback();
			if (t instanceof SPPersistenceException) {
				throw (SPPersistenceException) t;
			} else if (t instanceof RuntimeException) {
				throw (RuntimeException) t;
			} else {
				throw new RuntimeException(t);
			}
		}
	}

	/**
	 * XXX Any JSON persisters that extend this class must override this method
	 * before calling super.{@link #persistObject(String, String, String, int)}
	 * if null parentUUIDs need to be handled correctly.
	 */
	public void persistObject(String parentUUID, String type, String uuid, int index)
			throws SPPersistenceException {
		
		// XXX Not handling null parentUUIDs here. Extending classes should handle them before calling super.persistObject 
		
		if (transactionCount == 0) {
			throw new SPPersistenceException("Operation attempted while not in a transaction.");
		}
		JSONObject jsonObject = new JSONObject();
		try {
			jsonObject.put("method", SPPersistMethod.persistObject.toString());
			if (parentUUID == null) {
				jsonObject.put("parentUUID", JSONObject.NULL);
			} else {
				jsonObject.put("parentUUID", parentUUID);
			}
			jsonObject.put("type", type);
			jsonObject.put("uuid", uuid);
			jsonObject.put("index", index);
		} catch (JSONException e) {
			logger.error(e);
			rollback();
			throw new SPPersistenceException(uuid, e);
		}
		logger.debug(jsonObject);
		messageBuffer.add(jsonObject);
	}

	public void persistProperty(String uuid, String propertyName, DataType type,
			Object oldValue, Object newValue) throws SPPersistenceException {
		if (transactionCount == 0) {
			throw new SPPersistenceException("Operation attempted while not in a transaction.");
		}
		JSONObject jsonObject = new JSONObject();
		try {
			jsonObject.put("method", SPPersistMethod.changeProperty);
			jsonObject.put("uuid", uuid);
			jsonObject.put("propertyName", propertyName);
			jsonObject.put("type", type.toString());
			setValueInJSONObject(jsonObject, "oldValue", type, oldValue);
			setValueInJSONObject(jsonObject, "newValue", type, newValue);
		} catch (JSONException e) {
			logger.error(e);
			rollback();
			throw new SPPersistenceException(uuid, e);
		} catch (IOException e) {
			logger.error(e);
			rollback();
			throw new SPPersistenceException(uuid, e);
		}
		logger.debug(jsonObject);
		messageBuffer.add(jsonObject);
	}
	
	public void persistProperty(String uuid, String propertyName, DataType type, Object newValue) throws SPPersistenceException {
		if (transactionCount == 0) {
			throw new SPPersistenceException("Operation attempted while not in a transaction.");
		}
		JSONObject jsonObject = new JSONObject();
		try {
			jsonObject.put("method", SPPersistMethod.persistProperty);
			jsonObject.put("uuid", uuid);
			jsonObject.put("propertyName", propertyName);
			jsonObject.put("type", type.toString());
			setValueInJSONObject(jsonObject, "newValue", type, newValue);
		} catch (JSONException e) {
			logger.error(e);
			rollback();
			throw new SPPersistenceException(uuid, e);
		} catch (IOException e) {
			logger.error(e);
			rollback();
			throw new SPPersistenceException(uuid, e);
		}
		logger.debug(jsonObject);
		messageBuffer.add(jsonObject);
	}

	/**
	 * Sets the named property of the given JSON object to the given value. This
	 * is a nontrivial operation because JSON nulls are special, and so are
	 * values of type PNG_IMAGE.
	 * 
	 * @param jsonObject
	 *            The object whose property to set.
	 * @param jsonPropName
	 *            The property name to set on jsonObject.
	 * @param type
	 *            the SPPersister framework datatype for value.
	 * @param value
	 *            The actual value to set. Values for all possible DataTypes are
	 *            properly converted to a JavaScript data type before being set.
	 */
	private void setValueInJSONObject(JSONObject jsonObject, String jsonPropName, DataType type, Object value)
	throws IOException, JSONException, UnsupportedEncodingException {
		if (type == DataType.PNG_IMG && value != null) {
			InputStream in = (InputStream) value;
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			SQLPowerUtils.copyStream(in, out);
			byte[] bytes = out.toByteArray();
			byte[] base64Bytes = Base64.encodeBase64(bytes);
			jsonObject.put(jsonPropName, new String(base64Bytes, "ascii"));
		} else {
			jsonObject.put(jsonPropName, value == null ? JSONObject.NULL : value);
		}
	};
	
	public void removeObject(String parentUUID, String uuid)
			throws SPPersistenceException {
		if (transactionCount == 0) {
			throw new SPPersistenceException("Operation attempted while not in a transaction.");
		}
		JSONObject jsonObject = new JSONObject();
		try {
			jsonObject.put("method", SPPersistMethod.removeObject);
			jsonObject.put("parentUUID", parentUUID);
			jsonObject.put("uuid", uuid);
		} catch (JSONException e) {
			logger.error(e);
			rollback();
			throw new SPPersistenceException(uuid, e);
		}
		logger.debug(jsonObject);
		messageBuffer.add(jsonObject);
	}
	
	public void rollback() {
		messageBuffer.clear();
		messageSender.clear();
		transactionCount = 0;
	}
	
	public MessageSender<JSONObject> getMessageSender() {
		return messageSender;
	}
}
