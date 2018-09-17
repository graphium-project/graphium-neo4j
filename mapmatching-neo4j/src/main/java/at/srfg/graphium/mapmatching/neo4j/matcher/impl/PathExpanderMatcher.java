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
import java.util.Iterator;
import java.util.List;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.graphdb.traversal.Traverser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.srfg.graphium.mapmatching.model.IMatchedBranch;
import at.srfg.graphium.mapmatching.model.IMatchedWaySegment;
import at.srfg.graphium.mapmatching.model.ITrack;
import at.srfg.graphium.mapmatching.properties.IMapMatchingProperties;
import at.srfg.graphium.model.FuncRoadClass;
import at.srfg.graphium.model.IWaySegment;
import at.srfg.graphium.neo4j.model.WayGraphConstants;
import at.srfg.graphium.neo4j.model.WaySegmentRelationshipType;
import at.srfg.graphium.neo4j.persistence.INeo4jWayGraphReadDao;

public class PathExpanderMatcher {
	
	private static Logger log = LoggerFactory.getLogger(PathExpanderMatcher.class);
	
	private MapMatchingTask matchingTask;
	private IMapMatchingProperties properties;
	private Neo4jUtil neo4jUtil;

	public PathExpanderMatcher(MapMatchingTask mapMatchingTask,
			IMapMatchingProperties properties, Neo4jUtil neo4jUtil) {
		this.matchingTask = mapMatchingTask;
		this.properties = properties;
		this.neo4jUtil = neo4jUtil;
	}

	/**
	 * For every path, try to expand the path with connected segment and match
	 * further track points. If the tracks could not be expanded, use shortest
	 * path search to find further segments.
	 */
	List<IMatchedBranch> findPaths(
			List<IMatchedBranch> branches,
			ITrack track) {
		if (branches == null || branches.isEmpty()) {
			return null;
		}
		
		List<IMatchedBranch> newBranches = new ArrayList<IMatchedBranch>();
		List<IMatchedBranch> incomingBranchesWithDeadEnd = new ArrayList<IMatchedBranch>();
		List<IMatchedBranch> unmanipulatedBranches = new ArrayList<IMatchedBranch>();
		boolean oneOrMorePointsMatched = false;
		
		// for every path
		for (IMatchedBranch branch : branches) {
			matchingTask.checkCancelStatus();
			
			boolean oneOrMorePointsMatchedOfBranch = processPath(branch, track,
					newBranches, incomingBranchesWithDeadEnd,
					unmanipulatedBranches);
			
			if (oneOrMorePointsMatchedOfBranch) {
				oneOrMorePointsMatched = true;
			}
		}
		
		if (newBranches.isEmpty() || !oneOrMorePointsMatched) {
			/* At first it might seem odd to wait until all paths can not be matched any further, so that
			 * either shortest-path search is used for ALL paths or for NONE. Routing could also be used only
			 * for those paths that could not be expanded any further.
			 * But running alternative path search in every iteration with only those paths, 
			 * that can not be extended, might prefer bad paths with many matched points (and it is slower).
			 */
			
			newBranches = matchingTask.getAlternativePathMatcher().searchAlternativePaths(
					track, 
					branches, 
					newBranches);
			
		} else {
			// Einige Branches wurden erweitert, einige nicht. Jene, die nicht erweitert wurden, könnten trotzdem  
			// Lösungskandidaten sein. Diese könnten z.B. die höchste Anzahl an matchedPoints aufweisen und nun an einer Signallücke
			// angelangt sein (und daher kann kein normaler weiterer Match erfolgen). Die anderen Branches sind noch nicht so weit 
			// und können noch matchen. Falls die Anzahl der matchedPoints der nicht erweiterten Branches also größer oder gleich 
			// jener Anzahl der anderen Branches ist, müssen diese Branches weiter verfolgt werden.
			newBranches.addAll(unmanipulatedBranches);
		}
		
		if (!incomingBranchesWithDeadEnd.isEmpty() && newBranches != null) {
			newBranches.addAll(incomingBranchesWithDeadEnd);
		}
		
		return newBranches;
	}

	/**
	 * Tries to expand a path. First connected segments for the last path segment
	 * are queried, then these connected segment are matched. 
	 * 
	 * @param branch
	 * @param track
	 * @param newBranches
	 * @param incomingBranchesWithDeadEnd
	 * @param unmanipulatedBranches
	 * @param matchingTask
	 * @return True, it at least one point of a connected segment could be matched.
	 */
	protected boolean processPath(IMatchedBranch branch,
			ITrack track, List<IMatchedBranch> newBranches,
			List<IMatchedBranch> incomingBranchesWithDeadEnd,
			List<IMatchedBranch> unmanipulatedBranches) {
		boolean oneOrMorePointsMatched = false;
		
		IMatchedWaySegment segment = branch.getMatchedWaySegments().get(branch.getMatchedWaySegments().size() - 1);
		WaySegmentRelationshipType traverserDirection = getTraverserDirection(segment);
		
		// get all outgoing connections from the current segment
		Traverser traverser = getTraverser(
								segment, 
								traverserDirection, 
								matchingTask.getGraphDao(),
								matchingTask.getGraphName(),
								matchingTask.getGraphVersion());
			
		if (traverser != null) {
			Iterator<Path> connectedPaths = traverser.iterator();
			
			boolean branchExtended = false;
			int connectedPathCounter = 0;
			while (connectedPaths.hasNext()) {
				Path connectedPath = connectedPaths.next();
				Node connectedSegmentNode = connectedPath.endNode();
				
				IMatchedBranch clonedBranch = matchingTask.getSegmentMatcher().getClonedBranch(branch);
				if (clonedBranch == null) {
					continue;
				}

				IWaySegment connectedSegment = matchingTask.getGraphDao().mapNode(matchingTask.getGraphName(), matchingTask.getGraphVersion(), connectedSegmentNode);
				IMatchedWaySegment matchedSegment = null;

				int endPointIndexDiff = 0;
				int iTrackPointRematch = 0;
				if (segment.getLength() < properties.getMaxMatchingRadiusMeter() && !properties.isLowSamplingInterval()) {
					iTrackPointRematch = segment.getEndPointIndex() - segment.getStartPointIndex();
				}

				// try to match every connected segment (calculate matching factor)
				// in some cases it is possible that the connected segment does not match the next point but some matched points of the current segment, so
				// in those cases we have to try to rematch already matched points to the connected segment;
				// example for those cases: roundabouts!!!
				while (matchedSegment == null && endPointIndexDiff <= iTrackPointRematch) {
					matchedSegment = matchingTask.getSegmentMatcher().matchSegment(
							connectedSegment, 
							track, 
							segment.getEndPointIndex() - endPointIndexDiff++, 
							properties.getMaxMatchingRadiusMeter(), 
							clonedBranch);
				}
				endPointIndexDiff--;
				
				if ((matchedSegment == null || 														// segment could not be matched 
					  matchedSegment.getStartPointIndex() == matchedSegment.getEndPointIndex()) && 	// no points could be matched because it is a short segment
					 connectedPathCounter == 0 && !connectedPaths.hasNext() &&
					 connectedSegment.getFrc().equals(FuncRoadClass.MOTORWAY_FREEWAY_OR_OTHER_MAJOR_MOTORWAY)) {
					// next segment could not be matched, but it is the only segment connected to current (previous segment)
					// => try to match all further connected segments while the connection sizes are 1 (no crossing)
					int emptySegmentsCounter = 0;
					IMatchedWaySegment previousSegment = clonedBranch.getMatchedWaySegments().get(clonedBranch.getMatchedWaySegments().size()-1);
					do {
						emptySegmentsCounter++;
						if (matchedSegment == null) {
							matchedSegment = matchingTask.getSegmentMatcher().createEmptySegment(connectedSegment, previousSegment.getEndPointIndex(), track);
						}
						setSegmentDirection(previousSegment, matchedSegment);
						
						if (!matchingTask.getSegmentMatcher().checkIfMatchedSegmentIsValid(clonedBranch, matchedSegment)) {
							matchedSegment = null;
							break;
						}
						
						clonedBranch.addMatchedWaySegment(matchedSegment);
						
						WaySegmentRelationshipType directionType = null;
						if (isMatchedSegmentDirectionTow(previousSegment, matchedSegment)) {
							directionType = WaySegmentRelationshipType.SEGMENT_CONNECTION_ON_ENDNODE;
						} else {
							directionType = WaySegmentRelationshipType.SEGMENT_CONNECTION_ON_STARTNODE;
						}
						
						previousSegment = matchedSegment;
						
						if (numberOfFurtherConnections(connectedSegmentNode, directionType) == 1) {
							// try to match all further connected segments
							traverserDirection = getTraverserDirection(matchedSegment);
							traverser = getTraverser(
									matchedSegment, 
									traverserDirection, 
									matchingTask.getGraphDao(),
									matchingTask.getGraphName(),
									matchingTask.getGraphVersion());
								
							if (traverser != null) {
								connectedPaths = traverser.iterator();
								while (connectedPaths.hasNext()) {
									connectedPath = connectedPaths.next();
									connectedSegmentNode = connectedPath.endNode();
									connectedSegment = matchingTask.getGraphDao().mapNode(matchingTask.getGraphName(), matchingTask.getGraphVersion(), connectedSegmentNode);
									matchedSegment = matchingTask.getSegmentMatcher().matchSegment(
											connectedSegment, 
											track, 
											previousSegment.getEndPointIndex(), 
											properties.getMaxMatchingRadiusMeter(), 
											clonedBranch);
								}
							} else {
								matchedSegment = null;
								break;
							}
						} else {
							matchedSegment = null;
							break;
						}
					
					} while ((matchedSegment == null || matchedSegment.getStartPointIndex() == matchedSegment.getEndPointIndex()) && 
								emptySegmentsCounter <= properties.getMaxSegmentsForShortestPath());
					
					if (matchedSegment != null) {
						setSegmentDirection(previousSegment, matchedSegment);
						clonedBranch.addMatchedWaySegment(matchedSegment);
						// next track point could be matched
						if (matchingTask.getWeightingStrategy().branchIsValid(clonedBranch)) {
							branchExtended = true;
							newBranches.add(clonedBranch);
							
							if (matchedSegment.getStartPointIndex() < matchedSegment.getEndPointIndex()) {
								oneOrMorePointsMatched = true;
							}
						}
					}
					
				} else
				
				if (matchedSegment != null) {
					boolean segmentValid = matchingTask.getSegmentMatcher().checkIfMatchedSegmentIsValid(clonedBranch, matchedSegment);
					
					if (segmentValid) {						
						// add current segment to the branch
						clonedBranch.addMatchedWaySegment(matchedSegment);	
						// change endPointIndex of previous segment (if needed)
						if (endPointIndexDiff > 0) {
							IMatchedWaySegment previousSegment = clonedBranch.getMatchedWaySegments().get(clonedBranch.getMatchedWaySegments().size()-2);
							previousSegment.setEndPointIndex(matchedSegment.getStartPointIndex());
							previousSegment.calculateDistances(track);
						}
						// set direction
						setSegmentDirection(connectedPath, matchedSegment);
						
						if (matchingTask.getWeightingStrategy().branchIsValid(clonedBranch)) {
							branchExtended = true;
							newBranches.add(clonedBranch);
							
							if (matchedSegment.getStartPointIndex() < matchedSegment.getEndPointIndex()) {
								oneOrMorePointsMatched = true;
							}
						}
					}
				}
				
				connectedPathCounter++;

			}
			
			if (!branchExtended) {
				// end of street reached
				// put or incoming branch into outgoing newBranches (as it is) - maybe branch is OK
				storeNotExtendedPath(segment, branch,
						track, incomingBranchesWithDeadEnd,
						unmanipulatedBranches);
			}
			
		}
		
		return oneOrMorePointsMatched;
	}

	private int numberOfFurtherConnections(Node connectedSegmentNode, WaySegmentRelationshipType directionType) {
		Iterator<Relationship> itEndConns = connectedSegmentNode.getRelationships(Direction.OUTGOING, directionType).iterator();
		int endConnsCounter = 0;
		while (itEndConns.hasNext()) {
			endConnsCounter++;
			itEndConns.next();
		}
		return endConnsCounter;
	}

	/**
	 * Paths that could not be extended are still stored. Either as finished tracks,
	 * when all points are processed, or in the 'unmanipulatedBranches' list.
	 */
	private void storeNotExtendedPath(IMatchedWaySegment segment,
			IMatchedBranch branch, ITrack track,
			List<IMatchedBranch> incomingBranchesWithDeadEnd,
			List<IMatchedBranch> unmanipulatedBranches) {
		if (matchingTask.getSegmentMatcher().matchedAllTrackPoints(segment.getEndPointIndex(), track) 
				&& matchingTask.getWeightingStrategy().branchIsValid(branch)) {
			try {
				IMatchedBranch unmanipulatedBranch = (IMatchedBranch) branch.clone();
				unmanipulatedBranch.setFinished(true);
				incomingBranchesWithDeadEnd.add(unmanipulatedBranch);
			} catch (CloneNotSupportedException e) {
				log.error("unable to clone branches!", e);
			}
		} else {
			unmanipulatedBranches.add(branch);
		}
	}
	
	private Traverser getTraverser(IMatchedWaySegment segment, WaySegmentRelationshipType relationshipType, INeo4jWayGraphReadDao graphDao, String graphName, String version) {
		Node node = graphDao.getSegmentNodeBySegmentId(graphName, version, segment.getId());
		
		TraversalDescription traversalDescription = neo4jUtil.getTraverser(relationshipType);

		return traversalDescription.traverse(node);
	}
	
	static IMatchedWaySegment setSegmentDirection(IMatchedWaySegment previousSegment, IMatchedWaySegment matchedSegment) {
		if (isMatchedSegmentDirectionTow(previousSegment, matchedSegment)) {
			matchedSegment.setDirection(at.srfg.graphium.mapmatching.model.Direction.START_TO_END);
		} else {
			matchedSegment.setDirection(at.srfg.graphium.mapmatching.model.Direction.END_TO_START);
		}
		
		return matchedSegment;
	}

	static boolean isMatchedSegmentDirectionTow(IWaySegment previousSegment, IWaySegment matchedSegment) {
		if (previousSegment.getStartNodeId() == matchedSegment.getStartNodeId() ||
			previousSegment.getEndNodeId()   == matchedSegment.getStartNodeId()) {
			return true;
		} else {
			return false;
		}
	}
	
	static IMatchedWaySegment setSegmentDirection(Path path, at.srfg.graphium.mapmatching.model.IMatchedWaySegment matchedSegment) {
		if (((Long)path.lastRelationship().getProperty(WayGraphConstants.CONNECTION_NODE_ID)).equals 
			((Long)path.endNode().getProperty(WayGraphConstants.SEGMENT_STARTNODE_ID))) {
			matchedSegment.setDirection(at.srfg.graphium.mapmatching.model.Direction.START_TO_END);
		} else {
			matchedSegment.setDirection(at.srfg.graphium.mapmatching.model.Direction.END_TO_START);
		}
		
		return matchedSegment;
	}
	
	private static WaySegmentRelationshipType getTraverserDirection(IMatchedWaySegment segment) {
		WaySegmentRelationshipType direction;
		if (segment.getDirection().isLeavingThroughStartNode()) {
			direction = WaySegmentRelationshipType.SEGMENT_CONNECTION_ON_STARTNODE;
		} else {
			direction = WaySegmentRelationshipType.SEGMENT_CONNECTION_ON_ENDNODE;
		}
		return direction;
	}
}