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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import ca.sqlpower.object.AbstractSPObject;
import ca.sqlpower.object.SPObject;
import ca.sqlpower.object.annotation.Accessor;
import ca.sqlpower.object.annotation.Constructor;
import ca.sqlpower.object.annotation.ConstructorParameter;
import ca.sqlpower.object.annotation.Mutator;

/**
 * A Grant object represents a set of permissions on a single object, or class
 * of objects. Due to restrictions in the JCR, Grants should remain immutable.
 * To change a User's permissions, remove the old Grant, and create a new one.
 */
public class Grant extends AbstractSPObject {

    /**
     * Defines an absolute ordering of the child types of this class.
     * 
     * IMPORTANT!: When changing this, ensure you maintain the order specified by {@link #getChildren()}
     */
	public static List<Class<? extends SPObject>> allowedChildTypes = Collections.emptyList();
	
    private final String type;
    private final String subject;
    private final boolean createPrivilege;
    private final boolean modifyPrivilege;
    private final boolean deletePrivilege;
    private final boolean executePrivilege;
    private final boolean grantPrivilege;

    /**
     * Copy constructor
     * @param grant Grant to copy
     */
    public Grant(@Nonnull Grant grant) {
    	this(grant.getSubject(),grant.getType(),grant.isCreatePrivilege(),
    		grant.isModifyPrivilege(),grant.isDeletePrivilege(),
    		grant.isExecutePrivilege(),grant.isGrantPrivilege());
    }

	/**
	 * Creates a grant object. If subject is null, the grant will be system
	 * wide. Otherwise, it will only affect the subject referenced. Preferably,
	 * one of
	 * {@link #Grant(SPObject, boolean, boolean, boolean, boolean, boolean)} or
	 * {@link #Grant(String, boolean, boolean, boolean, boolean, boolean)}
	 * should be used, as these leave no ambiguity as to whether this grant is
	 * system level or not.
	 * 
	 * @param subject
	 *            The UUID of the object we want to grant access to. Can be null
	 *            if the type parameter is used.
	 * @param type
	 *            The class of object to grant access to. If the subject
	 *            parameter is null, this will represent a system level Grant on
	 *            this type.
	 * @param create
	 * @param modify
	 * @param delete
	 * @param execute
	 * @param grant
	 */
    @Constructor
    public Grant(@Nullable @ConstructorParameter(propertyName = "subject") String subject, 
    		@Nonnull @ConstructorParameter(propertyName = "type") String type,
            @ConstructorParameter(propertyName = "createPrivilege") boolean create, 
            @ConstructorParameter(propertyName = "modifyPrivilege") boolean modify, 
            @ConstructorParameter(propertyName = "deletePrivilege") boolean delete, 
            @ConstructorParameter(propertyName = "executePrivilege") boolean execute,
            @ConstructorParameter(propertyName = "grantPrivilege") boolean grant) 
    {
        this.subject = subject;
        this.type = type;
        this.createPrivilege = create;
        this.modifyPrivilege = modify;
        this.deletePrivilege = delete;
        this.executePrivilege = execute;
        this.grantPrivilege = grant;
    }

	/**
	 * Creates an object level Grant on the given subject
	 * 
	 * @param subject
	 *            The SPObject to which the grant references
	 * @param create
	 * @param modify
	 * @param delete
	 * @param execute
	 * @param grant
	 */
	public Grant(SPObject subject, boolean create, boolean modify,
			boolean delete, boolean execute, boolean grant) {
		this(subject.getUUID(), subject.getClass().getName(), create, modify,
				delete, execute, grant);
	}

	/**
	 * Creates a system level Grant on the given type
	 * 
	 * @param type
	 *            The type on which this applies. In Wabit, this is a simple
	 *            class name, in Architect, it is a fully qualified class name.
	 * @param create
	 * @param modify
	 * @param delete
	 * @param execute
	 * @param grant
	 */
	/*
	 * The type parameter here should probably be a Class, but since Wabit and
	 * Architect have differing conventions, we have to take a String for now.
	 * In future, we should convert wabit over to using fully qualified class
	 * names, and change the type parameter of this constructor to a Class
	 * object.
	 */
	public Grant(String type, boolean create, boolean modify, boolean delete, boolean execute, boolean grant) {
		this(null, type, create, modify, delete, execute, grant);
	}

    @Override
    @Mutator
    public void setName(String name) {
    	// no op (to not use default setName() is SPObject) 
    }
    
    @Override
    @Accessor
    public String getName() {
        if (this.subject != null) {
            return this.subject.concat(" - ").concat(this.getPermsString());
        } else if (this.type != null){
            return "All ".concat(this.type)
                .concat(" - ").concat(this.getPermsString());
        } else {
            throw new RuntimeException("Badly constructed grant object.");
        }
    }

    @Override
    protected boolean removeChildImpl(SPObject child) {
        return false;
    }

    public List<Class<? extends SPObject>> getAllowedChildTypes() {
    	return Collections.emptyList();
    }

    public List<? extends SPObject> getChildren() {
        return Collections.emptyList();
    }

    public List<SPObject> getDependencies() {
        return Collections.emptyList();
    }

    public void removeDependency(SPObject dependency) {
        // no-op
    }
    
    @Accessor
    public boolean isCreatePrivilege() {
        return createPrivilege;
    }

    @Accessor
    public boolean isModifyPrivilege() {
        return modifyPrivilege;
    }

    @Accessor
    public boolean isDeletePrivilege() {
        return deletePrivilege;
    }

    @Accessor
    public boolean isExecutePrivilege() {
        return executePrivilege;
    }

    @Accessor
    public boolean isGrantPrivilege() {
        return grantPrivilege;
    }

    public boolean isReadOnly() {
    	return (executePrivilege && 
    			!(createPrivilege || modifyPrivilege || deletePrivilege || grantPrivilege));
    }
    
    public boolean hasPermissions() {
    	return (executePrivilege || createPrivilege || modifyPrivilege
				|| deletePrivilege || grantPrivilege);
    }

    @Accessor
    public String getType() {
        return type;
    }

    @Accessor
    public String getSubject() {
        return subject;
    }
    
    public String getPermsString() {
        StringBuilder sb = new StringBuilder("");
        if (this.createPrivilege) {
            sb.append("C");
        }
        if (this.modifyPrivilege) {
            sb.append("M");
        }
        if (this.deletePrivilege) {
            sb.append("D");
        }
        if (this.executePrivilege) {
            sb.append("E");
        }
        if (this.grantPrivilege) {
            sb.append("G");
        }
        return sb.toString();
    }

	public boolean isSystemLevel() {
		return subject == null;
	}
}