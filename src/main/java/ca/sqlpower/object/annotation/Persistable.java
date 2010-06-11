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
import java.lang.annotation.Inherited;
import java.lang.annotation.Target;

import ca.sqlpower.dao.SPPersister;
import ca.sqlpower.object.SPObject;

/**
 * This {@link Annotation} defines an {@link SPObject} class to be persistable
 * by an {@link SPPersister}. The {@link SPAnnotationProcessorFactory} should
 * recognize this annotation as a supported annotation type by
 * {@link SPAnnotationProcessorFactory#supportedAnnotationTypes()}. The
 * {@link SPAnnotationProcessor} needs to validate that a given {@link SPObject}
 * class is tagged with this annotation before proceeding to generate source
 * code for persister helpers. Since this annotation is {@link Inherited}, any
 * extending class of the annotated {@link SPObject} will also be deemed as
 * {@link Persistable}, so there is no need to annotate subclasses as
 * {@link Persistable}.
 */
@Inherited
@Target(ElementType.TYPE)
public @interface Persistable {

    /**
     * Set to true if the object that is marked as persistable is actually
     * transient.
     */
    boolean isTransient() default false;

}
