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
import ca.sqlpower.object.annotation.Transient;

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

	/**
	 * Sets the value of the obsolete flag. Use this to mark this snapshot as
	 * obsolete (the original SPObject identified by {@link #getOriginalUUID()}
	 * has changed state since this snapshot was made).
	 * 
	 * @param isObsolete
	 *            Set to true if the original SPObject has changed state since
	 *            this snapshot was made. Otherwise, set to false.
	 */
	@Mutator
	public void setObsolete(boolean isObsolete);

	/**
	 * Returns whether or not this snapshot is obsolete when compared to its
	 * original (identified by {@link #getOriginalUUID()}) when most recently
	 * checked. Note that since this is based on when it was most recently
	 * checked, it is certainly possible that a snapshot can be obsolete and
	 * still return false.
	 * 
	 * @return True if this snapshot is obsolete (the original SPObject has
	 *         changed state). Return false if it is not obsolete as of the most
	 *         recent check.
	 */
	@Accessor
	public boolean isObsolete();

	/**
	 * Sets the value of the deleted flag. Use this to signal that the original
	 * object associated with this snapshot has been deleted.
	 * 
	 * @param isDeleted
	 */
	@Transient @Mutator
	void setDeleted(boolean isDeleted);

	/**
	 * Returns whether or not the original objects associated with this snapshot has been deleted.
	 * @return
	 */
	@Transient @Accessor
	boolean isDeleted();
}