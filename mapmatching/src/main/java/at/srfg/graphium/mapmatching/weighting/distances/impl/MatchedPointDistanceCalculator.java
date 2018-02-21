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
package at.srfg.graphium.mapmatching.weighting.distances.impl;

import java.util.List;

import at.srfg.graphium.mapmatching.model.IMatchedWaySegment;
import at.srfg.graphium.mapmatching.model.IMatchedWaySegment.IDistancesCache;
import at.srfg.graphium.mapmatching.model.ITrack;
import at.srfg.graphium.model.FormOfWay;

/**
 * {@link DistanceCalculator} for the matched-point distance, that is the
 * distance between the track points and the assigned street segments.
 */
public class MatchedPointDistanceCalculator extends DistanceCalculator {

	private final double penaltyForPseudoSkippedParts;
	private final double penaltyForSwitchedFrc;

	public MatchedPointDistanceCalculator(double penaltyForPseudoSkippedParts) {
		this.penaltyForPseudoSkippedParts = penaltyForPseudoSkippedParts;
		penaltyForSwitchedFrc = 50.0;
	}
	
	public MatchedPointDistanceCalculator(double penaltyForPseudoSkippedParts, double penaltyForSwitchedFrc) {
		this.penaltyForPseudoSkippedParts = penaltyForPseudoSkippedParts;
		this.penaltyForSwitchedFrc = penaltyForSwitchedFrc;
	}
	
	@Override
	public double getRoutingSegmentsDistanceValue(ITrack track, List<IMatchedWaySegment> segments) {
		if (segments.size() <= 2) {
			return 0;
		}
		
		List<IMatchedWaySegment> segmentsInBetween = RouteDistanceCalculator.getSegmentsInBetween(segments);
		double distancesOfSegmentsInBetween = sumDistances(segmentsInBetween);
		
		return distancesOfSegmentsInBetween;
	}

	private double sumDistances(List<IMatchedWaySegment> segments) {
		double distances = 0.0;
		
		for (IMatchedWaySegment segment : segments) {
			distances += segment.getMatchedFactor();
		}
		
		return distances;
	}

	@Override
	public Double getSegmentPointsDistanceValue(IMatchedWaySegment segment, int trackIndexFrom, int trackIndexTo) {
		return segment.getDistance(trackIndexFrom);
	}
	
	/**
	 * Calculates the average distance from the segment distances.
	 */
	@Override
	protected double reduceDistancesForSegment(List<Double> distancesForSegment) {
		if (distancesForSegment.size() <= 0) {
			return 0.0;
		} else {
			double distancesSum = super.reduceDistancesForSegment(distancesForSegment);
			
			return distancesSum / distancesForSegment.size();
		}
	}
	
	@Override
	public Double getDistanceForEmptyEndSegments(List<IMatchedWaySegment> previousSegments) {
		double distance = 0.0;
		
		for (IMatchedWaySegment segment : previousSegments) {
			if (segment.getMatchedPoints() == 0) {
				distance += segment.getMatchedFactor();
			}
		}
		
		return distance;
	}

	@Override
	public Double getPenaltyForPseudoSkippedParts(IMatchedWaySegment segment) {
		return penaltyForPseudoSkippedParts;
	}

	@Override
	public Double getPenaltyForSwitchedFrc(IMatchedWaySegment segment, IMatchedWaySegment previousSegment) {
		double totalPenalty = 0;
		
//		if (segment.isFromPathSearch()) {
			short prevFrc = (previousSegment.getFrc() != null ? previousSegment.getFrc().getValue() : 0);
			short prevFow = (previousSegment.getFormOfWay() != null ? previousSegment.getFormOfWay().getValue() : 0);
			short prevLanes = (previousSegment.getLanesTow() > 0 ? previousSegment.getLanesTow() : previousSegment.getLanesBkw());
			short frc = (segment.getFrc() != null ? segment.getFrc().getValue() : 0);
			short fow = (segment.getFormOfWay() != null ? segment.getFormOfWay().getValue() : 0);
			short lanes = (segment.getLanesTow() > 0 ? segment.getLanesTow() : segment.getLanesBkw());
			if (frc != prevFrc) {
				totalPenalty += penaltyForSwitchedFrc * Math.abs(prevFrc - frc);
			} else {
				// special treatment of slip roads (FRC=0, FormOfWay=10)
				if (frc == 0 && ((fow == 10 || prevFow == 10 ) && (fow != prevFow || lanes != prevLanes))) {
					totalPenalty += penaltyForSwitchedFrc * 3;
				}
			}
			
			// special case roundabouts: in most cases FRC is of roundabout's segments are different to connected segments
			if (fow == FormOfWay.PART_OF_ROUNDABOUT.getValue() ||
				prevFow == FormOfWay.PART_OF_ROUNDABOUT.getValue()) {
				totalPenalty = 0;
			}
//		}
		
		return totalPenalty;
	}

	@Override
	public IDistancesCache getDistancesCache(IMatchedWaySegment segment) {
		return segment.getMatchedPointDistancesCache();
	}

	@Override
	public String toString() {
		return "MatchedPointDistanceCalculator [penaltyForPseudoSkippedParts=" + penaltyForPseudoSkippedParts + "]";
	}
	
}