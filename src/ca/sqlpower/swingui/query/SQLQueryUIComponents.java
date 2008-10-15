/*
 * Copyright (c) 2008, SQL Power Group Inc.
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

package ca.sqlpower.swingui.query;

import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.undo.UndoManager;

import org.apache.log4j.Logger;

import ca.sqlpower.sql.CachedRowSet;
import ca.sqlpower.sql.DataSourceCollection;
import ca.sqlpower.sql.DatabaseListChangeEvent;
import ca.sqlpower.sql.DatabaseListChangeListener;
import ca.sqlpower.sql.SPDataSource;
import ca.sqlpower.swingui.SPSUtils;
import ca.sqlpower.swingui.SPSwingWorker;
import ca.sqlpower.swingui.SwingWorkerRegistry;
import ca.sqlpower.swingui.db.DatabaseConnectionManager;
import ca.sqlpower.swingui.table.ResultSetTableFactory;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

/**
 * A "bag of components" that are already wired together to cooperate as a GUI environment
 * for writing, debugging, and executing a SQL query. There are two approaches to using
 * this class:
 * <ol>
 *  <li>Use the provided factory method, which creates an instance of the class
 *      and arranges all the components in the usual way and returns a "ready
 *      to use" Swing component that behaves as an interactive SQL query tool.
 *      The factory method is {@link #createQueryPanel(SwingWorkerRegistry, DataSourceCollection)}.
 *  <li>Use the constructor to create an instance of this class, then use
 *      the public getter methods to retrieve all of the components you want
 *      in your UI, and arrange them yourself in any layout and combination
 *      that you require.
 * </ol>
 */
public class SQLQueryUIComponents {
    
    private static Logger logger = Logger.getLogger(SQLQueryUIComponents.class);
    
    /**
     * The entry value in the input map that will map a key press to our
     * "Execute" action.
     */
    private static final String EXECUTE_QUERY_ACTION = "Execute Query";

    /**
     * The entry value in the input map that will map a key press to our
     * undo action on the sql edit text area.
     */

    private static final Object UNDO_SQL_EDIT = "Undo SQL Edit";

    /**
     * The entry value in the input map that will map a key press to our
     * redo action on the sql edit text area.
     */
    private static final Object REDO_SQL_EDIT = "Redo SQL Edit";
    
    
    /**
     * A listener for item selection on a combo box containing {@link SPDataSource}s.
     * This will create a new entry in the connection map to store a live connection
     * for the selected database.
     */
    private class DatabaseItemListener implements ItemListener {
        
        public void itemStateChanged(ItemEvent e) {
            if (e.getStateChange() != ItemEvent.SELECTED) {
                return;
            }
            if (!conMap.containsKey(e.getItem())) {
                SPDataSource ds = (SPDataSource)e.getItem();
                try {
                    Connection con = ds.createConnection();
                    conMap.put(ds, new ConnectionAndStatementBean(con));
                } catch (SQLException e1) {
                    SPSUtils.showExceptionDialogNoReport(dialogOwner, Messages.getString("SQLQuery.failedConnectingToDBWithName", ds.getName()), e1);
                    return;
                }
            }
            try {
                autoCommitToggleButton.setSelected(conMap.get(e.getItem()).getConnection().getAutoCommit());
            } catch (SQLException ex) {
                SPSUtils.showExceptionDialogNoReport(dialogOwner, Messages.getString("SQLQuery.failedConnectingToDB"), ex);
            }
            stopButton.setEnabled(conMap.get(e.getItem()).getCurrentStmt() != null);
            executeButton.setEnabled(conMap.get(e.getItem()).getCurrentStmt() == null);
        }
    }
   
    /**
     * This will execute the sql statement in the sql text area.
     */
    private class ExecuteSQLWorker extends SPSwingWorker {
        
        List<CachedRowSet> resultSets = new ArrayList<CachedRowSet>();
        List<Integer> rowsAffected = new ArrayList<Integer>(); 
        
        public ExecuteSQLWorker(SwingWorkerRegistry registry) {
            super(registry);
        }

        @Override
        public void cleanup() throws Exception {
            Throwable e = getDoStuffException();
            if (e != null) {
                if (e instanceof SQLException) {
                    SPSUtils.showExceptionDialogNoReport(dialogOwner, Messages.getString("SQLQuery.failedConnectingToDB"), e);
                } else {
                    throw new RuntimeException(e);
                }
            }

                tableTabPane.removeAll();
                for (CachedRowSet rs : resultSets) {
                    ResultSet r = rs.createShared();
                    tableTabPane.add(Messages.getString("SQLQuery.result"), createResultSetTablePanel(r));
                }  
                logTextArea.setText("");
                for (Integer i : rowsAffected) {
                    logTextArea.append(Messages.getString("SQLQuery.rowsAffected", i.toString()));
                    logTextArea.append("\n\n");
                }   
        }

        @Override
        public void doStuff() throws Exception {
            logger.debug("Starting execute action.");
            SPDataSource ds = (SPDataSource)databaseComboBox.getSelectedItem();
            if (ds == null) {
                return;
            }
            Connection con = conMap.get(ds).getConnection();
            Statement stmt = null;
            try {
                executeButton.setEnabled(false);
                stopButton.setEnabled(true);
                stmt = con.createStatement();
                conMap.get(ds).setCurrentStmt(stmt);
                try {
                    rowLimitSpinner.commitEdit();
                } catch (ParseException e1) {
                    // If the spinner can't parse it's current value set it to it's previous
                    // value to keep it an actual number.
                    rowLimitSpinner.setValue(rowLimitSpinner.getValue());
                }
                int rowLimit = ((Integer) rowLimitSpinner.getValue()).intValue();
                logger.debug("Row limit is " + rowLimit);
                
                stmt.setMaxRows(rowLimit);
                String sql = queryArea.getText();
                logger.debug("Executing statement " + sql);
                boolean sqlResult = stmt.execute(sql);
                logger.debug("Finished execution");
                boolean hasNext = true;
                
                while (hasNext) {
                    if (sqlResult) {
                        ResultSet rs = stmt.getResultSet();
                        CachedRowSet rowSet = new CachedRowSet();
                        logger.debug("Populating cached row set");
                        rowSet.populate(rs);
                        logger.debug("Result set row count is " + rowSet.size());
                        resultSets.add(rowSet);
                        rowsAffected.add(new Integer(rowSet.size()));
                        rs.close();
                    } else {
                        rowsAffected.add(new Integer(stmt.getUpdateCount()));
                        logger.debug("Update count is : " + stmt.getUpdateCount());
                    }
                    sqlResult = stmt.getMoreResults();
                    hasNext = !((sqlResult == false) && (stmt.getUpdateCount() == -1));
                }
                logger.debug("Finished Execute method");
            } finally {
                if (stmt != null) {
                    try {
                        stmt.close();
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                    }
                    conMap.get(ds).setCurrentStmt(null);
                }
                executeButton.setEnabled(true);
                stopButton.setEnabled(false);
            }
        }
        
    }
    
    /**
     * The component whose nearest Window ancestor will own any dialogs
     * popped up by the query tool.
     */
    private final JComponent dialogOwner;
    
    /**
     * The worker that the execute action runs on to query the database and create the
     * result sets.
     */
    private ExecuteSQLWorker sqlExecuteWorker;
    
    /**
     * The action for executing and displaying a user's query.
     */
    private final AbstractAction executeAction; 
    
    /**
     * A mapping of data sources to live connections. These connections will be left
     * open until the panel's ancestor is closed. The connections are kept open so 
     * auto commit can be turned off and users can enter multiple queries before 
     * committing or rolling back. Additionally, it will allow switching of data
     * sources while keeping the commit or rollback execution sequence preserved.
     */
    private Map<SPDataSource, ConnectionAndStatementBean> conMap;
    
    /**
     * The text area users can enter SQL queries to get data from the database.
     */
    private final JTextArea queryArea;
    
    /**
     * A combo box of available connections the user have specified. The selected
     * one will have the query run on it when the user hits the execute button.
     */
    private final JComboBox databaseComboBox;
    
    /**
     * A JSpinner for the user to enter the row limit of a query.
     */
    private final JSpinner rowLimitSpinner;
    
    /**
     * Toggles auto commit on an off for the selected connection.
     */
    private final JToggleButton autoCommitToggleButton;
    
    /**
     * Commits the changes made on the currently selected connection.
     */
    private final JButton commitButton;
    
    /**
     * Rolls back the changes made on the currently selected connection.
     */
    private final JButton rollbackButton;
    
    
    private JButton undoButton; 
    private JButton redoButton; 

    private JTabbedPane tableTabPane;
    private JTextArea logTextArea;
    
    private SwingWorkerRegistry swRegistry;
    private final DataSourceCollection dsCollection;
    
    /**
     * The undo manager for the text area containing the SQL statement.
     */
    private UndoManager undoManager;
    
    private Action undoSQLStatementAction = new AbstractAction(Messages.getString("SQLQuery.undo")){

        public void actionPerformed(ActionEvent arg0) {
            if (undoManager.canUndo()) {
                undoManager.undo();
            }
            
        }
    };
        
    private Action redoSQLStatementAction = new AbstractAction(Messages.getString("SQLQuery.redo")){

        public void actionPerformed(ActionEvent arg0) {
            if (undoManager.canRedo()) {
                undoManager.redo();
            }
            
        }
    };
    
    
    /**
     * This recreates the database combo box when the list of databases changes.
     */
    private DatabaseListChangeListener dbListChangeListener = new DatabaseListChangeListener() {

        public void databaseAdded(DatabaseListChangeEvent e) {
            databaseComboBox.addItem(e.getDataSource());
        }

        public void databaseRemoved(DatabaseListChangeEvent e) {
            if (databaseComboBox.getSelectedItem() != null && databaseComboBox.getSelectedItem().equals(e.getDataSource())) {
                databaseComboBox.setSelectedItem(null);
            }
            
            databaseComboBox.removeItem(e.getDataSource());
        }
        
    };

    /**
     * Listens to when the an ancestor is added or removed. This will clean up open
     * connections and remove handlers when the ancestor is removed.
     */
    private AncestorListener closeListener = new AncestorListener(){

        public void ancestorAdded(AncestorEvent event) {
            dsCollection.addDatabaseListChangeListener(dbListChangeListener);
        }

        public void ancestorMoved(AncestorEvent event) {
        }

        public void ancestorRemoved(AncestorEvent event) {
            logger.debug("Removing database list change listener");
            dsCollection.removeDatabaseListChangeListener(dbListChangeListener);
            
            for (Map.Entry<SPDataSource, ConnectionAndStatementBean> entry : conMap.entrySet()) {
                try {
                    Connection con = entry.getValue().getConnection();
                    if (!con.getAutoCommit() && entry.getValue().isConnectionUncommitted()) {
                        int result = JOptionPane.showOptionDialog(dialogOwner, Messages.getString("SQLQuery.commitOrRollback", entry.getKey().getName()),
                                Messages.getString("SQLQuery.commitOrRollbackTitle"), JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null,
                                new Object[] {Messages.getString("SQLQuery.commit"), Messages.getString("SQLQuery.rollback")}, Messages.getString("SQLQuery.commit"));
                        if (result == JOptionPane.OK_OPTION) {
                            con.commit();
                        } else if (result == JOptionPane.CANCEL_OPTION) {
                            con.rollback();
                        }
                    }
                    con.close();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
            
        }};
    /**
     * This Listener listens to anything that drops onto the queryTextArea
     */
    private class QueryTextAreaDropListener implements DropTargetListener {
    	
    	private final JTextArea queryArea;
    	
    	public QueryTextAreaDropListener(JTextArea textArea){
    		queryArea = textArea;
    	}

		public void dragEnter(DropTargetDragEvent dtde) {
			logger.debug("We are in drag enter");
		}

		public void dragExit(DropTargetEvent dte) {
			logger.debug("We are in drag Exit");
		}

		public void dragOver(DropTargetDragEvent dtde) {
			// this would be better if there was a visible indication on the text area
			// of the caret position during the drag-over
			queryArea.setCaretPosition(queryArea.viewToModel(dtde.getLocation()));
		}

		public void drop(DropTargetDropEvent dtde) {

			DataFlavor[] flavours = dtde.getTransferable().getTransferDataFlavors();

			String[] droppedStrings = null;

			// find the first acceptable data flavour
			try {
				for (int i = 0; i < flavours.length; i++) {
					String mimeType = flavours[i].getMimeType();
					if (mimeType.equals("application/x-java-serialized-object; class=\"[Ljava.lang.String;\"")) {
						dtde.acceptDrop(DnDConstants.ACTION_COPY);
						logger.debug("Accepting drop of type: " + mimeType);
						droppedStrings = (String[]) dtde.getTransferable().getTransferData(flavours[i]);
						break;
					} else if (mimeType.equals("application/x-java-serialized-object; class=java.lang.String")) {
						dtde.acceptDrop(DnDConstants.ACTION_COPY);
						logger.debug("Accepting drop of type: " + mimeType);
						String text = (String) dtde.getTransferable().getTransferData(flavours[i]);
						droppedStrings = new String[] { text };
						break;
					} else {
						logger.debug("Unsupported flavour: " + mimeType + ". continuing...");
					}
				}
			} catch (UnsupportedFlavorException e) {
				dtde.dropComplete(false);
				throw new IllegalStateException(
						"DnD system says it doesn't support a data flavour"
								+ " it already offered to us!", e);
			} catch (IOException e) {
				dtde.dropComplete(false);
				throw new RuntimeException("Drop failed due to an I/O error", e);
			}

			if (droppedStrings == null) {
				logger.debug("No supported data flavours found. Rejecting drop.");
				dtde.rejectDrop();
				return;
			}

			StringBuilder buf = new StringBuilder();
			boolean first = true;
			for (String name : droppedStrings) {
				if (!first) {
					buf.append(", ");
				}
				buf.append(name);
				first = false;
			}
			queryArea.insert(buf.toString(), queryArea.getCaretPosition());
			dtde.dropComplete(true);

		}

		public void dropActionChanged(DropTargetDragEvent dtde) {
			logger.debug("We are in dropActionChanged");
		}
    }

    /**
     * This button will execute the sql statements in the text area.
     */
    private JButton executeButton;

    /**
     * This button will stop the execution of the currently executing statement
     * on the selected data source's connection that this panel holds.
     */
    private JButton stopButton;
    
    /**
     *  This button will clear the QueryTextField
     */
    private JButton clearButton;
    
    /**
     * Creates a SQLQueryEntryPanel and attaches a drag and drop listener
     * to a DB Tree.
     */
    
    /**
     * A JButton that opens up the DataBaseConnectionManager
     */
    private JButton dbcsManagerButton;
 
    /**
     * Creates a DataBaseConnectionManager so we can edit delete and add connections on the button 
     */
    DatabaseConnectionManager dbConnectionManager;
 
    /**
	 * Creates all of the components of a query tool, but does not lay them out
	 * in any physical configuration. Once you have created one of these
	 * component collections, you can obtain all of the individual pieces and
	 * put together a user interface in any way you like.
	 * <p>
	 * If you just want an easy way to build a full-featured query UI and don't
	 * want to customize its internals, see
	 * {@link #createQueryPanel(SwingWorkerRegistry, DataSourceCollection)}.
	 * 
	 * @param swRegistry
	 *            The registry with which all background tasks will be
	 *            registered. This argument must not be null.
	 * @param ds
	 *            The collection of data sources that will be available for
	 *            querying from the UI. This argument must not be null.
	 * @param panel
	 *            The component whose nearest Window ancestor will own any
	 *            dialogs generated by the parts of the query tool.
	 */
    public SQLQueryUIComponents(SwingWorkerRegistry s, DataSourceCollection ds, JComponent dialogOwner) {
        super();
        this.dialogOwner = dialogOwner;
        this.swRegistry = s;
        this.dsCollection = ds;
        tableTabPane = new JTabbedPane();
        logTextArea = new JTextArea();
        dbConnectionManager = new DatabaseConnectionManager(ds);
        
        executeAction = new AbstractSQLQueryAction(dialogOwner, Messages.getString("SQLQuery.execute")) {

            public void actionPerformed(ActionEvent e) {
                ConnectionAndStatementBean conBean = conMap.get(databaseComboBox.getSelectedItem());
                try {
                    if(conBean!= null) {
                        if (!conBean.getConnection().getAutoCommit()) {
                            conBean.setConnectionUncommitted(true);
                        }
                    }
                } catch (SQLException e1) {
                    SPSUtils.showExceptionDialogNoReport(dialogOwner, Messages.getString("SQLQuery.failedRetrievingConnection", ((SPDataSource)databaseComboBox.getSelectedItem()).getName()), e1);
                }
                sqlExecuteWorker = new ExecuteSQLWorker(swRegistry);
                new Thread(sqlExecuteWorker).start();
            }
        };
        
        autoCommitToggleButton = new JToggleButton(new AbstractSQLQueryAction(dialogOwner, Messages.getString("SQLQuery.autoCommit")) {
        
            public void actionPerformed(ActionEvent e) {
                Connection con = conMap.get(databaseComboBox.getSelectedItem()).getConnection();
                if (con == null) {
                    return;
                }
                try {
                    boolean isPressed = autoCommitToggleButton.getModel().isSelected();
                    if (isPressed && conMap.get(databaseComboBox.getSelectedItem()).isConnectionUncommitted()) {
                        int result = JOptionPane.showOptionDialog(dialogOwner, Messages.getString("SQLQuery.commitOrRollbackBeforeAutoCommit"),
                                Messages.getString("SQLQuery.commitOrRollbackTitle"), JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null,
                                new Object[] {Messages.getString("SQLQuery.commit"), Messages.getString("SQLQuery.cancel"), Messages.getString("SQLQuery.rollback")}, Messages.getString("SQLQuery.commit"));
                        if (result == JOptionPane.OK_OPTION) {
                            commitCurrentDB();
                        } else if (result == JOptionPane.CANCEL_OPTION) {
                            rollbackCurrentDB();
                        } else {
                            ((JToggleButton)e.getSource()).setSelected(con.getAutoCommit());
                            return;
                        }
                         
                        
                    }
                    con.setAutoCommit(isPressed);
                    logger.debug("The auto commit button is toggled " + isPressed);
                } catch (SQLException ex) {
                    SPSUtils.showExceptionDialogNoReport(dialogOwner, Messages.getString("SQLQuery.failedAutoCommit"), ex);
                }
        
            }
        
        });
        
        autoCommitToggleButton.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                if (autoCommitToggleButton.isSelected()) {
                    commitButton.setEnabled(false);
                    rollbackButton.setEnabled(false);
                } else {
                    commitButton.setEnabled(true);
                    rollbackButton.setEnabled(true);
                }
            }
        });
        
        commitButton = new JButton(new AbstractSQLQueryAction(dialogOwner, Messages.getString("SQLQuery.commit")) {
            public void actionPerformed(ActionEvent e) {
                commitCurrentDB();
            }});
        
        rollbackButton = new JButton(new AbstractSQLQueryAction(dialogOwner, Messages.getString("SQLQuery.rollback")){
            public void actionPerformed(ActionEvent e) {
                rollbackCurrentDB();
            }});
        
        
        rowLimitSpinner = new JSpinner(new SpinnerNumberModel(1000, 0, Integer.MAX_VALUE, 1));
        queryArea = new JTextArea();
        undoManager = new UndoManager();
        queryArea.getDocument().addUndoableEditListener(new UndoableEditListener() {
            public void undoableEditHappened(UndoableEditEvent e) {
                undoManager.addEdit(e.getEdit());
            }
        });
        queryArea.getActionMap().put(UNDO_SQL_EDIT, undoSQLStatementAction);
        queryArea.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()), UNDO_SQL_EDIT);
        
        queryArea.getActionMap().put(REDO_SQL_EDIT, redoSQLStatementAction);
        queryArea.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() + InputEvent.SHIFT_MASK), REDO_SQL_EDIT);
        
        conMap = new HashMap<SPDataSource, ConnectionAndStatementBean>();
        
        databaseComboBox = new JComboBox(dsCollection.getConnections().toArray());
        databaseComboBox.setSelectedItem(null);
        databaseComboBox.addItemListener(new DatabaseItemListener());
        
        dialogOwner.addAncestorListener(closeListener);
        
        dialogOwner.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask())
                , EXECUTE_QUERY_ACTION);
        dialogOwner.getActionMap().put(EXECUTE_QUERY_ACTION, executeAction);
        
        executeButton = new JButton(executeAction);
        
        stopButton = new JButton(new AbstractSQLQueryAction(dialogOwner, Messages.getString("SQLQuery.stop")) {
            public void actionPerformed(ActionEvent arg0) {
                ConnectionAndStatementBean conBean = conMap.get(databaseComboBox.getSelectedItem());
                if (conBean != null) {
                    Statement stmt = conBean.getCurrentStmt();
                    if (stmt != null) {
                        try {
                            logger.debug("stmt is being cancelled...supposely");
                            stmt.cancel();
                            if (sqlExecuteWorker != null) {
                                sqlExecuteWorker.kill();
                            }
                        } catch (SQLException e) {
                            SPSUtils.showExceptionDialogNoReport(dialogOwner, Messages.getString("SQLQuery.stopException", ((SPDataSource)databaseComboBox.getSelectedItem()).getName()), e);
                        }
                    }
                }
            }
             });
         clearButton = new JButton(new AbstractSQLQueryAction(dialogOwner, Messages.getString("SQLQuery.clear")){
            public void actionPerformed(ActionEvent arg0) {
                queryArea.setText("");
            }});
         
         dbcsManagerButton = new JButton(new AbstractAction() {
        
            public void actionPerformed(ActionEvent e) {
                Window w = SwingUtilities.getWindowAncestor(dbcsManagerButton);
                dbConnectionManager.showDialog(w);
        
            }
        
        });
         dbcsManagerButton.setText(Messages.getString("SQLQuery.mangeConnections"));
         
         undoButton= new JButton (undoSQLStatementAction);
         redoButton= new JButton (redoSQLStatementAction);
         new DropTarget(queryArea, new QueryTextAreaDropListener(queryArea));
    }
    
    /**
	 * Builds the UI of the {@link SQLQueryUIComponents}. If you just want an
	 * easy way to build a full-featured query UI and don't want to customize
	 * its internals, you have come to the right place.
	 * 
	 * @param swRegistry
	 *            The registry with which all background tasks will be
	 *            registered. This argument must not be null.
	 * @param ds
	 *            The collection of data sources that will be available for
	 *            querying from the UI. This argument must not be null.
	 */
    public static JComponent createQueryPanel(SwingWorkerRegistry swRegistry, DataSourceCollection ds) {
        
        JPanel defaultQueryPanel = new JPanel();
        SQLQueryUIComponents queryParts = new SQLQueryUIComponents(swRegistry, ds, defaultQueryPanel);
        JToolBar toolbar = new JToolBar();
        toolbar.add(queryParts.getExecuteButton());
        toolbar.add(queryParts.getStopButton());
        toolbar.add(queryParts.getClearButton());
        toolbar.addSeparator();
        toolbar.add(queryParts.getAutoCommitToggleButton());
        toolbar.add(queryParts.getCommitButton());
        toolbar.add(queryParts.getRollbackButton());
        toolbar.addSeparator();
        toolbar.add(queryParts.getUndoButton());
        toolbar.add(queryParts.getRedoButton());
        
        FormLayout textAreaLayout = new FormLayout(
                "pref:grow, 10dlu, pref, 10dlu, pref, 10dlu, pref"
                , "pref, pref, fill:max(100dlu;pref):grow");
        DefaultFormBuilder textAreaBuilder = new DefaultFormBuilder(textAreaLayout, defaultQueryPanel);
        textAreaBuilder.setDefaultDialogBorder();
        textAreaBuilder.append(toolbar, 7);
        textAreaBuilder.nextLine();
        textAreaBuilder.append(queryParts.getDatabaseComboBox());
        textAreaBuilder.append(queryParts.getDbcsManagerButton());
        textAreaBuilder.append(Messages.getString("SQLQuery.rowLimit"));
        textAreaBuilder.append(queryParts.getRowLimitSpinner());
        textAreaBuilder.nextLine();
        textAreaBuilder.append(new JScrollPane(queryParts.getQueryArea()), 7);
        
        
        JSplitPane queryPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        JTabbedPane tabPane = new JTabbedPane();
        queryPane.add(defaultQueryPanel, JSplitPane.TOP);
       
        tabPane.add(Messages.getString("SQLQuery.log"), new JScrollPane(queryParts.getLogTextArea()));
        
        tabPane.add(Messages.getString("SQLQuery.result"), queryParts.getTableTabPane());
        queryPane.add(tabPane, JSplitPane.BOTTOM);
        
        return queryPane;
  
    }
    
    
    /**
     * If the connection to the database currently selected in the combo box is not in 
     * auto commit mode then any changes will be committed.
     */
    private void commitCurrentDB() {
        ConnectionAndStatementBean conBean = conMap.get(databaseComboBox.getSelectedItem());
        Connection con = conBean.getConnection();
        if (con == null) {
            return;
        }
        try {
            if (!con.getAutoCommit()) {
                con.commit();
                conBean.setConnectionUncommitted(false);
            }
        } catch (SQLException ex) {
            SPSUtils.showExceptionDialogNoReport(dialogOwner, Messages.getString("SQlQuery.failedCommit"), ex);
        }
    }
    
    /**
     * If the connection to the database currently selected in the combo box is not in 
     * auto commit mode then any changes will be rolled back.
     */
    private void rollbackCurrentDB() {
        ConnectionAndStatementBean conBean = conMap.get(databaseComboBox.getSelectedItem());
        Connection con = conBean.getConnection();
        if (con == null) {
            return;
        }
        try {
            if (!con.getAutoCommit()) {
                con.rollback();
                conBean.setConnectionUncommitted(false);
            }
        } catch (SQLException ex) {
            SPSUtils.showExceptionDialogNoReport(dialogOwner, Messages.getString("SQLQuery.failedRollback"), ex);
        }
    }
    
    /**
     * Creates a new JPanel that displays a table of the result set.
     */
    private JPanel createResultSetTablePanel(ResultSet rs) {
        JTextArea tableFilterTextArea = new JTextArea();
        FormLayout tableAreaLayout = new FormLayout("pref, 10dlu, pref:grow", "pref, 10dlu, fill:min(pref;100dlu):grow");
        DefaultFormBuilder tableAreaBuilder = new DefaultFormBuilder(tableAreaLayout);
        tableAreaBuilder.setDefaultDialogBorder();
        tableAreaBuilder.append(Messages.getString("SQLQuery.filter"));
        tableAreaBuilder.append(new JScrollPane(tableFilterTextArea));
        tableAreaBuilder.nextLine();
        tableAreaBuilder.nextLine();
        JScrollPane tableScrollPane = new JScrollPane(ResultSetTableFactory.createResultSetJTableWithSearch(rs, tableFilterTextArea.getDocument()));
        tableAreaBuilder.append(tableScrollPane, 3);
        
        return tableAreaBuilder.getPanel();
    }

    
    
   public JButton getExecuteButton() {
       return executeButton;
   }
   
   public JButton getStopButton() {
       return stopButton;
   }
   
   public JButton getClearButton() {
       return clearButton;
   }
   
   public JToggleButton getAutoCommitToggleButton() {
       return autoCommitToggleButton;
   }
   
   public JButton getCommitButton() {
       return commitButton;
   }
   
   public JButton getRollbackButton() {
       return rollbackButton;
   }
   
   public JButton getUndoButton() {
       return undoButton;
   }
   
   public JButton getRedoButton() {
       return redoButton;
   }
      
   public JComboBox getDatabaseComboBox() {
       return databaseComboBox;
   }
   
   public JButton getDbcsManagerButton() {
       return dbcsManagerButton;
   }
   
   public JSpinner getRowLimitSpinner() {
       return rowLimitSpinner;
   }
   
   public JTextArea getQueryArea() {
       return queryArea;
   }
   
   public JTabbedPane getTableTabPane(){
       return tableTabPane;
   }
   public JTextArea getLogTextArea(){
       return logTextArea;
   }
 
}

