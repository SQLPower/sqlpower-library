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

package ca.sqlpower.swingui;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

import ca.sqlpower.object.AbstractSPObject;
import ca.sqlpower.object.SPObject;
import ca.sqlpower.sqlobject.SQLColumn;
import ca.sqlpower.sqlobject.SQLIndex;
import ca.sqlpower.sqlobject.SQLObject;
import ca.sqlpower.sqlobject.SQLObjectException;
import ca.sqlpower.sqlobject.SQLRelationship;
import ca.sqlpower.sqlobject.SQLTable;

/**
 * A visual node for the tree that groups children of the table together.
 */

public class FolderNode extends AbstractSPObject {

    protected final SPObject parentTable;
    protected final Class<? extends SPObject> containingChildType;
    protected final Callable<Boolean> isPopulatedRunnable;

    /**
     * @param parentTable
     *            The SQLTable that this folder is to appear under
     * @param containingChildType
     *            The type of child of the SQLTable this folder is to
     *            contain. Must be a valid type of child in the table.
     */
    public FolderNode(SPObject parentTable, Class<? extends SPObject> containingChildType,
            Callable<Boolean> isPopulatedRunnable) {
        this.parentTable = parentTable;
        if (!parentTable.getAllowedChildTypes().contains(containingChildType)) 
            throw new IllegalArgumentException(containingChildType + " is not a valid child type of " + parentTable);
        this.containingChildType = containingChildType;
        this.isPopulatedRunnable = isPopulatedRunnable;
    }
    
    public List<? extends SPObject> getChildren() {
        return parentTable.getChildren(containingChildType);
    }
    
    public Class<? extends SPObject> getContainingChildType() {
        return containingChildType;
    }
    
    @Override
    public SPObject getParent() {
        return parentTable;
    }

    public List<? extends SPObject> getChildrenWithoutPopulating() {
        if (parentTable instanceof SQLTable) {
        	return ((SQLTable) parentTable).getChildrenWithoutPopulating(containingChildType);
        } else {
        	return getChildren();
        }
    }

    public String getShortDisplayName() {
        return getName();
    }
    
    @Override
    public String toString() {
        return getShortDisplayName();
    }

    protected void populateImpl() throws SQLObjectException {
        //do nothing
    }

    @Override
    protected boolean removeChildImpl(SPObject child) {
        throw new IllegalStateException("Cannot remove children from a folder, " +
        		"remove them from the table the folder is contained by.");
    }

    public List<Class<? extends SPObject>> getAllowedChildTypes() {
        return Collections.<Class<? extends SPObject>>singletonList(containingChildType);
    }

    public List<? extends SPObject> getDependencies() {
        return Collections.emptyList();
    }

    public void removeDependency(SPObject dependency) {
        //do nothing
    }
    
    public boolean isPopulated() {
        try {
            return isPopulatedRunnable.call().booleanValue();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    
}
