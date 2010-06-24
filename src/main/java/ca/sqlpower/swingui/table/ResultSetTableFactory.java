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

package ca.sqlpower.swingui.table;

import java.sql.ResultSet;

import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.text.Document;

/**
 * A factory to create JTables. These tables can be sorted by clicking on their
 * headers and export selected rows to files.
 */
public class ResultSetTableFactory {
	
	/**
	 * Returns a JTable to display the result set. The table can be sorted and
	 * selections can be exported.
	 */
	public static JTable createResultSetJtable(ResultSet rs) {
		final ResultSetTableModel model = new ResultSetTableModel(rs);
		final JTable table = new FancyExportableJTable(model);
		final TableModelListener tableListener = new TableModelListener() {
		    public void tableChanged(TableModelEvent e) {
		        table.createDefaultColumnsFromModel();
		    }
		};
		model.addTableModelListener(tableListener);
		return table;
	}
	

	/**
	 * Returns a JTable to display the result set. The table can be sorted and
	 * selections can be exported. The table can also be filtered as you enter
	 * text into the document provided.
	 */
	public static JTable createResultSetJTableWithSearch(ResultSet rs, Document doc) {
		ResultSetTableModel model = new ResultSetTableModel(rs);
		final JTable t = new FancyExportableJTable(model, doc);
		final TableModelListener tableListener = new TableModelListener() {
		    public void tableChanged(TableModelEvent e) {
		        t.createDefaultColumnsFromModel();
		    }
		};
		model.addTableModelListener(tableListener);
		return t;
	}

}
