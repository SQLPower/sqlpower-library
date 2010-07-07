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

import javax.annotation.Nonnull;

import ca.sqlpower.object.SPObject;
import ca.sqlpower.object.annotation.Accessor;
import ca.sqlpower.object.annotation.Constructor;
import ca.sqlpower.object.annotation.ConstructorParameter;
import ca.sqlpower.object.annotation.Mutator;
import ca.sqlpower.object.annotation.Transient;

/**
 * This {@link SQLObject} represents an enumerated value that can be applied to
 * restrict the value of a {@link SQLColumn}.
 */
public class SQLEnumeration extends SQLObject {
	
	/**
	 * {@link List} of allowed child types, which is empty since
	 * {@link SQLEnumeration} has no children.
	 */
	public static final List<Class<? extends SPObject>> allowedChildTypes = 
		Collections.emptyList();

	/**
	 * Creates a new {@link SQLEnumeration} with a given name.
	 * 
	 * @param name
	 *            The name to give the enumeration.
	 */
	@Constructor
	public SQLEnumeration(
			@ConstructorParameter(propertyName="name") @Nonnull String name) {
		setName(name);
	}

	/**
	 * Copy constructor. Creates a copy of a {@link SQLEnumeration} with the
	 * same name.
	 * 
	 * @param enumeration
	 *            The {@link SQLEnumeration} to copy.
	 */
	public SQLEnumeration(@Nonnull SQLEnumeration enumeration) {
		this(enumeration.getName());
	}

	public int childPositionOffset(Class<? extends SPObject> childType) {
		return 0;
	}

	public void removeDependency(SPObject dependency) {
		// No operation.
	}

	public List<? extends SPObject> getDependencies() {
		return Collections.emptyList();
	}

	public List<Class<? extends SPObject>> getAllowedChildTypes() {
		return allowedChildTypes;
	}

	@Override
	protected void populateImpl() throws SQLObjectException {
		// No operation.
	}

	@Override @Transient @Accessor
	public String getShortDisplayName() {
		return getName();
	}

	@Override
	public boolean allowsChildren() {
		return false;
	}

	@Override
	public List<? extends SQLObject> getChildrenWithoutPopulating() {
		return Collections.emptyList();
	}

	@Override
	protected boolean removeChildImpl(SPObject child) {
		return false;
	}
	
	/**
	 * The name of an enumeration should never be null.
	 */
	@Override @Mutator
	public void setName(@Nonnull String name) {
		super.setName(name);
	}

	/**
	 * This method has been overridden to return a {@link SQLObject} parent.
	 */
	@Override @Accessor
	public SQLObject getParent() {
		return (SQLObject) super.getParent();
	}

	/**
	 * Because we constrained the return type on getParent there needs to be a
	 * setter that has the same constraint otherwise the reflection in the undo
	 * events will not find a setter to match the getter and won't be able to
	 * undo parent property changes.
	 */
	@Mutator
	public void setParent(SQLObject parent) {
		super.setParent(parent);
	}

}
