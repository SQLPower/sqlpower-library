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

package ca.sqlpower.object;

import java.util.Collections;
import java.util.List;

import ca.sqlpower.object.annotation.Accessor;
import ca.sqlpower.object.annotation.Constructor;
import ca.sqlpower.object.annotation.ConstructorParameter;
import ca.sqlpower.object.annotation.Mutator;
import ca.sqlpower.object.annotation.Transient;
import ca.sqlpower.sqlobject.UserDefinedSQLType;

/**
 * A base implementation of {@link SPObjectSnapshot} for SPObjects that
 * originated from the "system" workspace. It includes common implementations of
 * the SPObject methods.
 * 
 * @param <T>
 *            The SPObject implementation that will be snapshot
 */
public abstract class SystemSPObjectSnapshot<T extends SPObject> extends
		AbstractSPObject implements SPObjectSnapshot<T> {

	private static final String SYSTEM_WORKSPACE_UUID = "system";
	
	@Constructor
	public SystemSPObjectSnapshot(
			@ConstructorParameter(propertyName = "originalUUID") String originalUUID) {
		this.originalUUID = originalUUID;
	}

	/**
	 * The UUID of the original {@link UserDefinedSQLType}
	 */
	private final String originalUUID;
	
	/**
	 * Whether or not this snapshot is obsolete when compared to its original
	 * (identified by {@link #getOriginalUUID()}) when most recently checked.
	 * Note that since this is based on when it was most recently checked, it is
	 * certainly possible that a snapshot can be obsolete and still return
	 * false.
	 */       
	private boolean obsolete;

	/**
	 * Flag to signal that the original object has been deleted. If this is set,
	 * we should not try to look up the original.
	 */
	private boolean deleted = false;
	
	@Accessor
	public String getOriginalUUID() {
		return originalUUID;
	}

	@Accessor
	public String getWorkspaceUUID() {
		return SYSTEM_WORKSPACE_UUID;
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

	public List<? extends SPObject> getDependencies() {
		return Collections.emptyList();
	}

	public void removeDependency(SPObject dependency) {
		// no-op
	}
	
	@Mutator
	public void setObsolete(boolean isObsolete) {
		boolean oldValue = this.obsolete;
		this.obsolete = isObsolete;
		firePropertyChange("obsolete", oldValue, isObsolete);
	}
	
	@Accessor
	public boolean isObsolete() {
		return obsolete;
	}
	
	@Transient @Mutator
	public void setDeleted(boolean isDeleted) {
		boolean oldValue = this.deleted;
		this.deleted = isDeleted;
		firePropertyChange("deleted", oldValue, isDeleted);
	}
	
	@Transient @Accessor
	public boolean isDeleted() {
		return deleted;
	}
}
