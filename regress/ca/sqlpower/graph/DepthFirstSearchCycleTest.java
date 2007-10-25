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