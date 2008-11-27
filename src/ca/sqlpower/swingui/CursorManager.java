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

package ca.sqlpower.swingui;

import java.awt.Cursor;

import javax.swing.JComponent;
import javax.swing.JPanel;

/**
 * A simple class that encapsulates the logic for making the cursor image
 * look correct for the current activity.
 */
public class CursorManager {
    
    private boolean draggingTable = false;
    private boolean dragAllModeActive = false;
    private boolean placeModeActive = false;
    private JComponent component;
    
    public CursorManager(JComponent component){
    	this.component = component;
    }
    
    public void tableDragStarted() {
        draggingTable = true;
        modifyCursorImage();
    }
    
    public void tableDragFinished() {
        draggingTable = false;
        modifyCursorImage();
    }
    
    public void dragAllModeStarted() {
        dragAllModeActive = true;
        modifyCursorImage();
    }
    
    public void dragAllModeFinished() {
        dragAllModeActive = false;
        modifyCursorImage();
    }

    public void placeModeStarted() {
        placeModeActive = true;
        modifyCursorImage();
    }

    public void placeModeFinished() {
        placeModeActive = false;
        modifyCursorImage();
    }
    
    /**
     * Sets the appropriate cursor type based on the current
     * state of this cursor manager.
     */
    private void modifyCursorImage() {
        if (dragAllModeActive || draggingTable) {
            component.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
        } else if (placeModeActive) {
            component.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
        } else {
            component.setCursor(null);
        }
    }
}
