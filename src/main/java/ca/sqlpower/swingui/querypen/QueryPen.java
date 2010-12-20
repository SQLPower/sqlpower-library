/*
 * Copyright (c) 2008, SQL Power Group Inc.
 *
 * This file is part of Wabit.
 *
 * Wabit is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * Wabit is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>. 
 */

package ca.sqlpower.swingui.querypen;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apache.log4j.Logger;

import ca.sqlpower.object.SPObject;
import ca.sqlpower.object.SPVariableHelper;
import ca.sqlpower.query.Container;
import ca.sqlpower.query.Item;
import ca.sqlpower.query.Query;
import ca.sqlpower.query.QueryChangeEvent;
import ca.sqlpower.query.QueryChangeListener;
import ca.sqlpower.query.SQLJoin;
import ca.sqlpower.query.TableContainer;
import ca.sqlpower.sql.jdbcwrapper.DatabaseMetaDataDecorator;
import ca.sqlpower.sql.jdbcwrapper.DatabaseMetaDataDecorator.CacheType;
import ca.sqlpower.sqlobject.SQLDatabase;
import ca.sqlpower.sqlobject.SQLObject;
import ca.sqlpower.sqlobject.SQLObjectException;
import ca.sqlpower.sqlobject.SQLRelationship;
import ca.sqlpower.sqlobject.SQLRelationship.ColumnMapping;
import ca.sqlpower.sqlobject.SQLRelationship.SQLImportedKey;
import ca.sqlpower.sqlobject.SQLTable;
import ca.sqlpower.swingui.CursorManager;
import ca.sqlpower.swingui.dbtree.SQLObjectSelection;
import ca.sqlpower.swingui.querypen.event.CreateJoinEventHandler;
import ca.sqlpower.swingui.querypen.event.QueryPenSelectionEventHandler;
import ca.sqlpower.util.TransactionEvent;
import edu.umd.cs.piccolo.PCamera;
import edu.umd.cs.piccolo.PLayer;
import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.util.PPaintContext;
import edu.umd.cs.piccolox.pswing.PSwingCanvas;
import edu.umd.cs.piccolox.swing.PScrollPane;

/**
 * The pen where users can graphically create sql queries.
 */
public class QueryPen implements MouseState {
	
	private static Logger logger = Logger.getLogger(QueryPen.class);
	
	private static final Color SELECTION_COLOUR = new Color(0xcc333333);
	
    private static final String DELETE_ACTION = "Delete";
    
    private static final String BACKSPACE_ACTION = "Backspace";
    
    private static final String ZOOM_IN_ACTION = "Zoom In";
    
    private static final String ZOOM_OUT_ACTION = "Zoom Out";

    private static final String JOIN_ACTION = "Create Join";
    
	public static final Color SELECTED_CONTAINER_COLOUR = new Color(0xff9900);
	
	public static final Color SELECTED_CONTAINER_GRADIENT_COLOUR = new Color(0xffcc66);
    
	public static final Color UNSELECTED_CONTAINER_COLOUR = new Color(0x999999);
	
	public static final Color UNSELECTED_CONTAINER_GRADIENT_COLOUR = new Color(0xcccccc);
	
	public static final float CONTAINER_ROUND_CORNER_RADIUS = 8f;
	
	public static final Color WHERE_BACKGROUND_COLOUR = new Color(0xeeeeee);
	
	private static final String QUERY_EXECUTE = "Execute";
	
    private JPanel panel;
    
    
    private AbstractAction zoomInAction;
    private AbstractAction zoomOutAction;
    
	private final class QueryPenDropTargetListener implements
			DropTargetListener {
		

		public void dropActionChanged(DropTargetDragEvent dtde) {
			//no-op
		}

		public void drop(DropTargetDropEvent dtde) {
			if (!dtde.isLocalTransfer()) {
			    logger.debug("Rejecting non-local transfer");
			    dtde.rejectDrop();
				return;
			}
			
			if (!dtde.isDataFlavorSupported(SQLObjectSelection.LOCAL_SQLOBJECT_ARRAY_FLAVOUR)) {
                logger.debug("Rejecting transfer of unknown flavour");
                dtde.rejectDrop();
				return;
			}

			SQLObject[] draggedObjects;
			try {
				draggedObjects = (SQLObject[]) dtde.getTransferable().getTransferData(SQLObjectSelection.LOCAL_SQLOBJECT_ARRAY_FLAVOUR);
			} catch (UnsupportedFlavorException e) {
				dtde.dropComplete(false);
				throw new RuntimeException(e);
			} catch (IOException e) {
				dtde.dropComplete(false);
				throw new RuntimeException(e);
			}
			
			SQLDatabase tableDatabase = null;

			int response = 0;
			
			for (Object draggedSQLObject : draggedObjects) {
				if (draggedSQLObject instanceof SQLTable) {
			    	SQLTable table = (SQLTable) draggedSQLObject;
			    	if(tableDatabase != null) {
			    		if(!tableDatabase.equals(table.getParentDatabase())) {
			    			JOptionPane.showMessageDialog(null, "The tables your are adding are from different database connections. " +
			    					"This will cause errors in your query.", "Error", JOptionPane.ERROR_MESSAGE);
			    			response = 1;
			    		}
			    	}
					tableDatabase = table.getParentDatabase();
				} else {
					logger.debug("Rejecting drop of non-SQLTable SQLObject: " + draggedSQLObject);
					dtde.rejectDrop();
					return;
				}
			}
			
			//Check to see if the table being dragged in the selected database or not
			if(!getModel().getDatabase().equals(tableDatabase) && response == 0) {
				
				String message;
				if(draggedObjects.length == 1) { 
					message = "The table being added is not from \"" + getModel().getDatabase() + "\".";
				} else {
					message = "The tables being added are not from \"" + getModel().getDatabase() +  "\".";
				}
				
				message += "\nDo you want to change your connection to \"" + tableDatabase + "\"?\n" +
						"If so, your current query will be cleared and the new tables added.";
				
				String options[] = {"YES","NO"};
				response = JOptionPane.showOptionDialog(null, message, "Warning",
						JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, 
						null, options, options[1]);
				if(response == 0) {
					model.setDataSource(tableDatabase.getDataSource());
					model.reset();
				}
			}
			
			if(response == 0) {
				for (Object draggedSQLObject : draggedObjects) {
				    try {
				    	cursorManager.startWaitMode();
				    	SQLTable table = (SQLTable) draggedSQLObject;
				    	model.startCompoundEdit("Table " + table.getName() + " was dropped " +
				    			"on the query pen, adding it to the mode.");
				    	DatabaseMetaDataDecorator.putHint(DatabaseMetaDataDecorator.CACHE_TYPE, CacheType.EAGER_CACHE);
						TableContainer tableModel = new TableContainer(QueryPen.this.model.getDatabase(), table);
						
						Point location = dtde.getLocation();
						Point2D movedLoc = canvas.getCamera().localToView(location);
						tableModel.setPosition(movedLoc);
						
						int aliasCounter = 0;
						ArrayList<String> aliasNames = new ArrayList<String>();
						
						// This basically check if there exist a table with the same name as the one being dropped
						// compare all the alias names to see which number it needs to not create a duplicate table name.
						for (Container existingTable: model.getFromTableList()) {
							if (tableModel.getName().equals(existingTable.getName())){
								logger.debug("Found same tableName, going to alias");
								aliasNames.add(existingTable.getAlias());
							}
						}
						Collections.sort(aliasNames);
						for(String alias : aliasNames) {
							if(alias.equals(tableModel.getName()+ "_"+ aliasCounter)
									|| alias.equals("")) {
								aliasCounter++;
							}
						}
						if(aliasCounter != 0) {
							tableModel.setAlias(tableModel.getName()+ "_"+ aliasCounter);
						}
						
						QueryPen.this.model.addTable(tableModel);
						
						try {
							for (SQLRelationship relation : table.getExportedKeys()) {
								List<Container> fkContainers = getContainerPane(relation.getFkTable());
								for (Container fkContainer : fkContainers) {
									for (ColumnMapping mapping : relation.getChildren(ColumnMapping.class)) {
										logger.debug("PK container has model name " + tableModel.getName() + 
												" looking for col named " + mapping.getPkColumn().getName());
										Item pkItemNode = tableModel.getItem(mapping.getPkColumn());
										Item fkItemNode = fkContainer.getItem(mapping.getFkColumn());
										logger.debug("FK item node is " + fkItemNode);
										if (pkItemNode != null && fkItemNode != null) {
											if (pkItemNode.getParent() != fkItemNode.getParent()) {
											    SQLJoin join = new SQLJoin(pkItemNode, fkItemNode);
												join.addJoinChangeListener(queryChangeListener);
												QueryPen.this.model.addJoin(join);
											} else {
												logger.debug("we don't allow items joining on the same table");
											}
										} else {
											throw new IllegalStateException("Trying to join two columns, one of which does not exist");
										}
									}
								}
							}
							
							for (SQLImportedKey key : table.getImportedKeys()) {
								SQLRelationship relation = key.getRelationship();
								List<Container> pkContainers = getContainerPane(relation.getParent());
								for (Container pkContainer : pkContainers) {
									for (ColumnMapping mapping : relation.getChildren(ColumnMapping.class)) {
										Item fkItemNode = pkContainer.getItem(mapping.getPkColumn());
										Item pkItemNode = tableModel.getItem(mapping.getFkColumn());
										if (pkItemNode != null && fkItemNode != null) {
											if (pkItemNode.getParent() != fkItemNode.getParent()) {
												SQLJoin join = new SQLJoin(fkItemNode, pkItemNode);
												join.addJoinChangeListener(queryChangeListener);
												QueryPen.this.model.addJoin(join);
											} else {
												logger.debug("we don't allow items joining on the same table");
											}
										} else {
											throw new IllegalStateException("Trying to join two columns, one of which does not exist");
										}
									}
								}
							}
						} catch (SQLObjectException e) {
							throw new RuntimeException(e);
						}
						
	                    for (Item itemNode : tableModel.getItems()) {
	                        model.selectItem(itemNode);
	                    }
	
						dtde.acceptDrop(dtde.getDropAction());
						dtde.dropComplete(true);
				    } finally {
				    	cursorManager.finishWaitMode();
				    	DatabaseMetaDataDecorator.putHint(DatabaseMetaDataDecorator.CACHE_TYPE, CacheType.NO_CACHE);
				    	model.endCompoundEdit();
					}
				}
			}
		}

		public void dragOver(DropTargetDragEvent dtde) {
			//no-op
		}
		
		public void dragExit(DropTargetEvent dte) {
			//no-op
		}

		public void dragEnter(DropTargetDragEvent dtde) {
			//no-op
		}
	}

	private static final float SELECTION_TRANSPARENCY = 0.33f;

	/**
	 * The scroll pane that contains the visual query a user is building.
	 */
	private final JScrollPane scrollPane;

	/**
	 * The Piccolo canvas that allows zooming and the JComponents are placed in.
	 */
	private final PSwingCanvas canvas;
	
	/**
	 * The layer that contains all of the join lines. This will be behind the top layer.
	 */
	private final PLayer joinLayer;
	
	/**
	 * The top layer that has the tables and columns added to it. This should be used
	 * instead of getting the first layer from the canvas.
	 */
	private final PLayer topLayer;
	
	private final JButton createJoinButton;
	
	/**
	 * This text area is for any part of the WHERE clause
	 * that a user would want to add in that is not specific
	 * to a column in a table.
	 */
	private final JTextField globalWhereText;

	private final String acceleratorKeyString;
	
	/**
	 * This is the queryPen's model
	 */
	private final Query model;
	
	/**
	 * The mouse state in this query pen.
	 */
	private MouseStates mouseState = MouseStates.READY;
	
	/**
	 * A SelectionEventHandler that supports multiple select on Tables for deletion and dragging.
	 */
	private QueryPenSelectionEventHandler selectionEventHandler;
	
    /**
     * The cursor manager for this Query pen.
     */
	private final CursorManager cursorManager;
	
	/**
	 * This is the container pane that will hold constants to allow users to join on special
	 * things or add unusual values to a select statement.
	 */
	private ConstantsPane constantsContainer;
	
	/**
	 * This slider will zoom the canvas in and out.
	 */
	private JSlider zoomSlider;
	
	/**
	 * This panel contains the zoom slider and its associated images.
	 */
	private JPanel zoomSliderContainer;
	
	/**
	 * This action will be called when the query defined in this query pen is
	 * to be executed.
	 */
	private final Action executeQueryAction;

	/**
	 * Deletes the selected item from the QueryPen.
	 */
	private final Action deleteAction = new AbstractAction() {
		public void actionPerformed(ActionEvent e) {

			try {
				model.startCompoundEdit("Deleting objects from the query pen.");
				Iterator<?> selectedIter = selectionEventHandler.getSelection().iterator();
				while(selectedIter.hasNext()) {
					PNode pickedNode = (PNode) selectedIter.next();
					if (pickedNode.getParent() == topLayer) {
						if (pickedNode == constantsContainer) {
							return;
						}
						if (pickedNode instanceof ContainerPane) {
							ContainerPane pane = ((ContainerPane)pickedNode);
							deleteContainer(pane);
						}
					}
					if (pickedNode.getParent() == joinLayer) {
						deleteJoinLine((JoinLine)pickedNode);
					}
				}
			} finally {
				model.endCompoundEdit();
			}
		}
	};
	
	/**
	 * This will delete the given container from the model.
	 */
	public void deleteContainer(ContainerPane pickedNode) {
	    model.removeTable(pickedNode.getModel());
	}

    /**
     * This method will remove the Joined line from its left and right Nodes and
     * remove it from the joinLayer.
     */
	public void deleteJoinLine(JoinLine pickedNode) {
		model.removeJoin(pickedNode.getModel());
	}
	
	/**
	 * Listeners that will be notified when the query string has been modified.
	 */
	private List<PropertyChangeListener> queryListeners = new ArrayList<PropertyChangeListener>();

	/**
	 * This change listener will be invoked whenever a change is made to the query pen
	 * that will result in a change to the SQL script.
	 * 
	 * XXX Does anything use this?
	 */
	private PropertyChangeListener queryChangeListener = new PropertyChangeListener() {
		public void propertyChange(PropertyChangeEvent evt) {
			logger.debug("Query pen received state change.");
			for (PropertyChangeListener l : queryListeners) {
				l.propertyChange(evt);
			}
		}
	};

	private CreateJoinEventHandler joinCreationListener;

	/**
	 * This button will execute the query defined in the query pen.
	 */
    private final JButton playPenExecuteButton;

    /**
     * This button deletes the selected item in the query pen.
     */
    private final JButton deleteButton;
    
    /**
     * This button resets the query editor back to how it was originally
     * setup.
     */
    private final JButton resetButton;
    
    /**
     * This action will reset the query.
     */
    private final Action resetAction = new AbstractAction() {
    
        public void actionPerformed(ActionEvent e) {
            model.reset();
        }
    };
    
    /**
     * This toolbar will be placed on top of the query pen.
     */
    private JToolBar queryPenToolBar;

    /**
     * This listener will be attached to the query that is the model of this
     * component and handle all query changes.
     */
    private final QueryChangeListener queryListener = new QueryChangeListener() {
    
        public void propertyChangeEvent(PropertyChangeEvent evt) {
            //do nothing
        }
    
        public void joinRemoved(QueryChangeEvent evt) {
            SQLJoin removedJoin = evt.getJoinChanged();
            for (Object node : joinLayer.getAllNodes()) {
                if (node instanceof JoinLine && ((JoinLine) node).getModel() == removedJoin) {
                    JoinLine pickedNode = (JoinLine) node;
                    pickedNode.disconnectJoin();
                    joinLayer.removeChild(pickedNode);
                    break;
                }
            }
        }
    
        public void joinPropertyChangeEvent(PropertyChangeEvent evt) {
            //do nothing
        }
    
        public void joinAdded(QueryChangeEvent evt) {
            SQLJoin sqlJoin = evt.getJoinChanged();
            JoinLine join = new JoinLine(QueryPen.this, canvas, sqlJoin);
            joinLayer.addChild(join);
        }
    
        public void itemRemoved(QueryChangeEvent evt) {
            //do nothing
        }
    
        public void itemPropertyChangeEvent(PropertyChangeEvent evt) {
            //do nothing
        }
    
        public void itemAdded(QueryChangeEvent evt) {
            //do nothing
        }
    
        public void containerRemoved(QueryChangeEvent evt) {
            Container removedContainer = evt.getContainerChanged();
            ContainerPane removedPane = null;
            for (int i = 0; i < topLayer.getChildrenCount(); i++) {
                final PNode child = topLayer.getChild(i);
                if (child instanceof ContainerPane &&
                        ((ContainerPane) child).getModel().equals(removedContainer)) {
                    removedPane = ((ContainerPane) child);
                    break;
                }
            }
            if (removedPane == null) {
                JOptionPane.showMessageDialog(getCanvas(), "Cannot find the table " 
                        + removedContainer.getName() + " to remove from the query pen.", 
                        "Cannot find table.", JOptionPane.ERROR_MESSAGE);
                return;
            }
            topLayer.removeChild(removedPane);
            removedPane.removeQueryChangeListener(queryChangeListener);
        }
    
        public void containerAdded(QueryChangeEvent evt) {
        	Container containerChanged = evt.getContainerChanged();
			ContainerPane pane = new ContainerPane(QueryPen.this, canvas, containerChanged, variablesHelper);
			pane.addQueryChangeListener(queryChangeListener);
			topLayer.addChild(pane);
			
			canvas.repaint();
        }
    
        public void compoundEditEnded(TransactionEvent evt) {
            //do nothing
        }

        public void compoundEditStarted(TransactionEvent evt) {
            //do nothing
        }

    };

    /**
     * This action will allow users to create a join between two columns in
     * the query.
     */
    private final Action joinAction;

	private SPVariableHelper variablesHelper;

	public JPanel createQueryPen() {
        panel.setLayout(new BorderLayout());
        panel.add(getScrollPane(), BorderLayout.CENTER);
        
    	getPlayPenExecuteButton().setToolTipText(QUERY_EXECUTE + "(Shortcut "+ getAcceleratorKeyString()+ " R)");
    	canvas.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_R, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask())
                , QUERY_EXECUTE);
    	canvas.getActionMap().put(QUERY_EXECUTE, executeQueryAction);
        
        panel.setBackground(Color.WHITE);
		return panel;
	}
	
	private JToolBar createToolBar() {
	    JToolBar queryPenBarChild = new JToolBar(JToolBar.HORIZONTAL);
        queryPenBarChild.setFloatable(false);
        queryPenBarChild.add(getPlayPenExecuteButton());
        
        queryPenBarChild.addSeparator();
        
        queryPenBarChild.add(getResetButton());
        queryPenBarChild.add(getDeleteButton());
        queryPenBarChild.add(getCreateJoinButton());
        queryPenBarChild.addSeparator();
        queryPenBarChild.add(getZoomSliderContainer());
        
        queryPenBarChild.addSeparator();

        return queryPenBarChild;
	}

    /**
     * Creates a query pen set up with the given model.
     * 
     * @param executeQueryAction
     *            This action will be used to execute the query built in the
     *            query pen.
     * @param model
     *            This model will be used to set up the query pen initially and
     *            save changes to when the query pen changes.
     * @param toolBar
     *            This tool bar will be the tool bar defined to be at the top of
     *            the query pen. If the tool bar given is null a default tool
     *            bar will be created.
     */
	public QueryPen(Action executeQueryAction, Query model) {
	    this(executeQueryAction, model, true);
	}

    /**
     * Creates a query pen set up with the given model.
     * 
     * @param executeQueryAction
     *            This action will be used to execute the query built in the
     *            query pen.
     * @param model
     *            This model will be used to set up the query pen initially and
     *            save changes to when the query pen changes.
     * @param toolBar
     *            This tool bar will be the tool bar defined to be at the top of
     *            the query pen. If the tool bar given is null a default tool
     *            bar will be created.
     * @param showConstantContainer
     *            This value should be set to true if the constants container is
     *            to be shown in the query pen. In some cases we don't want to
     *            show the constants pane as we are only interested in using
     *            actual tables. The container still exists and gets hooked up
     *            but it is just not displayed.
     */
	public QueryPen(Action executeQueryAction, Query model, boolean showConstantContainer) {
        playPenExecuteButton = new JButton(executeQueryAction);
	    deleteButton = new JButton(getDeleteAction());
	    resetButton = new JButton(getResetAction());
        final ImageIcon deleteIcon = new ImageIcon(QueryPen.class.getClassLoader().getResource("ca/sqlpower/swingui/querypen/delete.png"));
        getDeleteButton().setToolTipText(DELETE_ACTION+ " (Shortcut Delete)");
        getDeleteButton().setIcon(deleteIcon);
		this.executeQueryAction = executeQueryAction;
		
		this.model = model;
		
		model.addQueryChangeListener(queryListener);
		panel = new JPanel();
	    cursorManager = new CursorManager(panel);
		if (System.getProperty("os.name").toLowerCase().startsWith("mac os x")) {
			acceleratorKeyString = "Cmd";
		} else {
			acceleratorKeyString = "Ctrl";
		}
		
		if (model instanceof SPObject) {
			this.variablesHelper = new SPVariableHelper((SPObject)model);
		} else {
			this.variablesHelper = null;
		}
		
		canvas = new PSwingCanvas();
		canvas.setBackground(Color.black);
		canvas.setAnimatingRenderQuality(PPaintContext.HIGH_QUALITY_RENDERING);
		canvas.setInteractingRenderQuality(PPaintContext.HIGH_QUALITY_RENDERING); 
		scrollPane = new PScrollPane(canvas);
		scrollPane.getVerticalScrollBar().setUnitIncrement(10);
		scrollPane.getHorizontalScrollBar().setUnitIncrement(10);
		
		canvas.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
	                KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), DELETE_ACTION);
	    canvas.getActionMap().put(DELETE_ACTION, getDeleteAction());
	    
	    if (System.getProperty("os.name").toLowerCase().startsWith("mac os x")) {
		    canvas.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
		    		KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), BACKSPACE_ACTION);
		    canvas.getActionMap().put(BACKSPACE_ACTION, getDeleteAction());
	    }
	    

        canvas.setPanEventHandler( null );
        topLayer = canvas.getLayer();
        joinLayer = new PLayer();
        canvas.getRoot().addChild(joinLayer);
        canvas.getCamera().addLayer(0, joinLayer);
               
        final int defaultSliderValue = 500;
        zoomSlider = new JSlider(JSlider.HORIZONTAL, 1, 1000, defaultSliderValue);
        zoomSlider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
                final double newScale = (double)zoomSlider.getValue()/defaultSliderValue;
                final PCamera camera = canvas.getCamera();
                double oldScale = camera.getViewScale();
                camera.scaleViewAboutPoint(newScale/oldScale, camera.getViewBounds().getCenterX(), camera.getViewBounds().getCenterY());
                logger.debug("Camera scaled by " + newScale/oldScale + " and is now at " + camera.getViewScale());
			}
		});
        zoomSlider.addMouseListener(new MouseAdapter() {
        	@Override
        	public void mouseReleased(MouseEvent e) {
        		if ((e.getModifiers() & Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()) > 0) {
        			zoomSlider.setValue(defaultSliderValue);
        		}
        	}
		});
        zoomInAction = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				zoomSlider.setValue(zoomSlider.getValue() + 50);
			}
		};
        zoomSliderContainer = new JPanel(new BorderLayout());
        zoomSliderContainer.setMaximumSize(new Dimension((int)zoomSlider.getPreferredSize().getWidth(), 200));
        zoomSliderContainer.add(zoomSlider, BorderLayout.CENTER);
        zoomSliderContainer.add(new JLabel(new ImageIcon(QueryPen.class.getClassLoader().getResource("ca/sqlpower/swingui/querypen/zoom_in16.png"))), BorderLayout.EAST);
        zoomSliderContainer.add(new JLabel(new ImageIcon(QueryPen.class.getClassLoader().getResource("ca/sqlpower/swingui/querypen/zoom_out16.png"))), BorderLayout.WEST);
        panel.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() | KeyEvent.SHIFT_MASK)
                , ZOOM_IN_ACTION);
        panel.getActionMap().put(ZOOM_IN_ACTION, zoomInAction);        
        zoomOutAction = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				zoomSlider.setValue(zoomSlider.getValue() - 50);
			}
		};
        panel.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() | KeyEvent.SHIFT_MASK)
                , ZOOM_OUT_ACTION);
        panel.getActionMap().put(ZOOM_OUT_ACTION, zoomOutAction);
        
        ImageIcon joinIcon = new ImageIcon(QueryPen.class.getClassLoader().getResource("ca/sqlpower/swingui/querypen/j.png"));
        joinAction = new AbstractAction() {
        	public void actionPerformed(ActionEvent e) {
        		setMouseState(MouseStates.CREATE_JOIN);
        		cursorManager.placeModeStarted();
        	}
        };
        createJoinButton = new JButton(getJoinAction());
        createJoinButton.setToolTipText(JOIN_ACTION + " (Shortcut "+ acceleratorKeyString+ " J)");
        createJoinButton.setIcon(joinIcon);
        canvas.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_J, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask())
                
                , JOIN_ACTION);
        canvas.getActionMap().put(JOIN_ACTION, getJoinAction());
        
        joinCreationListener = new CreateJoinEventHandler(this, canvas, cursorManager);
		canvas.addInputEventListener(joinCreationListener);
		joinCreationListener.addCreateJoinListener(queryChangeListener);
        
        new DropTarget(canvas, new QueryPenDropTargetListener());
        List<PLayer> layerList = new ArrayList<PLayer>();
        layerList.add(topLayer);
        layerList.add(joinLayer);
        selectionEventHandler = new QueryPenSelectionEventHandler(topLayer, layerList);
        selectionEventHandler.setMarqueePaint(SELECTION_COLOUR);
        selectionEventHandler.setMarqueePaintTransparency(SELECTION_TRANSPARENCY);
		canvas.addInputEventListener(selectionEventHandler);
		
		globalWhereText = new JTextField();
		globalWhereText.addFocusListener(new FocusListener() {
			public void focusLost(FocusEvent e) {
				queryChangeListener.propertyChange(new PropertyChangeEvent(globalWhereText, Container.PROPERTY_WHERE_MODIFIED, globalWhereText.getText(), globalWhereText.getText()));
			}
			public void focusGained(FocusEvent e) {
				//do nothing
			}
		});
		globalWhereText.addKeyListener(new KeyListener() {
			public void keyTyped(KeyEvent e) {
				//Do Nothing
			}
			public void keyReleased(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					queryChangeListener.propertyChange(new PropertyChangeEvent(globalWhereText, Container.PROPERTY_WHERE_MODIFIED, globalWhereText.getText(), globalWhereText.getText()));
				}
			}
			public void keyPressed(KeyEvent e) {
				//Do nothing
			}
		});
		
		setQueryPenToolBar(createToolBar());
		
		loadQueryCache(showConstantContainer);
	}
	
	/**
	 * This will load the related tables and their properties into the QueryPen.
	 */
	private void loadQueryCache(boolean showConstantContainer) {
        constantsContainer = new ConstantsPane(this, canvas, model.getConstantsContainer(), this.variablesHelper);
        constantsContainer.addChangeListener(queryChangeListener);
        
        if (showConstantContainer) {
            topLayer.addChild(constantsContainer);
        }
        
        Map<Item, UnmodifiableItemPNode> loadedItemPNodes = new HashMap<Item, UnmodifiableItemPNode>();
        for (Container c : model.getFromTableList()) {
        	ContainerPane container = new ContainerPane(this, canvas, c, this.variablesHelper);
			topLayer.addChild(container);
			for (UnmodifiableItemPNode node : container.getContainedItems()) {
				loadedItemPNodes.put(node.getItem(), node);
			}
        }
        for (SQLJoin join : model.getJoins()) {
        	joinLayer.addChild(new JoinLine(QueryPen.this, canvas, join));
        }
	}
	
	public QueryPenSelectionEventHandler getMultipleSelectEventHandler(){
		return selectionEventHandler;
	}
	
	public JScrollPane getScrollPane() {
		return scrollPane;
	}

	public JSlider getZoomSlider() {
		return zoomSlider;
	}
	
	public JPanel getZoomSliderContainer() {
		return zoomSliderContainer;
	}
	
	public JButton getCreateJoinButton() {
		return createJoinButton;
	}
	
	public PSwingCanvas getCanvas() {
		return canvas;
	}

	public MouseStates getMouseState() {
		return mouseState;
	}
	
	public PLayer getJoinLayer() {
		return joinLayer;
	}
	
	public PLayer getTopLayer() {
		return topLayer;
	}

	public synchronized void setMouseState(MouseStates mouseState) {
		this.mouseState = mouseState;
	}
	
	public Action getDeleteAction() {
		return deleteAction;
	}
	
	public Action getResetAction() {
	    return resetAction;
	}
	
	public JTextField getGlobalWhereText() {
		return globalWhereText;
	}
	
	public CursorManager getCursorManager() {
		return cursorManager;
	}

	/**
	 * Returns a list of containers, where each one wraps the same
	 * SQLTable, in the Query model. If no containers wraps the SQLTable in
	 * the Query model then this will return an empty list.
	 */
	private List<Container> getContainerPane(SQLTable table) {
		List<Container> containerList = new ArrayList<Container>();
		for (Container node : model.getFromTableList()) {
			if (node.getContainedObject() == table) {
				containerList.add(node);
			}
		}
		return containerList;
	}
	
	public void addQueryListener(PropertyChangeListener l) {
		queryListeners.add(l);
	}
	
	public void removeQueryListener(PropertyChangeListener l) {
		queryListeners.remove(l);
	}
	
	public Query getModel() {
		return model;
	}
	
	public String getAcceleratorKeyString () {
		return acceleratorKeyString;
	}
	
	public PSwingCanvas getQueryPenCanvas () {
		return canvas;
	}

	public void cleanup() {
	    model.removeQueryChangeListener(queryListener);
	    
		queryListeners.clear();
		for (Object o : topLayer.getAllNodes()) {
			if (o instanceof CleanupPNode) {
				((CleanupPNode)o).cleanup();
			}
		}
		
		for (Object o : joinLayer.getAllNodes()) {
			if (o instanceof CleanupPNode) {
				((CleanupPNode)o).cleanup();
			}
		}
	}

	public void setZoom(int zoomLevel) {
		if (zoomLevel > zoomSlider.getMinimum() && zoomLevel < zoomSlider.getMaximum()) {
			zoomSlider.setValue( zoomLevel);
		}
		
	}

    public JButton getPlayPenExecuteButton() {
        return playPenExecuteButton;
    }

    public JButton getDeleteButton() {
        return deleteButton;
    }

    public void setQueryPenToolBar(JToolBar queryPenToolBar) {
        this.queryPenToolBar = queryPenToolBar;
    }

    public JToolBar getQueryPenToolBar() {
        return queryPenToolBar;
    }
    
    public void setExecuteIcon(ImageIcon icon) {
        getPlayPenExecuteButton().setIcon(icon);
    }
    
    public JButton getResetButton() {
        return resetButton;
    }
    
    public Action getExecuteQueryAction() {
        return executeQueryAction;
    }

    public Action getJoinAction() {
        return joinAction;
    }
}
