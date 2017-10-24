/**
 * Graphium Neo4j - Module of Graphium for routing services via Neo4j
 * Copyright © 2017 Salzburg Research Forschungsgesellschaft (graphium@salzburgresearch.at)
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
package at.srfg.graphium.routing.service.neo4j.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.time.StopWatch;
import org.neo4j.graphalgo.CostEvaluator;
import org.neo4j.graphalgo.EstimateEvaluator;
import org.neo4j.graphalgo.GraphAlgoFactory;
import org.neo4j.graphalgo.PathFinder;
import org.neo4j.graphalgo.WeightedPath;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PathExpander;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.impl.StandardExpander;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.linearref.LengthIndexedLine;
import com.vividsolutions.jts.linearref.LinearLocation;
import com.vividsolutions.jts.linearref.LocationIndexedLine;

import at.srfg.graphium.core.persistence.IWayGraphVersionMetadataDao;
import at.srfg.graphium.geomutils.GeometryUtils;
import at.srfg.graphium.model.Access;
import at.srfg.graphium.model.IWaySegment;
import at.srfg.graphium.neo4j.model.WayGraphConstants;
import at.srfg.graphium.neo4j.model.WaySegmentRelationshipType;
import at.srfg.graphium.neo4j.persistence.INeo4jWayGraphReadDao;
import at.srfg.graphium.neo4j.persistence.configuration.IGraphDatabaseProvider;
import at.srfg.graphium.neo4j.persistence.impl.Neo4jWaySegmentHelperImpl;
import at.srfg.graphium.neo4j.persistence.nodemapper.INeo4jNodeMapper;
import at.srfg.graphium.routing.model.IRoute;
import at.srfg.graphium.routing.model.IRouteModelFactory;
import at.srfg.graphium.routing.model.IRoutingOptions;
import at.srfg.graphium.routing.model.impl.RoutingAlgorithms;
import at.srfg.graphium.routing.model.impl.RoutingMode;
import at.srfg.graphium.routing.neo4j.evaluators.INeo4jCostEvaluatorFactory;
import at.srfg.graphium.routing.neo4j.evaluators.impl.NodeBasedCostEstimator;
import at.srfg.graphium.routing.service.IRoutingService;

public class Neo4jRoutingServiceImpl<T extends IWaySegment> 
		implements IRoutingService<T> {
	
	private static Logger log = LoggerFactory.getLogger(Neo4jRoutingServiceImpl.class);
	
	private IGraphDatabaseProvider graphDatabaseProvider;
	
	protected INeo4jWayGraphReadDao graphReadDao;
	protected IWayGraphVersionMetadataDao metadataDao;
	protected INeo4jCostEvaluatorFactory costEvaluatorFactory;
	protected IRouteModelFactory<T> modelFactory;
	protected INeo4jNodeMapper<T> nodeMapper;
	
	// default search distance (15 meter)
	protected double searchDistance   = 0.0000904776810466969;
	
	public Neo4jRoutingServiceImpl() {super();}
	
	@PostConstruct
	public void setup() { }

	@Override
	public IRoute<T> route(IRoutingOptions options,
			T startSegment, T endSegment) {
		try (Transaction tx = graphDatabaseProvider.getGraphDatabase().beginTx()) {
			Node startNode = graphReadDao.getSegmentNodeBySegmentId(options.getGraphName(), options.getGraphVersion(), startSegment.getId());
			Node endNode = graphReadDao.getSegmentNodeBySegmentId(options.getGraphName(), options.getGraphVersion(), endSegment.getId());
			
			IRoute<T> route = null;
			if(startNode != null && endNode != null) {
				List<Node> startNodes = new ArrayList<Node>();
				startNodes.add(startNode);
				List<Node> endNodes = new ArrayList<Node>();
				endNodes.add(endNode);
				
				route = doRoute(options, startNodes, endNodes, null, null);
			}
			
			tx.success();
			return route;
		}
	}

	@Override
	public IRoute<T> route(IRoutingOptions options, double startX, double startY, double endX, double endY) {
		// default behaviour: segment cutting is enabled
		try (Transaction tx = graphDatabaseProvider.getGraphDatabase().beginTx()) {
			IRoute<T> route = route(options, startX, startY, endX, endY, true);

			tx.success();
			return route;
		} 
	}

	@Override
	public IRoute<T> route(IRoutingOptions options, double startX, double startY, double endX, double endY,
			boolean cutStartAndEndSegments) {
		try (Transaction tx = graphDatabaseProvider.getGraphDatabase().beginTx()) {
			Coordinate startCoord =  new Coordinate(startX, startY);
			Coordinate endCoord = new Coordinate(endX, endY);
			
			IRoute<T> route = doRoute(options, startCoord, endCoord);

			// if coordinates avialable cut the route
			if (cutStartAndEndSegments && startCoord != null && endCoord != null && 
				route != null && route.getSegments() != null && !route.getSegments().isEmpty()) {
				route = cutStartAndEndSegment(route, startCoord, endCoord);
			}
			
			tx.success();
			return route;
		}
	}

	@Override
	public IRoute<T> doRoute(IRoutingOptions options, Coordinate startCoord, Coordinate endCoord) {
		StopWatch timer = new StopWatch();
		if (log.isDebugEnabled()) {
			timer.start();
		}
		
		double routingSearchDistance = searchDistance;
		if (options.getSearchDistance() > 0) {
			routingSearchDistance = options.getSearchDistance();
		}
		
		List<Node> startNodes = graphReadDao.findNearestNodes(options.getGraphName(), options.getGraphVersion(),
				GeometryUtils.createPoint2D(startCoord.x, startCoord.y, 4326), routingSearchDistance, 1);
		
		if (log.isDebugEnabled()) {
			timer.stop();
			log.debug("segment lookup on start-coordinate " + startCoord + " took " + timer.getTime() + " ms");
			timer.reset();
		}

		if(startNodes != null && !startNodes.isEmpty()) {			
			timer.start();
			List<Node> endNodes = graphReadDao.findNearestNodes(options.getGraphName(), options.getGraphVersion(), 
					GeometryUtils.createPoint2D(endCoord.x, endCoord.y, 4326), routingSearchDistance, 1);
			
			if (log.isDebugEnabled()) {
				timer.stop();
				log.debug("segment lookup on end-coordinate " + endCoord + " took " + timer.getTime() + " ms");
			}
			
			if(endNodes != null && !endNodes.isEmpty()) {
				return doRoute(options, startNodes.subList(0, 1), endNodes.subList(0, 1), startCoord, endCoord);
			}		
		}
		
		log.info("routing failed...");
		return modelFactory.newRoute();
	}

	@Override
	public IRoute<T> route(IRoutingOptions options, List<Node> startNodes, List<Node> endNodes) {		
		return doRoute(options, startNodes, endNodes, null, null);
	}
	
	protected IRoute<T> doRoute(final IRoutingOptions options,
			List<Node> startNodes, List<Node> endNodes, Coordinate startCoord, Coordinate endCoord) {
		IRoute<T> route;
		long nsTime = System.nanoTime();
		PathExpander<Object> expander = getExpander(options); 
		
		// have to implement our own cost evaluator, because costs are mapped to nodes, not the relations.
		CostEvaluator<Double> costEvaluator;
		costEvaluator = costEvaluatorFactory.createCostEvaluator(options);				
		
    	WeightedPath weightedPath = null;
        try {        	
	        // decide which algorithm to use
        	PathFinder<WeightedPath> weightedFinder = createPathFinder(expander, costEvaluator, options);
        	
			List<WeightedPath> paths = new ArrayList<WeightedPath>();
			WeightedPath currentPath = null;
			for (Node startNode : startNodes) {
				for (Node endNode : endNodes) {
					
					if (startCoord != null && endCoord != null) {		
						costEvaluator = costEvaluatorFactory.createCoordAwareCostEvaluator(
								options, startNode.getId(), endNode.getId(), startCoord, endCoord);
						weightedFinder = createPathFinder(expander, costEvaluator, options);
					} 
					
					currentPath =  weightedFinder.findSinglePath(startNode, endNode);
					
					if (currentPath != null) {
						paths.add(currentPath);
					}
				}
			}
			weightedPath = getBestPath(paths);		
        } catch (NullPointerException e) {
        	log.error("nullpointer in router (astar has this problems on 1 segment routes)", e);
        } catch (Exception e) {
        	log.error("error in routing", e);
        } 
		if (weightedPath != null) {
			float length = 0;
			int time = 0;
			List<Long> path = new ArrayList<Long>();
			List<T> segments = new ArrayList<T>();
			T segment;
			
			T prevSegment = null;
			for (Node node : weightedPath.nodes()) {
				segment = nodeMapper.map(node);
				path.add(segment.getId());
				segments.add(segment);
				length = length + segment.getLength();
				// calculate duration
				if (prevSegment != null) {
					boolean directionTow = prevSegment.getEndNodeId()   == segment.getStartNodeId() || 
										   prevSegment.getStartNodeId() == segment.getStartNodeId();
					time = time + segment.getDuration(directionTow);
				}
				prevSegment = segment;
			}
			// calculate duration for first segment
			if (!segments.isEmpty()) {
				boolean directionTow = true;
				if (segments.size() > 1) {
					directionTow = segments.get(1).getEndNodeId()   == segments.get(0).getEndNodeId() || 
								   segments.get(1).getStartNodeId() == segments.get(0).getEndNodeId();
				}
				time = time + segments.get(0).getDuration(directionTow);
			}
			
			route = modelFactory.newRoute();
			route.setPath(path);
			route.setLength(length);
			route.setDuration(time);
			
			route.setSegments(segments);
			double msTime = ((double)(System.nanoTime()-nsTime) / 1000000);
			route.setRuntimeInMs((int) Math.ceil(msTime));
			log.debug("routing and conversion took " + msTime + " ms for a route with " + path.size() + " segments");
		} else {
			log.info("no route found");
			route = modelFactory.newRoute();
		}		
	
		route.setGraphName(options.getGraphName());
		route.setGraphVersion(options.getGraphVersion());
		
		return route;
	}
	
	protected PathFinder<WeightedPath> createPathFinder(PathExpander<Object> expander, CostEvaluator<Double> costEvaluator,
			IRoutingOptions options) {
		PathFinder<WeightedPath> weightedFinder;
    	if (options.getAlgorithm().equals(RoutingAlgorithms.DIJKSTRA)) {
    		weightedFinder =  GraphAlgoFactory.dijkstra(expander, costEvaluator);
    	}
    	else if (options.getAlgorithm().equals(RoutingAlgorithms.ASTAR)) {
    		EstimateEvaluator<Double> estimateEvaluator = new NodeBasedCostEstimator(options.getCriteria());
    		weightedFinder =  GraphAlgoFactory.aStar(expander, costEvaluator, estimateEvaluator); 
    	}
    	else {
    		throw new RuntimeException("unkown routing algorithm!");
    	}
    	return weightedFinder;
	}

	protected PathExpander<Object> getExpander(IRoutingOptions options) {
		// create default path expander
		StandardExpander expander = StandardExpander.create(WaySegmentRelationshipType.SEGMENT_CONNECTION_ON_STARTNODE,
				Direction.OUTGOING, WaySegmentRelationshipType.SEGMENT_CONNECTION_ON_ENDNODE, Direction.OUTGOING);
		
		// access type restriction on routing mode
		if (options.getMode() != null) {
			Access access = null;
			if (options.getMode().equals(RoutingMode.CAR)) {
				access = Access.PRIVATE_CAR;
			} else if (options.getMode().equals(RoutingMode.BIKE)) {
				access = Access.BIKE;
			} else if (options.getMode().equals(RoutingMode.PEDESTRIAN)) {
				access = Access.PEDESTRIAN;
			}
			if (access != null) {
				final Access accessToFilter = access;
				expander = expander.addNodeFilter(n -> (n.hasProperty(WayGraphConstants.SEGMENT_ACCESS_BKW) &&
												Neo4jWaySegmentHelperImpl.parseAccessTypes(
												(byte[]) n.getProperty(WayGraphConstants.SEGMENT_ACCESS_BKW)
												).contains(accessToFilter)) ||
												(n.hasProperty(WayGraphConstants.SEGMENT_ACCESS_TOW) &&
												Neo4jWaySegmentHelperImpl.parseAccessTypes(
												(byte[]) n.getProperty(WayGraphConstants.SEGMENT_ACCESS_TOW)
												).contains(accessToFilter)));
				expander = expander.addRelationshipFilter(r -> r.hasProperty(WayGraphConstants.CONNECTION_ACCESS) &&
												Neo4jWaySegmentHelperImpl.parseAccessTypes(
												(byte[]) r.getProperty(WayGraphConstants.CONNECTION_ACCESS)
												).contains(accessToFilter));
			}
		}
		
		if (options.getTagValueFilters() != null && !options.getTagValueFilters().isEmpty()) {
			for (String filterKey : options.getTagValueFilters().keySet()) {
				Set<Object> filterValues = options.getTagValueFilters().get(filterKey);
				if (filterValues != null && !filterValues.isEmpty()) {
					expander = expander.addNodeFilter(n -> n.hasProperty(filterKey) && filterValues.contains(n.getProperty(filterKey)));
//					expander = expander.addRelationshipFilter(r -> r.getProperty(filterKey) != null && filterValues.contains(r.getProperty(filterKey)));
				}
			}
			
		} 

		return expander;
	}

	protected WeightedPath getBestPath(List<WeightedPath> paths) {
		// no path return null
		if(paths == null || paths.isEmpty()) {
			return null;
		}
		// only one path so it has to be the best one
		else if(paths.size() == 1) {
			return paths.get(0);			
		}
		// several paths return the one with the best weight
		else {
			double bestWeight = Double.MAX_VALUE;
			WeightedPath bestPath = null;
			for(WeightedPath path : paths) {
				if(bestWeight > path.weight()) {
					bestPath = path;
					bestWeight = path.weight();
				}
			}
			return bestPath;
		}	
	}

	protected IRoute<T> cutStartAndEndSegment(IRoute<T> route, Coordinate startCoord, Coordinate endCoord) {

		T startSeg = route.getSegments().get(0);
		
		// first outgoing from start
		T afterStartSeg;
		// last segment before end
		T beforeEndSeg;
		// get endsegment
		T endSeg;
		if(route.getSegments().size() > 1) {
			endSeg = route.getSegments().get(route.getSegments().size()-1);	
			afterStartSeg = route.getSegments().get(1);
			beforeEndSeg = route.getSegments().get(route.getSegments().size()-2);
		}
		//  only one segment in path --> start is end segment
		else {
			endSeg = startSeg;
			afterStartSeg = startSeg;
			beforeEndSeg = startSeg;
			// special case, on segment check if start and endpoint are exactly start and end locations of segments 
			// there is no need to cut anything
			if(
				(startSeg.getGeometry().getStartPoint().getCoordinate().equals2D(startCoord) && 
					startSeg.getGeometry().getEndPoint().getCoordinate().equals2D(endCoord)) ||
				(startSeg.getGeometry().getStartPoint().getCoordinate().equals2D(endCoord) && 
							startSeg.getGeometry().getEndPoint().getCoordinate().equals2D(startCoord))	) {
					return route;
			}
			else {
				return cutInSingleSegment(route, startSeg, startCoord, endCoord);
			}
		}
		// store original costs
		float uncutStartSegLengthCost = startSeg.getLength();
		float uncutEndSegLengthCost = endSeg.getLength();
		int uncutStartSegDurationTow = startSeg.getDuration(true);
		int uncutStartSegDurationBkw = startSeg.getDuration(false);
		int uncutEndSegDurationTow = endSeg.getDuration(true);
		int uncutEndSegDurationBkw = endSeg.getDuration(false);
		
		// find out direction of path (true from coord 0 to end, false from end to 0)
		boolean direction = getDirection(startSeg, afterStartSeg);
		// refrence startpoint on segment
		LocationIndexedLine indexedStartSeg = new LocationIndexedLine(startSeg.getGeometry());
	
		LengthIndexedLine lengthIndexedStartSeg = new LengthIndexedLine(startSeg.getGeometry());
		double startSegEndLength =  lengthIndexedStartSeg.getEndIndex();	
		double startCoordLength = lengthIndexedStartSeg.project(startCoord);
		double startPercentageInLine = 100d / startSegEndLength * startCoordLength;
		double lengthCostInStartSeg = ((double)uncutStartSegLengthCost) / 100 * startPercentageInLine;
		
		// location in line string
		LinearLocation startLoc = indexedStartSeg.project(startCoord);
		
		Geometry cutStartGeom;
		// depending on direction cut the segment on the correct end
		if(direction) {
			cutStartGeom = indexedStartSeg.extractLine(indexedStartSeg.getStartIndex(), startLoc);
		} 
		else {
			cutStartGeom = indexedStartSeg.extractLine(startLoc, indexedStartSeg.getEndIndex());			
		}
		cutStartGeom.setSRID(startSeg.getGeometry().getSRID());
		
		// set cut segment
		startSeg.setGeometry((LineString) cutStartGeom);
		// adjust cost of segment
		
		if(direction) {
			startSeg.setLength((float) lengthCostInStartSeg);
		}
		else {
			startSeg.setLength((float) (uncutStartSegLengthCost - lengthCostInStartSeg));
		}
		
		direction = getDirection(endSeg, beforeEndSeg);	
		
		// refrence endpoint on segment
		LocationIndexedLine indexedEndSeg = new LocationIndexedLine(endSeg.getGeometry());
		// location in line string
		LinearLocation endLoc = indexedEndSeg.project(endCoord);
		
		LengthIndexedLine lengthIndexedEndSeg = new LengthIndexedLine(endSeg.getGeometry());
		double endSegEndLength =  lengthIndexedEndSeg.getEndIndex();	
		double endCoordLength = lengthIndexedEndSeg.project(endCoord);
		double endPercentageInLine = 100d / endSegEndLength * endCoordLength;
		double lengthCostInEndSeg = ((double)uncutEndSegLengthCost) / 100 * endPercentageInLine;
				
		Geometry cutEndGeom;
		// depending on direction cut the segment on the correct end
		if(direction) {
			cutEndGeom = indexedEndSeg.extractLine(indexedEndSeg.getStartIndex(), endLoc);
		} 
		else {
			cutEndGeom = indexedEndSeg.extractLine(endLoc, indexedEndSeg.getEndIndex());			
		}
		cutEndGeom.setSRID(endSeg.getGeometry().getSRID());
		
		// set cutted segment
		endSeg.setGeometry((LineString) cutEndGeom);
		// adjust cost of segment

		if(direction) {
			endSeg.setLength((float) lengthCostInEndSeg);
		}
		else {
			endSeg.setLength((float) (uncutEndSegLengthCost - lengthCostInEndSeg));
		}	
		
		// adjust route results
		route.setDuration(adjustRouteDuration(route, uncutStartSegDurationTow, uncutStartSegDurationBkw, uncutEndSegDurationTow, uncutEndSegDurationBkw));
		route.setLength(route.getLength()-uncutStartSegLengthCost-uncutEndSegLengthCost+startSeg.getLength()+ endSeg.getLength());
		return route;
	}

	private int adjustRouteDuration(IRoute<T> route, int uncutStartSegDurationTow, int uncutStartSegDurationBkw,
			int uncutEndSegDurationTow, int uncutEndSegDurationBkw) {
		int startSegmentDiff = 0;
		int endSegmentDiff = 0;
		if (!route.getSegments().isEmpty()) {
			if (route.getSegments().size() > 1) {
				if (route.getSegments().get(1).getEndNodeId()   == route.getSegments().get(0).getEndNodeId() || 
					route.getSegments().get(1).getStartNodeId() == route.getSegments().get(0).getEndNodeId()) {
					startSegmentDiff = uncutStartSegDurationTow - route.getSegments().get(0).getDuration(true);
				} else {
					startSegmentDiff = uncutStartSegDurationBkw - route.getSegments().get(0).getDuration(false);
				}
				if (route.getSegments().get(route.getSegments().size() - 2).getEndNodeId()   == route.getSegments().get(route.getSegments().size() - 1).getStartNodeId() || 
					route.getSegments().get(route.getSegments().size() - 2).getStartNodeId() == route.getSegments().get(route.getSegments().size() - 1).getStartNodeId()) {
					endSegmentDiff = uncutEndSegDurationTow - route.getSegments().get(route.getSegments().size() - 1).getDuration(true);
				} else {
					endSegmentDiff = uncutEndSegDurationBkw - route.getSegments().get(route.getSegments().size() - 1).getDuration(false);
				}
			}
		}
		return route.getDuration() - startSegmentDiff - endSegmentDiff;
	}

	protected IRoute<T> cutInSingleSegment(IRoute<T> route, T startSeg,
			Coordinate startCoord, Coordinate endCoord) {
		log.warn("cutting in a single segment is not implemented jet!");
		// TODO: implement me!
		return route;
	}

	protected boolean getDirection(T firstSeg,
			T secondSeg) {
		if(firstSeg.getStartNodeId() == secondSeg.getStartNodeId() || firstSeg.getStartNodeId() == secondSeg.getEndNodeId()) {
			return true;
		}
		else {
			return false;
		}		
	}

	public INeo4jWayGraphReadDao getGraphReadDao() {
		return graphReadDao;
	}

	public void setGraphReadDao(INeo4jWayGraphReadDao neo4jGraphDao) {
		this.graphReadDao = neo4jGraphDao;
	}

	public IWayGraphVersionMetadataDao getMetadataDao() {
		return metadataDao;
	}

	public void setMetadataDao(IWayGraphVersionMetadataDao metadataDao) {
		this.metadataDao = metadataDao;
	}

	public INeo4jCostEvaluatorFactory getCostEvaluatorFactory() {
		return costEvaluatorFactory;
	}

	public void setCostEvaluatorFactory(INeo4jCostEvaluatorFactory costEvaluatorFactory) {
		this.costEvaluatorFactory = costEvaluatorFactory;
	}

	public double getSearchDistance() {
		return searchDistance;
	}

	public void setSearchDistance(double searchDistance) {
		this.searchDistance = searchDistance;
	}
	
	public IGraphDatabaseProvider getGraphDatabaseProvider() {
		return graphDatabaseProvider;
	}

	public void setGraphDatabaseProvider(IGraphDatabaseProvider graphDatabaseProvider) {
		this.graphDatabaseProvider = graphDatabaseProvider;
	}

	public IRouteModelFactory<T> getModelFactory() {
		return modelFactory;
	}

	public void setModelFactory(IRouteModelFactory<T> modelFactory) {
		this.modelFactory = modelFactory;
	}

	public INeo4jNodeMapper<T> getNodeMapper() {
		return nodeMapper;
	}

	public void setNodeMapper(INeo4jNodeMapper<T> nodeMapper) {
		this.nodeMapper = nodeMapper;
	}

}