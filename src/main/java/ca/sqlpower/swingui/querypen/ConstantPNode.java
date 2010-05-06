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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JCheckBox;

import org.apache.log4j.Logger;

import ca.sqlpower.object.SPVariableHelper;
import ca.sqlpower.query.Item;
import ca.sqlpower.query.StringItem;
import edu.umd.cs.piccolo.PCanvas;
import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolox.pswing.PSwing;

/**
 * This PNode displays a single constant. These PNodes will be contained by a 
 * {@link ConstantsPane}. A constant is allowed to be edited and can be removed
 * from the {@link ConstantsPane}.
 */
public class ConstantPNode extends PNode implements CleanupPNode {
	
	private static final Logger logger = Logger.getLogger(ConstantPNode.class);
	
	private static final int SPACING_SIZE = 8;
	private static final String LONG_EMPTY_STRING = "        ";
	
	/**
	 * The item this PNode is displaying.
	 */
	private final Item item;
	private JCheckBox selectionCheckbox;
	private EditablePStyledText constantText;

	private EditablePStyledText aliasText;

	private EditablePStyledTextWithOptionBox whereText;
	
	private final List<PropertyChangeListener> changeListeners;
	
	private EditStyledTextListener removeItemListener = new EditStyledTextListener() {
		private String oldText;
		public void editingStopping() {
			if (constantText.getEditorPane().getText().length() <= 0) {
				removeItem();
			} else if (item instanceof StringItem) {
				((StringItem)item).setName(constantText.getEditorPane().getText());
			}
			for (PropertyChangeListener listener : changeListeners) {
				listener.propertyChange(new PropertyChangeEvent(constantText, Item.PROPERTY_ITEM, oldText, constantText.getEditorPane().getText().trim()));
			}
		}
		public void editingStarting() {
			oldText = constantText.getEditorPane().getText();
		}
	};
	
	private void removeItem() {
		logger.debug("removing item");
		for (PropertyChangeListener l : changeListeners) {
			l.propertyChange(new PropertyChangeEvent(ConstantPNode.this, Item.PROPERTY_ITEM_REMOVED, item, null));
		}
		item.getContainer().removeItem(item);
	}
	
	/**
	 * Listens to the item that backs this node and updates its fields
	 * if the model changes.
	 * <p>
	 * This also re-fires events which may not be used in the future.
	 */
	private final PropertyChangeListener itemChangeListener = new PropertyChangeListener() {
		public void propertyChange(PropertyChangeEvent evt) {
			if (evt.getPropertyName().equals("selected")) {
				selectionCheckbox.setSelected(item.isSelected());
			} else if (evt.getPropertyName().equals("alias")) {
				aliasText.getEditorPane().setText(item.getAlias());	
				aliasText.syncWithDocument();
			} else if (evt.getPropertyName().equals("where")) {
				whereText.getEditorPane().setText(item.getWhere());	
				whereText.syncWithDocument();
			} else if (evt.getPropertyName().equals(Item.PROPERTY_ITEM)) {
				constantText.getEditorPane().setText(evt.getNewValue().toString());
				if (constantText.getEditorPane().getText().length() <= 0) {
					removeItem();
				} else {
				logger.debug("Model Name changed, updating view");
				constantText.syncWithDocument();
				}
			}
			
			for (PropertyChangeListener l : changeListeners) {
				l.propertyChange(evt);
			}
		}
	};
	
	/**
	 * The query this node is attached to as its parent.
	 */
    private final QueryPen queryPen;

    public ConstantPNode(Item source, QueryPen mouseStates, PCanvas canvas) {
    	this(source, mouseStates, canvas, null);
    }
    
	public ConstantPNode(Item source, QueryPen mouseStates, PCanvas canvas, SPVariableHelper variables) {
		this.item = source;
        this.queryPen = mouseStates;
		item.addPropertyChangeListener(itemChangeListener);
		
		changeListeners = new ArrayList<PropertyChangeListener>();
		
		selectionCheckbox = new JCheckBox();
		selectionCheckbox.setOpaque(false);
		selectionCheckbox.setSelected(item.isSelected());
		PSwing swingCheckbox = new PSwing(selectionCheckbox);
		addChild(swingCheckbox);
		selectionCheckbox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
			    if (selectionCheckbox.isSelected()) {
			        queryPen.getModel().selectItem(item);
			    } else {
			        queryPen.getModel().unselectItem(item);
			    }
			}
		});
		
		constantText = new EditablePStyledText(source.getName(), mouseStates, canvas);
		constantText.addEditStyledTextListener(removeItemListener);
		double yPos = (swingCheckbox.getFullBounds().getHeight() - constantText.getHeight())/2;
		constantText.translate(swingCheckbox.getFullBounds().getWidth() + SPACING_SIZE, yPos);
		addChild(constantText);
		
		if (item.getAlias().trim().length() > 0) {
			aliasText = new EditablePStyledText(item.getAlias(), mouseStates, canvas, LONG_EMPTY_STRING.length());
		} else {
			aliasText = new EditablePStyledText(LONG_EMPTY_STRING, mouseStates, canvas, LONG_EMPTY_STRING.length());
		}
		
		aliasText.addEditStyledTextListener(new EditStyledTextListener() {
			public void editingStopping() {
			    item.setAlias(aliasText.getEditorPane().getText().trim());
			}
			public void editingStarting() {
			    //do nothing
			}
		});
		aliasText.translate(swingCheckbox.getFullBounds().getWidth() + constantText.getWidth() + 2 * SPACING_SIZE, yPos);
		addChild(aliasText);
		
		if (item.getWhere().trim().length() > 0) {
			whereText = new EditablePStyledTextWithOptionBox(item.getWhere(), mouseStates, canvas, LONG_EMPTY_STRING.length(), variables);
		} else {
			whereText = new EditablePStyledTextWithOptionBox(LONG_EMPTY_STRING, mouseStates, canvas, LONG_EMPTY_STRING.length(), variables);
		}
		
		whereText.addEditStyledTextListener(new EditStyledTextListener() {
			public void editingStopping() {
			    item.setWhere(whereText.getEditorPane().getText().trim());
			}
			public void editingStarting() {
			    //do nothing
			}
		});
		whereText.translate(swingCheckbox.getFullBounds().getWidth() + constantText.getWidth() + aliasText.getWidth() + 3 * SPACING_SIZE, yPos);
		addChild(whereText);
	}
	
	public Item getItem() {
		return item;
	}
	
	public String getAlias() {
		return aliasText.getEditorPane().getText().trim();
	}
	
	public String getWhereText() {
		return whereText.getEditorPane().getText().trim();
	}
	
	public double getAliasOffset() {
		return selectionCheckbox.getWidth() + constantText.getWidth() + 2 * SPACING_SIZE;
	}
	
	public double getWhereOffset() {
		double offset = aliasText.getFullBounds().getX() + aliasText.getWidth() + SPACING_SIZE;
		logger.debug("Returning where offset of " + offset + " where position is currently " + whereText.getFullBounds().getX());
		return offset;
	}
	
	public void setAliasXPosition(double position) {
		aliasText.translate(position - aliasText.getFullBounds().getX(), 0);
		whereText.translate(position - aliasText.getFullBounds().getX(), 0);
	}
	
	public void setWhereXPosition(double position) {
		whereText.translate(position - whereText.getFullBounds().getX(), 0);
	}
	
	public void addChangeListener(PropertyChangeListener l) {
		changeListeners.add(l);
	}
	
	public void removeChangeListener(PropertyChangeListener l) {
		changeListeners.remove(l);
	}

	public void setSelected(boolean selected) {
		selectionCheckbox.setSelected(selected);
		if (selected) {
            queryPen.getModel().selectItem(item);
        } else {
            queryPen.getModel().unselectItem(item);
        }
	}
	
	public boolean isInSelect() {
		return selectionCheckbox.isSelected();
	}

	public void cleanup() {
		item.removePropertyChangeListener(itemChangeListener);
	}

	public Item getModel() {
		return item;
	}

}
