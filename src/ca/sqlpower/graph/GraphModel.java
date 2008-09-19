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

/**
 * Provides the model of a graph's topology.  Does not deal with positioning,
 * layout, selection, rendering, or any other visual aspect of the graph.
 *
 * @param <V> The node (vertex) type
 * @param <E> The edge type
 */
public interface GraphModel<V, E> {

    /**
     * Returns an unmodifiable collection of the edges in this graph. Although
     * client code is unable to modify the returned collection, the collection
     * may or may not change as the graph structure changes. Thus, it is best
     * for clients to frequently request new copies of the edge list rather than
     * rely on the list staying in sync.
     */
    Collection<E> getEdges();
    
    /**
     * Returns an unmodifiable collection of the nodes in this graph. Although
     * client code is unable to modify the returned collection, the collection
     * may or may not change as the graph structure changes. Thus, it is best
     * for clients to frequently request new copies of the node list rather than
     * rely on the list staying in sync.
     */
    Collection<V> getNodes();

    /**
     * Returns all nodes reachable from the given node by traversing exactly one
     * outbound edge. The given node will be included in the returned collection
     * only if the node is self-referencing.
     * <p>
     * Note that calling code must not attempt to modify the returned
     * collection.
     * 
     * @param node
     *            The node to get the adjacency information for.
     * @return Nodes adjacent to the given node.
     */
    Collection<V> getAdjacentNodes(V node);
    
    /**
     * Returns all edges that point from nodes the given node is adjacent to.
     * <p>
     * Note that calling code must not attempt to modify the returned
     * collection.
     * 
     * @param node The node to get the inbound edge list for.
     * @return All inbound edges for the given node.
     */
    Collection<E> getInboundEdges(V node);
    
    /**
     * Returns all edges that lead to the given node's adjacent nodes.
     * <p>
     * Note that calling code must not attempt to modify the returned
     * collection.
     * 
     * @param node The node to get the outbound edge list for.
     * @return All outbound edges for the given node.
     */
    Collection<E> getOutboundEdges(V node);
}
