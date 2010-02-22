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
 * This {@link Annotation} defines a getter method in an {@link SPObject} class
 * as an accessor for a persistable property. The getter method name must be in
 * a JavaBean format. Every persistable property should have a respective
 * annotated setter and getter method. Thus, there should be an equal number of
 * {@link Accessor} and {@link Mutator} annotations within each {@link SPObject}
 * class. The {@link SPAnnotationProcessorFactory} should recognize this
 * annotation as a supported annotation type by the
 * {@link SPAnnotationProcessorFactory#supportedAnnotationTypes()} method. The
 * {@link SPAnnotationProcessor} needs to validate that a given method in
 * {@link SPObject} is tagged with this annotation before proceeding to generate
 * source code for the
 * {@link SPPersisterHelper#findProperty(SPObject, String, ca.sqlpower.dao.session.SessionPersisterSuperConverter)}
 * method.
 * <p>
 * This annotation is retained at runtime for the benefits of tests only. There
 * will be no need to access this annotation at runtime when the app is running.
 */
@Target(ElementType.METHOD) 
@Retention(RetentionPolicy.RUNTIME)
public @interface Accessor {

    /**
     * Set to true if the user would be interested in knowing about this
     * property when we are displaying diffs between objects.
     */
    boolean isInteresting() default false;
    
	/**
	 * Defines whether the property the annotated getter method returns should
	 * be persisted if its value is null. A false value means the property
	 * should not be persisted if its value is null. There may be cases where a
	 * certain property is null and should not be persisted. For example, the
	 * currentCube property in OlapQuery in Wabit. Thus the getCurrentCube()
	 * method for OlapQuery should be annotated as
	 * <code>@Accessor(persistOnlyIfNonNull = true)</code>. By default, this is
	 * false.
	 */
	boolean persistOnlyIfNonNull() default false;

	/**
	 * Defines the additional properties that this property depends on when
	 * persisting to an {@link SPPersister}, for converting from a complex to
	 * basic type. For example, the OlapQuery's currentCube property in Wabit
	 * requires olapDataSource in order to persist correctly. Thus the
	 * getCurrentCube() method for OlapQuery should be annotated as
	 * <code>@Accessor(additionalInfo = {"currentCube"})</code>.
	 * 
	 * @see SessionPersisterSuperConverter#convertToBasicType(Object, Object...)
	 */
	String[] additionalInfo() default {};
	
}
