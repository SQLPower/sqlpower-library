/*
 * Copyright (c) 2007, SQL Power Group Inc.
 *
 * This file is part of Power*MatchMaker.
 *
 * Power*MatchMaker is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * Power*MatchMaker is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>. 
 */

package ca.sqlpower.swingui.db;

import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Collections;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

import org.apache.log4j.Logger;

import ca.sqlpower.sql.DataSourceCollection;
import ca.sqlpower.sql.DatabaseListChangeEvent;
import ca.sqlpower.sql.DatabaseListChangeListener;
import ca.sqlpower.sql.SPDataSource;
import ca.sqlpower.swingui.SPSUtils;

import com.jgoodies.forms.builder.ButtonStackBuilder;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.debug.FormDebugPanel;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

/**
 * The database connection manager is a GUI facility for managing a DataSourceCollection.
 * It allows users to add, edit, and delete database connection specs.
 */
public class DatabaseConnectionManager {

	private static Logger logger = Logger.getLogger(DatabaseConnectionManager.class);

    /**
     * The GUI panel.  Lives inside the dialog {@link #d}.
     */
    private final JPanel panel;

    /**
     * The Dialog that contains all the GUI;
     */
    private JDialog d;

    /**
     * The current owner of the dialog.  Gets updated in the showDialog() method.
     */
    private Window currentOwner;

    private final DataSourceDialogFactory dsDialogFactory;
    
	private final Action jdbcDriversAction = new AbstractAction("JDBC Drivers"){

		public void actionPerformed(ActionEvent e) {
			// TODO Auto-generated method stub
            JOptionPane.showMessageDialog(d,
            		"This action is not implemented yet.");
		}
	};

    private final Action newDatabaseConnectionAction = new AbstractAction("New...") {

        public void actionPerformed(ActionEvent e) {
            final SPDataSource ds = new SPDataSource(getPlDotIni());
            Runnable onOk = new Runnable() {
                public void run() {
                    dsCollection.addDataSource(ds);
                }
            };
            dsDialogFactory.showDialog(d, ds, onOk);
        }
    };

	private final Action editDatabaseConnectionAction = new AbstractAction("Edit...") {

		public void actionPerformed(ActionEvent e) {
			int selectedRow = dsTable.getSelectedRow();
			if (selectedRow == -1) {
				return;
			}
			final SPDataSource ds = (SPDataSource) dsTable.getValueAt(selectedRow,0);
			
            Runnable onOk = new Runnable() {
                public void run() {
                    try {
                        for (int i = 0; i < dsTable.getRowCount(); i++) {
                            if (dsTable.getValueAt(i, 0) == ds) {
                                dsTable.setRowSelectionInterval(i, i);
                                dsTable.scrollRectToVisible(dsTable.getCellRect(i, 0, true));
                                break;
                            }
                        }
                    } catch (Exception ex) {
                        SPSUtils.showExceptionDialogNoReport(
                                d,
                                "Unexpected exception while editing a database connection.",
                                ex);
                    }
                }
            };

            dsDialogFactory.showDialog(d, ds, onOk);
		}
	};

	private final Action removeDatabaseConnectionAction = new AbstractAction("Remove") {

		public void actionPerformed(ActionEvent e) {
			int selectedRow = dsTable.getSelectedRow();
			if (selectedRow == -1) {
				return;
			}
			SPDataSource dbcs = (SPDataSource) dsTable.getValueAt(selectedRow,0);
			int option = JOptionPane.showConfirmDialog(
					d,
					"Do you want to delete this database connection? ["+dbcs.getName()+"]",
					"Remove",
					JOptionPane.YES_NO_OPTION);
			if (option != JOptionPane.YES_OPTION) {
				return;
			}
			dsCollection.removeDataSource(dbcs);
		}
	};

	

	private final Action closeAction = new AbstractAction("Close") {
		public void actionPerformed(ActionEvent e) {
			d.dispose();
		}
	};

    /**
     * The table that contains the list of all data sources in the
     * user's collection of data sources.
     */
	private JTable dsTable;
    
    /**
     * The data source collection of the session context this connection
     * manager belongs to.
     */
	private final DataSourceCollection dsCollection;

	/**
	 * Creates a new database connection manager with the default set of action buttons, plus
	 * those supplied in the given list.
	 */
	public DatabaseConnectionManager(DataSourceCollection dsCollection, DataSourceDialogFactory dsDialogFactory, List<Action> additionalActions) {
        this.dsCollection = dsCollection;
        this.dsDialogFactory = dsDialogFactory;
		panel = createPanel(additionalActions);
	}

	/**
	 * Creates a new database connection manager with the default set of action buttons.
	 * 
	 * @param dsCollection The data source collection to manage
	 * @param dsDialogFactory The factory that this manager will use to create all DataSource editor dialogs.
	 */
	@SuppressWarnings("unchecked")
	public DatabaseConnectionManager(DataSourceCollection dsCollection, DataSourceDialogFactory dsDialogFactory) {
		this(dsCollection, dsDialogFactory, Collections.EMPTY_LIST);
	}

    /**
     * Makes sure this database connection manager is visible,
     * focused, and in a dialog owned by the given owner.
     *
     * @param owner the Frame or Dialog that should own the
     *              DatabaseConnectionManager dialog.
     */
    public void showDialog(Window owner) {
        if (d != null && d.isVisible() && currentOwner == owner) {
            d.requestFocus();
            return;
        }

        if (d != null) {
            d.dispose();
        }
        if (panel.getParent() != null) {
            panel.getParent().remove(panel);
        }
        if (owner instanceof Dialog) {
            d = new JDialog((Dialog) owner);
        } else if (owner instanceof Frame) {
            d = new JDialog((Frame) owner);
        } else {
            throw new IllegalArgumentException(
                    "Owner has to be a Frame or Dialog.  You provided a " +
                    (owner == null ? null : owner.getClass().getName()));
        }

        currentOwner = owner;
        d.setTitle("Database Connection Manager");
        d.getContentPane().add(panel);
        d.pack();
        d.setLocationRelativeTo(owner);
        SPSUtils.makeJDialogCancellable(d, closeAction);
        d.setVisible(true);
        d.requestFocus();
    }

    /**
     * Closes the current dialog. It is safe to call this even if the dialog is not visible.
     */
	public void closeDialog() {
		if (d != null) {
			d.dispose();
		}
	}
	
	
	private JPanel createPanel(List<Action> additionalActions) {

		FormLayout layout = new FormLayout(
				"6dlu, fill:min(160dlu;default):grow, 6dlu, pref, 6dlu", // columns
				" 6dlu,10dlu,6dlu,fill:min(180dlu;default):grow,10dlu"); // rows

		layout.setColumnGroups(new int [][] { {1,3,5}});
		CellConstraints cc = new CellConstraints();

		PanelBuilder pb;
		JPanel p = logger.isDebugEnabled()  ? new FormDebugPanel(layout) : new JPanel(layout);
		pb = new PanelBuilder(layout,p);
		pb.setDefaultDialogBorder();

		pb.add(new JLabel("Available Database Connections:"), cc.xy(2, 2));

		TableModel tm = new ConnectionTableModel(dsCollection);
		dsTable = new JTable(tm);
		dsTable.setTableHeader(null);
		dsTable.setShowGrid(false);
		dsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		dsTable.addMouseListener(new DSTableMouseListener());

		JScrollPane sp = new JScrollPane(dsTable);

		pb.add(sp, cc.xy(2, 4));

		ButtonStackBuilder bsb = new ButtonStackBuilder();

		bsb.addGridded(new JButton(newDatabaseConnectionAction));
		bsb.addRelatedGap();
		bsb.addGridded(new JButton(editDatabaseConnectionAction));
		bsb.addRelatedGap();
		bsb.addGridded(new JButton(removeDatabaseConnectionAction));

		bsb.addUnrelatedGap();
		JButton jdbcDriversButton = new JButton(jdbcDriversAction);
		bsb.addGridded(jdbcDriversButton);

		for (Action a : additionalActions) {
			bsb.addUnrelatedGap();
			bsb.addGridded(new JButton(a));
		}

        bsb.addUnrelatedGap();
		bsb.addGridded(new JButton(closeAction));

		pb.add(bsb.getPanel(), cc.xy(4,4));
		return pb.getPanel();

	}

	private class ConnectionTableModel extends AbstractTableModel {

		public ConnectionTableModel(DataSourceCollection ini) {
			super();
			if ( ini != null ) {
				ini.addDatabaseListChangeListener(new DatabaseListChangeListener(){
					public void databaseAdded(DatabaseListChangeEvent e) {
						fireTableDataChanged();
					}

					public void databaseRemoved(DatabaseListChangeEvent e) {
						fireTableDataChanged();
					}
				});
			}
		}

		public int getRowCount() {
			return dsCollection == null?0:dsCollection.getConnections().size();
		}

		public int getColumnCount() {
			return 1;
		}

		@Override
		public String getColumnName(int columnIndex) {
			return "Connection Name";
		}

		@Override
		public Class<?> getColumnClass(int columnIndex) {
			return SPDataSource.class;
		}

		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex) {
			return false;
		}

		public Object getValueAt(int rowIndex, int columnIndex) {
			return dsCollection == null?null:dsCollection.getConnections().get(rowIndex);
		}

	}

	public DataSourceCollection getPlDotIni() {
		return dsCollection;
	}

	private class DSTableMouseListener implements MouseListener {

		public void mouseClicked(MouseEvent evt) {
            if (evt.getClickCount() == 2) {
            	editDatabaseConnectionAction.actionPerformed(null);
            }
        }

		public void mousePressed(MouseEvent e) {
			// we don't care
		}

		public void mouseReleased(MouseEvent e) {
			// we don't care
		}

		public void mouseEntered(MouseEvent e) {
			// we don't care
		}

		public void mouseExited(MouseEvent e) {
			// we don't care
		}

	}
	/**
	 * Returns the first selected spdatasource object from the list.
	 * Returns null if there are not any selected data sources
	 */
	public SPDataSource getSelectedConnection() {
		int selectedRow = dsTable.getSelectedRow();
		if (selectedRow == -1) {
			return null;
		}
		return (SPDataSource) dsTable.getValueAt(selectedRow,0);
	}


}