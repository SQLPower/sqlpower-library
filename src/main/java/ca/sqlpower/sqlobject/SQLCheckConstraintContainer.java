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

import java.util.List;

import ca.sqlpower.object.SPObject;

/**
 * This interface defines {@link SQLObject} classes that can either contain
 * {@link SQLCheckConstraint}s as children or have {@link SQLCheckConstraint}s
 * contained by grandchildren.
 */
public interface SQLCheckConstraintContainer extends SPObject {

	/**
	 * Returns the {@link List} of {@link SQLCheckConstraint}s this object
	 * contains. The order of this {@link List} is defined as:
	 * {@link SQLCheckConstraint}s that are contained by the lowest
	 * grandchildren in the hierarchy appear first, {@link SQLCheckConstraint}s
	 * that are contained by direct children of this object appear last.
	 * Depending on the implementing class, this method might not make sense so
	 * an empty list should be returned. In this case, a call to
	 * {@link #getCheckConstraints()} would be better instead.
	 */
	List<SQLCheckConstraint> getCheckConstraints();

	/**
	 * Returns the {@link List} of {@link SQLCheckConstraint}s this object
	 * contains under a specific database platform. The order of this
	 * {@link List} is defined as: {@link SQLCheckConstraint}s that are
	 * contained by the lowest grandchildren in the hierarchy appear first,
	 * {@link SQLCheckConstraint}s that are contained by direct children of this
	 * object appear last. Depending on the implementing class, this method
	 * might not make sense so an empty list should be returned. In this case, a
	 * call to {@link #getCheckConstraints()} would be better instead.
	 * 
	 * @param platform
	 *            The name of the database platform to get the
	 *            {@link SQLCheckConstraint} s from.
	 */
	List<SQLCheckConstraint> getCheckConstraints(String platform);

	/**
	 * Adds a {@link SQLCheckConstraint} to this container. Depending on the
	 * implementing class, this method might either add the constraint to its
	 * {@link List} of children, add the constraint to one of its subcontainers,
	 * or have no effect (if it does not make sense to implement it).
	 * 
	 * @param checkConstraint
	 *            The {@link SQLCheckConstraint} to add.
	 */
	void addCheckConstraint(SQLCheckConstraint checkConstraint);

	/**
	 * Adds a {@link SQLCheckConstraint} to this container to a specific index.
	 * Depending on the implementing class, this method might either add the
	 * constraint to its {@link List} of children, add the constraint to one of
	 * its subcontainers, or have no effect (if it does not make sense to
	 * implement it).
	 * 
	 * @param checkConstraint
	 *            The {@link SQLCheckConstraint} to add.
	 * @param index
	 *            The index in a container's {@link List} of children to add the
	 *            constraint to.
	 */
	void addCheckConstraint(SQLCheckConstraint checkConstraint, int index);

	/**
	 * Adds a {@link SQLCheckConstraint} to this container under a specific
	 * database platform. Depending on the implementing class, this method might
	 * either add the constraint to its {@link List} of children, add the
	 * constraint to one of its subcontainers, or have no effect (if it does not
	 * make sense to implement it.
	 * 
	 * @param platform
	 *            The platform name to add the constraint under.
	 * @param checkConstraint
	 *            The {@link SQLCheckConstraint} to add.
	 */
	void addCheckConstraint(String platform, SQLCheckConstraint checkConstraint);

	/**
	 * Adds a {@link SQLCheckConstraint} to this container under a specific
	 * database platform to a specific index. Depending on the implementing
	 * class, this method might either add the constraint to its {@link List} of
	 * children, add the constraint to one of its subcontainers, or have no
	 * effect (if it does not make sense to implement it.
	 * 
	 * @param platform
	 *            The platform name to add the constraint under.
	 * @param checkConstraint
	 *            The {@link SQLCheckConstraint} to add.
	 * @param index
	 *            The index in a container's {@link List} of children to add the
	 *            constraint to.
	 */
	void addCheckConstraint(String platform, SQLCheckConstraint checkConstraint, int index);

	/**
	 * Removes a {@link SQLCheckConstraint} from this container. Depending on
	 * the implementing class, this method might either remove the constraint
	 * from its {@link List} of children, remove the constraint from one of its
	 * subcontainers, or have no effect (if it does not make sense to implement
	 * it).
	 * 
	 * @param checkConstraint
	 *            The {@link SQLCheckConstraint} to remove.
	 * @return true if the removal was successful.
	 */
	boolean removeCheckConstraint(SQLCheckConstraint checkConstraint);

	/**
	 * Removes a {@link SQLCheckConstraint} from this container under a specific
	 * platform. Depending on the implementing class, this method might either
	 * remove the constraint from its {@link List} of children, remove the
	 * constraint from one of its subcontainers, or have no effect (if it does
	 * not make sense to implement it).
	 * 
	 * @param platform
	 *            The platform name to remove the constraint from.
	 * @param checkConstraint
	 *            The {@link SQLCheckConstraint} to remove.
	 * @return true if the removal was successful.
	 */
	boolean removeCheckConstraint(String platform, SQLCheckConstraint checkConstraint);
	
}
