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

import ca.sqlpower.object.annotation.Accessor;
import ca.sqlpower.object.annotation.Mutator;

/**
 * A 'snapshot' of an {@link SPObject} in the SPObject persistence mechanism in
 * a particular point in time. This is marked by the workspace revision number.
 * 
 * @param <T>
 *            The {@link SPObject} implementation that will have a snapshot
 *            taken of it
 */
public interface SPObjectSnapshot<T extends SPObject> extends SPObject {

	/**
	 * Returns the UUID of the {@link SPObject} that this is a snapshot of.
	 * 
	 * @return The UUID of the original {@link SPObject} as a {@link String}
	 */
	@Accessor
	public String getOriginalUUID();

	/**
	 * @return Returns the UUID of the workspace that the original SPObject
	 *         belonged to.
	 */
	@Accessor
	public String getWorkspaceUUID();
	
	/**
	 * Set the workspace revision number that this snapshot is based on. This
	 * will be typically used when initially creating or updating the
	 * snapshotted object
	 * 
	 * @param workspaceRevision
	 *            The revision number of the workspace identified by the UUID
	 *            given by {@link #getWorkspaceUUID()} at which this snapshot
	 *            was taken.
	 */
	@Mutator
	public void setWorkspaceRevision(int workspaceRevision);

	/**
	 * @return Returns the revision number of the original SPObject's workspace
	 *         when the snapshot was taken.
	 */
	@Accessor
	public int getWorkspaceRevision();

	/**
	 * Returns the {@link SPObject} that is a snapshot of another SPObject with
	 * the UUID given by {@link #getOriginalUUID()}. The returned
	 * {@link SPObject} should be in an identical state that the original was in
	 * in the workspace identified by {@link #getWorkspaceUUID()} in the
	 * revision returned by {@link #getWorkspaceRevision()}.
	 * 
	 * @return A copy of the original SPObject taken in the workspace identified
	 *         by {@link #getWorkspaceUUID()} at the revision returned by
	 *         {@link #getWorkspaceRevision()}
	 */
	@Accessor
	public T getSPObject();
}