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

import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.security.GrantedAuthority;

import ca.sqlpower.object.AbstractSPObject;
import ca.sqlpower.object.SPObject;
import ca.sqlpower.object.annotation.Accessor;
import ca.sqlpower.object.annotation.Constructor;
import ca.sqlpower.object.annotation.ConstructorParameter;
import ca.sqlpower.object.annotation.NonProperty;
import ca.sqlpower.object.annotation.Transient;

public class Group extends AbstractSPObject implements GrantedAuthority {

    /**
     * Defines an absolute ordering of the child types of this class.
     * 
     * IMPORTANT!: When changing this, ensure you maintain the order specified by {@link #getChildren()}
     */
	public static List<Class<? extends SPObject>> allowedChildTypes = 
        Collections.unmodifiableList(new ArrayList<Class<? extends SPObject>>(
        		Arrays.asList(GroupMember.class, Grant.class)));

	/**
	 * FIXME This enum defines the {@link SPObject} child classes a
	 * {@link Group} takes as well as the ordinal order of these child classes
	 * such that the class going before does not depend on the class that goes
	 * after.
	 */
	public enum SPObjectOrder {
		GROUP_MEMBER(GroupMember.class),
		GRANT(Grant.class);
		
		/**
		 * @see #getSuperChildClass()
		 */
		private final Class<? extends SPObject> superChildClass;
		
		/**
		 * @see #getChildClasses()
		 */
		private final Set<Class<? extends SPObject>> classes;

		/**
		 * Creates a new {@link SPObjectOrder},
		 * 
		 * @param superChildClass
		 *            The highest {@link SPObject} class that the
		 *            {@link SPObject#childPositionOffset(Class)} method looks
		 *            at to determine the index.
		 * @param classes
		 *            The list of child {@link SPObject} class varargs which
		 *            share the same ordering in the list of children. These
		 *            classes must be extending/implementing
		 *            {@link #superChildClass}.
		 */
		private SPObjectOrder(Class<? extends SPObject> superChildClass, Class<? extends SPObject>... classes) {
			this.superChildClass = superChildClass;
			this.classes = new HashSet<Class<? extends SPObject>>(Arrays.asList(classes));
		}

		/**
		 * Returns the highest {@link SPObject} class that the
		 * {@link SPObject#childPositionOffset(Class)} method looks at to
		 * determine the index.
		 */
		public Class<? extends SPObject> getSuperChildClass() {
			return superChildClass;
		}

		/**
		 * Returns the {@link Set} of {@link SPObject} classes that share the
		 * same ordering in the list of children. These classes must either
		 * extend/implement from the same class type given by
		 * {@link SPObjectOrder#getSuperChildClass()}.
		 */
		public Set<Class<? extends SPObject>> getChildClasses() {
			return Collections.unmodifiableSet(classes);
		}
		
		public static SPObjectOrder getOrderBySimpleClassName(String name) {
			for (SPObjectOrder order : values()) {
				if (order.getSuperChildClass().getSimpleName().equals(name)) {
					return order;
				} else {
					for (Class<? extends SPObject> childClass : order.getChildClasses()) {
						if (childClass.getSimpleName().equals(name)) {
							return order;
						}
					}
				}
			}
			throw new IllegalArgumentException("The " + SPObject.class.getSimpleName() + 
					" class \"" + name + "\" does not exist or is not a child type " +
							"of " + Group.class.getSimpleName() + ".");
		}
		
	}
	
    private final List<Grant> grants = new ArrayList<Grant>();
    private final List<GroupMember> members = new ArrayList<GroupMember>();

    @Constructor
    public Group(@ConstructorParameter(propertyName = "name") String name) {
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

    @NonProperty
    public List<SPObject> getChildren() {
        List<SPObject> children = new ArrayList<SPObject>();
        children.addAll(this.members);
        children.addAll(this.grants);
        return children;
    }
    
    @NonProperty
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

    @NonProperty
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
            fireChildRemoved(Grant.class, grant, index);
            grant.setParent(null);
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
    
    public void addUser(User user) {
    	addMember(new GroupMember(user));
    }
    
    public boolean removeMember(GroupMember member) {
    	boolean wasRemoved = false;
        if (this.members.contains(member)) {
            int index = this.members.indexOf(member);
            wasRemoved = this.members.remove(member);
            fireChildRemoved(GroupMember.class, member, index);
            member.setParent(null);
        }
        return wasRemoved;
    }
    
    public boolean removeUser(User user) {
    	for (GroupMember member : members) {
    		if (member.getUser().getUUID().equals(user.getUUID())) {
    			return removeMember(member);
    		}
    	}
    	return false;
    }

    @Transient @Accessor
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

	@NonProperty
	public List<Class<? extends SPObject>> getAllowedChildTypes() {
		List<Class<? extends SPObject>> childTypes = new ArrayList<Class<? extends SPObject>>();
		childTypes.add(GroupMember.class);
		childTypes.add(Grant.class);
		return childTypes;
	}

	/**
	 * Marking this class as not serializable. The {@link GrantedAuthority}
	 * interface extends from {@link Serializable}, which makes this class also
	 * {@link Serializable}. However, our object model does not use
	 * serialization.
	 * 
	 * Followed the article written by Sun at
	 * http://java.sun.com/developer/technicalArticles/ALT/serialization/
	 * 
	 * @param ois
	 * @throws ClassNotFoundException
	 * @throws IOException
	 */
	private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
		throw new NotSerializableException();
	}

	/**
	 * Marking this class as not serializable. The {@link GrantedAuthority}
	 * interface extends from {@link Serializable}, which makes this class also
	 * {@link Serializable}. However, our object model does not use
	 * serialization.
	 * 
	 * Followed the article written by Sun at
	 * http://java.sun.com/developer/technicalArticles/ALT/serialization/
	 * 
	 * @param ois
	 * @throws IOException
	 */
	private void writeObject(ObjectOutputStream ois) throws IOException {
		throw new NotSerializableException();
	}
}
