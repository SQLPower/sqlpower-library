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

package ca.sqlpower.swingui.querypen.event;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import ca.sqlpower.query.SQLJoin;
import ca.sqlpower.swingui.CursorManager;
import ca.sqlpower.swingui.querypen.ConstantPNode;
import ca.sqlpower.swingui.querypen.QueryPen;
import ca.sqlpower.swingui.querypen.UnmodifiableItemPNode;
import ca.sqlpower.swingui.querypen.MouseState.MouseStates;
import edu.umd.cs.piccolo.PCanvas;
import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.event.PBasicInputEventHandler;
import edu.umd.cs.piccolo.event.PInputEvent;

/**
 * Creates a join between two columns in two different tables.
 */
public class CreateJoinEventHandler extends PBasicInputEventHandler {
	
	private QueryPen queryPen;
	private UnmodifiableItemPNode leftText;
	private UnmodifiableItemPNode rightText;
	private PCanvas canvas;
	private CursorManager cursorManager;
	private double mouseFirstClickX;
	private double mouseSecondClickX;

	
	private PropertyChangeListener changeListener = new PropertyChangeListener() {
		public void propertyChange(PropertyChangeEvent evt) {
			for (PropertyChangeListener l : createJoinListeners) {
				l.propertyChange(evt);
			}
		}
	};
	
	private List<PropertyChangeListener> createJoinListeners = new ArrayList<PropertyChangeListener>();

	public CreateJoinEventHandler(QueryPen mouseStatePane, PCanvas canvas, CursorManager cursorManager) {
		this.queryPen = mouseStatePane;
		this.canvas = canvas;
		this.cursorManager = cursorManager;
		this.mouseFirstClickX=0;
		this.mouseSecondClickX=0;
	}
	
	@Override
	public void mousePressed(PInputEvent event) {
		super.mousePressed(event);
		if (queryPen.getMouseState().equals(MouseStates.CREATE_JOIN)) {
			PNode pick = event.getPickedNode();
			while (pick != null && !(pick instanceof UnmodifiableItemPNode)) {
				
				if(pick instanceof ConstantPNode) {
					JOptionPane.showMessageDialog(SwingUtilities.getWindowAncestor(canvas), "Joining on constants is not allowed.");
				}
				pick = pick.getParent();
			}
			if (pick != null) {
				if (leftText == null) {
					mouseFirstClickX = event.getPosition().getX();
					leftText = (UnmodifiableItemPNode)pick;
					leftText.setJoiningState(true);
				} else if (rightText == null) {
					leftText.setJoiningState(false);
					mouseSecondClickX = event.getPosition().getX();
					rightText = (UnmodifiableItemPNode)pick;
					if(leftText.getParent() == rightText.getParent()) {
						JOptionPane.showMessageDialog(SwingUtilities.getWindowAncestor(canvas), "You cannot join to your own Table.");
						 resetJoin();
						return;
					}
					if ( mouseFirstClickX != 0 && mouseSecondClickX!= 0 && mouseFirstClickX > mouseSecondClickX) {
						UnmodifiableItemPNode tempNode = leftText;
						leftText = rightText;
						rightText = tempNode;
						mouseFirstClickX = mouseSecondClickX = 0;
					}
					SQLJoin join = new SQLJoin(leftText.getItem(), rightText.getItem());
					join.addJoinChangeListener(changeListener);
					queryPen.getModel().addJoin(join);
					
					resetJoin();
				} else {
					throw new IllegalStateException("Trying to create a join while both ends have already been specified.");
				}
			} else {
				if(leftText != null) {
					leftText.setJoiningState(false);
				}
				 resetJoin();
			}
		}
	}
	
	private void resetJoin() {
		leftText = null;
		rightText = null;
		cursorManager.placeModeFinished();
		queryPen.setMouseState(MouseStates.READY);
	}
	
	public void addCreateJoinListener(PropertyChangeListener l) {
		createJoinListeners.add(l);
	}
	
	public void removeCreateJoinListener(PropertyChangeListener l) {
		createJoinListeners.remove(l);
	}
}
