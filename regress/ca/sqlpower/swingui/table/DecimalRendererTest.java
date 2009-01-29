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

import java.math.BigDecimal;

import javax.swing.JLabel;

import ca.sqlpower.swingui.table.BaseRendererTest;
import ca.sqlpower.swingui.table.DecimalTableCellRenderer;

public class DecimalRendererTest extends BaseRendererTest {

    public void test1() {

        DecimalTableCellRenderer fmt = new DecimalTableCellRenderer();
        JLabel renderer = (JLabel) fmt.getTableCellRendererComponent(table, 1.2345, false, false, 0, 0);
        String renderedValue = renderer.getText();
        assertEquals("renderer formatted OK", "1.2", renderedValue);

        renderer = (JLabel) fmt.getTableCellRendererComponent(table, new BigDecimal(111111111.11), false, false, 0, 0);
        renderedValue = renderer.getText();
        // This test is not Locale-specific because the format is hard-coded in DecimalRenderer
        assertEquals("renderer formatted OK", "111,111,111.1", renderedValue);

        renderer = (JLabel) fmt.getTableCellRendererComponent(table, null, false, false, 0, 0);
        renderedValue = renderer.getText();
        assertEquals("renderer formatted OK", "null", renderedValue);
    }
}
