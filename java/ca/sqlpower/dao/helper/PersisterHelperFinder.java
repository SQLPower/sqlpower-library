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
	 * All {@link SPPersisterHelper} classes will be placed in a package with 
	 * this name under the package where the class the helper makes.
	 */
	public static final String GENERATED_PACKAGE_NAME = "generated";

	/**
	 * Returns a new instance of the persister helper for the given class. At
	 * current all persisters are located in the
	 * ca.sqlpower.dao.helper.generated package but this will change.
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
		String className;
		if (persistClass.getSimpleName().indexOf("$") == -1) {
			className = persistClass.getSimpleName();
		} else {
			className = persistClass.getSimpleName().substring(persistClass.getSimpleName().lastIndexOf("$") + 1);
		}
		String persisterClassName = persistClass.getPackage().getName() + "." + GENERATED_PACKAGE_NAME + "." + className + "PersisterHelper";
		Class<?> persisterClass = PersisterHelperFinder.class.getClassLoader().loadClass(persisterClassName);
		return (SPPersisterHelper<? extends SPObject>) persisterClass.newInstance();
	}
	
	/**
	 * Returns a new instance of the persister helper for the given class. At
	 * current all persisters are located in the
	 * ca.sqlpower.dao.helper.generated package but this will change.
	 * 
	 * @param type
	 *            The new persister helper will create and modify objects of
	 *            this type. This must be the fully qualified class name.
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
			String type) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
		String persisterClassName = getPersisterHelperClassName(type);
		Class<?> persisterClass = PersisterHelperFinder.class.getClassLoader().loadClass(persisterClassName);
		return (SPPersisterHelper<? extends SPObject>) persisterClass.newInstance();
	}
	
	/**
	 * Returns the fully qualified class name of the persister helper for the fully
	 * qualified class name given. This works for both {@link SPPersisterHelper}s 
	 * and the abstract helper helpers.
	 */
	public static String getPersisterHelperClassName(String classNameToPersist) {
		String packageName = classNameToPersist.substring(0, classNameToPersist.lastIndexOf("."));
		String className = classNameToPersist.substring(classNameToPersist.lastIndexOf(".") + 1, classNameToPersist.length());
		if (className.indexOf("$") != -1) {
			className = className.substring(className.lastIndexOf("$") + 1);
		}
		String persisterClassName = packageName + "." + GENERATED_PACKAGE_NAME + "." + className + "PersisterHelper";
		return persisterClassName;
	}
	
	private PersisterHelperFinder() {
		//static methods only
	}
}
