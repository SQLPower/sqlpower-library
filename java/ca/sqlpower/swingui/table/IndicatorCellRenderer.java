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

import java.awt.Color;
import java.awt.Component;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

public class IndicatorCellRenderer extends DefaultTableCellRenderer  implements FormatFactory {

    Color success = new Color(255,255,255);
    Color warning = new Color(100,200,200);
    Color failed = new Color(255,150,150);
    Color unknown = new Color(200,200,200);

    public IndicatorCellRenderer() {
    }

    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {

        if (value == null) {
            setBackground(unknown);
        } else if (value instanceof String) {
            if ( ((String)value).equalsIgnoreCase("success") ) {
                setBackground(success);
            } else if ( ((String)value).equalsIgnoreCase("failed") ) {
                setBackground(failed);
            } else {
                setBackground(warning);
            }
        } else {
            setBackground(unknown);
        }
        return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
    }

    public Format fakeFormatter = new Format() {

        @Override
        public StringBuffer format(Object value, StringBuffer toAppendTo, FieldPosition pos) {
            return toAppendTo.append(value.toString());
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

