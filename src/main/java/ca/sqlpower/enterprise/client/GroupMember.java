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

import java.util.Collections;
import java.util.List;

import ca.sqlpower.object.AbstractSPObject;
import ca.sqlpower.object.SPObject;
import ca.sqlpower.object.annotation.Accessor;
import ca.sqlpower.object.annotation.Constructor;
import ca.sqlpower.object.annotation.ConstructorParameter;

public class GroupMember extends AbstractSPObject {
	
    /**
     * Defines an absolute ordering of the child types of this class.
     * 
     * IMPORTANT!: When changing this, ensure you maintain the order specified by {@link #getChildren()}
     */
	private static final List<Class<? extends SPObject>> allowedChildTypes = Collections.emptyList();

    private final User user;
    
    @Constructor
    public GroupMember(@ConstructorParameter(propertyName = "user") User user) {
        this.user = user;
        if (user == null) {
        	throw new NullPointerException("User is null");
        }
    }

    @Override
    @Accessor
    public String getName() {
    	return user.getName();
    }
    
    @Override
    protected boolean removeChildImpl(SPObject child) {
        return false;
    }

    public List<? extends SPObject> getChildren() {
        return Collections.emptyList();
    }

    public List<User> getDependencies() {
        return Collections.singletonList(this.user);
    }

    public void removeDependency(SPObject dependency) {
        if (dependency.equals(this.user)) {
            ((Group)getParent()).removeMember(this);
        }
    }

	public User getUser() {
		return user;
	}

	public List<Class<? extends SPObject>> getAllowedChildTypes() {
		return allowedChildTypes;
	}

    
}
