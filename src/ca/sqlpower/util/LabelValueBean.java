/*
 * $Header$
 *
 * ====================================================================
 *
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999-2001 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "The Jakarta Project", "Struts", and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 * This package contains code modified by employees of SQL Power Group
 * Inc.
 */


package ca.sqlpower.util;

import java.util.*;
import java.text.*;

/**
 * Simple JavaBean to represent label-value pairs for use in collections
 * that are utilized by the <code>&lt;form:options&gt;</code> tag.
 *
 * NOTE: This class used to be deprecated in favour of the
 * LabelValueBean in org.apache.struts.util, but it actually has some
 * static methods that we use for building label/value lists of
 * numbers for date forms and the like.  It's still best to use the
 * struts one in struts apps, but it doesn't matter if you use this
 * one instead.
 *
 * @author Craig R. McClanahan, Jonathan Fuerth
 * @version $Revision$ $Date$
 */

public class LabelValueBean implements java.io.Serializable {

    /**
     * Construct a new LabelValueBean with the specified values.
     *
     * @param label The label to be displayed to the user
     * @param value The value to be returned to the server
     */
    public LabelValueBean(String label, String value) {
        this.label = label;
        this.value = value;
    }

    /**
     * The label to be displayed to the user.
     */
    protected String label = null;

    public String getLabel() {
        return (this.label);
    }

    /**
     * The value to be returned to the server.
     */
    protected String value = null;

    public String getValue() {
        return (this.value);
    }

    /**
     * Returns a string representation of this object.
     */
    public String toString() {
        StringBuffer sb = new StringBuffer("LabelValueBean[");
        sb.append(this.label);
        sb.append(", ");
        sb.append(this.value);
        sb.append("]");
        return (sb.toString());
    }

    /**
     * Builds a List of label-value beans that can be used to populate
     * a drop-down list of numbers. Intended for web apps that want to
     * make a SELECT list of numbers.
     *
     * @param start The lowest number present in the list.
     * @param end The highest number present in the list. (Must be >= start)
     * @param step The amount that one number differs from the next
     * (must be positive)
     * @param nf A number formatter that will be used to format the label.
     * @return A List of LabelValueBeans as specified.
     */
    public static List getNumericList(int start, int end, int step,
				      NumberFormat nf)
	throws IllegalArgumentException {
	if(start > end) {
	    throw new IllegalArgumentException("Start number ("+start
					       +") must be less than or equal to end number ("
					       +end+").");
	}
	if(step <= 0) {
	    throw new IllegalArgumentException("Step must be positive");
	}

	List nums=new ArrayList(30);
	for(int i=start; i<=end; i+=step) {
	    nums.add(new LabelValueBean(nf.format(i), String.valueOf(i)));
	}
	return nums;
    }

    public static List getNumericList(int start, int end, int step)
	throws IllegalArgumentException {
	return getNumericList(start, end, step, new ca.sqlpower.util.NaanSafeNumberFormat("#"));
    }

    public static List getNumericList(int start, int end, NumberFormat nf)
	throws IllegalArgumentException {
	return getNumericList(start, end, 1, nf);
    }

    public static List getNumericList(int start, int end)
	throws IllegalArgumentException {
	return getNumericList(start, end, new ca.sqlpower.util.NaanSafeNumberFormat("#"));
    }
}
