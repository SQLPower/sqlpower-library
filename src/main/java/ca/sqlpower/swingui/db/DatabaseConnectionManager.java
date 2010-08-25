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

package ca.sqlpower.swingui.db;

import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;

import org.apache.log4j.Logger;

import ca.sqlpower.sql.DataSourceCollection;
import ca.sqlpower.sql.DatabaseListChangeEvent;
import ca.sqlpower.sql.DatabaseListChangeListener;
import ca.sqlpower.sql.JDBCDataSource;
import ca.sqlpower.sql.Olap4jDataSource;
import ca.sqlpower.sql.SPDataSource;
import ca.sqlpower.swingui.Messages;
import ca.sqlpower.swingui.SPSUtils;
import ca.sqlpower.swingui.table.CleanupTableModel;
import ca.sqlpower.swingui.table.EditableJTable;

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
	
	public static final Icon DB_ICON = new ImageIcon(DatabaseConnectionManager.class.getClassLoader().getResource("ca/sqlpower/swingui/db/connection-db-16.png"));
    public static final Icon OLAP_DB_ICON = new ImageIcon(DatabaseConnectionManager.class.getClassLoader().getResource("ca/sqlpower/swingui/db/connection-olap-16.png"));

	/**
	 * A property key that can be set with a value for any additional actions passed into the
	 * DatabaseConnectionManager constructor. If you set the Action to have a value of Boolean.TRUE
	 * with this key, then the DatabaseConnectionManager will disable the corresponding button
	 * it creates in the GUI for that action.
	 */
	public static final String DISABLE_IF_NO_CONNECTION_SELECTED = "disableIfNoConnectionSelected"; //$NON-NLS-1$
	
	/**
	 * A property key that can be set with a value for any additional actions passed into the
	 * DatabaseConnectionManager constructor. This property can be set to SwingConstants values of
	 * CENTER, TOP, or BOTTOM to define the text to be placed at the center, top, or bottom of it's button.
	 */
	public static final String VERTICAL_TEXT_POSITION = "verticalTextPosition"; //$NON-NLS-1$
	
	/**
	 * A property key that can be set with a value for any additional actions passed into the
	 * DatabaseConnectionManager constructor. This property can be set to a default height
	 * for the button.
	 */
	public static final String ADDITIONAL_BUTTON_HEIGHT = "additionalButtonHeight"; //$NON-NLS-1$

	/**
	 * A property key that can be set with a value for any additional actions passed into the
	 * DatabaseConnectionManager constructor. This property can be set to SwingConstants values of
	 * LEFT, RIGHT, CENTER, LEADING, or TRAILING.
	 */
	public static final String HORIZONTAL_TEXT_POSITION = "horizontalTextPosition";
	
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

    /**
     * This tracks the icon currently placed in the table of connections to the
     * left of each database connection. Changing this icon will change the icon
     * displayed in the table. This defaults to an orange database icon.
     */
    private Icon dbIcon = DB_ICON;
    
	private final Action jdbcDriversAction = new AbstractAction(Messages.getString("DatabaseConnectionManager.jdbcDriversActionName")){ //$NON-NLS-1$

		public void actionPerformed(ActionEvent e) {
			
			// Was previously: dsTypeDialogFactory.showDialog((d != null) ? d : DatabaseConnectionManager.this.currentOwner);
			// However, this caused a bug where a ghost dialog was created after the jdbcDialog
			// was closed. The jdbcDialog is now owned by a different window, and this dialog can be selected out from under
			// it. This was the only way of having the jdbcDialog maintain its state between closings.
			
			dsTypeDialogFactory.showDialog(DatabaseConnectionManager.this.currentOwner);
		}
	};
	
	/**
	 * This action will create a new data source and allow the user to decide
	 * what kind of data source to create as well as allow the user to define
	 * data source properties.
	 */
	private class NewConnectionAction extends AbstractAction {

        /**
         * This component is used to define the position where the popup will
         * appear. It should be the same position where the given component is.
         */
        private final JComponent parentComponent;

        public NewConnectionAction(String name, JComponent parentComponent) {
	        super(name);
            this.parentComponent = parentComponent;
	    }

        public void actionPerformed(ActionEvent e) {
            if (creatableDSTypes.size() > 1) {
                final JPopupMenu dsTypeMenu = new JPopupMenu();
                dsTypeMenu.setLocation(parentComponent.getLocationOnScreen());
                Iterator<Class<? extends SPDataSource>> iterator = creatableDSTypes.iterator();
                while (iterator.hasNext()) {
                    final Class<? extends SPDataSource> dsType = iterator.next();
                    AbstractAction newDSAction = new AbstractAction(SPDataSource.getUserFriendlyName(dsType) + "...") {
                        public void actionPerformed(ActionEvent e) {
                            showNewDSDialog(dsType);
                        }
                    };
                    JMenuItem dsItem = new JMenuItem(newDSAction);
                    if (dsType.equals(JDBCDataSource.class)) {
                    	dsItem.setIcon(DB_ICON);
                    } else if (dsType.equals(Olap4jDataSource.class)) {
                    	dsItem.setIcon(OLAP_DB_ICON);
                    }
					dsTypeMenu.add(dsItem);
                }
                dsTypeMenu.show(parentComponent, 0, 0);
            } else {
                Iterator<Class<? extends SPDataSource>> iterator = creatableDSTypes.iterator();
                Class<? extends SPDataSource> dsTypeToCreate = iterator.next();
                showNewDSDialog(dsTypeToCreate);
            }
            
        }
        
        public void showNewDSDialog(Class<? extends SPDataSource> dsTypeToCreate) {
            if (dsTypeToCreate.equals(JDBCDataSource.class)) {
                final JDBCDataSource ds = new JDBCDataSource(getPlDotIni());
                Runnable onOk = new Runnable() {
                    public void run() {
                        dsCollection.addDataSource(ds);
                        dsTable.updateUI();
                    }
                };
                dsDialogFactory.showDialog((d != null) ? d : DatabaseConnectionManager.this.currentOwner, ds, onOk);
            } else if (dsTypeToCreate.equals(Olap4jDataSource.class)) {
                final Olap4jDataSource ds = new Olap4jDataSource(getPlDotIni());
                Runnable onOk = new Runnable() {
                    public void run() {
                        dsCollection.addDataSource(ds);
                        dsTable.updateUI();
                    }
                };
                dsDialogFactory.showDialog((d != null) ? d : DatabaseConnectionManager.this.currentOwner, ds, getPlDotIni(), onOk);
            } else {
                throw new IllegalStateException("Cannot make a new data source of type " + dsTypeToCreate);
            }
        }
	}

	private final Action editDatabaseConnectionAction = new AbstractAction(Messages.getString("DatabaseConnectionManager.editDbConnectionActionName")) { //$NON-NLS-1$

		public void actionPerformed(ActionEvent e) {
			int selectedRow = dsTable.getSelectedRow();
			if (selectedRow == -1) {
				return;
			}
			final SPDataSource ds = (SPDataSource) dsTable.getValueAt(selectedRow,0);
			
			if (ds instanceof JDBCDataSource) {
			    JDBCDataSource jdbcDS = (JDBCDataSource) ds;
			    Runnable onOk = createOnOk(ds);

			    dsDialogFactory.showDialog((d != null) ? d : DatabaseConnectionManager.this.currentOwner, jdbcDS, onOk);
			} else if (ds instanceof Olap4jDataSource) {
			    Olap4jDataSource jdbcDS = (Olap4jDataSource) ds;
			    Runnable onOk = createOnOk(ds);

			    dsDialogFactory.showDialog((d != null) ? d : DatabaseConnectionManager.this.currentOwner, jdbcDS, getPlDotIni(), onOk);
			} else {
			    throw new IllegalStateException("Unknown SPDataSource type in the connection manager. Type is " + ds.getClass());
			}
		}

        private Runnable createOnOk(final SPDataSource ds) {
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
                                (d != null) ? d : DatabaseConnectionManager.this.currentOwner,
                                        "Unexpected exception while editing a database connection.", //$NON-NLS-1$
                                        ex);
                    }
                }
            };
            return onOk;
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
					(d != null) ? d : DatabaseConnectionManager.this.currentOwner,
					Messages.getString("DatabaseConnectionManager.deleteDbConnectionConfirmation", dbcs.getName()), //$NON-NLS-1$
					Messages.getString("DatabaseConnectionManager.removeButton"), //$NON-NLS-1$
					JOptionPane.YES_NO_OPTION);
			if (option != JOptionPane.YES_OPTION) {
				return;
			}
			
			dsCollection.removeDataSource(dbcs);
			dsTable.clearSelection();
			
			for (JButton b : additionalActionButtons) {
				Object disableValue = b.getAction().getValue(DISABLE_IF_NO_CONNECTION_SELECTED);
				if (disableValue instanceof Boolean && disableValue.equals(Boolean.TRUE)) {
					b.setEnabled(false);
				}
			}
			removeDatabaseConnectionAction.setEnabled(false);
			editDatabaseConnectionAction.setEnabled(false);
			
			dsTable.repaint();
			
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
	private final DataSourceCollection<SPDataSource> dsCollection;

	private List<JButton> additionalActionButtons = new ArrayList<JButton>();

	/**
	 * This list contains all of the classes that are able to be created from
	 * the new button.
	 */
    private List<Class<? extends SPDataSource>> creatableDSTypes;
	
	/**
	 * Creates a new database connection manager with the default data source
	 * and data source type dialog factories.
	 * 
	 * @param dsCollection The data source collection to manage
	 */
	public DatabaseConnectionManager(DataSourceCollection<SPDataSource> dsCollection) {
	    this(dsCollection,
	            new DefaultDataSourceDialogFactory(),
	            new DefaultDataSourceTypeDialogFactory(dsCollection),
	            (List<Action>) Collections.EMPTY_LIST);
	}
	
	/**
	 * This constructor allows defining a parent window to start and gives the option to hide the
	 * close button. The main purpose of using this constructor would be to make a db connection
	 * manager that is to be placed in another panel.
	 */
	public DatabaseConnectionManager(DataSourceCollection<SPDataSource> dsCollection, DataSourceDialogFactory dsDialogFactory,
			DataSourceTypeDialogFactory dsTypeDialogFactory, List<Action> additionalActions, List<JComponent> additionalComponents, Window owner, boolean showCloseButton) {
	    this(dsCollection, dsDialogFactory, dsTypeDialogFactory, 
	            additionalActions, additionalComponents, owner, showCloseButton,
	            new ArrayList<Class<? extends SPDataSource>>(Collections.singleton((Class<? extends SPDataSource>) JDBCDataSource.class)));
	}

    /**
     * Using this constructor over the other available constructors allows
     * defining a connection manager that can create {@link SPDataSource} types
     * other than the default {@link JDBCDataSource} type. If a constructor is
     * used other then this one only new {@link JDBCDataSource} types will be
     * able to be constructed although any {@link SPDataSource} type in the list
     * will be editable.
     */
	public DatabaseConnectionManager(DataSourceCollection<SPDataSource> dsCollection, DataSourceDialogFactory dsDialogFactory,
            DataSourceTypeDialogFactory dsTypeDialogFactory, List<Action> additionalActions, 
            List<JComponent> additionalComponents, Window owner, boolean showCloseButton,
            List<Class<? extends SPDataSource>> dsTypes) {
		this.dsCollection = dsCollection;
		this.dsDialogFactory = dsDialogFactory;
		this.dsTypeDialogFactory = dsTypeDialogFactory;
		logger.debug("Window owner is " + owner);
		currentOwner = owner;
		panel = createPanel(additionalActions, additionalComponents, showCloseButton, Messages.getString("DatabaseConnectionManager.availableDbConnections"));
		creatableDSTypes = new ArrayList<Class<? extends SPDataSource>>(dsTypes);
	}
	/**
	 * Creates a new database connection manager with the default set of action buttons, plus
	 * those supplied in the given list.
	 */
	public DatabaseConnectionManager(DataSourceCollection<SPDataSource> dsCollection, DataSourceDialogFactory dsDialogFactory,
			DataSourceTypeDialogFactory dsTypeDialogFactory, List<Action> additionalActions) {
		this(dsCollection, dsDialogFactory, dsTypeDialogFactory, additionalActions, new ArrayList<JComponent>(), null, true);
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
	
	public void setDbIcon(Icon dbIcon) {
        this.dbIcon = dbIcon;
    }
	
	/**
	 * This method returns the main panel in the database connection manager and additionally sets the dialog to be one
	 * that can be passed in. This is required for loading a project when a data source cannot be found. Wabit needs
	 * to pop up a window giving the user an option to skip the datasource, select a datasource or cancel the load
	 * and therefore this method can be used to create the proper panel and give it the proper parent so that it can
	 * then pop up dialogs.
	 */
	public JPanel createPanelStandalone(List<Action> additionalActions, List<JComponent> additionalComponents, boolean showCloseButton, String message, JDialog owner) {
		d = owner;
		return createPanel(additionalActions, additionalComponents, showCloseButton, message);
	}
	
	private JPanel createPanel(List<Action> additionalActions, List<JComponent> additionalComponents, boolean showCloseButton, String message) {

		FormLayout layout = new FormLayout(
				"6dlu, fill:min(160dlu;default):grow, 6dlu, pref, 6dlu", // columns //$NON-NLS-1$
				" 6dlu,10dlu,6dlu,fill:min(180dlu;default):grow,10dlu"); // rows //$NON-NLS-1$

		layout.setColumnGroups(new int [][] { {1,3,5}});
		CellConstraints cc = new CellConstraints();

		PanelBuilder pb;
		JPanel p = logger.isDebugEnabled()  ? new FormDebugPanel(layout) : new JPanel(layout);
		pb = new PanelBuilder(layout,p);
		pb.setDefaultDialogBorder();

		pb.add(new JLabel(message), cc.xyw(2, 2, 3)); //$NON-NLS-1$

		TableModel tm = new ConnectionTableModel(dsCollection);
		dsTable = new EditableJTable(tm);
		dsTable.setTableHeader(null);
		dsTable.setShowGrid(false);
		dsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		dsTable.addMouseListener(new DSTableMouseListener());
		dsTable.setDefaultRenderer(SPDataSource.class, new ConnectionTableCellRenderer());

		JScrollPane sp = new JScrollPane(dsTable);
		sp.getViewport().setBackground(dsTable.getBackground());

		pb.add(sp, cc.xy(2, 4));

		ButtonStackBuilder bsb = new ButtonStackBuilder();

		JButton newButton = new JButton();
		AbstractAction newDatabaseConnectionAction = new NewConnectionAction(Messages.getString("DatabaseConnectionManager.newDbConnectionActionName"), newButton); //$NON-NLS-1$
		newButton.setAction(newDatabaseConnectionAction);
		bsb.addGridded(newButton);
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
			
			Object heightValue = a.getValue(ADDITIONAL_BUTTON_HEIGHT);
			if (heightValue instanceof Integer ) {
				b.setPreferredSize(new Dimension((int) b.getPreferredSize().getWidth(), (Integer) heightValue));
			}
			
			Object verticalTextPos = a.getValue(VERTICAL_TEXT_POSITION);
			if (verticalTextPos instanceof Integer) {
				Integer verticalTextInt = (Integer) verticalTextPos;
				if (verticalTextInt == SwingConstants.TOP || verticalTextInt == SwingConstants.BOTTOM || verticalTextInt == SwingConstants.CENTER) {
					b.setVerticalTextPosition(verticalTextInt);
				}
			}
			
			Object horizontalTextPos = a.getValue(HORIZONTAL_TEXT_POSITION);
			if (horizontalTextPos instanceof Integer) {
				Integer horizontalTextInt = (Integer) horizontalTextPos;
				if (horizontalTextInt == SwingConstants.LEFT || horizontalTextInt == SwingConstants.RIGHT || horizontalTextInt == SwingConstants.CENTER 
						|| horizontalTextInt == SwingConstants.LEADING || horizontalTextInt == SwingConstants.TRAILING) {
					b.setHorizontalTextPosition(horizontalTextInt);
				}
			}
			
			additionalActionButtons.add(b);
			bsb.addFixed(b);
		}
		
		for (JComponent comp : additionalComponents) {
			bsb.addUnrelatedGap();
			bsb.addFixed(comp);
		}

		if (showCloseButton) {
			bsb.addUnrelatedGap();
			bsb.addGridded(new JButton(closeAction));
		}

		pb.add(bsb.getPanel(), cc.xy(4,4));
		return pb.getPanel();

	}

	private static class ConnectionTableModel extends AbstractTableModel implements CleanupTableModel {

		private final DatabaseListChangeListener databaseListChangeListener = new DatabaseListChangeListener(){
			public void databaseAdded(DatabaseListChangeEvent e) {
				fireTableDataChanged();
			}
			
			public void databaseRemoved(DatabaseListChangeEvent e) {
				fireTableDataChanged();
			}
		};
		
		private final DataSourceCollection<SPDataSource> dsCollection;
		
		public ConnectionTableModel(DataSourceCollection<SPDataSource> dsCollection) {
			super();
			this.dsCollection = dsCollection;
			dsCollection.addDatabaseListChangeListener(databaseListChangeListener);
		}

		public int getRowCount() {
			return dsCollection.getConnections().size();
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
			return dsCollection.getConnections().get(rowIndex);
		}

		public void cleanup() {
			dsCollection.removeDatabaseListChangeListener(databaseListChangeListener);
		}

	}
	
	private class ConnectionTableCellRenderer implements TableCellRenderer {

        public Component getTableCellRendererComponent(JTable table,
                Object value, boolean isSelected, boolean hasFocus, int row,
                int column) {
            Component comp = new DefaultTableCellRenderer().getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (comp instanceof JLabel) {
                if (value instanceof JDBCDataSource) {
                    ((JLabel) comp).setIcon(dbIcon);
                } else if (value instanceof Olap4jDataSource) {
                    ((JLabel) comp).setIcon(OLAP_DB_ICON);
                }
            }
            return comp;
        }

	}

	public DataSourceCollection<SPDataSource> getPlDotIni() {
		return dsCollection;
	}

	private class DSTableMouseListener implements MouseListener {

		/**
		 * Updates the state of all buttons when a database connection is clicked on
		 * @param evt  Mouse event
		 */
		private void updateAllButtonStates(MouseEvent evt) {
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
		}
		
		public void mouseClicked(MouseEvent evt) {
			if (evt.getClickCount() == 2) {
            	editDatabaseConnectionAction.actionPerformed(null);
            }
        }

		public void mousePressed(MouseEvent evt) {
			updateAllButtonStates(evt);
		}

		public void mouseReleased(MouseEvent evt) {
			updateAllButtonStates(evt);
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
	
	/**
	 * This will return the database connection manager as a panel 
	 * so it can be placed in other panels rather than appearing
	 * in its own window.
	 */
	public JPanel getPanel() {
		return panel;
	}


}