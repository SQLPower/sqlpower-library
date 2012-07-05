package ca.sqlpower.object;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import ca.sqlpower.graph.BreadthFirstSearch;
import ca.sqlpower.graph.GraphModel;
import ca.sqlpower.object.SPObject;

/**
 * This graph takes a {@link WabitObject} for its root and makes a graph
 * model that represents all of the root's dependencies. The root is
 * included in the dependencies. The children can be included or the graph
 * can just be of the dependencies. If the children are not included in the
 * graph then the parents will replace their children when adding edges.
 * <p>
 * Example:<br>
 * If we have a workspace with a report, chart, query, and 2 data sources 
 * the parent tree would look like the following
 * <pre>
 *                       Workspace
 *                           |
 *    ---------------------------------------------
 *   /     |       |             |                 \
 * DS1    DS2    Query         Chart             Report
 *                               |                 |
 *                           ---------             |
 *                          /    |    \            |
 *                       Col1  Col2  Col3         Page
 *                                                 |
 *                                            -----------
 *                                           /     |     \
 *                                          CB1   CB2   CB3
 *                                           |     |
 *                                          CR1   CR2
 * </pre>
 * Where a DS is a data source, a CB is a content box and a CR is a content renderer.<br>
 * If we said the Query was based on DS1, the Chart was based on the Query, and the Report
 * was displaying the Chart in CB1 and the Query in CB2 we would have dependencies between
 * the WabitObjects. With dependencies the tree would look like the following<br>
 * <img src="doc-files/ProjectGraphModel-1.JPG"><br>
 * If a graph is made with the report node as the root you will have a graph of the following
 * <pre>
 *             Report
 *               |
 *               |
 *               |
 *              Page
 *               |
 *          ----------------------------
 *         /                      |     \
 *        CB1                    CB2   CB3
 *         |                      |
 *        CR1                    CR2
 *         |                      |
 *       Chart----------------->Query
 *         |                      |
 *      ---------                DS1
 *     /    |    \
 *   Col1  Col2  Col3
 * </pre>
 * If a graph is made without the children of objects being included in the graph
 * the result will look like the following
 * <pre>
 *            Report
 *              |
 *         -----------
 *        /           \
 *     Chart-------->Query
 *                     |
 *                    DS1
 * </pre>
 * If we look at a graph without the objects children, just the dependencies, with
 * the query as the root we will get the graph
 * <pre>
 *    Query
 *      |
 *     DS1
 * </pre>
 * If we then reverse the polarity on the graph above we get
 * <pre>
 *              Query
 *                |
 *           -----------
 *          /           \
 *        Chart         CR2
 *          |
 *         CR1
 * </pre>
 */
public class WorkspaceGraphModel implements GraphModel<SPObject, WorkspaceGraphModelEdge> {
    
    /**
     * This list will contain all of the nodes in the entire graph.
     */
    private final Set<SPObject> nodes;
    
    /**
     * This list will map each node to a list of inbound edges on the node.
     */
    private final Map<SPObject, Set<WorkspaceGraphModelEdge>> inboundEdges;
    
    /**
     * This list will map each node to a list of outbound edges on the node.
     */
    private final Map<SPObject, Set<WorkspaceGraphModelEdge>> outboundEdges;

    /**
     * This node is the starting point when creating the graph.
     * @see #WorkspaceGraphModel(WabitObject, WabitObject, boolean, boolean)
     */
    private final SPObject graphStartNode;

    private WorkspaceGraphModel(Set<SPObject> nodes, Map<SPObject, Set<WorkspaceGraphModelEdge>> inboundEdges, Map<SPObject, Set<WorkspaceGraphModelEdge>> outboundEdges, SPObject graphStartNode) {
		this.nodes = nodes;
		this.inboundEdges = inboundEdges;
		this.outboundEdges = outboundEdges;
		this.graphStartNode = graphStartNode;
    }
    
    /**
     * @param root
     *            The Wabit object that is the highest ancestor of all of the
     *            objects in the graph. This node is used to create an initial
     *            graph ensuring no nodes are missed when reversing polarity.
     *            This will normally be the WabitWorkspace.
     * @param graphStartNode
     *            The Wabit object to start creating the graph model from. This
     *            will be the starting point for finding edges and nodes. Only
     *            objects that are dependencies or children, if the child flag
     *            is set, recursively will be included in the graph. If the
     *            entire workspace is desired to be made into a graph this
     *            should be the workspace object. This must be a
     *            child/grandchild/etc of the root.
     * @param showOnlyDependencies
     *            If false the children of objects will be included in the graph
     *            and their parent/child relationship will be included as an
     *            edge. If true only the dependency lines will be stored when
     *            creating the graph not the parent/child edges. The children
     *            will still be traversed if this is true but the edge created
     *            for the dependency will link the child traversed from's
     *            ancestor to the object the child depends on directly not its
     *            ancestor. The ancestor to link to will always be a child of
     *            the root object given.
     * @param reversePolarity
     *            If false the edges in the graph will go from the parent object
     *            to the child object for parent/child edges, and dependencies
     *            edges will go from the object that is dependent on an object
     *            to the object being depended on. If this is true then the
     *            edges in the graph will be reversed so the children of objects
     *            will have an edge pointing to their parent and objects being
     *            depended on will point to the objects that depend on them.
     */
    public WorkspaceGraphModel(SPObject root, SPObject graphStartNode, 
            boolean showOnlyDependencies, boolean reversePolarity) {
    	this(root, graphStartNode, showOnlyDependencies, reversePolarity, false);
    }

	/**
	 * @param includeChildren
	 *            If true the children of the {@link #graphStartNode} will be
	 *            included in the graph, even if the relationships are inverted.
	 */
    public WorkspaceGraphModel(SPObject root, SPObject graphStartNode, 
            boolean showOnlyDependencies, boolean reversePolarity, boolean includeChildren) {
        
        //The graph made by this constructor is done in two steps:
        //1) create an initial graph of the whole workspace and if necessary reverse 
        //   polarity. Child nodes will be hidden if we are showing only dependencies.
        //2) do a BFS on the graph starting at the start node to find what nodes should 
        //   stay in the graph, ie connected, and remove the rest.
        
    	nodes = new HashSet<SPObject>();
    	outboundEdges = new HashMap<SPObject, Set<WorkspaceGraphModelEdge>>();
    	inboundEdges = new HashMap<SPObject, Set<WorkspaceGraphModelEdge>>();
    	
        this.graphStartNode = graphStartNode;
        createGraph(root, showOnlyDependencies, reversePolarity);

        BreadthFirstSearch<SPObject, WorkspaceGraphModelEdge> bfs = 
            new BreadthFirstSearch<SPObject, WorkspaceGraphModelEdge>();
        Set<SPObject> connectedObjects = new HashSet<SPObject>(bfs.performSearch(this, graphStartNode));
        
        if (includeChildren) {
        	connectedObjects.addAll(performChildSearch(graphStartNode, connectedObjects));
        }

        restrictGraph(connectedObjects);
        
    }

	/**
	 * @param parent
	 *            The parent that we want to ensure its children are included in
	 *            the graph.
	 * @param existingChildren
	 *            The set of objects already to be included in the graph. This
	 *            set exists for performance reasons in case the child is
	 *            already included.
	 */
    private Set<SPObject> performChildSearch(SPObject parent, Set<SPObject> existingObjects) {
    	Set<SPObject> connectedObjects = new HashSet<SPObject>();
    	BreadthFirstSearch<SPObject, WorkspaceGraphModelEdge> bfs = 
            new BreadthFirstSearch<SPObject, WorkspaceGraphModelEdge>();
    	//Merges the existing objects with the recently found objects to pass the correct found
    	//collection to the recursive call.
    	Set<SPObject> foundObjects = new HashSet<SPObject>(existingObjects);
    	for (SPObject child : parent.getChildren()) {
    		if (!foundObjects.contains(child)) {
    			List<SPObject> newNodes = bfs.performSearch(this, child);
    			connectedObjects.addAll(newNodes);
    			foundObjects.addAll(newNodes);
    		}
    		Set<SPObject> newNodes = performChildSearch(child, foundObjects);
    		connectedObjects.addAll(newNodes);
    		foundObjects.addAll(newNodes);
    	}
    	return connectedObjects;
    }

	void restrictGraph(Set<SPObject> connectedObjects) {
		List<SPObject> removedObjects = new ArrayList<SPObject>();
        for (SPObject node : nodes) {
            if (!connectedObjects.contains(node)) {
                removedObjects.add(node);
            }
        }
        nodes.removeAll(removedObjects);
        for (SPObject removedNode : removedObjects) {
            inboundEdges.remove(removedNode);
            if (outboundEdges.get(removedNode) != null) {
                for (WorkspaceGraphModelEdge edge : outboundEdges.get(removedNode)) {
                    if (inboundEdges.get(edge.getChild()) != null) {
                        inboundEdges.get(edge.getChild()).remove(edge);
                    }
                }
            }
            outboundEdges.remove(removedNode);
        }
	}

	void createGraph(SPObject root, boolean showOnlyDependencies,
			boolean reversePolarity) {
		Queue<SPObject> adjacentNodes = new LinkedList<SPObject>();
        Map<SPObject, SPObject> childParentMap = new HashMap<SPObject, SPObject>();
        adjacentNodes.add(root);
        
        while (!adjacentNodes.isEmpty()) {
            SPObject node = adjacentNodes.remove();
            if (nodes.contains(node)) continue;
            
            if (node != root) {
                for (SPObject child : node.getChildren()) {
                    childParentMap.put(child, node);
                }
            }
            
            List<SPObject> edgeChildren = new ArrayList<SPObject>();
            edgeChildren.addAll(node.getDependencies());
            edgeChildren.addAll(node.getChildren());
            for (SPObject child : edgeChildren) {
                adjacentNodes.add(child);

                if (showOnlyDependencies && child.getParent() != null && 
                        child.getParent().equals(node)) continue;
                
                SPObject childNode = child;
                SPObject parentNode = node;
                if (reversePolarity) {
                    SPObject temp = parentNode;
                    parentNode = childNode;
                    childNode = temp;
                }
                
                if (showOnlyDependencies) {
                    while (childParentMap.get(parentNode) != null) {
                        parentNode = childParentMap.get(parentNode);
                    }
                }
                
                WorkspaceGraphModelEdge edge = new WorkspaceGraphModelEdge(parentNode, childNode);
                Set<WorkspaceGraphModelEdge> childInboundList = inboundEdges.get(childNode);
                if (childInboundList == null) {
                    childInboundList = new HashSet<WorkspaceGraphModelEdge>();
                    inboundEdges.put(childNode, childInboundList);
                }
                childInboundList.add(edge);
                Set<WorkspaceGraphModelEdge> parentOutboundList = outboundEdges.get(parentNode);
                if (parentOutboundList == null) {
                    parentOutboundList = new HashSet<WorkspaceGraphModelEdge>();
                    outboundEdges.put(parentNode, parentOutboundList);
                }
                parentOutboundList.add(edge);
            }
            
            if (!showOnlyDependencies) {
                nodes.add(node);
            } else {
                if (!childParentMap.containsKey(node) 
                        || inboundEdges.get(node) != null 
                        || outboundEdges.get(node) != null) {
                    nodes.add(node);
                }
            }
        }
	}

    public Collection<SPObject> getAdjacentNodes(SPObject node) {
        Collection<SPObject> adjacentNodes = new HashSet<SPObject>();
        Set<WorkspaceGraphModelEdge> edges = outboundEdges.get(node);
        if (edges == null) return Collections.emptySet();
        for (WorkspaceGraphModelEdge edge : edges) {
            adjacentNodes.add(edge.getChild());
        }
        return adjacentNodes;
    }

    public Collection<WorkspaceGraphModelEdge> getEdges() {
        Set<WorkspaceGraphModelEdge> allEdges = new HashSet<WorkspaceGraphModelEdge>();
        
        //All of the edged in the inbound map should also be in the outbound map 
        //so only add one of the two
        for (Set<WorkspaceGraphModelEdge> edges : inboundEdges.values()) {
            if (edges == null) continue;
            allEdges.addAll(edges);
        }
        return allEdges;
    }

    public Collection<WorkspaceGraphModelEdge> getInboundEdges(
            SPObject node) {
        Set<WorkspaceGraphModelEdge> edges = inboundEdges.get(node);
        if (edges == null) return Collections.emptyList();
        return edges;
    }

    public Collection<SPObject> getNodes() {
        return nodes;
    }

    public Collection<WorkspaceGraphModelEdge> getOutboundEdges(
            SPObject node) {
        Set<WorkspaceGraphModelEdge> edges = outboundEdges.get(node);
        if (edges == null) return Collections.emptyList();
        return edges;
    }

    public SPObject getGraphStartNode() {
        return graphStartNode;
    }

	/**
	 * Returns this graph model, with all edges reversed. This has a similar
	 * effect to the reverse polarity flag in the constructor, but the graph may
	 * contain a different set of nodes.
	 */
    public WorkspaceGraphModel invert() {
    	Map<SPObject, Set<WorkspaceGraphModelEdge>> invertedInboundEdges = new HashMap<SPObject, Set<WorkspaceGraphModelEdge>>();
    	for (Map.Entry<SPObject, Set<WorkspaceGraphModelEdge>> entry : inboundEdges.entrySet()) {
    		Set<WorkspaceGraphModelEdge> edges = new HashSet<WorkspaceGraphModelEdge>();
    		for (WorkspaceGraphModelEdge edge : entry.getValue()) {
    			edges.add(new WorkspaceGraphModelEdge(edge.getChild(), edge.getParent()));
    		}
    		invertedInboundEdges.put(entry.getKey(), edges);
    	}
    	
    	Map<SPObject, Set<WorkspaceGraphModelEdge>> invertedOutboundEdges = new HashMap<SPObject, Set<WorkspaceGraphModelEdge>>();
    	for (Map.Entry<SPObject, Set<WorkspaceGraphModelEdge>> entry : outboundEdges.entrySet()) {
    		Set<WorkspaceGraphModelEdge> edges = new HashSet<WorkspaceGraphModelEdge>();
    		for (WorkspaceGraphModelEdge edge : entry.getValue()) {
    			edges.add(new WorkspaceGraphModelEdge(edge.getChild(), edge.getParent()));
    		}
    		invertedOutboundEdges.put(entry.getKey(), edges);
    	}
    	
    	return new WorkspaceGraphModel(nodes, invertedOutboundEdges, invertedInboundEdges, graphStartNode);
    }
    
}