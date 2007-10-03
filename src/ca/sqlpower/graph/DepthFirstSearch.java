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

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

/**
 * The DepthFirstSearch class performs a depth-first search on a given
 * {@link GraphModel}, where vertices are of type V and the edges that connect
 * them are of type E.
 *
 * @param V the vertex type of the graph.
 * @param E The edge type of the graph.
 */
public class DepthFirstSearch<V, E> {
    
    private static final Logger logger = Logger.getLogger(DepthFirstSearch.class);
    
    /**
     * Maps the vertices of the graph) to their associated VertexInfo instances.  
     * VertexInfo objects store information about the DFS execution which will be 
     * of interest to users of the class. 
     */
    private Map<V, VertexInfo> vertexInfo;
    
    /**
     * Tracks the current visit time.  This variable is only useful
     * during the execution of the search.
     */
    private int visitTime;
    
    /**
     * Keeps track of the order the DFS finished with each of the vertices
     * in the graph.  The last vertex finished is at the head of the list.
     * This list constitutes a topological sort of the graph.
     * <p>
     * This is declared as a LinkedList so we can use the special addFirst()
     * method of LinkedList.
     */
    private LinkedList<V> finishOrder;

    /**
     * The VertexInfo class contains visit information related to the DFS
     * algorithm's discovery of a vertex in the graph.  It is capable of
     * classifying a vertex as "white," "grey," and "black" depending on
     * when it was started and finished by the DFS.
     */
    private class VertexInfo {
        
        /**
         * A serial number assigned to this vertex when it is first discovered
         * by the DFS.
         */
        private int discoveryTime;
        
        /**
         * A serial number assigned to this vertex when the DFS leaves it.
         */
        private int finishTime;
        
        /**
         * The vertex that the DFS was at when it discovered this vertex.
         * In SQLTable terms, the predecessor is a pkTable which exports its
         * key to this table.
         */
        private V predecessor;
        
        /**
         * Returns true iff this vertex has not been started (discovered) yet.
         */
        public boolean isWhite() {
            return (discoveryTime == 0 && finishTime == 0);
        }
        
        /**
         * Returns true iff this vertex has been started but not
         * finished.
         */
        public boolean isGrey() {
            return (discoveryTime != 0 && finishTime == 0);
        }

        /**
         * Returns true iff this vertex has been started and finished.
         */
        public boolean isBlack() {
            return finishTime != 0;
        }
        
        /**
         * See {@link #discoveryTime}.
         */
        public int getDiscoveryTime() {
            return discoveryTime;
        }
        /**
         * See {@link #discoveryTime}.
         */
        public void setDiscoveryTime(int discoveryTime) {
            this.discoveryTime = discoveryTime;
        }
        /**
         * See {@link #finishTime}.
         */
        public int getFinishTime() {
            return finishTime;
        }
        /**
         * See {@link #finishTime}.
         */
        public void setFinishTime(int finishTime) {
            this.finishTime = finishTime;
        }
        /**
         * See {@link #predecessor}.
         */
        public V getPredecessor() {
            return predecessor;
        }
        /**
         * See {@link #predecessor}.
         */
        public void setPredecessor(V predecessor) {
            this.predecessor = predecessor;
        }
    }
    
    public DepthFirstSearch(GraphModel<V, E> model) {
    	vertexInfo = new HashMap<V, VertexInfo>();
    	finishOrder = new LinkedList<V>();
    }

    /**
     * Performs a depth-first search on the given {@link GraphModel),
     * 
     * <p>This is an implementation of the DFS algorithm in section 23.3 of 
     * "Introduction to Algorithms" by Cormen et al (ISBN 0-07-013143-0).
     * 
     * @param model The {@link GraphModel} that the DFS will run on
     */
    public void performSearch(GraphModel<V,E> model) {
        if (logger.isDebugEnabled()) {
            logger.debug("Performing Search on: " + model);
        }
        Collection<V> vertices = model.getNodes();
        vertexInfo.clear();
        finishOrder.clear();
        for (V u : vertices) {
            vertexInfo.put(u, new VertexInfo());
        }
        visitTime = 0;
        for (V u : vertices) {
            VertexInfo vi = vertexInfo.get(u);
            if (vi.isWhite()) visit(u, model);
        }
    }

    /**
     * The recursive subroutine of performSearch.  Explores the connected
     * subgraph at u, colouring nodes as they are encountered. 
     * 
     * <p>This is an implementation of the DFS-VISIT routine in section
     * 23.3 of "Introduction to Algorithms" by Cormen et al (ISBN 
     * 0-07-013143-0).
     *
     * @param u
     * @param model
     */
    private void visit(V u, GraphModel<V, E> model) {
        VertexInfo vi = vertexInfo.get(u);
        vi.setDiscoveryTime(++visitTime);
        for (V v : model.getAdjacentNodes(u)) {
            VertexInfo vi2 = vertexInfo.get(v);
            if (vi2 == null) {
                logger.debug("Skipping vertex " + v + " because it is not in the set of tables to search");
            } else {
                if (vi2.isWhite()) {
                    vi2.setPredecessor(u);
                    visit(v, model);
                }
            }
        }
        vi.setFinishTime(++visitTime);
        finishOrder.addFirst(u);
    }

    
    
    /**
     * Gives back the order in which the vertices of these graphs were finished 
     * (coloured black) by the DFS. This list will be a topological sort of the graph.
     * 
     * <p>See {@link #finishOrder}.
     */
    public List<V> getFinishOrder() {
        return finishOrder;
    }
}
