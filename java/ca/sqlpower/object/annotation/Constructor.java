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

import ca.sqlpower.object.SPObject;

/**
 * This {@link Annotation} defines a constructor in an {@link SPObject} class as
 * the constructor to use when persisting a new object. This annotation must
 * only be applied to one constructor for each {@link SPObject} class. All of
 * the parameters within this annotated constructor must be annotated with
 * {@link ConstructorParameter} as well. The
 * {@link SPAnnotationProcessorFactory} should recognize this annotation as a
 * supported annotation type by
 * {@link SPAnnotationProcessorFactory#supportedAnnotationTypes()}.
 */
@Target(ElementType.CONSTRUCTOR)
public @interface Constructor {

}
