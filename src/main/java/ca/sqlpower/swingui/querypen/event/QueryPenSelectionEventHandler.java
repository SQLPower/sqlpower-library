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

import java.util.Iterator;
import java.util.List;

import ca.sqlpower.swingui.querypen.ConstantsPane;
import ca.sqlpower.swingui.querypen.ContainerPane;
import ca.sqlpower.swingui.querypen.JoinLine;
import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.event.PInputEvent;
import edu.umd.cs.piccolo.util.PDimension;
import edu.umd.cs.piccolox.event.PSelectionEventHandler;

/**
 * This selection handler is specific to the QueryPen. In the query pen while we
 * want to be able to select both the tables and the joins we only want to drag
 * the tables as the joins should update in relation to the tables.
 * <p>
 * If at a later date we want to allow moving the middle portion of the joins to
 * other locations then we can update this to allow dragging the middle of the
 * components as the end points of the splines would have to calculate where the
 * tables are.
 */
public class QueryPenSelectionEventHandler extends PSelectionEventHandler {
	
	public QueryPenSelectionEventHandler(PNode marqueeParent,
			List<?> selectableParents) {
		super(marqueeParent, selectableParents);
	}

	/*
	 * The regular docs comes from PSelectionEventHandler This is a modified
	 * version from PSelectionEventHandler. We only want to allow dragging of
	 * ContainerPane objects and the ConstantsPane. Joins will update to 
	 * connect to the moved container panes appropriately and should not
	 * be moved in addition to relocating their ends.
	 */
	@Override
	protected void dragStandardSelection(PInputEvent e) {
		// There was a press node, so drag selection
		PDimension d = e.getCanvasDelta();
		e.getTopCamera().localToView(d);

		PDimension gDist = new PDimension();
		Iterator<?> selectionEn = getSelection().iterator();
		while (selectionEn.hasNext()) {
			PNode node = (PNode) selectionEn.next();

			if (node instanceof ContainerPane || node instanceof ConstantsPane) {
				gDist.setSize(d);
				if (node.getParent() != null) {
					// This check is necessary because the parent might
					// be null if we are hadling an event that was queued
					// after a node deletion. The parent will be null and throw a NPE.
					node.getParent().globalToLocal(gDist);
					node.offset(gDist.getWidth(), gDist.getHeight());
				}
			}
		}	
	}
	/*
	 * we need to override the startStandard Selection since it deselects everything when
	 * you click on something other then the selected Items(such as its children)
	 */
	@Override
	protected void startStandardSelection(PInputEvent pie) {
		// Option indicator not down - clear selection, and start fresh
		PNode pickedNode = pie.getPath().getPickedNode();
		
		while (pickedNode != null) {
			if(pickedNode instanceof ConstantsPane 
					|| pickedNode instanceof ContainerPane 
					|| pickedNode instanceof JoinLine) {
				break;
			}
			pickedNode = pickedNode.getParent();
		}
		
		if (pickedNode == null) {
			return;
		}
		
		if (!(getSelection().contains(pickedNode) && (pickedNode instanceof ContainerPane
				|| pickedNode instanceof ConstantsPane))) {
			unselectAll();
		}

		if (isSelectable(pickedNode)) {
			select(pickedNode);
		}
	}
	
	@Override
	public void decorateSelectedNode(PNode node) {
		//Containers should decorate themselves not add resize bubbles.
	}
	
}
