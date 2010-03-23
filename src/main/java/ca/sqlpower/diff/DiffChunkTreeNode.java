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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ca.sqlpower.sqlobject.SQLObjectException;

/**
 * This class is used to build a tree of DiffChunks, based on uuid/parent mapping.
 * It is used for the purpose of creating an ordered list of DiffChunks.
 */
public class DiffChunkTreeNode {

    private String uuid;
    private DiffChunk<DiffInfo> chunk;
    private HashMap<String, DiffChunkTreeNode> children;

    public DiffChunkTreeNode(String uuid, DiffChunk<DiffInfo> chunk) {
        this.uuid = uuid;
        this.chunk = chunk;
        children = new HashMap<String, DiffChunkTreeNode>();
    }

    public void addChild(String uuid, DiffChunk<DiffInfo> chunk) {
        children.put(uuid, new DiffChunkTreeNode(uuid, chunk));
    }

    /**
     * This method will add the given DiffChunk to this node if this 
     * node is the one specified by nodeUUID. Otherwise, it will recursively
     * look for the descendant with the specified nodeUUID and the chunk will be
     * added there.
     * 
     * @param childUUID
     * @param chunk
     * @param nodeUUID
     * @return Whether the object was added successfully somewhere within the tree
     */
    public boolean addChildToNode(String childUUID, DiffChunk<DiffInfo> chunk, String nodeUUID) {
        if (this.uuid.equals(nodeUUID)) {
            addChild(childUUID, chunk);
            return true;
        } else {
            Iterator<String> keys = children.keySet().iterator();
            while (keys.hasNext()) {
                if (children.get(keys.next()).addChildToNode(childUUID, chunk, nodeUUID)) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Goes through the tree recursively, building a list of all the DiffChunks contained within.
     * The list is in the order of {first parent}, {children of first parent}, {second parent}, {children of second parent}, etc.
     * Sets the depth of all the DiffInfo items as it builds the list. 
     */
    public List<DiffChunk<DiffInfo>> buildOrderedList() {
        List<DiffChunk<DiffInfo>> diffChunks = new ArrayList<DiffChunk<DiffInfo>>();
        buildOrderedList(diffChunks, -1);
        return diffChunks;
    }
    
    private void buildOrderedList(List<DiffChunk<DiffInfo>> diffChunks, int depth) {
        if (chunk != null) {
            chunk.getData().setDepth(depth);
            diffChunks.add(chunk);          
        }
        Iterator<String> keys = children.keySet().iterator();            
        while (keys.hasNext()) {
            children.get(keys.next()).buildOrderedList(diffChunks, depth + 1);
        }               
    }
    
    /**
     * Constructs a tree of the given DiffChunks using the
     * given parentMap to know the structure of the tree.
     * This instance of DiffChunkTreeNode will act as the root,
     * having all passed DiffChunks added below it, somewhere.
     * 
     * @param diffChunks 
     * @param parentMap
     * @throws SQLObjectException 
     */
    public void constructTree(Map<String, DiffChunk<DiffInfo>> diffChunks, Map<String, String> parentMap) throws SQLObjectException {
        
        Set<String> uuidsInTree = new HashSet<String>();
        uuidsInTree.add(this.uuid);
        
        // Loop through all the uuids of the objects that were added/removed/changed.
        // Add each to the tree by adding all their ancestors if they are not already added, too.
        Iterator<String> uuids = parentMap.keySet().iterator();
        while (uuids.hasNext()) {
            String uuid = uuids.next();
            
            // Construct a list of ancestors that are not in the tree, but need to be.
            List<String> addToTree = new ArrayList<String>();            
            
            // Go through the ancestors of the object we intend to add
            // until we reach the root, or an object that is already in the tree.
            String nextUUID = uuid;
            while (!nextUUID.equals(this.uuid) && !uuidsInTree.contains(nextUUID)) {
                addToTree.add(nextUUID);
                nextUUID = parentMap.get(nextUUID);                
            }
            
            // Add all the objects, starting from the oldest ancestor.
            for (int i = addToTree.size() - 1; i >= 0; i--) {
                String u = addToTree.get(i);
                if (this.addChildToNode(u, diffChunks.get(u), parentMap.get(u))) {;
                    uuidsInTree.add(u);
                } else {
                    throw new SQLObjectException("Error adding DiffChunks to tree");
                }
            }       
        } 
    }

}
