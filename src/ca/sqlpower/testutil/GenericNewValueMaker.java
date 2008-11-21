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

import java.awt.Font;
import java.io.File;
import java.text.DecimalFormat;
import java.text.Format;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;

import ca.sqlpower.sql.PlDotIni;
import ca.sqlpower.sql.SPDataSource;

/**
 * An implementation of NewValueMaker that recognizes classes in the Java SE
 * library and the SQL Power library. Apps can extend this class to also provide
 * awareness for their own types that show up in bean properties.
 */
public class GenericNewValueMaker implements NewValueMaker {

    public Object makeNewValue(Class<?> valueType, Object oldVal, String propName) {
        Object newVal;  // don't init here so compiler can warn if the following code doesn't always give it a value
        if (valueType == Integer.TYPE) {
            newVal = ((Integer) oldVal)+1;
        } else if (valueType == Integer.class) {
            if (oldVal == null) {
                newVal = new Integer(1);
            } else {
                newVal = new Integer((Integer)oldVal+1);
            }
        } else if (valueType == String.class) {
            // make sure it's unique
            newVal = "new " + oldVal;
        } else if (valueType == Boolean.TYPE){
            newVal = new Boolean(! ((Boolean) oldVal).booleanValue());
        } else if (valueType == File.class) {
            newVal = new File("temp" + System.currentTimeMillis());
        } else if (valueType == SPDataSource.class) {
            newVal = new SPDataSource(new PlDotIni());
            ((SPDataSource)newVal).setName("Testing data source");
        } else if (valueType == Font.class) {
            newVal = Font.decode("Dialog");
            if (newVal.equals(oldVal)) {
                newVal = ((Font) newVal).deriveFont(((Font) newVal).getSize2D() + 1f);
            }
        }  else if (valueType.isAssignableFrom(NumberFormat.class)) {
        	if(oldVal == NumberFormat.getCurrencyInstance()) {
        		newVal = NumberFormat.getPercentInstance();
        	} else {
        		newVal = NumberFormat.getCurrencyInstance();
        	}
        } else if(valueType.isAssignableFrom(SimpleDateFormat.class)) {
        	if(oldVal == SimpleDateFormat.getDateTimeInstance()) {
        		newVal = SimpleDateFormat.getDateInstance();
        	} else {
        		newVal = SimpleDateFormat.getDateTimeInstance();
        	}
        } else {
            throw new RuntimeException(
                    "This new value maker doesn't handle type " + valueType.getName() +
                    " (for property " + propName + ")");
        }

        return newVal;
    }

    
}
