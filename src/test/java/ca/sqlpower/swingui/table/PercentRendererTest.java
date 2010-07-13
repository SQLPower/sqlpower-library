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

import javax.swing.JLabel;

import ca.sqlpower.swingui.table.PercentTableCellRenderer;

public class PercentRendererTest extends BaseRendererTest {

    public void test1() {

        PercentTableCellRenderer fmt = new PercentTableCellRenderer();
        JLabel renderer = (JLabel) fmt.getTableCellRendererComponent(table, 1.2345, false, false, 0, 0);
        String renderedValue = renderer.getText();
        assertEquals("renderer formatted OK", "123.45%", renderedValue);

        renderer = (JLabel) fmt.getTableCellRendererComponent(table, 0.8777, false, false, 0, 0);
        renderedValue = renderer.getText();
        assertEquals("renderer formatted OK", "87.77%", renderedValue);

        renderer = (JLabel) fmt.getTableCellRendererComponent(table, null, false, false, 0, 0);
        renderedValue = renderer.getText();
        assertEquals("renderer formatted OK", "N/A", renderedValue);
    }
}
