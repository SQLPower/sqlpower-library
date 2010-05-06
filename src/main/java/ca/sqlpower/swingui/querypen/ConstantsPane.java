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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.geom.Point2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JCheckBox;

import org.apache.log4j.Logger;

import ca.sqlpower.object.SPVariableHelper;
import ca.sqlpower.query.Container;
import ca.sqlpower.query.ContainerChildEvent;
import ca.sqlpower.query.ContainerChildListener;
import ca.sqlpower.query.Item;
import ca.sqlpower.query.StringItem;
import edu.umd.cs.piccolo.PCanvas;
import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.event.PDragSequenceEventHandler;
import edu.umd.cs.piccolo.event.PInputEvent;
import edu.umd.cs.piccolo.nodes.PPath;
import edu.umd.cs.piccolo.util.PPickPath;
import edu.umd.cs.piccolox.event.PNotification;
import edu.umd.cs.piccolox.event.PNotificationCenter;
import edu.umd.cs.piccolox.event.PSelectionEventHandler;
import edu.umd.cs.piccolox.nodes.PClip;
import edu.umd.cs.piccolox.nodes.PStyledText;
import edu.umd.cs.piccolox.pswing.PSwing;

/**
 * This PNode will contain all of the constants defined in the query. The constants
 * will be able to be aliased, joined on, and filtered same as other columns. Any
 * joins specified to this PNode will have the boolean operation placed in the where
 * clause as there is no actual table to join on. 
 */
public class ConstantsPane extends PNode implements CleanupPNode {
	
	private static final Logger logger = Logger.getLogger(ConstantsPane.class);
	
	private static final String TITLE_STRING = "CONSTANTS";
	
	private static final int BORDER_SIZE = 5;
	
	private static final int STROKE_SIZE = 2;
	
	/**
	 * The string in the addingNewItemPNode so users can tell where to click
	 * to add a new item string.
	 */
	private static final String ADDING_ITEM_STRING = "Add...";
	
	private final PCanvas canvas;
	private final QueryPen queryPen;

	private final Container model;
	
	private final List<PropertyChangeListener> changeListeners;
	
	private PPath outerRect;

	/**
	 * The text that defines the alias column.
	 */
	private PStyledText aliasHeader;

	private PStyledText whereHeader;

	private PStyledText columnHeader;

	/**
	 * This styled text will be placed at the bottom of this PNode to allow
	 * users to specify new constants.
	 */
	private EditablePStyledText addingNewItemPNode;

	/**
	 * This contains all the {@link ConstantPNode} objects that are contained
	 * and displayed by this ConstantsPane.
	 */
	private final List<ConstantPNode> constantPNodeList;

	/**
	 * This moves the PNode as the model's position moves.
	 * <p>
	 * This refires the property change event. This may no longer be necessary.
	 */
	private final PropertyChangeListener itemChangedListener = new PropertyChangeListener() {
		public void propertyChange(PropertyChangeEvent evt) {
			if (evt.getPropertyName().equals("position")) {
				translate(model.getPosition().getX() - getGlobalBounds().getX(), 
						model.getPosition().getY() - getGlobalBounds().getY());
			}
			
			for (PropertyChangeListener l : changeListeners) {
				l.propertyChange(evt);
			}
		}
	};

	/**
     * This listener adds items to this container when items are added to
     * the model. It also removes items from this container when they're
     * removed from the model.
     */
	private final ContainerChildListener childListener = new ContainerChildListener() {
    
        public void containerChildRemoved(ContainerChildEvent evt) {
            int constantPosition = -1;
            for (ConstantPNode constantNode : constantPNodeList) {
                if (constantNode.getItem() == evt.getChild()) {
                    constantPosition = constantPNodeList.indexOf(constantNode);
                    constantPNodeList.remove(constantNode);
                    constantNode.removeChangeListener(resizeListener);
                    ConstantsPane.this.removeChild(constantNode);
                    break;
                }
            }
            if (constantPosition != -1) {
                for (int i = constantPosition; i < constantPNodeList.size(); i++) {
                    constantPNodeList.get(i).translate(0, -title.getHeight() - BORDER_SIZE);
                }
                repositionAndResize();
            }
        }
    
        public void containerChildAdded(ContainerChildEvent evt) {
            addItem((Item)evt.getChild());
        }
    };
	
	/**
	 * The styled text that displays the title of this PNode.
	 */
	private PStyledText title;
	
	/**
	 * Listens for changes to the contained constants and resizes the window appropriately.
	 */
	private PropertyChangeListener resizeListener = new PropertyChangeListener() {
		public void propertyChange(PropertyChangeEvent evt) {
			for (PropertyChangeListener listener : changeListeners) {
				listener.propertyChange(evt);
			}
			repositionAndResize();
		}
	};
	
	/**
	 * This node contains all of the column headers for this pane.
	 */
	private PNode header;

	/**
	 * This checkbox will allow the user to check and uncheck all of the constanst in one click.
	 */
	private PSwing allSelectCheckbox;

	/**
	 * This header background will be placed behind the container header and the
	 * column headers. This will allow specifying a gradient across the header.
	 * This background needs to be taller than its clipping region so the
	 * rounded bottom gets clipped away.
	 */
	private PPath headerBackground;

	/**
	 * This clipping region will clip the header background to give it a flat line at the bottom.
	 */
	private PClip headerBackClip;

	/**
	 * This is the background for the area where the user can add where text.
	 */
	private PNode whereBackground;

	private final SPVariableHelper variables;
	
	public ConstantsPane(QueryPen mouseState, PCanvas canvas, Container containerModel) {
		this(mouseState, canvas, containerModel, null);
	}
	
	public ConstantsPane(QueryPen mouseState, PCanvas canvas, Container containerModel, SPVariableHelper variables) {
		this.queryPen = mouseState;
		this.canvas = canvas;
		this.model = containerModel;
		this.variables = variables;

		changeListeners = new ArrayList<PropertyChangeListener>();
		constantPNodeList = new ArrayList<ConstantPNode>();
		
		model.addPropertyChangeListener(itemChangedListener);
		model.addChildListener(childListener);
		
		title = new EditablePStyledText(TITLE_STRING, mouseState, canvas);
		addChild(title);
		
		header = new PNode();
		header.translate(0, title.getHeight() + BORDER_SIZE);
		final JCheckBox checkbox = new JCheckBox();
		checkbox.setOpaque(false);
		checkbox.addActionListener(new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				for (ConstantPNode node : constantPNodeList) {
					if(node.isInSelect() != ((JCheckBox)e.getSource()).isSelected()) {
						node.setSelected(checkbox.isSelected());
					}
				}
			}
		});
		allSelectCheckbox = new PSwing(checkbox);
		header.addChild(allSelectCheckbox);
		columnHeader = new EditablePStyledText("select all/none", mouseState, canvas);
		double headerYPos = (allSelectCheckbox.getFullBounds().getHeight() - columnHeader.getHeight())/2;
		double checkboxWidth = allSelectCheckbox.getFullBounds().getWidth();
		columnHeader.translate(checkboxWidth , headerYPos);
		header.addChild(columnHeader);
		aliasHeader = new EditablePStyledText("alias", mouseState, canvas);
		aliasHeader.translate(checkboxWidth + columnHeader.getWidth() + 2 * BORDER_SIZE, headerYPos);
		header.addChild(aliasHeader);
		whereHeader = new EditablePStyledText("where", mouseState, canvas);
		double whereHeaderX = checkboxWidth + columnHeader.getWidth() + aliasHeader.getWidth() + 3 * BORDER_SIZE;
		whereHeader.translate(whereHeaderX, headerYPos);
		header.addChild(whereHeader);
		addChild(header);
		
		addingNewItemPNode = new EditablePStyledText(ADDING_ITEM_STRING, mouseState, canvas);
		addingNewItemPNode.addEditStyledTextListener(new EditStyledTextListener() {
			public void editingStopping() {
				String text = addingNewItemPNode.getEditorPane().getText();
				if (!text.equals(ADDING_ITEM_STRING) && text.trim().length() > 0) {
					model.addItem(new StringItem(text));
				}
				addingNewItemPNode.getEditorPane().setText(ADDING_ITEM_STRING);
				addingNewItemPNode.syncWithDocument();
			}
			public void editingStarting() {
				addingNewItemPNode.getEditorPane().setText("");
			}
		});
		addingNewItemPNode.translate(0, (title.getHeight() + BORDER_SIZE) * 2 + BORDER_SIZE);
		addChild(addingNewItemPNode);
		
		outerRect = PPath.createRoundRectangle((float)-BORDER_SIZE, (float)-BORDER_SIZE, (float)(getFullBounds().getWidth() + 2 * BORDER_SIZE), (float)(getFullBounds().getHeight() + 3 * BORDER_SIZE), QueryPen.CONTAINER_ROUND_CORNER_RADIUS, QueryPen.CONTAINER_ROUND_CORNER_RADIUS);
		outerRect.setStroke(new BasicStroke(STROKE_SIZE));
		headerBackground = PPath.createRoundRectangle((float)-BORDER_SIZE, (float)-BORDER_SIZE, (float)outerRect.getWidth() - 1, (float)(title.getHeight() + BORDER_SIZE) * 2 + BORDER_SIZE + QueryPen.CONTAINER_ROUND_CORNER_RADIUS, QueryPen.CONTAINER_ROUND_CORNER_RADIUS, QueryPen.CONTAINER_ROUND_CORNER_RADIUS);
		headerBackground.setStrokePaint(new Color(0x00ffffff, true));
		headerBackClip = new PClip();
		headerBackClip.addChild(headerBackground);
		float headerClipHeight = (float)(title.getHeight() + BORDER_SIZE) * 2 + 2 * BORDER_SIZE;
		headerBackClip.setPathToRectangle((float)outerRect.getX(), (float)outerRect.getY(), (float)outerRect.getWidth(), headerClipHeight);
		headerBackClip.setStrokePaint(new Color(0x00ffffff, true));
		whereBackground = new PNode();
		whereBackground.translate(outerRect.getX() + whereHeaderX + BORDER_SIZE, outerRect.getY() + headerClipHeight);
		whereBackground.setWidth(outerRect.getWidth() - whereHeaderX - STROKE_SIZE - BORDER_SIZE - 1);
		whereBackground.setHeight(outerRect.getHeight() - headerClipHeight - STROKE_SIZE - 1);
		whereBackground.setPaint(QueryPen.WHERE_BACKGROUND_COLOUR);
		addChild(whereBackground);
		addChild(headerBackClip);
		addChild(outerRect);
		whereBackground.moveToBack();
		headerBackClip.moveToBack();
		outerRect.moveToBack();
		setBounds(outerRect.getBounds());
		translate(-getGlobalBounds().getX(), -getGlobalBounds().getY());
		translate(containerModel.getPosition().getX(), containerModel.getPosition().getY());
		logger.debug("Loaded constants pane in position " + getGlobalBounds().getX() + ", " + getGlobalBounds().getY());
		
		for (Item item : model.getItems()) {
			addItem(item);
		}
		
		addInputEventListener(new PDragSequenceEventHandler() {
			@Override
			protected void endDrag(PInputEvent e) {
				super.endDrag(e);
				model.setPosition(new Point2D.Double(getGlobalBounds().getX(), getGlobalBounds().getY()));
				logger.debug("Setting position " + getGlobalBounds().getX() + ", " + getGlobalBounds().getY());
			}
		});

		PNotificationCenter.defaultCenter().addListener(this, "setFocusAppearance", PSelectionEventHandler.SELECTION_CHANGED_NOTIFICATION, null);
		setFocusAppearance(new PNotification(null, null, null));
		
	}
	
	/**
	 * This method will shift the columns so they fit the maximum value as well
	 * as move the adding field and resize the outer rectangle and overall bounds.
	 */
	private void repositionAndResize() {
		addingNewItemPNode.translate(0, (title.getHeight() + BORDER_SIZE) * (2 + constantPNodeList.size()) + BORDER_SIZE - addingNewItemPNode.getFullBounds().getY());
		
		double translateAliasX = columnHeader.getFullBounds().getX() + columnHeader.getWidth() + BORDER_SIZE;
		for (ConstantPNode node : constantPNodeList) {
			translateAliasX = Math.max(translateAliasX, node.getAliasOffset());
		}
		aliasHeader.translate(translateAliasX - aliasHeader.getFullBounds().getX(), 0);
		whereHeader.translate(translateAliasX - aliasHeader.getFullBounds().getX(), 0);
		for (ConstantPNode node : constantPNodeList) {
			node.setAliasXPosition(translateAliasX);
		}
		
		double translateWhereX = aliasHeader.getFullBounds().getX() + aliasHeader.getWidth() + BORDER_SIZE;
		logger.debug("Translating where: max x is " + translateWhereX);
		for (ConstantPNode node : constantPNodeList) {
			translateWhereX = Math.max(translateWhereX, node.getWhereOffset());
			logger.debug("Translating where: max x is " + translateWhereX);
		}
		logger.debug("Translating where header " + translateWhereX + " from " + whereHeader.getFullBounds().getX());
		whereHeader.translate(translateWhereX - whereHeader.getFullBounds().getX(), 0);
		for (ConstantPNode node : constantPNodeList) {
			node.setWhereXPosition(translateWhereX);
		}
		
		double maxWidth = header.getFullBounds().getWidth();
		for (ConstantPNode node : constantPNodeList) {
			maxWidth = Math.max(maxWidth, node.getFullBounds().getWidth());
		}
		outerRect.setWidth(maxWidth + 2 * BORDER_SIZE);
		headerBackground.setWidth(maxWidth + 2 * BORDER_SIZE);
		headerBackClip.setWidth(maxWidth + 2 * BORDER_SIZE);
				
		outerRect.setHeight((title.getHeight() + BORDER_SIZE) * (3 + constantPNodeList.size()) + (2 * BORDER_SIZE));
		
		whereBackground.translate(translateWhereX - whereBackground.getFullBounds().getX(), 0);
		whereBackground.setWidth(outerRect.getWidth() - whereBackground.getFullBounds().getX() - STROKE_SIZE - BORDER_SIZE - 1);
		whereBackground.setHeight(outerRect.getHeight() - whereBackground.getFullBounds().getY() - STROKE_SIZE - BORDER_SIZE - 1);
		
		setBounds(outerRect.getBounds());
	}
	
	@Override
	/*
	 * Taken from PComposite. This keeps the title and container lines together in
	 * a unit but is modified to allow picking of internal components.
	 */
	public boolean fullPick(PPickPath pickPath) {
		if (super.fullPick(pickPath)) {
			
			PNode picked = pickPath.getPickedNode();
			
			// this code won't work with internal cameras, because it doesn't pop
			// the cameras view transform.
			
			//---Clickable elements
			for (PNode node : constantPNodeList) {
				if (node.getAllNodes().contains(picked)) {
					return true;
				}
			}
			
			if (picked == addingNewItemPNode || picked == allSelectCheckbox) {
				return true;
			}
			//---End clickable elements
			
			while (picked != this) {
				pickPath.popTransform(picked.getTransformReference(false));
				pickPath.popNode(picked);
				picked = pickPath.getPickedNode();
			}
			
			return true;
		}
		return false;
	}
	
    /**
     * This method should be called when the focus of this container changes. It
     * can be called through reflection from the {@link PNotificationCenter}.
     * 
     * @param notification
     *            The notification event from the {@link PNotificationCenter}.
     */
	public void setFocusAppearance(PNotification notification) {
		boolean hasFocus = queryPen.getMultipleSelectEventHandler().getSelection().contains(this);
		if (hasFocus) {
			outerRect.setStrokePaint(QueryPen.SELECTED_CONTAINER_COLOUR);
			headerBackground.setPaint(QueryPen.SELECTED_CONTAINER_COLOUR);
			moveToFront();
		} else {
			outerRect.setStrokePaint(QueryPen.UNSELECTED_CONTAINER_COLOUR);
			headerBackground.setPaint(QueryPen.UNSELECTED_CONTAINER_GRADIENT_COLOUR);
		}
	}
	
	/**
	 * Adds the item to this ConstanstPane in a new ConstantPNode.
	 */
	public void addItem(Item item) {
		ConstantPNode newConstantNode = new ConstantPNode(item, queryPen, canvas, this.variables);
		newConstantNode.addChangeListener(resizeListener);
		newConstantNode.translate(0, (title.getHeight() + BORDER_SIZE) * (2 + constantPNodeList.size()) + BORDER_SIZE);
		constantPNodeList.add(newConstantNode);
		ConstantsPane.this.addChild(newConstantNode);
		repositionAndResize();
	}
	
	public void addChangeListener(PropertyChangeListener l) {
		changeListeners.add(l);
	}
	
	public void removeChangeListener(PropertyChangeListener l) {
		changeListeners.remove(l);
	}

	public void cleanup() {
		model.removePropertyChangeListener(itemChangedListener);
		model.removeChildListener(childListener);
		for (Object o : getAllNodes()) {
			if (o instanceof CleanupPNode && o != this) {
				((CleanupPNode)o).cleanup();
			}
		}
		PNotificationCenter.defaultCenter().removeListener(this);
	}

	public Container getModel() {
		return model;
	}

}
