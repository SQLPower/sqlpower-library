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
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JTree;
import javax.swing.tree.TreePath;

/**
 * An action for collapsing all the descendants of the selected path in a JTree.
 */
public class JTreeCollapseAllAction extends AbstractAction {
	
	private final JTree tree;
	
    public JTreeCollapseAllAction(JTree tree, String text) {
        super(text);
        this.tree = tree;
    }
    
    public void actionPerformed(ActionEvent e) {
       TreePath selected = tree.getSelectionPath();
       Enumeration<TreePath> descendants = tree.getExpandedDescendants(selected);
       if (descendants == null) return;
       
       List<TreePath> paths = Collections.list(descendants);
       
       // Sort required to make sure the parent is collapsed after child.
       Collections.sort(paths, new Comparator<TreePath>(){
        public int compare(TreePath o1, TreePath o2) {
            return o2.getPathCount() - o1.getPathCount();
        }
       });
       
       for (TreePath path : paths) {
           tree.collapsePath(path);
       }
       tree.collapsePath(selected);
    }

}