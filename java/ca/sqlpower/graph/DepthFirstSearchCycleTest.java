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

import java.util.Arrays;
import java.util.Collection;

import junit.framework.TestCase;

/**
 * Tests the depth first search algorithm for its cycle detection.
 */
public class DepthFirstSearchCycleTest extends TestCase{
	
	/**
	 *           (1)
	 *       -1 /  \ -2
	 *         /    \
	 *       (2)<_   (3)
	 *   -3 /     \___/
	 *     /       -4     
	 *   (4) 
	 */
	public void testSimpleTree() {
		GraphModel<Integer, Integer> gm = new GraphModel<Integer, Integer>(){

			public Collection<Integer> getAdjacentNodes(Integer node) {
				int num = node.intValue();
				switch (num) {
				case 1:
					return Arrays.asList(new Integer[]{2,3});
				case 2:
					return Arrays.asList(new Integer[]{4});
				case 3:
					return Arrays.asList(new Integer[]{2});
				case 4: 
					return Arrays.asList(new Integer[]{});
				default:
					return Arrays.asList(new Integer[]{});
				}
			}

			public Collection<Integer> getEdges() {
				return Arrays.asList(new Integer[]{-1,-2,-3,-4});
			}

			public Collection<Integer> getInboundEdges(Integer node) {
				int num = node.intValue();
				switch (num) {
				case 1:
					return Arrays.asList(new Integer[]{});
				case 2:
					return Arrays.asList(new Integer[]{-1,-4});
				case 3:
					return Arrays.asList(new Integer[]{-2});
				case 4: 
					return Arrays.asList(new Integer[]{-3});
				default:
					return Arrays.asList(new Integer[]{});
				}
			}

			public Collection<Integer> getNodes() {
				return Arrays.asList(new Integer[]{1,2,3,4});
			}

			public Collection<Integer> getOutboundEdges(Integer node) {
				int num = node.intValue();
				switch (num) {
				case 1:
					return Arrays.asList(new Integer[]{-1,-2});
				case 2:
					return Arrays.asList(new Integer[]{-3});
				case 3:
					return Arrays.asList(new Integer[]{-4});
				case 4: 
					return Arrays.asList(new Integer[]{});
				default:
					return Arrays.asList(new Integer[]{});
				}
			}
			
		};
		
		DepthFirstSearch<Integer, Integer> dfs = new DepthFirstSearch<Integer, Integer>();
		dfs.performSearch(gm);
		assertEquals(false, dfs.isCyclic());
	}
	
	
	
	/**
	 *           (1)
	 *       -1 /  \ -2
	 *         /    \
	 *    _->(2)<-_  (3)
	 * -3 \__/     \__/
	 *              -4     
	 *   (4) 
	 */
	public void testGraphCycle() {
		GraphModel<Integer, Integer> gm = new GraphModel<Integer, Integer>(){

			public Collection<Integer> getAdjacentNodes(Integer node) {
				int num = node.intValue();
				switch (num) {
				case 1:
					return Arrays.asList(new Integer[]{2,3});
				case 2:
					return Arrays.asList(new Integer[]{2});
				case 3:
					return Arrays.asList(new Integer[]{2});
				case 4: 
					return Arrays.asList(new Integer[]{});
				default:
					return Arrays.asList(new Integer[]{});
				}
			}

			public Collection<Integer> getEdges() {
				return Arrays.asList(new Integer[]{-1,-2,-3,-4});
			}

			public Collection<Integer> getInboundEdges(Integer node) {
				int num = node.intValue();
				switch (num) {
				case 1:
					return Arrays.asList(new Integer[]{});
				case 2:
					return Arrays.asList(new Integer[]{-1,-4, -3});
				case 3:
					return Arrays.asList(new Integer[]{-2});
				case 4: 
					return Arrays.asList(new Integer[]{});
				default:
					return Arrays.asList(new Integer[]{});
				}
			}

			public Collection<Integer> getNodes() {
				return Arrays.asList(new Integer[]{1,2,3,4});
			}

			public Collection<Integer> getOutboundEdges(Integer node) {
				int num = node.intValue();
				switch (num) {
				case 1:
					return Arrays.asList(new Integer[]{-1,-2});
				case 2:
					return Arrays.asList(new Integer[]{-3});
				case 3:
					return Arrays.asList(new Integer[]{-4});
				case 4: 
					return Arrays.asList(new Integer[]{});
				default:
					return Arrays.asList(new Integer[]{});
				}
			}
			
		};
		
		DepthFirstSearch<Integer, Integer> dfs = new DepthFirstSearch<Integer, Integer>();
		dfs.performSearch(gm);
		assertEquals(true, dfs.isCyclic());
	}
	
	/**
	 *       _____
	 *       |   |
	 *       V   |
	 *      (1)  | -1
	 *       |___|
	 *        
	 */
	
	public void testSimpleCycle() {
		GraphModel<Integer, Integer> gm = new GraphModel<Integer, Integer>() {

			public Collection<Integer> getAdjacentNodes(Integer node) {
				return Arrays.asList(new Integer[]{1});
			}

			public Collection<Integer> getEdges() {
				return Arrays.asList(new Integer[]{-1});
			}

			public Collection<Integer> getInboundEdges(Integer node) {
				return Arrays.asList(new Integer[]{-1});
			}

			public Collection<Integer> getNodes() {
				return Arrays.asList(new Integer[]{1});
			}

			public Collection<Integer> getOutboundEdges(Integer node) {
				return Arrays.asList(new Integer[]{-1});
			}			
		};
		
		DepthFirstSearch< Integer, Integer> dfs = new DepthFirstSearch<Integer, Integer>();
		dfs.performSearch(gm);
		assertEquals(true, dfs.isCyclic());
	}

}