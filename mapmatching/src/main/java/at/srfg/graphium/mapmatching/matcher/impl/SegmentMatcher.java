/**
 * Graphium Neo4j - Map Matching module of Graphium
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
package at.srfg.graphium.mapmatching.matcher.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.common.math.DoubleMath;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;

import at.srfg.graphium.geomutils.GeometryUtils;
import at.srfg.graphium.mapmatching.model.IMatchedBranch;
import at.srfg.graphium.mapmatching.model.IMatchedWaySegment;
import at.srfg.graphium.mapmatching.model.ITrack;
import at.srfg.graphium.mapmatching.model.ITrackPoint;
import at.srfg.graphium.mapmatching.model.impl.MatchedWaySegmentImpl;
import at.srfg.graphium.mapmatching.properties.IMapMatchingProperties;
import at.srfg.graphium.model.IWaySegment;
import at.srfg.graphium.model.OneWay;

public class SegmentMatcher {
	
	/**
	 * Tolerance value for distances (10 cm)
	 */
	private static double TOLERANCE_IN_METER = 0.1;
	
	private IMapMatchingProperties properties;

	public SegmentMatcher(IMapMatchingProperties properties) {
		this.properties = properties;
	}

	/**
	 * Tries to find a match between a segment and a part of the track.
	 * Returns an IMatchedWaySegment in case of a match or if there is no match but the length of a segment is very short;
	 * returns null in case of no match.
	 * 
	 * @param newSegment
	 * @param track
	 * @param startIndex
	 * @param matchingRadius
	 * @param branch
	 * @return
	 */
	public IMatchedWaySegment matchSegment(IWaySegment newSegment, ITrack track, int startIndex, IMatchedBranch branch) {
		IMatchedWaySegment matchedWaySegment = new MatchedWaySegmentImpl();
		matchedWaySegment.setSegment(newSegment);
		
		// check if points matched to the previous segment better fit to the current segment
		IMatchedWaySegment previousSegment = branch.getMatchedWaySegments().get(branch.getMatchedWaySegments().size() - 1);
		int newStartIndex = this.updateMatchesOfPreviousSegment(startIndex, previousSegment, matchedWaySegment, track);
		matchedWaySegment.setStartPointIndex(newStartIndex);
		
		// get last matched point + distances for the current segment
		List<Double> distances = this.getValidPointDistances(matchedWaySegment, newStartIndex, track);
		
		int newEndIndex = newStartIndex + distances.size();
		boolean atLeastOnePointMatches = newEndIndex > newStartIndex;
		
		if (atLeastOnePointMatches) {
			if (!previousSegment.isAfterSkippedPart() 
					&& emptySegmentsAtEndOfBranch(branch)) {
				// if there are empty segments at the end of the branch, 
				// rematch all points starting from the last matching segment
				newStartIndex = this.updateMatchesOfPreviousEmptySegments(previousSegment, matchedWaySegment, branch, track);
				distances = this.getValidPointDistances(matchedWaySegment, newStartIndex, track);
				
				newEndIndex = newStartIndex + distances.size();
				atLeastOnePointMatches = newEndIndex > newStartIndex;
			} 
		}
		
		// if no point could be matched to the current segment and the segment is short, 
		// check if the segment is not too far away (distance <= 2 * minMatchingRadius)
		LineString segmentLine = matchedWaySegment.getGeometry();
		boolean keepShortSegment = false;

		double segmentLength = matchedWaySegment.getLength();
		if (!atLeastOnePointMatches && this.isShortSegment(track, newStartIndex, segmentLength)) {
			if (previousSegment.getEndPointIndex() > previousSegment.getStartPointIndex()) { // check short segments only if previous segment has a match
				keepShortSegment = this.shouldKeepShortSegment(newEndIndex, track,
						segmentLine, matchedWaySegment);
			}
		}
			
		// if no point matches => return null
		if (newEndIndex == newStartIndex && !keepShortSegment) {
			return null;
		}

		matchedWaySegment.setStartPointIndex(newStartIndex);
		matchedWaySegment.setEndPointIndex(newEndIndex);
		matchedWaySegment.calculateDistances(track);
		
		return matchedWaySegment;
	}
	
	/**
	 * Finds the distances for all points that are within a certain distance of a given segment.
	 * The algorithm starts with the point at index 'startIndex' of the track and continuously checks
	 * the following points. If a point is not inside the segment buffer anymore, the next point is
	 * checked. If this point is also outside the segment buffer, the algorithm stops.
	 * 
	 * @param segment
	 * @param startIndex
	 * @param track
	 * @param matchingRadius
	 * @return The distances for every matched point.
	 */
	public List<Double> getValidPointDistances(IWaySegment segment, int startIndex, ITrack track) {
		List<Double> distances = new ArrayList<Double>();
		List<Double> tempDistances = new ArrayList<Double>();
		
		LineString line = segment.getGeometry();
		boolean valid = true;
		int currentPointIndex = startIndex;
		
		while (currentPointIndex < track.getTrackPoints().size() && valid) {
			double distance = GeometryUtils.distanceMeters(line, track.getTrackPoints().get(currentPointIndex).getPoint());
		    
			if (distance <= properties.getMaxMatchingRadiusMeter()) {
				// the point is within the search radius
				valid = true;
				currentPointIndex++;
				
				if (!tempDistances.isEmpty()) {
					distances.addAll(tempDistances);
					tempDistances.clear();
				}
				
				distances.add(distance);
			} else {
				// The point is not within the search radius, but there could be a GPS error in this and some following points. Maybe some points afterwards
				// could be matched again.
				
				// project point onto linestring
				Point projectedPoint = GeometryUtils.projectPointOnLineString(track.getTrackPoints().get(currentPointIndex).getPoint(), line);
				
				// check if projected point is start or end point of linestring
				if (projectedPoint.equals(line.getStartPoint()) || projectedPoint.equals(line.getEndPoint()) ||
					!pointsValid(tempDistances, properties)) {
					valid = false;
				} else {
					valid = true;
					currentPointIndex++;
					tempDistances.add(distance);
				}
				
			}
		}
		
		return distances;
	}

	/**
	 * if the last 5 distances are greater than threshold (10 * matching radius) => following points are out of range
	 * @param tempDistances
	 * @param properties
	 * @return
	 */
	private boolean pointsValid(List<Double> tempDistances, IMapMatchingProperties properties) {
		int nrOfPossibleWrongPoints = 5;
		if (tempDistances != null && tempDistances.size() >= nrOfPossibleWrongPoints) {
			int nrOfOutOfRange = 0;
			for (int i=tempDistances.size()-1; i>tempDistances.size()-1-nrOfPossibleWrongPoints; i--) {
				if (tempDistances.get(i) > (10 * properties.getMaxMatchingRadiusMeter())) {
					nrOfOutOfRange++;
				}
			}

			if (nrOfOutOfRange == nrOfPossibleWrongPoints) {
				return false;
			} else {
				return true;
			}

		} else {
			return true;
		}
	}

	protected int getPossibleLowerStartIndex(IMatchedWaySegment previousSegment, IMatchedWaySegment currentSegment,
			ITrack track, int startIndex) {
		return this.getPossibleLowerStartIndex(previousSegment, currentSegment, track, startIndex, true);
	}

	private enum CompareMode {Normal, Equal, TowardsCurrentSegment, Skip};
	
	/**
	 * This function checks if points matched to the previous segment better fit to the current segment.
	 * 
	 * If {@code uturnMode} is set to true, all points of a previous u-turn segment that fall into the matching
	 * radius of the current segment, are matched to the current segment, even though they might be closer to
	 * the previous segment (this is to filter 'blind' u-turn segments).
	 * If {@code uturnMode} is set to true, only those points are rematched that are closer to the current segment.
	 * 
	 * This function uses a fuzzy comparison: A point at index i is rematched to the new segment, if
	 * 	- the point falls into the matching radius of the new segment and there is a u-turn on 
	 *    the previous segment (and 'uturnMode' is activated), or
	 * 	- the distance to the new segment is smaller than the distance to the previous segment (CompareMode.Normal), or
	 * 	- the distances are equal and point (i-1) is also rematched (CompareMode.Equal), or
	 * 	- the distance to the new segment is larger and the distance from point (i-1) to the 
	 * 	  new segment is smaller or equal to the distance to the previous segment (CompareMode.Skip).
	 * 
	 * @return The new start index for the current segment.
	 */
	public int getPossibleLowerStartIndex(IMatchedWaySegment previousSegment, IMatchedWaySegment currentSegment, ITrack track,
			int startIndex, boolean uturnMode) {
		int newStartIndex =  startIndex;
		
		if (previousSegment.getStartPointIndex() != previousSegment.getEndPointIndex()) {
			int trackPointIndex = previousSegment.getEndPointIndex() - 1;
			int distancesIndex = previousSegment.getDistances().size() - 1;
			
			CompareMode mode = CompareMode.Normal;
			while (trackPointIndex >= previousSegment.getStartPointIndex() &&
					distancesIndex >= 0) {
				double distance = GeometryUtils.distanceMeters(currentSegment.getGeometry(),
						track.getTrackPoints().get(trackPointIndex).getPoint());

				if (distance < properties.getMaxMatchingRadiusMeter() && (
						(uturnMode && previousSegment.isUTurnSegment())
						|| (distance < previousSegment.getDistances().get(distancesIndex)))) {
					newStartIndex = trackPointIndex;
					mode = CompareMode.Normal;
				} else {
					if (DoubleMath.fuzzyEquals(distance, previousSegment.getDistances().get(distancesIndex),
							TOLERANCE_IN_METER)) {
						// the distance to the new segment is equal to the distance to the previous segment,
						// also check the previous points
						mode = CompareMode.Equal;
					} else if (newStartIndex == startIndex) {
						// do not skip until newStartIndex has changed at least once
						mode = CompareMode.TowardsCurrentSegment;
					} else if (mode != CompareMode.Skip) {
						// the distance to the new segment is larger than the distance to the previous segment,
						// also check the very previous point
						mode = CompareMode.Skip;
					} else {
						// the distance is larger and the next point was already skipped, stop
						break;
					}
				}
				
				trackPointIndex--;
				distancesIndex--;
			}
		}
		
		return newStartIndex;
	}
	
	/**
	 * Decides if a segment can be treated as short segment. Whether a segment is
	 * considered "short" depends on the median distance between the previous 3-5 points 
	 * in relation to the segment length. The median distance is used to account for
	 * signal outages, where the next segment is far away. In this case routing should be
	 * used to avoid appending many 'bad' segments to the path.
	 */
	boolean isShortSegment(ITrack track, int startIndex, double segmentLineLength) {
		if (startIndex > 2 && startIndex < track.getTrackPoints().size()) {
			double medianDistanceOfPreviousPoints = this.getMedianPointDistances(track,
					startIndex);
			
			return segmentLineLength < medianDistanceOfPreviousPoints + properties.getMaxMatchingRadiusMeter();
		} else {
			return segmentLineLength < properties.getMaxMatchingRadiusMeter();
		}
	}

	/**
	 * Calculates the median (not the mean!) distance between the 3-5 predecessor
	 * points of point {@code startIndex}.
	 */
	private double getMedianPointDistances(ITrack track, int startIndex) {
		List<Double> distances = new ArrayList<Double>();
		
		// get the distances between the previous points
		ITrackPoint pointTo = track.getTrackPoints().get(startIndex);
		for (int i = startIndex - 1; i >= startIndex - 10; i--) {
			if (i < 0) break;
			
			ITrackPoint pointFrom = track.getTrackPoints().get(i);
			double distanceToPreviousPoint = GeometryUtils.distanceMeters(
					pointFrom.getPoint(), 
					pointTo.getPoint());
			distances.add(distanceToPreviousPoint);
			
			pointTo = pointFrom;
		}
		
		Collections.sort(distances);
		
		// calculate the median distance
		double medianDistance;
		if (distances.size() % 2 == 1) {
			medianDistance = distances.get(distances.size() / 2);
		} else {
			double midValue1 = distances.get(distances.size() / 2 - 1);
			double midValue2 = distances.get(distances.size() / 2);
			
			medianDistance = (midValue1 + midValue2) / 2;
		}
		
		return medianDistance;
	}

	/**
	 * If no point could be matched to a short segment, check if the segment is not too far
	 * away. A sub-line from the track around the point is constructed, and then the distance
	 * from this sub-line to the segment is calculated.
	 *  
	 * @param newEndIndex
	 * @param track
	 * @param segmentLine
	 * @param matchedWaySegment
	 * @return True, if the segment should be kept.
	 */
	private boolean shouldKeepShortSegment(int newEndIndex,
			ITrack track, LineString segmentLine, IMatchedWaySegment matchedWaySegment) {
//		if (properties.isLowSamplingInterval()) {
//			return false;
//		}
		
		boolean keepShortSegment = false;
		
		// start index of track sub-line
		int trackSubLineIndexFrom = newEndIndex;
		if (newEndIndex > 0) {
			trackSubLineIndexFrom--;
		}
		
		// end index of track sub-line
		int trackSubLineIndexTo = newEndIndex;
		if (newEndIndex > track.getTrackPoints().size() - 1) {
			trackSubLineIndexTo--;
		} else if (newEndIndex < track.getTrackPoints().size() - 2) {
			trackSubLineIndexTo++;
		}
		
		if (trackSubLineIndexTo > trackSubLineIndexFrom) {
			LineString trackSubLine = createTrackSubGeometry(track, trackSubLineIndexFrom, trackSubLineIndexTo);
			
			double distance = GeometryUtils.distanceMeters(trackSubLine, segmentLine);
			if (distance <= 2 * properties.getMaxMatchingRadiusMeter()) {
				// maybe segment is too short => try next segment
				keepShortSegment = true;
			} else { 
				// segment is short, but also too far away
				keepShortSegment = false;
			}
		}
		
		return keepShortSegment;
	}

	/**
	 * Creates a sub-linestring of the track with coordinates of the track points of index trackSubLineIndexFrom to index trackSubLineIndexTo.
	 * @param track
	 * @param trackSubLineIndexFrom
	 * @param trackSubLineIndexTo
	 * @return sub-linestring
	 */
	private LineString createTrackSubGeometry(ITrack track, int trackSubLineIndexFrom, int trackSubLineIndexTo) {
		Coordinate[] coords = new Coordinate[trackSubLineIndexTo - trackSubLineIndexFrom + 1];
		
		int j = 0;
		for (int i=trackSubLineIndexFrom; i<=trackSubLineIndexTo; i++) {
			ITrackPoint tp = track.getTrackPoints().get(i);
			coords[j] = tp.getPoint().getCoordinate();
			j++;
		}
		
		return GeometryUtils.createLineString(coords, track.getLineString().getSRID());
	}

	/**
	 * Updates the matches of the previous segment. The algorithm starts at the
	 * start point of the new segment and walks backwards. Every point of the
	 * previous segment, that is closer to the new segment, is rematched.
	 * 
	 * @return The new start index for the new segment.
	 */
	public int updateMatchesOfPreviousSegment(int startIndexNewSegment,
			IMatchedWaySegment previousSegment, IMatchedWaySegment newWaySegment, ITrack track) {
		int newStartIndexNewSegment = this.getPossibleLowerStartIndex(previousSegment, newWaySegment, track, startIndexNewSegment);
		
		if (newStartIndexNewSegment < startIndexNewSegment) {
			// at least one point was rematched
			
			if (newStartIndexNewSegment <= previousSegment.getStartPointIndex()) {
				// all points from the previous segment were rematched
				previousSegment.setEndPointIndex(previousSegment.getStartPointIndex());
				previousSegment.calculateDistances(track);
			} else {
				int diff = startIndexNewSegment - newStartIndexNewSegment;
				previousSegment.setEndPointIndex(previousSegment.getEndPointIndex() - diff);

				for (int i=0; i<diff; i++) {
					previousSegment.removeLastDistance();
				}
				previousSegment.calculateDistances(track);
			}
		}
		
		return newStartIndexNewSegment;
	}

	/**
	 * This method updates the matches of empty segments and the last matching segment before the new segment.
	 * 
	 * The main idea for this method is best explained with an example: Segment A is able to match point 0 to 63.
	 * Then segment B is added to the path. B could match 45 to 49, but can not match the last point of A, so that
	 * no point is matched to B. Then segment C is added to the path, which could match point 50 t0 67. But because the
	 * previous segment (B) has no match, also no point can be rematched to C (for example see gpx track 3155 at the start).
	 * 
	 * This method makes sure that the points of the last matching segment (here: segment A) are correctly matched to the 
	 * following segments. The algorithm begins at the start point of the last matching segment, and checks if the points
	 * are better matched to the next segment. Then it proceeds with the next segment. 
	 * 
	 * @return The start index for the new segment.
	 */
	public int updateMatchesOfPreviousEmptySegments(IMatchedWaySegment previousSegment, IMatchedWaySegment newSegment,
			IMatchedBranch branch, ITrack track) {
		List<IMatchedWaySegment> segmentsToRematch = this.getSegmentsToRematch(branch);
		
		if (segmentsToRematch.isEmpty() ||
				segmentsToRematch.get(0).getEndPointIndex() < newSegment.getStartPointIndex()) {
			/* If one of the previous empty segments is the first segment after a skipped part, do no start
			 * a rematching. Otherwise the rematching might fail, because also the skipped points would be
			 * rematched.
			 * The same applies for routed segments, when only a few points were skipped.
			 */
			
			return newSegment.getStartPointIndex();
		}

		segmentsToRematch.add(newSegment);
		for (int i = 0; i < segmentsToRematch.size() - 1; i++) {
			IMatchedWaySegment currentSegment = segmentsToRematch.get(i);
			IMatchedWaySegment nextSegment = segmentsToRematch.get(i + 1);
			
			updateMatchesOfSegment(currentSegment, nextSegment, track, i == 0);
		}
		
		return newSegment.getStartPointIndex();
	}


	/**
	 * This method begins with the start point of the current segment and checks whether the point
	 * better matches to the current or the next segment. The algorithm goes on until a point is
	 * found that is closer to the next segment.
	 */
	private void updateMatchesOfSegment(IMatchedWaySegment currentSegment, IMatchedWaySegment nextSegment,
			ITrack track, boolean useCachedDistance) {

		int newEndIndex = currentSegment.getStartPointIndex();
		int currentPointIndex = currentSegment.getStartPointIndex();

		boolean valid = true;
		while (currentPointIndex < track.getTrackPoints().size() && valid) {
			Point point = track.getTrackPoints().get(currentPointIndex).getPoint();
			
			double distanceToCurrentSegment = getDistanceToSegment(currentSegment, currentPointIndex, point, useCachedDistance);
		    
			if (distanceToCurrentSegment <= properties.getMaxMatchingRadiusMeter()) {
				// the point is within the search radius
				double distanceToNextSegment = getDistanceToSegment(nextSegment, currentPointIndex, point, false);
				
				if (distanceToCurrentSegment > distanceToNextSegment) {
					// point is closer to the next segment, stop here
					valid = false;
				} else {
					valid = true;
					currentPointIndex++;
					newEndIndex++;
				}
			} else {
				// the point is not within the search radius, but let's check the next point (do it fuzzy ...)
				if (currentPointIndex < track.getTrackPoints().size() - 1) {
					Point nextPoint = track.getTrackPoints().get(currentPointIndex + 1).getPoint();
					
				    double distanceForNextPoint = getDistanceToSegment(currentSegment, currentPointIndex + 1, nextPoint, useCachedDistance);
				    
					if (distanceForNextPoint <= properties.getMaxMatchingRadiusMeter()) {
						// next point is valid again, continue
						double distanceToNextSegmentForNextPoint =
								getDistanceToSegment(nextSegment, currentPointIndex + 1, nextPoint, false);
						
						if (distanceForNextPoint > distanceToNextSegmentForNextPoint) {
							// point is closer to the next segment, stop here
							valid = false;
						} else {
							newEndIndex += 2;
							currentPointIndex += 2;
							valid = true;
							
							continue;
						}
					}
				}
				
				valid = false;
			}
		}
		
		currentSegment.setEndPointIndex(newEndIndex);
		currentSegment.calculateDistances(track);
		
		nextSegment.setStartPointIndex(currentSegment.getEndPointIndex());
		nextSegment.setEndPointIndex(-1);
	}


	/**
	 * Returns the distance between the given segment and the given point.
	 * 
	 * If {@code useCachedDistance} is set, the distance might get returned from the
	 * cached distances list of the segment.
	 */
	private double getDistanceToSegment(
			IMatchedWaySegment segment, int currentPointIndex,
			Point point, boolean useCachedDistance) {
		if (!useCachedDistance || 
				(currentPointIndex < segment.getStartPointIndex() || currentPointIndex >= segment.getEndPointIndex())) {
			return GeometryUtils.distanceMeters(segment.getGeometry(), point); 
		} else {
			return segment.getDistance(currentPointIndex);
		}
	}

	/**
	 * Returns a list of segments, at the end of the given branch, that should be rematched.
	 * The list contains all segments from the last segment of the branch (which is empty) to
	 * the last matching segment with all empty segments in between.
	 */
	private List<IMatchedWaySegment> getSegmentsToRematch(IMatchedBranch branch) {
		List<IMatchedWaySegment> segments = branch.getMatchedWaySegments();
		int indexLastSegment = segments.size() - 1;
		
		List<IMatchedWaySegment> segmentsToRematch = new ArrayList<IMatchedWaySegment>();
		
		for (int i = indexLastSegment; i >= 0; i--) {
			IMatchedWaySegment segment = segments.get(i);
			
			if (segment.getMatchedPoints() == 0) {
				if (segment.isAfterSkippedPart()) {
					/* the segment has no matching point and comes right after a skipped part.
					 * as we do not want to rematch over a skipped part, cancel the rematching.
					 */
					return Collections.emptyList();
				} else {
					segmentsToRematch.add(segment);
				}
			} else {
				// this is the last matching segment before the last segment of the branch
				segmentsToRematch.add(segment);
				break;
			}
		}
		
		// reverse list so that the order of the segments is kept
		Collections.reverse(segmentsToRematch);
		
		return segmentsToRematch;
	}
	
	/**
	 * Returns {@code true} if there are empty segments at the end of the branch.
	 */
	public boolean emptySegmentsAtEndOfBranch(IMatchedBranch branch) {
		List<IMatchedWaySegment> segments = branch.getMatchedWaySegments(); 
		
		if (segments.size() < 1) {
			return false;
		}
		
		IMatchedWaySegment lastSegment = segments.get(segments.size() - 1);
		
		return lastSegment.getMatchedPoints() == 0;
	}

	/**
	 * Returns true if the roundabout and loop check are successful.
	 */
	public boolean checkIfMatchedSegmentIsValid(IMatchedBranch branch, IMatchedWaySegment matchedSegment) {
		return this.checkRoundabout(branch, matchedSegment)
				&& this.checkLoop(branch, matchedSegment);
	}


	/**
	 * If the new segment has no matches, this method checks if the new segment
	 * is already part of the path and if no further matches has been made since then.
	 * 
	 *  The purpose of this check is to filter loops on short segments, for example
	 *  in parking areas.
	 * 
	 * @param branch
	 * @param newSegment
	 * @return True, if no loop was detected
	 */
	public boolean checkLoop(IMatchedBranch branch, IMatchedWaySegment newSegment) {
		if (newSegment.getStartPointIndex() == newSegment.getEndPointIndex()) {
			// no points are assigned to the new segment, check if it is already in the path
			
			for (int i = branch.getMatchedWaySegments().size() - 1; i >= 0; i--) {
				IMatchedWaySegment segmentInPath = branch.getMatchedWaySegments().get(i);
				
				if (segmentInPath.getId() == newSegment.getId()) {
					if (segmentInPath.getEndPointIndex() == newSegment.getEndPointIndex()) {
						// the new segment is already in the path, but no further matches were made
						return false;
					} else {
						/*
						 * The new segment is already in the path, but further matches were made.
						 * At this point we can stop looking for further occurrences, because the
						 * 'endPointIndex' of previous segments will be less or equal to the
						 * current 'endPointIndex.
						 */
						return true;
					}
				}
			}
		}
		
		return true;
	}

	/**
	 * This method checks if a path tries to go several rounds in a roundabout.
	 * 
	 * @param branch
	 * @param newSegment
	 * @return False, if the last segments belong to a roundabout and 
	 * 'newSegment' is already one of these segments.
	 */
	public boolean checkRoundabout(IMatchedBranch branch, IMatchedWaySegment newSegment) {
		if (!newSegment.isOneway().equals(OneWay.NO_ONEWAY)) {
			// the new segment is an one-way segment, if the next segments are also one-way segments,
			// it is likely that we are in a roundabout
			
			for (int i = branch.getMatchedWaySegments().size() - 1; i >= 0; i--) {
				IMatchedWaySegment currentSegment = branch.getMatchedWaySegments().get(i);
				
				if (currentSegment.isOneway().equals(OneWay.NO_ONEWAY)) {
					// the current is not an one-way segment anymore, stop
					return true;
				} else if (currentSegment.getId() == newSegment.getId()) {
					/* The new segment is the same as the current segment and all segments 
					 * in between were one-way segments. Probably we are going on a second
					 * round in a roundabout, mark the path as invalid.
					 */
					return false;
				}
			}
		}
		
		return true;
	}

	public void rematchLastSegment(IMatchedBranch branch, ITrack track) {
		IMatchedWaySegment lastSegment = branch.getMatchedWaySegments().get(branch.getMatchedWaySegments().size() - 1);
		int startIndex = lastSegment.getStartPointIndex();
		
		List<Double> distances = this.getValidPointDistances(lastSegment, startIndex, track);
		int newEndIndex = startIndex + distances.size();
		
		lastSegment.setEndPointIndex(newEndIndex);
		lastSegment.calculateDistances(track);
	}

	/**
	 * Returns the first point that can not be matched anymore to the given segment (that is
	 * the point is not within the given radius of the segment), starting from the given point index.
	 * All points i between pointIndex <= i < lastPointIndex can be assigned to the given segment.
	 * 
	 * Note that single points might get skipped to account for outliers.
	 * 
	 * @param segment
	 * @param pointIndex
	 * @param track
	 * @param matchingRadius
	 * @return
	 */
	public int getLastPointIndex(IWaySegment segment, int pointIndex, ITrack track) {
		List<Double> distances = this.getValidPointDistances(segment, pointIndex, track);
		int index = pointIndex + distances.size();
		
		return index;
	}

	public IMatchedBranch getClonedBranch(IMatchedBranch branch) {
		IMatchedBranch clonedBranch;
		
		try {
			clonedBranch = (IMatchedBranch) branch.clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException("unable to clone branch!", e);
		}
		
		return clonedBranch;
	}

	public boolean matchedAllTrackPoints(int endPointIndex, ITrack track) {
		return endPointIndex >= track.getTrackPoints().size();
	}

	/**
	 * Creates an IMatchedWaySegment without any point match
	 * 
	 * @param newSegment
	 * @param endPointIndex
	 * @return
	 */
	public IMatchedWaySegment createEmptySegment(IWaySegment newSegment, int startPointIndex, ITrack track) {
		IMatchedWaySegment segment = new MatchedWaySegmentImpl();
		segment.setStartPointIndex(startPointIndex);
		segment.setEndPointIndex(startPointIndex);
		segment.setSegment(newSegment);
		segment.calculateDistances(track);
		return segment;
	}

	/**
	 * Recalculation of start and end point indexes of routed segments
	 * @param track
	 * @param endPointIndex
	 * @param matchedWaySegments
	 */
	public void recalculateSegmentsIndexes(ITrack track, int routeStartPointIndex, int routeEndPointIndex,
			List<IMatchedWaySegment> segments) {
		
		if (segments.isEmpty()) {
			return;
		}
		
		// identify segments with minimum distance to each track point (value refers to track point)
		Map<IMatchedWaySegment, Integer> segmentsNearTrackpoints = new LinkedHashMap<>();
		// identify segments with minimum distance to each track point (value refers to next matched track point)
		Map<IMatchedWaySegment, Integer> segmentsNearTrackpointNextIndex = new LinkedHashMap<>();
		IMatchedWaySegment previousMinDistanceSegment = null;
		int indexOfFirstTrackpointNearSegment = -1;
		for (int iTp=routeStartPointIndex; iTp<=routeEndPointIndex; iTp++) {
			ITrackPoint trackpoint = track.getTrackPoints().get(iTp);
			double minDistance = -1;
			IMatchedWaySegment minDistanceSegment = null;
			for (IMatchedWaySegment segment : segments) {
				double distance = GeometryUtils.distanceMeters(segment.getGeometry(), trackpoint.getPoint());
				if ((minDistance == -1 || minDistance >= distance) && distance < properties.getMaxMatchingRadiusMeter()) {
					minDistance = distance;
					minDistanceSegment = segment;
					if (indexOfFirstTrackpointNearSegment == -1) {
						indexOfFirstTrackpointNearSegment = iTp;
					}
				}
			}
			if (minDistanceSegment != null) {
				segmentsNearTrackpoints.put(minDistanceSegment, iTp);
				
				if (previousMinDistanceSegment != null) {
					segmentsNearTrackpointNextIndex.put(previousMinDistanceSegment, iTp);
				}
				//will be overridden at next track point, except for last track point
				segmentsNearTrackpointNextIndex.put(minDistanceSegment, iTp + 1);
				
				previousMinDistanceSegment = minDistanceSegment;
			}
		}
		
		// identify start point index for segments having minimum distances to track points
		int currentIndex = indexOfFirstTrackpointNearSegment;
		if (currentIndex == -1) {
			currentIndex = routeStartPointIndex;
		}
		for (IMatchedWaySegment segment : segments) {
			segment.setStartPointIndex(currentIndex);
			Integer segmentNextTrackpointIndex = segmentsNearTrackpointNextIndex.get(segment);
			if (segmentNextTrackpointIndex != null) {
				currentIndex = segmentNextTrackpointIndex;
			}
		}
		
		// set end point indexes
		IMatchedWaySegment lastSegment = segments.get(segments.size()-1);
		if (segments.size() > 1) {
			int nextStartPointIndex = 0;
			IMatchedWaySegment currentSegment = null;
			IMatchedWaySegment nextSegment = null;
			IMatchedWaySegment nextToLastSegment = segments.get(segments.size()-2);
			if (nextToLastSegment.getStartPointIndex() > lastSegment.getStartPointIndex()) {
				lastSegment.setStartPointIndex(routeEndPointIndex);
			}
			for (int iSegs = segments.size()-2; iSegs >= 0; iSegs--) {
				currentSegment = segments.get(iSegs);
				nextSegment = segments.get(iSegs + 1);
				
				Integer segmentNextTrackpointIndex = segmentsNearTrackpoints.get(currentSegment);
				if (segmentNextTrackpointIndex != null) {
					nextStartPointIndex = segmentNextTrackpointIndex.intValue() + 1;
				} else if (nextSegment.getStartPointIndex() > 0) {
					nextStartPointIndex = nextSegment.getStartPointIndex();
				}
				currentSegment.setEndPointIndex(nextStartPointIndex);
				
			}
		}
		
		if (lastSegment.getEndPointIndex() < lastSegment.getStartPointIndex()) {
			lastSegment.setEndPointIndex(routeEndPointIndex);
		}

		// set start point indexes
		for (IMatchedWaySegment seg : segments) {
			if (seg.getStartPointIndex() == 0) {
				seg.setStartPointIndex(seg.getEndPointIndex());
			}
		}
		
		// calculate distances for each segment
		for (IMatchedWaySegment seg : segments) {
			seg.calculateDistances(track);
		}
	}
}
