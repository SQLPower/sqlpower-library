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
			@ConstructorParameter(propertyName = "originalUUID") String originalUUID,
			@ConstructorParameter(propertyName = "workspaceRevision") int systemRevision) {
		this.originalUUID = originalUUID;
		this.workspaceRevision = systemRevision;
	}

	/**
	 * The UUID of the original {@link UserDefinedSQLType}
	 */
	private final String originalUUID;
	
	/**
	 * The revision number of the System workspace at the exact time the
	 * snapshot was taken
	 */
	private int workspaceRevision;
	
	@Accessor
	public String getOriginalUUID() {
		return originalUUID;
	}

	@Accessor
	public String getWorkspaceUUID() {
		return SYSTEM_WORKSPACE_UUID;
	}
	
	@Mutator
	public void setWorkspaceRevision(int workspaceRevision) {
		int oldValue = this.workspaceRevision;
		this.workspaceRevision = workspaceRevision;
		firePropertyChange("workspaceRevision", oldValue, workspaceRevision);
	}

	@Accessor
	public int getWorkspaceRevision() {
		return workspaceRevision;
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
}
