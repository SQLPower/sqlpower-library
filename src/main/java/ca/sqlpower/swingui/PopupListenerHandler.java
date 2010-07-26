/*
 * Copyright (c) 2010, SQL Power Group Inc.
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

package ca.sqlpower.swingui;

import java.awt.Component;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Arrays;

import javax.swing.JComponent;
import javax.swing.Popup;

/**
 * This class is a helper class for the
 * {@link SPSUtils#popupComponent(Component, JComponent, java.awt.Point)} method.
 * This will add listeners to the popup for clicking on the glass pane and
 * resizing the owning frame. If the popup passed in is hidden in other places
 * the cleanup method should be called.
 */
public class PopupListenerHandler {
    
    private final MouseAdapter clickListener;
    private final ComponentListener resizeListener;
    private final Popup popup;
    private final JComponent glassPane;
    private final Component owningFrame;

	/**
	 * Creates a new {@link PopupListenerHandler} for the given {@link Popup},
	 * glass pane and owning frame associated with the {@link Popup}. A
	 * {@link ComponentListener} and {@link MouseAdapter} is also created to be
	 * attached to the glass pane and owning frame to determine when to cleanup
	 * the {@link Popup}.
	 * 
	 * @param popup
	 *            The {@link Popup} this class handles.
	 * @param glassPane
	 *            The {@link JComponent} that mouse clicks should be listened
	 *            for to figure out when to close the {@link Popup}.
	 * @param owningFrame
	 *            The {@link Component} that the {@link Popup} belongs to.
	 */
    public PopupListenerHandler(final Popup popup, final JComponent glassPane, final Component owningFrame) {
        this.popup = popup;
        this.glassPane = glassPane;
        this.owningFrame = owningFrame;
        
        clickListener = new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                super.mouseReleased(e);
                cleanup();
            }
        };

        resizeListener = new ComponentAdapter() {
            public void componentMoved(ComponentEvent e) {
                cleanup();
            }
        };
        
    }

	/**
	 * Removes the listeners this class added to the glass pane and the owning
	 * frame. This also hides the {@link Popup}.
	 */
    public void cleanup() {
        popup.hide();
        owningFrame.removeComponentListener(resizeListener);
        glassPane.removeMouseListener(clickListener);
    }

	/**
	 * Connects listeners to the glass frame and owning frame. This also shows
	 * the {@link Popup}.
	 */
    public void connect() {
    	if (!isPopupVisible()) {
    		popup.show();
    		owningFrame.addComponentListener(resizeListener);
    		glassPane.addMouseListener(clickListener);
    	}
    }

	/**
	 * Returns true if the {@link Popup} this class handles is visible. Since
	 * there is no available method in the {@link Popup} class to check whether
	 * it is visible, it checks if listeners are attached to the owning frame or
	 * glass pane instead.
	 */
    public boolean isPopupVisible() {
    	ComponentListener[] componentListeners = owningFrame.getComponentListeners();
    	if (Arrays.asList(componentListeners).contains(resizeListener)) {
    		return true;
    	}
    	
    	MouseListener[] mouseListeners = glassPane.getMouseListeners();
    	if (Arrays.asList(mouseListeners).contains(clickListener)) {
    		return true;
    	}
    	
    	return false;
    	
    }
    
}
