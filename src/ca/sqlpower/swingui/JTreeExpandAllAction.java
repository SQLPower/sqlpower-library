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

package ca.sqlpower.swingui;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JTree;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

/**
 * An action for expanding all the descendants of the selected path in a JTree.
 */
public class JTreeExpandAllAction extends AbstractAction {
	
	private final JTree tree;
	
    public JTreeExpandAllAction(JTree tree, String text) {
        super(text);
        this.tree = tree;
    }
    
    public void actionPerformed(ActionEvent e) {
        TreePath selected = tree.getSelectionPath();
        if (selected == null) return;
        tree.expandPath(selected);
        expandChildren(selected);
    }
    
    private void expandChildren(TreePath parentPath) {
        Object parent = parentPath.getLastPathComponent();
        TreeModel model = tree.getModel();
        for (int i = 0 ; i < model.getChildCount(parent); i++) {
            Object child = model.getChild(parent, i);
            TreePath childPath = parentPath.pathByAddingChild(child);
            
            tree.expandPath(childPath);
            if (!model.isLeaf(child)) {
                expandChildren(childPath);
            }
        }
    }
}
