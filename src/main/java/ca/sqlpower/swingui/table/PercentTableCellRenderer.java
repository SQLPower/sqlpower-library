/*
 * Copyright (c) 2008, SQL Power Group Inc.
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
package ca.sqlpower.swingui.table;

import java.awt.Component;
import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

public class PercentTableCellRenderer extends DefaultTableCellRenderer implements FormatFactory {

    DecimalFormat pctFormat = new DecimalFormat("0.00%");

    /**
     * If false the % sign following the decimal formatted should be removed.
     * This is useful if you are using the format to export and want to do some
     * Maths on the values.
     */
    private final boolean includePercentSign;
    
    public PercentTableCellRenderer() {
        this(true);
    }

    public PercentTableCellRenderer(boolean includePercentSign) {
        this.includePercentSign = includePercentSign;
    }

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
            int row, int column) {

        String formattedValue;
        if (value == null) {
            formattedValue = "N/A";
        } else if (!(value instanceof Number)) {
            throw new IllegalArgumentException("Value must be a Number object");
        } else {
            formattedValue = pctFormat.format(value);
            if (!includePercentSign) {
                formattedValue = formattedValue.replace("%", "");
            }
        }
        return super.getTableCellRendererComponent(table, formattedValue, isSelected, hasFocus, row, column);
    }
    
    public Format fakeFormatter = new Format() {

        @Override
        public StringBuffer format(Object value, StringBuffer toAppendTo, FieldPosition pos) {
            if (value instanceof Number) {
                String formattedValue = pctFormat.format(value);
                if (!includePercentSign) {
                    formattedValue = formattedValue.replace("%", "");
                }
                return toAppendTo.append(formattedValue);
            } else {
                throw new IllegalArgumentException("Value must be a Number object");
            }
        }

        @Override
        public Object parseObject(String source, ParsePosition pos) {
            throw new UnsupportedOperationException("This formatter cannot parse");
        }
        
    };

    public Format getFormat() {
        return fakeFormatter;
    }
}
