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
import java.util.List;

import org.json.JSONObject;

/**
 * Class to receive and store JSON objects from persisters that communicate with MessageSenders.
 */
public class JsonMessageSender implements MessageSender<JSONObject> {

    List<JSONObject> jsonObjects;
    
    public JsonMessageSender() {
        jsonObjects = new ArrayList<JSONObject>();
    }
    
    public void clear() {
        jsonObjects = new ArrayList<JSONObject>();
    }

    public void flush() throws SPPersistenceException {
        // no-op
    }

    public void send(JSONObject content) throws SPPersistenceException {
        jsonObjects.add(content);
    }
    
    public List<JSONObject> getJsonObjects() {
        return jsonObjects;
    }

}
