/*
 * Copyright (c) 2008, SQL Power Group Inc.
 * 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in
 *       the documentation and/or other materials provided with the
 *       distribution.
 *     * Neither the name of SQL Power Group Inc. nor the names of its
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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
