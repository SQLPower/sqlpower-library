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

import java.util.ArrayList;
import java.util.List;

/**
 * This interface extension adds methods to get and set
 * an error text on a data entry panel. Updates to the
 * UI are done through ErrorTextListeners.
 */
public abstract class ChangeListeningDataEntryPanel implements DataEntryPanel {
    
    public static interface ErrorTextListener {
        /**
         * This method is expected to update the UI with the given text.
         * If it is non-null and non-empty, it is expected to disable the user's
         * ability to apply changes to the panel. If not, it is expected to
         * collapse/hide the label and re-enable the panel.
         */            
        public void textChanged(String s);
    }
    
    private String text = null;
    private List<ErrorTextListener> listeners = new ArrayList<ErrorTextListener>();
    
    public void addErrorTextListener(ErrorTextListener l) {
        if (!listeners.contains(l)) {
            listeners.add(l);
        }
    }
    
    public void removeErrorTextListener(ErrorTextListener l) {
        if (!listeners.remove(l)) throw new IllegalArgumentException("This panel did not contain the specified ");
    }
    
    public String getErrorText() {
        return text;
    }
    
    /**
     * Displays an error text and disables the data panel so the user cannot apply changes.
     * @param s The error text to display. If null or null-string (after trim),
     * it will collapse the label, and enable the panel.
     */
    public void setErrorText(String s) {
        text = s;
        for (ErrorTextListener l : listeners) {
            l.textChanged(s);
        }
    }
}
