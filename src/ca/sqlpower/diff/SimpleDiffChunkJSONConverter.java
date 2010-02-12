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

package ca.sqlpower.diff;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ca.sqlpower.sqlobject.SQLColumn;
import ca.sqlpower.sqlobject.SQLObject;
import ca.sqlpower.sqlobject.SQLRelationship;
import ca.sqlpower.sqlobject.SQLTable;

/**
 * This class is used to convert DiffChunks to and from JSON-formatted strings.
 * It is simple because it only the name properties of the data in the chunks
 * (and physical name for SQLTable)
 *
 * XXX Add reflection to decode method so that it is not limited to SQLTable, SQLColumn, and SQLRelationship
 */
public class SimpleDiffChunkJSONConverter {
    
    /**
     * Converts a list of DiffChunk to a JSONArray string.
     * @param chunks 
     * @throws JSONException 
     */
    public static String encode(List<DiffChunk<SQLObject>> chunks) throws JSONException {
        
        JSONArray jsonArray = new JSONArray();
        for (DiffChunk<SQLObject> chunk : chunks) {
            
            JSONObject object = new JSONObject();            
            object.put("dataType", chunk.getData().getClass().getName());
            JSONObject data = new JSONObject();
            data.put("name", chunk.getData().getName());
            if (chunk.getData() instanceof SQLTable) {
                data.put("physicalName", chunk.getData().getPhysicalName());
            }
            object.put("data", data);
            object.put("type", chunk.getType());
            jsonArray.put(object);
            
        }
        
        return jsonArray.toString();
        
    }
    
    /**
     * Converts a JSONArray string into a list of DiffChunk objects.
     * @param message
     * @throws JSONException 
     */
    public static List<DiffChunk<SQLObject>> decode(String message) throws JSONException {
        
        List<DiffChunk<SQLObject>> diffChunks = new ArrayList<DiffChunk<SQLObject>>();
        JSONArray jsonArray = new JSONArray(message);
        for (int i = 0; i < jsonArray.length(); i++) {
            
            JSONObject object = jsonArray.getJSONObject(i);            
            
            DiffType type = (DiffType) object.get("type");
            
            JSONObject jsonData = object.getJSONObject("data");
            SQLObject data = getImplementedType(jsonData.getString("dataType"));
            data.setName(jsonData.getString("name"));
            if (data instanceof SQLTable) {
                data.setPhysicalName(jsonData.getString("physicalName"));
            }
            
            diffChunks.add(new DiffChunk<SQLObject>(data, type));
        }
        
        return diffChunks;
        
    }
    
    /**
     * XXX Replace the necessity of this class with reflection
     * Simple method to get a new object that is one of the SQLObject types that are implemented.
     * 
     * @param typeName The name of the type wanted. SQLTable, SQLColumn, and SQLRelationship are supported.
     * @return A new object of the specified type, or null if it is not a supported type.
     */
    private static SQLObject getImplementedType(String typeName) {
        if (typeName == null) {
            return null;
        } else if (typeName.equals("SQLTable")) {
            return new SQLTable();            
        } else if (typeName.equals("SQLColumn")) {
            return new SQLColumn();
        } else if(typeName.equals("SQLRelationship")) {
            return new SQLRelationship();
        } else {
            return null;
        }
    }

}
