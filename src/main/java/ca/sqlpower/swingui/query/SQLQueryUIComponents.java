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

package ca.sqlpower.swingui.query;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
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
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.beans.PropertyChangeSupport;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Document;
import javax.swing.undo.UndoManager;

import org.apache.log4j.FileAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.RTextScrollPane;

import ca.sqlpower.sql.CachedRowSet;
import ca.sqlpower.sql.DataSourceCollection;
import ca.sqlpower.sql.DatabaseListChangeEvent;
import ca.sqlpower.sql.DatabaseListChangeListener;
import ca.sqlpower.sql.JDBCDataSource;
import ca.sqlpower.sql.RowSetChangeEvent;
import ca.sqlpower.sql.RowSetChangeListener;
import ca.sqlpower.sql.SPDataSource;
import ca.sqlpower.sqlobject.SQLDatabase;
import ca.sqlpower.sqlobject.SQLDatabaseMapping;
import ca.sqlpower.sqlobject.SQLObjectException;
import ca.sqlpower.swingui.SPSUtils;
import ca.sqlpower.swingui.SPSwingWorker;
import ca.sqlpower.swingui.SwingWorkerRegistry;
import ca.sqlpower.swingui.db.DatabaseConnectionManager;
import ca.sqlpower.swingui.event.TaskTerminationEvent;
import ca.sqlpower.swingui.event.TaskTerminationListener;
import ca.sqlpower.swingui.table.FancyExportableJTable;
import ca.sqlpower.swingui.table.ResultSetTableModel;
import ca.sqlpower.validation.swingui.StatusComponent;

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
    
    private static final Logger logger = Logger.getLogger(SQLQueryUIComponents.class);
    
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
        		updateStatus();
                return;
            }
            JDBCDataSource ds = (JDBCDataSource)e.getItem();
            SQLDatabase db = databaseMapping.getDatabase(ds);
            try {
				addConnection(db);
			} catch (SQLObjectException e1) {
				logTextArea.append(createErrorStringMessage(e1));
				logTextArea.setCaretPosition(logTextArea.getDocument().getLength());
			}
        }

    }
   
    /**
	 * This TextArea stores an exception Message if it ever Happens
     */
    private final JTextArea errorTextArea = new JTextArea();
    
    /**
     * This action will save the text in a document to a user selected file.
     * The text will either append to the file or overwrite the file's contents. 
     */
    private class SaveDocumentAction extends AbstractAction {

    	private final Document doc;
		private final Component parent;
		private final boolean append;

		public SaveDocumentAction(Document doc, Component parent, boolean append) {
			super("Save As...");
			this.doc = doc;
			this.parent = parent;
			this.append = append;
    	}
    	
		public void actionPerformed(ActionEvent e) {
			JFileChooser chooser = new JFileChooser();
			chooser.addChoosableFileFilter(SPSUtils.LOG_FILE_FILTER);
			chooser.addChoosableFileFilter(SPSUtils.TEXT_FILE_FILTER);
			chooser.setFileFilter(SPSUtils.LOG_FILE_FILTER);
			int retval = chooser.showSaveDialog(parent);
			if (retval == JFileChooser.APPROVE_OPTION) {
				if (logger.isDebugEnabled()) {
					try {
						logger.debug("Log has length " + doc.getLength() + " and text " + doc.getText(0, doc.getLength()) + " when writing to file.");
					} catch (BadLocationException e1) {
						throw new RuntimeException(e1);
					}
				}
				logger.debug("Are we appending? " + append);
				
				String filePath = chooser.getSelectedFile().getAbsolutePath();
				if (!chooser.getSelectedFile().getName().contains(".")) {
					if (chooser.getFileFilter() == SPSUtils.TEXT_FILE_FILTER) {
						filePath = filePath + ".txt";
					} else {
						filePath = filePath + ".log";
					}
				}
				if (append) {
					FileAppender appender = null;
					Logger logAppender = null;
					try {
						appender = new FileAppender(new PatternLayout("%m\n"), filePath);
						logAppender = Logger.getLogger("SQLQueryUIComponents Log Appender");
						logAppender.addAppender(appender);
						logAppender.info(doc.getText(0, doc.getLength()));
					} catch (Exception e1) {
						throw new RuntimeException(e1);
					} finally {
						if (logAppender != null && appender != null) {
							logAppender.removeAppender(appender);
						}
					}
				} else {
					try {
						FileWriter writer = new FileWriter(filePath);
						writer.write(doc.getText(0, doc.getLength()));
						writer.flush();
						writer.close();
					} catch (Exception e1) {
						throw new RuntimeException(e1);
					}
				}
			}
		}
    	
    }
   
    /**
     * This mouse listener will be attached to the log in the results area to give users
     * an easy way to save the log to a file.
     */
    private final MouseListener logPopUpMouseListener = new MouseListener() {
    	
    	private JCheckBoxMenuItem checkBoxMenuItem = new JCheckBoxMenuItem("Append", true);
    	
		public void mouseReleased(MouseEvent e) {
			logger.debug("Mouse released on log pop-up");
			showPopup(e);
		}
	
		public void mousePressed(MouseEvent e) {
			showPopup(e);	
		}
	
		public void mouseExited(MouseEvent e) {
			showPopup(e);
		}
	
		public void mouseEntered(MouseEvent e) {
			showPopup(e);
		}
	
		public void mouseClicked(MouseEvent e) {
			showPopup(e);
		}
		
		private void showPopup(MouseEvent e) {
			if (e.isPopupTrigger()) {
				JPopupMenu logPopupMenu = new JPopupMenu();
				logPopupMenu.add(new JMenuItem(new SaveDocumentAction(logTextArea.getDocument(), logTextArea, checkBoxMenuItem.isSelected())));
				logPopupMenu.add(checkBoxMenuItem);
				logPopupMenu.show(e.getComponent(), e.getX(), e.getY());
				logPopupMenu.setVisible(true);
			}
		}
	};
    
    /**
     * This will execute the sql statement in the sql text area. The
     * SQL statement used in execution will be stored with this swing
     * worker. If a different SQL statement is to be executed later
     * a new worker should be created.
     */
    private class ExecuteSQLWorker extends SPSwingWorker {
    	
        private List<CachedRowSet> resultSets = new ArrayList<CachedRowSet>();
        private List<Integer> rowsAffected = new ArrayList<Integer>(); 
		private final SQLDatabase db;
		private long startExecutionTime;
		private final StatementExecutor stmtExecutor;
		private final StatementExecutorListener executorListener = new StatementExecutorListener() {
			public void queryStopped() {
				stopButton.setEnabled(false);
			}
			public void queryStarted() {
				stopButton.setEnabled(true);
			}
		};

        /**
         * Constructs a new ExecuteSQLWorker that will use the given SQL
         * statement as the string to execute on.
         * 
         * @param registry
         *            The registry to notify when this task begins and ends.
         * @param stmtExecutor
         *            The statement executor that actually executes the query.
         *            This object will be considered this worker's
         *            {@link #getResponsibleObject() responsible object}.
         */
        public ExecuteSQLWorker(SwingWorkerRegistry registry, StatementExecutor stmtExecutor) {
        	super(registry, stmtExecutor);
			this.stmtExecutor = stmtExecutor;
        	if(stmtExecutor.getStatement().equals("")) {
        		logger.debug("Empty String");
        		// if the string is empty there will be no execute so we need to reset the Panel from here.
        		firstResultPanel.removeAll();
        		firstResultPanel.revalidate();
        	} 
        	
        	this.stmtExecutor.addStatementExecutorListener(executorListener);
        	
        	db = databaseMapping.getDatabase((JDBCDataSource) databaseComboBox.getSelectedItem());
        	
        	try {
                rowLimitSpinner.commitEdit();
            } catch (ParseException e1) {
                // If the spinner can't parse it's current value set it to it's previous
                // value to keep it an actual number.
                rowLimitSpinner.setValue(rowLimitSpinner.getValue());
            }
            
            updateStatus();
            
            setJobSize(null);
            setProgress(0);
            if (db != null) {
                setMessage(Messages.getString("SQLQuery.workerMessage", db.getName()));
            } else {
                setMessage(Messages.getString("SQLQuery.queryingNullDB"));
            }
        }

        @Override
        public void cleanup() throws Exception {
        	try {
        		long finishExecutionTime = System.currentTimeMillis();
        		DateFormat formatter = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.LONG);
        		logTextArea.append("Executed at " + formatter.format(new Date(startExecutionTime)) + ", took " + (finishExecutionTime - startExecutionTime) + " milliseconds\n");
        		Throwable e = getDoStuffException();
        		if (e != null) {
        			String errorMessage = createErrorStringMessage(e);
        			logTextArea.append(errorMessage + "\n");
        			logger.error(e.getStackTrace());
        			clearResultTables(false);
        			for (Map.Entry<JTable, JScrollPane> entry : resultJTableScrollPanes.entrySet()) {
        			    JPanel panel = new JPanel(new BorderLayout());
        			    panel.add(entry.getKey().getTableHeader(), BorderLayout.NORTH);
        			    panel.add(new JTextArea(Messages.getString("SQLQuery.queryFailedSeeLog", e.getMessage())));
        			    entry.getValue().getViewport().setView(panel);
        			}
        			return;
        		}
        		
        		if (queuedSQLStatement == null) {
        			createResultSetTables(resultSets, stmtExecutor);
        		}

        		resultSets.clear();
        		for (Integer i : rowsAffected) {
        			logTextArea.append(Messages.getString("SQLQuery.rowsAffected", i.toString()));
        			logTextArea.append("\n");
        		}  
        	} finally {
        		logTextArea.append("\n");
        		updateStatus();
        		logTextArea.setCaretPosition(logTextArea.getDocument().getLength());
        		this.stmtExecutor.removeStatementExecutorListener(executorListener);
        	}
        }

        @Override
        public void doStuff() throws Exception {
        	try {
        		startExecutionTime = System.currentTimeMillis();
        		logger.debug("Starting execute action of \"" + stmtExecutor.getStatement() + "\".");
        		if (db == null) {
        			return;
        		}
        		if (stmtExecutor.getStatement().trim().length() == 0) {
        			return;
        		}
        		if (conMap.get(db) == null || conMap.get(db).getConnection() == null || conMap.get(db).getConnection().isClosed()) {
        			addConnection(db);
        		}
        		
        		updateStatus();
        		
        		logger.debug("Executing statement " + stmtExecutor.getStatement());
        		boolean sqlResult = stmtExecutor.executeStatement();
        		logger.debug("Finished execution");
        		boolean hasNext = true;
        		
        		while (hasNext) {
        			if (sqlResult) {
        				ResultSet rs = stmtExecutor.getResultSet();
        				CachedRowSet rowSet;
        				logger.debug("Populating cached row set");
        				if (rs instanceof CachedRowSet) {
        					rowSet = (CachedRowSet)rs;
        				} else {
        					rowSet = new CachedRowSet();
        					rowSet.populate(rs);
        				}
        				logger.debug("Result set row count is " + rowSet.size());
        				resultSets.add(rowSet);
        				rowsAffected.add(new Integer(rowSet.size()));
        				rs.close();
        			} else {
        				rowsAffected.add(new Integer(stmtExecutor.getUpdateCount()));
        				logger.debug("Update count is : " + stmtExecutor.getUpdateCount());
        			}
        			sqlResult = stmtExecutor.getMoreResults();
        			hasNext = !((sqlResult == false) && (stmtExecutor.getUpdateCount() == -1));
        		}
        		logger.debug("Finished Execute method");
        		
        	} finally {
        		updateStatus();
        	}
        }
    }
    
    private class DefaultStatementExecutor implements StatementExecutor {
    	
    	private final SQLDatabase db;
		private final String sqlString;
		private final int rowLimit;
		private final List<ResultSet> resultSets = new ArrayList<ResultSet>();
		private final List<Integer> updateCounts = new ArrayList<Integer>();
		private final List<StatementExecutorListener> executorListeners = new ArrayList<StatementExecutorListener>();
		private int resultPosition;
		
		/**
		 * This list of listeners should be notified every time a CachedRowSet's
		 * populate listener is updated. This should be done when streaming connections
		 * is allowed in the Universal SQL Access tool.
		 */
		private final List<RowSetChangeListener> rowSetChangeListeners = new ArrayList<RowSetChangeListener>();
		
		private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);
		
		public DefaultStatementExecutor(SQLDatabase db, String sqlString, int rowLimit) {
			this.rowLimit = rowLimit;
			this.sqlString = sqlString;
			this.db = db;
			resultPosition = 0;
		}

		public boolean isRunning() {
			boolean dbSelected = databaseComboBox.getSelectedItem() != null;
	    	ConnectionAndStatementBean selectedConnection;
	    	if (dbSelected) {
	    	    JDBCDataSource selectedDS = (JDBCDataSource) databaseComboBox.getSelectedItem();
	            selectedConnection =
	    	        conMap.get(databaseMapping.getDatabase(selectedDS));
	    	} else {
	    	    selectedConnection = null;
	    	}
			return dbSelected 
					&& selectedConnection != null
					&& selectedConnection.getCurrentStmt() != null;
		}
		
		public boolean executeStatement() throws SQLException {
			resultPosition = 0;
			Connection con = null;
            Statement stmt = null;
            try {
            	fireQueryExecutionStart();
            	con = conMap.get(db).getConnection();
                stmt = con.createStatement();
                conMap.get(db).setCurrentStmt(stmt);
                
                stmt.setMaxRows(rowLimit);
                boolean initialResult = stmt.execute(sqlString);
                boolean sqlResult = initialResult;
                boolean hasNext = true;
                while (hasNext) {
                	if (sqlResult) {
                		CachedRowSet crs = new CachedRowSet();
                		crs.setMakeUppercase(false);
                		crs.populate(stmt.getResultSet());
                		resultSets.add(crs);
                	} else {
                		resultSets.add(null);
                	}
                    updateCounts.add(stmt.getUpdateCount());
                    sqlResult = stmt.getMoreResults();
                    hasNext = !((sqlResult == false) && (stmt.getUpdateCount() == -1));
                }
                return initialResult;
            } finally {
                if (stmt != null) {
                    try {
                        stmt.close();
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                    }
                    conMap.get(db).setCurrentStmt(null);
                }
                fireQueryExecutionStop();
            }
		}

		public ResultSet getResultSet() {
			if (resultPosition >= resultSets.size()) {
				return null;
			}
			return resultSets.get(resultPosition);
		}

		public String getStatement() {
			return sqlString;
		}

		public int getUpdateCount() {
			if (resultPosition >= updateCounts.size()) {
				return -1;
			}
			return updateCounts.get(resultPosition);
		}

		public boolean getMoreResults() {
			resultPosition++;
			return resultPosition < resultSets.size() && resultSets.get(resultPosition) != null;
		}

		public void addRowSetChangeListener(RowSetChangeListener l) {
			rowSetChangeListeners.add(l);
		}

		public void removeRowSetChangeListener(RowSetChangeListener l) {
			rowSetChangeListeners.remove(l);
		}
		
		private void fireQueryExecutionStart() {
			for (StatementExecutorListener listener : this.executorListeners) {
				listener.queryStarted();
			}
		}
		
		private void fireQueryExecutionStop() {
			for (StatementExecutorListener listener : this.executorListeners) {
				listener.queryStopped();
			}
		}
		
		public void addStatementExecutorListener(
				StatementExecutorListener qcl) {
			this.executorListeners.add(qcl);
		}
		
		public void removeStatementExecutorListener(
				StatementExecutorListener qcl) {
			this.executorListeners.remove(qcl);
		}

    }
    
    /**
     * This is the Panel that holds the first result JTable, This is normally used when multiple queries
     * not enabled and you wish to return this panel instead of the tabbedResult panel.
     */
    private JPanel firstResultPanel;
    
    /**
     * The component whose nearest Window ancestor will own any dialogs
     * popped up by the query tool.
     */
    private final JComponent dialogOwner;

	/**
	 * The worker that the execute action runs on to query the database and
	 * create the result sets. If this is null there is no currently executing
	 * worker.
	 */
    private volatile ExecuteSQLWorker sqlExecuteWorker;
    
    private final TaskTerminationListener sqlExecuteTerminationListener = new TaskTerminationListener() {
		public void taskFinished(TaskTerminationEvent e) {
			executeQuery((StatementExecutor) null);
		}
	};
    
    /**
     * This stores the next SQL statement to be run when the currently executing worker
     * is running.
     */
    private StatementExecutor queuedSQLStatement;
    
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
    private Map<SQLDatabase, ConnectionAndStatementBean> conMap;
    
    /**
     * The text area users can enter SQL queries to get data from the database.
     */
    private final RSyntaxTextArea queryArea;
    
    /**
     * A combo box of available connections the user have specified. The selected
     * one will have the query run on it when the user hits the execute button.
     */
    private final JComboBox databaseComboBox;
    
    /**
     * A JSpinner for the user to enter the row limit of a query.
     */
    private JSpinner rowLimitSpinner;
    
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

    private JTabbedPane resultTabPane;
    private JTextArea logTextArea;
    private static final ImageIcon ICON = new ImageIcon(StatusComponent.class.getClassLoader().getResource("ca/sqlpower/swingui/query/search.png"));
    private ArrayList<JTable> resultJTables;

    /**
     * These {@link JScrollPane}s each contain one table in the resultJTables
     * list. They are stored to place an exception message in the scroll pane if
     * a query fails. The result JTables are mapped to the scroll panes they
     * are contained in.
     */
    private final Map<JTable, JScrollPane> resultJTableScrollPanes = new HashMap<JTable, JScrollPane>();
    
    /**
     * This maps the JTables to the SQL statement that created them.
     * Multiple tables can share the same string.
     */
    private final Map<JTable, String> tableToSQLMap;
    
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
            if (!(e.getDataSource() instanceof JDBCDataSource)) return;
        	logger.debug("dataBase added");
            databaseComboBox.addItem(e.getDataSource());
            databaseComboBox.revalidate();
        }

        public void databaseRemoved(DatabaseListChangeEvent e) {
            if (!(e.getDataSource() instanceof JDBCDataSource)) return;
        	logger.debug("dataBase removed");
            if (databaseComboBox.getSelectedItem() != null && databaseComboBox.getSelectedItem().equals(e.getDataSource())) {
                databaseComboBox.setSelectedItem(null);
            }
            
            databaseComboBox.removeItem(e.getDataSource());
            databaseComboBox.revalidate();
        }
        
    };

    /**
     * This list keeps track of all previous queries executed to allow users to move through
     * their query history.
     */
    private final List<String> previousQueries;
    
    /**
     * The button to go back to old queries in the previousQueries list.
     */
    private final JButton prevQueryButton;
    
    /**
     * The button to go forward in the old query list.
     */
    private final JButton nextQueryButton;

    /**
     * This is the position in the list of previous queries where the user is if they
     * are moving between old and older queries.
     */
    private int prevQueryPosition;
    /**
     * This is the method that will close the dialog and remove any connections in the dialog
     */
    public void closingDialogOwner(){
    	
    	logger.debug("attempting to close");
        boolean commitedOrRollBacked = closeConMap();
        if(commitedOrRollBacked){
        	logger.debug("removing DatabaseListChangeListener and closing window");
        	disconnectListeners();
        	Window w = SwingUtilities.getWindowAncestor(dialogOwner);
        	if(w != null) {
        		w.setVisible(false);
        	}
        }
    }

    /**
     * Closes all of the connections in the connection mapping. If the
     * connection being closed is not in an auto-commit state, a dialog will be
     * displayed with the option to roll back or commit the changes.
     * <p>
     * Any SQLExceptions encountered while closing a connection are logged at
     * the WARN level and are otherwise ignored. This is important, since
     * applications such as Wabit call this method as part of their session
     * termination routine. An unchecked exception at that time would abort the
     * shutdown.
     */
	public boolean closeConMap() {
		boolean commitedOrRollBacked = true;
		final Iterator<Entry<SQLDatabase, ConnectionAndStatementBean>> iterator = conMap.entrySet().iterator();
		for (;iterator.hasNext();) {
			final Entry<SQLDatabase, ConnectionAndStatementBean> entry = iterator.next();
            try {	
                Connection con = entry.getValue().getConnection();
                if (!con.isClosed()) {
                	if (!con.getAutoCommit() && entry.getValue().isConnectionUncommitted()) {
                		commitedOrRollBacked = false;
                		int result = JOptionPane.showOptionDialog(dialogOwner, Messages.getString("SQLQuery.commitOrRollback", entry.getKey().getName()),
                				Messages.getString("SQLQuery.commitOrRollbackTitle"), JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null,
                				new Object[] {Messages.getString("SQLQuery.commit"), Messages.getString("SQLQuery.rollback"), "Cancel"}, Messages.getString("SQLQuery.commit"));
                		if (result == JOptionPane.OK_OPTION) {
                			con.commit();
                			commitedOrRollBacked = true;
                		} else if (result == JOptionPane.NO_OPTION) {
                			con.rollback();
                			commitedOrRollBacked = true;
                		}else if(result == JOptionPane.CANCEL_OPTION) {
                			return false;
                		}
                	}
                	con.close();
                }
                iterator.remove();
                
            } catch (SQLException e) {
                logger.warn("Failed to close connection " + entry.getValue() + ". Skipping it.", e);
            }
        }

		return commitedOrRollBacked;
	}

    /**
     * Listens to when the an window is added or removed. This will clean up open
     * connections and remove handlers when the window is removed.
     */
    private WindowListener windowListener = new WindowAdapter(){

		public void windowClosing(WindowEvent arg0) {
			closingDialogOwner();			
		}
	};

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
			boolean isCommaSeperated = false;

			// find the first acceptable data flavor
			try {
				for (int i = 0; i < flavours.length; i++) {
					String mimeType = flavours[i].getMimeType();
					//if the type is DBTree
					if (mimeType.equals("application/x-java-serialized-object; class=\"[Ljava.lang.String;\"")) {
						dtde.acceptDrop(DnDConstants.ACTION_COPY);
						logger.debug("Accepting drop of type: " + mimeType);
						droppedStrings = (String[]) dtde.getTransferable().getTransferData(flavours[i]);
						isCommaSeperated = true;
						break;
					//if the type is text
					} else if (mimeType.equals("application/x-java-serialized-object; class=java.lang.String")) {
						dtde.acceptDrop(DnDConstants.ACTION_COPY);
						logger.debug("Accepting drop of type: " + mimeType);
						String text = (String) dtde.getTransferable().getTransferData(flavours[i]);
						droppedStrings = new String[] { text };
						break;
					//if the type is file
					} else if (mimeType.equals("application/x-java-file-list; class=java.util.List")) {
						dtde.acceptDrop(DnDConstants.ACTION_COPY);
						List<?> fileList = (List<?>)dtde.getTransferable().getTransferData(flavours[i]);
						droppedStrings = new String[fileList.size()];
						for(int j = 0; j < droppedStrings.length; j++) {
							StringBuffer fileContent = new StringBuffer();
						    try {
						        BufferedReader in = new BufferedReader(new FileReader(((File)fileList.get(j))));
						        String str;
						        while ((str = in.readLine()) != null) {
						        	fileContent.append(str);
						        	fileContent.append("\n");
						        }
						        droppedStrings[j] = fileContent.toString();
						        in.close();  
						    } catch (IOException e) {
						    	logger.debug(" Can't open file " + ((File)fileList.get(j)).getName());
						    }
						}
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
				if (!first && isCommaSeperated) {
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
	 * This listener is attached to a statement executor and is used to pass row set changes
	 * from the row set to table models to update the table models.
	 */
	private class StreamingRowSetListener implements RowSetChangeListener {

		private AtomicBoolean hasUpdates = new AtomicBoolean(false);
    	
    	private final Timer timer = new Timer(1000, new ActionListener() {
			
    		public void actionPerformed(ActionEvent e) {				
				if (hasUpdates.get()) {
					listeningTableModel.dataChanged();
					hasUpdates.set(false);
				}
			}
		});
    	
		/**
		 * The result set this listener is listening to.
		 */
		private final CachedRowSet rowSet;
		
		/**
		 * These models will receive events of row changes when this listener receives
		 * a row added event.
		 */
		private final ResultSetTableModel listeningTableModel;

		/**
		 * @param rs The result set this listener is listening to.
		 */
		public StreamingRowSetListener(CachedRowSet rowSet, ResultSetTableModel tableModel) {
			this.rowSet = rowSet;
			listeningTableModel = tableModel;
			this.timer.setInitialDelay(0);
    		this.timer.setCoalesce(true);
    		this.timer.setRepeats(true);
    		this.timer.start();
		}
		
		/**
		 * This will disconnect the listener from what it is listening to and also disconnect
		 * all of the tables listening to this listener.
		 */
		public void disconnect() {
			this.timer.stop();
			rowSet.removeRowSetListener(this);
		}
		
		public void rowAdded(RowSetChangeEvent e) {
			hasUpdates.set(true);
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
    private DatabaseConnectionManager dbConnectionManager;
    
    /**
     * A list of listeners that get notified when tables are
     * added or removed from the components.
     */
    private final List<TableChangeListener> tableListeners;

    /**
     * This is the document used for searching across the current result sets. This will be
     * recreated each time new results are created as it is attached to the result set JTables.
     */
	private Document searchDocument;
	
	/**
	 * If true the search field will be shown on each result tab directly above the table. If
	 * this is false then a search field can be created by retrieving the search document
	 * from the tables.
	 * <p>
	 * This is set to true by default.
	 */
	private boolean showSearchOnResults = true;

	/**
	 * This statement executor will be used to run queries when the execute button is pressed.
	 * This will replace the default executor for text queries and is used in at least Wabit.
	 */
	private StatementExecutor stmtExecutor = null;
	
	/**
	 * These listeners will pass events from the row set last executed to the table models
	 * being displayed as results of the statement execution.
	 */
	private final List<StreamingRowSetListener> rowSetListeners = new ArrayList<StreamingRowSetListener>();
	
	/**
	 * This database mapping maps available {@link SQLDatabase} objects to 
	 * corresponding {@link SPDataSource} objects. This helps prevent extra
	 * connections from being created as the {@link SQLDatabase} does
	 * the connection pooling.
	 */
	private final SQLDatabaseMapping databaseMapping;

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
	 * @param stmtExecutor
	 *            The statement executor that will be used to execute queries
	 *            instead of a default statement executor. This will be used to
	 *            run queries entered in the text editor.
	 */
	public SQLQueryUIComponents(SwingWorkerRegistry s, DataSourceCollection ds, SQLDatabaseMapping mapping, JComponent dialogOwner, StatementExecutor stmtExecutor) {
		this(s, ds, mapping, dialogOwner);
		this.stmtExecutor = stmtExecutor;
	}
	
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
    public SQLQueryUIComponents(SwingWorkerRegistry s, DataSourceCollection dsCollection, SQLDatabaseMapping mapping, JComponent dialogOwner) {
        super();
        databaseMapping = mapping;
        previousQueries = new ArrayList<String>();
        this.dialogOwner = dialogOwner;
        this.swRegistry = s;
        this.dsCollection = dsCollection;
        this.errorTextArea.setEditable(false);
		dsCollection.addDatabaseListChangeListener(dbListChangeListener);
        resultTabPane = new JTabbedPane();
        firstResultPanel = new JPanel(new BorderLayout());
        logTextArea = new JTextArea();
        logTextArea.setEditable(false);
        logTextArea.addMouseListener(logPopUpMouseListener);
        resultTabPane.add(Messages.getString("SQLQuery.log"), new JScrollPane(logTextArea));
        
        resultJTables = new ArrayList<JTable>();
        tableToSQLMap = new HashMap<JTable, String>();
        tableListeners = new ArrayList<TableChangeListener>();
        dbConnectionManager = new DatabaseConnectionManager(dsCollection);
        
        executeAction = new AbstractSQLQueryAction(dialogOwner, Messages.getString("SQLQuery.execute")) {

            public void actionPerformed(ActionEvent e) {
            	String sql = queryArea.getText();
            	if (queryArea.getSelectedText() != null && queryArea.getSelectedText().trim().length() > 0) {
            		sql = queryArea.getSelectedText();
            	}
				executeQuery(sql);
            }

        };
        
        autoCommitToggleButton = new JToggleButton(new AbstractSQLQueryAction(dialogOwner, Messages.getString("SQLQuery.autoCommit")) {
        
            public void actionPerformed(ActionEvent e) {
            	
            	if(databaseComboBox.getSelectedItem() == null){
            		return;
            	}
                Connection con = conMap.get(databaseMapping.getDatabase((JDBCDataSource) databaseComboBox.getSelectedItem())).getConnection();
                if (con == null) {
                    return;
                }
                try {
                    boolean isPressed = autoCommitToggleButton.getModel().isSelected();
                    if (isPressed && conMap.get(databaseMapping.getDatabase((JDBCDataSource) databaseComboBox.getSelectedItem())).isConnectionUncommitted()) {
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
                updateStatus();
            }
        });
        
        commitButton = new JButton(new AbstractSQLQueryAction(dialogOwner, Messages.getString("SQLQuery.commit")) {
            public void actionPerformed(ActionEvent e) {
            	if(databaseComboBox.getSelectedItem() == null){
            		return;
            	}
                commitCurrentDB();
            }});
        
        rollbackButton = new JButton(new AbstractSQLQueryAction(dialogOwner, Messages.getString("SQLQuery.rollback")){
            public void actionPerformed(ActionEvent e) {
            	if(databaseComboBox.getSelectedItem() == null){
            		return;
            	}
                rollbackCurrentDB();
            }});
        
        
        rowLimitSpinner = new JSpinner(new SpinnerNumberModel(Integer.MAX_VALUE, 0, Integer.MAX_VALUE, 1));
        
        queryArea = new RSyntaxTextArea();
        queryArea.restoreDefaultSyntaxScheme();
        queryArea.setSyntaxEditingStyle(RSyntaxTextArea.SYNTAX_STYLE_SQL);
        
        
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
        
        conMap = new HashMap<SQLDatabase, ConnectionAndStatementBean>();
        
        databaseComboBox = new JComboBox(dsCollection.getConnections(JDBCDataSource.class).toArray());
        databaseComboBox.setSelectedItem(null);
        databaseComboBox.addItemListener(new DatabaseItemListener());
        
        dialogOwner.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask())
                , EXECUTE_QUERY_ACTION);
        dialogOwner.getActionMap().put(EXECUTE_QUERY_ACTION, executeAction);
        
        executeButton = new JButton(executeAction);
        
        stopButton = new JButton(new AbstractSQLQueryAction(dialogOwner, Messages.getString("SQLQuery.stop")) {
            public void actionPerformed(ActionEvent arg0) {
                ConnectionAndStatementBean conBean = conMap.get(databaseMapping.getDatabase((JDBCDataSource) databaseComboBox.getSelectedItem()));
                if (conBean != null) {
                    Statement stmt = conBean.getCurrentStmt();
                    if (stmt != null) {
                        try {
                            logger.debug("stmt is being cancelled...supposely");
                            stmt.cancel();
                            if (sqlExecuteWorker != null) {
                            	queuedSQLStatement = null;
                                sqlExecuteWorker.kill();
                                sqlExecuteWorker = null;
                            }
                        } catch (SQLException e) {
                            SPSUtils.showExceptionDialogNoReport(dialogOwner, Messages.getString("SQLQuery.stopException", ((SQLDatabase) databaseMapping.getDatabase((JDBCDataSource) databaseComboBox.getSelectedItem())).getName()), e);
                        }
                    }
                }
                updateStatus();
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
         
        prevQueryButton = new JButton(new AbstractAction("Prev") {
		    public void actionPerformed(ActionEvent e) {
			    if (prevQueryPosition > 0) {
					prevQueryPosition--;
					queryArea.setText(previousQueries.get(prevQueryPosition));
				}
				getPrevQueryButton().setEnabled(prevQueryPosition > 0);
				getNextQueryButton().setEnabled(prevQueryPosition < previousQueries.size() - 1);
			}
		});
        
        nextQueryButton = new JButton(new AbstractAction("Next") {
		
			public void actionPerformed(ActionEvent e) {
				if (prevQueryPosition < previousQueries.size() - 1) {
					prevQueryPosition++;
					queryArea.setText(previousQueries.get(prevQueryPosition));
				}
				getPrevQueryButton().setEnabled(prevQueryPosition > 0);
				getNextQueryButton().setEnabled(prevQueryPosition < previousQueries.size() - 1);
			}
		});
        
        getPrevQueryButton().setEnabled(false);
        getNextQueryButton().setEnabled(false);
        
        dbcsManagerButton.setText(Messages.getString("SQLQuery.manageConnections"));

        undoButton= new JButton (undoSQLStatementAction);
        redoButton= new JButton (redoSQLStatementAction);
        new DropTarget(queryArea, new QueryTextAreaDropListener(queryArea));
        
        updateStatus();
    }

    /**
     * Modifies the enabled/disabled state for the execute action as well as the
     * rollback, commit, and stop buttons. The correct state for these buttons
     * is determined by examining the state of the various components this class
     * ties together.
     */
    private void updateStatus() {
    	
    	boolean dbSelected = databaseComboBox.getSelectedItem() != null;
    	
    	executeAction.setEnabled(stmtExecutor != null || dbSelected);
    	executeButton.setEnabled(stmtExecutor != null || dbSelected);
    	
    	boolean autoCommit = autoCommitToggleButton.isSelected();
    	
    	rollbackButton.setEnabled(!autoCommit && dbSelected);
    	commitButton.setEnabled(!autoCommit && dbSelected);
    	
    	if (this.stmtExecutor != null) {
    		stopButton.setEnabled(this.stmtExecutor.isRunning());
    	} else {
    		stopButton.setEnabled(false);
    	}
    }
    
    /**
     * Executes a given query with the help of a worker. This will also clear
     * the results tabs before execution.
     * 
     * NOTE: If a query is currently executing then the query passed in will
     * execute after the current query is complete. Additionally, if there is 
     * a query already waiting to execute it will be REPLACED by the new query.
     * ie the previous query waiting to execute will not be run.
     */
    public synchronized void executeQuery(String sql) {
    	if (stmtExecutor == null) {
    		if (databaseComboBox.getSelectedItem() != null) {
    			executeQuery(new DefaultStatementExecutor(
    			        databaseMapping.getDatabase((JDBCDataSource) databaseComboBox.getSelectedItem()),
    			        sql,
    			        ((Integer) rowLimitSpinner.getValue()).intValue()));
    		}
    	} else {
    		executeQuery(stmtExecutor);
    	}
    }
    
    /**
     * Executes a given query with the help of a worker. This will also clear
     * the results tabs before execution.
     * 
     * NOTE: If a query is currently executing then the query passed in will
     * execute after the current query is complete. Additionally, if there is 
     * a query already waiting to execute it will be REPLACED by the new query.
     * ie the previous query waiting to execute will not be run.
     */
    public synchronized void executeQuery(StatementExecutor stmtExecutor) {
    	if (sqlExecuteWorker != null && !sqlExecuteWorker.isFinished()) {
    		if (stmtExecutor != null) {
    			queuedSQLStatement = stmtExecutor;
    		}
    		return;
    	} else if (sqlExecuteWorker != null && sqlExecuteWorker.isFinished()) {
    		if (stmtExecutor != null) {
    			queuedSQLStatement = null;
    		} else if (stmtExecutor == null && queuedSQLStatement != null) {
    			StatementExecutor tempSQL = stmtExecutor;
    			stmtExecutor = queuedSQLStatement;
   				queuedSQLStatement = tempSQL;
    		}
    		sqlExecuteWorker.removeTaskTerminationListener(sqlExecuteTerminationListener);
    		sqlExecuteWorker = null;
    	}
    	
    	if (stmtExecutor == null) {
    		return;
    	}
    	if (databaseComboBox.getSelectedIndex() == -1) return;
    	ConnectionAndStatementBean conBean = conMap.get(databaseMapping.getDatabase((JDBCDataSource) databaseComboBox.getSelectedItem()));
    	try {
    		if(conBean!= null) {
    			if (!conBean.getConnection().getAutoCommit()) {
    				conBean.setConnectionUncommitted(true);
    			}
    		}
    	} catch (SQLException e1) {
    		SPSUtils.showExceptionDialogNoReport(dialogOwner, Messages.getString("SQLQuery.failedRetrievingConnection", ((SQLDatabase) databaseMapping.getDatabase((JDBCDataSource) databaseComboBox.getSelectedItem())).getName()), e1);
    	}
    	
    	prevQueryPosition = previousQueries.size();
    	previousQueries.add(stmtExecutor.getStatement());
		getPrevQueryButton().setEnabled(prevQueryPosition > 0);
		getNextQueryButton().setEnabled(prevQueryPosition < previousQueries.size() - 1);
    	
    	logger.debug("Executing SQL using executor type " + stmtExecutor.getClass());
    	sqlExecuteWorker = new ExecuteSQLWorker(swRegistry, stmtExecutor);
    	sqlExecuteWorker.addTaskTerminationListener(sqlExecuteTerminationListener);
    	new Thread(sqlExecuteWorker).start();
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
    public static JComponent createQueryPanel(SwingWorkerRegistry swRegistry, DataSourceCollection ds, SQLDatabaseMapping mapping, Window owner) {
    	return createQueryPanel(swRegistry, ds, mapping, owner, null, null);
    }

	/**
	 * Builds the UI of the {@link SQLQueryUIComponents}. If you just want an
	 * easy way to build a full-featured query UI and don't want to customize
	 * its internals, you have come to the right place. This also allows a SQL
	 * string to initialize the query UI with.
	 * 
	 * @param swRegistry
	 *            The registry with which all background tasks will be
	 *            registered. This argument must not be null.
	 * @param dsCollection
	 *            The collection of data sources that will be available for
	 *            querying from the UI. This argument must not be null.
	 * 
	 * @param ds
	 *            The data source that the initial query will be executed on.
	 *            This data source must be contained in the dsCollection and not
	 *            null for the query to be executed. If the data source is null
	 *            then the query will not be executed.
	 * 
	 * @param initialSQL
	 *            The string that will be executed immediately when the query
	 *            tool is shown. If this is null then no query will be executed.
	 */
    public static JComponent createQueryPanel(SwingWorkerRegistry swRegistry, DataSourceCollection dsCollection, SQLDatabaseMapping mapping, Window owner, SQLDatabase db, String initialSQL) {
        
        JPanel defaultQueryPanel = new JPanel();
        SQLQueryUIComponents queryParts = new SQLQueryUIComponents(swRegistry, dsCollection, mapping, defaultQueryPanel);
        queryParts.addWindowListener(owner);
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);
        toolbar.add(queryParts.getPrevQueryButton());
        toolbar.add(queryParts.getNextQueryButton());
        toolbar.addSeparator();
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
        JSpinner rowlimitSpinner = queryParts.getRowLimitSpinner();
        rowlimitSpinner.setValue(new Integer(1000));
        textAreaBuilder.append(rowlimitSpinner);
        textAreaBuilder.nextLine();
        textAreaBuilder.append(new RTextScrollPane(queryParts.getQueryArea(), true), 7);
        
        
        JSplitPane queryPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        
        queryPane.add(defaultQueryPanel, JSplitPane.TOP);
       
   
        queryPane.add(queryParts.getResultTabPane(), JSplitPane.BOTTOM);
        
        if (db != null && initialSQL != null && dsCollection.getConnections().contains(db.getDataSource())) {
        	queryParts.getDatabaseComboBox().setSelectedItem(db.getDataSource());
        	queryParts.getQueryArea().setText(initialSQL);
        	queryParts.executeQuery(initialSQL);
        }
        
        return queryPane;
  
    }
    
    
    /**
     * If the connection to the database currently selected in the combo box is not in 
     * auto commit mode then any changes will be committed.
     */
    private void commitCurrentDB() {
        ConnectionAndStatementBean conBean = conMap.get(databaseMapping.getDatabase((JDBCDataSource) databaseComboBox.getSelectedItem()));
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
        ConnectionAndStatementBean conBean = conMap.get(databaseMapping.getDatabase((JDBCDataSource) databaseComboBox.getSelectedItem()));
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
     * Creates all of the JTables for the result tab and adds them to the result tab.
     * @throws SQLException 
     */
    private synchronized void createResultSetTables(List<CachedRowSet> resultSets, StatementExecutor executor) throws SQLException {
    	clearResultTables(true);
   		for (StreamingRowSetListener rowSetListener : rowSetListeners) {
			rowSetListener.disconnect();
		}
   		rowSetListeners.clear();

    	searchDocument = new DefaultStyledDocument();
    	for (CachedRowSet rs : resultSets) {
    		final JTable tempTable;
    		FormLayout tableAreaLayout = new FormLayout("pref, 3dlu, pref:grow", "pref, fill:min(pref;50dlu):grow");
    		DefaultFormBuilder tableAreaBuilder = new DefaultFormBuilder(tableAreaLayout);

    		if (showSearchOnResults) {
    			JLabel searchLabel = new JLabel(ICON);
    			searchLabel.setToolTipText("Search");
    			JTextField tableFilterTextField = new JTextField(searchDocument, null, 0);
    			tableAreaBuilder.append(searchLabel, tableFilterTextField);
    		}
    		ResultSetTableModel model = new ResultSetTableModel(rs);
    		StreamingRowSetListener rowSetListener = new StreamingRowSetListener(rs, model);
    		rs.addRowSetListener(rowSetListener);
    		rowSetListeners.add(rowSetListener);
    		
    		tempTable = new FancyExportableJTable(model, searchDocument);
    		final TableModelListener tableListener = new TableModelListener() {
    		    public void tableChanged(TableModelEvent e) {
    		        tempTable.createDefaultColumnsFromModel();
    		    }
    		};
    		model.addTableModelListener(tableListener);

    		// Allow users to select a single table cell.
    		tempTable.setCellSelectionEnabled(true);

    		tableAreaBuilder.nextLine();
    		JScrollPane tableScrollPane = new JScrollPane(tempTable);
    		tableScrollPane.setPreferredSize(new Dimension(
    		        (int) tableScrollPane.getPreferredSize().getWidth(),
    		        0));
    		resultJTableScrollPanes.put(tempTable, tableScrollPane);
    		tableAreaBuilder.append(tableScrollPane, 3);

    		resultJTables.add((JTable)tempTable);
    		tableToSQLMap.put(((JTable)tempTable), executor.getStatement());
    		JPanel tempResultPanel = tableAreaBuilder.getPanel();
    		resultTabPane.add(Messages.getString("SQLQuery.result"), tempResultPanel);
    		resultTabPane.setSelectedIndex(1);

    	}
    	for (JTable table : resultJTables) {
    		for (TableChangeListener l : tableListeners) {
    			l.tableAdded(new TableChangeEvent(this, table));
    		}
    	}
    }

    /**
     * Removes all of the result tables that have been created by this class.
     * All existing table listeners will be sent a tableRemoved event.
     * 
     * @see #tableListeners
     * @param removeTabs
     *            If true the tabs that the result tables were in will be
     *            removed. If false, the notification will still take place,
     *            but the tabs (and the result set viewers associated with
     *            them) will be left in place. In that case, a subsequent call
     *            to this method will send the tableRemoved event to the tableListeners
     *            notifying about the same table being removed again.
     */
	private void clearResultTables(boolean removeTabs) {
		tableToSQLMap.clear();
    	for (JTable table : resultJTables) {
    		for (int i = tableListeners.size() - 1; i >= 0; i--) {
    			tableListeners.get(i).tableRemoved(new TableChangeEvent(this, table));
    		}
    	}
    	
    	if (removeTabs) {
    	    resultJTables.clear();
    	    resultJTableScrollPanes.clear();
    	    if(resultTabPane.getComponentCount() > 1) {
    	        for(int i = resultTabPane.getComponentCount()-1; i >= 1; i--){
    	            resultTabPane.remove(i);
    	        }
    	    }
    	}
	}
	
    /**
     * This will add a connection to the map of known connections.
     * 
     * <p>This is package private for testing.
     */
	void addConnection(SQLDatabase db) throws SQLObjectException {
		if (!conMap.containsKey(db)) {
			Connection con = db.getConnection();
			conMap.put(db, new ConnectionAndStatementBean(con));
        }
        try {
            autoCommitToggleButton.setSelected(conMap.get(db).getConnection().getAutoCommit());
        } catch (SQLException ex) {
            SPSUtils.showExceptionDialogNoReport(dialogOwner, Messages.getString("SQLQuery.failedConnectingToDB"), ex);
        }
        updateStatus();
        logTextArea.append("\n" + JDBCDataSource.getConnectionInfoString(db.getDataSource(), false) + "\n\n");
        logTextArea.setCaretPosition(logTextArea.getDocument().getLength());
	}
    
    public void addWindowListener(Window container){
    	container.addWindowListener(windowListener);
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
    
    public void setRowLimitSpinner(JSpinner newRowLimitSpinner) {
    	rowLimitSpinner = newRowLimitSpinner;
    }

    public RSyntaxTextArea getQueryArea() {
    	return queryArea;
    }

    public JTabbedPane getResultTabPane(){
    	return resultTabPane;
    }

    public ArrayList<JTable> getResultTables (){
    	return resultJTables;
    }

    public void addTableChangeListener(TableChangeListener l) {
    	tableListeners.add(l);
    }

    public void removeTableChangeListener(TableChangeListener l) {
    	tableListeners.remove(l);
    }

    public JTextArea getLogTextArea () {
    	return logTextArea;
    }

    public JPanel getFirstResultPanel() {
    	return firstResultPanel;
    }
    
    /**
     * This will return the query that made the JTable's result set.
     * If this returns null then the table has already been removed from the
     * results tab.
     */
    public String getQueryForJTable(JTable table) {
    	return tableToSQLMap.get(table);
    }
    
    public void disconnectListeners() {
    	dsCollection.removeDatabaseListChangeListener(dbListChangeListener);
   		for (StreamingRowSetListener rowSetListener : rowSetListeners) {
			rowSetListener.disconnect();
		}
    }
    
    public Document getSearchDocument() {
		return searchDocument;
	}
    
    public void setShowSearchOnResults(boolean showSearchOnResults) {
		this.showSearchOnResults = showSearchOnResults;
	}
    
    /**
     * Sets the data source combo box to the given data source.
     * 
     * <p> This is used for testing.
     */
    void setCurrentDataSource(SPDataSource ds) {
    	databaseComboBox.getModel().setSelectedItem(ds);
    }
    
    /**
     * Gets the currently executing SQL worker or null if none are currently executing.
     * 
     * <p>Used in testing.
     */
    ExecuteSQLWorker getSqlExecuteWorker() {
		return sqlExecuteWorker;
	}

	public JButton getNextQueryButton() {
		return nextQueryButton;
	}

	public JButton getPrevQueryButton() {
		return prevQueryButton;
	}
	
	/**
	 * This will create the an error Message String similar to the details in the Exception Dialog.
	 */
	public static String createErrorStringMessage(Throwable e) {
		StringWriter stringWriter = new StringWriter();
		PrintWriter traceWriter = new PrintWriter(stringWriter);
		stringWriter.write(Messages.getString("SQLQuery.queryFailed"));
		e.printStackTrace(traceWriter);
		stringWriter.write("\n\n");
		stringWriter.write(Messages.getString("SQLQuery.queryFailedSeeAbove", SPSUtils.getRootCause(e).getMessage()));
		return stringWriter.toString();
	}
}


