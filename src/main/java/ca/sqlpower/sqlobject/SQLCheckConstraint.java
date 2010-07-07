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
 * This class represents a single SQL check constraint. This constraint can be
 * applied at the table, column, data type, or domain level.
 * 
 * i.e. {@link SQLCheckConstraint}s can only be children of either
 * {@link SQLTable}, {@link SQLColumn}, or {@link SQLTypePhysicalProperties}.
 */
public class SQLCheckConstraint extends SQLObject {

	/**
	 * {@link List} of allowed child types, which is empty since
	 * {@link SQLCheckConstraint} has no children.
	 */
	public static final List<Class<? extends SPObject>> allowedChildTypes = 
		Collections.emptyList();
	
	/**
	 * @see #getConstraint()
	 */
	private String constraint;

	/**
	 * Creates a new {@link SQLCheckConstraint}.
	 * 
	 * @param name
	 *            The name of the constraint. Note that this name must be unique
	 *            among its parent's list of {@link SQLCheckConstraint}
	 *            children, as well as the list of {@link SQLCheckConstraint}s
	 *            inherited from upstream types (if any).
	 * @param constraint
	 *            The condition for the constraint.
	 */
	@Constructor
	public SQLCheckConstraint(
			@ConstructorParameter(propertyName="name") @Nonnull String name, 
			@ConstructorParameter(propertyName="constraint") @Nonnull String constraint) {
		setName(name);
		this.constraint = constraint;
	}

	/**
	 * Copy constructor. Creates a copy of a given {@link SQLCheckConstraint}
	 * with the same name and check constraint condition.
	 * 
	 * @param constraint
	 *            The {@link SQLCheckConstraint} to copy from.
	 */
	public SQLCheckConstraint(@Nonnull SQLCheckConstraint constraint) {
		this(constraint.getName(), constraint.getConstraint());
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
	 * Returns the {@link String} check constraint condition. 
	 */
	@Accessor(isInteresting=true)
	public String getConstraint() {
		return constraint;
	}

	/**
	 * Sets the check constraint condition.
	 * 
	 * @param constraint
	 *            The {@link String} condition for the check constraint.
	 */
	@Mutator
	public void setConstraint(String constraint) {
		String oldConstraint = this.constraint;
		this.constraint = constraint;
		firePropertyChange("constraint", oldConstraint, constraint);
	}

	/**
	 * The name of a constraint should never be null to properly uniquely
	 * identify them among a collection of constraints on a shared parent
	 * {@link SQLObject}.
	 */
	@Override @Mutator
	public void setName(@Nonnull String name) {
		super.setName(name);
	}

	/**
	 * Overriding this method to restrict parent types to be {@link SQLObject}.
	 * Technically, this should only either be {@link SQLTable},
	 * {@link SQLColumn}, or {@link SQLTypePhysicalProperties}. Applying check
	 * constraints on any other level does not make sense.
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
