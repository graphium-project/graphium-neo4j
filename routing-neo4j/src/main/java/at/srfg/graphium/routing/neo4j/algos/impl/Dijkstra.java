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
import java.util.Set;

import org.neo4j.graphalgo.CostEvaluator;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PathExpander;
import org.neo4j.graphdb.Relationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.srfg.graphium.model.IBaseWaySegment;
import at.srfg.graphium.neo4j.model.WayGraphConstants;
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
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectHeapPriorityQueue;
import scala.NotImplementedError;

/**
 *
 * @author mwimmer
 */
public class Dijkstra<W extends IBaseWaySegment> implements IRoutingAlgo<IRoutingOptions, Node, Double> {

	private static Logger log = LoggerFactory.getLogger(Dijkstra.class);
	
	protected PathExpander<Object> expander;
	protected CostEvaluator<Double> costEvaluator;
	protected IRoutingOptions options;
	
	public Dijkstra(PathExpander<Object> expander, CostEvaluator<Double> costEvaluator, IRoutingOptions options) {
		super();
		this.expander = expander;
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
		if (path == null) {
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
		// LongOpenHashSet is about 10-20% faster than TLongHashSet!!!
		Set<Long> visited = new LongOpenHashSet(60000);
		//TLongHashSet visited = new TLongHashSet();
		// LongOpenHashSet is about 10% faster and needs less memory than Map-trick!!!
//		Map<Long, MinCost> visited = new Long2ObjectOpenHashMap<>();
		
		PriorityQueue<PathNode> prioQueue = new ObjectHeapPriorityQueue<PathNode>();
		long targetId = targetNode.getId();
		 
	    PathNode start = getStartNode(sourceNode);
	    prioQueue.enqueue(start);
	    
		while (!options.isCancelled() &&
			   prioQueue.size() > 0) {
			
			
			//////// MAP TRICK ////////
//			PathNode currentNode = prioQueue.dequeue();
//			
//			if (currentNode.getId() == targetId) {
//				// target found
//				// probably there will be several path to the target, but priority queue ensures that we get the shortest path
//				log.info("visited: " + visited.size() + " nodes to reach target");
//				return createPath(currentNode);
//			}
//			
//			// read neighbours
//			List<PathNode> neighbours = getNeighbours(currentNode);
//	        for (PathNode neighbour : neighbours) {
//
//	        	// calculate costs of neighbours and add them to the priority queue
//	        	calcCosts(neighbour, currentNode);
//
//	        	MinCost minCost = visited.get(neighbour.getId());
//	        	
//	        	if (minCost == null) {
//	        		visited.put(neighbour.getId(), new MinCost(neighbour.getCost()));
//	        		prioQueue.enqueue(neighbour);
//	        	} else if (minCost.cost > neighbour.getCost()) {
//	        		minCost.cost = neighbour.getCost();
//	        		prioQueue.enqueue(neighbour);
//	        	} else {
//	        		// do nothing
//	        	}
//	        	
//	        }
	        ////////////////////////////

			//////// LongOpenHashSet TRICK ////////
			PathNode currentNode = prioQueue.dequeue();
			if (!visited.contains(currentNode.getId())) {
				// to each node there could be several paths
				// by checking <visited> we ensure that we get the shortest path to this node
				visited.add(currentNode.getId());
				
				if (currentNode.getId() == targetId) {
					// target found
					// probably there will be several path to the target, but priority queue ensures that we get the shortest path
					log.debug("visited: " + visited.size() + " nodes to reach target");
					return createPath(currentNode);
				}
				
				// read neighbours
				List<PathNode> neighbours = getNeighbours(currentNode);
		        for (PathNode neighbour : neighbours) {
		        	// calculate costs of neighbours and add them to the priority queue
		        	calcCosts(neighbour, currentNode);
	        		prioQueue.enqueue(neighbour);
		        }
				
			}
			////////////////////////////////////////
		}

		// no path found
		return null;
	}

	private Path createPath(PathNode node) {
		Path path = new Path();
		PathNode currentNode = node;
		path.setTotalCost(node.getCost()); // cost of last node is total cost of path
		
		while (currentNode != null) {
			if (currentNode.getParentNode() != null) {
				if (currentNode.getParentNode().getParentNode() == null) {
					// second node found => determine direction of first node
					determineStartNodeDirection(currentNode);	
				}
				currentNode.setStartToEnd(determineDirection(currentNode.getLastRelationship(), currentNode.getNeo4jNode()));
			}
			
			path.addNodeAtStart(currentNode);
			currentNode = currentNode.getParentNode();
		}
		
		return path;
	}

	// read connected segments regarding their direction!
	protected List<PathNode> getNeighbours(PathNode currentNode) {
		Relationship lastRelationship = null;
		Node startNode = null;
		if (currentNode.getLastRelationship() != null) {
			lastRelationship = currentNode.getLastRelationship();
			startNode = lastRelationship.getStartNode();
		}
		org.neo4j.graphdb.Path neoPath = new SimplePathImpl(startNode, currentNode.getNeo4jNode(), lastRelationship);
		
		
		if (log.isDebugEnabled()) {
			log.debug("getting neighbours from Neo4j node with ID " + currentNode.getNeo4jNode().getId() 
					+ " and segment ID " + currentNode.getNeo4jNode().getProperty(WayGraphConstants.SEGMENT_ID));
			if (currentNode.getLastRelationship() != null) {
				log.debug("last relationship's connection ID = " + currentNode.getLastRelationship().getProperty(WayGraphConstants.CONNECTION_NODE_ID));
				log.debug("current segment's startNodeId = " + currentNode.getNeo4jNode().getProperty(WayGraphConstants.SEGMENT_STARTNODE_ID) + 
						", endNodeId = " + currentNode.getNeo4jNode().getProperty(WayGraphConstants.SEGMENT_ENDNODE_ID));
			}
		}
		
		Iterable<Relationship> relationships = expander.expand(neoPath, null);
	
		List<PathNode> nodes = new ArrayList<>();
		Node endNode;
		for(Relationship rel : relationships) {
			endNode = rel.getEndNode();
			nodes.add(new PathNode(getId(endNode), null, endNode, rel, 
					costEvaluator.getCost(rel, Direction.OUTGOING), 
					currentNode));
		}
		
		return nodes;
	}

	protected void determineStartNodeDirection(PathNode node) {
		if (node.getParentNode() != null) {
			PathNode firstNode = node.getParentNode();
			long firstNodeStartNodeId = (long) firstNode.getNeo4jNode().getProperty(WayGraphConstants.SEGMENT_STARTNODE_ID);
			PathNode secondNode = node;
			long secondNodeStartNodeId = (long) secondNode.getNeo4jNode().getProperty(WayGraphConstants.SEGMENT_STARTNODE_ID);
			long secondNodeEndNodeId = (long) secondNode.getNeo4jNode().getProperty(WayGraphConstants.SEGMENT_ENDNODE_ID);

			if (firstNodeStartNodeId == secondNodeStartNodeId || 
				firstNodeStartNodeId == secondNodeEndNodeId) {
				firstNode.setStartToEnd(false);
			} else {
				firstNode.setStartToEnd(true);
			}
		}
	}

	// TODO: in case of HD waysegments we need a better logic for extracting the direction (=> parallel lanes...)
	protected Boolean determineDirection(Relationship rel, Node endNode) {
		long connectionNodeId = (long)rel.getProperty(WayGraphConstants.CONNECTION_NODE_ID);
		long endSegmentStartNodeId = (long)endNode.getProperty(WayGraphConstants.SEGMENT_STARTNODE_ID);
		
		if (endSegmentStartNodeId == connectionNodeId) {
			return true;
		} else {
			return false;
		}
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
			return new PathNode(startNode.getId(),
							null,
							startNode, 
							null, 
							0, 
							null);
		}
	}
	
}