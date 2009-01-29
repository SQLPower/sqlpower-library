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

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import javax.swing.JLabel;

public class DateRendererTest extends BaseRendererTest {

    /* Test Column 5, create date */
    public void test5() {
        Calendar c = new GregorianCalendar(1999, 12-1, 31);
        Date d = c.getTime();
        long time = d.getTime();

        DateTableCellRenderer dateRenderer = new DateTableCellRenderer();
        JLabel renderer = (JLabel) dateRenderer.getTableCellRendererComponent(table, time, false, false, 0, 0);
        String renderedValue = renderer.getText();
        assertEquals("renderer formatted OK", "1999-12-31 12:00:00", renderedValue);

        // XXX we don't check for null here because that "can't happen"
        // checkForNull(column);
    }
}
