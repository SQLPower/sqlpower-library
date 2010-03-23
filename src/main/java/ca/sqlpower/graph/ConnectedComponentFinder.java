/*
 * Copyright (c) 2008, SQL Power Group Inc.
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

package ca.sqlpower.graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.log4j.Logger;

/**
 * Implements an algorithm that partitions a graph into its set of
 * connected components
 */
public class ConnectedComponentFinder<V, E> {

    private static final Logger logger = Logger.getLogger(ConnectedComponentFinder.class);
    
    /**
     * A comparator for the components in the sets. This is used if we want to sort the
     * sets as we find the connected components.
     */
    private Comparator<V> comparator;
    
    public ConnectedComponentFinder() {
    	comparator = null;
    }
    
    public ConnectedComponentFinder(Comparator<V> c) {
    	comparator = c;
    }
    
    public Set<Set<V>> findConnectedComponents(GraphModel<V, E> model) {
        
        // all nodes in the graph that we have not yet assigned to a component
        final Set<V> undiscovered;
        if (comparator != null) {
            ArrayList<V> sortedNodes = new ArrayList<V>(model.getNodes());
            Collections.sort(sortedNodes, comparator);
            undiscovered = new LinkedHashSet<V>(sortedNodes);
        } else {
            undiscovered = new HashSet<V>();
            undiscovered.addAll(model.getNodes());
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Nodes to process:" + undiscovered);
                
        }

        // the current component of the graph we're discovering using the BFS
        final Set<V> thisComponent = new HashSet<V>();
        
        // the components we've finished discovering
        Set<Set<V>> components = new LinkedHashSet<Set<V>>();
        
        BreadthFirstSearch<V, E> bfs = new BreadthFirstSearch<V, E>();
        bfs.addBreadthFirstSearchListener(new BreadthFirstSearchListener<V>() {
            public void nodeDiscovered(V node) {
                undiscovered.remove(node);
                thisComponent.add(node);
            }
        });
        
        while (!undiscovered.isEmpty()) {
            
            logger.debug("Starting new BFS");
            
            V node = undiscovered.iterator().next();
            bfs.performSearch(model, node);
            
            if (logger.isDebugEnabled()) {
                logger.debug("  Search found "+thisComponent.size()+" nodes");
            }
            
            components.add(new HashSet<V>(thisComponent));
            thisComponent.clear();
        }
        
        return components;
    }
}
