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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.srfg.graphium.mapmatching.model.Direction;
import at.srfg.graphium.mapmatching.model.IMatchedBranch;
import at.srfg.graphium.mapmatching.model.IMatchedWaySegment;
import at.srfg.graphium.mapmatching.model.ITrack;
import at.srfg.graphium.mapmatching.statistics.MapMatcherStatistics;


/**
 * This class contains the logic to expand paths by running shortest-path
 * searches or by skipping points.
 */
public class AlternativePathMatcher {
	
	private static Logger log = LoggerFactory.getLogger(AlternativePathMatcher.class);
	
	private MapMatchingTask matchingTask;
	
	public AlternativePathMatcher(MapMatchingTask mapMatchingTask) {
		this.matchingTask = mapMatchingTask;
	}

	
	/**
	 * If no path can be expanded with segments directly connected to the last
	 * segments of the paths, this function tries to find continuations by doing
	 * shortest-path-searches and by skipping track points.
	 * 
	 * This method stops when at least one path could be expanded or the end of the
	 * track was reached, that is a path was finished.
	 * 
	 */
	List<IMatchedBranch> searchAlternativePaths(
				ITrack track,
				List<IMatchedBranch> branches,
				List<IMatchedBranch> newBranches) {
		
		log.debug("search routes");
		
		List<SearchPath> searchPaths = new LinkedList<SearchPath>();
		for (IMatchedBranch branch : branches) {
			IMatchedWaySegment lastSegment = branch.getMatchedWaySegments().get(branch.getMatchedWaySegments().size() - 1);
			searchPaths.add(new SearchPath(lastSegment.getEndPointIndex(), branch));
		}
		
		if (!searchPaths.isEmpty()) {
			searchAlternativePaths2(searchPaths, newBranches, track);
		}

		log.debug("search routes finished");

		return newBranches;
	}

	private void searchAlternativePaths2(List<SearchPath> searchPaths,
			List<IMatchedBranch> newBranches, ITrack track) {
		List<SearchPath> newSearchPaths = new LinkedList<SearchPath>();
		
		Set<Integer> searchIndexes = new HashSet<>();
		
		Map<Integer, List<IMatchedBranch>> alternativeBranches = new HashMap<>();
		Map<Integer, List<IMatchedBranch>> fallbackBranches = new HashMap<>();
		Map<Integer, List<IMatchedBranch>> skippedPartsBranches = new HashMap<>();
		
		for (SearchPath searchPath : searchPaths) {
			IMatchedWaySegment lastSegment = searchPath.getBranch().getMatchedWaySegments().get(
					searchPath.getBranch().getMatchedWaySegments().size() - 1);
			
			log.debug("search route from segment " + lastSegment.getId());
						
			if (matchingTask.getSegmentMatcher().matchedAllTrackPoints(lastSegment.getEndPointIndex(), track)) {
				// do not run shortest path search when all points are matched
				IMatchedBranch branchClone =  matchingTask.getSegmentMatcher().getClonedBranch(searchPath.getBranch());
				branchClone.setFinished(true);
				newBranches.add(branchClone);
				
				continue;
			}
			
			List<AlternativePath> fallbackRoutes = new LinkedList<AlternativePath>();
			List<AlternativePath> alternativePaths = new LinkedList<AlternativePath>();
			List<AlternativePath> skippedPaths = new ArrayList<AlternativePath>();
			
			long startTime = System.currentTimeMillis();
			int newSearchIndex = matchingTask.getRoutingMatcher().routeToNextPoint(
					searchPath.getBranch(), lastSegment, searchPath.getSearchIndex(),
					alternativePaths, newBranches, skippedPaths, fallbackRoutes, track);
			long endTime = System.currentTimeMillis();
			
			// store the branch with the next point to process, so that we can try again
			// if no valid continuation can be found
			newSearchPaths.add(new SearchPath(newSearchIndex, searchPath.getBranch()));
		
			matchingTask.checkCancelStatus();
//			matchingTask.statistics.incrementValue(MapMatcherStatistics.SHORTEST_PATH_SEARCH);
			matchingTask.statistics.incrementValue(MapMatcherStatistics.SHORTEST_PATH_SEARCH_TOTAL_DURATION, (int)(endTime - startTime));
			
			List<IMatchedBranch> possibleBranches = new ArrayList<>();

			searchIndexes.add(searchPath.getSearchIndex());
			
			// check found routes for u-turns and save new branches
			boolean addedPath = false;
			int pathType = -2; // none
			if (alternativePaths != null && !alternativePaths.isEmpty()) {
				pathType = 0; // alternative path
				for (AlternativePath alternativePath : alternativePaths) {
					addedPath = checkAndPrepareShortestPath(alternativePath, track,
							possibleBranches, searchPath.getBranch(), searchPath.getSearchIndex()) 
							|| addedPath;
				}
			} else if (skippedPaths != null && !skippedPaths.isEmpty()) {
				pathType = -1; // skipped path
				for (AlternativePath alternativePath : skippedPaths) {
					addedPath = checkAndPrepareShortestPath(alternativePath, track,
							possibleBranches, searchPath.getBranch(), alternativePath.getSegments().get(0).getStartPointIndex()) 
							|| addedPath;
				}
			}
			
			if (!addedPath) {
				pathType = 1; // fallback path
				// if the branch has not been extended, try to extend the branch with a fallback route,
				// which skips the part between the last segment and the target segment
				Map<IMatchedWaySegment, Boolean> targetSegmentChecks = new HashMap<>();
				for (AlternativePath fallbackRoute : fallbackRoutes) {
					// check if target segments of fallback routes are already part of valid routes; if not => add to newBranches
					IMatchedWaySegment targetSegment = fallbackRoute.getSegments().get(0);
					
					Boolean valid = targetSegmentChecks.get(targetSegment);
					if (valid == null) {
						if (searchPath.getBranch().getMatchedWaySegments().size() > 2) {
							valid = isFallbackRouteValid(searchPath.getBranch().getMatchedWaySegments().get(searchPath.getBranch().getMatchedWaySegments().size()-2), // prüfe vorletztes Segment
														 targetSegment,
														 possibleBranches);
						} else {
							valid = true;
						}
						targetSegmentChecks.put(targetSegment, valid);
					}
					
					if (valid) {
						checkAndPrepareShortestPath(fallbackRoute, track,
								possibleBranches, searchPath.getBranch(), searchPath.getSearchIndex());
					}
				}
			}
			
			switch (pathType) {
			case -1:
				if (!skippedPartsBranches.containsKey(searchPath.getSearchIndex())) {
					skippedPartsBranches.put(searchPath.getSearchIndex(), new ArrayList<>());
				}
				skippedPartsBranches.get(searchPath.getSearchIndex()).addAll(possibleBranches);
				break;

			case 0:
				if (!alternativeBranches.containsKey(searchPath.getSearchIndex())) {
					alternativeBranches.put(searchPath.getSearchIndex(), new ArrayList<>());
				}
				alternativeBranches.get(searchPath.getSearchIndex()).addAll(possibleBranches);
				break;

			case 1:
				if (!fallbackBranches.containsKey(searchPath.getSearchIndex())) {
					fallbackBranches.put(searchPath.getSearchIndex(), new ArrayList<>());
				}
				fallbackBranches.get(searchPath.getSearchIndex()).addAll(possibleBranches);
				break;

			default:
				break;
			}
		}
		
		if (!alternativeBranches.isEmpty()) {
			for (Integer searchIndex : searchIndexes) {
				if (alternativeBranches.containsKey(searchIndex)) {
					newBranches.addAll(alternativeBranches.get(searchIndex));
				}
			}
		} else {
			for (Integer searchIndex : searchIndexes) {
				if (skippedPartsBranches.containsKey(searchIndex)) {
					newBranches.addAll(skippedPartsBranches.get(searchIndex));
				} else if (fallbackBranches.containsKey(searchIndex)) {
					newBranches.addAll(fallbackBranches.get(searchIndex));
				}
			}
		}
		
		if (newBranches.isEmpty()) {
			// no continuation found for none of the paths, try again with the next points
			searchAlternativePaths2(newSearchPaths, newBranches, track);
		}
	}
	
	/**
	 * @param fallbackRoute
	 * @return
	 */
	private boolean isFallbackRouteValid(IMatchedWaySegment fromSegment, IMatchedWaySegment toSegment, List<IMatchedBranch> newBranches) {
		for (IMatchedBranch currentBranch : newBranches) {
			boolean foundToSegment = false;
			for (int i = currentBranch.getMatchedWaySegments().size()-1; i > 0; i--) {
				if (!foundToSegment) {
					if (currentBranch.getMatchedWaySegments().get(i).getId() == toSegment.getId()) {
						foundToSegment = true;
					}
				} else {
					if (currentBranch.getMatchedWaySegments().get(i).getId() == fromSegment.getId()) {
						return false;
					}
				}
			}
		}
		
		return true;
	}

	/**
	 * Checks if the found segments create an u-turn and then
	 * adds the new segments to the existing path.
	 * 
	 * @return True, if the path was expanded
	 */
	boolean checkAndPrepareShortestPath(AlternativePath alternativePath, ITrack track,
			List<IMatchedBranch> newBranches, IMatchedBranch branch, int startPointIndex) {
		List<IMatchedWaySegment> routedSegments = alternativePath.getSegments();
			
		if (branch.getMatchedWaySegments().size() > 1 &&
			branch.getMatchedWaySegments().get(branch.getMatchedWaySegments().size() - 2).isStartSegment() && 
			branch.getMatchedWaySegments().get(branch.getMatchedWaySegments().size() - 2).getId() == 
				routedSegments.get(0).getId()) {
			// the existing path only consists of two segments and the first segment of the found shortest path
			// is the start segment again (-> U-Turn)
			return false;
		} else {
			IMatchedBranch clonedBranch =  matchingTask.getSegmentMatcher().getClonedBranch(branch);

			// set a flag if there is an u-turn on the last segment (the start segment of the shortest path search),
			// because the segment might get removed if there are no matches for the segment
			checkUTurnForLastSegment(routedSegments, alternativePath.isCausesUTurn(), clonedBranch);

			if (!clonedBranch.getMatchedWaySegments().isEmpty()) {
				// add segments of shortest path search to branch
				clonedBranch = addSegmentsToClonedBranch(
						startPointIndex, 
						routedSegments, track, clonedBranch);
				
				if (clonedBranch != null) {
					clonedBranch.incrementNrOfShortestPathSearches();
					
					if (alternativePath.isAfterSkippedPart()) {
						newBranches.add(clonedBranch);
					} else {
						matchRoutingPath(clonedBranch, newBranches, track);
					}
					
					return true;
				}
			}
		}
		
		return false;
	}

	/**
	 * Checks if there is an u-turn on the last matching segment before the alternative
	 * path segments.
	 * 
	 * First the last matching segment is searched, skipping empty segments.
	 * If the last matching segment was entered through the start node and the alternative path
	 * starts again from this node, then there must be an u-turn on the last matching segment
	 * (same for the end node). In case of an u-turn, a flag is set because the segment might get removed
	 * if there are no matches for the segment. Also the direction for the u-turn segment is adapted.
	 * 
	 * Empty segments after the last matching segment are removed from the path, if there is an u-turn.
	 */
	protected void checkUTurnForLastSegment(List<IMatchedWaySegment> newSegments, boolean uTurnOnLastSegment, IMatchedBranch clonedBranch) {
		IMatchedWaySegment firstNewSegment = newSegments.get(0);
		
		if (firstNewSegment.isAfterSkippedPart()) {
			// part was skipped, no u-turn
			return;
		}
		
		List<IMatchedWaySegment> segments = clonedBranch.getMatchedWaySegments();
		IMatchedWaySegment lastSegment = segments.get(segments.size() - 1);
		
		if (uTurnOnLastSegment) {
			// an u-turn was already detected on the last segment
			lastSegment.setUTurnSegment(true);
			if (lastSegment.getDirection().isEnteringThroughStartNode()) {
				lastSegment.setDirection(Direction.START_TO_START);
			} else {
				lastSegment.setDirection(Direction.END_TO_END);
			}
		}
		
		int emptySegmentsAtEnd = 0;
		boolean foundUTurnSegment = false;
		for (int i = segments.size() - 1; i >= 0; i--) {
			IMatchedWaySegment segment = segments.get(i);
			
			if (segment.getMatchedPoints() <= 0) {
				emptySegmentsAtEnd++;
				
				if (uTurnOnLastSegment) {
					foundUTurnSegment = true;
				}
			} else {
				// found the last matching segment, now check for an u-turn
				if (segment.getId() == firstNewSegment.getId()) {
					/* the last matching segment is the same as the first new segment, in this
					 * case also always remove all empty segments at the end
					 */
					foundUTurnSegment = true;
					
					if ((segment.getDirection().isEnteringThroughStartNode() && firstNewSegment.getDirection().isLeavingThroughStartNode()) ||
						(segment.getDirection().isEnteringThroughEndNode() && firstNewSegment.getDirection().isLeavingThroughEndNode())) {
						segment.setUTurnSegment(true);
						foundUTurnSegment = true;
						// the direction is not yet changed
					}
				} else if (segment.getDirection().isEnteringThroughStartNode() &&
							(segment.getStartNodeId() == firstNewSegment.getStartNodeId() || 
							 segment.getStartNodeId() == firstNewSegment.getEndNodeId())) {
					// u-turn: segments are connected through the start node of the last segment
					segment.setUTurnSegment(true);
					segment.setDirection(Direction.START_TO_START);
					foundUTurnSegment = true;
				} else if (segment.getDirection().isEnteringThroughEndNode() &&
			 				(segment.getEndNodeId() == firstNewSegment.getStartNodeId() || 
			 				 segment.getEndNodeId() == firstNewSegment.getEndNodeId())) {
					// u-turn: segments are connected through the end node of the last segment
					segment.setUTurnSegment(true);
					segment.setDirection(Direction.END_TO_END);
					foundUTurnSegment = true;
//				} else if (emptySegmentsAtEnd == 1 &&
//						 ((lastSegment.getDirection().isEnteringThroughStartNode() && firstNewSegment.getDirection().isLeavingThroughStartNode()) ||
// 						  (lastSegment.getDirection().isEnteringThroughEndNode() && firstNewSegment.getDirection().isLeavingThroughEndNode()))) {
//					foundUTurnSegment = true;
				} else {
					foundUTurnSegment = foundUTurnSegment || false;
				}
				
				break;
			}
		}
		
		if (foundUTurnSegment) {
			// remove empty segments after u-turn
			for (int i = 0; i < emptySegmentsAtEnd; i++) {
				clonedBranch.removeLastMatchedWaySegment();
			}
		}
	}

	/**
	 * Adds segments obtained through routing to an existing branch and also matches track
	 * points to the segments.
	 * 
	 * @return Returns null, if no valid branch could be created.
	 */
	IMatchedBranch addSegmentsToClonedBranch(int startPointIndex, List<IMatchedWaySegment> segments, ITrack track, 
														 IMatchedBranch clonedBranch) {
		final int previousEndPointIndex = startPointIndex;
		final int routeEndPointIndex = segments.get(segments.size()-1).getStartPointIndex();
		int segmentsStartPointIndex = routeEndPointIndex;
		IMatchedWaySegment previousSegment = clonedBranch.getMatchedWaySegments().get(clonedBranch.getMatchedWaySegments().size() - 1);
		IMatchedWaySegment routeStartSegment = previousSegment;
		
		boolean isPathSkipped = partWasSkipped(segments);
		
		if (isPathSkipped) {
			// remove empty segments before skipped parts
			previousSegment = removeLastEmptySegments(clonedBranch);
			// remove last segment if this is unused; this means in some cases the next to last segment connects the new 
			// segment found by shortest path search
			// (=> z.B. Autobahnabfahrten bilden das letzte Segment, das neu ermittelte Segment schließt am vorletzten
			//     Segment am Beginn der Autobahnabfahrt an)
			previousSegment = removeLastUnusedSegment(clonedBranch, segments.get(0));
		}
		
		if (clonedBranch.getMatchedWaySegments().isEmpty()) {
			return null;
		}
		
		boolean segmentValid = true;
		if (segments.isEmpty()) {
			segmentValid = false;
		} else {
			for (IMatchedWaySegment matchedWaySegment : segments) {
				if (previousSegment != null) {
					segmentValid = matchingTask.getSegmentMatcher().checkIfMatchedSegmentIsValid(clonedBranch, matchedWaySegment);
					if (!segmentValid) {
						break;
					}
		
					// maybe matched points of previous segment match better to current segment
					segmentsStartPointIndex = matchShortestPathSegment(matchedWaySegment,
							segmentsStartPointIndex, track,
							clonedBranch, true);
				} else {
					log.debug("previous segment is null");
				}
				
				previousSegment = matchedWaySegment;
			}
		}
		
		if (!isPathSkipped) {
			if (startPointIndex < previousEndPointIndex ||
				routeEndPointIndex != segments.get(segments.size()-1).getStartPointIndex()) {
				// if we have a routed path without skipped parts (e.g. track left graph and returns...) AND there is a gap between indexes (could occur if
				// some track points we want to route to have GPS errors and couldn't be matched on any segment) we have to recalculate indexes of routed segments
				this.matchingTask.getSegmentMatcher().recalculateSegmentsIndexes(track, startPointIndex, routeEndPointIndex, segments);
			}
		}
		
		// This code is buggy: checks also segments found NOT by routing
		// FIXME
//		// Check speed between track points of routed part
//		List<IMatchedWaySegment> segmentsToCheck = new ArrayList<IMatchedWaySegment>();
//		// TODO maybe a greater start value can be used for the loop (e.g. route start segment)
//		for (int i = 0; i < clonedBranch.getMatchedWaySegments().size(); i++) {
//			IMatchedWaySegment segment = clonedBranch.getMatchedWaySegments().get(i);
//			if (segmentsToCheck.size() > 0 || segment.getEndPointIndex() - segment.getStartPointIndex() > 0) {
//				segmentsToCheck.add(segment);
//			}
//			if (segmentsToCheck.size() >= 2 && segment.getStartPointIndex() < segment.getEndPointIndex()) {
//				boolean possibleRoute = this.matchingTask.getRoutingMatcher()
//						.checkImpossibleRoute(segmentsToCheck, track, segmentsToCheck.get(0).getStartPointIndex(), segment.getStartPointIndex());
//				if (!possibleRoute) {
//					return null;
//				}
//				segmentsToCheck.clear();
//				segmentsToCheck.add(segment);
//			}
//		}
		
		/* Number of last parts that are checked: every new segment that matches a point creates
		 * two parts + a buffer.
		 */
		int lastPartsToCheck = 2 * segments.size() + 4;
		
		if (segmentValid && matchingTask.getWeightingStrategy().branchIsValid(clonedBranch, lastPartsToCheck)) {
			if (!clonedBranch.getMatchedWaySegments().isEmpty()) {
				IMatchedWaySegment lastSegment = clonedBranch.getMatchedWaySegments().get(clonedBranch.getMatchedWaySegments().size() - 1);
				
				if (lastSegment.getEndPointIndex() > previousEndPointIndex) {
					/* if the new end point index of the path is lower than the previous end point index
					 * (probably caused by the rematching), do not accept the path
					 */
					return clonedBranch;
				}
			}
		}
		
		return null;
	}
	
	/**
	 * Updates the matched points of the previous segments and sets the matches
	 * for the current.
	 * Returns the index of the end point of {@code matchedWaySegment}.
	 */
	int matchShortestPathSegment(
			IMatchedWaySegment matchedWaySegment, int startPointIndex,
			ITrack track, IMatchedBranch clonedBranch, boolean uturnMode) {
		
		IMatchedWaySegment previousSegment = clonedBranch.getMatchedWaySegments().get(clonedBranch.getMatchedWaySegments().size() - 1);
		
		int newStartIndex = matchingTask.getSegmentMatcher().getPossibleLowerStartIndex(previousSegment, 
				matchedWaySegment, track, startPointIndex, uturnMode);
		
		// update matched points of previous segment
		if (newStartIndex <= previousSegment.getEndPointIndex()) {
			// some points of the previous segment better belong to the new one
			if (newStartIndex <= previousSegment.getStartPointIndex()) {
				// all points of the previous segment are better matched to the new segment
				
				if (previousSegment.getId() == matchedWaySegment.getId()) {
					/* if the last segment is the same as the first new segment, remove the old
					 * segment so that it is only once in the path. if there is an u-turn,
					 * change the direction
					 */
					if (previousSegment.isUTurnSegment()) {
						matchedWaySegment.setUTurnSegment(true);
						if (matchedWaySegment.getDirection().isLeavingThroughStartNode()) {
							matchedWaySegment.setDirection(Direction.START_TO_START);
						} else {
							matchedWaySegment.setDirection(Direction.END_TO_END);
						}
					}
					
					clonedBranch.removeLastMatchedWaySegment();
					previousSegment = null;
					
				} else if (previousSegment.isUTurnSegment()) {
					// if there is an u-turn on the previous segment and all previously matched points
					// can be rematched to the new segment, remove the previous segment from the path
					if (previousSegment.isAfterSkippedPart()) {
						// if the last segment marked a skipped part, set the flag on the new segment
						matchedWaySegment.setAfterSkippedPart(true);
					}
					
					clonedBranch.removeLastMatchedWaySegment();
					previousSegment = null;
					
					// calculate possible start index again because the previous segment has been removed
					if (!clonedBranch.getMatchedWaySegments().isEmpty()) {
						previousSegment = clonedBranch.getMatchedWaySegments().get(clonedBranch.getMatchedWaySegments().size() - 1);
						// previousSegment.setFromPathSearch(true); // TODO ???
						newStartIndex = matchingTask.getSegmentMatcher().getPossibleLowerStartIndex(previousSegment, 
								matchedWaySegment, track, startPointIndex, uturnMode);
						if (previousSegment.getEndPointIndex() > newStartIndex) {
							removeDiffDistances(previousSegment, newStartIndex);
						}
						previousSegment.calculateDistances(track);
//					} else {
//						newStartIndex = 0;
					}
					
				} else {
					previousSegment.setEndPointIndex(previousSegment.getStartPointIndex());
					previousSegment.calculateDistances(track);
				}
			} else {
				// not all points of the previous segment are rematched to the new segment
				
				if (uturnMode && previousSegment.isUTurnSegment()) {
					/**
					 * If 'uturnMode' is true, all points of the previous segment that are within the 
					 * matching radius of the new segment are matched to the new segment to filter 'blind'
					 * u-turn segments. But when not all points of the previous segment are matched to the
					 * new segment, some points might have been matched to the new one even though they are
					 * closer to the previous segment. In this case, we re-check the matching but this time
					 * with 'uturnMode' set to false, so that all points are matched to the closest segment. 
					 */
					return matchShortestPathSegment(matchedWaySegment, 
							startPointIndex, 
							track, clonedBranch, false);
				} else {
					if (previousSegment.getId() == matchedWaySegment.getId()) {
						/* if the last segment is the same as the first new segment, and no further points
						 * can be matched with the new segment (because points might get skipped), only
						 * keep one segment. if further points were matched, keep both segments and
						 * split the path.
						 */
						if (previousSegment.getEndPointIndex() == newStartIndex) {
							clonedBranch.removeLastMatchedWaySegment();
							previousSegment = null;
						} else {
							matchedWaySegment.setAfterSkippedPart(true);
							
							if (previousSegment.isUTurnSegment()) {
								previousSegment.setUTurnSegment(false);
							}
						}
					}
					
					if (previousSegment != null) {
						removeDiffDistances(previousSegment, newStartIndex);
					}
				}
			}
		}

		// set matched points + distances for new segment
		matchedWaySegment.setStartPointIndex(newStartIndex);
		int endPointIndex =  matchingTask.getSegmentMatcher().getLastPointIndex(matchedWaySegment, newStartIndex, track);
		
		if (endPointIndex > newStartIndex) {
			// at least one point matches
			if (previousSegment != null && !previousSegment.isAfterSkippedPart() 
					&& matchingTask.getSegmentMatcher().emptySegmentsAtEndOfBranch(clonedBranch)) {
				// if there are empty segments at the end of the branch, 
				// rematch all points starting from the last matching segment
				newStartIndex = matchingTask.getSegmentMatcher().updateMatchesOfPreviousEmptySegments(matchedWaySegment, 
						clonedBranch, track);
				// matchedWaySegment has wrong indexes
//				matchedWaySegment.setStartPointIndex(startPointIndex);

				//				matchedWaySegment.setEndPointIndex(endPointIndex);
//				List<Double> distances = matchingTask.getSegmentMatcher().getValidPointDistances(matchedWaySegment, newStartIndex, track, properties.getMaxMatchingRadiusMeter());
//				List<Double> distances = matchingTask.getSegmentMatcher().getValidPointDistances(matchedWaySegment, startPointIndex, track, properties.getMaxMatchingRadiusMeter());
				
//				endPointIndex = newStartIndex + distances.size();
			}
		}
		
		if (previousSegment != null && newStartIndex > previousSegment.getEndPointIndex() &&
			matchedWaySegment.getId() == previousSegment.getId()) {
			// same segment identified, but new start index is greater than old end index => some points in between could have not been matched
			// => update previous segment and ignore matched segment
			previousSegment.setEndPointIndex(endPointIndex);
			previousSegment.calculateDistances(track);
			previousSegment.setUTurnSegment(previousSegment.isUTurnSegment() || matchedWaySegment.isUTurnSegment());
			previousSegment.setFromPathSearch(previousSegment.isFromPathSearch() || matchedWaySegment.isFromPathSearch());
			// modify direction after merging previousSegment and matchedWaySegment
			if (previousSegment.getDirection().isEnteringThroughStartNode()) {
				if (matchedWaySegment.getDirection().isLeavingThroughStartNode()) {
					previousSegment.setDirection(Direction.START_TO_START);
				} else if (matchedWaySegment.getDirection().isLeavingThroughEndNode()) {
					previousSegment.setDirection(Direction.START_TO_END);
				}
			} else if (previousSegment.getDirection().isEnteringThroughEndNode()) {
				if (matchedWaySegment.getDirection().isLeavingThroughStartNode()) {
					previousSegment.setDirection(Direction.END_TO_START);
				} else if (matchedWaySegment.getDirection().isLeavingThroughEndNode()) {
					previousSegment.setDirection(Direction.END_TO_END);
				}
			}
		} else {
			
			matchedWaySegment.setEndPointIndex(endPointIndex);
			
			// calculate distances for matched points
			matchedWaySegment.calculateDistances(track);
			
			clonedBranch.addMatchedWaySegment(matchedWaySegment);
		}
		
		return endPointIndex;
	}

	private void removeDiffDistances(IMatchedWaySegment previousSegment, int newStartIndex) {
		int diff = previousSegment.getEndPointIndex() - newStartIndex;
		previousSegment.setEndPointIndex(previousSegment.getEndPointIndex() - diff);
		// remove distances of the points now matched to the new segment
		for (int j=0; j<diff; j++) {
			previousSegment.removeLastDistance();
		}
	}


	/**
	 * If the new segments were found by shortest-path-search and not by skipping parts, 
	 * match the next segment so that paths from skipped-parts and from routing both
	 * start the next iteration from the same segment. Otherwise paths from
	 * skipped-path might haven an advantage, because they always match the next segment
	 * so that the direction can be set for the segment after the skipped part.
	 * 
	 * @param routingBranch The path which has been expanded by routing.
	 * @param newBranches The list in which new paths will be stored.
	 * @param track
	 */
	private void matchRoutingPath(IMatchedBranch routingBranch, List<IMatchedBranch> newBranches, ITrack track) {
		newBranches.add(routingBranch);
	}

	private static boolean partWasSkipped(List<IMatchedWaySegment> segments) {
		IMatchedWaySegment firstNewSegment = segments.get(0);
		
		return firstNewSegment.isAfterSkippedPart();
	}

	private IMatchedWaySegment removeLastEmptySegments(
			IMatchedBranch clonedBranch) {
		this.matchingTask.removeEmptySegmentsAtEnd(clonedBranch);
		IMatchedWaySegment newLastSegment = null;
		if (!clonedBranch.getMatchedWaySegments().isEmpty()) {
			newLastSegment = clonedBranch.getMatchedWaySegments().get(
					clonedBranch.getMatchedWaySegments().size() - 1);
		}
		
		return newLastSegment;
	}
	
	private IMatchedWaySegment removeLastUnusedSegment(
			IMatchedBranch clonedBranch, IMatchedWaySegment firstAlternativeSegment) {
		if (clonedBranch.getMatchedWaySegments().size() > 1) {
			IMatchedWaySegment lastSegment = clonedBranch.getMatchedWaySegments().get(
					clonedBranch.getMatchedWaySegments().size() - 1);
			IMatchedWaySegment nextToLastSegment = clonedBranch.getMatchedWaySegments().get(
					clonedBranch.getMatchedWaySegments().size() - 2);
			
			long nodeId = 0;
			if (lastSegment.getEndNodeId() == nextToLastSegment.getEndNodeId() || 
				lastSegment.getEndNodeId() == nextToLastSegment.getStartNodeId()) {
				nodeId = lastSegment.getEndNodeId();
			} else if (lastSegment.getStartNodeId() == nextToLastSegment.getEndNodeId() || 
					   lastSegment.getStartNodeId() == nextToLastSegment.getStartNodeId()) {
				nodeId = lastSegment.getStartNodeId();
			}
			
			if (nodeId > 0 &&
				(firstAlternativeSegment.getEndNodeId() == nodeId ||
				 firstAlternativeSegment.getStartNodeId() == nodeId)) {
				clonedBranch.removeLastMatchedWaySegment();
			}
		}
		
		IMatchedWaySegment newLastSegment = null;
		if (!clonedBranch.getMatchedWaySegments().isEmpty()) {
			newLastSegment = clonedBranch.getMatchedWaySegments().get(
					clonedBranch.getMatchedWaySegments().size() - 1);
		}
		
		return newLastSegment;
	}

	/**
	 * Container class for paths the alternative path search will be
	 * applied to. The {@code searchIndex} is the point the search will
	 * start from.
	 */
	private class SearchPath {
		private final int searchIndex;
		private final IMatchedBranch branch;
		
		public SearchPath(int searchIndex, IMatchedBranch branch) {
			this.searchIndex = searchIndex;
			this.branch = branch;
		}

		public int getSearchIndex() {
			return searchIndex;
		}

		public IMatchedBranch getBranch() {
			return branch;
		}
	}
	
	/**
	 * Container class for segments obtained from alternative path search. A flag
	 * indicates whether the segments were found by shortest-path-search or by
	 * skipping parts.
	 */
	protected static class AlternativePath {
		private final boolean afterSkippedPart;
		private final List<IMatchedWaySegment> segments;
		
		/**
		 * Indicates if there is an u-turn on the first segment of the path.
		 */
		private final boolean causesUTurn;
		
		private AlternativePath(boolean afterSkippedPart, List<IMatchedWaySegment> segments, boolean causesUTurn) {
			this.afterSkippedPart = afterSkippedPart;
			this.segments = segments;
			this.causesUTurn = causesUTurn;
		}
		
		/**
		 * @see {@link AlternativePath#fromRouting(List)}
		 */
		public static AlternativePath fromRouting(List<IMatchedWaySegment> segments, boolean causesUTurn) {
			return new AlternativePath(false, segments, causesUTurn);
		}

		/**
		 * Creates a container for segments obtained from shortest-path-search.
		 */
		public static AlternativePath fromRouting(List<IMatchedWaySegment> segments) {
			return fromRouting(segments, false);
		}
	
		/**
		 * Creates a container for segments obtained from skipping parts.
		 */
		public static AlternativePath afterSkippedPart(List<IMatchedWaySegment> segments) {
			return new AlternativePath(true, segments, false);
		}

		public boolean isAfterSkippedPart() {
			return afterSkippedPart;
		}

		public List<IMatchedWaySegment> getSegments() {
			return segments;
		}
		
		public boolean isCausesUTurn() {
			return causesUTurn;
		}
	}
}
