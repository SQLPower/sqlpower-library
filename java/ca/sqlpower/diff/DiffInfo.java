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


/**
 * This class stores diff information about SQLObjects (or anything, for that matter)
 */
public class DiffInfo {
    
    /**
     * The simple name of the data type of the object
     */
    final private String dataType;
    
    /**
     * The name of the object
     * It will include
     */
    final private String name;
    
    /**
     * Keeps track of the depth of this object relative to other DiffChunks before it.
     * Used when there are multiple DiffChunks in a list, and they represent a tree.
     */
    private int depth;

    /**
     * @param dataType
     * @param name
     */
    public DiffInfo(String dataType, String name) {
        this.dataType = dataType;
        this.name = name;
        this.depth = 0;
    }

    public String getDataType() {
        return dataType;
    }

    public String getName() {
        return name;
    }
    
    public int getDepth() {
        return depth;
    }
    
    public void setDepth(int depth) {
        this.depth = depth;
    }
    
    public String getIndent() {
        String s = "";
        for (int i = 0; i < depth; i++) {
            s += "\t";
        }
        return s;
    }
    
    /**
     * Makes a string formatted as such: "{indent}{DataType} {name} "
     */
    public String toString() {       
        return getIndent() + dataType + " " + name;
    }

}
