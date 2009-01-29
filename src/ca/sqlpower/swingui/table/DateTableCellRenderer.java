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
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

public class DateTableCellRenderer extends DefaultTableCellRenderer implements FormatFactory {

    DateFormat df = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus,
            int row, int column) {

        String formattedValue = null;
        if ( value instanceof Long ) {
            formattedValue = df.format(new Date((Long) value));
        } else if ( value instanceof Timestamp ) {
            formattedValue = df.format(new Date(((Timestamp) value).getTime()));
        }
        return super.getTableCellRendererComponent(table, formattedValue, isSelected, hasFocus, row, column);
    }


    public Format fakeFormatter = new Format() {

        @Override
        public StringBuffer format(Object value, StringBuffer toAppendTo, FieldPosition pos) {
            return toAppendTo.append(df.format(value));
        }

        @Override
        public Object parseObject(String source, ParsePosition pos) {
            throw new UnsupportedOperationException("This formatter cannot parse");
        }

    };

    /* (non-Javadoc)
     * @see ca.sqlpowerswingui.table.FormatFactory#getFormat()
     */
    public Format getFormat() {
        return fakeFormatter;
    }
}
