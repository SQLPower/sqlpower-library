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

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * This class is used to convert DiffChunks to and from JSON-formatted strings.
 * It is simple because it only the name properties of the data in the chunks
 * (and physical name for SQLTable)
 *
 * XXX Add reflection to decode method so that it is not limited to SQLTable, SQLColumn, and SQLRelationship
 */
public class SimpleDiffChunkJSONConverter {
    
    public static final Logger logger = Logger.getLogger(SimpleDiffChunkJSONConverter.class);
    
    /**
     * Converts a list of DiffChunk to a JSONArray string.
     * @param chunks 
     * @throws JSONException 
     */
    public static String encode(List<DiffChunk<DiffInfo>> chunks) throws JSONException {
        
        JSONArray jsonArray = new JSONArray();
        for (DiffChunk<DiffInfo> chunk : chunks) {
            
            JSONObject object = new JSONObject();
            
            object.put("type", chunk.getType().toString());
            
            JSONArray changes = new JSONArray();
            for (PropertyChange change : chunk.getPropertyChanges()) {
                JSONObject json = new JSONObject();
                json.put("propertyName", change.getPropertyName());
                json.put("oldValue", change.getOldValue());
                json.put("newValue", change.getNewValue());
                changes.put(json);
            }
            object.put("changes", changes);
            
            JSONObject info = new JSONObject();
            info.put("dataType", chunk.getData().getDataType());
            info.put("name", chunk.getData().getName());
            info.put("depth", chunk.getData().getDepth());            
            object.put("info", info);                        
            
            jsonArray.put(object);
            
        }
        
        return jsonArray.toString();
        
    }
    
    /**
     * Converts a JSONArray string into a list of DiffChunk objects.
     * @param message
     * @throws JSONException 
     */
    public static List<DiffChunk<DiffInfo>> decode(String message) throws JSONException {
        
        List<DiffChunk<DiffInfo>> diffChunks = new ArrayList<DiffChunk<DiffInfo>>();
        logger.debug(message);
        JSONArray jsonArray = new JSONArray(message);
        logger.debug(jsonArray.toString());
        for (int i = 0; i < jsonArray.length(); i++) {
            
            JSONObject object = jsonArray.getJSONObject(i);            
            
            JSONObject jsonInfo = object.getJSONObject("info");
            DiffInfo info = new DiffInfo(jsonInfo.getString("dataType"), jsonInfo.getString("name"));
            info.setDepth(jsonInfo.getInt("depth"));                                   
            
            DiffType type = DiffType.valueOf(object.getString("type"));
            
            DiffChunk<DiffInfo> chunk = new DiffChunk<DiffInfo>(info, type);
            
            JSONArray changes = object.getJSONArray("changes");
            for (int j = 0; j < changes.length(); j++) {
                JSONObject change = changes.getJSONObject(j);                
                chunk.addPropertyChange(new PropertyChange(change.getString("propertyName"),
                        change.getString("oldValue"), change.getString("newValue")));                
            }            
            
            diffChunks.add(chunk);
        }
        
        return diffChunks;
        
    }

}
