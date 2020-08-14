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
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;

import at.srfg.graphium.mapmatching.model.IMatchedBranch;
import at.srfg.graphium.mapmatching.model.IMatchedWaySegment;
import at.srfg.graphium.mapmatching.model.ITrack;
import at.srfg.graphium.mapmatching.neo4j.matcher.impl.AlternativePathMatcher.AlternativePath;
import at.srfg.graphium.mapmatching.properties.IMapMatchingProperties;

public class MatchesFilter {
	
	private static Logger log = LoggerFactory.getLogger(MatchesFilter.class);
	
	private MapMatchingTask matchingTask;
	private AlternativePathMatcher alternativePathMatcher;
	private IMapMatchingProperties properties;
		
	public MatchesFilter(MapMatchingTask mapMatchingTask, AlternativePathMatcher alternativePathMatcher,
			IMapMatchingProperties properties) {
		this.matchingTask = mapMatchingTask;
		this.alternativePathMatcher = alternativePathMatcher;
		this.properties = properties;
	}
	
	/**
	 * Filters the found paths after every iteration:
	 * 
	 * 	- Keeps only the best path for each end segment.
	 * 	- Adds finished paths to the list of potential solutions.
	 * 	- Keeps only the best 'minNrOfBestPaths' active paths.
	 * 	- Expands slow paths, see {@link MatchesFilter#expandSlowPaths(List, List)}.
	 * 
	 */
	public List<IMatchedBranch> filterMatches(List<IMatchedBranch> paths, ITrack track,
			List<IMatchedBranch> finishedPaths,
			boolean hasCertainPath,
			IMapMatchingProperties properties) {
		
		updateStepInfo(paths);

		// filter not extended paths 
		paths = filterOnlyExtendedPaths(paths);
		
		// keep only one path per end segment
		paths = filterPaths(paths, hasCertainPath);
		
		// add finished tracks to the list of potential solutions for the current start segment (possiblePathsForStartSegment)
		if (paths != null && !paths.isEmpty()) {
			extractFinishedPaths(paths, finishedPaths);
		}
		
		if (paths != null && !paths.isEmpty()) {
			paths = filterBestPaths(
					paths, 
					properties.getMaxNrOfBestPaths());
			
			// try to expand all best paths to the end point index of the most advanced path 
			paths = expandSlowPaths(
					paths,
					finishedPaths,
					track,
					hasCertainPath);
		}
		
		return paths;
	}

	private void updateStepInfo(List<IMatchedBranch> paths) {
		for (IMatchedBranch path : paths) {
			path.incrementStep();
		}
	}

	private List<IMatchedBranch> filterOnlyExtendedPaths(List<IMatchedBranch> paths) {
		int maxStep = 0;
		int threshold = 2;
		int maxMatchedPoints = 0;
		
		for (IMatchedBranch path : paths) {
			maxStep = Math.max(maxStep, path.getStep());
			maxMatchedPoints = Math.max(maxMatchedPoints, path.getMatchedPoints());
		}
		
		List<IMatchedBranch> newPaths = new ArrayList<>();
		for (IMatchedBranch path : paths) {
			if ((path.getStep() + threshold) >= maxStep ||
				 path.getMatchedPoints() == maxMatchedPoints) {
				newPaths.add(path);
			}
		}
				
		return newPaths;
	}

	/**
	 * @param path
	 * @param newBranches
	 * @param finishedPaths
	 * @throws CloneNotSupportedException 
	 */
	private void mergeSegments(IMatchedBranch bestPath, IMatchedBranch pathToExpand, List<IMatchedBranch> newBranches,
			List<IMatchedBranch> finishedPaths) throws CloneNotSupportedException {
		
		newBranches.add(pathToExpand);
		
		if (pathToExpand.getMatchedWaySegments().size() < 2) {
			return;
		}
		
		// try to expand only paths without skipped parts
		for (IMatchedWaySegment segment : pathToExpand.getMatchedWaySegments()) {
			if (segment.isAfterSkippedPart()) {
				return;
			}
		}
		
		IMatchedWaySegment lastSegment = pathToExpand.getMatchedWaySegments().get(pathToExpand.getMatchedWaySegments().size()-1);
		IMatchedWaySegment nextToLastSegment = pathToExpand.getMatchedWaySegments().get(pathToExpand.getMatchedWaySegments().size()-2);
		Iterator<IMatchedWaySegment> itBestPath = bestPath.getMatchedWaySegments().iterator();

		boolean search = true;
		IMatchedWaySegment matchingSegmentOfBestPath1 = null;
		IMatchedWaySegment matchingSegmentOfBestPath2 = null;
		List<IMatchedWaySegment> segmentsToMerge = new ArrayList<>();
		
		while (search && itBestPath.hasNext()) {
			IMatchedWaySegment currentSegment = itBestPath.next();
			if (matchingSegmentOfBestPath1 == null) {
				// check identity of next to last segment of path to expand
				if (currentSegment.getId() == nextToLastSegment.getId()) {
					matchingSegmentOfBestPath1 = currentSegment;
					// check identity of last segment of path to expand
					if (itBestPath.hasNext()) {
						currentSegment = itBestPath.next();
						if (currentSegment.getId() == lastSegment.getId()) {
							matchingSegmentOfBestPath2 = currentSegment;
						} else {
							// next to last segment is equal to an segment found in best path, but last segment does not match the next 
							// segment in best path => break
							search = false;
						}
					}
				}
			} else {
				if (matchingSegmentOfBestPath2.getEndPointIndex() == currentSegment.getStartPointIndex() &&
						currentSegment.getStartPointIndex() >= lastSegment.getEndPointIndex()) {
					IMatchedWaySegment clonedSegment = (IMatchedWaySegment) currentSegment.clone();

					if (matchingSegmentOfBestPath2.getId() == lastSegment.getId()) {
						// rematch the last segment of path to expand
						matchingTask.getSegmentMatcher().updateMatchesOfPreviousSegment(lastSegment.getEndPointIndex(), 
							lastSegment, clonedSegment, matchingTask.getTrack());
					}
					
					segmentsToMerge.add(clonedSegment);
					matchingSegmentOfBestPath2 = currentSegment;
				} else {
					search = false;
				}
			}
		}
		
		if (!segmentsToMerge.isEmpty()) {
			IMatchedBranch expandedPath = (IMatchedBranch) pathToExpand.clone();
			for (IMatchedWaySegment seg : segmentsToMerge) {
				expandedPath.addMatchedWaySegment(seg);
			}
			
			if (expandedPath.getMatchedFactor() <= bestPath.getMatchedFactor()) {
				newBranches.add(expandedPath);
			}
		}
		
	}

	
	/**
	 * Tries to expand all best paths of {@code tempPathsForStartSegments} to the end point index
	 * of the most advanced path. 
	 * 
	 * The motivation for this is, that some paths take shortcuts, so that
	 * they are able to match more points of the track. While at the same time, paths that take the 'right route'
	 * with more segments (slower paths) have less match points. By the time, these better but slower paths might
	 * get ruled out by the worser but faster paths, so that worser paths could be preferred.
	 * 
	 */
	@VisibleForTesting
	protected List<IMatchedBranch> expandSlowPaths(List<IMatchedBranch> paths,
			List<IMatchedBranch> finishedPaths, ITrack track, boolean hasCertainPath) {
		if (paths.isEmpty()) {
			return Collections.emptyList();
		}
		if (paths.size() == 1) {
			return paths;
		}
		
		List<IMatchedBranch> newBranches = new ArrayList<IMatchedBranch>();
		
		// do not expand the most advanced path
		newBranches.add(paths.get(0));

		int maxEndPointIndex = getEndPointIndex(paths.get(0));
		for (int i = 1; i < paths.size(); i++) {
			// but try to expand all other paths
			IMatchedBranch path = paths.get(i);
			boolean expanded = expandPathToPoint(path, maxEndPointIndex, newBranches, finishedPaths, track);
			if (!expanded) {
				try {
					mergeSegments(paths.get(0), path, newBranches, finishedPaths);
				} catch (CloneNotSupportedException e) {
					log.error("Could not expand path", e);
				};
			}
		}
		
		// special treatment of best path if it turns to lower lever roads and returns back to higher level roads
		// look for routing path to avoid lower lever roads
		addHigherLevelSegmentsAtBypass(paths.get(0), newBranches, track);

//		for (IMatchedBranch path : paths) {
//			int prevIdx = 0;
//			for (IMatchedWaySegment seg : path.getMatchedWaySegments()) {
//				if (seg.getStartPointIndex() < prevIdx) {
//					log.debug("////////// (2) Index error at segment " + seg.getId() + ": " + seg.getStartPointIndex() + " < " + prevIdx);
//					
//					log.warn("Found indexing error of merged paths - return original paths");
//					return paths;
//					
//				}
//				prevIdx = seg.getEndPointIndex();
//			}	
//		}

		newBranches = filterPaths(newBranches, hasCertainPath);
		
		return filterBestPaths(
				newBranches, 
				properties.getMaxNrOfBestPaths());
	}

	/**
	 * @param iMatchedBranch
	 * @param newBranches
	 */
	private void addHigherLevelSegmentsAtBypass(IMatchedBranch path, List<IMatchedBranch> newBranches, ITrack track) {
		for (int checkSegmentIndex=0; checkSegmentIndex <=1; checkSegmentIndex++) {
			if (path.getMatchedWaySegments().size() >= (3 + checkSegmentIndex)) {
				IMatchedWaySegment lastSegment = path.getMatchedWaySegments().get(path.getMatchedWaySegments().size()-(1+checkSegmentIndex));
				short frcLastSegment = lastSegment.getFrc().getValue();
				short frcNextToLastSegment = path.getMatchedWaySegments().get(path.getMatchedWaySegments().size()-(2+checkSegmentIndex)).getFrc().getValue();
				if (frcLastSegment == frcNextToLastSegment) {
					return; // no possible bypass found
				}
				
				short frcCurrentSegment = -1;
				//for (int i=path.getMatchedWaySegments().size()-2; i>=0; i--) {
				int i = checkSegmentIndex + 1;
				do {
					i++;
					frcCurrentSegment = path.getMatchedWaySegments().get(path.getMatchedWaySegments().size()-(1+i)).getFrc().getValue();
				} while (frcNextToLastSegment == frcCurrentSegment && i < (5+checkSegmentIndex) && i > (path.getMatchedWaySegments().size()-1));
				
				if (i > 1 && frcLastSegment == frcCurrentSegment) {
					// bypass found => check route
					IMatchedWaySegment startSegmentForRouting = path.getMatchedWaySegments().get(path.getMatchedWaySegments().size()-(1+i));
					
					if (startSegmentForRouting.getEndPointIndex() < track.getTrackPoints().size()) {
						List<AlternativePath> fallbackRoutes = new LinkedList<AlternativePath>();
						List<AlternativePath> alternativePaths = new LinkedList<AlternativePath>();
						List<AlternativePath> skippedPaths = new ArrayList<AlternativePath>();
						matchingTask.getRoutingMatcher().routeToNextPoint(
								path, startSegmentForRouting, startSegmentForRouting.getEndPointIndex(),
								alternativePaths, newBranches, skippedPaths, fallbackRoutes, track);
						
						if (alternativePaths != null && !alternativePaths.isEmpty()) {
							List<IMatchedWaySegment> routedSegments = null;
							for (AlternativePath altPath : alternativePaths) {
								if (altPath.getSegments().get(altPath.getSegments().size()-1).getId() == lastSegment.getId()) {
									routedSegments = altPath.getSegments();
									break;
								}
							}
							
							if (routedSegments != null) {
								IMatchedBranch clonedBranch =  matchingTask.getSegmentMatcher().getClonedBranch(path);
								
								// remove last segments
								for (int j=1; j<=i; j++) {
									clonedBranch.getMatchedWaySegments().remove(clonedBranch.getMatchedWaySegments().size()-1);
								}
								
								// add segments of shortest path search to branch
								clonedBranch = alternativePathMatcher.addSegmentsToClonedBranch(
										routedSegments.get(0).getStartPointIndex(), 
										routedSegments, track, clonedBranch);
								if (clonedBranch != null) {
									newBranches.add(clonedBranch);
								}
							}
							return;
						}
					}
				}
			}
		}
	}

	/**
	 * Tries to expand a path to the given end point.
	 * 
	 * @param path The path that should be expanded.
	 * @param maxEndPointIndex The target end point the path should be expanded to.
	 * @param newPaths New paths are added to this list.
	 * @param finishedPaths Finished paths.
	 */
	@VisibleForTesting
	protected boolean expandPathToPoint(IMatchedBranch path, int maxEndPointIndex,
			List<IMatchedBranch> newPaths, List<IMatchedBranch> finishedPaths, ITrack track) {
		boolean expanded = false;
		Map<Long, IMatchedBranch> bestPathPerLastSegment = new HashMap<Long, IMatchedBranch>();
		
		int currentEndPointIndex = getEndPointIndex(path);
		if (currentEndPointIndex >= maxEndPointIndex) {
			// this path is as far as the best path, no need to expand it
			newPaths.add(path);
			return true;
		}
		
		// keep the current path, in case it can not be expanded or the expanded paths are worse
		newPaths.add(path);

		List<IMatchedBranch> currentPaths = Collections.singletonList(path);
		while (currentEndPointIndex < maxEndPointIndex) {
			List<IMatchedBranch> expandedPaths = new ArrayList<IMatchedBranch>();
			
				List<IMatchedBranch> newBranchesForBranch = new ArrayList<IMatchedBranch>();
				List<IMatchedBranch> finishedBranches = new ArrayList<IMatchedBranch>();
				List<IMatchedBranch> unmanipulatedBranches = new ArrayList<IMatchedBranch>();
				
			boolean pathExpanded = matchingTask.getPathExpanderMatcher().processPath(
					path, matchingTask.getTrack(),
					newBranchesForBranch, finishedBranches, unmanipulatedBranches);
				
			if (!newBranchesForBranch.isEmpty()) {
				expandedPaths.addAll(newBranchesForBranch);
			}
			
			if (!finishedBranches.isEmpty()) {
				finishedPaths.addAll(finishedBranches);
			}

			List<IMatchedBranch> pathsWithNewLastSegment = getPathsWithNewLastSegment(expandedPaths, bestPathPerLastSegment);
			if (pathsWithNewLastSegment.isEmpty()) {
				// no path could be expanded to a new segment, stop
				break;
			} else {
				currentPaths = pathsWithNewLastSegment;
				currentEndPointIndex = getEndPointIndex(currentPaths);
				newPaths.addAll(currentPaths);
				expanded = true;
			}
		}
		
		return expanded;
	}

	/**
	 * From all newly found paths, selects only those paths that were expanded to a new segment. If there is
	 * already a path with the same end segment, the new path is only kept if its matched factor is better.
	 */
	private List<IMatchedBranch> getPathsWithNewLastSegment(List<IMatchedBranch> expandedPaths, Map<Long, IMatchedBranch> bestPathPerLastSegment) {
		if (expandedPaths.isEmpty()) {
			return Collections.emptyList();
		}
		
		List<IMatchedBranch> pathsWithNewLastSegment = new ArrayList<IMatchedBranch>();
		
		for (IMatchedBranch path : expandedPaths) {
			IMatchedWaySegment lastSegment = path.getMatchedWaySegments().get(path.getMatchedWaySegments().size() - 1);
			
			if (bestPathPerLastSegment.containsKey(lastSegment.getId())) {
				// there is already a path with the same end segment
				IMatchedBranch otherPath = bestPathPerLastSegment.get(lastSegment.getId());
				
				if (path.compareTo(otherPath) < 0) {
					// the new path is better
					bestPathPerLastSegment.put(lastSegment.getId(), path);
					pathsWithNewLastSegment.add(path);
				}
			} else {
				bestPathPerLastSegment.put(lastSegment.getId(), path);
				pathsWithNewLastSegment.add(path);
			}
		}
		
		return pathsWithNewLastSegment;
	}

	/**
	 * Returns the maximum end point index of the given paths.
	 */
	@VisibleForTesting
	protected int getEndPointIndex(List<IMatchedBranch> currentPaths) {
		return getEndPointIndex(Collections.max(currentPaths, new Comparator<IMatchedBranch>() {

			@Override
			public int compare(IMatchedBranch path1, IMatchedBranch path2) {
				Integer endPointIndex1 = getEndPointIndex(path1);
				Integer endPointIndex2 = getEndPointIndex(path2);
				
				return endPointIndex1.compareTo(endPointIndex2);
			}
		}));
	}
	
	/**
	 * Returns the end point index of the last segment of the given path.
	 */
	@VisibleForTesting
	protected int getEndPointIndex(IMatchedBranch path) {
		IMatchedWaySegment lastSegmentOfBestPath = path.getMatchedWaySegments().get(
				path.getMatchedWaySegments().size() - 1);
		
		return lastSegmentOfBestPath.getEndPointIndex();
	}

	private void extractFinishedPaths(
			List<IMatchedBranch> tempPathsForStartSegments,
			List<IMatchedBranch> possiblePathsForStartSegment) {
		List<IMatchedBranch> finishedPaths = new ArrayList<IMatchedBranch>();
		
		// first get all finished paths
		for (IMatchedBranch br : tempPathsForStartSegments) {
			if (br.isFinished()) {
				finishedPaths.add(br);
			}
		}
		
		// then remove finished paths from list of active path and add to list of potential solutions 
		for (IMatchedBranch br : finishedPaths) {
			possiblePathsForStartSegment.add(br);
			tempPathsForStartSegments.remove(br);
		}
		
		// now filter only best finished paths to avoid memory leak
		finishedPaths = filterBestPaths(
						finishedPaths, 
						properties.getMaxNrOfBestPaths());
	}

	/**
	 * Keep the best 'minNrOfBestPaths' paths as active paths.
	 */
	private List<IMatchedBranch> filterBestPaths(
			List<IMatchedBranch> tempPathsForStartSegment, 
			int minNrOfBestPaths) {
		List<IMatchedBranch> bestBranches = new ArrayList<IMatchedBranch>();
		Collections.sort(tempPathsForStartSegment);
		
		Iterator<IMatchedBranch> it = tempPathsForStartSegment.iterator();
		if (it.hasNext()) {
			// filter best results
			IMatchedBranch branch = it.next();
			bestBranches.add(branch);
			
			while (it.hasNext()) {
				branch = it.next();
				
				if (bestBranches.size() < minNrOfBestPaths) {
					bestBranches.add(branch);
					branch = null;
				} else {
					break;
				}
			}
			tempPathsForStartSegment = bestBranches;
		}
		
		return tempPathsForStartSegment;
	}

	/**
	 * Filters the list of potential solutions by keeping only the best path for each end segment of the paths.
	 * 
	 */
	List<IMatchedBranch> filterPaths(List<IMatchedBranch> paths, boolean hasCertainPath) {
		int maxStep = 0;

		for (IMatchedBranch path : paths) {
			maxStep = Math.max(maxStep, path.getStep());
		}

		// find parts with same segments and remove following segments,
		// only applicable at beginning of track when more than one initial segment can occur
		if (!hasCertainPath && paths.size() > 1) {
			List<IMatchedBranch> expandedPaths = new ArrayList<>();
			for (IMatchedBranch pathA : paths) {

				if (pathA.getStep() == maxStep) {

					IMatchedWaySegment lastSegmentOfPathA = pathA.getMatchedWaySegments().get(pathA.getMatchedWaySegments().size() - 1);

					for (IMatchedBranch pathB : paths) {
						int indexOfSegment = findIndexOfSegment(pathB, lastSegmentOfPathA);
						if (pathA != pathB
								&& pathB.getStep() == maxStep //paths should have equal number of steps
								&& pathB.getMatchedPoints() > pathA.getMatchedPoints() //other path should have more matched points
								&& pathA.getMatchedWaySegments().get(0).getId() != pathB.getMatchedWaySegments().get(0).getId() //different first segments
								&& indexOfSegment > 0 // exists in path and not the first element
								&& indexOfSegment < pathB.getMatchedWaySegments().size() - 1) { // not the last element
							expandedPaths.add(expandPath(pathA, pathB, indexOfSegment));
							if (pathA.getMatchedWaySegments().size() >= 2 &&
								pathB.getMatchedWaySegments().size() >= indexOfSegment) {
								if (pathA.getMatchedWaySegments().get(pathA.getMatchedWaySegments().size() - 2).getId() !=
										pathB.getMatchedWaySegments().get(indexOfSegment - 1).getId()) {
									log.warn("Different segments before equal segments in paths");
								}
							}
						}
					}
				}
			}
			paths.addAll(expandedPaths);
		}

		// first get the best path for each last segment of the paths	
 		Map<IMatchedWaySegment, IMatchedBranch> pathsPerEndSegment = new HashMap<IMatchedWaySegment, IMatchedBranch>();
		for (IMatchedBranch path : paths) {
			IMatchedWaySegment lastSegmentOfPath = path.getMatchedWaySegments().get(path.getMatchedWaySegments().size() - 1);
			
			if (!pathsPerEndSegment.containsKey(lastSegmentOfPath)) {
				pathsPerEndSegment.put(lastSegmentOfPath, path);
			} else {
				IMatchedBranch otherPath = pathsPerEndSegment.get(lastSegmentOfPath);
				
				if (path.compareTo(otherPath) < 0) {
					// the new path is better
					pathsPerEndSegment.put(lastSegmentOfPath, path);
				}
			}
		}
		
		// convert map values to list
		List<IMatchedBranch> singleSegmentbranches = new ArrayList<IMatchedBranch>();
		for (Map.Entry<IMatchedWaySegment, IMatchedBranch> entry : pathsPerEndSegment.entrySet()) {
			IMatchedBranch bestPathForEndSegment = entry.getValue();
			singleSegmentbranches.add(bestPathForEndSegment);
		}
		
		return singleSegmentbranches;
	}
	
	private IMatchedBranch expandPath(IMatchedBranch pathA, IMatchedBranch pathB, int indexOfSegment) {
		IMatchedBranch clonedBranch = matchingTask.getSegmentMatcher().getClonedBranch(pathA);
		if (clonedBranch == null) {
			return null;
		}
		int index = indexOfSegment + 1;
		IMatchedWaySegment lastSegPathA = clonedBranch.getMatchedWaySegments().get(clonedBranch.getMatchedWaySegments().size()-1);
		IMatchedWaySegment currentSegPathB = null;
		while (index < pathB.getMatchedWaySegments().size()) {
			currentSegPathB = pathB.getMatchedWaySegments().get(index);
			
			clonedBranch.getMatchedWaySegments().add(currentSegPathB);
			if (index == indexOfSegment + 1) {
				lastSegPathA.setEndPointIndex(pathB.getMatchedWaySegments().get(indexOfSegment).getEndPointIndex());
				if (lastSegPathA.getStartPointIndex() > lastSegPathA.getEndPointIndex()) {
					lastSegPathA.setStartPointIndex(pathB.getMatchedWaySegments().get(indexOfSegment).getStartPointIndex());
					matchingTask.getSegmentMatcher().updateMatchesOfPreviousSegment(lastSegPathA.getEndPointIndex(), 
							lastSegPathA, clonedBranch.getMatchedWaySegments().get(clonedBranch.getMatchedWaySegments().size()-2), matchingTask.getTrack());
				} else {
					lastSegPathA.calculateDistances(matchingTask.getTrack());
				}
			}
			
			lastSegPathA = currentSegPathB;
			index++;
		}
		return clonedBranch;
	}

	/**
	 * Determine index of segment within the path
	 * @param path
	 * @param matchedWaySegment
	 * @return
	 */
	private int findIndexOfSegment(IMatchedBranch path, IMatchedWaySegment matchedWaySegment) {
		for (int i=path.getMatchedWaySegments().size()-1; i>=0; i--) {
			if (path.getMatchedWaySegments().get(i).getId() == matchedWaySegment.getId() &&
					path.getMatchedWaySegments().get(i).getDirection().equals(matchedWaySegment.getDirection())) {
				return i;
			}
		}
		return -1;
	}
	
	/**
	 * Removes all matched way segments after index from the path
	 * @param path
	 * @param index
	 */
	private void removeSegmentsAfterIndex(IMatchedBranch path, int index) {
		// Select last segment first
		int indexToRemove = path.getMatchedWaySegments().size() - 1;
		// Delete segments until index is reached
		while (indexToRemove > index) {
			path.getMatchedWaySegments().remove(indexToRemove);
			indexToRemove--;
		}
	}
	
	public IMatchedWaySegment identifyCertainSegment(List<IMatchedBranch> nonEmptyPaths) {	
		if (nonEmptyPaths == null || nonEmptyPaths.isEmpty()) {
			return null;
		}
		
		if (nonEmptyPaths.size() == 1) {
			return findLastSegmentWithMatchedPoint(nonEmptyPaths.get(0).getMatchedWaySegments(), -1);
		}
		
		boolean valid = true;
		for (IMatchedBranch path : nonEmptyPaths) {
			if (path.getMatchedWaySegments().size() < 2) {
				valid = false;
			}
		}
		if (!valid) {
			return null;
		}
		
		IMatchedWaySegment certainSegment = null;
		
		int i = 0;
		IMatchedWaySegment certainSegmentCandidate;
		while (valid) {
			certainSegmentCandidate = null;
			for (IMatchedBranch branch : nonEmptyPaths) {
				if (branch.getMatchedWaySegments().size() > i) {
					IMatchedWaySegment currentSegment = branch.getMatchedWaySegments().get(i);
					if (certainSegmentCandidate == null) {
						certainSegmentCandidate = currentSegment;
					} else if (certainSegmentCandidate.getId() != currentSegment.getId()) {
						valid = false;
					} else if (certainSegmentCandidate.getEndPointIndex() != currentSegment.getEndPointIndex()) {
						// To avoid unmatched track points between last certain and first uncertain segment
						valid = false;
					}
				} else {
					valid = false;
				}
			}
			
			if (valid) {
				if (certainSegmentCandidate.getMatchedPoints() > 0) {
					certainSegment = certainSegmentCandidate;
				}
			}
			
			i++;
		}
		
		return certainSegment;
		
	}

	private IMatchedWaySegment findLastSegmentWithMatchedPoint(List<IMatchedWaySegment> matchedWaySegments, int startIndex) {
		IMatchedWaySegment segmentToReturn = null;
		IMatchedWaySegment currentSegment = null;
		if (startIndex <= 0) {
			startIndex = matchedWaySegments.size()-1;
		}
		for (int i=startIndex; i>=0; i--) {
			currentSegment = matchedWaySegments.get(i);
			if (currentSegment.getMatchedPoints() > 0) {
				segmentToReturn = currentSegment;
				i = -1;
			}
		}
		return segmentToReturn;
	}
}
