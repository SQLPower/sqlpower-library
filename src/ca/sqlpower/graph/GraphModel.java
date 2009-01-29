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
