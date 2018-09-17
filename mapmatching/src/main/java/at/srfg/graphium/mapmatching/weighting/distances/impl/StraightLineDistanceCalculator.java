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

import at.srfg.graphium.geomutils.GeometryUtils;
import at.srfg.graphium.mapmatching.model.IMatchedWaySegment;
import at.srfg.graphium.mapmatching.model.IMatchedWaySegment.IDistancesCache;
import at.srfg.graphium.mapmatching.model.ITrack;
import at.srfg.graphium.mapmatching.model.ITrackPoint;

/**
 * {@link DistanceCalculator} implementation that calculates the straight-line
 * distance between track points.
 */
public class StraightLineDistanceCalculator extends DistanceCalculator {

	private final ITrack track;

	public StraightLineDistanceCalculator(ITrack track) {
		this.track = track;
	}
	
	/**
	 * Calculates the straight line distance from the last point of the first segment of {@code segments} to
	 * the first point of the last segment of {@code segments}.
	 */
	@Override
	public double getRoutingSegmentsDistanceValue(ITrack track, List<IMatchedWaySegment> segments) {
		IMatchedWaySegment firstSegment = segments.get(0);
		IMatchedWaySegment lastSegment = segments.get(segments.size() - 1);
		
		return getDistanceBetweenPoints(firstSegment.getEndPointIndex() - 1, lastSegment.getStartPointIndex());
	}

	/**
	 * Calculates the straight line distance between point {@code trackIndexFrom} and 
	 * point {@code trackIndexTo}.
	 */
	@Override
	public Double getSegmentPointsDistanceValue(IMatchedWaySegment segment, int trackIndexFrom, int trackIndexTo) {
		if (trackIndexTo == segment.getEndPointIndex()) {
			return 0.0;
		}
		
		return getDistanceBetweenPoints(trackIndexFrom, trackIndexTo);
	}
	
	private double getDistanceBetweenPoints(int trackIndexFrom, int trackIndexTo) {
		ITrackPoint pointFrom = track.getTrackPoints().get(trackIndexFrom);
		ITrackPoint pointTo = track.getTrackPoints().get(trackIndexTo);

		return GeometryUtils.distanceAndoyer(pointFrom.getPoint(), pointTo.getPoint());
	}

	@Override
	public Double getPenaltyForPseudoSkippedParts(IMatchedWaySegment segment) {
		return 0.0;
	}

	@Override
	public Double getPenaltyForSwitchedFrc(IMatchedWaySegment segment, IMatchedWaySegment previousSegment) {
		return 0.0;
	}

	@Override
	public IDistancesCache getDistancesCache(IMatchedWaySegment segment) {
		return segment.getStraightLineDistancesCache();
	}
	
	@Override
	public Double getPenaltyForBikesAgainstOneWay(IMatchedWaySegment segment, IMatchedWaySegment previousSegment) {
		return 0.0;
	}

	@Override
	public Double getPenaltyForBikesOnWalkways(IMatchedWaySegment segment) {
		return 0.0;
	}
}