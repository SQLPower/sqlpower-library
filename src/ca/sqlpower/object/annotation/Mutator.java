/*
 * Copyright (c) 2009, SQL Power Group Inc.
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

package ca.sqlpower.object.annotation;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import ca.sqlpower.dao.helper.SPPersisterHelper;
import ca.sqlpower.object.SPObject;

/**
 * This {@link Annotation} defines a setter method in an {@link SPObject} class
 * as a mutator for a persistable property. The setter method name must be in
 * JavaBean format. Every persistable property should have a respective
 * annotated setter and getter method. Thus, there should be an equal number of
 * {@link Accessor} and {@link Mutator} annotations within each {@link SPObject}
 * class. The {@link SPAnnotationProcessorFactory} should recognize this
 * annotation as a supported annotation type by the
 * {@link SPAnnotationProcessorFactory#supportedAnnotationTypes()} method. The
 * {@link SPAnnotationProcessor} needs to validate that a given method in
 * {@link SPObject} is tagged with this annotation before proceeding to generate
 * source code for the
 * {@link SPPersisterHelper#commitProperty(SPObject, String, Object, ca.sqlpower.dao.session.SessionPersisterSuperConverter)}
 * method.
 * <p>
 * This annotation is retained at runtime for the benefits of tests only. There
 * will be no need to access this annotation at runtime when the app is running.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Mutator {

	/**
	 * If true this mutator is only allowed to be used shortly after the object is
	 * constructed. These types of setters define properties that would normally
	 * be final but are not due to implementation restrictions.
	 * <p>
	 * This flag is mainly used for testing.
	 */
	boolean constructorMutator() default false;
}
