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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

/**
 * This class implements Dijkstra's algorithm.
 *
 * @param <V> The type of vertices in the graph that will be used.
 * @param <E> The type of edges in the graph that will be used.
 */
public class DijkstrasAlgorithm<V, E> {

	Logger logger = Logger.getLogger(DijkstrasAlgorithm.class);
	
	/**
	 * The map of nodes to the distance between them and the starting node. Infinity is 
	 * represented by the maximum size of an int.
	 */
	private Map<V, Integer> d = new HashMap<V, Integer>();
	
	/**
	 * The map that contains <node, parent node> pairs when Dijkstra's algorithm is run.
	 */
	private Map<V, V> pi = new HashMap<V, V>();
	
	/**
	 * Performs Dijkstra's algorithm on the given graph, starting with the given
	 * node. This algorithm is described in "Introduction to Algorithms" by
	 * Cormen et al, Chapter 25.
	 * 
	 * @param graph
	 *            The graph to run Dijkstra's algorithm on.
	 * @param startingNode
	 *            The node to start running Dijkstra's algorithm from.
	 * @return The pi map. The map contains <node, parent node> pairs given from
	 *         Dijkstra's algorithm. The map will not contain the node if it was
	 *         not reached.
	 */
	public Map<V, V> calculateShortestPaths(GraphModel<V, E> graph, V start) {
		initializeSingleSource(graph, start);
		List<V> S = new ArrayList<V>();
		List<V> Q = new ArrayList<V>(graph.getNodes());
		while (!Q.isEmpty()) {
			V u = extractMin(Q);
			S.add(u);
			for (V v: graph.getAdjacentNodes(u)) {
				relax(u, v);
			}
		}
		return pi;
	}

	/**
	 * This method relaxes the edge between the nodes by reducing the weight of
	 * the edges based on the shortest path from the starting node to v.
	 * 
	 * @param u
	 *            A node that has a path to the starting node and is connected
	 *            to v.
	 * @param v
	 *            The node that has an edge with u that we wish to relax.
	 */
	private void relax(V u, V v) {
		if (d.get(v) > d.get(u) + 1) {
			d.put(v, d.get(u) + 1);
			pi.put(v, u);
		}
		
	}

	/**
	 * Extracts the node that has the shortest path to the starting node. This
	 * node will be removed from the given list.
	 * 
	 * @param q
	 *            The list that contains the nodes we have not reached yet in
	 *            Dijkstra's algorithm.
	 * @return The node that has the shortest path to the starting node.
	 */
	private V extractMin(List<V> q) {
		V u = null;
		for (V v: q) {
			if (u == null || d.get(u) > d.get(v)) {
				u = v;
			}
		}
		q.remove(u);
		return u;
	}

	/**
	 * Initializes the d and pi maps for finding the shortest paths in a graph
	 * by using Dijkstra's algorithm.
	 * 
	 * @param graph
	 *            The list of nodes in the graph we will be running Dijkstra's
	 *            algorithm on.
	 * @param start
	 *            The starting node for finding the shortest path to.
	 */
	private void initializeSingleSource(GraphModel<V, E> graph, V start) {
		for(V v: graph.getNodes()) {
			d.put(v, new Integer(Integer.MAX_VALUE - 1));
		}
		d.put(start, new Integer(0));
	}
}
