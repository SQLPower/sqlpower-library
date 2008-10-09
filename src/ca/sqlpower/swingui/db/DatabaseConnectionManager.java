/*
 * Copyright (c) 2007, SQL Power Group Inc.
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

package ca.sqlpower.swingui.db;

import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
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
import ca.sqlpower.swingui.Messages;
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
	 * A property key that can be set with a value for any additional actions passed into the
	 * DatabaseConnectionManager constructor. If you set the Action to have a value of Boolean.TRUE
	 * with this key, then the DatabaseConnectionManager will disable the corresponding button
	 * it creates in the GUI for that action.
	 */
	public static final String DISABLE_IF_NO_CONNECTION_SELECTED = "disableIfNoConnectionSelected"; //$NON-NLS-1$
	
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
    
    private final DataSourceTypeDialogFactory dsTypeDialogFactory; 
    
	private final Action jdbcDriversAction = new AbstractAction(Messages.getString("DatabaseConnectionManager.jdbcDriversActionName")){ //$NON-NLS-1$

		public void actionPerformed(ActionEvent e) {
			dsTypeDialogFactory.showDialog(DatabaseConnectionManager.this.currentOwner);
		}
	};

    private final Action newDatabaseConnectionAction = new AbstractAction(Messages.getString("DatabaseConnectionManager.newDbConnectionActionName")) { //$NON-NLS-1$

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

	private final Action editDatabaseConnectionAction = new AbstractAction(Messages.getString("DatabaseConnectionManager.editDbConnectionActionName")) { //$NON-NLS-1$

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
                                dsTable.repaint();
                                break;
                            }
                        }
                    } catch (Exception ex) {
                        SPSUtils.showExceptionDialogNoReport(
                                d,
                                "Unexpected exception while editing a database connection.", //$NON-NLS-1$
                                ex);
                    }
                }
            };

            dsDialogFactory.showDialog(d, ds, onOk);
		}
	};

	private final Action removeDatabaseConnectionAction = new AbstractAction(Messages.getString("DatabaseConnectionManager.removeDbConnectionActionName")) { //$NON-NLS-1$

		public void actionPerformed(ActionEvent e) {
			int selectedRow = dsTable.getSelectedRow();
			if (selectedRow == -1) {
				return;
			}
			SPDataSource dbcs = (SPDataSource) dsTable.getValueAt(selectedRow,0);
			int option = JOptionPane.showConfirmDialog(
					d,
					Messages.getString("DatabaseConnectionManager.deleteDbConnectionConfirmation", dbcs.getName()), //$NON-NLS-1$
					Messages.getString("DatabaseConnectionManager.removeButton"), //$NON-NLS-1$
					JOptionPane.YES_NO_OPTION);
			if (option != JOptionPane.YES_OPTION) {
				return;
			}
			dsCollection.removeDataSource(dbcs);
		}
	};

	

	private final Action closeAction = new AbstractAction(Messages.getString("DatabaseConnectionManager.closeActionName")) { //$NON-NLS-1$
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

	private List<JButton> additionalActionButtons = new ArrayList<JButton>();
	
	/**
	 * Creates a new database connection manager with the default data source
	 * and data source type dialog factories.
	 * 
	 * @param dsCollection The data source collection to manage
	 */
	public DatabaseConnectionManager(DataSourceCollection dsCollection) {
	    this(dsCollection,
	            new DefaultDataSourceDialogFactory(),
	            new DefaultDataSourceTypeDialogFactory(),
	            (List<Action>) Collections.EMPTY_LIST);
	}
	/**
	 * Creates a new database connection manager with the default set of action buttons, plus
	 * those supplied in the given list.
	 */
	public DatabaseConnectionManager(DataSourceCollection dsCollection, DataSourceDialogFactory dsDialogFactory,
			DataSourceTypeDialogFactory dsTypeDialogFactory, List<Action> additionalActions) {
        this.dsCollection = dsCollection;
        this.dsDialogFactory = dsDialogFactory;
		this.dsTypeDialogFactory = dsTypeDialogFactory;
        panel = createPanel(additionalActions);
	}

	/**
	 * Creates a new database connection manager with the default set of action buttons.
	 * 
	 * @param dsCollection The data source collection to manage
	 * @param dsDialogFactory The factory that this manager will use to create all DataSource editor dialogs.
	 */
	@SuppressWarnings("unchecked")
	public DatabaseConnectionManager(DataSourceCollection dsCollection, DataSourceDialogFactory dsDialogFactory,
			DataSourceTypeDialogFactory dsTypeDialogFactory) {
		this(dsCollection, dsDialogFactory, dsTypeDialogFactory, Collections.EMPTY_LIST);
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
        	d.setVisible(true);  // even if the dialog is already visible, this brings it to the front and gives it focus
            d.requestFocus();    // this will rob focus from the previous focus owner
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
                    "Owner has to be a Frame or Dialog.  You provided a " + //$NON-NLS-1$
                    (owner == null ? null : owner.getClass().getName()));
        }

        currentOwner = owner;
        d.setTitle(Messages.getString("DatabaseConnectionManager.dialogTitle")); //$NON-NLS-1$
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
				"6dlu, fill:min(160dlu;default):grow, 6dlu, pref, 6dlu", // columns //$NON-NLS-1$
				" 6dlu,10dlu,6dlu,fill:min(180dlu;default):grow,10dlu"); // rows //$NON-NLS-1$

		layout.setColumnGroups(new int [][] { {1,3,5}});
		CellConstraints cc = new CellConstraints();

		PanelBuilder pb;
		JPanel p = logger.isDebugEnabled()  ? new FormDebugPanel(layout) : new JPanel(layout);
		pb = new PanelBuilder(layout,p);
		pb.setDefaultDialogBorder();

		pb.add(new JLabel(Messages.getString("DatabaseConnectionManager.availableDbConnections")), cc.xy(2, 2)); //$NON-NLS-1$

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
		
		removeDatabaseConnectionAction.setEnabled(false);
		editDatabaseConnectionAction.setEnabled(false);

		bsb.addUnrelatedGap();
		JButton jdbcDriversButton = new JButton(jdbcDriversAction);
		bsb.addGridded(jdbcDriversButton);

		for (Action a : additionalActions) {
			bsb.addUnrelatedGap();
			JButton b = new JButton(a);
			Object disableValue = a.getValue(DISABLE_IF_NO_CONNECTION_SELECTED);
			if (disableValue instanceof Boolean && disableValue.equals(Boolean.TRUE)) {
				b.setEnabled(false);
			}
			additionalActionButtons.add(b);
			bsb.addGridded(b);
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
			return Messages.getString("DatabaseConnectionManager.connectionName"); //$NON-NLS-1$
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
			
			for (JButton b : additionalActionButtons) {
				Object disableValue = b.getAction().getValue(DISABLE_IF_NO_CONNECTION_SELECTED);
				if (disableValue instanceof Boolean && disableValue.equals(Boolean.TRUE)) {
					if (getSelectedConnection() == null) {
						b.setEnabled(false);
					} else {
						b.setEnabled(true);
					}
				}
			}
			
			if (getSelectedConnection() == null) {
				removeDatabaseConnectionAction.setEnabled(false);
				editDatabaseConnectionAction.setEnabled(false);
			} else {
				removeDatabaseConnectionAction.setEnabled(true);
				editDatabaseConnectionAction.setEnabled(true);
			}
				
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