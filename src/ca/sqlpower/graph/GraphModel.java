/*
 * Copyright (c) 2007, SQL Power Group Inc.
 *
 * This file is part of Power*MatchMaker.
 *
 * Power*MatchMaker is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * Power*MatchMaker is distributed in the hope that it will be useful,
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

    Collection<E> getEdges();
    Collection<V> getNodes();
    Collection<V> getAdjacentNodes(V node);
    Collection<E> getInboundEdges(V node);
    Collection<E> getOutboundEdges(V node);
}
