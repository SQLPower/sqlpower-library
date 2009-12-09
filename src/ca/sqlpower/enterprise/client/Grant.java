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
import ca.sqlpower.object.annotation.Persistable;

/**
 * A Grant object represents a set of permissions on a single object, or class
 * of objects. Due to restrictions in the JCR, Grants should remain immutable.
 * To change a User's permissions, remove the old Grant, and create a new one.
 */
@Persistable
public class Grant extends AbstractSPObject {

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
     * Creates a grant object.
     * @param subject The object we want to grant access to. Can be null
     * if the type parameter is used.
     * @param type The class of wabit object to grant access to. Can be null
     * if the subject parameter is used.
     * @param create 
     * @param modify
     * @param delete
     * @param execute
     * @param grant
     */
    @Constructor
    public Grant(@Nullable @ConstructorParameter(propertyName = "subject") String subject, @Nonnull @ConstructorParameter(propertyName = "type") String type,
            @ConstructorParameter(propertyName = "createPrivilege") boolean create, @ConstructorParameter(propertyName = "modifyPrivilege") boolean modify, @ConstructorParameter(propertyName = "deletePrivilege") boolean delete, @ConstructorParameter(propertyName = "executePrivilege") boolean execute,
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

    @Override
    @Mutator
    public void setName(String name) {
    	// no op
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

    public boolean allowsChildren() {
        return false;
    }
    
    public int childPositionOffset(Class<? extends SPObject> childType) {
    	return 0;
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

    public boolean isCreatePrivilege() {
        return createPrivilege;
    }

    public boolean isModifyPrivilege() {
        return modifyPrivilege;
    }

    public boolean isDeletePrivilege() {
        return deletePrivilege;
    }

    public boolean isExecutePrivilege() {
        return executePrivilege;
    }

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

    public String getType() {
        return type;
    }

    public String getSubject() {
        return subject;
    }
    
    private String getPermsString() {
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
}