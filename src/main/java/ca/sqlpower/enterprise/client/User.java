/*
 * Copyright (c) 2009, SQL Power Group Inc.
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

package ca.sqlpower.enterprise.client;

import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.security.GrantedAuthority;
import org.springframework.security.userdetails.UserDetails;

import ca.sqlpower.object.AbstractSPObject;
import ca.sqlpower.object.SPObject;
import ca.sqlpower.object.annotation.Accessor;
import ca.sqlpower.object.annotation.Constructor;
import ca.sqlpower.object.annotation.ConstructorParameter;
import ca.sqlpower.object.annotation.Mutator;
import ca.sqlpower.object.annotation.NonProperty;
import ca.sqlpower.object.annotation.Transient;

public class User extends AbstractSPObject implements UserDetails {

    public static List<Class<? extends SPObject>> allowedChildTypes = 
        Collections.<Class<? extends SPObject>>singletonList(Grant.class);
	
    private final List<Grant> grants;
    private String password;
    private GrantedAuthority[] authorities = null;
    private String fullName = null;
    private String email = null;

    @Constructor
    public User(@ConstructorParameter(propertyName = "name") String username, 
    		@ConstructorParameter(propertyName = "password") String password) {
    	super();
        assert username != null;
        this.grants = new ArrayList<Grant>();
        this.password = password;
        super.setName(username);
    }

    protected boolean removeChildImpl(SPObject child) {
    	if (child instanceof Grant) {
    		return removeGrant((Grant) child);
    	} else {
    		return false;
    	}
    }

    @NonProperty
    public List<Grant> getChildren() {
        return this.grants;
    }
    
    @NonProperty
    @SuppressWarnings("unchecked")
    @Override
	public <T extends SPObject> List<T> getChildren(Class<T> type) {
    	if (type.equals(Grant.class)) {
    		return (List<T>) grants;
    	} else {
    		return Collections.emptyList();
    	}
    }

    @NonProperty
    public List<SPObject> getDependencies() {
        return Collections.emptyList();
    }

    public void removeDependency(SPObject dependency) {
        // no-op
    }

    @Accessor
    public String getPassword() {
        return password;
    }

    @Mutator
    public void setPassword(String password) {
        String oldPassword = this.password;
        this.password = password;
        firePropertyChange("password", oldPassword, password);
    }

    public void addGrant(Grant grant) {
    	addGrant(grant, grants.size());
    }
    
    public void addGrant(Grant grant, int index) {
    	childPositionOffset(grant.getClass());
        this.grants.add(index, grant);
        grant.setParent(this);
        fireChildAdded(Grant.class, grant, index);
    }
    
    public boolean removeGrant(Grant grant) {
    	boolean wasRemoved = false;
        if (this.grants.contains(grant)) {
            int index = this.grants.indexOf(grant);
            wasRemoved = this.grants.remove(grant);
            // Do not set parent to null as Grants are immutable in the JCR
            fireChildRemoved(Grant.class, grant, index);
        }
        return wasRemoved;
    }
    
    @Override
    protected void addChildImpl(SPObject child, int index) {
    	addGrant((Grant) child, index);
    }
    
    @Accessor
    public String getFullName() {
		return fullName;
	}

    @Mutator
	public void setFullName(String fullName) {
		String oldName = this.fullName;
		this.fullName = fullName;
		firePropertyChange("fullName", oldName, this.fullName);
	}

	@Accessor
	public String getEmail() {
		return email;
	}

	@Mutator
	public void setEmail(String email) {
		String oldEmail = this.email;
		this.email = email;
		firePropertyChange("email", oldEmail, this.email);
	}
    
    /**
     * The returned list is mutable. Beware.
     */
	@Transient @Accessor
    public List<Grant> getGrants() {
		return grants;
	}

    @Transient @Accessor
	public GrantedAuthority[] getAuthorities() {
		if (this.authorities==null) {
			throw new RuntimeException("Programmatic error. The user manager has to fill in this user's groups before passing back to the security framework.");
		} else {
			return this.authorities;
		}
	}
	
    @Transient @Mutator
	public void setAuthorities(GrantedAuthority[] authorities) {
		this.authorities = authorities;
	}

	@Transient @Accessor
	public String getUsername() {
		return super.getName();
	}

	@Transient @Accessor
	public boolean isAccountNonExpired() {
		return true;
	}

	@Transient @Accessor
	public boolean isAccountNonLocked() {
		return true;
	}

	@Transient @Accessor
	public boolean isCredentialsNonExpired() {
		return true;
	}

	@Transient @Accessor
	public boolean isEnabled() {
		return true;
	}
	
	@Override
	public String toString() {
		return getName();
	}

	@NonProperty
	public List<Class<? extends SPObject>> getAllowedChildTypes() {
		List<Class<? extends SPObject>> childTypes = new ArrayList<Class<? extends SPObject>>();
		childTypes.add(Grant.class);
		return childTypes;
	}

	/**
	 * Marking this class as not serializable. The {@link UserDetails} interface
	 * extends from {@link Serializable}, which makes this class also
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
	 * Marking this class as not serializable. The {@link UserDetails} interface
	 * extends from {@link Serializable}, which makes this class also
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
