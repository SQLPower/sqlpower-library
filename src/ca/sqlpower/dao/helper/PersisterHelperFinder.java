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

package ca.sqlpower.dao.helper;

import ca.sqlpower.object.SPObject;

/**
 * This utility class helps find a {@link SPPersisterHelper} for a given class
 * object.
 */
public class PersisterHelperFinder {

	/**
	 * Returns a new instance of the persister helper for the given class. At
	 * current all persisters are located in the
	 * ca.sqlpower.dao.helper.generated package but this will change.
	 * <p>
	 * This will eventually replace the {@link SPPersisterHelperFactory}. See
	 * bug 2697.
	 * 
	 * @param persistClass
	 *            The new persister helper will create and modify objects of
	 *            this type.
	 * @return A new persister helper that will create and modify objects of the
	 *         given type.
	 * @throws ClassNotFoundException
	 *             Thrown if there is no persister helper for the class. This
	 *             should only occur if the helpers have not been generated.
	 * @throws InstantiationException
	 *             Thrown if there is no default constructor for the persister
	 *             helper.
	 * @throws IllegalAccessException
	 *             Thrown if the default constructor is not visible for the
	 *             persister helper.
	 */
	@SuppressWarnings("unchecked")
	public static SPPersisterHelper<? extends SPObject> findPersister(
			Class<? extends SPObject> persistClass) 
			throws ClassNotFoundException, InstantiationException, IllegalAccessException {
		//XXX The package should be the same location as the file see bug 2697
		Class<?> persisterClass = PersisterHelperFinder.class.getClassLoader().loadClass(
				"ca.sqlpower.dao.helper.generated." + persistClass.getSimpleName() + "PersisterHelper");
		return (SPPersisterHelper<? extends SPObject>) persisterClass.newInstance();
	}
	
	private PersisterHelperFinder() {
		//static methods only
	}
}
