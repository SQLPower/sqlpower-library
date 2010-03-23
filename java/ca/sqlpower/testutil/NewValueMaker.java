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

package ca.sqlpower.testutil;

/**
 * An interface for creating new value by leveraging existing synergies and
 * exploiting value in niche market share by proactively invoking unique value
 * propositions. Let's touch base.
 * <p>
 * Alternate description: This is an interface that takes a type specification
 * and an optional existing instance of that type, and provides a different
 * instance of that type. It's very useful when building generic tests for
 * JavaBeans property changes.
 */
public interface NewValueMaker {

    /**
     * Creates a new value of type T which is not equal to the given oldValue.
     * 
     * @param valueType
     *            The Class object that represents the type of oldVal
     * @param oldVal
     *            The existing value, which must either be null or of type
     *            valueType.
     * @param propName
     *            The name of the property this value is being generated for.
     *            This parameter is only used when generating exception
     *            messages, but it's very useful to have when an exception gets
     *            thrown!
     * @return A new, non-null value of type valueType.
     */
    public Object makeNewValue(Class<?> valueType, Object oldVal, String propName);
}
