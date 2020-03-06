/**
 * Graphium Neo4j - Module of Graphium for routing services via Neo4j
 * Copyright © 2020 Salzburg Research Forschungsgesellschaft (graphium@salzburgresearch.at)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package at.srfg.graphium.routing.neo4j.algos.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.neo4j.graphalgo.CostEvaluator;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PathExpander;
import org.neo4j.graphdb.Relationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.srfg.graphium.model.IBaseWaySegment;
import at.srfg.graphium.neo4j.model.WayGraphConstants;
import at.srfg.graphium.neo4j.model.WaySegmentRelationshipType;
import at.srfg.graphium.routing.algo.IRoutedPath;
import at.srfg.graphium.routing.algo.IRoutingAlgo;
import at.srfg.graphium.routing.algo.impl.DefaultRoutingAlgoResultImpl;
import at.srfg.graphium.routing.model.IDirectedSegment;
import at.srfg.graphium.routing.model.IDirectedSegmentSet;
import at.srfg.graphium.routing.model.IRoutingOptions;
import at.srfg.graphium.routing.model.impl.DirectedSegmentImpl;
import at.srfg.graphium.routing.model.impl.DirectedSegmentSetImpl;
import at.srfg.graphium.routing.neo4j.model.impl.SimplePathImpl;
import it.unimi.dsi.fastutil.PriorityQueue;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectHeapPriorityQueue;
import scala.NotImplementedError;

/**
 * 
 * @author mwimmer
 */
public class BidirectionalDijkstra<W extends IBaseWaySegment> implements IRoutingAlgo<IRoutingOptions, Node, Double> {

	private static Logger log = LoggerFactory.getLogger(BidirectionalDijkstra.class);
	
	protected PathExpander<Object> expanderOutgoing;
	protected PathExpander<Object> expanderIncoming;
	protected CostEvaluator<Double> costEvaluator;
	protected IRoutingOptions options;
	
	public BidirectionalDijkstra(PathExpander<Object> expanderOutgoing, PathExpander<Object> expanderIncoming, CostEvaluator<Double> costEvaluator, IRoutingOptions options) {
		super();
		this.expanderOutgoing = expanderOutgoing;
		this.expanderIncoming = expanderIncoming;
		this.costEvaluator = costEvaluator;
		this.options = options;
	}

	@Override
	public IRoutedPath<Double> bestRoute(IRoutingOptions routeOptions, Node sourceNode, float precentageStartWeight,
			Node targetNode, float percentageEndWeight) {
		Path path = calculateShortestPath(sourceNode, targetNode);
		return convertPath2RoutedPath(path);
	}

	protected IRoutedPath<Double> convertPath2RoutedPath(Path path) {
		if (path == null || path.getNodes().isEmpty()) {
			return null;
		} else {
			List<IDirectedSegment> segments = new ArrayList<>();

			path.getNodes().forEach(node -> segments.add(new DirectedSegmentImpl((long) node.getNeo4jNode().getProperty(WayGraphConstants.SEGMENT_ID), node.isStartToEnd())));
			
			IDirectedSegmentSet segmentSet = new DirectedSegmentSetImpl(1L, segments);
			return new DefaultRoutingAlgoResultImpl(segmentSet, path.getTotalCost());
		}
	}

	@Override
	public List<IRoutedPath<Double>> bestRoutes(IRoutingOptions routeOptions, Node sourceNode, float precentageStartWeight,
			Node targetNode, float percentageEndWeight, short amount) {
		throw new NotImplementedError();
	}

	public Path calculateShortestPath(Node sourceNode, Node targetNode) {
		if (sourceNode.getId() == targetNode.getId()) {
			return null;
		}

		double bestPathLength = Double.POSITIVE_INFINITY;
		double mtmp = 0;
		
		Map<Long, PathNode> visitedF = new Long2ObjectOpenHashMap<>();
		Map<Long, PathNode> visitedB = new Long2ObjectOpenHashMap<>();
		
		PriorityQueue<PathNode> prioQueueF = new ObjectHeapPriorityQueue<PathNode>(); // front
		PriorityQueue<PathNode> prioQueueB = new ObjectHeapPriorityQueue<PathNode>(); // back
		
	    PathNode start = getStartNode(sourceNode);
	    PathNode end = getStartNode(targetNode);
	    prioQueueF.enqueue(start);
	    prioQueueB.enqueue(end);
	    
	    PathNode[] intersectingNodes = null;
	    PathNode[] tmpIntersectingNodes = null;
	    
		while (!options.isCancelled() &&
			   !prioQueueF.isEmpty() && !prioQueueB.isEmpty()) {
			
			List<PathNode> neighboursF = BFS(prioQueueF, visitedF, true);
			List<PathNode> neighboursB = BFS(prioQueueB, visitedB, false);
			
			Map<Long, PathNode> allFoundNodesF = createPathNodeMap(visitedF, neighboursF);
			Map<Long, PathNode> allFoundNodesB = createPathNodeMap(visitedB, neighboursB);
			
			tmpIntersectingNodes = getIntersectingNode(neighboursF, neighboursB, allFoundNodesF, allFoundNodesB);
			
			if (tmpIntersectingNodes != null) {
				// We found an intersecting node v.
				// Calculate length of paths from start to v and end to v.
				double tmpBestPathLength = tmpIntersectingNodes[0].getCost() + tmpIntersectingNodes[1].getCost();
				if (tmpBestPathLength < bestPathLength) {
					bestPathLength = tmpBestPathLength;
					intersectingNodes = tmpIntersectingNodes;
				}
				// Calculate the lengths of the best paths seen so far
				if (!prioQueueF.isEmpty() && !prioQueueB.isEmpty()) {
					mtmp = prioQueueF.first().getCost() + prioQueueB.first().getCost();
				} else {
					// one queue is empty => break processing, target found
					mtmp = bestPathLength;
				}
			}
			
			if (mtmp >= bestPathLength) {
				// Stop if lengths of best paths seen so far are equal or greater than the length of paths from start to v and end to v.
				// The intersecting node at this point does not have to be the same as the found intersecting node at first!
				if (log.isDebugEnabled()) {
					log.debug("intersection segment: " + intersectingNodes[0].getNeo4jNode().getProperty(WayGraphConstants.SEGMENT_ID));
					log.debug("visited: " + visitedF.size() + " / " + visitedB.size() + " nodes to reach target");
				}
				return createPath(intersectingNodes);
			}
			
		}

		// no path found
		if (log.isDebugEnabled()) {
			log.debug("visited: " + visitedF.size() + " / " + visitedB.size() + " nodes to reach target");
		}
		return null;
	}

	private Map<Long, PathNode> createPathNodeMap(Map<Long, PathNode> visited, List<PathNode> neighbours) {
		Map<Long, PathNode> allFoundNodes = new Long2ObjectOpenHashMap<>(visited);
		if (neighbours != null) {
			for (PathNode neighbour : neighbours) {
				allFoundNodes.put(neighbour.getId(), neighbour);
			}
		}
		return allFoundNodes;
	}

	private PathNode[] getIntersectingNode(List<PathNode> neighboursF, List<PathNode> neighboursB,
			Map<Long, PathNode> visitedF, Map<Long, PathNode> visitedB) {
		if (neighboursF != null) {
			for (PathNode neighbourF : neighboursF) {
				if (visitedB.containsKey(neighbourF.getId())) {
					PathNode intersectingCandidateB = visitedB.get(neighbourF.getId());
					if (intersectingCandidateB.getLastRelationship() == null ||
						neighbourF.getLastRelationship() == null ||
						!((Long)intersectingCandidateB.getLastRelationship().getProperty(WayGraphConstants.CONNECTION_NODE_ID)).equals(
						  (Long)neighbourF.getLastRelationship().getProperty(WayGraphConstants.CONNECTION_NODE_ID))) {
						// only valid if paths hit segment/node from diffent directions
						PathNode[] intersectingNodes = new PathNode[2];
						intersectingNodes[0] = neighbourF;
						intersectingNodes[1] = intersectingCandidateB;
						return intersectingNodes;
					}
				}
			}
		}
		if (neighboursB != null) {
			for (PathNode neighbourB : neighboursB) {
				if (visitedF.containsKey(neighbourB.getId())) {
					PathNode intersectingCandidateF = visitedF.get(neighbourB.getId());
					if (intersectingCandidateF.getLastRelationship() == null ||
						neighbourB.getLastRelationship() == null ||
						!((Long)intersectingCandidateF.getLastRelationship().getProperty(WayGraphConstants.CONNECTION_NODE_ID)).equals(
						  (Long)neighbourB.getLastRelationship().getProperty(WayGraphConstants.CONNECTION_NODE_ID))) {
						// only valid if paths hit segment/node from diffent directions
						PathNode[] intersectingNodes = new PathNode[2];
						intersectingNodes[0] = intersectingCandidateF;
						intersectingNodes[1] = neighbourB;
						return intersectingNodes;
					}
				}
			}
		}			
		return null;
	}

	private List<PathNode> BFS(PriorityQueue<PathNode> prioQueue, Map<Long, PathNode> visited, boolean front) {
		List<PathNode> neighbours = null;
		PathNode currentNode = prioQueue.dequeue();
		if (!visited.containsKey(currentNode.getId())) {
			visited.put(currentNode.getId(), currentNode);

			// read neighbours
			if (front) {
				neighbours = getNeighbours(currentNode, true);
			} else {
				neighbours = getNeighbours(currentNode, false);
			}
	        for (PathNode neighbour : neighbours) {
	        	// calculate costs of neighbours and add them to the priority queue
	        	calcCosts(neighbour, currentNode);
        		prioQueue.enqueue(neighbour);
	        }
		}
		return neighbours;
	}

	// read connected segments regarding their direction!
	protected List<PathNode> getNeighbours(PathNode currentNode, boolean front) {
		Relationship lastRelationship = null;
		Node startNode = null;
		Node endNode = null;
		
		if (log.isDebugEnabled()) {
			log.debug("getting neighbours from Neo4j node with ID " + currentNode.getNeo4jNode().getId()); // + " and segment ID " + currentNode.getId());
			if (currentNode.getLastRelationship() != null) {
				log.debug("last relationship's connection ID = " + currentNode.getLastRelationship().getProperty(WayGraphConstants.CONNECTION_NODE_ID));
				log.debug("current segment's startNodeId = " + currentNode.getNeo4jNode().getProperty(WayGraphConstants.SEGMENT_STARTNODE_ID) + 
						", endNodeId = " + currentNode.getNeo4jNode().getProperty(WayGraphConstants.SEGMENT_ENDNODE_ID));
			}
		}
		
		org.neo4j.graphdb.Path neoPath;
		if (front) {
			endNode = currentNode.getNeo4jNode();
			if (currentNode.getLastRelationship() != null) {
				lastRelationship = currentNode.getLastRelationship();
				startNode = lastRelationship.getStartNode();
			}
			neoPath = new SimplePathImpl(startNode, endNode, lastRelationship);
			return expandFront(neoPath, currentNode);
		
		} else {
			startNode = currentNode.getNeo4jNode();
			if (currentNode.getLastRelationship() != null) {
				lastRelationship = currentNode.getLastRelationship();
				endNode = currentNode.getParentNode().getNeo4jNode();
			}
			neoPath = new SimplePathImpl(startNode, endNode, lastRelationship);
			return expandBack(neoPath, currentNode);
		}
	}

	private List<PathNode> expandFront(org.neo4j.graphdb.Path neoPath, PathNode currentNode) {
		Iterable<Relationship> relationships = expanderOutgoing.expand(neoPath, null);
		
		List<PathNode> nodes = new ArrayList<>();
		Node endNode;
		for (Relationship rel : relationships) {
			endNode = rel.getEndNode();
			PathNode node = new PathNode(getId(endNode), null, endNode, rel, 
					costEvaluator.getCost(rel, Direction.OUTGOING), currentNode);
			
			if (log.isDebugEnabled()) {
				node.setSegmentId((long) endNode.getProperty(WayGraphConstants.SEGMENT_ID));
			}

			nodes.add(node);
		}
		
		return nodes;
	}

	private List<PathNode> expandBack(org.neo4j.graphdb.Path neoPath, PathNode currentNode) {
		Iterable<Relationship> relationships = expanderIncoming.expand(neoPath, null);
		
		List<PathNode> nodes = new ArrayList<>();
		Node startNode;
		for (Relationship rel : relationships) {
			startNode = rel.getStartNode();
			PathNode node = new PathNode(getId(startNode), null, startNode, rel, 
					costEvaluator.getCost(rel, Direction.OUTGOING), currentNode);
			
			if (log.isDebugEnabled()) {
				node.setSegmentId((long) startNode.getProperty(WayGraphConstants.SEGMENT_ID));
			}

			nodes.add(node);
		}
		
		return nodes;
	}

	// TODO: in case of HD waysegments we need a better logic for extracting the direction (=> parallel lanes...)
	protected Boolean determineDirectionFront(Relationship rel, Node endNode) {
		long connectionNodeId = (long)rel.getProperty(WayGraphConstants.CONNECTION_NODE_ID);
		long endSegmentStartNodeId = (long)endNode.getProperty(WayGraphConstants.SEGMENT_STARTNODE_ID);
		
		if (endSegmentStartNodeId == connectionNodeId) {
			return true;
		} else {
			return false;
		}
	}

	protected Boolean determineDirectionBack(Relationship rel) {
		if (rel.isType(WaySegmentRelationshipType.SEGMENT_CONNECTION_ON_ENDNODE)) {
			return true;
		} else {
			return false;
		}
	}

	protected void determineNodeDirections(PathNode node1, PathNode node2) {
		if (node1 != null && node2 != null) {
			long node1StartNodeId = (long) node1.getNeo4jNode().getProperty(WayGraphConstants.SEGMENT_STARTNODE_ID);
			long node1EndNodeId = (long) node1.getNeo4jNode().getProperty(WayGraphConstants.SEGMENT_ENDNODE_ID);
			long node2StartNodeId = (long) node2.getNeo4jNode().getProperty(WayGraphConstants.SEGMENT_STARTNODE_ID);
			long node2EndNodeId = (long) node2.getNeo4jNode().getProperty(WayGraphConstants.SEGMENT_ENDNODE_ID);

			if (node1StartNodeId == node2StartNodeId) {
				node1.setStartToEnd(false);
				node2.setStartToEnd(true);
			} else if (node1StartNodeId == node2EndNodeId) {
				node1.setStartToEnd(false);
				node2.setStartToEnd(false);
			} else if (node1EndNodeId == node2StartNodeId) {
				node1.setStartToEnd(true);
				node2.setStartToEnd(true);
			} else {
				node1.setStartToEnd(true);
				node2.setStartToEnd(false);
			}
		}
	}

	private Path createPath(PathNode[] intersectingNodes) {
		Path path = new Path();
		
		// intersectingNodes: [0] .. node coming from front search
		//					  [1] .. node coming from back search
		PathNode currentNodeF = intersectingNodes[0];
		PathNode currentNodeB = intersectingNodes[1];
		path.setTotalCost(currentNodeF.getCost() + currentNodeB.getCost()); // cost of last node is total cost of path
		
		while (currentNodeF != null) {
			path.addNodeAtStart(currentNodeF);
			currentNodeF = currentNodeF.getParentNode();
		}
		
		while (currentNodeB != null) {
			if (currentNodeB != intersectingNodes[1]) {
				path.addNode(currentNodeB);
			}
			currentNodeB = currentNodeB.getParentNode();
		}
		
		// determine node directions
		PathNode pred = null;
		for (PathNode succ : path.getNodes()) {
			if (pred != null) {
				determineNodeDirections(pred, succ);
			}
			pred = succ;
		}
		
		return path;
	}

	protected Long getId(Node node) {
		return node.getId();
	}

	protected void calcCosts(PathNode neighbour, PathNode currentNode) {
    	neighbour.setCost(neighbour.getCost() + currentNode.getCost());
	}

	protected PathNode getStartNode(Node startNode) {
		if (startNode == null) {
			return null;
		} else {
			PathNode start = new PathNode(startNode.getId(),
							null,
							startNode, 
							null, 
							0, 
							null);

			if (log.isDebugEnabled()) {
				start.setSegmentId((long) startNode.getProperty(WayGraphConstants.SEGMENT_ID));
			}
			
			return start;
		}
	}
	
}