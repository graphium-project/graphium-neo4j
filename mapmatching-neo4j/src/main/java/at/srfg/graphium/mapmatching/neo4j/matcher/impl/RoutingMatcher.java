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

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.neo4j.graphalgo.GraphAlgoFactory;
import org.neo4j.graphalgo.PathFinder;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.PathExpander;
import org.neo4j.graphdb.PathExpanders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.vividsolutions.jts.geom.Point;

import at.srfg.graphium.geomutils.GeometryUtils;
import at.srfg.graphium.mapmatching.matcher.impl.SegmentDistance;
import at.srfg.graphium.mapmatching.model.Direction;
import at.srfg.graphium.mapmatching.model.IMatchedBranch;
import at.srfg.graphium.mapmatching.model.IMatchedWaySegment;
import at.srfg.graphium.mapmatching.model.ITrack;
import at.srfg.graphium.mapmatching.model.impl.MatchedWaySegmentImpl;
import at.srfg.graphium.mapmatching.neo4j.matcher.impl.AlternativePathMatcher.AlternativePath;
import at.srfg.graphium.mapmatching.properties.IMapMatchingProperties;
import at.srfg.graphium.mapmatching.statistics.MapMatcherStatistics;
import at.srfg.graphium.model.IWaySegment;
import at.srfg.graphium.neo4j.model.WaySegmentRelationshipType;
import at.srfg.graphium.neo4j.persistence.INeo4jWayGraphReadDao;
import at.srfg.graphium.routing.exception.RoutingParameterException;
import at.srfg.graphium.routing.exception.UnkownRoutingAlgoException;
import at.srfg.graphium.routing.model.IRoute;
import at.srfg.graphium.routing.model.IRoutingOptions;
import at.srfg.graphium.routing.model.impl.RoutingAlgorithms;
import at.srfg.graphium.routing.model.impl.RoutingCriteria;
import at.srfg.graphium.routing.model.impl.RoutingMode;
import at.srfg.graphium.routing.model.impl.RoutingOptionsImpl;
import at.srfg.graphium.routing.service.IRoutingService;

public class RoutingMatcher {
	
	private static Logger log = LoggerFactory.getLogger(RoutingMatcher.class);
	
	private MapMatchingTask matchingTask;
	private IMapMatchingProperties properties;
	
	private IRoutingService<IWaySegment, Node, IRoutingOptions> routingClient;
	private IRoutingOptions routingOptions;
	private TrackSanitizer trackSanitizer;
	
	private Cache<Pair<Long, Long>, List<IMatchedWaySegment>> cachedRoutes = null;
	private int cacheSize = 100;
	
	private int maxNrOfTargetSegments = 5;
	private int MAXSPEED_CAR_FRC_0 = 150; // km/h
	private int MAXSPEED_CAR_FRC_1_X = 120; // km/h
	private int MAXSPEED_CAR_URBAN = 70; // km/h
	private int MAXSPEED_BIKE = 50; // km/h
	private int MAXSPEED_PEDESTRIAN = 20; // km/h
	private int skippedPointsThresholdToCreateNewPath = 3;
	
	public RoutingMatcher(MapMatchingTask mapMatchingTask,
			IRoutingService<IWaySegment, Node, IRoutingOptions> routingClient, IMapMatchingProperties properties, TrackSanitizer trackSanitizer) throws RoutingParameterException {
		this.matchingTask = mapMatchingTask;
		this.routingClient = routingClient;
		this.properties = properties;
		this.trackSanitizer = trackSanitizer;
		
		RoutingMode routingMode = null;
		if (properties.getRoutingMode() != null && properties.getRoutingMode().length() > 0) {
			routingMode = RoutingMode.fromValue(properties.getRoutingMode());
		} else {
			routingMode = RoutingMode.CAR;
		}
		RoutingCriteria routingCriteria = (RoutingCriteria) RoutingCriteria.fromValue(properties.getRoutingCriteria());
		RoutingAlgorithms routingAlgorithm;
		if (properties.getRoutingAlgorithm() != null && properties.getRoutingAlgorithm().length() > 0) {
			routingAlgorithm = RoutingAlgorithms.valueOf(properties.getRoutingAlgorithm());
		} else {
			routingAlgorithm = RoutingAlgorithms.DIJKSTRA;
		}
		
		routingOptions = new RoutingOptionsImpl(matchingTask.getGraphName(), mapMatchingTask.getGraphVersion());
		routingOptions.setAlgorithm(routingAlgorithm);
		routingOptions.setCriteria(routingCriteria);
		routingOptions.setMode(routingMode);
		
		cachedRoutes = CacheBuilder.newBuilder()
								   .maximumSize(cacheSize)
								   .build();
		
		if (log.isDebugEnabled()) {
			log.debug("created " + this.getClass().getSimpleName() + " with following routing options: " + routingOptions.toString());
		}
	}
	/**
	 * Finds shortest paths from the last matched segment to segments near the given next point. If no path
	 * could be found to that point, the following points are tried or points are skipped and a new path is
	 * started from there.
	 * 
	 * @param branch 
	 * @param lastSegment The last matched segment
	 * @param pointIndex The point to find a alternative path to
	 * @param pathsToReturn Found alternative paths
	 * @param newBranches 
	 * @param fallbackRoutes 
	 * @param track
	 * @return The index of the point that will be used as start index for a new
	 * call of this method if no continuation can be found.
	 */
	int routeToNextPoint(
			IMatchedBranch branch, IMatchedWaySegment lastSegment, int pointIndex,
			List<AlternativePath> pathsToReturn, List<IMatchedBranch> newBranches, 
			List<AlternativePath> skippedPaths,
			List<AlternativePath> fallbackRoutes, ITrack track) {
		List<AlternativePath> potentialShortestPaths = new ArrayList<AlternativePath>();
		
		/*  the search radius is the same as the matching radius, otherwise the
		 * found segments might not have any match points
		 */
		int radius = properties.getMaxMatchingRadiusMeter();

		int pointIndexRoutingTo = getPossiblePathsToNextPoint(lastSegment, pointIndex, radius,
				track, potentialShortestPaths, skippedPaths, fallbackRoutes);
		
		// prepare paths to return: drop first segment (start segment), mark as obtained from routing 
		if (!potentialShortestPaths.isEmpty()) {
			for (AlternativePath potentialShortestPath : potentialShortestPaths) {
				List<IMatchedWaySegment> path = new ArrayList<IMatchedWaySegment>();

				boolean causesUTurn = false;
				int i=0;
				List<IMatchedWaySegment> segments = potentialShortestPath.getSegments();
				for (IMatchedWaySegment segment : segments) {
					// only segments in between are relevant OR there is only one match (e.g. U-Turn...)
					if (i > 0 || segments.size() == 1) {
						// set same start index for all segments, the real indices are set in matchShortestPathSegment()
						segment.setStartPointIndex(pointIndexRoutingTo);
						segment.setFromPathSearch(true);
						path.add(segment);
					} else if (i == 0) {
						// the first segment of the route is the last segment of the path, so no need to add it again
						if (segment.getId() == lastSegment.getId() &&
							segment.getDirection().isLeavingThroughStartNode() != lastSegment.getDirection().isLeavingThroughStartNode() &&
							lastSegment.getStartPointIndex() > 0) { // except first segment in branch
							// there is an u-turn on the first segment of the route
							causesUTurn = true;
						}
					}
					i++;
				}
				
				if (!path.isEmpty()) {
					pathsToReturn.add(AlternativePath.fromRouting(path, causesUTurn));
				}
			}
		}
		
		if (pointIndexRoutingTo + properties.getNrOfPointsToSkip()
				>= track.getTrackPoints().size() -1) {
			// if the end of the track was reached while trying to find a next segment,
			// mark the current branch as finished
			IMatchedBranch branchClone = matchingTask.getSegmentMatcher().getClonedBranch(branch);
			branchClone.setFinished(true);
			newBranches.add(branchClone);
		}

		return pointIndexRoutingTo + properties.getNrOfPointsToSkip();
	}

	/**
	 * Searches the shortest paths from the given segment to a following point. First, nearby segments
	 * to the given point are determined. Then for the five nearest segments, a shortest path is searched
	 * between the given source segment and the new target segment. If no segments can be found, 
	 * nearby segments of following points are used as target. 
	 * If target segments are found, but no shortest path can be determined (because the path is too
	 * long or there is no path), the path is split and starts again from the target segments.
	 * 
	 * @param lastSegment The last matched segment (the source)
	 * @param pointIndex The point to find a shortest path to
	 * @param radius The radius around the point in which segment will be searched
	 * @param track
	 * @param potentialShortestPaths Found shortest paths
	 * @param skippedPaths Paths for skipped, non-matchable areas
	 * @param fallbackRoutes Paths skipping parts that are used when none of the found routes is valid
	 * @return The index of the point actually used for the shortest path search
	 */
	private int getPossiblePathsToNextPoint(IMatchedWaySegment lastSegment, int pointIndex,
			int radius, ITrack track,
			List<AlternativePath> potentialShortestPaths, List<AlternativePath> skippedPaths,
			List<AlternativePath> fallbackRoutes) {
		
		boolean foundTargetSegment = false;
		int skippedPoints = pointIndex - lastSegment.getEndPointIndex();
		
		int initialPointIndexRoutingFrom = lastSegment.getEndPointIndex() - 1;
		int tryRouteToNextPoint = 0;
		do {
			matchingTask.checkCancelStatus();
			
			if (log.isDebugEnabled()) {
				log.debug("Search possible paths for track " + track.getId() + " to point index " + pointIndex);
			}
			
			foundTargetSegment = getShortestPath(lastSegment, track, pointIndex, skippedPoints, radius, initialPointIndexRoutingFrom, 
					potentialShortestPaths, skippedPaths, fallbackRoutes);
			
			// if no route could be found for the next point, try one of the next points (the third)
			if (!foundTargetSegment && potentialShortestPaths.isEmpty()) {
				int pointsToSkip = 0;
				if (pointIndex == (track.getTrackPoints().size() - 1)) {
					pointsToSkip++; // Abbruchkriterium
				} else {
					// Aufspannen eines Envelopes mit Kantenlänge threshold Meter und Berücksichtigung aller Punkte innerhalb dieses Envelopes;
					// Nimm jenen Punkt, der wieder außerhalb dieses Envelopes liegt.
					pointsToSkip = trackSanitizer.determineNextValidPointForSegmentSearch(track, pointIndex, properties.getEnvelopeSideLength());
					
				}

				pointIndex += pointsToSkip;
				skippedPoints += pointsToSkip;

			} else if (foundTargetSegment && potentialShortestPaths.isEmpty()) {
				
 				while (properties.getMeanSamplingInterval() < properties.getThresholdSamplingIntervalForTryingFurtherPathSearches() && // statistically this methodology results in worse paths for higher sampling intervals
					   tryRouteToNextPoint <= properties.getNrOfPointsToSkip() && 
					   potentialShortestPaths.isEmpty()) {
					pointIndex++;
					skippedPoints++;
					tryRouteToNextPoint++;
					List<AlternativePath> dummySkippedPaths = new ArrayList<>();
					List<AlternativePath> dummyFallbackRoutes = new ArrayList<>();
					foundTargetSegment = getShortestPath(lastSegment, track, pointIndex, skippedPoints, radius, initialPointIndexRoutingFrom, 
							potentialShortestPaths, dummySkippedPaths, dummyFallbackRoutes);
					
				}
				
			}

		} while (!foundTargetSegment && potentialShortestPaths.isEmpty() && pointIndex < track.getTrackPoints().size());
		
		return pointIndex;
	}
	
	private boolean getShortestPath(IMatchedWaySegment lastSegment, ITrack track, int pointIndex, int skippedPoints, int radius, 
			int initialPointIndexRoutingFrom, List<AlternativePath> potentialShortestPaths, List<AlternativePath> skippedPaths, List<AlternativePath> fallbackRoutes) {
		boolean foundTargetSegment = false;
		// get segments near the next point
					List<SegmentDistance<IWaySegment>> targetSegments = matchingTask.getInitialMatcher()
																		.findStartSegments(track, pointIndex, radius, maxNrOfTargetSegments);
					
					// find path for the first five end segments, beginning with the nearest segment
					Iterator<SegmentDistance<IWaySegment>> it = targetSegments.iterator();
					while (it.hasNext()) {
						List<IMatchedWaySegment> shortestPath = null;
						SegmentDistance<IWaySegment> targetSegment = it.next();

						/* There is at least one segment near the target point. No matter if we find a valid 
						 * route or not, stop after the current point.
						 */
						foundTargetSegment = true;

						int pointDiff = (int) (track.getTrackPoints().get(pointIndex).getTimestamp().getTime() - 
											   track.getTrackPoints().get(lastSegment.getEndPointIndex()).getTimestamp().getTime()) / 60000;
						
						int pointDiffThreshold = properties.getPointsDiffThresholdForSkipRouting();
						if (properties.isLowSamplingInterval()) {
							pointDiffThreshold = pointDiffThreshold / 2;
						}
						pointDiffThreshold = Math.max(2, pointDiffThreshold);
						
						if (pointDiff < pointDiffThreshold &&
							targetSegment.getSegment().getId() != lastSegment.getId()) {
							/* only try to find a shortest path between the last and the target segment, if
							 * they are not the same segment and if not too much points have been skipped
							 */
							shortestPath = getShortestPath(
									lastSegment, 
									targetSegment.getSegment(),
									properties.getTempMaxSegmentsForShortestPath(),
									matchingTask.getGraphDao(), 
									matchingTask.getGraphName(),
									matchingTask.getGraphVersion());
							
							if (shortestPath != null && !shortestPath.isEmpty()) {
								if (checkImpossibleRoute(shortestPath, track, initialPointIndexRoutingFrom, pointIndex)) {
									potentialShortestPaths.add(AlternativePath.fromRouting(shortestPath));
								}
							}

							matchingTask.statistics.incrementValue(MapMatcherStatistics.SHORTEST_PATH_SEARCH);

						}

						if (skippedPoints > skippedPointsThresholdToCreateNewPath || 
								(potentialShortestPaths.isEmpty() && (shortestPath == null || shortestPath.isEmpty()))) {
							/* No route found or points skipped, split the track at this position
							 * and find new segments after the skipped points
							 */
//							skippedPaths.clear();
							addSegmentsAfterSkippedPoints(targetSegment, pointIndex, 
									skippedPaths, track);
							
						} else {
							/* Even if a route was found or no points were skipped, remember the found segment.
							 * In case it is not possible to create a valid branch from the found routes,
							 * the segment can still be used as fallback.
							 */
							addSegmentsAfterSkippedPoints(targetSegment, pointIndex, 
									fallbackRoutes, track);
						}
					}
		return foundTargetSegment;

	}

	/**
     * @return false if route is possible so the average time the route takes to drive through could match the track
     */
    private boolean checkImpossibleRoute(List<IMatchedWaySegment> routedSegments, ITrack track, int startIndex, int endIndex) {
    	boolean valid = true;
    	if (routedSegments.size() > 1) {
            if (startIndex == endIndex || endIndex <= 0) {
            	valid = true;
            } else {
	
	            long timeDiffS = (track.getTrackPoints().get(endIndex).getTimestamp().getTime() -
	                             track.getTrackPoints().get(startIndex).getTimestamp().getTime()) / 1000;
	
	            float durationS = 0;
	            int i = 0;
	            for (IMatchedWaySegment seg : routedSegments) {
	                float maxSpeed = 0;
	                
	                // MAXSPEEDs: GIP's maxSpeed is often too low
	                if (routingOptions.getMode().equals(RoutingMode.CAR)) {
		                if (seg.getFrc().getValue() == 0 &&
		                	seg.getFormOfWay().getValue() != 10) {
		                	maxSpeed = MAXSPEED_CAR_FRC_0;
		                } else {
		                	if (seg.isUrban()) {
		                		maxSpeed = MAXSPEED_CAR_URBAN;
		                	} else {
		                		maxSpeed = MAXSPEED_CAR_FRC_1_X;
		                	}
		                }
	                } else if (routingOptions.getMode().equals(RoutingMode.BIKE)) {
	                	maxSpeed = MAXSPEED_BIKE;
	                } else {
	                	maxSpeed = MAXSPEED_PEDESTRIAN;
	                }
	                
	                float length = 0;
	                if (i == 0) {
	                	// first segment is already partially travelled
	                	Point p = track.getTrackPoints().get(startIndex).getPoint();
	                	seg.getGeometry().setSRID(p.getSRID());
	                	length = (float) GeometryUtils.distanceOnLineStringInMeter(p, seg.getGeometry());
	                	if (seg.getDirection().isEnteringThroughStartNode()) {
	                		// gegen Digitalisierungsrichtung
	                		length = seg.getLength() - length;
	                	}
	                } else if (i == routedSegments.size() - 1) {
	                	// last segment is possibly only partially travelled
	                	Point p = track.getTrackPoints().get(endIndex).getPoint();
	                	seg.getGeometry().setSRID(p.getSRID());
	                	length = (float) GeometryUtils.distanceOnLineStringInMeter(p, seg.getGeometry());
	                	if (seg.getDirection().isEnteringThroughEndNode()) {
	                		// gegen Digitalisierungsrichtung
	                		length = seg.getLength() - length;
	                	}
	                } else {
	                	length = seg.getLength();
	                }
	                durationS += length / (maxSpeed / 3.6);
	                i++;
	            }
	
	            if (timeDiffS >= (int)durationS) { // (int) => round
	            	valid = true;
	            } else {
	            	if (log.isDebugEnabled()) {
	            		log.debug("Route from segment " + routedSegments.get(0).getId() + " to segment " + routedSegments.get(routedSegments.size()-1).getId() +
	            				" is not possible: calculated min duration = " + durationS + "sec > trackPoint's time diff = " + timeDiffS + "sec");
	            	}
	            	valid = false;
	            }
            }

            // check if route contains U-turn
            // hopefully we don't need this...
            if (valid) {
            	for (IMatchedWaySegment routedSegment : routedSegments) {
            		if (routedSegment.getDirection().equals(Direction.START_TO_START) ||
            			routedSegment.getDirection().equals(Direction.END_TO_END)) {
            			valid = false;
            		}
            	}
            }
            
        } else {
        	valid = false;
        }
    	
    	return valid;
    } 

	/**
	 * Starts new paths at index {@code pointIndexAfterSkippedPart} with segment
	 * {@code segmentAfterSkippedPart}. The found paths are added to the
	 * {@code skippedPaths} list.
	 * 
	 * @param segmentAfterSkippedPart
	 * @param pointIndexAfterSkippedPart
	 * @param skippedPaths
	 * @param track
	 */
	protected void addSegmentsAfterSkippedPoints(
			SegmentDistance<IWaySegment> segmentAfterSkippedPart, int pointIndexAfterSkippedPart,
			List<AlternativePath> skippedPaths, ITrack track) {
		
		IMatchedWaySegment matchedWaySegment = new MatchedWaySegmentImpl();
		
		matchedWaySegment.setSegment(segmentAfterSkippedPart.getSegment());
		matchedWaySegment.setStartPointIndex(pointIndexAfterSkippedPart);
		matchedWaySegment.setAfterSkippedPart(true);
		
		List<IMatchedBranch> branches = matchingTask.getInitialMatcher().initializePaths(
				matchedWaySegment, 
				pointIndexAfterSkippedPart, 
				track);
		
		if (branches.isEmpty()) {
			/* if no successor segment could be found for the segment, still keep the
			 * segment to account for map errors or cases when the vehicle drove the wrong
			 * way on an one-way segment
			 */
			setDirectionForUnattachedSegment(matchedWaySegment, track);
			matchedWaySegment.setStartSegment(false);
			skippedPaths.add(AlternativePath.afterSkippedPart(Collections.singletonList(matchedWaySegment)));
		}
		
		for (IMatchedBranch branch : branches) {
			branch.getMatchedWaySegments().get(0).setStartSegment(false);
			skippedPaths.add(AlternativePath.afterSkippedPart(branch.getMatchedWaySegments()));
		}
	}

	/**
	 * Estimates the driving direction for a single segment that has no direct predecessor
	 * or successor. The distances between the start and end point of the segment, and
	 * the first and last matched point of the segment are calculated. Then the direction
	 * is set so that the distances are minimal.
	 */
	protected void setDirectionForUnattachedSegment(IMatchedWaySegment matchedWaySegment, ITrack track) {
		int startMatchPointIndex = matchedWaySegment.getStartPointIndex();
		int endMatchPointIndex = Math.min(matchedWaySegment.getEndPointIndex(), track.getTrackPoints().size() - 1);
		
		if (endMatchPointIndex - startMatchPointIndex < 2) {
			// less than two points were matched, set a "random" direction
			matchedWaySegment.setDirection(Direction.START_TO_END);
		}
		
		Point segmentStartPoint = matchedWaySegment.getSegment().getGeometry().getStartPoint();
		Point segmentEndPoint = matchedWaySegment.getSegment().getGeometry().getEndPoint();
		
		Point firstMatchedPoint = track.getTrackPoints().get(startMatchPointIndex).getPoint();
		Point lastMatchedPoint = track.getTrackPoints().get(endMatchPointIndex).getPoint();
		
		double distanceStartToFirst = GeometryUtils.distanceMeters(segmentStartPoint, firstMatchedPoint);
		double distanceStartToLast = GeometryUtils.distanceMeters(segmentStartPoint, lastMatchedPoint);
		double distanceEndToFirst = GeometryUtils.distanceMeters(segmentEndPoint, firstMatchedPoint);
		double distanceEndToLast = GeometryUtils.distanceMeters(segmentEndPoint, lastMatchedPoint);
		
		if (distanceStartToFirst + distanceEndToLast < distanceStartToLast + distanceEndToFirst) {
			matchedWaySegment.setDirection(Direction.START_TO_END);
		} else {
			matchedWaySegment.setDirection(Direction.END_TO_START);
		}
	}

	/**
	 * Performs the actual shortest path search between two segments by internally using
	 * the Neo4J algorithms.
	 * 
	 * Returns the shortest (fastest) path between two segments.
	 */
	List<IMatchedWaySegment> getShortestPath(
			IWaySegment fromSegment, 
			IWaySegment toSegment, 
			int maxSegmentsForShortestPath,
			INeo4jWayGraphReadDao graphDao,
			String graphName,
			String version) {
		
		List<IMatchedWaySegment> segments = null;
		
		Pair<Long, Long> cacheKey = new ImmutablePair<Long, Long>(fromSegment.getId(), toSegment.getId());
		if (cachedRoutes != null) {
			segments = cachedRoutes.getIfPresent(cacheKey);
			
			if (segments != null) {
				if (log.isDebugEnabled()) {
					log.debug("found route from segment " + fromSegment.getId() + " to segment " + toSegment.getId() + " in cache");
				}
				return segments;
			}
		}
		
		if (log.isDebugEnabled()) {
			log.debug("find route from segment " + fromSegment.getId() + " to segment " + toSegment.getId());
		}

		long startTime = System.nanoTime();
		
		Node startNode = getNodeBySegmentId(graphDao, graphName, version, fromSegment.getId()); 
		Node targetNode = getNodeBySegmentId(graphDao, graphName, version, toSegment.getId());
		
		if (validPathExists(startNode, targetNode, maxSegmentsForShortestPath)) {
		
			List<Long> segmentIds = new ArrayList<>();
			segmentIds.add(fromSegment.getId());
			segmentIds.add(toSegment.getId());
			
			// if a path can be found with at most 'maxSegmentsForShortestPath' segments, run exact routing 
			IRoute<IWaySegment, Node> route = null;
			try {
				route = routingClient.routePerSegmentIds(routingOptions, segmentIds);
			} catch (UnkownRoutingAlgoException e) {
				log.error("Could not route!", e);
			}
			
			if (route != null && route.getSegments() != null && route.getSegments().size() > 1) {
				List<IWaySegment> routeSegments = route.getSegments();
				segments = new ArrayList<IMatchedWaySegment>(routeSegments.size());
				
				for (int i = 1; i < routeSegments.size(); i++) {
					IWaySegment previousSegment = routeSegments.get(i - 1);
					IWaySegment currentSegment = routeSegments.get(i);
					
					if (i == 1) {
						// save very first segment
						Direction direction;
						
						if (previousSegment.getEndNodeId() == currentSegment.getStartNodeId() ||
							previousSegment.getEndNodeId() == currentSegment.getEndNodeId()) {
							// the first segment was left through the end node
							direction = Direction.START_TO_END;
						} else {
							direction = Direction.END_TO_START;
						}
						
						addSegment(segments, previousSegment, direction);
					}
					
					// save the current segment
					Direction direction;
					
					if (currentSegment.getStartNodeId() == previousSegment.getEndNodeId() ||
						currentSegment.getStartNodeId() == previousSegment.getStartNodeId()) {
						// the current segment was entered through the start node
						if (i < routeSegments.size() - 1) {
							IWaySegment nextSegment = routeSegments.get(i + 1);
							if (currentSegment.getStartNodeId() == nextSegment.getStartNodeId() ||
								currentSegment.getStartNodeId() == nextSegment.getEndNodeId()) {
								direction = Direction.START_TO_START;
							} else {
								direction = Direction.START_TO_END;
							}
						} else {						
							direction = Direction.START_TO_END;
						}
					} else {
						// the current segment was entered through the end node
						if (i < routeSegments.size() - 1) {
							IWaySegment nextSegment = routeSegments.get(i + 1);
							if (currentSegment.getEndNodeId() == nextSegment.getStartNodeId() ||
								currentSegment.getEndNodeId() == nextSegment.getEndNodeId()) {
								direction = Direction.END_TO_END;
							} else {
								direction = Direction.END_TO_START;
							}
						} else {						
							direction = Direction.END_TO_START;
						}
					}
					
					addSegment(segments, currentSegment, direction);
				}
				
			}
		} else {
			log.debug("routing not possible");
		}
			
		if (log.isDebugEnabled()) {
			long endTime = System.nanoTime();
			log.debug("routing took " + (endTime - startTime) + "ns");
		}
		
		if (cacheKey != null && segments != null) {
			cachedRoutes.put(cacheKey, segments);
		}
		
		return segments;
	}

	/**
	 * Returns {@code true} if a path with at most {@code maxSegmentsForShortestPath} segments
	 * exists between {@code startNode} and {@code endNode}.
	 * This method is necessary to detect graph errors, especially missing connections
	 */
	private boolean validPathExists(Node startNode, Node targetNode,
			int maxSegmentsForShortestPath) {
		PathExpander<Object> expander = PathExpanders.forTypesAndDirections(
												WaySegmentRelationshipType.SEGMENT_CONNECTION_ON_STARTNODE, org.neo4j.graphdb.Direction.OUTGOING, 
												WaySegmentRelationshipType.SEGMENT_CONNECTION_ON_ENDNODE, org.neo4j.graphdb.Direction.OUTGOING);
		
		PathFinder<Path> pathFinder = GraphAlgoFactory.shortestPath(expander, maxSegmentsForShortestPath);
		Path path = pathFinder.findSinglePath(startNode, targetNode);
		
		return path != null;
	}

	private void addSegment(List<IMatchedWaySegment> segments,
			IWaySegment segment, Direction direction) {
		IMatchedWaySegment matchedWaySegment = new MatchedWaySegmentImpl();
		matchedWaySegment.setSegment(segment);
		matchedWaySegment.setDirection(direction);
		
		segments.add(matchedWaySegment);
	}
	
	private Node getNodeBySegmentId(INeo4jWayGraphReadDao graphDao, String graphName, String version, long id) {
		return graphDao.getSegmentNodeBySegmentId(graphName, version, id);
	}

	// TODO uncomment...
//	public void cancel() {
//		routingClient.cancel();
//	}
	
	public int getMaxNrOfTargetSegments() {
		return maxNrOfTargetSegments;
	}
	public void setMaxNrOfTargetSegments(int maxNrOfTargetSegments) {
		this.maxNrOfTargetSegments = maxNrOfTargetSegments;
	}
	
}