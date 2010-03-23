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
import java.lang.annotation.Target;

import ca.sqlpower.dao.SPPersister;

/**
 * This {@link Annotation} defines a parameter of a setter method annotated with
 * {@link Mutator}. This annotation should only be used on the second parameter
 * onwards. That is, the first parameter should be the value to set the property
 * as and not be annotated. The annotated parameter must be of a primitive or
 * {@link String} data type. The {@link SPAnnotationProcessorFactory} should
 * recognize this annotation as a supported annotation type by
 * {@link SPAnnotationProcessorFactory#supportedAnnotationTypes()}.
 */
@Target(ElementType.PARAMETER)
public @interface MutatorParameter {

	/**
	 * This should be a {@link String} representation of the primitive or
	 * {@link String} value to be used by session {@link SPPersister}s when
	 * passing in setter arguments to set the property.
	 */
	String value() default "";
	
}
