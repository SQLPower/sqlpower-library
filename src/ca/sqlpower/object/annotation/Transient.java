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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Each method annotated with {@link Accessor} or {@link Mutator} can be
 * optionally annotated with {@link Transient}. A method marked as transient
 * identifies that the property it returns or modifies should not be persisted
 * and will be ignored by persistence tests and other persistence classes.
 * However, since the method is an {@link Accessor} or {@link Mutator} it must
 * still fire events.
 * <p>
 * This annotation is retained at runtime for the benefits of tests only. There
 * will be no need to access this annotation at runtime when the app is running.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Transient {

}
