/*
 * Copyright (c) 2007, SQL Power Group Inc.
 * 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in
 *       the documentation and/or other materials provided with the
 *       distribution.
 *     * Neither the name of SQL Power Group Inc. nor the names of its
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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
