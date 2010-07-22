/*
 * Copyright (c) 2008, SQL Power Group Inc.
 *
 * This file is part of Power*Architect.
 *
 * Power*Architect is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * Power*Architect is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>. 
 */
package ca.sqlpower.sqlobject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ca.sqlpower.object.SPObject;
import ca.sqlpower.util.RunnableDispatcher;
import ca.sqlpower.util.StubWorkspaceContainer;
import ca.sqlpower.util.WorkspaceContainer;

/**
 * The StubSQLObject is a general-purpose SQLObject that you can use for testing
 * the Architect.  You might need to subclass it, or you might need to enhance
 * it directly.  Which is better is a judgement call!
 */
public class StubSQLObject extends SQLObject {
	
	private List<SQLObject> children = new ArrayList<SQLObject>();
	
	private StubWorkspaceContainer workspaceContainer = new StubWorkspaceContainer();

    /**
     * Keeps track of how many times populate() has been called.
     */
    private int populateCount = 0;
    
    public StubSQLObject() {
    }
    
    @Override
    public SQLObject getParent() {
        return null;
    }

    @Override
	public void setParent(SPObject parent) {
        // no op
    }

    @Override
    protected void populateImpl() throws SQLObjectException {
        populateCount++;
    }

    @Override
    public String getShortDisplayName() {
        return null;
    }

    // ======= non-SQLObject methods below this line ==========
    
    public int getPopulateCount() {
        return populateCount;
    }

	@Override
	public List<? extends SQLObject> getChildrenWithoutPopulating() {
		return Collections.unmodifiableList(children);
	}

	@Override
	protected boolean removeChildImpl(SPObject child) {
		int index = children.indexOf(child);
		if (index != -1) {
			children.remove(index);
			child.setParent(null);
			fireChildRemoved(child.getClass(), child, index);
			return true;
		}
		return false;
	}

	public List<? extends SPObject> getDependencies() {
		return Collections.emptyList();
	}

	public void removeDependency(SPObject dependency) {
		for (SQLObject child : children) {
			child.removeDependency(dependency);
		}
	}
	
	@Override
	protected void addChildImpl(SPObject child, int index) {
		children.add(index, (SQLObject) child);
		child.setParent(this);
		fireChildAdded(child.getClass(), child, index);
	}
	
	@Override
	public WorkspaceContainer getWorkspaceContainer() {
		return workspaceContainer;
	}
	
	@Override
	public RunnableDispatcher getRunnableDispatcher() {
		return workspaceContainer;
	}
	
	public List<Class<? extends SPObject>> getAllowedChildTypes() {
		return Collections.<Class<? extends SPObject>>singletonList(SQLObject.class);
	}
}