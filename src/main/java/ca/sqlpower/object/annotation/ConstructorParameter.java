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

import ca.sqlpower.dao.helper.SPPersisterHelper;
import ca.sqlpower.dao.session.SessionPersisterSuperConverter;
import ca.sqlpower.object.SPObject;

/**
 * This {@link Annotation} defines a constructor parameter within a constructor
 * annotated with {@link Constructor}. This indicates which JavaBean property
 * the annotated constructor parameter applies to. The
 * {@link SPAnnotationProcessor} should be able to process this annotation to
 * generate the appropriate constructor arguments when creating the
 * {@link SPPersisterHelper#commitObject(java.util.Collection)} method. The
 * {@link SPAnnotationProcessorFactory} should recognize this annotation as a
 * supported annotation type by
 * {@link SPAnnotationProcessorFactory#supportedAnnotationTypes()}.
 */
@Target(ElementType.PARAMETER)
public @interface ConstructorParameter {

	/**
	 * This enum distinguishes between the types of properties that can be
	 * passed into a constructor as arguments. Each type is documented to fully
	 * explain the type of parameter is supplied.
	 */
	public enum ParameterType {

		/**
		 * This is the default property type. Regular properties must be
		 * convertable by a {@link SessionPersisterSuperConverter} of some kind.
		 * Properties of this type will be converted to a unique value that
		 * allows the object to be identified or created. This unique value will
		 * be simple to allow the object to be persisted but also allow the
		 * object to be retrieved when converted back.
		 * <p>
		 * This includes references to object that exist in the tree.
		 */
		PROPERTY,

		/**
		 * Children must be an {@link SPObject} that is a child of the object to
		 * be constructed. These are different from the reference because the
		 * object cannot be found in the tree until the current object is
		 * created and added to the tree.
		 */
		CHILD;
	}

	/**
	 * Defines the {@link ParameterType} this annotated constructor parameter
	 * maps to. If the type is {@link ParameterType#PROPERTY}, then the
	 * parameter maps onto an {@link SPObject} property, and the property name
	 * is defined by {@link #propertyName()}. Otherwise, if the type is
	 * {@link ParameterType#CHILD}, then the property is an {@link SPObject}
	 * child. By default, this value is {@link ParameterType#PROPERTY}.
	 */
	ParameterType parameterType() default ParameterType.PROPERTY;

	/**
	 * This will be the JavaBean property that will be set to the annotated
	 * constructor parameter value.
	 */
	String propertyName() default "";

}
