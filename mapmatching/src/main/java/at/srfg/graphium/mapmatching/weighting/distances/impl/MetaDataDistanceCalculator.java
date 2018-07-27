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

import at.srfg.graphium.mapmatching.model.IMatchedWaySegment;
import at.srfg.graphium.mapmatching.model.ITrack;
import at.srfg.graphium.mapmatching.weighting.distances.IDistanceCalculator;
import at.srfg.graphium.mapmatching.weighting.distances.MetaData;
import at.srfg.graphium.mapmatching.weighting.distances.MetaDataType;

/**
 * {@link IDistanceCalculator} implementation which does not actually calculate distances but
 * logs for which part of the track and for which segments the distances are calculated.
 * This information is required by the {@link BadMatchDetector}.
 */
public class MetaDataDistanceCalculator implements IDistanceCalculator<MetaData>{
	
	@Override
	public MetaData getPenaltyForPseudoSkippedParts(IMatchedWaySegment segment) {
		return new MetaData(
				MetaDataType.PseudoSkippedPart,
				segment.getStartPointIndex(),
				segment.getEndPointIndex(),
				Collections.singletonList(segment));
	}

	@Override
	public MetaData getRoutingSegmentsDistance(List<IMatchedWaySegment> segments, ITrack track) {
		IMatchedWaySegment firstSegment = segments.get(0);
		IMatchedWaySegment lastSegment = segments.get(segments.size() - 1);
		int lastPointOfFirstSegment = firstSegment.getEndPointIndex() - 1;
		int firstPointOfLastSegment = lastSegment.getStartPointIndex();
		
		return new MetaData(MetaDataType.RoutedSection, lastPointOfFirstSegment, firstPointOfLastSegment, segments);
	}

	@Override
	public MetaData getDistanceForEmptyEndSegments(List<IMatchedWaySegment> segments) {
		IMatchedWaySegment firstSegment = segments.get(0);
		int endPointOfFirstSegment = firstSegment.getEndPointIndex();
		
		return new MetaData(MetaDataType.EmptyEndSegments, endPointOfFirstSegment, endPointOfFirstSegment, segments);
	}

	@Override
	public void getSegmentPointsDistance(IMatchedWaySegment segment, List<MetaData> distances) {
		MetaData metaData = new MetaData(
				MetaDataType.SingleSegment,
				segment.getStartPointIndex(),
				segment.getEndPointIndex(),
				Collections.singletonList(segment));
		
		distances.add(metaData);
	}

	@Override
	public MetaData getPenaltyForSwitchedFrc(IMatchedWaySegment segment, IMatchedWaySegment previousSegment) {
		return null;
	}

	@Override
	public MetaData getPenaltyForBikesAgainstOneWay(IMatchedWaySegment segment, IMatchedWaySegment previousSegment) {
		return null;
	}

	@Override
	public MetaData getPenaltyForBikesOnWalkways(IMatchedWaySegment segment) {
		return null;
	}

}
