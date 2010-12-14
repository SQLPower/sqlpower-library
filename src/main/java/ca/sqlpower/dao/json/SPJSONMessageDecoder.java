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

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;

import javax.annotation.Nonnull;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import ca.sqlpower.dao.MessageDecoder;
import ca.sqlpower.dao.SPPersistenceException;
import ca.sqlpower.dao.SPPersister;
import ca.sqlpower.dao.SPPersister.DataType;
import ca.sqlpower.dao.SPPersister.SPPersistMethod;

/**
 * An implementation of {@link MessageDecoder} that takes in a String that is
 * intended to be a JSON-formatted message, and constructs a JSONObject from it.
 * It then expects JSONObject key values that map to {@link SPPersister}
 * methods and their expected parameters. It then extracts this information from
 * the JSONObject and makes the appropriate method calls to an
 * {@link SPPersister} provided in the constructor.
 */
public class SPJSONMessageDecoder implements MessageDecoder<String> {

	private static final Logger logger = Logger
			.getLogger(SPJSONMessageDecoder.class);
	
	/**
	 * A {@link SPPersister} that the decoder will make method calls on
	 */
	private SPPersister persister;

	/**
	 * Creates an SPMessageDecoder with the given {@link SPPersister}. The
	 * messages that this class decodes will contain SPPersister method calls
	 * with their parameters. This decoder will use the messages to make method
	 * calls to the given SPPersister.
	 * 
	 * @param persister
	 *            The {@link SPPersister} that this decoder will make method
	 *            calls to
	 */
	public SPJSONMessageDecoder(@Nonnull SPPersister persister) {
		this.persister = persister;
	}

	/**
	 * Takes in a String, which is passed as a {@link JSONTokener}, where each
	 * token represents a single persister call.
	 * 
	 * @see #decode(JSONTokener)
	 */
	public void decode(@Nonnull String message) throws SPPersistenceException {
		decode(new JSONTokener(message));
	}

	/**
	 * Takes in a {@link JSONTokener} that contains persister calls. The tokener
	 * is used to parse each {@link JSONObject} token. Each JSONObject contains
	 * details for making a SPPersister method call. The parsing is done in this
	 * method in this way for performance reasons. Every time the next token is
	 * parsed, the corresponding persister call is made immediately after. Since
	 * the token is not used after parsing it, it will eventually be garbage
	 * collected.
	 * 
	 * It expects the following key-value pairs in each JSONObject message:
	 * <ul>
	 * <li>method - The String value of a {@link SPPersistMethod}. This is used
	 * to determine which {@link SPPersister} method to call.</li>
	 * <li>uuid - The UUID of the SPObject, if there is one, that the persist
	 * method call will act on. If there is none, it expects
	 * {@link JSONObject#NULL}</li>
	 * </ul>
	 * Other possible key-value pairs (depending on the intended method call)
	 * include:
	 * <ul>
	 * <li>parentUUID</li>
	 * <li>type</li>
	 * <li>newValue</li>
	 * <li>oldValue</li>
	 * <li>propertyName</li>
	 * </ul>
	 * See the method documentation of {@link SPPersister} for full details on
	 * the expected values
	 * <p>
	 */
	public void decode(JSONTokener tokener) throws SPPersistenceException {
		String uuid = null;
		JSONObject jsonObject = null;
		try {
			synchronized (persister) {
				// This code comes from JSONArray's constructor that takes in a
				// JSONTokener. The reason why a JSONArray is not used is because
				// we do not want to store all of the JSONObjects first before
				// persisting the calls. Instead, we want to make the persist calls
				// on the fly while parsing each token. By doing so, we allow
				// garbage collection on each JSONObject immediately after the
				// persist call is made.
				char c = tokener.nextClean();
				char q;
				if (c == '[') {
					q = ']';
				} else if (c == '(') {
					q = ')';
				} else {
					throw tokener.syntaxError("A JSONArray text must start with '['");
				}
				if (tokener.nextClean() == ']') {
					return;
				}
				tokener.back();

				int index = 0;

				while(true) {
					if (tokener.nextClean() == ',') {
						tokener.back();
						throw new JSONException("JSONArray[" + index + "] not found.");
					} else {
						tokener.back();
						Object nextValue = tokener.nextValue();

						if (nextValue instanceof JSONObject) {
							// Instead of storing the JSONObject in a List as
							// JSONArray does, simply make the persist call straight
							// from this object. Since it is not used after, it
							// will eventually be garbage collected.
							jsonObject = (JSONObject) nextValue;
							logger.debug("Decoding Message: " + jsonObject);
							uuid = jsonObject.getString("uuid");
							decode(jsonObject);
							index++;
						} else {
							throw new JSONException("JSONArray[" + index + "] is not a JSONObject.");
						}
					}
					c = tokener.nextClean();
					switch (c) {
					case ';':
					case ',':
						if (tokener.nextClean() == ']') {
							return;
						}
						tokener.back();
						break;
					case ']':
					case ')':
						if (q != c) {
							throw tokener.syntaxError("Expected a '" + new Character(q) + "'");
						}
						return;
					default:
						throw tokener.syntaxError("Expected a ',' or ']'");
					}
				}
			}
		} catch (JSONException e) {
			if (jsonObject != null) {
				logger.error("Error decoding JSONObject " + jsonObject);
			}
			throw new SPPersistenceException(uuid, e);
		}
	}

	/**
	 * Takes in a JSONArray of persister calls. The JSON message is expected to
	 * be a JSONArray of JSONObjects. Each JSONObject contains details for
	 * making a SPPersister method call.
	 * 
	 * It expects the following key-value pairs in each JSONObject message:
	 * <ul>
	 * <li>method - The String value of a {@link SPPersistMethod}. This is used
	 * to determine which {@link SPPersister} method to call.</li>
	 * <li>uuid - The UUID of the SPObject, if there is one, that the persist
	 * method call will act on. If there is none, it expects
	 * {@link JSONObject#NULL}</li>
	 * </ul>
	 * Other possible key-value pairs (depending on the intended method call)
	 * include:
	 * <ul>
	 * <li>parentUUID</li>
	 * <li>type</li>
	 * <li>newValue</li>
	 * <li>oldValue</li>
	 * <li>propertyName</li>
	 * </ul>
	 * See the method documentation of {@link SPPersister} for full details on
	 * the expected values
	 */
	public void decode(JSONArray json) throws SPPersistenceException {
		JSONObject jsonObject = null;
		String uuid = null;
		try {
			synchronized (persister) {
				for (int i=0; i < json.length(); i++) {
					jsonObject = json.getJSONObject(i);
					logger.debug("Decoding Message: " + jsonObject);
					uuid = jsonObject.getString("uuid");
					decode(jsonObject);
				}
			}
		} catch (JSONException e) {
			if (jsonObject != null) {
				logger.error("Error decoding JSONObject " + jsonObject);
			}
			throw new SPPersistenceException(uuid, e);
		}
	}

	private static Object getNullable(@Nonnull JSONObject jo, String propName) throws JSONException {
		final Object value = jo.get(propName);
		if (value == JSONObject.NULL) {
			return null;
		} else {
			return value;
		}
	}
	
	public static Object getWithType(@Nonnull JSONObject jo, DataType type, String propName) throws JSONException {
		if (getNullable(jo, propName) == null) return null;

		switch (type) {
		case BOOLEAN:
			return Boolean.valueOf(jo.getBoolean(propName));
		case DOUBLE:
			return Double.valueOf(jo.getDouble(propName));
		case INTEGER:	
			return Integer.valueOf(jo.getInt(propName));
		case LONG:
			return Long.valueOf(jo.getLong(propName));
		case SHORT:
			return Short.valueOf(jo.getShort(propName));
		case FLOAT:
			return Float.valueOf(jo.getFloat(propName));
		case PNG_IMG:
			getNullable(jo, propName);
			String base64Data = jo.getString(propName);
			byte[] decodedBytes;
			try {
				decodedBytes = Base64.decodeBase64(base64Data.getBytes("ascii"));
			} catch (UnsupportedEncodingException e) {
				throw new RuntimeException("ASCII should always be supported!", e);
			}
			return new ByteArrayInputStream(decodedBytes);
		case NULL:
		case STRING:
		case REFERENCE:
		default:
			return getNullable(jo, propName);
		}
	}

	/**
	 * Takes in a {@link JSONObject} that represents a single persister call.
	 * The {@link JSONObject} contains details for making a {@link SPPersister}
	 * method call.
	 * 
	 * It expects the following key-value pairs in the {@link JSONObject}
	 * message:
	 * <ul>
	 * <li>method - The String value of a {@link SPPersistMethod}. This is used
	 * to determine which {@link SPPersister} method to call.</li>
	 * <li>uuid - The UUID of the SPObject, if there is one, that the persist
	 * method call will act on. If there is none, it expects
	 * {@link JSONObject#NULL}</li>
	 * </ul>
	 * Other possible key-value pairs (depending on the intended method call)
	 * include:
	 * <ul>
	 * <li>parentUUID</li>
	 * <li>type</li>
	 * <li>newValue</li>
	 * <li>oldValue</li>
	 * <li>propertyName</li>
	 * </ul>
	 * See the method documentation of {@link SPPersister} for full details on
	 * the expected values
	 * 
	 * @param jsonObject
	 *            The {@link JSONObject} that represents the single persister
	 *            call.
	 * @throws SPPersistenceException
	 *             Thrown if a key-value pair in the {@link JSONObject} cannot
	 *             be retrieved.
	 */
	private void decode(JSONObject jsonObject) throws SPPersistenceException {
		String uuid = null;
		try {
			uuid = jsonObject.getString("uuid");
			SPPersistMethod method = SPPersistMethod.valueOf(jsonObject.getString("method"));
			String parentUUID;
			String propertyName;
			DataType propertyType;
			Object newValue;
			switch (method) {
			case begin:
				persister.begin();
				break;
			case commit:
				persister.commit();
				break;
			case persistObject:
				parentUUID = jsonObject.getString("parentUUID");
				if (parentUUID.equals("")) {
					//throw new SPPersistenceException(null, "Cannot persist object with null UUID, json is " + jsonObject);
				}
				String type = jsonObject.getString("type");
				int index = jsonObject.getInt("index");
				persister.persistObject(parentUUID, type, uuid, index);
				break;
			case changeProperty:
				propertyName = jsonObject.getString("propertyName");
				propertyType = DataType.valueOf(jsonObject.getString("type"));
				newValue = getWithType(jsonObject, propertyType, "newValue");
				Object oldValue = getWithType(jsonObject, propertyType, "oldValue");
				persister.persistProperty(uuid, propertyName,
						propertyType, oldValue, newValue);
				break;
			case persistProperty:
				propertyName = jsonObject.getString("propertyName");
				propertyType = DataType.valueOf(jsonObject.getString("type"));
				newValue = getWithType(jsonObject, propertyType, "newValue");
				if (newValue == null) logger.debug("newValue was null for propertyName " + propertyName);
				persister.persistProperty(uuid, propertyName,
						propertyType, newValue);
				break;
			case removeObject:			
				parentUUID = jsonObject.getString("parentUUID");
				if (parentUUID.equals("")) {
					throw new SPPersistenceException(null, "Cannot persist object with null UUID");
				}
				persister.removeObject(parentUUID, uuid);
				break;
			case rollback:
				persister.rollback();
				break;
			default:
				throw new SPPersistenceException(uuid,
						"Does not support SP persistence method " + method);
			}
		} catch (JSONException e) {
			if (jsonObject != null) {
				logger.error("Error decoding JSONObject " + jsonObject);
			}
			throw new SPPersistenceException(uuid, e);
		}
	}
}
