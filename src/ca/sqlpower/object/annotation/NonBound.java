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

package ca.sqlpower.object.annotation;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import ca.sqlpower.object.SPObject;

/**
 * This {@link Annotation} defines a getter or setter method in an
 * {@link SPObject} class as a property that does not fire events. If a property
 * is not bound then it is transient by default as an unbound persisted property
 * does not make sense. This annotation is currently for the benefit of testing
 * to describe a method that looks like a setter or getter to be ignored when
 * ensuring all of the getters and setters are annotated. In the future there
 * may be some use for getters and setters of properties that do not fire events
 * and are not transient.
 * <p>
 * This annotation is retained at runtime for the benefits of tests only. There
 * will be no need to access this annotation at runtime when the app is running.
 */
@Target(ElementType.METHOD) 
@Retention(RetentionPolicy.RUNTIME)
public @interface NonBound {

}
