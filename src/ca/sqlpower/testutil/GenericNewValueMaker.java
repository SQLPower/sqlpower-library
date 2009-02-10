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

import java.awt.Font;
import java.io.File;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;

import ca.sqlpower.sql.PlDotIni;
import ca.sqlpower.sql.SPDataSource;
import ca.sqlpower.sqlobject.SQLColumn;
import ca.sqlpower.sqlobject.SQLIndex;
import ca.sqlpower.sqlobject.SQLObjectException;
import ca.sqlpower.util.DefaultUserPrompter;
import ca.sqlpower.util.UserPrompter;
import ca.sqlpower.util.UserPrompter.UserPromptResponse;

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
        } else if (valueType == SQLColumn.class) {
        	newVal = new SQLColumn();
        	((SQLColumn) newVal).setName("testing!");
        } else if (valueType == SQLIndex.class) {
        	newVal = new SQLIndex();
        	((SQLIndex) newVal).setName("a new index");
        } else if (valueType.isAssignableFrom(Throwable.class)) {
        	newVal = new SQLObjectException("Test Exception");
        } else if (valueType == UserPrompter.class) {
            newVal = new DefaultUserPrompter(UserPromptResponse.CANCEL, null);
        } else {
            throw new RuntimeException(
                    "This new value maker doesn't handle type " + valueType.getName() +
                    " (for property " + propName + ")");
        }

        return newVal;
    }

    
}
