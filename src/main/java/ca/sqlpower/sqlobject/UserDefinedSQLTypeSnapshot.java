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

import ca.sqlpower.object.SystemSPObjectSnapshot;
import ca.sqlpower.object.ObjectDependentException;
import ca.sqlpower.object.SPObject;
import ca.sqlpower.object.annotation.Accessor;
import ca.sqlpower.object.annotation.Constructor;
import ca.sqlpower.object.annotation.ConstructorParameter;
import ca.sqlpower.object.annotation.ConstructorParameter.ParameterType;

/**
 * An {@link SystemSPObjectSnapshot} implementation specifically for {@link UserDefinedSQLType}
 */
public class UserDefinedSQLTypeSnapshot extends SystemSPObjectSnapshot<UserDefinedSQLType> {

	private final UserDefinedSQLType snapshotType;

	/**
	 * This particular constructor is intended to be used by the SPObject
	 * persistence mechanism. If you're actually creating a new snapshot, use
	 * {@link #UserDefinedSQLTypeSnapshot(UserDefinedSQLType, int)}
	 * 
	 * @param spObject
	 *            The snapshot {@link UserDefinedSQLType}
	 * @param originalUUID
	 *            The UUID of the original {@link UserDefinedSQLType} object
	 * @param systemRevision
	 *            The system workspace revision number from which the snapshot
	 *            was taken.
	 */
    @Constructor
	public UserDefinedSQLTypeSnapshot(
			@ConstructorParameter (isProperty = ParameterType.CHILD) UserDefinedSQLType spObject,
			@ConstructorParameter (isProperty = ParameterType.PRIMITIVE) String originalUUID,
			@ConstructorParameter (isProperty = ParameterType.PRIMITIVE, defaultValue = "0") int systemRevision) {
		super(originalUUID, systemRevision);
		this.snapshotType = spObject;
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
	 * @param original
	 *            The {@link UserDefinedSQLType} to make a snapshot of
	 * @param systemRevision
	 *            The system workspace revision number from which the snapshot
	 *            is being taken.
	 * @throws IllegalArgumentException
	 * @throws ObjectDependentException
	 */
	public UserDefinedSQLTypeSnapshot(UserDefinedSQLType original, int systemRevision) {
		super(original.getUUID(), systemRevision);
		setName(original.getName());
		snapshotType = new UserDefinedSQLType();
		UserDefinedSQLType.copyProperties(getSPObject(), original);
		getSPObject().setParent(this);
	}

	/**
	 * Create a snapshot of a {@link UserDefinedSQLType} and set the upstream
	 * type of the snapshotted {@link UserDefinedSQLType} object to be the
	 * {@link UserDefinedSQLType} from the given upstreamTypeSnapshot. This is
	 * intended to be used if the original {@link UserDefinedSQLType} has an
	 * upstreamType returned by {@link UserDefinedSQLType#getUpstreamType()}.
	 * 
	 * @param original
	 *            The {@link UserDefinedSQLType} to make a snapshot of
	 * @param systemRevision
	 *            The system workspace revision number from which the snapshot
	 *            is being taken.
	 * @param upstreamTypeSnapshot
	 *            A {@link UserDefinedSQLTypeSnapshot} of the upstreamType of
	 *            the original {@link UserDefinedSQLType}
	 * @throws IllegalArgumentException
	 * @throws ObjectDependentException
	 */
	public UserDefinedSQLTypeSnapshot(UserDefinedSQLType original,
			int systemRevision, UserDefinedSQLTypeSnapshot upstreamTypeSnapshot) {
		this(original, systemRevision);
		snapshotType.setUpstreamType(upstreamTypeSnapshot.getSPObject());
	}

	/**
	 * An unmodifiable {@link List} of allowed child types
	 */
	public static final List<Class<? extends SPObject>> allowedChildTypes = 
	     Collections.<Class<? extends SPObject>>singletonList(UserDefinedSQLType.class);
	
	public List<Class<? extends SPObject>> getAllowedChildTypes() {
		return allowedChildTypes;
	}

	public List<? extends SPObject> getChildren() {
		return Collections.singletonList(getSPObject());
	}

	@Accessor
	public UserDefinedSQLType getSPObject() {
		return snapshotType;
	}
}
