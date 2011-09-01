/*
 * Copyright (c) 2010, SQL Power Group Inc.
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

package ca.sqlpower.dao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Class to receive and store JSON objects from persisters that communicate with MessageSenders.
 */
public class JsonMessageSender implements MessageSender<JSONObject> {

    private final List<JSONObject> jsonObjects;
    
    public JsonMessageSender() {
        jsonObjects = new ArrayList<JSONObject>();
    }
    
    public synchronized void clear() {
        jsonObjects.clear();
    }

    public synchronized void flush() throws SPPersistenceException {
        // no-op
    }

    public synchronized void send(JSONObject content) throws SPPersistenceException {
        jsonObjects.add(content);
    }
    
    public synchronized List<JSONObject> getJsonObjects() {
        return Collections.unmodifiableList(jsonObjects);
    }
    
    public synchronized JSONArray getJsonArray() {
        JSONArray jsonArray = new JSONArray();
        for (JSONObject object : jsonObjects) {
            jsonArray.put(object);
        }
        return jsonArray;
    }
    
    public synchronized JSONArray getAndClear() {
    	JSONArray jsonArray = getJsonArray();
    	clear();
    	return jsonArray;
    }

    public synchronized String getJsonString() {
        return getJsonArray().toString();
    }
    
}
