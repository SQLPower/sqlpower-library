/*
 * Copyright (c) 2008, SQL Power Group Inc.
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

package ca.sqlpower.query;

/**
 * An enumeration of group (aggregate) functions we think SQL
 * databases will support.
 */
public enum SQLGroupFunction {
    GROUP_BY ("(Group By)"),
	SUM ("SUM"),
	MIN ("MIN"),
	MAX ("MAX"),
	AVG ("AVG"),
	COUNT ("COUNT");
	
	private final String groupingName;
    
    private SQLGroupFunction(String groupingName) {
        this.groupingName = groupingName;
    }
    
    public String getGroupingName() {
        return groupingName;
    }
    
    /**
     * This method will get a SQLGroupFunction object based on the grouping name.
     */
    public static SQLGroupFunction getGroupType(String groupingName) {
        for (SQLGroupFunction function : SQLGroupFunction.values()) {
            if (function.getGroupingName().equals(groupingName)) {
                return function;
            }
        }
        throw new IllegalArgumentException("Unknown name to group by " + groupingName);
    }
}
