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

package ca.sqlpower.sqlobject;

import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;

import ca.sqlpower.object.ObjectDependentException;
import ca.sqlpower.object.SPObject;
import ca.sqlpower.object.SystemSPObjectSnapshot;
import ca.sqlpower.object.annotation.Accessor;
import ca.sqlpower.object.annotation.Constructor;
import ca.sqlpower.object.annotation.ConstructorParameter;
import ca.sqlpower.object.annotation.Mutator;

/**
 * An {@link SystemSPObjectSnapshot} implementation specifically for {@link UserDefinedSQLType}
 */
public class UserDefinedSQLTypeSnapshot extends SystemSPObjectSnapshot<UserDefinedSQLType> {
	
	private static final Logger logger = Logger.getLogger(UserDefinedSQLTypeSnapshot.class);

	/**
	 * The {@link UserDefinedSQLType} that is a copy of the original
	 */
	private final UserDefinedSQLType spObject;

	/**
	 * Is true if the snapshot is of a {@link UserDefinedSQLType} that is a
	 * Domain, or if this snapshot is referred to as the upstream type from a
	 * domain {@link UserDefinedSQLTypeSnapshot}.
	 */
	private final boolean domainSnapshot;

	/**
	 * Counts the number of times the snapshot is used by a column in Architect.
	 * When this number reaches 0 this type can be removed. Starts at 0 since it
	 * starts not connected to a project. Cannot be null.
	 */
	private int snapshotUseCount = 0;

	/**
	 * This particular constructor is intended to be used by the SPObject
	 * persistence mechanism. If you're actually creating a new snapshot, use
	 * {@link #UserDefinedSQLTypeSnapshot(UserDefinedSQLType, int)}
	 * 
	 * @param spObject
	 *            The snapshot {@link UserDefinedSQLType}
	 * @param originalUUID
	 *            The UUID of the original {@link UserDefinedSQLType} object
	 * @param systemWorkspaceRevision
	 *            The system workspace revision number from which the snapshot
	 *            was taken.
	 * @param isDomainSnapshot
	 *            Is true if the snapshot is of a {@link UserDefinedSQLType}
	 *            that is a Domain, or if this snapshot is referred to as the
	 *            upstream type from a domain {@link UserDefinedSQLTypeSnapshot}
	 */
    @Constructor
	public UserDefinedSQLTypeSnapshot(
			@ConstructorParameter (propertyName = "spObject") UserDefinedSQLType spObject,
			@ConstructorParameter (propertyName = "originalUUID") String originalUUID,
			@ConstructorParameter (propertyName = "domainSnapshot") boolean isDomainSnapshot) {
		super(originalUUID);
		this.spObject = spObject;
		this.domainSnapshot = isDomainSnapshot;
	}

	/**
	 * Create a snapshot of a {@link UserDefinedSQLType}.
	 * 
	 * IMPORTANT: If the original {@link UserDefinedSQLType} has an upstream
	 * type, then create a {@link UserDefinedSQLTypeSnapshot} of the original
	 * upstream type first, and then use
	 * {@link #UserDefinedSQLTypeSnapshot(UserDefinedSQLType, int, UserDefinedSQLTypeSnapshot)}
	 * , using the upstream type snapshot as the third parameter.
	 * 
	 * It is also important that any client code that creates the the snapshot
	 * must add the copied {@link UserDefinedSQLType} (retrieved using
	 * {@link #getSPObject()}) as a node to the SPObject tree.
	 * 
	 * @param original
	 *            The {@link UserDefinedSQLType} to make a snapshot of
	 * @param systemRevision
	 *            The system workspace revision number from which the snapshot
	 *            is being taken.
	 * @param isDomainSnapshot
	 *            Is true if the snapshot is of a {@link UserDefinedSQLType}
	 *            that is a Domain, or if this snapshot is referred to as the
	 *            upstream type from a domain {@link UserDefinedSQLTypeSnapshot}
	 * @throws IllegalArgumentException
	 * @throws ObjectDependentException
	 */
	public UserDefinedSQLTypeSnapshot(UserDefinedSQLType original, boolean isDomainSnapshot) {
		super(original.getUUID());
		setName(original.getName());
		spObject = new UserDefinedSQLType();
		UserDefinedSQLType.copyProperties(getSPObject(), original);
		this.domainSnapshot = isDomainSnapshot;
	}

	/**
	 * Create a snapshot of a {@link UserDefinedSQLType} and set the upstream
	 * type of the snapshotted {@link UserDefinedSQLType} object to be the
	 * {@link UserDefinedSQLType} from the given upstreamTypeSnapshot. This is
	 * intended to be used if the original {@link UserDefinedSQLType} has an
	 * upstreamType returned by {@link UserDefinedSQLType#getUpstreamType()}.
	 * 
	 * It is also important that any client code that creates the the snapshot
	 * must add the copied {@link UserDefinedSQLType} (retrieved using
	 * {@link #getSPObject()}) as a node to the SPObject tree.
	 * 
	 * @param original
	 *            The {@link UserDefinedSQLType} to make a snapshot of
	 * @param systemRevision
	 *            The system workspace revision number from which the snapshot
	 *            is being taken.
	 * @param isDomainSnapshot
	 *            Is true if the snapshot is of a {@link UserDefinedSQLType}
	 *            that is a Domain, or if this snapshot is referred to as the
	 *            upstream type from a domain {@link UserDefinedSQLTypeSnapshot}
	 * @param upstreamTypeSnapshot
	 *            A {@link UserDefinedSQLTypeSnapshot} of the upstreamType of
	 *            the original {@link UserDefinedSQLType}
	 * @throws IllegalArgumentException
	 * @throws ObjectDependentException
	 */
	public UserDefinedSQLTypeSnapshot(UserDefinedSQLType original,
			boolean isDomainSnapshot, UserDefinedSQLTypeSnapshot upstreamTypeSnapshot) {
		this(original, isDomainSnapshot);
		spObject.setUpstreamType(upstreamTypeSnapshot.getSPObject());
	}

	/**
	 * An unmodifiable {@link List} of allowed child types
	 */
	public static final List<Class<? extends SPObject>> allowedChildTypes = 
	     Collections.emptyList();
	
	public List<Class<? extends SPObject>> getAllowedChildTypes() {
		return allowedChildTypes;
	}

	public List<? extends SPObject> getChildren() {
		return Collections.emptyList();
	}

	@Accessor
	public UserDefinedSQLType getSPObject() {
		return spObject;
	}

	@Accessor
	public boolean isDomainSnapshot() {
		return domainSnapshot;
	}

	@Mutator
	public void setSnapshotUseCount(int snapshotUseCount) {
		if (snapshotUseCount < 0) throw new IllegalArgumentException(
				"There cannot be a negative number of objects using the snapshot for " + 
				getSPObject().getName() + ".");
		if (getSPObject() != null) {
			logger.debug("Setting snapshot use count of " + getSPObject().getName() + " to " + snapshotUseCount);
		}
		int oldCount = this.snapshotUseCount;
		this.snapshotUseCount = snapshotUseCount;
		firePropertyChange("snapshotUseCount", oldCount, snapshotUseCount);
	}

	@Accessor
	public int getSnapshotUseCount() {
		return snapshotUseCount;
	}
}
