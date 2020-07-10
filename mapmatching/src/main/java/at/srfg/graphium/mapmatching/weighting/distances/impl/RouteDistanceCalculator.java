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

import java.util.Collections;
import java.util.List;

import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;

import at.srfg.graphium.geomutils.GeometryUtils;
import at.srfg.graphium.mapmatching.model.IMatchedWaySegment;
import at.srfg.graphium.mapmatching.model.IMatchedWaySegment.IDistancesCache;
import at.srfg.graphium.mapmatching.model.ITrack;

/**
 * {@link DistanceCalculator} for the route distance, that is the distance between
 * the matched points on the street segments.
 */
public class RouteDistanceCalculator extends DistanceCalculator {

	/**
	 * Calculates the distance from the last point of the first segment of {@code segments} to
	 * the first point of the last segment of {@code segments} traversing the segments in between.
	 */
	public double getRoutingSegmentsDistanceValue(ITrack track, List<IMatchedWaySegment> segments) {
		IMatchedWaySegment firstSegment = segments.get(0);
		IMatchedWaySegment lastSegment = segments.get(segments.size() - 1);
		
		List<IMatchedWaySegment> segmentsInBetween = RouteDistanceCalculator.getSegmentsInBetween(segments);
		
		double distanceOnFirstSegment = getDistanceToEnd(firstSegment);
		double distanceOnLastSegment = getDistanceFromStart(lastSegment);
		double distanceOnSegmentsInBetween = sumLengths(segmentsInBetween, track);
		
		return distanceOnFirstSegment + distanceOnSegmentsInBetween + distanceOnLastSegment;
	}

	/**
	 * Calculates the distance on the given segment from the last matched point to the end of the segment.
	 * 
	 * Note: The given segment is expected to have at least one matched point.
	 */
	protected double getDistanceToEnd(IMatchedWaySegment segment) {
		Point matchedPoint = segment.getMatchedPoint(segment.getEndPointIndex() - 1);
		LineString linestring = segment.getGeometry();
		
		if (segment.getDirection().isLeavingThroughEndNode()) {
			// TODO or use: linestring.getLength() - distance
			linestring = (LineString) linestring.reverse();
			linestring.setSRID(segment.getGeometry().getSRID());
		}
		
		//quick fix (TODO find reason why srids don't match)
		matchedPoint.setSRID(4326);
		linestring.setSRID(4326);
		
		return GeometryUtils.distanceOnLineStringInMeter(matchedPoint, linestring);
	}

	/**
	 * Calculates the distance on the given segment from the start of the segment to 
	 * the first matched point.
	 * 
	 * Note: The given segment is expected to have at least one matched point.
	 */
	protected double getDistanceFromStart(IMatchedWaySegment segment) {
		Point matchedPoint = segment.getMatchedPoint(segment.getStartPointIndex());
		LineString linestring = segment.getGeometry();
		
		if (segment.getDirection().isEnteringThroughEndNode()) {
			linestring = (LineString) linestring.reverse();
			linestring.setSRID(segment.getGeometry().getSRID());
		}
		
		//quick fix (TODO find reason why srids don't match)
		matchedPoint.setSRID(4326);
		linestring.setSRID(4326);

		return GeometryUtils.distanceOnLineStringInMeter(matchedPoint, linestring);
	}

	/**
	 * Calculates the distance on the given segment from point {@code trackIndexFrom} to
	 * point {@code trackIndexTo}.
	 */
	public Double getSegmentPointsDistanceValue(IMatchedWaySegment segment, int trackIndexFrom, int trackIndexTo) {
		if (trackIndexTo == segment.getEndPointIndex()) {
			return 0.0;
		}
		
		Point matchedPointStart, matchedPointEnd;
		
		if (segment.getDirection().isEnteringThroughStartNode()) {
			matchedPointStart = segment.getMatchedPoint(trackIndexFrom);
			matchedPointEnd = segment.getMatchedPoint(trackIndexTo);
		} else {
			matchedPointStart = segment.getMatchedPoint(trackIndexTo);
			matchedPointEnd = segment.getMatchedPoint(trackIndexFrom);
		}
		
		//quick fix (TODO find reason why srids don't match)
		matchedPointStart.setSRID(segment.getGeometry().getSRID());
		matchedPointEnd.setSRID(segment.getGeometry().getSRID());
		
		double distanceToStartPoint = GeometryUtils.distanceOnLineStringInMeter(matchedPointStart, segment.getGeometry());
		double distanceToEndPoint = GeometryUtils.distanceOnLineStringInMeter(matchedPointEnd, segment.getGeometry());
		
		// return the absolute value to account for u-turn segments
		return Math.abs(distanceToEndPoint - distanceToStartPoint);
	}

	private double sumLengths(List<IMatchedWaySegment> segments, ITrack track) {
		double length = 0.0;
        
		for (IMatchedWaySegment segment : segments) {
			length += segment.getLength();
		}
		
		return length;
	}

	protected static List<IMatchedWaySegment> getSegmentsInBetween(List<IMatchedWaySegment> segments) {
		if (segments.size() <= 2) {
			return Collections.emptyList();
		} else {
			return segments.subList(1, segments.size() - 1);
		}
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
		return segment.getRouteDistancesCache();
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