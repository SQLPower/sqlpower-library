/*
 * Copyright (c) 2009, SQL Power Group Inc.
 *
 * This file is part of Wabit.
 *
 * Wabit is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * Wabit is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>. 
 */

package ca.sqlpower.enterprise.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.security.GrantedAuthority;

import ca.sqlpower.object.AbstractSPObject;
import ca.sqlpower.object.SPObject;

public class Group extends AbstractSPObject implements GrantedAuthority {

    private final List<Grant> grants = new ArrayList<Grant>();
    private final List<GroupMember> members = new ArrayList<GroupMember>();

    public Group(String name) {
    	setName(name);
    }
    
    @Override
    protected boolean removeChildImpl(SPObject child) {
        if (child instanceof Grant) {
            return removeGrant((Grant)child);
        } else if (child instanceof GroupMember) {
            return removeMember((GroupMember)child);
        } else {
            return false;
        }
    }

    public boolean allowsChildren() {
        return true;
    }

    public int childPositionOffset(Class<? extends SPObject> childType) {
    	int offset = 0;
        if (GroupMember.class.isAssignableFrom(childType)) {
        	return offset;
        } else {
        	offset += members.size();
        }
        if (Grant.class.isAssignableFrom(childType)) {
        	return offset;
        } else {
        	offset += grants.size();
        }
        
        throw new IllegalArgumentException("Group does not allow children of type " + childType);
    }

    public List<SPObject> getChildren() {
        List<SPObject> children = new ArrayList<SPObject>();
        children.addAll(this.members);
        children.addAll(this.grants);
        return children;
    }
    
    @SuppressWarnings("unchecked")
	@Override
    public <T extends SPObject> List<T> getChildren(Class<T> type) {
    	List<T> children = new ArrayList<T>();
    	if (type.isAssignableFrom(Grant.class)) {
    		children.addAll((List<? extends T>) grants);
    	} 
    	if (type.isAssignableFrom(GroupMember.class)) {
    		children.addAll((List<? extends T>) members);
    	}
    	return children;
    }

    public List<SPObject> getDependencies() {
        return Collections.emptyList();
    }

    public void removeDependency(SPObject dependency) {
        // no-op
    }

    public void addGrant(Grant grant) {
    	addGrant(grant, grants.size());
    }
    
    public void addGrant(Grant grant, int index) {
    	this.grants.add(index, grant);
        grant.setParent(this);
        fireChildAdded(Grant.class, grant, index);    	
    }
    
    public boolean removeGrant(Grant grant) {
    	boolean wasRemoved = false;
        if (this.grants.contains(grant)) {
            int index = this.grants.indexOf(grant);
            wasRemoved = this.grants.remove(grant);
            grant.setParent(null);
            fireChildRemoved(Grant.class, grant, index);
        }
        return wasRemoved;
    }
    
    public void addMember(GroupMember member, int index) {
        this.members.add(index, member);
        member.setParent(this);
        fireChildAdded(GroupMember.class, member, index);
    }

    public void addMember(GroupMember member) {
        addMember(member, members.size());
    }
    
    public boolean removeMember(GroupMember member) {
    	boolean wasRemoved = false;
        if (this.members.contains(member)) {
            int index = this.members.indexOf(member);
            wasRemoved = this.members.remove(member);
            member.setParent(null);
            fireChildRemoved(GroupMember.class, member, index);
        }
        return wasRemoved;
    }

	public String getAuthority() {
		return super.getName();
	}

	public int compareTo(Object o) {
		assert o instanceof GrantedAuthority;
		return ((GrantedAuthority)o).getAuthority().compareTo(this.getAuthority());
	}
	
	@Override
	public String toString() {
		return super.getName();
	}
	
	@Override
	protected void addChildImpl(SPObject child, int index) {
		if (child instanceof GroupMember) {
			addMember((GroupMember) child, index);
		} else if (child instanceof Grant) {
			addGrant((Grant) child, index);
		} else {
			throw new IllegalArgumentException("Group does not accept this child: " + child);
		}
	}

	public List<Class<? extends SPObject>> getAllowedChildTypes() {
		List<Class<? extends SPObject>> childTypes = new ArrayList<Class<? extends SPObject>>();
		childTypes.add(GroupMember.class);
		childTypes.add(Grant.class);
		return childTypes;
	}
}
