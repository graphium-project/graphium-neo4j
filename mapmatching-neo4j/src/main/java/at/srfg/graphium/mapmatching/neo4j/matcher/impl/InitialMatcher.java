/**
 * Graphium Neo4j - Module of Graphserver for Map Matching using Neo4j
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
package at.srfg.graphium.mapmatching.neo4j.matcher.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.codehaus.jackson.map.util.LRUMap;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.graphdb.traversal.Traverser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import at.srfg.graphium.geomutils.GeometryUtils;
import at.srfg.graphium.core.exception.GraphNotExistsException;
import at.srfg.graphium.mapmatching.matcher.impl.SegmentDistance;
import at.srfg.graphium.mapmatching.model.Direction;
import at.srfg.graphium.mapmatching.model.IMatchedBranch;
import at.srfg.graphium.mapmatching.model.IMatchedWaySegment;
import at.srfg.graphium.mapmatching.model.ITrack;
import at.srfg.graphium.mapmatching.model.ITrackPoint;
import at.srfg.graphium.mapmatching.model.impl.MatchedBranchImpl;
import at.srfg.graphium.mapmatching.model.impl.MatchedWaySegmentImpl;
import at.srfg.graphium.mapmatching.properties.IMapMatchingProperties;
import at.srfg.graphium.model.IWaySegment;
import at.srfg.graphium.model.OneWay;
import at.srfg.graphium.neo4j.persistence.INeo4jWayGraphReadDao;

/**
 * This class takes care of the initial matching: It searches segments
 * nearby the first track points and tries to start a path for those segments.
 */
public class InitialMatcher {
	
	private static Logger log = LoggerFactory.getLogger(InitialMatcher.class);
	
	private MapMatchingTask matchingTask;
	private IMapMatchingProperties properties;

	// TODO use generic map
	private LRUMap<ITrackPoint, List<SegmentDistance<IWaySegment>>> startSegmentsCache;
	
	private TraversalDescription traversalDescription;

	public InitialMatcher(MapMatchingTask matchingTask,
			IMapMatchingProperties properties, Neo4jUtil neo4jUtil) {
		this.matchingTask = matchingTask;
		this.properties = properties;

		traversalDescription = neo4jUtil.getTraverser();
		
		startSegmentsCache = new LRUMap<ITrackPoint, List<SegmentDistance<IWaySegment>>>(50, 200);
	}

	/**
	 * Finds segments near the given point and builds a path consisting of the start segment
	 * and a connected, following segment for every valid match.
	 * 
	 * @param pointIndex
	 * @param track
	 * @return 
	 */
	public List<IMatchedBranch> getStartPaths(int pointIndex, ITrack track, int maxNrOfInitialSegments) {
		// initial state: find potential start segments
		List<SegmentDistance<IWaySegment>> distances = findStartSegments(
				track, 
				pointIndex, 
				properties.getIntialRadiusMeter(),
				maxNrOfInitialSegments );
		
		if (distances == null || distances.isEmpty()) {
			// no segments are nearby the given point
			return null;
		}
		
		// find path for each start segment, beginning with the nearest segment
		Iterator<SegmentDistance<IWaySegment>> it = distances.iterator();
		
		// list of paths for the start segments
		List<IMatchedBranch> branches = new ArrayList<IMatchedBranch>();
		
		// initialize paths for the best 3 start segments
		while (it.hasNext()) {
				SegmentDistance<IWaySegment> dist = it.next();
			
				IMatchedWaySegment matchedWaySegment = new MatchedWaySegmentImpl();
				matchedWaySegment.setSegment(dist.getSegment());
				matchedWaySegment.setStartPointIndex(pointIndex);
	
				branches.addAll(initializePaths(
						matchedWaySegment,
						pointIndex,
						track));
		}
		
		return branches;
	}

	/**
	 * Finds start segment by ID and builds a path consisting of the start segment
	 * and a connected, following segment for every valid match.
	 * 
	 * @param pointIndex
	 * @param track
	 * @param startSegmentId ID of start segment
	 * @return 
	 */
	public List<IMatchedBranch> getStartPathForStartSegment(int pointIndex, ITrack track, long startSegmentId) {
		// initial state: find start segments
		SegmentDistance<IWaySegment> distance = findStartSegment(track, pointIndex, properties.getIntialRadiusMeter(), startSegmentId);
		
		if (distance == null) {
			// no segments are nearby the given point
			return null;
		}
		
		// list of paths for the start segments
		List<IMatchedBranch> branches = new ArrayList<IMatchedBranch>();
		
		IMatchedWaySegment matchedWaySegment = new MatchedWaySegmentImpl();
		matchedWaySegment.setSegment(distance.getSegment());
		matchedWaySegment.setStartPointIndex(pointIndex);

		branches.addAll(initializePaths(
				matchedWaySegment,
				pointIndex,
				track));
		
		return branches;
	}

	/**
	 * Finds potential start segments for the point with the given index. First segments within the
	 * given radius of the point are searched using a Neo4J nearest neighbor search. Then the
	 * distances from these segments to the point is calculated.
	 * 
	 */
	@Transactional(readOnly=true)
	List<SegmentDistance<IWaySegment>> findStartSegments(ITrack track, int pointIndex, int radiusInMeter, int maxNrOfSegments) {
		if (pointIndex >= track.getTrackPoints().size()) {
			return Collections.emptyList();
		}
		
		final ITrackPoint startPoint = track.getTrackPoints().get(pointIndex);
	
		if (startSegmentsCache.containsKey(startPoint)) {
			// the start segments for the given point have already been calculated, return from cache
			return (List<SegmentDistance<IWaySegment>>) startSegmentsCache.get(startPoint);
		}
		
		Iterable<IWaySegment> startSegments = new ArrayList<IWaySegment>();
		try {
			startSegments = matchingTask.getGraphDao().findNearestSegments(
					matchingTask.getGraphName(), 
					matchingTask.getGraphVersion(),
					startPoint.getPoint(), 
					(double) radiusInMeter/1000,
					maxNrOfSegments);
		} catch (GraphNotExistsException e) {
			log.warn("could not find near segments for graph: " + e.getGraphName());
		}
		
		List<SegmentDistance<IWaySegment>> distances = getDistances(startSegments, startPoint, radiusInMeter);

		// sort segments by distance
		Collections.sort(distances);
		
		// store start segments in cache
		startSegmentsCache.put(startPoint, distances);
		
		return distances;
	}

	/**
	 * Finds start segment by ID for the point with the given index. The distances from the segment to the point is calculated.
	 * 
	 */
	@Transactional(readOnly=true)
	SegmentDistance<IWaySegment> findStartSegment(ITrack track, int pointIndex, int radiusInMeter, long startSegmentId) {
		if (pointIndex >= track.getTrackPoints().size()) {
			return null;
		}
		
		final ITrackPoint startPoint = track.getTrackPoints().get(pointIndex);
	
		IWaySegment startSegment = null;
		try {
			startSegment = (IWaySegment) matchingTask.getGraphDao().getSegmentById(matchingTask.getGraphName(), matchingTask.getGraphVersion(), startSegmentId, true);
		} catch (GraphNotExistsException e) {
			log.warn("could not find near segments for graph: " + e.getGraphName());
		}
		
		if (startSegment != null) {
			List<SegmentDistance<IWaySegment>> distances = getDistances(Collections.singletonList(startSegment), startPoint, radiusInMeter);
	
			if (!distances.isEmpty()) {
				
				// sort segments by distance
				Collections.sort(distances);
				
				// store start segments in cache
				startSegmentsCache.put(startPoint, distances);
				
				return distances.get(0);
				
			} else {
				return null;
			}
		} else {
			return null;
		}
	}

	/**
	 * Calculates the distances from the given segments to the first point.
	 */
	private List<SegmentDistance<IWaySegment>> getDistances(
			Iterable<IWaySegment> startSegments,
			final ITrackPoint startPoint, double radiusInMeter) {
		List<SegmentDistance<IWaySegment>> distances = new ArrayList<SegmentDistance<IWaySegment>>();
					
		if (startSegments != null) {
			for (IWaySegment seg : startSegments) {
				matchingTask.checkCancelStatus();
				
				double distance = GeometryUtils.distanceMeters(seg.getGeometry(), startPoint.getPoint());
				
				// check if segment is really within searching radius
			    if (distance <= radiusInMeter) {
			    	distances.add(new SegmentDistance<IWaySegment>(seg, distance));
			    } 
			}
		}
		
		return distances;
	}

	/**
	 * Retrieves the connected segments for the given start segment and checks whether the
	 * new segment can be matched to the track. If so, a new path is created. 
	 * 
	 * @param startSegment
	 * @param pointIndex
	 * @param track
	 * @return Paths for every connected and valid segment
	 */
	List<IMatchedBranch> initializePaths(IMatchedWaySegment startSegment, int pointIndex, ITrack track) {
		List<IMatchedBranch> paths = new ArrayList<IMatchedBranch>();
		
		// get all points that can be matched to the start segment
		assignMatchingPoints(startSegment, pointIndex, track);
	
		// retrieve all outgoing connections from the start segment
		Traverser traverser = this.getTraverserForStartSegment(
				startSegment, 
				matchingTask.getGraphDao(), 
				matchingTask.getGraphName(),
				matchingTask.getGraphVersion());
			
		if (traverser != null) {
			// per default add branch with start segment
			addSingleSegmentPaths(startSegment, paths, track);

			Iterator<Path> connectedPaths = traverser.iterator();
			
			while (connectedPaths.hasNext()) {
				Path connectedPath = connectedPaths.next();
				Node connectedSegmentNode = connectedPath.endNode();
			
				// create a new path for each connected segment with a clone of the start segment
				// a copy of the start segment is used, because the matched points of the start segment might change
				IMatchedWaySegment clonedSegment = cloneStartSegment(startSegment);
				if (clonedSegment == null) {
					continue;
				}
				IMatchedBranch branch = new MatchedBranchImpl(matchingTask.getWeightingStrategy());
				branch.addMatchedWaySegment(clonedSegment);
	
				// try to match every connected segment (calculate matching factor)
				IMatchedWaySegment matchedSegment = matchingTask.getSegmentMatcher().matchSegment(
						matchingTask.getGraphDao().mapNode(matchingTask.getGraphName(), matchingTask.getGraphVersion(), connectedSegmentNode), 
						track,
						startSegment.getEndPointIndex(),
						properties.getMaxMatchingRadiusMeter(),
						branch);

				if (matchedSegment != null && clonedSegment.getMatchedPoints() > 0) {
					// the matching for the connected segment is valid and the start segment also still
					// has match-points, then add the new segment to the initial path
					PathExpanderMatcher.setSegmentDirection(startSegment, matchedSegment);
					setStartSegmentDirection(clonedSegment, matchedSegment);
					
					branch.addMatchedWaySegment(matchedSegment);
					
					if (matchingTask.getWeightingStrategy().branchIsValid(branch)) {
						paths.add(branch);
					}
				}
			}
		}
		
		return paths;
	}

	/**
	 * If no valid connected segment can be found for a start segment,
	 * create two paths for the start segment. One path for each travel
	 * direction (START_TO_END and END_TO_START).
	 */
	private void addSingleSegmentPaths(IMatchedWaySegment startSegment,
			List<IMatchedBranch> paths, ITrack track) {
		if (!startSegment.isOneway().equals(OneWay.ONEWAY_BKW)) {
			addSingleSegmentPath(startSegment, Direction.START_TO_END, paths, track);
		}
		
		if (!startSegment.isOneway().equals(OneWay.ONEWAY_TOW)) {
			addSingleSegmentPath(startSegment, Direction.END_TO_START, paths, track);
		}
	}

	private void addSingleSegmentPath(IMatchedWaySegment startSegment, Direction direction,
			List<IMatchedBranch> paths, ITrack track) {
		IMatchedWaySegment clonedSegment = cloneStartSegment(startSegment);
		clonedSegment.setDirection(direction);
		
		IMatchedBranch branch = new MatchedBranchImpl(matchingTask.getWeightingStrategy());
		branch.addMatchedWaySegment(clonedSegment);
		
		if (clonedSegment.getMatchedPoints() > 0 &&
				matchingTask.getWeightingStrategy().branchIsValid(branch)) {
			paths.add(branch);
		}
	}

	static void setStartSegmentDirection(
			IMatchedWaySegment startSegment, IMatchedWaySegment nextSegment) {
		if (nextSegment.getDirection().isEnteringThroughStartNode()) {
			if (startSegment.getEndNodeId() == nextSegment.getStartNodeId()) {
				startSegment.setDirection(Direction.START_TO_END);
			} else {
				startSegment.setDirection(Direction.END_TO_START);
			}
		} else {
			if (startSegment.getEndNodeId() == nextSegment.getEndNodeId()) {
				startSegment.setDirection(Direction.START_TO_END);
			} else {
				startSegment.setDirection(Direction.END_TO_START);
			}
		}
	}

	/**
	 * Assigns all points that can be matched to the given segment.
	 * 
	 * @param startSegment
	 * @param pointIndex
	 * @param track
	 */
	private void assignMatchingPoints(IMatchedWaySegment startSegment,
			int pointIndex, ITrack track) {
		int lastPointIndex = matchingTask.getSegmentMatcher().getLastPointIndex(
				startSegment, 
				pointIndex, 
				track, 
				properties.getMaxMatchingRadiusMeter());
		startSegment.setEndPointIndex(lastPointIndex);
		
		startSegment.calculateDistances(track);
		startSegment.setStartSegment(true);
	}

	private Traverser getTraverserForStartSegment(IMatchedWaySegment startSegment, INeo4jWayGraphReadDao graphDao, String graphName, String version) {
		Node node = getNodeBySegmentId(graphDao, graphName, version, startSegment.getId());
		
		return traversalDescription.traverse(node);
	}
	
	private Node getNodeBySegmentId(INeo4jWayGraphReadDao graphDao, String graphName, String version, long id) {
		return graphDao.getSegmentNodeBySegmentId(graphName, version, id);
	}
	
	private static IMatchedWaySegment cloneStartSegment(
			IMatchedWaySegment startSegment) {
		IMatchedWaySegment clonedStartSegment = null;
		
		try {
			clonedStartSegment = (IMatchedWaySegment) startSegment.clone();
		} catch (CloneNotSupportedException e) {
			InitialMatcher.log.error("could not clone segment", e);
			return null;
		}
		
		return clonedStartSegment;
	}

	protected LRUMap getStartSegmentsCache() {
		return startSegmentsCache;
	}

}
